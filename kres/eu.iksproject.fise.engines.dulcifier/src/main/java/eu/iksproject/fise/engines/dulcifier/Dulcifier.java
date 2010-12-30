package eu.iksproject.fise.engines.dulcifier;

import eu.iksproject.kres.api.rules.util.KReSRuleList;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologySetProvider;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.OWLOntologyMerger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.fise.dereferencing.IDereferencer;
import eu.iksproject.fise.servicesapi.ContentItem;
import eu.iksproject.fise.servicesapi.EngineException;
import eu.iksproject.fise.servicesapi.EnhancementEngine;
import eu.iksproject.fise.servicesapi.ServiceProperties;
import eu.iksproject.kres.api.manager.DuplicateIDException;
import eu.iksproject.kres.api.manager.KReSONManager;
import eu.iksproject.kres.api.manager.ontology.OntologyInputSource;
import eu.iksproject.kres.api.manager.ontology.OntologyScope;
import eu.iksproject.kres.api.manager.ontology.OntologyScopeFactory;
import eu.iksproject.kres.api.manager.ontology.OntologySpace;
import eu.iksproject.kres.api.manager.ontology.OntologySpaceFactory;
import eu.iksproject.kres.api.manager.ontology.ScopeRegistry;
import eu.iksproject.kres.api.manager.ontology.UnmodifiableOntologySpaceException;
import eu.iksproject.kres.api.manager.session.KReSSession;
import eu.iksproject.kres.api.manager.session.KReSSessionManager;
import eu.iksproject.kres.api.reasoners.KReSReasoner;
import eu.iksproject.kres.api.rules.KReSRule;
import eu.iksproject.kres.api.rules.NoSuchRecipeException;
import eu.iksproject.kres.api.rules.Recipe;
import eu.iksproject.kres.api.rules.RuleStore;
import eu.iksproject.kres.api.semion.SemionManager;
import eu.iksproject.kres.api.semion.SemionRefactorer;
import eu.iksproject.kres.api.semion.SemionRefactoringException;
import eu.iksproject.kres.shared.transformation.OWLAPIToClerezzaConverter;

/**
 * 
 * This an engine to post-process the FISE enhancements. Its main goal is to
 * refactor the RDF produced by the enhancement applying some vocabulary related
 * to a specific task.
 * 
 * To do that, exploit a KReS recipe and an ontology scope.
 * 
 * The first implementation is targeted to SEO use case. * It retrieves data by
 * dereferencing the entities, * includes the DBpedia ontology * refactor the
 * data using the google rich snippets vocabulary.
 * 
 * @author andrea.nuzzolese
 * 
 */

@Component(immediate = true, metatype = true)
@Service(EnhancementEngine.class)
public class Dulcifier implements EnhancementEngine, ServiceProperties {

	/**
	 * TODO This are the scope and recipe IDs to be used by this implementation
	 * In future implementation this will be configurable
	 */
    
	@Property(value = "http://fise.iks-project.eu/dulcifier")
	public static final String DULCIFIER_SCOPE = "dulcifier.scope";

	@Property(value = "http://fise.iks-project.eu/dulcifier/recipe")
	public static final String DULCIFIER_RECIPE = "dulcifier.recipe";

    @Property(value={"peopleTypeRule[is(dbpedia:Person%2C?x) -> is(google:Person%2C?x)]",
//"myRule[has(fise:entity-reference%2C ?y%2C ?x) . has(<http://purl.org/dc/terms/relation>%2C ?y%2C ?r) ->  has(<http://purl.org/dc/terms/relation>%2C ?x%2C ?r)]",
"fiseStartRul1[has(fise:entity-reference%2C ?y%2C ?x) . has(<http://purl.org/dc/terms/relation>%2C ?y%2C ?r) . values(fise:selected-text%2C ?r%2C ?t) . values(fise:start%2C ?r%2C ?start) -> is(fise:enhancementContext%2C ?t) . values(fise:start-position%2C ?r%2C ?start) . has(fise:hasEnhancementContext%2C ?x%2C ?r)]",
"fiseEndRule1[has(fise:entity-reference%2C ?y%2C ?x) . has(<http://purl.org/dc/terms/relation>%2C ?y%2C ?r) . values(fise:selected-text%2C ?r%2C ?t) . values(fise:end%2C ?r%2C ?end) -> is(fise:enhancementContext%2C ?t) . values(fise:end-position%2C ?r%2C ?end) . has(fise:hasEnhancementContext%2C ?x%2C ?r) ]",
"fiseContextRule1[has(fise:entity-reference%2C ?y%2C ?x) . has(<http://purl.org/dc/terms/relation>%2C ?y%2C ?r) . values(fise:selected-text%2C ?r%2C ?t) . values(fise:selection-context%2C ?r%2C ?context) -> is(fise:enhancementContext%2C ?t) . values(fise:context%2C ?r%2C ?context) . has(fise:hasEnhancementContext%2C ?x%2C ?r)]",
//"fiseStartRul[has(fise:entity-reference%2C ?y%2C ?x) . values(fise:entity-label%2C ?y%2C ?z) . values(fise:selected-text%2C ?t%2C ?z) . values(fise:start%2C ?t%2C ?start) -> is(fise:enhancementContext%2C ?t) . values(fise:start-position%2C ?t%2C ?start) . has(fise:hasEnhancementContext%2C ?x%2C ?t)]",
//"fiseEndRule[has(fise:entity-reference%2C ?y%2C ?x) . values(fise:entity-label%2C ?y%2C ?z) . values(fise:selected-text%2C ?t%2C ?z) . values(fise:end%2C ?t%2C ?end) -> is(fise:enhancementContext%2C ?t) . values(fise:end-position%2C ?t%2C ?end) . has(fise:hasEnhancementContext%2C ?x%2C ?t) ]",
//"fiseContextRule[has(fise:entity-reference%2C ?y%2C ?x) . values(fise:entity-label%2C ?y%2C ?z) . values(fise:selected-text%2C ?t%2C ?z) . values(fise:selection-context%2C ?t%2C ?context) -> is(fise:enhancementContext%2C ?t) . values(fise:context%2C ?t%2C ?context) . has(fise:hasEnhancementContext%2C ?x%2C ?t)]",
"peopleNameRule[is(dbpedia:Person%2C?x) . values(foaf:name%2C?x%2C?y) -> values(google:name%2C?x%2C?y)]",
"peopleNickRule[is(dbpedia:Person%2C?x) . values(foaf:nick%2C?x%2C?y) -> values(google:nickname%2C?x%2C?y)]",
"peoplePhotoRule[is(dbpedia:Person%2C?x) . has(dbpedia:thumbnail%2C?x%2C?y) -> has(google:photo%2C?x%2C?y)]",
"peopleProfessionRule[is(dbpedia:Person%2C?x) . has(dbpedia:profession%2C?x%2C?y) -> has(google:title%2C?x%2C?y)]",
"peopleOccupationRule[is(dbpedia:Person%2C?x) . has(dbpedia:occupation%2C?x%2C?y) -> has(google:title%2C?x%2C?y)]",
"peopleRoleRule[is(dbpedia:Person%2C?x) . values(dbpedia:role%2C?x%2C?y) -> values(google:role%2C?x%2C?y)]",
"peopleHomepageRule[is(dbpedia:Person%2C?x) . has(foaf:homepage%2C?x%2C?y) -> has(google:url%2C?x%2C?y)]",
"peopleAffiliationRule[is(dbpedia:Person%2C?x) . has(dbpedia:employer%2C?x%2C?y) -> has(google:affiliation%2C?x%2C?y)]",
"peopleKnowsRule[is(dbpedia:Person%2C?x) . has(foaf:knows%2C?x%2C?y) -> has(google:friend%2C?x%2C?y)]",
"peopleAddressRule[is(dbpedia:Person%2C?x) . values(dbpedia:address%2C?x%2C?y) -> values(google:address%2C?x%2C?y)]",
"peopleOccupationRule2[is(dbpedia:Person%2C?x) . has(dc:description%2C?x%2C?y) -> has(google:title%2C?x%2C?y)]",
"peopleOccupationRule3[is(dbpedia:Person%2C?x) . has(skos:subject%2C?x%2C?y) -> has(google:affiliation%2C?x%2C?y)]",
"productTypeRule[is(dbpedia:Organisation%2C?x) . has(dbpedia:product%2C?x%2C?y) -> is(google:Product%2C?y)]",
"productNameRule1[is(dbpedia:Organisation%2C?x) . has(dbpedia:product%2C?x%2C?y) . values(foaf:name%2C?y%2C?z) -> values(google:name%2C?y%2C?z)]",
"productNameRule2[is(dbpedia:Organisation%2C?x) . has(dbpedia:product%2C?x%2C?y) . values(dbprop:name%2C?y%2C?z) -> values(google:name%2C?y%2C?z)]",
"productNameRule3[is(dbpedia:Organisation%2C?x) . has(dbpedia:product%2C?x%2C?y) . values(rdf:label%2C?y%2C?z) -> values(google:name%2C?y%2C?z)]",
"productImageRule[is(dbpedia:Organisation%2C?x) . has(dbpedia:product%2C?x%2C?y) . has(dbpedia:thumbnail%2C?y%2C?z) -> has(google:photo%2C?y%2C?z)]",
"productDescriptionRule[is(dbpedia:Organisation%2C?x) . has(dbpedia:product%2C?x%2C?y) . values(dbpedia:thumbnail%2C?y%2C?z) -> values(google:description%2C?y%2C?z)]",
"productBrandRule[is(dbpedia:Organisation%2C?x) . has(dbpedia:product%2C?x%2C?y) . values(rdf:label%2C?y%2C?z) -> values(google:brand%2C?y%2C?z)]",
"productIdentifierRule[is(dbpedia:Organisation%2C?x) . has(dbpedia:product%2C?x%2C?y) . values(dbpedia:isbn%2C?y%2C?z) -> values(google:identifier%2C?y%2C?z)]",
"productHomepageRule[is(dbpedia:Organisation%2C?x) . has(dbpedia:product%2C?x%2C?y) . values(foaf:homepage%2C?y%2C?z) -> values(google:url%2C?y%2C?z)]",
"productCategoryRule[is(dbpedia:Organisation%2C?x) . has(dbpedia:product%2C?x%2C?y) . has(skos:currency%2C?y%2C?z) -> has(google:category%2C?y%2C?z)]",
"organizationTypeRule[is(dbpedia:Organisation%2C?x) -> is(google:Organization%2C?x)]",
"organizationNameRule[is(dbpedia:Organisation%2C?x) . values(foaf:name%2C?x%2C?y) -> values(google:name%2C?x%2C?y)]",
"organizationHomepageRule[is(dbpedia:Organisation%2C?x) . values(foaf:homepage%2C?x%2C?y) -> values(google:url%2C?x%2C?y)]",
"organizationRegionRule[is(dbpedia:Organisation%2C?x) . has(dbpedia:region%2C?x%2C?y) -> has(google:region%2C?x%2C?y)]",
"organizationCountryRule[is(dbpedia:Organisation%2C?x) . has(dbpedia:locationCountry%2C?x%2C?y) -> has(google:country-name%2C?x%2C?y)]",
"organizationAddressRule[is(dbpedia:Organisation%2C?x) . values(dbprop:address%2C?x%2C?y) -> values(google:address%2C?x%2C?y)]",
"organizationStreetAddressRule[is(dbpedia:Organisation%2C?x) . values(dbprop:streetaddress%2C?x%2C?y) -> values(google:street-address%2C?x%2C?y)]",
"organizationLocationRule[is(dbpedia:Organisation%2C?x) . has(dbpedia:location%2C?x%2C?y) -> has(google:locality%2C?x%2C?y)]",
"organizationTelephoneRule[is(dbpedia:Organisation%2C?x) . values(dbprop:telephon%2C?x%2C?y) -> values(google:tel%2C?x%2C?y)]",
"organizationPostalCodeRule[is(dbpedia:Organisation%2C?x) . values(dbpedia:postalCode%2C?x%2C?y) -> has(google:postal-code%2C?x%2C?y)]",
"organizationGeoLatRule[is(dbpedia:Organisation%2C?x) . values(gn:lat%2C?x%2C?y) -> values(google:latitude%2C?x%2C?y)]",
"organizationGeoLongRule[is(dbpedia:Organisation%2C?x) . values(gn:long%2C?x%2C?y) -> values(google:longitude%2C?x%2C?y)]",
"organizationCategoryRule[is(dbpedia:Organisation%2C?x) . has(skos:subject%2C?x%2C?y) -> has(google:category%2C?x%2C?y)]",
"eventTypeRule[is(dbpedia:Event%2C?x) -> is(google:Event%2C?x)]",
"eventURLRule[is(dbpedia:Event%2C?x) . has(foaf:page%2C?x%2C?y) -> has(google:url%2C?x%2C?y)]",
"eventLocationRule1[is(dbpedia:Event%2C?x) . has(dbpedia:place%2C?x%2C?y) -> has(google:location%2C?x%2C?y)]",
"eventLocationRule2[is(dbpedia:Event%2C?x) . has(dbpedia:place%2C?x%2C?y) . has(owl:sameAs%2C?y%2C?z) . is(gn:Feature%2C?z) . values(wgs84_pos:lat%2C?z%2C?lat) . values(wgs84_pos:long%2C?z%2C?long) -> is(google:geo%2C?z) . has(google:location%2C?x%2C?y) . has(google:geo%2C?y%2C?z) . values(google:latitude%2C?z%2C?lat) . values(google:longitude%2C?z%2C?long)]",
"eventDateRule1[is(dbpedia:Event%2C?x) . values(dbpedia:date%2C?x%2C?y) -> values(google:startDate%2C?x%2C?y)]",
"eventCategoryRule[is(dbpedia:Event%2C?x) . has(skos:subject%2C?x%2C?y) -> has(google:eventType%2C?x%2C?y)]",
"eventPhotoRule[is(dbpedia:Event%2C?x) . has(dbpedia:thumbnail%2C?x%2C?y) -> has(google:photo%2C?x%2C?y)]",
"recipeClassAssertionRule[has(skos:subject%2C?x%2C<http://dbpedia.org/page/Category:World_cuisine>) -> is(google:Recipe%2C?x)]",
"recipeTypeRule[has(skos:subject%2C?x%2C<http://dbpedia.org/page/Category:World_cuisine>) . has(skos:subject%2C?x%2C?y) -> has(google:recipeType%2C?x%2C?y)]",
"recipePhotoRule1[has(skos:subject%2C?x%2C<http://dbpedia.org/page/Category:World_cuisine>) . has(dbpedia:thumbnail%2C?x%2C?y) -> has(google:photo%2C?x%2C?y)]",
"recipePhotoRule2[has(skos:subject%2C?x%2C<http://dbpedia.org/page/Category:World_cuisine>) . values(dbpedia:abstract%2C?x%2C?y) -> values(google:summary%2C?x%2C?y)]"},
                cardinality=1000, description="Rule/s specified in kReS syntax. This/these rule/s belong to the specified recipe")
    public static final String DULCIFIER_RECIPE_RULE ="dulcifier.recipe.rule";
    
    @Property(value={"dbpedia = <http://dbpedia.org/ontology/>",
    "dbprop = <http://dbpedia.org/property/>",
    "google = <http://rdf.data-vocabulary.org/#>",
    "foaf = <http://xmlns.com/foaf/0.1/>",
    "rdf = <http://www.w3.org/1999/02/22-rdf-syntax-ns#>",
    "wgs84_pos = <http://www.w3.org/2003/01/geo/wgs84_pos#>",
    "skos = <http://www.w3.org/2004/02/skos/core#>",
    "gn = <http://www.geonames.org/ontology#>",
    "fise = <http://fise.iks-project.eu/ontology/>",
    "owl = <http://www.w3.org/2002/07/owl#>",
    "dc = <http://purl.org/dc/elements/1.1/>"},
            cardinality=1000, description="Base prefix to be used for the rules.")
    public static final String DULCIFIER_RECIPE_RULE_PREFIX ="dulcifier.recipe.rule.prefix";

    @Property(value={"http://ontologydesignpatterns.org/ont/iks/kres/dbpedia_demo.owl",""}, cardinality=1000, description="To fix a set of resolvable ontology URIs for the scope's ontologies.")
    public static final String DULCIFIER_SCOPE_CORE_ONTOLOGY ="dulcifier.scope.core.ontology";

    @Property(value="true",description="If true: the previously generated RDF is deleted and substituted with the new one. If false: the new one is appended to the old RDF. Possible value: true or false.")
    public static final String DULCIFIER_APPEND = "dulcifier.append";

	@Reference
	KReSONManager onManager;

	@Reference
	IDereferencer dereferencer;

	@Reference
	RuleStore ruleStore;

	@Reference
	KReSReasoner reasoner;

	@Reference
	SemionManager semion;

	private OntologyScope scope;
	private IRI recipeIRI;
        private boolean graph_append;

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public int canEnhance(ContentItem ci) throws EngineException {
		/*
		 * Dulcifier can enhance only content items that are previously enhanced
		 * by other FISE engines, as it must be the last engine in the chain.
		 * 
		 * Works only if some enhancement has been produced.
		 */
		MGraph mGraph = ci.getMetadata();
		if (mGraph != null) {
			return ENHANCE_SYNCHRONOUS;
		} else {
			return CANNOT_ENHANCE;
		}
	}

	@Override
	public void computeEnhancements(ContentItem ci) throws EngineException {
		/**
		 * Retrieve the graph
		 */
		final MGraph mGraph = ci.getMetadata();

		/**
		 * We filter the entities recognized by the engines
		 */
		UriRef fiseEntityReference = new UriRef("http://fise.iks-project.eu/ontology/entity-reference");
		Iterator<Triple> tripleIt = mGraph.filter(null, fiseEntityReference,null);

		/**
		 * Now we prepare the KreS environment. First we create the kres session
		 * in which run the whole
		 */
		final IRI sessionIRI = createAndAddSessionSpaceToScope();


		/**
		 * Now we fetch any single entity by dereferencing the URI and we add
		 * the returned RDF to our session-ontology
		 */
		/**
		 * We retrieve the session space
		 */
		OntologySpace sessionSpace = scope.getSessionSpace(sessionIRI);

		while (tripleIt.hasNext()) {
			Triple triple = tripleIt.next();
			Resource entityReference = triple.getObject();
			/**
			 * the entity uri
			 */
			final String entityReferenceString = entityReference.toString()
					.replace("<", "").replace(">", "");
			log.debug("Trying to resolve entity " + entityReferenceString);
			/**
			 * We fetch the entity in the OntologyInputSource object
			 */
			try {
				final IRI fetchedIri = IRI.create(entityReferenceString);
				OWLOntologyManager manager = OWLManager
						.createOWLOntologyManager();
				final OWLOntology fetched = manager
						.loadOntologyFromOntologyDocument(dereferencer
								.resolve(entityReferenceString));
				OntologyInputSource ontologySource = new OntologyInputSource() {

					@Override
					public boolean hasRootOntology() {
						return (fetched != null);
					}

					@Override
					public boolean hasPhysicalIRI() {
						return true;
					}

					@Override
					public OWLOntology getRootOntology() {
						return fetched;
					}

					@Override
					public IRI getPhysicalIRI() {
						return fetchedIri;
					}
				};
				sessionSpace.addOntology(ontologySource);

				log.debug("Added " + entityReferenceString
						+ " to the session space of scope "
						+ scope.getID().toString(), this);
				
			} catch (OWLOntologyCreationException e1) {
				log.error("Cannot load the entity",e1);
			} catch (FileNotFoundException e1) {
				log.error("Cannot load the entity",e1);
			} catch (UnmodifiableOntologySpaceException e) {
				log.error("Cannot load the entity",e);
			}

		}

		/**
		 * Now we merge the RDF from the T-box - the ontologies - and the A-box
		 * - the RDF data fetched
		 * 
		 * FIXME TODO This results in a quite huge amunt of code. Since it is a
		 * very common operation for KReS clients, this should be moved to the
		 * KReS API.
		 */

		final OWLOntologyManager man = OWLManager.createOWLOntologyManager();

		OWLOntologySetProvider provider = new OWLOntologySetProvider() {

			@Override
			public Set<OWLOntology> getOntologies() {
				/**
				 * We need to remove data property assertions, since
				 */
				Set<OWLOntology> ontologies = new HashSet<OWLOntology>();
				OntologySpace sessionSpace = scope.getSessionSpace(sessionIRI);
				ontologies.addAll(sessionSpace.getOntologies());
				
				/**
				 * We add to the set the graph containing the metadata generated by previous
				 * enhancement engines. It is important becaus we want to menage during the refactoring
				 * also some information fron that graph.
				 * As the graph is provided as a Clerezza MGraph, we first need to convert it to an OWLAPI
				 * OWLOntology.
				 * There is no chance that the mGraph could be null as it was previously controlled by the JobManager
				 * through the canEnhance method and the computeEnhancement is always called iff the former returns true.  
				 */
				OWLOntology fiseMetadataOntology = OWLAPIToClerezzaConverter.clerezzaMGraphToOWLOntology(mGraph);
				ontologies.add(fiseMetadataOntology);
				return ontologies;
			}
		};

		/**
		 * We merge all the ontologies from the session space of the scope into
		 * a single ontology that will be used for the refactoring.
		 */
		OWLOntologyMerger merger = new OWLOntologyMerger(provider);

		OWLOntology ontology;
		try {
			ontology = merger.createMergedOntology(man,IRI.create("http://fise.iks-project.eu/dulcifier/integrity-check"));

			/**
			 * To perform the refactoring of the ontology to the google
			 * vocabulary we need to get the instance of the refactorer through
			 * the Semion Manager.
			 */

			SemionRefactorer refactorer = semion.getRegisteredRefactorer();

			log.debug("Refactoring recipe IRI is : " + recipeIRI);

			/**
			 * We pass the ontology and the recipe IRI to the refactorer that
			 * returns the refactorer ontology expressed by using the google
			 * vocabulary.
			 */
			try {

				Recipe recipe = ruleStore.getRecipe(recipeIRI);

				log.debug("Rules in the recipe are : "+ recipe.getkReSRuleList().size(), this);

				log.debug("The ontology to be refactor is : " + ontology, this);
				
				ontology = refactorer.ontologyRefactoring(ontology, recipeIRI);
                                
			} catch (SemionRefactoringException e) {
				log.error("The refactoring engine failed the execution.", e);
			} catch (NoSuchRecipeException e) {
				log.error("The recipe with ID " + recipeIRI
						+ " does not exists", e);
			}

			log.debug("Merged ontologies in " + ontology);
         	/**
         	 * The new generated ontology is converted to Clarezza format and than added os substitued to the old mGraph.
         	 */
			if(graph_append){
				mGraph.addAll(OWLAPIToClerezzaConverter.owlOntologyToClerezzaTriples(ontology));
				log.debug("Metadata of the content passd have been substituted",this);
			}
			else{
				mGraph.removeAll(mGraph);
				mGraph.addAll(OWLAPIToClerezzaConverter.owlOntologyToClerezzaTriples(ontology));
				log.debug("Metadata of the content is appended to the existent one",this);
			}

			/**
			 * The session needs to be destroyed, as it is no more useful.
			 */
			onManager.getSessionManager().destroySession(sessionIRI);

		} catch (OWLOntologyCreationException e) {
			log.error("Cannot create the ontology for the refactoring", e);
		}
	}

	/**
	 * Setup the KReS session
	 * 
	 * @return
	 */
	private IRI createAndAddSessionSpaceToScope() {
		/**
		 * Retrieve the session manager
		 */
		KReSSessionManager sessionManager = onManager.getSessionManager();
		log.debug("Starting create session for the dulcifier");
		/*
		 * Create and setup the session. TODO FIXME This is an operation that
		 * should be made easier for developers to do through the API
		 */
		KReSSession session = sessionManager.createSession();
		OntologySpaceFactory ontologySpaceFactory = onManager.getOntologySpaceFactory();
		OntologySpace sessionSpace = ontologySpaceFactory.createSessionOntologySpace(scope.getID());
		scope.addSessionSpace(sessionSpace, session.getID());
		/**
		 * Finally, we return the session ID to be used by the caller
		 */
		log.debug("Session " + session.getID() + " created", this);
		return session.getID();
	}

    /**
     * To create the input source necesary to load the ontology inside the scope.
     * @param uri -- A resolvable string uri.
     * @return An OntologyInputSource
     */
    private OntologyInputSource oisForScope(final String uri){
            /*
		 * The scope factory needs an OntologyInputSource as input for the core
		 * ontology space. We want to use the dbpedia ontology as core ontology
		 * of our scope.
		 */
		OntologyInputSource ois = new OntologyInputSource() {

			@Override
			public boolean hasRootOntology() {
				return true;
			}

			@Override
			public boolean hasPhysicalIRI() {
				return false;
			}

			@Override
			public OWLOntology getRootOntology() {

				InputStream inputStream;
				try {
					/*
					 * The input stream for the dbpedia ontology is obtained
					 * through the dereferencer component.
					 */
					inputStream = dereferencer.resolve(uri);
					OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
					return manager.loadOntologyFromOntologyDocument(inputStream);
				} catch (FileNotFoundException e) {
					log.error("Cannot load the ontology "+uri, e);
				} catch (OWLOntologyCreationException e) {
					log.error("Cannot load the ontology "+uri, e);
				} catch (Exception e) {
					log.error("Cannot load the ontology "+uri, e);
				}
				/** If some errors occur **/
				return null;
			}

			@Override
			public IRI getPhysicalIRI() {
				return null;
			}
		};

                return ois;
        }


        /**
         * Activating the component
         * @param context
         */
	protected void activate(ComponentContext context) {
            
     	/**
     	 * Read property to indicate if the the new eanchment metada must be append to the existing mGraph 
     	 */
		graph_append = Boolean.parseBoolean(((String)context.getProperties().get(DULCIFIER_APPEND)).toLowerCase());
		
		/**
		 * Get the Scope Factory from the ONM of KReS that allows to create new
		 * scopes
		 */
		OntologyScopeFactory scopeFactory = onManager.getOntologyScopeFactory();
    	/**
    	 * Adding ontologies to the scope core ontology.
    	 * 1) Get all the ontologies from the property.
    	 * 2) Create a base scope with an empity ontology.
    	 * 3) Retrieve the ontology space from the scope.
     	 * 4) Add the ontologies to the scope via ontology space.
     	 */
		//Step 1
		Object obj = context.getProperties().get(DULCIFIER_SCOPE_CORE_ONTOLOGY);
        String[] coreScopeOntologySet;
        if(obj instanceof String[]){
        	coreScopeOntologySet = (String[]) obj;
        }
        else{
        	String[] aux = new String[1];
        	aux[0] = (String) obj;
        	coreScopeOntologySet =aux;
        }
                
        //Step 2
        OntologyInputSource oisbase = new OntologyInputSource() {

        	@Override
			public boolean hasRootOntology() {
				return true;
			}

			@Override
			public boolean hasPhysicalIRI() {
				return false;
			}

			@Override
			public OWLOntology getRootOntology() {

				try {
					/*
					 * The input stream for the dbpedia ontology is obtained
					 * through the dereferencer component.
					 */
					OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
					return manager.createOntology();
				} catch (OWLOntologyCreationException e) {
					log.error("Cannot create the scope with empity ontology.", e);
				} catch (Exception e) {
					log.error("Cannot create the scope with empity ontology.", e);
				}
				/** If some errors occur **/
				return null;
			}

			@Override
			public IRI getPhysicalIRI() {
				return null;
			}
		};

		IRI dulcifierScopeIRI = IRI.create((String) context.getProperties().get(DULCIFIER_SCOPE));

		/**
		 * The scope is created by the ScopeFactory or loaded from the scope
		 * registry of KReS
		 */
		try {
			scope = scopeFactory.createOntologyScope(dulcifierScopeIRI, oisbase);
		} catch (DuplicateIDException e) {
			ScopeRegistry scopeRegistry = onManager.getScopeRegistry();
			scope = scopeRegistry.getScope(dulcifierScopeIRI);
		}

        /**
         * Step 3
         */
         OntologySpace ontologySpace = scope.getCoreSpace();
         
        /**
         * Step 4
         */
        ontologySpace.tearDown();
        for(int o = 0; o<coreScopeOntologySet.length; o++){
            OntologyInputSource ois = oisForScope(coreScopeOntologySet[o]);
            try {
                ontologySpace.addOntology(ois);
            } catch (UnmodifiableOntologySpaceException ex) {
                log.error("Unmodifiable Ontology SpaceException.",ex);
            }
        }
        ontologySpace.setUp();

        log.debug("The set of ontologies loaded in the core scope space is: "+ontologySpace.getOntologies()+
                "\nN.B. The root.owl ontology is the first (on the list) ontology added when the scope is created.");
        /** 
         * Set the recipe:
         * 1) Create the recipe
         * 2) Set the rule
         *  2.1) Gathering the base prefixes and put them in a single string to be added to the rule.
         *  2.2) Gathering the rules and put them in a single string to be added to the recipe.
         * 3) Add rule to the recipe
         */

        /**
         * step 1
         */
		recipeIRI = IRI.create((String) context.getProperties().get(DULCIFIER_RECIPE));

		log.debug("Start create the Recipe", this);

		ruleStore.addRecipe(recipeIRI, null);

		log.debug("The recipe has been created", this);
        
		
		/**
         * step 2
         *
         * Set the rules. The multiple rules gather from felix must be costomed to an array even if there is only one property.
         * This is due to the cardinality setting in "@Property".
         */
        
        /**
         * step 2.1
         */
        obj = context.getProperties().get(DULCIFIER_RECIPE_RULE_PREFIX);
        String[] ruleBasePrefix;
        if(obj instanceof String[]){
            ruleBasePrefix  = (String[]) obj;
        }else{
            String[] aux = new String[1];
            aux[0] = (String) obj;
            ruleBasePrefix  =aux;
        }
        
        
        String kReSRuleSyntax = "";
        
        /**
         * We add the prefixes in the rules head.
         * The syntax used for expressing the rules is the KReSRule syntax.
         */
        for(String auxruleprefix : ruleBasePrefix){
        	kReSRuleSyntax += auxruleprefix+" . ";
        }


        /**
         * step 2.2
         */
        obj = context.getProperties().get(DULCIFIER_RECIPE_RULE);
        String[] ruleSyntax;
        if(obj instanceof String[]){
            ruleSyntax  = (String[]) obj;
        }else{
            String[] aux = new String[1];
            aux[0] = (String) obj;
            ruleSyntax  =aux;
        }

        
        for(String auxrule : ruleSyntax){
        	kReSRuleSyntax += auxrule.replaceAll("%2C",",")+" . ";
        }


        if(kReSRuleSyntax.endsWith(" . "))
            kReSRuleSyntax = kReSRuleSyntax.substring(0,kReSRuleSyntax.lastIndexOf(" . ")+1);

        
        kReSRuleSyntax = kReSRuleSyntax.trim();
        
        log.debug("The complete rule to be added is: "+kReSRuleSyntax);

        //String kReSRulePerson = "dbpedia = <http://dbpedia.org/ontology/> . google = <http://rdf.data-vocabulary.org#> . foaf = <http://xmlns.com/foaf/0.1/homepage> . typeRule [is(dbpedia:Person, ?x) -> is(google:Person, ?x)] . nameRule [ values(foaf:name, ?x, ?y) -> values(google:name, ?x, ?y) ] . nickRule [ values(foaf:nick, ?x, ?y) -> values(google:nickname, ?x, ?y) ] . photoRule [ has(dbpedia:thumbnail, ?x, ?y) -> has(google:photo, ?x, ?y) ] . professionRule [has(dbpedia:profession, ?x, ?y) -> has(google:title, ?x, ?y)] . occupationRule [has(dbpedia:occupation, ?x, ?y) -> has(google:title, ?x, ?y)] . roleRule [values(dbpedia:role, ?x, ?y) -> values(google:role, ?x, ?y)] . homepageRule [has(foaf:homepage, ?x, ?y) -> has(google:url, ?x, ?y)] . affiliationRule [has(dbpedia:employer, ?x, ?y) -> has(google:affiliation, ?x, ?y)] . knowsRule [has(foaf:knows, ?x, ?y) -> has(google:friend, ?x, ?y)] . addressRule [values(dbpedia:address, ?x, ?y) -> values(google:address, ?x, ?y)]";

        /**
         * step 3
         */
        try {
        	ruleStore.addRuleToRecipe(recipeIRI.toString(), kReSRuleSyntax);
        	log.debug("Added rules to recipe " + recipeIRI.toString());
		} catch (NoSuchRecipeException e) {
			log.error("The recipe does not exists: ", e);
		}
		log.info("Activated Dulcifier engine");
                
	}

	protected void deactivate(ComponentContext context) {

		/** Deactivating the dulcifier. The procedure require:
		 * 1) get all the rules from the recipe
		 * 2) remove the recipe.
		 * 3) remove the single rule.
		 * 4) tear down the scope ontologySpace and the scope itself.
		 */

		try {
			/**
			 * step 1: get all the rule
			 */
			KReSRuleList recipeRuleList = ruleStore.getRecipe(recipeIRI).getkReSRuleList();

			/**
			 * step 2: remove the recipe
			 */
			if(ruleStore.removeRecipe(recipeIRI)){
				log.info("The recipe "+recipeIRI+" has been removed correctly");
			}
    		else{
    			log.error("The recipe "+recipeIRI+" can not be removed");
    		}

    		/**
    		 * step 3: remove the rules
    		 */
    		for(KReSRule rule : recipeRuleList){
    			if(ruleStore.removeRule(rule)){
    				log.info("The rule "+rule.getRuleName()+" has been removed correctly");
    			}
                else{
                	log.error("The rule "+rule.getRuleName()+" can not be removed");
                }
        	}

    		/**
    		 * step 4:
    		 */
    		scope.getCoreSpace().tearDown();
    		scope.tearDown();
               
//          	      //Step x: remove the ontology??????
//                OntologySpace ontologySpace = scope.getCoreSpace();
//                System.err.println(":::::::::::::: SCOPE "+)
//                //Collect all the ontologies
//                Iterator<OWLOntology> ontologies = ontologySpace.getOntologies().iterator();
//
//                //Remove the single ontology
//                ontologySpace.tearDown();
//                while(ontologies.hasNext()){
//                    OWLOntology ontology = ontologies.next();
//                    OntologyInputSource ois = oisForScope(ontology.getOWLOntologyManager().getOntologyDocumentIRI(ontology).toURI().toString());
//                    try{
//                        ontologySpace.removeOntology(ois);
//                    } catch (OntologySpaceModificationException ex) {
//                        log.error("Problem to remove the ontology "+ontology.getOntologyID(),ex);
//                  }
//                }
//                ontologySpace.setUp();
//
//                //Step y: de-registring the scope ???????
//                onManager.getScopeRegistry().deregisterScope(scope);
                
            } catch (NoSuchRecipeException ex) {
                log.error("The recipe "+recipeIRI+" doesn't exist",ex);
            }

            log.info("Deactivated Dulcifier engine");

	}

	@Override
	public Map<String, Object> getServiceProperties() {
		return Collections.unmodifiableMap(Collections.singletonMap(
				ServiceProperties.ENHANCEMENT_ENGINE_ORDERING,
				(Object) ServiceProperties.ORDERING_POST_PROCESSING));
	}
}

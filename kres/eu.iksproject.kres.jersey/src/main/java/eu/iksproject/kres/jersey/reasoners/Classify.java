/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.iksproject.kres.jersey.reasoners;

import java.io.File;
import java.net.URL;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.InconsistentOntologyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

import eu.iksproject.kres.api.format.KReSFormat;
import eu.iksproject.kres.api.manager.KReSONManager;
import eu.iksproject.kres.api.manager.ontology.OntologyScope;
import eu.iksproject.kres.api.manager.ontology.OntologySpace;
import eu.iksproject.kres.api.manager.ontology.ScopeRegistry;
import eu.iksproject.kres.api.manager.ontology.SessionOntologySpace;
import eu.iksproject.kres.api.rules.KReSRule;
import eu.iksproject.kres.api.rules.NoSuchRecipeException;
import eu.iksproject.kres.api.rules.RuleStore;
import eu.iksproject.kres.api.rules.util.KReSRuleList;
import eu.iksproject.kres.api.storage.OntologyStoreProvider;
import eu.iksproject.kres.manager.ONManager;
import eu.iksproject.kres.reasoners.KReSCreateReasoner;
import eu.iksproject.kres.reasoners.KReSRunReasoner;
import eu.iksproject.kres.reasoners.KReSRunRules;
import eu.iksproject.kres.rules.KReSKB;
import eu.iksproject.kres.rules.manager.KReSRuleStore;
import eu.iksproject.kres.rules.parser.KReSRuleParser;
import eu.iksproject.kres.storage.provider.OntologyStorageProviderImpl;

/**
 *
 * @author elvio
 */
@Path("/classify")
public class Classify {

     private RuleStore kresRuleStore;
     private OWLOntology inputowl;
     private OWLOntology scopeowl;

	protected KReSONManager onm;
	protected OntologyStoreProvider storeProvider;

	private Logger log = LoggerFactory.getLogger(getClass());

    /**
     * To get the KReSRuleStore where are stored the rules and the recipes
     *
	 * @param servletContext
	 *            {To get the context where the REST service is running.}
     */
    public Classify(@Context ServletContext servletContext){
		this.kresRuleStore = (RuleStore) servletContext
				.getAttribute(RuleStore.class.getName());
		this.onm = (KReSONManager) servletContext
				.getAttribute(KReSONManager.class.getName());
		this.storeProvider = (OntologyStoreProvider) servletContext
				.getAttribute(OntologyStoreProvider.class.getName());
		// Contingency code for missing components follows.
		/*
		 * FIXME! The following code is required only for the tests. This should
		 * be removed and the test should work without this code.
		 */
		if (storeProvider == null) {
			log
					.warn("No OntologyStoreProvider in servlet context. Instantiating manually...");
			storeProvider = new OntologyStorageProviderImpl();
		}
		if (onm == null) {
			log
					.warn("No KReSONManager in servlet context. Instantiating manually...");
			onm = new ONManager(storeProvider.getActiveOntologyStorage(),
					new Hashtable<String, Object>());
		}
       if (kresRuleStore == null) {
			log
					.warn("No KReSRuleStore with stored rules and recipes found in servlet context. Instantiating manually with default values...");
			this.kresRuleStore = new KReSRuleStore(onm,
					new Hashtable<String, Object>(), "");
			log
					.debug("PATH TO OWL FILE LOADED: "
							+ kresRuleStore.getFilePath());
        }
    }

     /**
     * To trasform a sequence of rules to a Jena Model
	 * 
	 * @param owl
	 *            {OWLOntology object contains a single recipe}
     * @return {A jena rdf model contains the SWRL rule.}
     */
	private Model fromRecipeToModel(OWLOntology owl)
			throws NoSuchRecipeException {

		// FIXME: why the heck is this method re-instantiating a rule store?!?
		RuleStore store = new KReSRuleStore(onm,
				new Hashtable<String, Object>(), owl);
        Model jenamodel = ModelFactory.createDefaultModel();

		OWLDataFactory factory = owl.getOWLOntologyManager()
				.getOWLDataFactory();
		OWLClass ontocls = factory
				.getOWLClass(IRI
						.create("http://kres.iks-project.eu/ontology/meta/rmi.owl#Recipe"));
        Set<OWLClassAssertionAxiom> cls = owl.getClassAssertionAxioms(ontocls);
        Iterator<OWLClassAssertionAxiom> iter = cls.iterator();
        IRI recipeiri = IRI.create(iter.next().getIndividual().toStringID());

		OWLIndividual recipeIndividual = factory
				.getOWLNamedIndividual(recipeiri);

		OWLObjectProperty objectProperty = factory
				.getOWLObjectProperty(IRI
						.create("http://kres.iks-project.eu/ontology/meta/rmi.owl#hasRule"));
		Set<OWLIndividual> rules = recipeIndividual.getObjectPropertyValues(
				objectProperty, store.getOntology());
        String kReSRules = "";
        for(OWLIndividual rule : rules){
			OWLDataProperty hasBodyAndHead = factory
					.getOWLDataProperty(IRI
							.create("http://kres.iks-project.eu/ontology/meta/rmi.owl#hasBodyAndHead"));
			Set<OWLLiteral> kReSRuleLiterals = rule.getDataPropertyValues(
					hasBodyAndHead, store.getOntology());

			for(OWLLiteral kReSRuleLiteral : kReSRuleLiterals){
				kReSRules += kReSRuleLiteral.getLiteral()
						+ System.getProperty("line.separator");
			}
		}

	//"ProvaParent = <http://www.semanticweb.org/ontologies/2010/6/ProvaParent.owl#> . rule1[ has(ProvaParent:hasParent, ?x, ?y) . has(ProvaParent:hasBrother, ?y, ?z) -> has(ProvaParent:hasUncle, ?x, ?z) ]");
        KReSKB kReSKB = KReSRuleParser.parse(kReSRules);
        KReSRuleList listrules = kReSKB.getkReSRuleList();
        Iterator<KReSRule> iterule = listrules.iterator();
        while(iterule.hasNext()){
            KReSRule singlerule = iterule.next();
            Resource resource = singlerule.toSWRL(jenamodel);
        }

        return jenamodel;

    }

   /**
	 * To run a classifying reasoner on a RDF input File or IRI on the base of a
	 * Scope (or an ontology) and a recipe. Can be used either HermiT or an
	 * owl-link server reasoner end-point
	 * 
	 * @param session
	 *            {A string contains the session IRI used to classify the
	 *            input.}
	 * @param scope
	 *            {A string contains either a specific scope's ontology or the
	 *            scope IRI used to classify the input.}
	 * @param recipe
	 *            {A string contains the recipe IRI from the service
	 *            http://localhost:port/kres/recipe/recipeName.}
     * @Param file {A file in a RDF (eihter RDF/XML or owl) to be classified.}
	 * @Param input_graph {A string contains the IRI of RDF (either RDF/XML or
	 *        OWL) to be classified.}
	 * @Param owllink_endpoint {A string contains the ressoner server end-point
	 *        URL.}
     * @return Return: <br/>
     *          200 The ontology is retrieved, containing only class axioms <br/>
     *          400 To run the session is needed the scope <br/>
     *          404 No data is retrieved <br/>
     *          409 Too much RDF inputs <br/>
     *          500 Some error occurred
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(value = { KReSFormat.RDF_XML, KReSFormat.TURTLE,
			KReSFormat.OWL_XML })
	public Response ontologyClassify(
			@FormParam(value = "session") String session,
			@FormParam(value = "scope") String scope,
			@FormParam(value = "recipe") String recipe,
			@FormParam(value = "input-graph") String input_graph,
			@FormParam(value = "file") File file,
			@FormParam(value = "owllink-endpoint") String owllink_endpoint) {
       
      try{
      
      if((session!=null)&&(scope==null)){
           System.err.println("ERROR: Cannot load session without scope.");
           return Response.status(Status.BAD_REQUEST).build();
        }

       //Check for input conflict. Only one input at once is allowed
       if((file!=null)&&(input_graph!=null)){
           System.err.println("ERROR: To much RDF input");
           return Response.status(Status.CONFLICT).build();
       }

      //Load input file or graph
      if(file!=null)
				this.inputowl = OWLManager.createOWLOntologyManager()
						.loadOntologyFromOntologyDocument(file);
      if(input_graph!=null)
				this.inputowl = OWLManager.createOWLOntologyManager()
						.loadOntologyFromOntologyDocument(
								IRI.create(input_graph));
      if(inputowl==null&&(session==null||scope==null))
        return Response.status(Status.NOT_FOUND).build();
      if(inputowl==null){
          if(scope!=null)
					this.inputowl = OWLManager.createOWLOntologyManager()
							.createOntology();
          else{
					this.inputowl = OWLManager.createOWLOntologyManager()
							.createOntology();
          }
      }

       //Create list to add ontologies as imported
       OWLOntologyManager mgr = inputowl.getOWLOntologyManager();
			OWLDataFactory factory = inputowl.getOWLOntologyManager()
					.getOWLDataFactory();
       List<OWLOntologyChange> additions = new LinkedList<OWLOntologyChange>();

       boolean ok = false;

      //Load ontologies from scope, RDF input and recipe
      //Try to resolve scope IRI
      if((scope!=null)&&(session==null))
      try{
          IRI iri = IRI.create(scope);
					ScopeRegistry reg = onm.getScopeRegistry();
          OntologyScope ontoscope = reg.getScope(iri);
					Iterator<OWLOntology> importscope = ontoscope
							.getCustomSpace().getOntologies().iterator();
					Iterator<OntologySpace> importsession = ontoscope
							.getSessionSpaces().iterator();

					// Add ontology as import form scope, if it is anonymus we
					// try to add single axioms.
          while(importscope.hasNext()){
            OWLOntology auxonto = importscope.next();
            if(!auxonto.getOntologyID().isAnonymous()){
							additions.add(new AddImport(inputowl, factory
									.getOWLImportsDeclaration(auxonto
											.getOWLOntologyManager()
											.getOntologyDocumentIRI(auxonto))));
            }else{
                mgr.addAxioms(inputowl,auxonto.getAxioms());
            }
         }

         //Add ontology form sessions
         while(importsession.hasNext()){
						Iterator<OWLOntology> sessionontos = importsession
								.next().getOntologies().iterator();
             while(sessionontos.hasNext()){
                OWLOntology auxonto = sessionontos.next();
                if(!auxonto.getOntologyID().isAnonymous()){
								additions
										.add(new AddImport(
												inputowl,
												factory
														.getOWLImportsDeclaration(auxonto
																.getOWLOntologyManager()
																.getOntologyDocumentIRI(
																		auxonto))));
                }else{
                    mgr.addAxioms(inputowl,auxonto.getAxioms());
                }
         }

         }
          
      }catch(Exception e){
          System.err.println("ERROR: Problem with scope: "+scope);
          e.printStackTrace();
          Response.status(Status.NOT_FOUND).build();
      }

      //Get Ontologies from session
      if((session!=null)&&(scope!=null))
      try{
          IRI iri = IRI.create(scope);
					ScopeRegistry reg = onm.getScopeRegistry();
          OntologyScope ontoscope = reg.getScope(iri);
					SessionOntologySpace sos = ontoscope.getSessionSpace(IRI
							.create(session));
          
					Set<OWLOntology> ontos = sos.getOntologyManager()
							.getOntologies();
          Iterator<OWLOntology> iteronto = ontos.iterator();

					// Add session ontologies as import, if it is anonymus we
					// try to add single axioms.
          while(iteronto.hasNext()){
            OWLOntology auxonto = iteronto.next();
            if(!auxonto.getOntologyID().isAnonymous()){
							additions.add(new AddImport(inputowl, factory
									.getOWLImportsDeclaration(auxonto
											.getOWLOntologyManager()
											.getOntologyDocumentIRI(auxonto))));
            }else{
                mgr.addAxioms(inputowl,auxonto.getAxioms());
            }
          }

      }catch(Exception e){
					System.err.println("ERROR: Problem with session: "
							+ session);
          e.printStackTrace();
          Response.status(Status.NOT_FOUND).build();
      }

			// After gathered the all ontology as imported now we apply the
			// changes
      if(additions.size()>0)
        mgr.applyChanges(additions);

      //Run HermiT if the reasonerURL is null;
      if(owllink_endpoint==null){

       try{
       if(recipe!=null) {
						OWLOntology recipeowl = OWLManager
								.createOWLOntologyManager()
								.loadOntologyFromOntologyDocument(
										IRI.create(recipe));
						// Get Jea RDF model of SWRL rule contained in the
						// recipe
            Model swrlmodel = fromRecipeToModel(recipeowl);

						// Create a reasoner to run rules contained in the
						// recipe
						KReSRunRules rulereasoner = new KReSRunRules(swrlmodel,
								inputowl);
						// Run the rule reasoner to the input RDF with the added
						// top-ontology
            inputowl = rulereasoner.runRulesReasoner();
       }

            //Create the reasoner for the classification
					KReSCreateReasoner newreasoner = new KReSCreateReasoner(
							inputowl);
					// Prepare and start the reasoner to classify ontology's
					// resources
					KReSRunReasoner reasoner = new KReSRunReasoner(newreasoner
							.getReasoner());

					// Create a new OWLOntology model where to put the inferred
					// axioms
					OWLOntology output = OWLManager.createOWLOntologyManager()
							.createOntology(inputowl.getOntologyID());
            //Initial input axioms count
            int startax = output.getAxiomCount();
            //Run the classification
            output = reasoner.runClassifyInference(output);
            //End output axioms count
            int endax = output.getAxiomCount();

            if((endax-startax)>0){
                //Some inference is retrieved
                return Response.ok(output).build();
            }else{
                //No data is retrieved
                return Response.status(Status.NOT_FOUND).build();
            }

       }catch (InconsistentOntologyException exc){
           System.err.println("CHECK ONTOLOGY CONSISTENCE");
           return Response.status(Status.NOT_FOUND).build();
        }

				// If there is an owl-link server end-point specified in the
				// form
      }else{

      try{
       if(recipe!=null) {
						OWLOntology recipeowl = OWLManager
								.createOWLOntologyManager()
								.loadOntologyFromOntologyDocument(
										IRI.create(recipe));
						// Get Jea RDF model of SWRL rule contained in the
						// recipe
         Model swrlmodel = fromRecipeToModel(recipeowl);
						// Create a reasoner to run rules contained in the
						// recipe by using the server and-point
						KReSRunRules rulereasoner = new KReSRunRules(swrlmodel,
								inputowl, new URL(owllink_endpoint));
						// Run the rule reasoner to the input RDF with the added
						// top-ontology
         inputowl = rulereasoner.runRulesReasoner();
       }
					// Create the reasoner for the consistency check by using
					// the server and-point
					KReSCreateReasoner newreasoner = new KReSCreateReasoner(
							inputowl, new URL(owllink_endpoint));
					// Prepare and start the reasoner to classify ontology's
					// resources
					KReSRunReasoner reasoner = new KReSRunReasoner(newreasoner
							.getReasoner());

					// Create a new OWLOntology model where to put the inferred
					// axioms
					OWLOntology output = OWLManager.createOWLOntologyManager()
							.createOntology(inputowl.getOntologyID());
         //Initial input axioms count
         int startax = output.getAxiomCount();
         //Run the classification
         output = reasoner.runClassifyInference(output);
         //End output axioms count
         int endax = output.getAxiomCount();

            if((endax-startax)>0){
                //Some inference is retrieved
                return Response.ok(output).build();
            }else{
                //No data is retrieved
                return Response.status(Status.NOT_FOUND).build();
            }

      }catch (InconsistentOntologyException exc){
           System.err.println("CHECK ONTOLOGY CONSISTENCE");
            return Response.status(Status.NOT_FOUND).build();
      }
      }
      }catch(Exception e){
          throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
      }

    }

}

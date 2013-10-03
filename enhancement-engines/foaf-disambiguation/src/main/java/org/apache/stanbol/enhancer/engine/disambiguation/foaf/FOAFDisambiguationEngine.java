package org.apache.stanbol.enhancer.engine.disambiguation.foaf;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.site.SiteException;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The FOAF Disambiguation Engine analyses the connected-ness of the entities
 * suggested in a content item by identifying correlated URI references of the
 * entities. The fise:confidence of the entities are increased with the number
 * of matches of references with other entities.
 * 
 * 
 * @author Dileepa Jayakody
 * 
 */
@Component(immediate = true, metatype = true)
@Service
@Properties(value = { @Property(name = EnhancementEngine.PROPERTY_NAME, value = "disambiguation-foaf") })
public class FOAFDisambiguationEngine extends
		AbstractEnhancementEngine<IOException, RuntimeException> implements
		EnhancementEngine, ServiceProperties {

	private static Logger log = LoggerFactory
			.getLogger(FOAFDisambiguationEngine.class);

	/**
	 * The default value for the execution of this Engine. Currently set to
	 * {@link ServiceProperties#ORDERING_POST_PROCESSING} + 90.
	 * <p>
	 * This should ensure that this engines runs as one of the first engines of
	 * the post-processing phase
	 */
	public static final Integer defaultOrder = ServiceProperties.ORDERING_POST_PROCESSING - 90;

	/**
	 * The {@link LiteralFactory} used to create typed RDF literals
	 */
	private final LiteralFactory literalFactory = LiteralFactory.getInstance();

	@Reference
	protected SiteManager siteManager;

	@Reference
	protected NamespacePrefixService namespacePrefixService;

	// all the URIReferences of entities and the entities which are linked to
	// those URIreferences
	// key: URIReference value: Set<EntityAnnotation>
	private Map<String, Set<UriRef>> urisReferencedByEntities = new HashMap<String, Set<UriRef>>();
	// all entity annotations suggested for the content
	private Map<UriRef, EntityAnnotation> allEnitityAnnotations = new HashMap<UriRef, EntityAnnotation>();
	//correlation scores extracted from URIReference correlations of the suggested entities
	private SortedSet<Integer> correlationScoresOfEntities = new TreeSet<Integer>();
	private String FOAF_NAMESPACE;

	@Override
	public Map<String, Object> getServiceProperties() {
		return Collections.unmodifiableMap(Collections.singletonMap(
				ENHANCEMENT_ENGINE_ORDERING, (Object) defaultOrder));
	}

	@Override
	public int canEnhance(ContentItem ci) throws EngineException {
		// check if content is present
		try {
			if ((ContentItemHelper.getText(ci.getBlob()) == null)
					|| (ContentItemHelper.getText(ci.getBlob()).trim()
							.isEmpty())) {
				return CANNOT_ENHANCE;
			}
		} catch (IOException e) {
			log.error("Failed to get the text for "
					+ "enhancement of content: " + ci.getUri(), e);
			throw new InvalidContentException(this, ci, e);
		}
		// default enhancement is synchronous enhancement
		return ENHANCE_SYNCHRONOUS;
	}

	@Override
	public void computeEnhancements(ContentItem ci) throws EngineException {
		MGraph graph = ci.getMetadata();
		FOAF_NAMESPACE = namespacePrefixService.getNamespace("foaf");
		Iterator<Triple> it = graph.filter(null, RDF_TYPE,
				TechnicalClasses.ENHANCER_TEXTANNOTATION);
		while (it.hasNext()) {
			UriRef textAnnotation = (UriRef) it.next().getSubject();
			// NOTE: this iterator will also include dc:relation between
			// fise:TextAnnotation's
			Iterator<Triple> relatedLinks = graph.filter(null, DC_RELATION,
					textAnnotation);
			// extracting selected text for foaf-name comparison
			Iterator<Triple> selectedTextsItr = graph.filter(textAnnotation,
					ENHANCER_SELECTED_TEXT, null);
			while (relatedLinks.hasNext()) {
				UriRef link = (UriRef) relatedLinks.next().getSubject();
				EntityAnnotation suggestion = EntityAnnotation.createFromUri(
						graph, link);
				// if returned suggestion is an entity-annotation proceed with
				// disambiguation process
				if (suggestion != null) {
					// process entityAnnotation for disambiguation
					try {
						// process co-referenced entity-references
						processEntityReferences(suggestion);
						// matching with foaf:name
						processFOAFNameDisambiguation(suggestion,
								selectedTextsItr);
						// adding new entity annotation to the global map
						allEnitityAnnotations.put(suggestion.getEntityUri(),
								suggestion);
					} catch (SiteException e) {
						log.error("Error occured while processing entity-annotations : \n"
								+ e.getMessage());
						e.printStackTrace();
					}
				}
			}
		}
		// calculate correlation scores for entities and disambiguate
		caculateURICorrelationScoreForEntities();
		disambiguateEntityReferences();
		// writing back to graph
		ci.getLock().writeLock().lock();
		try {
			applyDisambiguationResults(graph);
		} finally {
			ci.getLock().writeLock().unlock();
		}
		clearEhancementData();
	}

	public void clearEhancementData() {
		urisReferencedByEntities.clear();
		allEnitityAnnotations.clear();
	}

	public Entity getEntityFromEntityHub(EntityAnnotation sug)
			throws SiteException {
		UriRef entityUri = sug.getEntityUri();
		String entityhubSite = sug.getSite();
		Entity entity = null;
		// dereferencing the entity from the entityhub
		if (entityhubSite != null && entityUri != null) {
			entity = siteManager.getSite(entityhubSite).getEntity(
					entityUri.getUnicodeString());
		}
		return entity;
	}

	/**
	 * <p>
	 * Validates the foaf:name of the entity with the selected text from the
	 * content, if matched the confidence of the EntityAnnotation is increased.
	 * </p>
	 * 
	 * @param EntityAnnotation
	 *            ea
	 * @param The
	 *            fise:selected-text tokens of the content selectedTextsTriples
	 * @throws SiteException
	 */
	public void processFOAFNameDisambiguation(EntityAnnotation ea,
			Iterator<Triple> selectedTextsTriples) throws SiteException {
		Entity entity = this.getEntityFromEntityHub(ea);
		Representation entityRep = entity.getRepresentation();
		String foafNameURI = this.FOAF_NAMESPACE + "name";
		//when comparing selected text with foaf:name, all whitespaces and non-word chars are removed
		String regexPattern = "[\\s\\W]";
		Text foafNameText = ((Text) entityRep.getFirst(foafNameURI));
		if (foafNameText != null) {
			String foafName = foafNameText.getText();
			// if the selected-text matches exactly with the foaf-name then
			// increase the ds by 1
			Double foafNameScore = 0.0;
			while (selectedTextsTriples.hasNext()) {
				String selectedText = ((Literal) selectedTextsTriples.next()
						.getObject()).getLexicalForm();
				String selectedTextStr = selectedText.replaceAll(regexPattern, "");
				if (foafName != null) {
					String foafNameStr = foafName.replaceAll(regexPattern, "");
					System.out.println("the regexed foafName:" + foafNameStr);
					if (selectedTextStr.equalsIgnoreCase(foafNameStr)) {
						foafNameScore++;
						break;
					}
				}

			}
			ea.setFoafNameDisambiguationScore(foafNameScore);
		}
	}

	/**
	 * <p>
	 * Processes all the URIReference type fields of entities and add them to
	 * the global map as keys and entities as values
	 * </p>
	 * 
	 * @param The
	 *            EntityAnnotation to process entityAnnotation
	 * @throws SiteException
	 */
	public void processEntityReferences(EntityAnnotation entityAnnotation)
			throws SiteException {
		Entity entity = this.getEntityFromEntityHub(entityAnnotation);
		Representation entityRep = entity.getRepresentation();
		Iterator<String> fields = entityRep.getFieldNames();
		int linksFromEntity = 0;
		while (fields.hasNext()) {
			String field = fields.next();
			Iterator<org.apache.stanbol.entityhub.servicesapi.model.Reference> urisReferenced = entityRep
					.getReferences(field);
			while (urisReferenced.hasNext()) {
				org.apache.stanbol.entityhub.servicesapi.model.Reference uriReference = urisReferenced
						.next();
				linksFromEntity++;
				String referenceString = uriReference.getReference();
				if (urisReferencedByEntities.containsKey(referenceString)) {
					Set<UriRef> eas = urisReferencedByEntities
							.get(referenceString);
					eas.add(entityAnnotation.getEntityUri());
					urisReferencedByEntities.put(referenceString, eas);
				} else {
					Set<UriRef> eas = new HashSet<UriRef>();
					eas.add(entityAnnotation.getEntityUri());
					// key:link, value:entityAnnotation set referencing link
					urisReferencedByEntities.put(referenceString, eas);
				}
			}
		}
		entityAnnotation.setReferencesFromEntity(linksFromEntity);
	}

	/**
	 * <p>
	 * Counts the number of correlated URI-References and add that score to
	 * correlated entities
	 * </p>
	 */
	public void caculateURICorrelationScoreForEntities() {
		for (String uriReference : urisReferencedByEntities.keySet()) {
			Set<UriRef> entityAnnotationsLinked = urisReferencedByEntities
					.get(uriReference);
			int correlationScoreForURI = entityAnnotationsLinked.size();
			// adding the correlationscore to the global set for normalization
			// requirements
			this.correlationScoresOfEntities.add(new Integer(
					correlationScoreForURI));
			for (UriRef ea : entityAnnotationsLinked) {
				if (allEnitityAnnotations.get(ea) != null) {
					allEnitityAnnotations.get(ea).increaseCorrelationScore(
							correlationScoreForURI);
				}
			}
		}
	}

	public void disambiguateEntityReferences() {
		int allUriRefs = urisReferencedByEntities.keySet().size();
		for (EntityAnnotation ea : allEnitityAnnotations.values()) {
			this.performEntityReferenceDisambiguation(ea, allUriRefs);
		}
	}

	public void performEntityReferenceDisambiguation(EntityAnnotation ea,
			int allUriReferences) {
		int correlationScoreForEntity = ea.getCorrelationScore();
		int refsFromEntity = ea.getReferencesFromEntity();
		int correlationsWithOtherEntities = correlationScoreForEntity
				- refsFromEntity;
		ea.setCorrelationScore(correlationsWithOtherEntities);
	}

	public void applyDisambiguationResults(MGraph graph) {
		int max = this.correlationScoresOfEntities.last();
		int min = this.correlationScoresOfEntities.first();
	
		for (EntityAnnotation ea : allEnitityAnnotations.values()) {
			// calculate total dc
			ea.calculateFoafNameDisambiguatedConfidence();
			ea.calculateEntityReferenceDisambiguatedConfidence(max, min);
			ea.calculateDisambiguatedConfidence();
			/*
			System.out.println("\n\nEntity : " + ea.getEntityLabel()
					+ "\n site: " + ea.getSite() + "\n originalconf: "
					+ ea.getOriginalConfidnece().toString()
					+ "\n no of links from entity: "
					+ ea.getReferencesFromEntity()
					+ "\n  entity foafname-score :"
					+ ea.getFoafNameDisambiguationScore()
					+ "\n no of matches : " + ea.getCorrelationScore()
					+ "\n  entity correlation-score :"
					+ ea.getCorrelationScore() + "\n foaf name disamb-conf: "
					+ ea.getFoafNameDisambiguatedConfidence().toString()
					+ "\n entity reference disamb-conf: "
					+ ea.getEntityReferenceDisambiguatedConfidence().toString()
					+ "\n Total disamb-conf: "
					+ ea.getDisambiguatedConfidence().toString());
*/
			EnhancementEngineHelper.set(graph, ea.getUriLink(),
					ENHANCER_CONFIDENCE, ea.getDisambiguatedConfidence(),
					literalFactory);
			// adding this engine as a contributor
			EnhancementEngineHelper.addContributingEngine(graph,
					ea.getUriLink(), this);
		}
	}

	/**
	 * Activate and read the properties
	 * 
	 * @param ce
	 *            the {@link ComponentContext}
	 */
	@Activate
	protected void activate(ComponentContext ce) throws ConfigurationException {
		try {
			super.activate(ce);

		} catch (IOException e) {
			log.error("Error in activation method.", e);
		}
	}

	/**
	 * Deactivate
	 * 
	 * @param ce
	 *            the {@link ComponentContext}
	 */
	@Deactivate
	protected void deactivate(ComponentContext ce) {
		super.deactivate(ce);
	}
}

package org.apache.stanbol.enhancer.engine.disambiguation.freebase.graph;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.engine.disambiguation.freebase.graph.constants.FreebaseDisambiguatorEngineConstants;
import org.apache.stanbol.enhancer.engine.disambiguation.freebase.graph.helper.FreebaseDisambiguatorEngineHelper;
import org.apache.stanbol.enhancer.engine.disambiguation.mlt.DisambiguationData;
import org.apache.stanbol.enhancer.engine.disambiguation.mlt.SavedEntity;
import org.apache.stanbol.enhancer.engine.disambiguation.mlt.Suggestion;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;

@Component(immediate = true, metatype = true)
@Service
@Properties(value = {@Property(name = EnhancementEngine.PROPERTY_NAME, value = "freebase-disambiguation"),
                     @Property(name = FreebaseDisambiguatorEngine.GRAPH_LOCATION)})
public class FreebaseDisambiguatorEngine extends AbstractEnhancementEngine<IOException,RuntimeException>
        implements EnhancementEngine, ServiceProperties {
    public static final String GRAPH_LOCATION = "graph.location";

    private static Logger log = LoggerFactory.getLogger(FreebaseDisambiguatorEngine.class);

    /**
     * The default value for the execution of this Engine. Currently set to
     * {@link ServiceProperties#ORDERING_POST_PROCESSING} - 600.
     * <p>
     * This should ensure that this engines runs as one of the first engines of the post-processing phase
     */
    public static final Integer defaultOrder = ServiceProperties.ORDERING_POST_PROCESSING - 600;

    /**
     * The plain text might be required for determining the extraction context
     */

    public static final String PLAIN_TEXT_MIMETYPE = "text/plain";
    /**
     * Contains the only supported mime type {@link #PLAIN_TEXT_MIMETYPE}
     */
    public static final Set<String> SUPPORTED_MIMETYPES = Collections.singleton(PLAIN_TEXT_MIMETYPE);

    /**
     * <p>
     * Graph (Neo4jGraph) containing the whole graph
     * </p>
     */
    private Graph graph;

    /*
     * The following parameters describe the ratio of the original fise:confidence values and the
     * disambiguation scores contributing to the final disambiguated fise:confidence
     * 
     * TODO: make configurable
     */
    /**
     * Default ratio for Disambiguation (2.0)
     */
    public static final double DEFAULT_DISAMBIGUATION_RATIO = 2.0;
    /**
     * Default ratio for the original fise:confidence of suggested entities
     */
    public static final double DEFAULT_CONFIDENCE_RATIO = 1.0;

    /**
     * The weight for disambiguation scores <code>:= disRatio/(disRatio+confRatio)</code>
     */
    private double disambiguationWeight = DEFAULT_DISAMBIGUATION_RATIO
                                          / (DEFAULT_DISAMBIGUATION_RATIO + DEFAULT_CONFIDENCE_RATIO);
    /**
     * The weight for the original confidence scores <code>:= confRatio/(disRatio+confRatio)</code>
     */
    private double confidenceWeight = DEFAULT_CONFIDENCE_RATIO
                                      / (DEFAULT_DISAMBIGUATION_RATIO + DEFAULT_CONFIDENCE_RATIO);

    /**
     * The {@link LiteralFactory} used to create typed RDF literals
     */
    private final LiteralFactory literalFactory = LiteralFactory.getInstance();

    /**
     * Returns the properties containing the {@link ServiceProperties#ENHANCEMENT_ENGINE_ORDERING}
     */
    public Map<String,Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(ENHANCEMENT_ENGINE_ORDERING,
            (Object) defaultOrder));
    }

    /**
     * /**
     * <p>
     * Checks whether the current {@code ContentItem} can be enhanced or not
     * </p>
     * 
     * @param ci
     *            The ContentItem to be enhanced
     * 
     * @return An integer indicating whether the ContentItem can be enhanced or not. There are a few constants
     *         that can be returned: CANNOT_ENHANCE, ENHANCE_SYNCHRONOUS, ENHANCE_ASYNC
     */
    public int canEnhance(ContentItem ci) throws EngineException {
        // check if content is present
        try {
            if ((ContentItemHelper.getText(ci.getBlob()) == null)
                || (ContentItemHelper.getText(ci.getBlob()).trim().isEmpty())) {
                return CANNOT_ENHANCE;
            }
        } catch (IOException e) {
            log.error("Failed to get the text for " + "enhancement of content: " + ci.getUri(), e);
            throw new InvalidContentException(this, ci, e);
        }
        // default enhancement is synchronous enhancement
        return ENHANCE_SYNCHRONOUS;
    }

    /**
     * <p>
     * This function first evaluates all the possible ambiguations of each text annotation detected. The ids
     * of the referenced entities are used to check relations in the Freebase graph in order to obtain a new
     * disambiguation score based on the relations between entities in the graph
     * </p>
     * 
     * <p>
     * The results obtained are used to calculate new confidence values which are updated in the metadata.
     * </p>
     */
    public void computeEnhancements(ContentItem ci) throws EngineException {

        MGraph metadata = ci.getMetadata();

        // Read the data from the content item
        DisambiguationData disData;
        ci.getLock().readLock().lock();
        try {
            disData = DisambiguationData.createFromContentItem(ci);
        } finally {
            ci.getLock().readLock().unlock();
        }

        /*
         * Disambiguation Steps
         * 
         * 1. Generate the sets of entities for each text annotation
         * 
         * 2. Generate all the possible solutions for the text. That means, generate the combination of
         * entities of each set (cartesian product)
         * 
         * 3. Construct the subgraph from the whole graph using only the extracted entities
         * 
         * 4. Filter the possible solutions, removing those entities from each possible solution which are not
         * valid, i.e isolated entities in the graph
         * 
         * 5. For each possible solution, calculate the shortest path between every pair of entities in it and
         * store the disambiguation score for the possible solution
         * 
         * 6. For each entity annotation, modify the confidence value using the disambiguation score for the
         * suggestion of the entity annotation
         */

        /*
         * (1) Generating the Map TextAnnotation->List<UriRef EntityUri> to be used to generate the possible
         * solutions using cartesian product (one element for each entity of every text annotation)
         * 
         * Generating the allEntities list to be used to generate the subgraph
         */
        List<UriRef> allEntities = new ArrayList<UriRef>();
        Multimap<UriRef,Suggestion> taSuggestions = ArrayListMultimap.create();

        for (UriRef textAnnotation : disData.textAnnotations.keySet()) {
            SavedEntity savedEntity = disData.textAnnotations.get(textAnnotation);
            for (Suggestion suggestion : savedEntity.getSuggestions()) {
                taSuggestions.put(textAnnotation, suggestion);
                allEntities.add(suggestion.getEntityUri());
            }
        }

        /*
         * (2) Generate possible solutions
         */
        Set<List<Suggestion>> possibleSolutions = FreebaseDisambiguatorEngineHelper
                .generatePossibleSolutions(taSuggestions);

        /*
         * (3) Generating the subgraph
         */
        log.info("Generating Subgraph for current entities");
        Graph subgraph = FreebaseDisambiguatorEngineHelper.generateSubgraph(this.graph, allEntities);
        log.info("Subgraph generated");

        /*
         * Stops the disambiguation if the subgraph has no edges
         */
        if (!subgraph.getEdges().iterator().hasNext()) {
            // Graph with all its vertices isolated, don't modify the confidence
            log.info("Graph with all its vertices isolated, don't modify the confidence of EntityAnnotation's");
            return;
        }

        this.dumpGraph(subgraph);

        /*
         * (4) Filter possible solutions;
         */
        possibleSolutions = FreebaseDisambiguatorEngineHelper.filterPossibleSolutions(possibleSolutions,
            subgraph);

        /*
         * (5) Calculate the shortest-path between entities in the possible solution and modify the
         * disambiguation score of each Suggestion
         */

        FreebaseDisambiguatorEngineHelper.calculateDisambiguationScores(possibleSolutions, subgraph, disData);

        /*
         * (6) Modify confidence values of the entity annotations
         */
        ci.getLock().writeLock().lock();
        try {
            this.disambiguateAndApplyResults(metadata, disData);
        } finally {
            ci.getLock().writeLock().unlock();
        }

    }

    /**
     * <p>
     * Calculates the new confidence value for each Suggestion using the normalized disambiguation score of
     * the Suggestion
     * <p>
     * Applies the disambiguation results to the enhancements, modifying the confidence value of the entity
     * annotations
     * </p>
     * 
     * @param metadata
     *            the {@code MGraph} instance containing the enhancement triples
     * @param disData
     *            the {@code DisambiguationData} instance containing the Suggestion objects and their
     *            normalized disambiguation scores
     */
    private void disambiguateAndApplyResults(MGraph metadata, DisambiguationData disData) {
        for (SavedEntity savedEntity : disData.textAnnotations.values()) {
            for (Suggestion suggestion : savedEntity.getSuggestions()) {

                Double ns = suggestion.getNormalizedDisambiguationScore();
                /*
                 * Suggestions with null in disambiguated confidence means that they don't form part of any
                 * possible solution so their confidence value won't change
                 */
                if (ns == null) {
                    suggestion.setDisambiguatedConfidence(suggestion.getOriginalConfidnece());
                    continue;
                }

                Double c = suggestion.getOriginalConfidnece() == null ? 0 : suggestion
                        .getOriginalConfidnece();

                Double dc = c * confidenceWeight + ns * disambiguationWeight;
                suggestion.setDisambiguatedConfidence(dc);

                if (suggestion.getDisambiguatedConfidence() != null) {
                    // change the confidence
                    log.info("Modifying confidence for "
                             + suggestion.getEntityAnnotation().getUnicodeString() + " (Entity "
                             + suggestion.getEntityUri().getUnicodeString() + ") -> Last confidence: "
                             + suggestion.getOriginalConfidnece() + " - New confidence: "
                             + suggestion.getDisambiguatedConfidence());
                    EnhancementEngineHelper.set(metadata, suggestion.getEntityAnnotation(),
                        ENHANCER_CONFIDENCE, suggestion.getDisambiguatedConfidence(), literalFactory);
                    EnhancementEngineHelper.addContributingEngine(metadata, suggestion.getEntityAnnotation(),
                        this);
                }
            }
        }

    }

    /**
     * <p>
     * Dump the graph to the log debug
     * </p>
     * 
     * @param graph
     *            the {@code Graph> to dump

     */
    private void dumpGraph(Graph graph) {
        if (log.isDebugEnabled() || true) {
            log.debug("Dumping graph");
            for (Vertex vertex : graph.getVertices()) {
                log.debug("Vertex "
                          + vertex
                          + " : "
                          + vertex.getProperty(FreebaseDisambiguatorEngineConstants.VERTEX_ENTITY_URI_PROPERTY));
            }
            for (Edge edge : graph.getEdges()) {
                log.debug("Edge: " + edge);
            }
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
            @SuppressWarnings("unchecked")
            Dictionary<String,Object> properties = ce.getProperties();
            // update the service URL if it is defined
            // if (properties.get(FORMCEPT_SERVICE_URL) != null) {
            // this.serviceURL = (String) properties.get(FORMCEPT_SERVICE_URL);
            // }
            if (properties.get(GRAPH_LOCATION) != null
                && !((String) properties.get(GRAPH_LOCATION)).isEmpty()) {
                String location = (String) properties.get(GRAPH_LOCATION);
                log.info("Loading graph. Location: " + location);
                this.graph = new Neo4jGraph(location);
                log.info("Graph loaded");
            } else throw new IOException(GRAPH_LOCATION
                                         + " property is null or empty. Failed to initialize the engine");
        } catch (IOException e) { // log
            log.error("Failed to update the configuration");
        } catch (Exception e) {
            e.printStackTrace();
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
        try {
            // Closing the graph
            super.deactivate(ce);
            if (this.graph != null) this.graph.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

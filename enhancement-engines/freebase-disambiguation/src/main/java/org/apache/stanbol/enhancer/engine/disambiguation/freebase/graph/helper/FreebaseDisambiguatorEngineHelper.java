package org.apache.stanbol.enhancer.engine.disambiguation.freebase.graph.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.log4j.Logger;
import org.apache.stanbol.enhancer.engine.disambiguation.freebase.graph.CustomDijkstraDistance;
import org.apache.stanbol.enhancer.engine.disambiguation.freebase.graph.UndirectedGraphJung;
import org.apache.stanbol.enhancer.engine.disambiguation.freebase.graph.constants.FreebaseDisambiguatorEngineConstants;
import org.apache.stanbol.enhancer.engine.disambiguation.mlt.DisambiguationData;
import org.apache.stanbol.enhancer.engine.disambiguation.mlt.SavedEntity;
import org.apache.stanbol.enhancer.engine.disambiguation.mlt.Suggestion;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.KeyIndexableGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

/**
 * <p>
 * Helper class with utility methods for FreebaseDisambiguatorEngine
 * </p>
 * 
 * @author Antonio David Perez Morales <adperezmorales@gmail.com>
 * 
 */
public class FreebaseDisambiguatorEngineHelper {
    private static Logger log = Logger.getLogger(FreebaseDisambiguatorEngineHelper.class);

    /**
     * <p>
     * Generates the possible solutions. It means, generating all the possible combinations between the
     * suggested entities for each text annotation
     * </p>
     * <p>
     * It uses the cartesian product between sets to obtain them
     * </p>
     * <p>
     * For example:
     * 
     * <pre>
     * - TextAnnotation A -> Suggestions : [1,2]
     *  - TextAnnotation B -> Suggestions : [3,4]
     *  - TextAnnotation C -> Suggestions : [5]
     * 
     * - Result: [1,3,5] , [1,4,5] , [2,3,5] , [2,4,5]
     * 
     * </p>
     * 
     * @param textAnnotationSuggestions
     * @return
     */
    public static Set<List<Suggestion>> generatePossibleSolutions(Multimap<UriRef,Suggestion> textAnnotationSuggestions) {
        List<Set<Suggestion>> allSets = new ArrayList<Set<Suggestion>>();
        for (UriRef ref : textAnnotationSuggestions.keySet()) {
            allSets.add(ImmutableSet.copyOf(textAnnotationSuggestions.get(ref)));
        }

        Set<List<Suggestion>> possibleSolutions = Sets.cartesianProduct(allSets);

        return possibleSolutions;
    }

    /**
     * <p>
     * Generates a subgraph containing only the given entities and their relations
     * </p>
     * 
     * @param graph
     *            The {@code Graph} containing the whole graph
     * @param entities
     *            The {@code List<UriRef>} containing the entities to be used to generate the subgraph
     * @return a {@code Graph} instance containing the subgraph
     */
    public static Graph generateSubgraph(Graph graph, List<UriRef> entities) {
        KeyIndexableGraph subgraph = new TinkerGraph();
        UriRef[] ents = entities.toArray(new UriRef[0]);

        int size = ents.length;
        Map<UriRef,Vertex> targetMap = new HashMap<UriRef,Vertex>();
        Map<UriRef,Vertex> sourceMap = new HashMap<UriRef,Vertex>();
        for (int i = 0; i < size; i++) {
            Iterator<Vertex> itv = graph.getVertices(
                FreebaseDisambiguatorEngineConstants.VERTEX_ENTITY_URI_PROPERTY, ents[i].getUnicodeString())
                    .iterator();
            if (itv.hasNext()) {
                Vertex newVertex = subgraph.addVertex(null);
                sourceMap.put(ents[i], itv.next());
                targetMap.put(ents[i], newVertex);
                newVertex.setProperty(FreebaseDisambiguatorEngineConstants.VERTEX_ENTITY_URI_PROPERTY,
                    ents[i].getUnicodeString());
            }
        }

        for (int i = 0; i < size - 1; i++) {
            for (int j = i + 1; j < size; j++) {
                Vertex source = sourceMap.get(ents[i]);
                Vertex target = sourceMap.get(ents[j]);
                if (source == null || target == null) continue;

                Iterator<Edge> ite = graph
                        .getEdges(
                            FreebaseDisambiguatorEngineConstants.EDGE_KEY_VERTICES_CONNECTED_PROPERTY,
                            (String) source
                                    .getProperty(FreebaseDisambiguatorEngineConstants.VERTEX_ENTITY_URI_PROPERTY)
                                    + "|"
                                    + target.getProperty(FreebaseDisambiguatorEngineConstants.VERTEX_ENTITY_URI_PROPERTY))
                        .iterator();
                Edge originalEdge = null;
                while (ite.hasNext()) {
                    originalEdge = ite.next();
                    if (originalEdge.getLabel().equals(
                        FreebaseDisambiguatorEngineConstants.DIRECT_CONNECTION_EDGE_LABEL)) break;

                }

                if (originalEdge == null) {
                    ite = graph
                            .getEdges(
                                FreebaseDisambiguatorEngineConstants.EDGE_KEY_VERTICES_CONNECTED_PROPERTY,
                                (String) target
                                        .getProperty(FreebaseDisambiguatorEngineConstants.VERTEX_ENTITY_URI_PROPERTY)
                                        + "|"
                                        + source.getProperty(FreebaseDisambiguatorEngineConstants.VERTEX_ENTITY_URI_PROPERTY))
                            .iterator();

                    while (ite.hasNext()) {
                        originalEdge = ite.next();
                        if (originalEdge.getLabel().equals(
                            FreebaseDisambiguatorEngineConstants.DIRECT_CONNECTION_EDGE_LABEL)) break;
                    }
                }

                if (originalEdge != null) {
                    log.info("Creating edge between " + targetMap.get(ents[i]).getProperty("URI") + " and "
                             + targetMap.get(ents[j]).getProperty("URI") + " from the original edge "
                             + originalEdge);
                    Edge newEdge = subgraph.addEdge(null, targetMap.get(ents[i]), targetMap.get(ents[j]),
                        originalEdge.getLabel());
                    Double weight = 0.0;
                    for (String edgeProperty : originalEdge.getPropertyKeys()) {
                        if (!edgeProperty.contains(".")
                            && originalEdge.getProperty(edgeProperty) instanceof Integer) weight += (Integer) originalEdge
                                .getProperty(edgeProperty);
                    }

                    if (originalEdge.getLabel().equals(
                        FreebaseDisambiguatorEngineConstants.DIRECT_CONNECTION_EDGE_LABEL)) weight = weight * 10;
                    else weight = weight / 10;

                    newEdge.setProperty(FreebaseDisambiguatorEngineConstants.WEIGHT_EDGE_PROPERTY, weight);
                }

                /*
                 * for (Edge e : iterable) {
                 * 
                 * repetitions++; if (e.getVertex(Direction.IN).equals(target) ||
                 * e.getVertex(Direction.OUT).equals(target)) { Edge newEdge = subgraph.addEdge(null,
                 * targetMap.get(ents[i]), targetMap.get(ents[j]), "connection"); int weight = 0; for (String
                 * edgeProperty : e.getPropertyKeys()) { if (!edgeProperty.contains(".") &&
                 * e.getProperty(edgeProperty) instanceof Integer) weight += (Integer)
                 * e.getProperty(edgeProperty); }
                 * 
                 * newEdge.setProperty("weight", weight); break; } }
                 */

            }
        }

        return subgraph;
    }

    /**
     * <p>
     * Generates a subgraph containing only the given entities and their relations
     * </p>
     * 
     * @param graph
     *            The {@code Graph} containing the whole graph
     * @param entities
     *            The {@code List<UriRef>} containing the entities to be used to generate the subgraph
     * @return a {@code Graph} instance containing the subgraph
     */
    public static Graph generateSubgraphBak(Graph graph, List<UriRef> entities) {

        // Using in-memory graph with Key Index support

        KeyIndexableGraph subgraph = new TinkerGraph();
        UriRef[] ents = entities.toArray(new UriRef[0]);

        subgraph.createKeyIndex(FreebaseDisambiguatorEngineConstants.VERTEX_ENTITY_URI_PROPERTY, Vertex.class);

        int size = ents.length;
        Map<UriRef,Vertex> targetMap = new HashMap<UriRef,Vertex>();
        Map<UriRef,Vertex> sourceMap = new HashMap<UriRef,Vertex>();
        for (int i = 0; i < size; i++) {
            Iterator<Vertex> itv = graph.getVertices(
                FreebaseDisambiguatorEngineConstants.VERTEX_ENTITY_URI_PROPERTY, ents[i].getUnicodeString())
                    .iterator();
            if (itv.hasNext()) {
                Vertex newVertex = subgraph.addVertex(null);
                sourceMap.put(ents[i], itv.next());
                targetMap.put(ents[i], newVertex);
                newVertex.setProperty(FreebaseDisambiguatorEngineConstants.VERTEX_ENTITY_URI_PROPERTY,
                    ents[i].getUnicodeString());
            }
        }

        for (int i = 0; i < size - 1; i++) {
            Vertex source = sourceMap.get(ents[i]);
            UriRef[] dest = new UriRef[size - i - 1];
            System.arraycopy(ents, i + 1, dest, 0, size - i - 1);

            Iterable<Edge> iterable = source.getEdges(Direction.BOTH,
                FreebaseDisambiguatorEngineConstants.DIRECT_CONNECTION_EDGE_LABEL,
                FreebaseDisambiguatorEngineConstants.MEDIATED_CONNECTION_EDGE_LABEL);
            int processed = 0;
            int max = dest.length;
            for (Edge e : iterable) {
                if (processed == max || dest.length == 0) break;

                for (int j = 0; j < dest.length; j++) {
                    Vertex target = sourceMap.get(dest[j]);
                    if (e.getVertex(Direction.IN).equals(target) || e.getVertex(Direction.OUT).equals(target)) {
                        Edge newEdge = subgraph.addEdge(null, targetMap.get(ents[i]), targetMap.get(dest[j]),
                            e.getLabel());
                        Double weight = 0.0;
                        for (String edgeProperty : e.getPropertyKeys()) {
                            if (!edgeProperty.contains(".") && e.getProperty(edgeProperty) instanceof Integer) weight += (Integer) e
                                    .getProperty(edgeProperty);
                        }

                        /*
                         * if(e.getLabel().equals("direct-connection")) weight = weight/10; else weight =
                         * weight*10;
                         */

                        newEdge.setProperty(FreebaseDisambiguatorEngineConstants.WEIGHT_EDGE_PROPERTY, weight);
                        processed++;

                        if (j + 1 == dest.length) dest = new UriRef[0];
                        else dest = Arrays.copyOfRange(dest, j + 1, dest.length);

                        break;
                    }
                }
            }

        }

        return subgraph;
    }

    /**
     * <p>
     * Filter the possible solutions removing those entities which are isolated in the possible solution
     * </p>
     * 
     * @param possibleSolutions
     *            The {@code Set<List<Suggestion>>} instance containing the possible solutions
     * 
     * @param graph
     *            The {@code Graph} object containing the graph to be queried
     * 
     * @return The {@code Set<List<Suggestion>>} instance containing the filtered possible solutions
     * 
     */
    public static Set<List<Suggestion>> filterPossibleSolutions(Set<List<Suggestion>> possibleSolutions,
                                                                Graph graph) {

        Map<Suggestion,Boolean> toFilter = new HashMap<Suggestion,Boolean>();
        Set<List<Suggestion>> filtered = Sets.newHashSet();

        for (List<Suggestion> possibleSolution : possibleSolutions) {
            List<Suggestion> filteredPossibleSolution = Lists.newArrayList();

            for (Suggestion suggestion : possibleSolution) {
                if (toFilter.containsKey(suggestion)) {
                    if (toFilter.get(suggestion).equals(true)) continue;
                    else filteredPossibleSolution.add(suggestion);
                } else {
                    Iterator<Vertex> itv = graph.getVertices(
                        FreebaseDisambiguatorEngineConstants.VERTEX_ENTITY_URI_PROPERTY,
                        suggestion.getEntityUri().getUnicodeString()).iterator();
                    if (itv.hasNext()) {
                        Vertex v = itv.next();

                        if (!v.getEdges(Direction.BOTH).iterator().hasNext()) {
                            // Isolated Vertex. Filter the subGraph
                            toFilter.put(suggestion, true);
                            log.debug("Isolated Vertex "
                                      + v.getProperty(FreebaseDisambiguatorEngineConstants.VERTEX_ENTITY_URI_PROPERTY)
                                      + ". Filtering");
                        } else {
                            filteredPossibleSolution.add(suggestion);
                            toFilter.put(suggestion, false);
                        }
                    } else {
                        // No vertex exists. Filter the subGraph
                        toFilter.put(suggestion, true);
                        log.debug("No vertex exists with URI " + suggestion.getEntityUri().getUnicodeString());
                    }

                }
            }

            if (filteredPossibleSolution.size() > 1) filtered.add(filteredPossibleSolution);
        }

        return filtered;
    }

    /**
     * <p>
     * Calculates the disambiguation score for each possible solution using the Dijkstra Algorithm
     * (shortest-path)
     * </p>
     * 
     * @param possibleSolutions
     *            The {@code Set<List<Suggestion>>} object containing the possible solutions to be used
     * @param graph
     *            the {@code Graph} object containing the graph to use
     */
    public static void calculateDisambiguationScores(Set<List<Suggestion>> possibleSolutions,
                                                     Graph graph,
                                                     DisambiguationData disData) {

        UndirectedGraphJung undirectedGraph = new UndirectedGraphJung(graph);
        CustomDijkstraDistance<Vertex,Edge> dijkstra = new CustomDijkstraDistance<Vertex,Edge>(
                undirectedGraph, new CustomDijkstraDistance.Transformer<Edge,Double>() {
                    public Double transform(Edge input) {
                        return (1.0 / (Double) input
                                .getProperty(FreebaseDisambiguatorEngineConstants.WEIGHT_EDGE_PROPERTY));
                    }
                });

        // For normalization purposes
        Double maxDisambiguationScore = 0.0;
        Double minDisambiguationScore = Double.MAX_VALUE;

        /*
         * Storing the shortest path between each pair of vertices, both A -> B and B -> A, because we are
         * using undirected graph
         */
        Table<Vertex,Vertex,Double> precalculatedDistances = HashBasedTable.create();

        // Filter subgraphs;
        for (List<Suggestion> posSol : possibleSolutions) {
            log.info("Processing possible solution: " + posSol);

            Suggestion[] sga = posSol.toArray(new Suggestion[0]);
            int size = sga.length;
            Vertex source, target = null;

            Double notNormalizedDisambiguatonScore = 0.0;

            for (int i = 0; i < size - 1; i++) {
                for (int j = i + 1; j < size; j++) {
                    Iterator<Vertex> itv = graph.getVertices(
                        FreebaseDisambiguatorEngineConstants.VERTEX_ENTITY_URI_PROPERTY,
                        sga[i].getEntityUri().getUnicodeString()).iterator();
                    Iterator<Vertex> itv2 = graph.getVertices(
                        FreebaseDisambiguatorEngineConstants.VERTEX_ENTITY_URI_PROPERTY,
                        sga[j].getEntityUri().getUnicodeString()).iterator();

                    if (itv.hasNext()) source = itv.next();
                    else break;

                    if (itv2.hasNext()) target = itv2.next();
                    else break;

                    Double precalculated = precalculatedDistances.get(source, target);
                    log.info("Precalculated between " + source.getProperty("URI") + " and "
                             + target.getProperty("URI") + " is " + precalculated);
                    if (precalculated == null) {
                        log.info("Calculating distance");
                        Number n = dijkstra.getDistance(source, target);
                        n = n == null ? 0 : n;
                        Double val = n.doubleValue();
                        log.info("Storing precalculated between " + source.getProperty("URI") + " and "
                                 + target.getProperty("URI") + " ->  " + val);
                        precalculatedDistances.put(source, target, val);
                        precalculatedDistances.put(target, source, val);
                    }

                    notNormalizedDisambiguatonScore += precalculatedDistances.get(source, target);
                }
            }

            // Calculate the inverse to be used later in normalization
            notNormalizedDisambiguatonScore = 1 / Math.log1p(notNormalizedDisambiguatonScore + 1);

            if (notNormalizedDisambiguatonScore >= maxDisambiguationScore) maxDisambiguationScore = notNormalizedDisambiguatonScore;
            if (notNormalizedDisambiguatonScore <= minDisambiguationScore) minDisambiguationScore = notNormalizedDisambiguatonScore;

            /* Storing temporarily the not-normalized confidence for the suggestions */
            for (Suggestion sugg : posSol) {
                Double temp = sugg.getNormalizedDisambiguationScore();
                if (temp == null) temp = 0.0;

                // Change score only if the new score is greater than the old one
                sugg.setNormalizedDisambiguationScore(notNormalizedDisambiguatonScore > temp ? notNormalizedDisambiguatonScore
                        : temp);
            }

        }

        if (minDisambiguationScore == Double.MAX_VALUE) minDisambiguationScore = 0.0;

        /* Normalize confidences in the range of [0,1] for all Suggestion */
        /*
         * Those suggestion which don't belong to any possible solution won't have disambiguation score so,
         * their confidence value won't change
         */
        for (SavedEntity entity : disData.textAnnotations.values()) {
            for (Suggestion sugg : entity.getSuggestions()) {
                if (sugg.getNormalizedDisambiguationScore() == null) {
                    sugg.setNormalizedDisambiguationScore(0.0);
                    continue;
                }
                // If maxDisambiguationScore equals to minDisambiguationScore and equals to current
                // disambiguation
                // score, then set the max normalized value (1.0)
                Double normalizedDisambiguationScore = sugg.getNormalizedDisambiguationScore().equals(
                    maxDisambiguationScore)
                                                       && maxDisambiguationScore
                                                               .equals(minDisambiguationScore) ? 1.0
                        : (sugg.getNormalizedDisambiguationScore() - minDisambiguationScore)
                          / (maxDisambiguationScore - minDisambiguationScore);

                log.info(sugg.getEntityUri().getUnicodeString() + " -> Normalized disambiguation score: "
                         + normalizedDisambiguationScore);

                sugg.setNormalizedDisambiguationScore(normalizedDisambiguationScore);

            }
        }
    }
}

package org.apache.stanbol.enhancer.engine.disambiguation.freebase.graph.constants;

/**
 * <p>
 * Class containing the constants used by the Freebase Disambiguator Engine
 * </p>
 * 
 * @author Antonio David Perez Morales <adperezmorales@gmail.com>
 * 
 */
public class FreebaseDisambiguatorEngineConstants {
    /**
     * Property in a Vertex to put the URI
     */
    public static final String VERTEX_ENTITY_URI_PROPERTY = "URI";

    /**
     * Edge label representing direct connections
     */
    public static final String DIRECT_CONNECTION_EDGE_LABEL = "direct-connection";

    /**
     * Edge label representing mediated connections
     */
    public static final String MEDIATED_CONNECTION_EDGE_LABEL = "mediated-connection";

    /**
     * Edge property containing the property used for generate an index for the edges
     */
    public static final String EDGE_KEY_VERTICES_CONNECTED_PROPERTY = "vertices.connected";

    /**
     * Edge property used to store the calculated weight in the subgraph
     */
    public static final String WEIGHT_EDGE_PROPERTY = "weight";
}

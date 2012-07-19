package org.apache.stanbol.contenthub.servicesapi.index;

/**
 * Possible REST endpoint types for search operations offered by {@link SemanticIndex}.
 * 
 */
public enum EndpointType {
    /**
     * RESTful endpoint of the Solr
     */
    SOLR,
    /**
     * RESTful search endpoint specific to the Contenthub
     */
    CONTENTHUB;

    private static final String prefix = "http://stanbol.apache.org/ontology/contenthub#endpointType_";

    public String getUri() {
        return prefix + name().toLowerCase();
    }

    @Override
    public String toString() {
        return getUri();
    }

}

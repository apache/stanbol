package org.apache.stanbol.entityhub.query.sparql;

public enum SparqlEndpointTypeEnum {
    Standard,
    Virtuoso(true),
    LARQ,
    ARQ,
    Sesame(true);
    boolean supportsSparql11SubSelect;

    /**
     * Default feature set (SPARQL 1.0)
     */
    SparqlEndpointTypeEnum() {
        this(false);
    }

    /**
     * Allows to enable SPARQL 1.1 features
     * 
     * @param supportsSparql11SubSelect
     */
    SparqlEndpointTypeEnum(boolean supportsSparql11SubSelect) {
        this.supportsSparql11SubSelect = supportsSparql11SubSelect;
    }

    public final boolean supportsSubSelect() {
        return supportsSparql11SubSelect;
    }
}
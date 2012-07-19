package org.apache.stanbol.contenthub.servicesapi.index;

/**
 * This enumeration defines the possible states for a {@link SemanticIndex}.
 */
public enum IndexState {
    /**
     * The index was defined, the configuration is ok, but the contents are not yet indexed and the indexing
     * has not yet started. (Intended to be used as default state after creations)
     */
    UNINIT,
    /**
     * The indexing of content items is currently in progress. This indicates that the index is currently NOT
     * active.
     */
    INDEXING,
    /**
     * The semantic index is available and in sync
     */
    ACTIVE,
    /**
     * The (re)-indexing of content times is currently in progress. This indicates that the configuration of
     * the semantic index was changed in a way that requires to rebuild the whole semantic index. This still
     * requires the index to be active - meaning the searches can be performed normally - but recent
     * updates/changes to ContentItems might not be reflected. This also indicates that the index will be
     * replaced by a different version (maybe with changed fields) in the near future.
     */
    REINDEXING;

    private static final String prefix = "http://stanbol.apache.org/ontology/contenthub#indexState_";

    public String getUri() {
        return prefix + name().toLowerCase();
    }

    @Override
    public String toString() {
        return getUri();
    }
}

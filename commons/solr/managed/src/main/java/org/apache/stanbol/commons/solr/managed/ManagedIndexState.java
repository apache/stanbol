package org.apache.stanbol.commons.solr.managed;

/**
 * Enumeration for the different states of a managed index
 * @author Rupert Westenthaler
 *
 */
public enum ManagedIndexState {
    
    /**
     * The index was registered, but required data are still missing
     */
    UNINITIALISED,
    /**
     * The index is ready, but currently not activated
     */
    INACTIVE,
    /**
     * The index is active and can be used
     */
    ACTIVE,
    /**
     * The index encountered an error during Initialisation. The kind of the
     * Error is available via the {@link IndexMetadata}.
     */
    ERROR,

}

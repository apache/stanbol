/**
 * 
 */
package org.apache.stanbol.entityhub.indexing.core.source;

/**
 * State of resources managed by the ResourceLoader
 * @author Rupert Westenthaler
 *
 */
public enum ResourceState {
    /**
     * Resources that are registered but not yet processed
     */
    REGISTERED,
    /**
     * Resources that are currently processed
     */
    LOADING,
    /**
     * Resources that where successfully loaded
     */
    LOADED,
    /**
     * Resources that where ignored
     */
    IGNORED,
    /**
     * Indicates an Error while processing a resource
     */
    ERROR
}
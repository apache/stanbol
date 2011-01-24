package org.apache.stanbol.entityhub.servicesapi.yard;

public enum CacheingLevel {
    /**
     * This indicated that a document in the cache confirms to the specification
     * as stored within the cache. This configuration is stored within the cache
     * and only be changed for an empty cache.<p>
     */
    base,
    /**
     * If a Document is updated in the cache, than there may be more information
     * be stored as defined by the initial creation of a cache. <p> This level indicates
     * that a document includes all the field defined for {@link #base} but
     * also some additional information.
     */
    special
}

/**
 *
 */
package eu.iksproject.rick.servicesapi.yard;

public enum CacheStrategy{
    /**
     * All entities of this site should be cached
     */
    all,
    /**
     * Only entities are cached that where retrieved by some past request
     */
    used,
    /**
     * Entities of this site are not cached
     */
    none
}

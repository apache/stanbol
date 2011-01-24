package org.apache.stanbol.entityhub.servicesapi.yard;



/**
 * Indicates an Exception while the initialisation of a cache. This is usually
 * the case if the cache is configured to use a Yard that does not include the
 * required configuration meta data needed to correctly configure the cache.<p>
 * For errors while performing CRUD operations on the Cache {@link YardException}s
 * are used.
 *
 * @author Rupert Westenthaler
 *
 */
public class CacheInitialisationException extends RuntimeException {

    public CacheInitialisationException(String reason) {
        super(reason);
    }

    public CacheInitialisationException(String reason, Throwable cause) {
        super(reason, cause);
    }

    /**
     *
     */
    private static final long serialVersionUID = 1L;

}

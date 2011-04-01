package org.apache.stanbol.ontologymanager.store.api;

/**
 * Single writer, multiple readers lock implementation for persistence store
 * 
 * @author Cihan
 */
public interface LockManager {

    public static String GLOBAL_SPACE = "GLOBAL_SPACE";

    /**
     * Obtain a read lock for specified ontology
     * 
     * @param ontologyURI
     *            URI of the ontology
     */
    public abstract void obtainReadLockFor(String ontologyURI);

    /**
     * Release read lock for specified ontology
     * 
     * @param ontologyURI
     *            URI of the ontology
     */
    public abstract void releaseReadLockFor(String ontologyURI);

    /**
     * Obtain a write lock for specified ontology
     * 
     * @param ontologyURI
     *            URI of the ontology
     */
    public abstract void obtainWriteLockFor(String ontologyURI);

    /**
     * Release write lock for specified ontology
     * 
     * @param ontologyURI
     */
    public abstract void releaseWriteLockFor(String ontologyURI);

}
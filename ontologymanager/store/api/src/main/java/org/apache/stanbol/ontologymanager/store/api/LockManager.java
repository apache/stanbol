package org.apache.stanbol.ontologymanager.store.api;

/**
 * Single writer, multiple readers lock implementation for persistence store
 * 
 * @author Cihan
 */
public interface LockManager {

    String GLOBAL_SPACE = "GLOBAL_SPACE";

    /**
     * Obtain a read lock for specified ontology
     * 
     * @param ontologyURI
     *            URI of the ontology
     */
    void obtainReadLockFor(String ontologyURI);

    /**
     * Release read lock for specified ontology
     * 
     * @param ontologyURI
     *            URI of the ontology
     */
    void releaseReadLockFor(String ontologyURI);

    /**
     * Obtain a write lock for specified ontology
     * 
     * @param ontologyURI
     *            URI of the ontology
     */
    void obtainWriteLockFor(String ontologyURI);

    /**
     * Release write lock for specified ontology
     * 
     * @param ontologyURI
     */
    void releaseWriteLockFor(String ontologyURI);

}
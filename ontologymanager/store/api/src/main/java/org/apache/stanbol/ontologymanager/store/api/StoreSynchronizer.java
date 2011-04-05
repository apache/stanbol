package org.apache.stanbol.ontologymanager.store.api;

/**
 * Interface for synchronizing the resources managed by {@link ResourceManager} with underlying
 * {@link JenaPersistenceProvider} implementation
 * 
 * @author Cihan
 * 
 */
public interface StoreSynchronizer {

    /**
     * Synchronizes all graphs that is stored in a {@link ResourceManager}
     * 
     * @param force
     *            <p>
     *            If set {@link ResourceManager} will be cleared and all resource-graph mappings will be
     *            rebuilt.
     *            <p>
     *            If not set the synchronizer should consider only deletion/addition of graphs.
     */
    void synchronizeAll(boolean force);

    /**
     * Synchronizes only specified graph. After synchronization the resource-graph mappings of the
     * {@link ResourceManager} for the particular graph is synchronized
     * 
     * @param graphURI
     *            URI of the graph of which resources will be synchronized
     */
    void synchronizeGraph(String graphURI);

    /**
     * StoreSynchronizer is obtained through a factory and when its job is finished this method should be
     * invoked to remove graph listeners.
     */
    void clear();

}

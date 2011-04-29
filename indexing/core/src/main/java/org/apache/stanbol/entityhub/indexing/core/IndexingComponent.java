package org.apache.stanbol.entityhub.indexing.core;

import java.util.Map;

import org.apache.stanbol.entityhub.indexing.core.impl.IndexerImpl;

/**
 * Parent Interface defining the indexing work flow methods for all kinds of
 * data sources used for the Indexer.
 * @author Rupert Westenthaler
 *
 */
public interface IndexingComponent {

    /**
     * Setter for the configuration
     * @param config the configuration
     */
    public void setConfiguration(Map<String,Object> config);
    /**
     * Used by the {@link IndexerImpl} to check if this source needs to be
     * {@link #initialise()}.
     * @return If <code>true</code> is returned the {@link IndexerImpl} will call
     * {@link #initialise()} during the initialisation phase of the indexing
     * process.
     */
    public boolean needsInitialisation();
    /**
     * Initialise the IndexingSource. This should be used to perform 
     * time consuming initialisations.
     */
    public void initialise();
    
    /**
     * Called by the {@link IndexerImpl} as soon as this source is no longer needed
     * for the indexing process.
     */
    public void close();
    
}

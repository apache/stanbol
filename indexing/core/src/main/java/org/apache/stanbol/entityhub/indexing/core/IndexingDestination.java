package org.apache.stanbol.entityhub.indexing.core;

import java.util.Dictionary;

import org.apache.stanbol.entityhub.servicesapi.yard.Yard;


/**
 * Interface that defines the target for indexing. 
 * @author Rupert Westenthaler
 *
 */
public interface IndexingDestination extends IndexingComponent {

    /**
     * Getter for the Yard to store the indexed Entities
     * @return the yard
     */
    Yard getYard();
    
    /**
     * Called after the indexing is completed to allow some post processing and
     * packaging the stored data and writing of the OSGI configuration used to
     * initialise the Yard.
     */
    void finalise();
}

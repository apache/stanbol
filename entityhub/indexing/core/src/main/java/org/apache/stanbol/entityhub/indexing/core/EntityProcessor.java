package org.apache.stanbol.entityhub.indexing.core;

import org.apache.stanbol.entityhub.indexing.core.impl.IndexerImpl;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;

public interface EntityProcessor extends IndexingComponent {
    /**
     * Processes the source representation based on some processing rules.
     * This interface is used by the {@link IndexerImpl} to process Entities
     * retrieved from the Source before they are stored to the {@link Yard}
     * @param source the source
     * @return the processed/mapped representation
     */
    Representation process(Representation source);

}

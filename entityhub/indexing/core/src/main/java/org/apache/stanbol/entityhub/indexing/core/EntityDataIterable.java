package org.apache.stanbol.entityhub.indexing.core;

/**
 * Interface used to create an instance of an {@link EntityDataIterator}
 * @author Rupert Westenthaler
 *
 */
public interface EntityDataIterable extends IndexingComponent {
    /**
     * Returns an iterator over all Representations of the Entities.
     * @return A new instance of an {@link EntityDataIterator}
     */
    EntityDataIterator entityDataIterator();
    
}

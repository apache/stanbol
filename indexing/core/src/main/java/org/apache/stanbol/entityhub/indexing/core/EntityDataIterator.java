package org.apache.stanbol.entityhub.indexing.core;

import java.util.Iterator;

import org.apache.stanbol.entityhub.servicesapi.model.Representation;

/**
 * Interface used to iterate over all entities.
 * By calling {@link #next()} one can iterate over the IDs of the Entities.
 * The data ({@link Representation}) of the current entity are available by
 * calling {@link #getRepresentation()}.<p>
 * This interface is intended for data source that prefer to read entity
 * information as a stream (e.g. from an tab separated text file) and therefore
 * can not provide an implementation of the {@link EntityDataProvider} interface.
 * @see EntityDataProvider 
 * @author Rupert Westenthaler
 *
 */
public interface EntityDataIterator extends Iterator<String>{

    /**
     * Getter for the Representation of the current active Element. This is the
     * Representation of the Entity with the ID returned for the previous
     * call to {@link #next()}. This method does not change the current element
     * of the iteration.
     * @return the Representation for the entity with the ID returned by 
     * {@link #next()}
     */
    Representation getRepresentation();
    /**
     * Close the iteration.
     */
    void close();
}

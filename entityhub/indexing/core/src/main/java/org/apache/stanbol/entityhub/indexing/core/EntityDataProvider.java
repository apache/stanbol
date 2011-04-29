package org.apache.stanbol.entityhub.indexing.core;

import org.apache.stanbol.entityhub.servicesapi.model.Representation;

/**
 * Interface used to get the representation (data) for an entity based on the
 * id. This Interface is used for indexing in cases, where the list of entities
 * to index is known in advance and the data source provides the possibility to
 * retrieve the entity data based on the ID (e.g. a RDF triple store).
 * @see {@link EntityDataIterator}
 * @author Rupert Westenthaler
 *
 */
public interface EntityDataProvider extends IndexingComponent {
    
    Representation getEntityData(String id);

}

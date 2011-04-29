/**
 * 
 */
package org.apache.stanbol.entityhub.indexing.core;

import java.util.Iterator;
import java.util.Map;

import org.apache.stanbol.entityhub.servicesapi.model.Representation;

/**
 * Dummy implementation of an {@link EntityDataIterable} and {@link EntityDataProvider}
 * that reads the entity data directly form {@link IndexerTest#testData}
 * @author Rupert Westenthaler
 *
 */
public class DummyEntityDataSource implements EntityDataIterable, EntityDataProvider {

    @Override
    public EntityDataIterator entityDataIterator() {
        return new EntityDataIterator() {
            Iterator<Representation> rep = IndexerTest.testData.values().iterator();
            Representation current = null;
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
            @Override
            public String next() {
                current = rep.next();
                return current.getId();
            }
            @Override
            public boolean hasNext() {
                return rep.hasNext();
            }
            @Override
            public Representation getRepresentation() {
                return current;
            }
            @Override
            public void close() {}
        };
    }

    @Override
    public void close() {
    }

    @Override
    public void initialise() {
    }

    @Override
    public boolean needsInitialisation() {
        return false;
    }

    @Override
    public void setConfiguration(Map<String,Object> config) {
    }

    @Override
    public Representation getEntityData(String id) {
        return IndexerTest.testData.get(id);
    }
}
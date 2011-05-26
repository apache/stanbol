/**
 * 
 */
package org.apache.stanbol.entityhub.indexing.core;

import java.util.Iterator;
import java.util.Map;

import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;

/**
 * Dummy implementation of an {@link EntityIterator} that reads entity ids
 * directly form {@link IndexerTest#testData}
 * @author Rupert Westenthaler
 *
 */
public class DummyEntityIdSource implements EntityIterator {
    private Iterator<Representation> entiyIterator = IndexerTest.testData.values().iterator();
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
    public boolean hasNext() {
        return entiyIterator.hasNext();
    }

    @Override
    public EntityScore next() {
        Representation next = entiyIterator.next();
        Number score = next.getFirst(RdfResourceEnum.entityRank.getUri(), Number.class);
        return new EntityScore(next.getId(), score == null?0:score.floatValue());
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
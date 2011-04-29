/**
 * 
 */
package org.apache.stanbol.entityhub.indexing.core;

import java.util.Map;

import org.apache.stanbol.entityhub.indexing.core.EntityIterator.EntityScore;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;

/**
 * Dummy implementation of an {@link EntityScoreProvider} that creates
 * {@link EntityScore} instances directly based on the test data stored in
 * {@link IndexerTest#testData}
 * @author Rupert Westenthaler
 *
 */
public class DummyEntityScoreSource implements EntityScoreProvider {

    @Override
    public boolean needsData() {
        return true;
    }

    @Override
    public Float process(String id) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Float process(Representation entity) throws UnsupportedOperationException {
        return entity.getFirst(RdfResourceEnum.signRank.getUri(), Float.class);
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
}
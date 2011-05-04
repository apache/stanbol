package org.apache.stanbol.entityhub.indexing.core.source;

import java.util.Map;

import org.apache.stanbol.entityhub.indexing.core.EntityScoreProvider;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;

/**
 * Returns <code>null</code> as score for each entity and therefore indicating
 * that there is no score available for any entity.
 * @author Rupert Westenthaler
 *
 */
public class NoEntityScoreProvider implements EntityScoreProvider {

    @Override
    public boolean needsData() {
        return false;
    }

    @Override
    public Float process(String id) {
        return null;
    }

    @Override
    public Float process(Representation entity) {
        return null;
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

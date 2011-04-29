package org.apache.stanbol.entityhub.indexing.core.processor;

import java.util.Map;

import org.apache.stanbol.entityhub.indexing.core.EntityProcessor;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;

/**
 * Returns the parsed Representation. Intended to be used in cases where a
 * <code>null</code> value is not allowed for the {@link EntityProcessor}.
 * @author Rupert Westenthaler
 *
 */
public class EmptyProcessor implements EntityProcessor{

    @Override
    public Representation process(Representation source) {
        return source;
    }

    @Override
    public void close() {
        //nothing to do
    }

    @Override
    public void initialise() {
        //nothing to do
    }

    @Override
    public boolean needsInitialisation() {
        return false;
    }

    @Override
    public void setConfiguration(Map<String,Object> config) {
        //no configuration supported
    }

}

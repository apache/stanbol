package org.apache.stanbol.enhancer.engines.dereference.entityhub;

import org.apache.stanbol.entityhub.ldpath.backend.AbstractBackend;
import org.apache.stanbol.entityhub.servicesapi.EntityhubException;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;


final class ParseBackend<T> extends AbstractBackend {
    /**
     * 
     */
    private final ValueFactory valueFactory;

    /**
     * @param trackingDereferencerBase
     */
    public ParseBackend(ValueFactory vf) {
        this.valueFactory = vf;
    }

    @Override
    protected QueryResultList<String> query(FieldQuery query) throws EntityhubException {
        throw new UnsupportedOperationException("Not expected to be called");
    }

    @Override
    protected ValueFactory getValueFactory() {
        return valueFactory;
    }

    @Override
    protected Representation getRepresentation(String id) throws EntityhubException {
        throw new UnsupportedOperationException("Not expected to be called");
    }

    @Override
    protected FieldQuery createQuery() {
        throw new UnsupportedOperationException("Not expected to be called");
    }
}
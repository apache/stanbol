package org.apache.stanbol.entityhub.ldpath.backend;

import org.apache.stanbol.entityhub.core.mapping.ValueConverterFactory;
import org.apache.stanbol.entityhub.servicesapi.EntityhubException;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;

public class YardBackend extends AbstractBackend {

    protected final Yard yard;
    
    public YardBackend(Yard yard) {
        this(yard,null);
    }
    public YardBackend(Yard yard,ValueConverterFactory valueConverter) {
        super(valueConverter);
        if(yard == null){
            throw new IllegalArgumentException("The parsed Yard MUST NOT be NULL");
        }
        this.yard = yard;
    }
    @Override
    protected FieldQuery createQuery() {
        return yard.getQueryFactory().createFieldQuery();
    }
    @Override
    protected Representation getRepresentation(String id) throws EntityhubException {
        return yard.getRepresentation(id);
    }
    @Override
    protected ValueFactory getValueFactory() {
        return yard.getValueFactory();
    }
    @Override
    protected QueryResultList<String> query(FieldQuery query) throws EntityhubException {
        return yard.findReferences(query);
    }
}

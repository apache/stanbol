package org.apache.stanbol.entityhub.ldpath.backend;

import java.util.HashMap;
import java.util.Map;

import org.apache.stanbol.entityhub.servicesapi.EntityhubException;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;

import at.newmedialab.ldpath.api.backend.RDFBackend;

/**
 * Wrapper over an QueryResult and a {@link RDFBackend} implementation intended
 * to be used to execute a LDPath program over Query results. This will typically
 * speed up execution, because the Representations of the original query need
 * not to be looked up again, while executing the LDPath path segments with
 * length 1.  
 * @author Rupert Westenthaler
 *
 */
public class QueryResultBackend extends AbstractBackend {

    QueryResultList<Representation> resultList;
    AbstractBackend backend;
    final Map<String,Representation> resultMap;
    public QueryResultBackend(QueryResultList<Representation> result,AbstractBackend backend){
        if(result == null){
            throw new IllegalArgumentException("The parsed resultList MUST NOT be NULL!");
        }
        resultList = result;
        this.backend = backend;
        resultMap = new HashMap<String,Representation>(result.size());
        for(Representation r : resultList){
            resultMap.put(r.getId(), r);
        }
    }
    @Override
    protected FieldQuery createQuery() {
        return backend.createQuery();
    }
    @Override
    protected Representation getRepresentation(String id) throws EntityhubException {
        Representation r = resultMap.get(id);
        if(r == null) {
            r = backend.getRepresentation(id);
        }
        return r;
    }
    @Override
    protected ValueFactory getValueFactory() {
        return backend.getValueFactory();
    }
    @Override
    protected QueryResultList<String> query(FieldQuery query) throws EntityhubException {
        return backend.query(query);
    }
    

}

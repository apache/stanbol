package org.apache.stanbol.entityhub.query.clerezza;

import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;

public final class SparqlFieldQueryFactory implements FieldQueryFactory {

    private static SparqlFieldQueryFactory instance;

    public static SparqlFieldQueryFactory getInstance(){
        if(instance == null){
            instance = new SparqlFieldQueryFactory();
        }
        return instance;
    }
    private SparqlFieldQueryFactory(){
        super();
    }
    @Override
    public SparqlFieldQuery createFieldQuery() {
        return new SparqlFieldQuery();
    }
    /**
     * Utility Method to create an {@link SparqlFieldQuery} based on the parse {@link FieldQuery}
     * @param parsedQuery the parsed Query
     */
    public static SparqlFieldQuery getSparqlFieldQuery(FieldQuery parsedQuery) {
        if(parsedQuery == null){
            return null;
        } else if(parsedQuery instanceof SparqlFieldQuery){
            return (SparqlFieldQuery)parsedQuery;
        } else {
            return parsedQuery.copyTo(new SparqlFieldQuery());
        }
    }


}

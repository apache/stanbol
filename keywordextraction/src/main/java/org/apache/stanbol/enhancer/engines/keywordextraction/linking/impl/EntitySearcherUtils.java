package org.apache.stanbol.enhancer.engines.keywordextraction.linking.impl;

import java.util.List;
import java.util.Set;

import org.apache.stanbol.enhancer.engines.keywordextraction.linking.EntitySearcher;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint;

public class EntitySearcherUtils {

    /**
     * Validated the parsed parameter as parsed to 
     * {@link EntitySearcher#lookup(String, Set, List, String...)}
     * and creates a fieldQuery for the parsed parameter
     * @param field
     * @param includeFields
     * @param search
     * @param languages
     * @return
     */
    public final static FieldQuery createFieldQuery(FieldQueryFactory factory,
                                        String field,
                                        Set<String> includeFields,
                                        List<String> search,
                                        String... languages) {
        if(field == null || field.isEmpty()){
            throw new IllegalArgumentException("The parsed search field MUST NOT be NULL nor empty");
        }
        if(search == null || search.isEmpty()){
            throw new IllegalArgumentException("The parsed list of search strings MUST NOT be NULL nor empty");
        }
        //build the query and than return the result
        FieldQuery query = factory.createFieldQuery();
        if(includeFields == null){
            query.addSelectedField(field);
        } else {
            if(!includeFields.contains(field)){
                query.addSelectedField(field);
            }
            for(String select : includeFields){
                query.addSelectedField(select);
            }
        }
        query.setLimit(20);//TODO make configurable
        query.setConstraint(field, new TextConstraint(search, languages));
        return query;
    }

}

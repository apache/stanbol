/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.stanbol.enhancer.engines.entityhublinking;

import java.util.List;
import java.util.Set;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.enhancer.engines.entitylinking.EntitySearcher;
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
                                        UriRef field,
                                        Set<UriRef> includeFields,
                                        List<String> search,
                                        String... languages) {
        if(field == null || field.getUnicodeString().isEmpty()){
            throw new IllegalArgumentException("The parsed search field MUST NOT be NULL nor empty");
        }
        if(search == null || search.isEmpty()){
            throw new IllegalArgumentException("The parsed list of search strings MUST NOT be NULL nor empty");
        }
        //build the query and than return the result
        FieldQuery query = factory.createFieldQuery();
        if(includeFields == null){
            query.addSelectedField(field.getUnicodeString());
        } else {
            if(!includeFields.contains(field.getUnicodeString())){
                query.addSelectedField(field.getUnicodeString());
            }
            for(UriRef select : includeFields){
                query.addSelectedField(select.getUnicodeString());
            }
        }
        query.setLimit(20);//TODO make configurable
        query.setConstraint(field.getUnicodeString(), new TextConstraint(search, languages));
        return query;
    }

}

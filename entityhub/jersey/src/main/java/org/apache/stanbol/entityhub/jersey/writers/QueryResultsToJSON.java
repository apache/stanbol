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
package org.apache.stanbol.entityhub.jersey.writers;

import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

final class QueryResultsToJSON {

    private QueryResultsToJSON() { /* do not create instances of utility classes */}

    static <T> JSONObject toJSON(QueryResultList<?> resultList, NamespacePrefixService nsPrefixService) throws JSONException{
        JSONObject jResultList = new JSONObject();
        if(resultList.getQuery() != null){
            jResultList.put("query", FieldQueryToJSON.toJSON(resultList.getQuery(),nsPrefixService));
        }
        jResultList.put("results", convertResultsToJSON(resultList,resultList.getType()));
        return jResultList;
    }

    private static <T> JSONArray convertResultsToJSON(Iterable<?> results,Class<?> type) throws JSONException{
        JSONArray jResults = new JSONArray();
        if(String.class.isAssignableFrom(type)){
            for(Object result : results){
                jResults.put(result);
            }
        } else if(Representation.class.isAssignableFrom(type)){
            for(Object result : results){
                jResults.put(EntityToJSON.toJSON((Representation)result));
            }
        } else if(Entity.class.isAssignableFrom(type)){
            for(Object result : results){
                jResults.put(EntityToJSON.toJSON((Entity)result));
            }
        }
        return jResults;
    }
}

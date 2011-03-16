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
package org.apache.stanbol.entityhub.core.query;

import org.apache.stanbol.entityhub.servicesapi.query.Query;


/**
 * Utility class for queries.
 * @author Rupert Westenthaler
 *
 */
public final class QueryUtils {
    private QueryUtils(){}
    /**
     * Getter for the Limit calculated bye on the limit defined by the query
     * and the configuration of the default results (for queries that do not
     * define a limit) and the maximum number of Results.<p>
     * Configurations for defaultResults and maxResults <= 0 are ignored. Return
     * values < = 0 should be interpreted as no constraints.
     * @param query the query
     * @param defaultResults the default number of results
     * @param maxResults the maximum number of queries
     * @return if > 0, than the value represents the number of results for the
     * query. Otherwise no constraint.
     */
    public static int getLimit(Query query,int defaultResults, int maxResults){
        int limit = query.getLimit() != null?query.getLimit():-1;
        if(defaultResults > 0){
            limit = Math.min(limit, defaultResults);
        }
        if(maxResults > 0){
            limit = Math.min(limit,maxResults);
        }
        return limit;
    }

}

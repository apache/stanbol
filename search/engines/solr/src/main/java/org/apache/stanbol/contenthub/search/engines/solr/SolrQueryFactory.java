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

package org.apache.stanbol.contenthub.search.engines.solr;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.solr.client.solrj.SolrQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author anil.pacaci
 * 
 */
public class SolrQueryFactory {
    private final static Logger logger = LoggerFactory.getLogger(SolrQueryFactory.class);
    private final static String and = " AND ";
    private final static String delimiter = ":";

    public static SolrQuery keywordQueryWithFacets(String keyword, Map<String,List<Object>> constraints) {
        SolrQuery query = new SolrQuery();
        String queryString = keyword;

        try {
            for (Entry<String,List<Object>> entry : constraints.entrySet()) {
                String fieldName = entry.getKey();
                for (Object value : entry.getValue()) {
                    queryString = queryString + and + fieldName + delimiter + value;
                }
            }
        } catch (Exception e) {
            logger.warn("Facet constraints could not be added to Query", e);
        }
        queryString += "&fl=*,score";
        query.setQuery(queryString);
        return query;
    }

}
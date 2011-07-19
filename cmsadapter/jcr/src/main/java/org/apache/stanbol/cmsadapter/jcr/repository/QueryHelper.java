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
package org.apache.stanbol.cmsadapter.jcr.repository;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.query.Query;

public class QueryHelper {

    public static final String JCR_ITEM_BY_PATH = "SELECT * from nt:base WHERE jcr:path = '%s'";
    public static final String JCR_ITEM_BY_ID = "SELECT * from nt:base WHERE jcr:id = '%s'";
    public static final String JCR_ITEM_BY_NAME = "//element(%s ,nt:base)";

    public static List<JCRQueryRepresentation> getJCRItemByPathQuery(String path) {
        List<JCRQueryRepresentation> queries = new ArrayList<JCRQueryRepresentation>();
        String queryString;
        if (path.endsWith("/%")) {
            // Assumed that the root node is wanted to be included in the query result
            queryString = String.format(JCR_ITEM_BY_PATH, path.substring(0, path.length() - 2));
            queries.add(new JCRQueryRepresentation(queryString, Query.SQL));
        }
        queryString = String.format(JCR_ITEM_BY_PATH, path);
        queries.add(new JCRQueryRepresentation(queryString, Query.SQL));

        return queries;
    }

    public static JCRQueryRepresentation getJCRItemByIDQuery(String ID) {
        String queryString = String.format(JCR_ITEM_BY_ID, ID);
        return new JCRQueryRepresentation(queryString, Query.SQL);
    }

    public static JCRQueryRepresentation getJCRItemByNameQuery(String name) {
        String queryString = String.format(JCR_ITEM_BY_NAME, name);
        return new JCRQueryRepresentation(queryString, Query.XPATH);
    }

}

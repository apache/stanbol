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
package org.apache.stanbol.entityhub.servicesapi.site;

import java.io.IOException;

import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;

/**
 * Interface used to provide service/technology specific implementation of the
 * search interface provided by {@link Site}.
 * @author Rupert Westenthaler
 *
 */
public interface EntitySearcher {

    /**
     * The key used to define the baseUri of the query service used for the
     * implementation of this interface.<br>
     * This constants actually uses the value of {@link SiteConfiguration#QUERY_URI}
     */
    String QUERY_URI = ReferencedSiteConfiguration.QUERY_URI;
    /**
     * Searches for Entities based on the parsed {@link FieldQuery}
     * @param query the query
     * @return the result of the query
     */
    QueryResultList<String> findEntities(FieldQuery query) throws IOException;
    /**
     * Searches for Entities based on the parsed {@link FieldQuery} and returns
     * for each entity an Representation over the selected fields and values
     * @param query the query
     * @return the found entities as representation containing only the selected
     * fields and there values.
     */
    QueryResultList<Representation> find(FieldQuery query) throws IOException;


}

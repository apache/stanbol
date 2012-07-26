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
package org.apache.stanbol.entityhub.servicesapi.query;

import java.io.IOException;
import java.util.Collection;

import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;

/**
 * This interface defines the Java API for searching Entities.
 * @author Rupert Westenthaler
 * TODO Currently unused
 */
public interface QueryService {


    /**
     * Searches for entities based on the parsed {@link FieldQuery} and returns
     * the references (ids). Note that selected fields of the query are ignored.
     * @param query the query
     * @return the references of the found entities
     * @throws IOException If the referenced Site is not accessible
     * @throws UnsupportedQueryTypeException if the type of the parsed query is
     * not supported by this query service. Or in other words if the value of
     * {@link Query#getQueryType()} is not part of the collections returned by
     * {@link QueryService#getSupportedQueryTypes()}.
     */
    QueryResultList<String> findReferences(Query query) throws IOException, UnsupportedQueryTypeException;
    /**
     * Searches for entities based on the parsed {@link FieldQuery} and returns
     * representations as defined by the selected fields of the query. Note that
     * if the query defines also {@link Constraint}s for selected fields, that
     * the returned representation will only contain values selected by such
     * constraints.
     * @param query the query
     * @return the found entities as representation containing only the selected
     * fields and there values.
     * @throws IOException If the referenced Site is not accessible
     * @throws UnsupportedQueryTypeException if the type of the parsed query is
     * not supported by this query service. Or in other words if the value of
     * {@link Query#getQueryType()} is not part of the collections returned by
     * {@link QueryService#getSupportedQueryTypes()}.
     */
    QueryResultList<? extends Representation> find(Query query) throws IOException, UnsupportedQueryTypeException;
    /**
     * Searches for Signs based on the parsed {@link FieldQuery} and returns
     * the selected Signs including the whole representation. Note that selected
     * fields of the query are ignored.
     * @param query the query
     * @return All Entities selected by the Query.
     * @throws IOException If the referenced Site is not accessible
     * @throws UnsupportedQueryTypeException if the type of the parsed query is
     * not supported by this query service. Or in other words if the value of
     * {@link Query#getQueryType()} is not part of the collections returned by
     * {@link QueryService#getSupportedQueryTypes()}.
     */
    QueryResultList<? extends Entity> findSigns(Query query) throws IOException, UnsupportedQueryTypeException;
    /**
     * Getter for the types of queries supported by this implementation.
     * {@link Query#getQueryType()} is used to check if a query is supported.
     * @return the ids of the supported query types.
     */
    Collection<String> getSupportedQueryTypes();
}

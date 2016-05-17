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
package org.apache.stanbol.entityhub.query.clerezza;

import java.util.Iterator;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.entityhub.model.clerezza.RdfRepresentation;
import org.apache.stanbol.entityhub.query.sparql.SparqlEndpointTypeEnum;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;

/**
 * Moved to {@link org.apache.stanbol.entityhub.query.sparql.SparqlFieldQuery}
 * 
 * @author Rupert Westenthaler
 * @see org.apache.stanbol.entityhub.query.sparql.SparqlQueryUtils
 */
@Deprecated
public final class SparqlQueryUtils {

    /**
     * Restrict instantiation
     */
    private SparqlQueryUtils() {}

    /**
     * {@link IRI} constant for {@link RdfResourceEnum#queryResult}
     * 
     * @see ClerezzaQueryUtils#FIELD_QUERY_RESULT
     */
    @Deprecated
    public static final IRI FIELD_QUERY_RESULT = ClerezzaQueryUtils.FIELD_QUERY_RESULT;
    /**
     * {@link IRI} constant for {@link RdfResourceEnum#QueryResultSet}
     * 
     * @see ClerezzaQueryUtils#FIELD_QUERY_RESULT_SET
     */
    @Deprecated
    public static final IRI FIELD_QUERY_RESULT_SET = new IRI(RdfResourceEnum.QueryResultSet.getUri());

    /**
     * Use {@link org.apache.stanbol.entityhub.query.sparql.SparqlQueryUtils}
     * and {@link SparqlEndpointTypeEnum} instead
     *
     * @see SparqlEndpointTypeEnum
     */
    @Deprecated
    public static enum EndpointTypeEnum {
        Standard,
        Virtuoso(true),
        LARQ,
        ARQ,
        /**
         * Added to keep compatibility to {@link SparqlEndpointTypeEnum#Sesame}
         */
        Sesame(true);
        boolean supportsSparql11SubSelect;

        /**
         * Default feature set (SPARQL 1.0)
         */
        EndpointTypeEnum() {
            this(false);
        }

        /**
         * Allows to enable SPARQL 1.1 features
         * 
         * @param supportsSparql11SubSelect
         */
        EndpointTypeEnum(boolean supportsSparql11SubSelect) {
            this.supportsSparql11SubSelect = supportsSparql11SubSelect;
        }

        public final boolean supportsSubSelect() {
            return supportsSparql11SubSelect;
        }
    }

    /**
     * Creates a SPARWL CONSTRUCT query that creates triples for all the selected fields of representations in
     * the result set.
     * <p>
     * In addition the query also constructs <code>entityhub-query:ieldQueryResultSet
     * entityhub-query:fieldQueryResult ?representation </code> triples that can be used to create an iterator
     * over the results of the query
     * 
     * @param query
     *            the field query
     * @param endpointType
     *            The type of the Endpoint (used to write optimized queries for endpoint type specific
     *            extensions
     * @param additionalFields
     *            This allows to parse additional fields that are optionally selected in the data set and
     *            added to the CONSTRUCT part of the query
     * @return the SPARQL CONSTRUCT Query
     * @see org.apache.stanbol.entityhub.query.sparql.SparqlQueryUtils#createSparqlConstructQuery(org.apache.stanbol.entityhub.query.sparql.SparqlFieldQuery, SparqlEndpointTypeEnum, String...)
     */
    @Deprecated
    public static String createSparqlConstructQuery(SparqlFieldQuery query,
                                                    EndpointTypeEnum endpointType,
                                                    String... additionalFields) {
        SparqlEndpointTypeEnum type = endpointType == null ? null :
            SparqlEndpointTypeEnum.valueOf(endpointType.name());
        return org.apache.stanbol.entityhub.query.sparql.SparqlQueryUtils.createSparqlConstructQuery(
            query, type, additionalFields);
    }

    /**
     * Creates a SPARWL CONSTRUCT query that creates triples for all the selected fields of representations in
     * the result set.
     * <p>
     * In addition the query also constructs <code>entityhub-query:ieldQueryResultSet
     * entityhub-query:fieldQueryResult ?representation </code> triples that can be used to create an iterator
     * over the results of the query
     * 
     * @param query
     *            the field query
     * @param limit
     *            if a value > 0 is parsed, than this value overwrites the limit defined by the query.
     * @param endpointType
     *            The type of the Endpoint (used to write optimized queries for endpoint type specific
     *            extensions
     * @param additionalFields
     *            This allows to parse additional fields that are optionally selected in the data set and
     *            added to the CONSTRUCT part of the query
     * @return the SPARQL CONSTRUCT Query
     * @see org.apache.stanbol.entityhub.query.sparql.SparqlQueryUtils#createSparqlConstructQuery(org.apache.stanbol.entityhub.query.sparql.SparqlFieldQuery, int, SparqlEndpointTypeEnum, String...)
     */
    @Deprecated
    public static String createSparqlConstructQuery(SparqlFieldQuery query,
                                                    int limit,
                                                    EndpointTypeEnum endpointType,
                                                    String... additionalFields) {
        SparqlEndpointTypeEnum type = endpointType == null ? null :
            SparqlEndpointTypeEnum.valueOf(endpointType.name());
        return org.apache.stanbol.entityhub.query.sparql.SparqlQueryUtils.createSparqlConstructQuery(
            query,limit,type,additionalFields);

    }

    /**
     * Creates the SPARQL representation of the parse field query.
     * 
     * @param query
     *            A field query implementation that additionally supports a field to variable mapping
     * @param endpointType
     *            The type of the Endpoint (used to write optimized queries for endpoint type specific
     *            extensions
     * @return the SPARQL query as String
     * @see org.apache.stanbol.entityhub.query.sparql.SparqlQueryUtils#createSparqlSelectQuery(org.apache.stanbol.entityhub.query.sparql.SparqlFieldQuery, SparqlEndpointTypeEnum)
     */
    @Deprecated
    public static String createSparqlSelectQuery(SparqlFieldQuery query, EndpointTypeEnum endpointType) {
        SparqlEndpointTypeEnum type = endpointType == null ? null :
            SparqlEndpointTypeEnum.valueOf(endpointType.name());
        return org.apache.stanbol.entityhub.query.sparql.SparqlQueryUtils.createSparqlSelectQuery(query, type);
    }

    /**
     * Creates the SPARQL representation of the parse field query.
     * 
     * @param query
     *            A field query implementation that additionally supports a field to variable mapping
     * @param limit
     *            If > 0, than the limit parsed by the query is overriden by this value
     * @param endpointType
     *            The type of the Endpoint (used to write optimized queries for endpoint type specific
     *            extensions
     * @return the SPARQL query as String
     * @see org.apache.stanbol.entityhub.query.sparql.SparqlQueryUtils#createSparqlSelectQuery(org.apache.stanbol.entityhub.query.sparql.SparqlFieldQuery, int, SparqlEndpointTypeEnum)
     */
    @Deprecated
    public static String createSparqlSelectQuery(SparqlFieldQuery query,
                                                 int limit,
                                                 EndpointTypeEnum endpointType) {
        SparqlEndpointTypeEnum type = endpointType == null ? null :
            SparqlEndpointTypeEnum.valueOf(endpointType.name());
        return org.apache.stanbol.entityhub.query.sparql.SparqlQueryUtils.createSparqlSelectQuery(query,limit,type);
    }

    /**
     * Creates the SPARQL representation of the parse field query.
     * 
     * @param query
     *            A field query implementation that additionally supports a field to variable mapping
     * @param includeFields
     *            if <code>false</code> only the root is selected (selected fields are ignored)
     * @param endpointType
     *            The type of the Endpoint (used to write optimized queries for endpoint type specific
     *            extensions
     * @return the SPARQL query as String
     * @see org.apache.stanbol.entityhub.query.sparql.SparqlQueryUtils#createSparqlSelectQuery(org.apache.stanbol.entityhub.query.sparql.SparqlFieldQuery, boolean, SparqlEndpointTypeEnum)
     */
    @Deprecated
    public static String createSparqlSelectQuery(SparqlFieldQuery query,
                                                 boolean includeFields,
                                                 EndpointTypeEnum endpointType) {
        SparqlEndpointTypeEnum type = endpointType == null ? null :
            SparqlEndpointTypeEnum.valueOf(endpointType.name());
        return org.apache.stanbol.entityhub.query.sparql.SparqlQueryUtils.createSparqlSelectQuery(query,includeFields,type);
    }

    /**
     * Creates the SPARQL representation of the parse field query.
     * 
     * @param query
     *            A field query implementation that additionally supports a field to variable mapping
     * @param includeFields
     *            if <code>false</code> only the root is selected (selected fields are ignored)
     * @param limit
     *            if > 0 than the limit defined by the query is overridden by the parsed value
     * @param endpointType
     *            The type of the Endpoint (used to write optimized queries for endpoint type specific
     *            extensions
     * @return the SPARQL query as String
     * @see org.apache.stanbol.entityhub.query.sparql.SparqlQueryUtils#createSparqlSelectQuery(org.apache.stanbol.entityhub.query.sparql.SparqlFieldQuery, boolean, int, SparqlEndpointTypeEnum)
     */
    @Deprecated
    public static String createSparqlSelectQuery(SparqlFieldQuery query,
                                                 boolean includeFields,
                                                 int limit,
                                                 EndpointTypeEnum endpointType) {
        SparqlEndpointTypeEnum type = endpointType == null ? null :
            SparqlEndpointTypeEnum.valueOf(endpointType.name());
        return org.apache.stanbol.entityhub.query.sparql.SparqlQueryUtils.createSparqlSelectQuery(query,includeFields,limit, type);

    }

    /**
     * @param query
     * @param resultGraph
     * @return
     * @see ClerezzaQueryUtils#parseQueryResultsFromGraph(Graph)
     */
    @Deprecated
    public static Iterator<RdfRepresentation> parseQueryResultsFromGraph(final Graph resultGraph) {
        return ClerezzaQueryUtils.parseQueryResultsFromGraph(resultGraph);
    }

}

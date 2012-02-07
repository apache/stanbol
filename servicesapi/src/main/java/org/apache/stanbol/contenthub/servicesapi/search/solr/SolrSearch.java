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
package org.apache.stanbol.contenthub.servicesapi.search.solr;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.SolrParams;
import org.apache.stanbol.contenthub.servicesapi.ldpath.SemanticIndexManager;
import org.apache.stanbol.contenthub.servicesapi.search.SearchException;

/**
 * Apache Solr based search interface of Stanbol Contenthub. It makes use of SolrJ API in the provided
 * services such that it is possible to provide queries passed in {@link SolrParams} and response are returned
 * in the form of {@link QueryResponse}s. This interface also allows querying different Solr cores which are
 * created based on the LDPath programs submitted through the {@link SemanticIndexManager}.
 * 
 * @author anil.sinaci
 * 
 */
public interface SolrSearch {

    /**
     * Queries the default Solr core of Contenthub with the given <code>queryTerm</code>.
     * 
     * @param queryTerm
     *            Query term to be searched
     * @return the {@link QueryResponse} as is obtained from Solr.
     * @throws SearchException
     */
    QueryResponse search(String queryTerm) throws SearchException;

    /**
     * Queries the Solr core corresponding to the given <code>ldProgramName</code> of Contenthub with the
     * given <code>queryTerm</code>.
     * 
     * @param queryTerm
     *            Query term to be searched
     * @param indexName
     *            LDPath program name (Solr core/index name) to obtain the corresponding Solr core to be
     *            searched
     * @return the {@link QueryResponse} as is obtained from Solr.
     * @throws SearchException
     */
    QueryResponse search(String queryTerm, String indexName) throws SearchException;

    /**
     * Executes the given <code>solrQuery</code> on the default Solr core of Contenthub.
     * 
     * @param solrQuery
     *            {@link SolrParams} to be executed
     * @return the {@link QueryResponse} as is obtained from Solr.
     * @throws SearchException
     */
    QueryResponse search(SolrParams solrQuery) throws SearchException;

    /**
     * Executes the given <code>solrQuery</code> on the Solr core corresponding to the given
     * <code>ldProgramName</code> of Contenthub.
     * 
     * @param solrQuery
     *            {@link SolrParams} to be executed
     * @param indexName
     *            LDPath program name (Solr core/index name) to obtain the corresponding Solr core to be
     *            searched
     * @return the {@link QueryResponse} as is obtained from Solr.
     * @throws SearchException
     */
    QueryResponse search(SolrParams solrQuery, String indexName) throws SearchException;

}

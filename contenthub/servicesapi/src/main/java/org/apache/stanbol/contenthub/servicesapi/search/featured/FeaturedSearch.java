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
package org.apache.stanbol.contenthub.servicesapi.search.featured;

import java.util.List;

import org.apache.solr.common.params.SolrParams;
import org.apache.stanbol.contenthub.servicesapi.search.SearchException;
import org.apache.stanbol.contenthub.servicesapi.search.related.RelatedKeywordSearch;
import org.apache.stanbol.contenthub.servicesapi.search.solr.SolrSearch;

/**
 * This interface provides search functionalities over the submitted content items to the Contenthub. It
 * mainly uses the two main search components of Contenthub which are {@link SolrSearch} and
 * {@link RelatedKeywordSearch}. The main aim of the interface to provide user with a unified search
 * functionality which returns results containing fetched documents from underlying Solr cores together with
 * the related keywords that are obtained from various types of sources.
 * 
 * @author anil.sinaci
 * @see SolrSearch
 * @see RelatedKeywordSearch
 */
public interface FeaturedSearch {

    /**
     * This method returns a {@link SearchResult} as a unified search response. The response contains content
     * items retrieved from the default index of Contenthub for the given <code>queryTerm</code>. It also
     * consists of related keywords which are obtained from the available {@link RelatedKeywordSearch}
     * instances. To obtain related keywords, the given query term is tokenized with
     * {@link #tokenizeEntities(String)}. And then, related keyword searchers are queried for all the query
     * tokens. Furthermore, the {@link SearchResult} includes Solr facets that are obtained for the obtained
     * content items.
     * 
     * @param queryTerm
     *            Query term for which the unified response will be obtained
     * @return {@link SearchResult} for the given query term. For details of the response see
     *         {@link SearchResult}.
     * @throws SearchException
     */
    SearchResult search(String queryTerm) throws SearchException;

    /**
     * This method returns a {@link SearchResult} as a unified search response. The response contains content
     * items retrieved from the index, which is accessed using the given <code>indexName</code>, of Contenthub
     * for the given <code>queryTerm</code>. This name corresponds to a Solr Core name within Contenthub. It
     * also consists of related keywords that are obtained from the available {@link RelatedKeywordSearch}
     * instances. This method also takes an ontology URI. Using the URI, actual ontology is retrieved and it
     * is used as related keyword source. To obtain related keywords, the given query term is tokenized with
     * {@link #tokenizeEntities(String)}. And then, related keyword searchers are queried for all the query
     * tokens. Furthermore, the {@link SearchResult} includes Solr facets that are obtained for the obtained
     * content items.
     * 
     * @param queryTerm
     *            Query term for which the unified response will be obtained
     * @param ontologyURI
     *            URI of an ontology in which related keywords will be searched
     * @param indexName
     *            LDPath program name (name of the Solr core/index) which is used to obtained the
     *            corresponding Solr core which will be searched for the given query term
     * @return {@link SearchResult} for the given query term. For details of the response see
     *         {@link SearchResult}.
     * @throws SearchException
     */
    SearchResult search(String queryTerm, String ontologyURI, String indexName) throws SearchException;

    /**
     * This methods returns a {@link SearchResult} as a unified search response. The response contains content
     * items retrieved from the default index of Contenthub after executing the given <code>solrQuery</code>.
     * It also consists of related keywords that are obtained from the available {@link RelatedKeywordSearch}
     * instances. To obtain related keywords, first the meaningful query terms are extracted from the Solr
     * query and then they are tokenized with {@link #tokenizeEntities(String)}. And then, related keyword
     * searchers are queried for all the query tokens. Furthermore, the {@link SearchResult} includes Solr
     * facets that are obtained for the obtained content items.
     * 
     * @param solrQuery
     *            for which the search results will be obtained
     * @return a unified response in a {@link SearchResult} containing actual content items, related keywords
     *         and facets for the obtained content items.
     * @throws SearchException
     */
    SearchResult search(SolrParams solrQuery) throws SearchException;

    /**
     * This methods returns a {@link SearchResult} as a unified search response. The response contains content
     * items retrieved from the index, which is accessed using the given <code>indexName</code>, of
     * Contenthub for the given <code>queryTerm</code>. This name corresponds to a Solr Core name within
     * Contenthub. It also consists of related keywords that are obtained from the available
     * {@link RelatedKeywordSearch} instances. This method also takes an ontology URI. Using the URI, actual
     * ontology is obtained and it is used as related keyword source. To obtain related keywords, first the
     * meaningful query terms are extracted from the Solr query and then they are tokenized with
     * {@link #tokenizeEntities(String)}. And then, related keyword searchers are queried for all the query
     * tokens. Furthermore, the {@link SearchResult} includes Solr facets that are obtained for the obtained
     * content items.
     * 
     * @param solrQuery
     *            for which the search results will be obtained
     * @return a unified response in a {@link SearchResult} containing actual content items, related keywords
     *         and facets for the obtained content items.
     * @throws SearchException
     */
    SearchResult search(SolrParams solrQuery, String ontologyURI, String indexName) throws SearchException;

    /**
     * This method obtains the available field names of the default index of Contenthub.
     * 
     * @return {@link List} of field names related index
     * @throws SearchException
     */
    List<FacetResult> getAllFacetResults() throws SearchException;

    /**
     * This method obtains the available field names of the index, corresponding to the given
     * <code>indexName</code> of Contenthub. This name corresponds to a Solr Core name within Contenthub.
     * 
     * @param indexName
     *            Name of the index for which the field names will be obtained.
     * @return {@link List} of field names related index
     * @throws SearchException
     */
    List<FacetResult> getAllFacetResults(String indexName) throws SearchException;

    /**
     * This method tokenizes the given query term with the help of Stanbol Enhancer. The query term is fed to
     * Enhancer and labels of obtained named entities are search in the original query term and if any found
     * they are treated as a single query term.
     * 
     * @param queryTerm
     *            To be tokenized
     * @return {@link List} of query tokens of the given <code>queryTerm</code>.
     */
    List<String> tokenizeEntities(String queryTerm);

}

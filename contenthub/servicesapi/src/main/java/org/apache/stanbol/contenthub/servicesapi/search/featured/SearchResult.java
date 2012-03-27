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
import java.util.Map;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.stanbol.contenthub.servicesapi.search.related.RelatedKeyword;
import org.apache.stanbol.contenthub.servicesapi.search.related.RelatedKeywordSearchManager;

/**
 * This interface defines the structure of a unified search result returned by {@link FeaturedSearch} and
 * {@link RelatedKeywordSearchManager} interfaces. The results contain {@link DocumentResult}s which are
 * retrieved from underlying Solr cores, {@link RelatedKeyword}s about the search query and {@link FacetField}
 * s for the obtained resultant documents. {@link FacetField}s keep information of possible facet values and
 * corresponding documents counts matching with the facet value. All search results of a search operation are
 * encapsulated within this interface.
 * <p>
 * 
 * @author anil.sinaci
 * 
 */
public interface SearchResult {

    /**
     * Returns the resultant documents for the query term that is specified for the search operation. These
     * resultant documents correspond to content items stored in the underlying Solr cores which are managed
     * by the Contenthub.
     * 
     * @return {@link List} of {@link DocumentResult} encapsulated in this search result
     */
    List<DocumentResult> getDocuments();

    /**
     * Returns the facets generated as a result of the search operations. Each search result has its own
     * facets.
     * 
     * @return A {@link List} of {@link FacetResult}s.
     */
    List<FacetResult> getFacets();

    /**
     * Returns the {@link RelatedKeyword}s for the query term that is specified for the search operation.
     * 
     * @return A {@link Map} containing the {@link RelatedKeyword}s. Keys of the map represents different
     *         tokens that are produced from the original query term. Value of a key is another {@link Map}.
     *         Keys of this inner map represents sources/categories of the related keywords. Values of the
     *         inner map keeps {@link List} of {@link RelatedKeyword}s.
     */
    Map<String,Map<String,List<RelatedKeyword>>> getRelatedKeywords();

    /**
     * Setter for the resultant documents list
     * 
     * @param documentResults
     */
    void setDocuments(List<DocumentResult> documentResults);

    /**
     * Setter for the facets list
     * 
     * @param facets
     */
    void setFacets(List<FacetResult> facets);

    /**
     * Setter for the related keywords
     * 
     * @param relatedKeywords
     */
    void setRelatedKeywords(Map<String,Map<String,List<RelatedKeyword>>> relatedKeywords);

}

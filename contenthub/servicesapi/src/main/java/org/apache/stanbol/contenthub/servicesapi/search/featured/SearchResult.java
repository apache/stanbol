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

/**
 * This interface defines the structure of a search result returned by
 * {@link Search#search(String[], String, List, Map)}. All results of a search operation are encapsulated.
 * 
 * @author anil.sinaci
 * 
 */
public interface SearchResult {

    List<ResultantDocument> getResultantDocuments();

    /**
     * Returns the facets generated as a result of the search operations. Each search result has its own
     * facets.
     * 
     * @return A map of <code>property:[value1,value2]</code> pairs.
     */
    List<FacetField> getFacets();

    Map<String,Map<String,List<RelatedKeyword>>> getRelatedKeywords();
    
    void setDocuments(List<ResultantDocument> resultantDocuments);

    void setFacets(List<FacetField> facets);
    
    void setRelatedKeywords(Map<String,Map<String,List<RelatedKeyword>>> relatedKeywords);

}

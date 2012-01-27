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
package org.apache.stanbol.contenthub.search.featured;

import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.stanbol.contenthub.servicesapi.search.featured.ResultantDocument;
import org.apache.stanbol.contenthub.servicesapi.search.featured.SearchResult;
import org.apache.stanbol.contenthub.servicesapi.search.related.RelatedKeyword;

public class FeaturedSearchResult implements SearchResult {

    private List<ResultantDocument> resultantDocuments;
    private List<FacetField> facets;
    private Map<String,Map<String,List<RelatedKeyword>>> relatedKeywords;
    
    public FeaturedSearchResult(List<ResultantDocument> resultantDocuments,
                                List<FacetField> facets,
                                Map<String,Map<String,List<RelatedKeyword>>> relatedKeywords) {
        this.resultantDocuments = resultantDocuments;
        this.facets = facets;
        this.relatedKeywords = relatedKeywords;
    }

    @Override
    public List<ResultantDocument> getResultantDocuments() {
        return this.resultantDocuments;
    }

    @Override
    public List<FacetField> getFacets() {
        return this.facets;
    }
    
    @Override
    public Map<String,Map<String,List<RelatedKeyword>>> getRelatedKeywords() {
        return this.relatedKeywords;
    }

    @Override
    public void setDocuments(List<ResultantDocument> resultantDocuments) {
        this.resultantDocuments = resultantDocuments;
    }

    @Override
    public void setFacets(List<FacetField> facets) {
        this.facets = facets;
    }

    @Override
    public void setRelatedKeywords(Map<String,Map<String,List<RelatedKeyword>>> relatedKeywords) {
        this.relatedKeywords = relatedKeywords;
    }

}

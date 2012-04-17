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
package org.apache.stanbol.contenthub.search.related;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.contenthub.search.related.ontologyresource.OntologyResourceSearch;
import org.apache.stanbol.contenthub.search.related.referencedsite.ReferencedSiteSearch;
import org.apache.stanbol.contenthub.servicesapi.search.SearchException;
import org.apache.stanbol.contenthub.servicesapi.search.featured.SearchResult;
import org.apache.stanbol.contenthub.servicesapi.search.related.RelatedKeyword;
import org.apache.stanbol.contenthub.servicesapi.search.related.RelatedKeywordSearch;
import org.apache.stanbol.contenthub.servicesapi.search.related.RelatedKeywordSearchManager;

@Component
@Service
public class RelatedKeywordSearchManagerImpl implements RelatedKeywordSearchManager {

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, referenceInterface = RelatedKeywordSearch.class, policy = ReferencePolicy.DYNAMIC, strategy = ReferenceStrategy.EVENT, bind = "bindRelatedKeywordSearch", unbind = "unbindRelatedKeywordSearch")
    private List<RelatedKeywordSearch> rkwSearchers = new CopyOnWriteArrayList<RelatedKeywordSearch>();

    protected void bindRelatedKeywordSearch(RelatedKeywordSearch relatedKeywordSearch) {
        rkwSearchers.add(relatedKeywordSearch);
    }

    protected void unbindRelatedKeywordSearch(RelatedKeywordSearch relatedKeywordSearch) {
        rkwSearchers.remove(relatedKeywordSearch);
    }
    
    @Override
    public SearchResult getRelatedKeywordsFromAllSources(String keyword) throws SearchException {
        return getRelatedKeywordsFromAllSources(keyword, null);
    }

    @Override
    public SearchResult getRelatedKeywordsFromAllSources(String keyword, String ontologyURI) throws SearchException {
        Map<String,List<RelatedKeyword>> relatedKeywords = new HashMap<String,List<RelatedKeyword>>();
        for (RelatedKeywordSearch searcher : rkwSearchers) {
            relatedKeywords.putAll(searcher.search(keyword, ontologyURI));
        }
        Map<String,Map<String,List<RelatedKeyword>>> relatedKeywordsMap = new HashMap<String,Map<String,List<RelatedKeyword>>>();
        relatedKeywordsMap.put(keyword, relatedKeywords);
        return new RelatedKeywordSearchResult(relatedKeywordsMap);
    }

    private SearchResult getRelatedKeywordsFrom(String keyword, Class<? extends RelatedKeywordSearch> cls, String... ontologyURI) throws SearchException {
        Map<String,List<RelatedKeyword>> relatedKeywords = new HashMap<String,List<RelatedKeyword>>();
        for (RelatedKeywordSearch searcher : rkwSearchers) {
            if(cls.isAssignableFrom(searcher.getClass())) {
                if(ontologyURI != null && ontologyURI.length > 0) {
                    relatedKeywords.putAll(searcher.search(keyword, ontologyURI[0]));    
                } else {
                    relatedKeywords.putAll(searcher.search(keyword));
                }
                break;
            }
        }
        Map<String,Map<String,List<RelatedKeyword>>> relatedKeywordsMap = new HashMap<String,Map<String,List<RelatedKeyword>>>();
        relatedKeywordsMap.put(keyword, relatedKeywords);
        return new RelatedKeywordSearchResult(relatedKeywordsMap);
    }

    @Override
    public SearchResult getRelatedKeywordsFromOntology(String keyword, String ontologyURI) throws SearchException {
        return getRelatedKeywordsFrom(keyword, OntologyResourceSearch.class, ontologyURI);
    }

    @Override
    public SearchResult getRelatedKeywordsFromReferencedSites(String keyword) throws SearchException {
        return getRelatedKeywordsFrom(keyword, ReferencedSiteSearch.class);
    }
}

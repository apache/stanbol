package org.apache.stanbol.contenthub.servicesapi.search.related;

import org.apache.stanbol.contenthub.servicesapi.search.SearchException;
import org.apache.stanbol.contenthub.servicesapi.search.featured.SearchResult;

public interface RelatedKeywordSearchManager {
    
    SearchResult getRelatedKeywordsFromAllSources(String keyword) throws SearchException;

    SearchResult getRelatedKeywordsFromAllSources(String keyword, String ontologyURI) throws SearchException;
    
    SearchResult getRelatedKeywordsFromWordnet(String keyword) throws SearchException;
    
    SearchResult getRelatedKeywordsFromOntology(String keyword, String ontologyURI) throws SearchException;
    
    SearchResult getRelatedKeywordsFromReferencedCites(String keyword) throws SearchException;
}

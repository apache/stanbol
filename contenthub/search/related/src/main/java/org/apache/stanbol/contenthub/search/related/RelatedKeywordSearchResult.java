/**
 * 
 */
package org.apache.stanbol.contenthub.search.related;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.stanbol.contenthub.servicesapi.search.featured.ResultantDocument;
import org.apache.stanbol.contenthub.servicesapi.search.featured.SearchResult;
import org.apache.stanbol.contenthub.servicesapi.search.related.RelatedKeyword;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author anil.sinaci
 *
 */
public class RelatedKeywordSearchResult implements SearchResult {

    private static final Logger log = LoggerFactory.getLogger(RelatedKeywordSearchResult.class); 
    
    private Map<String,Map<String,List<RelatedKeyword>>> relatedKeywords;
    
    public RelatedKeywordSearchResult(Map<String,Map<String,List<RelatedKeyword>>> relatedKeywords) {
        this.relatedKeywords = relatedKeywords;
    }
    
    @Override
    public List<ResultantDocument> getResultantDocuments() {
        log.warn("RelatedKeywordSearchResult does not contain any ResultantDocument");
        return null;
    }

    @Override
    public List<FacetField> getFacets() {
        log.warn("RelatedKeywordSearchResult does not contain any FacetField");
        return null;
    }

    @Override
    public Map<String,Map<String,List<RelatedKeyword>>> getRelatedKeywords() {
        return this.relatedKeywords;
    }

    @Override
    public void setDocuments(List<ResultantDocument> resultantDocuments) {
        String msg = "RelatedKeywordSearchResult cannot contain any ResultantDocument";
        log.error(msg);
        throw new NotImplementedException(msg);
    }

    @Override
    public void setFacets(List<FacetField> facets) {
        String msg = "RelatedKeywordSearchResult cannot contain any FacetField";
        log.error(msg);
        throw new NotImplementedException(msg);
    }

    @Override
    public void setRelatedKeywords(Map<String,Map<String,List<RelatedKeyword>>> relatedKeywords) {
        this.relatedKeywords = relatedKeywords;
    }

}

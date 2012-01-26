package org.apache.stanbol.contenthub.servicesapi.search.solr;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.SolrParams;
import org.apache.stanbol.contenthub.servicesapi.search.SearchException;

public interface SolrSearch {

    QueryResponse search(String queryTerm) throws SearchException;
    
    QueryResponse search(String queryTerm, String ldProgramName) throws SearchException;
    
    QueryResponse search(SolrParams solrQuery) throws SearchException;
    
    QueryResponse search(SolrParams solrQuery, String ldProgramName) throws SearchException;
    
}

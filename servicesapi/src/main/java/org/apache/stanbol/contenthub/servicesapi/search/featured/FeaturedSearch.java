/**
 * 
 */
package org.apache.stanbol.contenthub.servicesapi.search.featured;

import java.util.List;

import org.apache.solr.common.params.SolrParams;
import org.apache.stanbol.contenthub.servicesapi.search.SearchException;

/**
 * @author anil.sinaci
 * 
 */
public interface FeaturedSearch {

    SearchResult search(String queryTerm) throws SearchException;
    
    SearchResult search(String queryTerm, String ontologyURI, String ldProgramName) throws SearchException;
    
    SearchResult search(SolrParams solrQuery) throws SearchException;
    
    SearchResult search(SolrParams solrQuery, String ontologyURI, String ldProgramName) throws SearchException;

    List<String> getFacetNames() throws SearchException;
    
    List<String> getFacetNames(String ldProgramName) throws SearchException;
    
    List<String> tokenizeEntities(String queryTerm);

}

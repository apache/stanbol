package org.apache.stanbol.contenthub.test;

import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.apache.stanbol.contenthub.servicesapi.ldpath.LDPathException;
import org.apache.stanbol.contenthub.servicesapi.ldpath.SemanticIndexManager;
import org.apache.stanbol.contenthub.servicesapi.search.SearchException;
import org.apache.stanbol.contenthub.servicesapi.search.featured.FeaturedSearch;
import org.apache.stanbol.contenthub.servicesapi.store.StoreException;
import org.apache.stanbol.contenthub.servicesapi.store.solr.SolrStore;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SlingAnnotationsTestRunner.class)
public class FeaturedSearchTest {

	
/*
SearchResult search(String queryTerm)
SearchResult search(String queryTerm, String ontologyURI, String ldProgramName)
SearchResult search(SolrParams solrQuery)
SearchResult search(SolrParams solrQuery, String ontologyURI, String ldProgramName)
List<String> getFacetNames()
List<String> getFacetNames(String ldProgramName)
List<String> tokenizeEntities(String queryTerm)
*/
	@TestReference
	FeaturedSearch featuredSearch;
	
	@TestReference
	SemanticIndexManager semanticIndexManager;
	
	@TestReference
	SolrStore solrStore;
	
	@Test
	public void testSearch() throws SearchException, LDPathException, StoreException{
//		semanticIndexManager.submitProgram(TestVocabulary.programName, TestVocabulary.ldPathProgram);
//		SolrContentItem sci = solrStore.create(TestVocabulary.contentByte, TestVocabulary.id, TestVocabulary.title, TestVocabulary.contentType, TestVocabulary.constraints);
//		solrStore.put(sci, TestVocabulary.programName);
//		featuredSearch.search(TestVocabulary.queryTerm, TestVocabulary.ontologyURI.getUnicodeString(), TestVocabulary.programName);
//		semanticIndexManager.deleteProgram(TestVocabulary.programName);
	}
}

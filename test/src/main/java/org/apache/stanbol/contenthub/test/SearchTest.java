package org.apache.stanbol.contenthub.test;

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.apache.stanbol.contenthub.servicesapi.ldpath.LDPathException;
import org.apache.stanbol.contenthub.servicesapi.ldpath.SemanticIndexManager;
import org.apache.stanbol.contenthub.servicesapi.search.SearchException;
import org.apache.stanbol.contenthub.servicesapi.search.solr.SolrSearch;
import org.apache.stanbol.contenthub.servicesapi.store.StoreException;
import org.apache.stanbol.contenthub.servicesapi.store.solr.SolrStore;
import org.apache.stanbol.contenthub.store.solr.manager.SolrCoreManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;

@RunWith(SlingAnnotationsTestRunner.class)
public class SearchTest {

/*
SolrQueryUtil.java:
	String extractQueryTermFromSolrQuery(SolrParams solrQuery)
	SolrQuery prepareFacetedSolrQuery(String queryTerm,List<String> allAvailableFacetNames,Map<String,List<Object>> constraints)
	SolrQuery prepareDefaultSolrQuery(SolrServer solrServer, String queryTerm)
	SolrQuery prepareDefaultSolrQuery(String queryTerm, List<String> allAvailableFacetNames)
	List<String> getFacetNames(SolrServer solrServer)
*/
	@TestReference
	private SolrSearch solrSearch;

	@TestReference
	private SolrStore solrStore;

	@TestReference
	private SemanticIndexManager semanticIndexManager;
	
	@TestReference
	private BundleContext bundleContext;

	@TestReference
	private ManagedSolrServer managedSolrServer;
	
	@Before
	public void before(){
		if(semanticIndexManager.isManagedProgram(TestVocabulary.programName)){
			semanticIndexManager.deleteProgram(TestVocabulary.programName);
		}
		if(SolrCoreManager.getInstance(bundleContext, managedSolrServer).isManagedSolrCore(TestVocabulary.programName)){
			SolrCoreManager.getInstance(bundleContext, managedSolrServer).deleteSolrCore(TestVocabulary.programName);
		}
	}
	
	@Test
	public void testSearch() throws StoreException, SearchException, SolrServerException, IOException {

		SolrServer solrServer = SolrCoreManager.getInstance(bundleContext, managedSolrServer).getServer();
		TestUtils.submitDocumentToSolr(solrServer, 1, true);

		QueryResponse qr = solrSearch.search(TestVocabulary.queryTerm);
		assertTrue(qr.getResults().size() > 0);

		TestUtils.deleteDocument(solrServer, solrStore.getEnhancementGraph());
	}

	@Test
	public void testSearchWithLD() throws SearchException, LDPathException,
			StoreException, SolrServerException, IOException {
		semanticIndexManager.submitProgram(TestVocabulary.programName,
				TestVocabulary.ldPathProgram);
		QueryResponse qr = solrSearch.search(TestVocabulary.queryTerm,
				TestVocabulary.programName);
		assertTrue(qr.getResults().size() == 0);

		SolrServer solrServer = SolrCoreManager.getInstance(bundleContext, managedSolrServer).getServer(TestVocabulary.programName);
		TestUtils.submitDocumentToSolr(solrServer, 1, false);

		qr = solrSearch.search(TestVocabulary.queryTerm,
				TestVocabulary.programName);

		assertTrue(qr.getResults().size() > 0);
		
		TestUtils.deleteDocument(solrServer, solrStore.getEnhancementGraph());
		semanticIndexManager.deleteProgram(TestVocabulary.programName);
	}
	
	@Test
	public void testSearchWithSolrParams() throws StoreException, SolrServerException, IOException, SearchException{
		SolrServer solrServer = SolrCoreManager.getInstance(bundleContext, managedSolrServer).getServer();
		TestUtils.submitDocumentToSolr(solrServer, 1, true);

		QueryResponse qr = solrSearch.search(TestVocabulary.solrQuery);
		assertTrue(qr.getResults().size() > 0);

		TestUtils.deleteDocument(solrServer, solrStore.getEnhancementGraph());
	}
	
	@Test
	public void testSearchWithSolrParamsLD() throws LDPathException, SearchException, StoreException, SolrServerException, IOException{
		semanticIndexManager.submitProgram(TestVocabulary.programName,
				TestVocabulary.ldPathProgram);
		QueryResponse qr = solrSearch.search(TestVocabulary.queryTerm,
				TestVocabulary.programName);
		assertTrue(qr.getResults().size() == 0);

		SolrServer solrServer = SolrCoreManager.getInstance(bundleContext, managedSolrServer).getServer(TestVocabulary.programName);
		TestUtils.submitDocumentToSolr(solrServer, 1, false);

		qr = solrSearch.search(TestVocabulary.solrQuery,
				TestVocabulary.programName);

		assertTrue(qr.getResults().size() > 0);
		
		TestUtils.deleteDocument(solrServer, solrStore.getEnhancementGraph());
		semanticIndexManager.deleteProgram(TestVocabulary.programName);
	}
}

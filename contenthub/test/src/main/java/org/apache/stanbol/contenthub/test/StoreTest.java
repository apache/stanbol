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
package org.apache.stanbol.contenthub.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.stanbol.commons.solr.managed.ManagedIndexState;
import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.apache.stanbol.contenthub.servicesapi.Constants;
import org.apache.stanbol.contenthub.servicesapi.ldpath.LDPathException;
import org.apache.stanbol.contenthub.servicesapi.ldpath.SemanticIndexManager;
import org.apache.stanbol.contenthub.servicesapi.store.StoreException;
import org.apache.stanbol.contenthub.servicesapi.store.solr.SolrContentItem;
import org.apache.stanbol.contenthub.servicesapi.store.solr.SolrStore;
import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary.SolrFieldName;
import org.apache.stanbol.contenthub.store.solr.SolrContentItemImpl;
import org.apache.stanbol.contenthub.store.solr.manager.SolrCoreManager;
import org.apache.stanbol.contenthub.store.solr.util.ContentItemIDOrganizer;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;

@RunWith(SlingAnnotationsTestRunner.class)
public class StoreTest {

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
	public void testSolrContentItemImpl() throws IOException{
		SolrContentItem sci = new SolrContentItemImpl(TestVocabulary.id,
				TestVocabulary.title, TestVocabulary.contentByte,
				TestVocabulary.contentType, null, TestVocabulary.constraints);
		assertEqual(sci,false);
		assertEquals(TestVocabulary.consValuesArray, sci.getConstraints().get(TestVocabulary.consFieldName));
	}
	
	@Test
	public void testDummyCreate() throws IOException {
		ContentItem ci = solrStore.create(TestVocabulary.id,
				TestVocabulary.contentByte, TestVocabulary.contentType);

		assertArrayEquals(TestVocabulary.contentByte,
				IOUtils.toByteArray(ci.getStream()));
		assertEquals(TestVocabulary.id,
				ContentItemIDOrganizer.detachBaseURI(ci.getUri()
						.getUnicodeString()));
		assertEquals(TestVocabulary.contentType, ci.getMimeType());
	}

	@Test
	public void testCreate() throws IOException {
		SolrContentItem sci = solrStore.create(TestVocabulary.contentByte,
				TestVocabulary.id, TestVocabulary.title,
				TestVocabulary.contentType, TestVocabulary.constraints);
		assertEqual(sci,false);
		assertEquals(TestVocabulary.consValuesArray, sci.getConstraints().get(TestVocabulary.consFieldName));
	}

	@Test
	public void testPut() throws StoreException, IOException,
			SolrServerException {

		SolrContentItem sci = new SolrContentItemImpl(TestVocabulary.id,
				TestVocabulary.title, TestVocabulary.contentByte,
				TestVocabulary.contentType, null, TestVocabulary.constraints);
		solrStore.put(sci);
		SolrServer solrServer = SolrCoreManager.getInstance(bundleContext,managedSolrServer).getServer();
		SolrDocument result = TestUtils.getSolrDocument(solrServer);
		assertEqual(result,true);

		TestUtils.deleteDocument(solrServer,solrStore.getEnhancementGraph());
	}

	@Test
	public void testPutWithLD() throws StoreException, IOException,
			LDPathException, SolrServerException {
		//We assume that if user give an LD Program, he will not give any constraints
		SolrContentItem sci = new SolrContentItemImpl(TestVocabulary.id,
				TestVocabulary.title, TestVocabulary.contentByte,
				TestVocabulary.contentType, null, null);

		semanticIndexManager.submitProgram(TestVocabulary.programName,
				TestVocabulary.ldPathProgram);
		solrStore.put(sci, TestVocabulary.programName);
		SolrServer solrServer = SolrCoreManager.getInstance(bundleContext,managedSolrServer).getServer(TestVocabulary.programName);
		SolrDocument result = TestUtils.getSolrDocument(solrServer);
		assertEqual(result,false);

		TestUtils.deleteDocument(solrServer,solrStore.getEnhancementGraph());
		semanticIndexManager.deleteProgram(TestVocabulary.programName);
	}

	@Test
	public void testGet() throws StoreException, SolrServerException, IOException {
		SolrServer solrServer = SolrCoreManager.getInstance(bundleContext, managedSolrServer).getServer();
		TestUtils.submitDocumentToSolr(solrServer,1,true);
        
        SolrContentItem sci = (SolrContentItem) solrStore.get(TestVocabulary.id);
        assertEqual(sci,true);
        
        TestUtils.deleteDocument(solrServer,solrStore.getEnhancementGraph());
	}
	
	@Test
	public void testGetWithLD() throws LDPathException, StoreException, SolrServerException, IOException{
		semanticIndexManager.submitProgram(TestVocabulary.programName, TestVocabulary.ldPathProgram);
		SolrServer solrServer = SolrCoreManager.getInstance(bundleContext, managedSolrServer).getServer(TestVocabulary.programName);
		TestUtils.submitDocumentToSolr(solrServer,1,false);
		
		SolrContentItem sci = solrStore.get(TestVocabulary.id, TestVocabulary.programName);
		assertEqual(sci,false);
		
		TestUtils.deleteDocument(solrServer,solrStore.getEnhancementGraph());
		semanticIndexManager.deleteProgram(TestVocabulary.programName);
	}
	
	@Test
	public void testDeleteById() throws StoreException, SolrServerException, IOException{
		SolrServer solrServer = SolrCoreManager.getInstance(bundleContext, managedSolrServer).getServer();
		TestUtils.submitDocumentToSolr(solrServer,1,true);
		
		solrStore.deleteById(TestVocabulary.id);
		SolrDocument result = TestUtils.getSolrDocument(solrServer);
		assertNull(result);
	}
	
	@Test
	public void testDeleteByIdWithLD() throws StoreException, SolrServerException, IOException, LDPathException{
		
		semanticIndexManager.submitProgram(TestVocabulary.programName, TestVocabulary.ldPathProgram);
		SolrServer solrServer = SolrCoreManager.getInstance(bundleContext, managedSolrServer).getServer(TestVocabulary.programName);
		TestUtils.submitDocumentToSolr(solrServer,1,false);
		
		solrStore.deleteById(TestVocabulary.id, TestVocabulary.programName);
		SolrDocument result = TestUtils.getSolrDocument(solrServer);
		assertNull(result);
		
		semanticIndexManager.deleteProgram(TestVocabulary.programName);
	}
	
	@Test
	public void testDeleteByIds() throws StoreException, SolrServerException, IOException{
		SolrServer solrServer = SolrCoreManager.getInstance(bundleContext, managedSolrServer).getServer();
		TestUtils.submitDocumentToSolr(solrServer,2,true);
		
		List<String> idList = new ArrayList<String>();
		idList.add(TestVocabulary.id);
		idList.add(TestVocabulary.id+"2");
		
		solrStore.deleteById(idList);
		SolrDocument result = TestUtils.getSolrDocument(solrServer);
		assertNull(result);
	}
	
	@Test
	public void testDeleteByIdsWithLD() throws StoreException, SolrServerException, IOException, LDPathException{
		
		semanticIndexManager.submitProgram(TestVocabulary.programName, TestVocabulary.ldPathProgram);
		SolrServer solrServer = SolrCoreManager.getInstance(bundleContext, managedSolrServer).getServer(TestVocabulary.programName);
		TestUtils.submitDocumentToSolr(solrServer,2,false);
		
		List<String> idList = new ArrayList<String>();
		idList.add(TestVocabulary.id);
		idList.add(TestVocabulary.id+"2");
		
		solrStore.deleteById(idList, TestVocabulary.programName);
		SolrDocument result = TestUtils.getSolrDocument(solrServer);
		assertNull(result);
		
		semanticIndexManager.deleteProgram(TestVocabulary.programName);
	}
	
	@Test
	public void testGetInstance(){
		assertNotNull(SolrCoreManager.getInstance(bundleContext, managedSolrServer));
	}
	
	@Test
	public void testGetServer() throws StoreException, LDPathException{
		assertNotNull(SolrCoreManager.getInstance(bundleContext, managedSolrServer).getServer());
		semanticIndexManager.submitProgram(TestVocabulary.programName, TestVocabulary.ldPathProgram);
		assertNotNull(SolrCoreManager.getInstance(bundleContext, managedSolrServer).getServer(TestVocabulary.programName));
		semanticIndexManager.deleteProgram(TestVocabulary.programName);
	}
	
	@Test
	public void testCreateDefaultSolrServer() throws StoreException{
		SolrCoreManager.getInstance(bundleContext, managedSolrServer).createDefaultSolrServer();
		assertEquals(ManagedIndexState.ACTIVE, managedSolrServer.getIndexState(SolrCoreManager.CONTENTHUB_SOLR_SERVER_NAME));
	}
	
//	@Test
//	public void testCreateSolrCore(){
//		SolrCoreManager ins = SolrCoreManager.getInstance(bundleContext, managedSolrServer);
//		ins.createSolrCore(
//                TestVocabulary.programName, ldPathUtils.createSchemaArchive(TestVocabulary.programName, TestVocabulary.ldPathProgram));
//		ins.isManagedSolrCore(TestVocabulary.programName);
//		ins.deleteSolrCore(TestVocabulary.programName);
//	}
	
//	@Test
//	public void testDeleteSolrCore(){
//	
//	}
	

	private Map<String, List<Object>> getSolrConstraints(SolrDocument result) {
		Map<String, List<Object>> solrConstraints = new HashMap<String, List<Object>>();
		Iterator<Entry<String, Object>> itr = result.iterator();
		while (itr.hasNext()) {
			Entry<String, Object> entry = itr.next();
			String key = entry.getKey();
			if (!SolrFieldName.isNameReserved(key)) {
				List<Object> values = (List<Object>) result.getFieldValues(key);
				solrConstraints.put(key, values);
			}
		}
		return solrConstraints;
	}
	
	private void assertEqual(SolrContentItem sci, boolean checkCons) throws IOException{
        assertEquals(TestVocabulary.attachedId, sci.getUri().getUnicodeString());
        assertEquals(TestVocabulary.content, IOUtils.toString(sci.getStream(), Constants.DEFAULT_ENCODING));
        assertEquals(TestVocabulary.contentType, sci.getMimeType());
        assertEquals(TestVocabulary.title, sci.getTitle());
        if(checkCons){
        	assertEquals(TestVocabulary.consValuesArray, sci.getConstraints().get(TestVocabulary.consFieldName+TestVocabulary.consFieldType));
        }
	}
	
	private void assertEqual(SolrDocument result, boolean checkCons) {
		assertEquals(TestVocabulary.content,
				(String) result.getFieldValue(SolrFieldName.CONTENT.toString()));
		assertEquals(TestVocabulary.id,
				ContentItemIDOrganizer.detachBaseURI((String) result
						.getFieldValue(SolrFieldName.ID.toString())));
		assertEquals(TestVocabulary.title,
				(String) result.getFieldValue(SolrFieldName.TITLE.toString()));
		assertEquals(
				TestVocabulary.contentType,
				(String) result.getFieldValue(SolrFieldName.MIMETYPE.toString()));

		Map<String, List<Object>> solrConstraints = getSolrConstraints(result);

		if (checkCons) {
			assertEquals(
					TestVocabulary.constraints
							.get(TestVocabulary.consFieldName),
					solrConstraints.get(TestVocabulary.consFieldName+TestVocabulary.consFieldType));
		}
	}
}

package org.apache.stanbol.contenthub.test.it;

import static org.apache.stanbol.contenthub.store.solr.util.ContentItemIDOrganizer.CONTENT_ITEM_URI_PREFIX;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.ws.rs.core.Response.Status;


import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.sling.junit.annotations.TestReference;
import org.apache.stanbol.commons.testing.http.RequestExecutor;
import org.apache.stanbol.commons.testing.stanbol.StanbolTestBase;
import org.apache.stanbol.contenthub.servicesapi.Constants;
import org.apache.stanbol.contenthub.servicesapi.store.StoreException;
import org.apache.stanbol.contenthub.servicesapi.store.solr.SolrContentItem;
import org.apache.stanbol.contenthub.servicesapi.store.solr.SolrStore;
import org.apache.stanbol.contenthub.test.TestVocabulary;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.junit.Test;

public class StoreResourceTest extends StanbolTestBase{

	@TestReference
	SolrStore solrStore;
	
	@Test
	public void testGetContent(){
////		@Path("/content/{uri:.+}")
//		
	}
	
	@Test
	public void testDownloadContentItem(){
		//metadata,content
	}
	
	@Test
	public void testGetContentItemMetaData(){
		
	}
	
//	getRawContent
	
	@Test
	public void testCreateContentItemWithId(){
		
	}
	
	@Test
	public void testCreateContentItemFromForm() throws ClientProtocolException, UnsupportedEncodingException, IOException, StoreException {
		RequestExecutor test = executor.execute(builder.buildPostRequest(
				"/contenthub/store").withFormContent("content", TestVocabulary.content,
				"constraints", URLEncoder.encode("{author:meric}", Constants.DEFAULT_ENCODING)));
		test.assertStatus(201);
		// test.assertContentType("ContentItemResource");
		// test.assertContentContains("I live in Paris.");

		solrStore.deleteById(ContentItemHelper.makeDefaultUri(CONTENT_ITEM_URI_PREFIX, TestVocabulary.contentByte).toString());
	}
	
	
	
	@Test
	public void testUpdateContentItemFromForm() throws StoreException, ClientProtocolException, UnsupportedEncodingException, IOException{
		String initialID = "updatedItemid01";
		String initialContent = "TestContent for update";
		SolrContentItem sci = solrStore.create(initialContent.getBytes(), initialID, TestVocabulary.title, TestVocabulary.contentType, TestVocabulary.constraints);
		solrStore.put(sci);
		
		RequestExecutor test = executor.execute(builder.buildPostRequest(
				"/contenthub/update").withFormContent("uri", initialID,
				"content", TestVocabulary.content, "title", TestVocabulary.title));
		test.assertStatus(Status.OK.getStatusCode());
	}
	
	@Test
	public void testDeleteContentItem() throws StoreException, ClientProtocolException, UnsupportedEncodingException, IOException{
		SolrContentItem sci = solrStore.create(TestVocabulary.contentByte, TestVocabulary.id, TestVocabulary.title, TestVocabulary.contentType, TestVocabulary.constraints);
		solrStore.put(sci);
		
		String path = builder.buildUrl("/contenthub/" + TestVocabulary.id);

		RequestExecutor test = executor.execute(builder
				.buildOtherRequest(new HttpDelete(path)));
		test.assertStatus(Status.OK.getStatusCode());
		
//		ContentItem res = solrStore.get(TestVocabulary.id);
	}
}

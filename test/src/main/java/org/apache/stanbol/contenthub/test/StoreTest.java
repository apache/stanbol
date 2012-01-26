package org.apache.stanbol.contenthub.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.apache.stanbol.contenthub.servicesapi.store.solr.SolrContentItem;
import org.apache.stanbol.contenthub.servicesapi.store.solr.SolrStore;
import org.apache.stanbol.contenthub.store.solr.util.ContentItemIDOrganizer;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SlingAnnotationsTestRunner.class)
public class StoreTest {
	@TestReference
	private SolrStore solrStore;

	private static byte[] content;
	private static String id, title, contentType;
	private static Map<String, List<Object>> constraints = new HashMap<String, List<Object>>();

	@BeforeClass
	public static void beforeClass() {
		content = "I love Paris".getBytes();
		id = "ab41c32a";
		title = "Paris";
		contentType = "text/plain";
		List<Object> value = new ArrayList<Object>();
		value.add("meric");
		constraints.put("author", value);
	}

	@Test
	public void testCreate() {
		SolrContentItem sci = solrStore.create(content, id, title, contentType,
				constraints);
//		assertEquals(content, sci.getStream());
		assertEquals(id,ContentItemIDOrganizer.detachBaseURI(sci.getUri().toString()));
		assertEquals(title,sci.getTitle());
		assertEquals(contentType,sci.getMimeType());
		assertEquals(constraints,sci.getConstraints());
	}
}

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
import java.util.List;
import java.util.Map;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.commons.io.IOUtils;
import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.apache.stanbol.contenthub.core.store.SolrContentItemImpl;
import org.apache.stanbol.contenthub.core.utils.ContentItemIDOrganizer;
import org.apache.stanbol.contenthub.servicesapi.store.SolrContentItem;
import org.apache.stanbol.contenthub.servicesapi.store.SolrStore;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RunWith(SlingAnnotationsTestRunner.class)
public class CoreUnitTest {

	private static final Logger logger = LoggerFactory
			.getLogger(CoreUnitTest.class);

	private static MGraph metadata;
	private static String id;
	private static String mimeType;
	private static byte[] data;
	private static String content;
	private static Map<String, List<Object>> constraints;
	private static String title;

	@TestReference
	private SolrStore store;

	@BeforeClass
	public static void before() {
		metadata = new SimpleMGraph();
		id = "a76b7e72a923";
		mimeType = "text/plain";
		data = "test content".getBytes();
		content = "test content";
		constraints = new HashMap<String, List<Object>>();
		List<Object> titleList = new ArrayList<Object>();
		titleList.add("meric");
		constraints.put("author", titleList);
		title = "Test Title";
	}

	public void assertEqualItem(SolrContentItem sci) throws IOException {
		assertEquals("SolrContentItem's id is not matching with original id",
				id, ContentItemIDOrganizer.detachBaseURI(sci.getUri()
						.getUnicodeString()));
//		assertEquals(
//				"SolrContentItem's title is not matching with original title",
//				title, sci.getTitle());
		assertEquals(
				"SolrContentItem's content is not matching with original content",
				content, IOUtils.toString(sci.getStream()));
		assertEquals(
				"SolrContentItem's mimeType is not matching with original mimeType",
				mimeType, sci.getMimeType());
		 assertEquals("SolrContentItem's metadata is not matching with original metadata",
		 metadata.toString(), sci.getMetadata().toString());
		assertEquals(
				"SolrContentItem's constraints is not matching with original constraints",
				constraints, sci.getConstraints());

	}

	@Test
	public void SolrContentItemImplConsTest() throws IOException {
		SolrContentItem sci = new SolrContentItemImpl(data,
				new UriRef(id), null, mimeType, metadata, constraints);
		assertEqualItem(sci);
	}
	
//	@Test
//	public void testGetEnhancementGraph(){
//		
//	}
//	
//	@Test
//	public void testEnhanceAndPut(){
//		
//	}
	
	@Test
	public void testCreate() throws IOException {

		SolrContentItem ci = (SolrContentItem) store.create("", data, mimeType);
		assertFalse("Creating item with empty id is failed", ci.getUri()
				.getUnicodeString().equals(""));

		SolrContentItem sci = store.create(data, id, title, mimeType,
				constraints);
		assertEqualItem(sci);
	}
	
	@Test
	public void testPut() throws IOException {
//		SolrContentItem sci = store.create(data, null, title, mimeType,
//				constraints);
//		try {
//			store.put(sci);
//			fail("put() should have thrown an exception!");
//		} catch (IllegalArgumentException e) {
//
//		}
		//TODO: null must be title field.
		SolrContentItem sci = store.create(data, id, title, mimeType, constraints);
		String sciID = store.put(sci);
		sci = (SolrContentItem) store.get(sciID);
		//TODO: constraint {author_t=[meric]}
//		assertEqualItem(sci);
		
		store.deleteById(sciID);
		assertNull("Returned a non-exist SolrContentItem", store.get(sciID));
		
	}

}

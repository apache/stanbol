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

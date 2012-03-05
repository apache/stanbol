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
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.apache.stanbol.contenthub.crawler.cnn.CNNImporter;
import org.apache.stanbol.contenthub.servicesapi.store.StoreException;
import org.apache.stanbol.contenthub.servicesapi.store.solr.SolrStore;
import org.apache.stanbol.contenthub.store.solr.manager.SolrCoreManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;

@RunWith(SlingAnnotationsTestRunner.class)
public class CNNTest {
	
	@TestReference
	private CNNImporter cNNImporter;
	
	@TestReference
	private SolrStore solrStore;
	
	@TestReference
	private BundleContext bundleContext;

	@TestReference
	private ManagedSolrServer managedSolrServer;

	@Test
	public void testImportCNNNews() throws SolrServerException, IOException, StoreException{
		Map<URI, String> res = cNNImporter.importCNNNews("Paris", 1, false);
		assertTrue(res.size() > 0);
		
		SolrServer solrServer = SolrCoreManager.getInstance(bundleContext, managedSolrServer).getServer();
		MGraph enhancementGraph = solrStore.getEnhancementGraph();
		
        Iterator<Triple> it = enhancementGraph.filter(new UriRef(TestVocabulary.attachedId), null, null);
        List<Triple> willBeRemoved = new ArrayList<Triple>();
        while (it.hasNext()) {
            willBeRemoved.add(it.next());
        }
        enhancementGraph.removeAll(willBeRemoved);
        
        solrServer.deleteById(res.keySet().toArray()[0].toString());
        solrServer.commit();
	}
}

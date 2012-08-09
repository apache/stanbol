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
package org.apache.stanbol.contenthub.test.index.ldpath;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.stanbol.commons.semanticindex.index.IndexException;
import org.apache.stanbol.commons.semanticindex.index.IndexManagementException;
import org.apache.stanbol.commons.semanticindex.index.IndexState;
import org.apache.stanbol.commons.semanticindex.index.SemanticIndex;
import org.apache.stanbol.commons.semanticindex.index.SemanticIndexManager;
import org.apache.stanbol.commons.semanticindex.store.Store;
import org.apache.stanbol.commons.semanticindex.store.StoreException;
import org.apache.stanbol.contenthub.index.ldpath.LDPathSemanticIndex;
import org.apache.stanbol.contenthub.index.ldpath.LDPathSemanticIndexManager;
import org.apache.stanbol.contenthub.servicesapi.index.search.SearchException;
import org.apache.stanbol.contenthub.servicesapi.index.search.featured.FeaturedSearch;
import org.apache.stanbol.contenthub.servicesapi.index.search.solr.SolrSearch;
import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary.SolrFieldName;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EnhancementException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(SlingAnnotationsTestRunner.class)
public class LDPathSemanticIndexTest {

    private static Logger logger = LoggerFactory.getLogger(LDPathSemanticIndexTest.class);

    private static final int TESTCOUNT = 15;

    @TestReference
    private LDPathSemanticIndexManager ldPathSemanticIndexManager;

    @TestReference
    private ContentItemFactory contentItemFactory;

    @TestReference
    private SemanticIndexManager semanticIndexManager;

    @TestReference
    private SolrSearch solrSearch;

    @TestReference
    private EnhancementJobManager jobManager;

    @TestReference
    private BundleContext bundleContext;

    private static LDPathSemanticIndex semanticIndex;
    private static SolrServer solrServer;
    private static String pid;
    private static int counter = 0;

    private Store<ContentItem> store;

    @Before
    public void before() throws IndexManagementException, IndexException, InterruptedException, IOException {
        String name = "test_index_name";
        if (counter == 0) {
            String program = "@prefix dbp-ont : <http://dbpedia.org/ontology/>; city = dbp-ont:city / rdfs:label :: xsd:string; country = dbp-ont:country / rdfs:label :: xsd:string; ";
            pid = ldPathSemanticIndexManager.createIndex(name, "test_index_description", program);
            SemanticIndex<ContentItem> tempSemanticIndex = getLDPathSemanticIndex(name);
            assertTrue("This tests assume that the Semantic Index with the name '" + name + "' is of type "
                       + LDPathSemanticIndex.class.getSimpleName(),
                tempSemanticIndex instanceof LDPathSemanticIndex);
            semanticIndex = (LDPathSemanticIndex) tempSemanticIndex;
            solrServer = semanticIndex.getServer();
        }
        if (store == null) {
            if (bundleContext != null) {
                store = getContenthubStore();
                if (store == null) {
                    throw new IllegalStateException("Null Store");
                }
            } else {
                throw new IllegalStateException("Null bundle context");
            }
        }
    }

    @Test
    public void ldPathSemanticIndexTest() {
        assertNotNull("Expecting LDPathSemanticIndex to be injected by Sling test runner", semanticIndex);
        assertTrue("Expection LDPathSemanticIndex implementation of SemanticIndex interface",
            semanticIndex instanceof LDPathSemanticIndex);
    }

    @Test
    public void ldPathSemanticIndexManagerTest() {
        assertNotNull("Expecting LDPathSemanticIndexManager to be injected by Sling test runner",
            ldPathSemanticIndexManager);
    }

    @Test
    public void contentItemFactoryTest() {
        assertNotNull("Expecting ContentItemFactory to be injected by Sling test runner", contentItemFactory);
    }

    @Test
    public void semanticIndexManagerTest() {
        assertNotNull("Expecting SemanticIndexManager to be injected by Sling test runner",
            semanticIndexManager);
    }

    @Test
    public void solrSearchTest() {
        assertNotNull("Expecting SolrSearch to be injected by Sling test runner", solrSearch);
    }

    @Test
    public void jobManagerTest() {
        assertNotNull("Expecting EnhancementJobManager to be injected by Sling test runner", jobManager);
    }

    @Test
    public void bundleContextTest() {
        assertNotNull("Expecting BundleContext to be injected by Sling test runner", bundleContext);
    }

    @Test
    public void testIndex() throws IndexException {
        String name = semanticIndex.getName();
        String id = "test_document_id";
        ContentItem ci;
        try {
            ci = contentItemFactory.createContentItem(new UriRef(id), new StringSource(
                    "test_document_content"));
        } catch (IOException e) {
            logger.error("Failed to create contentitem");
            throw new IndexException("Failed to create contentitem");
        }
        semanticIndex.index(ci);

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(SolrFieldName.ID.toString() + ":" + id);
        SolrDocumentList sdl = null;
        try {
            sdl = solrServer.query(solrQuery).getResults();
        } catch (SolrServerException e) {
            logger.error("Failed to get indexed document from solr for index " + name, e);
            throw new IndexException("Failed to get indexed document from solr for index " + name, e);
        }
        assertTrue("Indexed content item cannot be found", sdl.size() == 1);

        try {
            solrServer.deleteById(id);
            solrServer.commit();
        } catch (SolrServerException e) {
            logger.error("Failed to remove indexed document from solr for index " + name, e);
            throw new IndexException("Failed to remove indexed document from solr for index " + name, e);
        } catch (IOException e) {
            logger.error("Failed to remove indexed document from solr for index " + name, e);
            throw new IndexException("Failed to remove indexed document from solr for index " + name, e);
        }
    }

    @Test
    public void testRemove() throws IndexException {
        String name = semanticIndex.getName();
        String id = "test_document_id";
        try {
            SolrInputDocument doc = new SolrInputDocument();
            doc.addField(SolrFieldName.ID.toString(), id);
            doc.addField("title_t", "test_document_title");
            solrServer.add(doc);
            solrServer.commit();
        } catch (SolrServerException e) {
            logger.error("Test Document cannot be added to Solr Server with name " + name, e);
            throw new IndexException("Test Document cannot be added to Solr Server with name " + name, e);
        } catch (IOException e) {
            logger.error("Test Document cannot be added to Solr Server with name " + name, e);
            throw new IndexException("Test Document cannot be added to Solr Server with name " + name, e);
        }

        semanticIndex.remove(id);

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(SolrFieldName.ID.toString() + ":" + id);
        SolrDocumentList sdl = null;
        try {
            sdl = solrServer.query(solrQuery).getResults();
        } catch (SolrServerException e) {
            logger.error("Failed to get test document from solr for index " + name, e);
            throw new IndexException("Failed to get test document from solr for index " + name, e);
        }
        assertTrue("Test Document cannot be removed from Index with name " + name, sdl.size() == 0);
    }

    @Test
    public void testPersist() throws IndexException {
        semanticIndex.persist(3);
        assertTrue("Revision cannot be persist with given value 3", semanticIndex.getRevision() == 3);

        Properties indexMetadata = ldPathSemanticIndexManager.getIndexMetadata(pid);
        long revision = Long.parseLong((String) indexMetadata.get(SemanticIndex.PROP_REVISION));
        assertTrue(
            "Return value of getRevision() is not match with the revision stored in IndexMetadata File",
            (semanticIndex.getRevision() == revision));
    }

    @Test
    public void testGetRevision() throws IndexException {
        Field field = null;
        long revision = 0;
        try {
            field = semanticIndex.getClass().getDeclaredField("revision");
            field.setAccessible(true);
            revision = (Long) field.get(semanticIndex);
        } catch (SecurityException e) {
            logger.error("Failed to get private member revision");
            throw new IndexException("Failed to get private member revision");
        } catch (NoSuchFieldException e) {
            logger.error("There is no field with name revision");
            throw new IndexException("There is no field with name revision");
        } catch (IllegalArgumentException e) {
            logger.error("revision field cannot be retrieved from Semantic Index");
            throw new IndexException("revision field cannot be retrieved from Semantic Index");
        } catch (IllegalAccessException e) {
            logger.error("revision field cannot be retrieved from Semantic Index");
            throw new IndexException("revision field cannot be retrieved from Semantic Index");
        }
        assertTrue("Return value of getRevision() is not match with actual revision " + revision,
            revision == semanticIndex.getRevision());
    }

    @Test
    public void testGetFieldsNames() throws IndexException {
        List<String> fieldNames = semanticIndex.getFieldsNames();
        assertTrue(String.format("Index must contain %s field", SolrFieldName.ID.toString()),
            fieldNames.contains(SolrFieldName.ID.toString()));
        assertTrue("Index must contain city field", fieldNames.contains("city"));
        assertTrue("Index must contain country field", fieldNames.contains("country"));
    }

    @Test
    public void testGetFieldProperties() throws IndexException {
        Map<String,Object> fieldProperties = semanticIndex.getFieldProperties(SolrFieldName.ID.toString());
        String type = (String) fieldProperties.get("type");
        assertNotNull(String.format("Type property must exist in %s field", SolrFieldName.ID.toString()),
            type);
        assertTrue(String.format("Type property must be string in %s field", SolrFieldName.ID.toString()),
            type.equals("string"));

        Boolean required = (Boolean) fieldProperties.get("required");
        assertNotNull(String.format("required property must exist in %s field", SolrFieldName.ID.toString()),
            required);
        assertTrue(String.format("Type property must be string in %s field", SolrFieldName.ID.toString()),
            required.equals(true));
    }

    // @Test
    // public void testGetRESTSearchEndpoints() {
    // Map<EndpointType,String> searchEndpoints = semanticIndex.getRESTSearchEndpoints();
    // for(Entry<EndpointType,String> entry : searchEndpoints.entrySet()){
    // EndpointType endpointType = entry.getKey();
    // String endpoint = entry.getValue();
    // }
    // }

    @Test
    public void testGetSearchEndPoints() throws ClassNotFoundException {
        Map<String,ServiceReference> searchEndpoints = semanticIndex.getSearchEndPoints();
        int serviceCount = 0;
        for (Entry<String,ServiceReference> entry : searchEndpoints.entrySet()) {
            String className = entry.getKey();
            ServiceReference serviceReference = entry.getValue();
            Object service;
            if (SolrSearch.class.getName().equals(className)) {
                service = (SolrSearch) bundleContext.getService(serviceReference);
                assertNotNull(String.format(
                    "Service cannot be retrieved by given %s Class and its Service Reference", className),
                    service);
                serviceCount++;
            } else if (FeaturedSearch.class.getName().equals(className)) {
                service = (FeaturedSearch) bundleContext.getService(serviceReference);
                assertNotNull(String.format(
                    "Service cannot be retrieved by given %s Class and its Service Reference", className),
                    service);
                serviceCount++;
            }
        }
        assertTrue("One or more expected search service were not available", serviceCount == 2);
    }

    @Test
    public void testReindexingState() throws IndexManagementException,
                                     InterruptedException,
                                     IOException,
                                     IndexException,
                                     SearchException,
                                     StoreException,
                                     EnhancementException {
        String name = "test_index_name_for_reindexing";
        String program = "@prefix dbp-ont: <http://dbpedia.org/ontology/>; person_entities = .[rdf:type is dbp-ont:Person]:: xsd:anyURI (termVectors=\"true\");";
        String newProgram = "@prefix dbp-ont: <http://dbpedia.org/ontology/>; place_entities = .[rdf:type is dbp-ont:Place]:: xsd:anyURI (termVectors=\"true\");";
        String pid = ldPathSemanticIndexManager.createIndex(name, "", program);

        try {
            SemanticIndex<ContentItem> semanticIndex = getLDPathSemanticIndex(name);
            ContentItem ci = contentItemFactory.createContentItem(new StringSource(
                    "Michael Jackson is a very famous person, and he was born in Indiana."));
            jobManager.enhanceContent(ci);
            semanticIndex.index(ci);
            String query = "person_entities:\"http://dbpedia.org/resource/Michael_Jackson\"";
            SolrDocumentList sdl = solrSearch.search(query, name).getResults();
            assertNotNull("Result must not be null for query " + query, sdl);

            ServiceReference reference = bundleContext
                    .getServiceReference(ConfigurationAdmin.class.getName());
            ConfigurationAdmin configAdmin = (ConfigurationAdmin) bundleContext.getService(reference);
            Configuration config = configAdmin.getConfiguration(pid);
            @SuppressWarnings("unchecked")
            Dictionary<String,String> properties = config.getProperties();
            properties.put(LDPathSemanticIndex.PROP_LD_PATH_PROGRAM, newProgram);
            properties.put(LDPathSemanticIndex.PROP_DESCRIPTION, "reindexing");
            config.update(properties);
            Thread.sleep(1000);

            semanticIndex = getLDPathSemanticIndex(name);
            // index ci to new semantic index
            while (semanticIndex.getState() != IndexState.ACTIVE) {
                Thread.sleep(500);
            }
            semanticIndex.index(ci);

            Properties indexMetadata = ldPathSemanticIndexManager.getIndexMetadata(pid);
            assertTrue("LDPathSemanticIndex is changed, but it uses old program",
                indexMetadata.get(LDPathSemanticIndex.PROP_LD_PATH_PROGRAM).equals(newProgram));
            query = "place_entities:\"http://dbpedia.org/resource/Indiana\"";
            sdl = solrSearch.search(query, name).getResults();
            assertNotNull("Result must not be null for query " + query, sdl);

        } finally {
            ldPathSemanticIndexManager.removeIndex(pid);
        }
    }

    @After
    public void after() throws IndexManagementException {
        counter++;
        if (counter == TESTCOUNT) {
            counter = 0;
            ldPathSemanticIndexManager.removeIndex(pid);
        }
    }

    @SuppressWarnings("unchecked")
    private SemanticIndex<ContentItem> getLDPathSemanticIndex(String name) throws InterruptedException,
                                                                          IndexManagementException {

        SemanticIndex<ContentItem> tempSemanticIndex = (SemanticIndex<ContentItem>) semanticIndexManager
                .getIndex(name);
        int timeoutCount = 0;
        while (tempSemanticIndex == null) {
            if (timeoutCount == 8) break;
            Thread.sleep(500);
            tempSemanticIndex = (SemanticIndex<ContentItem>) semanticIndexManager.getIndex(name);
            timeoutCount++;
        }
        assertNotNull("Failed to retrieve SemanticIndex: " + name, tempSemanticIndex);
        return tempSemanticIndex;
    }

    @SuppressWarnings("unchecked")
    private Store<ContentItem> getContenthubStore() {
        Store<ContentItem> contentHubStore = null;
        try {
            ServiceReference[] stores = bundleContext.getServiceReferences(Store.class.getName(), null);
            for (ServiceReference serviceReference : stores) {
                Object store = bundleContext.getService(serviceReference);
                Type[] genericInterfaces = store.getClass().getGenericInterfaces();
                if (genericInterfaces.length == 1 && genericInterfaces[0] instanceof ParameterizedType) {
                    Type[] types = ((ParameterizedType) genericInterfaces[0]).getActualTypeArguments();
                    try {
                        @SuppressWarnings("unused")
                        Class<ContentItem> contentItemClass = (Class<ContentItem>) types[0];
                        if (((Store<ContentItem>) store).getName().equals("contenthubFileStore")) {
                            contentHubStore = (Store<ContentItem>) store;
                        }
                    } catch (ClassCastException e) {
                        // ignore
                    }
                }
            }
        } catch (InvalidSyntaxException e) {
            // ignore as there is no filter
        }
        return contentHubStore;
    }
}

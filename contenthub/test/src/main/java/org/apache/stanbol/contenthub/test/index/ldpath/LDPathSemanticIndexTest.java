package org.apache.stanbol.contenthub.test.index.ldpath;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.stanbol.contenthub.index.ldpath.LDPathSemanticIndex;
import org.apache.stanbol.contenthub.index.ldpath.LDPathSemanticIndexManager;
import org.apache.stanbol.contenthub.servicesapi.index.IndexException;
import org.apache.stanbol.contenthub.servicesapi.index.IndexManagementException;
import org.apache.stanbol.contenthub.servicesapi.index.SemanticIndex;
import org.apache.stanbol.contenthub.servicesapi.index.SemanticIndexManager;
import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary.SolrFieldName;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(SlingAnnotationsTestRunner.class)
public class LDPathSemanticIndexTest {

    private static Logger logger = LoggerFactory.getLogger(LDPathSemanticIndexTest.class);

    private static final int TESTCOUNT = 12;

    @TestReference
    private ContentItemFactory contentItemFactory;

    @TestReference
    private LDPathSemanticIndexManager ldPathSemanticIndexManager;

    @TestReference
    private SemanticIndexManager semanticIndexManager;

    @TestReference
    private BundleContext bundleContext;

    private static LDPathSemanticIndex semanticIndex;
    private static SolrServer solrServer;
    private static String pid;
    private static int counter = 0;

    @Before
    public void before() throws IndexManagementException, IndexException, InterruptedException, IOException {
        String name = "test_index_name";
        if (counter == 0) {
            String program = "@prefix dbp-ont : <http://dbpedia.org/ontology/>; city = dbp-ont:city / rdfs:label :: xsd:string; country = dbp-ont:country / rdfs:label :: xsd:string; ";
            pid = ldPathSemanticIndexManager.createIndex(name, "test_index_description", program);
            SemanticIndex tempSemanticIndex = semanticIndexManager.getIndex(name);
            int timeoutCount = 0;
            while (tempSemanticIndex == null) {
                if(timeoutCount == 8) break;
                Thread.sleep(500);
                tempSemanticIndex = semanticIndexManager.getIndex(name);
                timeoutCount++;
            }
            semanticIndex = (LDPathSemanticIndex) tempSemanticIndex;
            solrServer = semanticIndex.getServer();
        }
    }

    @Test
    public void ldPathSemanticIndexTest() {
        assertNotNull("Expecting LDPathSemanticIndex to be injected by Sling test runner", semanticIndex);
        assertTrue("Expection LDPathSemanticIndex implementation of SemanticIndex interface",
            semanticIndex instanceof LDPathSemanticIndex);
    }

    @Test
    public void bundleContextTest() {
        assertNotNull("Expecting BundleContext to be injected by Sling test runner", bundleContext);
    }

    @Test
    public void contentItemFactoryTest() {
        assertNotNull("Expecting ContentItemFactory to be injected by Sling test runner", contentItemFactory);
    }

    @Test
    public void ldPathSemanticIndexManagerTest() {
        assertNotNull("Expecting LDPathSemanticIndexManager to be injected by Sling test runner",
            ldPathSemanticIndexManager);
    }

    @Test
    public void semanticIndexManagerTest() {
        assertNotNull("Expecting SemanticIndexManager to be injected by Sling test runner",
            semanticIndexManager);
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

        semanticIndex.remove(new UriRef(id));

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
        String name = semanticIndex.getName();
        semanticIndex.persist(3);
        assertTrue("Revision cannot be persist with given value 3", semanticIndex.getRevision() == 3);

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(SolrFieldName.ID.toString() + ":" + SolrFieldName.REVISIONID.toString());
        SolrDocumentList sdl = null;
        try {
            sdl = solrServer.query(solrQuery).getResults();
        } catch (SolrServerException e) {
            logger.error("Failed to get revision from solr for index " + name, e);
            throw new IndexException("Failed to get revision from solr for index " + name, e);
        }
        SolrDocument solrDocument = sdl.get(0);
        assertTrue("Return value of getRevision() is not match with the revision stored in Solr",
            (semanticIndex.getRevision() == (Long) solrDocument.getFieldValue(SolrFieldName.REVISION
                    .toString())));
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
    public void testGetSearchEndPoints() {
        Map<Class<?>,ServiceReference> searchEndpoints = semanticIndex.getSearchEndPoints();
        for (Entry<Class<?>,ServiceReference> entry : searchEndpoints.entrySet()) {
            Class<?> clazz = entry.getKey();
            ServiceReference serviceReference = entry.getValue();
            Object service = clazz.cast(bundleContext.getService(serviceReference));
            assertNotNull(String.format(
                "Service cannot be retrieved by given %s Class and its Service Reference", clazz.getName()),
                service);
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
}

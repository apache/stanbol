package org.apache.stanbol.entityhub.yard.solr;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.apache.stanbol.entityhub.yard.solr.impl.SolrYard;
import org.apache.stanbol.entityhub.yard.solr.impl.SolrYardConfig;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TestInitOutsideOfOsgi {

    public static final String solrServer = "http://localhost:8181/solr/test";
    public static final String testYardId = "testYard";

    private static Logger log = LoggerFactory.getLogger(TestInitOutsideOfOsgi.class);

    private static URL solrServerUrl;
    private static boolean solrServerAvailable;
    @BeforeClass
    public static void init() throws MalformedURLException {
        solrServerUrl = new URL(solrServer);
        //test if the SolrServer needed for the test is available!
        SolrServer server = new CommonsHttpSolrServer(solrServerUrl);
        try {
            server.ping();
            solrServerAvailable = true;
        } catch (SolrServerException e) {
            solrServerAvailable = false;
            log.warn(String.format("No SolrServer available for URL %s. Will skip test that require a Solr Server!",solrServerUrl),e);
        } catch (IOException e) {
            solrServerAvailable = false;
            log.warn(String.format("No SolrServer available for URL %s. Will skip test that require a Solr Server!",solrServerUrl),e);
        }
        server = null;
    }
    @Test(expected=IllegalArgumentException.class)
    public void testSolrYardConfigInitWithNullParams() {
            new SolrYardConfig(null, null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void testSolrYardConfigInitWithNullUrl() {
            new SolrYardConfig(testYardId, null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void testSolrYardConfigInitWithNullID() {
            new SolrYardConfig(null, solrServerUrl);
    }
    @Test
    public void testMinimalSolrYardConfigInit() {
            new SolrYardConfig(testYardId, solrServerUrl);
    }
    @Test
    public void testSolrYardInitWithEmptyConfig() throws YardException,IllegalArgumentException {
        if(!solrServerAvailable){
            log.warn(" > Skip testSolrYardInitWithEmptyConfig because no SolrServer is available");
            return;
        }
        //create a yard, create a representation, add a value, store the
        //representation, check the value, remove the representation
        // -> this is not to test the Yard implementation, but only check the
        //    correct initialisation without an OSGI framework
        SolrYardConfig config = new SolrYardConfig(testYardId, solrServerUrl);
        SolrYard yard = new SolrYard(config);
        String testRepId = "urn:test.outsideOsgiEnv:test.1";
        String testField = NamespaceEnum.dcTerms+"title";
        String testFieldValue = "test1";
        String testFieldLanguage = "en";
        Representation test1 = yard.create(testRepId);
        test1.addNaturalText(testField, testFieldValue,testFieldLanguage);
        yard.store(test1);
        test1 = null;
        test1 = yard.getRepresentation(testRepId);
        Text value = test1.getFirst(testField, testFieldLanguage);
        assertTrue(value != null);
        assertTrue(testFieldValue.equals(value.getText()));
        assertTrue(testFieldLanguage.equals(value.getLanguage()));
        yard.remove(testRepId);
        test1 = null;
        test1 = yard.getRepresentation(testRepId);
        assertNull(test1);
    }
}

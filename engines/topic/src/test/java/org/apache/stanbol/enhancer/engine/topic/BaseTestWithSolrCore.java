package org.apache.stanbol.enhancer.engine.topic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.xml.sax.SAXException;

public class BaseTestWithSolrCore {

    public static final String TEST_CORE_ID = "test";

    /**
     * Create single core test solr server with it's own folder hierarchy.
     */
    public EmbeddedSolrServer makeEmptyEmbeddedSolrServer(File rootFolder,
                                                          String solrServerId,
                                                          String testCoreConfig) throws IOException,
                                                                                ParserConfigurationException,
                                                                                SAXException {
        File solrFolder = new File(rootFolder, solrServerId);
        FileUtils.deleteQuietly(solrFolder);
        solrFolder.mkdir();

        // solr conf file
        File solrFile = new File(solrFolder, "solr.xml");
        InputStream is = getClass().getResourceAsStream("/" + testCoreConfig + "/solr.xml");
        TestCase.assertNotNull("missing test solr.xml file", is);
        IOUtils.copy(is, new FileOutputStream(solrFile));

        // solr conf folder with schema
        File solrCoreFolder = new File(solrFolder, TEST_CORE_ID);
        solrCoreFolder.mkdir();
        File solrConfFolder = new File(solrCoreFolder, "conf");
        solrConfFolder.mkdir();
        File schemaFile = new File(solrConfFolder, "schema.xml");
        is = getClass().getResourceAsStream("/" + testCoreConfig + "/schema.xml");
        TestCase.assertNotNull("missing test solr schema.xml file", is);
        IOUtils.copy(is, new FileOutputStream(schemaFile));

        File solrConfigFile = new File(solrConfFolder, "solrconfig.xml");
        is = getClass().getResourceAsStream("/" + testCoreConfig + "/solrconfig.xml");
        TestCase.assertNotNull("missing test solrconfig.xml file", is);
        IOUtils.copy(is, new FileOutputStream(solrConfigFile));

        // create the embedded server
        CoreContainer coreContainer = new CoreContainer(solrFolder.getAbsolutePath(), solrFile);
        return new EmbeddedSolrServer(coreContainer, TEST_CORE_ID);
    }

}

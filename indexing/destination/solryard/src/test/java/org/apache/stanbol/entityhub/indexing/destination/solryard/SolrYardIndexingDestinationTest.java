package org.apache.stanbol.entityhub.indexing.destination.solryard;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import javax.naming.NameParser;

import org.apache.stanbol.entityhub.indexing.core.IndexingDestination;
import org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.apache.stanbol.entityhub.yard.solr.impl.SolrYard;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

/**
 * What to test:
 *  - correct initialisation
 *    - special schema initialisation
 *    - default schema initialisation
 *  - finalisation
 *    - writing of the IndexFieldConfiguration
 *    - creating of the {name}.solrindex.zip
 *    - creating of the {name}.solrindex.ref
 *    
 * Indexing needs not to be tested, because this is the responsibility of the
 * Unit Tests for the used Yard implementation.
 * 
 * @author Rupert Westenthaler
 *
 */
public class SolrYardIndexingDestinationTest {

    private static final Logger log = LoggerFactory.getLogger(SolrYardIndexingDestinationTest.class);
    /**
     * mvn copies the resources in "src/test/resources" to target/test-classes
     */
    private static final String TEST_CONFIGS_ROOT = "/target/test-classes/testConfigs/";

    /**
     * The path to the folder used as root for the tests
     */
    private static String testRoot;
    @BeforeClass
    public static void init(){
        //initialise based on basedir or user.dir
        String baseDir = System.getProperty("basedir");
        if(baseDir == null){
            baseDir = System.getProperty("user.dir");
        }
        testRoot = baseDir+TEST_CONFIGS_ROOT;
        log.info("Test Root ="+testRoot);
    }
    @Test(expected=IllegalArgumentException.class)
    public void testMissingBoostConfig(){
        IndexingConfig config = new IndexingConfig(testRoot+"missingBoostConfig");
        config.getIndexingDestination();
    }
    @Test(expected=IllegalArgumentException.class)
    public void testInvalidBoostConfig(){
        IndexingConfig config = new IndexingConfig(testRoot+"invalidBoostConfig");
        config.getIndexingDestination();
    }
    /**
     * Tests that the Solr configuration is required, but the name of the config
     * file is the default. The referenced directory is missing
     */
    @Test(expected=IllegalArgumentException.class)
    public void testMissingDefaultSolrSchemaConfig(){
        IndexingConfig config = new IndexingConfig(testRoot+"missingDefaultSolrConf");
        config.getIndexingDestination();
    }
    /**
     * Tests that the Solr configuration is required and the name of the config
     * file is specified. The referenced directory is missing
     */
    @Test(expected=IllegalArgumentException.class)
    public void testMissingSolrSchemaConfig(){
        IndexingConfig config = new IndexingConfig(testRoot+"missingSolrConf");
        config.getIndexingDestination();
    }
    @Test
    public void testSimple() throws YardException, IOException {
        IndexingConfig config = new IndexingConfig(testRoot+"simple");
        validateSolrDestination(config);
    }
    @Test
    public void testWithSolrConf() throws YardException, IOException {
        IndexingConfig config = new IndexingConfig(testRoot+"withSolrConf");
        validateSolrDestination(config);
    }
    
    /**
     * Checks if the SolrYardIndexingDestination returned by the 
     * {@link IndexingConfig} is valid and functional
     * @param config the configuration
     * @throws YardException indicates problems while working with the {@link SolrYard}
     * returned by {@link IndexingDestination#getYard()}
     * @throws IOException indicates problems while validating the SolrArchives
     * created by the {@link IndexingDestination#finalise()} method
     */
    private void validateSolrDestination(IndexingConfig config) throws YardException,
                                                               IOException {
        //get the destination
        IndexingDestination destination = config.getIndexingDestination();
        assertNotNull(destination);
        assertEquals(destination.getClass(), SolrYardIndexingDestination.class);
        //initialise
        assertTrue(destination.needsInitialisation());
        destination.initialise();
        //test that the returned Yard instance is functional
        Yard yard = destination.getYard();
        assertNotNull(yard);
        assertEquals(yard.getClass(), SolrYard.class);
        Representation rep = yard.create("http://www.example.com/entity#123");
        rep.add(NamespaceEnum.rdfs+"label", "test");
        rep.add(NamespaceEnum.rdfs+"description", "Representation to test storage while indexing");
        rep.add(RdfResourceEnum.signRank.getUri(), Float.valueOf(0.8f));
        yard.store(rep);
        //finalise
        destination.finalise();
        //test the archives
        File expectedSolrArchiveFile = 
            new File(config.getDistributionFolder(),config.getName()+".solrindex.zip");
        assertTrue(expectedSolrArchiveFile.isFile());
        //TODO: validate the archive
        
        //check for the solrArchive reference file and validate required properties
        File expectedSolrArchiveReferenceFile = 
            new File(config.getDistributionFolder(),config.getName()+".solrindex.ref");
        assertTrue(expectedSolrArchiveReferenceFile.isFile());
        Properties solrRefProperties = new Properties();
        solrRefProperties.load(new FileInputStream(expectedSolrArchiveReferenceFile));
        assertTrue(solrRefProperties.getProperty("Index-Archive").equals(expectedSolrArchiveFile.getName()));
        assertTrue(solrRefProperties.getProperty("Name") != null);
    }
    
}

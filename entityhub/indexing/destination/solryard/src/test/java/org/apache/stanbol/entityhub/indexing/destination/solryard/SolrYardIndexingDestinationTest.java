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
package org.apache.stanbol.entityhub.indexing.destination.solryard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FilenameUtils;
import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.apache.stanbol.commons.solr.managed.standalone.StandaloneManagedSolrServer;
import org.apache.stanbol.entityhub.indexing.core.IndexingDestination;
import org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig;
import org.apache.stanbol.entityhub.indexing.core.destination.OsgiConfigurationUtil;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.apache.stanbol.entityhub.yard.solr.impl.SolrYard;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static final Collection<String> EXPECTED_INDEX_ARCHIVE_FILE_NAMES = 
        Arrays.asList(
            "schema.xml",
            "solrconfig.xml",
            "segments.gen");
    
    public static final Collection<String> UNEXPECTED_INDEX_ARCHIVE_FILE_NAMES =
            Arrays.asList("write.lock"); //excluded with STANBOL1176
    
    private static final Logger log = LoggerFactory.getLogger(SolrYardIndexingDestinationTest.class);
    private static final String CONFIG_ROOT = "testConfigs";
    /**
     * mvn copies the resources in "src/test/resources" to target/test-classes.
     * This folder is than used as classpath.<p>
     * "/target/test-files/" does not exist, but is created by the
     * {@link IndexingConfig}.
     */
    private static final String TEST_ROOT = 
        FilenameUtils.separatorsToSystem("target/test-files");
    private static String  userDir;
    private static String testRoot;
    /**
     * The methods resets the "user.dir" system property
     */
    @BeforeClass
    public static void initTestRootFolder(){
        String baseDir = System.getProperty("basedir");
        if(baseDir == null){
            baseDir = System.getProperty("user.dir");
        }
        //store the current user.dir
        userDir = System.getProperty("user.dir");
        testRoot = FilenameUtils.concat(baseDir,TEST_ROOT);
        log.info("ConfigTest Root : "+testRoot);
        //set the user.dir to the testRoot (needed to test loading of missing
        //configurations via classpath
        //store the current user.dir and reset it after the tests
        System.setProperty("user.dir", testRoot);
    }
    /**
     * resets the "user.dir" system property the the original value
     */
    @AfterClass
    public static void cleanup(){
        System.setProperty("user.dir", userDir);
    }
    @After
    public void close(){
        //after each test we need ensure to shutdown the default ManagedSolrServer
        //because different tests use different Directories and therefore a new
        //instance needs to be created
        StandaloneManagedSolrServer.shutdownManagedServer();
    }
    @Test(expected=IllegalArgumentException.class)
    public void testMissingBoostConfig(){
        String testName = "missingBoostConfig";
        IndexingConfig config = new IndexingConfig(CONFIG_ROOT+File.separatorChar+testName,
            CONFIG_ROOT+'/'+testName){};
        config.getIndexingDestination();
    }
    @Test(expected=IllegalArgumentException.class)
    public void testInvalidBoostConfig(){
        String testName = "invalidBoostConfig";
        IndexingConfig config = new IndexingConfig(CONFIG_ROOT+File.separatorChar+testName,
            CONFIG_ROOT+'/'+testName){};
        config.getIndexingDestination();
    }
    /**
     * Tests that the Solr configuration is required, but the name of the config
     * file is the default. The referenced directory is missing
     */
    @Test(expected=IllegalArgumentException.class)
    public void testMissingDefaultSolrSchemaConfig(){
        String testName = "missingDefaultSolrConf";
        IndexingConfig config = new IndexingConfig(CONFIG_ROOT+File.separatorChar+testName,
            CONFIG_ROOT+'/'+testName){};
        config.getIndexingDestination();
    }
    /**
     * Tests that the Solr configuration is required and the name of the config
     * file is specified. The referenced directory is missing
     */
    @Test(expected=IllegalArgumentException.class)
    public void testMissingSolrSchemaConfig(){
        String testName = "missingSolrConf";
        IndexingConfig config = new IndexingConfig(CONFIG_ROOT+File.separatorChar+testName,
            CONFIG_ROOT+'/'+testName){};
        config.getIndexingDestination();
    }
    @Test
    public void testSimple() throws YardException, IOException {
        String testName = "simple";
        IndexingConfig config = new IndexingConfig(CONFIG_ROOT+File.separatorChar+testName,
            CONFIG_ROOT+'/'+testName){};
        validateSolrDestination(config);
    }
    @Test
    public void testWithSolrConf() throws YardException, IOException {
        String testName = "withSolrConf";
        IndexingConfig config = new IndexingConfig(CONFIG_ROOT+File.separatorChar+testName,
            CONFIG_ROOT+'/'+testName){};
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
        Representation rep = yard.getValueFactory().createRepresentation("http://www.example.com/entity#123");
        rep.add(NamespaceEnum.rdfs+"label", "test");
        rep.add(NamespaceEnum.rdfs+"description", "Representation to test storage while indexing");
        rep.add(RdfResourceEnum.entityRank.getUri(), Float.valueOf(0.8f));
        yard.store(rep);
        //finalise
        destination.finalise();
        //test the archives
        File expectedSolrArchiveFile = 
            new File(config.getDistributionFolder(),config.getName()+".solrindex.zip");
        assertTrue(expectedSolrArchiveFile.isFile());
        // validate the archive
        ZipFile archive = new ZipFile(expectedSolrArchiveFile);
        Set<String> expected = new HashSet<String>(EXPECTED_INDEX_ARCHIVE_FILE_NAMES);
        for(Enumeration<? extends ZipEntry> entries = archive.entries();entries.hasMoreElements();){
            ZipEntry entry = entries.nextElement();
            //the name of the index MUST be the root folder within the Archive!
            assertTrue(entry.getName().startsWith(config.getName()));
            String name = FilenameUtils.getName(entry.getName());
            if(expected.remove(name)){
                log.info("found expected Entry '{}'",entry.getName());
            }
            Assert.assertFalse("found unexpected Entry '"+entry.getName()+"' in "
                + "SolrIndexArchive", UNEXPECTED_INDEX_ARCHIVE_FILE_NAMES.contains(name));
        }
        assertTrue("missing Files in index archive: "+expected,expected.isEmpty());
        
        //TODO: reimplement to validate the created bundle!
//        //check for the solrArchive reference file and validate required properties
//        File expectedSolrArchiveReferenceFile = 
//            new File(,config.getName()+".solrindex.ref");
//        assertTrue(expectedSolrArchiveReferenceFile.isFile());
//        Properties solrRefProperties = new Properties();
//        solrRefProperties.load(new FileInputStream(expectedSolrArchiveReferenceFile));
//        assertTrue(solrRefProperties.getProperty("Index-Archive").equals(expectedSolrArchiveFile.getName()));
//        assertTrue(solrRefProperties.getProperty("Name") != null);
    }
    
}

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
package org.apache.stanbol.entityhub.indexing.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig;
import org.apache.stanbol.entityhub.indexing.core.normaliser.MinScoreNormalizer;
import org.apache.stanbol.entityhub.indexing.core.normaliser.NaturalLogNormaliser;
import org.apache.stanbol.entityhub.indexing.core.normaliser.RangeNormaliser;
import org.apache.stanbol.entityhub.indexing.core.normaliser.ScoreNormaliser;
import org.apache.stanbol.entityhub.indexing.core.source.LineBasedEntityIterator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigTest {
    private static final Logger log = LoggerFactory.getLogger(ConfigTest.class);
    private static final String CONFIG_ROOT = 
        FilenameUtils.separatorsToSystem("testConfigs/");
    /**
     * mvn copies the resources in "src/test/resources" to target/test-classes.
     * This folder is than used as classpath.<p>
     * "/target/test-files/" does not exist, but is created by the
     * {@link IndexingConfig}.
     */
    private static final String TEST_ROOT = 
        FilenameUtils.separatorsToSystem("/target/test-files");
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
        testRoot = baseDir+TEST_ROOT;
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
    /**
     * In the test setup there is no default configuration
     */
    @Test(expected=IllegalArgumentException.class)
    public void missingDefault(){
        new IndexingConfig(); //there is no indexing folder in the user.dir
    }
    /**
     * Tests failed initialisation because the configuration folder does not 
     * exist and no configuration with the name does exist
     */
    @Test(expected=IllegalArgumentException.class)
    public void missingConfig(){
        //this should create the specified folder and than throw an
        //illegalArgumentException because the indexing.properties file can not
        //be found in the classpath under
        new IndexingConfig(CONFIG_ROOT+"noConfig",CONFIG_ROOT+"noConfig"){};
    }
    /**
     * In this case the config exists in the classpath, but is not valid because
     * the required indexing.properties is missing
     */
    @Test(expected=IllegalArgumentException.class)
    public void missingConfigDir(){
        new IndexingConfig(CONFIG_ROOT+"missingconfig",CONFIG_ROOT+"missingconfig"){};
    }
    /**
     * Loads a simple but not functional configuration to test the loading and
     * parsing of configuration files
     */
    @Test
    public void loadSimpleConfigDir() throws IOException {
        String name = CONFIG_ROOT+"simple";
        IndexingConfig config = new IndexingConfig(name,name){};
        //assert that this directory exists (is created)
        File expectedRoot = new File(testRoot,name);
        expectedRoot = new File(expectedRoot,"indexing");
        assertTrue("Root Dir not created",expectedRoot.isDirectory());
        assertEquals("Root dir other the expected ",
            expectedRoot.getCanonicalPath(),config.getIndexingFolder().getCanonicalPath());
        assertTrue(config.getConfigFolder().isDirectory());
        assertTrue(config.getSourceFolder().isDirectory());
        assertTrue(config.getDestinationFolder().isDirectory());
        assertTrue(config.getDistributionFolder().isDirectory());
        //test the name
        assertEquals(config.getName(),"simple");
        assertEquals(config.getDescription(), "Simple Configuration");
        //test if the normaliser configuration was parsed correctly!
        final ScoreNormaliser normaliser = config.getNormaliser();
        //test if the config files where copied form the classpath to the
        //config directory.
        assertTrue("Config File for the RangeNormaliser not copied",
            new File(config.getConfigFolder(),"range.properties").isFile());
        assertTrue("Config File for the MinScoreNormalizer not copied",
            new File(config.getConfigFolder(),"minscore.properties").isFile());
        //now test if the configuration was parsed correctly
        ScoreNormaliser testNormaliser = normaliser;
        assertNotNull(testNormaliser);
        assertEquals(testNormaliser.getClass(), RangeNormaliser.class);
        testNormaliser = testNormaliser.getChained();
        assertNotNull(testNormaliser);
        assertEquals(testNormaliser.getClass(), NaturalLogNormaliser.class);
        testNormaliser = testNormaliser.getChained();
        assertNotNull(testNormaliser);
        assertEquals(testNormaliser.getClass(), MinScoreNormalizer.class);
        EntityIterator entityIterator = config.getEntityIdIterator();
        assertNotNull(entityIterator);
        assertEquals(entityIterator.getClass(), LineBasedEntityIterator.class);
        if(entityIterator.needsInitialisation()){
            entityIterator.initialise();
        }
        Map<String,Float> entityIds = new HashMap<String,Float>();
        //the values test if the normaliser configuration was readed correctly
        //the keys if the configured entiyScore file was configured correctly
        float boost = 10f/(float)Math.log1p(100);
        entityIds.put("http://www.example.org/entity/test", Float.valueOf(10));
        entityIds.put("http://www.example.org/entity/test2", Float.valueOf((float)(Math.log1p(10)*boost)));
        entityIds.put("http://www.example.org/entity/test3", Float.valueOf(-1));
        while(entityIterator.hasNext()){
            EntityIterator.EntityScore entityScore = entityIterator.next();
            Float expectedScore = entityIds.remove(entityScore.id);
            assertNotNull("Entity with ID "+entityScore.id+" not found!",expectedScore);
            Float score = normaliser.normalise(entityScore.score);
            assertTrue("Entity score "+score+" is not the expected "+expectedScore,expectedScore.compareTo(score)==0);
        }
        assertTrue(entityIds.isEmpty());
        List<EntityProcessor> processors = config.getEntityProcessors();
        assertNotNull(processors);
    }
    

}

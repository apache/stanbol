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
package org.apache.stanbol.entityhub.ldpath.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.stanbol.commons.solr.IndexReference;
import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.apache.stanbol.commons.solr.managed.standalone.StandaloneEmbeddedSolrServerProvider;
import org.apache.stanbol.entityhub.ldpath.backend.YardBackend;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.util.ModelUtils;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.yard.solr.impl.SolrYard;
import org.apache.stanbol.entityhub.yard.solr.impl.SolrYardConfig;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LDPathTestBase {

    private final static Logger log = LoggerFactory.getLogger(LDPathTestBase.class);
    /**
     * The SolrYard used for the tests
     */
    protected static SolrYard yard;
    protected static YardBackend backend;
    /**
     * The SolrDirectoryManager also tested within this unit test
     */
    public static final String TEST_YARD_ID = "dbpedia";
    public static final String TEST_SOLR_CORE_NAME = "dbpedia";
    public static final String TEST_SOLR_CORE_CONFIGURATION = "dbpedia_26k.solrindex.bz2";
    protected static final String TEST_INDEX_REL_PATH = File.separatorChar + "target" + File.separatorChar
                                                        + ManagedSolrServer.DEFAULT_SOLR_DATA_DIR;

    protected static final String DBPEDIA = "http://dbpedia.org/resource/";
    
    @BeforeClass
    public static void setup() throws Exception {
        // get the working directory
        // use property substitution to test this feature!
        String prefix = System.getProperty("basedir") == null ? "." : "${basedir}";
        String solrServerDir = prefix + TEST_INDEX_REL_PATH;
        log.info("Test Solr Server Directory: {}", solrServerDir);
        System.setProperty(ManagedSolrServer.MANAGED_SOLR_DIR_PROPERTY, solrServerDir);
        SolrYardConfig config = new SolrYardConfig(TEST_YARD_ID, TEST_SOLR_CORE_NAME);
        config.setAllowInitialisation(false);
        config.setIndexConfigurationName(TEST_SOLR_CORE_CONFIGURATION); //the dbpedia default data
        config.setAllowInitialisation(true); //init from datafile provider
        config.setName("DBpedia.org default data");
        config.setDescription("Data used for the LDPath setup");
        // create the Yard used for the tests
        IndexReference solrIndexRef = IndexReference.parse(config.getSolrServerLocation());
        
        SolrServer server = StandaloneEmbeddedSolrServerProvider.getInstance().getSolrServer(
            solrIndexRef, config.getIndexConfigurationName());
        Assert.assertNotNull("Unable to initialise SolrServer for testing",server);
        yard = new SolrYard(server,config,null);
        backend = new YardBackend(yard);
    }
    @AfterClass
    public static void cleanup() throws Exception {
        yard.close();
        yard = null;
    }
    
    protected abstract Collection<String> checkContexts();
    
    /**
     * Tests that the yard is setup correctly by checking for the
     * {@link Representation}s of the ids returned by {@link #checkContexts()}.
     * <p>
     * This methods should make is more easy to detect if a failure of a test
     * is because of a wrong setup of the Yard. 
     * @throws Exception 
     */
    @Test
    public void testSetup() throws Exception {
        log.info("check Setup");
        for(String context : checkContexts()){
            Representation rep = yard.getRepresentation(context);
            log.info("  > check Entity {}",rep.getId());
            assertNotNull(rep);
            assertEquals(rep.getId(),context);
            if(log.isInfoEnabled()){
                log.info("Data for Entity {}: \n {}",rep.getId(),ModelUtils.getRepresentationInfo(rep));
            }
        }
        log.info("   ... check completed");
    }
    
    /**
     * Utility method that checks the results of an LDPath execution against 
     * a map whit expected results 
     * @param result the results of the execution
     * @param expected the expected results
     * @throws IllegalStateException if the parsed expected results are <code>null</code>
     */
    protected final void assertLDPathResult(Map<String,Collection<?>> result, Map<String,Collection<?>> expected) {
        if(expected == null){
            throw new IllegalStateException("The expected LDPath execution results MUST NOT be NULL. This" +
            		"indicated an ERROR in the implementation of the Unit Test and NOT in the tested Component!");
        }
        assertNotNull("The result of the LDPath execution MUST NOT be NULL " +
            "(entity: %s)",result);
        Map<String,Collection<?>> expectedClone = cloneExpected(expected);
        for(Entry<String,Collection<?>> entry : result.entrySet()){
            log.info("{}: {}",entry.getKey(),entry.getValue());
            Collection<?> expectedValues = expectedClone.remove(entry.getKey());
            assertNotNull("Current field '"+entry.getKey()+"' is not expected (expected: " +
                    expectedClone.keySet()+"!",
                expectedValues);
            expectedValues.removeAll(entry.getValue());
            assertTrue("Missing expected Result '"+expectedValues+"' (present: '"
                +entry.getValue()+"'", expectedValues.isEmpty());
        }
        assertTrue("Missing expected Field '"+expectedClone.keySet()+"' (present: '"+
            result.keySet()+"'!",expectedClone.isEmpty());
    }
    /**
     * Creates a clone of expected results. This is necessary because assertion of
     * results does remove found fields and results and checks at the end for
     * empty collections of expected results of the field and an empty map with
     * the expected fields.
     * @param expected the expected results
     * @return the clone
     */
    protected final Map<String,Collection<?>> cloneExpected(Map<String,Collection<?>> expected) {
        Map<String,Collection<?>> expectedClone = new HashMap<String,Collection<?>>();
        for(Entry<String,Collection<?>> expectedEntries : expected.entrySet()){
            expectedClone.put(expectedEntries.getKey(), new HashSet<Object>(expectedEntries.getValue()));
        }
        return expectedClone;
    }
}

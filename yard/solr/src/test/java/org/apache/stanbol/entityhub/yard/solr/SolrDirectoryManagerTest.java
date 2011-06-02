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
package org.apache.stanbol.entityhub.yard.solr;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.stanbol.entityhub.yard.solr.impl.SolrYard;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrDirectoryManagerTest {

    private static final Logger log = LoggerFactory.getLogger(SolrDirectoryManagerTest.class);

    private static SolrDirectoryManager solrDirectoryManager;

    private static File expectedManagedDirectory;
    private static Collection<String> expectedIndexNames = Arrays.asList("entityhub", "cache");

    @BeforeClass
    public static void init() throws IOException {
        // set to "${basedir}/some/rel/path" to test if property substitution works!
        String prefix = System.getProperty("basedir") == null ? "." : "${basedir}";
        String resolvedPrefix = System.getProperty("basedir") == null ? "." : System.getProperty("basedir");
        String solrServerDir = prefix + SolrYardTest.TEST_INDEX_REL_PATH;
        log.info("configured directory: " + solrServerDir);
        System.setProperty(SolrDirectoryManager.MANAGED_SOLR_DIR_PROPERTY, solrServerDir);
        // store the expected managed directory for later testing
        expectedManagedDirectory = new File(resolvedPrefix, SolrYardTest.TEST_INDEX_REL_PATH);
        log.info("expected managed directory: " + expectedManagedDirectory);
        // create the SolrDirectoryManager used for the tests
        Iterator<SolrDirectoryManager> providerIt = ServiceLoader.load(SolrDirectoryManager.class,
            SolrDirectoryManager.class.getClassLoader()).iterator();
        if (providerIt.hasNext()) {
            solrDirectoryManager = providerIt.next();
        } else {
            throw new IllegalStateException("Unable to instantiate "
                                            + SolrDirectoryManager.class.getSimpleName()
                                            + " service by using " + ServiceLoader.class.getName() + "!");
        }
        //setup the entityhub and the cache index (as it would be done by the Entityhub)
        //to test this initialisation
        solrDirectoryManager.createSolrDirectory("entityhub", "entityhub", null);
        //for the cahce we use the default configuration
        solrDirectoryManager.createSolrDirectory("cache", SolrYard.DEFAULT_SOLR_INDEX_CONFIGURATION_NAME, null);
    }

    @Test
    public void testManagedDirectoryInitialisation() {
        // the managed directory must be set based on the
        expectedManagedDirectory.equals(solrDirectoryManager.getManagedDirectory());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullIndexName() {
        solrDirectoryManager.getSolrIndexDirectory(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyIndexName() {
        solrDirectoryManager.getSolrIndexDirectory("");
    }

    @Test
    public void testGetManagedIndexes() {
        Set<String> expected = new HashSet<String>(expectedIndexNames);
        for (Entry<String,File> index : solrDirectoryManager.getManagedIndices().entrySet()) {
            expected.remove(index.getKey());
            // test that the index dir is the expected location
            File expectedLocation = new File(expectedManagedDirectory, index.getKey());
            assertEquals(expectedLocation, index.getValue());
        }
        // test that the expected indexes where returned
        assertTrue(expected.isEmpty());
    }

    @Test
    public void testIsManagedIndex() {
        for (String name : expectedIndexNames) {
            assertTrue(solrDirectoryManager.isManagedIndex(name));
        }
        assertFalse(solrDirectoryManager.isManagedIndex("notAnIndex" + System.currentTimeMillis()));
    }

    @Test
    public void testDefaultIndexInitialisation() throws IOException {
        // this is actually tested already by the initialisation of the
        // SolrYardTest ...
        String indexName = "testIndexInitialisation_" + System.currentTimeMillis();
        File indexDir = solrDirectoryManager.createSolrDirectory(indexName, 
            SolrYard.DEFAULT_SOLR_INDEX_CONFIGURATION_NAME, null);
        assertEquals(new File(expectedManagedDirectory, indexName), indexDir);
        assertTrue(indexDir.isDirectory());
    }
}

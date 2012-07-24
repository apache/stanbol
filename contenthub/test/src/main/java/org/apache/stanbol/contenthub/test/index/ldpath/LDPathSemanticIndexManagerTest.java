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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.apache.stanbol.commons.semanticindex.index.IndexManagementException;
import org.apache.stanbol.commons.semanticindex.index.SemanticIndex;
import org.apache.stanbol.commons.semanticindex.index.SemanticIndexManager;
import org.apache.stanbol.contenthub.index.ldpath.LDPathSemanticIndex;
import org.apache.stanbol.contenthub.index.ldpath.LDPathSemanticIndexManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(SlingAnnotationsTestRunner.class)
public class LDPathSemanticIndexManagerTest {
    private static Logger logger = LoggerFactory.getLogger(LDPathSemanticIndexManagerTest.class);

    private final static String INDEX_METADATA_FOLDER_PATH = "LDPathSemanticIndexMetadata";

    @TestReference
    private LDPathSemanticIndexManager ldPathSemanticIndexManager;

    @TestReference
    private SemanticIndexManager semanticIndexManager;

    @TestReference
    private BundleContext bundleContext;

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
    public void bundleContextTest() {
        assertNotNull("Expecting BundleContext to be injected by Sling test runner", bundleContext);
    }

    @Test
    public void testCreateIndexWithoutName() {
        Properties indexMetadata = new Properties();
        indexMetadata
                .put(LDPathSemanticIndex.PROP_LD_PATH_PROGRAM,
                    "@prefix dbp-ont : <http://dbpedia.org/ontology/>; city = dbp-ont:city / rdfs:label :: xsd:string; ");
        try {
            ldPathSemanticIndexManager.createIndex(indexMetadata);
            assertTrue("An Index cannot be created without name", false);
        } catch (IndexManagementException e) {

        }
    }

    @Test
    public void testCreateIndexWithoutProgram() {
        Properties indexMetadata = new Properties();
        indexMetadata.put(SemanticIndex.PROP_NAME, "test_index_name");
        try {
            ldPathSemanticIndexManager.createIndex(indexMetadata);
            assertTrue("An Index cannot be created without program", false);
        } catch (IndexManagementException e) {

        }
    }

    @Test
    public void testCreateRemoveIndex() throws IndexManagementException, InterruptedException {
        String name = "test_index_name";
        String program = "@prefix dbp-ont : <http://dbpedia.org/ontology/>; city = dbp-ont:city / rdfs:label :: xsd:string;";
        Properties indexMetadata = new Properties();
        indexMetadata.put(SemanticIndex.PROP_NAME, name);
        indexMetadata.put(LDPathSemanticIndex.PROP_LD_PATH_PROGRAM, program);
        String pid = ldPathSemanticIndexManager.createIndex(indexMetadata);

        // create and retrieve LDPathSemanticIndex
        LDPathSemanticIndex semanticIndex = (LDPathSemanticIndex) semanticIndexManager.getIndex(name);
        int timeoutCount = 0;
        while (semanticIndex == null) {
            if (timeoutCount == 8) break;
            Thread.sleep(500);
            semanticIndex = (LDPathSemanticIndex) semanticIndexManager.getIndex(name);
            timeoutCount++;
        }
        assertNotNull("Failed to create LDPathSemanticIndex with name " + name, semanticIndex);

        // check IndexMetadata folder exists
        File indexMetadataDirectory = bundleContext
                .getServiceReference(LDPathSemanticIndexManager.class.getName()).getBundle()
                .getBundleContext().getDataFile(INDEX_METADATA_FOLDER_PATH);
        assertTrue("IndexMetadata Directory does not exist", indexMetadataDirectory.exists());

        // check IndexMetadata files of indexes before remove index
        String indexMetadataFilePath = indexMetadataDirectory.getAbsolutePath() + File.separator + pid
                                       + ".props";
        File file = new File(indexMetadataFilePath);
        assertTrue("IndexMetadata File cannot be found for pid: " + pid, file.exists());

        // remove LDPathSemanticIndex
        ldPathSemanticIndexManager.removeIndex(pid);

        semanticIndex = (LDPathSemanticIndex) semanticIndexManager.getIndex(name);

        assertNull(String.format("LDPathSemanticIndex with name %s cannot be removed", name), semanticIndex);
        if (semanticIndex == null) {
            assertFalse("SemanticIndex is removed, but still appears configured.",
                ldPathSemanticIndexManager.isConfigured(pid));
        }

        // check IndexMetadata files of indexes after remove index
        indexMetadataFilePath = indexMetadataDirectory.getAbsolutePath() + File.separator + pid + ".props";
        file = new File(indexMetadataFilePath);
        assertFalse("IndexMetadata File cannot be removed for pid: " + pid, file.exists());

    }

    @Test
    public void testIndexMetaData() throws IndexManagementException, InterruptedException {

        String name1 = "test_index_name1";
        String name2 = "test_index_name2";
        String program = "@prefix dbp-ont : <http://dbpedia.org/ontology/>; city = dbp-ont:city / rdfs:label :: xsd:string;";

        Properties indexMetadata = new Properties();
        indexMetadata.put(SemanticIndex.PROP_NAME, name1);
        indexMetadata.put(LDPathSemanticIndex.PROP_LD_PATH_PROGRAM, program);
        String pid1 = ldPathSemanticIndexManager.createIndex(indexMetadata);
        String pid2 = ldPathSemanticIndexManager.createIndex(name2, "Test Index Description", program);

        // create and retrieve LDPathSemanticIndex
        LDPathSemanticIndex semanticIndex1 = (LDPathSemanticIndex) semanticIndexManager.getIndex(name1);
        LDPathSemanticIndex semanticIndex2 = (LDPathSemanticIndex) semanticIndexManager.getIndex(name2);
        int timeoutCount = 0;
        while (semanticIndex1 == null || semanticIndex2 == null) {
            if (timeoutCount == 8) break;
            Thread.sleep(500);
            semanticIndex1 = (LDPathSemanticIndex) semanticIndexManager.getIndex(name1);
            semanticIndex2 = (LDPathSemanticIndex) semanticIndexManager.getIndex(name2);
            timeoutCount++;
        }

        File indexMetadataDirectory = bundleContext
                .getServiceReference(LDPathSemanticIndexManager.class.getName()).getBundle()
                .getBundleContext().getDataFile(INDEX_METADATA_FOLDER_PATH);

        Map<String,Properties> indexMetadataMap = new HashMap<String,Properties>();
        // load index metadata to memory
        File[] metadataList = indexMetadataDirectory.listFiles();
        for (File configFile : metadataList) {
            String pid = configFile.getName().substring(0, configFile.getName().lastIndexOf('.'));
            Properties props = new Properties();
            InputStream is = null;
            try {
                is = new FileInputStream(configFile);
                props.load(is);
                indexMetadataMap.put(pid, props);
                logger.info("Index metadata has been loaded from the location: {}",
                    configFile.getAbsolutePath());
            } catch (FileNotFoundException e) {
                logger.error("IndexMetadata file cannot be found");
                throw new IndexManagementException("IndexMetadata file cannot be found");
            } catch (IOException e) {
                logger.error("Failed to read from input stream");
                throw new IndexManagementException("Failed to read from input stream");
            } finally {
                IOUtils.closeQuietly(is);
            }
        }

        // test getAllIndexMetadata
        Map<String,Properties> allIndexMetadata = ldPathSemanticIndexManager.getAllIndexMetadata();
        assertTrue("getAllIndexMetadata() cannot return the expected value",
            allIndexMetadata.equals(indexMetadataMap));

        // check IndexMetadata map entries
        assertTrue(String.format("IndexMetadata for %s is not match with the expected value", pid1),
            ldPathSemanticIndexManager.getIndexMetadata(pid1).equals(indexMetadataMap.get(pid1)));
        assertTrue(String.format("IndexMetadata for %s is not match with the expected value", pid1),
            ldPathSemanticIndexManager.getIndexMetadata(pid2).equals(indexMetadataMap.get(pid2)));

        // update indexMetadataMap
        indexMetadataMap.get(pid1).put("test field", "test value");
        ldPathSemanticIndexManager.updateIndexMetadata(pid1, indexMetadataMap.get(pid1));
        assertTrue("IndexMetadata cannot be updated properly",
            ldPathSemanticIndexManager.getIndexMetadata(pid1).equals(indexMetadataMap.get(pid1)));

        // remove LDPathSemanticIndex
        ldPathSemanticIndexManager.removeIndex(pid1);
        ldPathSemanticIndexManager.removeIndex(pid2);
    }
}

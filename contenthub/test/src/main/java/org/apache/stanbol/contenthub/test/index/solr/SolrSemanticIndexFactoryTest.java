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
package org.apache.stanbol.contenthub.test.index.solr;

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
import org.apache.stanbol.contenthub.index.solr.SolrSemanticIndex;
import org.apache.stanbol.contenthub.index.solr.SolrSemanticIndexFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(SlingAnnotationsTestRunner.class)
public class SolrSemanticIndexFactoryTest {
    private static Logger logger = LoggerFactory.getLogger(SolrSemanticIndexFactoryTest.class);

    @TestReference
    private SolrSemanticIndexFactory solrSemanticIndexFactory;

    @TestReference
    private SemanticIndexManager semanticIndexManager;

    @TestReference
    private BundleContext bundleContext;

    @Test
    public void solrSemanticIndexFactoryTest() {
        assertNotNull("Expecting SolrSemanticIndexFactory to be injected by Sling test runner",
            solrSemanticIndexFactory);
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
                .put(SolrSemanticIndex.PROP_LD_PATH_PROGRAM,
                    "@prefix dbp-ont : <http://dbpedia.org/ontology/>; city = dbp-ont:city / rdfs:label :: xsd:string; ");
        try {
            solrSemanticIndexFactory.createIndex(indexMetadata);
            assertTrue("An Index cannot be created without name", false);
        } catch (IndexManagementException e) {

        }
    }

    @Test
    public void testCreateIndexWithoutProgram() {
        Properties indexMetadata = new Properties();
        indexMetadata.put(SemanticIndex.PROP_NAME, "test_index_name");
        try {
            solrSemanticIndexFactory.createIndex(indexMetadata);
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
        indexMetadata.put(SolrSemanticIndex.PROP_LD_PATH_PROGRAM, program);
        String pid = solrSemanticIndexFactory.createIndex(indexMetadata);

        // retrieve SolrSemanticIndex
        SolrSemanticIndex semanticIndex;
        String indexMetadataFilePath;
        File indexMetadataDirectory;
        File file;
        try {
            semanticIndex = (SolrSemanticIndex) semanticIndexManager.getIndex(name);
            int timeoutCount = 0;
            while (semanticIndex == null) {
                if (timeoutCount == 8) break;
                Thread.sleep(500);
                semanticIndex = (SolrSemanticIndex) semanticIndexManager.getIndex(name);
                timeoutCount++;
            }
            assertNotNull("Failed to create SolrSemanticIndex with name " + name, semanticIndex);

            // check IndexMetadata folder exists
            indexMetadataDirectory = bundleContext
                    .getServiceReference(SolrSemanticIndexFactory.class.getName()).getBundle()
                    .getBundleContext().getDataFile(SolrSemanticIndexFactory.class.getName());
            assertTrue("IndexMetadata Directory does not exist", indexMetadataDirectory.exists());

            // check IndexMetadata files of indexes before remove index
            indexMetadataFilePath = indexMetadataDirectory.getAbsolutePath() + File.separator + pid
                                    + ".props";
            file = new File(indexMetadataFilePath);
            assertTrue("IndexMetadata File cannot be found for pid: " + pid, file.exists());

        } finally {
            // remove SolrSemanticIndex
            solrSemanticIndexFactory.removeIndex(pid);
        }

        // wait some time to let OSGi remove the configuration
        int timeoutCount = 0;
        semanticIndex = (SolrSemanticIndex) semanticIndexManager.getIndex(name);
        while (semanticIndex != null) {
            if (timeoutCount == 8) break;
            Thread.sleep(500);
            semanticIndex = (SolrSemanticIndex) semanticIndexManager.getIndex(name);
            timeoutCount++;
        }

        assertNull(String.format("SolrSemanticIndex with name %s cannot be removed", name), semanticIndex);
        if (semanticIndex == null) {
            assertFalse("SemanticIndex is removed, but still appears configured.", solrSemanticIndexFactory
                    .getSemanticIndexMetadataManager().isConfigured(pid));
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
        indexMetadata.put(SolrSemanticIndex.PROP_LD_PATH_PROGRAM, program);
        String pid1 = solrSemanticIndexFactory.createIndex(indexMetadata);
        String pid2 = solrSemanticIndexFactory.createIndex(name2, "Test Index Description", program);

        // create and retrieve SolrSemanticIndex
        SolrSemanticIndex semanticIndex1 = (SolrSemanticIndex) semanticIndexManager.getIndex(name1);
        SolrSemanticIndex semanticIndex2 = (SolrSemanticIndex) semanticIndexManager.getIndex(name2);
        int timeoutCount = 0;
        while (semanticIndex1 == null || semanticIndex2 == null) {
            if (timeoutCount == 8) break;
            Thread.sleep(500);
            semanticIndex1 = (SolrSemanticIndex) semanticIndexManager.getIndex(name1);
            semanticIndex2 = (SolrSemanticIndex) semanticIndexManager.getIndex(name2);
            timeoutCount++;
        }

        File indexMetadataDirectory = bundleContext
                .getServiceReference(SolrSemanticIndexFactory.class.getName()).getBundle()
                .getBundleContext().getDataFile(SolrSemanticIndexFactory.class.getName());

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
        Map<String,Properties> allIndexMetadata = solrSemanticIndexFactory
                .getSemanticIndexMetadataManager().getAllIndexMetadata();
        assertTrue("getAllIndexMetadata() cannot return the expected value",
            allIndexMetadata.equals(indexMetadataMap));

        // check IndexMetadata map entries
        assertTrue(
            String.format("IndexMetadata for %s is not match with the expected value", pid1),
            solrSemanticIndexFactory.getSemanticIndexMetadataManager().getIndexMetadata(pid1)
                    .equals(indexMetadataMap.get(pid1)));
        assertTrue(
            String.format("IndexMetadata for %s is not match with the expected value", pid1),
            solrSemanticIndexFactory.getSemanticIndexMetadataManager().getIndexMetadata(pid2)
                    .equals(indexMetadataMap.get(pid2)));

        // update indexMetadataMap
        indexMetadataMap.get(pid1).put("test field", "test value");
        solrSemanticIndexFactory.getSemanticIndexMetadataManager().updateIndexMetadata(pid1,
            indexMetadataMap.get(pid1));
        assertTrue("IndexMetadata cannot be updated properly", solrSemanticIndexFactory
                .getSemanticIndexMetadataManager().getIndexMetadata(pid1).equals(indexMetadataMap.get(pid1)));

        // remove SolrSemanticIndex
        solrSemanticIndexFactory.removeIndex(pid1);
        solrSemanticIndexFactory.removeIndex(pid2);
    }
}

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
package org.apache.stanbol.contenthub.test.store.file;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.contenthub.servicesapi.store.Store;
import org.apache.stanbol.contenthub.servicesapi.store.StoreException;
import org.apache.stanbol.contenthub.store.file.FileStore;
import org.apache.stanbol.contenthub.store.file.FileStoreDBManager;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.codehaus.jettison.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(SlingAnnotationsTestRunner.class)
public class FileStoreTest {
    private static final Logger log = LoggerFactory.getLogger(FileStoreTest.class);

    @TestReference(name = "FileStore")
    private Store<ContentItem> store;

    @TestReference()
    private BundleContext bundleContext;

    @TestReference
    private ContentItemFactory contentItemFactory;

    @TestReference
    private FileStoreDBManager dbManager;

    @Test
    public void fileStoreTest() {
        assertNotNull("Expecting FileStore to be injected by Sling test runner", store);
        assertTrue("Expection FileStore implementation of Store interface", store instanceof FileStore);
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
    public void dbManagerTest() {
        assertNotNull("Expecting FileStoreDBManager to be injected by Sling test runner", dbManager);
    }

    @Test
    public void storeFolderTest() {
        String fileStoreFolder = bundleContext.getProperty("sling.home") + "/"
                                 + FileStore.FILE_STORE_FOLDER_PATH + "/" + FileStore.FILE_STORE_NAME;
        File storeFolder = new File(fileStoreFolder);
        assertTrue("No store folder exists", storeFolder.exists());
    }

    @Test
    public void putTest() throws IOException, StoreException, JSONException {
        String id = null;
        try {
            StringSource stringSource = new StringSource("I love Paris.");
            ContentItem ci = contentItemFactory.createContentItem(stringSource);
            id = store.put(ci);

            // check zip file
            String fileStoreFolder = bundleContext.getProperty("sling.home") + "/"
                                     + FileStore.FILE_STORE_FOLDER_PATH + "/" + FileStore.FILE_STORE_NAME;
            File storeFolder = new File(fileStoreFolder);
            String[] fileNames = storeFolder.list();
            boolean zipExists = false;
            for (String fileName : fileNames) {
                if (fileName.equals(encodeId(id) + ".zip")) {
                    zipExists = true;
                    break;
                }
            }
            assertTrue("Failed to find content item in store folder", zipExists);

            // check content of zip file
            ZipFile zipFile = new ZipFile(new File(storeFolder.getPath() + "/" + encodeId(id) + ".zip"));
            ZipEntry zipEntry = zipFile.getEntry("metadata");
            assertTrue("Zip file does not contain the metadata entry", zipEntry != null);
            zipEntry = zipFile.getEntry("org.apache.stanbol.contenthub.htmlmetadata");
            assertTrue("Zip file does not contain the html metadata entry", zipEntry != null);
            zipEntry = zipFile.getEntry(encodeId(id) + "_main");
            assertTrue("Zip file does not contain the metadata entry", zipEntry != null);
            zipEntry = zipFile.getEntry("header");
            assertTrue("Zip file does not contain the header entry", zipEntry != null);

        } finally {
            if (id != null) {
                clearTestContentItem(id);
            }
        }
    }

    @Test
    public void getTest() throws IOException, StoreException {
        String id = null;
        try {
            // put a content item
            StringSource stringSource = new StringSource("I love Paris.");
            ContentItem ci = contentItemFactory.createContentItem(stringSource);
            ci.addPart(new UriRef("dummypart"), new IndexedMGraph());
            id = store.put(ci);

            // check metadata
            ci = store.get(id);
            assertTrue("Failed to find metadata of retrieved content item", ci.getMetadata() != null);

            // check main blob
            assertTrue("Failed to find main blob of retrieved content item",
                ci.getPart(new UriRef(id + "_main"), Blob.class) != null);

            // check additional part
            assertTrue("Failed to find additional part of retrieved content item",
                ci.getPart(new UriRef("dummypart"), MGraph.class) != null);

        } finally {
            if (id != null) {
                clearTestContentItem(id);
            }
        }
    }

    private void clearTestContentItem(String id) throws StoreException {
        // delete the file
        String fileStoreFolder = bundleContext.getProperty("sling.home") + "/"
                                 + FileStore.FILE_STORE_FOLDER_PATH + "/" + FileStore.FILE_STORE_NAME;
        File f = new File(fileStoreFolder + "/" + encodeId(id) + ".zip");
        f.delete();

        // delete the database records
        String query = "DELETE FROM content_item_revisions WHERE id = ?";
        Connection connection = dbManager.getConnection();
        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(query);
            ps.setString(1, id);
            int result = ps.executeUpdate();
            if (result != 1) {
                log.warn(
                    "Wrong number of updated records while removing the test record. Updated record number: {}",
                    result);
            }
        } catch (SQLException e) {
            log.warn("Failed to remove the test record", e);
        } finally {
            dbManager.closeStatement(ps);
        }
        try {
            query = "DELETE FROM recently_enhanced_content_items WHERE id = ?";
            ps = connection.prepareStatement(query);
            ps.setString(1, id);
            int result = ps.executeUpdate();
            if (result != 1) {
                log.warn(
                    "Wrong number of updated records while removing the test record. Updated record number: {}",
                    result);
            }
        } catch (SQLException e) {
            log.warn("Failed to remove the test record", e);
        } finally {
            dbManager.closeStatement(ps);
            dbManager.closeConnection(connection);
        }
    }

    private String encodeId(String id) throws StoreException {
        try {
            return URLEncoder.encode(id, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("Failed to encode id. {}", id, e);
            throw new StoreException("Failed to encode id: " + id, e);
        }
    }
}

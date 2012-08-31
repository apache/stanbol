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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.apache.stanbol.commons.semanticindex.index.IndexException;
import org.apache.stanbol.commons.semanticindex.index.IndexManagementException;
import org.apache.stanbol.commons.semanticindex.store.Store;
import org.apache.stanbol.commons.semanticindex.store.StoreException;
import org.apache.stanbol.contenthub.revisionmanager.RevisionManager;
import org.apache.stanbol.contenthub.revisionmanager.StoreDBManager;
import org.apache.stanbol.contenthub.store.file.FileStore;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.codehaus.jettison.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(SlingAnnotationsTestRunner.class)
public class FileStoreTest {
    private static final Logger log = LoggerFactory.getLogger(FileStoreTest.class);

    @TestReference()
    private BundleContext bundleContext;

    @TestReference
    private ContentItemFactory contentItemFactory;

    @TestReference
    private StoreDBManager dbManager;

    @TestReference
    private RevisionManager revisionManager;

    private Store<ContentItem> store;

    private String fileStoreFolder;

    @Before
    public void before() throws IndexManagementException,
                        IndexException,
                        InterruptedException,
                        IOException,
                        NoSuchFieldException,
                        SecurityException,
                        IllegalArgumentException,
                        IllegalAccessException {
        if (store == null) {
            if (bundleContext != null) {
                store = getContenthubStore();

                // get store folder
                Field field = store.getClass().getDeclaredField("storeFolder");
                field.setAccessible(true);
                fileStoreFolder = ((File) field.get(store)).getPath();

                if (store == null) {
                    throw new IllegalStateException("Null Store");
                }
            } else {
                throw new IllegalStateException("Null bundle context");
            }
        }
    }

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
        File storeFolder = new File(fileStoreFolder);
        assertTrue("No store folder exists", storeFolder.exists());
    }

    @Test
    public void removeTest() throws SQLException, StoreException, IOException {
        // create zip file
        String id = "filestoretestid";
        File file = putTemplateZipFile(id);

        store.remove(id);

        // check zip file is exist
        file = new File(fileStoreFolder + "/" + encodeId(id) + ".zip");
        assertFalse(String.format("Zip file is not removed after removing contentitem with id: %s", id),
            file.exists());

        // check revision is updated, then delete it
        String selectRevision = "SELECT id,revision FROM " + revisionManager.getStoreID(store)
                                + " content_item_revision WHERE id = '%s'";
        Connection con = dbManager.getConnection();
        // Create a Statement for scrollable ResultSet
        Statement stmt = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
        ResultSet rs = stmt.executeQuery(String.format(selectRevision, id));

        if (rs.next()) {
            rs.deleteRow();
        } else {
            assertTrue(
                String.format("Revision table is not updated after removing contentitem with id: %s", id),
                false);
        }

        dbManager.closeResultSet(rs);
        dbManager.closeStatement(stmt);
        dbManager.closeConnection(con);
    }

    @Test
    public void removeUrisTest() throws StoreException, SQLException, IOException {
        // create zip files
        String id1 = "filestoretestid01";
        String id2 = "filestoretestid02";
        File file1 = putTemplateZipFile(id1);
        File file2 = putTemplateZipFile(id2);

        store.remove(Arrays.asList(id1, id2));

        // check zip files are exist
        file1 = new File(fileStoreFolder + "/" + encodeId(id1) + ".zip");
        assertFalse(String.format("Zip file is not removed after removing contentitem with id: %s", id1),
            file1.exists());
        file2 = new File(fileStoreFolder + "/" + encodeId(id2) + ".zip");
        assertFalse(String.format("Zip file is not removed after removing contentitem with id: %s", id2),
            file2.exists());

        // check revisions are updated, then delete them
        String selectRevision = "SELECT id,revision FROM " + revisionManager.getStoreID(store)
                                + " content_item_revision WHERE id = '%s' OR id = '%s'";
        Connection con = dbManager.getConnection();
        // Create a Statement for scrollable ResultSet
        Statement stmt = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
        ResultSet rs = stmt.executeQuery(String.format(selectRevision, id1, id2));

        if (rs.next()) {
            rs.deleteRow();
        } else {
            assertTrue(
                String.format("Revision table is not updated after removing contentitem with id: %s", id1),
                false);
        }

        if (rs.next()) {
            rs.deleteRow();
        } else {
            assertTrue(
                String.format("Revision table is not updated after removing contentitem with id: %s", id2),
                false);
        }

        dbManager.closeResultSet(rs);
        dbManager.closeStatement(stmt);
        dbManager.closeConnection(con);
    }

    @Test
    public void putTest() throws IOException, StoreException, JSONException {
        String id = null;
        ZipFile zipFile = null;
        try {
            StringSource stringSource = new StringSource("I love Paris.");
            ContentItem ci = contentItemFactory.createContentItem(stringSource);
            id = store.put(ci);

            // check zip file
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
            zipFile = new ZipFile(new File(storeFolder.getPath() + "/" + encodeId(id) + ".zip"));
            ZipEntry zipEntry = zipFile.getEntry("metadata");
            assertTrue("Zip file does not contain the metadata entry", zipEntry != null);
            zipEntry = zipFile.getEntry("org.apache.stanbol.contenthub.htmlmetadata");
            assertTrue("Zip file does not contain the html metadata entry", zipEntry != null);
            zipEntry = zipFile.getEntry(encodeId(id) + "_main");
            assertTrue("Zip file does not contain the metadata entry", zipEntry != null);
            zipEntry = zipFile.getEntry("header");
            assertTrue("Zip file does not contain the header entry", zipEntry != null);

        } finally {
            if (zipFile != null) {
                zipFile.close();
            }
            if (id != null) {
                clearTestContentItem(id);
            }
        }
    }

    @Test
    public void putItemsTest() throws IOException, StoreException {
        List<ContentItem> cis = null;
        List<String> ids = null;
        ZipFile zipFile = null;
        try {
            // create contentitems
            StringSource stringSource1 = new StringSource("I love Paris.");
            StringSource stringSource2 = new StringSource("I love Istanbul.");
            ContentItem ci1 = contentItemFactory.createContentItem(stringSource1);
            ContentItem ci2 = contentItemFactory.createContentItem(stringSource2);

            String id1 = ci1.getUri().getUnicodeString();
            String id2 = ci2.getUri().getUnicodeString();
            cis = Arrays.asList(ci1, ci2);
            ids = Arrays.asList(id1, id2);

            store.put(cis);

            // check zip files
            File storeFolder = new File(fileStoreFolder);
            String[] fileNames = storeFolder.list();
            boolean zip1Exists = false;
            boolean zip2Exists = false;
            for (String fileName : fileNames) {
                if (fileName.equals(encodeId(id1) + ".zip")) {
                    zip1Exists = true;
                }
                if (fileName.equals(encodeId(id2) + ".zip")) {
                    zip2Exists = true;
                }
            }
            assertTrue("Failed to find content item in store folder", (zip1Exists && zip2Exists));

            for (String id : ids) {
                // check content of zip file
                zipFile = new ZipFile(new File(storeFolder.getPath() + "/" + encodeId(id) + ".zip"));
                ZipEntry zipEntry = zipFile.getEntry("metadata");
                assertTrue("Zip file does not contain the metadata entry", zipEntry != null);
                zipEntry = zipFile.getEntry("org.apache.stanbol.contenthub.htmlmetadata");
                assertTrue("Zip file does not contain the html metadata entry", zipEntry != null);
                zipEntry = zipFile.getEntry(encodeId(id) + "_main");
                assertTrue("Zip file does not contain the metadata entry", zipEntry != null);
                zipEntry = zipFile.getEntry("header");
                assertTrue("Zip file does not contain the header entry", zipEntry != null);
            }

        } finally {
            if (zipFile != null) {
                zipFile.close();
            }
            for (String id : ids) {
                if (id != null) {
                    clearTestContentItem(id);
                }
            }
        }
    }

    @Test
    public void getTest() throws IOException, StoreException {
        String id = null;
        try {
            // put a content item
            id = "file_store_get_test_id";
            putTemplateZipFile(id);

            // check metadata
            ContentItem ci = store.get(id);
            assertTrue("Failed to find metadata of retrieved content item", ci.getMetadata() != null);

            // check main blob
            assertTrue("Failed to find main blob of retrieved content item",
                ci.getPart(new UriRef(id + "_main"), Blob.class) != null);

            // check additional part
            assertTrue("Failed to find additional part of retrieved content item",
                ci.getPart(new UriRef("dummypart"), MGraph.class) != null);

        } finally {
            if (id != null) {
                // delete the file
                File f = new File(fileStoreFolder + "/" + encodeId(id) + ".zip");
                f.delete();
            }
        }
    }

    private File putTemplateZipFile(String id) throws IOException, StoreException {
        byte[] data = IOUtils.toByteArray(FileStoreTest.class.getResource(
            "/FileStoreTest/templateContentItem.zip").openStream());
        File file = new File(fileStoreFolder + "/" + encodeId(id) + ".zip");
        FileUtils.writeByteArrayToFile(file, data);
        return file;
    }

    private void clearTestContentItem(String id) throws StoreException {
        // delete the file
        File f = new File(fileStoreFolder + "/" + encodeId(id) + ".zip");
        f.delete();

        // delete the database records
        String query = String.format("DELETE FROM %s WHERE id = ?", revisionManager.getStoreID(store));
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

    @SuppressWarnings("unchecked")
    private Store<ContentItem> getContenthubStore() {
        Store<ContentItem> contentHubStore = null;
        try {
            ServiceReference[] stores = bundleContext.getServiceReferences(Store.class.getName(), null);
            for (ServiceReference serviceReference : stores) {
                Object store = bundleContext.getService(serviceReference);
                Type[] genericInterfaces = store.getClass().getGenericInterfaces();
                if (genericInterfaces.length == 1 && genericInterfaces[0] instanceof ParameterizedType) {
                    Type[] types = ((ParameterizedType) genericInterfaces[0]).getActualTypeArguments();
                    try {
                        @SuppressWarnings("unused")
                        Class<ContentItem> contentItemClass = (Class<ContentItem>) types[0];
                        if (((Store<ContentItem>) store).getName().equals("contenthubFileStore")) {
                            contentHubStore = (Store<ContentItem>) store;
                        }
                    } catch (ClassCastException e) {
                        // ignore
                    }
                }
            }
        } catch (InvalidSyntaxException e) {
            // ignore as there is no filter
        }
        return contentHubStore;
    }
}

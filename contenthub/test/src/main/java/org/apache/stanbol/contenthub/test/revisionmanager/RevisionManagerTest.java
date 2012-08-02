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
package org.apache.stanbol.contenthub.test.revisionmanager;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.apache.stanbol.commons.semanticindex.index.IndexException;
import org.apache.stanbol.commons.semanticindex.index.IndexManagementException;
import org.apache.stanbol.commons.semanticindex.store.ChangeSet;
import org.apache.stanbol.commons.semanticindex.store.Store;
import org.apache.stanbol.commons.semanticindex.store.StoreException;
import org.apache.stanbol.contenthub.revisionmanager.RevisionManager;
import org.apache.stanbol.contenthub.revisionmanager.StoreDBManager;
import org.apache.stanbol.contenthub.store.file.FileStore;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(SlingAnnotationsTestRunner.class)
public class RevisionManagerTest {
    private static Logger log = LoggerFactory.getLogger(RevisionManagerTest.class);

    @TestReference
    private RevisionManager revisionManager;

    @TestReference
    private StoreDBManager dbManager;

    @TestReference
    private BundleContext bundleContext;

    private Store<?> store;

    @Before
    public void before() throws IndexManagementException, IndexException, InterruptedException, IOException {
        if (store == null) {
            if (bundleContext != null) {
                store = getContenthubStore();
                if (store == null) {
                    throw new IllegalStateException("Null Store");
                }
            } else {
                throw new IllegalStateException("Null bundle context");
            }
        }
    }

    @Test
    public void revisionManagerTest() {
        assertNotNull("Expecting RevisionManager to be injected by Sling test runner", revisionManager);
    }

    @Test
    public void dbManagerTest() {
        assertNotNull("Expecting FileStoreDBManager to be injected by Sling test runner", dbManager);
    }

    @Test
    public void bundleContextTest() {
        assertNotNull("Expecting BundleContext to be injected by Sling test runner", bundleContext);
    }

    @Test
    public void updateRevisionTest() throws StoreException, SQLException {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        // do the update
        String contentItemID = "contenthub_test_content_item_id";
        long newRevision = revisionManager.updateRevision(store, contentItemID);
        boolean recordExist = false;
        long revisionNumber = Long.MAX_VALUE;
        String query;
        try {
            query = String.format("SELECT id, revision FROM %s WHERE id = ?",
                revisionManager.getStoreID(store));

            // check the update
            con = dbManager.getConnection();
            ps = con.prepareStatement(query);
            ps.setString(1, contentItemID);
            rs = ps.executeQuery();
            if (rs.next()) {
                recordExist = true;
                revisionNumber = rs.getLong(2);
            }
            dbManager.closeStatement(ps);

            assertTrue("failed to obtain content item revision", recordExist);
            assertTrue("wrong revision number", (revisionNumber == newRevision));

        } catch (SQLException e) {
            log.warn("Failed to remove the test record", e);
        } finally {
            // clear the update
            try {
                query = String.format("DELETE FROM %s WHERE id = ?", revisionManager.getStoreID(store));
                ps = con.prepareStatement(query);
                ps.setString(1, contentItemID);
                int result = ps.executeUpdate();
                if (result != 1) {
                    log.warn(
                        "Wrong number of updated records while removing the test record. Updated record number: {}",
                        result);
                }
            } finally {
                dbManager.closeResultSet(rs);
                dbManager.closeStatement(ps);
                dbManager.closeConnection(con);
            }
        }
    }

    /**
     * IMPORTANT NOTE: At time of executing the test below, there MUST be no other requests updating the
     * revision table, otherwise the test will fail.
     * 
     * @throws InterruptedException
     * @throws SQLException
     */
    @Test
    public void iterativeChangesTest() throws StoreException, InterruptedException, SQLException {
        log.warn("DO NOT UPDATE THE STORE: {} DURING EXECUTION OF THIS TEST", store.getName());
        Connection con = dbManager.getConnection();
        String contentItemID = "contenthub_test_content_item_id";
        PreparedStatement ps = null;
        int insertCount = 0;
        try {
            // do the update
            String query = "INSERT INTO " + revisionManager.getStoreID(store)
                           + " (id, revision) VALUES (?,?)";
            long startRevision = System.currentTimeMillis();
            long revision = System.currentTimeMillis();
            for (int i = 0; i < 5; i++) {
                // to ensure different revisions
                ps = con.prepareStatement(query);
                ps.setString(1, contentItemID + i);
                ps.setLong(2, ++revision);
                ps.executeUpdate();
                ps.clearParameters();
                insertCount++;
            }

            // check changes
            ChangeSet<?> changeSet = revisionManager.getChanges(store, startRevision, 3);
            Iterator<String> changedItems = changeSet.iterator();
            int itemCount = 0;
            while (changedItems.hasNext()) {
                changedItems.next();
                itemCount++;
            }
            assertTrue("Wrong number of changed items", (itemCount == 3));
            for (int i = 0; i < 3; i++) {
                changedItems = changeSet.iterator();
                itemCount = 0;
                while (changedItems.hasNext()) {
                    String changedItem = changedItems.next();
                    if (changedItem.equals(contentItemID + i)) break;
                    itemCount++;
                }
                assertTrue("Changes does not include correct URIs", itemCount < 3);
            }
            assertTrue("Changes does not include correct fromRevision value",
                (changeSet.fromRevision() == revision - 4));
            assertTrue("Changes does not include correct toRevision value",
                (changeSet.toRevision() == revision - 2));

            changeSet = revisionManager.getChanges(store, revision - 2, 3);
            itemCount = 0;
            changedItems = changeSet.iterator();
            while (changedItems.hasNext()) {
                changedItems.next();
                itemCount++;
            }
            assertTrue("Wrong number of changed items", (itemCount == 2));
            for (int i = 0; i < 2; i++) {
                changedItems = changeSet.iterator();
                itemCount = 0;
                while (changedItems.hasNext()) {
                    String changedItem = changedItems.next();
                    if (changedItem.equals(contentItemID + (i + 3))) break;
                    itemCount++;
                }
                assertTrue("Changes does not include correct URIs", itemCount < 2);
            }

            assertTrue("Changes does not include correct fromRevision value",
                (changeSet.fromRevision() == revision - 1));
            assertTrue("Changes does not include correct toRevision value",
                (changeSet.toRevision() == revision));
        } finally {
            // clear test changes
            String query = String.format(
                "DELETE FROM %s WHERE id = ? OR id = ? OR id = ? OR id = ? OR id = ?",
                revisionManager.getStoreID(store));
            try {
                ps = con.prepareStatement(query);
                for (int i = 0; i < insertCount; i++) {
                    ps.setString(i + 1, contentItemID + i);
                }
                int result = ps.executeUpdate();
                if (result != 5) {
                    log.warn(
                        "Wrong number of updated records while removing the test record. Updated record number: {}",
                        result);
                }
            } catch (SQLException e) {
                log.warn("Failed to remove the test record", e);
            } finally {
                dbManager.closeStatement(ps);
                dbManager.closeConnection(con);
            }
        }
    }

    @Test
    public void batchSizeDoesNotFitToRevisionTest() throws StoreException, SQLException {
        log.warn("DO NOT UPDATE THE STORE: {} DURING EXECUTION OF THIS TEST", store.getName());
        Connection con = dbManager.getConnection();
        String contentItemID = "contenthub_test_content_item_id";
        PreparedStatement ps = null;
        int insertCount = 0;
        try {
            // do the update
            String query = "INSERT INTO " + revisionManager.getStoreID(store)
                           + " (id, revision) VALUES (?,?)";
            long revision = System.currentTimeMillis();
            for (int i = 0; i < 2; i++) {
                ps = con.prepareStatement(query);
                ps.setString(1, contentItemID + i);
                ps.setLong(2, revision + 1);
                ps.executeUpdate();
                ps.clearParameters();
                insertCount++;
            }

            // get changes
            ChangeSet<?> changeSet = revisionManager.getChanges(store, revision, 1);
            int itemCount = 0;
            Iterator<String> changedItems = changeSet.iterator();
            while (changedItems.hasNext()) {
                changedItems.next();
                itemCount++;
            }
            assertTrue("Wrong number of changed items", (itemCount == 2));
            for (int i = 0; i < 2; i++) {
                changedItems = changeSet.iterator();
                itemCount = 0;
                while (changedItems.hasNext()) {
                    String changedItem = changedItems.next();
                    if (changedItem.equals(contentItemID + i)) break;
                    itemCount++;
                }
                assertTrue("Changes does not include correct URIs", itemCount < 2);
            }
        } finally {
            // clear test changes
            String query = String.format("DELETE FROM %s WHERE id = ? OR id = ?",
                revisionManager.getStoreID(store));
            try {
                ps = con.prepareStatement(query);
                for (int i = 0; i < insertCount; i++) {
                    ps.setString(i + 1, contentItemID + i);
                }
                int result = ps.executeUpdate();
                if (result != 2) {
                    log.warn(
                        "Wrong number of updated records while removing the test record. Updated record number: {}",
                        result);
                }
            } catch (SQLException e) {
                log.warn("Failed to remove the test record", e);
            } finally {
                dbManager.closeStatement(ps);
                dbManager.closeConnection(con);
            }
        }
    }

    @Test
    public void emptyChangesTest() throws StoreException {
        long revision = System.currentTimeMillis();
        ChangeSet<?> changeSet = revisionManager.getChanges(store, revision, 1);
        assertTrue("There must be no changes", !changeSet.iterator().hasNext());
    }

    @Test
    public void getStoreIDTest() {
        assertTrue("Store ID must be same with the name of the Store", revisionManager.getStoreID(store)
                .equals(store.getName()));
    }

    @Test
    public void initializeRevisionTablesTest() throws StoreException, SQLException {
        FileStore fileStore = new FileStore("revisionManagerTestStore");
        Connection con = null;
        PreparedStatement ps = null;
        String tableName = revisionManager.getStoreID(fileStore);
        try {
            revisionManager.initializeRevisionTables(fileStore);
            // check table
            assertTrue(String.format("There is no table having name: %s", tableName),
                dbManager.existsTable(tableName));

            // check epoch table entry
            con = dbManager.getConnection();
            ps = null;
            ResultSet rs = null;
            boolean recordExists = false;
            try {
                ps = con.prepareStatement("SELECT epoch FROM " + StoreDBManager.EPOCH_TABLE_NAME
                                          + " WHERE tableName = ?");
                ps.setString(1, tableName);
                rs = ps.executeQuery();
                if (rs.next()) {
                    recordExists = true;
                }

                assertTrue(String.format("There is no entry for tableName: %s in epochTable", tableName),
                    recordExists);
            } finally {
                dbManager.closeResultSet(rs);
                dbManager.closeStatement(ps);
            }
        } finally {
            // clear test data
            Statement stmt = null;
            try {
                // first remove the the table
                stmt = con.createStatement();
                stmt.executeUpdate("DROP TABLE " + tableName);

            } finally {
                dbManager.closeStatement(stmt);
            }
            try {
                // delete the entry from epoch table
                ps = con.prepareStatement("DELETE FROM " + StoreDBManager.EPOCH_TABLE_NAME
                                          + " WHERE tableName = ?");
                ps.setString(1, tableName);
                ps.executeUpdate();

            } finally {
                dbManager.closeStatement(ps);
                dbManager.closeConnection(con);
            }
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

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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.apache.stanbol.commons.semanticindex.store.ChangeSet;
import org.apache.stanbol.commons.semanticindex.store.StoreException;
import org.apache.stanbol.commons.semanticindex.store.revisionmanager.RevisionManager;
import org.apache.stanbol.contenthub.revisionmanager.DerbyDBManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(SlingAnnotationsTestRunner.class)
public class DerbyRevisionManagerTest {
    private static Logger log = LoggerFactory.getLogger(DerbyRevisionManagerTest.class);

    @TestReference
    private RevisionManager revisionManager;

    @TestReference
    private DerbyDBManager dbManager;

    @TestReference
    private BundleContext bundleContext;

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
        String storeID = "updateRevisionTestStore";
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        // do the update
        String contentItemID = "contenthub_test_content_item_id";
        long newRevision;
        boolean recordExist = false;
        long revisionNumber = Long.MAX_VALUE;
        String query;
        try {
            revisionManager.registerStore(storeID);
            newRevision = revisionManager.updateRevision(storeID, contentItemID);
            query = String.format("SELECT id, revision FROM \"%s\" WHERE id = ?", storeID);

            // check the update
            con = dbManager.getConnection();
            ps = con.prepareStatement(query);
            ps.setString(1, contentItemID);
            rs = ps.executeQuery();
            if (rs.next()) {
                recordExist = true;
                revisionNumber = rs.getLong(2);
            }

            assertTrue("failed to obtain content item revision", recordExist);
            assertTrue("wrong revision number", (revisionNumber == newRevision));

        } finally {
            dbManager.closeResultSet(rs);
            dbManager.closeStatement(ps);
            clearDummyStoreResources(storeID, con);
        }
    }

    @Test
    public void iterativeChangesTest() throws StoreException, InterruptedException, SQLException {
        String storeID = "iterativeChangesTestStore";
        Connection con = null;
        String contentItemID = "contenthub_test_content_item_id";
        PreparedStatement ps = null;
        try {
            con = dbManager.getConnection();
            revisionManager.registerStore(storeID);

            // do the update
            String query = "INSERT INTO \"" + storeID + "\" (id, revision) VALUES (?,?)";
            long startRevision = System.currentTimeMillis();
            long revision = System.currentTimeMillis();
            for (int i = 0; i < 5; i++) {
                // to ensure different revisions
                ps = con.prepareStatement(query);
                ps.setString(1, contentItemID + i);
                ps.setLong(2, ++revision);
                ps.executeUpdate();
                ps.clearParameters();
            }

            // check changes
            ChangeSet changeSet = revisionManager.getChanges(storeID, startRevision, 3);
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

            changeSet = revisionManager.getChanges(storeID, revision - 2, 3);
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
            dbManager.closeStatement(ps);
            clearDummyStoreResources(storeID, con);
        }
    }

    @Test
    public void batchSizeDoesNotFitToRevisionTest() throws StoreException, SQLException {
        String storeID = "batchSizeDoesNotFitToRevisionTestStore";
        Connection con = null;
        String contentItemID = "contenthub_test_content_item_id";
        PreparedStatement ps = null;
        try {
            revisionManager.registerStore(storeID);
            con = dbManager.getConnection();
            // do the update
            String query = "INSERT INTO \"" + storeID + "\" (id, revision) VALUES (?,?)";
            long revision = System.currentTimeMillis();
            for (int i = 0; i < 2; i++) {
                ps = con.prepareStatement(query);
                ps.setString(1, contentItemID + i);
                ps.setLong(2, revision + 1);
                ps.executeUpdate();
                ps.clearParameters();
            }

            // get changes
            ChangeSet changeSet = revisionManager.getChanges(storeID, revision, 1);
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
            dbManager.closeStatement(ps);
            clearDummyStoreResources(storeID, con);
        }
    }

    @Test
    public void emptyChangesTest() throws StoreException {
        String storeID = "emptyChangesTestStore";
        try {
            revisionManager.registerStore(storeID);
            long revision = System.currentTimeMillis();
            ChangeSet changeSet = revisionManager.getChanges(storeID, revision, 1);
            assertTrue("There must be no changes", changeSet.iterator().hasNext() == false);
        } finally {
            clearDummyStoreResources(storeID, dbManager.getConnection());
        }
    }

    @Test
    public void initializeRevisionTablesTest() throws StoreException, SQLException {
        String storeID = "revisionManagerTestStore";
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String tableName = storeID;
        try {
            revisionManager.registerStore(storeID);
            // check table
            assertTrue(String.format("There is no table having name: %s", tableName),
                dbManager.existsTable(tableName));

            // check epoch table entry
            con = dbManager.getConnection();
            boolean recordExists = false;
            ps = con.prepareStatement("SELECT epoch FROM " + DerbyDBManager.EPOCH_TABLE_NAME
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
            clearDummyStoreResources(storeID, con);
        }
    }

    private void clearDummyStoreResources(String storeID, Connection con) {
        Statement stmt = null;
        PreparedStatement ps = null;
        String tableName = storeID;
        try {
            // first remove the the table
            stmt = con.createStatement();
            stmt.executeUpdate("DROP TABLE \"" + tableName + "\"");

            // delete the entry from epoch table
            ps = con.prepareStatement("DELETE FROM " + DerbyDBManager.EPOCH_TABLE_NAME
                                      + " WHERE tableName = ?");
            ps.setString(1, tableName);
            ps.executeUpdate();

        } catch (SQLException e) {
            log.error("Failed clear test resources for the table: {}", tableName);
        } finally {
            dbManager.closeStatement(stmt);
            dbManager.closeStatement(ps);
            dbManager.closeConnection(con);
        }
    }
}

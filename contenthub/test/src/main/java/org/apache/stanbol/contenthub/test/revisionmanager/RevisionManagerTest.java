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
import java.util.Iterator;

import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.apache.stanbol.commons.semanticindex.store.ChangeSet;
import org.apache.stanbol.commons.semanticindex.store.Store;
import org.apache.stanbol.commons.semanticindex.store.StoreException;
import org.apache.stanbol.contenthub.revisionmanager.RevisionManager;
import org.apache.stanbol.contenthub.revisionmanager.StoreDBManager;
import org.junit.Test;
import org.junit.runner.RunWith;
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
    private Store<?> store;

    @Test
    public void revisionManagerTest() {
        assertNotNull("Expecting revisionManager to be injected by Sling test runner", revisionManager);
    }

    @Test
    public void dbManagerTest() {
        assertNotNull("Expecting FileStoreDBManager to be injected by Sling test runner", dbManager);
    }

    @Test
    public void updateChangeTest() throws StoreException, SQLException {
        // do the update
        String contentItemID = "contenthub_test_content_item_id";
        revisionManager.updateRevision(store, contentItemID);

        // check the update
        String query = String.format("SELECT id, revision FROM %s WHERE id = ?",
            revisionManager.getStoreID(store));
        Connection con = dbManager.getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        boolean recordExist = false;
        long revisionNumber = Long.MAX_VALUE;
        try {
            ps = con.prepareStatement(query);
            ps.setString(1, contentItemID);
            rs = ps.executeQuery();
            if (rs.next()) {
                recordExist = true;
                revisionNumber = rs.getLong(2);
            }
        } finally {
            dbManager.closeResultSet(rs);
            dbManager.closeStatement(ps);
        }

        assertTrue("failed to obtain content item revision", recordExist);
        assertTrue("wrong revision number", (revisionNumber <= System.currentTimeMillis()));

        // clear the update
        query = String.format("DELETE FROM %s WHERE id = ?", revisionManager.getStoreID(store));
        ps = null;
        try {
            ps = con.prepareStatement(query);
            ps.setString(1, contentItemID);
            int result = ps.executeUpdate();
            if (result != 1) {
                log.warn(
                    "Wrong number of updated records while removing the test record. Updated record number: {}",
                    result);
            }
        } catch (SQLException e) {
            log.warn("Failed to remove the test record", e);
        } finally {
            dbManager.closeConnection(con);
            dbManager.closeStatement(ps);
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
        log.warn("DO NOT UPDATE THE STORE: {}DURING EXECUTION OF THIS TEST", store.getName());
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
}

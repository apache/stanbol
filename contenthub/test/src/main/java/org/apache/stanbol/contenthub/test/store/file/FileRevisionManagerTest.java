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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.apache.stanbol.commons.semanticindex.store.ChangeSet;
import org.apache.stanbol.commons.semanticindex.store.StoreException;
import org.apache.stanbol.contenthub.store.file.FileRevisionManager;
import org.apache.stanbol.contenthub.store.file.FileStoreDBManager;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(SlingAnnotationsTestRunner.class)
public class FileRevisionManagerTest {
    private static Logger log = LoggerFactory.getLogger(FileRevisionManagerTest.class);

    @TestReference
    FileRevisionManager fileRevisionManager;

    @TestReference
    FileStoreDBManager dbManager;

    @Test
    public void fileRevisionManagerTest() {
        assertNotNull("Expecting FileRevisionManager to be injected by Sling test runner",
            fileRevisionManager);
    }

    @Test
    public void dbManagerTest() {
        assertNotNull("Expecting FileStoreDBManager to be injected by Sling test runner", dbManager);
    }

    @Test
    public void updateChangeTest() throws StoreException, SQLException {
        // do the update
        String contentItemID = "contenthub_test_content_item_id";
        fileRevisionManager.updateRevision(contentItemID);

        // check the update
        String query = "SELECT id, revision FROM content_item_revisions WHERE id = ?";
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
        query = "DELETE FROM content_item_revisions WHERE id = ?";
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
        log.warn("DO NOT UPDATE REVISION MANAGER DURING EXECUTION OF THIS TEST");
        Connection con = dbManager.getConnection();
        String contentItemID = "contenthub_test_content_item_id";
        PreparedStatement ps = null;
        int insertCount = 0;
        try {
            // do the update
            String query = "INSERT INTO " + FileRevisionManager.REVISION_TABLE_NAME
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
            ChangeSet<ContentItem> changeSet = fileRevisionManager.getChanges(startRevision, 3);
            Set<String> changedItems = changeSet.changed();
            assertTrue("Wrong number of changed items", (changedItems.size() == 3));
            for (int i = 0; i < 3; i++) {
                assertTrue("Changes does not include correct URIs",
                    changedItems.contains(contentItemID + i));
            }
            assertTrue("Changes does not include correct fromRevision value",
                (changeSet.fromRevision() == revision - 4));
            assertTrue("Changes does not include correct toRevision value",
                (changeSet.toRevision() == revision - 2));

            changeSet = fileRevisionManager.getChanges(revision - 2, 3);
            changedItems = changeSet.changed();
            assertTrue("Wrong number of changed items", (changedItems.size() == 2));
            for (int i = 0; i < 2; i++) {
                assertTrue("Changes does not include correct URIs",
                    changedItems.contains(contentItemID + (i + 3)));
            }
            assertTrue("Changes does not include correct fromRevision value",
                (changeSet.fromRevision() == revision - 1));
            assertTrue("Changes does not include correct toRevision value",
                (changeSet.toRevision() == revision));
        } finally {
            // clear test changes
            String query = "DELETE FROM content_item_revisions WHERE id = ? OR id = ? OR id = ? OR id = ? OR id = ?";
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
        log.warn("DO NOT UPDATE REVISION MANAGER DURING EXECUTION OF THIS TEST");
        Connection con = dbManager.getConnection();
        String contentItemID = "contenthub_test_content_item_id";
        PreparedStatement ps = null;
        int insertCount = 0;
        try {
            // do the update
            String query = "INSERT INTO " + FileRevisionManager.REVISION_TABLE_NAME
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
            Set<String> changedItems = fileRevisionManager.getChanges(revision, 1).changed();
            assertTrue("Wrong number of changed items", (changedItems.size() == 2));
            for (int i = 0; i < 2; i++) {
                assertTrue("Changes does not include correct URIs",
                    changedItems.contains(contentItemID + i));
            }
        } finally {
            // clear test changes
            String query = "DELETE FROM content_item_revisions WHERE id = ? OR id = ?";
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
        ChangeSet<ContentItem> changeSet = fileRevisionManager.getChanges(revision, 1);
        assertTrue("There must be no changes", changeSet.changed().size() == 0);
        assertTrue("Wrong start version", changeSet.fromRevision() == -1);
        assertTrue("Wrong end version", changeSet.toRevision() == -1);
    }
}

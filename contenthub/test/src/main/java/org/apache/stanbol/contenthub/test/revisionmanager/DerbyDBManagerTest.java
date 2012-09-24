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
import java.util.ArrayList;
import java.util.List;

import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.apache.stanbol.commons.semanticindex.store.StoreException;
import org.apache.stanbol.commons.semanticindex.store.revisionmanager.RevisionBean;
import org.apache.stanbol.commons.semanticindex.store.revisionmanager.RevisionManagerException;
import org.apache.stanbol.contenthub.revisionmanager.DerbyDBManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;

@RunWith(SlingAnnotationsTestRunner.class)
public class DerbyDBManagerTest {
    @TestReference
    private DerbyDBManager dbManager;

    @TestReference
    private BundleContext bundleContext;

    Connection con = null;

    @Before
    public void before() throws RevisionManagerException {
        if (con == null) {
            con = dbManager.getConnection();
        }
    }

    @After
    public void after() {
        dbManager.closeConnection(con);
    }

    @Test
    public void dbManagerTest() {
        assertNotNull("Expecting StoreDBManager to be injected by Sling test runner", dbManager);
    }

    @Test
    public void bundleContextTest() {
        assertNotNull("Expecting BundleContext to be injected by Sling test runner", bundleContext);
    }

    @Test
    public void testConnection() throws StoreException {
        Connection con = dbManager.getConnection();
        assertTrue("Null connection", con != null);
        dbManager.closeConnection(con);
    }

    @Test
    public void testEpochTable() throws StoreException {
        assertTrue(DerbyDBManager.EPOCH_TABLE_NAME + " has not been created",
            dbManager.existsTable(DerbyDBManager.EPOCH_TABLE_NAME));
    }

    @Test
    public void testCreateRevisionTable() throws RevisionManagerException {
        String tableName = "StoreDBManagerRevisionTable";
        try {
            dbManager.createRevisionTable(tableName);
            assertTrue("Failed to create " + tableName, dbManager.existsTable(tableName));
        } finally {
            dbManager.removeTable(tableName);
        }
    }

    @Test
    public void testExistsTable() throws SQLException, RevisionManagerException {
        String tableName = "existTable";
        Statement stmt = null;
        try {
            String createRevisionTable = "CREATE TABLE \""
                                         + tableName
                                         + "\" (id VARCHAR(1024) NOT NULL PRIMARY KEY,revision BIGINT NOT NULL)";
            stmt = con.createStatement();
            stmt.executeUpdate(createRevisionTable);
            assertTrue(String.format("Failed to detect existence of the %s table", tableName),
                dbManager.existsTable(tableName));

        } finally {
            dbManager.closeStatement(stmt);
            dbManager.removeTable(tableName);
        }
    }

    public void testRemoveTable() throws SQLException, RevisionManagerException {
        String tableName = "removeTable";
        Statement stmt = null;
        try {
            String createRevisionTable = "CREATE TABLE \""
                                         + tableName
                                         + "\" (id VARCHAR(1024) NOT NULL PRIMARY KEY,revision BIGINT NOT NULL)";
            stmt = con.createStatement();
            stmt.executeUpdate(createRevisionTable);
            assertTrue(String.format("Failed to remove the %s table", tableName),
                !dbManager.existsTable(tableName));

        } finally {
            dbManager.closeStatement(stmt);
        }
    }

    @Test
    public void testTruncateTable() throws StoreException, SQLException {
        String tableName = "truncatetable";
        // create dummy table
        dbManager.createRevisionTable(tableName);

        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            // put some value into it
            String insertQuery = "INSERT INTO \"" + tableName + "\" (id, revision) VALUES(?,?)";
            long initialRevision = System.currentTimeMillis();
            for (int i = 0; i < 5; i++) {
                ps = con.prepareStatement(insertQuery);
                ps.setString(1, "id" + i);
                ps.setLong(2, initialRevision + i);
                ps.executeUpdate();
                ps.clearParameters();
            }

            // truncate table
            ps.close();
            dbManager.truncateTable(tableName);

            // check the values
            ps = con.prepareStatement("SELECT * FROM \"" + tableName + "\"");
            rs = ps.executeQuery();

            boolean recordExists = false;
            if (rs.next()) {
                recordExists = true;
            }
            assertTrue("There are still records after truncate", recordExists == false);
        } finally {
            dbManager.closeResultSet(rs);
            dbManager.closeStatement(ps);
            dbManager.removeTable(tableName);
        }
    }

    @Test
    public void testGetEpochEntry() throws SQLException, RevisionManagerException {
        long epoch = 413;
        String tableName = "getEpochTable";
        try {
            dbManager.createEpochEntry(tableName, epoch);
            long retrivedEpoch = dbManager.getEpoch(tableName);
            assertTrue("Failed to retrieve expected epoch value", retrivedEpoch == epoch);

        } finally {
            // remove test entry
            dbManager.removeEpochEntry(tableName);
        }
    }

    @Test
    public void testCreateEpochEntry() throws RevisionManagerException {
        String tableName = "createEpochEntryTable";
        long epoch = 413;
        try {
            dbManager.createEpochEntry(tableName, epoch);
            long retrievedEpoch = dbManager.getEpoch(tableName);
            assertTrue("Created and retrieved epochs should be the same", retrievedEpoch == epoch);
        } finally {
            // remove test entry
            dbManager.removeEpochEntry(tableName);
        }
    }

    @Test
    public void testUpdateEpochEntry() throws RevisionManagerException {
        String tableName = "updateEpochEntryTable";
        long epoch = 413;
        long updatedEpoch = 909;
        try {
            dbManager.createEpochEntry(tableName, epoch);
            dbManager.updateEpochEntry(tableName, updatedEpoch);
            long retrievedEpoch = dbManager.getEpoch(tableName);
            assertTrue("Updated and retrieved epoch should be the same", updatedEpoch == retrievedEpoch);
        } finally {
            dbManager.removeEpochEntry(tableName);
        }
    }

    @Test(expected = RevisionManagerException.class)
    public void testRemoveEpochEntry() throws RevisionManagerException {
        String tableName = "removeEpochEntry";
        long epoch = 413;
        dbManager.createEpochEntry(tableName, epoch);
        dbManager.removeEpochEntry(tableName);
        dbManager.getEpoch(tableName);
    }

    @Test
    public void testCreateExistsRevisionMethods() throws RevisionManagerException {
        String tableName = "revisionMethodsTable";
        String itemID = "itemID";
        long revision = 413;
        try {
            dbManager.createRevisionTable(tableName);
            dbManager.createRevisionEntry(tableName, itemID, revision);
            assertTrue("Failed to create revision", dbManager.existsRevisionEntry(tableName, itemID));
        } finally {
            dbManager.removeTable(tableName);
        }
    }

    @Test
    public void testCreateGetRevisionEntries() throws RevisionManagerException {
        String tableName = "createEntriesTable";
        String itemID = "itemID";
        List<String> itemIDs = new ArrayList<String>();
        for (int i = 0; i < 5; i++) {
            itemIDs.add(itemID + i);
        }
        try {
            dbManager.createRevisionTable(tableName);
            dbManager.createRevisionEntries(tableName, itemIDs);
            List<RevisionBean> revisions = dbManager.getRevisions(tableName, Long.MIN_VALUE, Long.MAX_VALUE,
                5, 0);
            for (int i = 0; i < 5; i++) {
                String id = revisions.get(i).getID();
                assertTrue(String.format("Original items does not contain the ID: %s", id),
                    itemIDs.contains(id));
            }
        } finally {
            dbManager.removeTable(tableName);
        }
    }

    @Test
    public void testUpdateRevisionEntry() throws RevisionManagerException {
        String tableName = "updateRevisionEntryTable";
        String itemID = "itemID";
        long revision = 413;
        long updatedRevision = 909;
        try {
            dbManager.createRevisionTable(tableName);
            dbManager.createRevisionEntry(tableName, itemID, revision);
            dbManager.updateRevisionEntry(tableName, itemID, updatedRevision);
            List<RevisionBean> revisions = dbManager.getRevisions(tableName, 0, 1000, 1, 0);
            assertTrue("Failed to retrieve the updated revision",
                revisions.get(0).getRevision() == updatedRevision);
        } finally {
            dbManager.removeTable(tableName);
        }
    }
}

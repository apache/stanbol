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

import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.apache.stanbol.commons.semanticindex.store.StoreException;
import org.apache.stanbol.contenthub.revisionmanager.RevisionManager;
import org.apache.stanbol.contenthub.revisionmanager.StoreDBManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(SlingAnnotationsTestRunner.class)
public class StoreDBManagerTest {
    @TestReference
    private StoreDBManager dbManager;

    @TestReference
    private RevisionManager revisionManager;

    @TestReference
    private BundleContext bundleContext;

    @Test
    public void dbManagerTest() {
        assertNotNull("Expecting StoreDBManager to be injected by Sling test runner", dbManager);
    }

    @Test
    public void dbRevisionManagerTest() {
        assertNotNull("Expecting RevisionManager to be injected by Sling test runner", revisionManager);
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
        assertTrue(StoreDBManager.EPOCH_TABLE_NAME + " has not been created",
            dbManager.existsTable(StoreDBManager.EPOCH_TABLE_NAME));
    }

    @Test
    public void testCreateRevisionTable() throws StoreException, SQLException {
        String tableName = "StoreDBManagerRevisionTable";
        dbManager.createRevisionTable(tableName);
        assertTrue("Failed to create " + tableName, dbManager.existsTable(tableName));
        // clear test data
        Connection con = dbManager.getConnection();
        Statement stmt = null;
        try {
            // first remove the the table
            stmt = con.createStatement();
            stmt.executeUpdate("DROP TABLE " + tableName);

        } finally {
            dbManager.closeStatement(stmt);
            dbManager.closeConnection(con);
        }
    }

    @Test
    public void testTruncateTable() throws StoreException, SQLException {
        String tableName = "truncatetable";
        // create dummy table
        dbManager.createRevisionTable(tableName);

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            // put some value into it
            String insertQuery = "INSERT INTO " + tableName + "(id, revision) VALUES(?,?)";
            long initialRevision = System.currentTimeMillis();
            con = dbManager.getConnection();
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
            ps = con.prepareStatement("SELECT * FROM " + tableName);
            rs = ps.executeQuery();

            boolean recordExists = false;
            if (rs.next()) {
                recordExists = true;
            }
            assertTrue("There are still records after truncate", recordExists == false);
        } finally {
            dbManager.closeResultSet(rs);
            dbManager.closeStatement(ps);

            // clear test data
            Statement stmt = null;
            try {
                // first remove the the table
                stmt = con.createStatement();
                stmt.executeUpdate("DROP TABLE " + tableName);

            } finally {
                dbManager.closeStatement(stmt);
                dbManager.closeConnection(con);
            }
        }
    }
}

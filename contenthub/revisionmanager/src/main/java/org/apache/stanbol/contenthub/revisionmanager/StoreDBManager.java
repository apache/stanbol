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
package org.apache.stanbol.contenthub.revisionmanager;

import java.net.ConnectException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.semanticindex.store.Store;
import org.apache.stanbol.commons.semanticindex.store.StoreException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This class manages the Apache Derby database tables utilized in the scope of {@link FileStore}. It is
 * responsible only for existence of the tables. Population of the tables is done by dedicated classes.
 * </p>
 * <p>
 * This class also provides common methods regarding with SQL objects e.g obtaining connection; closing
 * connection, statement, result set.
 * </p>
 * 
 * @author suat
 * 
 */
@Component(immediate = true)
@Service(value = StoreDBManager.class)
public class StoreDBManager {

    public static final String EPOCH_TABLE_NAME = "epochTable";

    private static Logger log = LoggerFactory.getLogger(StoreDBManager.class);

    private static int MAX_ID_LENGTH = 1024;

    private static String DB_URL;

    @Activate
    protected void activate(ComponentContext componentContext) throws StoreException {
        String stanbolHome = componentContext.getBundleContext().getProperty("sling.home");
        DB_URL = "jdbc:derby:" + stanbolHome + "/contenthub/store/revisions;create=true";
        // initialize the epoch table
        createEpochTable();
    }

    /**
     * Creates an empty revision table with the given name. Different {@link Store} implementations are
     * expected to call this method in their initializations with the value to be obtained by
     * {@link RevisionManager#getStoreID(Store)} method.
     * 
     * @param tableName
     *            name of the table to be created
     * @throws StoreException
     */
    public void createRevisionTable(String tableName) throws StoreException {
        Connection con = getConnection();
        Statement stmt = null;
        try {
            // try to create revision table
            if (!existsTable(tableName)) {
                String createRevisionTable = "CREATE TABLE " + tableName + " (" + "id VARCHAR("
                                             + MAX_ID_LENGTH + ") NOT NULL PRIMARY KEY,"
                                             + "revision BIGINT NOT NULL)";
                stmt = con.createStatement();
                stmt.executeUpdate(createRevisionTable);
                log.info("Revision table created for {}.", tableName);
            } else {
                log.info("Revision table already exists for {}", tableName);
            }

        } catch (SQLException e) {
            log.error(String.format("Failed to create table %s", tableName), e);
            throw new StoreException(String.format("Failed to create table %s", tableName), e);
        } finally {
            closeStatement(stmt);
            closeConnection(con);
        }
    }

    private void createEpochTable() throws StoreException {
        Connection con = getConnection();
        Statement stmt = null;
        try {
            // try to create revision table
            if (!existsTable(EPOCH_TABLE_NAME)) {
                String createRevisionTable = "CREATE TABLE " + EPOCH_TABLE_NAME + " (" + "tableName VARCHAR("
                                             + MAX_ID_LENGTH + ") NOT NULL PRIMARY KEY,"
                                             + "epoch BIGINT NOT NULL)";
                stmt = con.createStatement();
                stmt.executeUpdate(createRevisionTable);
                log.info("'" + EPOCH_TABLE_NAME + "' table has been created.");
            } else {
                log.info("'" + EPOCH_TABLE_NAME + "' table already exists.");
            }

        } catch (SQLException e) {
            log.error(String.format("Failed to create table %s", EPOCH_TABLE_NAME), e);
            throw new StoreException(String.format("Failed to create table %s", EPOCH_TABLE_NAME), e);
        } finally {
            closeStatement(stmt);
            closeConnection(con);
        }
    }

    /**
     * Check for the existence of the specified table
     * 
     * @param tableName
     *            name of the table to be checked
     * @return <code>true</code> if the table already exists, otherwise <code>false</code>.
     * @throws StoreException
     */
    public boolean existsTable(String tableName) throws StoreException {
        boolean exists = false;
        ResultSet rs = null;
        Connection con = getConnection();
        try {
            con = DriverManager.getConnection(DB_URL);
            DatabaseMetaData meta = con.getMetaData();
            rs = meta.getTables(null, null, null, new String[] {"TABLE"});
            while (rs.next()) {
                if (rs.getString("TABLE_NAME").equalsIgnoreCase(tableName)) {
                    exists = true;
                    break;
                }
            }
        } catch (SQLException e) {
            log.error("Failed to check existence of the table: {}", tableName);
            throw new StoreException(String.format("Failed to check existence of the table: %s", tableName),
                    e);
        } finally {
            closeResultSet(rs);
            closeConnection(con);
        }
        return exists;
    }

    /**
     * Truncates the content of the table specified with the {@code tableName}
     * 
     * @param tableName
     *            name of the table to be truncated
     * @throws StoreException
     */
    public void truncateTable(String tableName) throws StoreException {
        boolean exists = false;
        ResultSet rs = null;
        Connection con = getConnection();
        try {
            con = DriverManager.getConnection(DB_URL);
            DatabaseMetaData meta = con.getMetaData();
            rs = meta.getTables(null, null, null, new String[] {"TABLE"});
            while (rs.next()) {
                if (rs.getString("TABLE_NAME").equalsIgnoreCase(tableName)) {
                    exists = true;
                    break;
                }
            }
        } catch (SQLException e) {
            log.error("Failed to check existence of the table: {}", tableName);
            throw new StoreException(String.format("Failed to check existence of the table: %s", tableName),
                    e);
        } finally {
            closeResultSet(rs);
            closeConnection(con);
        }
        if (!exists) {
            throw new IllegalArgumentException(String.format("There is no table having name: %s", tableName));
        }
        String truncateTable = "TRUNCATE TABLE " + tableName;
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            stmt.execute(truncateTable);
        } catch (SQLException e) {
            log.error("Failed to truncate table: {}", tableName, e);
            throw new StoreException(String.format("Failed to truncate table: %s", tableName), e);
        }
        log.debug("Table having name: {} has been truncated", tableName);
    }

    /**
     * Closes the given {@link Connection}
     * 
     * @param con
     */
    public void closeConnection(Connection con) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                log.warn("Failed to close connection", e);
            }
        }
    }

    /**
     * Closes the given {@link Statement}
     * 
     * @param stmt
     */
    public void closeStatement(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                log.warn("Failed to close prepared statement", e);
            }
        }
    }

    /**
     * Closes the given {@link ResultSet}
     * 
     * @param rs
     */
    public void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                log.warn("Failed to close result set", e);
            }
        }
    }

    /**
     * Creates a {@link ConnectException}
     * 
     * @return the {@link Connection} if successfully established
     * @throws StoreException
     */
    public Connection getConnection() throws StoreException {
        Connection con = null;
        try {
            con = DriverManager.getConnection(DB_URL);
        } catch (SQLException e) {
            log.error("Failed to obtain Derby connection", e);
            throw new StoreException("Failed to obtain Derby connection", e);
        }
        return con;
    }
}

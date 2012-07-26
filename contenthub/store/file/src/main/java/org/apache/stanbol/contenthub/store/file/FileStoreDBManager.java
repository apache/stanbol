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
package org.apache.stanbol.contenthub.store.file;

import static org.apache.stanbol.contenthub.store.file.FileRevisionManager.REVISION_TABLE_NAME;
import static org.apache.stanbol.contenthub.store.file.FileStore.FIELD_ENHANCEMENT_COUNT;
import static org.apache.stanbol.contenthub.store.file.FileStore.FIELD_ID;
import static org.apache.stanbol.contenthub.store.file.FileStore.FIELD_MIME_TYPE;
import static org.apache.stanbol.contenthub.store.file.FileStore.FIELD_TITLE;
import static org.apache.stanbol.contenthub.store.file.FileStore.RECENTLY_ENHANCED_TABLE_NAME;

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
import org.apache.stanbol.commons.semanticindex.store.StoreException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages the Apache Derby database tables utilized in the scope of {@link FileStore}. It is
 * responsible only for existence of the tables. Population of the tables is done by dedicated classes. This
 * class also provides common methods regarding with SQL objects e.g obtaining connection; closing connection,
 * statement, result set.
 * 
 * @author suat
 * 
 */
@Component(immediate = true)
@Service(value = FileStoreDBManager.class)
public class FileStoreDBManager {

    private static Logger log = LoggerFactory.getLogger(FileRevisionManager.class);

    private static int MAX_ID_LENGTH = 1024;

    private static final String CREATE_REVISION_TABLE = "CREATE TABLE " + REVISION_TABLE_NAME + " ("
                                                        + "id VARCHAR(" + MAX_ID_LENGTH
                                                        + ") NOT NULL PRIMARY KEY,"
                                                        + "revision BIGINT NOT NULL)";

    private static final String CREATE_RECENTLY_ENHANCED_TABLE = "CREATE TABLE "
                                                                 + RECENTLY_ENHANCED_TABLE_NAME + " ("
                                                                 + FIELD_ID + " VARCHAR(" + MAX_ID_LENGTH
                                                                 + " ) NOT NULL PRIMARY KEY,"
                                                                 + FIELD_MIME_TYPE + " VARCHAR("
                                                                 + MAX_ID_LENGTH + "),"
                                                                 + FIELD_ENHANCEMENT_COUNT + " BIGINT,"
                                                                 + FIELD_TITLE + " VARCHAR(" + MAX_ID_LENGTH
                                                                 + "))";

    private static String DB_URL;

    @Activate
    protected void activate(ComponentContext componentContext) throws StoreException {
        String stanbolHome = componentContext.getBundleContext().getProperty("sling.home");
        //TODO: do not use the datafiles folder for storing things.
        //      the contenthub should use its own folder -> {sling.home}/contenthub/...
        DB_URL = "jdbc:derby:" + stanbolHome + "/datafiles/contenthub/filestore/filestorerevisions;create=true";
        log.info("Initializing file store revision database");
        Connection con = getConnection();
        Statement stmt = null;
        try {
            // try to create revision table
            if (!existsTable(FileRevisionManager.REVISION_TABLE_NAME)) {
                stmt = con.createStatement();
                stmt.executeUpdate(CREATE_REVISION_TABLE);
                log.info("Revision table created.");
            } else {
                log.info("Revision table already exists");
            }

            // try to create recently_enhanced table
            if (!existsTable(RECENTLY_ENHANCED_TABLE_NAME)) {
                stmt = con.createStatement();
                stmt.executeUpdate(CREATE_RECENTLY_ENHANCED_TABLE);
                log.info("RecentlyEnhanced table created.");
            } else {
                log.info("RecentlyEnhanced table already exists");
            }
        } catch (SQLException e) {
            log.error("Failed to create table", e);
            throw new StoreException("Failed to create table", e);
        } finally {
            closeStatement(stmt);
            closeConnection(con);
        }
        log.info("File store databases initialized.");
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

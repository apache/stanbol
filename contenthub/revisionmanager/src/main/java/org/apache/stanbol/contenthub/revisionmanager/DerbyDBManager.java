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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.semanticindex.store.Store;
import org.apache.stanbol.commons.semanticindex.store.revisionmanager.RevisionBean;
import org.apache.stanbol.commons.semanticindex.store.revisionmanager.RevisionManagerException;
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
@Service(value = DerbyDBManager.class)
public class DerbyDBManager {

    private static final int INSERT_BATCH_SIZE = 1000;

    public static final String EPOCH_TABLE_NAME = "epochTable";

    private static int MAX_ID_LENGTH = 1024;

    // private String SELECT_CHANGES =
    // "SELECT id, revision FROM \"%s\" WHERE revision > ? ORDER BY revision ASC";
    private String SELECT_CHANGES = "SELECT id, revision FROM \"%s\" WHERE revision > ? AND revision <= ? ORDER BY revision ASC OFFSET ? ROWS";

    private String SELECT_REVISION = "SELECT id,revision FROM \"%s\" content_item_revision WHERE id = ?";

    private String INSERT_REVISION = "INSERT INTO \"%s\" (id, revision) VALUES (?,?)";

    private String UPDATE_REVISION = "UPDATE \"%s\" SET revision=? WHERE id=?";

    private String SELECT_EPOCH = "SELECT epoch FROM " + EPOCH_TABLE_NAME + " WHERE tableName = ?";

    private String INSERT_EPOCH = "INSERT INTO " + DerbyDBManager.EPOCH_TABLE_NAME
                                  + " (epoch, tableName) values (?, ?)";

    private String UPDATE_EPOCH = "UPDATE " + DerbyDBManager.EPOCH_TABLE_NAME
                                  + " SET epoch = ? WHERE tableName = ?";

    private static Logger log = LoggerFactory.getLogger(DerbyDBManager.class);

    private static String DB_URL;

    @Activate
    protected void activate(ComponentContext componentContext) throws RevisionManagerException {
        System.setProperty("derby.language.statementCacheSize", "0");
        String stanbolHome = componentContext.getBundleContext().getProperty("sling.home");
        DB_URL = "jdbc:derby:" + stanbolHome + "/contenthub/store/revisions;create=true";
        // initialize the epoch table
        createEpochTable();
    }

    /*
     * Table scoped methods
     */

    /**
     * Creates an empty revision table with the given name. Different {@link Store} implementations are
     * expected to call this method in their initializations with the value to be obtained by
     * {@link DerbyRevisionManager#getStoreID(Store)} method. If there is already a revision table for this
     * name, nothing is done.
     * 
     * @param tableName
     *            name of the table to be created
     * @throws RevisionManagerException
     */
    public void createRevisionTable(String tableName) throws RevisionManagerException {
        Connection con = null;
        Statement stmt = null;
        try {
            // try to create revision table
            if (!existsTable(tableName)) {
                con = getConnection();
                String createRevisionTable = "CREATE TABLE \"" + tableName + "\" (id VARCHAR("
                                             + MAX_ID_LENGTH + ") NOT NULL PRIMARY KEY,"
                                             + "revision BIGINT NOT NULL)";
                stmt = con.createStatement();
                stmt.executeUpdate(createRevisionTable);
                log.info("Revision table: {} has beem created.", tableName);
            } else {
                log.info("Revision table already exists for {}", tableName);
            }

        } catch (SQLException e) {
            log.error(String.format("Failed to create table %s", tableName), e);
            throw new RevisionManagerException(String.format("Failed to create table %s", tableName), e);
        } finally {
            closeStatement(stmt);
            closeConnection(con);
        }
    }

    private void createEpochTable() throws RevisionManagerException {
        Connection con = getConnection();
        Statement stmt = null;
        try {
            // try to create revision table
            if (!existsTable(EPOCH_TABLE_NAME)) {
                String createRevisionTable = "CREATE TABLE " + EPOCH_TABLE_NAME + " (tableName VARCHAR("
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
            throw new RevisionManagerException(String.format("Failed to create table %s", EPOCH_TABLE_NAME),
                    e);
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
     * @throws RevisionManagerException
     */
    public boolean existsTable(String tableName) throws RevisionManagerException {
        boolean exists = false;
        ResultSet rs = null;
        Connection con = null;
        try {
            con = getConnection();
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
            throw new RevisionManagerException(String.format("Failed to check existence of the table: %s",
                tableName), e);
        } finally {
            closeResultSet(rs);
            closeConnection(con);
        }
        return exists;
    }

    /**
     * Drops the table specified with the {@code tableName}
     * 
     * @param tableName
     *            name of the table to be dropped
     * @throws RevisionManagerException
     */
    public void removeTable(String tableName) throws RevisionManagerException {
        Connection con = getConnection();
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            stmt.executeUpdate("DROP TABLE \"" + tableName + "\"");
            log.info("The revision table having name: {} has been dropped", tableName);
        } catch (SQLException e) {
            log.error("Failed to drop the table: {}", tableName);
            throw new RevisionManagerException(String.format("Failed to drop the table: %s", tableName), e);
        } finally {
            closeStatement(stmt);
            closeConnection(con);
        }
    }

    /**
     * Truncates the content of the table specified with the {@code tableName}
     * 
     * @param tableName
     *            name of the table to be truncated
     * @throws RevisionManagerException
     */
    public void truncateTable(String tableName) throws RevisionManagerException {
        boolean exists = existsTable(tableName);
        if (!exists) {
            throw new IllegalArgumentException(String.format("There is no table having name: %s", tableName));
        }
        String truncateTable = "TRUNCATE TABLE \"" + tableName + "\"";
        Connection con = getConnection();
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            stmt.execute(truncateTable);
            log.info("Table having name: {} has been truncated", tableName);
        } catch (SQLException e) {
            log.error("Failed to truncate table: {}", tableName, e);
            throw new RevisionManagerException(String.format("Failed to truncate table: %s", tableName), e);
        } finally {
            closeStatement(stmt);
            closeConnection(con);
        }
    }

    /*
     * Index scoped methods
     */

    /**
     * Creates an index over the <b>revision</b> fields of the revision tables created via the
     * {@link #createRevisionTable(String)} method.
     * 
     * @param indexName
     *            name of the index to be created
     * @param tableName
     *            name of the table on which the index will be created
     * @throws RevisionManagerException
     */
    public void createRevisionIndex(String indexName, String tableName) throws RevisionManagerException {
        Connection con = getConnection();
        Statement stmt = null;

        String createIndex = "CREATE INDEX \"" + indexName + "\" ON \"" + tableName + "\" (revision)";
        try {
            stmt = con.createStatement();
            stmt.executeUpdate(createIndex);
            log.info("Revision index has been created for the revision table: {}.", tableName);
        } catch (SQLException e) {
            log.error(
                String.format("Failed to create the index: %s for the table: %s", indexName, tableName), e);
            throw new RevisionManagerException(String.format(
                "Failed to create the index: %s for the table: %s", indexName, tableName), e);
        } finally {
            closeStatement(stmt);
            closeConnection(con);
        }
    }

    /**
     * Drops the index specified with the {@code indexName}
     * 
     * @param indexName
     *            name of the index to be dropped
     * @throws RevisionManagerException
     */
    public void removeIndex(String indexName) throws RevisionManagerException {
        Connection con = getConnection();
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            stmt.executeUpdate("DROP INDEX \"" + indexName + "\"");
            log.info("The index: {} has been dropped", indexName);
        } catch (SQLException e) {
            log.error("Failed to drop the index: {}", indexName);
            throw new RevisionManagerException(String.format("Failed to drop the index: %s", indexName), e);
        } finally {
            closeStatement(stmt);
            closeConnection(con);
        }
    }

    /*
     * Epoch entry modification methods
     */

    /**
     * Returns the epoch value for the given {@link Store}
     * 
     * @param storeID
     *            identifier of the Store of which epoch value will be returned
     * @return the epoch value for the given store if there exists such a value
     * @throws RevisionManagerException
     */
    public long getEpoch(String storeID) throws RevisionManagerException {
        Connection con = getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        long epoch;
        try {
            ps = con.prepareStatement(SELECT_EPOCH);
            ps.setString(1, storeID);
            rs = ps.executeQuery();

            if (rs.next()) {
                epoch = rs.getLong(1);
            } else {
                log.error(String.format("There is not an epoch record for the Store: %s", storeID));
                throw new RevisionManagerException(String.format(
                    "There is not an epoch record for the Store: %s", storeID));
            }
            log.info("The epoch for the Store: %s has been fetched as %s", storeID, epoch);

        } catch (SQLException e) {
            log.error("Failed to get epoch of the Store: {}", storeID, e);
            throw new RevisionManagerException(
                    String.format("Failed to get epoch of the Store: %s", storeID), e);
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
            closeConnection(con);
        }
        return epoch;
    }

    /**
     * Creates a new epoch entry for the given {@link Store}
     * 
     * @param storeID
     *            identifier of the {@link Store} for which a new epoch entry will be created
     * @param epoch
     *            value of the epoch
     * @throws RevisionManagerException
     */
    public void createEpochEntry(String storeID, long epoch) throws RevisionManagerException {
        Connection con = getConnection();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(INSERT_EPOCH);
            ps.setLong(1, epoch);
            ps.setString(2, storeID);
            ps.executeUpdate();
            log.info("New epoch: {} has been created for the Store: {}", epoch, storeID);
        } catch (SQLException e) {
            log.error("Failed to create new epoch entry for the Store: {}", storeID, e);
            throw new RevisionManagerException(String.format(
                "Failed to create new epoch entry for the Store: %s", storeID), e);
        } finally {
            closeStatement(ps);
            closeConnection(con);
        }
    }

    /**
     * Updates the existing epoch entry for the given {@link Store}
     * 
     * @param storeID
     *            identifier of the Store of which epoch will be updated
     * @param epoch
     *            new epoch value
     * @throws RevisionManagerException
     */
    public void updateEpochEntry(String storeID, long epoch) throws RevisionManagerException {
        Connection con = getConnection();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(UPDATE_EPOCH);
            ps.setLong(1, epoch);
            ps.setString(2, storeID);
            ps.executeUpdate();
            log.info("New epoch: {} has been inserted for the Store: {}", epoch, storeID);
        } catch (SQLException e) {
            log.error("Failed to create new epoch entry for the Store: {}", storeID, e);
            throw new RevisionManagerException(String.format(
                "Failed to create new epoch entry for the Store: %s", storeID), e);
        } finally {
            closeStatement(ps);
            closeConnection(con);
        }
    }

    /**
     * Removes the epoch entry regarding the given {@link Store}
     * 
     * @param storeID
     *            identifier of the Store of which epoch record will be deleted
     * @throws RevisionManagerException
     */
    public void removeEpochEntry(String storeID) throws RevisionManagerException {
        Connection con = getConnection();
        PreparedStatement ps = null;

        try {
            ps = con.prepareStatement("DELETE FROM " + EPOCH_TABLE_NAME + " WHERE tableName = ?");
            ps.setString(1, storeID);
            ps.executeUpdate();
            log.info("The epoch entry for the the Store: {} has been deleted", storeID);
        } catch (SQLException e) {
            log.error("Failed to remove the epoch entry for the store: {}", storeID, e);
            throw new RevisionManagerException(String.format(
                "Failed to remove the epoch entry for the store: {}", storeID), e);
        } finally {
            closeStatement(ps);
            closeConnection(con);
        }
    }

    /*
     * Revision entry modification methods
     */

    /**
     * Obtains changes which are larger than the given {@code revision} from the specified table
     * 
     * @param tableName
     *            name of the table from which the revisions will be obtained
     * @param revision
     *            results will be larger than the value specified in this parameter
     * @param upperRevision
     *            results will be smaller than or equal to the value specified in this parameter
     * @param limit
     *            maximum number changes to be returned
     * @param offset
     *            starting point of entries to be obtained from the database
     * @return {@link List} of {@link RevisionBean}s. RevisionBeans contain the id representing the item and
     *         the changed revision
     * @throws RevisionManagerException
     */
    public List<RevisionBean> getRevisions(String tableName,
                                           long revision,
                                           long upperRevision,
                                           int limit,
                                           int offset) throws RevisionManagerException {
        List<RevisionBean> results = new ArrayList<RevisionBean>();
        Connection con = getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement(String.format(SELECT_CHANGES, tableName),
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ps.setLong(1, revision);
            ps.setLong(2, upperRevision);
            ps.setInt(3, offset);
            ps.setMaxRows(limit);
            rs = ps.executeQuery();
            while (rs.next()) {
                results.add(new RevisionBean(rs.getString("id"), rs.getLong("revision")));
            }
            return results;
        } catch (SQLException e) {
            log.error("Failed to get changes from the table: {}", tableName, e);
            throw new RevisionManagerException(String.format("Failed to get changes from the table: %s",
                tableName), e);
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
            closeConnection(con);
        }
    }

    /**
     * Checks a revision entry exists for the given item and table
     * 
     * @param tableName
     *            name of the table on which the check will be done
     * @param itemID
     *            identifier of the item to be checked
     * @return {@code true} if a record exists, otherwise {@code false}
     * @throws RevisionManagerException
     */
    public boolean existsRevisionEntry(String tableName, String itemID) throws RevisionManagerException {
        Connection con = getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        boolean recordExist = false;
        try {
            ps = con.prepareStatement(String.format(SELECT_REVISION, tableName));
            ps.setString(1, itemID);
            rs = ps.executeQuery();
            if (rs.next()) {
                recordExist = true;
            }
            log.info(String
                    .format(
                        "Checked the existence of revision entry for the table: %s and for the item: %s with the result: %b",
                        new Object[] {tableName, itemID, recordExist}));
            return recordExist;
        } catch (SQLException e) {
            log.error(String.format(
                "Failed to check existence of revision entry for the table: %s and item: %s", tableName,
                itemID), e);
            throw new RevisionManagerException(String.format(
                "Failed to check existence of revision entry for the table: %s and item: %s", tableName,
                itemID), e);
        } finally {
            closeResultSet(rs);
            closeStatement(ps);
            closeConnection(con);
        }
    }

    /**
     * Inserts a new revision entry for the item in the specified table
     * 
     * @param tableName
     *            name of the table in which the revision entry for the given item will be created
     * @param itemID
     *            identifier of the item for which a revision entry will be created
     * @param revision
     *            revision value
     * @throws RevisionManagerException
     */
    public void createRevisionEntry(String tableName, String itemID, long revision) throws RevisionManagerException {
        Connection con = getConnection();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(String.format(INSERT_REVISION, tableName));
            ps.setString(1, itemID);
            ps.setLong(2, revision);
            ps.executeUpdate();
            log.info("Revision of the item: %s in table: %s has been inserted as %d",
                new Object[] {itemID, tableName, revision});
        } catch (SQLException e) {
            log.error(
                String.format("Failed to insert revision for the table: %s and item: %s", tableName, itemID),
                e);
            throw new RevisionManagerException(String.format(
                "Failed to insert revision for the table: %s and item: %s", tableName, itemID), e);
        } finally {
            closeStatement(ps);
            closeConnection(con);
        }
    }

    /**
     * Updates the existing revision of an item with the new revision
     * 
     * @param tableName
     *            name of the table keeping the revision entry for the given item
     * @param itemID
     *            identifier of the item of which revision will be updated
     * @param revision
     *            new revision value
     * @throws RevisionManagerException
     */
    public void updateRevisionEntry(String tableName, String itemID, long revision) throws RevisionManagerException {
        Connection con = getConnection();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(String.format(UPDATE_REVISION, tableName));
            ps.setString(1, itemID);
            ps.setLong(2, revision);
            ps.executeUpdate();
            log.info("Revision of the item: %s in table: %s has been updated as %d", new Object[] {itemID,
                                                                                                   tableName,
                                                                                                   revision});
        } catch (SQLException e) {
            log.error(
                String.format("Failed to update revision for the table: %s and item: %s", tableName, itemID),
                e);
            throw new RevisionManagerException(String.format(
                "Failed to update revision for the table: %s and item: %s", tableName, itemID), e);
        } finally {
            closeStatement(ps);
            closeConnection(con);
        }
    }

    /**
     * Creates new revision entries for the given items in the specified table. Revision numbers start from
     * the {@link Long#MIN_VALUE} and it is incremented 1 by 1. A new revision number is given to each item.
     * To create revision entries in an efficient way, {@link PreparedStatement}s are prepared so that
     * {@value #INSERT_BATCH_SIZE} entries are created in a single query execution.
     * 
     * @param tableName
     *            name of the table in which the revision entries will be created
     * @param itemIDs
     *            identifiers of the items for which revision entries will be created
     * @throws RevisionManagerException
     */
    public void createRevisionEntries(String tableName, List<String> itemIDs) throws RevisionManagerException {
        Connection con = getConnection();
        PreparedStatement ps = null;
        long start = System.currentTimeMillis();
        String query = "INSERT INTO \"" + tableName + "\" (id, revision) VALUES";
        StringBuilder sb = new StringBuilder(query);
        try {
            long revision = Long.MIN_VALUE;
            int i;
            for (i = 0; i < itemIDs.size(); i++) {
                sb.append("(?,?),");
                if ((i + 1) % INSERT_BATCH_SIZE == 0) {
                    sb.setCharAt(sb.length() - 1, ' ');
                    if (ps == null) {
                        ps = con.prepareStatement(sb.toString());
                    }
                    for (int j = i - (INSERT_BATCH_SIZE - 1), l = 1; j <= i; j++, l += 2) {
                        ps.setString(l, itemIDs.get(j));
                        ps.setLong(l + 1, revision++);
                    }
                    ps.executeUpdate();
                    ps.clearParameters();
                    log.info("{} item have been inserted in {}ms", (i + 1),
                        (System.currentTimeMillis() - start));
                    sb = null;
                    sb = new StringBuilder(query);
                }
            }

            if (ps != null) {
                ps.close();
            }

            sb.setCharAt(sb.length() - 1, ' ');
            ps = con.prepareStatement(sb.toString());
            for (int j = i - itemIDs.size() % INSERT_BATCH_SIZE, l = 1; j < i; j++, l += 2) {
                ps.setString(l, itemIDs.get(j));
                ps.setLong(l + 1, revision++);
            }
            ps.executeUpdate();

            long finish = System.currentTimeMillis();
            log.info("Revisions have been created in {}ms in {} number of entities", (finish - start),
                itemIDs.size());

        } catch (SQLException e) {
            log.error("Failed to create revision entries in the table: {}", tableName);
            throw new RevisionManagerException(String.format(
                "Failed to create revision entries in the table: {}", tableName), e);
        } finally {
            closeStatement(ps);
            closeConnection(con);
        }
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
     * @throws RevisionManagerException
     */
    public Connection getConnection() throws RevisionManagerException {
        Connection con = null;
        try {
            con = DriverManager.getConnection(DB_URL);
            log.debug("New database connection has been prepared");
        } catch (SQLException e) {
            log.error("Failed to obtain Derby connection", e);
            throw new RevisionManagerException("Failed to obtain Derby connection", e);
        }
        return con;
    }
}

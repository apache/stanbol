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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.semanticindex.core.store.ChangeSetImpl;
import org.apache.stanbol.commons.semanticindex.store.ChangeSet;
import org.apache.stanbol.commons.semanticindex.store.Store;
import org.apache.stanbol.commons.semanticindex.store.StoreException;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This class aims to manage the epochs of several {@link Store} implementations and the revisions regarding
 * to the {@link ContentItem}s stored in {@link Store} instances. The revisions are kept in a single table in
 * the scope of Apache Derby database. The system time is set as the new revision of the {@link ContentItem}
 * when the {@link #updateRevision(Store, String)} method is called. Epochs of the Stores are managed in a
 * single table initialized by {@link StoreDBManager}.
 * </p>
 * <p>
 * In the scope of this RevisionManager {@link Store}s are identified by according to their names which can be
 * obtained via the {@link Store#getName()} method.
 * </p>
 * 
 * @author suat
 * 
 */
@Component(immediate = true)
@Service(value = RevisionManager.class)
public class RevisionManager {
    private static Logger log = LoggerFactory.getLogger(RevisionManager.class);

    private String SELECT_REVISION = "SELECT id,revision FROM %s content_item_revision WHERE id = ?";

    private String INSERT_REVISION = "INSERT INTO %s (id, revision) VALUES (?,?)";

    private String UPDATE_REVISION = "UPDATE %s SET revision=? WHERE id=?";

    private String SELECT_CHANGES = "SELECT id, revision FROM %s WHERE revision > ? ORDER BY revision ASC";

    private String SELECT_MORECHANGES = "SELECT id, revision FROM %s WHERE revision > ? AND revision <= ? ORDER BY revision ASC OFFSET ? ROWS";

    private String SELECT_EPOCH = "SELECT epoch FROM " + StoreDBManager.EPOCH_TABLE_NAME
                                  + " WHERE tableName = ?";

    private String INSERT_EPOCH = "INSERT INTO " + StoreDBManager.EPOCH_TABLE_NAME
                                  + " (epoch, tableName) values (?, ?)";

    private String UPDATE_EPOCH = "UPDATE " + StoreDBManager.EPOCH_TABLE_NAME
                                  + " SET epoch = ? WHERE tableName = ?";

    @Reference
    StoreDBManager dbManager;

    /**
     * Updates revision of the {@link ContentItem} specified with the <code>contentItemID</code> parameter and
     * managed within the given {@code store}. The system time set as the new revision number by
     * {@link System#currentTimeMillis()}.
     * 
     * @param store
     *            The {@link Store} instance in which the {@link ContentItem} specified by the
     *            {@code contentItemID} is stored
     * @param contentItemID
     *            ID of the {@link ContentItem} of which revision to be updated
     * @return the new revision
     * @throws StoreException
     */
    public <Item> long updateRevision(Store<Item> store, String contentItemID) throws StoreException {
        // get connection
        Connection con = dbManager.getConnection();
        String revisionTableName = getStoreID(store);

        // check existence of record for the given content item id
        PreparedStatement ps = null;
        ResultSet rs = null;
        boolean recordExist = false;
        try {
            ps = con.prepareStatement(String.format(SELECT_REVISION, revisionTableName));
            ps.setString(1, contentItemID);
            rs = ps.executeQuery();
            if (rs.next()) {
                recordExist = true;
            }

        } catch (SQLException e) {
            dbManager.closeConnection(con);
            log.error("Failed to query revision of content item", e);
            throw new StoreException("Failed to query revision of content item", e);
        } finally {
            dbManager.closeResultSet(rs);
            dbManager.closeStatement(ps);
        }

        // update the table
        try {
            long newRevision = System.currentTimeMillis();
            if (!recordExist) {
                log.debug("New revision: {} for the content item: {} of Store: {} will be added",
                    new Object[] {newRevision, contentItemID, revisionTableName});
                ps = con.prepareStatement(String.format(INSERT_REVISION, revisionTableName));
                ps.setString(1, contentItemID);
                ps.setLong(2, newRevision);
            } else {
                log.debug("New revision: {} for the content item: {} of Store: {} will be updated",
                    new Object[] {newRevision, contentItemID, revisionTableName});

                ps = con.prepareStatement(String.format(UPDATE_REVISION, revisionTableName));
                ps.setLong(1, newRevision);
                ps.setString(2, contentItemID);
            }
            int updatedRecordNum = ps.executeUpdate();
            // exactly one record should be affected
            if (updatedRecordNum != 1) {
                log.warn("Unexpected number of updated records: {}, should be 1", updatedRecordNum);
            }
            return newRevision;
        } catch (SQLException e) {
            log.error("Failed to update revision", e);
            throw new StoreException("Failed to update revision", e);
        } finally {
            dbManager.closeStatement(ps);
            dbManager.closeConnection(con);
        }
    }

    /**
     * Returns the updates after the given revision number. If the total size of revisions after the given
     * revision number fit in <code>batchSize</code>, all of the changes for the last revision are also
     * included in the results. This is because we do not want to include a subset of the changes regarding to
     * a particular revision in the result.
     * 
     * @param store
     *            {@link Store} instance for which the changes are requested
     * @param revision
     *            Starting revision of the to be returned in the results
     * @param batchSize
     *            Maximum number of changes to be returned. However to return all changes regarding to
     *            particular revisions, the results might not have as much items as specified with this
     *            number. For instance, if there are 10 changes regarding a requested particular revision and
     *            this parameter is specified as 5, the results will include 10 changes.
     * @return a {@link ChangeSet} including the changes in the specified Store.
     * @throws StoreException
     */
    public <Item> ChangeSet<Item> getChanges(Store<Item> store, long revision, int batchSize) throws StoreException {
        Set<String> changedUris = new LinkedHashSet<String>();
        long to, from;

        // get connection
        Connection con = dbManager.getConnection();
        String revisionTableName = getStoreID(store);
        batchSize = batchSize == Integer.MAX_VALUE ? batchSize - 1 : batchSize;

        // check existence of record for the given content item id
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            boolean moreChanges = false;
            long lastRowRevision = 0;
            try {
                ps = con.prepareStatement(String.format(SELECT_CHANGES, revisionTableName),
                    ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                ps.setLong(1, revision);
                // set max rows one more than the batchsize to be able to see if there are other changes for
                // the given revision beyond the batchsize
                ps.setMaxRows(batchSize + 1);
                rs = ps.executeQuery();

                // if there is no row, return an empty ChangeSet
                if (!rs.first()) {
                    return new ChangeSetImpl<Item>(store, store.getEpoch(), Long.MIN_VALUE, Long.MAX_VALUE,
                            changedUris);
                }

                // if the number of changes >= batchsize + 1
                if (rs.absolute(batchSize + 1)) {
                    lastRowRevision = rs.getLong(2);
                    rs.previous();
                    long nextToLastRowRevision = rs.getLong(2);
                    rs.beforeFirst();
                    // if we are in the middle of a revision, set the moreChanges flag to add all changes in
                    // that revision to changedUris
                    if (lastRowRevision == nextToLastRowRevision) {
                        moreChanges = true;
                    }
                    while (rs.next()) {
                        if (rs.isLast()) {
                            break;
                        }
                        changedUris.add(rs.getString(1));
                    }
                    rs.previous();
                } else {
                    rs.beforeFirst();
                    while (rs.next()) {
                        changedUris.add(rs.getString(1));
                    }
                    rs.last();
                }

                to = rs.getLong(2);
                rs.first();
                from = rs.getLong(2);

            } catch (SQLException e) {
                log.error("Failed to get changes", e);
                throw new StoreException("Failed to get changes", e);
            } finally {
                dbManager.closeResultSet(rs);
                dbManager.closeStatement(ps);
            }
            if (moreChanges) {
                try {
                    ps = con.prepareStatement(String.format(SELECT_MORECHANGES, revisionTableName),
                        ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                    ps.setLong(1, revision);
                    ps.setLong(2, lastRowRevision);
                    ps.setLong(3, batchSize);
                    rs = ps.executeQuery();

                    while (rs.next()) {
                        changedUris.add(rs.getString(1));
                    }
                } catch (SQLException e) {
                    log.error("Failed to get changes", e);
                    throw new StoreException("Failed to get changes", e);
                } finally {
                    dbManager.closeResultSet(rs);
                    dbManager.closeStatement(ps);
                }
            }
            return new ChangeSetImpl<Item>(store, store.getEpoch(), from, to, changedUris);
        } finally {
            dbManager.closeConnection(con);
        }
    }

    public <Item> long getEpoch(Store<Item> store) throws StoreException {
        // get connection
        Connection con = dbManager.getConnection();

        PreparedStatement ps = null;
        ResultSet rs = null;
        long epoch;
        try {
            ps = con.prepareStatement(SELECT_EPOCH);
            ps.setString(1, getStoreID(store));
            rs = ps.executeQuery();

            if (rs.next()) {
                epoch = rs.getLong(1);
            } else {
                log.error(String.format("There is not an epoch record for the Store: %s", store.getName()));
                throw new StoreException(String.format("There is not an epoch record for the Store: %s",
                    store.getName()));
            }

        } catch (SQLException e) {
            log.error("Failed to execute query", e);
            throw new StoreException("Failed to execute query", e);
        } finally {
            dbManager.closeResultSet(rs);
            dbManager.closeStatement(ps);
            dbManager.closeConnection(con);
        }
        return epoch;
    }

    /**
     * Updates the epoch of the given {@link Store} with the {@link System#currentTimeMillis()}
     * 
     * @param store
     *            Store instance of which epoch will be updated.
     * @throws StoreException
     */
    public <Item> void updateEpoch(Store<Item> store) throws StoreException {
        updateEpoch(store, false);
    }

    private <Item> void updateEpoch(Store<Item> store, boolean insert) throws StoreException {
        String storeID = getStoreID(store);

        // get connection
        Connection con = dbManager.getConnection();
        PreparedStatement ps = null;

        // update the table
        try {
            long newEpoch = System.currentTimeMillis();
            if (!insert) {
                // truncate the revision table
                dbManager.truncateTable(storeID);

                log.debug("New epoch: {} for the Store: {} will be updated", newEpoch, storeID);
                ps = con.prepareStatement(UPDATE_EPOCH);
            } else {
                // check existence of the epoch entry for the given Store
                ResultSet rs = null;
                boolean recordExist = false;
                try {
                    ps = con.prepareStatement(SELECT_EPOCH);
                    ps.setString(1, storeID);
                    rs = ps.executeQuery();
                    if (rs.next()) {
                        recordExist = true;
                    }

                } catch (SQLException e) {
                    log.error("Failed to query revision of content item", e);
                    throw new StoreException("Failed to query revision of content item", e);
                } finally {
                    dbManager.closeResultSet(rs);
                    dbManager.closeStatement(ps);
                }

                if (!recordExist) {
                    log.debug("New epoch: {} for the Store: {} will be added", newEpoch, storeID);
                    ps = con.prepareStatement(INSERT_EPOCH);
                } else {
                    // if there already exists an entry in the "epochTable"
                    // for the given store, return from the method
                    return;
                }
            }
            ps.setLong(1, newEpoch);
            ps.setString(2, storeID);
            int updatedRecordNum = ps.executeUpdate();
            // exactly one record should be affected
            if (updatedRecordNum != 1) {
                log.warn("Unexpected number of updated records: {}, should be 1", updatedRecordNum);
            }
        } catch (SQLException e) {
            log.error("Failed to update epoch for Store identified as: {}", storeID, e);
            throw new StoreException(String.format("Failed to update epoch for identified as: %s", storeID),
                    e);
        } finally {
            dbManager.closeStatement(ps);
            dbManager.closeConnection(con);
        }
    }

    /**
     * Initializes the revision table for the given {@link Store} if not already done and creates an entry in
     * the "epochTable" for the given Store. This method is expected to be called while initializing the
     * {@link Store} implementations. For instance OSGi based Store implementations can call this method
     * within the @Activate method.
     * 
     * @param store
     *            Store instance of which revision tables are initialized
     * @throws StoreException
     */
    public <Item> void initializeRevisionTables(Store<Item> store) throws StoreException {
        // initialize tables if not already
        dbManager.createRevisionTable(getStoreID(store));

        // add initial epoch for the store
        updateEpoch(store, true);
    }

    /**
     * Clear the resources including the revision table and the epoch entry regarding the given {@link Store}
     * 
     * @param store
     *            Store instance of which resources will be cleared
     * @throws StoreException
     */
    public <Item> void clearRevisionTables(Store<Item> store) throws StoreException {
        Connection con = null;
        Statement stmt = null;
        PreparedStatement ps = null;
        String tableName = getStoreID(store);
        try {
            con = dbManager.getConnection();
            // first remove the the table
            stmt = con.createStatement();
            stmt.executeUpdate("DROP TABLE " + tableName);

            // delete the entry from epoch table
            ps = con.prepareStatement("DELETE FROM " + StoreDBManager.EPOCH_TABLE_NAME
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

    /**
     * Returns a string identifying the given {@link Store} instance in the scope of the revision management.
     * Currently, the name of the Store is used as the identifiers of the Store. The name is obtained through
     * the {@link Store#getName()} method.
     * 
     * @param store
     * @return
     */
    public <Item> String getStoreID(Store<Item> store) {
        return store.getName();
    }
}

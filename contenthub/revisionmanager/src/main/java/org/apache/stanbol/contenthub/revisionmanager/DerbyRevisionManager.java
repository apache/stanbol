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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.semanticindex.core.store.ChangeSetImpl;
import org.apache.stanbol.commons.semanticindex.store.ChangeSet;
import org.apache.stanbol.commons.semanticindex.store.Store;
import org.apache.stanbol.commons.semanticindex.store.StoreException;
import org.apache.stanbol.commons.semanticindex.store.revisionmanager.RevisionManager;
import org.apache.stanbol.commons.semanticindex.store.revisionmanager.RevisionManagerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Apache Derby based implementation of {@link RevisionManager} interface. It does not depend on a specific
 * generic type, so any {@link Store} instance can use it.
 * </p>
 * <p>
 * The revisions of items are kept in a single table in the scope of Apache Derby database. For each Store a
 * separate table is created using the specified {@code storeID}. The system time is set as the new revision
 * of the items when the {@link #updateRevision(Store, String)} method is called.
 * </p>
 * <p>
 * Epochs of the Stores are managed in a single table initialized by {@link DerbyDBManager}. The epoch table
 * contains a single record for each Store. When updating the epochs of the Stores, again the system time is
 * used.
 * </p>
 * 
 * @author suat
 * 
 */
@Component(immediate = true)
@Service
public class DerbyRevisionManager implements RevisionManager {
    private static Logger log = LoggerFactory.getLogger(DerbyRevisionManager.class);

    private String SELECT_REVISION = "SELECT id,revision FROM \"%s\" content_item_revision WHERE id = ?";

    private String INSERT_REVISION = "INSERT INTO \"%s\" (id, revision) VALUES (?,?)";

    private String UPDATE_REVISION = "UPDATE \"%s\" SET revision=? WHERE id=?";

    private String SELECT_CHANGES = "SELECT id, revision FROM \"%s\" WHERE revision > ? ORDER BY revision ASC";

    private String SELECT_MORECHANGES = "SELECT id, revision FROM \"%s\" WHERE revision > ? AND revision <= ? ORDER BY revision ASC OFFSET ? ROWS";

    private String SELECT_EPOCH = "SELECT epoch FROM " + DerbyDBManager.EPOCH_TABLE_NAME
                                  + " WHERE tableName = ?";

    private String INSERT_EPOCH = "INSERT INTO " + DerbyDBManager.EPOCH_TABLE_NAME
                                  + " (epoch, tableName) values (?, ?)";

    private String UPDATE_EPOCH = "UPDATE " + DerbyDBManager.EPOCH_TABLE_NAME
                                  + " SET epoch = ? WHERE tableName = ?";

    @Reference
    DerbyDBManager dbManager;

    /**
     * Initializes the revision table for the {@link Store} specified by the given {@code storeID} if not
     * already done and creates an entry in the "epochTable" for the same Store. This method is expected to be
     * called while initializing the {@link Store} implementations. For instance OSGi based Store
     * implementations can call this method within the @Activate method.
     * 
     * @param storeID
     *            the identifier of the {@link Store}
     * @throws RevisionManagerException
     */
    @Override
    public void registerStore(String storeID) throws RevisionManagerException {
        // initialize tables if not already
        try {
            dbManager.createRevisionTable(storeID);
            log.debug("Revision table initialized for the Store: {}", storeID);
        } catch (StoreException e) {
            log.error("Failed to create revision table for the store: {}", storeID);
            throw new RevisionManagerException(String.format(
                "Failed to create revision table for the store: %s", storeID), e);
        }

        // add initial epoch for the store
        updateEpoch(storeID, true);
        log.debug("Epoch is initialized for the Store: {}", storeID);
    }

    /**
     * Removes the revision table and the epoch entry regarding the given {@link Store}
     * 
     * @param storeID
     *            the identifier of the {@link Store}
     * @throws StoreException
     */
    public void unregisterStore(String storeID) throws RevisionManagerException {
        // get connection
        Connection con = getConnection();
        Statement stmt = null;
        PreparedStatement ps = null;
        String tableName = storeID;
        try {
            // first remove the the table
            stmt = con.createStatement();
            stmt.executeUpdate("DROP TABLE \"" + tableName + "\"");
            log.debug("The revision table for the Store: {} has been dropped", storeID);

            // delete the entry from epoch table
            ps = con.prepareStatement("DELETE FROM " + DerbyDBManager.EPOCH_TABLE_NAME
                                      + " WHERE tableName = ?");
            ps.setString(1, tableName);
            ps.executeUpdate();
            log.debug("The epoch entry for the the Store: {} has been deleted", storeID);

        } catch (SQLException e) {
            log.error("Failed clear test resources for the table: {}", tableName);
            throw new RevisionManagerException(String.format("Failed clear test resources for the table: %s",
                tableName), e);
        } finally {
            dbManager.closeStatement(stmt);
            dbManager.closeStatement(ps);
            dbManager.closeConnection(con);
        }
    }

    @Override
    public long updateRevision(String storeID, String itemID) throws RevisionManagerException {
        // get connection
        Connection con = getConnection();
        String revisionTableName = storeID;

        // check existence of record for the given content item id
        PreparedStatement ps = null;
        ResultSet rs = null;
        boolean recordExist = false;
        try {
            ps = con.prepareStatement(String.format(SELECT_REVISION, revisionTableName));
            ps.setString(1, itemID);
            rs = ps.executeQuery();
            if (rs.next()) {
                recordExist = true;
            }

        } catch (SQLException e) {
            dbManager.closeConnection(con);
            log.error("Failed to query revision of content item", e);
            throw new RevisionManagerException("Failed to query revision of content item", e);
        } finally {
            dbManager.closeResultSet(rs);
            dbManager.closeStatement(ps);
        }

        // update the table
        try {
            long newRevision = System.currentTimeMillis();
            if (!recordExist) {
                log.debug("New revision: {} for the item: {} of Store: {} will be added",
                    new Object[] {newRevision, itemID, revisionTableName});
                ps = con.prepareStatement(String.format(INSERT_REVISION, revisionTableName));
                ps.setString(1, itemID);
                ps.setLong(2, newRevision);
            } else {
                log.debug("New revision: {} for the item: {} of Store: {} will be updated",
                    new Object[] {newRevision, itemID, revisionTableName});

                ps = con.prepareStatement(String.format(UPDATE_REVISION, revisionTableName));
                ps.setLong(1, newRevision);
                ps.setString(2, itemID);
            }
            int updatedRecordNum = ps.executeUpdate();
            // exactly one record should be affected
            if (updatedRecordNum != 1) {
                log.warn("Unexpected number of updated records: {}, should be 1", updatedRecordNum);
            }
            return newRevision;
        } catch (SQLException e) {
            log.error("Failed to update revision", e);
            throw new RevisionManagerException("Failed to update revision", e);
        } finally {
            dbManager.closeStatement(ps);
            dbManager.closeConnection(con);
        }
    }

    /**
     * Returns the changes larger than the given revision number. If the total size of revisions coming after
     * the given revision number <b>DOES NOT</b> fit in <code>batchSize</code>, <b>all of the changes</b> for
     * the last revision are also included in the results. This is because we do not want to include a subset
     * of the changes regarding to a particular revision in the result.
     * 
     * @param store
     *            {@link Store} instance for which the changes are requested
     * @param revision
     *            Starting revision of the to be returned in the results
     * @param batchSize
     *            Maximum number of changes to be returned. Please note that while returning all changes
     *            regarding to particular revisions, the changes might be more than the number specified with
     *            this parameter. For instance, if there are 10 changes regarding a particular revision and
     *            this parameter is specified as 5, the results will include 10 changes.
     * @return a {@link ChangeSet} including the changes in the specified Store.
     * @throws StoreException
     */
    @Override
    public ChangeSet getChanges(String storeID, long revision, int batchSize) throws RevisionManagerException {
        // get connection
        Connection con = getConnection();

        Set<String> changedUris = new LinkedHashSet<String>();
        long to, from;
        String revisionTableName = storeID;
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

                if (!rs.first()) {
                    log.debug("There is no changes in store: {}, after revision: {}", storeID, revision);
                    return new ChangeSetImpl(Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE,
                            new ArrayList<String>());
                }

                // if the number of changes >= batchsize + 1
                if (rs.absolute(batchSize + 1)) {
                    log.debug("There are changes after given revision more than the given batch size");
                    lastRowRevision = rs.getLong(2);
                    rs.previous();
                    long nextToLastRowRevision = rs.getLong(2);
                    rs.beforeFirst();
                    // if we are in the middle of a revision, set the moreChanges flag to add all changes in
                    // that revision to changedUris
                    if (lastRowRevision == nextToLastRowRevision) {
                        log.debug("The changes for the revision: {} exceeds the batch size");
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
                    log.debug("There are changes after given revision less than or equal to the given batch size");
                    rs.beforeFirst();
                    while (rs.next()) {
                        changedUris.add(rs.getString(1));
                    }
                    rs.last();
                }

                to = rs.getLong(2);
                rs.first();
                from = rs.getLong(2);
                log.debug("Changes have been fetched for the initial batch size");

            } catch (SQLException e) {
                log.error("Failed to get changes for the Store: {}", storeID, e);
                throw new RevisionManagerException(String.format("Failed to get changes for the Store: %s",
                    storeID), e);
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
                    log.debug("Changes exceeding the batch size have been fetched");
                } catch (SQLException e) {
                    log.error("Failed to get changes", e);
                    throw new RevisionManagerException("Failed to get changes", e);
                } finally {
                    dbManager.closeResultSet(rs);
                    dbManager.closeStatement(ps);
                }
            }

            long epoch = getEpoch(storeID);
            return new ChangeSetImpl(epoch, from, to, changedUris);
        } finally {
            dbManager.closeConnection(con);
        }
    }

    @Override
    public long getEpoch(String storeID) throws RevisionManagerException {
        // get connection
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
            log.debug("The epoch: {} for the Store: {} has been fetched", epoch, storeID);

        } catch (SQLException e) {
            log.error("Failed to execute query", e);
            throw new RevisionManagerException("Failed to execute query", e);
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
    @Override
    public long updateEpoch(String storeID) throws RevisionManagerException {
        return updateEpoch(storeID, false);
    }

    private long updateEpoch(String storeID, boolean insert) throws RevisionManagerException {
        // get connection
        Connection con = getConnection();
        PreparedStatement ps = null;

        // update the table
        try {
            long newEpoch = System.currentTimeMillis();
            if (!insert) {
                // truncate the revision table
                try {
                    dbManager.truncateTable(storeID);
                } catch (StoreException e) {
                    log.error("Failed to truncate table while updating the epoch for the Store: {}", storeID);
                    throw new RevisionManagerException(String.format(
                        "Failed to truncate table while updating the epoch for the Store: %s", storeID), e);
                }

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
                        newEpoch = rs.getLong("epoch");
                    }

                } catch (SQLException e) {
                    log.error("Failed to query revision of content item", e);
                    throw new RevisionManagerException("Failed to query revision of content item", e);
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
                    return newEpoch;
                }
            }
            ps.setLong(1, newEpoch);
            ps.setString(2, storeID);
            int updatedRecordNum = ps.executeUpdate();
            log.debug("Epoch update/insertion has been done");

            // exactly one record should be affected
            if (updatedRecordNum != 1) {
                log.warn("Unexpected number of updated records: {}, should be 1", updatedRecordNum);
            }
            return newEpoch;
        } catch (SQLException e) {
            log.error("Failed to update epoch for Store identified as: {}", storeID, e);
            throw new RevisionManagerException(String.format("Failed to update epoch for identified as: %s",
                storeID), e);
        } finally {
            dbManager.closeStatement(ps);
            dbManager.closeConnection(con);
        }
    }

    private Connection getConnection() throws RevisionManagerException {
        Connection con;
        try {
            con = dbManager.getConnection();
            return con;
        } catch (StoreException e) {
            log.error("Failed to obtain the database connection", e);
            throw new RevisionManagerException("Failed to obtain the database connection", e);
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
    // public <Item> String getStoreID(Store<Item> store) {
    // return store.getName();
    // }
}

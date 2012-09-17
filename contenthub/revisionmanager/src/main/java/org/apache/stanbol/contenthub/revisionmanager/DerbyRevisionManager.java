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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.semanticindex.core.store.ChangeSetImpl;
import org.apache.stanbol.commons.semanticindex.store.ChangeSet;
import org.apache.stanbol.commons.semanticindex.store.Store;
import org.apache.stanbol.commons.semanticindex.store.StoreException;
import org.apache.stanbol.commons.semanticindex.store.revisionmanager.RevisionBean;
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
        dbManager.createRevisionTable(storeID);

        // add initial epoch for the store
        updateEpoch(storeID, true);

        log.info("The Store: {} has been registered", storeID);
    }

    /**
     * Removes the revision table and associated index on the revision from the Derby database. Also, the
     * epoch entry is removed from the epoch table.
     * 
     * @param storeID
     *            the identifier of the {@link Store}
     * @throws StoreException
     */
    public void unregisterStore(String storeID) throws RevisionManagerException {
        // first remove the table
        dbManager.removeTable(storeID);
        // remove the index
        dbManager.removeIndex(getRevisionIndexName(storeID));
        // delete the entry from epoch table
        dbManager.removeEpochEntry(storeID);

        log.info("The Store: {} has been unregistered");
    }

    @Override
    public long updateRevision(String storeID, String itemID) throws RevisionManagerException {
        String revisionTableName = storeID;

        // check existence of record for the given content item id
        boolean recordExist = dbManager.existsRevisionEntry(revisionTableName, itemID);

        long newRevision = System.currentTimeMillis();
        if (!recordExist) {
            dbManager.createRevisionEntry(revisionTableName, itemID, newRevision);
        } else {
            dbManager.updateRevisionEntry(revisionTableName, itemID, newRevision);
        }
        log.info("Revision of the item: {} in Store: {} has been updated", itemID, storeID);
        return newRevision;
    }

    @Override
    public void registerRevisions(String storeID, List<String> itemIDs) throws RevisionManagerException {
        dbManager.createRevisionEntries(storeID, itemIDs);
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
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be larger than 0");
        }
        // get connection
        Connection con = getConnection();

        Set<String> changedUris = new LinkedHashSet<String>();
        long to, from;
        String revisionTableName = storeID;
        batchSize = batchSize == Integer.MAX_VALUE ? batchSize - 1 : batchSize;

        // check existence of record for the given content item id
        try {
            boolean moreChanges = false;
            long lastRowRevision = 0;
            // set max rows one more than the batchsize to be able to see if there are other changes for
            // the given revision beyond the batchsize
            List<RevisionBean> revisionBeans = dbManager.getRevisions(revisionTableName, revision,
                Long.MAX_VALUE, batchSize + 1, 0);

            if (revisionBeans.isEmpty()) {
                log.debug("There is no changes in store: {}, after revision: {}", storeID, revision);
                return new ChangeSetImpl(Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE,
                        new ArrayList<String>());
            }

            from = revisionBeans.get(0).getRevision();
            // if the number of changes >= batchsize + 1
            if (revisionBeans.size() == (batchSize + 1)) {
                log.debug("There are changes more than the given batch size: {}", batchSize);
                lastRowRevision = revisionBeans.get(batchSize).getRevision();
                long nextToLastRowRevision = revisionBeans.get(batchSize - 1).getRevision();
                // if we are in the middle of a revision, set the moreChanges flag to add all changes in
                // that revision to changedUris
                if (lastRowRevision == nextToLastRowRevision) {
                    log.debug("The changes for the revision: {} exceeds the batch size", revision);
                    moreChanges = true;
                }
                for (int i = 0; i < revisionBeans.size() - 1; i++) {
                    changedUris.add(revisionBeans.get(i).getID());
                }
                to = revisionBeans.get(revisionBeans.size() - 2).getRevision();
                log.info("Changes have been fetched for the initial batch size");
            } else {
                for (RevisionBean rb : revisionBeans) {
                    changedUris.add(rb.getID());
                }
                to = revisionBeans.get(revisionBeans.size() - 1).getRevision();
                log.info("There are {} changes in total", revisionBeans.size());
            }

            if (moreChanges) {
                revisionBeans = dbManager.getRevisions(revisionTableName, revision, lastRowRevision,
                    Integer.MAX_VALUE, batchSize);
                for (RevisionBean rb : revisionBeans) {
                    changedUris.add(rb.getID());
                }
                log.info("Changes exceeding the batch size have been fetched. Total fetched changes: {}",
                    changedUris.size());
            }

            long epoch = getEpoch(storeID);
            return new ChangeSetImpl(epoch, from, to, changedUris);
        } finally {
            dbManager.closeConnection(con);
        }
    }

    @Override
    public long getEpoch(String storeID) throws RevisionManagerException {
        return dbManager.getEpoch(storeID);
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

    private long updateEpoch(String storeID, boolean initialize) throws RevisionManagerException {
        long newEpoch = System.currentTimeMillis();
        if (!initialize) {
            // truncate the revision table
            dbManager.truncateTable(storeID);
            dbManager.updateEpochEntry(storeID, newEpoch);
        } else {
            try {
                newEpoch = dbManager.getEpoch(storeID);
            } catch (RevisionManagerException e) {
                if (e.getCause() == null) {
                    dbManager.createEpochEntry(storeID, newEpoch);
                } else {
                    throw e;
                }
            }
        }

        log.info("Epoch update has been completed for the Store: {}", storeID);
        return newEpoch;
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
     * Returns a name for the index to be created on the <b>revision</b> of the given table
     * 
     * @param tableName
     *            name of the revision table on which the index will be created
     * @return
     */
    private String getRevisionIndexName(String tableName) {
        return tableName + "_revisionindex";
    }
}

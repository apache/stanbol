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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.contenthub.servicesapi.store.ChangeSet;
import org.apache.stanbol.contenthub.servicesapi.store.Store;
import org.apache.stanbol.contenthub.servicesapi.store.StoreException;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class aims to manage the revisions regarding to the {@link ContentItem}s stored in a {@link Store}.
 * The revisions are kept in a single table in the scope of Apache Derby database. The system time is set as
 * the new revision of the {@link ContentItem} when the {@link #updateRevision(String)} method is called.
 * 
 * @author suat
 * 
 */
@Component(immediate = true)
@Service(value = FileRevisionManager.class)
public class FileRevisionManager {
    private static Logger log = LoggerFactory.getLogger(FileRevisionManager.class);

    public static final String REVISION_TABLE_NAME = "content_item_revisions";

    private static final String SELECT_REVISION = "SELECT id,revision FROM " + REVISION_TABLE_NAME
                                                  + " content_item_revision WHERE id = ?";

    private static final String INSERT_REVISION = "INSERT INTO " + REVISION_TABLE_NAME
                                                  + " (id, revision) VALUES (?,?)";

    private static final String UPDATE_REVISION = "UPDATE " + REVISION_TABLE_NAME
                                                  + " SET revision=? WHERE id=?";

    private static final String SELECT_CHANGES = "SELECT id, revision FROM " + REVISION_TABLE_NAME
                                                 + " WHERE revision > ? ORDER BY revision ASC OFFSET ? ROWS";

    @Reference
    FileStoreDBManager dbManager;

    /**
     * Updates revision of the {@link ContentItem} specified with the <code>contentItemID</code> parameter.
     * The system time set as the new revision number by {@link System#currentTimeMillis()}.
     * 
     * @param contentItemID
     *            ID of the {@link ContentItem} of which revision to be updated
     * @throws StoreException
     */
    public void updateRevision(String contentItemID) throws StoreException {
        // get connection
        Connection con = dbManager.getConnection();

        // check existence of record for the given content item id
        PreparedStatement ps = null;
        ResultSet rs = null;
        boolean recordExist = false;
        try {
            ps = con.prepareStatement(SELECT_REVISION);
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
                log.debug("New revision: {} for the content item: {} will be added", newRevision,
                    contentItemID);
                ps = con.prepareStatement(INSERT_REVISION);
                ps.setString(1, contentItemID);
                ps.setLong(2, newRevision);
            } else {
                log.debug("New revision: {} for the content item: {} will be updated", newRevision,
                    contentItemID);
                ps = con.prepareStatement(UPDATE_REVISION);
                ps.setString(2, contentItemID);
                ps.setLong(1, newRevision);
            }
            int updatedRecordNum = ps.executeUpdate();
            // exactly one record should be affected
            if (updatedRecordNum != 1) {
                log.warn("Unexpected number of updated records: {}, should be 1", updatedRecordNum);
            }
        } catch (SQLException e) {
            log.error("Failed to update revision", e);
            throw new StoreException("Failed to update revision", e);
        } finally {
            dbManager.closeStatement(ps);
            dbManager.closeConnection(con);
        }
    }

    /**
     * Returns the updates after the given revision number. It returns at most <code>batchSize</code> number
     * of changes within the returned {@link ChangeSet} object starting from the given <code>offset</code>.
     * This method does not necessarily return the all changes for the given revision number. If there are
     * more changes than the batch size for the given version, only batch size number of changes are returned.
     * Other changes must be obtained by giving the same revision and the suitable offset.
     * 
     * @param revision
     *            Starting revision number for the returned {@link ChangeSet}
     * @param offset
     *            Starting number of the changes as of the given <code>revision</code>.
     * @param batchSize
     *            Maximum number of changes to be returned
     * @return a {@link ChangeSet} including the changes in the store
     * @throws StoreException
     */
    public ChangeSet getChanges(long revision, int offset, int batchSize) throws StoreException {
        ChangeSetImpl changes = new ChangeSetImpl();

        // get connection
        Connection con = dbManager.getConnection();

        // check existence of record for the given content item id
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement(SELECT_CHANGES, ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);
            ps.setLong(1, revision);
            ps.setLong(2, offset);
            ps.setMaxRows(batchSize);
            rs = ps.executeQuery();

            // set changed uris
            Set<UriRef> changedUris = new LinkedHashSet<UriRef>();
            while (rs.next()) {
                changedUris.add(new UriRef(rs.getString(1)));
            }
            changes.setChangedUris(changedUris);
            // set minimum and maximum revision numbers of the change set
            rs.first();
            changes.setFrom(rs.getLong(2));
            rs.last();
            changes.setTo(rs.getLong(2));

        } catch (SQLException e) {
            log.error("Failed to get changes", e);
            throw new StoreException("Failed to get changes", e);
        } finally {
            dbManager.closeResultSet(rs);
            dbManager.closeStatement(ps);
            dbManager.closeConnection(con);
        }
        return changes;
    }

}

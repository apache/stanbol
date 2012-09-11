package org.apache.stanbol.commons.semanticindex.store.revisionmanager;

import org.apache.stanbol.commons.semanticindex.store.ChangeSet;
import org.apache.stanbol.commons.semanticindex.store.Store;

/**
 * <p>
 * This interface aims to provide functionalities to manage revisions of items managed by {@link Store}s and
 * the epochs of Stores.
 * </p>
 * <p>
 * All of the methods of this interface take a {@code storeID} identifying the Store instance. In OSGi based
 * Store implementations, it is suggested to use <b>service.pid</b> property of the Store.
 * 
 * @author suat
 * 
 */
public interface RevisionManager {

    /**
     * Initialize the revision management infrastructure for the {@link Store} specified by the
     * {@code storeID}
     * 
     * @param storeID
     *            the identifier of the {@link Store}
     * @throws RevisionManagerException
     */
    void registerStore(String storeID) throws RevisionManagerException;

    /**
     * Clears all resources related with the {@link Store} instance specified by the {@code storeID} from the
     * {@link RevisionManager} e.g revisions regarding the items in the Store and epoch records of the Store.
     * 
     * @param storeID
     *            the identifier of the {@link Store}
     * @throws RevisionManagerException
     */
    void unregisterStore(String storeID) throws RevisionManagerException;

    /**
     * Updates the revision of the item represented by {@code itemID} which is managed by the given
     * {@link Store}. If there does not exist a revision for the given item a new is created.
     * 
     * @param storeID
     *            the identifier of the keeper {@link Store} instance for the given item
     * @param itemID
     *            identifier of the item of which revision will be update/created.
     * @throws RevisionManagerException
     */
    long updateRevision(String storeID, String itemID) throws RevisionManagerException;

    /**
     * Gets changes from the given store according to specified {@code revision} and {@code batchSize}.
     * 
     * @param storeID
     *            the identifier of the {@link Store}from which the changes will be obtained
     * @param revision
     *            the <b>exclusive</b> bottom border for the revisions to be obtained. Obtained revisions will
     *            be higher than this value.
     * @param batchSize
     *            the maximum number of changes to be obtained. However, this is not a strict value. Different
     *            implementations may return more values than the number specified with this parameter. It is
     *            suggested to read the implementation specific documentation.
     * @return a {@link ChangeSet} object if there is any changes, otherwise {@code null}
     * @throws RevisionManagerException
     */
    ChangeSet getChanges(String storeID, long revision, int batchSize) throws RevisionManagerException;

    /**
     * Gets the epoch of the given {@link Store}
     * 
     * @param storeID
     *            the identifier of the {@link Store} instance of which epoch value will be returned
     * @return
     * @throws RevisionManagerException
     */
    long getEpoch(String storeID) throws RevisionManagerException;

    /**
     * Updates the epoch for {@link Store} instance speficied by the {@code storeID}. If there does not exist
     * an epoch for the specified {@link Store}, a new one is created.
     * 
     * @param storeID
     *            {@link Store} instance for which the epoch will be updated/created.
     * @return the updated epoch
     * @throws RevisionManagerException
     */
    long updateEpoch(String storeID) throws RevisionManagerException;
}

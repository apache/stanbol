package org.apache.stanbol.commons.semanticindex.store.revisionmanager;

import org.apache.stanbol.commons.semanticindex.store.ChangeSet;
import org.apache.stanbol.commons.semanticindex.store.Store;

/**
 * This interface aims to provide functionalities to manage revisions of items managed by {@link Store}s and
 * the epochs of Stores.
 * 
 * @author suat
 * 
 */
public interface RevisionManager<Item> {

    /**
     * <p>
     * This methods allows to create new revisions for the already existing items managed in the scope of the
     * given {@link Store} instance. This is situation is expected to occur when initializing the Store. For
     * instance, when a Store is initialized with an initial set of items which may be obtained from various
     * sources such as an RDF graph or a content management system, new revisions for those items should be
     * created in this method.
     * </p>
     * <p>
     * So, this method is expected to be called during initialization of {@link Store} instances e.g in @Activate
     * methods of OSGi based Store implementations.
     * 
     * @param store
     *            revisions will be created for the initial items of this {@link Store} instance
     * @throws RevisionManagerException
     */
    void initializeRevisions(Store<Item> store) throws RevisionManagerException;

    /**
     * Clears all resources from the given {@link Store} from the {@link RevisionManager} e.g revisions
     * regarding the items in the Store and epoch records of the Store.
     * 
     * @param store
     *            the {@link Store} instance for which the resource will be deleted from the
     *            {@link RevisionManager}
     * @throws RevisionManagerException
     */
    void clearRevisions(Store<Item> store) throws RevisionManagerException;

    /**
     * Updates the revision of the item represented by {@code itemID} which is managed by the given
     * {@link Store}. If there does not exist a revision for the given item a new is created.
     * 
     * @param store
     *            the keeper {@link Store} instance for the given item
     * @param itemID
     *            identifier of the item of which revision will be update/created.
     * @throws RevisionManagerException
     */
    void updateRevision(Store<Item> store, String itemID) throws RevisionManagerException;

    /**
     * Gets changes from the given store according to specified {@code revision} and {@code batchSize}.
     * 
     * @param store
     *            the store from which the changes will be obtained
     * @param revision
     *            the <b>exclusive</b> bottom border for the revisions to be obtained. Obtained revisions will
     *            be higher than this value.
     * @param batchSize
     *            the maximum number of changes to be obtained. However, this is not a strict value. Different
     *            implementations may return more values than the number specified with this parameter
     * @return a {@link ChangeSet} object if there is any changes, otherwise {@code null}
     * @throws RevisionManagerException
     */
    ChangeSet<Item> getChanges(Store<Item> store, long revision, int batchSize) throws RevisionManagerException;

    /**
     * Gets the epoch of the given {@link Store}
     * 
     * @param store
     *            {@link Store} instance of which epoch value will be returned
     * @return
     * @throws RevisionManagerException
     */
    long getEpoch(Store<Item> store) throws RevisionManagerException;

    /**
     * Updates the epoch for the given {@link Store}. If there does not exist an epoch for the specified
     * {@link Store}, a new one is created.
     * 
     * @param store
     *            {@link Store} instance for which the epoch will be updated/created.
     * @return the updated epoch
     * @throws RevisionManagerException
     */
    long updateEpoch(Store<Item> store) throws RevisionManagerException;
}

package org.apache.stanbol.commons.semanticindex.store;

import java.util.Map;

import org.apache.stanbol.commons.semanticindex.index.SemanticIndex;

/**
 * Minimal interface required by the Semantic Index as an indexing source.
 * <p>
 * 
 * This interface provides metadata, read-only access to Items as well as {@link ChangeSet changes} based on
 * epoch and revision.
 * <p>
 * An <b>epoch</b> describes a span over time within that changes to Items will generate new revisions. New
 * epoches are usually triggered if the whole dataset of an indexing source is replace (e.g. if a new
 * data-dump is applied). Changes of Items (create, update, delete) will not trigger a new epoch but increase
 * the revision.
 * <p>
 * This is intended to be used by {@link SemanticIndex}s as follows:
 * <ul>
 * <li>In case of a new epoch the semantic index needs to re-index all items of an indexing source. This is
 * done by calling {@link #changes(long, long, int)} with the new {@link #getEpoch() epoch} and
 * {@link Long#MIN_VALUE} as revision.
 * <li>Revision changes are applied incrementally by the {@link SemanticIndex} calling
 * {@link #changes(long, long, int)} with the last processed epoch and revision. If there are changes the
 * IndexingSource will return a non-empty {@link ChangeSet}. In case of a new epoch an {@link EpochException}
 * will be throw - indicating the need of a complete re-index to the SemanticIndex.
 * </ul>
 * 
 * @param <Item>
 */
public interface IndexingSource<Item> {

    /**
     * The property used for {@link #getName()}
     */
    String PROPERTY_NAME = "org.apache.stanbol.indexingsource.name";
    String PROPERTY_DESCRIPTION = "org.apache.stanbol.indexingsource.description";
    String PROPERTY_ITEM_TYPE = "org.apache.stanbol.indexingsource.itemtype";

    /**
     * The name of the IndexingSource
     * 
     * @return the name. MUST NOT be <code>null</code> nor empty.
     */
    String getName();

    /**
     * An optional human readable description that provides some additional information about this
     * IndexingSource
     * 
     * @return the description
     */
    String getDescription();

    /**
     * Getter for the type of Items managed by this Store
     * 
     * @return
     */
    Class<Item> getItemType();

    /**
     * Read-only map with additional properties about this IndexingSource
     * 
     * @return an read-only map with additional metadata available for this indexing source.
     */
    Map<String,Object> getProperties();

    /**
     * Gets a Item by uri, null if non-existing
     * 
     * @param uri
     *            the uri of the item
     * @return the item or <code>null</code> if not present
     * @throws StoreException
     *             on any error while retrieving the item
     */
    Item get(String uri) throws StoreException;

    /**
     * The current Epoch used by this indexing source. Newer epochs need to use hither values.
     * <p>
     * An change in the Epoch indicates that data provided by an indexing source may have completely changed.
     * All revisions of a previous epoch are invalid. {@link SemanticIndex}s that uses an indexing source are
     * required to start indexing from scratch.
     * 
     * @return the active Epoch
     */
    long getEpoch() throws StoreException;

    /**
     * Requests the next <code>batchSize</code> changes starting from <code>revision</code> in the context of
     * an <code>epoch</code>.
     * <p>
     * The following actions are expected based on the result of this Method:
     * <ul>
     * <li>An empty {@link ChangeSet} indicated that there are no changes. Typically the caller will use the
     * same <code>epoch:revision</code> for an other call at an later time
     * <li>An non-empty {@link ChangeSet} indicates that there are changes. Callers are expected to process
     * those {@link ChangeSet} and to repeatedly call {@link #changes(long, long, int)} with
     * {@link ChangeSet#toRevision()} until an empty {@link ChangeSet} is returned
     * <li>if the {@link #getEpoch() active Epoch} of the indexing source has changed an
     * {@link EpochException} is thrown. This indicating that a complete re-indexing of all Entities is
     * required.
     * <li> {@link StoreException} should trigger an other attempt at a later time
     * </ul>
     * 
     * @param epoch
     *            The epoch of the parsed revision
     * @param revision
     *            Starting revision number for the returned {@link ChangeSet}
     * @param batchSize
     *            Maximum number of changes to be returned. The returned {@link ChangeSet} will provides
     *            information about the revision range for included changes.
     * @return the {@link ChangeSet} with a maximum of <code>batchSize</code> changes
     * @throws StoreException
     *             On any error while accessing the store.
     * @throws EpochException
     *             If the Epoch used by this IndexingSource is different of the epoch parsed in the request
     * @see ChangeSet
     */
    ChangeSet changes(long epoch, long revision, int batchSize) throws StoreException, EpochException;
}

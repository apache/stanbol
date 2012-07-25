package org.apache.stanbol.commons.semanticindex.store;


/**
 * Minimal interface required by the Semantic Index as an indexing source.
 *
 * @param <Item>
 */
public interface IndexingSource<Item> {

	/**
	 * Getter for the type of Items managed by this Store
	 * @return
	 */
	Class<Item> getItemType();

	/** 
     * Gets a Item by uri, null if non-existing 
     * @param uri the uri of the item
     * @return the item or <code>null</code> if not present
     * @throws StoreException on any error while retrieving the item
     */
    Item get(String uri) throws StoreException;

    /**
     * Requests the next <code>batchSize</code> changes starting from <code>revision</code>. If there are no
     * more revisions that a {@link ChangeSet} with an empty {@link ChangeSet#changed()} set. There can be
     * more changes in the results than the given <code>batchSize</code> not to return a subset of changes
     * regarding a specific revision. For instance, if the batch size is 5, given revision is 9 and there 15
     * changes regarding revision 10. As a result, there will be 10 changed items in the returned change set.
     * 
     * @param revision
     *            Starting revision number for the returned {@link ChangeSet}
     * @param batchSize
     *            Maximum number of changes to be returned
     * @return the {@link ChangeSet} with a maximum of <code>batchSize</code> changes
     * @throws StoreException
     *             On any error while accessing the store.
     * @see ChangeSet
     */
    ChangeSet<Item> changes(long revision, int batchSize) throws StoreException;
}

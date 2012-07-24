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
package org.apache.stanbol.commons.semanticindex.store;

/**
 * Interface to store and retrieve Items instances persistently.
 */
public interface Store<Item> {

	/**
	 * Getter for the type of Items managed by this Store
	 * @return
	 */
	Class<Item> getItemType();
	
	/**
	 * Removes an item with the given uri from the  store
	 * @param uri
	 * @return
	 * @throws StoreException
	 */
	Item remove(String uri) throws StoreException;

    /**
     * Stores supplied item and return its uri, which is assigned by the store if not defined
     * yet.
     * 
     * If the {@link ContentItem} already exists, it is overwritten.
     * @param item the item to store
     * @param the URI of the stored item
     * @throws StoreException on any error while storing the item
     */
    String put(Item item) throws StoreException;

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

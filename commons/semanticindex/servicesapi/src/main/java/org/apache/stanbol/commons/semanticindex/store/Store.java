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
 * This extends the {@link IndexingSource} interface to support
 * full CRUD operations.
 */
public interface Store<Item> extends IndexingSource<Item>{

	
	/**
	 * Removes an item with the given uri from the  store
	 * @param uri
	 * @return
	 * @throws StoreException
	 */
	Item remove(String uri) throws StoreException;

    /**
     * Stores supplied item and return its uri, which is assigned by the store if not defined
     * yet.<p>
     * 
     * If an Item already exists it is replaced with the parsed version.
     * @param item the item to store
     * @param the URI of the stored item
     * @throws StoreException on any error while storing the item
     */
    String put(Item item) throws StoreException;

    /**
     * Stores all supplied items and returns it uris, which is assigned by the store if not defined
     * yet.<p>
     * If an Item already exists it is replaced with the parsed version.
     * @param item
     * @return the ids of the stored Items
     * @throws StoreException on any error while storing the item
     */
    Iterable<String> put(Iterable<Item> items) throws StoreException ;
    /**
     * Removes all items with the parsed uris. <p>
     * <i>NOTE:</i> This method does NOT return the removed Items to
     * avoid loading a lot of data when deleting a big about of
     * Items. Users that do need the data for removed Items should use
     * {@link #remove(String)} instead.
     * @param uris the uris of the items to delete
     * @throws StoreException on any error while removing the items
     */
    void remove(Iterable<String> uris) throws StoreException ;
    /**
     * Clears the store by removing all items
     * @throws StoreException on any error while removing all items
     */
    void removeAll() throws StoreException ;
}

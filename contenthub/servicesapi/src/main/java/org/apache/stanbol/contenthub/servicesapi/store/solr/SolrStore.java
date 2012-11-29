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

package org.apache.stanbol.contenthub.servicesapi.store.solr;

import java.util.List;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.contenthub.servicesapi.store.Store;
import org.apache.stanbol.contenthub.servicesapi.store.StoreException;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;

/**
 * {@link Store} interface for Solr.
 * 
 * @author anil.sinaci
 * @author meric.taze
 * 
 */
public interface SolrStore extends Store {

    public static final UriRef TITLE_URI = new UriRef("org.apache.stanbol.contenthub.store.solr.title");

    /**
     * Creates a {@link ContentItem} with the given parameters. Created {@link ContentItem} is not persisted,
     * this function just creates the object.
     * 
     * @param content
     *            The content itself.
     * @param id
     *            The unique ID for the item. If it is null, {@link SolrStore} should assign a unique ID for
     *            this item.
     * @param title
     *            The title for the content item.
     * @param contentType
     *            The mimeType of the content.
     * @return Created {@link ContentItem}.
     * @throws StoreException
     */
    ContentItem create(byte[] content, String id, String title, String contentType) throws StoreException;

    /**
     * Sends the {@link ContentItem} to the {@link EnhancementJobManager} to enhance the content. Afterwards
     * saves the item in the default Solr core of the Contenthub.
     * 
     * @param ci
     *            The {@link ContentItem} to be enhanced and saved.
     * @param chain
     *            name of a particular {@link Chain} in which the enhancement engines are ordered according to
     *            a specific use case or need
     * @return The unique ID of the {@link ContentItem}.
     * @throws StoreException
     */
    String enhanceAndPut(ContentItem ci, String chain) throws StoreException;

    /**
     * Sends the {@link ContentItem} to the {@link EnhancementJobManager} to enhance the content. Afterwards
     * saves the item in the Solr core corresponding to the given <code>indexName</code>.
     * 
     * @param ci
     *            The {@link ContentItem} to be enhanced and saved
     * @param indexName
     *            LDPath program name (name of the Solr core/index) to obtain the corresponding Solr core to
     *            store the content item
     * @param chain
     *            name of a particular {@link Chain} in which the enhancement engines are ordered according to
     *            a specific use case or need
     * @return The unique ID of the {@link ContentItem}.
     * @throws StoreException
     */
    String enhanceAndPut(ContentItem ci, String indexName, String chain) throws StoreException;

    /**
     * Stores the passed {@link ContentItem} in the Solr core corresponding to the specified
     * <code>indexName</code>. If <code>null</code> is passed as the LDPath program name (index name), the
     * default Solr core of Contenthub is used.
     * 
     * @param ci
     *            {@link ContentItem} to be stored
     * @param indexName
     *            LDPath program name (name of the Solr core/index) to obtain the corresponding Solr core to
     *            store the content item
     * @return The unique ID of the {@link ContentItem}.
     * @throws StoreException
     */
    String put(ContentItem ci, String indexName) throws StoreException;

    /**
     * Retrieves the {@link ContentItem} from the Solr core corresponding to the specified
     * <code>indexName</code>. If <code>null</code> is passed as the LDPath program name (index name), the
     * default Solr core of Contenthub is used.
     * 
     * @param id
     *            The ID of {@link ContentItem} to be retrieved.
     * @param indexName
     *            LDPath program name (name of the Solr core/index) to obtain the corresponding Solr core from
     *            which the content item will be retrieved
     * @return {@link ContentItem} having the specified id
     * @throws StoreException
     */
    ContentItem get(String id, String indexName) throws StoreException;

    /**
     * Deletes the {@link ContentItem} from the default Solr core/index of Contenthub.
     * 
     * @param id
     *            The ID of the item to be deleted.
     */
    void deleteById(String id) throws StoreException;

    /**
     * Deletes the {@link ContentItem} from the default Solr core corresponding to the given
     * <code>indexName</code> of the Contenthub.
     * 
     * @param id
     *            The ID of the item to be deleted.
     * @param indexName
     *            LDPath program name (name of the Solr core/index) to obtain the corresponding Solr core from
     *            which the content item will be deleted
     * @throws StoreException
     */
    void deleteById(String id, String indexName) throws StoreException;

    /**
     * Deletes the {@link ContentItem}s from the default Solr core of Contenthub.
     * 
     * @param id
     *            The list of IDs of the items to be deleted.
     */
    void deleteById(List<String> idList) throws StoreException;

    /**
     * Deletes the {@link ContentItem}s from the Solr core corresponding to the given <code>indexName</code>.
     * 
     * @param idList
     *            The list of IDs of the items to be deleted.
     * @param indexName
     *            LDPath program name (name of the Solr core/index) to obtain the corresponding Solr core from
     *            which the content items will be deleted
     */
    void deleteById(List<String> idList, String indexName) throws StoreException;
}

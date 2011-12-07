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

package org.apache.stanbol.contenthub.servicesapi.store;

import java.util.List;
import java.util.Map;

import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.Store;

/**
 * {@link Store} interface for Solr.
 * 
 * @author anil.sinaci
 * @author meric.taze
 * 
 */
public interface SolrStore extends Store {

    /**
     * Creates a {@link SolrContentItem} with the given parameters. Created {@link SolrContentItem} is not
     * persisted, this function just creates the object.
     * 
     * @param id
     *            The unique ID for the item. If it is null, {@link SolrStore} should assign a unique ID for
     *            this item.
     * @param title
     *            The title for the content item.
     * @param content
     *            The content itself.
     * @param contentType
     *            The mimeType of the content.
     * @param constraints
     *            The facets in <code>key:[value1,value2]</code> pairs.
     * @return Created {@link SolrContentItem}.
     */
    SolrContentItem create(String id,
                           String title,
                           byte[] content,
                           String contentType,
                           Map<String,List<Object>> constraints);

    /**
     * Sends the {@link SolrContentItem} to the {@link EnhancementJobManager} to enhance the content.
     * Afterwards saves the item to Solr.
     * 
     * @param sci
     *            The {@link SolrContentItem} to be enhanced and saved.
     * @return The unique ID of the {@link SolrContentItem}.
     */
    String enhanceAndPut(SolrContentItem sci);

    /**
     * Deletes the {@link ContentItem} from the {@link SolrStore}.
     * 
     * @param id
     *            The ID of the item to be deleted.
     */
    void deleteById(String id);

    /**
     * Deletes the {@link ContentItem}s from the {@link SolrStore}.
     * 
     * @param id
     *            The list of IDs of the items to be deleted.
     */
    void deleteById(List<String> id);
}

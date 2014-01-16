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
package org.apache.stanbol.enhancer.engines.lucenefstlinking.cache;

import org.apache.lucene.document.Document;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;

/**
 * Manages a Cache for Entity data required for EntityLinking. Cached entity
 * data are represented by Lucene {@link Document}s.
 * <p>
 * This is expected to manage a single {@link EntityCache} for the current
 * version of the Lucene index. A 'new' version is expected as soon as
 * {@link #getCache(Object)} is called for a different version. In that case
 * the current {@link EntityCache} should be cleared and a new empty one
 * needs to be created. The new Cache might get autowarmed (if supported and
 * configured)
 * <p>
 * Implementations need to wait with clearing/closing outdated {@link EntityCache}
 * instances until the old version is no longer used 
 * ({@link RefCounted#close()} is called).
 */
public interface EntityCacheManager {
    
    /**
     * Getter for a reference counting instance of the {@link EntityCache}.
     * Callers need to ensure that {@link RefCounted#decref()} is called when the
     * do no longer need the obtained DocumentCache instance.
     * @param version the version object. Typically the current 
     * {@link SolrIndexSearcher} instance can be used as version object as a new 
     * cache instance should be created if a new index searcher was opened by 
     * the SolrCore.
     * @return A counting reference to the EntityCache
     */
    RefCounted<EntityCache> getCache(Object version);

    /**
     * Called if the EntityCacheManager is no longer used
     */
    void close();
    
}

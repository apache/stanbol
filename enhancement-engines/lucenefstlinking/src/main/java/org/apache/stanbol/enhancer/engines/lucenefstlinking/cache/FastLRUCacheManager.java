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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.lucene.document.Document;
import org.apache.solr.search.CacheRegenerator;
import org.apache.solr.search.FastLRUCache;
import org.apache.solr.search.SolrCache;
import org.apache.solr.util.RefCounted;

/**
 * Implementation of the {@link EntityCacheManager} based on the Solr
 * {@link FastLRUCache} implementation
 * 
 * @author Rupert Westenthaler
 *
 */
public class FastLRUCacheManager implements EntityCacheManager {

    RefCounted<EntityCache> current;
    private final CacheRegenerator regenerator;
    private final Map<String,String> config;
    
    /**
     * Creates a cache manager instance with the parsed maximum size and no 
     * support for autowarming
     * @param size the maximum size or values <= 0 to use the default size
     */
    public FastLRUCacheManager(int size){
        this(size,0,null);
    }
    /**
     * Creates a cache manager instance with the parsed maximum size and support
     * for autowarming.
     * @param size the maximum size
     * @param autowarmCount the number of documents added to the new cache based
     * on entries in an old version
     * @param regenerator the regenerator instance used for autowarming
     */
    public FastLRUCacheManager(int size, int autowarmCount, CacheRegenerator regenerator){
        Map<String,String> config = new HashMap<String,String>();
        config.put("name", "Tagging Document Cache");
        if(size > 0){
            config.put("size",Integer.toString(size));
        }
        if(regenerator != null){
            config.put("autowarmCount",Integer.toString(autowarmCount));
        }
        this.config = Collections.unmodifiableMap(config);
        this.regenerator = regenerator;
    }
    
    
    @Override
    public RefCounted<EntityCache> getCache(Object version) {
        if(current == null || !current.get().getVersion().equals(version)){
            if(current != null){
                //the the old one as outdated!
                ((RefCountedImpl)current).setOutdated();
            }
            //create a new cache
            SolrCache<Integer,Document> cache = new FastLRUCache<Integer,Document>();
            cache.init(config, null, regenerator);
            current = new RefCountedImpl(new SolrEntityCache(version, cache));
        }
        current.incref();
        return current;
    }

    /**
     * {@link RefCounted} implementation that ensures that outdated caches are
     * cleared and closed as soon as they are no longer in use.
     * 
     * @author Rupert Westenthaler
     *
     */
    protected class RefCountedImpl extends RefCounted<EntityCache>{
        
        public RefCountedImpl(SolrEntityCache resource) {
            super(resource);
        }

        private boolean outdated;

        /**
         * Used by the manager implementation to set the RefCounted EntityCache
         * as outdated
         */
        protected void setOutdated() {
            outdated = true;
        }

        /**
         * clears the cache if outdated
         */
        protected void close(){
            if(outdated){
                ((SolrEntityCache)get()).close();
            }
        }

    }
    
}

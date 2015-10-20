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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.lucene.document.Document;
import org.apache.solr.search.CacheRegenerator;
import org.apache.solr.search.FastLRUCache;
import org.apache.solr.search.SolrCache;
import org.apache.solr.util.RefCounted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the {@link EntityCacheManager} based on the Solr
 * {@link FastLRUCache} implementation
 * 
 * @author Rupert Westenthaler
 *
 */
public class FastLRUCacheManager implements EntityCacheManager {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
    RefCounted<EntityCache> current;
    private final CacheRegenerator regenerator;
    private final Map<String,String> config;
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    
    
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
        log.debug("> create {} (size: {}| autowarmCount: {}| regenerator: {})",
            new Object[]{getClass().getSimpleName(),size,autowarmCount,regenerator});
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
        lock.readLock().lock();
        try {
            if(current != null && current.get().getVersion().equals(version)){
                current.incref(); //this increase is for the holder of the returned instance
                log.debug(" > increase RefCount for EntityCache for version {} to {}", 
                        version, current.getRefcount());
                return current;
            }
        } finally {
            lock.readLock().unlock();
        }
        //still here ... looks like we need to build a new one
        lock.writeLock().lock();
        try {
            //check again ... an other thread might have already built the cache
            //for the requested version
            if(current == null || !current.get().getVersion().equals(version)){
                if(current != null){
                	log.debug(" > invalidate EntityCache for version {}", current.get().getVersion());
                	//remove the reference to the old instance. This will allow to
                	//destroy the old cache as soon as it is no longer used
                	current.decref(); 
                	log.debug("  ... {} remaining users for invalidated Cache", current.getRefcount());
                	current = null;
                }
                //create a new cache
                log.debug(" > create EntityCache for version {}", version);
                SolrCache<Integer,Document> cache = new FastLRUCache<Integer,Document>();
                cache.init(config, null, regenerator);
                current = new RefCountedImpl(new SolrEntityCache(version, cache));
                //add a reference to the new cache by this class. This will be removed
                //as soon as the instance is outdated
                current.incref(); 
            }
            current.incref(); //this increase is for the holder of the returned instance
            log.debug(" > increase RefCount for EntityCache for version {} to {}", 
            		version, current.getRefcount());
            return current;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void close() {
        lock.writeLock().lock();
        try {
        	if(current != null){
            	    Object version = log.isDebugEnabled() ? current.get().getVersion() : null;
            		current.decref();
                    log.debug(" > close EntityCache for version {} (remaining refCount: {})", 
                        version , current.getRefcount());
            		current = null;
        	}
        } finally{ 
            lock.writeLock().unlock();
        }
    }
    
    @Override
    protected void finalize() {
    	if(current != null){
    		log.warn("[finalize] EntityCache Manager was not closed. This can "
    				+ "cause Memory Leaks as Cached Entities will be kept in " 
    				+ "Memory until finalization!");
    	}
    	close();
    }
    
    /**
     * {@link RefCounted} implementation that closes the {@link SolrEntityCache}
     * when {@link #close()} is called by the supoer implementation.
     * 
     * @author Rupert Westenthaler
     *
     */
    protected class RefCountedImpl extends RefCounted<EntityCache>{
        
        public RefCountedImpl(SolrEntityCache resource) {
            super(resource);
        }

        @Override
        public void decref() {
        	super.decref();
        	if(log.isDebugEnabled()){
	            log.debug(" > decrease RefCount for EntityCache for version {} to {}", 
	            		get().getVersion(), current.getRefcount());
        	}
        }
        /**
         * closes the {@link SolrEntityCache}
         */
        protected void close(){
        	if(log.isDebugEnabled()){
        		log.debug(" > close EntityCache for version {}", 
        				current.get().getVersion());
        	}
            ((SolrEntityCache)get()).close();
        }

    }
    
}

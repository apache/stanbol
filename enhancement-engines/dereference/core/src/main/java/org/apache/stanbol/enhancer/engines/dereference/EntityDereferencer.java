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
package org.apache.stanbol.enhancer.engines.dereference;

import java.util.ConcurrentModificationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.commons.stanboltools.offline.OfflineMode;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;

/**
 * Interface used by the {@link EntityDereferenceEngine} to dereference
 * Entities
 * 
 * @author Rupert Westenthaler
 *
 */
public interface EntityDereferencer {

    /**
     * If this EntityDereferences can dereference Entities when in 
     * {@link OfflineMode}. This method is expected to only return <code>false</code>
     * when an implementation can not dereference any Entities when in offline
     * mode. If some (e.g. locally cached) Entities can be dereferenced
     * the dereferences should return <code>true<code> and just ignore calles
     * for Entities that are not locally available.
     * @return the {@link OfflineMode} status
     */
    boolean supportsOfflineMode();
    
    /**
     * EntityDereferencer can optionally provide an ExecutorService used to
     * dereference Entities. 
     * @return the {@link ExecutorService} or <code>null</code> if not used
     * by this implementation
     */
    ExecutorService getExecutor();

    /**
     * Dereferences the Entity with the parsed {@link UriRef} by copying the
     * data to the parsed graph
     * @param graph the graph to add the dereferenced entity 
     * @param entity the uri of the Entity to dereference
     * @param offlineMode <code>true</code> if {@link OfflineMode} is active.
     * Otherwise <code>false</code>
     * @param writeLock The writeLock for the graph. Dereferences MUST require
     * a <code>{@link Lock#lock() writeLock#lock()}</code>  before adding 
     * dereferenced data to the parsed graph. This is essential for using multiple 
     * threads  to dereference Entities. Failing to do so will cause
     * {@link ConcurrentModificationException}s in this implementations or
     * other components (typically other {@link EnhancementEngine}s) accessing the
     * same graph.
     * @return if the entity was dereferenced
     * @throws DereferenceException on any error while dereferencing the
     * requested Entity
     */
    boolean dereference(UriRef entity, MGraph graph, boolean offlineMode, 
            Lock writeLock) throws DereferenceException;
        
}

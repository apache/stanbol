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
package org.apache.stanbol.entityhub.servicesapi.yard;

import java.util.Collection;


/**
 * Manages the different active Yards and provides the possibility to lookup/
 * create {@link Cache} instances based on already available Yards.
 *
 * @author Rupert Westenthaler
 */
public interface YardManager {

    /**
     * Getter for the IDs of Yards currently managed by this Manager
     * @return the Ids of the currently active Yards
     */
    Collection<String> getYardIDs();
    /**
     * Returns if there is a Yard for the parsed ID
     * @param id the id
     * @return <code>true</code> if a {@link Yard} with the parsed ID is managed
     * by this YardManager.
     */
    boolean isYard(String id);
    /**
     * Getter for the Yard based on the parsed Id
     * @param id the ID
     * @return The Yard or <code>null</code> if no Yard with the parsed ID is
     * active.
     */
    Yard getYard(String id);
    /**
     * Getter for the Cache based on the yard id used to cache the data.<p>
     * If no cache is present for the parsed yard, than a new instance is
     * created by using the {@link ComponentFactory}.
     * @param id the ID of the Yard used to store the data of the cache
     * @return the cache or <code>null</code> if no Yard with the parsed ID
     * is active.
     * @throws YardException on any Error while creating a new Cache for the
     * Yard with the parsed id.
     * TODO: replace YardException by a better one. However I do not like to use
     * the ComponentException because it is a runtime exception and would
     * introduce a dependency to OSGI in the ServicesApi bundle
     */
    Cache getCache(String id);
    /**
     * Returns if a Cache with the requested ID is registered. NOTE that a Cache
     * always shares the same ID with the Yard used to store the cached data.
     * @param id the id of the cache (or the Yard used by the Cache)
     * @return <code>true</code> if a {@link Cache} with the parsed ID is managed
     * by this YardManager.
     */
    boolean isCache(String id);
    /**
     * Getter for the IDs of Caches currently managed by this Manager
     * @return the Ids of the currently active Caches
     */
    Collection<String> getCacheIDs();

}

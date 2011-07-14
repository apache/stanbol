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
package org.apache.stanbol.entityhub.indexing.core;

import org.apache.stanbol.entityhub.servicesapi.model.Representation;

/**
 * Interface used be the {@link RdfIndexer} to check if an entity should be
 * indexes or not.
 * @author Rupert Westenthaler
 *
 */
public interface EntityScoreProvider extends IndexingComponent {

    /**
     * Indicates if the data of the Entity are needed for calculating/providing
     * the score for Entities. If an implementation returns <code>true</code> 
     * {@link #process(Representation)} will be called. If <code>false</code>
     * is returned the {@link #process(String)} is called. <p>
     * Implementors should consider that supporting the String variant will
     * improve indexing speed because there is no need to get the actual
     * Representation for Entities that need not to be indexed.
     * @return if the indexer needs to parse the {@link Representation} for the
     * entity.
     */
    boolean needsData();
    /**
     * Method called to evaluate an entity in case {@link #needsData()} returns
     * <code>false</code>
     * @param id the ID of the entity
     * @return The score or <code>null</code> if no score is available for the
     * parsed Entity. Values <code>&lt; 0</code> indicate that the entity should
     * not be indexed.
     * @throws UnsupportedOperationException if <code>{@link #needsData()}</code>
     * returns <code>true</code>
     */
    Float process(String id) throws UnsupportedOperationException;
    /**
     * Method called to evaluate an entity in case {@link #needsData()} returns
     * <code>true</code>
     * @param entity the {@link Representation} of the entity
     * @return The score or <code>null</code> if no score is available for the
     * parsed Entity. Values <code>&lt; 0</code> indicate that the entity should
     * not be indexed.
     * @throws UnsupportedOperationException if <code>{@link #needsData()}</code>
     * returns <code>false</code>
     */
    Float process(Representation entity) throws UnsupportedOperationException;
}

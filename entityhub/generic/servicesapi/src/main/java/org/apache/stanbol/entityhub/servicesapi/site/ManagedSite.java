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
package org.apache.stanbol.entityhub.servicesapi.site;

import java.util.Collection;

import org.apache.stanbol.entityhub.servicesapi.EntityhubException;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.ManagedEntityState;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;

/**
 * Extends ReferencedSite by create/update/delete functionalities
 * @author Rupert Westenthaler
 *
 */
public interface ManagedSite extends Site {

    /**
     * Stores (create or updates) the parsed representation. 
     * @param representation the representation to be stored/updated
     * @throws ManagedSiteException On any error while performing the operation
     * @throws IllegalArgumentException if <code>null</code> is parsed as
     * Representation.
     */
    void store(Representation representation) throws ManagedSiteException;
    /**
     * Stores (create or updates) the parsed representations. 
     * @param representation the representation to be stored/updated
     * @return The updated entity.
     * @throws ManagedSiteException On any error while performing the operation
     * @throws IllegalArgumentException if <code>null</code> is parsed
     */
    void store(Iterable<Representation> representation) throws ManagedSiteException;
    /**
     * Deletes the Entity with the parsed id. This will delete the entity
     * and all its information including metadata and mappings to other entities
     * form the Entityhub. To mark the Entity as removed use 
     * {@link #setState(String, ManagedEntityState)} with 
     * {@link ManagedEntityState#removed} as second parameter.
     * @param id The id of the Entity to delete
     * @throws EntityhubException On any error while performing the operation
     * @throws IllegalArgumentException if <code>null</code> or an empty String
     * is parsed as id
     */
    void delete(String id) throws ManagedSiteException;
    /**
     * Deletes all Entities and their Mappings from the Entityhub.
     * @throws EntityhubException On any error while performing the operation
     */
    void deleteAll() throws ManagedSiteException;
    
}

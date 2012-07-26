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
package org.apache.stanbol.entityhub.servicesapi;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapper;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapping;
import org.apache.stanbol.entityhub.servicesapi.model.ManagedEntityState;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.site.Site;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;

/**
 * <p>The Entityhub defines an interface that allows to manage Entities. 
 * Entities managed by the Entityhub are often referred by "locally managed
 * Entities" to differentiate them form entities managed by 
 * {@link Site}s.<p>
 * The Entityhub supports full CRUD support for Entities and also allows to 
 * import Entities from Referenced sites.<p>
 * In addition to Entities the Entityhub also allows to work with mappings
 * between Entities of {@link Site}s with locally managed Entities.
 * 
 * @author Rupert Westenthaler
 *
 */
public interface Entityhub {

    String DEFAUTL_ENTITYHUB_PREFIX = "urn:org.apache.stanbol:entityhub";
    /**
     * Protected keys to be used as name for the Entityhub. Such keys MUST NOT
     * be used as {@link Site#getId() id}s for 
     * {@link Site}s. (case insensitive)<p>
     * The protected values are <ul>
     * <li><code>"local"</code>
     * <li><code>"entityhub"</code>
     * </ul>
     */
    Set<String> ENTITYHUB_IDS = Collections.unmodifiableSet(
        new HashSet<String>(Arrays.asList(
            "local","entityhub")));

    /**
     * Getter for the Yard storing the Entities and Mappings managed by this
     * Entityhub.<p>
     * Note that the Yard can be reconfigured without restarting the
     * Entityhub.
     * @return The yard instance used to store the data of the Entityhub
     *  - the EntityhubYard
     */
    Yard getYard();

    /**
     * Getter for the locally managed Entity based on a reference to a 
     * {@link Entity}. If a id of an locally managed Entity is parsed this
     * Entity is returned. In any other case this Method searches if the parsed 
     * reference is mapped to a locally managed Entity and returns this Entity 
     * instead.
     * @param reference the id of any Entity
     * @return the locally managed Entity or <code>null</code> if
     * no symbol for the parsed entity is available
     * @throws EntityhubException On any error while performing the operation
     */
    Entity lookupLocalEntity(String reference) throws EntityhubException;
    /**
     * Getter for the locally managed Entity based on a reference to a 
     * {@link Entity}. If a id of an locally managed Entity is parsed this
     * Entity is returned. In any other case this Method searches if the parsed 
     * reference is mapped to a locally managed Entity and returns this Entity 
     * instead.<p>
     * If <code>create=true</code> this method can imports Entities to the
     * entityhub based on the definition(s) of referenced sites.
     *
     * @param reference the id of the referenced Entity
     * @param create if <code>true</code> the {@link Entityhub} will try to create a
     * new locally managed {@link Entity} by importing an Entity from 
     * referenced sites.
     * @return the locally managed Entity or <code>null</code> if the parsed reference is not
     * known by any referenced sites.
     * @throws IllegalArgumentException If the referenced {@link Entity} was found, no
     * existing {@link EntityMapping} is present, but it is not possible to
     * create a locally managed {@link Entity} for the referenced {@link Entity} 
     * (normally this is because of insufficient/invalid information of the
     * referenced Entity.
     * @throws EntityhubException On any error while performing the operation
     */
    Entity lookupLocalEntity(String reference, boolean create) throws IllegalArgumentException, EntityhubException;
    /**
     * Getter for an Entity managed by the Entityhub. This method does only work 
     * with references to locally managed Entities.
     * @param entityId the ID of the locally managed Entity
     * @return the Entity or <code>null</code> if no {@link Entity} with that
     * ID is managed by the Entityhub.
     * @throws IllegalArgumentException if <code>null</code> or an empty String
     * is parsed as symbolId or if the parsed ID does not represent a
     * {@link Entity}
     * @throws EntityhubException On any error while performing the operation
     */
    Entity getEntity(String entityId) throws IllegalArgumentException, EntityhubException;
    /**
     * Imports an Entity from a referenced site to the Entityhub. If there is 
     * already an Entity present for the parsed reference, than this Method throws an
     * {@link IllegalStateException}. If the referenced Entity is not found on
     * any referenced site, than <code>null</code> is returned.
     * If the referenced {@link Entity} provides insufficient data to create a 
     * locally managed Entity, than an {@link IllegalArgumentException} is thrown.
     * @param reference the id of the {@link Entity} to import
     * @return the imported Entity or <code>null</code> if the import was not
     * successful. 
     * @throws IllegalStateException if there exists already a {@link Entity} for
     * the parsed reference in the entityhub
     * @throws IllegalArgumentException If an import is not possible (e.g. 
     * because the {@link Representation} of the {@link Entity} provides 
     * insufficient data or some configuration that importing Entities from this
     * referenced site is not allowed).
     * @throws EntityhubException On any error while performing the operation
     */
    Entity importEntity(String reference) throws IllegalStateException,IllegalArgumentException,EntityhubException;
    /**
     * Getter for a MappedEntity based on the ID of the mapping itself.
     * @param id the id of the mapped entity
     * @return the MappedEntity or <code>null</code> if none was found
     * @throws EntityhubException On any error while performing the operation
     * @throws IllegalArgumentException If <code>null</code> or an empty string
     * is parsed as ID or if the parsed ID does not represent an 
     * {@link EntityMapping}
     */
    Entity getMappingById(String id) throws EntityhubException, IllegalArgumentException;
    /**
     * Getter for all mappings by the ID of the source. The source is the id
     * of an Entity managed by an referenced site.
     * TODO: check if an Entity of an referenced site can be mapped to more than 
     * one locally managed Entity
     * @param source the ID of the source (an entity managed by some referenced
     * site)
     * @return Iterator over all the Mappings defined for this entity
     * @throws EntityhubException On any error while performing the operation
     */
    Entity getMappingBySource(String source) throws EntityhubException;
    /**
     * Getter for the {@link FieldQueryFactory} instance of the Entityhub. Typical
     * implementation will return the factory implementation used by the current
     * {@link Yard} used by the entity hub.
     * @return the query factory
     */
    FieldQueryFactory getQueryFactory();
    /**
     * Getter for the FieldMappings configured for this Site
     * @return The {@link FieldMapping} present for this Site.
     */
    FieldMapper getFieldMappings();
//    /**
//     * Getter for the Configuration for the entity hub
//     * @return the configuration of the entity hub
//     */
//    EntityhubConfiguration getEntityhubConfiguration();
    /**
     * Getter for all the mappings by the id of the target. The target is an
     * locally managed entity.
     * @param entityId the id of the target (a locally managed entity)
     * @return the mappings for the parsed target
     * @throws EntityhubException On any error while performing the operation
     */
    Collection<Entity> getMappingsByTarget(String entityId) throws EntityhubException;

    /**
     * Searches for symbols based on the parsed {@link FieldQuery} and returns
     * the references (ids). Note that selected fields of the query are ignored.
     * @param query the query
     * @return the references of the found symbols
     * @throws EntityhubException On any error while performing the operation
     */
    QueryResultList<String> findEntityReferences(FieldQuery query) throws EntityhubException;
    /**
     * Searches for symbols based on the parsed {@link FieldQuery} and returns
     * representations as defined by the selected fields of the query. Note that
     * if the query defines also {@link Constraint}s for selected fields, that
     * the returned representation will only contain values selected by such
     * constraints.
     * @param query the query
     * @return the found symbols as representation containing only the selected
     * fields and there values.
     * @throws EntityhubException On any error while performing the operation
     */
    QueryResultList<Representation> find(FieldQuery query) throws EntityhubException;
    /**
     * Searches for Signs based on the parsed {@link FieldQuery} and returns
     * the selected Signs including the whole representation. Note that selected
     * fields of the query are ignored.
     * @param query the query
     * @return All Entities selected by the Query.
     * @throws EntityhubException On any error while performing the operation
     */
    QueryResultList<Entity> findEntities(FieldQuery query) throws EntityhubException;

    /**
     * Checks if an Entity with the parsed id is managed by the Entityhub.
     * @param id the id of the entity
     * @return If an Entity with the given id is managed by the Entityhub
     * @throws EntityhubException On any error while performing the operation
     * @throws IllegalArgumentException if <code>null</code> or an empty String
     * is parsed as id
     */
    boolean isRepresentation(String id) throws EntityhubException, IllegalArgumentException;
    /**
     * Stores (create or updates) the parsed representation within the Entityhub. 
     * The representation can be both data or metadata of an entity.<p>
     * To only allow create or update operations check first with 
     * {@link #isRepresentation(String)}. 
     * @param representation the representation to be updated
     * @return The updated entity.
     * @throws EntityhubException On any error while performing the operation
     * @throws IllegalArgumentException if <code>null</code> is parsed as
     * Representation or if no {@link Representation} with the parsed id is
     * managed by the Entityhub.
     */
    Entity store(Representation representation) throws EntityhubException, IllegalArgumentException;
    /**
     * Deletes the Entity with the parsed id. This will delete the entity
     * and all its information including metadata and mappings to other entities
     * form the Entityhub. To mark the Entity as removed use 
     * {@link #setState(String, ManagedEntityState)} with 
     * {@link ManagedEntityState#removed} as second parameter.
     * @param id The id of the Entity to delete
     * @return The deleted Entity
     * @throws EntityhubException On any error while performing the operation
     * @throws IllegalArgumentException if <code>null</code> or an empty String
     * is parsed as id
     */
    Entity delete(String id) throws EntityhubException, IllegalArgumentException;
    /**
     * Deletes all Entities and their Mappings from the Entityhub.
     * @throws EntityhubException On any error while performing the operation
     */
    void deleteAll() throws EntityhubException;
    /**
     * Setter for the state of an Entity. This can be used to directly set the
     * {@link ManagedEntityState} as stored with the 
     * {@link Entity#getMetadata() metadata} of an entity.
     * @param id The id of the Entity (or the metadata of the entity)
     * @param state the new state
     * @return the entity with the new state or <code>null</code> if no entity
     * for the parsed id was found.
     * @throws EntityhubException On any error while performing the operation
     * @throws IllegalArgumentException if <code>null</code> is parsed as any of
     * the two parameter or if the parsed id is an empty string
     */
    Entity setState(String id, ManagedEntityState state) throws EntityhubException, IllegalArgumentException;
    /**
     * Getter for the Configuration of the Entityhub
     * @return the configuration
     */
    EntityhubConfiguration getConfig();
}

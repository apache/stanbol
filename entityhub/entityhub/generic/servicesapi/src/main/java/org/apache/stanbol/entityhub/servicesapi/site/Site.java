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

import java.io.InputStream;
import java.util.Set;

import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapper;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapping;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.query.Constraint;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.yard.Cache;

/**
 * A site that provides Entities for the Entityhub. Sites are read-only and
 * do support {@link #getEntity(String) dereferencing} of {@link Entity entities}. 
 * They optionally can {@link #supportsSearch() support search}.<p>
 * {@link ManagedSite}s do also support create/update/delete on
 * managed entities. They are also required to support the query.
 * 
 * <i>NOTE:</i> this interface replaces ReferencedSite in versions
 * later than 0.10.0-incubating.
 * 
 * @author Rupert Westenthaler
 */
public interface Site {
    /**
     * List of {@link #getId() ids} that are not allowed to be used (case
     * insensitive) for referenced sites.
     */
    Set<String> PROHIBITED_SITE_IDS = Entityhub.ENTITYHUB_IDS;
    /**
     * The Id of this site. This Method MUST return the same value as
     * <code>{@link #getConfiguration()}.getId()</code>.
     * The configured ID MUST NOT be <code>null</code>, empty or one of the
     * {@link #PROHIBITED_SITE_IDS}.
     * @return the ID of this site
     */
    String getId();
    /**
     * Searches for entities based on the parsed {@link FieldQuery} and returns
     * the references (ids). Note that selected fields of the query are ignored.
     * @param query the query
     * @return the references of the found entities
     * @throws SiteException If the request can not be executed both on
     * the {@link Cache} and by using the {@link EntityDereferencer}/
     * {@link EntitySearcher} accessing the remote site. For errors with the
     * remote site the cause will always be a Yard Exceptions. Errors for remote
     * Sites are usually IOExceptions.
     */
    QueryResultList<String> findReferences(FieldQuery query) throws SiteException;
    /**
     * Searches for entities based on the parsed {@link FieldQuery} and returns
     * representations as defined by the selected fields of the query. Note that
     * if the query defines also {@link Constraint}s for selected fields, that
     * the returned representation will only contain values selected by such
     * constraints.
     * @param query the query
     * @return the found entities as representation containing only the selected
     * fields and there values.
     * @throws SiteException If the request can not be executed both on
     * the {@link Cache} and by using the {@link EntityDereferencer}/
     * {@link EntitySearcher} accessing the remote site. For errors with the
     * remote site the cause will always be a Yard Exceptions. Errors for remote
     * Sites are usually IOExceptions.
     */
    QueryResultList<Representation> find(FieldQuery query) throws SiteException;
    /**
     * Searches for Entities based on the parsed {@link FieldQuery} and returns
     * the selected Entities including the whole representation. Note that selected
     * fields of the query are ignored.
     * @param query the query
     * @return All Entities selected by the Query.
     * @throws SiteException If the request can not be executed both on
     * the {@link Cache} and by using the {@link EntityDereferencer}/
     * {@link EntitySearcher} accessing the remote site. For errors with the
     * remote site the cause will always be a Yard Exceptions. Errors for remote
     * Sites are usually IOExceptions.
     */
    QueryResultList<Entity> findEntities(FieldQuery query) throws SiteException;

    /**
     * Getter for the Entity by the id
     * @param id the id of the entity
     * @return the entity or <code>null</code> if not found
     * @throws SiteException If the request can not be executed both on
     * the {@link Cache} and by using the {@link EntityDereferencer}/
     * {@link EntitySearcher} accessing the remote site. For errors with the
     * remote site the cause will always be a Yard Exceptions. Errors for remote
     * Sites are usually IOExceptions.
     */
    Entity getEntity(String id) throws SiteException;
    /**
     * Getter for the Content of the Entity
     * @param id the id of the Entity
     * @param contentType the requested contentType
     * @return the content or <code>null</code> if no entity with the parsed id
     * was found or the parsed ContentType is not supported for this Entity
     * @throws SiteException If the request can not be executed both on
     * the {@link Cache} and by using the {@link EntityDereferencer}/
     * {@link EntitySearcher} accessing the remote site. For errors with the
     * remote site the cause will always be a Yard Exceptions. Errors for remote
     * Sites are usually IOExceptions.
     */
    InputStream getContent(String id,String contentType) throws SiteException;
    /**
     * Getter for the FieldMappings configured for this Site
     * @return The {@link FieldMapping} present for this Site.
     */
    FieldMapper getFieldMapper();
    /**
     * Getter for the QueryFactory implementation preferable used with this Site.
     * Note that Site MUST support query instances regardless of there specific
     * implementation. However specific implementations might have performance
     * advantages for query processing and may be even execution. Therefore
     * if one creates queries that are specifically executed on this specific
     * site, that it is best practice to use the instance provided by this
     * method.
     * @return The query factory of this site.
     */
    FieldQueryFactory getQueryFactory();

    /**
     * Getter for the configuration of this referenced site
     * @return the configuration 
     */
    SiteConfiguration getConfiguration();
    /**
     * Returns if this referenced site supports local mode - meaning, that
     * it can be used to search and retrieve entities in offline mode. <p>
     * The result MUST reflect the current situation and not be based on the
     * configuration. Meaning, that if the configuration defines a local cache
     * but the cache is currently not available this method needs to return
     * <code>false</code>.
     * @return if the local mode is currently supported by this referenced site
     */
    boolean supportsLocalMode();
    /**
     * Returns if this referenced site supports queries. Some Referenced sites
     * might not be able to support queries (e.g. if a remote linked data
     * endpoint does not support an SPARQL endpoint). In such cases the
     * dereferencing functionality can still be used to retrieve representations
     * for entities by IDs. However all find** methods could not be used.<p>
     * The result MUST refelct the current situation and not be based on the
     * configuration. E.g. if the remote site does not support querys and the 
     * local cache is temporary not available this method would need to support
     * <code>false</code> even that based on the configuration the site would
     * theoretically support queries.<p>
     * TODO: This need to be extended as soon as support for multiple query
     * languages is added.
     * @return if this referenced site supports queries for entities.
     */
    boolean supportsSearch();


}

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
import java.util.Collection;
import java.util.Dictionary;

import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;

public interface SiteManager {


    /**
     * Returns if a site with the parsed id is referenced
     * @param baseUri the base URI
     * @return <code>true</code> if a site with the parsed ID is present.
     * Otherwise <code>false</code>.
     */
    boolean isReferred(String id);

    /**
     * Getter for the referenced site based on the id
     * @param baseUri the base URI of the referred Site
     * @return the {@link Site} or <code>null</code> if no site is
     * present for the parsed base ID.
     */
    Site getSite(String id);

    /**
     * Getter for Sites that manages entities with the given ID. A Site can
     * define a list of prefixes of Entities ID it manages. This method can
     * be used to retrieve all the Site that may be able to dereference the
     * parsed entity id
     * @param entityUri the ID of the entity
     * @return A list of referenced sites that may manage the entity in question.
     */
    Collection<Site> getSitesByEntityPrefix(String entityUri);

    /**
     * Getter for the Entity referenced by the parsed ID. This method will search
     * all referenced sites
     * @param id the id of the entity
     * @return the Sign or <code>null</code> if not found
     */
    Entity getEntity(String reference);

    /**
     * Returns the Entities that confirm to the parsed Query
     * @param query the query
     * @return the id's of Entities
     */
    QueryResultList<Entity> findEntities(FieldQuery query);
    /**
     * Searches for Entities based on the parsed query and returns representations
     * including the selected fields and filtered values
     * @param query The query
     * @return The representations including selected fields/values
     */
    QueryResultList<Representation> find(FieldQuery query);
    /**
     * Searches for Entities based on the parsed query and returns the ids.
     * @param query The query
     * @return the ids of the selected entities
     */
    QueryResultList<String> findIds(FieldQuery query);
    /**
     * Getter for the content of the entity
     * @param entity the id of the entity
     * @param contentType the content type
     * @return the content as {@link InputStream} or <code>null</code> if no
     * entity with this ID is known by the Entityhub or no representation for 
     * the requested entity is available for the parsed content type
     */
    InputStream getContent(String entity,String contentType);
    /**
     * Getter for the Id's of all active referenced sites
     * @return Unmodifiable collections of the id#s of all referenced sites
     */
    Collection<String> getSiteIds();

}

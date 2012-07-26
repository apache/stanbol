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

import org.apache.stanbol.entityhub.servicesapi.EntityhubConfiguration;
import org.apache.stanbol.entityhub.servicesapi.model.ManagedEntityState;
import org.apache.stanbol.entityhub.servicesapi.model.MappingState;
import org.apache.stanbol.entityhub.servicesapi.yard.CacheStrategy;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;

/**
 * Configuration for a {@link Site} that uses external services to access
 * and query for Entities. ReferencedSites may cache remote entities locally.
 * 
 * @author Rupert Westenthaler
 *
 */
public interface ReferencedSiteConfiguration extends SiteConfiguration {
    /**
     * Key used for the configuration of the AccessURI  for a site
     */
    String ACCESS_URI = "org.apache.stanbol.entityhub.site.accessUri";
    /**
     * The URI used to access the Data of this Site. This is usually a different
     * URI as the ID of the site.<p>
     *
     * To give some Examples: <p>
     *
     * symbol.label: DBPedia<br>
     * symbol.id: http://dbpedia.org<br>
     * site.acessUri: http://dbpedia.org/resource/<p>
     *
     * symbol.label: Freebase<br>
     * symbol.id: http://www.freebase.com<br>
     * site.acessUri: http://rdf.freebase.com/<p>
     *
     * @return the accessURI for the data of the referenced site
     */
    String getAccessUri();
    /**
     * Key used for the configuration of the name of the dereferencer type to be
     * used for this site
     */
    String ENTITY_DEREFERENCER_TYPE = "org.apache.stanbol.entityhub.site.dereferencerType";
    /**
     * The name of the {@link EntityDereferencer} to be used for accessing
     * representations of entities managed by this Site
     * @return the id of the entity dereferencer implementation
     */
    String getEntityDereferencerType();
    /**
     * Key used for the configuration of the uri to access the query service of
     * the site
     */
    String QUERY_URI = "org.apache.stanbol.entityhub.site.queryUri";
    /**
     * Getter for the queryUri of the site.
     * @return the uri to access the query service of this site
     */
    String getQueryUri();
    /**
     * Key used for the configuration of the type of the query
     */
    String ENTITY_SEARCHER_TYPE = "org.apache.stanbol.entityhub.site.searcherType";
    /**
     * The type of the {@link EntitySearcher} to be used to query for
     * representations of entities managed by this Site.
     * @return the id of the entity searcher implementation.
     */
    String getEntitySearcherType();

    /**
     * Key used for the configuration of the default expiration duration for entities and
     * data for a site
     */
    String CACHE_STRATEGY = "org.apache.stanbol.entityhub.site.cacheStrategy";
    /**
     * The cache strategy used by for this site to be used.
     * @return the cache strategy
     */
    CacheStrategy getCacheStrategy();

    /**
     * The key used for the configuration of the id for the yard used as a cache
     * for the data of a referenced Site. This property is ignored if
     * {@link CacheStrategy#none} is used.
     */
    String CACHE_ID = "org.apache.stanbol.entityhub.site.cacheId";

    /**
     * The id of the Yard used to cache data of this referenced site.
     * @return the id of the {@link Yard} used as a cache. May be <code>null</code>
     * if {@link CacheStrategy#none} is configured for this yard
     */
    String getCacheId();


}

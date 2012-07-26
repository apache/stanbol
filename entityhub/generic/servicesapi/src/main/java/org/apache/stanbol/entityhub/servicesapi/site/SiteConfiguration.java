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


import java.util.Dictionary;

import org.apache.stanbol.entityhub.servicesapi.EntityhubConfiguration;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapping;
import org.apache.stanbol.entityhub.servicesapi.model.ManagedEntityState;
import org.apache.stanbol.entityhub.servicesapi.model.MappingState;
import org.apache.stanbol.entityhub.servicesapi.yard.Cache;
import org.apache.stanbol.entityhub.servicesapi.yard.CacheStrategy;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;

/**
 * This interface defines the getter as well as the keys used to configure
 * such properties when parsing an configuration for a {@link Site}.<p>
 *
 * @author Rupert Westenthaler
 *
 */
public interface SiteConfiguration {

    /**
     * The key to be used for the site id
     */
    String ID = "org.apache.stanbol.entityhub.site.id";
    /**
     * Getter for the id of this site.
     * @return The id of the Site
     */
    String getId();
    /**
     * The key to be used for the name of the site
     */
    String NAME = "org.apache.stanbol.entityhub.site.name";
    /**
     * The preferred name of this site (if not present use the id)
     * @return the name (or if not defined the id) of the site
     */
    String getName();
    /**
     * The key to be used for the site description
     */
    String DESCRIPTION = "org.apache.stanbol.entityhub.site.description";
    /**
     * Getter for the default short description of this site.
     * @return The description or <code>null</code> if non is defined
     */
    String getDescription();

    /**
     * The key used to configure the FieldMappings for a Site. Note that values
     * are parsed by using {@link FieldMapping#parseFieldMapping(String)}
     */
    String SITE_FIELD_MAPPINGS = "org.apache.stanbol.entityhub.site.fieldMappings";
    /**
     * Getter for the field mappings used for this site when importing entities
     * to the Entityhub.<p>
     * Note that this field mappings are used in addition to the field mappings
     * defined by the {@link EntityhubConfiguration}.
     * @return the FieldMappings or <code>null</code> if none.
     */
    String[] getFieldMappings();
    
    /**
     * Key used for the configuration of prefixes used by Entities managed by this Site
     */
    String ENTITY_PREFIX = "org.apache.stanbol.entityhub.site.entityPrefix";
    /**
     * Getter for the prefixes of entities managed by this Site
     * @return the entity prefixes. In case there are non an empty array is returned.
     */
    String[] getEntityPrefixes();

    /**
     * Key used for the configuration of the default {@link ManagedEntityState} for a site
     */
    String DEFAULT_SYMBOL_STATE = "org.apache.stanbol.entityhub.site.defaultSymbolState";
    /**
     * The initial state if a {@link Symbol} is created for an entity managed
     * by this site
     * @return the default state for new symbols
     */
    ManagedEntityState getDefaultManagedEntityState();
    /**
     * Key used for the configuration of the default {@link EntityMapping} state
     * ({@link MappingState} for a site
     */
    String DEFAULT_MAPPING_STATE = "org.apache.stanbol.entityhub.site.defaultMappedEntityState";
    /**
     * The initial state for mappings of entities managed by this site
     * @return the default state for mappings to entities of this site
     */
    MappingState getDefaultMappedEntityState();

    /**
     * Key used for the configuration of the default expiration duration for entities and
     * data for a site
     */
    String DEFAULT_EXPIRE_DURATION = "org.apache.stanbol.entityhub.site.defaultExpireDuration";
    /**
     * Return the duration in milliseconds or values <= 0 if mappings to entities
     * of this Site do not expire.
     * @return the duration in milliseconds or values <=0 if not applicable.
     */
    long getDefaultExpireDuration();

    /**
     * The key used to configure the name of License used by a referenced Site
     */
    String SITE_LICENCE_NAME = "org.apache.stanbol.entityhub.site.licenseName";
    /**
     * The key used to configure the License of a referenced Site
     */
    String SITE_LICENCE_TEXT = "org.apache.stanbol.entityhub.site.licenseText";
    /**
     * The key used to configure the link to the License used by a referenced Site
     */
    String SITE_LICENCE_URL = "org.apache.stanbol.entityhub.site.licenseUrl";
    
    /**
     * Getter for the the License(s) used for the data provided by this 
     * site. If multiple Liceneses are given it is assumed that any of them can
     * be used. <p>
     * Licenses are defined by the name ({@link #SITE_LICENCE_NAME}),
     * the text ({@link #SITE_LICENCE_TEXT}) and the url 
     * ({@link #SITE_LICENCE_URL}) to the page of the license. This three keys
     * are combined to the {@link License} object.<p>
     * It is recommended to use "cc:license" to link those licenses to the
     * page.
     * @return The name of the license
     */
    License[] getLicenses();
    /**
     * The attribution for the data provided by this referenced site
     */
    String SITE_ATTRIBUTION = "org.apache.stanbol.entityhub.site.attribution";
    /**
     * The name the creator of the Work (representations in case of referenced
     * sites) would like used when attributing re-use. <code>null</code> if
     * none is required by the license.<p>
     * It is recommended to use "cc:attributionName" to represent this in
     * RDF graphs
     * @return the attribution name
     */
    String getAttribution();
    /**
     * The URL to the attribution for the data provided by this referenced site
     */
    String SITE_ATTRIBUTION_URL = "org.apache.stanbol.entityhub.site.attributionUrl";
    /**
     * The URL the creator of a Work (representations in case of referenced
     * sites) would like used when attributing re-use.  <code>null</code> if
     * none is required by the license.<p>
     * It is recommended to use "cc:attributionURL" to represent this in
     * RDF graphs
     * @return the link to the URL providing the full attribution
     */
    String getAttributionUrl();
    /**
     * The configuration as {@link Dictionary} (as used by OSGI)
     * @return the configuration as Dictionary
     */
    Dictionary<String,Object> getConfiguration();

}

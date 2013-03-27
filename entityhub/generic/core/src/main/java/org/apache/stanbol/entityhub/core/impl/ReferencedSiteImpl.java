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
package org.apache.stanbol.entityhub.core.impl;

import static java.util.Collections.singletonMap;
import static org.apache.stanbol.entityhub.core.utils.SiteUtils.initEntityMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.entityhub.core.mapping.DefaultFieldMapperImpl;
import org.apache.stanbol.entityhub.core.mapping.FieldMappingUtils;
import org.apache.stanbol.entityhub.core.mapping.ValueConverterFactory;
import org.apache.stanbol.entityhub.core.model.EntityImpl;
import org.apache.stanbol.entityhub.core.query.DefaultQueryFactory;
import org.apache.stanbol.entityhub.core.query.QueryResultListImpl;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapper;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapping;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.site.EntityDereferencer;
import org.apache.stanbol.entityhub.servicesapi.site.EntitySearcher;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteConfiguration;
import org.apache.stanbol.entityhub.servicesapi.site.Site;
import org.apache.stanbol.entityhub.servicesapi.site.SiteConfiguration;
import org.apache.stanbol.entityhub.servicesapi.site.SiteException;
import org.apache.stanbol.entityhub.servicesapi.yard.Cache;
import org.apache.stanbol.entityhub.servicesapi.yard.CacheStrategy;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This in the Default implementation of the {@link Site} interface. However this implementation forwards
 * calls to methods defined within the {@link EntityDereferencer} and {@link EntitySearcher} to sub components
 * (See the detailed description below).
 * <p>
 * Each {@link Site} with an {@link CacheStrategy} other than {@link CacheStrategy#none} needs an associated
 * {@link Cache} (actually a wrapper over a {@link Yard}).
 * <p>
 * Referenced Components
 * <ul>
 * <li><b>{@link EntityDereferencer}:</b> Implementations of this interface are specific to the used
 * protocol/technology of the referenced site. Because of that calls to methods defined in this interface are
 * forwarded to an site specific instance of the {@link EntityDereferencer} interface as configured by the
 * {@link SiteConfiguration#ENTITY_DEREFERENCER_TYPE} property.<br>
 * <li><b> {@link EntitySearcher}:</b> Implementations of this interface are also specific to the used
 * protocol/technology of the referenced site. Because of that calls to methods defined in this interface are
 * forwarded to an site specific instance of the {@link EntitySearcher} interface as configured by the
 * {@link SiteConfiguration#ENTITY_SEARCHER_TYPE} property. Support for Search is optional. If no
 * {@link EntitySearcher} is configured that all find** methods will throw {@link SiteException}s<br>n.
 * <li><b>{@link Cache}: </b> An instance of a {@link Cache} is used to cache {@link Representation}s loaded
 * form the Site. A cache is a wrapper over a {@link Yard} instance that allows to configure what data are
 * stored for each representation cached form this referenced site. In case of
 * {@link CacheStrategy#all} the Cache is also used to search for Entities. Otherwise only
 * dereferencing of Entities is done via the Cache.
 * </ul>
 * 
 * @author Rupert Westenthaler
 * 
 */
public class ReferencedSiteImpl implements Site {
    private final Logger log = LoggerFactory.getLogger(ReferencedSiteImpl.class);

    private FieldMapper fieldMappings;

    private EntityDereferencer dereferencer;

    private EntitySearcher entitySearcher;

    private Cache cache;

    private ReferencedSiteConfiguration siteConfiguration;
    /**
     * Stores keys -> values to be added to the metadata of {@link Entity Entities} created by this site.
     */
    private Map<String,Object> siteMetadata;

    public ReferencedSiteImpl(ReferencedSiteConfiguration config, 
            EntityDereferencer dereferencer, EntitySearcher searcher,
            Cache cache, NamespacePrefixService nsPrefixService) {
        if(config == null){
            throw new IllegalArgumentException("The parsed SiteConfiguration MUST NOT be NULL!");
        }
        if(config.getId() == null || config.getId().isEmpty()){
            throw new IllegalArgumentException("The ReferencedSite ID (config#getId()) MUST NOT "
                + "NULL nor empty!");
        }
        if(config.getCacheStrategy() != CacheStrategy.all &&
                dereferencer == null){
            throw new IllegalArgumentException("The EntityDerefernencer MUST NOT be NULL if "
                + "CacheStrategy is NOT FULL (all entities in local Cache)!");
        }
        if(config.getCacheStrategy() != CacheStrategy.none && cache == null){
            throw new IllegalArgumentException("The Cache MUST NOT be NULL if the "
                + "CacheStrategy is set to an other value as NONE!");
        }
        this.siteConfiguration = config;
        this.cache = cache;
        this.dereferencer = dereferencer;
        this.entitySearcher = searcher;
        // init the fieldMapper based on the configuration
        fieldMappings = new DefaultFieldMapperImpl(ValueConverterFactory.getDefaultInstance());
        if (siteConfiguration.getFieldMappings() != null) {
            log.debug(" > Initialise configured field mappings");
            for (String configuredMapping : siteConfiguration.getFieldMappings()) {
                FieldMapping mapping =
                        FieldMappingUtils.parseFieldMapping(configuredMapping, nsPrefixService);
                if (mapping != null) {
                    log.debug("   - add FieldMapping {}", mapping);
                    fieldMappings.addMapping(mapping);
                }
            }
        }

    }

    public String getId() {
        return siteConfiguration.getId();
    }

    @Override
    public QueryResultList<Entity> findEntities(FieldQuery query) throws SiteException {
        List<Entity> results;
        if (siteConfiguration.getCacheStrategy() == CacheStrategy.all) {
            try {
                // When using the Cache, directly get the representations!
                QueryResultList<Representation> representations = cache.findRepresentation((query));
                results = new ArrayList<Entity>(representations.size());
                for (Representation result : representations) {
                    Entity entity = new EntityImpl(getId(), result, null);
                    results.add(entity);
                    initEntityMetadata(entity, siteMetadata,
                        singletonMap(RdfResourceEnum.isChached.getUri(), (Object) Boolean.TRUE));
                }
                return new QueryResultListImpl<Entity>(query, results, Entity.class);
            } catch (YardException e) {
                if (entitySearcher == null) {
                    throw new SiteException("Unable to execute query on Cache "
                            + siteConfiguration.getCacheId(), e);
                } else {
                    log.warn(
                        String.format(
                            "Error while performing query on Cache %s! Try to use remote site %s as fallback!",
                            siteConfiguration.getCacheId(), siteConfiguration.getQueryUri()), e);
                }
            }
        }
        QueryResultList<String> entityIds;
        if (entitySearcher == null) {
            throw new SiteException(String.format("The ReferencedSite %s does not support queries!",
                getId()));
        }
        try {
            entityIds = entitySearcher.findEntities(query);
        } catch (IOException e) {
            throw new SiteException(String.format(
                "Unable to execute query on remote site %s with entitySearcher %s!",
                siteConfiguration.getQueryUri(), siteConfiguration.getEntitySearcherType()), e);
        }
        int numResults = entityIds.size();
        List<Entity> entities = new ArrayList<Entity>(numResults);
        int errors = 0;
        SiteException lastError = null;
        for (String id : entityIds) {
            Entity entity;
            try {
                entity = getEntity(id);
                if (entity == null) {
                    log.warn("Unable to create Entity for ID that was selected by an FieldQuery (id=" + id
                            + ")");
                }
                entities.add(entity);
                // use the position in the list as resultSocre
                entity.getRepresentation().set(RdfResourceEnum.resultScore.getUri(),
                    Float.valueOf((float) numResults));
            } catch (SiteException e) {
                lastError = e;
                errors++;
                log.warn(String.format("Unable to get Representation for Entity "
                    + "%s. -> %d Error%s for %d Entities in QueryResult (Reason:%s)",
                    id, errors, errors > 1 ? "s" : "", entityIds.size(), e.getMessage()));
            }
            // decrease numResults because it is used as resultScore for
            // entities
            numResults--;
        }
        if (lastError != null) {
            if (entities.isEmpty()) {
                throw new SiteException("Unable to get anly Representations for "
                    + "Entities selected by the parsed Query (Root-Cause is the "
                    + "last Exception trown)", lastError);
            } else {
                log.warn(String.format("Unable to get %d/%d Represetnations for selected Entities.", errors,
                    entityIds.size()));
                log.warn("Stack trace of the last Exception:", lastError);
            }
        }
        return new QueryResultListImpl<Entity>(query, entities, Entity.class);
    }

    @Override
    public QueryResultList<Representation> find(FieldQuery query) throws SiteException {
        if (siteConfiguration.getCacheStrategy() == CacheStrategy.all) {
            try {
                return cache.find(query);
            } catch (YardException e) {
                if (entitySearcher == null) {
                    throw new SiteException("Unable to execute query on Cache "
                            + siteConfiguration.getCacheId(), e);
                } else {
                    log.warn(
                        String.format(
                            "Error while performing query on Cache %s! Try to use remote site %s as fallback!",
                            siteConfiguration.getCacheId(), siteConfiguration.getQueryUri()), e);
                }
            }
        }
        
        if (entitySearcher == null) {
            throw new SiteException(String.format("ReferencedSite %s does not support queries!",
                getId()));
        }
        try {
            return entitySearcher.find(query);
        } catch (IOException e) {
            throw new SiteException("Unable execute Query on remote site " + siteConfiguration.getQueryUri(),
                    e);
        }
    }

    @Override
    public QueryResultList<String> findReferences(FieldQuery query) throws SiteException {
        if (siteConfiguration.getCacheStrategy() == CacheStrategy.all) {
            try {
                return cache.findReferences(query);
            } catch (YardException e) {
                if (entitySearcher == null) {
                    throw new SiteException("Unable to execute query on Cache "
                            + siteConfiguration.getCacheId(), e);
                } else {
                    log.warn(
                        String.format(
                            "Error while performing query on Cache %s! Try to use remote site %s as fallback!",
                            siteConfiguration.getCacheId(), siteConfiguration.getQueryUri()), e);
                }
            }
        }
        if (entitySearcher == null) {
            throw new SiteException(String.format("The referencedSite %s dose not support queries!",
                getId()));
        }
        try {
            return entitySearcher.findEntities(query);
        } catch (IOException e) {
            throw new SiteException("Unable execute Query on remote site " + siteConfiguration.getQueryUri(),
                    e);
        }
    }

    @Override
    public InputStream getContent(String id, String contentType) throws SiteException {
        if (siteConfiguration.getEntityDereferencerType() == null) {
            throw new SiteException(
                    String.format(
                        "Unable to get Content for Entity %s because No dereferencer configured for ReferencedSite %s",
                        id, getId()));
        }
        if (dereferencer == null) {
            throw new SiteException(String.format("Dereferencer %s for remote site %s is not available",
                siteConfiguration.getEntityDereferencerType(), siteConfiguration.getAccessUri()));
        }
        try {
            return dereferencer.dereference(id, contentType);
        } catch (IOException e) {
            throw new SiteException(
                    String.format(
                        "Unable to load content for Entity %s and mediaType %s from remote site %s by using dereferencer %s",
                        id, contentType, siteConfiguration.getAccessUri(),
                        siteConfiguration.getEntityDereferencerType()), e);
        }
    }

    @Override
    public Entity getEntity(String id) throws SiteException {
        Representation rep = null;
        Boolean cachedVersion = Boolean.FALSE;
        long start = System.currentTimeMillis();
        if (cache != null) {
            try {
                rep = cache.getRepresentation(id);
                if (rep == null){
                    if(siteConfiguration.getCacheStrategy() == CacheStrategy.all) {
                        return null; // do no remote lookups on CacheStrategy.all!!
                    }
                } else {
                    cachedVersion = Boolean.TRUE;
                }
            } catch (YardException e) {
                if (dereferencer == null) {
                    throw new SiteException(String.format("Unable to get Represetnation %s form Cache %s",
                        id, siteConfiguration.getCacheId()), e);
                } else {
                    log.warn(
                        String.format(
                            "Unable to get Represetnation %s form Cache %s. Will dereference from remote site %s",
                            id, siteConfiguration.getCacheId(), siteConfiguration.getAccessUri()), e);
                }
            }
        }
        if (rep == null && dereferencer != null) {
            try {
                rep = dereferencer.dereference(id);
            } catch (IOException e) {
                throw new SiteException(String.format(
                    "Unable to load Representation for entity %s form remote site %s with dereferencer %s",
                    id, siteConfiguration.getAccessUri(), siteConfiguration.getEntityDereferencerType()), e);
            }
            // representation loaded from remote site and cache is available
            if (rep != null && cache != null) {// -> cache the representation
                try {
                    start = System.currentTimeMillis();
                    // return the the cached version
                    rep = cache.store(rep);
                    cachedVersion = Boolean.TRUE;
                    log.debug("  - cached Representation {} in {} ms", id,
                        (System.currentTimeMillis() - start));
                } catch (YardException e) {
                    log.warn(String.format(
                        "Unable to cache Represetnation %s in Cache %s! Representation not cached!", id,
                        siteConfiguration.getCacheId()), e);
                }
            }
        }
        if(rep != null){
            Entity entity = new EntityImpl(getId(), rep, null);
            initEntityMetadata(entity, siteMetadata,
                singletonMap(RdfResourceEnum.isChached.getUri(), (Object) cachedVersion));
            return entity;
        } else {
            return null;
        }
    }

    @Override
    public SiteConfiguration getConfiguration() {
        return siteConfiguration;
    }

    @Override
    public String toString() {
        return siteConfiguration != null ? siteConfiguration.getName() : null;
    }

    @Override
    public int hashCode() {
        return siteConfiguration != null ? getId().hashCode() : -1;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Site) {
            SiteConfiguration osc = ((Site) obj).getConfiguration();
            // this will return false if one of the two sites is not activated
            // -> this should be OK
            return siteConfiguration != null && osc != null && getId().equals(osc.getId());
        } else {
            return false;
        }
    }

    @Override
    public FieldMapper getFieldMapper() {
        return fieldMappings;
    }

    /**
     * In case {@link CacheStrategy#all} this Method returns the query factory of the Cache. Otherwise it
     * returns {@link DefaultQueryFactory#getInstance()}.
     */
    @Override
    public FieldQueryFactory getQueryFactory() {
        FieldQueryFactory factory = null;
        if (siteConfiguration.getCacheStrategy() == CacheStrategy.all) {
            if (cache != null) {
                factory = cache.getQueryFactory();
            }
        }
        if (factory == null) {
            factory = DefaultQueryFactory.getInstance();
        }
        return factory;
    }

    public boolean supportsLocalMode() {
        return siteConfiguration.getCacheStrategy() == CacheStrategy.all && cache != null;
    }

    public boolean supportsSearch() {
        return supportsLocalMode() || entitySearcher != null;
    }

}

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
package org.apache.stanbol.entityhub.site.managed.impl;

import static org.apache.stanbol.entityhub.core.utils.SiteUtils.extractSiteMetadata;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.entityhub.core.mapping.DefaultFieldMapperImpl;
import org.apache.stanbol.entityhub.core.mapping.FieldMappingUtils;
import org.apache.stanbol.entityhub.core.mapping.ValueConverterFactory;
import org.apache.stanbol.entityhub.core.model.EntityImpl;
import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.core.query.QueryResultListImpl;
import org.apache.stanbol.entityhub.core.utils.SiteUtils;
import org.apache.stanbol.entityhub.servicesapi.EntityhubException;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapper;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapping;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.site.ManagedSite;
import org.apache.stanbol.entityhub.servicesapi.site.ManagedSiteException;
import org.apache.stanbol.entityhub.servicesapi.site.Site;
import org.apache.stanbol.entityhub.servicesapi.site.SiteException;
import org.apache.stanbol.entityhub.servicesapi.site.SiteConfiguration;
import org.apache.stanbol.entityhub.servicesapi.util.AdaptingIterator;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic implementation of the {@link ManagedSite} interface on top of a
 * {@link Yard} instance.<p>
 * This is expected to be a private class that needs not to be extended.
 * @author Rupert Westenthaler
 *
 */
public final class YardSite implements ManagedSite {

    public static final Logger log = LoggerFactory.getLogger(YardSite.class);
    
    private Yard yard;
    private SiteConfiguration config;
    private Map<String,Object> siteMetadata;
    private FieldMapper fieldMapper = new DefaultFieldMapperImpl(ValueConverterFactory.getDefaultInstance());
    /**
     * used to support '{prefix}:{loacalname}' configurations in the {@link SiteConfiguration}
     */
    private NamespacePrefixService nsPrefixService;
    
    public YardSite(Yard yard, SiteConfiguration config, NamespacePrefixService nsPrefixService) {
        this.yard = yard;
        this.config = config;
        this.nsPrefixService = nsPrefixService;
        this.siteMetadata = extractSiteMetadata(config,InMemoryValueFactory.getInstance());
        //all entities of managed sites are locally cached - so we can add this
        //to the site metadata
        this.siteMetadata.put(RdfResourceEnum.isChached.getUri(), Boolean.TRUE);
        fieldMapper = new DefaultFieldMapperImpl(ValueConverterFactory.getDefaultInstance());
        if(config.getFieldMappings() != null){
            log.debug(" > Initialise configured field mappings");
            for(String configuredMapping : config.getFieldMappings()){
                FieldMapping mapping = FieldMappingUtils.parseFieldMapping(configuredMapping,nsPrefixService);
                if(mapping != null){
                    log.debug("   - add FieldMapping {}",mapping);
                    fieldMapper.addMapping(mapping);
                }
            }
        }

    }
    
    @Override
    public String getId() {
        return config.getId();
    }

    @Override
    public QueryResultList<String> findReferences(FieldQuery query) throws ManagedSiteException {
        try {
            return getYard().findReferences(query);
        } catch (YardException e) {
            throw new ManagedSiteException(e.getMessage(), e);
        }
    }

    @Override
    public QueryResultList<Representation> find(FieldQuery query) throws ManagedSiteException {
        try {
            return getYard().find(query);
        } catch (YardException e) {
            throw new ManagedSiteException(e.getMessage(), e);
        }
    }

    @Override
    public QueryResultList<Entity> findEntities(FieldQuery query) throws ManagedSiteException {
        QueryResultList<Representation> results;
        try {
            results = getYard().findRepresentation(query);
        } catch (YardException e) {
            throw new ManagedSiteException(e.getMessage(), e);
        }
        return new QueryResultListImpl<Entity>(results.getQuery(),
                new AdaptingIterator<Representation,Entity>(
                        results.iterator(), 
                        new AdaptingIterator.Adapter<Representation,Entity>() {
                            private final String siteId = config.getId();
                            @Override
                            public Entity adapt(Representation value, Class<Entity> type) {
                                Entity entity = new EntityImpl(siteId,value,null);
                                SiteUtils.initEntityMetadata(entity, siteMetadata, null);
                                return entity;
                            }
                        }, Entity.class),Entity.class);
    }

    @Override
    public Entity getEntity(String id) throws ManagedSiteException {
        Representation rep;
        try {
            rep = getYard().getRepresentation(id);
        } catch (YardException e) {
            throw new ManagedSiteException(e.getMessage(), e);
        }
        if(rep != null){
            Entity entity = new EntityImpl(config.getId(), rep, null);
            SiteUtils.initEntityMetadata(entity, siteMetadata, null);
            return entity;
        } else {
            return null;
        }
    }
    
    /**
     * Stores the parsed representation to the Yard and also applies the
     * configured {@link #getFieldMapper() FieldMappings}.
     * @param The representation to store
     */
    @Override
    public void store(Representation representation) throws ManagedSiteException {
        try {
            Yard yard = getYard();
            fieldMapper.applyMappings(representation, representation, yard.getValueFactory());
            yard.store(representation);
        }  catch (YardException e) {
            throw new ManagedSiteException(e.getMessage(), e);
        }
        
    }
    /**
     * Stores the parsed representations to the Yard and also applies the
     * configured {@link #getFieldMapper() FieldMappings}.
     * @param The representations to store
     */
    @Override
    public void store(final Iterable<Representation> representations) throws ManagedSiteException {
        try {
            Yard yard = getYard();
            final ValueFactory vf = yard.getValueFactory();
            yard.store(new Iterable<Representation>() {                
                @Override
                public Iterator<Representation> iterator() {
                    return new Iterator<Representation>() {
                        Iterator<Representation> it = representations.iterator();
                        @Override
                        public boolean hasNext() { return it.hasNext(); }
                        @Override
                        public Representation next() {
                            Representation next = it.next();
                            fieldMapper.applyMappings(next, next, vf);
                            return next;
                        }
                        @Override
                        public void remove() { it.remove(); }
                    };
                }
            });
        }  catch (YardException e) {
            throw new ManagedSiteException(e.getMessage(), e);
        }
    }

    @Override
    public void delete(String id) throws ManagedSiteException {
        try {
            getYard().remove(id);
        }  catch (YardException e) {
            throw new ManagedSiteException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteAll() throws ManagedSiteException {
        try {
            getYard().removeAll();
        }  catch (YardException e) {
            throw new ManagedSiteException(e.getMessage(), e);
        }
    }

    @Override
    public InputStream getContent(String id, String contentType) throws ManagedSiteException {
        throw new UnsupportedOperationException("Content retrieval not supported");
    }

    @Override
    public FieldMapper getFieldMapper() {
        return fieldMapper;
    }

    @Override
    public FieldQueryFactory getQueryFactory() {
        return getYard().getQueryFactory();
    }

    @Override
    public SiteConfiguration getConfiguration() {
        return config;
    }

    @Override
    public boolean supportsLocalMode() {
        return true;
    }

    @Override
    public boolean supportsSearch() {
        return true;
    }
    
    /**
     * Retruns the {@link Yard} or an {@link IllegalStateException} if the
     * instance is already {@link #close() closed}
     * @return the yard
     * @throws IllegalStateException if the site was already {@link #close() closed}
     */
    protected Yard getYard() {
        Yard yard = this.yard;
        if(yard == null){
            throw new IllegalStateException("This ManagedSite is no longer active");
        }
        return yard;
    }
    
    public void close() {
        this.yard = null;
        this.config = null;
        
    }
}

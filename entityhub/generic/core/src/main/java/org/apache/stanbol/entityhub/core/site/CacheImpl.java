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
package org.apache.stanbol.entityhub.core.site;


import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.entityhub.core.mapping.DefaultFieldMapperImpl;
import org.apache.stanbol.entityhub.core.mapping.FieldMappingUtils;
import org.apache.stanbol.entityhub.core.mapping.ValueConverterFactory;
import org.apache.stanbol.entityhub.core.query.DefaultQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapper;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapping;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.yard.Cache;
import org.apache.stanbol.entityhub.servicesapi.yard.CacheStrategy;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This is the Implementation of the {@link Cache} Interface as defined by the
 * entityhub services API.<p>
 * 
 * @author Rupert Westenthaler
 */
public class CacheImpl implements Cache {
    private Logger log = LoggerFactory.getLogger(CacheImpl.class);

    private FieldMapper baseMapper;
    private FieldMapper additionalMapper;
    private final Yard yard;

    /**
     * Constructs a new Cache for the parsed Yard and mappings
     * @param yard
     * @param additionalMappings
     * @param nsPrefixService
     * @throws YardException if loading the base mappings from the Yard fails
     * @throws IllegalStateException when parsing the additional mappings do fail
     * throws {@link IllegalArgumentException} if <code>null</code> is parsed as Yard
     */
    public CacheImpl(Yard yard, String[] additionalMappings, NamespacePrefixService nsPrefixService) throws YardException {
        if(yard == null){
            throw new IllegalArgumentException("The parsed Yard MUST NOT be NULL!");
        }
        this.yard = yard;
        //(1) Read the base mappings from the Yard
        this.baseMapper = CacheUtils.loadBaseMappings(yard,nsPrefixService);
        FieldMapper configuredMappings = null;
        if(additionalMappings != null && additionalMappings.length > 0){
            configuredMappings = new DefaultFieldMapperImpl(ValueConverterFactory.getDefaultInstance());
            for (String mappingString : additionalMappings) {
                FieldMapping fieldMapping = FieldMappingUtils.parseFieldMapping(mappingString, nsPrefixService);
                if (fieldMapping != null) {
                    configuredMappings.addMapping(fieldMapping);
                }
            }
            //check if there are valid mappings
            if (configuredMappings.getMappings().isEmpty()) {
                configuredMappings = null; //if no mappings where found set to null
            }
        }
        FieldMapper yardAdditionalMappings = CacheUtils.loadAdditionalMappings(yard,nsPrefixService);
        if (yardAdditionalMappings == null) {
            if (configuredMappings != null) {
                setAdditionalMappings(yard, configuredMappings);
            }
        } else if (!yardAdditionalMappings.equals(configuredMappings)) {
            //this may also set the additional mappings to null!
            log.info("Replace Additional Mappings for Cache {} with Mappings configured by OSGI",yard.getId());
            setAdditionalMappings(yard, configuredMappings);
        } //else current config equals configured one -> nothing to do!    
    }
    
    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public CacheStrategy isField(String field) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CacheStrategy isLanguage(String lang) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CacheStrategy strategy() {
        // TODO Auto-generated method stub
        return null;
    }

    /*--------------------------------------------------------------------------
     * Store and Update calls MUST respect the mappings configured for the
     * Cache!
     * --------------------------------------------------------------------------
     */
    @Override
    public Representation store(Representation representation) throws IllegalArgumentException, YardException {
        return yard.store(applyCacheMappings(yard, representation));
    }

    @Override
    public Representation update(Representation representation) throws YardException, IllegalArgumentException {
        return yard.update(applyCacheMappings(yard, representation));
    }

    /**
     * Applies the mappings defined by the {@link #baseMapper} and the {@link #additionalMapper}
     * to the parsed Representation.
     *
     * @param yard The yard (local reference to avoid syncronization)
     * @param representation The representation to map
     * @return the mapped representation
     */
    private Representation applyCacheMappings(Yard yard, Representation representation) {
        long start = System.currentTimeMillis();
        Representation mapped = null;
        ValueFactory valueFactory = getValueFactory();
        if (baseMapper != null) {
            mapped = yard.getValueFactory().createRepresentation(representation.getId());
            baseMapper.applyMappings(representation, mapped,valueFactory);
        }
        if (additionalMapper != null) {
            if (mapped == null) {
                mapped = yard.getValueFactory().createRepresentation(representation.getId());
            }
            additionalMapper.applyMappings(representation, mapped,valueFactory);
        }
        log.info("  -- applied mappings in " + (System.currentTimeMillis() - start) + "ms");
        return mapped != null ? mapped : representation;
    }


    /*--------------------------------------------------------------------------
     * Methods that forward calls to the Yard configured for this Cache
     * --------------------------------------------------------------------------
     */
    @Override
    public Representation create() throws YardException {
        return yard.create();
    }

    @Override
    public Representation create(String id) throws IllegalArgumentException, YardException {
        return yard.create(id);
    }

    @Override
    public QueryResultList<Representation> find(FieldQuery query) throws YardException, IllegalArgumentException {
        return yard.find(query);
    }

    @Override
    public QueryResultList<String> findReferences(FieldQuery query) throws YardException, IllegalArgumentException {
        return yard.findReferences(query);
    }

    @Override
    public QueryResultList<Representation> findRepresentation(FieldQuery query) throws YardException, IllegalArgumentException {
        return yard.findRepresentation(query);
    }

    @Override
    public String getDescription() {
        return String.format("Cache Wrapper for Yard %s ", yard.getId());
    }

    @Override
    public String getId() {
        return yard.getId();
    }

    @Override
    public String getName() {
        return yard.getName() + " Cache";
    }

    @Override
    public FieldQueryFactory getQueryFactory() {
        return DefaultQueryFactory.getInstance();
    }

    @Override
    public Representation getRepresentation(String id) throws YardException, IllegalArgumentException {
        return yard.getRepresentation(id);
    }

    @Override
    public ValueFactory getValueFactory() {
        return yard.getValueFactory();
    }

    @Override
    public boolean isRepresentation(String id) throws YardException, IllegalArgumentException {
        return yard.isRepresentation(id);
    }

    @Override
    public void remove(String id) throws IllegalArgumentException, YardException {
        yard.remove(id);
    }


    /*--------------------------------------------------------------------------
     * Methods for reading and storing and changing the cache configuration
     * --------------------------------------------------------------------------
     */

    /**
     * Getter for the base mappings used by this Cache. Modifications on the
     * returned object do not have any influence on the mappings, because this
     * method returns a clone. Use {@link #setBaseMappings(FieldMapper)} to
     * change the used base mappings. However make sure you understand the
     * implications of changing the base mappings as described in the
     * documentation of the setter method
     *
     * @return A clone of the base mappings or <code>null</code> if no base
     *         mappings are defined
     */
    @Override
    public final FieldMapper getBaseMappings() {
        return baseMapper == null ? null : baseMapper.clone();
    }

    /**
     * Getter for the additional mappings used by this Cache. Modifications on the
     * returned object do not have any influence on the mappings, because this
     * method returns a clone. Use {@link #setAdditionalMappings(FieldMapper)} to
     * change the used additional mappings. However make sure you understand the
     * implications of changing the base mappings as described in the
     * documentation of the setter method
     *
     * @return A clone of the additional mappings or <code>null</code> if no
     *         additional mappings are defined
     */
    @Override
    public final FieldMapper getAdditionalMappings() {
        return additionalMapper == null ? null : additionalMapper.clone();
    }

    @Override
    public void setAdditionalMappings(FieldMapper fieldMapper) throws YardException {
        setAdditionalMappings(yard, fieldMapper);
    }

    /**
     * Internally used in the initialisation to be able to parse the Yard instance
     *
     * @param yard the yard used to set the configured additional mappings
     * @param fieldMapper the configuration
     * @throws YardException on any error while accessing the yard
     */
    protected void setAdditionalMappings(Yard yard, FieldMapper fieldMapper) throws YardException {
        FieldMapper old = this.additionalMapper;
        this.additionalMapper = fieldMapper;
        try {
            CacheUtils.storeAdditionalMappingsConfiguration(yard, additionalMapper);
        } catch (YardException e) {
            this.additionalMapper = old;
            throw e;
        }
    }

    @Override
    public void setBaseMappings(FieldMapper fieldMapper) throws YardException {
        if (isAvailable()) {
            FieldMapper old = this.baseMapper;
            this.baseMapper = fieldMapper;
            try {
                CacheUtils.storeBaseMappingsConfiguration(yard, baseMapper);
            } catch (YardException e) {
                this.baseMapper = old;
                throw e;
            }
        }
    }

    @Override
    public void remove(Iterable<String> ids) throws IllegalArgumentException, YardException {
        yard.remove(ids);
    }
    @Override
    public void removeAll() throws YardException {
        //ensure that the baseConfig (if present) is not deleted by this
        //operation
        Representation baseConfig = yard.getRepresentation(Cache.BASE_CONFIGURATION_URI);
        yard.removeAll();
        if(baseConfig != null){
            yard.store(baseConfig);
        }
    }
    
    @Override
    public Iterable<Representation> store(Iterable<Representation> representations) throws IllegalArgumentException, YardException {
        return yard.store(representations);
    }

    @Override
    public Iterable<Representation> update(Iterable<Representation> representations) throws YardException, IllegalArgumentException {
        return yard.update(representations);
    }
}

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


import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.entityhub.core.mapping.DefaultFieldMapperImpl;
import org.apache.stanbol.entityhub.core.mapping.FieldMappingUtils;
import org.apache.stanbol.entityhub.core.mapping.ValueConverterFactory;
import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.core.query.DefaultQueryFactory;
import org.apache.stanbol.entityhub.core.utils.OsgiUtils;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapper;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapping;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.util.ModelUtils;
import org.apache.stanbol.entityhub.servicesapi.yard.Cache;
import org.apache.stanbol.entityhub.servicesapi.yard.CacheStrategy;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This is the Implementation of the {@link Cache} Interface as defined by the
 * entityhub services API.<p>
 * Currently the dependency to the Cache is managed vial a {@link ServiceTracker}.
 * This means that the Cache is activated/keeps active even if the Yard is not
 * available or is disabled.
 * If the Yard is not available all Yard related methods like get/store/remove/
 * find(..) throw {@link YardException}s.<p>
 * TODO This is not the intended way to do it, but because I have no Idea how
 * to start/stop a OSGI Component from within a class :(
 *
 * @author Rupert Westenthaler
 * @see Cache
 */
@Component(
        configurationFactory = true,
        policy = ConfigurationPolicy.REQUIRE, //the baseUri is required!
        specVersion = "1.1",
        metatype = true,
        immediate = true)
@Service(value = Cache.class)
@Properties(
    value = {
        @Property(name = Cache.CACHE_YARD), 
        @Property(name = Cache.ADDITIONAL_MAPPINGS, cardinality = 1000)})
public class CacheImpl implements Cache {
    private Logger log = LoggerFactory.getLogger(CacheImpl.class);
    public static final String CACHE_FACTORY_NAME = "org.apache.stanbol.entityhub.yard.CacheFactory";

    private ServiceTracker yardTracker;
    //private Yard yard;

    private String yardId;
    private boolean initWithYard = false;

    private FieldMapper baseMapper;
    private FieldMapper additionalMapper;
    private ComponentContext context;

    @Activate
    protected void activate(ComponentContext context) throws ConfigurationException, YardException, IllegalStateException, InvalidSyntaxException {
        if (context == null || context.getProperties() == null) {
            throw new IllegalStateException(String.format("Invalid ComponentContext parsed in activate (context=%s)", context));
        }
        this.context = context;
        yardId = OsgiUtils.checkProperty(context.getProperties(), CACHE_YARD).toString();
        String cacheFilter = String.format("(&(%s=%s)(%s=%s))", Constants.OBJECTCLASS, Yard.class.getName(), Yard.ID, yardId);
        yardTracker = new ServiceTracker(context.getBundleContext(), context.getBundleContext().createFilter(cacheFilter), null);
        yardTracker.open();
    }

    /**
     * Lazy initialisation of the yard the first time the yard is ued by this
     * cache
     *
     * @param yard the yard instance. MUST NOT be NULL!
     * @throws YardException
     */
    protected void initWithCacheYard(Yard yard) throws YardException {
        //(1) Read the base mappings from the Yard
        this.baseMapper = CacheUtils.loadBaseMappings(yard);

        //(2) Init the additional mappings based on the configuration
        Object mappings = context.getProperties().get(Cache.ADDITIONAL_MAPPINGS);
        FieldMapper configuredMappings = null;
        if (mappings instanceof String[] && ((String[]) mappings).length > 0) {
            configuredMappings = new DefaultFieldMapperImpl(ValueConverterFactory.getDefaultInstance());
            for (String mappingString : (String[]) mappings) {
                FieldMapping fieldMapping = FieldMappingUtils.parseFieldMapping(mappingString);
                if (fieldMapping != null) {
                    configuredMappings.addMapping(fieldMapping);
                }
            }
            //check if there are valid mappings
            if (configuredMappings.getMappings().isEmpty()) {
                configuredMappings = null; //if no mappings where found set to null
            }
        }
        FieldMapper yardAdditionalMappings = CacheUtils.loadAdditionalMappings(yard);
        if (yardAdditionalMappings == null) {
            if (configuredMappings != null) {
                setAdditionalMappings(yard, configuredMappings);
            }
        } else if (!yardAdditionalMappings.equals(configuredMappings)) {
            //this may also set the additional mappings to null!
            log.info("Replace Additional Mappings for Cache " + yardId + "with Mappings configured by OSGI");
            setAdditionalMappings(yard, configuredMappings);
        } //else current config equals configured one -> nothing to do!
        initWithYard = true;
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
//        context.getBundleContext().removeServiceListener(this);
        this.yardTracker.close();
        this.yardTracker = null;
        this.initWithYard = false;
        this.yardId = null;
        this.baseMapper = null;
        this.additionalMapper = null;
        this.context = null;
    }

    @Override
    public boolean isAvailable() {
        return yardTracker.getService() != null;
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

// Currently not needed, because instances are created and disposed by the YardManagerImpl!
//    @Override
//    public void serviceChanged(ServiceEvent event) {
//        log.info("Print Service Event for "+event.getSource());
//        for(String key : event.getServiceReference().getPropertyKeys()){
//            log.info("  > "+key+"="+event.getServiceReference().getProperty(key));
//        }
//        Object cacheYardPropertyValue = event.getServiceReference().getProperty(CACHE_YARD);
//        //TODO: check the type of the Service provided by the Reference!
//        if(cacheYardPropertyValue != null && yardId.equals(cacheYardPropertyValue.toString())){
//            //process the Event
//            if(event.getType() == ServiceEvent.REGISTERED){
//
//            } else if(event.getType() == ServiceEvent.UNREGISTERING){
//
//            }
//
//        }
//    }

    /**
     * Getter for the Yard used by this Cache.
     *
     * @return the Yard used by this Cache or <code>null</code> if currently not
     *         available.
     */
    public Yard getCacheYard() {
        Yard yard = (Yard) yardTracker.getService();
        if (yard != null && !initWithYard) {
            try {
                initWithCacheYard(yard);
            } catch (YardException e) {
                //this case can be recovered because initWithYard will not be
                //set to true.
                throw new IllegalStateException("Unable to initialize the Cache with Yard " + yardId + "! This is usually caused by Errors while reading the Cache Configuration from the Yard.", e);
            }
        }
        return yard;
    }

    /*--------------------------------------------------------------------------
     * Store and Update calls MUST respect the mappings configured for the
     * Cache!
     * --------------------------------------------------------------------------
     */
    @Override
    public Representation store(Representation representation) throws IllegalArgumentException, YardException {
        Yard yard = getCacheYard();
        if (yard == null) {
            throw new YardException(String.format("The Yard %s for this cache is currently not available", yardId));
        } else {
            return yard.store(applyCacheMappings(yard, representation));
        }
    }

    @Override
    public Representation update(Representation representation) throws YardException, IllegalArgumentException {
        Yard yard = getCacheYard();
        if (yard == null) {
            throw new YardException(String.format("The Yard %s for this cache is currently not available", yardId));
        } else {
            return yard.update(applyCacheMappings(yard, representation));
        }
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
        Yard yard = getCacheYard();
        if (yard == null) {
            return createRepresentation(null);
        } else {
            return yard.create();
        }
    }

    @Override
    public Representation create(String id) throws IllegalArgumentException, YardException {
        Yard yard = getCacheYard();
        if (yard == null) {
            return createRepresentation(id);
        } else {
            return yard.create(id);
        }
    }

    /**
     * Only used to create a representation if the Yard is currently not available
     *
     * @param id the id or <code>null</code> if a random one should be generated
     * @return the created Representation
     */
    private Representation createRepresentation(String id) {
        if (id == null) {
            id = String.format("urn:org.apache.stanbol:entityhub.yard.%s:%s.%s", getClass().getSimpleName(), yardId, ModelUtils.randomUUID().toString());
        }
        return InMemoryValueFactory.getInstance().createRepresentation(id);
    }

    @Override
    public QueryResultList<Representation> find(FieldQuery query) throws YardException, IllegalArgumentException {
        Yard yard = getCacheYard();
        if (yard == null) {
            throw new YardException(String.format("The Yard %s for this cache is currently not available", yardId));
        } else {
            return yard.find(query);
        }
    }

    @Override
    public QueryResultList<String> findReferences(FieldQuery query) throws YardException, IllegalArgumentException {
        Yard yard = getCacheYard();
        if (yard == null) {
            throw new YardException(String.format("The Yard %s for this cache is currently not available", yardId));
        } else {
            return yard.findReferences(query);
        }
    }

    @Override
    public QueryResultList<Representation> findRepresentation(FieldQuery query) throws YardException, IllegalArgumentException {
        Yard yard = getCacheYard();
        if (yard == null) {
            throw new YardException(String.format("The Yard %s for this cache is currently not available", yardId));
        } else {
            return yard.findRepresentation(query);
        }
    }

    @Override
    public String getDescription() {
        return String.format("Cache Wrapper for Yard %s ", yardId);
    }

    @Override
    public String getId() {
        return yardId;
    }

    @Override
    public String getName() {
        return yardId + " Cache";
    }

    @Override
    public FieldQueryFactory getQueryFactory() {
        Yard yard = getCacheYard();
        if (yard == null) {
            return DefaultQueryFactory.getInstance();
        } else {
            return yard.getQueryFactory();
        }
    }

    @Override
    public Representation getRepresentation(String id) throws YardException, IllegalArgumentException {
        Yard yard = getCacheYard();
        if (yard == null) {
            throw new YardException(String.format("The Yard %s for this cache is currently not available", yardId));
        } else {
            return yard.getRepresentation(id);
        }
    }

    @Override
    public ValueFactory getValueFactory() {
        Yard yard = getCacheYard();
        if (yard == null) {
            return InMemoryValueFactory.getInstance();
        } else {
            return yard.getValueFactory();
        }
    }

    @Override
    public boolean isRepresentation(String id) throws YardException, IllegalArgumentException {
        Yard yard = getCacheYard();
        if (yard == null) {
            throw new YardException(String.format("The Yard %s for this cache is currently not available", yardId));
        } else {
            return yard.isRepresentation(id);
        }
    }

    @Override
    public void remove(String id) throws IllegalArgumentException, YardException {
        Yard yard = getCacheYard();
        if (yard == null) {
            throw new YardException(String.format("The Yard %s for this cache is currently not available", yardId));
        } else {
            yard.remove(id);
        }
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
        Yard yard = getCacheYard();
        if (yard == null) {
            throw new YardException(String.format("The Yard %s for this cache is currently not available", yardId));
        } else {
            setAdditionalMappings(yard, fieldMapper);
        }
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
        Yard yard = getCacheYard();
        if (yard == null) {
            throw new YardException(String.format("The Yard %s for this cache is currently not available", yardId));
        } else {
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
    }

    @Override
    public void remove(Iterable<String> ids) throws IllegalArgumentException, YardException {
        Yard yard = getCacheYard();
        if (yard == null) {
            throw new YardException(String.format("The Yard %s for this cache is currently not available", yardId));
        } else {
            yard.remove(ids);
        }
    }

    @Override
    public Iterable<Representation> store(Iterable<Representation> representations) throws IllegalArgumentException, YardException {
        Yard yard = getCacheYard();
        if (yard == null) {
            throw new YardException(String.format("The Yard %s for this cache is currently not available", yardId));
        } else {
            return yard.store(representations);
        }
    }

    @Override
    public Iterable<Representation> update(Iterable<Representation> representations) throws YardException, IllegalArgumentException {
        Yard yard = getCacheYard();
        if (yard == null) {
            throw new YardException(String.format("The Yard %s for this cache is currently not available", yardId));
        } else {
            return yard.update(representations);
        }
    }
}

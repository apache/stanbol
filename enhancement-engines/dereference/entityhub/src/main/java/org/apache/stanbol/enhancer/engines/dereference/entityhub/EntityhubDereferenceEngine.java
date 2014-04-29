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
package org.apache.stanbol.enhancer.engines.dereference.entityhub;

import static org.apache.stanbol.enhancer.engines.dereference.DereferenceConstants.DEREFERENCE_ENTITIES_FIELDS;
import static org.apache.stanbol.enhancer.engines.dereference.DereferenceConstants.DEREFERENCE_ENTITIES_LDPATH;
import static org.apache.stanbol.enhancer.servicesapi.EnhancementEngine.PROPERTY_NAME;
import static org.osgi.framework.Constants.SERVICE_RANKING;

import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.enhancer.engines.dereference.DereferenceConfigurationException;
import org.apache.stanbol.enhancer.engines.dereference.DereferenceConstants;
import org.apache.stanbol.enhancer.engines.dereference.DereferenceContext;
import org.apache.stanbol.enhancer.engines.dereference.DereferenceContextFactory;
import org.apache.stanbol.enhancer.engines.dereference.DereferenceEngineConfig;
import org.apache.stanbol.enhancer.engines.dereference.EntityDereferenceEngine;
import org.apache.stanbol.enhancer.engines.dereference.entityhub.shared.SharedDereferenceThreadPool;
import org.apache.stanbol.enhancer.engines.dereference.entityhub.shared.SharedExecutorServiceProvider;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
/**
 * The EntityhubLinkingEngine in NOT an {@link EnhancementEngine} but only an
 * OSGI {@link Component} that allows to configure instances of the
 * {@link EntityLinkingEngine} using an {@link SiteDereferencer} or
 * {@link EntityhubDereferencer} to link entities.
 * @author Rupert Westenthaler
 *
 */
@Component(
    configurationFactory = true, 
    policy = ConfigurationPolicy.REQUIRE, // the baseUri is required!
    specVersion = "1.1", 
    metatype = true, 
    immediate = true,
    inherit = true)
@org.apache.felix.scr.annotations.Properties(value={
    @Property(name=PROPERTY_NAME),
    @Property(name=EntityhubDereferenceEngine.SITE_ID),
    @Property(name=DereferenceConstants.FALLBACK_MODE, 
    	boolValue=DereferenceConstants.DEFAULT_FALLBACK_MODE),
    @Property(name=DereferenceConstants.URI_PREFIX, cardinality=Integer.MAX_VALUE),
    @Property(name=DereferenceConstants.URI_PATTERN, cardinality=Integer.MAX_VALUE),
    @Property(name=DereferenceConstants.FILTER_CONTENT_LANGUAGES, 
    	boolValue=DereferenceConstants.DEFAULT_FILTER_CONTENT_LANGUAGES),
    @Property(name=DEREFERENCE_ENTITIES_FIELDS,cardinality=Integer.MAX_VALUE,
    	value={"rdfs:comment","geo:lat","geo:long","foaf:depiction","dbp-ont:thumbnail"}),
    @Property(name=DEREFERENCE_ENTITIES_LDPATH, cardinality=Integer.MAX_VALUE),
    @Property(name=EntityhubDereferenceEngine.SHARED_THREAD_POOL_STATE,
    	boolValue=EntityhubDereferenceEngine.DEFAULT_SHARED_THREAD_POOL_STATE),
    @Property(name=EntityhubDereferenceEngine.THREAD_POOL_SIZE,
    	intValue=EntityhubDereferenceEngine.DEFAULT_THREAD_POOL_SIZE),
    @Property(name=SERVICE_RANKING,intValue=0)
})
public class EntityhubDereferenceEngine implements ServiceTrackerCustomizer {

    private final Logger log = LoggerFactory.getLogger(EntityhubDereferenceEngine.class);

    @Reference(cardinality=ReferenceCardinality.OPTIONAL_UNARY)
    protected NamespacePrefixService prefixService;
    
    /**
     * The id of the Entityhub Site (Referenced or Managed Site) used for matching. <p>
     * To match against the Entityhub use "entityhub" as value.
     */
    public static final String SITE_ID = "enhancer.engines.dereference.entityhub.siteId";

    /**
     * If a Entityhub dereferencer should use the shared thread pool
     */
    public static final String SHARED_THREAD_POOL_STATE = "enhancer.engines.dereference.entityhub.threads.shared";

    public static final boolean DEFAULT_SHARED_THREAD_POOL_STATE = true;
    
    public static final String THREAD_POOL_SIZE = "enhancer.engines.dereference.entityhub.threads.size";
    
    public static final int DEFAULT_THREAD_POOL_SIZE = 0;
    
    private int threadPoolSize = DEFAULT_THREAD_POOL_SIZE;
    
    /**
     * The engine initialised based on the configuration of this component
     */
    protected EntityDereferenceEngine entityDereferenceEngine;
    protected Dictionary<String,Object> engineMetadata;
    /**
     * The service registration for the {@link #entityDereferenceEngine}
     */
    protected ServiceRegistration engineRegistration;
    /**
     * The EntitySearcher used for the {@link #entityDereferenceEngine}
     */
    private TrackingDereferencerBase<?> entityDereferencer;
    int trackedServiceCount = 0;
    
    /**
     * The name of the reference site ('local' or 'entityhub') if the
     * Entityhub is used for enhancing
     */
    protected String siteName;

    private BundleContext bundleContext;

	private ExecutorService executorService;

    /**
     * Default constructor as used by OSGI. This expects that 
     * {@link #activate(ComponentContext)} is called before usage
     */
    public EntityhubDereferenceEngine() {
    }

    @Activate
    @SuppressWarnings("unchecked")
    protected void activate(ComponentContext ctx) throws ConfigurationException {
        Dictionary<String,Object> properties = ctx.getProperties();
        bundleContext = ctx.getBundleContext();
        log.info("> activate {}",getClass().getSimpleName());
        //get the metadata later set to the enhancement engine
        DereferenceEngineConfig engineConfig = new DereferenceEngineConfig(properties, prefixService);
        log.debug(" - engineName: {}", engineConfig.getEngineName());
        //parse the Entityhub Site used for dereferencing
        Object value = properties.get(SITE_ID);
        //init the EntitySource
        if (value == null) {
            siteName = "*"; //all referenced sites
        } else {
            siteName = value.toString();
        }
        if (siteName.isEmpty()) {
            siteName = "*";
        }
        log.debug(" - siteName: {}", siteName);
        
        final boolean sharedPoolState;
        value = properties.get(SHARED_THREAD_POOL_STATE);
        if(value instanceof Boolean){
        	sharedPoolState = ((Boolean)value).booleanValue();
        } else if(value != null && !StringUtils.isBlank(value.toString())){
        	sharedPoolState = Boolean.parseBoolean(value.toString());
        } else {
        	sharedPoolState = DEFAULT_SHARED_THREAD_POOL_STATE;
        }
        final ExecutorServiceProvider esProvider;
        log.debug(" - shared thread pool state: {}", sharedPoolState);
        if(sharedPoolState){
        	esProvider = new SharedExecutorServiceProvider(ctx.getBundleContext());
        } else { //we need to create our own ExecutorService
	        value = properties.get(THREAD_POOL_SIZE);
	        if(value instanceof Number){
	        	this.threadPoolSize = ((Number)value).intValue();
	        } else if(value != null){
	        	try {
	        		this.threadPoolSize = Integer.parseInt(value.toString());
	        	} catch (NumberFormatException e){
	        		throw new ConfigurationException(THREAD_POOL_SIZE, "Value '" + value
	        				+ "'(type: "+value.getClass().getName()+") can not be parsed "
	        				+ "as Integer");
	        	}
	        } else {
	        	this.threadPoolSize = DEFAULT_THREAD_POOL_SIZE;
	        }
	        if(threadPoolSize > 0){
	            String namePattern = getClass().getSimpleName()+"-"
	                    + engineConfig.getEngineName()+ "-thread-%s";
	            ThreadFactory threadFactory = new ThreadFactoryBuilder()
	                .setNameFormat(namePattern)
	                .setDaemon(true).build();
	            log.debug(" - create Threadpool(namePattern='{}' | size='{}')",
	                namePattern,threadPoolSize);
	            executorService = Executors.newFixedThreadPool(threadPoolSize, threadFactory);
	        } else {
	        	log.debug(" - no thread pool configured (poolSize: {})",threadPoolSize);
	            executorService = null;
	        }
	        esProvider = new StaticExecutorServiceProvider(executorService);
        }
        //init the tracking entity searcher
        trackedServiceCount = 0;
        if(Entityhub.ENTITYHUB_IDS.contains(siteName.toLowerCase())){
            log.info("  ... init Entityhub dereferencer");
            entityDereferencer = new EntityhubDereferencer(bundleContext, this, esProvider);
        } else if(siteName.equals("*")){
            log.info("  ... init dereferencer for all referenced sites");
            entityDereferencer = new SitesDereferencer(bundleContext, this, esProvider);
        } else {
            log.info(" ... init dereferencer for referenced site {}", siteName);
            entityDereferencer = new SiteDereferencer(bundleContext,siteName, this, esProvider);
        }
        //set the namespace prefix service to the dereferencer
        entityDereferencer.setNsPrefixService(prefixService);
        //now parse dereference field config
        entityDereferencer.setDereferencedFields(engineConfig.getDereferenceFields());
        entityDereferencer.setLdPath(engineConfig.getLdPathProgram());
        entityDereferenceEngine = new EntityDereferenceEngine(entityDereferencer, engineConfig,
            new DereferenceContextFactory() { //we want to use our own DereferenceContext impl
                
                @Override
                public DereferenceContext createContext(EntityDereferenceEngine engine,
                        Map<String,Object> enhancementProperties) throws DereferenceConfigurationException {
                    return new EntityhubDereferenceContext(engine, enhancementProperties);
                }
            });
        
        //NOTE: registration of this instance as OSGI service is done as soon as the
        //      entityhub service backing the entityDereferencer is available.
        
        //finally start tracking
        entityDereferencer.open();
        
    }
    /**
     * Deactivates this components. 
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {
        //* unregister service
        ServiceRegistration reg = engineRegistration;
        if(reg != null){
            reg.unregister();
            engineRegistration = null;
        }
        if(executorService != null){
        	executorService.shutdown();
        	executorService = null;
        }
        //* reset engine
        entityDereferenceEngine = null;
        engineMetadata = null;
        //close the tracking EntitySearcher
        entityDereferencer.close();
        entityDereferencer = null;
    }
    @Override
    public Object addingService(ServiceReference reference) {
        BundleContext bc = this.bundleContext;
        if(bc != null){
            Object service =  bc.getService(reference);
            if(service != null){
                if(trackedServiceCount == 0){
                    //register the service
                    engineRegistration = bc.registerService(
                        new String[]{EnhancementEngine.class.getName(),
                                     ServiceProperties.class.getName()},
                    entityDereferenceEngine,
                    entityDereferenceEngine.getConfig().getDict());
                    
                }
                trackedServiceCount++;
            }
            return service;
        } else {
            return null;
        }
    }
    @Override
    public void modifiedService(ServiceReference reference, Object service) {
    }
    
    @Override
    public void removedService(ServiceReference reference, Object service) {
        BundleContext bc = this.bundleContext;
        if(bc != null){
            trackedServiceCount--;
            if(trackedServiceCount == 0 && engineRegistration != null){
               engineRegistration.unregister();
            }
            bc.ungetService(reference);
        }
    }
}

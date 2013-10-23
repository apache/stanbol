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

import static org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteConfiguration.ACCESS_URI;
import static org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteConfiguration.CACHE_ID;
import static org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteConfiguration.CACHE_STRATEGY;
import static org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteConfiguration.ENTITY_DEREFERENCER_TYPE;
import static org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteConfiguration.ENTITY_SEARCHER_TYPE;
import static org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteConfiguration.QUERY_URI;
import static org.apache.stanbol.entityhub.servicesapi.site.Site.PROHIBITED_SITE_IDS;
import static org.apache.stanbol.entityhub.servicesapi.site.SiteConfiguration.DEFAULT_SYMBOL_STATE;
import static org.apache.stanbol.entityhub.servicesapi.site.SiteConfiguration.SITE_FIELD_MAPPINGS;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.commons.stanboltools.offline.OfflineMode;
import org.apache.stanbol.entityhub.core.mapping.DefaultFieldMapperImpl;
import org.apache.stanbol.entityhub.core.mapping.FieldMappingUtils;
import org.apache.stanbol.entityhub.core.mapping.ValueConverterFactory;
import org.apache.stanbol.entityhub.core.site.ReferencedSiteConfigurationImpl;
import org.apache.stanbol.entityhub.core.utils.OsgiUtils;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapper;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapping;
import org.apache.stanbol.entityhub.servicesapi.site.EntityDereferencer;
import org.apache.stanbol.entityhub.servicesapi.site.EntitySearcher;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteConfiguration;
import org.apache.stanbol.entityhub.servicesapi.site.Site;
import org.apache.stanbol.entityhub.servicesapi.site.SiteConfiguration;
import org.apache.stanbol.entityhub.servicesapi.yard.Cache;
import org.apache.stanbol.entityhub.servicesapi.yard.CacheStrategy;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGI component that reads the configuration and tracks other services required
 * for the {@link ReferencedSiteImpl}. If all those services are available it
 * registers the referenced {@link Site} as OSGI service
 * @author Rupert Westenthaler
 *
 */
@Component(name = "org.apache.stanbol.entityhub.site.referencedSite",
    configurationFactory = true,
    policy = ConfigurationPolicy.REQUIRE, // the baseUri is required!
    specVersion = "1.1",
    metatype = true,
    immediate = true)
@Properties(value = {
    @Property(name = SiteConfiguration.ID),
    @Property(name = SiteConfiguration.NAME),
    @Property(name = SiteConfiguration.DESCRIPTION),
    @Property(name = SiteConfiguration.ENTITY_PREFIX, cardinality = Integer.MAX_VALUE),
    @Property(name = ACCESS_URI),
    @Property(name = ENTITY_DEREFERENCER_TYPE,
        options = {
                @PropertyOption(value = '%' + ENTITY_DEREFERENCER_TYPE + ".option.none", name = ""),
                @PropertyOption(value = '%' + ENTITY_DEREFERENCER_TYPE + ".option.sparql",
                    name = "org.apache.stanbol.entityhub.dereferencer.SparqlDereferencer"),
                @PropertyOption(value = '%' + ReferencedSiteConfiguration.ENTITY_DEREFERENCER_TYPE
                        + ".option.coolUri",
                    name = "org.apache.stanbol.entityhub.dereferencer.CoolUriDereferencer")},
        value = "org.apache.stanbol.entityhub.dereferencer.SparqlDereferencer"),
    @Property(name = QUERY_URI), // the deri server has better performance
    @Property(name = ENTITY_SEARCHER_TYPE,
        options = {
                @PropertyOption(value = '%' + ENTITY_SEARCHER_TYPE + ".option.none", name = ""),
                @PropertyOption(value = '%' + ENTITY_SEARCHER_TYPE + ".option.sparql",
                    name = "org.apache.stanbol.entityhub.searcher.SparqlSearcher"),
                @PropertyOption(value = '%' + ENTITY_SEARCHER_TYPE + ".option.sparql-virtuoso",
                    name = "org.apache.stanbol.entityhub.searcher.VirtuosoSearcher"),
                @PropertyOption(value = '%' + ENTITY_SEARCHER_TYPE + ".option.sparql-larq",
                    name = "org.apache.stanbol.entityhub.searcher.LarqSearcher")},
        value = "org.apache.stanbol.entityhub.searcher.SparqlSearcher"),
    @Property(name = DEFAULT_SYMBOL_STATE,
        options = {
                @PropertyOption(value = '%' + DEFAULT_SYMBOL_STATE + ".option.proposed",
                    name = "proposed"),
                @PropertyOption(value = '%' + DEFAULT_SYMBOL_STATE + ".option.active", name = "active")},
        value = "proposed"),
//deactivate rarely used properties in the configuration UI
//    @Property(name = DEFAULT_MAPPING_STATE,
//        options = {
//                @PropertyOption(value = '%' + DEFAULT_MAPPING_STATE + ".option.proposed",
//                    name = "proposed"),
//                @PropertyOption(value = '%' + DEFAULT_MAPPING_STATE + ".option.confirmed",
//                    name = "confirmed")}, value = "proposed"),
//    @Property(name = DEFAULT_EXPIRE_DURATION,
//        options = {
//                @PropertyOption(value = '%' + DEFAULT_EXPIRE_DURATION + ".option.oneMonth", name = ""
//                        + (1000L * 60 * 60 * 24 * 30)),
//                @PropertyOption(value = '%' + DEFAULT_EXPIRE_DURATION + ".option.halfYear", name = ""
//                        + (1000L * 60 * 60 * 24 * 183)),
//                @PropertyOption(value = '%' + DEFAULT_EXPIRE_DURATION + ".option.oneYear", name = ""
//                        + (1000L * 60 * 60 * 24 * 365)),
//                @PropertyOption(value = '%' + DEFAULT_EXPIRE_DURATION + ".option.none", name = "0")},
//        value = "0"),
    @Property(name = CACHE_STRATEGY, options = {
            @PropertyOption(value = '%' + CACHE_STRATEGY + ".option.none", name = "none"),
            @PropertyOption(value = '%' + CACHE_STRATEGY + ".option.used", name = "used"),
            @PropertyOption(value = '%' + CACHE_STRATEGY + ".option.all", name = "all")}, value = "none"),
    @Property(name = CACHE_ID), 
    @Property(name = SITE_FIELD_MAPPINGS, cardinality = Integer.MAX_VALUE)})
public class ReferencedSiteComponent {
    
    private static final Logger log = LoggerFactory.getLogger(ReferencedSiteComponent.class);

    private ComponentContext cc;
    private BundleContext bc;

    private ReferencedSiteConfigurationImpl siteConfiguration;

    private boolean dereferencerEqualsEntitySearcherComponent;
    /**
     * The {@link OfflineMode} is used by Stanbol to indicate that no external service should be referenced.
     * For the ReferencedSiteImpl this means that the {@link EntityDereferencer} and {@link EntitySearcher}
     * interfaces are no longer used.
     * <p>
     * 
     * @see #enableOfflineMode(OfflineMode)
     * @see #disableOfflineMode(OfflineMode)
     * @see #isOfflineMode()
     * @see #ensureOnline(String, Class)
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY,
        policy = ReferencePolicy.DYNAMIC,
        bind = "enableOfflineMode",
        unbind = "disableOfflineMode",
        strategy = ReferenceStrategy.EVENT)
    private OfflineMode offlineMode;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY,
            policy = ReferencePolicy.DYNAMIC,
            bind = "bindNamespacePrefixService",
            unbind = "unbindNamespacePrefixService",
            strategy = ReferenceStrategy.EVENT)
    private NamespacePrefixService nsPrefixService;

    private ServiceTracker cacheTracker;
    private Cache cache;

    private final Object searcherAndDereferencerLock = new Object();

    private ComponentFactoryListener searcherComponentFactoryListener;

    private ComponentFactoryListener dereferencerComponentFactoryListener;

    private ComponentInstance dereferencerComponentInstance;

    private ComponentInstance entitySearcherComponentInstance;
    
    private ServiceRegistration referencedSiteRegistration;
    
    private Site referencedSite;
    
    
    protected void bindNamespacePrefixService(NamespacePrefixService ps){
        this.nsPrefixService = ps;
        updateServiceRegistration(bc, siteConfiguration, 
            dereferencerComponentInstance, entitySearcherComponentInstance, 
            cache, nsPrefixService, offlineMode);
    }
    
    protected void unbindNamespacePrefixService(NamespacePrefixService ps){
        if(ps.equals(this.nsPrefixService)){
            this.nsPrefixService = null;
            updateServiceRegistration(bc, siteConfiguration, 
                dereferencerComponentInstance, entitySearcherComponentInstance, 
                cache, nsPrefixService, offlineMode);
        }
    }
    /**
     * Called by the ConfigurationAdmin to bind the {@link #offlineMode} if the service becomes available
     * 
     * @param mode
     */
    protected final void enableOfflineMode(OfflineMode mode) {
        this.offlineMode = mode;
        updateServiceRegistration(bc, siteConfiguration, 
            dereferencerComponentInstance, entitySearcherComponentInstance, 
            cache, nsPrefixService, offlineMode);

    }

    /**
     * Called by the ConfigurationAdmin to unbind the {@link #offlineMode} if the service becomes unavailable
     * 
     * @param mode
     */
    protected final void disableOfflineMode(OfflineMode mode) {
        if(offlineMode != null){
            this.offlineMode = null;
            updateServiceRegistration(bc, siteConfiguration, 
                dereferencerComponentInstance, entitySearcherComponentInstance, 
                cache, nsPrefixService, offlineMode);
        }
    }

    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(final ComponentContext ctx) throws ConfigurationException, YardException,
            InvalidSyntaxException {
        log.debug("in {} activate with properties {}", ReferencedSiteImpl.class.getSimpleName(),
            ctx.getProperties());
        if (ctx == null || ctx.getProperties() == null) {
            throw new IllegalStateException(
                    "No Component Context and/or Dictionary properties object parsed to the acticate methode");
        }
        this.cc = ctx;
        this.bc = ctx.getBundleContext();
        // create the SiteConfiguration based on the parsed properties
        // NOTE that the constructor also validation of the parsed configuration
        siteConfiguration = new ReferencedSiteConfigurationImpl(ctx.getProperties());
        if (PROHIBITED_SITE_IDS.contains(siteConfiguration.getId().toLowerCase())) {
            throw new ConfigurationException(SiteConfiguration.ID, String.format(
                "The ID '%s' of this Referenced Site is one of the following "
                        + "prohibited IDs: {} (case insensitive)", siteConfiguration.getId(),
                PROHIBITED_SITE_IDS));
        }
        
        log.info(" > initialise Referenced Site {}", siteConfiguration.getName());

        // if the accessUri is the same as the queryUri and both the
        // dereferencer and the entitySearcher uses the same component, than we 
        //need only one component for both dependencies.
        this.dereferencerEqualsEntitySearcherComponent =
                // (1) accessURI == queryURI
                siteConfiguration.getAccessUri() != null
                && siteConfiguration.getAccessUri().equals(siteConfiguration.getQueryUri())
                // (2) entity dereferencer == entity searcher
                && siteConfiguration.getEntityDereferencerType() != null
                && siteConfiguration.getEntityDereferencerType().equals(
                    siteConfiguration.getEntitySearcherType());

        // init the fieldMapper based on the configuration
        FieldMapper fieldMappings = new DefaultFieldMapperImpl(ValueConverterFactory.getDefaultInstance());
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
        // now init the referenced Services
        initDereferencerAndEntitySearcher();

        // If a cache is configured init the ServiceTracker used to manage the
        // Reference to the cache!
        if (siteConfiguration.getCacheId() != null) {
            String cacheFilter =
                    String.format("(&(%s=%s)(%s=%s))", Constants.OBJECTCLASS, Cache.class.getName(),
                        Cache.CACHE_YARD, siteConfiguration.getCacheId());
            cacheTracker = new ServiceTracker(ctx.getBundleContext(), 
                ctx.getBundleContext().createFilter( cacheFilter), 
                new ServiceTrackerCustomizer() {
                    
                    @Override
                    public void removedService(ServiceReference reference, Object service) {
                        if(service.equals(cache)){
                            cache = (Cache)cacheTracker.getService();
                            updateServiceRegistration(bc, siteConfiguration, 
                                dereferencerComponentInstance, entitySearcherComponentInstance, 
                                cache, nsPrefixService, offlineMode);
                        }
                        bc.ungetService(reference);
                    }
                    
                    @Override
                    public void modifiedService(ServiceReference reference, Object service) {
                        //the service.ranking might have changed ... so check if the
                        //top ranked Cache is a different one
                        Cache newCache = (Cache)cacheTracker.getService();
                        if(newCache == null || !newCache.equals(cache)){
                            cache = newCache; //set the new cahce
                            //and update the service registration
                            updateServiceRegistration(bc, siteConfiguration, 
                                dereferencerComponentInstance, entitySearcherComponentInstance, 
                                cache, nsPrefixService, offlineMode);
                        }
                    }
                    
                    @Override
                    public Object addingService(ServiceReference reference) {
                        Object service = bc.getService(reference);
                        if(service != null){
                            if(cacheTracker.getServiceReference() == null || //the first added Service or
                                    //the new service as higher ranking as the current
                                    (reference.compareTo(cacheTracker.getServiceReference()) > 0)){
                                cache = (Cache)service;
                                updateServiceRegistration(bc, siteConfiguration, 
                                    dereferencerComponentInstance, entitySearcherComponentInstance, 
                                    cache, nsPrefixService, offlineMode);
                            } // else the new service has lower ranking as the currently use one
                        } //else service == null -> ignore
                        return service;
                    }
                });
            cacheTracker.open();
        }
    }

    /**
     * Initialise the dereferencer and searcher component as soon as the according {@link ComponentFactory}
     * gets registered.
     * <p>
     * First this Methods tries to find the according {@link ServiceReference}s directly. If they are not
     * available (e.g. because the component factories are not yet started) than it adds a
     * {@link ServiceListener} for the missing {@link ComponentFactory} that calls the
     * {@link #createDereferencerComponent(ComponentFactory)} and
     * {@link #createEntitySearcherComponent(ComponentFactory)} as soon as the factory gets registered.
     * 
     * @throws InvalidSyntaxException
     *             if the #entitySearcherComponentName or the {@link #dereferencerComponentName} somehow cause
     *             an invalid formated string that can not be used to parse a {@link Filter}.
     */
    private void initDereferencerAndEntitySearcher() throws InvalidSyntaxException {
        if (siteConfiguration.getAccessUri() != null && // initialise only if a
                                                        // accessUri
                !siteConfiguration.getAccessUri().isEmpty() && // is configured
                siteConfiguration.getEntitySearcherType() != null) {
            String componentNameFilterString =
                    String.format("(%s=%s)", "component.name", siteConfiguration.getEntitySearcherType());
            String filterString =
                    String.format("(&(%s=%s)%s)", Constants.OBJECTCLASS, ComponentFactory.class.getName(),
                        componentNameFilterString);
            ServiceReference[] refs =
                    bc.getServiceReferences(ComponentFactory.class.getName(),
                        componentNameFilterString);
            if (refs != null && refs.length > 0) {
                createEntitySearcherComponent((ComponentFactory) bc.getService(
                    refs[0]));
            } else { // service factory not yet available -> add servicelistener
                this.searcherComponentFactoryListener =
                        new ComponentFactoryListener(bc);
                // NOTE: here the filter MUST include also the objectClass!
                bc.addServiceListener(this.searcherComponentFactoryListener,
                    filterString);
            }
            // context.getComponentInstance().dispose();
            // throw an exception to avoid an successful activation
        }
        if (siteConfiguration.getQueryUri() != null
                && // initialise only if a query URI
                !siteConfiguration.getQueryUri().isEmpty()
                && // is configured
                siteConfiguration.getEntityDereferencerType() != null
                && !this.dereferencerEqualsEntitySearcherComponent) {
            String componentNameFilterString =
                    String.format("(%s=%s)", "component.name", siteConfiguration.getEntityDereferencerType());
            String filterString =
                    String.format("(&(%s=%s)%s)", Constants.OBJECTCLASS, ComponentFactory.class.getName(),
                        componentNameFilterString);
            ServiceReference[] refs =
                    bc.getServiceReferences(ComponentFactory.class.getName(),
                        componentNameFilterString);
            if (refs != null && refs.length > 0) {
                createDereferencerComponent((ComponentFactory) bc.getService(refs[0]));
            } else { // service factory not yet available -> add servicelistener
                this.dereferencerComponentFactoryListener =
                        new ComponentFactoryListener(bc);
                this.bc.addServiceListener(this.dereferencerComponentFactoryListener,
                    filterString); // NOTE: here the filter MUST
                                   // include also the objectClass!
            }
        }
    }

    /**
     * Creates the entity searcher component used by this {@link Site} (and configured via the
     * {@link SiteConfiguration#ENTITY_SEARCHER_TYPE} property).
     * <p>
     * If the {@link SiteConfiguration#ENTITY_DEREFERENCER_TYPE} is set to the same vale and the
     * {@link #accessUri} also equals the {@link #queryUri}, than the component created for the entity
     * searcher is also used as dereferencer.
     * 
     * @param factory
     *            The component factory used to create the {@link #entitySearcherComponentInstance}
     */
    @SuppressWarnings("unchecked")
    protected void createEntitySearcherComponent(ComponentFactory factory) {
        // both create*** methods sync on the searcherAndDereferencerLock to
        // avoid
        // multiple component instances because of concurrent calls
        synchronized (searcherAndDereferencerLock) {
            if (entitySearcherComponentInstance == null) {
                entitySearcherComponentInstance =
                        factory.newInstance(OsgiUtils.copyConfig(cc.getProperties()));
                if(dereferencerEqualsEntitySearcherComponent){
                    //use the same instance for dereferencer and entity searcher
                    dereferencerComponentInstance = entitySearcherComponentInstance;
                }
                updateServiceRegistration(bc, siteConfiguration, 
                    dereferencerComponentInstance, entitySearcherComponentInstance, 
                    cache, nsPrefixService, offlineMode);
            }
        }
    }

    /**
     * Creates the entity dereferencer component used by this {@link Site}. The implementation used as the
     * dereferencer is configured by the {@link SiteConfiguration#ENTITY_DEREFERENCER_TYPE} property.
     * 
     * @param factory
     *            the component factory used to create the {@link #dereferencer}
     */
    @SuppressWarnings("unchecked")
    protected void createDereferencerComponent(ComponentFactory factory) {
        // both create*** methods sync on searcherAndDereferencerLock to avoid
        // multiple component instances because of concurrent calls
        synchronized (searcherAndDereferencerLock) {
            if (dereferencerComponentInstance == null) {
                dereferencerComponentInstance =
                        factory.newInstance(OsgiUtils.copyConfig(cc.getProperties()));
                updateServiceRegistration(bc, siteConfiguration, 
                    dereferencerComponentInstance, entitySearcherComponentInstance, 
                    cache, nsPrefixService, offlineMode);
            }
        }
    }

    /**
     * Simple {@link ServiceListener} implementation that is used to get notified if one of the
     * {@link ComponentFactory component factories} for the configured implementation of the
     * {@link EntityDereferencer} or {@link EntitySearcher} interfaces get registered.
     * 
     * @author Rupert Westenthaler
     * 
     */
    private class ComponentFactoryListener implements ServiceListener {
        private BundleContext bundleContext;

        protected ComponentFactoryListener(BundleContext bundleContext) {
            if (bundleContext == null) {
                throw new IllegalArgumentException("The BundleContext MUST NOT be NULL!");
            }
            this.bundleContext = bundleContext;
        }

        @Override
        public void serviceChanged(ServiceEvent event) {
            Object eventComponentName = event.getServiceReference().getProperty("component.name");
            if (event.getType() == ServiceEvent.REGISTERED) {
                log.info("Process ServiceEvent for ComponentFactory {} and State REGISTERED",
                    eventComponentName);
                ComponentFactory factory =
                        (ComponentFactory) bundleContext.getService(event.getServiceReference());
                if (siteConfiguration.getEntityDereferencerType() != null
                        && siteConfiguration.getEntityDereferencerType().equals(eventComponentName)) {
                    createDereferencerComponent(factory);
                }
                if (siteConfiguration.getEntitySearcherType() != null
                        && siteConfiguration.getEntitySearcherType().equals(eventComponentName)) {
                    createEntitySearcherComponent(factory);
                }
            } else {
                log.info("Ignore ServiceEvent for ComponentFactory {} and state {}", eventComponentName,
                    event.getType() == ServiceEvent.MODIFIED ? "MODIFIED"
                            : event.getType() == ServiceEvent.UNREGISTERING ? "UNREGISTERING"
                                    : "MODIFIED_ENDMATCH");
            }
        }

    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("deactivate Referenced Site {}", siteConfiguration.getName());
        if (this.dereferencerComponentInstance != null) {
            this.dereferencerComponentInstance.dispose();
            this.dereferencerComponentInstance = null;
        }
        if (this.entitySearcherComponentInstance != null) {
            this.entitySearcherComponentInstance.dispose();
            this.entitySearcherComponentInstance = null;
        }
        if (searcherComponentFactoryListener != null) {
            context.getBundleContext().removeServiceListener(searcherComponentFactoryListener);
            searcherComponentFactoryListener = null;
        }
        if (dereferencerComponentFactoryListener != null) {
            context.getBundleContext().removeServiceListener(dereferencerComponentFactoryListener);
            dereferencerComponentFactoryListener = null;
        }
        cache = null;
        if (cacheTracker != null) {
            cacheTracker.close();
            cacheTracker = null;
        }
        this.siteConfiguration = null;
        this.bc = null;
        this.cc = null;
        referencedSite = null;
        if(referencedSiteRegistration != null){
            referencedSiteRegistration.unregister();
            referencedSiteRegistration = null;
        }
    }
    /**
     * Updates the ServiceRegistration based on the parsed parameters
     * @param bc
     * @param config
     */
    @SuppressWarnings("unchecked")
    private synchronized void updateServiceRegistration(BundleContext bc, 
            ReferencedSiteConfiguration config, 
            ComponentInstance dereferencerComponentInstance,
            ComponentInstance searcherComponentInstance,
            Cache cache, NamespacePrefixService nsPrefixService,
            OfflineMode offlineMode){
        log.debug("> update ReferencedSite service:");
        if(referencedSiteRegistration != null){
            log.debug("  - unregister ReferencedSite '{}'",referencedSite.getId());
            referencedSiteRegistration.unregister();
            referencedSiteRegistration = null;
            referencedSite = null;
        }
        if(bc == null || config == null){
            log.debug(" - not active ... return");
            return;
        }
        //validate the parsed parameter
        boolean valid = true; //use state so that we check all services for logging
        log.debug(" - validate available services:");
        if(config.getAccessUri() != null &&
                config.getEntityDereferencerType() != null && 
                dereferencerComponentInstance == null){
            log.debug("    ... EntityDereference not available");
            valid = false;
        }
        if(config.getQueryUri() != null &&
                config.getEntitySearcherType() != null &&
                searcherComponentInstance == null){
            log.debug("    ... EntitySearcher not available");
            valid = false;
        }
        if(config.getCacheStrategy() != CacheStrategy.none &&
                cache == null){
            log.debug("    ... Local Cache not available");
            valid = false;
        }
        if(config.getCacheStrategy() != CacheStrategy.all &&
                offlineMode != null){
            log.debug("    ... Offline Mode is active and CacheStrategy != ALL");
            valid = false;
        }
        if(valid){
            log.debug("    ... all required Services present.");
            log.info(" - register ReferencedSite '{}'",config.getId());
            try {
                referencedSite = new ReferencedSiteImpl(config,
                    offlineMode == null && dereferencerComponentInstance != null ? 
                        (EntityDereferencer)dereferencerComponentInstance.getInstance() :
                            null,
                    offlineMode == null && searcherComponentInstance != null ? 
                        (EntitySearcher)searcherComponentInstance.getInstance() :
                            null,
                    cache, nsPrefixService);
                referencedSiteRegistration = bc.registerService(Site.class.getName(), referencedSite, 
                    OsgiUtils.copyConfig(cc.getProperties()));
            } catch (RuntimeException e) {
                log.warn("  ... unable to initialise ReferencedSite.",e);
            }
        }
    }

    
    
    
}

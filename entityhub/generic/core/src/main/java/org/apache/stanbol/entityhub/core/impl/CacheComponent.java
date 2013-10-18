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

import java.util.Collection;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.entityhub.core.site.CacheImpl;
import org.apache.stanbol.entityhub.core.utils.OsgiUtils;
import org.apache.stanbol.entityhub.servicesapi.yard.Cache;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * OSGI component for {@link CacheImpl}
 * @since 0.12.0
 * @author Rupert Westenthaler
 *
 */
@Component(
    //set the Class name of CacheImpl for backward compatibility
    name="org.apache.stanbol.entityhub.core.site.CacheImpl",
    configurationFactory = true,
    policy = ConfigurationPolicy.REQUIRE, //the baseUri is required!
    specVersion = "1.1",
    metatype = true,
    immediate = true)
@Properties(
value = {
    @Property(name = Cache.CACHE_YARD), 
    @Property(name = Cache.ADDITIONAL_MAPPINGS, cardinality = Integer.MAX_VALUE)})
public class CacheComponent {

    private final Logger log = LoggerFactory.getLogger(CacheComponent.class);
    
    private ServiceTracker yardTracker;
    private Yard yard;
    private ComponentContext cc;
    
    private ServiceRegistration cacheRegistration;
    private Cache cache;
    private String[] additionalMappings;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY,
            policy = ReferencePolicy.DYNAMIC,
            bind = "bindNamespacePrefixService",
            unbind = "unbindNamespacePrefixService",
            strategy = ReferenceStrategy.EVENT)
    private NamespacePrefixService nsPrefixService;

    
    protected void bindNamespacePrefixService(NamespacePrefixService ps){
        this.nsPrefixService = ps;
        updateServiceRegistration(cc, yard, additionalMappings, nsPrefixService);
    }
    
    protected void unbindNamespacePrefixService(NamespacePrefixService ps){
        if(ps.equals(this.nsPrefixService)){
            this.nsPrefixService = null;
            updateServiceRegistration(cc, yard, additionalMappings, nsPrefixService);
        }
    }


    @Activate
    protected void activate(final ComponentContext context) throws ConfigurationException, YardException, IllegalStateException, InvalidSyntaxException {
        if (context == null || context.getProperties() == null) {
            throw new IllegalStateException(String.format("Invalid ComponentContext parsed in activate (context=%s)", context));
        }
        this.cc = context;
        Object value = context.getProperties().get(Cache.ADDITIONAL_MAPPINGS);
        if(value instanceof String[]){
            this.additionalMappings = (String[])value;
        } else if(value instanceof String){
            this.additionalMappings = new String[]{(String)value};
        } else if(value instanceof Collection<?>){
            try {
                additionalMappings = ((Collection<?>)value).toArray(new String[((Collection<?>)value).size()]);
            } catch (ArrayStoreException e) {
                throw new ConfigurationException(Cache.ADDITIONAL_MAPPINGS, 
                    "Additional Mappings MUST BE a String, String[] or Collection<String>!",e);
            }
        } else {
            additionalMappings = null;
        }
        String yardId = OsgiUtils.checkProperty(context.getProperties(), Cache.CACHE_YARD).toString();
        String cacheFilter = String.format("(&(%s=%s)(%s=%s))", Constants.OBJECTCLASS, Yard.class.getName(), Yard.ID, yardId);
        yardTracker = new ServiceTracker(context.getBundleContext(), context.getBundleContext().createFilter(cacheFilter), 
            new ServiceTrackerCustomizer() {
            //store the reference to the ComponentContext to avoid NPE if deactivate
            //is called for the CacheComponent
            final ComponentContext cc = context;
            @Override
            public void removedService(ServiceReference reference, Object service) {
                if(service.equals(yard)){
                    yard = (Yard)yardTracker.getService();
                    updateServiceRegistration(cc, yard, additionalMappings, nsPrefixService);
                }
                cc.getBundleContext().ungetService(reference);
            }
            
            @Override
            public void modifiedService(ServiceReference reference, Object service) {
                //the service.ranking might have changed ... so check if the
                //top ranked Cache is a different one
                Yard newYard = (Yard)yardTracker.getService();
                if(newYard == null || !newYard.equals(cache)){
                    yard = newYard; //set the new cahce
                    //and update the service registration
                    updateServiceRegistration(cc, yard, additionalMappings, nsPrefixService);
                }
            }
            
            @Override
            public Object addingService(ServiceReference reference) {
                Object service = cc.getBundleContext().getService(reference);
                if(service != null){
                    if(yardTracker.getServiceReference() == null || //the first added Service or
                            //the new service as higher ranking as the current
                            (reference.compareTo(yardTracker.getServiceReference()) > 0)){
                        yard = (Yard)service;
                        updateServiceRegistration(cc, yard, additionalMappings, nsPrefixService);
                    } // else the new service has lower ranking as the currently use one
                } //else service == null -> ignore
                return service;
            }
        });
        yardTracker.open();
    }
    
    @Deactivate
    protected void deactivate(ComponentContext context) {
        this.yardTracker.close();
        this.yardTracker = null;
        this.cc = null;
        this.yard = null;
        this.cache = null;
        if(this.cacheRegistration != null){
            cacheRegistration.unregister();
            cacheRegistration = null;
        }
    }

    private synchronized void updateServiceRegistration(ComponentContext cc, Yard yard, String[] additionalMappings, NamespacePrefixService nsPrefixService) {
        if(cacheRegistration != null){
            cacheRegistration.unregister();
            cacheRegistration = null;
            cache = null;
        }
        if(cc != null && yard != null){
            try {
                cache = new CacheImpl(yard,additionalMappings, nsPrefixService);
            } catch (YardException e) {
                log.warn("Unable to init Cache for Yard '"+yard.getId()+"'!",e);
            }
            cacheRegistration = cc.getBundleContext().registerService(Cache.class.getName(), cache, 
                OsgiUtils.copyConfig(cc.getProperties()));
        }
        
    }

    
}

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
package org.apache.stanbol.commons.namespaceprefix.provider.prefixcc.component;

import java.math.BigDecimal;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

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
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixProvider;
import org.apache.stanbol.commons.namespaceprefix.provider.prefixcc.PrefixccProvider;
import org.apache.stanbol.commons.stanboltools.offline.OfflineMode;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGI component configuring and registering the {@link PrefixccProvider}.
 * Non-OSGI users do not need to bother with this. <p>
 * This class mainly exists to keep the {@link PrefixccProvider} independent of
 * the Stanbol {@link OfflineMode} switch
 * 
 * @author Rupert Westenthaler
 *
 */
@Component(immediate=true,policy=ConfigurationPolicy.OPTIONAL,metatype = true)
@Properties(value={
    @Property(name=PrefixccProviderComponent.UPDATE_INTERVAL,intValue=60),
    @Property(name=Constants.SERVICE_RANKING,intValue=-100)
})
public class PrefixccProviderComponent {

    private Logger log = LoggerFactory.getLogger(PrefixccProviderComponent.class);
    
    /**
     * The duration 
     */
    public static final String UPDATE_INTERVAL = "stanbol.commons.namespaceprovider.prefixcc.update-interval";
    public static final int DEFAULT_UPDATE_INTERVAL = 60;
    
    ServiceRegistration providerRegistration;
    
    @Reference(cardinality=ReferenceCardinality.OPTIONAL_UNARY,
            policy=ReferencePolicy.DYNAMIC,strategy=ReferenceStrategy.EVENT,
            bind="bindOfflineMode",unbind="unbindOfflineMode")
    private OfflineMode offlineMode;
    
    int updateInterval;
    private Dictionary<String,Object> providerProperties = null;
    private BundleContext bc;
    private PrefixccProvider provider;
    
    protected void bindOfflineMode(OfflineMode mode){
        this.offlineMode = mode;
        updateProviderState();
    }
    protected void unbindOfflineMode(OfflineMode mode){
        this.offlineMode = null;
        updateProviderState();
    }
    
    @Activate
    protected void activate(ComponentContext ctx) throws ConfigurationException {
        bc = ctx.getBundleContext();
        Object value = ctx.getProperties().get(UPDATE_INTERVAL);
        if(value instanceof Number){
            updateInterval = ((Number)value).intValue();
        } else if(value != null && !value.toString().isEmpty()){
            try {
                updateInterval = new BigDecimal(value.toString()).intValue();
            } catch (NumberFormatException e) {
                throw new ConfigurationException(UPDATE_INTERVAL, 
                    "Unable to parse integer value from the configured value '"
                    + value +"' (type: "+value.getClass()+")");
            }
        } else {
            updateInterval = DEFAULT_UPDATE_INTERVAL;
        }
        if(updateInterval < 0){
            log.warn("Negative update interval '{}' configured. Will use default '{}'!",
                updateInterval,DEFAULT_UPDATE_INTERVAL);
            updateInterval = DEFAULT_UPDATE_INTERVAL;
        } else if(updateInterval == 0){
            updateInterval = DEFAULT_UPDATE_INTERVAL;
        }
        //we need to copy over the service ranking
        providerProperties = new Hashtable<String,Object>();
        Object ranking = ctx.getProperties().get(Constants.SERVICE_RANKING);
        if(ranking != null){
            providerProperties.put(Constants.SERVICE_RANKING, ranking);
        }
        updateProviderState();
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx){
        providerProperties = null;
        updateProviderState();
        bc = null;
    }
    
    /**
     * uses the {@link #providerProperties} and {@link #offlineMode}
     * state to decide if the {@link PrefixccProvider} should be registered as
     * a service or not. If the current state is different the desired state it
     * creates and register / unregister destroys the {@link #provider<p>
     * Consumes: {@link #providerProperties} and {@link #offlineMode}<br>
     * Manages: {@link #provider} and {@link #providerRegistration}
     */
    private synchronized void updateProviderState(){
        if(providerProperties != null && offlineMode == null){ //register
            if(providerRegistration == null){
                provider = new PrefixccProvider(updateInterval, TimeUnit.MINUTES);
                providerRegistration = bc.registerService(
                    NamespacePrefixProvider.class.getName(), provider, providerProperties);
                log.info("registered prefix.cc NamespacePrefixProvider ...");
            }
        } else { //unregister
            if(providerRegistration != null){
                providerRegistration.unregister();
                log.info("unregistered prefix.cc NamespacePrefixProvider ...");
            }
            if(provider != null){
                provider.close();
                provider = null;
            }
        }
    }
    
    
}

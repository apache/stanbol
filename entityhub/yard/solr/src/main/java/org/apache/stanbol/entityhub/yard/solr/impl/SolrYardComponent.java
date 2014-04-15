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
package org.apache.stanbol.entityhub.yard.solr.impl;

import static org.apache.stanbol.entityhub.core.yard.AbstractYard.DEFAULT_QUERY_RESULT_NUMBER;
import static org.apache.stanbol.entityhub.core.yard.AbstractYard.MAX_QUERY_RESULT_NUMBER;
import static org.apache.stanbol.entityhub.servicesapi.yard.Yard.DESCRIPTION;
import static org.apache.stanbol.entityhub.servicesapi.yard.Yard.ID;
import static org.apache.stanbol.entityhub.servicesapi.yard.Yard.NAME;
import static org.apache.stanbol.entityhub.yard.solr.impl.SolrYardConfig.ALLOW_INITIALISATION_STATE;
import static org.apache.stanbol.entityhub.yard.solr.impl.SolrYardConfig.DEFAULT_ALLOW_INITIALISATION_STATE;
import static org.apache.stanbol.entityhub.yard.solr.impl.SolrYardConfig.DEFAULT_MAX_BOOLEAN_CLAUSES;
import static org.apache.stanbol.entityhub.yard.solr.impl.SolrYardConfig.DEFAULT_SOLR_INDEX_CONFIGURATION_NAME;
import static org.apache.stanbol.entityhub.yard.solr.impl.SolrYardConfig.MAX_BOOLEAN_CLAUSES;
import static org.apache.stanbol.entityhub.yard.solr.impl.SolrYardConfig.MULTI_YARD_INDEX_LAYOUT;
import static org.apache.stanbol.entityhub.yard.solr.impl.SolrYardConfig.SOLR_INDEX_CONFIGURATION_NAME;
import static org.apache.stanbol.entityhub.yard.solr.impl.SolrYardConfig.SOLR_SERVER_LOCATION;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

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
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.core.SolrCore;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.commons.solr.IndexReference;
import org.apache.stanbol.commons.solr.RegisteredSolrServerTracker;
import org.apache.stanbol.commons.solr.managed.IndexMetadata;
import org.apache.stanbol.commons.solr.managed.ManagedIndexState;
import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * OSGI Component that configures and registers the {@link SolrYard} service
 * as soon as all services referenced by the Configuration are available<p>
 * <b>NOTE:</b> This component uses the name <code>org.apache.stanbol.entityhub.yard.solr.impl.SolrYard</code>
 * to make it backward compatible with SolrYard configurations of version
 * <code>0.11.0</code>!
 * @author Rupert Westenthaler
 * @since 0.12.0
 *
 */
@Component(
    //set the name to the old class name of the SolrYard so that old
    //configurations do still work
    name="org.apache.stanbol.entityhub.yard.solr.impl.SolrYard",
    metatype = true, 
    immediate = true,
    configurationFactory = true, 
    policy = ConfigurationPolicy.REQUIRE,
    specVersion = "1.1")
@Properties(
    value = {
         // NOTE: Added propertied from AbstractYard to fix ordering!
         @Property(name = ID),
         @Property(name = NAME),
         @Property(name = DESCRIPTION),
         @Property(name = DEFAULT_QUERY_RESULT_NUMBER, intValue = -1),
         @Property(name = MAX_QUERY_RESULT_NUMBER, intValue = -1),
         // BEGIN SolrYard specific Properties
         @Property(name = SOLR_SERVER_LOCATION),
         @Property(name = ALLOW_INITIALISATION_STATE,boolValue=DEFAULT_ALLOW_INITIALISATION_STATE),
         @Property(name = SOLR_INDEX_CONFIGURATION_NAME, value=DEFAULT_SOLR_INDEX_CONFIGURATION_NAME),
         @Property(name = MULTI_YARD_INDEX_LAYOUT,boolValue=false),
         @Property(name = MAX_BOOLEAN_CLAUSES, intValue = DEFAULT_MAX_BOOLEAN_CLAUSES)})
public class SolrYardComponent {

    private final Logger log = LoggerFactory.getLogger(SolrYardComponent.class);
    
    private BundleContext context;
    private SolrYardConfig config;
    /**
     * The SolrServer used for the registered SorYard
     */
    private SolrServer solrServer;
    /**
     * The {@link ServiceRegistration} for the {@link SolrYard}
     */
    private ServiceRegistration yardRegistration;
    /**
     * Optionally a {@link ManagedSolrServer} that is used to create new 
     * Solr indexes based on parsed configurations.
     */
    @Reference(cardinality=ReferenceCardinality.OPTIONAL_UNARY,
        bind="bindManagedSolrServer",
        unbind="unbindManagedSolrServer",
        strategy=ReferenceStrategy.EVENT,
        policy=ReferencePolicy.DYNAMIC)
    private ManagedSolrServer managedSolrServer;

    private RegisteredSolrServerTracker registeredServerTracker;
    
    protected void bindManagedSolrServer(ManagedSolrServer manager){
        SolrYardConfig config = this.config;
        log.info(" ... bind ManagedSolrServer '{}' to SolrYardConfig '{}'",
            manager.getServerName(),config != null ? config.getId() : "<not yet activated>");
        this.managedSolrServer = manager;
        if(config != null){ //if activated
            try {
                initManagedSolrIndex(manager, config);
            } catch (Exception e) {
                log.error("Exception while checking SolrIndex '"+ config.getSolrServerLocation()
                    +"' on ManagedSolrServer '"+manager.getServerName()+"'!",e);
            }
        }
    }
    
    protected void unbindManagedSolrServer(ManagedSolrServer manager){
        SolrYardConfig config = this.config;
        log.info(" ... unbind ManagedSolrServer '{}' from SolrYard '{}'",
            manager.getServerName(),config != null ? config.getId() : "<not yet activated>");
        this.managedSolrServer = null;
    }
    /**
     * Optionally a {@link NamespacePrefixService} that is set to the SolrYard
     */
    @Reference(cardinality=ReferenceCardinality.OPTIONAL_UNARY,
        bind="bindNamespacePrefixService",
        unbind="unbindNamespacePrefixService",
        strategy=ReferenceStrategy.EVENT,
        policy=ReferencePolicy.DYNAMIC)
    private NamespacePrefixService nsPrefixService;

    private SolrYard yard;
    
    protected void bindNamespacePrefixService(NamespacePrefixService nsPrefixService){
        this.nsPrefixService = nsPrefixService;
        updateSolrYardRegistration(solrServer, config);
    }
    
    protected void unbindNamespacePrefixService(NamespacePrefixService nsPrefixService){
        this.nsPrefixService = null;
        updateSolrYardRegistration(solrServer, config);
    }
    
    @Activate
    protected void activate(ComponentContext ctx) throws ConfigurationException {
        this.context = ctx.getBundleContext();
        @SuppressWarnings("unchecked")
        SolrYardConfig config = new SolrYardConfig((Dictionary<String,Object>) ctx.getProperties());
        log.info("activate {} (name:{})",getClass().getSimpleName(),config.getId());
        String indexLocation = config.getSolrServerLocation();
        if(indexLocation.startsWith("http") && indexLocation.indexOf("://") > 0){
            solrServer = new HttpSolrServer(indexLocation);
            //directly register configs that use a remote server
            updateSolrYardRegistration(solrServer, config);
        } else { //locally managed Server
            IndexReference solrServerRef = IndexReference.parse(config.getSolrServerLocation());
            //We do not (yet) support creating SolrIndexes on ManagedSolrServers other than the
            //default
            if(config.isAllowInitialisation() && solrServerRef.getServer() != null){
                throw new ConfigurationException(SolrYardConfig.SOLR_SERVER_LOCATION,
                    "The SolrServerLocation ({server-name}:{index-name}) MUST NOT define a " 
                            + "{server-name} if '"+SolrYardConfig.ALLOW_INITIALISATION_STATE
                            + "' is enabled. Change the cofiguration to use just a {index-name}");
            }
            //check if we need to init the SolrIndex
            initManagedSolrIndex(managedSolrServer, config);
        }
        //globally set the config
        this.config = config;
    }
    
    @Deactivate
    protected void deactivate(ComponentContext ctx){
        log.info("deactivate {} (name:{})",getClass().getSimpleName(),config.getId());
        if(registeredServerTracker != null){
            registeredServerTracker.close();
        }
        updateSolrYardRegistration(null, null); //unregister
        this.config = null;
        this.context = null;
    }
    
    
    /**
     * initialise ManagedSolrIndex and that starts tracking for the {@link SolrCore}
     * @param managedServer the managedServer to init the SolrCore (if necessary)
     * @param config the {@link SolrYardConfig}
     * @throws IllegalStateException if the initialization fails
     */
    private void initManagedSolrIndex(final ManagedSolrServer managedServer, final SolrYardConfig config) {
        if(managedServer == null || config == null){
            //this means that either no ManagedSolrServer is present or this
            //component was not yet activated ... will be called again
            return; 
        }
        IndexReference solrIndexRef = IndexReference.parse(config.getSolrServerLocation());
        if(config.isAllowInitialisation()){ //are we allowed to create the SolrServer
            //get the name of the config to be used (default: default.solrindex.zip")
            String configName = config.getIndexConfigurationName();
            IndexMetadata metadata = managedServer.getIndexMetadata(solrIndexRef.getIndex());
            if(metadata == null){ //create a new index
                log.info(" ... creating Managed SolrIndex {} (configName: {}) on Server {}",
                    new Object[]{solrIndexRef.getIndex(),configName,managedServer.getServerName()});
                try {
                    metadata = managedServer.createSolrIndex(solrIndexRef.getIndex(), configName, null);
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to create Managed SolrIndex "
                        + solrIndexRef.getIndex()+" (configName: "+configName+") on Server "
                        + managedServer.getServerName()+"!",e);
                }
            } else if(metadata.getState() != ManagedIndexState.ACTIVE){
                log.info(" ... activating Managed SolrIndex {} on Server {} (current state: {})",
                    new Object[]{solrIndexRef.getIndex(),managedServer.getServerName(),metadata.getState()});
                try {
                    managedServer.activateIndex(metadata.getIndexName());
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to activate Managed SolrIndex "
                            + solrIndexRef.getIndex()+" (configName: "+configName+") on Server "
                            + managedServer.getServerName()+"!",e);
                } catch (SAXException e) {
                    throw new IllegalStateException("Unable to activate Managed SolrIndex "
                            + solrIndexRef.getIndex()+" (configName: "+configName+") on Server "
                            + managedServer.getServerName()+"!",e);
                }
            } //else already active nothing todo
            solrIndexRef = metadata.getIndexReference();
        } //else the SolrServer will be supplied (e.g. created by installing a full index)
        try {
            registeredServerTracker = new RegisteredSolrServerTracker(
                context, solrIndexRef,null){
                    
                    @Override
                    public void removedService(ServiceReference reference, Object service) {
                        updateSolrYardRegistration(registeredServerTracker.getService(),config); 
                        super.removedService(reference, service);
                    }
                    

                    @Override
                    public void modifiedService(ServiceReference reference, Object service) {
                        updateSolrYardRegistration(registeredServerTracker.getService(),config);
                        super.modifiedService(reference, service);
                    }
                    
                    @Override
                    public SolrServer addingService(ServiceReference reference) {
                        SolrServer server = super.addingService(reference);
                        if(solrServer != null){
                            log.warn("Multiple SolrServer for IndexLocation {} available!",
                                config.getSolrServerLocation());
                        } else {
                            updateSolrYardRegistration(server,config);
                        }
                        return server;
                    }
                };
            log.info(" ... start tracking for SolrCore based on {}",solrIndexRef);
            registeredServerTracker.open(); //start tracking
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException("Unable to track Managed SolrIndex "
                    + solrIndexRef.getIndex() + "on Server "
                    + managedServer.getServerName()+"!",e);
        }
    }
    
    protected synchronized void updateSolrYardRegistration(SolrServer solrServer, SolrYardConfig config) {
        if(solrServer == null || config ==null){
            return; //ignore call
        }
        if(yardRegistration != null){
            log.info(" ... unregister SolrYard (name:{})",config.getId());
            yardRegistration.unregister();
            yardRegistration = null;
        }
        if(yard != null){
            yard.close();
            yard = null;
        }
        if(solrServer != null && config != null){
            log.info(" ... register SolrYard (name:{})",config.getId());
            yard = new SolrYard(solrServer,config, nsPrefixService);
            Dictionary<String,Object> properties = new Hashtable<String,Object>();
            //copy over configuration from the component (to make it easier to filter)
            for(Enumeration<String> keys = config.getDictionary().keys();keys.hasMoreElements();){
                String key = keys.nextElement();
                if(key.contains("stanbol.entityhub.yard") || Constants.SERVICE_RANKING.equals(key)){
                    properties.put(key,config.getDictionary().get(key));
                }
            }
            yardRegistration = context.registerService(Yard.class.getName(), yard, properties);
        }
    }

}

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
package org.apache.stanbol.commons.solr;

import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_CORE_DATA_DIR;
import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_CORE_DIR;
import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_CORE_INDEX_DIR;
import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_CORE_NAME;
import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_CORE_RANKING;
import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_CORE_SCHEMA;
import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_CORE_SERVER_ID;
import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_CORE_SOLR_CONF;
import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_SERVER_CORES;
import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_SERVER_DIR;
import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_SERVER_NAME;
import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_SERVER_PUBLISH_REST;
import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_SERVER_RANKING;
import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_SOLR_XML_NAME;
import static org.apache.stanbol.commons.solr.SolrConstants.SOLR_CONFIG_NAME;
import static org.apache.stanbol.commons.solr.SolrConstants.SOLR_SCHEMA_NAME;
import static org.apache.stanbol.commons.solr.SolrConstants.SOLR_XML_NAME;
import static org.osgi.framework.Constants.SERVICE_ID;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.hadoop.hdfs.protocol.UnregisteredNodeException;
import org.apache.solr.cloud.ZkController;
import org.apache.solr.cloud.ZkSolrResourceLoader;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.cloud.ZooKeeperException;
import org.apache.solr.core.CloseHook;
import org.apache.solr.core.ConfigSolr;
import org.apache.solr.core.ConfigSolrXml;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.SolrConfig;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.core.ZkContainer;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.IndexSchemaFactory;
import org.apache.stanbol.commons.solr.impl.OsgiSolrResourceLoader;
import org.apache.stanbol.commons.solr.impl.OsgiZkSolrResourceLoader;
import org.apache.zookeeper.KeeperException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class 'wraps' a Solr {@link CoreContainer} with all its registered 
 * {@link SolrCore}s and registers them as OSGI services. It therefore adapts
 * the components framework as used by Apache Solr to the OSGI.<p>
 * This class itself is no OSGI component, but is intended to be used by
 * other classes that allow to register/manage Solr {@link CoreContainer}
 * running within the same JVM.<p>
 * Properties set for CoreContainers are: <ul>
 * <li> {@link SolrConstants#PROPERTY_SERVER_NAME}: The name assigned to
 * the SolrServer as parsed by {@link SolrServerProperties#getServerName()}. If
 * not defined than the directory of the SolrServer is used as name
 * <li> {@link SolrConstants#PROPERTY_SERVER_DIR} the directory of the 
 * {@link CoreContainer} (SolrHome)
 * <li> {@link SolrConstants#PROPERTY_SOLR_XML_NAME}: the name of the
 * 'solr.xml' file
 * <li> {@link SolrConstants#PROPERTY_SERVER_RANKING}: The 
 * {@link Constants#SERVICE_RANKING} for the {@link CoreContainer} used for all
 * its core if not overridden.  
 * </ul>
 * <p>
 * Properties set for {@link SolrCore}s are:<ul>
 * <li> The {@link SolrConstants#PROPERTY_SERVER_NAME name} and 
 * {@link SolrConstants#PROPERTY_SERVER_DIR dir} of the 
 * {@link CoreContainer} this {@link SolrCore} is part of
 * <li> {@link SolrConstants#PROPERTY_CORE_NAME}: The name this SolrCore
 * is registered with the CoreContainer.
 * <li> {@link SolrConstants#PROPERTY_CORE_DIR}: The 
 * {@link CoreDescriptor#getInstanceDir() instance directory} of this SolrCore.
 * By default this is '{@link SolrConstants#PROPERTY_SERVER_DIR CoreContainer.dir}/
 * {@link SolrConstants#PROPERTY_CORE_NAME SolrCore.name}'
 * <li> The {@link SolrConstants#PROPERTY_CORE_SCHEMA} and 
 * {@link SolrConstants#PROPERTY_CORE_SOLR_CONF} holding the names of the
 * according core configuration files.
 * <li> The {@link SolrConstants#PROPERTY_CORE_DATA_DIR} and
 * {@link SolrConstants#PROPERTY_CORE_INDEX_DIR}.
 * <li> The {@link SolrConstants#PROPERTY_CORE_RANKING}: the 
 * {@link Constants#SERVICE_RANKING} for the {@link SolrCore}. If not set the
 * service ranking of the {@link CoreContainer} is used.
 * </ul>
 * <p>
 * <b>Notes:</b><ul>
 * <li>{@link CoreContainer} does not provide Events for changes in its configuration.
 * Therefore the OSGI service registration for the CoreContainer can only be
 * assured if the interface of this Class is used to change the state of the
 * CoreContainer.
 * <li> {@link SolrCore}s allow to register a {@link CloseHook} to get notified if a
 * SolrCore is closed. Therefore this implementation can assure that closed
 * {@link SolrCore}s are also unregistered as OSGI services.
 * </ul>
 * 
 * @author Rupert Westenthaler
 *
 */
public class SolrServerAdapter {

    private final Logger log = LoggerFactory.getLogger(SolrServerAdapter.class);
    
    private final Map<String,CoreRegistration> registrations;
    protected final CoreContainer server;
    protected final ServiceRegistration serverRegistration;
    protected final SolrServerProperties serverProperties;
    protected final BundleContext context;
    /**
     * Used to skip registration of OSGI services during the initialisation
     * of the {@link CoreContainer}. <p>
     * This is necessary to avoid deadlocks with
     * OSGI allowing only a single thread to register services and the
     * {@link CoreContainer} using a thread pool to initialise SolrCores during
     * startup. The combination of this would result in deadlock if the
     * {@link SolrServerAdapter} is created within a activate method and the
     * CoreContainer does contain more then a single {@link SolrCore} to load.
     * <p>
     * 
     */
    private boolean initialised = false;
    
    /**
     * This implements CloseHook as used by {@link SolrCore} to notify about
     * the event that a specific SolrCore was finally closed. This is the only
     * way to get notified if a SolrCore is removed by other means than using
     * this class (e.g. by using the AdminRequestHandler)
     */
    private final CloseHook closeHook = new CloseHook() {
        
        @Override
        public void preClose(SolrCore core) {
            log.debug("  ... in preClose SolrCore {}", core.getName());
            Collection<String> names = server.getCoreNames(core);
            if(names != null){
                synchronized (registrations) {
                    for(String name : names){
                        CoreRegistration coreRegistration = registrations.get(name);
                        //we need to check if the core registered for the 
                        //parsed name is still the same as parsed
                        if(coreRegistration != null){
                            if(coreRegistration.getCore().equals(core)){
                                log.info("unregister Core with name '{}' based on call to" +
                                    " CloseHook#close()",name);
                                CoreRegistration removed = registrations.remove(name);
                                if(removed != null){
                                    removed.unregister();
                                } //else removed in the meantime by an other thread ... nothing to do
                            } else {
                                log.info("Core registered for name '{}' is not the same as" +
                                        " parsed to CloseHook#close()",name);
                            }
                        } //else the core was removed by using the API of the SolrServerAdapter
                    }
                }
            }
            //update the OSGI service for the CoreContainer
            try {
                updateServerRegistration();
            } catch (IllegalStateException e) {
                log.debug("Not updating Server Registration (already unregistered)");
            }
        }

        @Override
        public void postClose(SolrCore core) {
            //If we want to delete SolrCores from Disc, this can be done here!
        }
    };
    /**
     * Creates and Initialise a Solr {@link CoreContainer} based on the provided
     * {@link SolrServerProperties} and registers it and all its configured 
     * {@link SolrCore}s as OSGI services by using the provided {@link BundleContext}.
     * initialise the {@link CoreContainer}
     * @param context Bundle Context
     * @param parsedServerProperties Solr Core properties
     * @throws SolrException if the Solr {@link CoreContainer} could not be 
     * created.
     * @throws IllegalArgumentException if any of the parsed parameters is
     * <code>null</code> or the {@link SolrServerProperties} do not contain a
     * valid value for the {@link SolrConstants#PROPERTY_SERVER_DIR} 
     * property.
     */
    public SolrServerAdapter(final BundleContext context,SolrServerProperties parsedServerProperties) {
        if(parsedServerProperties == null){
            throw new IllegalArgumentException("The prsed Server Properties MUST NOT be NULL!");
        }
        if(context == null){
            throw new IllegalArgumentException("The parsed BundlContext used to register " +
                    "the Solr Components as OSGI services MUST NOT be NULL!");
        }
        File solrDir = parsedServerProperties.getServerDir();
        if(solrDir == null){
            throw new IllegalArgumentException("The parsed SolrServerPropertis MUST contain a value for the '"+
                PROPERTY_SERVER_DIR+"' property (value: '"+
                parsedServerProperties.get(PROPERTY_SERVER_DIR)+"')");
        }
        this.context = context;
        //create a clone so that only we control who changes to the properties
        serverProperties = parsedServerProperties.clone();

        File solrCof = new File(solrDir,parsedServerProperties.getSolrXml());
        ClassLoader classLoader = updateContextClassLoader();
        CoreContainer container;
        try {
            log.info("   ... create OSGI enabled SolrCore (conf: {}",solrCof);
            SolrResourceLoader loader = new OsgiSolrResourceLoader(context, solrDir.getAbsolutePath(), 
                SolrServerAdapter.class.getClassLoader());
            container = new OsgiCoreContainer(loader, context,solrCof);
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
        this.server = container;
        this.registrations = Collections.synchronizedMap(
            new HashMap<String,CoreRegistration>());
        String serverName = serverProperties.getServerName();
        if(serverName == null){
            //set the name to the absolute path of the solr dir
            serverProperties.setServerName(solrDir.getAbsolutePath());
        }
        //now load the cores
        classLoader = updateContextClassLoader();
        try {
            container.load();
            log.info("      - loaded SolrConfig {}",solrCof);
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
        //add the currently available cores to the properties
        updateCoreNamesInServerProperties();
        //register the SolrServer
        this.serverRegistration = context.registerService(
            CoreContainer.class.getName(), server, serverProperties);
        //activate OSGI service registration on changes of the CoreContainer
        initialised = true;
        //register the cores;
        for(String coreName : server.getCoreNames()){
            SolrCore core = server.getCore(coreName);
            try {
                registerCoreService(coreName, core);
                core.addCloseHook(closeHook); //add a closeHook
            } finally { //decrease the reference count
                core.close();
            }
            
        }
    }
    /**
     * Shutdown the {@link CoreContainer} and all {@link SolrCore}s managed by 
     * this instance. This will also cause all OSGI services to be unregistered
     */
    public void shutdown(){
        log.debug(" ... in shutdown for SolrServer {}",serverProperties.getServerName());
        Collection<CoreRegistration> coreRegistrations;
        synchronized (registrations) {
            coreRegistrations = new ArrayList<CoreRegistration>(registrations.values());
        }
        for(CoreRegistration reg : coreRegistrations){
            reg.unregister();
            registrations.remove(reg.getName());
            log.debug("removed Registration for SolrCore {}",reg.getName());
        }
        //unregister the serviceRegistration for the CoreContainer
        serverRegistration.unregister();
        //shutdown the CoreContainer itself
        server.shutdown();
    }
    /**
     * Removes the SolrCore for with the given name. This will also unregister
     * the according OSGI service. Note that SolrCores can be registerd with
     * several names.
     * @param name the name of the core to remove
     */
    public void removeCore(String name){
        log.info("Remove Core {} on CoreContainer {}",name,serverProperties.getServerName());
        SolrCore core = server.remove(name);
        if(core != null){
            CoreRegistration reg = registrations.remove(name);
            if(reg != null){
                reg.unregister();
            }
            cleanupSolrCore(core);
            //server.persist();
            //update the OSGI service for the CoreContainer
            updateServerRegistration();
        } //else core already removed -> nothing to do

    }
    /**
     * Reloads a SolrCore e.g. to apply a change in its configuration
     * @param name the name of the Core to reload
     */
    public void reloadCore(String name) {
        //try to reload
        log.info("Reload Core {} on CoreContainer {}",name,serverProperties.getServerName());
        
        ClassLoader classLoader = updateContextClassLoader();
        try {
            //TODO: what happens if the core with 'name' is no longer present?
            server.reload(name);
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
        //if succeeded (re-)register the core
        registerCoreService(name,null);
        //update the OSGI service for the CoreContainer
        updateServerRegistration();

    }
    /**
     * Sets the {@link ClassLoader} of the {@link Thread#currentThread()} to the
     * ClassLoader of {@link SolrServerAdapter} to ensure that all needed
     * Solr dependencies are loaded via the Bundle Classpath of the
     * <code>org.apache.commons.solr</code> bundle.<p>
     * Make sure that the ClassLoader is reset to the original value - as
     * returned by this method by adding a 
     * <pre><code>
     *     ClassLoader classLoader = updateContextClassLoader();
     *     try {
     *         //init Solr component
     *     } finally {
     *         Thread.currentThread().setContextClassLoader(classLoader);
     *     }
     * </code></pre><p>
     * <b>TODO:</b><p>
     * This currently sets the ClassLoader of {@link SolrServerAdapter}
     * to set the {@link Thread#setContextClassLoader(ClassLoader)}. It would 
     * be better to explicitly get the ClassLoader of the Bundle providing the
     * Solr Classes.
     * @return the {@link ClassLoader} of {@link Thread#currentThread()} before
     * calling this method
     */
    private ClassLoader updateContextClassLoader() {
        /*
         * NOTE: We need to reset the ContextClassLoader to the one used for this Bundle, because Solr
         * uses this ClassLoader to load all the plugins configured in the SOLR_XML_NAME and
         * schema.xml. The finally block resets the context class loader to the previous value.
         * (Rupert Westenthaler 20010209)
         */
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(CoreContainer.class.getClassLoader());
        return classLoader;
    }
    /**
     * Swaps two cores
     * @param core1 the first core to swap
     * @param core2 the second core to swap
     */
    public void swap(String core1,String core2){
        log.info("Swap Core {} with Core {}on CoreContainer {}",
            new Object[]{core1,core2, serverProperties.getServerName()});
        //swap the cores
        server.swap(core1, core2);
        //if succeeded (re-)register the swapped core
        registerCoreService(core1,null);
        registerCoreService(core2,null);
        //update the OSGI service for the CoreContainer
        updateServerRegistration();
    }
    
    /**
     * Registers a SolrCore based on the parsed configuration. If a SolrCore
     * with the same name as provided by the configuration is already present
     * it will be replace by this one.
     * @param parsedCoreConfig The configuration.
     * @return The ServiceReference to the SolrCore.
     * @throws SolrException If the core could not be registered
     */
    public ServiceReference registerCore(SolrCoreProperties parsedCoreConfig) {
        SolrCoreProperties coreConfig = parsedCoreConfig.clone();
        String coreName = coreConfig.getCoreName();
        log.info("Register Core {} to SolrServerAdapter (coreContainer: {})",coreName,serverProperties.getServerName());
        if(coreName == null){
            coreName = server.getDefaultCoreName();
        }
        File coreDir = coreConfig.getCoreDir();
        if(coreDir == null){
            coreDir = new File(serverProperties.getServerDir(),coreName);
        }
        //SolrCore old = null;
        ClassLoader classLoader = updateContextClassLoader();
        SolrCore core;
        try {
            //NOTE: this code depends on the fact that the create method of the
            //      CoreContainer is overridden by the SolrServerAdapter
            //      to use the OSGI specific SolrResourceLoader!
            core = server.create(new CoreDescriptor(server, 
                coreName, coreDir.getPath()));
            //CloseHook is now appied by the overridden registerCore(..) method
            //of the wrapped CoreContainer!
            //core.addCloseHook(closeHook);
            // parse ture as third argument to avoid closing the current core for now
            //old = 
            server.register(coreName, core, true);
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
        //NOTE: core registration is now done as part of the registration of the
        //      SolrCore to the CoreContainer
//        ServiceReference coreRef = registerCoreService(coreName,core);
//        if(old != null){
//            //cleanup the old core
//            cleanupSolrCore(old);
//        }
//        // persist the new core to have it available on the next start
//        //server.persist();
//        //update the OSGI service is now done by the overridden CoreContainer#create(..)
//        //method
//        updateServerRegistration();
        //so just get the ServiceReference for the ServiceRegistration
        CoreRegistration reg = registrations.get(coreName);
        if (reg == null){
            throw new IllegalStateException("No OSGI ServiceRegistration present after "
                + "adding SolrCore '"+coreName+"' to SolrCore!");
        } else {
            return reg.getServiceReference();
        }
    }
    
    /**
     * Closes the parsed SolrCore
     * @param old the core to close
     */
    private void cleanupSolrCore(SolrCore old) {
        if(old == null){
            return;
        }
        old.close(); //this frees the reference hold by the SolrServerAdapter
        //We do no longer free additional references (that could be hold by other
        //OSGI components that do retrieve a SolrCore from the CoreContainer)
        //Those components will need to close their SolrCores themselves!
        //As it is expected that most components will use EmbeddedSolrServer
        //to use registered CoreContainer and SolrCores and this implementation
        //does correctly call SolrCore#close() on each retrieved SolrCore this
        //assumption will be true in most Situations.
        
//        if(!old.isClosed()){
//            log.warn("Old SolrCore was not Closed correctly - this indicates that some other" +
//                  "components calling CoreContainer#getSolrCore() has not colled SolrCore#close()" +
//                  "after using it.");
//            log.warn("To avoid memory leaks this will call SolrCore#close() until closed");
//            int i=0;
//            for(;!old.isClosed();i++){
//                old.close();
//            }
//            log.warn("   ... called SolrCore#close() {} times before closed",i);
//        }
    }
    /**
     * Registers a {@link SolrCore} as OSGI service with the some additional
     * metadata allowing other components to explicitly register for this
     * SolrCore
     * @param name the name of the core
     * @param core the Core or <code>null</code> if not available. If 
     * <code>null</code> is parsed the {@link SolrCore} will be looked up by
     * using the {@link #server}. This is mainly to do not increase the
     * {@link SolrCore#getOpenCount()}.
     * @return the registered service
     */
    protected ServiceReference registerCoreService(String name,SolrCore core) {
        //STANBOL-1235: we want to unregister the old before registering the new
        //   but we do not want all solrCores to be closed as otherwise the
        //   SolrCore would be deactivated/activated. So if we find a old
        //   registration we will acquire a 2nd reference to the same core
        //   for the time of the re-registration
        SolrCore sameCore = null;
        try {
            CoreRegistration current;
            synchronized (registrations) {
                CoreRegistration old = registrations.remove(name);
                //NOTE: we register the new before unregistering the old to allow
                //      uninterrupted usage of this core by components.
                current = new CoreRegistration(name,core);
                log.info("   ... register {}",current);
                registrations.put(name,current);
                if(old != null){
                    sameCore = this.server.getCore(name); //2nd reference to the core
                    log.info("  ... unregister old registration {}", old);
                    old.unregister();
                }
            }
            return current.getServiceReference();
        } finally {
            if(sameCore != null){ //clean up the 2nd reference
                sameCore.close();
            }
        }
    }
    
    /**
     * Returns the ServiceReference for the {@link SolrCore} of the core
     * with the parsed name
     * @param name the name of the core
     * @return the reference to the {@link SolrCore} or <code>null</code> if
     * not managed.
     */
    public ServiceReference getCore(String name) {
        CoreRegistration reg = registrations.get(name);
        return reg != null ? reg.getServiceReference() : null;
    }
    /**
     * Returns the ServiceReference for the {@link SolrCore} of the parsed
     * directory
     * @param directory the directory
     * @return the reference of <code>null</code> if no {@link SolrCore} for the
     * parsed directory is registered for this {@link CoreContainer}.
     */
    public ServiceReference getCoreForDir(String directory){
        //solr always uses ending '/'
        if(directory.charAt(directory.length()-1) != File.separatorChar){
            directory = directory+File.separatorChar;
        }
        synchronized (registrations) {
            for(CoreRegistration reg : registrations.values()){
                ServiceReference ref = reg.getServiceReference();
                if(FilenameUtils.equalsNormalizedOnSystem(
                    directory,(String)ref.getProperty(PROPERTY_CORE_DIR))){
                    return ref;
                }
            }
        }
        return null;
    }

    /**
     * Getter for a read-only list of cores that are currently managed by this
     * ManagedSolrServer
     * @return the read-only list of managed cores.
     */
    @SuppressWarnings("unchecked")
    public Collection<String> getCores() {
        return Collections.unmodifiableCollection(
            (Collection<String>)serverProperties.get(PROPERTY_SERVER_CORES));
    }

    /**
     * Checks if a core with this name is managed.
     * @param name the name
     * @return the state
     */
    public boolean isCore(String name) {
        CoreRegistration reg = registrations.get(name);
        return reg != null && reg.getServiceReference() != null;
    }
    /**
     * The Name of the registered {@link CoreContainer}
     * @return server name
     */
    public String getServerName(){
        Object value = serverRegistration.getReference().getProperty(PROPERTY_SERVER_NAME);
        return value == null ? null : value.toString();
    }
    /**
     * The Directory of the registered {@link CoreContainer}
     * @return Server Directory name
     */
    public String getServerDir(){
        Object value = serverRegistration.getReference().getProperty(PROPERTY_SERVER_DIR);
        return value == null ? null : value.toString();
        
    }
    /**
     * Getter for the {@link ServiceReference} for the {@link CoreContainer}
     * managed by this instance
     * @return the {@link ServiceReference}
     */
    public ServiceReference getServerReference(){
        return serverRegistration.getReference();
    }
    
    @Override
    public int hashCode() {
        return server.hashCode()+serverProperties.hashCode();
    }
    @Override
    public boolean equals(Object obj) {
        return obj instanceof SolrServerAdapter && 
            ((SolrServerAdapter)obj).server.equals(server) &&
            ((SolrServerAdapter)obj).context.equals(context) &&
            ((SolrServerAdapter)obj).serverProperties.equals(serverProperties);
    }
    
    @Override
    public String toString() {
        String serverName = getServerName();
        return String.format("ManagedSolrServerImps[server=%s,cores=%s]",
            (serverName==null?"default":serverName),getCores());
    }
    /**
     * @return
     */
    private Set<String> updateCoreNamesInServerProperties() {
        Set<String> coreNames = Collections.unmodifiableSet(
            new HashSet<String>(server.getCoreNames()));
        serverProperties.put(PROPERTY_SERVER_CORES, coreNames);
        return coreNames;
    }
    /**
     * Updates the {@link ServiceRegistration} of the {@link CoreContainer} after
     * some changes to the cores
     */
    private void updateServerRegistration() {
        serverRegistration.setProperties(serverProperties);
    }
    /**
     * We need to override some methods of the CoreContainer to
     * (1) ensure the OsigSolrResourceLoader is used
     * (2) update the OSGI service registrations
     * Previously this was done in the SolrServerAdapter, but to also support
     * ReferencedSolrServer (STANBOL-1081) we do it now directly for the
     * CoreContainer. This allows also to correctly load and register
     * cores that are created/changed via the Solr RESTful API
     * @author Rupert Westenthaler
     */
    private final class OsgiCoreContainer extends CoreContainer {
        private final BundleContext context;

        private OsgiCoreContainer(SolrResourceLoader loader, BundleContext context, File solrConf) {
            super(loader,ConfigSolr.fromFile(loader, solrConf));
            this.context = context;
        }

        //override this to ensure that the OsgiSolrResourceLodaer is used
        //to create SolrCores
        @Override
        public SolrCore create(CoreDescriptor dcore) {
            if (isShutDown()) {
                throw new SolrException(ErrorCode.SERVICE_UNAVAILABLE, "Solr has shutdown.");
            }
            log.info(" .... createCore {}:{}",serverProperties.getServerName(),dcore.getName());
            File idir = new File(dcore.getInstanceDir());
            String instanceDir = idir.getPath();
            SolrCore created;
            if (getZkController() != null) {
                created = createFromZk(instanceDir, dcore);
            } else {
                created = createFromLocal(dcore, instanceDir);
            }
            //TODO: solrCores is private ... 
            //solrCores.addCreated(created); // For persisting newly-created cores.
            return created;
        }

        /*
         * Create from local configuration (replaces the method with the same
         * name in the parent class)
         */
        private SolrCore createFromLocal(CoreDescriptor dcore, String instanceDir) {
            SolrResourceLoader loader = new OsgiSolrResourceLoader(context, instanceDir, 
                CoreContainer.class.getClassLoader());
            SolrConfig config;
            try {
                config = new SolrConfig(loader, dcore.getConfigName(), null);
            } catch (Exception e) {
                log.error("Failed to load file {}", new File(instanceDir, dcore.getConfigName()).getAbsolutePath());
                throw new SolrException(ErrorCode.SERVER_ERROR, "Could not load config for " + dcore.getConfigName(), e);
            }
            IndexSchema schema = null;
            if (indexSchemaCache != null) {
              final String resourceNameToBeUsed = IndexSchemaFactory.getResourceNameToBeUsed(dcore.getSchemaName(), config);
              File schemaFile = new File(resourceNameToBeUsed);
              if (!schemaFile.isAbsolute()) {
                schemaFile = new File(loader.getConfigDir(), schemaFile.getPath());
              }
              if (schemaFile.exists()) {
                String key = schemaFile.getAbsolutePath()
                    + ":"
                    + new SimpleDateFormat("yyyyMMddHHmmss", Locale.ROOT).format(new Date(
                    schemaFile.lastModified()));
                schema = indexSchemaCache.get(key);
                if (schema == null) {
                  log.info("creating new schema object for core: " + dcore.getProperty(CoreDescriptor.CORE_NAME));
                  schema = IndexSchemaFactory.buildIndexSchema(dcore.getSchemaName(), config);
                  indexSchemaCache.put(key, schema);
                } else {
                  log.info("re-using schema object for core: " + dcore.getProperty(CoreDescriptor.CORE_NAME));
                }
              }
            }

            if (schema == null) {
              schema = IndexSchemaFactory.buildIndexSchema(dcore.getSchemaName(), config);
            }

            SolrCore core = new SolrCore(dcore.getName(), null, config, schema, dcore);
            if (core.getUpdateHandler().getUpdateLog() != null) {
                // always kick off recovery if we are in standalone mode.
                core.getUpdateHandler().getUpdateLog().recoverFromLog();
            }
            return core;
        }
        /*
         * Create from Zookeeper (replaces the method with the same name in
         * {@link ZkContainer})
         */
        private SolrCore createFromZk(String instanceDir, CoreDescriptor dcore) {
            try {
                SolrResourceLoader solrLoader = null;
                SolrConfig config = null;
                String zkConfigName = null;
                IndexSchema schema;
                String collection = dcore.getCloudDescriptor().getCollectionName();
                ZkController zkController = getZkController();
                zkController.createCollectionZkNode(dcore.getCloudDescriptor());

                zkConfigName = zkController.readConfigName(collection);
                if (zkConfigName == null) {
                    log.error("Could not find config name for collection:" + collection);
                    throw new ZooKeeperException(SolrException.ErrorCode.SERVER_ERROR,
                            "Could not find config name for collection:" + collection);
                }
                solrLoader = new OsgiZkSolrResourceLoader(context, instanceDir, zkConfigName,
                        CoreContainer.class.getClassLoader(),
                        // TODO: Core properties are not accessible -> parse null
                        // ConfigSolrXml.getCoreProperties(instanceDir, dcore),
                        null, zkController);
                config = zkSys.getSolrConfigFromZk(zkConfigName, dcore.getConfigName(), solrLoader);
                schema = IndexSchemaFactory.buildIndexSchema(dcore.getSchemaName(), config);
                return new SolrCore(dcore.getName(), null, config, schema, dcore);

            } catch (KeeperException e) {
                log.error("", e);
                throw new ZooKeeperException(SolrException.ErrorCode.SERVER_ERROR, "", e);
            } catch (InterruptedException e) {
                // Restore the interrupted status
                Thread.currentThread().interrupt();
                log.error("", e);
                throw new ZooKeeperException(SolrException.ErrorCode.SERVER_ERROR, "", e);
            }
        }
        
        //this ensures that a closeHook is added to registered cores
        @Override
        protected SolrCore registerCore(boolean isTransientCore, String name, SolrCore core, boolean returnPrevNotClosed) {
            log.info(" .... registerCore {}:{}",serverProperties.getServerName(),name);
            SolrCore old =  super.registerCore(isTransientCore, name, core, returnPrevNotClosed);
            //NOTE: we can not register the services here, as this can trigger
            //      a deadlock!!
            //Reason: OSGI ensures that activation is done by a single thread.
            //        Solr uses a Threadpool to activate SolrCores. This means
            //        that this method is called in a different thread context 
            //        as the OSGI activation thread. However the registration
            //        of the SolrCore would try to re-sync on the OSGI activation
            //        Thread and therefore cause a deadlock as the
            //        constructor of the SolrServerAdapter is typically expected
            //        to be called within an activate method.
            //Solution: the 'initialised' switch is only set to TRUE after the
            //          initialisation of the CoreContainer. During initialisation
            //          the SolrCores ore only registered after the construction
            //          of the CoreContainer. This ensures that the OSGI
            //          activation thread context is used for registration
            //          If SolrCores are registered afterwards (e.g a SolrCore
            //          is added to a ManagedSolrServer) the registration is
            //          done as part of this method (because 'initialised' is
            //          already set to TRUE). 
            if(initialised){ //already initialised ?
                //register the core as OSGI service
                registerCoreService(name, core);
                updateCoreNamesInServerProperties();
                updateServerRegistration();
                //add a closeHook so that we know when to unregister
                core.addCloseHook(closeHook);
            } //else ignore registration during startup
            return old;
        }

        //in the case of a swap we need to update the OSGI service registrations
        @Override
        public void swap(String name1, String name2) {
            log.info(" .... swap {}:{} with {}:{}",new Object[]{
                    serverProperties.getServerName(),name1,
                    serverProperties.getServerName(),name2
            });
            super.swap(name1, name2);
            //also update the OSGI Service registrations
            if(initialised){
                registerCoreService(name1,null);
                registerCoreService(name2,null);
                //update the OSGI service for the CoreContainer
                updateCoreNamesInServerProperties();
                updateServerRegistration();
            } //else ignore registration during startup
        }
    }
    /**
     * Internally used to manage the OSGI service registration for
     * {@link SolrCore}s of the {@link CoreContainer} managed by this
     * {@link SolrServerAdapter} instance
     * @author Rupert Westenthaler
     *
     */
    private class CoreRegistration {
        protected final String name;
        protected SolrCore core;
        private ServiceRegistration registration;
        /**
         * Creates and registers a {@link CoreRegistration}
         * @param name the name used to register the core
         * @param parsedCore the SolrCore to register
         * @throws IllegalStateException if the parsed name is <code>null</code>
         * or empty; if the {@link SolrServerAdapter#server} does not know a
         * SolrCore with the parsed name or if the {@link SolrServerAdapter#context}
         * is no longer valid
         */
        protected CoreRegistration(String name, SolrCore parsedCore) {
            if(name == null || name.isEmpty()){
                throw new IllegalStateException("The name of a SolrCore MUST NOT be NULL nor emtpy");
            }
            this.name = name;
            if(parsedCore == null){
                this.core = server.getCore(name); //increases the reference count
            } else {
                this.core = parsedCore;
                parsedCore.open(); //increase the reference count!!
            }
            if(core == null){
                throw new IllegalStateException("Unable to getCore with name "+name+" from CoreContainer "+server);
            }
            Dictionary<String,Object> props = new Hashtable<String,Object>();
            props.put(PROPERTY_CORE_NAME, name);
            props.put(PROPERTY_CORE_DIR, core.getCoreDescriptor().getInstanceDir());
            props.put(PROPERTY_CORE_DATA_DIR, core.getDataDir());
            props.put(PROPERTY_CORE_INDEX_DIR, core.getIndexDir());
            props.put(PROPERTY_CORE_SCHEMA, core.getSchemaResource());
            props.put(PROPERTY_CORE_SOLR_CONF, core.getConfigResource());
            props.put(PROPERTY_SERVER_NAME, serverProperties.get(PROPERTY_SERVER_NAME));
            props.put(PROPERTY_SERVER_DIR, serverProperties.get(PROPERTY_SERVER_DIR));
            //looks like the SERVICE_PID property is not present within the metadata
            //so we use SERVICE_ID instead. However keep on mind that SERVIVE_ID
            //values change if a service is restarted.
//            props.put(PROPERTY_CORE_SERVER_PID, serverRegistration.getReference().getProperty(SERVICE_PID));
            props.put(PROPERTY_CORE_SERVER_ID, serverRegistration.getReference().getProperty(SERVICE_ID));
            Object ranking = serverProperties.get(PROPERTY_SERVER_RANKING);
            if(ranking != null)
                props.put(PROPERTY_CORE_RANKING, ranking);
            try {
                this.registration = context.registerService(SolrCore.class.getName(), core, props);
            } catch (RuntimeException e) {
                log.warn("Unable to refister Service for SolrCore "+name+": Clean-up and rethrow");
                this.registration = null;
                //core.close(); ... do not close (this registration need to keep a reference
                throw e;
            }
        }
        /**
         * Unregisters this {@link CoreRegistration}
         */
        protected void unregister(){
            ServiceRegistration tmp;
            synchronized (this) { //to avoid multiple unregister calls
                tmp = registration;
                if(tmp == null){ //already unregistered
                    return; //nothing to do
                } else {
                    registration = null;
                }
            }
            try {
                if(tmp != null){
                    try {
                        tmp.unregister(); //unregister the service
                    } catch (IllegalStateException e) {
                        log.info(String.format(
                            "Looks like that the registration for SolrCore %s was already unregisterd",
                            name),e);
                    }
                }
            } finally { //ensure that the core is closed to decrease the reference count
                //ensure this is only done once
                SolrCore core;
                synchronized (this) {
                    core = this.core; //copy over to a local variable
                    this.core = null; //set the field to null
                }
                if(core != null){
                    core.close(); //decrease the reference count!!
                }
            }
        }
        /**
         * The name under witch the {@link #getCore() SolrCore} is registered.
         * {@link SolrCore#getName()} MAY NOT be equals to the name returned by
         * this Method.
         * @return the name under witch the SolrCore is registered. This can be
         * also retrieved by using {@link ServiceReference#getProperty(String)
         * gerServiceReference().getProperty(String)} with the key
         * {@link SolrConstants#PROPERTY_CORE_NAME}.
         */
        public String getName() {
            return name;
        }
        /**
         * The registered SolrCore. If <code>{@link #isRegistered()} == false</code>
         * the {@link SolrCore#isClosed()} will return true;
         * @return the SolrCore
         */
        public SolrCore getCore() {
            return core;
        }
        /**
         * The ServiceReference
         * @return the {@link ServiceReference} or <code>null</code> if already
         * unregistered
         */
        public ServiceReference getServiceReference(){
            return registration == null ? null : registration.getReference();
        }
        /**
         * If the {@link #getCore()} is still registered
         * @return the registration state
         */
        public boolean isRegistered(){
            return registration != null;
        }
        @Override
        public int hashCode() {
            return name.hashCode();
        }
        @Override
        public boolean equals(Object obj) {
            return obj instanceof CoreRegistration && ((CoreRegistration)obj).name.equals(name);
        }
        @Override
        public String toString() {
            return String.format("CoreRegistration[name: %s, registered: %s]",
                name,isRegistered());
        }
    }
    /**
     * {@link Dictionary} implementation that provides getter and setter for
     * typical properties configured for a {@link SolrServerAdapter}.<p>
     * Stores its state in the {@link Dictionary} and implements {@link Cloneable}
     * @author Rupert Westenthaler
     */
    public static class SolrServerProperties extends Dictionary<String,Object> implements Cloneable {
        private final Logger log = LoggerFactory.getLogger(SolrServerProperties.class);
        protected Hashtable<String,Object> properties = new Hashtable<String,Object>();
        
        public SolrServerProperties(File solrServerDir) {
            if(solrServerDir == null || !solrServerDir.isDirectory()){
                throw new IllegalArgumentException("The parsed SolrServerDir '" +
                        solrServerDir+ "' MUST refer to a Directory");
            }
            properties.put(PROPERTY_SERVER_DIR, solrServerDir);
            properties.put(PROPERTY_SOLR_XML_NAME, SOLR_XML_NAME);
            properties.put(PROPERTY_SERVER_RANKING, Integer.valueOf(0));
        }
        /**
         * Internally used for the implementation of {@link #clone()}
         * @param properties the already cloned properties
         */
        private SolrServerProperties(Hashtable<String,Object> properties){
            this.properties = properties;
        }
        public File getServerDir(){
            Object value = properties.get(PROPERTY_SERVER_DIR);
            if(value instanceof File){
                return (File) value;
            } else if (value != null){
                return new File(value.toString());
            } else {
                return null;
            }
        }
        public String getSolrXml(){
            Object value = properties.get(PROPERTY_SOLR_XML_NAME);
            return value != null ? value.toString() : SOLR_XML_NAME;
        }
        /**
         * Setter for the file name of the "solr.xml". If <code>null</code> or
         * an empty string is parsed the value will be reset to the default
         * {@link SolrConstants#SOLR_XML_NAME}
         * @param solrXmlName Solr Config File Name
         */
        public void setSorlXml(String solrXmlName){
            if(solrXmlName == null || solrXmlName.isEmpty()){
                solrXmlName = SOLR_XML_NAME;
            }
            properties.put(PROPERTY_SOLR_XML_NAME, solrXmlName);
        }
        /**
         * Returns the value of the name for this SolrCore or <code>null</code>
         * if non is explicitly set.
         * @return the human readable name for this {@link CoreContainer}
         */
        public String getServerName(){
            Object value = properties.get(PROPERTY_SERVER_NAME);
            return value != null ? value.toString() : null;
        }
        /**
         * Setter for the human readable name of this {@link CoreContainer Solr Server}.
         * Parsed values will be trimmed. If <code>null</code> or an empty
         * string is parsed the parameter will be removed
         * @param name the name of the server. Parsed values will be trimmed. 
         * If <code>null</code> or an empty string is parsed the parameter will 
         * be removed
         */
        public void setServerName(String name){
            if(name == null || name.trim().isEmpty()){
                properties.remove(PROPERTY_SERVER_NAME);
            } else {
                properties.put(PROPERTY_SERVER_NAME, name.trim());
            }
        }
        public Integer getServerRanking(){
            Object value = properties.get(PROPERTY_SERVER_RANKING);
            if(value instanceof Number){
                return ((Number)value).intValue();
            } else if(value != null){
                try {
                    return Integer.valueOf(value.toString());
                } catch (NumberFormatException e) {
                    log.warn("Illegal ServerRanking '"+value+"' return default ranking '0'!",e);
                }
            }
            return Integer.valueOf(0);
        }
        public void setServerRanking(Integer ranking){
            properties.put(PROPERTY_SERVER_RANKING, ranking);
        }
        
        public boolean isPublishREST(){
            Object value = properties.get(PROPERTY_SERVER_PUBLISH_REST);
            if(value instanceof Boolean){
                return ((Boolean)value).booleanValue();
            } else if (value != null){
                return Boolean.parseBoolean(value.toString());
            } else {
                return SolrConstants.DEFAULT_PUBLISH_REST;
            }
        }
        
        public void setPublishREST(Boolean state){
            if(state == null){
                properties.remove(PROPERTY_SERVER_PUBLISH_REST);
            } else {
                properties.put(PROPERTY_SERVER_PUBLISH_REST, state);
            }
        }
        
        @Override
        public Enumeration<Object> elements() {
            return properties.elements();
        }
        @Override
        public Object get(Object key) {
            return properties.get(key);
        }
        @Override
        public boolean isEmpty() {
            return properties.isEmpty();
        }
        @Override
        public Enumeration<String> keys() {
            return properties.keys();
        }
        @Override
        public Object put(String key, Object value) {
            return properties.put(key, value);
        }
        @Override
        public Object remove(Object key) {
            return properties.remove(key);
        }
        @Override
        public int size() {
            return properties.size();
        }
        @Override
        public SolrServerProperties clone() {
            Hashtable<String,Object> clonedProperties = new Hashtable<String,Object>(properties);
            return new SolrServerProperties(clonedProperties);
        }
    }
    /**
     * {@link Dictionary} implementation that provides getter and setter for
     * typical properties configured for a {@link SolrCore} registered to a 
     * {@link SolrServerAdapter}.<p>
     * Stores its state in the {@link Dictionary} and implements {@link Cloneable}
     * @author Rupert Westenthaler
     */
    public static class SolrCoreProperties extends Dictionary<String,Object> implements Cloneable {
        private final Logger log = LoggerFactory.getLogger(SolrCoreProperties.class);
       
        protected Hashtable<String,Object> coreProperties = new Hashtable<String,Object>();
        /**
         * Creates a new SolrCore properties instance.
         * @param coreName The name of the core to add or <code>null</code> to 
         * create properties for the {@link CoreContainer#getDefaultCoreName() default SolrCore}.
         */
        public SolrCoreProperties(String coreName){
            if(coreName != null){
                coreProperties.put(PROPERTY_CORE_NAME, coreName);
            }
            coreProperties.put(PROPERTY_CORE_SOLR_CONF, SOLR_CONFIG_NAME);
            coreProperties.put(PROPERTY_CORE_SCHEMA, SOLR_SCHEMA_NAME);
        }
        /**
         * internally used for the {@link #clone()} implementation
         * @param properties the already cloned properties
         */
        private SolrCoreProperties(Hashtable<String,Object> properties){
            this.coreProperties = properties;
        }
        public String getSolrconf(){
            Object value = coreProperties.get(PROPERTY_CORE_SOLR_CONF);
            return value != null ? value.toString() : SOLR_CONFIG_NAME;
        }
        public String getSchema(){
            Object value = coreProperties.get(PROPERTY_CORE_SCHEMA);
            return value != null ? value.toString() : SOLR_SCHEMA_NAME;
        }
        public File getCoreDir(){
            Object value = coreProperties.get(PROPERTY_CORE_DIR);
            if(value instanceof File){
                return (File)value;
            } else if(value != null){
                return new File(value.toString());
            } else {
                return null;
            }
        }
        public void setCoreDir(File directory){
            if(directory == null){
                coreProperties.remove(PROPERTY_CORE_DIR);
            } else {
                coreProperties.put(PROPERTY_CORE_DIR, directory);
            }
        }
        public String getCoreName(){
            Object value = coreProperties.get(PROPERTY_CORE_NAME);
            return value != null ? value.toString() : null;
        }
        public Integer getRanking(){
            Object value = coreProperties.get(PROPERTY_CORE_RANKING);
            if(value == null){
                value = coreProperties.get(PROPERTY_SERVER_RANKING);
            }
            if(value instanceof Number){
                return ((Number)value).intValue();
            } else if(value != null){
                try {
                    return Integer.valueOf(value.toString());
                } catch (NumberFormatException e) {
                    log.warn("Illegal ServerRanking '"+value+"' return default ranking '0'!",e);
                }
            }
            return Integer.valueOf(0);
        }
        
        @Override
        public Enumeration<Object> elements() {
            return coreProperties.elements();
        }
        @Override
        public Object get(Object key) {
            return coreProperties.get(key);
        }
        @Override
        public boolean isEmpty() {
            return coreProperties.isEmpty();
        }
        @Override
        public Enumeration<String> keys() {
            return coreProperties.keys();
        }
        @Override
        public Object put(String key, Object value) {
            return coreProperties.put(key, value);
        }
        @Override
        public Object remove(Object key) {
            return coreProperties.remove(key);
        }
        @Override
        public int size() {
            return coreProperties.size();
        }
        @Override
        public SolrCoreProperties clone() {
            Hashtable<String,Object> clonedProperties = new Hashtable<String,Object>(coreProperties);
            return new SolrCoreProperties(clonedProperties);
        }
        
    }
}

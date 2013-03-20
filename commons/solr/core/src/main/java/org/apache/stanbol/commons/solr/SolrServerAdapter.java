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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FilenameUtils;
import org.apache.solr.core.CloseHook;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.SolrCore;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

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
     * This implements CloseHook as used by {@link SolrCore} to notify about
     * the event that a specific SolrCore was finally closed. This is the only
     * way to get notified if a SolrCore is removed by other means than using
     * this class (e.g. by using the AdminRequestHandler)
     */
    private final CloseHook closeHook = new CloseHook() {
        
        @Override
        public void preClose(SolrCore core) {
            Collection<String> names = server.getCoreNames(core);
            if(names != null){
                synchronized (registrations) {
                    for(String name : names){
                        CoreRegistration coreRegistration = registrations.get(name);
                        //we need to check if the core registered for the 
                        //parsed name is still the same as parsed 
                        if(coreRegistration.getCore().equals(core)){
                            log.info("unregister Core with name '{}' based on call to" +
                                " CloseHook#close()",name);
                            registrations.remove(name);
                            coreRegistration.unregister();
                        } else {
                            log.info("Core registered for name '{}' is not the same as" +
                                    " parsed to CloseHook#close()",name);
                        }
                    }
                }
            }
            //update the OSGI service for the CoreContainer
            updateServerRegistration();
        }

        @Override
        public void postClose(SolrCore core) {
            //nothing to do
        }
    };
    /**
     * Creates and Initialise a Solr {@link CoreContainer} based on the provided
     * {@link SolrServerProperties} and registers it and all its configured 
     * {@link SolrCore}s as OSGI services by using the provided {@link BundleContext}.
     * @throws SAXException On any error while parsing the solr.xml file used to 
     * initialise the {@link CoreContainer}
     * @throws IOException On any error while accessing the solr.xml used to 
     * initialise the {@link CoreContainer} or the home directory for the 
     * {@link CoreContainer}
     * @throws ParserConfigurationException Configuration error of the XML parser
     * @throws IllegalArgumentException if any of the parsed parameters is
     * <code>null</code> or the {@link SolrServerProperties} do not contain a
     * valid value for the {@link SolrConstants#PROPERTY_SERVER_DIR} 
     * property.
     */
    public SolrServerAdapter(BundleContext context,SolrServerProperties parsedServerProperties) throws ParserConfigurationException, IOException, SAXException{
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
//        SolrResourceLoader loader = new OsgiResourceLoader(solrDir.getAbsolutePath(),
//            SolrServerAdapter.class.getClassLoader());
//        CoreContainer container = new CoreContainer(loader);
        CoreContainer container = new CoreContainer(solrDir.getAbsolutePath());
        File solrCof = new File(solrDir,parsedServerProperties.getSolrXml());
        ClassLoader classLoader = updateContextClassLoader();
        try {
            container.load(solrDir.getAbsolutePath(), solrCof);
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
        //add the currently available cores to the properties
        Set<String> coreNames = updateCoreNamesInServerProperties();
        //register the SolrServer
        this.serverRegistration = context.registerService(
            CoreContainer.class.getName(), server, serverProperties);
        //now register the cores
        for(String name : coreNames){
            registerCoreService(name,null);
        }
    }
    /**
     * Shutdown the {@link CoreContainer} and all {@link SolrCore}s managed by 
     * this instance. This will also cause all OSGI services to be unregistered
     */
    public void shutdown(){
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
     * @return The ServiceReference to the SolrCore.
     * @throws ParserConfigurationException if the XML parser could not be configured
     * @throws IOException indicated an error related to accessing the configured resource
     * @throws SAXException indicated an formatting error in the xml configuration files.
     */
    public void reloadCore(String name) throws ParserConfigurationException, IOException, SAXException {
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
        //TODO: what happens if one/both cores are no longer present?
        server.swap(core1, core2);
        //(re-)register the two cores
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
     * @throws ParserConfigurationException if the XML parser could not be configured
     * @throws IOException indicated an error related to accessing the configured resource
     * @throws SAXException indicated an formatting error in the xml configuration files.
     */
    public ServiceReference registerCore(SolrCoreProperties parsedCoreConfig) throws ParserConfigurationException, IOException, SAXException{
        SolrCoreProperties coreConfig = parsedCoreConfig.clone();
        String coreName = coreConfig.getCoreName();
        log.info("Register Core {} to CoreContainer {}",coreName,serverProperties.getServerName());
        if(coreName == null){
            coreName = server.getDefaultCoreName();
        }
        File coreDir = coreConfig.getCoreDir();
        if(coreDir == null){
            coreDir = new File(serverProperties.getServerDir(),coreName);
        }
        if(!coreDir.isDirectory()){
            throw new IllegalArgumentException("The Core Directory '"+
                coreDir+" for the Core '"+coreName+"' does not exist or is not an directory");
        }
        SolrCore old = null;
        ClassLoader classLoader = updateContextClassLoader();
        SolrCore core;
        try {
//            SolrResourceLoader loader = new OsgiResourceLoader(coreDir.getAbsolutePath(), 
//                SolrServerAdapter.class.getClassLoader());
//            SolrConfig config = new OsgiEnabledSolrConfig(loader, "solrconfig.xml", null);
//            IndexSchema schema = new IndexSchema(config,"schema.xml",null);
            CoreDescriptor coreDescriptor = new CoreDescriptor(server, 
                coreName, coreDir.getAbsolutePath());
//            core = new SolrCore(coreName, coreDir.getAbsolutePath(), config, schema,coreDescriptor);
//            server.register(coreName, core, false);
            core = server.create(coreDescriptor);
            //add the CloseHook
            core.addCloseHook(closeHook);
            // parse ture as third argument to avoid closing the current core for now
            old = server.register(coreName, core, true);
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
        //update the OSGI service for the new Core
        ServiceReference coreRef = registerCoreService(coreName,core);
        if(old != null){
            //cleanup the old core
            cleanupSolrCore(old);
        }
        // persist the new core to have it available on the next start
        //server.persist();
        //update the OSGI service for the CoreContainer
        updateServerRegistration();
        return coreRef;
    }
    
    /**
     * Closes the parsed SolrCore
     * @param old the core to close
     */
    private void cleanupSolrCore(SolrCore old) {
        /*
         * TODO: Validate if it is a good Idea to decrease the reference count
         * until the SolrCore is closed.
         */
        if(old == null){
            return;
        }
        old.close();
        if(!old.isClosed()){
            log.warn("Old SolrCore was not Closed correctly - this indicates that some other" +
            		"components calling CoreContainer#getSolrCore() has not colled SolrCore#close()" +
            		"after using it.");
            log.warn("To avoid memory leaks this will call SolrCore#close() until closed");
            int i=0;
            for(;!old.isClosed();i++){
                old.close();
            }
            log.warn("   ... called SolrCore#close() {} times before closed",i);
        }
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
     */
    private ServiceReference registerCoreService(String name,SolrCore core) {
        //first create the new and only than unregister the old (to ensure that 
        //the reference count of the SolrCore does not reach 0)
        CoreRegistration current = new CoreRegistration(name,core);
        CoreRegistration old = registrations.put(name,current);
        if(old != null){
            old.unregister();
        }
        log.debug("added Registration for SolrCore {}",name);
        return current.getServiceReference();
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

    public String getServerName(){
        Object value = serverRegistration.getReference().getProperty(PROPERTY_SERVER_NAME);
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
     * Internally used to manage the OSGI service registration for
     * {@link SolrCore}s of the {@link CoreContainer} managed by this
     * {@link SolrServerAdapter} instance
     * @author Rupert Westenthaler
     *
     */
    private class CoreRegistration {
        protected final String name;
        protected final SolrCore core;
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
            this.core = parsedCore != null ? parsedCore : server.getCore(name); //increases the reference count
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
                core.close();
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
                tmp.unregister(); //unregister the service
            } catch (IllegalStateException e) {
                log.info(String.format(
                    "Looks like that the registration for SolrCore %s was already unregisterd",
                    name),e);
            } finally {
                core.close(); //close the core
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
         * @param solrXmlName
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
            } else if(directory.isDirectory()){
                coreProperties.put(PROPERTY_CORE_DIR, directory);
            } else {
                throw new IllegalArgumentException("The parsed File '"+
                    directory+"' MUST represent a Directory!");
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

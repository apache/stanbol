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
package org.apache.stanbol.entityhub.yard.solr.embedded;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.ResourceLoader;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.SolrCore;
import org.apache.stanbol.entityhub.yard.solr.provider.SolrServerProvider;
import org.apache.stanbol.entityhub.yard.solr.provider.SolrServerProvider.Type;
import org.apache.stanbol.entityhub.yard.solr.utils.ConfigUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
/**
 * Support for the use of {@link EmbeddedSolrPorovider} in combination with the
 * SolrYard implementation. This implements the {@link SolrServerProvider}
 * interface for the {@link Type#EMBEDDED}.<p>
 * This implementation manages an internally managed instance of an 
 * {@link EmbeddedSolrServer} that is used to create/lookup cores if a
 * relative path is parsed as second parameter to 
 * {@link #getSolrServer(Type, String, String...)}. In case such a relative path
 * refers to a core that does not already exist this provider will create and
 * initialise a new one. It is also possible for other components to copy the
 * required files to the according directory and than use this provider to 
 * initialise the core. The {@link ConfigUtils} class provides some utilities
 * for that.<p>
 * The {@link #SOLR_DATA_DIR_PROPERTY} property can be used to define the location
 * of the internally managed index. By default it is within the 
 * {@link #DEFAULT_SOLR_DATA_DIR} directory located within the working-directory
 * (if running outside an OSGI environment) or within the data directory of the
 * SolrYard bundle (when running within an OSGI environment).<p>
 * Note that property substitution is supported for the 
 * {@link #SOLR_DATA_DIR_PROPERTY} property by using {@link System#getProperty(String, String)}
 * to search for values (empty string is used as default)<p>
 * Parsing the value "${data.dir}/indexes" will lookup the value of the system
 * property "data.dir" and the index will be located at "{property-value}/indexes".
 * 
 * TODO: add functionality to lookup the internally managed {@link CoreContainer}.
 * Maybe this requires to add a second service
 * @author Rupert Westenthaler
 *
 */
@Component(immediate=true,metatype=true)
@Service
public class EmbeddedSolrPorovider implements SolrServerProvider {
    private final Logger log = LoggerFactory.getLogger(EmbeddedSolrPorovider.class);
    //define the default values here because they are not accessible via the Solr API
    public static final String SOLR_XML_NAME = "solr.xml";
    public static final String SOLR_CONFIG_NAME = "solrconfig.xml";
    public static final String SOLR_SCHEMA_NAME = "schema.xml";
    
    /**
     * internally used to keep track of active {@link CoreContainer}s for
     * requested paths.
     */
    @SuppressWarnings("unchecked")
    private Map<String, CoreContainer> coreContainers = new ReferenceMap(); 
    
    /**
     * This property can be used to configure the location of the internally
     * managed EmbeddedSolrServer.<p>
     * Configuring an absolute path (starting with {@link File#separatorChar}) 
     * will cause the index to be initialised in this directory.<p>
     * Configuring an relative value will use  <ul>
     * <li> the working directory (<code>Systen.getProperty("user.dir")</code>)
     *      outside of an OSGI environment
     * <li> the data directory provided by the SolrYard bundle (by calling
     *      {@link BundleContext#getDataFile(String)} with the relative path.
     * </ul>
     * In case this property is not present the {@link #DEFAULT_SOLR_DATA_DIR}
     * (an relative path) is used.
     */
    @Property(value=EmbeddedSolrPorovider.DEFAULT_SOLR_DATA_DIR)
    public static final String SOLR_DATA_DIR_PROPERTY = "org.apache.stanbol.entityhub.yard.solr.embedded.solrDataDir";
    /**
     * default value for the relative path used if the {@link #SOLR_DATA_DIR_PROPERTY}
     * is not present
     */
    public static final String DEFAULT_SOLR_DATA_DIR = "indexes";
    
    /**
     * The directory used by the internally managed embedded solr server.
     */
    private File solrDataDir;
    
    /**
     * The component context. Only available when running within an OSGI 
     * Environment and the component is active.
     */
    private ComponentContext componentContext;
    /**
     * For some functionality within this component it is important to track
     * if this instance operates within or outside of an OSGI environment.
     * because of this this boolean is set to true as soon as the first time
     * {@link #activate(ComponentContext)} or {@link #deactivate(ComponentContext)}
     * is called. If someone knows a better method to check that feel free to
     * change!
     */
    private boolean withinOSGI = false;
    
    public EmbeddedSolrPorovider() {
    }
    
    @Override
    public SolrServer getSolrServer(Type type, String uriOrPath, String... additional) throws NullPointerException, IllegalArgumentException {
        log.debug(String.format("getSolrServer Request for %s and path %s",type,uriOrPath));
        if(uriOrPath == null){
            throw new IllegalArgumentException("The Path to the Index MUST NOT be NULL!");
        }
        log.info("parsed solr server location "+uriOrPath);
        //first try as file (but keep in mind it could also be an URI)
        File index = new File(uriOrPath);
        //if it is absolute and it exists -> take it
        if(!index.isAbsolute() || !index.exists()){
            //otherwise test if it is an URI
            try {
                URI fileUri = new URI(uriOrPath);
                index = new File(fileUri);
            } catch (URISyntaxException e) {
                //not an URI -> ignore
            } catch (IllegalArgumentException e){
                //this happens if it is a URI but can not be converted to a file
                //still we should try to work with the parsed file ...
            }
            //ok now we have the file path ... do some evaluation
            if(!index.isAbsolute()){ //if it is not absolute
                //use the internally managed EmbeddedSolrServer
                index = initManagedCoreDirectory(uriOrPath);
                if(index == null){ 
                    throw new IllegalArgumentException(String.format("The parsed Index Path %s does not exist and con not be initialised ",uriOrPath));
                }
            } else if(!index.exists()){ //if it is absolute -> check that it exist
                throw new IllegalArgumentException(String.format("The parsed Index Path %s does not exist",uriOrPath));
            }
        }
        log.info("get solr server for location "+index);
        File coreDir = null;
        if(index.isDirectory()){
            File solr = getFile(index, SOLR_XML_NAME);
            String coreName;
            if(solr != null){
                //in that case we assume that this is a single core installation
                coreName = "";
            } else {
                solr = getFile(index.getParentFile(), SOLR_XML_NAME);
                if(solr != null){
                    //assume this is a multi core
                    coreName = index.getName();
                    coreDir = index;
                    index = index.getParentFile(); //set the index dir to the parent
                } else {
                    throw new IllegalArgumentException(String.format("The parsed Index Path %s is not an Solr " +
                    		"Index nor a Core of an Multi Core Configuration " +
                    		"(no \""+SOLR_XML_NAME+"\" was found in this nor the parent directory!)",uriOrPath));
                }
            }
            //now init the EmbeddedSolrServer
            log.info(String.format("Create EmbeddedSolrServer for index %s and core %s",index.getAbsolutePath(),coreName));
            CoreContainer coreContainer = getCoreContainer(index.getAbsolutePath(), solr);
            //if we have a multi core environment and the core is not yet registered
            if(!coreName.isEmpty() && !coreContainer.getCoreNames().contains(coreName)){
                //register this core first
                /*
                 * NOTE: This assumes that the data for the core are already copied
                 * to the required location, but the core itself is not yet registered
                 * in the SOLR_XML_NAME.
                 * This uses the java API to register the core. Changes are saved
                 * within the SOLR_XML_NAME if persistent="true" is present within the
                 * solr element.
                 */
                /*
                 * NOTE:
                 * We need to reset the ContextClassLoader to the one used for this
                 * Bundle, because Solr uses this ClassLoader to load all the
                 * plugins configured in the SOLR_XML_NAME and schema.xml.
                 * The finally block resets the context class loader to the previous
                 * value. (Rupert Westenthaler 20010209)
                 */
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(EmbeddedSolrPorovider.class.getClassLoader());
                try {
                    //SolrResourceLoader solrLoader = new SolrResourceLoader(coreDir.getAbsolutePath());
                    CoreDescriptor coreDescriptor = new CoreDescriptor(coreContainer,coreName,coreDir.getAbsolutePath());
                    SolrCore core;
                    try {
                        core = coreContainer.create(coreDescriptor);
                    } catch (Exception e) {
                        throw new IllegalStateException(
                            String.format("Unable to load/register Solr Core %s " +
                            		"to SolrServer %s!",coreName,index.getAbsoluteFile()),e);
                    }
                    coreContainer.register(coreName, core,false);
                    //persist the new core to have it available on the next start
                    coreContainer.persist(); 
                } finally {
                    Thread.currentThread().setContextClassLoader(classLoader);
                }
            }
            return new EmbeddedSolrServer(coreContainer, coreName);
        } else {
            throw new IllegalArgumentException(String.format("The parsed Index Path %s is no Directory",uriOrPath));
        }
    }
    /**
     * The {@link CoreContainer} managed internally for the SolrYard. This is
     * used whenever a {@link #getSolrServer(Type, String, String...)} parses
     * an relative path as second parameter.<p>
     * Within an OSGI Environment this CoreContainer is started/stopped
     * within the activate/deactivate methods. With the exception if the
     * {@link #solrDataDir} does not exist, because this means that this internally
     * managed CoreContainer is not yet used. A lazy initialisation will be
     * performed when required.<p>
     * This Method does not perform any initialisation!
     * @return the managed CoreContainer or <code>null</code> if it is not yet
     * initialised or if the component is deactivated.
     */
    public CoreContainer getManagedCoreContainer(){
        File managedCoreContainerDir = getManagedSolrDataDir();
        if(managedCoreContainerDir != null){
            return coreContainers.get(managedCoreContainerDir.getAbsolutePath());
        } else {
            return null;
        }
    }
    /**
     * This will invalidate all EmbeddedSolrServers created by using this
     * CoreContainer or any of the Cores associated with this one
     * ({@link CoreContainer#getCores()})
     * @return <code>true</code> if the managed core was shutdown <code>false</code>
     * if the managed core was not active and therefore no change was performed
     */
    private boolean shutdownManagedCoreContainer(){
        File managedCoreContainerDir = getManagedSolrDataDir();
        if(managedCoreContainerDir != null){
            CoreContainer managedCoreContainer = coreContainers.remove(managedCoreContainerDir);
            if(managedCoreContainer != null){
                managedCoreContainer.shutdown();
                return true;
            }
        }
        return false;
    }
    /**
     * Getter for the defaultCore
     * @param coreName the name of the core or <code>null</code> to get/init the
     * default directory of the default CoreContainer.<p>
     * Works only within an OSGI Environment
     * @return the directory (instanceDir) of the core or the solr server (if
     * <code>null</code> is parsed as coreName). <code>null</code> is returned
     * if the initialisation was not successful.
     */
    protected final File initManagedCoreDirectory(final String coreName){
        File managedCoreContainerDirectory = getManagedSolrDataDir();
        if(managedCoreContainerDirectory == null){
            return null;
        }
        ComponentContext context = componentContext;
        if(!managedCoreContainerDirectory.exists()){
            try {
                if(context != null){ //load via bundle
                    managedCoreContainerDirectory = ConfigUtils.copyDefaultConfig(
                        context.getBundleContext().getBundle(),managedCoreContainerDirectory, false);
                } else { //load from jar
                    managedCoreContainerDirectory = ConfigUtils.copyDefaultConfig(
                        (Class<?>)null, managedCoreContainerDirectory, false);
                }
            } catch (Exception e) {
                log.warn("Unable to initialise the default EmbeddedSolrServer!",e);
                return null;
            }
        }
        if(coreName == null){
            return managedCoreContainerDirectory;
        }
        File coreDir = new File(managedCoreContainerDirectory,coreName);
        if(!coreDir.exists()){
            try {
                if(context != null){ //load via bundle
                    ConfigUtils.copyCore(context.getBundleContext().getBundle(),
                        coreDir, null, false);
                } else { //load from jar
                    ConfigUtils.copyCore((Class<?>)null, coreDir, null, false);
                }
            } catch (Exception e) {
                log.warn("Unable to initialise the core "+coreName+" with the default Core configuration!",e);
                return null;
            }
        }
        return coreDir;
    }

    /**
     * Internally used during initialisation to get the location of the directory
     * used for the data of the internally managed {@link CoreContainer}.<p>
     * This method checks the configuration based on of this EmbeddesSolrServerProvider
     * operates within or outside of an OSGI environment. It is not intended for
     * @return the directory of the Solr Home used for the internally managed
     * {@link CoreContainer} or <code>null</code> if running within an OSGI
     * Environment and this component is deactivated.
     */
    private File getManagedSolrDataDir() {
        //local copy to avoid NullPointerExceptions when deactivate is called
        //during this method
        ComponentContext context = componentContext;
        if(solrDataDir == null){
            String configuredDataDir;
            if(context == null){
                configuredDataDir = System.getProperty(SOLR_DATA_DIR_PROPERTY,DEFAULT_SOLR_DATA_DIR);
            } else {
                Object value = context.getProperties().get(SOLR_DATA_DIR_PROPERTY);
                if(value != null){
                    configuredDataDir = value.toString();
                } else {
                    configuredDataDir = DEFAULT_SOLR_DATA_DIR;
                }
            }
            //property substitution
            configuredDataDir = substituteProperty(configuredDataDir);
            //determine the directory holding the SolrIndex
            if(!configuredDataDir.isEmpty() && configuredDataDir.charAt(0) == File.separatorChar){
                //absolute path (same within/outside OSGI environment)
                if(withinOSGI && context == null){
                    //however set to  null if within OSGI environment and
                    //deactivated
                    solrDataDir = null;
                } else { //set the the absolute path
                    solrDataDir = new File(configuredDataDir);
                }
            } else { //relative path
                if(!withinOSGI){ //not within OSGI Environment
                    String workingDir = System.getProperty("user.dir");
                    solrDataDir = new File(workingDir,configuredDataDir);
                } else if(context != null){ //within OSGI && activated
                    solrDataDir = context.getBundleContext().getDataFile(configuredDataDir);
                } else { //within OSGI && deactivated
                    solrDataDir = null;
                }
            }
        }
        return solrDataDir;
    }

    /**
     * Substitutes ${property.name} with the values retrieved via
     * {@link System#getProperty(String, String)}. An empty string is used as
     * default<p>
     * If someone knows a default implementation feel free to replace!
     * @param value the value to substitute
     * @return the substituted value
     */
    private static String substituteProperty(String value) {
        int prevAt = 0;
        int foundAt = 0;
        StringBuilder substitution = new StringBuilder();
        while((foundAt = value.indexOf("${",prevAt))>=prevAt){
            substitution.append(value.substring(prevAt, foundAt));
            String propertyName = value.substring(
                foundAt+2,value.indexOf('}',foundAt));
            substitution.append(System.getProperty(propertyName, ""));
            prevAt = foundAt+propertyName.length()+3;
        }
        substitution.append(value.substring(prevAt, value.length()));
        return substitution.toString();
    }
    public static void main(String[] args) {
        System.out.println(substituteProperty("this ${user.dir} is ${not.existant} of version ${java.version}"));
    }
    
    protected final CoreContainer getCoreContainer(File solrDir) throws IllegalArgumentException, IllegalStateException {
        return getCoreContainer(solrDir.getAbsolutePath(), new File(solrDir,SOLR_XML_NAME));
    }
    protected final CoreContainer getCoreContainer(String solrDir, File solrConf) throws IllegalArgumentException, IllegalStateException {
        CoreContainer container = coreContainers.get(solrDir);
        if(container == null){
            container = new CoreContainer(solrDir);
            /*
             * NOTE:
             * We need to reset the ContextClassLoader to the one used for this
             * Bundle, because Solr uses this ClassLoader to load all the
             * plugins configured in the SOLR_XML_NAME and schema.xml.
             * The finally block resets the context class loader to the previous
             * value. (Rupert Westenthaler 20010209)
             */
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(EmbeddedSolrPorovider.class.getClassLoader());
            try {
                container.load(solrDir, solrConf);
            } catch (ParserConfigurationException e) {
                throw new IllegalStateException("Unable to parse Solr Configuration",e);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to access Solr Configuration",e);
            } catch (SAXException e) {
                throw new IllegalStateException("Unable to parse Solr Configuration",e);
            } finally {
                Thread.currentThread().setContextClassLoader(loader);
            }
            coreContainers.put(solrDir, container);
        }
        return container;
    }
    
    @Override
    public Set<Type> supportedTypes() {
        return Collections.singleton(Type.EMBEDDED);
    }
    @Activate
    protected void activate(ComponentContext context) {
        this.withinOSGI = true;
        this.componentContext = context;
        File managedCoreContainerDir = getManagedSolrDataDir();
        if(managedCoreContainerDir != null){
            if(managedCoreContainerDir.exists()) {
                //this indicated, that the core was alredy used ... so start it
                getCoreContainer(managedCoreContainerDir);
            } //else -> the internally managed core was not yet used ->
            // use lazy initialisation on first usage (no need to load all the
            //solr stuff in environments that do not use an EmbeddedSolrServer
        } else {
            //that should never happen ... 
            throw new IllegalStateException("Unable to get Directory for the internally managed Solr Server");
        }
    }
    @Deactivate
    protected void deactivate(ComponentContext context) {
        this.withinOSGI = true;
        shutdownManagedCoreContainer();
        solrDataDir = null;
        this.componentContext = null;
        //should we remove the coreContainers -> currently I don't because
        // (1) activate deactivate do not have any affect
        // (2) it are soft references anyway.
    }
    /**
     * Checks if the parsed directory contains a file that starts with the parsed
     * name. Parsing "hallo" will find "hallo.all", "hallo.ween" as well as "hallo".
     * @param dir the Directory. This assumes that the parsed File is not
     * <code>null</code>, exists and is an directory
     * @param name the name. If <code>null</code> any file is accepted, meaning
     * that this will return true if the directory contains any file 
     * @return the state
     */
    private boolean hasFile(File dir, String name){
        return dir.listFiles(new SimpleFileNameFilter(name)).length>0;
    }
    /**
     * Returns the first file that matches the parsed name.
     * Parsing "hallo" will find "hallo.all", "hallo.ween" as well as "hallo".
     * @param dir the Directory. This assumes that the parsed File is not
     * <code>null</code>, exists and is an directory.
     * @param name the name. If <code>null</code> any file is accepted, meaning
     * that this will return true if the directory contains any file 
     * @return the first file matching the parsed name.
     */
    private File getFile(File dir, String name){
        File[] files =  dir.listFiles(new SimpleFileNameFilter(name));
        return files.length>0?files[0]:null;
    }
    /**
     * Could not find a simple implementation of {@link FilenameFilter} that
     * can be used if a file exists. If someone knows one, feel free to replace
     * this one! 
     * @author Rupert Westenthaler
     *
     */
    private static class SimpleFileNameFilter implements FilenameFilter {

        private String name;
        public SimpleFileNameFilter(String name) {
            this.name = name;
        }
        @Override
        public boolean accept(File dir, String name) {
            return this.name == null?true:name.startsWith(this.name);
        }
        
    }
}

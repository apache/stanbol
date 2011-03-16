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
 * This implementation also adds support for dynamic configuration of an
 * {@link EmbeddedSolrServer} within the "/data" directory provided by the
 * OSGI Environment for the SolrYard bundle. This Embedded SolrServer is 
 * configured using an default configuration part of the SolrYard Bundle. The
 * Indexes are located at an folder with the name {@link #DEFAULT_SOLR_DATA_DIR}
 * (see {@link BundleContext#getDataFile(String)}).<p>
 * If a relative path is parsed as second parameter to 
 * {@link #getSolrServer(org.apache.stanbol.entityhub.yard.solr.provider.SolrServerProvider.Type, String, String...)}
 * than the value is looked up by using the internally managed EmbeddedSolrServer.<p>
 * This implementation also supports the dynamic initialisation, creation and
 * registration of new Cores for the internally managed EmbeddedSolrServer.
 * So if a parsed relative path does not correspond with an existing SolrCore,
 * than the default Core configuration (part of the SolrYard bundle) is used
 * to create the missing core.<p>
 * Note that it is also a valid Scenario that an other components copies data
 * (e.g. a prepared index) to the directory used by the internally managed
 * EmbeddedSolrServer. Than this implementation just registers the new core
 * with the EmbeddedSolrServer (creation and registration of the Core).
 * 
 * @author Rupert Westenthaler
 *
 */
@Component(immediate=true)
@Service
public class EmbeddedSolrPorovider implements SolrServerProvider {
    private final Logger log = LoggerFactory.getLogger(EmbeddedSolrPorovider.class);
    /**
     * internally used to keep track of active {@link CoreContainer}s for
     * requested paths.
     */
    @SuppressWarnings("unchecked")
    private Map<String, CoreContainer> coreContainers = new ReferenceMap(); 
    
    @Property
    public static final String SOLR_HOME = "solr.solr.home";

    public static final String DEFAULT_SOLR_DATA_DIR = "indexes";
    
    /**
     * The component context. Only available when running within an OSGI 
     * Environment and the component is active.
     */
    private ComponentContext componentContext;
    
    public EmbeddedSolrPorovider() {
    }
    
    @Override
    public SolrServer getSolrServer(Type type, String uriOrPath, String... additional) throws NullPointerException, IllegalArgumentException {
        log.debug(String.format("getSolrServer Request for %s and path %s",type,uriOrPath));
        if(uriOrPath == null){
            throw new IllegalArgumentException("The Path to the Index MUST NOT be NULL!");
        }
        File index = new File(uriOrPath);
        if(!index.exists()){
            try {
                URI fileUri = new URI(uriOrPath);
                index = new File(fileUri);
            } catch (URISyntaxException e) {
                //also not an URI -> ignore
            } catch (IllegalArgumentException e){
                //URI can not be converted to a file (e.g. not an absolute URI starting with "file:")
            }
            if(!index.exists()){
                //try to init the core via the default EmbeddedSolrServer
                // -> Initialised based on the default values in the bundle
                index = initCoreDirectory(uriOrPath);
                if(index == null){
                    throw new IllegalArgumentException(String.format("The parsed Index Path %s does not exist",uriOrPath));
                }
            }
        }
        File coreDir = null;
        if(index.isDirectory()){
            File solr = getFile(index, "solr.xml");
            String coreName;
            if(solr != null){
                //in that case we assume that this is a single core installation
                coreName = "";
            } else {
                solr = getFile(index.getParentFile(), "solr.xml");
                if(solr != null){
                    //assume this is a multi core
                    coreName = index.getName();
                    coreDir = index;
                    index = index.getParentFile(); //set the index dir to the parent
                } else {
                    throw new IllegalArgumentException(String.format("The parsed Index Path %s is not an Solr " +
                    		"Index nor a Core of an Multi Core Configuration " +
                    		"(no \"solr.xml\" was found in this nor the parent directory!)",uriOrPath));
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
                 * in the solr.xml.
                 * This uses the java API to register the core. Changes are saved
                 * within the solr.xml if persistent="true" is present within the
                 * solr element.
                 */
                /*
                 * NOTE:
                 * We need to reset the ContextClassLoader to the one used for this
                 * Bundle, because Solr uses this ClassLoader to load all the
                 * plugins configured in the solr.xml and schema.xml.
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
     * Getter for the defaultCore
     * @param coreName the name of the core or <code>null</code> to get/init the
     * default directory of the default CoreContainer.<p>
     * Works only within an OSGI Environment
     * @return the directory (instanceDir) of the core or the solr server (if
     * <code>null</code> is parsed as coreName). <code>null</code> is returned
     * if the initialisation was not successful.
     */
    protected final File initCoreDirectory(final String coreName){
        ComponentContext context = componentContext;
        if(context == null){ //not within OSGI Environment and active! 
            return null; //no default core container available
        }
        File defaultCoreDirectory = context.getBundleContext().getDataFile(DEFAULT_SOLR_DATA_DIR);
        if(!defaultCoreDirectory.exists()){
            try {
                defaultCoreDirectory = ConfigUtils.copyDefaultConfig(context.getBundleContext().getBundle(),
                    defaultCoreDirectory, false);
            } catch (Exception e) {
                log.warn("Unable to initialise the default EmbeddedSolrServer!",e);
                return null;
            }
        }
        if(coreName == null){
            return defaultCoreDirectory;
        }
        File coreDir = new File(defaultCoreDirectory,coreName);
        if(!coreDir.exists()){
            try {
                ConfigUtils.copyCore(context.getBundleContext().getBundle(),
                    coreDir, null, false);
            } catch (Exception e) {
                log.warn("Unable to initialise the core "+coreName+" with the default Core configuration!",e);
                return null;
            }
        }
        return coreDir;
    }
    
    protected final CoreContainer getCoreContainer(String solrDir, File solrConf) throws IllegalArgumentException, IllegalStateException {
        CoreContainer container = coreContainers.get(solrDir);
        if(container == null){
            container = new CoreContainer(solrDir);
            coreContainers.put(solrDir, container);
            /*
             * NOTE:
             * We need to reset the ContextClassLoader to the one used for this
             * Bundle, because Solr uses this ClassLoader to load all the
             * plugins configured in the solr.xml and schema.xml.
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
        }
        return container;
    }
    
    @Override
    public Set<Type> supportedTypes() {
        return Collections.singleton(Type.EMBEDDED);
    }
    @Activate
    protected void activate(ComponentContext context) {
        this.componentContext = context;
    }
    @Deactivate
    protected void deactivate(ComponentContext context) {
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

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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.entityhub.yard.solr.SolrDirectoryManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the {@link SolrDirectoryManager} interface that supports
 * the dynamic initialisation of new cores based on the default core configuration
 * contained within the SolrYard bundle.
 * 
 * @author Rupert Westenthaler
 *
 */
@Component(immediate=true,metatype=true)
@Service
@Properties(value={
           @Property(name=SolrDirectoryManager.MANAGED_SOLR_DIR_PROPERTY,value=SolrDirectoryManager.DEFAULT_SOLR_DATA_DIR)
    })
public class DefaultSolrDirectoryManager implements SolrDirectoryManager {
    private final Logger log = LoggerFactory.getLogger(DefaultSolrDirectoryManager.class);
    /**
     * The directory used by the internally managed embedded solr server. 
     * Use {@link #lookupManagedSolrDir()} instead of using this member, because
     * this member is not initialised within the constructor or the 
     * {@link #activate(ComponentContext)} method.
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
    /**
     * Initialising Solr Indexes with a lot of data may take some time. Especially
     * if the data need to be copied to the managed directory. Therefore it is
     * important to wait for the initialisation to be complete before opening
     * an Solr Index on it.<p>
     * To this set all cores that are currently initialised are added. As soon
     * as an initialisation completed this set is notified.
     */
    private Set<String> initCores = new HashSet<String>();
    
    public DefaultSolrDirectoryManager() {
    }
    
    /* (non-Javadoc)
     * @see org.apache.stanbol.entityhub.yard.solr.impl.ManagedSolrDirectory#isSolrDir(java.lang.String)
     */
    public final boolean isManagedIndex(String solrIndexName) throws IllegalStateException {
        if(solrIndexName == null){
            throw new IllegalArgumentException("The parsed name of the Solr index MUST NOT be NULL");
        }
        if(solrIndexName.isEmpty()){
            throw new IllegalArgumentException("The parsed name of the Solr index MUST NOT be empty");
        }
        //also here we need to initialise the SolrDirectory if not already done
        return new File(lookupManagedSolrDir(componentContext),solrIndexName).exists();
    }
    /* (non-Javadoc)
     * @see org.apache.stanbol.entityhub.yard.solr.impl.ManagedSolrDirectory#getManagedIndices()
     */
    public final Map<String,File> getManagedIndices() throws IllegalStateException {
        File solrDir = lookupManagedSolrDir(componentContext);
        String[] indexNames = solrDir.list(DirectoryFileFilter.INSTANCE);
        Map<String,File> indexes = new HashMap<String,File>();
        for(String indexName:indexNames){
            //TODO: validate that this is actually a SolrCore!
            indexes.put(indexName, new File(solrDir,indexName));
        }
        return indexes;
    }
    /* (non-Javadoc)
     * @see org.apache.stanbol.entityhub.yard.solr.impl.ManagedSolrDirectory#getSolrDirectory(java.lang.String)
     */
    public final File getSolrDirectory(final String solrIndexName,boolean create) throws IllegalArgumentException {
        return initSolrDirectory(solrIndexName,null,create,componentContext);
    }
    public final File createSolrDirectory(final String solrIndexName, ArchiveInputStream ais){
        return initSolrDirectory(solrIndexName,ais,true,componentContext);
    }
    /**
     * Internally used to get/init the Solr directory of a SolrCore or the root
     * Solr directory (if <code>null</code> is parsed)
     * @param solrIndexName the name of the Core or <code>null</code> to get/init
     * the root solr directory
     * @param ais The Input stream of the Archive to load the index from or
     * <code>null</code> to load the default core configuration.
     * @param create If <code>true</code> a new core is initialised if not already
     * present. Make sure this is set to <code>true</code> if parsing an InputStream.
     * Otherwise the index will not be created from the parsed stream!
     * @param context A reference to the component context or <code>null</code> if
     * running outside an OSGI container. This is needed to avoid that 
     * {@link #deactivate(ComponentContext)} sets the context to <code>null</code> 
     * during this method does its initialisation work.
     * @return the Solr directory or <code>null</code> in case this component is
     * deactivated
     * @throws IllegalStateException in case this method is called when this
     * component is running within an OSGI environment and it is deactivated or
     * the initialisation for the parsed index failed.
     * @throws IllegalArgumentException if the parsed solrIndexName is <code>null</code> or
     * empty.
     */
    private final File initSolrDirectory(final String solrIndexName,ArchiveInputStream ais,boolean create,ComponentContext context) throws IllegalStateException {
        if(solrIndexName == null){
            throw new IllegalArgumentException("The parsed name of the Solr index MUST NOT be NULL");
        }
        if(solrIndexName.isEmpty()){
            throw new IllegalArgumentException("The parsed name of the Solr index MUST NOT be empty");
        }
        File managedCoreContainerDirectory = lookupManagedSolrDir(context);
        if(solrIndexName == null){
            return managedCoreContainerDirectory;
        }
        File coreDir = new File(managedCoreContainerDirectory,solrIndexName);
        if(create && !coreDir.exists()){
            synchronized (initCores) {
                log.info(" > start initializing SolrIndex "+solrIndexName);
                initCores.add(solrIndexName);
            }
            try {
                if(ais != null){
                    ConfigUtils.copyCore(ais, coreDir, solrIndexName, false);
                } else if(context != null){ //load via bundle
                    ConfigUtils.copyCore(context.getBundleContext().getBundle(),
                        coreDir, null, false);
                } else { //load from jar
                    ConfigUtils.copyCore((Class<?>)null, coreDir, null, false);
                }
            } catch (Exception e) {
                throw new IllegalStateException(
                    String.format("Unable to copy default configuration for Solr Index %s to the configured path %s"
                        ,solrIndexName==null?"":solrIndexName,managedCoreContainerDirectory.getAbsoluteFile()),e);
            } finally {
                synchronized (initCores) {
                    initCores.remove(solrIndexName);
                    log.info("   ... finished inizializaiton of SolrIndex "+solrIndexName);
                    //notify that the initialisation completed or failed
                    initCores.notifyAll(); 
                }
            }
        } else { //the dir exists
            //check if still initialising ... and wait until the initialisation
            //is complete
            synchronized (initCores) {
                while(initCores.contains(solrIndexName)){
                    log.info(" > wait for initialisation of SolrIndex "+solrIndexName);
                    try {
                        initCores.wait();
                    } catch (InterruptedException e) {
                        // a core is initialised ... back to work
                    }
                }
            }
        }
        return coreDir;
    }

    /* (non-Javadoc)
     * @see org.apache.stanbol.entityhub.yard.solr.impl.ManagedSolrDirectory#getManagedSolrDir()
     */
    public File getManagedDirectory() {
        return lookupManagedSolrDir(componentContext);
    }
    /**
     * Lookup the location of the managed Solr directory. Also initialised the
     * default configuration if the directory does not yet exist.
     * @param context A reference to the component context or <code>null</code> if
     * running outside an OSGI container. This is needed to avoid that 
     * {@link #deactivate(ComponentContext)} sets the context to <code>null</code> 
     * during this method does its initialisation work.
     * @return the directory based on the current configuration
     * @throws IllegalStateException in case this method is called when this
     * component is running within an OSGI environment and it is deactivated.
     */
    private File lookupManagedSolrDir(ComponentContext context) throws IllegalStateException {
        if(solrDataDir == null){
            String configuredDataDir;
            if(context == null){ //load via system properties
                configuredDataDir = System.getProperty(MANAGED_SOLR_DIR_PROPERTY,DEFAULT_SOLR_DATA_DIR);
            } else { //load via OSGI config
                Object value = context.getProperties().get(MANAGED_SOLR_DIR_PROPERTY);
                if(value != null){
                    configuredDataDir = value.toString();
                } else {
                    configuredDataDir = DEFAULT_SOLR_DATA_DIR;
                }
            }
            //property substitution
            configuredDataDir = substituteProperty(configuredDataDir,
                context != null?context.getBundleContext():null);
            //determine the directory holding the SolrIndex
            /*
             * NOTE:
             * In case the configuredDataDir.isAbsolute()==false this code will
             * initialise the index relative to the "user.dir" of the application.
             */
            if(withinOSGI && context == null){
                //ensure to do not set an solrDataDir if this component is
                //running within an  OSGI environment and is deactivated
                throw new IllegalStateException("Unable to lookup managed Solr directories when component is deactivated!");
            } else { //set the the absolute path
                solrDataDir = new File(configuredDataDir);
            }
            if(!solrDataDir.exists()){
                try {
                    if(context != null){ //load via bundle
                        solrDataDir = ConfigUtils.copyDefaultConfig(
                            context.getBundleContext().getBundle(),solrDataDir, false);
                    } else { //load from jar
                        solrDataDir = ConfigUtils.copyDefaultConfig(
                            (Class<?>)null, solrDataDir, false);
                    }
                } catch (IOException e) {
                    throw new IllegalStateException(
                        String.format("Unable to copy default configuration for the manages Solr Directory to the configured path %s!"
                            , solrDataDir.getAbsoluteFile()),e);
                }
            }
        }
        return solrDataDir;
    }

    /**
     * Substitutes ${property.name} with the values retrieved via
     * {@link System#getProperty(String, String)}. An empty string is used as
     * default<p>
     * Nested substitutions are NOTE supported. However multiple substitutions
     * are supported. <p>
     * If someone knows a default implementation feel free to replace!
     * @param value the value to substitute
     * @param bundleContext If not <code>null</code> the 
     * {@link BundleContext#getProperty(String)} is used instead of the 
     * {@link System#getProperty(String)}. By that it is possible to use
     * OSGI only properties for substitution.
     * @return the substituted value
     */
    private static String substituteProperty(String value,BundleContext bundleContext) {
        int prevAt = 0;
        int foundAt = 0;
        StringBuilder substitution = new StringBuilder();
        while((foundAt = value.indexOf("${",prevAt))>=prevAt){
            substitution.append(value.substring(prevAt, foundAt));
            String propertyName = value.substring(
                foundAt+2,value.indexOf('}',foundAt));
            String propertyValue = bundleContext == null? //if no bundleContext is available
                    System.getProperty(propertyName): //use the System properties
                        bundleContext.getProperty(propertyName);
            substitution.append(propertyValue==null?"":propertyValue);
            prevAt = foundAt+propertyName.length()+3; //+3 -> "${}".length
        }
        substitution.append(value.substring(prevAt, value.length()));
        return substitution.toString();
    }
    @Activate
    protected void activate(ComponentContext context) {
        componentContext = context;
        withinOSGI = true;
    }
    @Deactivate
    protected void deactivate(ComponentContext context) {
        componentContext = null;
    }
}

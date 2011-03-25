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
import java.util.Map;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.entityhub.yard.solr.SolrDirectoryManager;
import org.apache.stanbol.entityhub.yard.solr.utils.ConfigUtils;
import org.osgi.service.component.ComponentContext;

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
    //private final Logger log = LoggerFactory.getLogger(DefaultSolrDirectoryManager.class);
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
        return new File(initSolrDirectory(null),solrIndexName).exists();
    }
    /* (non-Javadoc)
     * @see org.apache.stanbol.entityhub.yard.solr.impl.ManagedSolrDirectory#getManagedIndices()
     */
    public final Map<String,File> getManagedIndices() throws IllegalStateException {
        File solrDir = initSolrDirectory(null);
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
    public final File getSolrDirectory(final String solrIndexName) throws IllegalArgumentException {
        if(solrIndexName == null){
            throw new IllegalArgumentException("The parsed name of the Solr index MUST NOT be NULL");
        }
        if(solrIndexName.isEmpty()){
            throw new IllegalArgumentException("The parsed name of the Solr index MUST NOT be empty");
        }
        return initSolrDirectory(solrIndexName);
    }
    /**
     * Internally used to get/init the Solr directory of a SolrCore or the root
     * Solr directory (if <code>null</code> is parsed)
     * @param solrIndexName the name of the Core or <code>null</code> to get/init
     * the root solr directory
     * @return the Solr directory or <code>null</code> in case this component is
     * deactivated
     * @throws IllegalStateException in case this method is called when this
     * component is running within an OSGI environment and it is deactivated or
     * the initialisation for the parsed index failed.
     */
    private final File initSolrDirectory(final String solrIndexName) throws IllegalStateException {
        File managedCoreContainerDirectory = lookupManagedSolrDir();
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
            } catch (IOException e) {
                throw new IllegalStateException(
                    String.format("Unable to copy default configuration for the manages Solr Directory to the configured path %s!"
                        , managedCoreContainerDirectory.getAbsoluteFile()),e);
            }
        }
        if(solrIndexName == null){
            return managedCoreContainerDirectory;
        }
        File coreDir = new File(managedCoreContainerDirectory,solrIndexName);
        if(!coreDir.exists()){
            try {
                if(context != null){ //load via bundle
                    ConfigUtils.copyCore(context.getBundleContext().getBundle(),
                        coreDir, null, false);
                } else { //load from jar
                    ConfigUtils.copyCore((Class<?>)null, coreDir, null, false);
                }
            } catch (Exception e) {
                throw new IllegalStateException(
                    String.format("Unable to copy default configuration for Solr Index %s to the configured path %s"
                        ,solrIndexName==null?"":solrIndexName,managedCoreContainerDirectory.getAbsoluteFile()),e);
            }
        }
        return coreDir;
    }

    /* (non-Javadoc)
     * @see org.apache.stanbol.entityhub.yard.solr.impl.ManagedSolrDirectory#getManagedSolrDir()
     */
    public File getManagedDirectory() {
        // call initSolrDirectory(null) to initialise the internally managed
        // Solr directory in case it is not already initialised.
        return initSolrDirectory(null);
    }
    /**
     * Lookup the location of the managed Solr directory
     * @return the directory based on the current configuration
     * @throws IllegalStateException in case this method is called when this
     * component is running within an OSGI environment and it is deactivated.
     */
    private File lookupManagedSolrDir() throws IllegalStateException {
        //local copy to avoid NullPointerExceptions when deactivate is called
        //during this method
        ComponentContext context = componentContext;
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
            configuredDataDir = substituteProperty(configuredDataDir);
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
    @Activate
    protected void activate(ComponentContext context) {
        this.componentContext = context;
        this.withinOSGI = true;
    }
    @Deactivate
    protected void deactivate(ComponentContext context) {
        this.componentContext = null;
    }
}

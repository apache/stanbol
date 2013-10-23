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
/**
 * 
 */
package org.apache.stanbol.commons.solr.managed.impl;

import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_SERVER_NAME;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.stanbol.commons.solr.SolrConstants;
import org.apache.stanbol.commons.solr.managed.IndexMetadata;
import org.apache.stanbol.commons.solr.managed.ManagedIndexState;
import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.apache.stanbol.commons.solr.utils.ConfigUtils;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class that manages the configuration files for SolrIndexed managed
 * by {@link ManagedSolrServer} implementations.<p>
 * It can both operate within and outside an OSGI environment. In case a
 * OSGI environment is available is uses the persistent storage area of the
 * bundle and creates a subfolder based on the {@link Constants#SERVICE_PID}
 * of the 
 * @author westei
 *
 */
public class ManagedIndexMetadata {
    
    private Logger log = LoggerFactory.getLogger(ManagedIndexMetadata.class);
    /**
     * The path to the directory used to store metadata about managed indexes.
     * <p>
     * This directory will store properties files with the indexName as name, properties as extension and the
     * properties as value
     */
    private static final String DEFAULT_INDEX_CONFIG_DIR = ".config/index-config";

    /**
     * This map is used synchronise access to configuration files
     *   
     */
    //private Map<String,int[]> configDirSync = Collections.synchronizedMap(
    //    new HashMap<String,int[]>());
    
    private final String serverName;
    //private final ComponentContext context;
    private final String pid;
    private final File configDir;

    private Map<ManagedIndexState,Map<String,IndexMetadata>> managed = new EnumMap<ManagedIndexState,Map<String,IndexMetadata>>(ManagedIndexState.class);
//    private final Map<String,IndexMetadata> uninitialised = new HashMap<String,IndexMetadata>();
    private final Map<String, Collection<String>> archiveName2CoreName = new HashMap<String,Collection<String>>();
//    private final Map<String, IndexMetadata> active = new HashMap<String,IndexMetadata>();
    /**
     * The {@link #managed} is used to synchronise  while reading/writing the
     * in-memory model 
     */
    private Object inMemoryModelLock = managed;

    
    private ManagedIndexMetadata(String serverName, String pid,ComponentContext context){
        this.serverName = serverName;
        this.pid = pid;
        //this.context = context;
        //init the Maps for manageing Indexes with the different states
        for(ManagedIndexState state : ManagedIndexState.values()){
            managed.put(state, new HashMap<String,IndexMetadata>());
        }
        File dir = null;
        if(context != null){
            dir = context.getBundleContext().getDataFile(DEFAULT_INDEX_CONFIG_DIR+'/'+pid);
            //dir might be null if the OSGI environment is missing file system support
        }
        if (dir == null) { //outside OSGI or OSGI has no file system support
            // use config directory relative to the the Managed Solr Directory
            dir = new File(DEFAULT_INDEX_CONFIG_DIR,pid).getAbsoluteFile();
        }
        log.info("SolrYard Config Directory: "+dir);
        this.configDir = dir;
    }
    /**
     * Constructor to be used outside of an OSGI context
     * @param serverName the name of the Server
     */
    public ManagedIndexMetadata(String serverName){
        this(serverName,serverName,null);
        if(serverName == null || serverName.isEmpty()){
            throw new IllegalArgumentException("The parsed ServerName MUST NOT be NULL nor empty!");
        }
    }
    /**
     * The constructor to be used inside an OSGI environment. 
     * The {@link #serverName} is parsed form the {@link SolrConstants#PROPERTY_SERVER_NAME}.
     * @param context the context of the {@link ManagedSolrServer} implementation
     * @throws IllegalStateException if the OSGI environment does not have
     * FileSystem support
     */
    public ManagedIndexMetadata(ComponentContext context) {
        this((String)context.getProperties().get(PROPERTY_SERVER_NAME),
            (String)context.getProperties().get(Constants.SERVICE_PID),
            context);
        if(serverName == null){
            throw new IllegalArgumentException("The properties of the " +
                "ComponentContext of the ManagedSolrServer '"+serverName+
                "'MUST contain a '"+SolrConstants.PROPERTY_SERVER_NAME+"' value!");
        }
        if(pid == null){
            throw new IllegalArgumentException("The properties of the " +
            		"ComponentContext of the ManagedSolrServer '"+serverName+
            		"'MUST contain a '"+Constants.SERVICE_PID+"' value!");
        }
        Map<String,IndexMetadata> indexConfigs;
        try {
            indexConfigs = loadIndexConfigs();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load information about" +
                    "uninitialised SolrCores for managed Solr server '"+
                    serverName+"'!",e);
        }
        synchronized (inMemoryModelLock) { //build the in-memory model
            for(Entry<String,IndexMetadata> entry : indexConfigs.entrySet()){
                //read the state from the value and 
                //put the entry to the map for the state
                managed.get(entry.getValue().getState()).put(
                    entry.getKey(), entry.getValue());
                for(String archiveName : entry.getValue().getIndexArchives()){
                    Collection<String> indexes = archiveName2CoreName.get(archiveName);
                    if(indexes == null){
                        indexes = new HashSet<String>();
                        archiveName2CoreName.put(archiveName, indexes);
                    }
                    indexes.add(entry.getKey());
                }
            }
        }
    }
    public boolean isInState(ManagedIndexState state,String indexName) {
        synchronized (inMemoryModelLock) {
            return managed.get(state).containsKey(indexName);
        }
    }
    public Collection<String> getInState(ManagedIndexState state) {
        synchronized (inMemoryModelLock) {
            return new HashSet<String>(managed.get(state).keySet());
        }
    }
    public boolean isManaged(String indexName){
        synchronized (inMemoryModelLock) {
            for(Map<String,IndexMetadata> inState : managed.values()) {
                if(inState.containsKey(indexName)){
                    return true;
                }
            }
        }
        return false;
    }
    public Set<String> getManaged() {
        Set<String> names = new HashSet<String>();
        synchronized (inMemoryModelLock) {
            for(Map<String,IndexMetadata> inState : managed.values()) {
                names.addAll(inState.keySet());
            }
        }
        return names;
    }
    /**
     * Provides the state of an index based on the managed metadata
     * @param indexName the name of the index
     * @return the state of <code>null</code> if the index name is not known by
     * the in-memory model
     */
    public ManagedIndexState getState(String indexName){
        synchronized (inMemoryModelLock) {
            for(Entry<ManagedIndexState,Map<String,IndexMetadata>> entry : managed.entrySet()) {
                if(entry.getValue().containsKey(indexName)){
                    return entry.getKey();
                }
            }
        }
        return null;
    }
    /**
     * Getter for the metadata of all indexes in a given state. Changing the
     * returned {@link Collection} or the Entries does not affect the state
     * of this class.
     * @param state the state
     * @return the metadata of all the indexes in that state (empty if none,
     * <code>null</code> if <code>null</code> was parsed as state)
     */
    public Collection<IndexMetadata> getIndexMetadata(ManagedIndexState state){
        if(state == null){
            return null;
        }
        Collection<IndexMetadata> clones = new HashSet<IndexMetadata>();
        synchronized (inMemoryModelLock) {
            for(IndexMetadata metadata : managed.get(state).values()){
                IndexMetadata clone = new IndexMetadata();
                clone.putAll(metadata);
                clones.add(clone);
            }
        }
        return clones;
        
    }
    public IndexMetadata getIndexMetadata(String indexName){
        IndexMetadata metadata = null;
        synchronized (inMemoryModelLock) {
            Iterator<Map<String,IndexMetadata>> inStateIt = managed.values().iterator();
            while(metadata == null && inStateIt.hasNext()) {
                metadata = inStateIt.next().get(indexName);
            }
        }
        //we need to return a clone to prevent changes by external changes to
        //the internal state!
        if(metadata != null){
            IndexMetadata clone = new IndexMetadata();
            clone.putAll(metadata);
            return clone;
        } else {
            return null;
        }
    }
    @SuppressWarnings("unchecked")
    public Collection<String> getIndexNames(String archiveName){
        synchronized (inMemoryModelLock) {
            Collection<String> indexNames = archiveName2CoreName.get(archiveName);
            return indexNames == null ? 
                    (Collection<String>)Collections.EMPTY_LIST : 
                        Collections.unmodifiableCollection(new ArrayList<String>(indexNames));
        }
    }
    
    public void addUninitialisedIndex(String indexName, String indexArchiveName, Boolean sync) throws IOException {
        IndexMetadata config  = new IndexMetadata();
        config = new IndexMetadata();
        config.setIndexName(indexName);
        config.setServerName(serverName);
        config.setState(ManagedIndexState.UNINITIALISED);
        if(sync != null){
            config.setSynchronized(sync);
        }
        //no need to clone, because we have created the instance
        updateIndexProperties(null, config, false);
    }
    public void store(IndexMetadata properties){
        updateIndexProperties(null, properties, true);
    }
    /**
     * Removes the metadata of the index with that name
     * @param name the name of the index to remove
     * @return the removed metadata of the index of <code>null</code> if no
     * matadata for an index with that name where present.
     */
    public IndexMetadata remove(String name){
        return updateIndexProperties(name, null, true);
    }
    /**
     * Adds, update and deletes index metadata
     * @param name the name of the index (can be <code>null</code> of properties are parsed)
     * @param properties the properties or <code>null</code> to remove
     * @param clone If <code>true</code> the parsed properties are cloned. Clones
     * are required for parsed properties to prevent external changes of properties
     * stored in the internal lists. Only parse <code>false</code> in case the
     * parsed properties are already a clone
     * @return The old {@link IndexMetadata} instance (especially usefull in case of
     * remove operations)
     */
    private IndexMetadata updateIndexProperties(String name, IndexMetadata properties, boolean clone) {
        if(name == null && properties == null){
            return null;
        }
        if(name != null && properties != null && !name.equals(properties.getIndexName())){
            throw new IllegalArgumentException("The value of the Index-Name property '"+
                properties.getIndexName()+"' is not the same as the parsed name '"+
                name+"'!");
        }
        if(name == null) {
            name  = properties.getIndexName();
        }
        //first persist
        if(properties != null){
            try {
                saveIndexConfig(name, properties);
            } catch (IOException e) {
                log.error("Unable to store Properties (see Exception below): {}",properties.toString());
                throw new IllegalStateException("Unable to save Index metadata for index '"+
                    name+"'!",e);
            }
        } else {
            removeIndexConfig(name);
        }
        //clone is meaningless if properties are NULL
        if(clone && properties != null){
            IndexMetadata tmp = properties;
            properties = new IndexMetadata();
            properties.putAll(tmp);
        }
        Map<String,IndexMetadata> toAdd,toRemove;
        IndexMetadata oldMetadata = null;
        synchronized (inMemoryModelLock ) {
            ManagedIndexState currentState = getState(name);
            if(currentState != null){
                toRemove = managed.get(currentState);
            } else {
                toRemove = null;
            }
            if(properties == null){
                toAdd = null; //remove
            } else {
                ManagedIndexState newState = properties.getState();
                toAdd = managed.get(newState);
            }
            //now update in-memory state
            if(toRemove != null){
                oldMetadata = toRemove.remove(name);
            }
            if(toAdd != null){
                toAdd.put(name, properties);
            }
            //now update the archive name to core name mappings
            if(oldMetadata != null){
                for(String indexArchive : oldMetadata.getIndexArchives()){
                    Collection<String> indexes = archiveName2CoreName.get(indexArchive);
                    if(indexes.remove(name) && indexes.isEmpty()){
                        archiveName2CoreName.remove(indexArchive);
                    }
                }
            }
            if(properties != null){
                for(String indexArchive : properties.getIndexArchives()){
                    Collection<String> indexes = archiveName2CoreName.get(indexArchive);
                    if(indexes == null){
                        indexes = new HashSet<String>();
                        archiveName2CoreName.put(indexArchive, indexes);
                    }
                    indexes.add(name);
                }
            }
        }
        return oldMetadata;
    }
    

    /**
     * Saves the configuration of an uninitialised index
     * 
     * @param indexName
     *            the name of the uninitialised index
     * @param properties
     *            the properties of the uninitialised index
     * @throws IOException
     *             on any error while saving the configuration
     */
    private void saveIndexConfig(String indexName, IndexMetadata properties) throws IOException {
        File configDir = getIndexConfigDirectory(true);
        File config = new File(configDir, indexName + '.'
            + ConfigUtils.SOLR_INDEX_ARCHIVE_EXTENSION + ".ref");
        synchronized (pid) {
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(config);
                properties.store(out, null);
            } finally {
                IOUtils.closeQuietly(out);
            }
        }
    }


    /**
     * Returns the directory used to store the configurations of uninitialised Solr Indexes
     * @param init
     *            if <code>true</code> the directory is created if needed
     * @return the directory
     */
    private File getIndexConfigDirectory(boolean init) {
        if (!configDir.exists()) {
            if(init) {
                if (!configDir.mkdirs()) {
                    throw new IllegalStateException("Unable to create Directory "
                                                    + DEFAULT_INDEX_CONFIG_DIR
                                                    + "for storing information of uninitialised Solr Indexes");
                }
            }
        } else if (!configDir.isDirectory()) {
            throw new IllegalStateException("The directory " + DEFAULT_INDEX_CONFIG_DIR
                                            + "for storing uninitialised Solr Indexes Information exists"
                                            + "but is not a directory!");
        } // else -> it exists and is a dir -> nothing todo
        return configDir;
    }

    /**
     * Loads the configurations of uninitialised Solr Indexes
     * 
     * @return the map with the index name as key and the properties as values
     * @throws IOException
     *             on any error while loading the configurations
     */
    private Map<String,IndexMetadata> loadIndexConfigs() throws IOException {
        File uninstalledConfigDir = getIndexConfigDirectory(false);
        Map<String,IndexMetadata> configs = new HashMap<String,IndexMetadata>();
        synchronized (pid) {
            if (uninstalledConfigDir.exists()) {
                for (String file : uninstalledConfigDir.list(new SuffixFileFilter(
                        ConfigUtils.SOLR_INDEX_ARCHIVE_EXTENSION + ".ref"))) {
                    String indexName = file.substring(0, file.indexOf('.'));
                    File configFile = new File(uninstalledConfigDir, file);
                    IndexMetadata props = new IndexMetadata();
                    InputStream is = null;
                    try {
                        is = new FileInputStream(configFile);
                        props.load(is);
                        //validate Index-Name and Server-Name properties!
                        if(!indexName.equals(props.getIndexName())){
                            throw new IOException("The IndexName '"+props.getIndexName()+
                                "within the IndexConfig file does not correspond to the file name '"+
                                file+"'!");
                        }
                        if(!serverName.equals(props.getServerName())){
                            throw new IOException("The Name of the Referenced Solr server '"+
                                serverName+" does not correspond with the Server-Name value '"+
                                props.getServerName()+"' within the property file '"+
                                file+"'!");
                        }
                        configs.put(indexName, props);
                    } finally {
                        IOUtils.closeQuietly(is);
                    }
                }
            }
        }
        return configs;
    }

    /**
     * Removes the configuration for the index with the parsed name form the list if uninitialised indexes
     * 
     * @param context
     *            the context used to get the data storage or <code>null</code>
     *            if not available (e.g. outside an OSGI environment)
     * @param serverName
     *            the name of the managed solr server
     * @param indexName
     *            the name of the index
     * @return if the file was deleted.
     */
    private boolean removeIndexConfig(String indexName) {
        File configFile = new File(getIndexConfigDirectory(false),
                indexName + '.' + ConfigUtils.SOLR_INDEX_ARCHIVE_EXTENSION + ".ref");
        synchronized (pid) { 
            return configFile.delete();
        }
    }        
    @Override
    public int hashCode() {
        return pid.hashCode();
    }
    @Override
    public boolean equals(Object o) {
        return o instanceof ManagedIndexMetadata && 
            ((ManagedIndexMetadata)o).pid.equals(pid);
    }
    @Override
    public String toString() {
        return String.format("ManagedCores [name:%s|pid:%s|managed:%s]",
            serverName,pid,managed);
    }
    
}
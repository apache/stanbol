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
package org.apache.stanbol.commons.solr.managed.standalone;

import static org.apache.stanbol.commons.solr.managed.util.ManagementUtils.getArchiveCoreName;
import static org.apache.stanbol.commons.solr.managed.util.ManagementUtils.getMetadata;
import static org.apache.stanbol.commons.solr.managed.util.ManagementUtils.substituteProperty;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.SolrCore;
import org.apache.stanbol.commons.solr.managed.IndexMetadata;
import org.apache.stanbol.commons.solr.managed.ManagedIndexState;
import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.apache.stanbol.commons.solr.managed.util.ManagementUtils;
import org.apache.stanbol.commons.solr.utils.ConfigUtils;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Basic implementation of the {@link ManagedSolrServer} interface that
 * can be used without an OSGI environment.
 * <p>
 * NOTE: {@link ServiceLoader} is used to search for DataFileProviders outside of
 * OSGI. An instance of {@link ClassPathDataFileProvider} is registered by 
 * default that loads Index-Archives form "solr/core/". if you want to load
 * Data-Files form different locations you will need to provide your own 
 * DataFileProvider. Extending {@link ClassPathDataFileProvider} might be the
 * simplest way to do this.
 * 
 * @author Rupert Westenthaler
 *
 */
public final class StandaloneManagedSolrServer implements ManagedSolrServer {

    private static final String DEFAULT_SERVER_NAME = "default";
    private final Logger log = LoggerFactory.getLogger(StandaloneManagedSolrServer.class);
    /**
     * Outside OSGI we need an instance of a data file provider that can load
     * Index Configuration via the classpath
     */
    private static ServiceLoader<DataFileProvider> dataFileProviders = ServiceLoader.load(DataFileProvider.class);
    //private static DataFileProvider dataFileProvider = new ClassPathSolrIndexConfigProvider(null);
    /**
     * Initialising Solr Indexes with a lot of data may take some time. Especially if the data need to be
     * copied to the managed directory. Therefore it is important to wait for the initialisation to be
     * complete before opening an Solr Index on it.
     * <p>
     * To this set all cores that are currently initialised are added. As soon as an initialisation completed
     * this set is notified.
     */
    private Set<String> initCores = new HashSet<String>();
    /**
     * List with the managed Solr servers. The name is used as key 
     */
    public static final Map<String, StandaloneManagedSolrServer> managedServers = new HashMap<String,StandaloneManagedSolrServer>();
    
    /**
     * The directory on the File system used to manage this {@link CoreContainer}
     */
    private File managedSolrDir;
    /**
     * The managed Solr server
     */
    private CoreContainer server;
    private String serverName;
    /**
     * Getter for the ManagedSolrServer for the parsed name. If
     * {@link #getDefaultServerName()} is parsed as name the default managed
     * Solr server is returned (and created if needed). For any other name 
     * <code>null</code> is returned if no {@link ManagedSolrServer} for this
     * name is present.
     * @param name the name
     * @return The managed Solr server or <code>null</code> if not known.
     */
    public static StandaloneManagedSolrServer getManagedServer(String name){
        if(name == null){
            name = DEFAULT_SERVER_NAME;
        }
        if(name.equals(DEFAULT_SERVER_NAME)){
            return createManagedServer(name);
        } else {
            synchronized (managedServers) {
                return managedServers.get(name);
            }
        }
    }
    /**
     * Getter for the name of the default managed Solr server
     * @return the name of the default server
     */
    public static String getDefaultServerName(){
        return DEFAULT_SERVER_NAME;
    }
    /**
     * Shutdowns the default server
     */
    public static void shutdownManagedServer() {
        shutdownManagedServer(DEFAULT_SERVER_NAME);
    }
    public static void shutdownManagedServer(String name){
        synchronized (managedServers) {
            StandaloneManagedSolrServer server = managedServers.remove(name);
            if(server != null){
                server.shutdown();
            }
        }
    }

    /**
     * Getter for the default managed Solr server. This method is guaranteed to
     * NOT return <code>null</code>.
     * @return the default server
     */
    public static StandaloneManagedSolrServer getManagedServer(){
        return getManagedServer(null);
    }
    /**
     * Creates a new ManagedSolrServer with the specified name
     * @param name the name. MUST NOT be <code>null</code>, empty or contain
     * any chars that are used as {@link File} separators, extensions. In other
     * words {@link FilenameUtils#getBaseName(String)} MUST NOT change the
     * parsed name!
     * @return The created or already existing {@link ManagedSolrServer} for
     * this name
     */
    public static StandaloneManagedSolrServer createManagedServer(String name){
        synchronized (managedServers) {
            StandaloneManagedSolrServer server = managedServers.get(name);
            if(server == null){
                server = new StandaloneManagedSolrServer(name);
                //use server.getServerName(), because NULL is replaced with default
                managedServers.put(server.getServerName(), server);
            }
            return server;
        }
    }
    /**
     * Private constructor used by the {@link #createManagedServer(String)}
     * method
     * @param name the name
     */
    private StandaloneManagedSolrServer(String name) {
        if(name == null){
            throw new IllegalArgumentException("The parsed Name MUST be NULL!");
        } else if (name.isEmpty()){
            throw new IllegalArgumentException("The parsed Name MUST be Empty!");
        } else if(!FilenameUtils.getBaseName(name).equals(name)){
            throw new IllegalArgumentException("The parsed Name '"+name+
                "' contains path seperator, seperator and/or extension seperator chars!");
        }
        this.serverName = name;
        //init the manange Solr Directory
        String configuredDataDir = System.getProperty(MANAGED_SOLR_DIR_PROPERTY, DEFAULT_SOLR_DATA_DIR);
        // property substitution
        configuredDataDir = FilenameUtils.separatorsToSystem(
            substituteProperty(configuredDataDir, null));
        // determine the directory holding the SolrIndex
        managedSolrDir = new File(configuredDataDir,name).getAbsoluteFile();
        try {
            managedSolrDir = managedSolrDir.getCanonicalFile();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to get the Canonical File for '"+configuredDataDir+"'!");
        }
        initServer();
    }

    private void initServer(){
        File solrConf = new File(managedSolrDir, "solr.xml");
        if (!solrConf.exists()) {
            try {
                managedSolrDir = ConfigUtils.copyDefaultConfig((Class<?>) null, managedSolrDir, false);
            } catch (IOException e) {
                throw new IllegalStateException(String.format(
                    "Unable to copy default configuration for the manages Solr " +
                    "Directory to the configured path '%s'!",
                     managedSolrDir.getAbsoluteFile()), e);
            }
        }
        server = new CoreContainer(managedSolrDir.getAbsolutePath());
        //File solrXml = new File(managedSolrDir,"solr.xml");
        server.load();
    }    
    private void shutdown() {
        server.shutdown();
    }
    
    @Override
    public IndexMetadata createSolrIndex(String name, ArchiveInputStream ais) throws IOException {
        if(!isManagedIndex(name)) {
            return updateIndex(name, ais);
        } else {
            throw new IllegalStateException("Can not create core with name '"+name+
                "' because a Core with that name does already exist!");
        }
    }

    @Override
    public IndexMetadata createSolrIndex(String coreName, String resourceName, Properties properties) throws IOException {
        if(!isManagedIndex(coreName)) {
            return updateIndex(coreName,resourceName,properties);
        } else {
            throw new IllegalStateException("Can not create core with name '"+coreName+
                "' because a Core with that name does already exist!");
        }
    }

    @Override
    public File getManagedDirectory() {
        return managedSolrDir;
    }
    @Override
    public String getServerName() {
        return serverName;
    }

    @Override
    public Collection<IndexMetadata> getIndexes(ManagedIndexState state) {
        if(state == ManagedIndexState.ACTIVE){
            Collection<IndexMetadata> coreMetadata = new ArrayList<IndexMetadata>();
            for(SolrCore core : server.getCores()){
                coreMetadata.add(getMetadata(core,serverName));
            }
            return coreMetadata;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public File getSolrIndexDirectory(String name) {
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException("The parsed index name MUST NOT be NULL nor empty!");
        }
        SolrCore core = server.getCore(name);
        if(core != null){
            File instanceDir = new File(core.getCoreDescriptor().getInstanceDir());
            core.close();
            return instanceDir;
        } else {
            return null;
        }
    }

    @Override
    public IndexMetadata getIndexMetadata(String indexName) {
        IndexMetadata metadata;
        SolrCore core  = server.getCore(indexName);
        if(core != null){
             metadata = getMetadata(core,serverName);
            core.close();
        } else {
            metadata = null;
        }
        return metadata;
    }
    @Override
    public ManagedIndexState getIndexState(String indexName) {
        //if the core is not active it does not exist
        return server.getCoreNames().contains(indexName) ? ManagedIndexState.ACTIVE : null;
    }

    @Override
    public boolean isManagedIndex(String name) {
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException("The parsed index name MUST NOT be NULL nor empty!");
        }
        return server.getCoreNames().contains(name);
    }

    @Override
    public void removeIndex(String name, boolean deleteFiles) {
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException("The parsed index name MUST NOT be NULL nor empty!");
        }
        SolrCore core = server.remove(name);
        core.close(); //decrease reference count
        if(deleteFiles){
            String instanceDir = core.getCoreDescriptor().getInstanceDir();
            while(!core.isClosed()){ //ensure the core is closed!
                core.close();
            }
            try {
                FileUtils.deleteDirectory(new File(instanceDir));
            } catch (IOException e) {
                log.error("Unable to delete instance directory '"+
                    instanceDir+"' of SolrCore '"+name+"'! Please delete this" +
                            "directory manually.");
            }
        }
    }

    @Override
    public IndexMetadata updateIndex(String name, ArchiveInputStream ais) throws IOException {
        return updateIndex(name, ais, null);
    }

    @Override
    public IndexMetadata updateIndex(String name, ArchiveInputStream ais, String archiveCoreName) throws IOException {

        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("The parsed index name MUST NOT be NULL nor empty!");
        }
        IndexMetadata metadata = new IndexMetadata();
        metadata.setIndexName(name);
        metadata.setServerName(DEFAULT_SERVER_NAME);
        metadata.setSynchronized(false);
        metadata.setState(ManagedIndexState.ACTIVE);
        if (archiveCoreName != null) {
            metadata.setArchive(archiveCoreName);
        }
        return updateCore(metadata, ais);
    }

    @Override
    public IndexMetadata updateIndex(String name, String parsedResourceName, Properties properties) throws IOException {
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException("The parsed index name MUST NOT be NULL nor empty!");
        }
        String resourceName;
        if(!ConfigUtils.isValidSolrIndexFileName(parsedResourceName)){
            log.debug("add SolrIndexFileExtension to parsed indexArchive {}",parsedResourceName);
            resourceName = ConfigUtils.appandSolrIndexFileExtension(parsedResourceName, null);
        } else {
            resourceName = parsedResourceName;
        }
        Map<String,String> comments = new HashMap<String,String>();
        if(properties != null){
            for(Entry<Object,Object> prop : properties.entrySet()){
                comments.put(prop.getKey().toString(),prop.getValue().toString());
            }
        }
        InputStream is = null;
        for(Iterator<DataFileProvider> it = dataFileProviders.iterator();is == null && it.hasNext();){
            DataFileProvider dfp = it.next();
            try {
                is = dfp.getInputStream(null, resourceName, comments);
            }catch (IOException e) {
                //not found
            }
        }
        if(is != null || new File(managedSolrDir,parsedResourceName).isDirectory()){
            ArchiveInputStream ais;
            try {
                ais = ManagementUtils.getArchiveInputStream(resourceName, is);
            } catch (ArchiveException e) {
                throw new IOException("Unable to open ArchiveInputStream for resource '"+
                    resourceName+"'!",e);
            }
            IndexMetadata metadata = new IndexMetadata();
            if(properties != null){
                metadata.putAll(properties);
            }
            metadata.setIndexName(name);
            metadata.setServerName(DEFAULT_SERVER_NAME);
            metadata.setSynchronized(false);
            metadata.setState(ManagedIndexState.ACTIVE);
            metadata.setArchive(resourceName);
            return updateCore(metadata, ais);
        } else {
            return null;
        }
    }
    
    public String getDefaultCore(){
        return server.getDefaultCoreName();
    }
    @Override
    public IndexMetadata activateIndex(String indexName) throws IOException, SAXException {
        //if the index is already active -> return it
        IndexMetadata metadata = getIndexMetadata(indexName);
        if(metadata != null){
            return metadata;
        } else {
            //try to init an core for that directory located within the
            //managedDir
            return updateIndex(indexName, null);
        }
    }
    @Override
    public IndexMetadata deactivateIndex(String indexName) {
        IndexMetadata metadata;
        SolrCore core = server.remove(indexName);
        if(core != null){
            metadata = getMetadata(core,serverName);
            core.close();
            metadata.setState(ManagedIndexState.INACTIVE);
        } else {
            metadata = null;
        }
        return metadata;
    }
    /**
     * registers a {@link SolrCore} to the {@link #server} managed by this
     * instance. Will replace an already existing {@link SolrCore} with the 
     * same name
     * @param coreName the name of the {@link SolrCore} to register
     * @param coreDir the directory for the Core. If <code>null</code> is parsed
     * {@link #managedSolrDir}/coreName is used as default.
     */
    private void registerCore(String coreName, File coreDir) {
        if(coreName == null){
            coreName = server.getDefaultCoreName();
        }
        if(coreDir == null){ //use the coreName as default
            coreDir = new File(managedSolrDir,coreName);
        }
        if(!coreDir.isDirectory()){
            throw new IllegalArgumentException("The Core Directory '"+
                coreDir+" for the Core '"+coreName+"' does not exist or is not an directory");
        }
        SolrCore core;
        CoreDescriptor coreDescriptor = new CoreDescriptor(server, 
            coreName, coreDir.getAbsolutePath());
        core = server.create(coreDescriptor);
        //this will also replace an existing core with the same name
        server.register(coreName, core, false);
        server.persist(); //store the new/updated SolrCore in the solr.xml
    }
    private IndexMetadata updateCore(IndexMetadata metadata, ArchiveInputStream ais){
        String indexName = metadata.getIndexName();
        File coreDir = new File(managedSolrDir, indexName);
        if(!initCores.contains(indexName)){
            synchronized (initCores) {
                log.debug(" > start initializing SolrIndex {}" + indexName);
                initCores.add(indexName);
            }
            try {
                if(ais != null) { //copy the data
                    //not the third parameter (coreName) is not the name of this
                    //core, but the original name within the indexArchive
                    String archiveCoreName = getArchiveCoreName(metadata);
                    ConfigUtils.copyCore(ais, coreDir, archiveCoreName, false);
                    //third register the new Core
                } //else the data are already in place
                registerCore(indexName, coreDir);
                metadata.setDirectory(coreDir.getAbsolutePath());
            } catch (Exception e) {
                throw new IllegalStateException(String.format(
                    "Unable to copy default configuration for Solr Index %s to the configured path %s",
                    indexName, managedSolrDir.getAbsoluteFile()), e);
            } finally {
                // regardless what happened remove the index from the currently init
                // indexes and notify all other waiting for the initialisation
                synchronized (initCores) {
                    // initialisation done
                    initCores.remove(indexName);
                    log.debug("   ... notify after trying to init SolrIndex {}",indexName);
                    // notify that the initialisation completed or failed
                    initCores.notifyAll();
                }
            }
        } else { // the core is currently initialised ... wait until complete
            synchronized (initCores) {
                while (initCores.contains(indexName)) {
                    log.info(" > wait for initialisation of SolrIndex {}", indexName);
                    try {
                        initCores.wait();
                    } catch (InterruptedException e) {
                        // a core is initialised ... back to work
                    }
                }
                metadata.setDirectory(coreDir.getAbsolutePath());
            }
        }
        return metadata;       
    }
    public String getCoreForDirectory(String coreNameOrPath) {
        if(coreNameOrPath.charAt(coreNameOrPath.length()-1) != File.separatorChar){
            coreNameOrPath = coreNameOrPath+File.separatorChar;
        }
        for(SolrCore core : server.getCores()){
            String instanceDir = core.getCoreDescriptor().getInstanceDir();
            if(FilenameUtils.equalsNormalizedOnSystem(
                coreNameOrPath, instanceDir)){
                return core.getName();
            }
        }
        return null;
    }
    public CoreContainer getCoreContainer() {
        return server;
    }

    @Override
    public void swapIndexes(String indexName1, String indexName2) {
        if (!(isManagedIndex(indexName1) && isManagedIndex(indexName2))) {
            throw new IllegalArgumentException(String.format(
                "Both core names (%s,%s) must correspond to a managed index", indexName1, indexName2));
        }
        server.swap(indexName1, indexName2);
    }
}

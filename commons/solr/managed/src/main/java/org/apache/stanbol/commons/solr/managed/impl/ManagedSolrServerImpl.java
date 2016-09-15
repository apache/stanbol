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
package org.apache.stanbol.commons.solr.managed.impl;

import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_CORE_DIR;
import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_CORE_NAME;
import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_SERVER_DIR;
import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_SERVER_NAME;
import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_SERVER_PUBLISH_REST;
import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_SERVER_RANKING;
import static org.apache.stanbol.commons.solr.managed.ManagedIndexConstants.INDEX_NAME;
import static org.apache.stanbol.commons.solr.managed.ManagedSolrServer.MANAGED_SOLR_DIR_PROPERTY;
import static org.apache.stanbol.commons.solr.managed.util.ManagementUtils.getArchiveCoreName;
import static org.apache.stanbol.commons.solr.managed.util.ManagementUtils.substituteProperty;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.solr.common.SolrException;
import org.apache.stanbol.commons.solr.IndexReference;
import org.apache.stanbol.commons.solr.SolrConstants;
import org.apache.stanbol.commons.solr.SolrServerAdapter;
import org.apache.stanbol.commons.solr.SolrServerAdapter.SolrCoreProperties;
import org.apache.stanbol.commons.solr.SolrServerAdapter.SolrServerProperties;
import org.apache.stanbol.commons.solr.managed.IndexMetadata;
import org.apache.stanbol.commons.solr.managed.ManagedIndexState;
import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.apache.stanbol.commons.solr.managed.standalone.ClassPathDataFileProvider;
import org.apache.stanbol.commons.solr.managed.util.ManagementUtils;
import org.apache.stanbol.commons.solr.utils.ConfigUtils;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileListener;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProvider;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileTracker;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Implementation of the {@link ManagedSolrServer} interface for OSGI
 * @author Rupert Westenthaler
 *
 */
@Component(configurationFactory=true,
    immediate=true,
    specVersion="1.1",
    metatype=true,
    policy=ConfigurationPolicy.REQUIRE)
@Service(value=ManagedSolrServer.class)
@org.apache.felix.scr.annotations.Properties(value={
    @Property(name=PROPERTY_SERVER_NAME),
    @Property(name=MANAGED_SOLR_DIR_PROPERTY), 
    @Property(name=PROPERTY_SERVER_RANKING,intValue=0),
    @Property(name=PROPERTY_SERVER_PUBLISH_REST,boolValue=true)
})
public class ManagedSolrServerImpl implements ManagedSolrServer {

    private final Logger log = LoggerFactory.getLogger(ManagedSolrServerImpl.class);

    /**
     * The default path to root directory. All instances that do not define a 
     * value for {@link ManagedSolrServer#MANAGED_SOLR_DIR_PROPERTY} will
     * use this path + the name of the server as {@link #managedSolrDir}.<p>
     * The default is "${sling.home}indexes". '${sling.home}' ensures that indexes
     * will be stored relative to the sling home directory. However if this
     * property is not present indexes will be relative to the user home
     * instead.
     * @see ManagementUtils#substituteProperty(String, BundleContext)
     */
    public static final String DEFAULT_ROOT_PATH = "${sling.home}indexes";
    
    /**
     * Used by the {@link #indexArchiveTracker} to track index archive files
     * referenced by managed indexes
     * @see IndexArchiveTracker
     */
    @Reference
    private DataFileTracker dataFileTracker;
    /**
     * Used by the {@link #indexArchiveTracker} to directly request
     * {@link IndexMetadata#getIndexArchives() alternate index archives} in case
     * the {@link IndexMetadata#getArchive() currently used} gets 
     * {@link DataFileListener#unavailable(String) unavailable}
     * @see DataFileListener#unavailable(String)
     * @see IndexMetadata#getIndexArchives()
     * @see IndexMetadata#getArchive()
     */
    @Reference
    private DataFileProvider dataFileProvider;
    /**
     * Listener instance used for handling {@link DataFileTracker} notifications
     */
    private IndexArchiveTracker indexArchiveTracker;
    
    /**
     * The File representing the managed directory.
     */
    private File managedSolrDir;
    /**
     * The Solr CoreContainer to OSGI adapter
     */
    private SolrServerAdapter server;
    /**
     * The name of this server. Kept in an own variable to avoid access to
     * {@link SolrServerAdapter#getServerName()} for logging reasons that would
     * otherwise require to check if the {@link #server} variable != null.
     */
    private String serverName;
    /**
     * Used to keep track if a {@link #updateCore(String, ArchiveInputStream)}
     * or {@link #removeIndex(String, boolean)} operation is currently 
     * performing a CRUD operation on the {@link #server} instance to prevent
     * calls to {@link SolrServerAdapter#shutdown()}.
     * If such operations start an {@link Object token} is added to this collection
     * and as soon as the opertaion completes the token is removed and the
     * {@link Object#notifyAll()} is called on the list. Within the
     * {@link #deactivate(ComponentContext)} method it is waited until this
     * list is empty before {@link SolrServerAdapter#shutdown()} is called on
     * {@link #server}.
     */
    private Collection<Object> serverInUser = new HashSet<Object>();
    //private ComponentContext context;
    /**
     * Holds the list of cores that where installed by using
     * {@link #createSolrIndex(String, String, java.util.Properties)} but the {@link DataFileProvider}
     * could not yet provide the necessary data for the initialisation.
     * <p>
     * The list of uninitialised cores is stored within the data folder of the bundle under
     * {@link #UNINITIALISED_SITE_DIRECTORY_NAME}/{@link #serverName}.<p>
     * initialised during activation and cleared during deactivation. The
     * property files are saved to disc on any change. Therefore the in-memory
     * state is always in sync with the data on the disc.
     */
    private ManagedIndexMetadata managedCores;
    /**
     * Initialising Solr Indexes with a lot of data may take some time. Especially if the data need to be
     * copied to the managed directory. Therefore it is important to wait for the initialisation to be
     * complete before opening an Solr Index on it.
     * To this set all cores that are currently initialised are added. As soon as an initialisation completed
     * this set is notified.<p>
     * The name of the SolrIndex is used as key. The directory where the data
     * are copied represents the value.
     */
    private Map<String,File> initCores = new HashMap<String,File>();

    /**
     * Used to async call {@link #updateCore(String, ArchiveInputStream)}.
     * Initialised in {@link #activate(ComponentContext)} and closed in
     * {@link #deactivate(ComponentContext)}
     */
    private IndexUpdateDaemon updateDaemon;
//    private ServiceRegistration dfpServiceRegistration;
    /**
     * used to append suffixes to the core directories using the date of its
     * creation (patter: yyyy.MM.dd)
     */
    private DateFormat coreSuffixDateFormat = new SimpleDateFormat("yyyy.MM.dd");
    @Activate
    protected void activate(ComponentContext context) throws ConfigurationException {
        log.info("Activate ManagedSolrServer:");
//        this.context = context;
        BundleContext bc = context.getBundleContext();
        //first parse the configured Servername
        Object value = context.getProperties().get(PROPERTY_SERVER_NAME);
        if(value == null || value.toString().isEmpty()){
            throw new ConfigurationException(PROPERTY_SERVER_NAME, "The Server Name is a required" +
                    "Configuration and MUST NOT be NULL nor empty!");
        } else {
            serverName = value.toString();
            log.info(" > Name = {}",value.toString());
        }
        value = context.getProperties().get(MANAGED_SOLR_DIR_PROPERTY);
        if(value == null || value.toString().isEmpty()){
            managedSolrDir = new File(
                FilenameUtils.separatorsToSystem(
                    substituteProperty(DEFAULT_ROOT_PATH,bc)),
                serverName);
        } else {
            //note that property substitution is used on the parsed 
            //PROPERTY_SERVER_DIR value
            managedSolrDir = new File(
                FilenameUtils.separatorsToSystem(
                    substituteProperty(value.toString(), bc)));
            if(!managedSolrDir.isAbsolute()){
                managedSolrDir = new File(DEFAULT_ROOT_PATH,
                    //make sure to convert '/' and '\' to the platform separator
                    FilenameUtils.separatorsToSystem(value.toString()));
            }
        }
        log.info(" > managedDir = {}",managedSolrDir.getAbsolutePath());
        if(managedSolrDir.isFile()){
            throw new ConfigurationException(PROPERTY_SERVER_DIR, String.format(
                "The configured managed directory '%s'(dir: %s|name:%s) " +
                "exists but is no Directory!",managedSolrDir.getAbsolutePath(),
                value,serverName));
        }
        // check if the "solr.xml" file exists in the directory
        File solrConf = new File(managedSolrDir, "solr.xml");
        if (!solrConf.exists()) {
            log.info("   ... initialise managed directory '{}'",managedSolrDir);
            try {
                managedSolrDir = ConfigUtils.copyDefaultConfig(bc.getBundle(), managedSolrDir, false);
            } catch (IOException e) {
                throw new IllegalStateException(String.format(
                    "Unable to copy default configuration for the manages Solr Directory " +
                    "to the configured path '%s'!", managedSolrDir.getAbsoluteFile()), e);
            }
        } else {
            log.info("   .... managed directory '{}' already present and initialised",managedSolrDir);
        }
        //init the SolrServerProperties and read the other parameters form the config
        SolrServerProperties serverProperties = new SolrServerProperties(managedSolrDir);
        serverProperties.setServerName(serverName);
        value = context.getProperties().get(PROPERTY_SERVER_RANKING);
        if(value instanceof Number){
            serverProperties.setServerRanking(((Number)value).intValue());
        } else if(value != null && !value.toString().isEmpty()){
            try {
                serverProperties.setServerRanking(Integer.parseInt(value.toString()));
                log.info(" > Ranking = {}",serverProperties.getServerRanking());
            }catch (NumberFormatException e) {
               throw new ConfigurationException(PROPERTY_SERVER_RANKING, "The configured Server Ranking '"+
                   value+" can not be converted to an Integer!",e);
            }
        } //else not present or empty string -> do not set a ranking!
        value = context.getProperties().get(PROPERTY_SERVER_PUBLISH_REST);
        if(value == null || value instanceof Boolean) {
            serverProperties.setPublishREST((Boolean)value);
        } else {
            serverProperties.setPublishREST(Boolean.parseBoolean(value.toString()));
        }
        try {
            server = new SolrServerAdapter(context.getBundleContext(), serverProperties);
        } catch (SolrException e) {
            throw new ConfigurationException(PROPERTY_SERVER_DIR, "Unable to initialise " +
                    "a SolrServer based on the Directory '"+serverProperties.getServerDir() +
                    "'!",e);
        }
//        dfpServiceRegistration = context.getBundleContext().registerService(
//            DataFileProvider.class.getName(), 
//            new ClassPathSolrIndexConfigProvider(
//                context.getBundleContext().getBundle().getSymbolicName()), null);

        managedCores = new ManagedIndexMetadata(context);
        //After a restart of the CoreContainer we need to synchronise the state of
        //The cores with the state in the configs.
        //This may result in the activation of missing SolrCores as well as the
        //deactivation of unknown or inactive cores. It may also need to
        //change the state in the configuration in case a user has manually fixed
        //encountered problems while this service was deactivated.
        Collection<String> activeByMetadata = managedCores.getInState(ManagedIndexState.ACTIVE);
        Collection<String> activeOnSolrServer = new HashSet<String>(server.getCores());
        activeOnSolrServer.removeAll(activeByMetadata);
        activeByMetadata.removeAll(server.getCores());
        //NOW:
        // - activeOnSolrServer contains all active SolrCores that are not marked
        //   as active in the metadata
        // - activeByMetadata contains all active Indexes that are not registered
        //   as SolrCores on the CoreContainer
        //(1) Try to activate missing cores on the CoreContainer
        if(!activeByMetadata.isEmpty()){
            log.info("The following active managed Cores are not available on " +
            		"the SolrServer: {}",activeByMetadata);
            for(String indexName : activeByMetadata){
                IndexMetadata metadata = managedCores.getIndexMetadata(indexName);
                try {
                    activateCore(metadata, server);
                    log.info("  ... index {} successfully started!",indexName);
                } catch (IOException e) {
                    metadata.setError(e);
                    log.error("Unable to activate previously active SolrIndex '"+
                        metadata.getIndexReference()+"'!",e);
                } catch (SAXException e) {
                    metadata.setError(e);
                    log.error("Unable to activate previously active SolrIndex '"+
                        metadata.getIndexReference()+"'!",e);
                } catch (RuntimeException e) {
                    metadata.setError(e);
                    log.error("Unable to activate previously active SolrIndex '"+
                        metadata.getIndexReference()+"'!",e);
                //} finally { The metadata are not modified anyway!
                //    managedCores.store(metadata);
                }
            }
        }
        //(2) Process active SolrCores on the CoreContainer that are not active
        //    based on the configuration
        if(!activeOnSolrServer.isEmpty()){
            log.info("The following Cores active on the SolrServer are not " +
            		"marked as active in the Metadata: {}",activeOnSolrServer);
            log.info("Based on the Metadata (UNKNOWN ... no Index for that name):");
            for(String indexName : activeOnSolrServer){
                IndexMetadata metadata = managedCores.getIndexMetadata(indexName);
                ManagedIndexState state = metadata != null ? metadata.getState() : null;
                log.info("   - {} has state {}",indexName, state != null ? state : "UNKNOWN");
                if(metadata == null){
                    //unknown core ... deactivate
                    deactivateCore(indexName, server);
                    log.info("  ... deactiaved UNKOWN SolrCore {} on managed Solr Server {}",
                        indexName, serverName);
                } else if(state == ManagedIndexState.INACTIVE){
                    ////the metadata way this core should be deactivated!
                    deactivateCore(indexName, server); 
                    log.info("  ... deactiaved INACTIVE SolrCore {} on managed Solr Server {}",
                        indexName, serverName);
                } else if(state == ManagedIndexState.ERROR){
                    //looks like that the error was resolved ...
                    // ... maybe someone has manually edited some files and 
                    //     restarted this server
                    metadata.setState(ManagedIndexState.ACTIVE);
                    managedCores.store(metadata);
                    log.info("  ... successfully ACTIVATED SolrCore {} on managed Solr Server {}",
                        indexName, serverName);
                } else if(state == ManagedIndexState.UNINITIALISED){
                    //looks like someone has copied the required files manually
                    //to the solrServer ... update the metadata an activate
                    ManagementUtils.updateMetadata(metadata, server.getCore(indexName));
                    metadata.setState(ManagedIndexState.ACTIVE);
                    managedCores.store(metadata);
                    log.info("  ... successfully ACTIVATED SolrCore {} on managed Solr Server {}",
                        indexName, serverName);
                }
            }
        }
        //now init uninitialised cores and dataFile tracking for those
        //(1) start the daemon that asyc updates cores on DataFileListener events
        updateDaemon = new IndexUpdateDaemon();
        updateDaemon.start(); //start the thread
        //(2) init IndexArhive tracking
        indexArchiveTracker = new IndexArchiveTracker(
            dataFileTracker,dataFileProvider,managedCores,updateDaemon);
        log.info("   ... Managed SolrServer '{}' successfully initialised!", serverName);
    }
    @Deactivate
    protected void deactivate(ComponentContext context){
        if(server != null){
            synchronized (serverInUser) {
                while(!serverInUser.isEmpty()){
                    try {
                        serverInUser.wait();
                    } catch (InterruptedException e) {
                        log.debug("Waiting for outstanding Solr server opertations Interupped: '{}' left",
                            serverInUser.size());
                    }
                }
                server.shutdown();
            }
            server = null;
        }
//        if(dfpServiceRegistration != null) {
//            dfpServiceRegistration.unregister();
//            dfpServiceRegistration = null;
//        }
        if(indexArchiveTracker != null){
            indexArchiveTracker.close();
            indexArchiveTracker = null;
        }
        //shutting down the update daemon
        if(updateDaemon != null){
            updateDaemon.close();
            updateDaemon = null;
        }
        
        //stop tracking for uninitialised indexes
        indexArchiveTracker = null;
        managedCores = null;
        //serverName and managedSolrDir are not set to null to allow access
        //in loggings even if the component is already deactivated
        //managedSolrDir = null;
        //serverName = null;
    }
    @Override
    public IndexMetadata createSolrIndex(String indexName, String resourceName, Properties properties) throws IOException {
        if(indexName == null || indexName.isEmpty()){
            throw new IllegalArgumentException("The parsed index name MUST NOT be NULL nor empty!");
        }
        if(!ConfigUtils.isValidSolrIndexFileName(resourceName)){
            log.debug("add SolrIndexFileExtension to parsed indexArchive {}",resourceName);
            resourceName = ConfigUtils.appandSolrIndexFileExtension(resourceName, null);
        }
        if(isManagedIndex(indexName)){
            throw new IllegalArgumentException("An index with the parsed name '"+
                indexName+"' already exists on this managed Solr server '"+serverName+"'!");
        }
        IndexMetadata metadata = new IndexMetadata();
        if(properties != null){
            metadata.putAll(properties);
        }
        metadata.setServerName(serverName);
        metadata.setIndexName(indexName);
        metadata.setIndexArchives(Collections.singletonList(resourceName));
        metadata.setState(ManagedIndexState.UNINITIALISED);
        //TODO: we need to deal with the synchronised property!
        // now add the index to the list of uninitialised
        managedCores.store(metadata);
        //now start tracking this archive file
        indexArchiveTracker.addTracking(metadata);
        dataFileTracker.add(indexArchiveTracker, resourceName,
            IndexMetadata.toStringMap(metadata));
        return metadata;
    }

    @Override
    public IndexMetadata createSolrIndex(String indexName, ArchiveInputStream ais) throws IOException, SAXException {
        if(indexName == null || indexName.isEmpty()){
            throw new IllegalArgumentException("The parsed name of the index MUST NOT be NULL!");
        }
        if(isManagedIndex(indexName)){
            throw new IllegalArgumentException("An index with the parsed name '"+
                indexName+"' already exists on this managed Solr server '"+serverName+"'!");
        }
        return updateIndex(indexName, ais);
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
        return managedCores.getIndexMetadata(state);
    }

    @Override
    public File getSolrIndexDirectory(String indexName) {
        if(indexName == null || indexName.isEmpty()){
            throw new IllegalArgumentException("The parsed index name MUST NOT be NULL nor empty!");
        }
        SolrServerAdapter server = this.server;
        ServiceReference ref = server.getCore(indexName);
        String dirName = ref != null ?
                (String)ref.getProperty(SolrConstants.PROPERTY_CORE_DIR) :
                    null;
        return dirName == null ? null : new File(dirName);
    }

    @Override
    public ManagedIndexState getIndexState(String indexName) {
        if(indexName == null || indexName.isEmpty()){
            throw new IllegalArgumentException("The parsed index reference MUST NOT be NULL nor empty!");
        }
        return managedCores.getState(indexName);
    }

    @Override
    public boolean isManagedIndex(String indexName) {
        if(indexName == null || indexName.isEmpty()){
            throw new IllegalArgumentException("The parsed index reference MUST NOT be NULL nor empty!");
        }
        return managedCores.isManaged(indexName);
    }

    @Override
    public void removeIndex(String indexName, boolean deleteFiles) {
        if(indexName == null || indexName.isEmpty()){
            throw new IllegalArgumentException("The parsed index name MUST NOT be NULL nor empty!");
        }
        //remove the index from the metadata
        IndexMetadata metadata = managedCores.remove(indexName);
        if(metadata != null){
            //and also tracked index archives from the DataFileTracker
            indexArchiveTracker.removeTracking(metadata);
            uninitialiseCore(metadata,deleteFiles);
        }
    }
    /**
     * Uninitialise the index referenced by the parsed metadata and also deletes
     * the index data from the local file system if deleteFiles is enabled.
     * Updates to the state of the index are stored within the parsed
     * {@link IndexMetadata}.<p>
     * If the index is active, than the {@link SolrCore}
     * is first deactivated. 
     * @param metadata the metadata for the core. This instance is modified
     * but not saved to {@link #managedCores} within this method. 
     * So depending if callers want to remove or only uninitialise this core
     * the might want to store the updated version of this instance after this
     * method completes!
     * @param deleteFiles if the files on the local fileSystem should be deleted
     */
    protected final void uninitialiseCore(IndexMetadata metadata,boolean deleteFiles) {
        SolrServerAdapter server = this.server;
        File coreDir = null;
        if(metadata.isActive()){
            coreDir = deactivateCore(metadata.getIndexName(), server);
        }
        if(coreDir == null){
            String coreDirName = metadata.getDirectory();
            if(coreDirName != null){
                coreDir = new File(coreDirName);
            }
        }
        if(deleteFiles){
            metadata.setDirectory(null); //no directory assigned
            metadata.setArchive(null); //no archive used for the index
            if(coreDir != null){
                try {
                    FileUtils.deleteDirectory(coreDir);
                } catch (IOException e) {
                    log.error(String.format("Unable to delete Directory %s of the " +
                            "removed index '%s' of the managed SolrServer '{}'. " +
                            "Please try to delete this directory manually!",
                            coreDir.getAbsolutePath(),metadata.getIndexName(),
                            serverName),e); 
                }
            }
        }
        metadata.setState(ManagedIndexState.UNINITIALISED);
    }
    /**
     * Synchronises on {@link #serverInUser} and removes the core from the
     * {@link SolrServerAdapter}
     * @param indexName the name of the Index
     * @param server the server
     * @return the directory of the deactivated core or <code>null</code> if 
     * no core with that name was found.
     */
    private File deactivateCore(String indexName, SolrServerAdapter server) {
        ServiceReference coreRef = server != null ? server.getCore(indexName) : null;
        if(coreRef != null){
            Object token = new Object();
            synchronized (serverInUser) {
                serverInUser.add(token);
            }
            File coreDir;
            try {
                coreDir = getCoreDir(coreRef,true);
                server.removeCore(indexName);
            }finally {
                synchronized (serverInUser) {
                    serverInUser.remove(token);
                    token = null;
                    serverInUser.notifyAll();
                }
            }
            return coreDir;
        } else {
            return null;
        }
    }

    @Override
    public IndexMetadata updateIndex(String indexName, ArchiveInputStream ais) throws IOException, SAXException {
        return updateIndex(indexName, ais, null);
    }
    
    @Override
    public IndexMetadata updateIndex(String indexName, ArchiveInputStream ais, String archiveCoreName) throws IOException, SAXException {
        if(indexName == null || indexName.isEmpty()){
            throw new IllegalArgumentException("The parsed name for the index MUST NOT" +
            		"be NULL nor empty!");
        }
        if(ais == null){
            throw new IOException("The parsed ArchiveInputStream MUST NOT be NULL!");
        }
        IndexMetadata metadata = new IndexMetadata();
        metadata.setServerName(serverName);
        metadata.setIndexName(indexName);
        metadata.setSynchronized(false);
        if (archiveCoreName != null) {
            metadata.setArchive(archiveCoreName);
        }
        try {
            updateCore(metadata, ais);
        } finally {
            managedCores.store(metadata);
        }
        return metadata;
    }

    @Override
    public IndexMetadata updateIndex(String indexName, String resourceName, Properties properties) throws IOException {
        //NOTE: this does not deactivate the current index version, but only updates
        //the metadata and re-registers the DataFileTracking
        IndexMetadata oldMetadata = managedCores.getIndexMetadata(indexName);
        IndexMetadata metadata = new IndexMetadata();
        if(properties != null){
            metadata.putAll(properties);
        }
        metadata.setServerName(serverName);
        metadata.setIndexName(indexName);
        metadata.setIndexArchives(Collections.singletonList(resourceName));
        if(oldMetadata != null){ //we need to
            metadata.setState(oldMetadata.getState()); //same as for the old version
            metadata.setDirectory(oldMetadata.getDirectory());
        } else {
            metadata.setState(ManagedIndexState.UNINITIALISED);
        }
        //TODO: we need to deal with the synchronised property!
        // now add the index to the list of uninitialised
        managedCores.store(metadata);
        indexArchiveTracker.updateTracking(oldMetadata,metadata);
        return metadata;
    }
    @Override
    public IndexMetadata deactivateIndex(String indexName) {
        IndexMetadata metadata = managedCores.getIndexMetadata(indexName);
        if(metadata != null && metadata.getState() == ManagedIndexState.ACTIVE){
            try {
                deactivateCore(indexName, server);
                metadata.setState(ManagedIndexState.INACTIVE);
            } catch (RuntimeException e) {
                metadata.setError(e);
            } finally {
                managedCores.store(metadata);
            }
        }
        return metadata;
    }
    @Override
    public IndexMetadata activateIndex(String indexName) throws IOException, SAXException {
        IndexMetadata metadata = managedCores.getIndexMetadata(indexName);
        if(metadata != null && metadata.getState() == ManagedIndexState.INACTIVE){
            try {
                activateCore(metadata, server);
                metadata.setState(ManagedIndexState.ACTIVE);
            } catch (IOException e) {
                metadata.setError(e);
                throw e;
            } catch (SAXException e) {
                metadata.setError(e);
                throw e;
            } catch (RuntimeException e) {
                metadata.setError(e);
                throw e;
            } finally {
                managedCores.store(metadata);
            }
        }
        return metadata;
    }
    @Override
    public IndexMetadata getIndexMetadata(String indexName) {
        if(indexName == null || indexName.isEmpty()){
            throw new IllegalArgumentException("The parsed index name MUST NOT be NULL nor empty!");
        }
        return managedCores.getIndexMetadata(indexName);
    }
    /**
     * Getter for the name of a Core based on the {@link IndexReference}.
     * Checks for both path and name
     * @param indexRef the indexreference
     * @return the name of the core or <code>null</code> if not found
     */
    private String getCoreName(IndexReference indexRef) {
        String coreName;
        if(indexRef.isPath()){ //try to convert the path to the name
            ServiceReference coreRef = server.getCoreForDir(indexRef.getIndex());
            if(coreRef != null){
                coreName = (String)coreRef.getProperty(PROPERTY_CORE_NAME);
            } else {
                coreName = null;
            }
        } else {
            coreName = indexRef.getIndex();
        }
        return coreName;
    }
    /**
     * Updates the core with the parsed name with the data parsed within the
     * ArchiveInputStream and stores updated to the state of the index within the
     * parsed {@link IndexMetadata} instance.
     * @param metadata the metadata of the index to update. The parsed instance
     * is updated within this method
     * @param ais the data
     * @throws IOException On any Error while copying the data for the Index
     * @throws SAXException On any Error while parsing the Solr Configuration for
     */
    protected final void updateCore(final IndexMetadata metadata, ArchiveInputStream ais) throws IOException, SAXException {
        if (metadata == null) {
            throw new IllegalArgumentException("The parsed metadata for the Solr index MUST NOT be NULL");
        }
        if (metadata.isEmpty()) {
            throw new IllegalArgumentException("The parsed metadata for the Solr index MUST NOT be empty");
        }
        String coreName = metadata.getIndexName();
        if(coreName == null || coreName.isEmpty()){
            throw new IllegalArgumentException("The parse metadata do not contain a valid value for the '"+
                INDEX_NAME+"'!");
        }
        SolrServerAdapter server = this.server;
        if(server == null){
            log.info("Unable to update core '{}' because this ManagedSolrServer is already deactivated.");
            return;
        }
        ServiceReference coreRef = server.getCore(coreName);
        File currentCoreDir; //dir of the previous version
        if(coreRef != null){
            currentCoreDir = getCoreDir(coreRef,true);
        } else { //no old version
            currentCoreDir = null;
        }
        String coreDirName; //the name of the "new" core directory
        synchronized (coreSuffixDateFormat) {
            //SimpleDateFormat is not thread save. It may fail badly on two 
            //concurrent calls. 
            coreDirName = coreName+'-'+coreSuffixDateFormat.format(new Date());
        }
        File coreDir = new File(managedSolrDir, coreDirName);
        int count = 1;
        while(coreDir.exists()){
            //if this is the second call on a day ... add a count
            //if directories get deleted directories with a higher count might
            //be older versions than directories without or with a lower count!
            coreDir = new File(managedSolrDir, coreDirName+"-"+count);
            count++;
        }
        metadata.setDirectory(coreDir.getName()); //TODO maybe we need to call getAbsolute path
        // no the initialisation/update of this core starts!
        synchronized (initCores) {
            log.debug(" > start initializing SolrIndex {}" + coreName);
            initCores.put(coreName,coreDir);
        }
        try { //outer try for finally removal from initCores
            try {
                //not the third parameter (coreName) is not the name of this
                //core, but the original name within the indexArchive
                String archiveCoreName = getArchiveCoreName(metadata);
                ConfigUtils.copyCore(ais, coreDir, archiveCoreName, false);
            } catch (IOException e) {
                e =  new IOException(String.format(
                    "Unable to copy Data for index '%s' (server '%s')",
                    coreName,serverName),e);
                //store this Error in the metadata
                metadata.setError(e);
                throw e;
            }
            try {
                activateCore(metadata, server);
                metadata.setState(ManagedIndexState.ACTIVE);
                if(currentCoreDir != null){
                    //remove the data of the old core
                    try {
                        FileUtils.deleteDirectory(currentCoreDir);
                    } catch (IOException e) {
                        //only log an Error and do not throw an Exception in that case
                        log.error(String.format("Unable to delete Directory %s of the " +
                                "old (and no longer needed) version of the index '%s' " +
                                "of the managed SolrServer '{}'. Please try to " +
                                "delete this directory manually!",
                                currentCoreDir.getAbsolutePath(),coreName,
                                serverName),e); 
                    }
                }
            }catch (IOException e) {
                //store Errors in the metadata
                metadata.setError(e);
                throw e;
            } catch (SAXException e) {
                metadata.setError(e);
                throw e;
            } catch (RuntimeException e) {
                metadata.setError(e);
                throw e;
            }
        } finally {
            // regardless what happened remove the index from the currently init
            // indexes and notify all other waiting for the initialisation
            synchronized (initCores) {
                // initialisation done
                initCores.remove(coreName);
                log.debug("   ... notify after trying to init SolrIndex {}" + coreName);
                // notify that the initialisation completed or failed
                initCores.notifyAll();
            }
        }
    }
    
    /**
     * Activates the core and updates the parsed metadata accordingly
     * @param metadata the metadata. Not modified by this method
     * @param server the server to activate the core on
     * @throws IOException On any error while accessing the core configuration files
     * @throws SAXException On any error while parsing the core configuration files
     * @throws IllegalStateException if a {@link ParserConfigurationException}
     * is thrown by the server.
     */
    protected final void activateCore(IndexMetadata metadata, SolrServerAdapter server) throws IOException, SAXException {
        SolrCoreProperties coreConfig = new SolrCoreProperties(metadata.getIndexName());
        coreConfig.setCoreDir(new File(managedSolrDir,metadata.getDirectory()));
        Object token = new Object();
        synchronized (serverInUser) {
            //prevent shutting down the server while we initialise a core
            serverInUser.add(token);
        }
        try {
            server.registerCore(coreConfig);
        } catch (SolrException e) {
            throw new IOException(String.format(
                "Unable to activate the SolrCore configuration for index " +
                "'%s' of managed SolrServer '%s'",
                metadata.getIndexName(), serverName), e);
        } finally {
            synchronized (serverInUser) {
                serverInUser.remove(token);
                token = null;
                serverInUser.notifyAll();
            }
        }
    }

    /**
     * Getter for the CoreDir based on the ServiceReference. In addition
     * it checks if the value exists and if the value is an Directory on the
     * local file System.
     * @param coreRef the ServiceReference to a core (MUST NOT be NULL)
     * @param exists if <code>true</code> it is checked that the coreDir exists
     * and is an directory. if <code>false</code> it is ensured that the
     * coreDir does not yet exist
     * @return the directory of this core
     * @throws IllegalStateException if the reference is missing a value for
     * {@link SolrConstants#PROPERTY_CORE_NAME} or if the validation based on the
     * parsed exists state fails.
     */
    private File getCoreDir(ServiceReference coreRef,boolean exists) {
        String coreName = (String)coreRef.getProperty(PROPERTY_CORE_NAME);
        File coreDir;
        String dirName = (String)coreRef.getProperty(PROPERTY_CORE_DIR);
        if(dirName == null){
            //this should never happen -> fail early
            throw new IllegalStateException("Required Property '" +
                PROPERTY_CORE_DIR+"' not present in properties of Core '"+
                coreName+" of managed SolrServer '"+
                serverName+"'!");
        }
        coreDir = new File(dirName);
        if((exists && !coreDir.isDirectory()) || //must exist and is not a directory
                !exists && coreDir.exists()){ //must not exist but File exists
            //this should never happen -> fail early
            throw new IllegalStateException("Property '" +
                PROPERTY_CORE_DIR+"' of Core '"+coreName+
                " (managedSolrServer= "+serverName+
                " points to a Directory "+dirName+" that MUST "+
                (exists ? "" : "NOT")+"exist!");
        }
        return coreDir;
    }

    
    /**
     * Listener for Solr Index Archives of uninitialised cores.
     * @author Rupert Westenthaler
     *
     */
    private static class IndexArchiveTracker implements DataFileListener {
        
        private final Logger log = LoggerFactory.getLogger(IndexArchiveTracker.class);
        private final DataFileTracker tracker;
        private final DataFileProvider provider;
        private final ManagedIndexMetadata managedCores;
        private final IndexUpdateDaemon indexUpdateDaemon;
        protected IndexArchiveTracker(DataFileTracker tracker, 
                                      DataFileProvider provider,
                                      ManagedIndexMetadata managedCores,
                                      IndexUpdateDaemon indexUpdateDaemon){
            this.tracker = tracker;
            this.provider = provider;
            this.managedCores = managedCores;
            this.indexUpdateDaemon = indexUpdateDaemon;
            for(String indexName : managedCores.getManaged()){
                addTracking(managedCores.getIndexMetadata(indexName));
            }
        }
        public void updateTracking(IndexMetadata oldMetadata, IndexMetadata metadata) {
            //for now a simple remove/re-add implementation
            removeTracking(oldMetadata);
            addTracking(metadata);
        }
        public void close() {
            tracker.removeAll(this);
        }
        @Override
        protected void finalize() throws Throwable {
            close();
            super.finalize();
        }
        /**
         * @param indexName
         */
        public void addTracking(IndexMetadata metadata) {
            if(metadata != null){ //may be removed in the meantime
                if(!(metadata.isActive() || metadata.isInactive()) || metadata.isSynchronized()){
                    String archive = metadata.getArchive();
                    boolean found = false; //only track higher priority files as the current
                    for(String indexArchive : metadata.getIndexArchives()){
                        if(!found){
                            if(indexArchive.equals(archive)){
                                found = true; 
                            }
                            tracker.add(this, indexArchive,
                                IndexMetadata.toStringMap(metadata));
                        } // else higher priority archive present -> no tracking
                    }
                } //else (active || inactive) and not syncronized -> no tracking
            }
        };
        /**
         * Stops tracking for Archive files of this specific index
         * @param metadata
         */
        public void removeTracking(IndexMetadata metadata){
            if(metadata != null){
                for(String indexArchive : metadata.getIndexArchives()){
                    //check if this archive is still used by an other index
                    if(managedCores.getIndexNames(indexArchive).isEmpty()){
                        tracker.remove(this, indexArchive);
                    }
                }
            }
            
        }
        
        @Override
        public boolean unavailable(String resource) {
            log.info("IndexArchive {} unavailable ...",resource);
            for(String indexName : managedCores.getIndexNames(resource)){
                IndexMetadata metadata = managedCores.getIndexMetadata(indexName);
                if(metadata != null){ //may be removed in the meantime
                    String currentArchive = metadata.getArchive();
                    boolean inSync = metadata.isSynchronized();
                    if(resource.equals(currentArchive)){ //current archive may be null
                        currentArchive = null; //reset the current archive to null (none)
                        ArchiveInputStream ais = null;
                        for(String archive : metadata.getIndexArchives()){
                            if(!archive.equals(resource)) {
                                if(currentArchive == null){
                                    try {
                                        InputStream is = provider.getInputStream(null, archive, null);
                                        if(is != null){
                                            ais = ManagementUtils.getArchiveInputStream(archive, is);
                                        } else {
                                            ais = null;
                                        }
                                    } catch (IOException e) {
                                       //not available
                                        ais = null;
                                    } catch (ArchiveException e) {
                                        log.error("Unable to open ArchiveInputStream for RDFTerm '"+
                                            archive+"'!",e);
                                        ais = null;
                                    }
                                    if(ais != null){ //ais != null also
                                        currentArchive = archive; //currentArchive != null
                                    }
                                }
                                //if resource become unavailable we might need to
                                //add resources for tracking
                                if(!tracker.isTracked(this, null, archive) && //if not already tracked
                                        (currentArchive == null || ( //and no archive found
                                                currentArchive != null && inSync))){ //or found but inSync
                                        tracker.add(this, archive,
                                            IndexMetadata.toStringMap(metadata));
                                } // else already tracked or no tracking needed
                            }
                        }
                        //If we have now a currentArchive and an ais we can
                        //switch to an alternate archive.
                        //If not we need to switch this in index in the UNAVAILABLE
                        // state
                        metadata.setArchive(currentArchive);//update the metadata
                        managedCores.store(metadata);
                        //if the parsed ais is NULL the index will be uninitialised
                        indexUpdateDaemon.update(
                            currentArchive == null ? ManagedIndexState.UNINITIALISED :
                                    ManagedIndexState.ACTIVE,
                                    metadata, ais);
                    } // else an unused archive is no longer available -> nothing to do
                } //else are not available -> nothing to do
            } //end for all Indexes using the archive that is no longer available
            return false; //never remove an registration after an unavailable event
        }
    
        @Override
        public boolean available(String resourceName, InputStream is) {
            log.info("IndexArchive {} available ...",resourceName);
            ArchiveInputStream ais;
            try {
                ais = ManagementUtils.getArchiveInputStream(resourceName, is);
            } catch (ArchiveException e) {
                log.error("Unable to open ArchiveInputStream for RDFTerm '"+
                    resourceName+"'!",e);
                ais = null;
            }
            if(ais != null){
                boolean keepTracking = false;
                for(String indexName : managedCores.getIndexNames(resourceName)){
                    IndexMetadata metadata = managedCores.getIndexMetadata(indexName);
                    if(metadata != null){ //the core might be deleted in the meantime
                        List<String> archives = metadata.getIndexArchives();
                        String currentArchive = metadata.getArchive();
                        if(currentArchive == null || 
                                archives.indexOf(resourceName) < archives.indexOf(currentArchive)){
                            metadata.setArchive(resourceName);
                            managedCores.store(metadata);
                            indexUpdateDaemon.update(ManagedIndexState.ACTIVE,metadata, ais);
                            //if synchronised do not remove this listener
                            keepTracking = keepTracking || metadata.isSynchronized();
                        } else { //currently used Archive is of higher priority as
                            // this one.
                            //keep tracking if synchronised
                            keepTracking = keepTracking || metadata.isSynchronized();
                        }
                    } //else managed core was deleted in the meantime ...
                }
                return !keepTracking;
            } else { //unable to create an ArchiveInputStrem 
                return false; //TODO: add support for ERROR state to the Tracker!
            }
        }

    }
    
    
    
    /**
     * Used to perform the potential long running update operations on Solr
     * cores in an own daemon. This can also be used to create a thread pool to
     * allow initialisation of n cores at the same time. 
     * @author Rupert Westenthaler
     *
     */
    private class IndexUpdateDaemon extends Thread {

        private final class IndexActionInfo {
            public final ArchiveInputStream ais;
            public final IndexMetadata metadata;
            public final ManagedIndexState action;
            private IndexActionInfo(ManagedIndexState action,ArchiveInputStream ais, IndexMetadata metadata){
                this.ais = ais;
                this.metadata = metadata;
                this.action = action;
            }
        }
        private Map<String, IndexActionInfo> toUpdate = new HashMap<String,IndexActionInfo>();

        private boolean active = true;
        
        public void close(){
            active = false;
            synchronized (toUpdate) {
                toUpdate.notifyAll();
            }
        }
        public void update(ManagedIndexState desiredState, IndexMetadata metadata,ArchiveInputStream ais){
            if(metadata == null){
                throw new IllegalArgumentException("The parsed IndexMetadata MUST NOT be NULL");
            }
            String name = metadata.getIndexName();
            if(name == null || name.isEmpty()){
                throw new IllegalArgumentException("The parsed IndexMetadata MUST contain a valid name (NOT NULL and NOT empty)!");
            }
            if(desiredState == null){
                throw new IllegalArgumentException("The parsed desired ManagedIndexState MUST NOT be NULL");
            }
            log.info("Update Request for {} (server: {}, desired state: {}, from Archive: {})", 
                new Object[]{metadata.getIndexName(), metadata.getServerName(),
                    desiredState.name(), metadata.getArchive()});
            switch (desiredState) {
                case ACTIVE:
                    if(ais == null){
                        throw new IllegalArgumentException("If the parsed ManagedIndexState is ACTIVE, " +
                        		"than the parsed ArchiveInputStream MUST NOT be NULL!");
                    }
                    break;
                case UNINITIALISED:
                    if(ais != null){
                        log.warn("Parsed ArchiveInputStream is NOT NULL but desired ManagedIndexState is UNINITIALISED." +
                        		"The parsed stream will not be used!");
                    }
                    IOUtils.closeQuietly(ais); //close the stream
                    ais = null;
                    break;
                default:
                    throw new IllegalArgumentException("The IndexUpdateDeamon only supports the ManagedIndexStates ACTIVE and UNINITIALISED!");
            }
            synchronized (toUpdate) {
                toUpdate.put(name, new IndexActionInfo(desiredState,ais, metadata));
                toUpdate.notifyAll();
            }
        }
        @Override
        public void run() {
            while(active){
                Entry<String,IndexActionInfo> entry;
                while(!toUpdate.isEmpty()) {
                    synchronized (toUpdate) {
                        Iterator<Entry<String,IndexActionInfo>> it = toUpdate.entrySet().iterator();
                        if(it.hasNext()){ //get the next element
                            entry = it.next();
                            it.remove(); //and remove it
                        } else {
                            entry = null;
                        }
                    }
                    if(entry != null){
                        //IndexMetadata metadata = managedCores.getIndexMetadata(entry.getKey());
                        IndexActionInfo info = entry.getValue();
                        if(isManagedIndex(entry.getKey())){
                            if(info.action == ManagedIndexState.ACTIVE){
                                log.info(" ... start to ACTIVATE Index {} on ManagedSolrServer",entry.getKey(),info.metadata.getServerName());
                                try {
                                    updateCore(info.metadata, info.ais);
                                    log.info(" ... Index {} on ManagedSolrServer {} is now ACTIVE",entry.getKey(),info.metadata.getServerName());
                                } catch (IOException e) {
                                        log.error("IOException while activating Index '"+
                                            info.metadata.getServerName()+':'+
                                            info.metadata.getIndexName()+"'!",e);
                                        info.metadata.setError(e);
                                } catch (SAXException e) {
                                        log.error("SAXException while activating Index '"+
                                            info.metadata.getServerName()+':'+
                                            info.metadata.getIndexName()+"'!",e);
                                        info.metadata.setError(e);
                                } catch (RuntimeException e) {
                                        log.error("Exception while activating Index '"+
                                            info.metadata.getServerName()+':'+
                                            info.metadata.getIndexName()+"'!",e);
                                        info.metadata.setError(e);
                                } finally {
                                    managedCores.store(info.metadata);
                                }
                            } else { //desired state UNINITIALISED
                                log.info(" ... start to UNINITIALISE Index {} on ManagedSolrServer",entry.getKey(),info.metadata.getServerName());
                                try {
                                    uninitialiseCore(info.metadata,true);
                                    log.info(" ... Index {} on ManagedSolrServer {} is now UNINITIALISED",entry.getKey(),info.metadata.getServerName());
                                } catch (RuntimeException e) {
                                    log.error("Exception while uninitialising Index '"+
                                        info.metadata.getServerName()+':'+
                                        info.metadata.getIndexName()+"'!",e);
                                    info.metadata.setError(e);
                                } finally {
                                    // store the updated metadata
                                    managedCores.store(info.metadata);
                                }
                            }
                        } else { //else removed in the meantime -> nothing to do
                            log.info("ingonre Update request for Index {} with desired state {} " +
                            		"because this index seams to be no longer managed!",
                            		entry.getKey(),info.action);
                        }
                    }
                }
                synchronized (toUpdate) {
                    try {
                        toUpdate.wait();
                    } catch (InterruptedException e) {
                        log.debug("interrupted to update {} core",toUpdate.size());
                    }
                }
            }
        }
    }

    @Override
    public void swapIndexes(String indexName1, String indexName2) {
        if (!(isManagedIndex(indexName1) && isManagedIndex(indexName2))) {
            throw new IllegalArgumentException(String.format(
                "Both core names (%s,%s) must correspond to a managed index", indexName1, indexName2));
        }
        if (!(managedCores.isInState(ManagedIndexState.ACTIVE, indexName1) && managedCores.isInState(
            ManagedIndexState.ACTIVE, indexName2))) {
            throw new IllegalStateException(String.format(
                "Both cores (%s,%s) should be in ManagedIndexState.ACTIVE state", indexName1, indexName2));
        }
        Object token = new Object();
        synchronized (serverInUser) {
            serverInUser.add(token);
        }
        try {
            server.swap(indexName1, indexName2);
            IndexMetadata core1Metadata = getIndexMetadata(indexName1);
            IndexMetadata core2Metadata = getIndexMetadata(indexName2);
            String core2Directory = core2Metadata.getDirectory();
            core2Metadata.setDirectory(core1Metadata.getDirectory());
            core1Metadata.setDirectory(core2Directory);
            managedCores.store(core1Metadata);
            managedCores.store(core2Metadata);
        } finally {
            synchronized (serverInUser) {
                serverInUser.remove(token);
                token = null;
                serverInUser.notifyAll();
            }
        }
    }
}

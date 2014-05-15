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
package org.apache.stanbol.entityhub.indexing.destination.solryard;

import static org.apache.stanbol.entityhub.yard.solr.impl.SolrYardConfig.ALLOW_INITIALISATION_STATE;
import static org.apache.stanbol.entityhub.yard.solr.impl.SolrYardConfig.DOCUMENT_BOOST_FIELD;
import static org.apache.stanbol.entityhub.yard.solr.impl.SolrYardConfig.MULTI_YARD_INDEX_LAYOUT;
import static org.apache.stanbol.entityhub.yard.solr.impl.SolrYardConfig.SOLR_SERVER_LOCATION;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.dataimport.SolrWriter;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.apache.stanbol.entityhub.core.mapping.FieldMappingUtils;
import org.apache.stanbol.entityhub.core.site.CacheUtils;
import org.apache.stanbol.entityhub.indexing.core.IndexingDestination;
import org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig;
import org.apache.stanbol.entityhub.indexing.core.destination.OsgiConfigurationUtil;
import org.apache.stanbol.entityhub.indexing.destination.solryard.fst.CorpusCreationInfo;
import org.apache.stanbol.entityhub.indexing.destination.solryard.fst.CorpusCreationTask;
import org.apache.stanbol.entityhub.indexing.destination.solryard.fst.FstConfig;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapper;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapping;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.commons.solr.IndexReference;
import org.apache.stanbol.commons.solr.SolrConstants;
import org.apache.stanbol.commons.solr.managed.ManagedIndexConstants;
import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.apache.stanbol.commons.solr.managed.standalone.DefaultStandaloneManagedSolrServerWrapper;
import org.apache.stanbol.commons.solr.managed.standalone.StandaloneEmbeddedSolrServerProvider;
import org.apache.stanbol.commons.solr.managed.standalone.StandaloneManagedSolrServer;
import org.apache.stanbol.entityhub.yard.solr.impl.SolrYard;
import org.apache.stanbol.entityhub.yard.solr.impl.SolrYardConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrYardIndexingDestination implements IndexingDestination {
    
    private static final Logger log = LoggerFactory.getLogger(SolrYardIndexingDestination.class);

    /**
     * The relative path to the Solr 'write.lock' file (relative to the Solr
     * instance dir). This file is excluded from the
     * Solr Index archive built by {@link #writeSolrIndexArchive()} (see
     * <a href="https://issues.apache.org/jira/browse/STANBOL-1176">STANBOL-1176</a>
     * for more details).
     */
    private static final String SOLR_WRITE_LOCK = FilenameUtils.separatorsToSystem("data/index/write.lock");
    
    /**
     * Parameter used to refer to the name of the properties file containing the
     * field names as key and the {@link Float} boost factors as values. As
     * default no boosts will be used for indexing.
     */
    public static final String PARAM_FIELD_BOOST_CONFIG = "boosts";
    /**
     * Parameter used to explicitly set the name of the creates SolrYard
     * configuration. The default value will be set to the name of the dataSet
     * and adding "Index" to the end.
     */
    public static final String PARAM_YARD_NAME = "name";
    /**
     * Parameter used to explicitly set the name of the created Solr Index. The
     * default will be set to the name of the dataset to be indexed as returned
     * by {@link IndexingConfig#getName()}.
     */
    public static final String PARAM_SOLR_INDEX_NAME = "indexName";
    
    /**
     * Parameter used to set the name of the directory used as root for the 
     * SolrIndex. The value is relative to the 
     * {@link IndexingConfig#getDestinationFolder()}. The default value is
     * {@link #DEFAULT_SOLR_INDEX_DIRECTORY}
     */
    public static final String PARAM_SOLR_INDEX_DIRECTORY = "solrDir";
    /**
     * Parameter used to specify the name of the directory relative to the
     * {@link IndexingConfig#getConfigFolder()} that contains the
     * Solr configuration used for indexing. The default is that the config is
     * searches under a folder with the name provided by
     * {@link #PARAM_SOLR_INDEX_NAME} (that defaults to
     * {@link IndexingConfig#getName()}). <p>
     * However note that when this parameter is missing this configuration is 
     * optional (meaning that if it is not found the
     * default Solr Configuration is used). When this parameter is used, than
     * the configuration is required and an {@link IllegalArgumentException} is
     * thrown it not found.<p>
     * To use the default, but marking the configuration as required one can
     * add this parameter without a value.
     */
    public static final String PARAM_SOLR_CONFIG = "solrConf";
    /**
     * The default value for the directory holding the Solr index set to
     * {@link SolrDirectoryManager#DEFAULT_SOLR_DATA_DIR}
     */
    public static final String DEFAULT_SOLR_INDEX_DIRECTORY = 
        ManagedSolrServer.DEFAULT_SOLR_DATA_DIR;
    /**
     * The field used to boost documents while indexing. This is set to
     * {@link RdfResourceEnum#entityRank}
     */
    public static final String DOCUMENT_BOOST_FIELD = RdfResourceEnum.entityRank.getUri();
    /**
     * The extension of the distribution file
     */
    public static final String SOLR_INDEX_ARCHIVE_EXTENSION = ".solrindex.zip";
    /**
     * The extension of the solrIndex reference file
     */
    public static final String SOLR_INDEX_ARCHIVE_REF_EXTENSION = ".solrindex.ref";
    /**
     * The ID of the OSGI component used by the SolrYard implementation
     */
    public static final String SOLR_YARD_COMPONENT_ID = "org.apache.stanbol.entityhub.yard.solr.impl.SolrYard";

    /**
     * The default value for the {@link ManagedIndexConstants#SYNCHRONIZED}
     * property added to the SorlIndex reference file. The default is set to
     * <code>true</code>. This will users allow to update the data for the
     * ReferencedSite by simple replacing the solrindex Archive in the
     * <code>/datafile</code> folder.<p>
     * This property can be configured by using the main "indexing.properties"
     * file.
     */
    public static final boolean DEFAULT_SYNCHRONIZED_STATE = true;
    
    /**
     * The name of the properties file containing the FST configuration.<p>
     * If not present no FST models will be created in the {@link #finalise()}
     * state.
     */
    public static final String FST_CONF = "fstConf";
    /**
     * The number of Threads used to concurrently build FST models
     */
    public static final String FST_THREADS = "fstThreads";
    
    private static final int DEFAULT_FST_THREADS = 4;
    /**
     * The location of the SolrIndex. This MUST BE an absolute Path in case it 
     * refers to a directory of the local file system and <code>null</code> in
     * case an external SolrServer is used.
     * Also NOTE that this can be an different value than returned by calling
     * {@link SolrYardConfig#getSolrServerLocation()} on {@link #solrIndexConfig}
     */
    private File solrIndexLocation;

    /**
     * Directory holding the specialised Solr configuration or <code>null</code>
     * if the default configuration should be used
     */
    private File solrIndexConfig;
    
    /**
     * The configuration used to instantiate the {@link SolrYard} returned by
     * {@link #getYard()}.
     */
    private SolrYardConfig solrYardConfig;

    private SolrYard solrYard;

    /**
     * File used to write the ZIP archive containing the solr index.
     */
    private File solrArchive;
    /**
     * File used to write the properties file that refers to {@link #solrArchive}.
     * This is typically included in distributions of huge indexes and will
     * request the user to download the archive with the actual data.
     */
    private File solrArchiveRef;
    /**
     * This provides metadata about what fields and languages are indexed in the
     * created SolrIndex.
     */
    private Collection<FieldMapping> indexFieldConfiguration;

    private IndexingConfig indexingConfig;

    /*
     * Fields required for the FST model creation 
     */
    /**
     * The SolrCore used by the {@link #solrYard}
     */
    private SolrCore core;
    /**
     * The FST configurations. Parsed in the {@link #setConfiguration(Map)}
     * and initialised during {@link #initialise()}. <code>null</code> if no
     * {@link #FST_CONF} is set.
     */
    private List<FstConfig> fstConfigs;
    /**
     * The number of threads used to build FST models. 
     * Set in {@link #setConfiguration(Map)}
     */
    private int fstThreads = DEFAULT_FST_THREADS;

    private NamespacePrefixService  namespacePrefixService;
    
    /**
     * This Constructor relays on a subsequent call to 
     * {@link #setConfiguration(Map)} to parse the required configuration
     */
    public SolrYardIndexingDestination(){
        
    }
    /**
     * Constructs an SolrYard based IndexingTarget based on the parsed parameters
     * @param yardName the name of the SolrYard
     * @param solrLocation the location of the SolrYard
     */
    public SolrYardIndexingDestination(String yardName,String solrLocation, 
            NamespacePrefixService namespacePrefixService){
        this(yardName,solrLocation,null,null,null,namespacePrefixService);
    }
    /**
     * Constructs an SolrYard based IndexingTarget based on the parsed parameters
     * @param yardName the name of the SolrYard
     * @param parsedSolrLocation the location of the SolrYard
     * @param solrConfig directory holding the Solr schema used for the indexing or 
     * <code>null</code> to use the default
     * @param indexFieldConfig The field and languages indexed in this index
     * @param fieldBoostMap A map containing field names as key and boost factors
     * as values. Parse <code>null</code> to use no boosts.
     */
    public SolrYardIndexingDestination(final String yardName,
                                       final String parsedSolrLocation,
                                       final String solrConfig,
                                       Collection<FieldMapping> indexFieldConfig,
                                       Map<String,Float> fieldBoostMap,
                                       NamespacePrefixService namespacePrefixService){
        if(yardName == null || yardName.isEmpty()){
            throw new IllegalArgumentException("Tha name of the Yard MUST NOT be NULL nor empty!");
        }
        if(parsedSolrLocation == null || parsedSolrLocation.isEmpty()){
            throw new IllegalArgumentException("Tha parsed Solr location MUST NOT be NULL nor empty!");
        }
        if(namespacePrefixService == null){
            throw new IllegalArgumentException("The parsed NamespacePrefixService MUST NOT be NULL!");
        }
        this.indexFieldConfiguration = indexFieldConfig;
        this.solrYardConfig = createSolrYardConfig(yardName, parsedSolrLocation);
        //init the manages solr directory relative to the working directory
        File managedDirectory = new File(System.getProperty("user.dir"), DEFAULT_SOLR_INDEX_DIRECTORY);
        //init the solr directory and validate the parsed values
        File[] solrDirectories = initSolrDirectories(parsedSolrLocation, solrConfig, 
            managedDirectory);
        this.solrIndexLocation = solrDirectories[0];
        this.solrIndexConfig = solrDirectories[1];
        this.solrArchive = solrDirectories[2];
        this.solrArchiveRef = solrDirectories[3];
        //set Boost related stuff
        solrYardConfig.setDocumentBoostFieldName(DOCUMENT_BOOST_FIELD);
        if(fieldBoostMap != null){
            solrYardConfig.setFieldBoosts(fieldBoostMap);
        }
    }
    /**
     * Processes the parsed solr index location (may be an URL, an absolute path
     * or a relative one) and the optional solr schema configuration (only valid
     * in case an relative path was parsed as location) and does all the
     * initialisation work (including to set the 
     * {@link SolrDirectoryManager#MANAGED_SOLR_DIR_PROPERTY} system property)
     * @param parsedSolrLocation the parsed location of the SolrServer (may be 
     * an URL, an absolute path or a relative one)
     * @param solrConfig the path to the directory holding the configuration
     * for the Solr index used for the indexing or <code>null</code> to use the
     * default (only supported in case parsedSolrLocation is a relative path)
     * @param managedDirectory the directory used to manage the Solr index (only
     * needed in case parsedSolrLocation is an
     * @return An array with the length 4 where index <ul>
     * <li>"0" contains the File pointing to the directory holding the 
     * index on the local file system
     * <li>"1" contains the File pointing to the directory containing the
     * configuration used to initialise the index.
     * <li>"2" contains the File used to create the compressed ZIP archive with
     * the indexed data
     * <li>"3" contains the File used to create the properties file used to link
     * to the Solr index archive.
     * 
     * All files will be <code>null</code> if the values are not applicable to 
     * the current configuration.
     */
    private File[] initSolrDirectories(final String parsedSolrLocation,
                                   final String solrConfig,
                                   File managedDirectory) {
        File solrIndexLocation;
        File solrConfigLocation;
        File solrIndexArchive;
        File solrIndexArchiveRef;
        //set the SolrLocation and init the SolrDirectoryManager system property
        //in case the solrLocation is not an remote SolrServer
        if(parsedSolrLocation.startsWith("http") 
                && parsedSolrLocation.indexOf("://") > 0){ //matches http[s]://{host}
            solrIndexLocation = null;
            if(solrConfig != null){
                //rather throw an error as indexing for some hours to an index
                //with the wrong schema!
                throw new IllegalArgumentException(String.format(
                    "Parsing special Solr Configurations (directory=%s) is not " +
                    "supported for remote SolrServer (url=%s",
                    parsedSolrLocation,solrConfig));
            }
            solrConfigLocation = null; //no configuration supported
            solrIndexArchive = null;
            solrIndexArchiveRef = null;
        } else { // local Directory
            File parsedSolrLocationFile = new File(parsedSolrLocation);
            if(parsedSolrLocationFile.isAbsolute()){ //if absolute 
                //-> assume an already configured Solr index
                solrIndexLocation = parsedSolrLocationFile;
                if(solrConfig != null){
                    throw new IllegalArgumentException(String.format(
                        "Parsing special Solr Configurations (directory=%s) is not " +
                        "supported for Embedded SolrServer configured via an absolute" +
                        "file path (path=%s", parsedSolrLocation,solrConfig));
                }
                solrConfigLocation = null; //no solr conf supported
            } else { //relative path -> init the Solr directory
                //set the managed directory
                if(managedDirectory == null){
                    throw new IllegalStateException("In case the Solr index location"+
                        "is a relative path the parsed managed directory MUST NOT be NULL!");
                }
                System.setProperty(ManagedSolrServer.MANAGED_SOLR_DIR_PROPERTY, 
                    managedDirectory.getAbsolutePath());
                //add the name of the core and save it to solrLocation
                //TODO: get the name of the default server somehow ...
                File serverLocation = new File(managedDirectory,"default");
                solrIndexLocation = new File(serverLocation,parsedSolrLocation);
                //check if there is a special SolrLocation
                if(solrConfig != null){
                    solrConfigLocation = new File(solrConfig);
                    if(!solrConfigLocation.isDirectory()){
                        throw new IllegalArgumentException("The parsed Solr Configuration "+
                            solrConfigLocation+" does not exist or is not an direcotry!");
                    } //else the directory exists ... 
                    //lets assume it is a valid configuration
                    //otherwise an exception will be thrown in initialise().
                } else {
                    solrConfigLocation = null; //no configuration parsed
                }
            }
            solrIndexArchive = new File(solrIndexLocation.getName()+SOLR_INDEX_ARCHIVE_EXTENSION);
            solrIndexArchiveRef = new File(solrIndexLocation.getName()+SOLR_INDEX_ARCHIVE_REF_EXTENSION);
        }
        return new File[]{solrIndexLocation,solrConfigLocation,
                          solrIndexArchive,solrIndexArchiveRef};
    }

    @Override
    public void setConfiguration(Map<String,Object> config) {
        indexingConfig = (IndexingConfig)config.get(IndexingConfig.KEY_INDEXING_CONFIG);
        namespacePrefixService = indexingConfig.getNamespacePrefixService();
        String yardName;
        //read the Yard name configuration
        Object value = config.get(PARAM_YARD_NAME);
        if(value == null || value.toString().isEmpty()){
            yardName = indexingConfig.getName()+"Index";
        } else {
            yardName = value.toString();
        }
        //read the Solr index name configuration
        String indexName;
        value = config.get(PARAM_SOLR_INDEX_NAME);
        if(value == null || value.toString().isEmpty()){
            indexName = indexingConfig.getName();
        } else {
            indexName = value.toString();
        }
        this.solrYardConfig = createSolrYardConfig(yardName, indexName);
        this.solrYardConfig.setName("SolrIndex for "+indexingConfig.getName());
        //set the Index Field Configuration
        this.indexFieldConfiguration = indexingConfig.getIndexFieldConfiguration();
        //set a description for the yard
        if(indexingConfig.getDescription() != null){
            //reuse the description
            solrYardConfig.setDescription(indexingConfig.getDescription());
        } else {
            solrYardConfig.setDescription("SolrYard based Index for "+indexingConfig.getName());
        }
        //get the directors holding the solr configuration
        String solrConfig;
        if(!config.containsKey(PARAM_SOLR_CONFIG)){ //not present
            // -> use the default config
            File configDir = indexingConfig.getConfigFile(indexName);
            if(!configDir.isDirectory()){
                log.info("use default Solr index configuration for index "+indexName);
                solrConfig = null;
            } else {
                solrConfig = configDir.getAbsolutePath();
            }
        } else { //require the config
            value = config.get(PARAM_SOLR_CONFIG);
            if(value == null || value.toString().isEmpty()){
                value = indexName; //use the indexName as default
            }
            File configDir = indexingConfig.getConfigFile(value.toString());
            if(!configDir.isDirectory()){
                throw new IllegalArgumentException("Required Solr Configuration "+
                    value.toString()+" not found within the config directory "+
                    indexingConfig.getConfigFolder().getAbsolutePath());
            } else {
                solrConfig = configDir.getAbsolutePath();
            }
        }
        //init the managed directory within the destination folder
        //read the Solr directory configuration
        String solrDir;
        value = config.get(PARAM_SOLR_INDEX_DIRECTORY);
        if(value == null || value.toString().isEmpty()){
            solrDir = DEFAULT_SOLR_INDEX_DIRECTORY;
        } else {
            solrDir = value.toString();
        }
        File managedDirectory = new File(indexingConfig.getDestinationFolder(),solrDir);
        File[] solrDirectories = initSolrDirectories(indexName, solrConfig, 
            managedDirectory);
        this.solrIndexLocation = solrDirectories[0];
        this.solrIndexConfig = solrDirectories[1];
        this.solrArchive = solrDirectories[2];
        this.solrArchiveRef = solrDirectories[3];
        //init other configurations
        solrYardConfig.setDocumentBoostFieldName(DOCUMENT_BOOST_FIELD);
        value = config.get(PARAM_FIELD_BOOST_CONFIG);
        if(value != null && !value.toString().isEmpty()){
            Map<String,Float> fieldBoosts = new HashMap<String,Float>();
            //load this configuration as required if set to cause an Exception
            //if not found! -> an exception is the better option as creating an
            //index with missing Field Boosts!
            for(Entry<String,Object> entry : indexingConfig.getConfig(value.toString(),true).entrySet()){
                try {
                    fieldBoosts.put(entry.getKey(), Float.valueOf(entry.getValue().toString()));
                } catch (Exception e) {
                    //throw exception for any invalid entry!
                    throw new IllegalArgumentException(String.format(
                        "Unable to parse Field Boost entry from field %s and boost %s",
                        entry.getKey(),entry.getValue()),e);
                }
            }
            solrYardConfig.setFieldBoosts(fieldBoosts);
        }
        //read the FST config
        value = config.get(FST_CONF);
        if(value != null && !StringUtils.isBlank(value.toString())){
            File fstConfigFile = indexingConfig.getConfigFile(value.toString());
            if(!fstConfigFile.isFile()){
                throw new IllegalArgumentException(String.format(
                    "Unable to find configured FST configuration file %s",
                    fstConfigFile));
            }
            Collection<String> lines;
            try {
                lines = FileUtils.readLines(fstConfigFile, "UTF-8");
            } catch (IOException e) {
                throw new IllegalArgumentException(String.format(
                    "Unable to read FST configuration file %s",
                    fstConfigFile),e);
            }
            setFstConfig(lines);
        }
        value = config.get(FST_THREADS);
        if(value instanceof Number){
            fstThreads = ((Number)value).intValue();
        } else if(value != null){
            try {
                fstThreads = Integer.parseInt(value.toString());
            }catch (NumberFormatException e) {
                throw new IllegalArgumentException("Unable to parse the FST thread number from "
                    +value.toString(), e);
            }
        }
        if(fstThreads <= 0){
            fstThreads = DEFAULT_FST_THREADS;
        }
        
    }
    /**
     * Setter for the FST configurations using the same format as defined by the
     * configuration file. This defines the FST models create in the
     * {@link #finalise()} phase of the indexing process
     * @param lines the single FST configurations
     */
    public void setFstConfig(Collection<String> lines) {
        this.fstConfigs = Collections.unmodifiableList(parseFstConfig(lines));
    }
    /**
     * Setter for the ThreadPool used to create FST models
     * @param size
     */
    public void setFstThreads(int size){
        if(size < 1){
            this.fstThreads = DEFAULT_FST_THREADS;
        } else {
            this.fstThreads = size;
        }
    }
    public int getFstThreads() {
        return fstThreads;
    }
    /**
     * Getter for the FST models that are created in the {@link #finalise()}
     * phase
     * @return the FST c
     */
    public List<FstConfig> getFstConfig(){
        return fstConfigs;
    }
    /**
     * @param lines
     */
    private List<FstConfig> parseFstConfig(Collection<String> lines) {
        List<FstConfig> fstConfigs = new ArrayList<FstConfig>();
        for(String line : lines){
            line = line.trim();
            if(!line.isEmpty() && line.charAt(0) != '#'){
                String[] fields = new String[] {null,null};
                int index = -1;
                for(String part : line.split("=|;")){
                    if(index >= 0){
                        fields[index] = part;
                        index = -1;
                    } else if("index".equalsIgnoreCase(part)){
                        index = 0;
                    } else if("store".equalsIgnoreCase(part)){
                        index = 1;
                    }
                }
                if(fields[0] == null){
                    throw new IllegalArgumentException("Invalid FST configuration "
                        + "line: "+line +". Param 'index={field}' is required "
                        + "(syntax: 'index={field};store={field}', 'store is optional'')!");
                }
                fstConfigs.add(new FstConfig(fields[0], fields[1]));
            }
        }
        return fstConfigs;
    }
    /**
     * Creates a {@link SolrYardConfig} and initialised it to used single Yard
     * Layout, lazy commits and a commitWithin duration of an minute
     * @param yardName the name of the yard
     * @param indexName the name of the index
     */
    private SolrYardConfig createSolrYardConfig(String yardName, String indexName) {
        SolrYardConfig solrYardConfig = new SolrYardConfig(yardName, indexName);
        solrYardConfig.setMultiYardIndexLayout(Boolean.FALSE);
        //use the lazy commit feature
        solrYardConfig.setImmediateCommit(Boolean.FALSE);
        solrYardConfig.setCommitWithinDuration(1000*60);//one minute
        return solrYardConfig;
    }

    @Override
    public boolean needsInitialisation() {
        return true;
    }

    @Override
    public void initialise() {
        log.info("initialise {}",getClass().getSimpleName());
        //The constructors and the setConfiguration(..) only validate the parsed
        //parameters and initialise the member variables. This method performs 
        //the the actual initialisation of the SolrYard!
        //copy a custom configuration (if present)
        EmbeddedSolrServer server;
        IndexReference solrServerRef = IndexReference.parse(solrYardConfig.getSolrServerLocation());
        if(solrIndexConfig != null){ //can only be != null if also solrIndexLocation
            //copy the configuration
            try {
                log.info(" ... copy Solr Configuration form {} to {}",solrIndexConfig,solrIndexLocation);
                FileUtils.copyDirectory(solrIndexConfig, solrIndexLocation);
            } catch (IOException e) {
                throw new IllegalStateException(String.format(
                    "Unable to copy the Solr index configuration from %s to %s!",
                    solrIndexConfig,solrIndexLocation),e);
            }
            solrYardConfig.setAllowInitialisation(Boolean.FALSE);
            server = StandaloneEmbeddedSolrServerProvider.getInstance().getSolrServer(
                solrServerRef,solrServerRef.getIndex());
            this.core = server.getCoreContainer().getCore(solrServerRef.getIndex());
        } else {
            //allow the default initialisation
            solrYardConfig.setAllowInitialisation(Boolean.TRUE);
            StandaloneEmbeddedSolrServerProvider.getInstance();
            server = StandaloneEmbeddedSolrServerProvider.getInstance().getSolrServer(
                solrServerRef,solrYardConfig.getIndexConfigurationName());
            if(server != null){
                log.info("   ... initialised SolrCore with default configuration");
                this.core = server.getCoreContainer().getCore(solrServerRef.getIndex());
            } else if(solrServerRef.isPath() && new File(solrServerRef.getIndex()).isAbsolute()){
                //the parsed absolute path is not within the managed SolrServer
                //so we need to create some CoreContainer and init/register
                //the core at the parsed location
                StandaloneManagedSolrServer s;
                if(solrServerRef.getServer() == null){
                    s = StandaloneManagedSolrServer.getManagedServer();
                } else {
                    s = StandaloneManagedSolrServer.getManagedServer(solrServerRef.getServer());
                }
                CoreContainer cc = s.getCoreContainer();
                CoreDescriptor cd = new CoreDescriptor(cc, "dummy", 
                    solrServerRef.getIndex());
                this.core = cc.create(cd);
                cc.register(core, false);
                server = new EmbeddedSolrServer(cc, "dummy");
                log.info("   ... initialised existing SolrCore at {}",solrServerRef.getIndex());
            } else {
                throw new IllegalStateException("Unable to initialise SolrCore "+solrServerRef);
            }
        }
        log.info("   ... create SolrYard");
        this.solrYard = new SolrYard(server,solrYardConfig, namespacePrefixService);
    }

    @Override
    public Yard getYard() {
        if(solrYard == null){
            throw new IllegalStateException("SolrYard not initialised. Call initialise first!");
        }
        return solrYard;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void finalise() {
        //write the indexing configuration
        if(indexFieldConfiguration != null){
            FieldMapper mapper = FieldMappingUtils.createDefaultFieldMapper(indexFieldConfiguration);
            try {
                CacheUtils.storeBaseMappingsConfiguration(solrYard, mapper);
            } catch (YardException e) {
                log.error("Unable to store FieldMapperConfiguration to the Store!",e);
            }
        }
        log.info(" ... optimize SolrCore");
        try {
            solrYard.optimize();
        } catch (YardException e) {
            log.error("Unable to optimize SolrIndex after indexing! IndexArchive will not be optimized ...",e);
        }
        //build the FST models
        if(fstConfigs != null){
            //(1) FST config initialisation
            log.info(" ... init FST configuration(s)");
            IndexSchema schema = core.getLatestSchema();
            File fstDir = new File(new File(core.getDataDir()),"fst");
            if(!fstDir.isDirectory()){
                try {
                    FileUtils.forceMkdir(fstDir);
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to create Directory "
                        + fstDir.getAbsolutePath() + "for storing the FST models "
                        + "of SolrCore "+core.getName());
                }
            }
            RefCounted<SolrIndexSearcher> searcherRef = core.getSearcher();
            try {
                for(FstConfig fstConfig : fstConfigs){
                    fstConfig.setFstDirectory(fstDir); //set the FST directory
                    log.info("> FST config {}", fstConfig);
                    fstConfig.buildConfig(schema, searcherRef.get().getAtomicReader());
                    for(CorpusCreationInfo corpus : fstConfig.getCorpusCreationInfos()){
                        log.info("  - {}",corpus);
                    }
                }
            } finally {
                searcherRef.decref();
            }

            List<Future<?>> fstCreationTasks = new ArrayList<Future<?>>();
            ExecutorService es = Executors.newFixedThreadPool(fstThreads);
            log.info(" ... build FST models ");
            for(FstConfig config : fstConfigs){
                for(final CorpusCreationInfo corpus : config.getCorpusCreationInfos()){
                    fstCreationTasks.add(es.submit(new CorpusCreationTask(core, corpus)));
                }
            }
            //now wait for the completion of the tasks
            Iterator<Future<?>> taskIt = fstCreationTasks.iterator();
            while(taskIt.hasNext()){
                Future<?> task = taskIt.next();
                try {
                    task.get(); //wait until ready
                    taskIt.remove();
                } catch (ExecutionException e) {
                    log.error("Exception while building FST models for SolrCore "
                            + core.getName(),e);
                } catch (InterruptedException e) {
                    log.error("Interupped while building FST models for SolrCore "
                            + core.getName(),e);
                    Thread.currentThread().interrupt();
                    
                }
            }
            if(!fstCreationTasks.isEmpty()){
                log.warn("Unable to build {} FST models for SolrCore {}",
                    fstCreationTasks.size(), core.getName());
            } else {
                log.info("All FST modles for SolrCore {} build successfully!",
                    core.getName());
            }
        } //no FST modles to build
        
        //all Solr specific stuff is now ready
        log.info(" ... close SolrCore");
        solrYard.close();
        
        //if a indexing config is present we need to create the distribution files
        if(indexingConfig != null){
        	//first check if the distribution folder needs to be created and is valid
        	File distFolder = indexingConfig.getDistributionFolder();
        	if(!distFolder.exists()){
        		if(!distFolder.mkdirs()){
        			throw new IllegalStateException("Unable to create distribution folder " +
        					distFolder.getAbsolutePath());
        		}
        	} else if(!distFolder.isDirectory()){
        		throw new IllegalStateException("Distribution folder" + distFolder.getAbsolutePath()
        				+ "is not a Directory!");
        	}
            //zip the index and copy it over to distribution
            log.info(" ... build Solr index archive");
            if(solrArchive != null){
                try {
                    writeSolrIndexArchive(indexingConfig);
                }catch (IOException e) {
                    log.error("Error while creating Solr Archive "+solrArchive.getAbsolutePath()+
                        "! The archive will not be created!",e);
                    log.error("As a Workaround you can manually create the Solr Archive " +
                            "by creating a ZIP archive with the contents of the Folder " +
                            solrIndexLocation+"!");
                }
            }
            if(solrArchiveRef != null){
                try {
                    writeSolrIndexReference(indexingConfig);
                } catch (IOException e) {
                    log.error("Error while creating Solr Archive Reference "+
                        solrArchiveRef.getAbsolutePath()+
                        "! The file will not be created!",e);
                }
            }
            //finally create the Osgi Configuration
            try {
                OsgiConfigurationUtil.writeSiteConfiguration(indexingConfig);
            } catch (IOException e) {
                log.error("Unable to write OSGI configuration file for the referenced site",e);
            }
            try {
                OsgiConfigurationUtil.writeCacheConfiguration(indexingConfig);
            } catch (IOException e) {
                log.error("Unable to write OSGI configuration file for the Cache",e);
            }
            //create the SolrYard configuration
            try {
                writeSolrYardConfiguration(indexingConfig);
            } catch (IOException e) {
                log.error("Unable to write OSGI configuration file for the SolrYard",e);
            }
            //create the bundle
            OsgiConfigurationUtil.createBundle(indexingConfig);
        }
    }
    /**
     * 
     */
    private void writeSolrIndexReference(IndexingConfig indexingConfig) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("Index-Archive", solrArchive.getName());
        properties.setProperty("Name", solrYardConfig.getName());
        if(solrYardConfig.getDescription() != null){
            properties.setProperty("Description", solrYardConfig.getDescription());
        }
        Object syncronizedConfig = indexingConfig.getProperty(ManagedIndexConstants.SYNCHRONIZED);
        if(syncronizedConfig != null){
            properties.setProperty(ManagedIndexConstants.SYNCHRONIZED, 
                Boolean.toString(Boolean.parseBoolean(syncronizedConfig.toString())));
        } else {
            properties.setProperty(ManagedIndexConstants.SYNCHRONIZED,
                Boolean.toString(DEFAULT_SYNCHRONIZED_STATE));
        }
        File solrArchiveFile = new File(
            OsgiConfigurationUtil.getConfigDirectory(indexingConfig),solrArchiveRef.getName());
        properties.store(new FileOutputStream(solrArchiveFile), null);
    }
    /**
     * 
     */
    private void writeSolrIndexArchive(IndexingConfig indexingConfig) throws IOException{
        //we need to get the length of the parent to calc the entry names for
        //the archvie
        //Note that the Archive need to include the name of the index,
        //therefore we need use the parent dir as context
        int parentPathLength = solrIndexLocation.getParentFile().getAbsolutePath().length();
        if(solrIndexLocation.getAbsolutePath().charAt(parentPathLength-1) != File.separatorChar){
            parentPathLength++; //add the missing '/'
        }
        //Moved over to use java.util.zip because Apache commons compression
        //seams not support files > 2Gb
        File solrArchiveFile = new File(indexingConfig.getDistributionFolder(),solrArchive.getName());
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(solrArchiveFile));
        for(File file : FileUtils.listFiles(solrIndexLocation, null, true)){
            String name = file.getAbsolutePath().substring(parentPathLength);
            if(!file.isHidden() && !name.endsWith(SOLR_WRITE_LOCK)){
                log.info("add "+name);
                out.putNextEntry(new ZipEntry(name));
                if(!file.isDirectory()){
                    FileInputStream fileIn = new FileInputStream(file);
                    IOUtils.copyLarge(fileIn,out);
                    out.closeEntry();
                    IOUtils.closeQuietly(fileIn);
                }
            } else {
                log.info("exclude "+name);
            }
        }
        out.finish();
        IOUtils.closeQuietly(out);
    }
    /**
     * @throws IOException 
     * 
     */
    private void writeSolrYardConfiguration(IndexingConfig indexingConfig) throws IOException {
        Dictionary<String,Object> yardConfig = OsgiConfigurationUtil.createYardConfig(indexingConfig);
        //we need now add the solrYard specific parameters
        String fieldBoostName = solrYardConfig.getDocumentBoostFieldName();
        if(fieldBoostName != null){
            yardConfig.put(DOCUMENT_BOOST_FIELD, fieldBoostName);
        }
        //TODO: fieldBoosts are currently not supported by the SolrYard Config
        //solrYardConfig.getFieldBoosts();
        
        //The default values for the following parameters are OK 
        //solrYardConfig.getMaxBooleanClauses();
        //solrYardConfig.getMaxQueryResultNumber();
        yardConfig.put(SOLR_SERVER_LOCATION, FilenameUtils.getName(solrYardConfig.getSolrServerLocation()));
        //the server type needs not to be set. It is automatically detected by
        //the value of the server location
        //solrYardConfig.getSolrServerType();
        
        //deactivate default initialisation!
        yardConfig.put(ALLOW_INITIALISATION_STATE, Boolean.FALSE);
        
        //for immediate commit use the default value (optionally one could also
        //fore TRUE)
        //yardConfig.put(SolrYard.IMMEDIATE_COMMIT, Boolean.TRUE);
        
        //deactivate multi yard layout!
        yardConfig.put(MULTI_YARD_INDEX_LAYOUT, Boolean.FALSE);
        
        String solrYardConfigFileName = SOLR_YARD_COMPONENT_ID+'-'+indexingConfig.getName()+".config";
        OsgiConfigurationUtil.writeOsgiConfig(indexingConfig,solrYardConfigFileName, yardConfig);
    }

    @Override
    public void close() {
        //nothing todo
    }

}

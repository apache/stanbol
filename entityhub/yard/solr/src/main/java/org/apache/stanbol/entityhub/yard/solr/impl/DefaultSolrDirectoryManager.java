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

import static org.apache.stanbol.entityhub.yard.solr.impl.ConfigUtils.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProvider;
import org.apache.stanbol.entityhub.yard.solr.SolrDirectoryManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the {@link SolrDirectoryManager} interface that supports the dynamic initialisation of
 * new cores based on the default core configuration contained within the SolrYard bundle.
 * 
 * @author Rupert Westenthaler
 * 
 */
@Component(immediate = true, metatype = true)
@Service
@Properties(value = {@Property(name = SolrDirectoryManager.MANAGED_SOLR_DIR_PROPERTY, value = SolrDirectoryManager.DEFAULT_SOLR_DATA_DIR)})
public class DefaultSolrDirectoryManager implements SolrDirectoryManager {
    /**
     * The logger
     */
    private final Logger log = LoggerFactory.getLogger(DefaultSolrDirectoryManager.class);

    /**
     * This key is used to store the file name of the archive supposed to provide the data for the
     * uninitialised index within the configuration the configuration
     */
    private static final String UNINITIALISED_INDEX_ARCHIVE_NAME_KEY = "Uninitialised-Index-Archive-Name";

    /**
     * The dataFileProvider used to lookup index data.
     */
    @Reference
    private DataFileProvider dataFileProvider;
    /**
     * The directory used by the internally managed embedded solr server. Use {@link #lookupManagedSolrDir()}
     * instead of using this member, because this member is not initialised within the constructor or the
     * {@link #activate(ComponentContext)} method.
     */
    private File managedSolrDir;

    /**
     * The component context. Only available when running within an OSGI Environment and the component is
     * active.
     */
    private ComponentContext componentContext;
    /**
     * For some functionality within this component it is important to track if this instance operates within
     * or outside of an OSGI environment. because of this this boolean is set to true as soon as the first
     * time {@link #activate(ComponentContext)} or {@link #deactivate(ComponentContext)} is called. If someone
     * knows a better method to check that feel free to change!
     */
    private boolean withinOSGI = false;
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
     * Holds the list of cores that where installed by using
     * {@link #createSolrDirectory(String, String, java.util.Properties)} but the {@link DataFileProvider}
     * could not yet provide the necessary data for the initialisation.
     * <p>
     * The list of uninitialised cores is stored within the data folder of the bundle under
     * {@link #UNINITIALISED_SITE_DIRECTORY_NAME} and loaded at activation.
     * 
     */
    private Map<String,java.util.Properties> uninitialisedCores = new HashMap<String,java.util.Properties>();

    /**
     * OSGI {@link ServiceRegistration} for the internal {@link DataFileProvider}
     * used to load index configurations via classpath.<p>
     * This service is registered/unregistered in the activate/deactivate
     * method 
     */
    private ServiceRegistration dfpServiceRegistration;
    
    public DefaultSolrDirectoryManager() {}

    /**
     * Internally used to lookup the {@link DataFileProvider} instance because
     * this Method initialises an instance of the {@link ClassPathSolrIndexConfigProvider}
     * in case the component runs outside of an OSGI environment.
     * @return the DatafileProvider
     */
    private DataFileProvider getDataFileProvider(){
        if(dataFileProvider == null && !withinOSGI){
            this.dataFileProvider = new ClassPathSolrIndexConfigProvider(null);
        }
        return dataFileProvider;
    }
    /*
     * (non-Javadoc)
     * 
     * @see org.apache.stanbol.entityhub.yard.solr.impl.ManagedSolrDirectory#isSolrDir(java.lang.String)
     */
    @Override
    public final boolean isManagedIndex(String solrIndexName) throws IllegalStateException {
        if (solrIndexName == null) {
            throw new IllegalArgumentException("The parsed name of the Solr index MUST NOT be NULL");
        }
        if (solrIndexName.isEmpty()) {
            throw new IllegalArgumentException("The parsed name of the Solr index MUST NOT be empty");
        }
        // first check if the directory for the parsed index exists
        boolean exists = new File(lookupManagedSolrDir(componentContext), solrIndexName).exists();
        return !exists ? // if no directory exists
        // check also if an uninitialised index was requested
        uninitialisedCores.containsKey(solrIndexName)
                : true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.stanbol.entityhub.yard.solr.impl.ManagedSolrDirectory#getManagedIndices()
     */
    @Override
    public final Map<String,File> getManagedIndices() throws IllegalStateException {
        File solrDir = lookupManagedSolrDir(componentContext);
        String[] indexNames = solrDir.list(DirectoryFileFilter.INSTANCE);
        Map<String,File> indexes = new HashMap<String,File>();
        for (String indexName : indexNames) {
            File coreSchema = new File(solrDir,indexName+File.separatorChar+"conf"+File.separatorChar+"schema.xml");
            File coreConf =  new File(solrDir,indexName+File.separatorChar+"conf"+File.separatorChar+"solrconfig.xml");
            // TODO: validate that this is actually a SolrCore!
            if(coreSchema.isFile() && coreConf.isFile()){
                indexes.put(indexName, new File(solrDir, indexName));
            } else {
                log.debug("directory {} in managed Solr directory {} is no Solr Core!",
                    indexName,solrDir);
            }
        }
        // we need also add the uninitialised indexes (with a null as value)
        for (String indexName : uninitialisedCores.keySet()) {
            if (!indexes.containsKey(indexName)) {
                indexes.put(indexName, null);
            }
        }
        return indexes;
    }
    @Override
    public boolean isInitialisedIndex(String solrIndexName) {
        if (solrIndexName == null) {
            throw new IllegalArgumentException("The parsed name of the Solr index MUST NOT be NULL");
        }
        if (solrIndexName.isEmpty()) {
            throw new IllegalArgumentException("The parsed name of the Solr index MUST NOT be empty");
        }
        if (initCores.contains(solrIndexName)) { // if it is currently initialised
            return false; // return false
        } else { // check if the dir is there
            return new File(lookupManagedSolrDir(componentContext), solrIndexName).exists();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.stanbol.entityhub.yard.solr.impl.ManagedSolrDirectory#getSolrDirectory(java.lang.String)
     */
    @Override
    public final File getSolrIndexDirectory(final String solrIndexName) throws IllegalArgumentException {
        return initSolrDirectory(solrIndexName, null, componentContext);
    }

    @Override
    public final File createSolrIndex(final String solrIndexName, ArchiveInputStream ais) {
        return initSolrDirectory(solrIndexName, ais, componentContext);
    }

    @Override
    public final File createSolrDirectory(String solrIndexName,
                                          String indexArchiveName,
                                          java.util.Properties properties) throws IllegalArgumentException,
                                                                          IOException {
        if(!ConfigUtils.isValidSolrIndexFileName(indexArchiveName)){
            log.debug("add SolrIndexFileExtension to parsed indexArchive {}",indexArchiveName);
            indexArchiveName = ConfigUtils.appandSolrIndexFileExtension(indexArchiveName, null);
        }
        // now add the index to the list of uninitialised
        addUninitialisedIndex(solrIndexName, indexArchiveName, properties);
        return initSolrDirectory(solrIndexName, null, componentContext);
    }

    /**
     * Tries to get the {@link ArchiveInputStream} for the index from the {@link DataFileProvider}.
     * 
     * @param symbolicName
     *            the symbolic name of this bundle or <code>null</code> if
     *            not available (e.g. when running outside OSGI
     * @param solrIndexName
     *            the name of the index to initialise
     * @param properties
     *            the properties for this index. Must contain the
     *            {@link #UNINITIALISED_INDEX_ARCHIVE_NAME_KEY}.
     * @return The {@link ArchiveInputStream} or <code>null</code> if the data are still not available
     * @throws IOException
     *             on any IO related error while initialising the index
     * @throws IllegalStateException
     *             if the parsed configuration does not provide a value for
     *             {@link #UNINITIALISED_INDEX_ARCHIVE_NAME_KEY}.
     */
    private ArchiveInputStream lookupIndexArchive(String symbolicName,
                                                  String solrIndexName,
                                                  java.util.Properties properties) throws IOException,
                                                                                  IllegalStateException {
        // we need to copy the properties to a map
        Map<String,String> propMap;
        if (properties == null) {
            properties = new java.util.Properties(); // create an empty properties file
            propMap = null;
        } else {
            propMap = new HashMap<String,String>();
            for (Entry<Object,Object> entry : properties.entrySet()) {
                propMap.put(entry.getKey().toString(), entry.getValue() != null ? entry.getValue().toString()
                        : null);
            }
        }
        String archiveName = properties.getProperty(UNINITIALISED_INDEX_ARCHIVE_NAME_KEY);
        if (archiveName == null) {
            throw new IllegalStateException(
                    "Found uninitialised index config that does not contain the required "
                            + UNINITIALISED_INDEX_ARCHIVE_NAME_KEY + " property!");
        }
        propMap.remove(UNINITIALISED_INDEX_ARCHIVE_NAME_KEY);// do not parse this internal property
        InputStream is = getDataFileProvider().getInputStream(symbolicName, archiveName, propMap);
        return is == null ? null : ConfigUtils.getArchiveInputStream(archiveName, is);
    }

    private void addUninitialisedIndex(String indexName, String sourceFileName, java.util.Properties config) throws IOException {
        ComponentContext context = componentContext;
        if (config == null) {
            config = new java.util.Properties();
        }
        config.setProperty(UNINITIALISED_INDEX_ARCHIVE_NAME_KEY, sourceFileName);
        synchronized (uninitialisedCores) {
            if (uninitialisedCores.put(indexName, config) != null) {
                removeUninitialisedIndexConfig(context, indexName); // remove the old version
            }
            saveUninitialisedIndexConfig(context, indexName, config); // save the new version
        }
    }

    private void removeUninitialisedIndex(String indexName) {
        ComponentContext context = componentContext;
        synchronized (uninitialisedCores) {
            if (uninitialisedCores.remove(indexName) != null) {
                removeUninitialisedIndexConfig(context, indexName); // remove the old version
            }
        }
    }

    /**
     * Internally used to get/init the Solr directory of a SolrCore or the root Solr directory (if
     * <code>null</code> is parsed)
     * 
     * @param solrIndexName
     *            the name of the Core or <code>null</code> to get/init the root solr directory
     * @param ais
     *            The Input stream of the Archive to load the index from or <code>null</code> to load the
     *            default core configuration.
     * @param context
     *            A reference to the component context or <code>null</code> if running outside an OSGI
     *            container. This is needed to avoid that {@link #deactivate(ComponentContext)} sets the
     *            context to <code>null</code> during this method does its initialisation work.
     * @return the Solr directory or <code>null</code> if the requested index could not be created (e.g.
     *         because of <code>false</code> was parsed as create) or in case this component is deactivated
     * @throws IllegalStateException
     *             in case this method is called when this component is running within an OSGI environment and
     *             it is deactivated or the initialisation for the parsed index failed.
     * @throws IllegalArgumentException
     *             if the parsed solrIndexName is <code>null</code> or empty.
     */
    private final File initSolrDirectory(final String solrIndexName,
                                         ArchiveInputStream ais,
                                         ComponentContext context) throws IllegalStateException {
        if (solrIndexName == null) {
            throw new IllegalArgumentException("The parsed name of the Solr index MUST NOT be NULL");
        }
        if (solrIndexName.isEmpty()) {
            throw new IllegalArgumentException("The parsed name of the Solr index MUST NOT be empty");
        }
        /*
         * The name of the index used for the configuration may differ from the
         * name of the target index.
         * Therefore hold this name in an own Var and init it with the target
         * index name (as default)
         */
        String indexConfigurationName = solrIndexName;
        File managedCoreContainerDirectory = lookupManagedSolrDir(context);
        File coreDir = new File(managedCoreContainerDirectory, solrIndexName);
        if (!coreDir.exists()) {
            // first add the index to the list of currently init cores
            synchronized (initCores) {
                log.debug(" > start initializing SolrIndex {}" + solrIndexName);
                initCores.add(solrIndexName);
            }
            // second check if the Index is an uninitialised one and if this is the case
            // try to get the ArchiveInputStream form the DataFileProvider
            java.util.Properties uninitialisedProperties;
            synchronized (uninitialisedCores) {
                uninitialisedProperties = uninitialisedCores.get(solrIndexName);
                if (uninitialisedProperties != null) {
                    // NOTE: this may override an parsed ArchiveInputStream
                    // -> this is an error by the implementation of this class
                    // so throw an Exception to detect such errors early!
                    if (ais != null) {
                        throw new IllegalStateException(
                                "The parsed ArchiveInputStream is not null for an uninitialised Index. "
                                        + "Please report this error the the stanbol-def mailing list!");
                    }
                    try {
                        String symbolicName = context != null ? context.getBundleContext().getBundle()
                                .getSymbolicName() :
                                    null;
                        ais = lookupIndexArchive(symbolicName, solrIndexName, uninitialisedProperties);
                        //we need to parse the name of the source index
                        String indexArchiveName = uninitialisedProperties.getProperty(UNINITIALISED_INDEX_ARCHIVE_NAME_KEY);
                        if(indexArchiveName != null){
                            //the name of the Archive (eluding extensions) MUST
                            //BE the same as the name of the folder within the
                            //archive that holds the data.
                            indexConfigurationName = indexArchiveName.substring(0, 
                                indexArchiveName.indexOf('.'));
                        }
                    } catch (Exception e) {
                        log.warn(
                            "The Index Archive for index {} not available (see \"Stanbol Data File Provider\" Tab of the Apache Webconsole for details).",
                            solrIndexName);
                    }
                }
            }
            // third do the actual initialisation work
            try {
                if (ais != null) {
                    ConfigUtils.copyCore(ais, coreDir, indexConfigurationName, false);
                    // try to remove from uninitialised
                    removeUninitialisedIndex(solrIndexName);
                }
            } catch (Exception e) {
                throw new IllegalStateException(String.format(
                    "Unable to copy default configuration for Solr Index %s to the configured path %s",
                    solrIndexName == null ? "" : solrIndexName,
                    managedCoreContainerDirectory.getAbsoluteFile()), e);
            } finally {
                // regardless what happened remove the index from the currently init
                // indexes and notify all other waiting for the initialisation
                synchronized (initCores) {
                    // initialisation done
                    initCores.remove(solrIndexName);
                    log.debug("   ... notify after trying to init SolrIndex {}" + solrIndexName);
                    // notify that the initialisation completed or failed
                    initCores.notifyAll();
                }
            }
        } else { // the dir exists
            // check if still initialising ... and wait until the initialisation
            // is complete
            synchronized (initCores) {
                while (initCores.contains(solrIndexName)) {
                    log.info(" > wait for initialisation of SolrIndex " + solrIndexName);
                    try {
                        initCores.wait();
                    } catch (InterruptedException e) {
                        // a core is initialised ... back to work
                    }
                }
            }
        }
        if(coreDir.isDirectory()){
            return coreDir;
        } else {
            return null; //not initialised
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.stanbol.entityhub.yard.solr.impl.ManagedSolrDirectory#getManagedSolrDir()
     */
    @Override
    public File getManagedDirectory() {
        return lookupManagedSolrDir(componentContext);
    }

    /**
     * Lookup the location of the managed Solr directory. Also initialised the default configuration if the
     * directory does not yet exist.
     * 
     * @param context
     *            A reference to the component context or <code>null</code> if running outside an OSGI
     *            container. This is needed to avoid that {@link #deactivate(ComponentContext)} sets the
     *            context to <code>null</code> during this method does its initialisation work.
     * @return the directory based on the current configuration
     * @throws IllegalStateException
     *             in case this method is called when this component is running within an OSGI environment and
     *             it is deactivated.
     */
    private File lookupManagedSolrDir(ComponentContext context) throws IllegalStateException {
        if (managedSolrDir == null) {
            String configuredDataDir;
            if (context == null) { // load via system properties
                configuredDataDir = System.getProperty(MANAGED_SOLR_DIR_PROPERTY, DEFAULT_SOLR_DATA_DIR);
            } else { // load via OSGI config
                Object value = context.getProperties().get(MANAGED_SOLR_DIR_PROPERTY);
                if (value != null) {
                    configuredDataDir = value.toString();
                } else {
                    configuredDataDir = DEFAULT_SOLR_DATA_DIR;
                }
            }
            // property substitution
            configuredDataDir = substituteProperty(configuredDataDir,
                context != null ? context.getBundleContext() : null);
            // determine the directory holding the SolrIndex
            /*
             * NOTE: In case the configuredDataDir.isAbsolute()==false this code will initialise the index
             * relative to the "user.dir" of the application.
             */
            if (withinOSGI && context == null) {
                // ensure to do not set an solrDataDir if this component is
                // running within an OSGI environment and is deactivated
                throw new IllegalStateException(
                        "Unable to lookup managed Solr directories when component is deactivated!");
            } else { // set the the absolute path
                managedSolrDir = new File(configuredDataDir).getAbsoluteFile();
            }
            // check if the "solr.xml" file exists in the directory
            File solrConf = new File(managedSolrDir, "solr.xml");
            if (!solrConf.exists()) {
                try {
                    if (context != null) { // load via bundle
                        managedSolrDir = ConfigUtils.copyDefaultConfig(context.getBundleContext().getBundle(),
                            managedSolrDir, false);
                    } else { // load from jar
                        managedSolrDir = ConfigUtils.copyDefaultConfig((Class<?>) null, managedSolrDir, false);
                    }
                } catch (IOException e) {
                    throw new IllegalStateException(
                            String.format(
                                "Unable to copy default configuration for the manages Solr Directory to the configured path '%s'!",
                                managedSolrDir.getAbsoluteFile()), e);
                }
            }
        }
        return managedSolrDir;
    }

    /*
     * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - Methods for storing and
     * loading configurations for uninitialised indexes - - - - - - - - - - - - - - - - - - - - - - - - - - -
     * - - - - - - - - - -
     */

    /**
     * The path to the directory used to store properties of uninitialised referenced sites.
     * <p>
     * Such sites are one that are created by using {@link #createSolrDirectory(String, String, Map)} but the
     * {@link DataFileProvider} does not yet provide the necessary data to initialise the index.
     * <p>
     * This directory will store properties files with the indexName as name, properties as extension and the
     * properties as value
     */
    private final String UNINITIALISED_INDEX_DIRECTORY = ".config/uninitialised-index";

    /**
     * Saves the configuration of an uninitialised index
     * 
     * @param context
     *            the context used to get the data storage
     * @param indexName
     *            the name of the uninitialised index
     * @param properties
     *            the properties of the uninitialised index
     * @throws IOException
     *             on any error while saving the configuration
     */
    private void saveUninitialisedIndexConfig(ComponentContext context,
                                                    String indexName,
                                                    java.util.Properties properties) throws IOException {
        File uninstalledConfigDir = getUninitialisedSiteDirectory(context, true);
        File config = new File(uninstalledConfigDir, indexName + '.'
                                                     + ConfigUtils.SOLR_INDEX_ARCHIVE_EXTENSION + ".ref");
        FileOutputStream out = null;
        if (properties == null) { // if no config is provided
            properties = new java.util.Properties();// save an empty one
        }
        try {
            out = new FileOutputStream(config);
            properties.store(out, null);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    /**
     * Returns the directory used to store the configurations of uninitialised Solr Indexes
     * 
     * @param init
     *            if <code>true</code> the directory is created if needed
     * @return the directory
     */
    private File getUninitialisedSiteDirectory(ComponentContext context, boolean init) {
        File uninstalledConfigDir;
        if (context == null) { //outside OSGI
            // use config directory relative to the the Managed Solr Directory
            uninstalledConfigDir = new File(lookupManagedSolrDir(context),UNINITIALISED_INDEX_DIRECTORY).getAbsoluteFile();
        } else { //whithin an OSGI environment
            //use the DataFile directory of the bundle
            uninstalledConfigDir = context.getBundleContext().getDataFile(UNINITIALISED_INDEX_DIRECTORY);
        }
        log.info("SolrYard Config Directory: "+uninstalledConfigDir);
        if (!uninstalledConfigDir.exists()) {
            if (init) {
                if (!uninstalledConfigDir.mkdirs()) {
                    throw new IllegalStateException("Unable to create Directory "
                                                    + UNINITIALISED_INDEX_DIRECTORY
                                                    + "for storing information of uninitialised Solr Indexes");
                }
            }
        } else if (!uninstalledConfigDir.isDirectory()) {
            throw new IllegalStateException("The directory " + UNINITIALISED_INDEX_DIRECTORY
                                            + "for storing uninitialised Solr Indexes Information exists"
                                            + "but is not a directory!");
        } // else -> it exists and is a dir -> nothing todo
        return uninstalledConfigDir;
    }

    /**
     * Loads the configurations of uninitialised Solr Indexes
     * 
     * @return the map with the index name as key and the properties as values
     * @throws IOException
     *             on any error while loading the configurations
     */
    private Map<String,java.util.Properties> loadUninitialisedIndexConfigs(ComponentContext context) throws IOException {
        File uninstalledConfigDir = getUninitialisedSiteDirectory(context, false);
        Map<String,java.util.Properties> configs = new HashMap<String,java.util.Properties>();
        if (uninstalledConfigDir.exists()) {
            for (String file : uninstalledConfigDir.list(new SuffixFileFilter(
                    ConfigUtils.SOLR_INDEX_ARCHIVE_EXTENSION + ".ref"))) {
                String indexName = file.substring(0, file.indexOf('.'));
                java.util.Properties props = new java.util.Properties();
                InputStream is = null;
                try {
                    is = new FileInputStream(new File(uninstalledConfigDir, file));
                    props.load(is);
                    configs.put(indexName, props);
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
        }
        return configs;
    }

    /**
     * Removes the configuration for the index with the parsed name form the list if uninitialised indexes
     * 
     * @param indexName
     *            the name of the index
     * @return if the file was deleted.
     */
    private boolean removeUninitialisedIndexConfig(ComponentContext context, String indexName) {
        File configFile = new File(getUninitialisedSiteDirectory(context, false),
                indexName + '.' + ConfigUtils.SOLR_INDEX_ARCHIVE_EXTENSION + ".ref");
        return configFile.delete();
    }
    
    /**
     * Substitutes ${property.name} with the values retrieved via {@link System#getProperty(String, String)}.
     * An empty string is used as default
     * <p>
     * Nested substitutions are NOTE supported. However multiple substitutions are supported.
     * <p>
     * If someone knows a default implementation feel free to replace!
     * 
     * @param value
     *            the value to substitute
     * @param bundleContext
     *            If not <code>null</code> the {@link BundleContext#getProperty(String)} is used instead of
     *            the {@link System#getProperty(String)}. By that it is possible to use OSGI only properties
     *            for substitution.
     * @return the substituted value
     */
    private static String substituteProperty(String value, BundleContext bundleContext) {
        int prevAt = 0;
        int foundAt = 0;
        StringBuilder substitution = new StringBuilder();
        while ((foundAt = value.indexOf("${", prevAt)) >= prevAt) {
            substitution.append(value.substring(prevAt, foundAt));
            String propertyName = value.substring(foundAt + 2, value.indexOf('}', foundAt));
            String propertyValue = bundleContext == null ? // if no bundleContext is available
            System.getProperty(propertyName)
                    : // use the System properties
                    bundleContext.getProperty(propertyName);
            substitution.append(propertyValue == null ? "" : propertyValue);
            prevAt = foundAt + propertyName.length() + 3; // +3 -> "${}".length
        }
        substitution.append(value.substring(prevAt, value.length()));
        return substitution.toString();
    }

    @Activate
    protected void activate(ComponentContext context) throws IOException {
        componentContext = context;
        withinOSGI = true;
        synchronized (uninitialisedCores) {
            uninitialisedCores.putAll(loadUninitialisedIndexConfigs(componentContext));
        }
        // Need our DataFileProvider before building the models
        dfpServiceRegistration = context.getBundleContext().registerService(
                DataFileProvider.class.getName(), 
                new ClassPathSolrIndexConfigProvider(
                    context.getBundleContext().getBundle().getSymbolicName()), null);

    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        if(dfpServiceRegistration != null) {
            dfpServiceRegistration.unregister();
            dfpServiceRegistration = null;
        }
        synchronized (uninitialisedCores) {
            uninitialisedCores.clear();
        }
        componentContext = null;

    }
}

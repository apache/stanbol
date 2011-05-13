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
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
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
     * The dataFileProvider used to lookup index data
     */
    @Reference
    private DataFileProvider dataFileProvider;
    /**
     * The directory used by the internally managed embedded solr server. Use {@link #lookupManagedSolrDir()}
     * instead of using this member, because this member is not initialised within the constructor or the
     * {@link #activate(ComponentContext)} method.
     */
    private File solrDataDir;

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

    public DefaultSolrDirectoryManager() {}

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.stanbol.entityhub.yard.solr.impl.ManagedSolrDirectory#isSolrDir(java.lang.String)
     */
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
    public final Map<String,File> getManagedIndices() throws IllegalStateException {
        File solrDir = lookupManagedSolrDir(componentContext);
        String[] indexNames = solrDir.list(DirectoryFileFilter.INSTANCE);
        Map<String,File> indexes = new HashMap<String,File>();
        for (String indexName : indexNames) {
            // TODO: validate that this is actually a SolrCore!
            indexes.put(indexName, new File(solrDir, indexName));
        }
        // we need also add the uninitialised indexes (with a null as value)
        for (String indexName : uninitialisedCores.keySet()) {
            if (!indexes.containsKey(indexName)) {
                indexes.put(indexName, null);
            }
        }
        return indexes;
    }

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
    public final File getSolrIndexDirectory(final String solrIndexName, boolean allowDefaultInit) throws IllegalArgumentException {
        return initSolrDirectory(solrIndexName, null, allowDefaultInit, componentContext);
    }

    public final File createSolrIndex(final String solrIndexName, ArchiveInputStream ais) {
        return initSolrDirectory(solrIndexName, ais, false, componentContext);
    }

    @Override
    public final File createSolrDirectory(String solrIndexName,
                                          String indexArchiveRef,
                                          java.util.Properties properties) throws IllegalArgumentException,
                                                                          IOException {
        if (componentContext == null) {
            throw new IllegalStateException(
                    "Creating an Index by using the DataFileProvider does only work when running within an OSGI");
        }
        // now add the index to the list of uninitialised
        addUninitialisedIndex(solrIndexName, indexArchiveRef, properties);
        return initSolrDirectory(solrIndexName, null, false, componentContext);
    }

    /**
     * Tries to get the {@link ArchiveInputStream} for the index from the {@link DataFileProvider}.
     * 
     * @param context
     *            the context used to perform the operations
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
    private ArchiveInputStream lookupIndexArchive(ComponentContext context,
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
        InputStream is = dataFileProvider.getInputStream(context.getBundleContext().getBundle()
                .getSymbolicName(), archiveName, propMap);
        return is == null ? null : ConfigUtils.getArchiveInputStream(archiveName, is);
    }

    private void addUninitialisedIndex(String indexName, String sourceFileName, java.util.Properties config) throws IOException {
        ComponentContext context = componentContext;
        if (context == null) {
            throw new IllegalStateException(
                    "This feature is only available when running within an OSGI environment");
        }
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
                if (context == null) {
                    // check only for the context if we need actually to remove
                    // an entry, because this method is also called outside an
                    // OSGI environment (but will never remove something from
                    // uninitialisedCores)
                    throw new IllegalStateException(
                            "This feature is only available when running within an OSGI environment");
                }
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
     * @param allowDefaultInitialisation
     *            If <code>true</code> a new core is initialised with the default configuration (empty index
     *            with the default Solr schema and configuration). If <code>false</code> the core is only
     *            created if a valid configuration is parsed.
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
                                         boolean allowDefaultInitialisation,
                                         ComponentContext context) throws IllegalStateException {
        if (solrIndexName == null) {
            throw new IllegalArgumentException("The parsed name of the Solr index MUST NOT be NULL");
        }
        if (solrIndexName.isEmpty()) {
            throw new IllegalArgumentException("The parsed name of the Solr index MUST NOT be empty");
        }
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
                        ais = lookupIndexArchive(context, solrIndexName, uninitialisedProperties);
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
                    ConfigUtils.copyCore(ais, coreDir, solrIndexName, false);
                    // try to remove from uninitialised
                    removeUninitialisedIndex(solrIndexName);
                } else if (allowDefaultInitialisation) {
                    // TODO: Refactor so that the lookup via Bundle and/or jar
                    // file works via an internal implementation of an
                    // FileDataProvider
                    if (context != null) { // load via bundle
                        ConfigUtils.copyCore(context.getBundleContext().getBundle(), coreDir, null, false);
                    } else { // load from jar
                        ConfigUtils.copyCore((Class<?>) null, coreDir, null, false);
                    }
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
        return coreDir;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.stanbol.entityhub.yard.solr.impl.ManagedSolrDirectory#getManagedSolrDir()
     */
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
        if (solrDataDir == null) {
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
                solrDataDir = new File(configuredDataDir);
            }
            // check if the "solr.xml" file exists in the directory
            File solrConf = new File(solrDataDir, "solr.xml");
            if (!solrConf.exists()) {
                try {
                    if (context != null) { // load via bundle
                        solrDataDir = ConfigUtils.copyDefaultConfig(context.getBundleContext().getBundle(),
                            solrDataDir, false);
                    } else { // load from jar
                        solrDataDir = ConfigUtils.copyDefaultConfig((Class<?>) null, solrDataDir, false);
                    }
                } catch (IOException e) {
                    throw new IllegalStateException(
                            String.format(
                                "Unable to copy default configuration for the manages Solr Directory to the configured path %s!",
                                solrDataDir.getAbsoluteFile()), e);
                }
            }
        }
        return solrDataDir;
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
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        synchronized (uninitialisedCores) {
            uninitialisedCores.clear();
        }
        componentContext = null;

    }
}

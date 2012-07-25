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
package org.apache.stanbol.contenthub.index.ldpath;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.semanticindex.index.IndexManagementException;
import org.apache.stanbol.commons.semanticindex.index.SemanticIndex;
import org.apache.stanbol.commons.semanticindex.index.SemanticIndexManager;
import org.apache.stanbol.commons.solr.managed.IndexMetadata;
import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * This class provides facilities to create {@link LDPathSemanticIndex} instances based on given LDPath
 * program other parameters. {@link SemanticIndex}es can be created through both Felix Web console by
 * configuring a new {@link LDPathSemanticIndex} or through the REST/Java API by giving the necessary
 * information to create the index.
 * 
 * @author suat
 * @author anil
 * 
 */
@Component(immediate = true)
@Service(value = LDPathSemanticIndexManager.class)
public class LDPathSemanticIndexManager {

    private final Logger logger = LoggerFactory.getLogger(LDPathSemanticIndexManager.class);

    private final static String INDEX_METADATA_FOLDER_PATH = "LDPathSemanticIndexMetadata";

    private File indexMetadataDirectory;

    private Map<String,Properties> indexMetadataMap = new HashMap<String,Properties>();

    private LDPathUtils ldPathUtils;

    private BundleContext bundleContext;

    @Reference(target = "(org.apache.solr.core.CoreContainer.name=contenthub)")
    private ManagedSolrServer managedSolrServer;

    @Reference
    private SiteManager siteManager;

    @Activate
    public void activator(ComponentContext context) throws IndexManagementException {
        this.bundleContext = context.getBundleContext();
        ldPathUtils = new LDPathUtils(bundleContext.getBundle(), siteManager);

        indexMetadataDirectory = bundleContext.getDataFile(INDEX_METADATA_FOLDER_PATH);

        // if directory for programs does not exist, create it
        if (!indexMetadataDirectory.exists()) {
            if (indexMetadataDirectory.mkdirs()) {
                logger.info("Directory for index metadata created succesfully");
            } else {
                logger.error("Directory for index metadata COULD NOT be created");
                throw new IndexManagementException("Directory : " + indexMetadataDirectory.getAbsolutePath()
                                                   + " cannot be created");
            }
        }

        // load index metadata to memory
        synchronized (indexMetadataMap) {
            File[] metadataList = indexMetadataDirectory.listFiles();
            for (File configFile : metadataList) {
                String pid = configFile.getName().substring(0, configFile.getName().lastIndexOf('.'));
                Properties props = new Properties();
                InputStream is = null;
                try {
                    is = new FileInputStream(configFile);
                    props.load(is);
                    indexMetadataMap.put(pid, props);
                    logger.info("Index metadata has been loaded from the location: {}",
                        configFile.getAbsolutePath());
                } catch (FileNotFoundException e) {
                    logger.error("IndexMetadata file cannot be found");
                    throw new IndexManagementException("IndexMetadata file cannot be found");
                } catch (IOException e) {
                    logger.error("Failed to read from input stream");
                    throw new IndexManagementException("Failed to read from input stream");
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
        }
    }

    /**
     * Creates an {@link LDPathSemanticIndex} instance based on the given name, description and LDPath
     * program. For other parameters that are used to create the index, the following default values are
     * provided:
     * <ul>
     * <li>{@link LDPathSemanticIndex#PROP_INDEX_CONTENT}: {@code true}</li>
     * <li>{@link LDPathSemanticIndex#PROP_BATCH_SIZE}: 10</li>
     * <li>{@link LDPathSemanticIndex#PROP_STORE_CHECK_PERIOD}: 10</li>
     * <li>{@link LDPathSemanticIndex#PROP_SOLR_CHECK_TIME}: 5</li>
     * <li>{@link Constants#SERVICE_RANKING}: 0</li>
     * </ul>
     * 
     * Each created {@link LDPathSemanticIndex} becomes an OSGi component and it can be obtained as a
     * {@link ServiceReference}. It is possible to get indexes through the {@link SemanticIndexManager} or
     * {@link BundleContext}.
     * 
     * @param indexName
     *            name of the index to be created
     * @param indexDescription
     *            description of the index to be created
     * @param ldPathProgram
     *            LDPath program on which the Solr core will created
     * @throws IndexManagementException
     */
    public String createIndex(String indexName, String indexDescription, String ldPathProgram) throws IndexManagementException {
        if (indexName == null || indexName.isEmpty() || ldPathProgram == null || ldPathProgram.isEmpty()) {
            throw new IndexManagementException("Index name and LDPath program cannot be null or empty");
        }

        if (managedSolrServer.isManagedIndex(indexName)) {
            throw new IndexManagementException(String.format("There is already an index with the name: %s",
                indexName));
        }

        Properties props = new Properties();
        props.put(LDPathSemanticIndex.PROP_NAME, indexName);
        props.put(LDPathSemanticIndex.PROP_LD_PATH_PROGRAM, ldPathProgram);
        props.put(LDPathSemanticIndex.PROP_DESCRIPTION, indexDescription);
        return createIndex(props);
    }
    
    /**
     * Creates an {@link LDPathSemanticIndex} instance based on the given index metadata. However, provided
     * {@link Properties} must include the following items.
     * <ul>
     * <li><b>{@link LDPathSemanticIndex#PROP_NAME}</b></li>
     * <li><b>{@link LDPathSemanticIndex#PROP_LD_PATH_PROGRAM}</b></li>
     * </ul>
     * For other parameters that are used to create the index, the following default values are provided:
     * <ul>
     * <li>{@link LDPathSemanticIndex#PROP_INDEX_CONTENT}: {@code true}</li>
     * <li>{@link LDPathSemanticIndex#PROP_BATCH_SIZE}: 10</li>
     * <li>{@link LDPathSemanticIndex#PROP_STORE_CHECK_PERIOD}: 10</li>
     * <li>{@link LDPathSemanticIndex#PROP_SOLR_CHECK_TIME}: 5</li>
     * <li>{@link Constants#SERVICE_RANKING}: 0</li>
     * </ul>
     * An new configuration for the {@link LDPathSemanticIndex} is created by {@link ConfigurationAdmin} of
     * OSGi using the provided index metadata. So, each created {@link LDPathSemanticIndex} becomes an OSGi
     * component and it can be obtained as a {@link ServiceReference}. It is possible to get indexes through
     * the {@link SemanticIndexManager} or {@link BundleContext}.
     * 
     * @param indexMetadata
     *            {@link Properties} containing the possible metadata about the index to be created
     * @throws IndexManagementException
     */
    public String createIndex(Properties indexMetadata) throws IndexManagementException {
        // validate properties
        String indexName = (String) indexMetadata.get(SemanticIndex.PROP_NAME);
        String ldPathProgram = (String) indexMetadata.get(LDPathSemanticIndex.PROP_LD_PATH_PROGRAM);
        String configurationPID = (String) indexMetadata.get(Constants.SERVICE_PID);

        if (indexName == null || indexName.isEmpty() || ldPathProgram == null || ldPathProgram.isEmpty()) {
            throw new IndexManagementException("Index name and LDPath program cannot be null or empty");
        }

        if (managedSolrServer.isManagedIndex(indexName)) {
            throw new IndexManagementException(String.format("There is already an index with the name: %s",
                indexName));
        }

        checkValues(indexMetadata);

        // create solr core
        createSolrCore(indexName, ldPathProgram);
        logger.info("Solr core for the Semantic Index: {} has been created successfully", indexName);

        // activate the OSGI component if it is not already done. This is the case when an LDPathSemanticIndex
        // is created through the REST/Java API
        if (configurationPID == null) {
            ServiceReference reference = bundleContext
                    .getServiceReference(ConfigurationAdmin.class.getName());
            ConfigurationAdmin configAdmin = (ConfigurationAdmin) bundleContext.getService(reference);
            Configuration config;
            try {
                config = configAdmin.createFactoryConfiguration(LDPathSemanticIndex.class.getName(), null);
                configurationPID = config.getPid();

                storeIndexMetadata(configurationPID, indexMetadata);
                config.update(indexMetadata);
                logger.info(
                    "A new configuration has been created for the Semantic Index: {} and its metadata was stored",
                    indexName);
            } catch (IOException e) {
                logger.error("Failed to create Factory Configuration for LDPathSemanticIndex: {}", indexName);
                throw new IndexManagementException(String.format(
                    "Failed to create Factory Configuration for LDPathSemanticIndex: %s", indexName), e);
            }

            // the index created through the Felix Web console
        } else {
            storeIndexMetadata(configurationPID, indexMetadata);
            logger.info(
                "A configuration has already been created for the Semantic Index: {}, so only its metadata was stored",
                indexName);
        }
        return configurationPID;
    }

    private void checkValues(Properties indexMetadata) {
        if (indexMetadata.get(LDPathSemanticIndex.PROP_DESCRIPTION) == null) {
            indexMetadata.put(LDPathSemanticIndex.PROP_DESCRIPTION, "");
        }
        if (indexMetadata.get(LDPathSemanticIndex.PROP_INDEX_CONTENT) == null) {
            indexMetadata.put(LDPathSemanticIndex.PROP_INDEX_CONTENT, true);
        }
        if (indexMetadata.get(LDPathSemanticIndex.PROP_BATCH_SIZE) == null) {
            indexMetadata.put(LDPathSemanticIndex.PROP_BATCH_SIZE, 10);
        }
        if (indexMetadata.get(LDPathSemanticIndex.PROP_STORE_CHECK_PERIOD) == null) {
            indexMetadata.put(LDPathSemanticIndex.PROP_STORE_CHECK_PERIOD, 10);
        }
        if (indexMetadata.get(LDPathSemanticIndex.PROP_SOLR_CHECK_TIME) == null) {
            indexMetadata.put(LDPathSemanticIndex.PROP_SOLR_CHECK_TIME, 5);
        }
        if (indexMetadata.get(Constants.SERVICE_RANKING) == null) {
            indexMetadata.put(Constants.SERVICE_RANKING, 0);
        }
    }

    /**
     * Create a Solr core using the given {@code coreName} and {@code ldPathProgram}. A solr schema
     * configuration archive is formed based on the given parameters through the
     * {@link LDPathUtils#createSchemaArchive(String, String)} and this archive is used to create a Solr core
     * by {@link ManagedSolrServer}.
     * 
     * @param coreName
     *            Name of the Solr core to be created
     * @param ldPathProgram
     *            LDPath program to adjust the Solr core configuration
     * @return the {@link IndexMetadata} of the created Solr core
     * @throws IndexManagementException
     */
    public IndexMetadata createSolrCore(String coreName, String ldPathProgram) throws IndexManagementException {
        ArchiveInputStream coreArchive = ldPathUtils.createSchemaArchive(coreName, ldPathProgram);
        try {
            return managedSolrServer.createSolrIndex(coreName, coreArchive);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new IndexManagementException(e.getMessage(), e);
        } catch (SAXException e) {
            String msg = "ManagedSolrServer cannot parse the related XML files.";
            logger.error(msg, e);
            throw new IndexManagementException(msg, e);
        }
    }

    /**
     * Returns the metadata of the index specified with the given persistent identifier (pid)
     * 
     * @param pid
     * @return
     */
    public Properties getIndexMetadata(String pid) {
        synchronized (indexMetadataMap) {
            return indexMetadataMap.get(pid);
        }
    }

    /**
     * Updates the metadata of the index specified with the given persistent identifier (pid)
     * 
     * @param pid
     * @param indexMetadata
     * @throws IndexManagementException
     * @throws IOException
     */
    public void updateIndexMetadata(String pid, Properties indexMetadata) throws IndexManagementException {
        synchronized (indexMetadataMap) {
            storeIndexMetadata(pid, indexMetadata);
        }
    }

    /**
     * Returns the {@link Map} containing metadata of the {@link LDPathSemanticIndex} instances.
     * 
     * @return
     */
    public Map<String,Properties> getAllIndexMetadata() {
        synchronized (indexMetadataMap) {
            return indexMetadataMap;
        }
    }

    /**
     * Returns whether a Solr core for the given {@code pid} is already configured.
     * 
     * @param pid
     *            persistent identifier(pid) of the factory configuration of {@link LDPathSemanticIndex}
     * @return
     */
    public boolean isConfigured(String pid) {
        synchronized (indexMetadataMap) {
            return indexMetadataMap.containsKey(pid);
        }
    }

    /**
     * Remove the underlying Solr core and index metadata associated with the {@code pid}
     * 
     * @param pid
     *            persistent identifier (pid) of the factory configuration of {@link LDPathSemanticIndex}
     * @throws IndexManagementException
     */
    public void removeIndex(String pid) throws IndexManagementException {

        Properties indexMetadata = removeIndexMetadata(pid);
        managedSolrServer.removeIndex(indexMetadata.getProperty(SemanticIndex.PROP_NAME), true);
        ServiceReference reference = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
        ConfigurationAdmin configAdmin = (ConfigurationAdmin) bundleContext.getService(reference);
        Configuration config;
        String indexName = (String) indexMetadata.get(SemanticIndex.PROP_NAME);
        try {
            config = configAdmin.getConfiguration(pid);
            config.delete();
            logger.info("Semantic Index: {} was removed successfully", indexName);
        } catch (IOException e) {
            throw new IndexManagementException(String.format(
                "Failed to remove configuration for the Semantic Index: %s", indexName));
        }
    }

    private Properties removeIndexMetadata(String pid) throws IndexManagementException {
        String indexMetadataFilePath = indexMetadataDirectory.getAbsolutePath() + File.separator + pid
                                       + ".props";
        File file = new File(indexMetadataFilePath);
        logger.info("Index metadata file to be deleted: {}", file.getAbsolutePath());
        if (file.exists()) {
            file.delete();
        } else {
            logger.error("Failed to delete IndexMetadata file");
            throw new IndexManagementException("Failed to delete IndexMetadata file");
        }

        Properties indexMetadata;
        synchronized (indexMetadataMap) {
            indexMetadata = indexMetadataMap.remove(pid);
        }

        return indexMetadata;
    }

    private void storeIndexMetadata(String pid, Properties indexMetadata) throws IndexManagementException {
        String indexMetadataFilePath = indexMetadataDirectory.getAbsolutePath() + File.separator + pid
                                       + ".props";
        synchronized (indexMetadataMap) {
            FileOutputStream out = null;
            Properties stringValues = getStringValues(indexMetadata);
            try {
                out = new FileOutputStream(indexMetadataFilePath);
                stringValues.store(out, null);
            } catch (IOException e) {
                logger.error("Failed to write indexMetadataFilePath to the specified output stream");
                throw new IndexManagementException(
                        "Failed to write indexMetadataFilePath to the specified output stream");
            } finally {
                IOUtils.closeQuietly(out);
            }
            indexMetadataMap.put(pid, stringValues);
        }
    }

    private Properties getStringValues(Properties indexMetadata) {
        Properties properties = new Properties();
        for (Entry<Object,Object> property : indexMetadata.entrySet()) {
            Object value = property.getValue();
            if (value instanceof String) {
                properties.put(property.getKey(), value);
            } else {
                properties.put(property.getKey(), value.toString());
            }
        }
        return properties;
    }
}

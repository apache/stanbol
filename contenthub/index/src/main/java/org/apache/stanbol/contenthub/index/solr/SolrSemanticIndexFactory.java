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
package org.apache.stanbol.contenthub.index.solr;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.semanticindex.index.IndexManagementException;
import org.apache.stanbol.commons.semanticindex.index.SemanticIndex;
import org.apache.stanbol.commons.semanticindex.index.SemanticIndexManager;
import org.apache.stanbol.commons.solr.managed.IndexMetadata;
import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.apache.stanbol.contenthub.index.AbstractLDPathSemanticIndex;
import org.apache.stanbol.contenthub.index.AbstractLDPathSemanticIndexFactory;
import org.apache.stanbol.contenthub.index.SemanticIndexMetadataManager;
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
 * This class provides facilities to create {@link SolrSemanticIndex} instances based on given LDPath program
 * other parameters. {@link SemanticIndex}es can be created through both Felix Web console by configuring a
 * new {@link SolrSemanticIndex} or through the REST/Java API by giving the necessary information to create
 * the index.
 * 
 * @author suat
 * @author anil
 * 
 */
@Component(immediate = true)
@Service(value = SolrSemanticIndexFactory.class)
public class SolrSemanticIndexFactory extends AbstractLDPathSemanticIndexFactory {

    private final Logger logger = LoggerFactory.getLogger(SolrSemanticIndexFactory.class);

    private LDPathUtils ldPathUtils;

    private BundleContext bundleContext;

    @Reference(target = "(org.apache.solr.core.CoreContainer.name=contenthubSolrSemanticIndex)")
    private ManagedSolrServer managedSolrServer;

    @Reference
    private SiteManager siteManager;

    @Activate
    public void activator(ComponentContext context) throws IndexManagementException {
        this.bundleContext = context.getBundleContext();
        ldPathUtils = new LDPathUtils(bundleContext.getBundle(), siteManager);
        File indexMetadataDirectory = bundleContext.getDataFile(SolrSemanticIndexFactory.class.getName());
        this.semanticIndexMetadataManager = new SemanticIndexMetadataManager(indexMetadataDirectory);
    }

    /**
     * <p>
     * This method creates a Solr based {@link SemanticIndex} using the given parameters. More specifically,
     * {@code indexName} is used as the name of the Solr core and the configuration of the Solr core is
     * adjusted according to the given {@code ldPathProgram}. See
     * {@link LDPathUtils#createSchemaArchive(String, String)} method to investigate how the Solr schema
     * configuration is created.
     * </p>
     * Other than the given parameters the following parameters are used to create the index with the
     * presented default values:
     * <ul>
     * <li>{@link AbstractLDPathSemanticIndex#PROP_INDEX_CONTENT}: {@code true}</li>
     * <li>{@link AbstractLDPathSemanticIndex#PROP_BATCH_SIZE}: 10</li>
     * <li>{@link AbstractLDPathSemanticIndex#PROP_INDEXING_SOURCE_NAME}: contenthubFileStore</li>
     * <li>{@link AbstractLDPathSemanticIndex#PROP_INDEXING_SOURCE_CHECK_PERIOD}: 10</li>
     * <li>{@link AbstractLDPathSemanticIndexFactory#PROP_SOLR_CHECK_TIME}: 5</li>
     * <li>{@link Constants#SERVICE_RANKING}: 0</li>
     * </ul>
     * 
     * Each created {@link SemanticIndex} becomes an OSGi component and it can be obtained as a
     * {@link ServiceReference}. It is possible to get indexes through the {@link SemanticIndexManager} or
     * {@link BundleContext}.
     */
    @Override
    public String createIndex(String indexName, String indexDescription, String ldPathProgram) throws IndexManagementException {
        if (indexName == null || indexName.isEmpty() || ldPathProgram == null || ldPathProgram.isEmpty()) {
            throw new IndexManagementException("Index name and LDPath program cannot be null or empty");
        }

        if (managedSolrServer.isManagedIndex(indexName)) {
            throw new IndexManagementException(String.format("There is already an index with the name: %s",
                indexName));
        }

        Properties props = new Properties();
        props.put(SolrSemanticIndex.PROP_NAME, indexName);
        props.put(SolrSemanticIndex.PROP_LD_PATH_PROGRAM, ldPathProgram);
        props.put(SolrSemanticIndex.PROP_DESCRIPTION, indexDescription);
        return createIndex(props);
    }

    /**
     * <p>
     * This method creates a Solr based {@link SemanticIndex} using the parameters passed in the given
     * {@code indexMetadata}. As specified in the
     * {@link AbstractLDPathSemanticIndexFactory#createIndex(Properties)}, the following parameters are
     * mandatory.
     * <ul>
     * <li>{@link AbstractLDPathSemanticIndex#PROP_LD_PATH_PROGRAM}</li>
     * <li>{@link SemanticIndex#PROP_NAME}</li>
     * </ul>
     * {@link SemanticIndex#PROP_NAME} is used as the name of the Solr core and the configuration of the Solr
     * core is adjusted according to the given {@link AbstractLDPathSemanticIndex#PROP_LD_PATH_PROGRAM}. See
     * {@link LDPathUtils#createSchemaArchive(String, String)} method to investigate how the Solr schema
     * configuration is created.
     * </p>
     * <p>
     * If they are not already specified the following parameters are used to create the index with the
     * presented default values. Different values for these parameters can also be passed in the
     * {@code indexMetadata}:
     * </p>
     * <ul>
     * <li>{@link AbstractLDPathSemanticIndex#PROP_INDEX_CONTENT}: {@code true}</li>
     * <li>{@link AbstractLDPathSemanticIndex#PROP_BATCH_SIZE}: 10</li>
     * <li>{@link AbstractLDPathSemanticIndex#PROP_INDEXING_SOURCE_NAME}: contenthubFileStore</li>
     * <li>{@link AbstractLDPathSemanticIndex#PROP_INDEXING_SOURCE_CHECK_PERIOD}: 10</li>
     * <li>{@link AbstractLDPathSemanticIndexFactory#PROP_SOLR_CHECK_TIME}: 5</li>
     * <li>{@link Constants#SERVICE_RANKING}: 0</li>
     * </ul>
     * 
     * Each created {@link SemanticIndex} becomes an OSGi component and it can be obtained as a
     * {@link ServiceReference}. It is possible to get indexes through the {@link SemanticIndexManager} or
     * {@link BundleContext}.
     */
    @Override
    public String createIndex(Properties indexMetadata) throws IndexManagementException {
        // validate properties
        String indexName = (String) indexMetadata.get(SemanticIndex.PROP_NAME);
        String ldPathProgram = (String) indexMetadata.get(SolrSemanticIndex.PROP_LD_PATH_PROGRAM);
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

        // activate the OSGI component if it is not already done. This is the case when an SolrSemanticIndex
        // is created through the REST/Java API
        if (configurationPID == null) {
            ServiceReference reference = bundleContext
                    .getServiceReference(ConfigurationAdmin.class.getName());
            ConfigurationAdmin configAdmin = (ConfigurationAdmin) bundleContext.getService(reference);
            Configuration config;
            try {
                config = configAdmin.createFactoryConfiguration(SolrSemanticIndex.class.getName(), null);
                configurationPID = config.getPid();

                semanticIndexMetadataManager.storeIndexMetadata(configurationPID, indexMetadata);
                config.update(indexMetadata);
                logger.info(
                    "A new configuration has been created for the Semantic Index: {} and its metadata was stored",
                    indexName);
            } catch (IOException e) {
                logger.error("Failed to create Factory Configuration for SolrSemanticIndex: {}", indexName);
                throw new IndexManagementException(String.format(
                    "Failed to create Factory Configuration for SolrSemanticIndex: %s", indexName), e);
            }

            // the index created through the Felix Web console
        } else {
            semanticIndexMetadataManager.storeIndexMetadata(configurationPID, indexMetadata);
            logger.info(
                "A configuration has already been created for the Semantic Index: {}, so only its metadata was stored",
                indexName);
        }
        return configurationPID;
    }

    private void checkValues(Properties indexMetadata) {
        if (indexMetadata.get(SolrSemanticIndex.PROP_DESCRIPTION) == null) {
            indexMetadata.put(SolrSemanticIndex.PROP_DESCRIPTION, "");
        }
        if (indexMetadata.get(SolrSemanticIndex.PROP_INDEX_CONTENT) == null) {
            indexMetadata.put(SolrSemanticIndex.PROP_INDEX_CONTENT, true);
        }
        if (indexMetadata.get(SolrSemanticIndex.PROP_BATCH_SIZE) == null) {
            indexMetadata.put(SolrSemanticIndex.PROP_BATCH_SIZE, 10);
        }
        if (indexMetadata.get(SolrSemanticIndex.PROP_INDEXING_SOURCE_NAME) == null) {
            indexMetadata.put(SolrSemanticIndex.PROP_INDEXING_SOURCE_NAME, "contenthubFileStore");
        }
        if (indexMetadata.get(SolrSemanticIndex.PROP_INDEXING_SOURCE_CHECK_PERIOD) == null) {
            indexMetadata.put(SolrSemanticIndex.PROP_INDEXING_SOURCE_CHECK_PERIOD, 10);
        }
        if (indexMetadata.get(SolrSemanticIndex.PROP_SOLR_CHECK_TIME) == null) {
            indexMetadata.put(SolrSemanticIndex.PROP_SOLR_CHECK_TIME, 5);
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

    @Override
    public void removeIndex(String pid) throws IndexManagementException {

        Properties indexMetadata = semanticIndexMetadataManager.removeIndexMetadata(pid);
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
                "Failed to remove configuration for the Semantic Index: %s", indexName), e);
        }
    }
}

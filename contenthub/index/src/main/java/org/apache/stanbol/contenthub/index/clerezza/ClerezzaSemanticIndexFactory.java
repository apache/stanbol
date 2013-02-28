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
package org.apache.stanbol.contenthub.index.clerezza;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.NoSuchEntityException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.semanticindex.index.IndexManagementException;
import org.apache.stanbol.commons.semanticindex.index.SemanticIndex;
import org.apache.stanbol.contenthub.index.AbstractLDPathSemanticIndex;
import org.apache.stanbol.contenthub.index.AbstractLDPathSemanticIndexFactory;
import org.apache.stanbol.contenthub.index.AbstractSemanticIndex;
import org.apache.stanbol.contenthub.index.SemanticIndexMetadataManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides facilities to create {@link ClerezzaSemanticIndex} instances based on given LDPath
 * program other parameters. {@link SemanticIndex}es can be created through both Felix Web console by
 * configuring a new {@link ClerezzaSemanticIndex} or through the REST/Java API by giving the necessary
 * information to create the index.
 * 
 * @author suat
 * @author meric
 * 
 */
@Component(immediate = true)
@Service(value = ClerezzaSemanticIndexFactory.class)
public class ClerezzaSemanticIndexFactory extends AbstractLDPathSemanticIndexFactory {

    private final Logger logger = LoggerFactory.getLogger(ClerezzaSemanticIndexFactory.class);

    private BundleContext bundleContext;

    @Reference
    private TcManager tcManager;

    @Activate
    public void activator(ComponentContext context) throws IndexManagementException {
        this.bundleContext = context.getBundleContext();
        File indexMetadataDirectory = bundleContext.getDataFile(ClerezzaSemanticIndexFactory.class.getName());
        this.semanticIndexMetadataManager = new SemanticIndexMetadataManager(indexMetadataDirectory);
    }

    @Override
    public String createIndex(String indexName, String indexDescription, String ldPathProgram) throws IndexManagementException {
        if (indexName == null || indexName.isEmpty() || ldPathProgram == null || ldPathProgram.isEmpty()) {
            throw new IndexManagementException("Index name and LDPath program cannot be null or empty");
        }

        if (isExist(indexName)) {
            throw new IndexManagementException(String.format("There is already an index with the name: %s",
                indexName));
        }

        Properties props = new Properties();
        props.put(SemanticIndex.PROP_NAME, indexName);
        props.put(AbstractLDPathSemanticIndex.PROP_LD_PATH_PROGRAM, ldPathProgram);
        props.put(SemanticIndex.PROP_DESCRIPTION, indexDescription);
        return createIndex(props);
    }

    @Override
    public String createIndex(Properties indexMetadata) throws IndexManagementException {
        // validate properties
        String indexName = (String) indexMetadata.get(SemanticIndex.PROP_NAME);
        String ldPathProgram = (String) indexMetadata.get(AbstractLDPathSemanticIndex.PROP_LD_PATH_PROGRAM);
        String configurationPID = (String) indexMetadata.get(Constants.SERVICE_PID);

        if (indexName == null || indexName.isEmpty() || ldPathProgram == null || ldPathProgram.isEmpty()) {
            throw new IndexManagementException("Index name and LDPath program cannot be null or empty");
        }

        if (isExist(indexName)) {
            throw new IndexManagementException(String.format("There is already an index with the name: %s",
                indexName));
        }

        checkValues(indexMetadata);

        // create triple collection
        tcManager.createMGraph(new UriRef(indexName));
        logger.info("Triple collection for the Semantic Index: {} has been created successfully", indexName);

        // activate the OSGI component if it is not already done. This is the case when an SemanticIndex
        // is created through the REST/Java API
        if (configurationPID == null) {
            ServiceReference reference = bundleContext
                    .getServiceReference(ConfigurationAdmin.class.getName());
            ConfigurationAdmin configAdmin = (ConfigurationAdmin) bundleContext.getService(reference);
            Configuration config;
            try {
                config = configAdmin.createFactoryConfiguration(ClerezzaSemanticIndex.class.getName(), null);
                configurationPID = config.getPid();

                semanticIndexMetadataManager.storeIndexMetadata(configurationPID, indexMetadata);
                config.update(indexMetadata);
                logger.info(
                    "A new configuration has been created for the Semantic Index: {} and its metadata was stored",
                    indexName);
            } catch (IOException e) {
                logger.error("Failed to create Factory Configuration for SemanticIndex: {}", indexName);
                throw new IndexManagementException(String.format(
                    "Failed to create Factory Configuration for SemanticIndex: %s", indexName), e);
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
        if (indexMetadata.get(SemanticIndex.PROP_DESCRIPTION) == null) {
            indexMetadata.put(SemanticIndex.PROP_DESCRIPTION, "");
        }
        if (indexMetadata.get(AbstractSemanticIndex.PROP_BATCH_SIZE) == null) {
            indexMetadata.put(AbstractSemanticIndex.PROP_BATCH_SIZE, 10);
        }
        if (indexMetadata.get(AbstractSemanticIndex.PROP_INDEXING_SOURCE_NAME) == null) {
            indexMetadata.put(AbstractSemanticIndex.PROP_INDEXING_SOURCE_NAME, "contenthubFileStore");
        }
        if (indexMetadata.get(AbstractSemanticIndex.PROP_INDEXING_SOURCE_CHECK_PERIOD) == null) {
            indexMetadata.put(AbstractSemanticIndex.PROP_INDEXING_SOURCE_CHECK_PERIOD, 10);
        }
        if (indexMetadata.get(Constants.SERVICE_RANKING) == null) {
            indexMetadata.put(Constants.SERVICE_RANKING, 0);
        }
    }

    @Override
    public void removeIndex(String pid) throws IndexManagementException {

        Properties indexMetadata = semanticIndexMetadataManager.removeIndexMetadata(pid);
        tcManager.deleteTripleCollection(new UriRef(indexMetadata
                .getProperty(SemanticIndex.PROP_NAME)));
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

    public boolean isExist(String name) throws IndexManagementException {
        try {
            tcManager.getTriples(new UriRef(name));
            return true;
        } catch (NoSuchEntityException e) {
            return false;
        }
    }

}

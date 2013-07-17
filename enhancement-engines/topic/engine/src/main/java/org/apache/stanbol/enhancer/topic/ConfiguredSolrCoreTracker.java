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
package org.apache.stanbol.enhancer.topic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.stanbol.commons.solr.IndexReference;
import org.apache.stanbol.commons.solr.RegisteredSolrServerTracker;
import org.apache.stanbol.commons.solr.managed.IndexMetadata;
import org.apache.stanbol.commons.solr.managed.ManagedIndexState;
import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.apache.stanbol.enhancer.engine.topic.TopicClassificationEngine;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Helper class to factorize some common code for Solr Core tracking OSGi component
 */
public abstract class ConfiguredSolrCoreTracker {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    protected ManagedSolrServer managedSolrServer;

    protected String solrCoreId;

    protected RegisteredSolrServerTracker indexTracker;

    // instance of classifierSolrServer to use if not using the OSGi service tracker (e.g. for tests)
    protected SolrServer solrServer;

    protected ComponentContext context;

    protected String solrCoreConfig;

    //protected String indexArchiveName;

    abstract public void configure(Dictionary<String,Object> config) throws ConfigurationException;

    protected String getRequiredStringParam(Dictionary<String,Object> parameters, String paramName) throws ConfigurationException {
        return getRequiredStringParam(parameters, paramName, null);
    }

    protected String getRequiredStringParam(Dictionary<String,Object> config,
                                            String paramName,
                                            String defaultValue) throws ConfigurationException {
        Object paramValue = config.get(paramName);
        if (paramValue == null) {
            if (defaultValue == null) {
                throw new ConfigurationException(paramName, paramName + " is a required parameter.");
            } else {
                return defaultValue;
            }
        }
        return paramValue.toString();
    }

    @SuppressWarnings("unchecked")
    protected List<String> getStringListParan(Dictionary<String,Object> config, String paramName) throws ConfigurationException {
        Object paramValue = config.get(paramName);
        if (paramValue == null) {
            return new ArrayList<String>();
        } else if (paramValue instanceof String) {
            return Arrays.asList(paramValue.toString().split(",\\s*"));
        } else if (paramValue instanceof String[]) {
            return Arrays.asList((String[]) paramValue);
        } else if (paramValue instanceof List) {
            return (List<String>) paramValue;
        } else {
            throw new ConfigurationException(paramName, String.format(
                "Unexpected parameter type for '%s': %s", paramName, paramValue));
        }
    }

    /**
     * @return the manually bound classifierSolrServer instance or the one tracked by the OSGi service
     *         tracker.
     */
    public SolrServer getActiveSolrServer() {
        SolrServer result;
        if(solrServer != null){
            result = solrServer;
        } else {
            result = indexTracker.getService();
            if(result == null){
                //try to wait for the server (mainly because the evaluation
                //server is created on demand and will need some time to be
                //initialised).
                for(int i = 0; i < 5 && result == null; i++){
                    try {
                        result = (SolrServer) indexTracker.waitForService(1000);
                    } catch (InterruptedException e) {/* ignore */ }
                }
            }
        }
        if (result == null) {
            if (solrCoreId != null) {
                throw new RuntimeException("No Solr Core registered with id: " + solrCoreId);
            } else {
                throw new RuntimeException("No Solr Core registered");
            }
        }
        return result;
    }

    protected void configureSolrCore(Dictionary<String,Object> config,
            String solrCoreProperty, String defaultCoreId,
            String solrCoreConfigProperty) 
                    throws ConfigurationException {
        Object solrCoreInfo = config.get(solrCoreProperty);
        if (solrCoreInfo instanceof SolrServer) {
            // Bind a fixed Solr server client instead of doing dynamic OSGi lookup using the service tracker.
            // This can be useful both for unit-testing .
            solrServer = (SolrServer) config.get(solrCoreProperty);
            solrCoreConfig = TopicClassificationEngine.DEFAULT_SOLR_CORE_CONFIG;
        } else {
            if (context == null) {
                throw new ConfigurationException(solrCoreProperty,
                        solrCoreProperty + " should be a SolrServer instance for using"
                                + " the engine without any OSGi context. Got: " + solrCoreId);
            }
            if (solrCoreInfo != null && !solrCoreInfo.toString().trim().isEmpty()) {
                this.solrCoreId = solrCoreInfo.toString().trim();
            } else {
                this.solrCoreId = defaultCoreId;
            }
            solrCoreConfig = getRequiredStringParam(config, solrCoreConfigProperty, 
                this.solrCoreId + ".solrindex.zip");
            try {
                IndexReference indexReference = IndexReference.parse(solrCoreId);
                //String configName = getRequiredStringParam(config, SOLR_CONFIG, defaultValue)
                indexReference = checkInitSolrIndex(indexReference, solrCoreConfig);
                // track the solr core OSGi updates
                indexTracker = new RegisteredSolrServerTracker(context.getBundleContext(), indexReference);
                indexTracker.open();
            } catch (Exception e) {
                throw new ConfigurationException(solrCoreProperty, e.getMessage(), e);
            }
        }
    }
    /**
     * Checks if the SolrIndex is available and if not it tries to initialise it
     * @param indexReference the SolrCore reference
     * @param solrCoreConfig the name of the SolrIndex configuration ({name}.solrindex.zip)
     * @return
     * @throws IOException
     * @throws ConfigurationException
     * @throws SAXException
     */
    protected IndexReference checkInitSolrIndex(IndexReference indexReference, String solrCoreConfig) 
            throws IOException, ConfigurationException, SAXException {
        // if the solr core is managed, check that the index is properly activated
        if (managedSolrServer != null && indexReference.checkServer(managedSolrServer.getServerName())
            && context != null && solrCoreConfig != null) {
            log.info(" > check/init index {} on ManagedSolrServer {}", indexReference, managedSolrServer.getServerName());
            String indexName = indexReference.getIndex();
            final IndexMetadata indexMetadata;
            ManagedIndexState indexState = managedSolrServer.getIndexState(indexName);
            if(indexState == null){
                if(solrCoreConfig.indexOf(".solrindex.") < 0){ //if the suffix is missing
                    solrCoreConfig = solrCoreConfig + ".solrindex.zip"; //append it
                }
                log.info("Create SolrCore {} (config: {}) on ManagedSolrServer {} ...",
                    new Object[]{indexName,solrCoreConfig,managedSolrServer.getServerName()});
                indexMetadata = managedSolrServer.createSolrIndex(indexName, 
                    solrCoreConfig, null);
                if(indexMetadata != null)
                log.info("  ... created {}", indexMetadata.getIndexReference());
            } else {
                indexMetadata = managedSolrServer.getIndexMetadata(indexName);
                if(indexState != ManagedIndexState.ACTIVE){
                    log.info("  ... activate {}", indexMetadata.getIndexReference());
                    managedSolrServer.activateIndex(indexName);
                } else {
                    log.info("  ... index {} already active", indexMetadata.getIndexReference());
                }
            }
//            IndexMetadata indexMetadata = managedSolrServer.getIndexMetadata(indexName);
//            if (indexMetadata == null) {
//                // TODO: debug the DataFileProvider init race conditions instead
//                // indexMetadata = managedSolrServer.createSolrIndex(indexName, indexArchiveName, null);
//                dfp.getInputStream(context.getBundleContext().getBundle().getSymbolicName(), 
//                    indexArchiveName + ".solrindex.zip", null);
//                URL archiveUrl = context.getBundleContext().getBundle()
//                        .getEntry("/data-files/" + indexArchiveName + ".solrindex.zip");
//                if (archiveUrl == null) {
//                    throw new ConfigurationException(solrCoreId, "Could not find index archive for "
//                                                                 + indexArchiveName);
//                }
//                ZipArchiveInputStream zis = new ZipArchiveInputStream(archiveUrl.openStream());
//                indexMetadata = managedSolrServer.updateIndex(indexName, zis, indexArchiveName);
//            }
//            if (!indexMetadata.isActive()) {
//                managedSolrServer.activateIndex(indexName);
//            }
            indexReference = indexMetadata.getIndexReference();
        }
        return indexReference;
    }

    protected void bindManagedSolrServer(ManagedSolrServer managedSolrServer) throws IOException,
                                                                             SAXException {
        this.managedSolrServer = managedSolrServer;
    }

    protected void unbindManagedSolrServer(ManagedSolrServer managedSolrServer) {
        if (this.managedSolrServer == managedSolrServer || solrCoreId != null) {
            IndexReference indexReference = IndexReference.parse(solrCoreId);
            if (!indexReference.checkServer(managedSolrServer.getServerName())) {
                return;
            }
            String indexName = indexReference.getIndex();
            IndexMetadata indexMetadata = managedSolrServer.getIndexMetadata(indexName);
            if (indexMetadata != null && indexMetadata.isActive()) {
                managedSolrServer.deactivateIndex(indexName);
            }
            this.managedSolrServer = null;
        }
    }
}

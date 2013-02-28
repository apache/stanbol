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
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.NoSuchEntityException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.semanticindex.index.EndpointTypeEnum;
import org.apache.stanbol.commons.semanticindex.index.IndexException;
import org.apache.stanbol.commons.semanticindex.index.IndexManagementException;
import org.apache.stanbol.commons.semanticindex.index.IndexState;
import org.apache.stanbol.commons.semanticindex.index.SemanticIndex;
import org.apache.stanbol.commons.semanticindex.store.ChangeSet;
import org.apache.stanbol.commons.semanticindex.store.Store;
import org.apache.stanbol.commons.semanticindex.store.StoreException;
import org.apache.stanbol.contenthub.index.AbstractSemanticIndex;
import org.apache.stanbol.contenthub.index.SemanticIndexMetadataManager;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clerezza based {@link SemanticIndex} implementation. This implementation creates an {@link MGraph} by
 * collecting enhancements of ContentItems.
 * 
 * @author meric
 */
@Component(immediate = true)
@Service(value = SemanticIndex.class)
public class EnhancementSemanticIndex extends AbstractSemanticIndex {

    private final Logger logger = LoggerFactory.getLogger(EnhancementSemanticIndex.class);

    public static final String PROP_GRAPH_URI = "org.apache.stanbol.contenthub.index.clerezza.EnhancementSemanticIndex.graphURI";

    // Manager to keep track of the metadata regarding the {@link
    // EnhancementSemanticIndex}
    private SemanticIndexMetadataManager semanticIndexMetadataManager;

    @Reference
    private TcManager tcManager;
    
    private ServiceRegistration enhancementGraphRegistry;

    // URI of the graph documents are indexed or removed
    private UriRef graphURI;

    @Activate
    protected void activate(ComponentContext context) throws IndexException,
                                                     IndexManagementException,
                                                     ConfigurationException,
                                                     StoreException {
        super.activate(context);
        File indexMetadataDirectory = context.getBundleContext().getDataFile(
            EnhancementSemanticIndex.class.getName());
        this.semanticIndexMetadataManager = new SemanticIndexMetadataManager(indexMetadataDirectory);

        // first run of the index, store IndexMetadata of EnhancementSemanticIndex
        if (!semanticIndexMetadataManager.isConfigured(pid)) {
            this.graphURI = new UriRef(this.name);

            logger.info("New Graph will be created for the Semantic Index: {}", this.name);
            Properties indexMetadata = getConfigProperties();

            // create triple collection
            tcManager.createMGraph(this.graphURI);
            logger.info("Triple collection for the Semantic Index: {} has been created successfully",
                this.name);

            semanticIndexMetadataManager.storeIndexMetadata(this.pid, indexMetadata);
            logger.info(
                "A configuration has already been created for the Semantic Index: {}, so only its metadata was stored",
                this.name);

            this.state = IndexState.ACTIVE;
            this.epoch = indexingSource.getEpoch();

        } else {
            // get the last configuration of the index
            java.util.Properties oldMetadata = semanticIndexMetadataManager.getIndexMetadata(pid);
            // load revision of the index and update the index state
            this.revision = Long.parseLong((String) oldMetadata.get(SemanticIndex.PROP_REVISION));
            this.epoch = Long.parseLong((String) oldMetadata.getProperty(SemanticIndex.PROP_EPOCH));
            this.state = IndexState.valueOf(oldMetadata.getProperty(SemanticIndex.PROP_STATE));
            this.graphURI = new UriRef((String) oldMetadata.get(EnhancementSemanticIndex.PROP_GRAPH_URI));
        }
        if (this.state != IndexState.REINDEXING) {
            // start polling the changes in the store
            startIndexingSourceCheckThread();
        }
        updateIndexMetadata();

        // register the graph to the OSGi environment
        registerEnhancementGraph();
        logger.info("The SemanticIndex: {} initialized successfully", this.name);
    }

    private void registerEnhancementGraph() {
        UriRef graphUri = new UriRef(this.name);
        Dictionary<String,Object> props = new Hashtable<String,Object>();
        props.put("graph.uri", graphUri);
        props.put("graph.name", this.name);
        props.put("graph.description", String.format(
            "Stores the enhancements of all content items indexed in the EnhacementSemanticIndex: %s",
            this.name));
        props.put(org.osgi.framework.Constants.SERVICE_RANKING, Integer.MAX_VALUE);
        enhancementGraphRegistry = this.componentContext.getBundleContext().registerService(
            TripleCollection.class.getName(), tcManager.getMGraph(graphUri), props);
        logger.info("TripleCollection: {} is registered to the OSGi environment", graphUri);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        super.deactivate(context);

        // unregister the TripleCollection
        enhancementGraphRegistry.unregister();
        logger.info("TripleColleciton: {} is unregistered from the OSGi environment", this.name);
    }

    @Override
    public boolean index(ContentItem ci) throws IndexException {
        if (this.state == IndexState.REINDEXING) {
            throw new IndexException(String.format(
                "The index '%s' is read-only as it is in reindexing state.", name));
        }
        semUp();
        try {
            performIndex(ci);
        } finally {
            semDown();
        }
        return true;
    }

    private void performIndex(ContentItem ci) throws IndexException {
        MGraph enhancementGraph = tcManager.getMGraph(this.graphURI);

        // remove existing triples regarding this content item
        Iterator<Triple> itr = enhancementGraph.filter(ci.getUri(), null, null);
        List<Triple> tcRemoved = new ArrayList<Triple>();
        while (itr.hasNext()) {
            tcRemoved.add(itr.next());
        }
        enhancementGraph.removeAll(tcRemoved);

        enhancementGraph.addAll(ci.getMetadata());
        logger.debug("Documents are added to index successfully.");
    }

    @Override
    public void remove(String uri) throws IndexException {
        if (this.state == IndexState.REINDEXING) {
            throw new IndexException(String.format(
                "The index '%s' is read-only as it is in reindexing state.", name));
        }
        semUp();
        try {
            performRemove(uri);
        } finally {
            semDown();
        }
    }

    private void performRemove(String ciURI) throws IndexException {
        if (ciURI == null || ciURI.isEmpty()) {
            String msg = "URI of ContentItem cannot be null or empty";
            logger.error(msg);
            throw new IndexException(msg);
        }

        MGraph enhancementGraph = tcManager.getMGraph(this.graphURI);
        Iterator<Triple> itr = enhancementGraph.filter(new UriRef(ciURI), null, null);
        List<Triple> tcRemoved = new ArrayList<Triple>();
        while (itr.hasNext()) {
            tcRemoved.add(itr.next());
        }
        enhancementGraph.removeAll(tcRemoved);
        logger.info("Given Uri {} is removed from index successfully", ciURI);
    }

    @Override
    public List<String> getFieldsNames() throws IndexException {
        // TODO we can return property list of triples in the enhancements
        return new ArrayList<String>();
    }

    @Override
    public Map<String,Object> getFieldProperties(String name) throws IndexException {
        return new HashMap<String,Object>();
    }

    @Override
    public Map<String,String> getRESTSearchEndpoints() {
        Map<String,String> searchEndpoints = new HashMap<String,String>();
        searchEndpoints.put(EndpointTypeEnum.SPARQL.getUri(), "sparql");
        return searchEndpoints;
    }

    @Override
    public Map<String,ServiceReference> getSearchEndPoints() {
        return new HashMap<String,ServiceReference>();
    }

    @Override
    protected void updateIndexMetadata() throws IndexException {
        java.util.Properties properties = getConfigProperties();
        try {
            semanticIndexMetadataManager.updateIndexMetadata(this.pid, properties);
        } catch (IndexManagementException e) {
            logger.error("Failed to update the metadata of the index: {}", this.name, e);
            throw new IndexException(String.format("Failed to update the metadata of the index: %s",
                this.name), e);
        }
    }

    private java.util.Properties getConfigProperties() {
        @SuppressWarnings("rawtypes")
        Dictionary properties = componentContext.getProperties();
        java.util.Properties propertiesSubset = new java.util.Properties();
        propertiesSubset.put(PROP_NAME, properties.get(PROP_NAME));
        propertiesSubset.put(PROP_DESCRIPTION, properties.get(PROP_DESCRIPTION));
        propertiesSubset.put(AbstractSemanticIndex.PROP_BATCH_SIZE,
            properties.get(AbstractSemanticIndex.PROP_BATCH_SIZE));
        propertiesSubset.put(AbstractSemanticIndex.PROP_INDEXING_SOURCE_NAME,
            properties.get(AbstractSemanticIndex.PROP_INDEXING_SOURCE_NAME));
        propertiesSubset.put(AbstractSemanticIndex.PROP_INDEXING_SOURCE_CHECK_PERIOD,
            properties.get(AbstractSemanticIndex.PROP_INDEXING_SOURCE_CHECK_PERIOD));
        propertiesSubset.put(Constants.SERVICE_PID, properties.get(Constants.SERVICE_PID));
        propertiesSubset.put(Constants.SERVICE_RANKING, properties.get(Constants.SERVICE_RANKING));
        propertiesSubset.put(PROP_REVISION, this.revision);
        propertiesSubset.put(PROP_EPOCH, this.epoch);
        propertiesSubset.put(PROP_STATE, this.state.name());
        propertiesSubset.put(PROP_GRAPH_URI, this.graphURI.getUnicodeString());
        return propertiesSubset;
    }

    @Override
    protected void startReindexing() {
        reindexerThread = new Thread(new Reindexer());
        reindexerThread.start();
    }

    /**
     * Separate thread to perform reindexing operation in the background. It creates a temporary graph,
     * indexes all documents obtained from the {@link Store}. After the indexing operation finishes, the
     * temporary graph is replaced with the existing one and the temporary graph is deleted.
     * 
     * @author suat
     * 
     */
    private class Reindexer implements Runnable {
        @Override
        public void run() {
            // create temporary graph
            String temporaryGraphName = null;
            try {
                temporaryGraphName = createTemporaryGraph();
                logger.info("Temporary graph: {} has been created for reindexing of the Semantic Index: {}",
                    temporaryGraphName, name);
            } catch (IndexManagementException e) {
                logger.error("Failed to create temporary graph while reindexing the index: {}", name, e);
                return;
            }

            try {
                // set the active graph to the temporary graph so that documents
                // are submitted to temporary graph
                graphURI = new UriRef(temporaryGraphName);
                // index documents in the store according to the new
                // configuration
                revision = indexDocuments();
                logger.info(
                    "Documents have been re-indexed according to the new configuration of the Semantic Index: {}",
                    name);
            } catch (StoreException e) {
                logger.error("Failed to obtain changes from Store while reindexing the index: {}", name, e);
                tcManager.deleteTripleCollection(graphURI);
                return;
            } catch (IndexException e) {
                logger.error("IndexException while reindexing the index: {}", name, e);
                tcManager.deleteTripleCollection(graphURI);
                return;
            } catch (Exception e) {
                logger.error("Exception while reindexing the index: {}", name, e);
                tcManager.deleteTripleCollection(graphURI);
                return;
            }

            // swap indexes and remove the old one
            swapIndexes(name, temporaryGraphName);

            // set the active graph
            graphURI = new UriRef(name);

            // change the state of the index and update the metadata
            try {
                state = IndexState.ACTIVE;
                updateIndexMetadata();
            } catch (IndexException e) {
                logger.error("Failed to set the state while reindexing the index: {}", name, e);
                return;
            }

            // start update checker
            startIndexingSourceCheckThread();
            logger.info("Reindexing of Semantic Index: {} has completed successfully", name);
        }

        private String createTemporaryGraph() throws IndexManagementException {
            // determine a temporary name
            String indexName = name;
            int count = 1;
            do {
                indexName = name + "-" + count;
                count++;
            } while (isExist(indexName));

            tcManager.createMGraph(new UriRef(indexName));
            return indexName;
        }

        private long indexDocuments() throws StoreException, IndexException {
            ChangeSet cs;
            long revision = Long.MIN_VALUE;
            boolean noChange = false;
            do {
                cs = indexingSource.changes(indexingSource.getEpoch(), revision, batchSize);
                Iterator<String> changedItr = cs.iterator();
                while (changedItr.hasNext()) {
                    String changed = changedItr.next();
                    ContentItem ci = indexingSource.get(changed);
                    if (ci == null) {
                        performRemove(changed);
                    } else {
                        performIndex(ci);
                    }
                }
                noChange = cs.iterator().hasNext() ? false : true;
                if (!noChange) {
                    revision = cs.toRevision();
                }
            } while (!noChange);
            return revision;
        }

        private void swapIndexes(String indexName1, String indexName2) {
            MGraph g1 = null;
            MGraph g2 = null;
            try {
                g1 = tcManager.getMGraph(new UriRef(indexName1));
                g2 = tcManager.getMGraph(new UriRef(indexName2));
            } catch (NoSuchEntityException e) {
                logger.error("Failed to swap indexes");
            }
            // TODO must be in sync block
            g1.removeAll(g1);
            g1.addAll(g2);
            tcManager.deleteTripleCollection(new UriRef(indexName2));
        }

        private boolean isExist(String name) throws IndexManagementException {
            try {
                tcManager.getMGraph(new UriRef(name));
                return true;
            } catch (NoSuchEntityException e) {
                return false;
            }
        }
    }
}

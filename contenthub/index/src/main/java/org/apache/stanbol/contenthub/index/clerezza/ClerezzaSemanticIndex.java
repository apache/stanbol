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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.NoSuchEntityException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.semanticindex.index.EndpointTypeEnum;
import org.apache.stanbol.commons.semanticindex.index.IndexException;
import org.apache.stanbol.commons.semanticindex.index.IndexManagementException;
import org.apache.stanbol.commons.semanticindex.index.IndexState;
import org.apache.stanbol.commons.semanticindex.index.SemanticIndex;
import org.apache.stanbol.commons.semanticindex.store.ChangeSet;
import org.apache.stanbol.commons.semanticindex.store.IndexingSource;
import org.apache.stanbol.commons.semanticindex.store.Store;
import org.apache.stanbol.commons.semanticindex.store.StoreException;
import org.apache.stanbol.contenthub.index.AbstractLDPathSemanticIndex;
import org.apache.stanbol.contenthub.index.AbstractSemanticIndex;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.newmedialab.ldpath.model.fields.FieldMapping;

/**
 * Clerezza based {@link SemanticIndex} implementation. This implementations creates an {@link MGraph} by
 * parsing the provided LDPath program. Several LDPath based semantic indexes can be created through the
 * associated RESTful services deployed under {stanbolhost}/contenthub/index/ldpath or through the Felix Web
 * Console.
 * 
 * Following parameters can be configured while creating a SemanticIndex:
 * <ul>
 * <li><b>Name:</b> Name of the index</li>
 * <li><b>Description:</b> Description of the index</li>
 * <li><b>LDPathProgram: </b> LDPath program that will be used as a source to create the semantic index.</li>
 * <li><b>Batch Size:</b> Maximum number of changes to be processed in a single step while iteratively
 * checking the changes in the {@link IndexingSource}</li>
 * <li><b>Indexing Source Name: </b> Name of the {@link IndexingSource} instance to be checked for updates.
 * This {@link IndexingSource} should be an IndexingSource<ContentItem> implementation.
 * <li><b>Indexing Source Check Period:</b> Time to check changes in the {@link IndexingSource} in second
 * units</li>
 * <li><b>Service Ranking:</b> To be able adjust priorities of {@link SemanticIndex}es with same name or same
 * {@link EndpointTypeEnum}, this property is used. The higher value of this property, the higher priority of
 * the {@link SemanticIndex} instance.</li>
 * </ul>
 * 
 * @author suat
 * @author meric
 * 
 */
@Component(configurationFactory = true, policy = ConfigurationPolicy.REQUIRE, metatype = true, immediate = true)
@Service(value = SemanticIndex.class)
public class ClerezzaSemanticIndex extends AbstractLDPathSemanticIndex {

    private final Logger logger = LoggerFactory.getLogger(ClerezzaSemanticIndex.class);

    public static final String PROP_GRAPH_URI = "org.apache.stanbol.contenthub.index.clerezza.ClerezzaSemanticIndex.graphURI";

    @Reference
    private ClerezzaSemanticIndexFactory clerezzaSemanticIndexFactory;

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
        // create index if it is not already done. When an instance of this component is created through the
        // REST/Java services first the Graph created and then the associated OSGI component is
        // activated
        if (!clerezzaSemanticIndexFactory.getSemanticIndexMetadataManager().isConfigured(pid)) {
            this.graphURI = new UriRef(this.name);
            // new semantic index created from OSGI console, initialize it
            logger.info("New Graph will be created for the Semantic Index: {}", this.name);
            clerezzaSemanticIndexFactory.createIndex(getConfigProperties());
            this.state = IndexState.ACTIVE;
            this.epoch = indexingSource.getEpoch();
        } else {
            // semantic index already created is re-activated or new semantic index created from REST/Java
            // services

            // check the configuration has changed
            java.util.Properties oldMetadata = clerezzaSemanticIndexFactory.getSemanticIndexMetadataManager()
                    .getIndexMetadata(pid);

            checkUnmodifiableConfigurations(name, indexingSource.getName(), oldMetadata);
            if (checkReindexingCondition(ldPathProgram, oldMetadata)) {
                logger.info(
                    "LDPath program of the Semantic Index: {} has been changed. Reindexing will start now...",
                    this.name);
                // ldpath has changed, reindexing is needed
                this.state = IndexState.REINDEXING;
                this.epoch = Long.parseLong((String) oldMetadata.getProperty(SemanticIndex.PROP_EPOCH));
                this.graphURI = new UriRef((String) oldMetadata.get(ClerezzaSemanticIndex.PROP_GRAPH_URI));

                startReindexing();

            } else {
                if (oldMetadata.get(SemanticIndex.PROP_REVISION) != null) {
                    // load revision of the index and update the index state
                    this.revision = Long.parseLong((String) oldMetadata.get(SemanticIndex.PROP_REVISION));
                    this.epoch = Long.parseLong((String) oldMetadata.getProperty(SemanticIndex.PROP_EPOCH));
                    this.state = IndexState.valueOf(oldMetadata.getProperty(SemanticIndex.PROP_STATE));
                    // get the previously stored graphURI
                    this.graphURI = new UriRef((String) oldMetadata.get(ClerezzaSemanticIndex.PROP_GRAPH_URI));

                } else {
                    // newly created index, store the metadata. Index was created through the REST/Java
                    // services
                    this.state = IndexState.ACTIVE;
                    this.epoch = indexingSource.getEpoch();
                    this.graphURI = new UriRef(this.name);
                }
            }
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
            "Stores the enhancements of all content items indexed in the ClerezzaSemanticIndex: %s",
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

        // check the configuration is deleted or just deactivated
        ServiceReference reference = context.getBundleContext().getServiceReference(
            ConfigurationAdmin.class.getName());
        ConfigurationAdmin configAdmin = (ConfigurationAdmin) context.getBundleContext()
                .getService(reference);
        Configuration config;
        try {
            config = configAdmin.getConfiguration(this.pid);
            // if the configuration for the index has been removed, clear all files for this index
            if (config.getProperties() == null) {
                logger.info(
                    "Configuration for the Semantic Index: {} has been deleted. All resources will be removed.",
                    this.name);
                if (clerezzaSemanticIndexFactory.getSemanticIndexMetadataManager().isConfigured(pid)) {
                    // this case is a check for the remove operation from the felix web console
                    clerezzaSemanticIndexFactory.removeIndex(this.pid);
                }
            } // the index is deactivated. do nothing.
        } catch (IOException e) {
            logger.error("Failed to obtain configuration for the Semantic Index: {}.", this.name, e);
        } catch (IndexManagementException e) {
            logger.error("Failed to remove index: {} while deactivating", this.name, e);
        }
    }

    /**
     * This implementation of {@link #index(ContentItem)} method first, gets the enhancements having
     * {@link org.apache.stanbol.enhancer.servicesapi.rdf.Properties#ENHANCER_ENTITY_REFERENCE} property.
     * Target values (a set of entities declared in different external sites) are queried from the Entityhub.
     * During the querying operation the LDPath program which was used to create this index is used. Obtained
     * results are indexed along with the actual content.
     */
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
        TripleCollection tc = null;
        try {
            tc = getTripleCollection(ci);
        } catch (IndexManagementException e) {
            logger.error("Cannot execute the ldPathProgram on ContentItem's metadata", e);
            throw new IndexException("Cannot execute the ldPathProgram on ContentItem's metadata", e);
        }
        MGraph g = tcManager.getMGraph(this.graphURI);

        // remove existing triples regarding this content item
        Iterator<Triple> itr = g.filter(ci.getUri(), null, null);
        List<Triple> tcRemoved = new ArrayList<Triple>();
        while (itr.hasNext()) {
            tcRemoved.add(itr.next());
        }
        g.removeAll(tcRemoved);

        g.addAll(tc);
        logger.debug("Documents are added to index successfully.");
    }

    private TripleCollection getTripleCollection(ContentItem ci) throws IndexManagementException {
        Iterator<Triple> it = ci.getMetadata().filter(null,
            org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE, null);
        Set<String> contexts = new HashSet<String>();
        while (it.hasNext()) {
            Resource r = it.next().getObject();
            if (r instanceof UriRef) {
                contexts.add(((UriRef) r).getUnicodeString());
            }
        }
        TripleCollection tc = new SimpleMGraph();
        Map<String,Collection<?>> results = executeProgram(contexts, ci);
        for (Entry<String,Collection<?>> entry : results.entrySet()) {
            // String predicate = NamespaceEnum.getFullName(entry.getKey());
            // if (entry.getKey().equals(predicate)) {
            // logger.error("Undefined namespace in predicate: {}", predicate);
            // throw new IndexManagementException(String.format("Undefined namespace in predicate: %s",
            // predicate));
            // }
            for (Object value : entry.getValue()) {
                tc.add(new TripleImpl(ci.getUri(), new UriRef(entry.getKey()), LiteralFactory.getInstance()
                        .createTypedLiteral(value.toString())));
            }
        }
        return tc;
    }

    @Override
    public void remove(String uri) throws IndexException {
        if (this.state == IndexState.REINDEXING) {
            String msg = String.format("The index '%s' is read-only as it is in reindexing state.", name);
            logger.error(msg);
            throw new IndexException(msg);
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

        MGraph g = tcManager.getMGraph(this.graphURI);
        Iterator<Triple> itr = g.filter(new UriRef(ciURI), null, null);
        List<Triple> tcRemoved = new ArrayList<Triple>();
        while (itr.hasNext()) {
            tcRemoved.add(itr.next());
        }
        g.removeAll(tcRemoved);
        logger.info("Given Uri {} is removed from index successfully", ciURI);
    }

    @Override
    public List<String> getFieldsNames() throws IndexException {
        List<String> fieldsNames = new ArrayList<String>();
        for (FieldMapping<?,Object> field : this.objectProgram.getFields()) {
            fieldsNames.add(field.getFieldName());
        }
        return (fieldsNames == null || fieldsNames.size() == 0) ? null : fieldsNames;
    }

    @Override
    public Map<String,Object> getFieldProperties(String name) throws IndexException {
        Map<String,Object> fieldProperties = new HashMap<String,Object>();
        FieldMapping<?,Object> field = this.objectProgram.getField(name);
        if (field.getFieldConfig() != null) {
            fieldProperties.putAll(field.getFieldConfig());
        }
        fieldProperties.put("type", field.getFieldType());
        return fieldProperties;
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

    protected void updateIndexMetadata() throws IndexException {
        java.util.Properties properties = getConfigProperties();
        try {
            clerezzaSemanticIndexFactory.getSemanticIndexMetadataManager().updateIndexMetadata(this.pid,
                properties);
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
        propertiesSubset.put(AbstractLDPathSemanticIndex.PROP_LD_PATH_PROGRAM,
            properties.get(AbstractLDPathSemanticIndex.PROP_LD_PATH_PROGRAM));
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

    protected void startReindexing() {
        reindexerThread = new Thread(new Reindexer());
        reindexerThread.start();
    }

    /**
     * Separate thread to perform reindexing operation in the background. It creates a temporary Graph,
     * indexes all documents obtained from the {@link Store} using the new LDPath program. After the indexing
     * operation finishes, the temporary graph is replaced with the existing one and the temporary graph is
     * deleted.
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
                logger.info(
                    "Temporary graph core: {} has been created for reindexing of the Semantic Index: {}",
                    temporaryGraphName, name);
            } catch (IndexManagementException e) {
                logger.error("Failed to create temporary Graph while reindexing the index: {}", name, e);
                return;
            }

            try {
                // set the active graph to the temporary graph so that documents
                // are submitted to temporary graph
                graphURI = new UriRef(temporaryGraphName);
                // index documents in the store according to the new configuration
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
            } while ((clerezzaSemanticIndexFactory.isExist(indexName)));

            tcManager.createMGraph(new UriRef(indexName));
            return indexName;
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
    }
}
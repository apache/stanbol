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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.LukeRequest;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.apache.stanbol.commons.semanticindex.index.EndpointTypeEnum;
import org.apache.stanbol.commons.semanticindex.index.IndexException;
import org.apache.stanbol.commons.semanticindex.index.IndexManagementException;
import org.apache.stanbol.commons.semanticindex.index.IndexState;
import org.apache.stanbol.commons.semanticindex.index.SemanticIndex;
import org.apache.stanbol.commons.semanticindex.store.ChangeSet;
import org.apache.stanbol.commons.semanticindex.store.IndexingSource;
import org.apache.stanbol.commons.semanticindex.store.Store;
import org.apache.stanbol.commons.semanticindex.store.StoreException;
import org.apache.stanbol.commons.solr.IndexReference;
import org.apache.stanbol.commons.solr.RegisteredSolrServerTracker;
import org.apache.stanbol.commons.solr.managed.IndexMetadata;
import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.apache.stanbol.contenthub.index.AbstractLDPathSemanticIndex;
import org.apache.stanbol.contenthub.index.AbstractSemanticIndex;
import org.apache.stanbol.contenthub.servicesapi.index.search.featured.FeaturedSearch;
import org.apache.stanbol.contenthub.servicesapi.index.search.solr.SolrSearch;
import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary.SolrFieldName;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.entityhub.core.utils.OsgiUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Solr based {@link SemanticIndex} implementation. This implementations creates the underlying Solr core by
 * parsing the provided LDPath program. Several LDPath based semantic indexes can be created through the
 * associated RESTful services deployed under {stanbolhost}/contenthub/index/ldpath or through the Felix Web
 * Console.
 * 
 * Following parameters can be configured while creating a SemanticIndex:
 * <ul>
 * <li><b>Name:</b> Name of the index</li>
 * <li><b>Description:</b> Description of the index</li>
 * <li><b>LDPathProgram: </b> LDPath program that will be used as a source to create the semantic index. Index
 * fields and Solr specific configurations regarding those index fields are given in this parameter.</li>
 * <li><b>Index Content: </b> If this configuration is true plain text content of the {@link ContentItem} is
 * also indexed to be used in the full text search.</li>
 * <li><b>Batch Size:</b> Maximum number of changes to be processed in a single step while iteratively
 * checking the changes in the {@link IndexingSource}</li>
 * <li><b>Indexing Source Name: </b> Name of the {@link IndexingSource} instance to be checked for updates.
 * This {@link IndexingSource} should be an IndexingSource<ContentItem> implementation.
 * <li><b>Indexing Source Check Period:</b> Time to check changes in the {@link IndexingSource} in second
 * units</li>
 * <li><b>Solr Server Check Time:</b> Maximum time in seconds to wait for the availability of the Solr core
 * associated with this index</li>
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
@Service
@Properties(value = {@Property(name = SolrSemanticIndex.PROP_SOLR_CHECK_TIME, intValue = 5),
                     @Property(name = SolrSemanticIndex.PROP_INDEX_CONTENT, boolValue = true)})
public class SolrSemanticIndex extends AbstractLDPathSemanticIndex {

    public static final String PROP_SOLR_CHECK_TIME = "org.apache.stanbol.contenthub.index.solr.SolrSemanticIndex.solrCheckTime";
    public static final String PROP_INDEX_CONTENT = "org.apache.stanbol.contenthub.index.solr.SolrSemanticIndex.indexContent";

    private static final Logger logger = LoggerFactory.getLogger(SolrSemanticIndex.class);

    private static final Set<String> SUPPORTED_MIMETYPES = Collections.unmodifiableSet(new HashSet<String>(
            Arrays.asList("text/html", "text/plain", "text/xml")));

    private boolean indexContent;

    @Reference
    private SolrSemanticIndexFactory solrSemanticIndexFactory;

    @Reference(target = "(org.apache.solr.core.CoreContainer.name=contenthubSolrSemanticIndex)")
    private ManagedSolrServer managedSolrServer;

    private int solrCheckTime;
    private RegisteredSolrServerTracker registeredServerTracker;

    @Activate
    protected void activate(ComponentContext context) throws IndexException,
                                                     IndexManagementException,
                                                     ConfigurationException,
                                                     StoreException {

        super.activate(context);
        @SuppressWarnings("rawtypes")
        Dictionary properties = context.getProperties();
        this.solrCheckTime = (Integer) OsgiUtils.checkProperty(properties, PROP_SOLR_CHECK_TIME);
        this.indexContent = (Boolean) OsgiUtils.checkProperty(properties,
            SolrSemanticIndex.PROP_INDEX_CONTENT);

        // create index if it is not already done. When an instance of this component is created through the
        // REST/Java services first the Solr core initalized and then the associated OSGI component is
        // activated
        if (!solrSemanticIndexFactory.getSemanticIndexMetadataManager().isConfigured(pid)) {
            // solr core has not been created. create now...
            logger.info("New Solr core will be created for the Semantic Index: {}", this.name);
            solrSemanticIndexFactory.createIndex(getConfigProperties());
            this.state = IndexState.ACTIVE;
            this.epoch = indexingSource.getEpoch();

        } else {
            // solr core has already been created or the semantic index component is re-activated second or
            // more

            // check the configuration has changed
            java.util.Properties oldMetadata = solrSemanticIndexFactory.getSemanticIndexMetadataManager()
                    .getIndexMetadata(pid);

            checkUnmodifiableConfigurations(name, indexingSource.getName(), oldMetadata);
            if (checkReindexingCondition(ldPathProgram, oldMetadata)) {
                logger.info(
                    "LDPath program of the Semantic Index: {} has been changed. Reindexing will start now...",
                    this.name);
                // ldpath has changed, reindexing is needed
                this.state = IndexState.REINDEXING;
                this.epoch = Long.parseLong((String) oldMetadata.getProperty(SemanticIndex.PROP_EPOCH));

                startReindexing();

            } else {
                if (oldMetadata.get(SemanticIndex.PROP_REVISION) != null) {
                    // load revision of the index and update the index state
                    this.revision = Long.parseLong((String) oldMetadata.get(SemanticIndex.PROP_REVISION));
                    this.epoch = Long.parseLong((String) oldMetadata.getProperty(SemanticIndex.PROP_EPOCH));
                    this.state = IndexState.valueOf(oldMetadata.getProperty(SemanticIndex.PROP_STATE));

                } else {
                    // newly created index, store the metadata. Index was created through the REST/Java
                    // services
                    this.state = IndexState.ACTIVE;
                    this.epoch = indexingSource.getEpoch();
                }
            }
        }

        if (this.state != IndexState.REINDEXING) {
            // start tracking for the Solr core
            initializeTracker(this.name);

            // start polling the changes in the store
            startIndexingSourceCheckThread();
        }
        updateIndexMetadata();
        logger.info("The SemanticIndex: {} initialized successfully", this.name);
    }

    private void initializeTracker(String name) {
        try {
            registeredServerTracker = new RegisteredSolrServerTracker(componentContext.getBundleContext(),
                    new IndexReference(managedSolrServer.getServerName(), name), null);
        } catch (InvalidSyntaxException e) {
            // ignore as there is no filter specified
        }

        // start tracking
        registeredServerTracker.open();
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        super.deactivate(context);
        if (registeredServerTracker != null) {
            registeredServerTracker.close();
        }

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
                if (solrSemanticIndexFactory.getSemanticIndexMetadataManager().isConfigured(pid)) {
                    // this case is a check for the remove operation from the felix web console
                    solrSemanticIndexFactory.removeIndex(this.pid);
                }
            } // the index is deactivated. do not nothing.
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
        try {
            SolrServer solrServer = getServer();
            SolrInputDocument doc = getSolrDocument(ci);
            if (this.indexContent && SUPPORTED_MIMETYPES.contains(ci.getMimeType())) {
                doc.addField(SolrFieldName.CONTENT.toString(),
                    org.apache.commons.io.IOUtils.toString(ci.getStream()));
            }
            solrServer.add(doc);
            solrServer.commit();
            logger.debug("Documents are committed to Solr Server successfully.");

        } catch (SolrServerException e) {
            logger.error("Given SolrInputDocument cannot be added to Solr Server with name " + this.name, e);
            throw new IndexException("Given SolrInputDocument cannot be added to Solr Server with name "
                                     + this.name, e);
        } catch (IOException e) {
            logger.error("Given SolrInputDocument cannot be added to Solr Server with name " + this.name, e);
            throw new IndexException("Given SolrInputDocument cannot be added to Solr Server with name "
                                     + this.name, e);
        } catch (IndexManagementException e) {
            logger.error("Cannot execute the ldPathProgram on ContentItem's metadata", e);
            throw new IndexException("Cannot execute the ldPathProgram on ContentItem's metadata", e);
        }
    }

    private SolrInputDocument getSolrDocument(ContentItem ci) throws IndexManagementException {
        Iterator<Triple> it = ci.getMetadata().filter(null,
            org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE, null);
        Set<String> contexts = new HashSet<String>();
        while (it.hasNext()) {
            Resource r = it.next().getObject();
            if (r instanceof UriRef) {
                contexts.add(((UriRef) r).getUnicodeString());
            }
        }
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField(SolrFieldName.ID.toString(), ci.getUri().getUnicodeString());
        Map<String,Collection<?>> results = executeProgram(contexts, ci);
        for (Entry<String,Collection<?>> entry : results.entrySet()) {
            doc.addField(entry.getKey(), entry.getValue());
        }

        return doc;
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

        SolrServer solrServer = null;
        try {
            solrServer = getServer();
            solrServer.deleteById(ciURI);
            solrServer.commit();
            logger.info("Given Uri {} is removed from index successfully", ciURI);
        } catch (SolrServerException e) {
            logger.error("Given SolrInputDocument cannot be added to Solr Server with name " + this.name, e);
            throw new IndexException("Given SolrInputDocument cannot be added to Solr Server with name "
                                     + this.name, e);
        } catch (IOException e) {
            logger.error("Given SolrInputDocument cannot be added to Solr Server with name " + this.name, e);
            throw new IndexException("Given SolrInputDocument cannot be added to Solr Server with name "
                                     + this.name, e);
        }
    }

    @Override
    public List<String> getFieldsNames() throws IndexException {
        SolrServer solrServer = null;
        LukeRequest qr = new LukeRequest();
        qr.setShowSchema(true);
        NamedList<Object> qresp = null;
        try {
            solrServer = getServer();
            qresp = solrServer.request(qr);
        } catch (SolrServerException e) {
            logger.error("Cannot retrieve fields from solr with Luke Request", e);
            throw new IndexException("Cannot retrieve field names from solr with Luke Request", e);
        } catch (IOException e) {
            logger.error("Cannot retrieve fields from solr with Luke Request", e);
            throw new IndexException("Cannot retrieve field names from solr with Luke Request", e);
        }
        List<String> fieldNames = null;
        Object schema = qresp.get("schema");
        @SuppressWarnings("unchecked")
        NamedList<Object> schemaList = (NamedList<Object>) schema;
        Object fields = schemaList.get("fields");
        if (fields instanceof NamedList<?>) {
            @SuppressWarnings("unchecked")
            NamedList<Object> fieldsList = (NamedList<Object>) fields;
            fieldNames = new ArrayList<String>();
            for (int i = 0; i < fieldsList.size(); i++) {
                fieldNames.add(fieldsList.getName(i));
            }
        }
        return (fieldNames == null || fieldNames.size() == 0) ? null : fieldNames;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String,Object> getFieldProperties(String name) throws IndexException {
        SolrServer solrServer = null;
        Map<String,Object> fieldProperties = new HashMap<String,Object>();

        LukeRequest qr = new LukeRequest();
        qr.setShowSchema(true);
        NamedList<Object> qresp = null;
        try {
            solrServer = getServer();
            qresp = solrServer.request(qr);
        } catch (SolrServerException e) {
            logger.error("Cannot retrieve fields from solr with Luke Request", e);
            throw new IndexException("Cannot retrieve field names from solr with Luke Request", e);
        } catch (IOException e) {
            logger.error("Cannot retrieve fields from solr with Luke Request", e);
            throw new IndexException("Cannot retrieve field names from solr with Luke Request", e);
        }
        Object schema = qresp.get("schema");
        NamedList<Object> schemaList = (NamedList<Object>) schema;
        Object fields = schemaList.get("fields");
        if (fields instanceof NamedList<?>) {

            NamedList<Object> fieldsList = (NamedList<Object>) fields;
            Object field = fieldsList.get(name);
            if (field instanceof NamedList<?>) {
                NamedList<Object> fieldProps = (NamedList<Object>) field;
                for (int i = 0; i < fieldProps.size(); i++) {
                    fieldProperties.put(fieldProps.getName(i), fieldProps.getVal(i));
                }
            }
        } else {
            throw new IllegalStateException(
                    "Fields container is not a NamedList, so there is no facet information available");
        }
        return (fieldProperties == null || fieldProperties.size() == 0) ? null : fieldProperties;
    }

    @Override
    public Map<String,String> getRESTSearchEndpoints() {
        Map<String,String> searchEndpoints = new HashMap<String,String>();
        searchEndpoints.put(EndpointTypeEnum.CONTENTHUB.getUri(), "contenthub/" + this.name
                                                                  + "/search/featured");
        searchEndpoints.put(EndpointTypeEnum.SOLR.getUri(), "solr/" + managedSolrServer.getServerName() + "/"
                                                            + this.name);
        return searchEndpoints;
    }

    @Override
    public Map<String,ServiceReference> getSearchEndPoints() {
        BundleContext bundleContext = this.componentContext.getBundleContext();
        Map<String,ServiceReference> serviceEndPoints = new HashMap<String,ServiceReference>();
        ServiceReference serviceReference = null;

        serviceReference = bundleContext.getServiceReference(SolrSearch.class.getName());
        if (serviceReference != null) {
            serviceEndPoints.put(SolrSearch.class.getName(), serviceReference);
        }
        serviceReference = bundleContext.getServiceReference(FeaturedSearch.class.getName());
        if (serviceReference != null) {
            serviceEndPoints.put(FeaturedSearch.class.getName(), serviceReference);
        }

        return serviceEndPoints;
    }

    /**
     * Obtains the {@link SolrServer} for the given Solr core identified by the name of this Semantic Index.
     * It uses the {@link RegisteredSolrServerTracker} and waits for the server by {@link #solrCheckTime}.
     * 
     * @return the {@link SolrServer} instance for the given {@code coreName}.
     * @throws IndexException
     */
    public SolrServer getServer() throws IndexException {
        SolrServer solrServer = registeredServerTracker.getService();

        // if server is null wait as long as the specified the waiting time for the solr server
        if (solrServer == null) {
            for (int i = 0; i < solrCheckTime; i++) {
                try {
                    logger.info(" ... waiting 1sec for SolrServer");
                    solrServer = (SolrServer) registeredServerTracker.waitForService(1000);
                } catch (InterruptedException e) {

                }
            }
        }

        if (solrServer == null) {
            logger.error("Failed to obtain SolrServer through RegisteredSolrServerTracker");
            throw new IndexException(
                    String.format("Failed to obtain SolrServer through RegisteredSolrServerTracker"));
        }
        return solrServer;
    }

    protected void updateIndexMetadata() throws IndexException {
        java.util.Properties properties = getConfigProperties();
        try {
            solrSemanticIndexFactory.getSemanticIndexMetadataManager().updateIndexMetadata(this.pid,
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
        propertiesSubset.put(SolrSemanticIndex.PROP_INDEX_CONTENT,
            properties.get(SolrSemanticIndex.PROP_INDEX_CONTENT));
        propertiesSubset.put(AbstractSemanticIndex.PROP_BATCH_SIZE,
            properties.get(AbstractSemanticIndex.PROP_BATCH_SIZE));
        propertiesSubset.put(AbstractSemanticIndex.PROP_INDEXING_SOURCE_NAME,
            properties.get(AbstractSemanticIndex.PROP_INDEXING_SOURCE_NAME));
        propertiesSubset.put(PROP_SOLR_CHECK_TIME, properties.get(PROP_SOLR_CHECK_TIME));
        propertiesSubset.put(AbstractSemanticIndex.PROP_INDEXING_SOURCE_CHECK_PERIOD,
            properties.get(AbstractSemanticIndex.PROP_INDEXING_SOURCE_CHECK_PERIOD));
        propertiesSubset.put(Constants.SERVICE_PID, properties.get(Constants.SERVICE_PID));
        propertiesSubset.put(Constants.SERVICE_RANKING, properties.get(Constants.SERVICE_RANKING));
        propertiesSubset.put(PROP_REVISION, this.revision);
        propertiesSubset.put(PROP_EPOCH, this.epoch);
        propertiesSubset.put(PROP_STATE, this.state.name());
        return propertiesSubset;
    }

    protected void startReindexing() {
        reindexerThread = new Thread(new Reindexer());
        reindexerThread.start();
    }

    /**
     * Separate thread to perform reindexing operation in the background. It creates a temporary Solr core,
     * indexes all documents obtained from the {@link Store} using the new LDPath program. After the indexing
     * operation finishes, the temporary core is replaced with the existing one and the temporary core is
     * deleted.
     * 
     * @author suat
     * 
     */
    private class Reindexer implements Runnable {
        @Override
        public void run() {
            // create temporary core
            IndexMetadata temporaryCoreMetadata = null;
            try {
                temporaryCoreMetadata = createTemporarySolrCore();
                logger.info(
                    "Temporary solr core: {} has been created for reindexing of the Semantic Index: {}",
                    temporaryCoreMetadata.getIndexName(), name);
            } catch (IndexManagementException e) {
                logger.error("Failed to create temporary Solr core while reindexing the index: {}", name, e);
                return;
            }
            String temporaryCoreName = temporaryCoreMetadata.getIndexName();

            try {
                // initialize solr server tracker for the temporary core
                initializeTracker(temporaryCoreName);

                // index documents in the store according to the new configuration

                revision = indexDocuments();
                logger.info(
                    "Documents have been re-indexed according to the new configuration of the Semantic Index: {}",
                    name);
            } catch (StoreException e) {
                logger.error("Failed to obtain changes from Store while reindexing the index: {}", name, e);
                managedSolrServer.removeIndex(temporaryCoreName, true);
                return;
            } catch (IndexException e) {
                logger.error("IndexException while reindexing the index: {}", name, e);
                managedSolrServer.removeIndex(temporaryCoreName, true);
                return;
            } catch (Exception e) {
                logger.error("Exception while reindexing the index: {}", name, e);
                managedSolrServer.removeIndex(temporaryCoreName, true);
                return;
            }

            // swap indexes
            managedSolrServer.swapIndexes(name, temporaryCoreName);

            // remove the old one
            managedSolrServer.removeIndex(temporaryCoreName, true);

            // adjust the tracker so that it tracks the actual solr core
            registeredServerTracker.close();
            initializeTracker(name);

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

        private IndexMetadata createTemporarySolrCore() throws IndexManagementException {
            // determine a temporary name
            String coreName = name;
            int count = 1;
            do {
                coreName = name + "-" + count;
                count++;
            } while ((managedSolrServer.isManagedIndex(coreName)));

            return solrSemanticIndexFactory.createSolrCore(coreName, ldPathProgram);
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
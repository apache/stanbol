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
package org.apache.stanbol.contenthub.index;

import java.io.StringReader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.clerezza.rdf.core.Resource;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.stanbol.commons.semanticindex.index.IndexException;
import org.apache.stanbol.commons.semanticindex.index.IndexManagementException;
import org.apache.stanbol.commons.semanticindex.index.IndexState;
import org.apache.stanbol.commons.semanticindex.index.SemanticIndex;
import org.apache.stanbol.commons.semanticindex.store.ChangeSet;
import org.apache.stanbol.commons.semanticindex.store.EpochException;
import org.apache.stanbol.commons.semanticindex.store.IndexingSource;
import org.apache.stanbol.commons.semanticindex.store.StoreException;
import org.apache.stanbol.contenthub.index.solr.LDPathUtils;
import org.apache.stanbol.enhancer.ldpath.EnhancerLDPath;
import org.apache.stanbol.enhancer.ldpath.backend.ContentItemBackend;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.core.utils.OsgiUtils;
import org.apache.stanbol.entityhub.ldpath.EntityhubLDPath;
import org.apache.stanbol.entityhub.ldpath.backend.SiteManagerBackend;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.newmedialab.ldpath.LDPath;
import at.newmedialab.ldpath.exception.LDPathParseException;
import at.newmedialab.ldpath.model.programs.Program;

/**
 * <p>
 * Abstract class which can be used to develop new OSGi based {@link SemanticIndex} implementations. It
 * provides various kind of methods which are possibly used by different extenstion of this class. By default
 * it implements the {@link SemanticIndex} interface. However, not all of the methods are implemented. So, new
 * implementations are expected to implement the missing ones.
 * </p>
 * <p>
 * For the provided methods please refer to the specific documentations of methods below.
 * </p>
 * 
 * @author meric
 * @author suat
 * 
 */
@Properties(value = {
                     @Property(name = SemanticIndex.PROP_NAME),
                     @Property(name = SemanticIndex.PROP_DESCRIPTION),
                     @Property(name = AbstractLDPathSemanticIndex.PROP_LD_PATH_PROGRAM),
                     @Property(name = AbstractLDPathSemanticIndex.PROP_BATCH_SIZE, intValue = 10),
                     @Property(name = AbstractLDPathSemanticIndex.PROP_INDEXING_SOURCE_NAME, value = "contenthubFileStore"),
                     @Property(name = AbstractLDPathSemanticIndex.PROP_INDEXING_SOURCE_CHECK_PERIOD, intValue = 10),
                     @Property(name = Constants.SERVICE_RANKING, intValue = 0)})
public abstract class AbstractLDPathSemanticIndex implements SemanticIndex<ContentItem> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractLDPathSemanticIndex.class);

    public static final String PROP_LD_PATH_PROGRAM = "org.apache.stanbol.contenthub.index.AbstractLDPathSemanticIndex.ldPathProgram";
    public static final String PROP_INDEXING_SOURCE_CHECK_PERIOD = "org.apache.stanbol.contenthub.index.AbstractLDPathSemanticIndex.indexingSourceCheckPeriod";
    public static final String PROP_INDEXING_SOURCE_NAME = "org.apache.stanbol.contenthub.index.AbstractLDPathSemanticIndex.indexingSourceName";
    public static final String PROP_BATCH_SIZE = "org.apache.stanbol.contenthub.index.AbstractLDPathSemanticIndex.batchSize";

    protected String name;
    protected String description;
    protected String ldPathProgram;
    protected Program<Object> objectProgram;
    protected int batchSize;
    protected int indexingSourceCheckPeriod;
    protected long epoch;
    protected long revision = Long.MIN_VALUE;
    protected volatile IndexState state = IndexState.UNINIT;
    protected String pid;
    protected ComponentContext componentContext;
    private Integer indexingCount;
    // store update check thread
    private Thread pollingThread;
    private volatile Boolean deactivate = new Boolean(false);
    // reindexer thread
    protected Thread reindexerThread;
    protected IndexingSource<ContentItem> indexingSource;
    protected SemanticIndexMetadataManager semanticIndexMetadataManager;

    @Reference
    protected SiteManager siteManager;

    /**
     * Updates metadata of the index persistently
     * 
     * @throws IndexException
     */
    protected abstract void updateIndexMetadata() throws IndexException;

    /**
     * Starts the reindexing operation within the index. This situation occurs when the LDPath of the index
     * changes.
     */
    protected abstract void startReindexing();

    /**
     * Initializes the common properties to be obtained from the {@link ComponentContext}. Furthermore, this
     * method tries to fetch the specified {@link IndexingSource} from the OSGi environment. This method is
     * expected to be called in the {@code activate} method of the actual implementation of this abstract
     * class.
     * 
     * @param componentContext
     *            {@link ComponentException} of the actual implementation of this abstract class
     * @throws ConfigurationException
     * @throws IndexException
     * @throws IndexManagementException
     * @throws StoreException
     */
    protected void activate(ComponentContext componentContext) throws ConfigurationException,
                                                              IndexException,
                                                              IndexManagementException,
                                                              StoreException {
        @SuppressWarnings("rawtypes")
        Dictionary properties = componentContext.getProperties();
        this.name = (String) OsgiUtils.checkProperty(properties, SemanticIndex.PROP_NAME);
        this.ldPathProgram = (String) OsgiUtils.checkProperty(properties,
            AbstractLDPathSemanticIndex.PROP_LD_PATH_PROGRAM);
        this.description = (String) OsgiUtils.checkProperty(properties, SemanticIndex.PROP_DESCRIPTION);
        this.batchSize = (Integer) OsgiUtils.checkProperty(properties,
            AbstractLDPathSemanticIndex.PROP_BATCH_SIZE);
        this.indexingSourceCheckPeriod = (Integer) OsgiUtils.checkProperty(properties,
            AbstractLDPathSemanticIndex.PROP_INDEXING_SOURCE_CHECK_PERIOD);
        this.componentContext = componentContext;
        this.indexingCount = 0;
        this.pid = (String) properties.get(Constants.SERVICE_PID);

        initializeIndexingSource((String) OsgiUtils.checkProperty(properties,
            AbstractLDPathSemanticIndex.PROP_INDEXING_SOURCE_NAME), componentContext);
        initializeLDPathProgram();
    }

    protected void deactivate(ComponentContext componentContext) {
        // close store check thread and solr core tracker
        deactivate = true;
        if (pollingThread != null) {
            pollingThread.interrupt();
        }
    }

    @SuppressWarnings("unchecked")
    private void initializeIndexingSource(String indexingSourceName, ComponentContext componentContext) throws ConfigurationException {
        BundleContext bundleContext = componentContext.getBundleContext();
        try {
            ServiceReference[] indexingSources = bundleContext.getServiceReferences(
                IndexingSource.class.getName(), null);
            for (ServiceReference serviceReference : indexingSources) {
                Object indexingSource = bundleContext.getService(serviceReference);
                Type[] genericInterfaces = indexingSource.getClass().getGenericInterfaces();
                if (genericInterfaces.length == 1 && genericInterfaces[0] instanceof ParameterizedType) {
                    Type[] types = ((ParameterizedType) genericInterfaces[0]).getActualTypeArguments();
                    try {
                        @SuppressWarnings("unused")
                        Class<ContentItem> contentItemClass = (Class<ContentItem>) types[0];
                        if (((IndexingSource<ContentItem>) indexingSource).getName().equals(
                            indexingSourceName)) {
                            this.indexingSource = (IndexingSource<ContentItem>) indexingSource;
                        }
                    } catch (ClassCastException e) {
                        // ignore
                    }
                }
            }
            if (this.indexingSource == null) {
                throw new ConfigurationException(AbstractLDPathSemanticIndex.PROP_INDEXING_SOURCE_NAME,
                        "There is no IndexingSource<ContentItem> for the given IndexingSource name: "
                                + indexingSourceName);
            }
        } catch (InvalidSyntaxException e) {
            // ignore as there is no filter
        }
    }

    private void initializeLDPathProgram() throws IndexException, IndexManagementException {
        // create program for EntityhubLDPath
        SiteManagerBackend backend = new SiteManagerBackend(siteManager);
        ValueFactory vf = InMemoryValueFactory.getInstance();
        EntityhubLDPath ldPath = new EntityhubLDPath(backend, vf);
        try {
            this.objectProgram = ldPath.parseProgram(LDPathUtils.constructReader(this.ldPathProgram));
        } catch (LDPathParseException e) {
            logger.error("Should never happen!!!!!", e);
            throw new IndexException("Failed to create Program from the parsed LDPath", e);
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Class<ContentItem> getIntdexType() {
        return ContentItem.class;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public IndexState getState() {
        synchronized (state) {
            return state;
        }
    }

    @Override
    public void persist(long revision) throws IndexException {
        this.revision = revision;
        updateIndexMetadata();
    }

    @Override
    public long getRevision() {
        return this.revision;
    }

    /**
     * Checks whether the name of the associated {@link IndexingSource} or name of the index has been changed.
     * 
     * @param name
     *            new name of the SemanticIndex
     * @param indexingSourceName
     *            new name of the {@link IndexingSource} associated with this index
     * @param oldMetadata
     *            old metadata of the SemanticIndex
     * @return {@code true} if the LDPath program has changed, otherwise {@code false}
     * @throws ConfigurationException
     */
    protected void checkUnmodifiableConfigurations(String name,
                                                   String indexingSourceName,
                                                   java.util.Properties oldMetadata) throws ConfigurationException {

        // name of the semantic index has changed
        if (!name.equals(oldMetadata.get(SemanticIndex.PROP_NAME))) {
            throw new ConfigurationException(SemanticIndex.PROP_NAME,
                    "It is not allowed to change the name of a Semantic Index");
        }

        // name of the indexing source has changed
        if (!indexingSourceName
                .equals(oldMetadata.get(AbstractLDPathSemanticIndex.PROP_INDEXING_SOURCE_NAME))) {
            throw new ConfigurationException(AbstractLDPathSemanticIndex.PROP_INDEXING_SOURCE_NAME,
                    "For the time being, it is not allowed to change the name of the indexing source.");
        }
    }

    /**
     * Checks the reindexing condition by comparing the new current LDPath program of the index with the older
     * one
     * 
     * @param ldPath
     *            new LDPath program of the SemanticIndex
     * @param oldMetadata
     *            old metadata of the SemanticIndex
     * @return {@code true} if the LDPath has changed, otherwise {@code false}
     */
    protected boolean checkReindexingCondition(String ldPath, java.util.Properties oldMetadata) {
        // ldpath of the semantic has changed, reindexing needed
        if (!ldPath.equals(oldMetadata.get(AbstractLDPathSemanticIndex.PROP_LD_PATH_PROGRAM))) {
            return true;
        }
        return false;
    }

    /**
     * Increases the counter e.g the {@link #indexingCount} by one. This means that a client is occupying the
     * index for an indexing operation. For each separate request the counter is increased by one.
     * Furthermore, this method sets the state of the index as {@link IndexState#INDEXING} and updates the
     * metadata of the index persistently.
     * 
     * @throws IndexException
     */
    protected void semUp() throws IndexException {
        synchronized (this.indexingCount) {
            this.indexingCount++;
            this.state = IndexState.INDEXING;
            updateIndexMetadata();
        }
    }

    /**
     * Decreases the counter e.g the {@link #indexingCount} by one. This means that a client has finished the
     * interaction with the index. Furthermore, this method sets the state of the index as
     * {@link IndexState#ACTIVE} and updates the metadata of the index persistently.
     * 
     * @throws IndexException
     */
    protected void semDown() throws IndexException {
        synchronized (this.indexingCount) {
            this.indexingCount--;
            if (this.indexingCount == 0) {
                this.state = IndexState.ACTIVE;
                updateIndexMetadata();
            }
        }
    }

    /**
     * This method executes the LDPath program, which was used to configure this index, on the enhancements of
     * submitted content by means of the Entityhub. In other words, additional information is gathered from
     * the Entityhub for each entity detected in the enhancements by querying the ldpath of this index.
     * Furthermore, the same LDPath is also executed on the given {@link ContentItem} through the
     * {@link ContentItemBackend}.
     * 
     * @param contexts
     *            a {@link Set} of URIs (string representations) that are used as starting nodes to execute
     *            LDPath program of this index. The context are the URIs of the entities detected in the
     *            enhancements of the content submitted.
     * @param ci
     *            {@link ContentItem} on on which the LDPath associated with this index will be executed
     * @return the {@link Map} containing the results obtained by executing the given program on the given
     *         contexts. Keys of the map corresponds to fields in the program and values of the map
     *         corresponds to results obtained for the field specified in the key.
     * @throws IndexManagementException
     */
    protected Map<String,Collection<?>> executeProgram(Set<String> contexts, ContentItem ci) throws IndexManagementException {
        Map<String,Collection<?>> results = new HashMap<String,Collection<?>>();

        // execute the given LDPath for each context passed in contexts parameter
        SiteManagerBackend backend = new SiteManagerBackend(siteManager);
        ValueFactory vf = InMemoryValueFactory.getInstance();
        EntityhubLDPath entityhubPath = new EntityhubLDPath(backend, vf);
        Representation representation;
        for (String context : contexts) {
            representation = entityhubPath.execute(vf.createReference(context), this.objectProgram);
            Iterator<String> fieldNames = representation.getFieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                Iterator<Object> valueIterator = representation.get(fieldName);
                Set<Object> values = new HashSet<Object>();
                while (valueIterator.hasNext()) {
                    values.add(valueIterator.next());
                }
                if (results.containsKey(fieldName)) {
                    @SuppressWarnings("unchecked")
                    Collection<Object> resultCollection = (Collection<Object>) results.get(fieldName);
                    Collection<Object> tmpCol = (Collection<Object>) values;
                    for (Object o : tmpCol) {
                        resultCollection.add(o);
                    }
                } else {
                    results.put(fieldName, values);
                }
            }
        }

        // execute the LDPath on the given ContentItem
        ContentItemBackend contentItemBackend = new ContentItemBackend(ci, true);
        LDPath<Resource> resourceLDPath = new LDPath<Resource>(contentItemBackend, EnhancerLDPath.getConfig());
        Program<Resource> resourceProgram;
        try {
            resourceProgram = resourceLDPath.parseProgram(new StringReader(this.ldPathProgram));
            Map<String,Collection<?>> ciBackendResults = resourceProgram.execute(contentItemBackend,
                ci.getUri());
            for (Entry<String,Collection<?>> result : ciBackendResults.entrySet()) {
                if (results.containsKey(result.getKey())) {
                    @SuppressWarnings("unchecked")
                    Collection<Object> resultsValue = (Collection<Object>) results.get(result.getKey());
                    resultsValue.addAll(result.getValue());
                } else {
                    results.put(result.getKey(), result.getValue());
                }

            }
        } catch (LDPathParseException e) {
            logger.error("Failed to create Program<Resource> from the LDPath program", e);
        }

        return results;
    }

    /**
     * Starts to poll the changes from the associated {@link IndexingSource} using a
     * {@link IndexingSourceUpdateChecker}
     */
    protected void startIndexingSourceCheckThread() {
        pollingThread = new Thread(new IndexingSourceUpdateChecker(), "IndexingSourceUpdateChecker");
        pollingThread.start();
    }

    /**
     * Separate thread to poll changes in the {@link IndexingSource}
     * 
     * @author meric
     * 
     */
    private class IndexingSourceUpdateChecker implements Runnable {
        @Override
        public void run() {
            while (!deactivate) {
                logger.info("Pooling thread for index: {} will check the changes", name);
                // if the polling thread is interrupted i.e the parent index component is deactivated,
                // stop polling
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }

                ChangeSet changeSet = null;
                try {
                    changeSet = indexingSource.changes(epoch, revision, batchSize);
                } catch (StoreException e) {
                    logger.error(
                        String.format(
                            "Failed to get changes from FileRevisionManager with start revision: %s and batch size: %s for IndexingSource: %s",
                            revision, batchSize, indexingSource.getName()), e);
                } catch (EpochException e) {
                    if (e.getActiveEpoch() > e.getRequestEpoch()) {
                        // epoch of the IndexingSource has increased. So, a reindexing is needed.
                        // Start the reindexing thread and terminate this one
                        logger.info(
                            "Epoch of the IndexingSource: {} has increase. So, a reindexing will be in progress",
                            indexingSource.getName());
                        epoch = e.getActiveEpoch();
                        state = IndexState.REINDEXING;
                        startReindexing();
                        return;
                    }
                }
                if (changeSet != null) {
                    Iterator<String> changedItems = changeSet.iterator();
                    boolean persist = true;
                    while (changedItems.hasNext()) {
                        String changedItem = changedItems.next();
                        ContentItem ci;
                        try {
                            ci = indexingSource.get(changedItem);
                            if (ci != null) {
                                index(ci);
                                logger.info("ContentItem with Uri {} is indexed to {}", changedItem, name);
                            } else {
                                remove(changedItem);
                            }

                        } catch (StoreException e) {
                            logger.error("Failed to retrieve contentitem with uri: {}", changedItem);
                            persist = false;
                            break;
                        } catch (IndexException e) {
                            logger.error("Failed to index contentitem with uri: {}", changedItem);
                            persist = false;
                            break;
                        }
                    }
                    if (persist) {
                        try {
                            if (changeSet.iterator().hasNext()) {
                                persist(changeSet.toRevision());
                            }
                        } catch (IndexException e) {
                            logger.error("Index revision cannot be persist", e);
                        }
                    }
                }
                try {
                    Thread.sleep(1000 * indexingSourceCheckPeriod);
                } catch (InterruptedException e) {
                    logger.info(
                        "Indexing Source Checker for index: {} is interrupted while sleeping. Closing the thread",
                        name);
                    return;
                }
            }
        }
    }
}

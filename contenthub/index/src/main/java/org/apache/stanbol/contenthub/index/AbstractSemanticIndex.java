package org.apache.stanbol.contenthub.index;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Dictionary;
import java.util.Iterator;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.stanbol.commons.semanticindex.index.IndexException;
import org.apache.stanbol.commons.semanticindex.index.IndexManagementException;
import org.apache.stanbol.commons.semanticindex.index.IndexState;
import org.apache.stanbol.commons.semanticindex.index.SemanticIndex;
import org.apache.stanbol.commons.semanticindex.store.ChangeSet;
import org.apache.stanbol.commons.semanticindex.store.EpochException;
import org.apache.stanbol.commons.semanticindex.store.IndexingSource;
import org.apache.stanbol.commons.semanticindex.store.StoreException;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.entityhub.core.utils.OsgiUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Properties(value = {
                     @Property(name = SemanticIndex.PROP_NAME),
                     @Property(name = SemanticIndex.PROP_DESCRIPTION),
                     @Property(name = AbstractSemanticIndex.PROP_BATCH_SIZE, intValue = 10),
                     @Property(name = AbstractSemanticIndex.PROP_INDEXING_SOURCE_NAME, value = "contenthubFileStore"),
                     @Property(name = AbstractSemanticIndex.PROP_INDEXING_SOURCE_CHECK_PERIOD, intValue = 10),
                     @Property(name = Constants.SERVICE_RANKING, intValue = 0)})
public abstract class AbstractSemanticIndex implements SemanticIndex<ContentItem> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractSemanticIndex.class);

    public static final String PROP_INDEXING_SOURCE_CHECK_PERIOD = "org.apache.stanbol.contenthub.index.AbstractSemanticIndex.indexingSourceCheckPeriod";
    public static final String PROP_INDEXING_SOURCE_NAME = "org.apache.stanbol.contenthub.index.AbstractSemanticIndex.indexingSourceName";
    public static final String PROP_BATCH_SIZE = "org.apache.stanbol.contenthub.index.AbstractSemanticIndex.batchSize";

    protected String name;
    protected String description;
    protected long revision = Long.MIN_VALUE;
    protected volatile IndexState state = IndexState.UNINIT;
    protected int batchSize;
    protected long epoch;
    private volatile Boolean deactivate = new Boolean(false);
    protected int indexingSourceCheckPeriod;
    protected IndexingSource<ContentItem> indexingSource;
    protected String pid;
    protected ComponentContext componentContext;
    private Integer indexingCount;
    // store update check thread
    private Thread pollingThread;
    // reindexer thread
    protected Thread reindexerThread;

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
        this.description = (String) OsgiUtils.checkProperty(properties, SemanticIndex.PROP_DESCRIPTION);
        this.batchSize = (Integer) OsgiUtils.checkProperty(properties, AbstractSemanticIndex.PROP_BATCH_SIZE);
        this.indexingSourceCheckPeriod = (Integer) OsgiUtils.checkProperty(properties,
            AbstractSemanticIndex.PROP_INDEXING_SOURCE_CHECK_PERIOD);
        this.componentContext = componentContext;
        this.indexingCount = 0;
        this.pid = (String) properties.get(Constants.SERVICE_PID);

        initializeIndexingSource(
            (String) OsgiUtils.checkProperty(properties, AbstractSemanticIndex.PROP_INDEXING_SOURCE_NAME),
            componentContext);
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
                throw new ConfigurationException(AbstractSemanticIndex.PROP_INDEXING_SOURCE_NAME,
                        "There is no IndexingSource<ContentItem> for the given IndexingSource name: "
                                + indexingSourceName);
            }
        } catch (InvalidSyntaxException e) {
            // ignore as there is no filter
        }
    }

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
     * Starts to poll the changes from the associated {@link IndexingSource} using a
     * {@link IndexingSourceUpdateChecker}
     */
    protected void startIndexingSourceCheckThread() {
        pollingThread = new Thread(new IndexingSourceUpdateChecker(), "IndexingSourceUpdateChecker");
        pollingThread.start();
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
     * Separate thread to poll changes in the {@link IndexingSource}
     * 
     * @author meric
     * 
     */
    private class IndexingSourceUpdateChecker implements Runnable {
        @Override
        public void run() {
            while (!deactivate) {
                logger.debug("Polling thread for index: {} will check the changes", name);
                // if the polling thread is interrupted i.e the parent index
                // component is deactivated,
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
                        // epoch of the IndexingSource has increased. So, a
                        // reindexing is needed.
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

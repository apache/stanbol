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

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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
import org.apache.commons.io.IOUtils;
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
import org.apache.stanbol.commons.solr.IndexReference;
import org.apache.stanbol.commons.solr.RegisteredSolrServerTracker;
import org.apache.stanbol.commons.solr.managed.IndexMetadata;
import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.apache.stanbol.contenthub.servicesapi.index.EndpointType;
import org.apache.stanbol.contenthub.servicesapi.index.IndexException;
import org.apache.stanbol.contenthub.servicesapi.index.IndexManagementException;
import org.apache.stanbol.contenthub.servicesapi.index.IndexState;
import org.apache.stanbol.contenthub.servicesapi.index.SemanticIndex;
import org.apache.stanbol.contenthub.servicesapi.index.search.featured.FeaturedSearch;
import org.apache.stanbol.contenthub.servicesapi.index.search.solr.SolrSearch;
import org.apache.stanbol.contenthub.servicesapi.store.ChangeSet;
import org.apache.stanbol.contenthub.servicesapi.store.Store;
import org.apache.stanbol.contenthub.servicesapi.store.StoreException;
import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary;
import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary.SolrFieldName;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.NoSuchPartException;
import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.core.utils.OsgiUtils;
import org.apache.stanbol.entityhub.ldpath.EntityhubLDPath;
import org.apache.stanbol.entityhub.ldpath.backend.SiteManagerBackend;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
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

import at.newmedialab.ldpath.exception.LDPathParseException;
import at.newmedialab.ldpath.model.fields.FieldMapping;
import at.newmedialab.ldpath.model.programs.Program;

/**
 * LDPath based {@link SemanticIndex} implementation. This implementations creates the underlying Solr core by
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
 * <li><b>Batch Size:</b> Maximum number of changes to be processed in a single step while iteratively
 * checking the changes in the {@link Store}</li>
 * <li><b>Store Check Period:</b> Time to check changes in the {@link Store} in second units</li>
 * <li><b>Solr Server Check Time:</b> Maximum time in seconds to wait for the availability of the Solr core
 * associated with this index</li>
 * <li><b>Service Ranking:</b> To be able adjust priorities of {@link SemanticIndex}es with same name or same
 * {@link EndpointType}, this property is used. The higher value of this property, the higher priority of the
 * {@link SemanticIndex} instance.</li>
 * </ul>
 * 
 * @author suat
 * @author meric
 * 
 */
@Component(configurationFactory = true, policy = ConfigurationPolicy.REQUIRE, metatype = true, immediate = true)
@Service
@Properties(value = {@Property(name = SemanticIndex.PROP_NAME),
                     @Property(name = SemanticIndex.PROP_DESCRIPTION),
                     @Property(name = LDPathSemanticIndex.PROP_LD_PATH_PROGRAM),
                     @Property(name = LDPathSemanticIndex.PROP_INDEX_CONTENT, boolValue = true),
                     @Property(name = LDPathSemanticIndex.PROP_BATCH_SIZE, intValue = 10),
                     @Property(name = LDPathSemanticIndex.PROP_STORE_CHECK_PERIOD, intValue = 10),
                     @Property(name = LDPathSemanticIndex.PROP_SOLR_CHECK_TIME, intValue = 5),
                     @Property(name = Constants.SERVICE_RANKING, intValue = 0)})
public class LDPathSemanticIndex implements SemanticIndex {

    public static final String PROP_LD_PATH_PROGRAM = "org.apache.stanbol.contenthub.index.ldpath.LDPathSemanticIndex.ldPathProgram";
    public static final String PROP_INDEX_CONTENT = "org.apache.stanbol.contenthub.index.ldpath.LDPathSemanticIndex.indexContent";
    public static final String PROP_BATCH_SIZE = "org.apache.stanbol.contenthub.index.ldpath.LDPathSemanticIndex.batchSize";
    public static final String PROP_STORE_CHECK_PERIOD = "org.apache.stanbol.contenthub.index.ldpath.LDPathSemanticIndex.storeCheckPeriod";
    public static final String PROP_SOLR_CHECK_TIME = "org.apache.stanbol.contenthub.index.ldpath.LDPathSemanticIndex.solrCheckTime";

    private final Logger logger = LoggerFactory.getLogger(LDPathSemanticIndex.class);

    @Reference
    private LDPathSemanticIndexManager ldpathSemanticIndexManager;

    @Reference(target = "(org.apache.solr.core.CoreContainer.name=contenthub)")
    private ManagedSolrServer managedSolrServer;

    @Reference
    private Store store;

    @Reference
    private SiteManager siteManager;

    private String name;
    private String description;
    private String ldPathProgram;
    private Program<Object> program;
    private boolean indexContent;
    private int batchSize;
    private int storeCheckPeriod;
    private int solrCheckTime;
    private long revision = Long.MIN_VALUE;
    private volatile IndexState state = IndexState.UNINIT;
    private String pid;
    private RegisteredSolrServerTracker registeredServerTracker;
    private ComponentContext componentContext;
    private Integer indexingCount;
    // store update check thread
    private Thread pollingThread;
    private volatile Boolean deactivate = new Boolean(false);
    // reindexer thread
    private Thread reindexerThread;

    @Activate
    protected void activate(ComponentContext context) throws IndexException,
                                                     IndexManagementException,
                                                     ConfigurationException {
        @SuppressWarnings("rawtypes")
        Dictionary properties = context.getProperties();
        this.name = (String) OsgiUtils.checkProperty(properties, PROP_NAME);
        this.ldPathProgram = (String) OsgiUtils.checkProperty(properties, PROP_LD_PATH_PROGRAM);
        this.program = getProgram();
        this.indexContent = (Boolean) OsgiUtils.checkProperty(properties, PROP_INDEX_CONTENT);
        this.description = (String) OsgiUtils.checkProperty(properties, PROP_DESCRIPTION);
        this.batchSize = (Integer) OsgiUtils.checkProperty(properties, PROP_BATCH_SIZE);
        this.storeCheckPeriod = (Integer) OsgiUtils.checkProperty(properties, PROP_STORE_CHECK_PERIOD);
        this.solrCheckTime = (Integer) OsgiUtils.checkProperty(properties, PROP_SOLR_CHECK_TIME);
        this.componentContext = context;
        this.indexingCount = 0;
        // create index if it is not already done. When an instance of this component is created through the
        // REST/Java services first the Solr core initalized and then the associated OSGI component is
        // activated
        this.pid = (String) properties.get(Constants.SERVICE_PID);
        if (!ldpathSemanticIndexManager.isConfigured(pid)) {
            // solr core has not been created. create now...
            logger.info("New Solr core will be created for the Semantic Index: {}", this.name);
            ldpathSemanticIndexManager.createIndex(getConfigProperties());
            this.state = IndexState.ACTIVE;

        } else {
            // solr core has already been created or the semantic index component is re-activated second or
            // more

            // check the configuration has changed
            java.util.Properties oldMetadata = ldpathSemanticIndexManager.getIndexMetadata(pid);
            if (checkIndexConfiguration(name, ldPathProgram, pid, oldMetadata)) {
                logger.info(
                    "LDPath program of the Semantic Index: {} has been changed. Reindexing will start now...",
                    this.name);
                // ldpath has changed, reindexing is needed
                this.state = IndexState.REINDEXING;

                this.reindexerThread = new Thread(new Reindexer());
                this.reindexerThread.start();

            } else {
                if (oldMetadata.get(SemanticIndex.PROP_REVISION) != null) {
                    // load revision of the index and update the index state
                    this.revision = Long.parseLong((String) oldMetadata.get(SemanticIndex.PROP_REVISION));
                    this.state = IndexState.valueOf(oldMetadata.getProperty(SemanticIndex.PROP_STATE));
                } else {
                    // newly created index, store the metadata. Index was created through the REST/Java
                    // services
                    this.state = IndexState.ACTIVE;
                }
            }
        }

        if (this.state != IndexState.REINDEXING) {
            // start tracking for the Solr core
            initializeTracker(this.name);

            // start polling the changes in the store
            startStoreCheckThread();
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

    /**
     * Compares the new configuration (metadata) of this SemanticIndex with the old one. The old metadata is
     * obtained by using the pid of the this SemanticIndex through
     * {@link LDPathSemanticIndexManager#getIndexMetadata(pid)}. If the name has changed an exception is
     * thrown, if the LDPath program has changed the {@link Reindexer} thread is activated.
     * 
     * @param name
     *            new name of the SemanticIndex
     * @param ldPath
     *            new LDPath program of the SemanticIndex
     * @param pid
     *            unique pid of the SemanticIndex
     * @param oldMetadata
     *            old metadata of the SemanticIndex
     * @return {@code true} if the LDPath program has changed, otherwise {@code false}
     * @throws ConfigurationException
     */
    private boolean checkIndexConfiguration(String name,
                                            String ldPath,
                                            String pid,
                                            java.util.Properties oldMetadata) throws ConfigurationException {

        // name of the semantic index has changed
        if (!name.equals(oldMetadata.get(PROP_NAME))) {
            throw new ConfigurationException(PROP_NAME,
                    "It is not allowed to change the name of a Semantic Index");
        }

        // ldpath of the semantic has changed, reindexing needed
        if (!ldPath.equals(oldMetadata.get(PROP_LD_PATH_PROGRAM))) {
            return true;
        }
        return false;
    }

    private Program<Object> getProgram() throws IndexException, IndexManagementException {
        SiteManagerBackend backend = new SiteManagerBackend(siteManager);
        ValueFactory vf = InMemoryValueFactory.getInstance();
        EntityhubLDPath ldPath = new EntityhubLDPath(backend, vf);
        Program<Object> program = null;
        try {
            program = ldPath.parseProgram(LDPathUtils.constructReader(this.ldPathProgram));
        } catch (LDPathParseException e) {
            logger.error("Should never happen!!!!!", e);
            throw new IndexException("Failed to create Program from the parsed LDPath", e);
        }
        return program;
    }

    @Override
    public String getName() {
        return this.name;
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
            if (this.indexContent) {
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
        Map<String,Collection<?>> results = executeProgram(contexts);
        for (Entry<String,Collection<?>> entry : results.entrySet()) {
            doc.addField(entry.getKey(), entry.getValue());
        }

        JSONObject constraints = getConstraints(ci);
        if (constraints != null) {
            @SuppressWarnings("unchecked")
            Iterator<String> keyIterator = constraints.keys();
            while (keyIterator.hasNext()) {
                String key = keyIterator.next();
                Iterator<FieldMapping<?,Object>> fieldIt = this.program.getFields().iterator();
                boolean contains = false;
                while (fieldIt.hasNext()) {
                    FieldMapping<?,Object> fm = fieldIt.next();
                    if (fm.getFieldName().equals(key)) {
                        contains = true;
                    }
                }

                String solrFieldName = key;
                if (!contains) {
                    // field passed from the constraints does not included in the schema, create a dynamic
                    // field

                    try {
                        Object values = constraints.get(key);
                        if (values instanceof JSONArray) {
                            solrFieldName = addSolrDynamicFieldExtension(key,
                                ((JSONArray) values).getString(0));
                            for (int i = 0; i < ((JSONArray) values).length(); i++) {
                                doc.addField(solrFieldName, ((JSONArray) values).getString(i));
                            }
                        } else {
                            logger.warn("Values for the key: {} is not a JSONArray");
                        }
                    } catch (JSONException e) {
                        logger.error(
                            String.format(
                                "Failed to add field: %s to the Solr document while indexing the Content Item: %s",
                                key, ci.getUri()), e);
                        continue;
                    }
                }
            }
        } else {
            logger.debug("No additional constraint while indexing the Content Item: {}", ci.getUri());
        }
        return doc;
    }

    private JSONObject getConstraints(ContentItem ci) {
        JSONObject constraints = null;
        try {
            Blob constraintsPart = ci.getPart(new UriRef("org.apache.stanbol.contenthub.constraints"),
                Blob.class);
            if (constraintsPart != null) {
                try {
                    constraints = new JSONObject(IOUtils.toString(constraintsPart.getStream()));
                } catch (JSONException e) {
                    logger.error("Failed to parse constraints of content item: {}", ci.getUri(), e);
                } catch (IOException e) {
                    logger.error("Failed to parse constraints of content item: {}", ci.getUri(), e);
                }
            }
        } catch (NoSuchPartException e) {
            // ignore the exception. no constraint case is handled in the caller method(getSolrDocument)
        }
        return constraints;
    }

    private Object inferObjectType(Object val) {
        Object ret = null;
        try {
            ret = DateFormat.getInstance().parse(val.toString());
        } catch (Exception e) {
            try {
                ret = Long.valueOf(val.toString());
            } catch (Exception e1) {
                try {
                    ret = Double.valueOf(val.toString());
                } catch (Exception e2) {
                    try {
                        ret = String.valueOf(val.toString());
                    } catch (Exception e3) {}
                }
            }
        }

        if (ret == null) ret = val;
        return ret;
    }

    private String addSolrDynamicFieldExtension(String fieldName, String strValue) {
        Object typed = inferObjectType(strValue);
        String dynamicFieldName = fieldName;
        if (typed instanceof String) {
            dynamicFieldName += SolrVocabulary.SOLR_DYNAMIC_FIELD_TEXT;
        } else if (typed instanceof Long) {
            dynamicFieldName += SolrVocabulary.SOLR_DYNAMIC_FIELD_LONG;
        } else if (typed instanceof Double) {
            dynamicFieldName += SolrVocabulary.SOLR_DYNAMIC_FIELD_DOUBLE;
        } else if (typed instanceof Date) {
            dynamicFieldName += SolrVocabulary.SOLR_DYNAMIC_FIELD_DATE;
        }
        return dynamicFieldName;
    }

    @Override
    public void remove(UriRef ciURI) throws IndexException {
        if (this.state == IndexState.REINDEXING) {
            throw new IndexException(String.format(
                "The index '%s' is read-only as it is in reindexing state.", name));
        }

        semUp();
        try {
            performRemove(ciURI);
        } finally {
            semDown();
        }
    }

    private void performRemove(UriRef ciURI) throws IndexException {
        if (ciURI == null || ciURI.getUnicodeString().trim().isEmpty()) {
            return;
        }

        SolrServer solrServer = null;
        try {
            solrServer = getServer();
            solrServer.deleteById(ciURI.getUnicodeString());
            solrServer.commit();
            logger.info("Given Uri {} is removed from index successfully", ciURI.getUnicodeString());
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
    public void persist(long revision) throws IndexException {
        this.revision = revision;
        updateIndexMetadata();
    }

    @Override
    public long getRevision() {
        return this.revision;
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
    public Map<EndpointType,String> getRESTSearchEndpoints() {
        Map<EndpointType,String> searchEndpoints = new HashMap<EndpointType,String>();
        searchEndpoints.put(EndpointType.CONTENTHUB, "contenthub/" + this.name + "/search/featured");
        searchEndpoints.put(EndpointType.SOLR, "solr/" + managedSolrServer.getServerName() + "/" + this.name);
        return searchEndpoints;
    }

    @Override
    public Map<Class<?>,ServiceReference> getSearchEndPoints() {
        BundleContext bundleContext = this.componentContext.getBundleContext();
        Map<Class<?>,ServiceReference> serviceEndPoints = new HashMap<Class<?>,ServiceReference>();
        ServiceReference serviceReference = null;

        serviceReference = bundleContext.getServiceReference(SolrSearch.class.getName());
        if (serviceReference != null) {
            serviceEndPoints.put(SolrSearch.class, serviceReference);
        }
        serviceReference = bundleContext.getServiceReference(FeaturedSearch.class.getName());
        if (serviceReference != null) {
            serviceEndPoints.put(FeaturedSearch.class, serviceReference);
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

    /**
     * This method executes the LDPath program, which was used to configure this index, on the enhancements of
     * submitted content by means of the Entityhub. In other words, additional information is gathered from
     * the Entityhub for each entity detected in the enhancements by querying the ldpath of this index.
     * 
     * @param contexts
     *            a {@link Set} of URIs (string representations) that are used as starting nodes to execute
     *            LDPath program of this index. The context are the URIs of the entities detected in the
     *            enhancements of the content submitted.
     * @return the {@link Map} containing the results obtained by executing the given program on the given
     *         contexts. Keys of the map corresponds to fields in the program and values of the map
     *         corresponds to results obtained for the field specified in the key.
     * @throws IndexManagementException
     */
    private Map<String,Collection<?>> executeProgram(Set<String> contexts) throws IndexManagementException {
        Map<String,Collection<?>> results = new HashMap<String,Collection<?>>();
        SiteManagerBackend backend = new SiteManagerBackend(siteManager);
        ValueFactory vf = InMemoryValueFactory.getInstance();
        EntityhubLDPath ldPath = new EntityhubLDPath(backend, vf);
        Representation representation;
        for (String context : contexts) {
            representation = ldPath.execute(vf.createReference(context), this.program);
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
        return results;
    }

    private void updateIndexMetadata() throws IndexException {
        // java.util.Properties properties = ldpathSemanticIndexManager.getIndexMetadata(this.pid);
        // properties.put(PROP_REVISION, this.revision);
        // properties.put(PROP_STATE, this.state.name());
        java.util.Properties properties = getConfigProperties();
        try {
            ldpathSemanticIndexManager.updateIndexMetadata(this.pid, properties);
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
        propertiesSubset.put(PROP_LD_PATH_PROGRAM, properties.get(PROP_LD_PATH_PROGRAM));
        propertiesSubset.put(PROP_INDEX_CONTENT, properties.get(PROP_INDEX_CONTENT));
        propertiesSubset.put(PROP_BATCH_SIZE, properties.get(PROP_BATCH_SIZE));
        propertiesSubset.put(PROP_SOLR_CHECK_TIME, properties.get(PROP_SOLR_CHECK_TIME));
        propertiesSubset.put(PROP_STORE_CHECK_PERIOD, properties.get(PROP_STORE_CHECK_PERIOD));
        propertiesSubset.put(Constants.SERVICE_PID, properties.get(Constants.SERVICE_PID));
        propertiesSubset.put(Constants.SERVICE_RANKING, properties.get(Constants.SERVICE_RANKING));
        propertiesSubset.put(PROP_REVISION, this.revision);
        propertiesSubset.put(PROP_STATE, this.state.name());
        return propertiesSubset;
    }

    private void semUp() throws IndexException {
        synchronized (this.indexingCount) {
            this.indexingCount++;
            this.state = IndexState.INDEXING;
            updateIndexMetadata();
        }
    }

    private void semDown() throws IndexException {
        synchronized (this.indexingCount) {
            this.indexingCount--;
            if (this.indexingCount == 0) {
                this.state = IndexState.ACTIVE;
                updateIndexMetadata();
            }
        }
    }

    private void startStoreCheckThread() {
        pollingThread = new Thread(new StoreUpdateChecker(), "StoreUpdateChecker");
        pollingThread.start();
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        // close store check thread and solr core tracker
        deactivate = true;
        if (pollingThread != null) {
            pollingThread.interrupt();
        }
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
            config = configAdmin.getConfiguration(pid);
            // if the configuration for the index has been removed, clear all files for this index
            if (config.getProperties() == null) {
                logger.info(
                    "Configuration for the Semantic Index: {} has been deleted. All resources will be removed.",
                    this.name);
                managedSolrServer.removeIndex(this.name, true);
            } // the index is deactivated. do not nothing.
        } catch (IOException e) {
            logger.error("Failed to obtain configuration for the Semantic Index: {}.", this.name, e);
        }
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
            IndexMetadata temporaryCoreMetadata;
            try {
                temporaryCoreMetadata = createTemporarySolrCore();
                logger.info(
                    "Temporary solr core: {} has been created for reindexing of the Semantic Index: {}",
                    temporaryCoreMetadata.getIndexName(), name);
            } catch (IndexManagementException e) {
                logger.error("Failed to create temporary Solr core while reindexing the index: {}", name, e);
                return;
            }
            // initialize solr server tracker for the temporary core
            String temporaryCoreName = temporaryCoreMetadata.getIndexName();
            initializeTracker(temporaryCoreName);

            // index documents in the store according to the new configuration
            try {
                revision = indexDocuments();
                logger.info(
                    "Documents have been re-indexed according to the new configuration of the Semantic Index: {}",
                    name);
            } catch (StoreException e) {
                logger.error("Failed to obtain changes from Store while reindexing the index: {}", name, e);
                return;
            } catch (IndexException e) {
                logger.error("IndexException while reindexing the index: {}", name, e);
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
            startStoreCheckThread();
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

            return ldpathSemanticIndexManager.createSolrCore(coreName, ldPathProgram);
        }

        private long indexDocuments() throws StoreException, IndexException {
            ChangeSet cs;
            long revision = Long.MIN_VALUE;
            boolean noChange = false;
            do {
                cs = store.changes(revision, batchSize);
                for (UriRef changed : cs.changed()) {
                    ContentItem ci = store.get(changed);
                    if (ci == null) {
                        performRemove(changed);
                    } else {
                        performIndex(ci);
                    }
                }
                noChange = cs.changed().isEmpty() ? true : false;
                if (!noChange) {
                    revision = cs.toRevision();
                }
            } while (!noChange);
            return revision;
        }
    }

    /**
     * Separate thread to poll changes in the {@link Store}
     * 
     * @author meric
     * 
     */
    private class StoreUpdateChecker implements Runnable {
        @Override
        public void run() {
            while (!deactivate) {
                logger.debug("Pooling thread for index: {} will check the changes", name);
                // if the polling thread is interrupted i.e the parent index component is deactivated,
                // stop polling
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }

                ChangeSet changeSet = null;
                try {
                    changeSet = store.changes(revision, batchSize);
                } catch (StoreException e) {
                    logger.error(
                        "Failed to get changes from FileRevisionManager with start revision: {} and batch size: {}",
                        revision, batchSize);
                }
                if (changeSet != null) {
                    Iterator<UriRef> changedItems = changeSet.changed().iterator();
                    boolean persist = true;
                    while (changedItems.hasNext()) {
                        UriRef changedItem = changedItems.next();
                        ContentItem ci;
                        try {
                            ci = store.get(changedItem);
                            if (ci != null) {
                                index(ci);
                                logger.info("ContentItem with Uri {} is indexed to {}",
                                    changedItem.getUnicodeString(), name);
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
                            if (changeSet.changed().size() != 0) {
                                persist(changeSet.toRevision());
                            }
                        } catch (IndexException e) {
                            logger.error("Index revision cannot be persist to solr", e);
                        }
                    }
                }
                try {
                    Thread.sleep(1000 * storeCheckPeriod);
                } catch (InterruptedException e) {
                    logger.error(
                        "Store Checker for index: {} is interrupted while sleeping. Closing the thread", name);
                    return;
                }
            }
        }
    }
}
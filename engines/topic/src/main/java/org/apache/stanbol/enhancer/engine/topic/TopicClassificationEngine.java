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
package org.apache.stanbol.enhancer.engine.topic;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.NIE_PLAINTEXTCONTENT;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.MoreLikeThisParams;
import org.apache.stanbol.commons.solr.utils.StreamQueryRequest;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses;
import org.apache.stanbol.enhancer.topic.Batch;
import org.apache.stanbol.enhancer.topic.ClassificationReport;
import org.apache.stanbol.enhancer.topic.ClassifierException;
import org.apache.stanbol.enhancer.topic.ConfiguredSolrCoreTracker;
import org.apache.stanbol.enhancer.topic.TopicClassifier;
import org.apache.stanbol.enhancer.topic.TopicSuggestion;
import org.apache.stanbol.enhancer.topic.TrainingSet;
import org.apache.stanbol.enhancer.topic.TrainingSetException;
import org.apache.stanbol.enhancer.topic.UTCTimeStamper;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enhancement Engine that provides the ability to assign a text document to a set of topics indexed in a
 * dedicated Solr core. The assignment logic comes from terms frequencies match of the text of the document to
 * categorize with the text indexed for each topic.
 * 
 * The solr server is expected to be configured with the MoreLikeThisHandler and the matching fields from the
 * engine configuration.
 */
@Component(metatype = true, immediate = true, configurationFactory = true, policy = ConfigurationPolicy.REQUIRE)
@Service
@Properties(value = {
                     @Property(name = TopicClassificationEngine.ENGINE_ID),
                     @Property(name = TopicClassificationEngine.ORDER, intValue = 100),
                     @Property(name = TopicClassificationEngine.SOLR_CORE),
                     @Property(name = TopicClassificationEngine.LANGUAGES),
                     @Property(name = TopicClassificationEngine.SIMILARTITY_FIELD),
                     @Property(name = TopicClassificationEngine.TOPIC_URI_FIELD),
                     @Property(name = TopicClassificationEngine.BROADER_FIELD),
                     @Property(name = TopicClassificationEngine.MODEL_UPDATE_DATE_FIELD, value = "last_update_dt"),
                     @Property(name = TopicClassificationEngine.PRECISION_FIELD, value = "precision"),
                     @Property(name = TopicClassificationEngine.RECALL_FIELD, value = "recall"),
                     @Property(name = TopicClassificationEngine.F1_FIELD, value = "f1"),
                     @Property(name = TopicClassificationEngine.MODEL_ENTRY_ID_FIELD, value = "model_entry_id"),
                     @Property(name = TopicClassificationEngine.MODEL_EVALUATION_DATE_FIELD, value = "last_evaluation_dt"),
                     @Property(name = TopicClassificationEngine.FALSE_NEGATIVES_FIELD, value = "false_negatives"),
                     @Property(name = TopicClassificationEngine.FALSE_POSITIVES_FIELD, value = "false_positives"),
                     @Property(name = TopicClassificationEngine.POSITIVE_SUPPORT_FIELD, value = "positive_support"),
                     @Property(name = TopicClassificationEngine.NEGATIVE_SUPPORT_FIELD, value = "negative_support")})
public class TopicClassificationEngine extends ConfiguredSolrCoreTracker implements EnhancementEngine,
        ServiceProperties, TopicClassifier {

    public static final String MODEL_ENTRY = "model";

    public static final String METADATA_ENTRY = "metadata";

    public static final String ENGINE_ID = "org.apache.stanbol.enhancer.engine.id";

    public static final String SOLR_CORE = "org.apache.stanbol.enhancer.engine.topic.solrCore";

    public static final String LANGUAGES = "org.apache.stanbol.enhancer.engine.topic.languages";

    public static final String ORDER = "org.apache.stanbol.enhancer.engine.topic.order";

    public static final String ENTRY_ID_FIELD = "org.apache.stanbol.enhancer.engine.topic.entryIdField";

    public static final String ENTRY_TYPE_FIELD = "org.apache.stanbol.enhancer.engine.topic.entryTypeField";

    public static final String SIMILARTITY_FIELD = "org.apache.stanbol.enhancer.engine.topic.similarityField";

    public static final String TOPIC_URI_FIELD = "org.apache.stanbol.enhancer.engine.topic.uriField";

    public static final String BROADER_FIELD = "org.apache.stanbol.enhancer.engine.topic.broaderField";

    public static final String MODEL_UPDATE_DATE_FIELD = "org.apache.stanbol.enhancer.engine.topic.modelUpdateDateField";

    public static final String MODEL_EVALUATION_DATE_FIELD = "org.apache.stanbol.enhancer.engine.topic.modelEvaluationDateField";

    public static final String MODEL_ENTRY_ID_FIELD = "org.apache.stanbol.enhancer.engine.topic.modelEntryIdField";

    public static final String PRECISION_FIELD = "org.apache.stanbol.enhancer.engine.topic.precisionField";

    public static final String RECALL_FIELD = "org.apache.stanbol.enhancer.engine.topic.recallField";

    public static final String F1_FIELD = "org.apache.stanbol.enhancer.engine.topic.f1Field";

    public static final String FALSE_POSITIVES_FIELD = "org.apache.stanbol.enhancer.engine.topic.falsePositivesField";

    public static final String FALSE_NEGATIVES_FIELD = "org.apache.stanbol.enhancer.engine.topic.falseNegativesField";

    public static final String POSITIVE_SUPPORT_FIELD = "org.apache.stanbol.enhancer.engine.topic.positiveSupportField";

    public static final String NEGATIVE_SUPPORT_FIELD = "org.apache.stanbol.enhancer.engine.topic.negativeSupportField";

    private static final Logger log = LoggerFactory.getLogger(TopicClassificationEngine.class);

    // TODO: make the following bounds configurable

    public int MAX_CHARS_PER_TOPIC = 100000;

    public Integer MAX_ROOTS = 1000;

    protected String engineId;

    protected List<String> acceptedLanguages;

    protected Integer order = ORDERING_EXTRACTION_ENHANCEMENT;

    protected String similarityField;

    protected String topicUriField;

    protected String broaderField;

    protected String modelUpdateDateField;

    protected String modelEvaluationDateField;

    protected String precisionField;

    protected String recallField;

    protected String f1Field;

    protected int numTopics = 10;

    protected TrainingSet trainingSet;

    // the ENTRY_*_FIELD are basically a hack to use a single Solr core to make documents with partially
    // updateable stored fields: the logical document is splitted into two parts joined by entryIdField. The
    // first part has entryTypeField field with value METADATA_ENTRY and the second half has entryTypeField
    // with value MODEL_ENTRY.
    // The logical primary key stays the topic id.
    protected String entryIdField;

    protected String entryTypeField;

    protected String modelEntryIdField;

    protected String positiveSupportField;

    protected String negativeSupportField;

    protected String falsePositivesField;

    protected String falseNegativesField;

    // customize the behavior of the classifier instance for model evaluation
    protected int cvFoldIndex = 0;

    protected int cvFoldCount = 0;

    protected File evaluationFolder;

    @Activate
    protected void activate(ComponentContext context) throws ConfigurationException, InvalidSyntaxException {
        @SuppressWarnings("unchecked")
        Dictionary<String,Object> config = context.getProperties();
        this.context = context;
        configure(config);
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        if (indexTracker != null) {
            indexTracker.close();
        }
    }

    public void configure(Dictionary<String,Object> config) throws ConfigurationException {
        engineId = getRequiredStringParam(config, ENGINE_ID);
        entryIdField = getRequiredStringParam(config, ENTRY_ID_FIELD);
        modelEntryIdField = getRequiredStringParam(config, MODEL_ENTRY_ID_FIELD);
        topicUriField = getRequiredStringParam(config, TOPIC_URI_FIELD);
        entryTypeField = getRequiredStringParam(config, ENTRY_TYPE_FIELD);
        similarityField = getRequiredStringParam(config, SIMILARTITY_FIELD);
        acceptedLanguages = getStringListParan(config, LANGUAGES);
        precisionField = getRequiredStringParam(config, PRECISION_FIELD);
        recallField = getRequiredStringParam(config, RECALL_FIELD);
        f1Field = getRequiredStringParam(config, F1_FIELD);
        modelUpdateDateField = getRequiredStringParam(config, MODEL_UPDATE_DATE_FIELD);
        modelEvaluationDateField = getRequiredStringParam(config, MODEL_EVALUATION_DATE_FIELD);
        falsePositivesField = getRequiredStringParam(config, FALSE_POSITIVES_FIELD);
        falseNegativesField = getRequiredStringParam(config, FALSE_NEGATIVES_FIELD);
        positiveSupportField = getRequiredStringParam(config, POSITIVE_SUPPORT_FIELD);
        negativeSupportField = getRequiredStringParam(config, NEGATIVE_SUPPORT_FIELD);
        configureSolrCore(config, SOLR_CORE);

        // optional fields, can be null
        broaderField = (String) config.get(BROADER_FIELD);
        Object orderParamValue = config.get(ORDER);
        if (orderParamValue != null) {
            order = (Integer) orderParamValue;
        }
    }

    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        String text = getTextFromContentItem(ci);
        if (getActiveSolrServer() == null) {
            log.warn(String.format("Solr Core '%s' is not available.", solrCoreId));
            return CANNOT_ENHANCE;
        }
        if (text.trim().length() == 0) {
            return CANNOT_ENHANCE;
        }
        return ENHANCE_SYNCHRONOUS;
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        String text = getTextFromContentItem(ci);
        MGraph metadata = ci.getMetadata();
        List<TopicSuggestion> topics;
        try {
            topics = suggestTopics(text);
        } catch (ClassifierException e) {
            throw new EngineException(e);
        }
        for (TopicSuggestion topic : topics) {
            UriRef enhancement = EnhancementEngineHelper.createEntityEnhancement(ci, this);
            metadata.add(new TripleImpl(enhancement,
                    org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE,
                    TechnicalClasses.ENHANCER_TOPICANNOTATION));

            // add link to entity
            metadata.add(new TripleImpl(enhancement,
                    org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE,
                    new UriRef(topic.uri)));
            // TODO: make it possible to dereference and the path to the root the entities according to a
            // configuration parameter
        }
    }

    @Override
    public Map<String,Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(ENHANCEMENT_ENGINE_ORDERING,
            (Object) order));
    }

    protected String getTextFromContentItem(ContentItem ci) throws InvalidContentException {
        // Refactor the following using an adapter.
        String text = "";
        if (ci.getMimeType().startsWith("text/plain")) {
            try {
                // TODO: handle explicit charsets if any and fallback to UTF-8 if missing
                text = IOUtils.toString(ci.getStream(), "UTF-8");
            } catch (IOException e) {
                throw new InvalidContentException(this, ci, e);
            }
        } else {
            Iterator<Triple> it = ci.getMetadata().filter(ci.getUri(), NIE_PLAINTEXTCONTENT, null);
            while (it.hasNext()) {
                text += it.next().getObject();
            }
        }
        return text;
    }

    public static TopicClassificationEngine fromParameters(Dictionary<String,Object> config) throws ConfigurationException {
        TopicClassificationEngine engine = new TopicClassificationEngine();
        engine.configure(config);
        return engine;
    }

    // classifier API

    @Override
    public String getSchemeId() {
        return engineId;
    }

    @Override
    public List<String> getAcceptedLanguages() {
        return acceptedLanguages;
    }

    public List<TopicSuggestion> suggestTopics(String text) throws ClassifierException {
        List<TopicSuggestion> suggestedTopics = new ArrayList<TopicSuggestion>(numTopics);
        SolrServer solrServer = getActiveSolrServer();
        SolrQuery query = new SolrQuery();
        query.setQueryType("/" + MoreLikeThisParams.MLT);
        query.setFilterQueries(entryTypeField + ":" + MODEL_ENTRY);
        query.set(MoreLikeThisParams.MATCH_INCLUDE, false);
        query.set(MoreLikeThisParams.MIN_DOC_FREQ, 1);
        query.set(MoreLikeThisParams.MIN_TERM_FREQ, 1);
        // TODO: find a way to parse the interesting terms and report them
        // for debugging / explanation in dedicated RDF data structure.
        // query.set(MoreLikeThisParams.INTERESTING_TERMS, "details");
        query.set(MoreLikeThisParams.SIMILARITY_FIELDS, similarityField);
        query.set(CommonParams.STREAM_BODY, text);
        query.setRows(numTopics);
        query.setFields(topicUriField);
        query.setIncludeScore(true);
        try {
            StreamQueryRequest request = new StreamQueryRequest(query);
            QueryResponse response = request.process(solrServer);
            SolrDocumentList results = response.getResults();
            for (SolrDocument result : results.toArray(new SolrDocument[0])) {
                String uri = (String) result.getFirstValue(topicUriField);
                if (uri == null) {
                    throw new ClassifierException(String.format(
                        "Solr Core '%s' is missing required field '%s'.", solrCoreId, topicUriField));
                }
                Float score = (Float) result.getFirstValue("score");
                suggestedTopics.add(new TopicSuggestion(uri, score));
            }
        } catch (SolrServerException e) {
            if ("unknown handler: /mlt".equals(e.getCause().getMessage())) {
                String message = String.format("SolrServer with id '%s' for topic engine '%s' lacks"
                                               + " configuration for the MoreLikeThisHandler", solrCoreId,
                    engineId);
                throw new ClassifierException(message, e);
            } else {
                throw new ClassifierException(e);
            }
        }
        return suggestedTopics;
    }

    @Override
    public Set<String> getNarrowerTopics(String broadTopicId) throws ClassifierException {
        LinkedHashSet<String> narrowerTopics = new LinkedHashSet<String>();
        if (broaderField == null) {
            return narrowerTopics;
        }
        SolrServer solrServer = getActiveSolrServer();
        SolrQuery query = new SolrQuery(entryTypeField + ":" + METADATA_ENTRY);
        // use a filter query to avoid string escaping issues with special solr chars
        query.addFilterQuery("{!field f=" + broaderField + "}" + broadTopicId);
        query.addField(topicUriField);
        query.addSortField(topicUriField, SolrQuery.ORDER.asc);
        try {
            for (SolrDocument result : solrServer.query(query).getResults()) {
                narrowerTopics.add(result.getFirstValue(topicUriField).toString());
            }
        } catch (SolrServerException e) {
            String msg = String.format("Error while fetching narrower topics of '%s' on Solr Core '%s'.",
                broadTopicId, solrCoreId);
            throw new ClassifierException(msg, e);
        }
        return narrowerTopics;
    }

    @Override
    public Set<String> getBroaderTopics(String id) throws ClassifierException {
        LinkedHashSet<String> broaderTopics = new LinkedHashSet<String>();
        if (broaderField == null) {
            return broaderTopics;
        }
        SolrServer solrServer = getActiveSolrServer();
        SolrQuery query = new SolrQuery(topicUriField + ":" + ClientUtils.escapeQueryChars(id));
        query.addField(broaderField);
        try {
            for (SolrDocument result : solrServer.query(query).getResults()) {
                // there should be only one results
                Collection<Object> broaderFieldValues = result.getFieldValues(broaderField);
                if (broaderFieldValues == null) {
                    continue;
                }
                for (Object value : broaderFieldValues) {
                    broaderTopics.add(value.toString());
                }
            }
        } catch (SolrServerException e) {
            String msg = String.format("Error while fetching broader topics of '%s' on Solr Core '%s'.", id,
                solrCoreId);
            throw new ClassifierException(msg, e);
        }
        return broaderTopics;
    }

    @Override
    public Set<String> getTopicRoots() throws ClassifierException {
        LinkedHashSet<String> rootTopics = new LinkedHashSet<String>();
        SolrServer solrServer = getActiveSolrServer();
        SolrQuery query = new SolrQuery();
        // TODO: this can be very big on flat thesauri: should we enable a paging API instead?
        query.setRows(MAX_ROOTS);
        query.setFields(topicUriField);
        query.setSortField(topicUriField, SolrQuery.ORDER.asc);
        if (broaderField != null) {
            // find any topic with an empty broaderField
            query.setParam("q", entryTypeField + ":" + METADATA_ENTRY + " AND -" + broaderField
                                + ":[\"\" TO *]");
        } else {
            // find any topic
            query.setQuery(entryTypeField + ":" + METADATA_ENTRY);
        }
        try {
            QueryResponse response = solrServer.query(query);
            if (response.getResults().size() >= MAX_ROOTS) {
                log.warn(String.format("TopicClassifier '%s' has more than %d registered topic roots."
                                       + " Some roots might be ignored.", engineId, MAX_ROOTS));
            }
            for (SolrDocument result : response.getResults()) {
                rootTopics.add(result.getFirstValue(topicUriField).toString());
            }
        } catch (SolrServerException e) {
            String msg = String.format("Error while fetching root topics on Solr Core '%s'.", solrCoreId);
            throw new ClassifierException(msg, e);
        }
        return rootTopics;
    }

    @Override
    public void addTopic(String topicId, Collection<String> broaderTopics) throws ClassifierException {
        // ensure that there is no previous topic registered with the same id
        removeTopic(topicId);

        SolrInputDocument metadataEntry = new SolrInputDocument();
        String metadataEntryId = UUID.randomUUID().toString();
        String modelEntryId = UUID.randomUUID().toString();
        metadataEntry.addField(topicUriField, topicId);
        metadataEntry.addField(entryIdField, metadataEntryId);
        metadataEntry.addField(modelEntryIdField, modelEntryId);
        metadataEntry.addField(entryTypeField, METADATA_ENTRY);
        if (broaderTopics != null && broaderField != null) {
            metadataEntry.addField(broaderField, broaderTopics);
        }
        SolrInputDocument modelEntry = new SolrInputDocument();
        modelEntry.addField(entryIdField, modelEntryId);
        modelEntry.addField(topicUriField, topicId);
        modelEntry.addField(entryTypeField, MODEL_ENTRY);
        if (broaderTopics != null) {
            invalidateModelFields(broaderTopics, modelUpdateDateField, modelEvaluationDateField);
        }
        SolrServer solrServer = getActiveSolrServer();
        try {
            UpdateRequest request = new UpdateRequest();
            request.add(metadataEntry);
            request.add(modelEntry);
            solrServer.request(request);
            solrServer.commit();
        } catch (Exception e) {
            String msg = String.format("Error adding topic with id '%s' on Solr Core '%s'", topicId,
                solrCoreId);
            throw new ClassifierException(msg, e);
        }
    }

    /*
     * The commit is the responsibility of the caller.
     */
    protected void invalidateModelFields(Collection<String> topicIds, String... fieldNames) throws ClassifierException {
        if (topicIds.isEmpty() || fieldNames.length == 0) {
            return;
        }
        SolrServer solrServer = getActiveSolrServer();
        List<String> invalidatedFields = Arrays.asList(fieldNames);
        try {
            UpdateRequest request = new UpdateRequest();
            for (String topicId : topicIds) {
                SolrQuery query = new SolrQuery(entryTypeField + ":" + METADATA_ENTRY + " AND "
                                                + topicUriField + ":" + ClientUtils.escapeQueryChars(topicId));
                for (SolrDocument result : solrServer.query(query).getResults()) {
                    // there should be only one (or none: tolerated)
                    SolrInputDocument newEntry = new SolrInputDocument();
                    for (String fieldName : result.getFieldNames()) {
                        if (!invalidatedFields.contains(fieldName)) {
                            newEntry.setField(fieldName, result.getFieldValues(fieldName));
                        }
                    }
                    request.add(newEntry);
                }
            }
            solrServer.request(request);
        } catch (Exception e) {
            String msg = String.format("Error invalidating topics [%s] on Solr Core '%s'",
                StringUtils.join(topicIds, ", "), solrCoreId);
            throw new ClassifierException(msg, e);
        }
    }

    @Override
    public void removeTopic(String topicId) throws ClassifierException {
        SolrServer solrServer = getActiveSolrServer();
        try {
            solrServer.deleteByQuery(topicUriField + ":" + ClientUtils.escapeQueryChars(topicId));
            solrServer.commit();
        } catch (Exception e) {
            String msg = String.format("Error removing topic with id '%s' on Solr Core '%s'", topicId,
                solrCoreId);
            throw new ClassifierException(msg, e);
        }
    }

    @Override
    public boolean isUpdatable() {
        return trainingSet != null;
    }

    @Override
    public void setTrainingSet(TrainingSet trainingSet) {
        this.trainingSet = trainingSet;
    }

    protected int batchOverTopics(BatchProcessor<SolrDocument> processor) throws TrainingSetException {
        // TODO: implement incremental update by using the date informations
        int processedCount = 0;
        SolrServer solrServer = getActiveSolrServer();
        SolrQuery query = new SolrQuery();
        String q = entryTypeField + ":" + METADATA_ENTRY;
        String offset = null;
        boolean done = false;
        int batchSize = 1000;
        query.addSortField(topicUriField, SolrQuery.ORDER.asc);
        query.setRows(batchSize + 1);
        try {
            while (!done) {
                // batch over all the indexed topics
                if (offset != null) {
                    q += " AND " + topicUriField + ":[" + ClientUtils.escapeQueryChars(offset.toString())
                         + " TO *]";
                }
                query.setQuery(q);
                QueryResponse response = solrServer.query(query);
                int count = 0;
                List<SolrDocument> batchDocuments = new ArrayList<SolrDocument>();
                for (SolrDocument result : response.getResults()) {
                    String topicId = result.getFirstValue(topicUriField).toString();
                    if (count == batchSize) {
                        offset = topicId;
                    } else {
                        count++;
                        batchDocuments.add(result);
                    }
                }
                processedCount += processor.process(batchDocuments);
                solrServer.commit();
                if (count < batchSize) {
                    done = true;
                }
            }
            solrServer.optimize();
        } catch (Exception e) {
            String msg = String.format("Error while updating topics on Solr Core '%s'.", solrCoreId);
            throw new TrainingSetException(msg, e);
        }
        return processedCount;
    }

    @Override
    public int updateModel(boolean incremental) throws TrainingSetException, ClassifierException {
        checkTrainingSet();
        long start = System.currentTimeMillis();
        if (incremental && modelUpdateDateField == null) {
            log.warn(MODEL_UPDATE_DATE_FIELD + " field is not configured: switching to batch update mode.");
            incremental = false;
        }
        final boolean incr = incremental;
        int updatedTopics = batchOverTopics(new BatchProcessor<SolrDocument>() {
            int offset = 0;

            @Override
            public int process(List<SolrDocument> batch) throws ClassifierException, TrainingSetException {
                int processed = 0;
                for (SolrDocument result : batch) {
                    offset++;
                    if (cvFoldCount != 0 && offset % cvFoldCount == cvFoldIndex) {
                        // we are performing a cross validation session and this example belong to the test
                        // fold hence should be skipped
                        continue;
                    }
                    String topicId = result.getFirstValue(topicUriField).toString();
                    List<String> impactedTopics = new ArrayList<String>();
                    impactedTopics.add(topicId);
                    impactedTopics.addAll(getNarrowerTopics(topicId));
                    if (incr) {
                        Date lastModelUpdate = (Date) result.getFirstValue(modelUpdateDateField);
                        if (lastModelUpdate != null
                            && !trainingSet.hasChangedSince(impactedTopics, lastModelUpdate)) {
                            continue;
                        }
                    }
                    String metadataEntryId = result.getFirstValue(entryIdField).toString();
                    String modelEntryId = result.getFirstValue(modelEntryIdField).toString();
                    updateTopic(topicId, metadataEntryId, modelEntryId, impactedTopics,
                        result.getFieldValues(broaderField));
                    processed++;
                }
                return processed;
            }
        });
        long stop = System.currentTimeMillis();
        log.info("Sucessfully updated {} topics in {}s", updatedTopics, (double) (stop - start) / 1000.);
        return updatedTopics;
    }

    /**
     * @param topicId
     *            the topic model to update
     * @param metadataEntryId
     *            of the metadata entry id of the topic
     * @param modelEntryId
     *            of the model entry id of the topic
     * @param impactedTopics
     *            the list of impacted topics (e.g. the topic node and direct children)
     * @param broaderTopics
     *            the collection of broader to re-add in the broader field
     */
    protected void updateTopic(String topicId,
                               String metadataId,
                               String modelId,
                               List<String> impactedTopics,
                               Collection<Object> broaderTopics) throws TrainingSetException,
                                                                ClassifierException {
        long start = System.currentTimeMillis();
        Batch<String> examples = Batch.emtpyBatch(String.class);
        StringBuffer sb = new StringBuffer();
        do {
            examples = trainingSet.getPositiveExamples(impactedTopics, examples.nextOffset);
            for (String example : examples.items) {
                sb.append(example);
                sb.append("\n\n");
            }
        } while (sb.length() < MAX_CHARS_PER_TOPIC && examples.hasMore);

        // reindex the topic with the new text data collected from the examples
        SolrInputDocument modelEntry = new SolrInputDocument();
        modelEntry.addField(entryIdField, modelId);
        modelEntry.addField(topicUriField, topicId);
        modelEntry.addField(entryTypeField, MODEL_ENTRY);
        if (sb.length() > 0) {
            modelEntry.addField(similarityField, sb);
        }

        // update the metadata of the topic model
        SolrInputDocument metadataEntry = new SolrInputDocument();
        metadataEntry.addField(entryIdField, metadataId);
        metadataEntry.addField(modelEntryIdField, modelId);
        metadataEntry.addField(entryTypeField, METADATA_ENTRY);
        metadataEntry.addField(topicUriField, topicId);
        if (broaderTopics != null && broaderField != null) {
            metadataEntry.addField(broaderField, broaderTopics);
        }
        if (modelUpdateDateField != null) {
            metadataEntry.addField(modelUpdateDateField, UTCTimeStamper.nowUtcDate());
        }
        SolrServer solrServer = getActiveSolrServer();
        try {
            UpdateRequest request = new UpdateRequest();
            request.add(metadataEntry);
            request.add(modelEntry);
            solrServer.request(request);
            // the commit is done by the caller in batch
        } catch (Exception e) {
            String msg = String.format("Error updating topic with id '%s' on Solr Core '%s'", topicId,
                solrCoreId);
            throw new ClassifierException(msg, e);
        }
        long stop = System.currentTimeMillis();
        log.debug("Sucessfully updated topic {} in {}s", topicId, (double) (stop - start) / 1000.);
    }

    protected void checkTrainingSet() throws TrainingSetException {
        if (trainingSet == null) {
            throw new TrainingSetException(
                    String.format("TopicClassificationEngine %s has no registered"
                                  + " training set hence cannot be updated.", engineId));
        }
    }

    @Override
    public void setCrossValidationInfo(int foldIndex, int foldCount) {
        if (foldIndex > foldCount - 1) {
            throw new IllegalArgumentException(String.format(
                "foldIndex=%d should be smaller than foldCount=%d - 1", foldIndex, foldCount));
        }
        cvFoldIndex = foldIndex;
        cvFoldCount = foldCount;
    }

    protected Dictionary<String,Object> getCanonicalConfiguration(EmbeddedSolrServer server) {
        Hashtable<String,Object> config = new Hashtable<String,Object>();
        config.put(TopicClassificationEngine.ENGINE_ID, engineId + "-evaluation");
        config.put(TopicClassificationEngine.ENTRY_ID_FIELD, "entry_id");
        config.put(TopicClassificationEngine.ENTRY_TYPE_FIELD, "entry_type");
        config.put(TopicClassificationEngine.MODEL_ENTRY_ID_FIELD, "model_entry_id");
        config.put(TopicClassificationEngine.SOLR_CORE, server);
        config.put(TopicClassificationEngine.TOPIC_URI_FIELD, "topic");
        config.put(TopicClassificationEngine.SIMILARTITY_FIELD, "classifier_features");
        config.put(TopicClassificationEngine.BROADER_FIELD, "broader");
        config.put(TopicClassificationEngine.MODEL_UPDATE_DATE_FIELD, "last_update_dt");
        config.put(TopicClassificationEngine.MODEL_EVALUATION_DATE_FIELD, "last_evaluation_dt");
        config.put(TopicClassificationEngine.PRECISION_FIELD, "precision");
        config.put(TopicClassificationEngine.RECALL_FIELD, "recall");
        config.put(TopicClassificationEngine.F1_FIELD, "f1");
        config.put(TopicClassificationEngine.POSITIVE_SUPPORT_FIELD, "positive_support");
        config.put(TopicClassificationEngine.NEGATIVE_SUPPORT_FIELD, "negative_support");
        config.put(TopicClassificationEngine.FALSE_POSITIVES_FIELD, "false_positives");
        config.put(TopicClassificationEngine.FALSE_NEGATIVES_FIELD, "false_negatives");
        return config;
    }

    protected EmbeddedSolrServer makeTopicClassifierSolrServer(File folder) {
        // TODO
        return null;
    }

    public boolean isEvaluationRunning() {
        return evaluationFolder != null;
    }

    public int updatePerformanceEstimates(boolean incremental) throws ClassifierException,
                                                              TrainingSetException {
        if (evaluationFolder != null) {
            throw new ClassifierException("Another evaluation is already running");
        }
        int updatedTopics = 0;
        int cvFoldCount = 3; // 3-folds CV is hardcoded for now

        TopicClassificationEngine classifier = new TopicClassificationEngine();
        classifier.setTrainingSet(trainingSet);
        try {
            // TODO: make the temporary folder path configurable with a property
            evaluationFolder = File.createTempFile("stanbol-classifier-evaluation-", "-solr");
            for (int cvFoldIndex = 0; cvFoldIndex < cvFoldCount; cvFoldIndex++) {
                performCVFold(classifier, cvFoldIndex, cvFoldCount);
            }
        } catch (ConfigurationException e) {
            throw new ClassifierException(e);
        } catch (IOException e) {
            throw new ClassifierException(e);
        } finally {
            FileUtils.deleteQuietly(evaluationFolder);
            evaluationFolder = null;
        }
        return updatedTopics;
    }

    protected void performCVFold(final TopicClassificationEngine classifier, int cvFoldIndex, int cvFoldCount) throws ConfigurationException,
                                                                                                              TrainingSetException,
                                                                                                              ClassifierException {

        log.info(String.format("Performing evaluation CV iteration %d/%d on classifier %s", cvFoldIndex + 1,
            cvFoldCount, engineId));
        long start = System.currentTimeMillis();
        FileUtils.deleteQuietly(evaluationFolder);
        evaluationFolder.mkdir();
        EmbeddedSolrServer evaluationServer = makeTopicClassifierSolrServer(evaluationFolder);
        classifier.configure(getCanonicalConfiguration(evaluationServer));

        // iterate over all the topics to register them in the evaluation classifier
        batchOverTopics(new BatchProcessor<SolrDocument>() {
            @Override
            public int process(List<SolrDocument> batch) throws ClassifierException {
                for (SolrDocument topicEntry : batch) {
                    String topicId = topicEntry.getFirstValue(topicUriField).toString();
                    Collection<Object> broader = topicEntry.getFieldValues(broaderField);
                    if (broader == null) {
                        classifier.addTopic(topicId, null);
                    } else {
                        List<String> broaderTopics = new ArrayList<String>();
                        for (Object broaderTopic : broader) {
                            broaderTopics.add(broaderTopic.toString());
                        }
                        classifier.addTopic(topicId, broaderTopics);
                    }
                }
                return batch.size();
            }
        });

        // build the model on the for the current train CV folds
        classifier.setCrossValidationInfo(cvFoldIndex, cvFoldCount);
        classifier.updateModel(false);

        // iterate over the topics again to compute scores on the test fold
        batchOverTopics(new BatchProcessor<SolrDocument>() {
            @Override
            public int process(List<SolrDocument> batch) {
                return 0;
            }
        });

        float averageF1 = 0.0f;
        long stop = System.currentTimeMillis();
        log.info(String.format("Finished CV iteration %d/%d on classifier %s in %fs. F1-score = %f",
            cvFoldIndex + 1, cvFoldCount, engineId, (stop - start) / 1000.0, averageF1));
    }

    @Override
    public ClassificationReport getPerformanceEstimates(String topic) throws ClassifierException {

        SolrServer solrServer = getActiveSolrServer();
        SolrQuery query = new SolrQuery(entryIdField + ":" + METADATA_ENTRY + " AND " + topicUriField + ":"
                                        + ClientUtils.escapeQueryChars(topic));
        try {
            SolrDocumentList results = solrServer.query(query).getResults();
            if (results.isEmpty()) {
                throw new ClassifierException(String.format("%s is not a registered topic", topic));
            }
            SolrDocument metadata = results.get(0);
            float precision = (Float) metadata.getFirstValue(precisionField);
            float recall = (Float) metadata.getFirstValue(recallField);
            float f1 = (Float) metadata.getFirstValue(f1Field);
            int positiveSupport = (Integer) metadata.getFirstValue(positiveSupportField);
            int negativeSupport = (Integer) metadata.getFirstValue(negativeSupportField);
            Date evaluationDate = (Date) metadata.getFirstValue(modelEvaluationDateField);
            boolean uptodate = evaluationDate != null;
            ClassificationReport report = new ClassificationReport(precision, recall, f1, positiveSupport,
                    negativeSupport, uptodate, evaluationDate);
            for (Object falsePositiveId : metadata.getFieldValues(FALSE_POSITIVES_FIELD)) {
                report.falsePositiveExampleIds.add(falsePositiveId.toString());
            }
            for (Object falseNegativeId : metadata.getFieldValues(FALSE_NEGATIVES_FIELD)) {
                report.falseNegativeExampleIds.add(falseNegativeId.toString());
            }
            return report;
        } catch (SolrServerException e) {
            throw new ClassifierException(String.format("Error fetching the performance report for topic "
                                                        + topic));
        }
    }
}

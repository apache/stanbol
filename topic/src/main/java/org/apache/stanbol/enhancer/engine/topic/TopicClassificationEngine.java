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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.commons.io.IOUtils;
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
import org.apache.solr.client.solrj.response.QueryResponse;
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
import org.apache.stanbol.enhancer.topic.ClassifierException;
import org.apache.stanbol.enhancer.topic.ConfiguredSolrCoreTracker;
import org.apache.stanbol.enhancer.topic.TopicClassifier;
import org.apache.stanbol.enhancer.topic.TopicSuggestion;
import org.apache.stanbol.enhancer.topic.TrainingSet;
import org.apache.stanbol.enhancer.topic.TrainingSetException;
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
@Properties(value = {@Property(name = TopicClassificationEngine.ENGINE_ID),
                     @Property(name = TopicClassificationEngine.ORDER, intValue = 100),
                     @Property(name = TopicClassificationEngine.SOLR_CORE),
                     @Property(name = TopicClassificationEngine.LANGUAGES),
                     @Property(name = TopicClassificationEngine.SIMILARTITY_FIELD),
                     @Property(name = TopicClassificationEngine.TOPIC_URI_FIELD),
                     @Property(name = TopicClassificationEngine.BROADER_FIELD),
                     @Property(name = TopicClassificationEngine.MATERIALIZED_PATH_FIELD),
                     @Property(name = TopicClassificationEngine.MODEL_UPDATE_DATE_FIELD)})
public class TopicClassificationEngine extends ConfiguredSolrCoreTracker implements EnhancementEngine,
        ServiceProperties, TopicClassifier {

    public static final String ENGINE_ID = "org.apache.stanbol.enhancer.engine.id";

    public static final String SOLR_CORE = "org.apache.stanbol.enhancer.engine.topic.solrCore";

    public static final String LANGUAGES = "org.apache.stanbol.enhancer.engine.topic.languages";

    public static final String ORDER = "org.apache.stanbol.enhancer.engine.topic.order";

    public static final String SIMILARTITY_FIELD = "org.apache.stanbol.enhancer.engine.topic.similarityField";

    public static final String TOPIC_URI_FIELD = "org.apache.stanbol.enhancer.engine.topic.uriField";

    public static final String BROADER_FIELD = "org.apache.stanbol.enhancer.engine.topic.broaderField";

    public static final String MATERIALIZED_PATH_FIELD = "org.apache.stanbol.enhancer.engine.topic.materializedPathField";

    public static final String MODEL_UPDATE_DATE_FIELD = "org.apache.stanbol.enhancer.engine.topic.modelUpdateField";

    private static final Logger log = LoggerFactory.getLogger(TopicClassificationEngine.class);

    // TODO: make the following bounds configurable

    public int MAX_CHARS_PER_TOPIC = 100000;

    public Integer MAX_ROOTS = 1000;

    protected String engineId;

    protected List<String> acceptedLanguages;

    protected Integer order = ORDERING_EXTRACTION_ENHANCEMENT;

    protected String similarityField;

    protected String topicUriField;

    protected String modelUpdateDateField;

    protected String broaderField;

    protected String materializedPathField;

    protected int numTopics = 10;

    protected TrainingSet trainingSet;

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
        similarityField = getRequiredStringParam(config, SIMILARTITY_FIELD);
        topicUriField = getRequiredStringParam(config, TOPIC_URI_FIELD);
        acceptedLanguages = getStringListParan(config, LANGUAGES);
        configureSolrCore(config, SOLR_CORE);

        // optional fields, can be null
        broaderField = (String) config.get(BROADER_FIELD);
        materializedPathField = (String) config.get(MATERIALIZED_PATH_FIELD);
        modelUpdateDateField = (String) config.get(MODEL_UPDATE_DATE_FIELD);
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
        query.set(MoreLikeThisParams.MATCH_INCLUDE, false);
        query.set(MoreLikeThisParams.MIN_DOC_FREQ, 1);
        query.set(MoreLikeThisParams.MIN_TERM_FREQ, 1);
        // TODO: find a way to parse the interesting terms and report them
        // for debugging / explanation in dedicated RDF data structure.
        // query.set(MoreLikeThisParams.INTERESTING_TERMS, "details");
        query.set(MoreLikeThisParams.SIMILARITY_FIELDS, similarityField);
        query.set(CommonParams.STREAM_BODY, text);
        query.setRows(numTopics);
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
                suggestedTopics.add(new TopicSuggestion(uri, 0.0));
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
        SolrQuery query = new SolrQuery("*:*");
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
        SolrQuery query = new SolrQuery("*:*");
        // use a filter query to avoid string escaping issues with special solr chars
        query.addFilterQuery("{!field f=" + topicUriField + "}" + id);
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
        if (broaderField != null) {
            // find any topic with an empty broaderField
            query.setParam("q", "-" + broaderField + ":[\"\" TO *]");
        } else {
            // find any topic
            query.setQuery("*:*");
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
    public void addTopic(String id, Collection<String> broaderTopics) throws ClassifierException {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField(topicUriField, id);
        if (broaderTopics != null && broaderField != null) {
            doc.addField(broaderField, broaderTopics);
        }
        SolrServer solrServer = getActiveSolrServer();
        try {
            solrServer.add(doc);
            solrServer.commit();
        } catch (Exception e) {
            String msg = String.format("Error adding topic with id '%s' on Solr Core '%s'", id, solrCoreId);
            throw new ClassifierException(msg, e);
        }
    }

    @Override
    public void removeTopic(String id) throws ClassifierException {
        SolrServer solrServer = getActiveSolrServer();
        try {
            solrServer.deleteByQuery(topicUriField + ":" + id);
            solrServer.commit();
        } catch (Exception e) {
            String msg = String.format("Error adding topic with id '%s' on Solr Core '%s'", id, solrCoreId);
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

    @Override
    public int updateModel(boolean incremental) throws TrainingSetException, ClassifierException {
        checkTrainingSet();
        long start = System.currentTimeMillis();
        if (incremental && modelUpdateDateField == null) {
            log.warn(MODEL_UPDATE_DATE_FIELD + " field is not configured: switching to batch update mode.");
            incremental = false;
        }
        // TODO: implement incremental update by using the date informations
        int updatedTopics = 0;
        SolrServer solrServer = getActiveSolrServer();
        SolrQuery query = new SolrQuery();
        String q = "*:*";
        if (modelUpdateDateField != null) {
            query.setFields(topicUriField, broaderField);
        } else {
            query.setFields(topicUriField, broaderField, modelUpdateDateField);
        }
        String offset = null;
        boolean done = false;
        int batchSize = 1000;
        query.addSortField(topicUriField, SolrQuery.ORDER.asc);
        query.setRows(batchSize + 1);
        while (!done) {
            // batch over all the indexed topics
            try {
                if (offset != null) {
                    q += " AND " + topicUriField + ":[" + offset.toString() + " TO *]";
                }
                query.setQuery(q);
                QueryResponse response = solrServer.query(query);
                int count = 0;
                for (SolrDocument result : response.getResults()) {
                    String topicId = result.getFirstValue(topicUriField).toString();
                    if (count == batchSize) {
                        offset = topicId;
                    } else {
                        count++;
                        List<String> impactedTopics = new ArrayList<String>();
                        impactedTopics.add(topicId);
                        impactedTopics.addAll(getNarrowerTopics(topicId));
                        if (incremental) {
                            Date lastModelUpdate = (Date) result.getFirstValue(modelUpdateDateField);
                            if (lastModelUpdate != null
                                && !trainingSet.hasChangedSince(impactedTopics, lastModelUpdate)) {
                                continue;
                            }
                        }
                        updateTopic(topicId, impactedTopics, result.getFieldValues(broaderField));
                        updatedTopics++;
                    }
                }
                if (count < batchSize) {
                    done = true;
                }
                solrServer.optimize();
            } catch (Exception e) {
                String msg = String.format("Error while updating topics on Solr Core '%s'.", solrCoreId);
                throw new TrainingSetException(msg, e);
            }
        }
        long stop = System.currentTimeMillis();
        log.info("Sucessfully updated {} topics in {}s", updatedTopics, (double) (stop - start) / 1000.);
        return updatedTopics;
    }

    /**
     * @param topicId
     *            the topic model to update
     * @param impactedTopics
     *            the list of impacted topics (e.g. the topic node and direct children)
     * @param broaderTopics
     *            the collection of broader to re-add in the broader field
     */
    public void updateTopic(String topicId, List<String> impactedTopics, Collection<Object> broaderTopics) throws TrainingSetException,
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
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField(topicUriField, topicId);
        if (broaderTopics != null && broaderField != null) {
            doc.addField(broaderField, broaderTopics);
        }
        if (sb.length() > 0) {
            doc.addField(similarityField, sb);
        }
        if (modelUpdateDateField != null) {
            // TODO: force UTC timezone here
            doc.addField(modelUpdateDateField, new Date());
        }
        SolrServer solrServer = getActiveSolrServer();
        try {
            solrServer.add(doc);
            solrServer.commit();
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
}

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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
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
import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.apache.stanbol.commons.solr.utils.StreamQueryRequest;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ChainException;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;
import org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses;
import org.apache.stanbol.enhancer.topic.ConfiguredSolrCoreTracker;
import org.apache.stanbol.enhancer.topic.EmbeddedSolrHelper;
import org.apache.stanbol.enhancer.topic.UTCTimeStamper;
import org.apache.stanbol.enhancer.topic.api.Batch;
import org.apache.stanbol.enhancer.topic.api.BatchProcessor;
import org.apache.stanbol.enhancer.topic.api.ClassificationReport;
import org.apache.stanbol.enhancer.topic.api.ClassifierException;
import org.apache.stanbol.enhancer.topic.api.TopicClassifier;
import org.apache.stanbol.enhancer.topic.api.TopicSuggestion;
import org.apache.stanbol.enhancer.topic.api.training.Example;
import org.apache.stanbol.enhancer.topic.api.training.TrainingSet;
import org.apache.stanbol.enhancer.topic.api.training.TrainingSetException;
import org.apache.stanbol.enhancer.topic.training.SolrTrainingSet;
import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.EntityhubException;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enhancement Engine that provides the ability to assign a text document to a set of concepts indexed in a
 * dedicated Solr core. The assignment logic comes from terms frequencies match of the text of the document to
 * categorize with the text indexed for each concept.
 * 
 * The data model of the concept tree follows the SKOS model: concepts are organized in a hierarchical
 * "scheme" with a "broader" relation (and the inferred "narrower" inverse relation). Concepts can also
 * optionally be grounded in the real world by the mean of a foaf:primaryTopic link to an external resource
 * such as a DBpedia entry.
 * 
 * A document is typically classified with the concept by using the dct:subject property to link the document
 * (subject) to the concept (object).
 * 
 * The Solr server is expected to be configured with the MoreLikeThisHandler and the matching fields from the
 * engine configuration.
 * 
 * This text classifier method sometimes goes by the name of "Rocchio classification" or "Nearest Centroid
 * classification" in the IR and machine learning literature. It is often slightly less accurate than fitting
 * penalized linear model such as linear kernel Support Vector Machines or penalized Logistic Regression but
 * has the advantage to scale to large number of categories (e.g. more that tens of thousands) without having
 * to load the full statistical model in memory thanks to the use of the inverted index datastructure that
 * also provides the feature extraction and TF-IDF weighting for free.
 * 
 * Furthermore it could be refined by using a "Learning to Rank" approach by training a RankSVM, Gradient
 * Boosted Trees or Random Forests on the output on the raw output of the Rocchio classifier so as to re-rank
 * candidate classes more finely. The Learning to Rank refinement is not implemented yet.
 */
@Component(metatype = true, immediate = true, configurationFactory = true, policy = ConfigurationPolicy.REQUIRE)
@Service
@Properties(value = {
        @Property(name = EnhancementEngine.PROPERTY_NAME),
        @Property(name = TopicClassificationEngine.SOLR_CORE),
        @Property(name = TopicClassificationEngine.SOLR_CORE_CONFIG,
            value = TopicClassificationEngine.DEFAULT_SOLR_CORE_CONFIG),
        @Property(name = TopicClassificationEngine.LANGUAGES),
     // those properties can still be set via a configuration file, but as most users
     // will not use them exclude those from the configuration form
//             @Property(name = TopicClassificationEngine.SIMILARTITY_FIELD, value = TopicClassificationEngine.DEFAULT_SIMILARTITY_FIELD),
//             @Property(name = TopicClassificationEngine.CONCEPT_URI_FIELD, value = TopicClassificationEngine.DEFAULT_CONCEPT_URI_FIELD),
//             @Property(name = TopicClassificationEngine.PRIMARY_TOPIC_URI_FIELD, value = TopicClassificationEngine.DEFAULT_PRIMARY_TOPIC_URI_FIELD),
//             @Property(name = TopicClassificationEngine.BROADER_FIELD, value = TopicClassificationEngine.DEFAULT_BROADER_FIELD),
//             @Property(name = TopicClassificationEngine.MODEL_UPDATE_DATE_FIELD, value = TopicClassificationEngine.DEFAULT_MODEL_UPDATE_DATE_FIELD),
//             @Property(name = TopicClassificationEngine.PRECISION_FIELD, value = TopicClassificationEngine.DEFAULT_PRECISION_FIELD),
//             @Property(name = TopicClassificationEngine.RECALL_FIELD, value = TopicClassificationEngine.DEFAULT_RECALL_FIELD),
//             @Property(name = TopicClassificationEngine.ENTRY_ID_FIELD, value = TopicClassificationEngine.DEFAULT_ENTRY_ID_FIELD),
//             @Property(name = TopicClassificationEngine.MODEL_ENTRY_ID_FIELD, value = TopicClassificationEngine.DEFAULT_MODEL_ENTRY_ID_FIELD),
//             @Property(name = TopicClassificationEngine.ENTRY_TYPE_FIELD, value = TopicClassificationEngine.DEFAULT_ENTRY_TYPE_FIELD),
//             @Property(name = TopicClassificationEngine.MODEL_EVALUATION_DATE_FIELD, value = TopicClassificationEngine.DEFAULT_MODEL_EVALUATION_DATE_FIELD),
//             @Property(name = TopicClassificationEngine.FALSE_NEGATIVES_FIELD, value = TopicClassificationEngine.DEFAULT_FALSE_NEGATIVES_FIELD),
//             @Property(name = TopicClassificationEngine.FALSE_POSITIVES_FIELD, value = TopicClassificationEngine.DEFAULT_FALSE_POSITIVES_FIELD),
//             @Property(name = TopicClassificationEngine.POSITIVE_SUPPORT_FIELD, value = TopicClassificationEngine.DEFAULT_POSITIVE_SUPPORT_FIELD),
//             @Property(name = TopicClassificationEngine.NEGATIVE_SUPPORT_FIELD, value = TopicClassificationEngine.DEFAULT_NEGATIVE_SUPPORT_FIELD),
//             @Property(name = TopicClassificationEngine.ORDER, intValue = TopicClassificationEngine.DEFAULT_ENGINE_ORDER),
        @Property(name = TopicClassificationEngine.TRAINING_SET_ID),
        @Property(name = Constants.SERVICE_RANKING, intValue = 0)
})
public class TopicClassificationEngine extends ConfiguredSolrCoreTracker implements EnhancementEngine,
        ServiceProperties, TopicClassifier {

    public static final String DEFAULT_SOLR_CORE_CONFIG = "default-topic-model.solrindex.zip";

    public static final String MODEL_ENTRY = "model";

    public static final String METADATA_ENTRY = "metadata";
    /**
     * The reference to the SolrCore used ny the TopicClassificationEngine.
     * The default is the engine name with the suffix '-model'. This also supports
     * the {server-name}:{index-name} syntax. if n
     */
    public static final String SOLR_CORE = "org.apache.stanbol.enhancer.engine.topic.solrCore";
    /**
     * The name of the Solr Index archive (default: "default-topic-model.solrindex.zip").
     * The file is loaded by using the DataFileProvider infrastructure. The archive may
     * also include a pre-trained model.
     */
    public static final String SOLR_CORE_CONFIG = "org.apache.stanbol.enhancer.engine.topic.solrCoreConfig";

    public static final String LANGUAGES = "org.apache.stanbol.enhancer.engine.topic.languages";

    public static final String ORDER = "org.apache.stanbol.enhancer.engine.topic.order";
    
    public static final Integer DEFAULT_ENGINE_ORDER = ServiceProperties.ORDERING_CONTENT_EXTRACTION;

    public static final String ENTRY_ID_FIELD = "org.apache.stanbol.enhancer.engine.topic.entryIdField";
    
    public static final String DEFAULT_ENTRY_ID_FIELD = "entry_id";

    public static final String ENTRY_TYPE_FIELD = "org.apache.stanbol.enhancer.engine.topic.entryTypeField";
    
    public static final String DEFAULT_ENTRY_TYPE_FIELD = "entry_type";

    public static final String SIMILARTITY_FIELD = "org.apache.stanbol.enhancer.engine.topic.similarityField";
    
    public static final String DEFAULT_SIMILARTITY_FIELD = "classifier_features";

    public static final String CONCEPT_URI_FIELD = "org.apache.stanbol.enhancer.engine.topic.conceptUriField";
    
    public static final String DEFAULT_CONCEPT_URI_FIELD = "concept";

    public static final String BROADER_FIELD = "org.apache.stanbol.enhancer.engine.topic.broaderField";
    
    public static final String DEFAULT_BROADER_FIELD = "broader";

    public static final String PRIMARY_TOPIC_URI_FIELD = "org.apache.stanbol.enhancer.engine.topic.primaryTopicField";
    
    public static final String DEFAULT_PRIMARY_TOPIC_URI_FIELD = "primary_topic";

    public static final String MODEL_UPDATE_DATE_FIELD = "org.apache.stanbol.enhancer.engine.topic.modelUpdateDateField";

    public static final String DEFAULT_MODEL_UPDATE_DATE_FIELD = "last_update_dt";
    
    public static final String MODEL_EVALUATION_DATE_FIELD = "org.apache.stanbol.enhancer.engine.topic.modelEvaluationDateField";
    
    public static final String DEFAULT_MODEL_EVALUATION_DATE_FIELD = "last_evaluation_dt";

    public static final String MODEL_ENTRY_ID_FIELD = "org.apache.stanbol.enhancer.engine.topic.modelEntryIdField";
    
    public static final String DEFAULT_MODEL_ENTRY_ID_FIELD = "model_entry_id";

    public static final String PRECISION_FIELD = "org.apache.stanbol.enhancer.engine.topic.precisionField";
    
    public static final String DEFAULT_PRECISION_FIELD = "precision";

    public static final String RECALL_FIELD = "org.apache.stanbol.enhancer.engine.topic.recallField";
    
    public static final String DEFAULT_RECALL_FIELD = "recall";

    public static final String FALSE_POSITIVES_FIELD = "org.apache.stanbol.enhancer.engine.topic.falsePositivesField";
    
    public static final String DEFAULT_FALSE_POSITIVES_FIELD = "false_positives";

    public static final String FALSE_NEGATIVES_FIELD = "org.apache.stanbol.enhancer.engine.topic.falseNegativesField";
    
    public static final String DEFAULT_FALSE_NEGATIVES_FIELD = "false_negatives";

    public static final String POSITIVE_SUPPORT_FIELD = "org.apache.stanbol.enhancer.engine.topic.positiveSupportField";

    public static final String DEFAULT_POSITIVE_SUPPORT_FIELD = "positive_support";
    
    public static final String NEGATIVE_SUPPORT_FIELD = "org.apache.stanbol.enhancer.engine.topic.negativeSupportField";

    public static final String DEFAULT_NEGATIVE_SUPPORT_FIELD = "negative_support";
    
    public static final String TRAINING_SET_ID = "org.apache.stanbol.enhancer.engine.topic.trainingSetId";

    private static final Logger log = LoggerFactory.getLogger(TopicClassificationEngine.class);

    /**
     * The "text/plain" mime type
     */
    public static final String PLAIN_TEXT_MIMETYPE = "text/plain";

    /**
     * Contains the only supported mime type {@link #PLAIN_TEXT_MIMETYPE}
     */
    public static final Set<String> SUPPORTED_MIMETYPES = Collections.singleton(PLAIN_TEXT_MIMETYPE);

    public static final String SOLR_NON_EMPTY_FIELD = "[\"\" TO *]";

    @Reference
    protected Entityhub entityhub;

    @Reference
    protected SiteManager referencedSiteManager;

    // TODO: make the following fields configurable

    private int MAX_COLLECTED_EXAMPLES = 1000;

    // Limit the evaluation time by computing the performance estimates on a bounded random selection of
    // labeled examples in the training set.
    public int MAX_EVALUATION_SAMPLES = 500;

    // Do not try to compute performance estimates if there is not at least a minimum number of example
    // documents for this concept.
    public int MIN_EVALUATION_SAMPLES = 10;

    public int MAX_CHARS_PER_TOPIC = 100000;

    public Integer MAX_ROOTS = 1000;

    public int MAX_SUGGESTIONS = 5; // never suggest more than this: this is expected to be a reasonable
                                    // estimate of the number of topics occurring in each documents

    protected String engineName;

    protected List<String> acceptedLanguages;
    private Set<String> acceptedLanguageSet;

    protected Integer order = ORDERING_EXTRACTION_ENHANCEMENT;

    protected String similarityField;

    protected String conceptUriField;

    protected String broaderField;

    protected String primaryTopicUriField;

    protected String modelUpdateDateField;

    protected String modelEvaluationDateField;

    protected String precisionField;

    protected String recallField;

    // without OSGi
    protected TrainingSet trainingSet;

    // with OSGi
    protected ServiceTracker trainingSetTracker;

    protected String trainingSetId;

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

    protected boolean evaluationRunning = false;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, bind = "bindManagedSolrServer", unbind = "unbindManagedSolrServer", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC)
    protected ManagedSolrServer managedSolrServerDummy; // trick to call the super class binders

    /**
     * Only used for testing outside an OSGI environment (see STANBOL-811: 
     * the previously used {@link File#createTempFile(String, String)} does not
     * work on some Windows versions.
     */
    private File embeddedSolrServerDir;

    /**
     * Embedded Solr server used for {@link #performCVFold(File, int, int, int, boolean)}
     * if no ManagedSolrServer is present (e.g. when running outside of OSGI).
     * Lazily initialised relative to {@link #embeddedSolrServerDir} on the first
     * call to performCVFold
     */
    private EmbeddedSolrServer __evaluationServer;

    private File __evaluationServerDir;

    void configureEmbeddedSolrServerDir(File directory){
        embeddedSolrServerDir = directory;
    }
    
    @Activate
    protected void activate(ComponentContext context) throws ConfigurationException, InvalidSyntaxException {
        @SuppressWarnings("unchecked")
        Dictionary<String,Object> config = context.getProperties();
        activate(context, config);
    }

    protected void activate(ComponentContext context, Dictionary<String,Object> config) throws ConfigurationException,
                                                                                       InvalidSyntaxException {
        this.context = context;
        configure(config);

        // if training set is not null, track it
        if (trainingSetId != null) {
            String filter = String.format("(&(%s=%s)(%s=%s))", Constants.OBJECTCLASS,
                TrainingSet.class.getName(), SolrTrainingSet.TRAINING_SET_NAME, trainingSetId);
            trainingSetTracker = new ServiceTracker(context.getBundleContext(), context.getBundleContext()
                    .createFilter(filter), null);
            trainingSetTracker.open();
        }
        // TODO if training set is null, make it possible to programmatically create a SolrTrainingSet
        // instance using the same managed solr server and register it under the same name as the engine
        // it-self.
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        if (indexTracker != null) {
            indexTracker.close();
        }
        if (trainingSetTracker != null) {
            trainingSetTracker.close();
        }
        //shutdown the evaluation server and delete the data of the evaluation server
        if(__evaluationServer != null){
            try {
                __evaluationServer.getCoreContainer().shutdown();
            }catch (Exception e){
              //ignore  
            } finally {
                FileUtils.deleteQuietly(__evaluationServerDir);
            }
        }
        context = null;
    }

    public void configure(Dictionary<String,Object> config) throws ConfigurationException {
        engineName = getRequiredStringParam(config, EnhancementEngine.PROPERTY_NAME);
        entryIdField = getRequiredStringParam(config, ENTRY_ID_FIELD, DEFAULT_ENTRY_ID_FIELD);
        modelEntryIdField = getRequiredStringParam(config, MODEL_ENTRY_ID_FIELD, DEFAULT_MODEL_ENTRY_ID_FIELD);
        conceptUriField = getRequiredStringParam(config, CONCEPT_URI_FIELD, DEFAULT_CONCEPT_URI_FIELD);
        entryTypeField = getRequiredStringParam(config, ENTRY_TYPE_FIELD, DEFAULT_ENTRY_TYPE_FIELD);
        similarityField = getRequiredStringParam(config, SIMILARTITY_FIELD, DEFAULT_SIMILARTITY_FIELD);
        acceptedLanguages = getStringListParan(config, LANGUAGES);
        acceptedLanguageSet = new HashSet<String>(acceptedLanguages);
        precisionField = getRequiredStringParam(config, PRECISION_FIELD, DEFAULT_PRECISION_FIELD);
        recallField = getRequiredStringParam(config, RECALL_FIELD, DEFAULT_RECALL_FIELD);
        modelUpdateDateField = getRequiredStringParam(config, MODEL_UPDATE_DATE_FIELD, DEFAULT_MODEL_UPDATE_DATE_FIELD);
        modelEvaluationDateField = getRequiredStringParam(config, MODEL_EVALUATION_DATE_FIELD, DEFAULT_MODEL_EVALUATION_DATE_FIELD);
        falsePositivesField = getRequiredStringParam(config, FALSE_POSITIVES_FIELD, DEFAULT_FALSE_POSITIVES_FIELD);
        falseNegativesField = getRequiredStringParam(config, FALSE_NEGATIVES_FIELD, DEFAULT_FALSE_NEGATIVES_FIELD);
        positiveSupportField = getRequiredStringParam(config, POSITIVE_SUPPORT_FIELD, DEFAULT_POSITIVE_SUPPORT_FIELD);
        negativeSupportField = getRequiredStringParam(config, NEGATIVE_SUPPORT_FIELD, DEFAULT_NEGATIVE_SUPPORT_FIELD);
        configureSolrCore(config, SOLR_CORE, engineName + "-model",SOLR_CORE_CONFIG);

        // optional fields, can be null
        broaderField = getRequiredStringParam(config, BROADER_FIELD, DEFAULT_BROADER_FIELD);
        primaryTopicUriField = getRequiredStringParam(config, PRIMARY_TOPIC_URI_FIELD, DEFAULT_PRIMARY_TOPIC_URI_FIELD);
        trainingSetId = (String) config.get(TRAINING_SET_ID);
        Object orderParamValue = config.get(ORDER);
        if (orderParamValue instanceof Number) {
            order = ((Number) orderParamValue).intValue();
        } else if(orderParamValue != null){
            try {
                Integer.parseInt(orderParamValue.toString());
            }catch (NumberFormatException e) {
                throw new ConfigurationException(ORDER, "The configured EnhancementEngine "
                    + "order MUST BE an Intever value!",e);
            }
        } else {
            order = DEFAULT_ENGINE_ORDER;
        }
    }

    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        if (ContentItemHelper.getBlob(ci, SUPPORTED_MIMETYPES) != null && getActiveSolrServer() != null) {
            String language = EnhancementEngineHelper.getLanguage(ci);
            if(acceptedLanguageSet.isEmpty() || acceptedLanguageSet.contains(language) ||
                    acceptedLanguageSet.contains("")){
                return ENHANCE_SYNCHRONOUS;
            } else {
                return CANNOT_ENHANCE;
            }
        } else {
            return CANNOT_ENHANCE;
        }
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        Entry<IRI,Blob> contentPart = ContentItemHelper.getBlob(ci, SUPPORTED_MIMETYPES);
        if (contentPart == null) {
            throw new IllegalStateException(
                    "No ContentPart with a supported Mime Type" + "found for ContentItem " + ci.getUri()
                            + "(supported: '" + SUPPORTED_MIMETYPES
                            + "') -> this indicates that canEnhance was"
                            + "NOT called and indicates a bug in the used EnhancementJobManager!");
        }
        String language = EnhancementEngineHelper.getLanguage(ci);
        if(!(acceptedLanguageSet.isEmpty() || acceptedLanguageSet.contains(language) ||
                acceptedLanguageSet.contains(""))){
            throw new IllegalStateException("The language '"+language+"' of the ContentItem is not configured as "
                +" active for this Engine (active: "+acceptedLanguageSet+").");
        }
        String text;
        try {
            text = ContentItemHelper.getText(contentPart.getValue());
        } catch (IOException e) {
            throw new InvalidContentException(String.format(
                "Unable to extract " + " textual content from ContentPart %s of ContentItem %s!",
                contentPart.getKey(), ci.getUri()), e);
        }
        if (text.trim().isEmpty()) {
            log.warn(
                "ContentPart {} of ContentItem {} does not contain any " + "text to extract topics from",
                contentPart.getKey(), ci.getUri());
            return;
        }
        Graph metadata = ci.getMetadata();
        List<TopicSuggestion> topics;
        try {
            topics = suggestTopics(text);
            if (topics.isEmpty()) {
                return;
            }
        } catch (ClassifierException e) {
            throw new EngineException(e);
        }
        IRI precision = new IRI(NamespaceEnum.fise + "classifier/precision");
        IRI recall = new IRI(NamespaceEnum.fise + "classifier/recall");
        IRI f1 = new IRI(NamespaceEnum.fise + "classifier/f1");

        LiteralFactory lf = LiteralFactory.getInstance();
        ci.getLock().writeLock().lock();
        try {
            // Global text annotation to attach all the topic annotation to it.
            IRI textAnnotation = EnhancementEngineHelper.createTextEnhancement(ci, this);
            metadata.add(new TripleImpl(textAnnotation,
                    org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE,
                    OntologicalClasses.SKOS_CONCEPT));
            for (TopicSuggestion topic : topics) {
                IRI enhancement = EnhancementEngineHelper.createEntityEnhancement(ci, this);
                metadata.add(new TripleImpl(enhancement,
                        org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE,
                        TechnicalClasses.ENHANCER_TOPICANNOTATION));
                metadata.add(new TripleImpl(enhancement,
                        org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_RELATION, textAnnotation));

                // add link to entity
                metadata.add(new TripleImpl(enhancement,
                        org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE,
                        new IRI(topic.conceptUri)));
                metadata.add(new TripleImpl(enhancement,
                        org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_TYPE,
                        OntologicalClasses.SKOS_CONCEPT));

                // add confidence information
                metadata.add(new TripleImpl(enhancement,
                        org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE, lf
                                .createTypedLiteral(Double.valueOf(topic.score))));

                // add performance estimates of the classifier if available
                ClassificationReport perf = getPerformanceEstimates(topic.conceptUri);
                if (perf.uptodate) {
                    metadata.add(new TripleImpl(enhancement, precision, lf.createTypedLiteral(Double
                            .valueOf(perf.precision))));
                    metadata.add(new TripleImpl(enhancement, recall, lf.createTypedLiteral(Double
                            .valueOf(perf.recall))));
                    metadata.add(new TripleImpl(enhancement, f1, lf.createTypedLiteral(Double
                            .valueOf(perf.f1))));
                }
                // fetch concept label from the entityhub or a referenced site if available
                Entity entity = entityhub.getEntity(topic.conceptUri);
                if (entity == null) {
                    entity = referencedSiteManager.getEntity(topic.conceptUri);
                }
                if (entity != null) {
                    Representation representation = entity.getRepresentation();
                    // TODO: extract all languages based on some configuration instead of hardcoding English
                    Text label = representation.getFirst(NamespaceEnum.skos + "prefLabel", "en", "en-US",
                        "en-GB");
                    if (label == null) {
                        label = representation.getFirst(NamespaceEnum.rdfs + "label", "en", "en-US", "en-GB");
                    }
                    if (label != null) {
                        metadata.add(new TripleImpl(enhancement,
                                org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_LABEL,
                                new PlainLiteralImpl(label.getText())));
                    }
                }
            }
        } catch (ClassifierException e) {
            throw new EngineException(e);
        } catch (IllegalArgumentException e) {
            throw new EngineException(e);
        } catch (EntityhubException e) {
            throw new EngineException(e);
        } finally {
            ci.getLock().writeLock().unlock();
        }
    }

    @Override
    public Map<String,Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(ENHANCEMENT_ENGINE_ORDERING,
            (Object) order));
    }

    public static TopicClassificationEngine fromParameters(Dictionary<String,Object> config) throws ConfigurationException {
        TopicClassificationEngine engine = new TopicClassificationEngine();
        engine.configure(config);
        return engine;
    }

    // classifier API

    @Override
    public String getName() {
        return engineName;
    }

    @Override
    public List<String> getAcceptedLanguages() {
        return acceptedLanguages;
    }

    public List<TopicSuggestion> suggestTopics(Collection<Object> contents) throws ClassifierException {
        return suggestTopics(StringUtils.join(contents, "\n\n"));
    }

    public List<TopicSuggestion> suggestTopics(String text) throws ClassifierException {
        List<TopicSuggestion> suggestedTopics = new ArrayList<TopicSuggestion>(MAX_SUGGESTIONS * 3);
        SolrServer solrServer = getActiveSolrServer();
        SolrQuery query = new SolrQuery();
        query.setRequestHandler("/" + MoreLikeThisParams.MLT);
        query.setFilterQueries(entryTypeField + ":" + MODEL_ENTRY);
        query.set(MoreLikeThisParams.MATCH_INCLUDE, false);
        query.set(MoreLikeThisParams.MIN_DOC_FREQ, 1);
        query.set(MoreLikeThisParams.MIN_TERM_FREQ, 1);
        query.set(MoreLikeThisParams.MAX_QUERY_TERMS, 30);
        query.set(MoreLikeThisParams.MAX_NUM_TOKENS_PARSED, 10000);
        // TODO: find a way to parse the interesting terms and report them
        // for debugging / explanation in dedicated RDF data structure.
        // query.set(MoreLikeThisParams.INTERESTING_TERMS, "details");
        query.set(MoreLikeThisParams.SIMILARITY_FIELDS, similarityField);
        query.set(CommonParams.STREAM_BODY, text);
        // over query the number of suggestions to find a statistical cut based on the curve of the scores of
        // the top suggestion
        query.setRows(MAX_SUGGESTIONS * 3);
        query.setFields(conceptUriField);
        query.setIncludeScore(true);
        try {
            StreamQueryRequest request = new StreamQueryRequest(query);
            QueryResponse response = request.process(solrServer);
            SolrDocumentList results = response.getResults();
            for (SolrDocument result : results.toArray(new SolrDocument[0])) {
                String conceptUri = (String) result.getFirstValue(conceptUriField);
                if (conceptUri == null) {
                    throw new ClassifierException(String.format(
                        "Solr Core '%s' is missing required field '%s'.", solrCoreId, conceptUriField));
                }
                Float score = (Float) result.getFirstValue("score");

                // fetch metadata
                SolrQuery metadataQuery = new SolrQuery("*:*");
                // use filter queries to leverage the Solr cache explicitly
                metadataQuery.addFilterQuery(entryTypeField + ":" + METADATA_ENTRY);
                metadataQuery
                        .addFilterQuery(conceptUriField + ":" + ClientUtils.escapeQueryChars(conceptUri));
                metadataQuery.setFields(conceptUriField, broaderField, primaryTopicUriField);
                SolrDocument metadata = solrServer.query(metadataQuery).getResults().get(0);
                String primaryTopicUri = (String) metadata.getFirstValue(primaryTopicUriField);
                suggestedTopics.add(new TopicSuggestion(conceptUri, primaryTopicUri, metadata
                        .getFieldValues(broaderField), score));
            }
        } catch (SolrServerException e) {
            if ("unknown handler: /mlt".equals(e.getCause().getMessage())) {
                String message = String.format("SolrServer with id '%s' for topic engine '%s' lacks"
                                               + " configuration for the MoreLikeThisHandler", solrCoreId,
                    engineName);
                throw new ClassifierException(message, e);
            } else {
                throw new ClassifierException(e);
            }
        }
        if (suggestedTopics.size() <= 1) {
            // no need to apply the cutting heuristic
            return suggestedTopics;
        }
        // filter out suggestions that are less than some threshold based on the mean of the top scores
        float mean = 0.0f;
        for (TopicSuggestion suggestion : suggestedTopics) {
            mean += suggestion.score / suggestedTopics.size();
        }
        float threshold = 0.25f * suggestedTopics.get(0).score + 0.75f * mean;
        List<TopicSuggestion> filteredSuggestions = new ArrayList<TopicSuggestion>();
        for (TopicSuggestion suggestion : suggestedTopics) {
            if (filteredSuggestions.size() >= MAX_SUGGESTIONS) {
                return filteredSuggestions;
            }
            if (filteredSuggestions.isEmpty() || suggestion.score > threshold) {
                filteredSuggestions.add(suggestion);
            } else {
                break;
            }
        }
        return filteredSuggestions;
    }

    @Override
    public Set<String> getNarrowerConcepts(String broadTopicId) throws ClassifierException {
        LinkedHashSet<String> narrowerConcepts = new LinkedHashSet<String>();
        if (broaderField == null) {
            return narrowerConcepts;
        }
        SolrServer solrServer = getActiveSolrServer();
        SolrQuery query = new SolrQuery("*:*");
        query.addFilterQuery(entryTypeField + ":" + METADATA_ENTRY);
        query.addFilterQuery(broaderField + ":" + ClientUtils.escapeQueryChars(broadTopicId));
        query.addField(conceptUriField);
        query.addSortField(conceptUriField, SolrQuery.ORDER.asc);
        try {
            for (SolrDocument result : solrServer.query(query).getResults()) {
                narrowerConcepts.add(result.getFirstValue(conceptUriField).toString());
            }
        } catch (SolrServerException e) {
            String msg = String.format("Error while fetching narrower topics of '%s' on Solr Core '%s'.",
                broadTopicId, solrCoreId);
            throw new ClassifierException(msg, e);
        }
        return narrowerConcepts;
    }

    @Override
    public Set<String> getBroaderConcepts(String id) throws ClassifierException {
        LinkedHashSet<String> broaderConcepts = new LinkedHashSet<String>();
        if (broaderField == null) {
            return broaderConcepts;
        }
        SolrServer solrServer = getActiveSolrServer();
        SolrQuery query = new SolrQuery("*:*");
        query.addFilterQuery(conceptUriField + ":" + ClientUtils.escapeQueryChars(id));
        query.addField(broaderField);
        try {
            for (SolrDocument result : solrServer.query(query).getResults()) {
                // there should be only one results
                Collection<Object> broaderFieldValues = result.getFieldValues(broaderField);
                if (broaderFieldValues == null) {
                    continue;
                }
                for (Object value : broaderFieldValues) {
                    broaderConcepts.add(value.toString());
                }
            }
        } catch (SolrServerException e) {
            String msg = String.format("Error while fetching broader topics of '%s' on Solr Core '%s'.", id,
                solrCoreId);
            throw new ClassifierException(msg, e);
        }
        return broaderConcepts;
    }

    @Override
    public Set<String> getRootConcepts() throws ClassifierException {
        LinkedHashSet<String> rootConcepts = new LinkedHashSet<String>();
        SolrServer solrServer = getActiveSolrServer();
        SolrQuery query = new SolrQuery("*:*");
        // TODO: this can be very big on flat thesauri: should we enable a paging API instead?
        query.setRows(MAX_ROOTS);
        query.setFields(conceptUriField);
        query.setSortField(conceptUriField, SolrQuery.ORDER.asc);
        query.addFilterQuery(entryTypeField + ":" + METADATA_ENTRY);
        if (broaderField != null) {
            // find any topic with an empty broaderField
            query.addFilterQuery(" -" + broaderField + ":" + SOLR_NON_EMPTY_FIELD);
        }
        try {
            QueryResponse response = solrServer.query(query);
            if (response.getResults().size() >= MAX_ROOTS) {
                log.warn(String.format("TopicClassifier '%s' has more than %d registered topic roots."
                                       + " Some roots might be ignored.", engineName, MAX_ROOTS));
            }
            for (SolrDocument result : response.getResults()) {
                rootConcepts.add(result.getFirstValue(conceptUriField).toString());
            }
        } catch (SolrServerException e) {
            String msg = String.format("Error while fetching root topics on Solr Core '%s'.", solrCoreId);
            throw new ClassifierException(msg, e);
        }
        return rootConcepts;
    }

    @Override
    public void addConcept(String conceptUri, String primaryTopicUri, Collection<String> broaderConcepts) throws ClassifierException {
        // ensure that there is no previous topic registered with the same id
        removeConcept(conceptUri);

        SolrInputDocument metadataEntry = new SolrInputDocument();
        String metadataEntryId = UUID.randomUUID().toString();
        String modelEntryId = UUID.randomUUID().toString();
        metadataEntry.addField(conceptUriField, conceptUri);
        metadataEntry.addField(entryIdField, metadataEntryId);
        metadataEntry.addField(modelEntryIdField, modelEntryId);
        metadataEntry.addField(entryTypeField, METADATA_ENTRY);
        if (broaderConcepts != null && broaderField != null) {
            metadataEntry.addField(broaderField, broaderConcepts);
        }
        if (primaryTopicUri != null && primaryTopicUriField != null) {
            metadataEntry.addField(primaryTopicUriField, primaryTopicUri);
        }
        SolrInputDocument modelEntry = new SolrInputDocument();
        modelEntry.addField(entryIdField, modelEntryId);
        modelEntry.addField(conceptUriField, conceptUri);
        modelEntry.addField(entryTypeField, MODEL_ENTRY);
        if (broaderConcepts != null) {
            invalidateModelFields(broaderConcepts, modelUpdateDateField, modelEvaluationDateField);
        }
        SolrServer solrServer = getActiveSolrServer();
        try {
            UpdateRequest request = new UpdateRequest();
            request.add(metadataEntry);
            request.add(modelEntry);
            solrServer.request(request);
            solrServer.commit();
        } catch (Exception e) {
            String msg = String.format("Error adding topic with id '%s' on Solr Core '%s'", conceptUri,
                solrCoreId);
            throw new ClassifierException(msg, e);
        }
    }

    @Override
    public void addConcept(String conceptId, Collection<String> broaderConcepts) throws ClassifierException {
        addConcept(conceptId, null, broaderConcepts);
    }

    /*
     * The commit is the responsibility of the caller.
     */
    protected void invalidateModelFields(Collection<String> conceptIds, String... fieldNames) throws ClassifierException {
        if (conceptIds.isEmpty() || fieldNames.length == 0) {
            return;
        }
        SolrServer solrServer = getActiveSolrServer();
        List<String> invalidatedFields = Arrays.asList(fieldNames);
        try {
            UpdateRequest request = new UpdateRequest();
            for (String conceptId : conceptIds) {
                SolrQuery query = new SolrQuery("*:*");
                query.addFilterQuery(entryTypeField + ":" + METADATA_ENTRY);
                query.addFilterQuery(conceptUriField + ":" + ClientUtils.escapeQueryChars(conceptId));
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
            if (request.getDocuments() != null && request.getDocuments().size() > 0) {
                solrServer.request(request);
            }
        } catch (Exception e) {
            String msg = String.format("Error invalidating topics [%s] on Solr Core '%s'",
                StringUtils.join(conceptIds, ", "), solrCoreId);
            throw new ClassifierException(msg, e);
        }
    }

    @Override
    public void removeAllConcepts() throws ClassifierException {
        SolrServer solrServer = getActiveSolrServer();
        try {
            solrServer.deleteByQuery("*:*");
            solrServer.commit();
        } catch (Exception e) {
            String msg = String.format("Error deleting concepts from Solr Core '%s'", solrCoreId);
            throw new ClassifierException(msg, e);
        }
    }

    @Override
    public void removeConcept(String conceptId) throws ClassifierException {
        if (conceptId == null || conceptId.isEmpty()) {
            throw new ClassifierException("conceptId must not be null or empty");
        }
        SolrServer solrServer = getActiveSolrServer();
        try {
            solrServer.deleteByQuery(conceptUriField + ":" + ClientUtils.escapeQueryChars(conceptId));
            solrServer.commit();
        } catch (Exception e) {
            String msg = String
                    .format("Error removing concept '%s' on Solr Core '%s'", conceptId, solrCoreId);
            throw new ClassifierException(msg, e);
        }
    }

    @Override
    public TrainingSet getTrainingSet() {
        if (trainingSet != null) {
            return trainingSet;
        }
        if (trainingSetTracker != null) {
            TrainingSet trainingsSet = (TrainingSet) trainingSetTracker.getService();
            if(trainingsSet == null){
                for(int i=0; i < 5 && trainingsSet == null; i++){
                    try {
                        trainingsSet = (TrainingSet) trainingSetTracker.waitForService(1000);
                    } catch (InterruptedException e) {/*ignore*/}
                }
            }
            return trainingsSet;
        }
        return null;
    }

    @Override
    public boolean isUpdatable() {
        return getTrainingSet() != null;
    }

    @Override
    public void setTrainingSet(TrainingSet trainingSet) {
        this.trainingSet = trainingSet;
    }

    protected int batchOverTopics(BatchProcessor<SolrDocument> processor) throws TrainingSetException {
        // TODO: implement incremental update by using the date informations
        int processedCount = 0;
        SolrServer solrServer = getActiveSolrServer();
        SolrQuery query = new SolrQuery("*:*");
        query.addFilterQuery(entryTypeField + ":" + METADATA_ENTRY);
        String offset = null;
        boolean done = false;
        int batchSize = 1000;
        query.addSortField(conceptUriField, SolrQuery.ORDER.asc);
        query.setRows(batchSize + 1);
        try {
            while (!done) {
                // batch over all the indexed topics
                if (offset != null) {
                    query.addFilterQuery(conceptUriField + ":["
                                         + ClientUtils.escapeQueryChars(offset) + " TO *]");
                }
                QueryResponse response = solrServer.query(query);
                int count = 0;
                List<SolrDocument> batchDocuments = new ArrayList<SolrDocument>();
                for (SolrDocument result : response.getResults()) {
                    String conceptId = result.getFirstValue(conceptUriField).toString();
                    if (count == batchSize) {
                        offset = conceptId;
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

            @Override
            public int process(List<SolrDocument> batch) throws ClassifierException, TrainingSetException {
                int processed = 0;
                for (SolrDocument result : batch) {
                    String conceptId = result.getFirstValue(conceptUriField).toString();
                    List<String> impactedTopics = new ArrayList<String>();
                    impactedTopics.add(conceptId);
                    impactedTopics.addAll(getNarrowerConcepts(conceptId));
                    if (incr) {
                        Date lastModelUpdate = (Date) result.getFirstValue(modelUpdateDateField);
                        if (lastModelUpdate != null
                            && !getTrainingSet().hasChangedSince(impactedTopics, lastModelUpdate)) {
                            continue;
                        }
                    }
                    String metadataEntryId = result.getFirstValue(entryIdField).toString();
                    String modelEntryId = result.getFirstValue(modelEntryIdField).toString();
                    String primaryTopicUri = null;
                    if (primaryTopicUriField != null) {
                        primaryTopicUri = (String) result.getFirstValue(primaryTopicUriField);
                    }
                    updateTopic(conceptId, metadataEntryId, modelEntryId, impactedTopics, primaryTopicUri,
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
     * @param conceptUri
     *            the topic model to update
     * @param metadataEntryId
     *            of the metadata entry id of the topic
     * @param modelEntryId
     *            of the model entry id of the topic
     * @param impactedTopics
     *            the list of impacted topics (e.g. the topic node and direct children)
     * @param primaryTopicUri
     * @param broaderConcepts
     *            the collection of broader to re-add in the broader field
     */
    protected void updateTopic(String conceptUri,
                               String metadataId,
                               String modelId,
                               List<String> impactedTopics,
                               String primaryTopicUri,
                               Collection<Object> broaderConcepts) throws TrainingSetException,
                                                                  ClassifierException {
        long start = System.currentTimeMillis();
        Batch<Example> examples = Batch.emtpyBatch(Example.class);
        StringBuffer sb = new StringBuffer();
        int offset = 0;
        do {
            examples = getTrainingSet().getPositiveExamples(impactedTopics, examples.nextOffset);
            for (Example example : examples.items) {
                if ((cvFoldCount != 0) && (offset % cvFoldCount == cvFoldIndex)) {
                    // we are performing a cross validation session and this example belong to the test
                    // fold hence should be skipped
                    offset++;
                    continue;
                }
                offset++;
                sb.append(StringUtils.join(example.contents, "\n\n"));
                sb.append("\n\n");
            }
        } while (sb.length() < MAX_CHARS_PER_TOPIC && examples.hasMore);

        // reindex the topic with the new text data collected from the examples
        SolrInputDocument modelEntry = new SolrInputDocument();
        modelEntry.addField(entryIdField, modelId);
        modelEntry.addField(conceptUriField, conceptUri);
        modelEntry.addField(entryTypeField, MODEL_ENTRY);
        if (sb.length() > 0) {
            modelEntry.addField(similarityField, sb);
        }

        // update the metadata of the topic model
        SolrInputDocument metadataEntry = new SolrInputDocument();
        metadataEntry.addField(entryIdField, metadataId);
        metadataEntry.addField(modelEntryIdField, modelId);
        metadataEntry.addField(entryTypeField, METADATA_ENTRY);
        metadataEntry.addField(conceptUriField, conceptUri);
        if (primaryTopicUriField != null) {
            metadataEntry.addField(primaryTopicUriField, primaryTopicUri);
        }
        if (broaderConcepts != null && broaderField != null) {
            metadataEntry.addField(broaderField, broaderConcepts);
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
            String msg = String.format("Error updating topic with id '%s' on Solr Core '%s'", conceptUri,
                solrCoreId);
            throw new ClassifierException(msg, e);
        }
        long stop = System.currentTimeMillis();
        log.debug("Sucessfully updated topic {} in {}s", conceptUri, (double) (stop - start) / 1000.);
    }

    protected void checkTrainingSet() throws TrainingSetException {
        if (getTrainingSet() == null) {
            throw new TrainingSetException(String.format("TopicClassificationEngine %s has no registered"
                                                         + " training set hence cannot be updated.",
                engineName));
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

    protected Dictionary<String,Object> getCanonicalConfiguration(Object server, Object coreConfig) {
        Hashtable<String,Object> config = new Hashtable<String,Object>();
        config.put(EnhancementEngine.PROPERTY_NAME, engineName + "-evaluation");
        config.put(TopicClassificationEngine.ENTRY_ID_FIELD, "entry_id");
        config.put(TopicClassificationEngine.ENTRY_TYPE_FIELD, "entry_type");
        config.put(TopicClassificationEngine.MODEL_ENTRY_ID_FIELD, "model_entry_id");
        config.put(TopicClassificationEngine.SOLR_CORE, server);
        config.put(TopicClassificationEngine.SOLR_CORE_CONFIG, coreConfig);
        config.put(TopicClassificationEngine.CONCEPT_URI_FIELD, "concept");
        config.put(TopicClassificationEngine.PRIMARY_TOPIC_URI_FIELD, "primary_topic");
        config.put(TopicClassificationEngine.SIMILARTITY_FIELD, "classifier_features");
        config.put(TopicClassificationEngine.BROADER_FIELD, "broader");
        config.put(TopicClassificationEngine.MODEL_UPDATE_DATE_FIELD, "last_update_dt");
        config.put(TopicClassificationEngine.MODEL_EVALUATION_DATE_FIELD, "last_evaluation_dt");
        config.put(TopicClassificationEngine.PRECISION_FIELD, "precision");
        config.put(TopicClassificationEngine.RECALL_FIELD, "recall");
        config.put(TopicClassificationEngine.POSITIVE_SUPPORT_FIELD, "positive_support");
        config.put(TopicClassificationEngine.NEGATIVE_SUPPORT_FIELD, "negative_support");
        config.put(TopicClassificationEngine.FALSE_POSITIVES_FIELD, "false_positives");
        config.put(TopicClassificationEngine.FALSE_NEGATIVES_FIELD, "false_negatives");
        return config;
    }

    public boolean isEvaluationRunning() {
        return evaluationRunning;
    }

    synchronized public int updatePerformanceEstimates(boolean incremental) throws ClassifierException,
                                                                           TrainingSetException {
        checkTrainingSet();
        if (evaluationRunning) {
            throw new ClassifierException("Another evaluation is already running");
        }
        int updatedTopics = 0;
        // NOTE: The folder used to create the SolrServer used for CVFold
        //       is now created within the #embeddedSolrServerDir
        try {
            evaluationRunning = true;
            int cvFoldCount = 3; // 3-folds CV is hardcoded for now
            int cvIterationCount = 3; // make it possible to limit the number of folds to use

            // We will use the training set quite intensively, ensure that the index is packed and its
            // statistics are up to date
            getTrainingSet().optimize();
            for (int cvFoldIndex = 0; cvFoldIndex < cvIterationCount; cvFoldIndex++) {
                updatedTopics = performCVFold(cvFoldIndex, cvFoldCount, cvIterationCount,
                    incremental);
            }
            SolrServer solrServer = getActiveSolrServer();
            solrServer.optimize();
        } catch (ConfigurationException e) {
            throw new ClassifierException(e);
        } catch (IOException e) {
            throw new ClassifierException(e);
        } catch (SolrServerException e) {
            throw new ClassifierException(e);
        } finally {
            FileUtils.deleteQuietly(__evaluationServerDir);
            evaluationRunning = false;
        }
        return updatedTopics;
    }

    protected int performCVFold(int cvFoldIndex, int cvFoldCount, int cvIterations, boolean incremental)
            throws ConfigurationException, TrainingSetException, ClassifierException {

        cvIterations = cvIterations <= 0 ? cvFoldCount : cvFoldCount;
        log.info(String.format("Performing evaluation %d-fold CV iteration %d/%d on classifier %s",
            cvFoldCount, cvFoldIndex + 1, cvIterations, engineName));
        long start = System.currentTimeMillis();
        final TopicClassificationEngine classifier = new TopicClassificationEngine();
        try {
            if (managedSolrServer != null) {
                // OSGi setup: the evaluation server will be generated automatically using the
                // managedSolrServer
                classifier.bindManagedSolrServer(managedSolrServer);
                classifier.activate(context, getCanonicalConfiguration(
                    engineName + "-evaluation", //TODO: maybe we should use the SolrCoreName instead
                    solrCoreConfig));
            } else {
                if(__evaluationServer == null){
                    __evaluationServerDir = new File(embeddedSolrServerDir,engineName + "-evaluation");
                    if(!__evaluationServerDir.exists()){
                        FileUtils.forceMkdir(__evaluationServerDir);
                    }
                    __evaluationServer = EmbeddedSolrHelper.makeEmbeddedSolrServer(__evaluationServerDir,
                        "evaluationclassifierserver", "default-topic-model", "default-topic-model");
                }
                classifier.configure(getCanonicalConfiguration(__evaluationServer,solrCoreConfig));
            }
        } catch (Exception e) {
            throw new ClassifierException(e);
        }

        // clean all previous concepts from the evaluation classifier in case we are reusing an existing solr
        // index from OSGi.
        classifier.removeAllConcepts();

        // iterate over all the topics to register them in the evaluation classifier
        batchOverTopics(new BatchProcessor<SolrDocument>() {
            @Override
            public int process(List<SolrDocument> batch) throws ClassifierException {
                for (SolrDocument topicEntry : batch) {
                    String conceptId = topicEntry.getFirstValue(conceptUriField).toString();
                    Collection<Object> broader = topicEntry.getFieldValues(broaderField);
                    if (broader == null) {
                        classifier.addConcept(conceptId, null, null);
                    } else {
                        List<String> broaderConcepts = new ArrayList<String>();
                        for (Object broaderConcept : broader) {
                            broaderConcepts.add(broaderConcept.toString());
                        }
                        classifier.addConcept(conceptId, null, broaderConcepts);
                    }
                }
                return batch.size();
            }
        });

        // build the model on the for the current train CV folds
        classifier.setCrossValidationInfo(cvFoldIndex, cvFoldCount);
        // bind our new classifier to the same training set at the parent
        classifier.setTrainingSet(getTrainingSet());
        classifier.updateModel(false);

        final int foldCount = cvFoldCount;
        final int foldIndex = cvFoldIndex;

        // iterate over the topics again to compute scores on the test fold
        int updatedTopics = batchOverTopics(new BatchProcessor<SolrDocument>() {

            @Override
            public int process(List<SolrDocument> batch) throws TrainingSetException, ClassifierException {
                int offset;
                int updated = 0;
                for (SolrDocument topicMetadata : batch) {
                    String topic = topicMetadata.getFirstValue(conceptUriField).toString();
                    List<String> topics = Arrays.asList(topic);
                    List<String> falseNegativeExamples = new ArrayList<String>();
                    int truePositives = 0;
                    int falseNegatives = 0;
                    int positiveSupport = 0;
                    offset = 0;
                    Batch<Example> examples = Batch.emtpyBatch(Example.class);
                    boolean skipTopic = false;
                    do {
                        examples = getTrainingSet().getPositiveExamples(topics, examples.nextOffset);
                        if (offset == 0 && examples.items.size() < MIN_EVALUATION_SAMPLES) {
                            // we need a minimum about of examples otherwise it's really not
                            // worth computing statistics
                            skipTopic = true;
                            break;
                        }
                        for (Example example : examples.items) {
                            if (!(offset % foldCount == foldIndex)) {
                                // this example is not part of the test fold, skip it
                                offset++;
                                continue;
                            }
                            positiveSupport++;
                            offset++;
                            List<TopicSuggestion> suggestedTopics = classifier
                                    .suggestTopics(example.contents);
                            boolean match = false;
                            for (TopicSuggestion suggestedTopic : suggestedTopics) {
                                if (topic.equals(suggestedTopic.conceptUri)) {
                                    match = true;
                                    truePositives++;
                                    break;
                                }
                            }
                            if (!match) {
                                falseNegatives++;
                                if (falseNegativeExamples.size() < MAX_COLLECTED_EXAMPLES / foldCount) {
                                    falseNegativeExamples.add(example.id);
                                }
                            }
                        }
                    } while (!skipTopic && examples.hasMore && offset < MAX_EVALUATION_SAMPLES);

                    List<String> falsePositiveExamples = new ArrayList<String>();
                    int falsePositives = 0;
                    int negativeSupport = 0;
                    offset = 0;
                    examples = Batch.emtpyBatch(Example.class);
                    do {
                        if (skipTopic) {
                            break;
                        }
                        examples = getTrainingSet().getNegativeExamples(topics, examples.nextOffset);
                        for (Example example : examples.items) {
                            if (!(offset % foldCount == foldIndex)) {
                                // this example is not part of the test fold, skip it
                                offset++;
                                continue;
                            }
                            negativeSupport++;
                            offset++;
                            List<TopicSuggestion> suggestedTopics = classifier
                                    .suggestTopics(example.contents);
                            for (TopicSuggestion suggestedTopic : suggestedTopics) {
                                if (topic.equals(suggestedTopic.conceptUri)) {
                                    falsePositives++;
                                    if (falsePositiveExamples.size() < MAX_COLLECTED_EXAMPLES / foldCount) {
                                        falsePositiveExamples.add(example.id);
                                    }
                                    break;
                                }
                            }
                            // we don't need to collect true negatives
                        }
                    } while (examples.hasMore && offset < MAX_EVALUATION_SAMPLES);

                    if (skipTopic) {
                        log.debug("Skipping evaluation of {} because too few positive examples.", topic);
                    } else {
                        // compute precision, recall and f1 score for the current test fold and topic
                        float precision = 0;
                        if (truePositives != 0 || falsePositives != 0) {
                            precision = truePositives / (float) (truePositives + falsePositives);
                        }
                        float recall = 0;
                        if (truePositives != 0 || falseNegatives != 0) {
                            recall = truePositives / (float) (truePositives + falseNegatives);
                        }
                        updatePerformanceMetadata(topic, precision, recall, positiveSupport, negativeSupport,
                            falsePositiveExamples, falseNegativeExamples);
                        updated += 1;
                    }
                }
                try {
                    getActiveSolrServer().commit();
                } catch (Exception e) {
                    throw new ClassifierException(e);
                }
                return updated;
            }
        });

        long stop = System.currentTimeMillis();
        log.info(String.format("Finished CV iteration %d/%d on classifier %s in %fs.", cvFoldIndex + 1,
            cvFoldCount, engineName, (stop - start) / 1000.0));
        if (context != null) {
            // close open trackers
            classifier.deactivate(context);
        }
        return updatedTopics;
    }

    /**
     * Update the performance statistics in a metadata entry of a topic. It is the responsibility of the
     * caller to commit.
     */
    protected void updatePerformanceMetadata(String conceptId,
                                             float precision,
                                             float recall,
                                             int positiveSupport,
                                             int negativeSupport,
                                             List<String> falsePositiveExamples,
                                             List<String> falseNegativeExamples) throws ClassifierException {
        SolrServer solrServer = getActiveSolrServer();
        try {
            SolrQuery query = new SolrQuery("*:*");
            query.addFilterQuery(entryTypeField + ":" + METADATA_ENTRY);
            query.addFilterQuery(conceptUriField + ":" + ClientUtils.escapeQueryChars(conceptId));
            for (SolrDocument result : solrServer.query(query).getResults()) {
                // there should be only one (or none: tolerated)
                // fetch any old values to update (all metadata fields are assumed to be stored)s
                Map<String,Collection<Object>> fieldValues = new HashMap<String,Collection<Object>>();
                for (String fieldName : result.getFieldNames()) {
                    fieldValues.put(fieldName, result.getFieldValues(fieldName));
                }
                addToList(fieldValues, precisionField, precision);
                addToList(fieldValues, recallField, recall);
                increment(fieldValues, positiveSupportField, positiveSupport);
                increment(fieldValues, negativeSupportField, negativeSupport);
                addToList(fieldValues, falsePositivesField, falsePositiveExamples);
                addToList(fieldValues, falseNegativesField, falseNegativeExamples);
                SolrInputDocument newEntry = new SolrInputDocument();
                for (Map.Entry<String,Collection<Object>> entry : fieldValues.entrySet()) {
                    newEntry.addField(entry.getKey(), entry.getValue());
                }
                newEntry.setField(modelEvaluationDateField, UTCTimeStamper.nowUtcDate());
                solrServer.add(newEntry);
            }
            log.info(String.format("Performance for concept '%s': precision=%f, recall=%f,"
                                   + " positiveSupport=%d, negativeSupport=%d", conceptId, precision, recall,
                positiveSupport, negativeSupport));
        } catch (Exception e) {
            String msg = String
                    .format("Error updating performance metadata for topic '%s' on Solr Core '%s'",
                        conceptId, solrCoreId);
            throw new ClassifierException(msg, e);
        }
    }

    protected void increment(Map<String,Collection<Object>> fieldValues, String fieldName, int count) {
        // this collection is expected to be a singleton for this particular field
        Collection<Object> oldValues = fieldValues.get(fieldName);
        if (oldValues != null && !oldValues.isEmpty()) {
            count += (Integer) oldValues.iterator().next();
        }
        Collection<Object> values = new ArrayList<Object>();
        values.add(count);
        fieldValues.put(fieldName, values);
    }

    @SuppressWarnings("unchecked")
    protected void addToList(Map<String,Collection<Object>> fieldValues, String fieldName, Object value) {
        Collection<Object> values = new ArrayList<Object>();
        if (fieldValues.get(fieldName) != null) {
            values.addAll(fieldValues.get(fieldName));
        }
        if (value instanceof Collection) {
            values.addAll((Collection<Object>) value);
        } else {
            values.add(value);
        }
        fieldValues.put(fieldName, values);
    }

    @Override
    public ClassificationReport getPerformanceEstimates(String conceptId) throws ClassifierException {

        SolrServer solrServer = getActiveSolrServer();
        SolrQuery query = new SolrQuery("*:*");
        query.addFilterQuery(entryTypeField + ":" + METADATA_ENTRY);
        query.addFilterQuery(conceptUriField + ":" + ClientUtils.escapeQueryChars(conceptId));
        try {
            SolrDocumentList results = solrServer.query(query).getResults();
            if (results.isEmpty()) {
                throw new ClassifierException(String.format("'%s' is not a registered topic", conceptId));
            }
            SolrDocument metadata = results.get(0);
            Float precision = computeMeanValue(metadata, precisionField);
            Float recall = computeMeanValue(metadata, recallField);
            int positiveSupport = computeSumValue(metadata, positiveSupportField);
            int negativeSupport = computeSumValue(metadata, negativeSupportField);
            Date evaluationDate = (Date) metadata.getFirstValue(modelEvaluationDateField);
            boolean uptodate = evaluationDate != null;
            ClassificationReport report = new ClassificationReport(precision, recall, positiveSupport,
                    negativeSupport, uptodate, evaluationDate);
            if (metadata.getFieldValues(falsePositivesField) == null) {
                metadata.setField(falsePositivesField, new ArrayList<Object>());
            }
            for (Object falsePositiveId : metadata.getFieldValues(falsePositivesField)) {
                report.falsePositiveExampleIds.add(falsePositiveId.toString());
            }
            if (metadata.getFieldValues(falseNegativesField) == null) {
                metadata.setField(falseNegativesField, new ArrayList<Object>());
            }
            for (Object falseNegativeId : metadata.getFieldValues(falseNegativesField)) {
                report.falseNegativeExampleIds.add(falseNegativeId.toString());
            }
            return report;
        } catch (SolrServerException e) {
            throw new ClassifierException(String.format("Error fetching the performance report for topic "
                                                        + conceptId));
        }
    }

    protected Float computeMeanValue(SolrDocument metadata, String fielName) {
        Float mean = 0f;
        Collection<Object> values = metadata.getFieldValues(fielName);
        if (values == null || values.isEmpty()) {
            return mean;
        }
        for (Object v : values) {
            mean += (Float) v / values.size();
        }
        return mean;
    }

    protected Integer computeSumValue(SolrDocument metadata, String fielName) {
        Integer sum = 0;
        Collection<Object> values = metadata.getFieldValues(fielName);
        if (values == null || values.isEmpty()) {
            return sum;
        }
        for (Object v : values) {
            sum += (Integer) v;
        }
        return sum;
    }

    @Override
    public List<String> getChainNames() throws InvalidSyntaxException {
        List<String> chainNames = new ArrayList<String>();
        BundleContext bundleContext = context.getBundleContext();
        ServiceReference[] references = bundleContext.getServiceReferences(Chain.class.getName(), null);
        if (references != null) {
            for (ServiceReference ref : references) {
                Chain chain = (Chain) bundleContext.getService(ref);
                try {
                    if (chain.getEngines().contains(getName())) {
                        chainNames.add(chain.getName());
                    }
                } catch (ChainException e) {
                    // This chain is currently not active ... ignore
                }
            }
        }
        return chainNames;
    }

    @Override
    public int importConceptsFromGraph(ImmutableGraph graph, IRI conceptClass, IRI broaderProperty) throws ClassifierException {
        int importedCount = 0;
        Iterator<Triple> conceptIterator = graph.filter(null,
            org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE, conceptClass);
        while (conceptIterator.hasNext()) {
            Triple conceptTriple = conceptIterator.next();
            if (!(conceptTriple.getSubject() instanceof IRI)) {
                continue;
            }
            IRI conceptUri = (IRI) conceptTriple.getSubject();
            GraphNode node = new GraphNode(conceptUri, graph);
            List<String> broaderConcepts = new ArrayList<String>();
            // TODO: use OWL property inference on sub-properties here instead of explicit
            // property filter
            Iterator<GraphNode> broaderIterator = node.getObjectNodes(broaderProperty);
            while (broaderIterator.hasNext()) {
                RDFTerm node2 = broaderIterator.next().getNode();
                if (node2 instanceof IRI) {
                    broaderConcepts.add(((IRI) node2).getUnicodeString());
                }
            }
            addConcept(conceptUri.getUnicodeString(), broaderConcepts);
            importedCount++;
        }
        return importedCount;
    }
}
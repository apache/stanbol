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
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
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
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.MoreLikeThisParams;
import org.apache.stanbol.commons.solr.IndexReference;
import org.apache.stanbol.commons.solr.RegisteredSolrServerTracker;
import org.apache.stanbol.commons.solr.utils.StreamQueryRequest;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
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
                     @Property(name = TopicClassificationEngine.LANGUAGE),
                     @Property(name = TopicClassificationEngine.SIMILARTITY_FIELD),
                     @Property(name = TopicClassificationEngine.TOPIC_URI_FIELD),
                     @Property(name = TopicClassificationEngine.MATERIALIZED_PATH_FIELD)})
public class TopicClassificationEngine implements EnhancementEngine, ServiceProperties {

    public static final String ENGINE_ID = "org.apache.stanbol.enhancer.engine.id";

    public static final String SOLR_CORE = "org.apache.stanbol.enhancer.engine.topic.solrCore";

    public static final String LANGUAGE = "org.apache.stanbol.enhancer.engine.topic.language";

    public static final String ORDER = "org.apache.stanbol.enhancer.engine.topic.order";

    public static final String SIMILARTITY_FIELD = "org.apache.stanbol.enhancer.engine.topic.similarityField";

    public static final String TOPIC_URI_FIELD = "org.apache.stanbol.enhancer.engine.topic.uriField";

    public static final String MATERIALIZED_PATH_FIELD = "org.apache.stanbol.enhancer.engine.topic.materializedPathField";

    private static final Logger log = LoggerFactory.getLogger(TopicClassificationEngine.class);

    protected String engineId;

    protected String solrCoreId;

    protected List<String> acceptedLanguages;

    protected Integer order = ORDERING_EXTRACTION_ENHANCEMENT;

    protected RegisteredSolrServerTracker indexTracker;

    // instance of solrServer to use if not using the OSGi service tracker (e.g. for tests)
    protected SolrServer solrServer;

    protected String similarityField;

    protected String topicUriField;

    protected String materializedPathField;

    protected ComponentContext context;

    protected int numTopics = 10;

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
        acceptedLanguages = getStringListParan(config, LANGUAGE);
        if (config.get(SOLR_CORE) instanceof SolrServer) {
            // Bind a fixed Solr server client instead of doing dynamic OSGi lookup using the service tracker.
            // This can be useful both for unit-testing .
            // The Solr server is expected to be configured with the MoreLikeThisQueryHandler and the matching
            // fields from the configuration.
            solrServer = (SolrServer) config.get(SOLR_CORE);
        } else {
            String solrCoreId = getRequiredStringParam(config, SOLR_CORE);
            if (context == null) {
                throw new ConfigurationException(SOLR_CORE, SOLR_CORE
                                                            + " should be a SolrServer instance for using"
                                                            + " the engine without any OSGi context. Got: "
                                                            + solrCoreId);
            }
            try {
                indexTracker = new RegisteredSolrServerTracker(context.getBundleContext(),
                        IndexReference.parse(solrCoreId));
                indexTracker.open();
            } catch (InvalidSyntaxException e) {
                throw new ConfigurationException(SOLR_CORE, e.getMessage(), e);
            }
        }
        // optional field, can be null
        materializedPathField = (String) config.get(TOPIC_URI_FIELD);
        Object orderParamValue = config.get(ORDER);
        if (orderParamValue != null) {
            order = (Integer) orderParamValue;
        }
    }

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
        suggestTopics(text);

        // TODO: express the results as RDF.
    }

    public List<TopicSuggestion> suggestTopics(String text) throws EngineException {
        List<TopicSuggestion> suggestedTopics = new ArrayList<TopicSuggestion>(numTopics);
        SolrServer solrServer = getActiveSolrServer();
        SolrQuery query = new SolrQuery();
        query.setQueryType("/" + MoreLikeThisParams.MLT);
        query.set(MoreLikeThisParams.MATCH_INCLUDE, false);
        query.set(MoreLikeThisParams.MIN_DOC_FREQ, 1);
        query.set(MoreLikeThisParams.MIN_TERM_FREQ, 1);
        // TODO: find a way to parse the interesting terms and report them
        // for debugging / explanation in dedicated RDF datastucture.
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
                    throw new EngineException(String.format("Solr Core '%s' is missing required field '%s'.",
                        solrCoreId, topicUriField));
                }
                suggestedTopics.add(new TopicSuggestion(uri, 0.0));
            }
        } catch (SolrServerException e) {
            if ("unknown handler: /mlt".equals(e.getCause().getMessage())) {
                String message = String.format("SolrServer with id '%s' for topic engine '%s' lacks"
                                               + " configuration for the MoreLikeThisHandler", solrCoreId,
                    engineId);
                throw new EngineException(message, e);
            } else {
                throw new EngineException(e);
            }
        }
        return suggestedTopics;
    }

    /**
     * @return the manually bound solrServer instance or the one tracked by the OSGi service tracker.
     */
    protected SolrServer getActiveSolrServer() {
        return solrServer != null ? solrServer : indexTracker.getService();
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
            Iterator<Triple> it = ci.getMetadata().filter(new UriRef(ci.getId()), NIE_PLAINTEXTCONTENT, null);
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

}

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
package org.apache.stanbol.enhancer.topic.training;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Dictionary;
import java.util.List;
import java.util.UUID;

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
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.apache.stanbol.enhancer.topic.ConfiguredSolrCoreTracker;
import org.apache.stanbol.enhancer.topic.UTCTimeStamper;
import org.apache.stanbol.enhancer.topic.api.Batch;
import org.apache.stanbol.enhancer.topic.api.training.Example;
import org.apache.stanbol.enhancer.topic.api.training.TrainingSet;
import org.apache.stanbol.enhancer.topic.api.training.TrainingSetException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the {@code TrainingSet} interface that uses a Solr Core as backend to store and retrieve
 * the text examples used to train a classifier.
 */
@Component(metatype = true, immediate = true, configurationFactory = true, policy = ConfigurationPolicy.REQUIRE)
@Service
@Properties(value = {@Property(name = SolrTrainingSet.TRAINING_SET_NAME),
        @Property(name = SolrTrainingSet.SOLR_CORE),
        @Property(name = SolrTrainingSet.SOLR_CORE_CONFIG, value = SolrTrainingSet.DEFAULT_SOLR_CORE_CONFIG)
//        @Property(name = SolrTrainingSet.EXAMPLE_ID_FIELD, value = SolrTrainingSet.DEFAULT_EXAMPLE_ID_FIELD),
//        @Property(name = SolrTrainingSet.EXAMPLE_TEXT_FIELD, value = SolrTrainingSet.DEFAULT_EXAMPLE_TEXT_FIELD),
//        @Property(name = SolrTrainingSet.TOPICS_URI_FIELD, value = SolrTrainingSet.DEFAULT_TOPICS_URI_FIELD),
//        @Property(name = SolrTrainingSet.MODIFICATION_DATE_FIELD, value = SolrTrainingSet.DEFAULT_MODIFICATION_DATE_FIELD)
})
public class SolrTrainingSet extends ConfiguredSolrCoreTracker implements TrainingSet {

    public static final String TRAINING_SET_NAME = "org.apache.stanbol.enhancer.topic.trainingset.id";

    public static final String SOLR_CORE = "org.apache.stanbol.enhancer.engine.topic.solrCore";

    public static final String SOLR_CORE_CONFIG = "org.apache.stanbol.enhancer.engine.topic.solrCoreConfig";
    
    public static final String DEFAULT_SOLR_CORE_CONFIG = "default-topic-trainingset.solrindex.zip";

    public static final String TOPICS_URI_FIELD = "org.apache.stanbol.enhancer.engine.topic.topicsUriField";
    
    public static final String DEFAULT_TOPICS_URI_FIELD = "topics";

    public static final String EXAMPLE_ID_FIELD = "org.apache.stanbol.enhancer.engine.topic.exampleIdField";
    
    public static final String DEFAULT_EXAMPLE_ID_FIELD = "id";

    public static final String EXAMPLE_TEXT_FIELD = "org.apache.stanbol.enhancer.engine.topic.exampleTextField";
    
    public static final String DEFAULT_EXAMPLE_TEXT_FIELD = "text";

    public static final String MODIFICATION_DATE_FIELD = "org.apache.stanbol.enhancer.engine.topic.modificiationDateField";
    
    public static final String DEFAULT_MODIFICATION_DATE_FIELD = "modification_dt";

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(SolrTrainingSet.class);

    protected String trainingSetId;

    protected String exampleIdField;

    protected String exampleTextField;

    protected String topicUrisField;

    protected String modificationDateField;

    // TODO: make me configurable using an OSGi property
    protected int batchSize = 100;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, bind = "bindManagedSolrServer", unbind = "unbindManagedSolrServer", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC)
    protected ManagedSolrServer managedSolrServer;
    
    public String getName() {
        return trainingSetId;
    }

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

    @Override
    public void configure(Dictionary<String,Object> config) throws ConfigurationException {
        trainingSetId = getRequiredStringParam(config, TRAINING_SET_NAME);
        exampleIdField = getRequiredStringParam(config, EXAMPLE_ID_FIELD, DEFAULT_EXAMPLE_ID_FIELD);
        exampleTextField = getRequiredStringParam(config, EXAMPLE_TEXT_FIELD, DEFAULT_EXAMPLE_TEXT_FIELD);
        topicUrisField = getRequiredStringParam(config, TOPICS_URI_FIELD, DEFAULT_TOPICS_URI_FIELD);
        modificationDateField = getRequiredStringParam(config, MODIFICATION_DATE_FIELD, DEFAULT_MODIFICATION_DATE_FIELD);
        configureSolrCore(config, SOLR_CORE, trainingSetId, SOLR_CORE_CONFIG);
    }

    public static ConfiguredSolrCoreTracker fromParameters(Dictionary<String,Object> config) throws ConfigurationException {
        ConfiguredSolrCoreTracker engine = new SolrTrainingSet();
        engine.configure(config);
        return engine;
    }

    @Override
    public boolean isUpdatable() {
        return true;
    }

    @Override
    public String registerExample(String exampleId, String text, List<String> topics) throws TrainingSetException {
        if (text == null) {
            // special case: example removal
            if (exampleId == null) {
                throw new IllegalArgumentException("exampleId and text should not be null simultaneously");
            }
            SolrServer solrServer = getActiveSolrServer();
            try {
                solrServer.deleteByQuery(exampleIdField + ":" + exampleId);
                solrServer.commit();
                return exampleId;
            } catch (Exception e) {
                String msg = String.format("Error deleting example with id '%s' on Solr Core '%s'",
                    exampleId, solrCoreId);
                throw new TrainingSetException(msg, e);
            }
        }

        if (exampleId == null || exampleId.isEmpty()) {
            exampleId = UUID.randomUUID().toString();
        }
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField(exampleIdField, exampleId);
        doc.addField(exampleTextField, text);
        if (topics != null) {
            doc.addField(topicUrisField, topics);
        }
        doc.addField(modificationDateField, UTCTimeStamper.nowUtcDate());
        SolrServer server = getActiveSolrServer();
        try {
            server.add(doc);
            server.commit();
        } catch (Exception e) {
            String msg = String.format("Could not register example '%s' with topics: ['%s']", exampleId,
                StringUtils.join(topics, "', '"));
            throw new TrainingSetException(msg, e);
        }
        return exampleId;
    }

    @Override
    public boolean hasChangedSince(List<String> topics, Date referenceDate) throws TrainingSetException {
        String utcIsoDate = UTCTimeStamper.utcIsoString(referenceDate);
        StringBuffer sb = new StringBuffer();
        sb.append(modificationDateField);
        sb.append(":[");
        sb.append(utcIsoDate);
        sb.append(" TO *]");
        if (topics != null && topics.size() > 0) {
            sb.append(" AND (");
            List<String> parts = new ArrayList<String>();
            for (String topic : topics) {
                // use a nested query to avoid string escaping issues with special solr chars
                parts.add(topicUrisField + ":" + ClientUtils.escapeQueryChars(topic));
            }
            sb.append(StringUtils.join(parts, " OR "));
            sb.append(")");
        }
        SolrQuery query = new SolrQuery(sb.toString());
        query.setRows(1);
        query.setFields(exampleIdField);
        try {
            SolrServer solrServer = getActiveSolrServer();
            return solrServer.query(query).getResults().size() > 0;
        } catch (SolrServerException e) {
            String msg = String.format(
                "Error while fetching topics for examples modified after '%s' on Solr Core '%s'.",
                utcIsoDate, solrCoreId);
            throw new TrainingSetException(msg, e);
        }
    }

    @Override
    public Batch<Example> getPositiveExamples(List<String> topics, Object offset) throws TrainingSetException {
        return getExamples(topics, offset, true);
    }

    @Override
    public Batch<Example> getNegativeExamples(List<String> topics, Object offset) throws TrainingSetException {
        return getExamples(topics, offset, false);
    }

    protected Batch<Example> getExamples(List<String> topics, Object offset, boolean positive) throws TrainingSetException {
        List<Example> items = new ArrayList<Example>();
        SolrServer solrServer = getActiveSolrServer();
        SolrQuery query = new SolrQuery();
        List<String> parts = new ArrayList<String>();
        String q = "";
        if (topics.isEmpty()) {
            q += "*:*";
        } else if (positive) {
            for (String topic : topics) {
                parts.add(topicUrisField + ":" + ClientUtils.escapeQueryChars(topic));
            }
            if (offset != null) {
                q += "(";
            }
            q += StringUtils.join(parts, " OR ");
            if (offset != null) {
                q += ")";
            }
        } else {
            for (String topic : topics) {
                parts.add("-" + topicUrisField + ":" + ClientUtils.escapeQueryChars(topic));
            }
            q += StringUtils.join(parts, " AND ");
        }
        if (offset != null) {
            q += " AND " + exampleIdField + ":[" + offset.toString() + " TO *]";
        }
        query.setQuery(q);
        query.addSortField(exampleIdField, SolrQuery.ORDER.asc);
        query.set("rows", batchSize + 1);
        String nextExampleId = null;
        try {
            int count = 0;
            QueryResponse response = solrServer.query(query);
            for (SolrDocument result : response.getResults()) {
                if (count == batchSize) {
                    nextExampleId = result.getFirstValue(exampleIdField).toString();
                } else {
                    count++;
                    String exampleId = result.getFirstValue(exampleIdField).toString();
                    Collection<Object> labelValues = result.getFieldValues(topicUrisField);
                    Collection<Object> textValues = result.getFieldValues(exampleTextField);
                    if (textValues == null) {
                        continue;
                    }
                    items.add(new Example(exampleId, labelValues, textValues));
                }
            }
        } catch (SolrServerException e) {
            String msg = String.format(
                "Error while fetching positive examples for topics ['%s'] on Solr Core '%s'.",
                StringUtils.join(topics, "', '"), solrCoreId);
            throw new TrainingSetException(msg, e);
        }
        return new Batch<Example>(items, nextExampleId != null, nextExampleId);
    }

    @Override
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    @Override
    public void optimize() throws TrainingSetException {
        try {
            getActiveSolrServer().optimize();
        } catch (Exception e) {
            throw new TrainingSetException("Error optimizing training dataset " + getName(), e);
        }
    }

}

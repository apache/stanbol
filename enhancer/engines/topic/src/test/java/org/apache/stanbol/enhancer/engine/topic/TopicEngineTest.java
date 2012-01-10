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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.CommonParams;
import org.apache.stanbol.commons.solr.utils.StreamQueryRequest;
import org.apache.stanbol.enhancer.topic.SolrTrainingSet;
import org.apache.stanbol.enhancer.topic.TopicSuggestion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicEngineTest extends BaseTestWithSolrCore {

    private static final Logger log = LoggerFactory.getLogger(TopicEngineTest.class);

    EmbeddedSolrServer classifierSolrServer;

    EmbeddedSolrServer trainingSetSolrServer;

    File solrHome;

    SolrTrainingSet trainingSet;

    TopicClassificationEngine classifier;

    @Before
    public void setup() throws Exception {
        solrHome = File.createTempFile("topicEngineTest_", "_solr_cores");
        solrHome.delete();
        solrHome.mkdir();
        classifierSolrServer = makeEmptyEmbeddedSolrServer(solrHome, "classifierserver", "classifier");
        classifier = TopicClassificationEngine.fromParameters(getDefaultClassifierConfigParams());

        trainingSetSolrServer = makeEmptyEmbeddedSolrServer(solrHome, "trainingsetserver", "trainingset");
        trainingSet = new SolrTrainingSet();
        trainingSet.configure(getDefaultTrainingSetConfigParams());
    }

    @After
    public void cleanupEmbeddedSolrServer() {
        FileUtils.deleteQuietly(solrHome);
        solrHome = null;
        classifierSolrServer = null;
        trainingSetSolrServer = null;
        trainingSet = null;
    }

    protected void loadSampleTopicsFromTSV() throws IOException, SolrServerException {
        assertNotNull(classifierSolrServer);
        String topicSnippetsPath = "/classifier/topics_abstracts_snippet.tsv";
        InputStream is = getClass().getResourceAsStream(topicSnippetsPath);
        assertNotNull("Could not find test resource: " + topicSnippetsPath, is);

        // Build a query for the CSV importer
        SolrQuery query = new SolrQuery();
        query.setQueryType("/update/csv");
        query.set("commit", true);
        query.set("separator", "\t");
        query.set("headers", false);
        query.set("fieldnames", "topic,popularity,paths,text");
        query.set(CommonParams.STREAM_CONTENTTYPE, "text/plan;charset=utf-8");
        query.set(CommonParams.STREAM_BODY, IOUtils.toString(is, "utf-8"));

        // Upload an index
        QueryResponse response = new StreamQueryRequest(query).process(classifierSolrServer);
        assertNotNull(response);
        log.info(String.format("Indexed test topics in %dms", response.getElapsedTime()));
    }

    @Test
    public void testEngineConfiguation() throws ConfigurationException {
        Hashtable<String,Object> config = getDefaultClassifierConfigParams();
        TopicClassificationEngine classifier = TopicClassificationEngine.fromParameters(config);
        assertNotNull(classifier);
        assertEquals(classifier.engineId, "test-engine");
        assertEquals(classifier.getActiveSolrServer(), classifierSolrServer);
        assertEquals(classifier.topicUriField, "topic");
        assertEquals(classifier.similarityField, "text");
        assertEquals(classifier.acceptedLanguages, new ArrayList<String>());

        // check some required attributes
        Hashtable<String,Object> configWithMissingTopicField = new Hashtable<String,Object>();
        configWithMissingTopicField.putAll(config);
        configWithMissingTopicField.remove(TopicClassificationEngine.TOPIC_URI_FIELD);
        try {
            TopicClassificationEngine.fromParameters(configWithMissingTopicField);
            fail("Should have raised a ConfigurationException");
        } catch (ConfigurationException e) {}

        Hashtable<String,Object> configWithMissingEngineId = new Hashtable<String,Object>();
        configWithMissingEngineId.putAll(config);
        configWithMissingEngineId.remove(TopicClassificationEngine.ENGINE_ID);
        try {
            TopicClassificationEngine.fromParameters(configWithMissingEngineId);
            fail("Should have raised a ConfigurationException");
        } catch (ConfigurationException e) {}

        // check accept language optional param
        Hashtable<String,Object> configWithAcceptLangage = new Hashtable<String,Object>();
        configWithAcceptLangage.putAll(config);
        configWithAcceptLangage.put(TopicClassificationEngine.LANGUAGES, "en, fr");
        classifier = TopicClassificationEngine.fromParameters(configWithAcceptLangage);
        assertNotNull(classifier);
        assertEquals(classifier.acceptedLanguages, Arrays.asList("en", "fr"));
    }

    @Test
    public void testProgrammaticThesaurusConstruction() throws Exception {
        // Register the roots of the taxonomy
        classifier.addTopic("http://example.com/topics/root1", null);
        classifier.addTopic("http://example.com/topics/root2", null);
        classifier.addTopic("http://example.com/topics/root3", new ArrayList<String>());
        assertEquals(0, classifier.getBroaderTopics("http://example.com/topics/root1").size());
        assertEquals(0, classifier.getBroaderTopics("http://example.com/topics/root2").size());
        assertEquals(0, classifier.getBroaderTopics("http://example.com/topics/root3").size());
        assertEquals(3, classifier.getTopicRoots().size());

        // Register some non root nodes
        classifier.addTopic("http://example.com/topics/node1",
            Arrays.asList("http://example.com/topics/root1", "http://example.com/topics/root2"));
        classifier.addTopic("http://example.com/topics/node2",
            Arrays.asList("http://example.com/topics/root3"));
        classifier.addTopic("http://example.com/topics/node3",
            Arrays.asList("http://example.com/topics/node1", "http://example.com/topics/node2"));

        // the root where not impacted
        assertEquals(0, classifier.getBroaderTopics("http://example.com/topics/root1").size());
        assertEquals(0, classifier.getBroaderTopics("http://example.com/topics/root2").size());
        assertEquals(0, classifier.getBroaderTopics("http://example.com/topics/root3").size());
        assertEquals(3, classifier.getTopicRoots().size());

        // the other nodes have the same broader topics as at creation time
        assertEquals(2, classifier.getBroaderTopics("http://example.com/topics/node1").size());
        assertEquals(1, classifier.getBroaderTopics("http://example.com/topics/node2").size());
        assertEquals(2, classifier.getBroaderTopics("http://example.com/topics/node3").size());

        // check the induced narrower relationships
        assertEquals(1, classifier.getNarrowerTopics("http://example.com/topics/root1").size());
        assertEquals(1, classifier.getNarrowerTopics("http://example.com/topics/root2").size());
        assertEquals(1, classifier.getNarrowerTopics("http://example.com/topics/root3").size());
        assertEquals(1, classifier.getNarrowerTopics("http://example.com/topics/node1").size());
        assertEquals(1, classifier.getNarrowerTopics("http://example.com/topics/node2").size());
        assertEquals(0, classifier.getNarrowerTopics("http://example.com/topics/node3").size());
    }

    @Test
    public void testEmptyIndexTopicClassification() throws Exception {
        TopicClassificationEngine engine = TopicClassificationEngine
                .fromParameters(getDefaultClassifierConfigParams());
        List<TopicSuggestion> suggestedTopics = engine.suggestTopics("This is a test.");
        assertNotNull(suggestedTopics);
        assertEquals(suggestedTopics.size(), 0);
    }

    @Test
    public void testTopicClassification() throws Exception {
        loadSampleTopicsFromTSV();
        List<TopicSuggestion> suggestedTopics = classifier
                .suggestTopics("The Man Who Shot Liberty Valance is a 1962"
                               + " American Western film directed by John Ford,"
                               + " narrated by Charlton Heston and starring James"
                               + " Stewart, John Wayne and Vivien Leigh.");
        assertNotNull(suggestedTopics);
        assertEquals(suggestedTopics.size(), 10);
        TopicSuggestion bestSuggestion = suggestedTopics.get(0);
        assertEquals(bestSuggestion.uri, "Category:American_films");
    }

    @Test
    public void testTrainClassifierFromExamples() throws Exception {

        // mini taxonomy for news articles
        String business = "urn:topics/business";
        String technology = "urn:topics/technology";
        String apple = "urn:topics/apple";
        String sport = "urn:topics/sport";
        String football = "urn:topics/football";
        String worldcup = "urn:topics/worldcup";
        String music = "urn:topics/music";
        String law = "urn:topics/law";

        classifier.addTopic(business, null);
        classifier.addTopic(technology, null);
        classifier.addTopic(sport, null);
        classifier.addTopic(music, null);
        classifier.addTopic(apple, Arrays.asList(business, technology));
        classifier.addTopic(football, Arrays.asList(sport));
        classifier.addTopic(worldcup, Arrays.asList(football));

        // train the classifier on an empty dataset
        classifier.setTrainingSet(trainingSet);
        assertEquals(7, classifier.updateModel(true));

        // the model is updated but does not predict anything
        List<TopicSuggestion> suggestions = classifier
                .suggestTopics("I like the sound of vuvuzula in the morning!");
        assertEquals(0, suggestions.size());

        // check that updating the model incrementally without changing the dataset won't change anything.
        assertEquals(0, classifier.updateModel(true));

        // lets register some examples
        trainingSet.registerExample(null, "Money, money, money is the root of all evil.",
            Arrays.asList(business));
        trainingSet.registerExample(null, "VC invested more money in tech startups in 2011.",
            Arrays.asList(business, technology));

        trainingSet.registerExample(null, "Apple's iPad is a small handheld computer with a touch screen UI",
            Arrays.asList(apple, technology));
        trainingSet.registerExample(null, "Apple sold the iPad at a very high price"
                                          + " and made record profits.", Arrays.asList(apple, business));

        trainingSet.registerExample(null, "Manchester United won 3-2 against FC Barcelona.",
            Arrays.asList(football));
        trainingSet.registerExample(null, "The 2012 Football Worldcup takes place in Brazil.",
            Arrays.asList(football, worldcup));
        trainingSet.registerExample(null, "Vuvuzela made the soundtrack of the"
                                          + " football worldcup of 2010 in South Africa.",
            Arrays.asList(football, worldcup, music));

        trainingSet.registerExample(null, "Amon Tobin will be live in Paris soon.", Arrays.asList(music));

        // retrain the model: all topics are recomputed
        assertEquals(7, classifier.updateModel(true));

        // test the trained classifier
        suggestions = classifier.suggestTopics("I like the sound of vuvuzula in the morning!");
        assertTrue(suggestions.size() >= 4);
        assertEquals(worldcup, suggestions.get(0).uri);
        assertEquals(music, suggestions.get(1).uri);
        assertEquals(football, suggestions.get(2).uri);
        assertEquals(sport, suggestions.get(3).uri);

        suggestions = classifier.suggestTopics("Apple is no longer a startup.");
        assertTrue(suggestions.size() >= 3);
        assertEquals(apple, suggestions.get(0).uri);
        assertEquals(technology, suggestions.get(1).uri);
        assertEquals(business, suggestions.get(2).uri);

        suggestions = classifier.suggestTopics("You can watch the worldcup on your iPad.");
        assertTrue(suggestions.size() >= 4);
        assertEquals(apple, suggestions.get(0).uri);
        assertEquals(worldcup, suggestions.get(1).uri);
        assertEquals(technology, suggestions.get(2).uri);
        assertEquals(football, suggestions.get(3).uri);

        // test incremental update of a single root node
        Thread.sleep(10);
        trainingSet.registerExample(null, "Dubstep is a broken beat musical style as are Hip-Hop,"
                                          + " Dancehall or Drum & Bass", Arrays.asList(music));
        assertEquals(1, classifier.updateModel(true));
        suggestions = classifier.suggestTopics("Glory box is best mixed as dubstep.");
        assertTrue(suggestions.size() >= 1);
        assertEquals(music, suggestions.get(0).uri);
        assertEquals(0, classifier.updateModel(true));

        // test incremental update of a leaf node (the parent topic needs re-indexing too)
        Thread.sleep(10);
        trainingSet.registerExample(null, "The Brazil team has won the cup so many times.",
            Arrays.asList(worldcup));
        assertEquals(2, classifier.updateModel(true));
        assertEquals(0, classifier.updateModel(true));

        // it's always possible to rebuild all models from scratch
        assertEquals(7, classifier.updateModel(false));

        // it's also possible to define new topics on an existing model and leverage incremental indexing for
        // them as long as there are effectively registered on the classifier
        trainingSet.registerExample(null,
            "Under Belgian law, judges and prosecutors are judicial officers with equal rank and pay.",
            Arrays.asList(law));
        trainingSet.registerExample(null, "Prosecutors are typically lawyers who possess a law degree,"
                                          + " and are recognized as legal professionals by the court"
                                          + " in which they intend to represent the state.",
            Arrays.asList(law));
        assertEquals(0, classifier.updateModel(true));
        classifier.addTopic(law, null);
        assertEquals(1, classifier.updateModel(true));
    }

    protected Hashtable<String,Object> getDefaultClassifierConfigParams() {
        Hashtable<String,Object> config = new Hashtable<String,Object>();
        config.put(TopicClassificationEngine.ENGINE_ID, "test-engine");
        config.put(TopicClassificationEngine.SOLR_CORE, classifierSolrServer);
        config.put(TopicClassificationEngine.TOPIC_URI_FIELD, "topic");
        config.put(TopicClassificationEngine.SIMILARTITY_FIELD, "text");
        config.put(TopicClassificationEngine.BROADER_FIELD, "broader");
        config.put(TopicClassificationEngine.MODEL_UPDATE_DATE_FIELD, "last_update_dt");
        return config;
    }

    protected Hashtable<String,Object> getDefaultTrainingSetConfigParams() {
        Hashtable<String,Object> config = new Hashtable<String,Object>();
        config.put(SolrTrainingSet.SOLR_CORE, trainingSetSolrServer);
        config.put(SolrTrainingSet.TRAINING_SET_ID, "test-training-set");
        config.put(SolrTrainingSet.EXAMPLE_ID_FIELD, "id");
        config.put(SolrTrainingSet.EXAMPLE_TEXT_FIELD, "text");
        config.put(SolrTrainingSet.TOPICS_URI_FIELD, "topics");
        config.put(SolrTrainingSet.MODIFICATION_DATE_FIELD, "modification_dt");
        return config;
    }
}

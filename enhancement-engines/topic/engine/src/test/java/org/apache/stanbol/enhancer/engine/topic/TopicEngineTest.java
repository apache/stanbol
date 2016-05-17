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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.jena.parser.JenaParserProvider;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.CommonParams;
import org.apache.stanbol.commons.solr.utils.StreamQueryRequest;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.topic.EmbeddedSolrHelper;
import org.apache.stanbol.enhancer.topic.api.ClassificationReport;
import org.apache.stanbol.enhancer.topic.api.ClassifierException;
import org.apache.stanbol.enhancer.topic.api.TopicSuggestion;
import org.apache.stanbol.enhancer.topic.api.training.TrainingSetException;
import org.apache.stanbol.enhancer.topic.training.SolrTrainingSet;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicEngineTest extends EmbeddedSolrHelper {

    private static final Logger log = LoggerFactory.getLogger(TopicEngineTest.class);

    private static final String TEST_ROOT = 
        FilenameUtils.separatorsToSystem("/target/enginetest-files");
    private static String  userDir;
    private static File testRoot;
    private static int testCounter = 1;

    EmbeddedSolrServer classifierSolrServer;

    EmbeddedSolrServer trainingSetSolrServer;

    File solrHome;

    SolrTrainingSet trainingSet;

    TopicClassificationEngine classifier;
    

    
    @BeforeClass
    public static void initTestFolder() throws IOException {
        //basedir - if present - is the project base folder
        String baseDir = System.getProperty("basedir");
        if(baseDir == null){ //if missing fall back to user.dir
            baseDir = System.getProperty("user.dir");
        }
        //store the current user.dir
        userDir = System.getProperty("user.dir");
        testRoot = new File(baseDir,TEST_ROOT);
        log.info("Topic Enigne Test Folder : "+testRoot);
        if(testRoot.exists()){
            log.info(" ... delete files from previouse test");
            FileUtils.deleteDirectory(testRoot);
        }
        FileUtils.forceMkdir(testRoot);
        System.setProperty("user.dir", testRoot.getName());
    }
    /**
     * resets the "user.dir" system property the the original value
     */
    @AfterClass
    public static void cleanup(){
        System.setProperty("user.dir", userDir);
    }
    
    @Before
    public void setup() throws Exception {
        solrHome = new File(testRoot, "test"+testCounter);
        testCounter++;
        assertTrue("Unable to create solr.home directory '"+solrHome+"'!",solrHome.mkdir());
        classifierSolrServer = makeEmbeddedSolrServer(solrHome, "classifierserver", "test-topic-model",
            "default-topic-model");
        classifier = TopicClassificationEngine.fromParameters(getDefaultClassifierConfigParams());
        //configure the directory used to create Embedded SolrServers for CVFold
        classifier.configureEmbeddedSolrServerDir(solrHome);
        trainingSetSolrServer = makeEmbeddedSolrServer(solrHome, "trainingsetserver",
            "test-topic-trainingset", "default-topic-trainingset");
        trainingSet = new SolrTrainingSet();
        trainingSet.configure(getDefaultTrainingSetConfigParams());
    }

    @After
    public void cleanupEmbeddedSolrServer() {
        //FileUtils.deleteQuietly(solrHome);
        solrHome = null;
        classifierSolrServer = null;
        trainingSetSolrServer = null;
        trainingSet = null;
        classifier.deactivate(null);
    }

    protected void loadSampleTopicsFromTSV() throws IOException, SolrServerException {
        assertNotNull(classifierSolrServer);
        String topicSnippetsPath = "/topics_abstracts_snippet.tsv";
        InputStream is = getClass().getResourceAsStream(topicSnippetsPath);
        assertNotNull("Could not find test resource: " + topicSnippetsPath, is);

        // Build a query for the CSV importer
        SolrQuery query = new SolrQuery();
        query.setQueryType("/update/csv");
        query.set("commit", true);
        query.set("separator", "\t");
        query.set("headers", false);
        query.set("fieldnames", "topic,popularity,broader,text");
        query.set(CommonParams.STREAM_CONTENTTYPE, "text/plan;charset=utf-8");
        query.set(CommonParams.STREAM_BODY, IOUtils.toString(is, "utf-8"));

        // Upload an index
        QueryResponse response = new StreamQueryRequest(query).process(classifierSolrServer);
        assertNotNull(response);
        log.info(String.format("Indexed test topics in %dms", response.getElapsedTime()));
    }

    @Test
    public void testEngineConfiguration() throws ConfigurationException {
        log.info(" --- testEngineConfiguration --- ");
        Hashtable<String,Object> config = getDefaultClassifierConfigParams();
        TopicClassificationEngine classifier = TopicClassificationEngine.fromParameters(config);
        assertNotNull(classifier);
        assertEquals(classifier.engineName, "test-engine");
        assertEquals(classifier.getActiveSolrServer(), classifierSolrServer);
        assertEquals(classifier.conceptUriField, "concept");
        assertEquals(classifier.similarityField, "classifier_features");
        assertEquals(classifier.acceptedLanguages, new ArrayList<String>());

        // check some required attributes
// NOTE: This is no longer an required field, but uses a default values instead
//        Hashtable<String,Object> configWithMissingTopicField = new Hashtable<String,Object>();
//        configWithMissingTopicField.putAll(config);
//        configWithMissingTopicField.remove(TopicClassificationEngine.CONCEPT_URI_FIELD);
//        try {
//            TopicClassificationEngine.fromParameters(configWithMissingTopicField);
//            fail("Should have raised a ConfigurationException");
//        } catch (ConfigurationException e) {}

        Hashtable<String,Object> configWithMissingEngineName = new Hashtable<String,Object>();
        configWithMissingEngineName.putAll(config);
        configWithMissingEngineName.remove(EnhancementEngine.PROPERTY_NAME);
        try {
            TopicClassificationEngine.fromParameters(configWithMissingEngineName);
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
    public void testImportModelFromSKOS() throws Exception {
        log.info(" --- testImportModelFromSKOS --- ");
        Parser parser = Parser.getInstance();
        parser.bindParsingProvider(new JenaParserProvider());
        ImmutableGraph graph = parser.parse(getClass().getResourceAsStream("/sample-scheme.skos.rdf.xml"),
            SupportedFormat.RDF_XML);
        int imported = classifier.importConceptsFromGraph(graph, OntologicalClasses.SKOS_CONCEPT,
            Properties.SKOS_BROADER);
        assertEquals(imported, 4);
        assertEquals(0, classifier.getBroaderConcepts("http://example.com/ns#someconceptscheme/100").size());
        assertEquals(0, classifier.getBroaderConcepts("http://example.com/ns#someconceptscheme/200").size());
        assertEquals(1, classifier.getBroaderConcepts("http://example.com/ns#someconceptscheme/010").size());
        assertEquals(1, classifier.getBroaderConcepts("http://example.com/ns#someconceptscheme/020").size());
        assertEquals(2, classifier.getRootConcepts().size());
    }

    @Test
    public void testProgrammaticThesaurusConstruction() throws Exception {
        log.info(" --- testProgrammaticThesaurusConstruction --- ");
        // Register the roots of the taxonomy
        classifier.addConcept("http://example.com/topics/root1", null);
        classifier.addConcept("http://example.com/topics/root2", null);
        classifier.addConcept("http://example.com/topics/root3", new ArrayList<String>());
        assertEquals(0, classifier.getBroaderConcepts("http://example.com/topics/root1").size());
        assertEquals(0, classifier.getBroaderConcepts("http://example.com/topics/root2").size());
        assertEquals(0, classifier.getBroaderConcepts("http://example.com/topics/root3").size());
        assertEquals(3, classifier.getRootConcepts().size());

        // Register some non root nodes
        classifier.addConcept("http://example.com/topics/node1",
            Arrays.asList("http://example.com/topics/root1", "http://example.com/topics/root2"));
        classifier.addConcept("http://example.com/topics/node2",
            Arrays.asList("http://example.com/topics/root3"));
        classifier.addConcept("http://example.com/topics/node3",
            Arrays.asList("http://example.com/topics/node1", "http://example.com/topics/node2"));

        // the root where not impacted
        assertEquals(0, classifier.getBroaderConcepts("http://example.com/topics/root1").size());
        assertEquals(0, classifier.getBroaderConcepts("http://example.com/topics/root2").size());
        assertEquals(0, classifier.getBroaderConcepts("http://example.com/topics/root3").size());
        assertEquals(3, classifier.getRootConcepts().size());

        // the other nodes have the same broader topics as at creation time
        assertEquals(2, classifier.getBroaderConcepts("http://example.com/topics/node1").size());
        assertEquals(1, classifier.getBroaderConcepts("http://example.com/topics/node2").size());
        assertEquals(2, classifier.getBroaderConcepts("http://example.com/topics/node3").size());

        // check the induced narrower relationships
        assertEquals(1, classifier.getNarrowerConcepts("http://example.com/topics/root1").size());
        assertEquals(1, classifier.getNarrowerConcepts("http://example.com/topics/root2").size());
        assertEquals(1, classifier.getNarrowerConcepts("http://example.com/topics/root3").size());
        assertEquals(1, classifier.getNarrowerConcepts("http://example.com/topics/node1").size());
        assertEquals(1, classifier.getNarrowerConcepts("http://example.com/topics/node2").size());
        assertEquals(0, classifier.getNarrowerConcepts("http://example.com/topics/node3").size());
    }

    @Test
    public void testEmptyIndexTopicClassification() throws Exception {
        log.info(" --- testEmptyIndexTopicClassification --- ");
        TopicClassificationEngine engine = TopicClassificationEngine
                .fromParameters(getDefaultClassifierConfigParams());
        List<TopicSuggestion> suggestedTopics = engine.suggestTopics("This is a test.");
        assertNotNull(suggestedTopics);
        assertEquals(suggestedTopics.size(), 0);
    }

    // @Test
    // to get updated to work with the new Solr schema + move the CSV import directly to the classifier or
    // training set API
    public void testTopicClassification() throws Exception {
        log.info(" --- testTopicClassification --- ");
        loadSampleTopicsFromTSV();
        List<TopicSuggestion> suggestedTopics = classifier
                .suggestTopics("The Man Who Shot Liberty Valance is a 1962"
                               + " American Western film directed by John Ford,"
                               + " narrated by Charlton Heston and starring James"
                               + " Stewart, John Wayne and Vivien Leigh.");
        assertNotNull(suggestedTopics);
        assertEquals(suggestedTopics.size(), 10);
        TopicSuggestion bestSuggestion = suggestedTopics.get(0);
        assertEquals(bestSuggestion.conceptUri, "Category:American_films");
    }

    @Test
    public void testTrainClassifierFromExamples() throws Exception {
        log.info(" --- testTrainClassifierFromExamples --- ");
        // mini taxonomy for news articles
        String[] business = {"urn:topics/business", "http://dbpedia.org/resource/Business"};
        String[] technology = {"urn:topics/technology", "http://dbpedia.org/resource/Technology"};
        String apple = "urn:topics/apple";
        String sport = "urn:topics/sport";
        String football = "urn:topics/football";
        String worldcup = "urn:topics/worldcup";
        String music = "urn:topics/music";
        String law = "urn:topics/law";

        classifier.addConcept(business[0], business[1], null);
        classifier.addConcept(technology[0], technology[1], null);
        classifier.addConcept(sport, null);
        classifier.addConcept(music, null);
        classifier.addConcept(apple, Arrays.asList(business[0], technology[0]));
        classifier.addConcept(football, Arrays.asList(sport));
        classifier.addConcept(worldcup, Arrays.asList(football));

        // train the classifier on an empty dataset
        classifier.setTrainingSet(trainingSet);
        assertEquals(7, classifier.updateModel(true));

        // the model is updated but does not predict anything
        List<TopicSuggestion> suggestions = classifier
                .suggestTopics("I like the sound of vuvuzula in the morning!");
        assertEquals(0, suggestions.size());

        // check that updating the model incrementally without changing the dataset won't change anything.
        assertEquals(0, classifier.updateModel(true));

        // lets register some examples including stop words as well to limit statistical artifacts cause by
        // the small size of the training set.
        String STOP_WORDS = " the a is are be in at ";
        trainingSet.registerExample(null, "Money, money, money is the root of all evil." + STOP_WORDS,
            Arrays.asList(business));
        trainingSet.registerExample(null, "VC invested more money in tech startups in 2011." + STOP_WORDS,
            Arrays.asList(business[0], technology[0]));

        trainingSet.registerExample(null, "Apple's iPad is a small handheld computer with a touch screen UI"
                                          + STOP_WORDS, Arrays.asList(apple, technology[0]));
        trainingSet.registerExample(null, "Apple sold the iPad at a very high price"
                                          + " and made record profits." + STOP_WORDS,
            Arrays.asList(apple, business[0]));

        trainingSet.registerExample(null, "Manchester United won 3-2 against FC Barcelona." + STOP_WORDS,
            Arrays.asList(football));
        trainingSet.registerExample(null, "The 2012 Football Worldcup takes place in Brazil." + STOP_WORDS,
            Arrays.asList(football, worldcup));
        trainingSet.registerExample(null, "Vuvuzela made the soundtrack of the"
                                          + " football worldcup of 2010 in South Africa." + STOP_WORDS,
            Arrays.asList(football, worldcup, music));

        trainingSet.registerExample(null, "Amon Tobin will be live in Paris soon." + STOP_WORDS,
            Arrays.asList(music));

        // retrain the model: all topics are recomputed
        assertEquals(7, classifier.updateModel(true));

        // test the trained classifier
        suggestions = classifier.suggestTopics("I like the sound of vuvuzula in the morning!");
        assertTrue(suggestions.size() >= 4);
        assertEquals(worldcup, suggestions.get(0).conceptUri);
        assertEquals(music, suggestions.get(1).conceptUri);
        assertEquals(football, suggestions.get(2).conceptUri);
        assertEquals(sport, suggestions.get(3).conceptUri);
        // check that the scores are decreasing:
        assertTrue(suggestions.get(0).score >= suggestions.get(1).score);
        assertTrue(suggestions.get(1).score >= suggestions.get(2).score);
        assertTrue(suggestions.get(2).score >= suggestions.get(3).score);

        suggestions = classifier.suggestTopics("Apple is no longer a startup.");
        assertTrue(suggestions.size() >= 3);
        assertEquals(apple, suggestions.get(0).conceptUri);
        assertNull(suggestions.get(0).primaryTopicUri);
        assertEquals(Arrays.asList(business[0], technology[0]), suggestions.get(0).broader);

        assertEquals(technology[0], suggestions.get(1).conceptUri);
        assertEquals(technology[1], suggestions.get(1).primaryTopicUri);

        assertEquals(business[0], suggestions.get(2).conceptUri);
        assertEquals(business[1], suggestions.get(2).primaryTopicUri);

        suggestions = classifier.suggestTopics("You can watch the worldcup on your iPad.");
        assertTrue(suggestions.size() >= 2);
        assertEquals(apple, suggestions.get(0).conceptUri);
        assertEquals(worldcup, suggestions.get(1).conceptUri);

        // test incremental update of a single root node
        Thread.sleep(10);
        trainingSet.registerExample(null, "Dubstep is a broken beat musical style as are Hip-Hop,"
                                          + " Dancehall or Drum & Bass", Arrays.asList(music));
        assertEquals(1, classifier.updateModel(true));
        assertEquals(0, classifier.updateModel(true));
        suggestions = classifier.suggestTopics("Glory box is best mixed as dubstep.");
        assertTrue(suggestions.size() >= 1);
        assertEquals(music, suggestions.get(0).conceptUri);

        // test incremental update of a leaf node (the parent topic needs re-indexing too)
        Thread.sleep(10);
        trainingSet.registerExample(null, "The Brazil team has won the cup so many times.",
            Arrays.asList(worldcup));
        assertEquals(2, classifier.updateModel(true));
        assertEquals(0, classifier.updateModel(true));

        // it's always possible to rebuild all models from scratch
        assertEquals(7, classifier.updateModel(false));
        assertEquals(0, classifier.updateModel(true));

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
        classifier.addConcept(law, null);
        assertEquals(1, classifier.updateModel(true));
        assertEquals(0, classifier.updateModel(true));

        // registering new subtopics invalidate the models of the parent as well
        classifier.addConcept("urn:topics/sportsmafia", Arrays.asList(football, business[0]));
        assertEquals(3, classifier.updateModel(true));
        assertEquals(0, classifier.updateModel(true));
    }

    @Test
    public void testUpdatePerformanceEstimates() throws Exception {
        log.info(" --- testUpdatePerformanceEstimates --- ");
        ClassificationReport performanceEstimates;
        // no registered topic
        try {
            classifier.getPerformanceEstimates("urn:t/001");
            fail("Should have raised ClassifierException");
        } catch (ClassifierException e) {
            // expected
        }

        // register some topics
        classifier.addConcept("urn:t/001", null);
        classifier.addConcept("urn:t/002", Arrays.asList("urn:t/001"));
        performanceEstimates = classifier.getPerformanceEstimates("urn:t/002");
        assertFalse(performanceEstimates.uptodate);

        // update the performance metadata manually
        classifier.updatePerformanceMetadata("urn:t/002", 0.76f, 0.60f, 34, 32,
            Arrays.asList("ex14", "ex78"), Arrays.asList("ex34", "ex23", "ex89"));
        classifier.getActiveSolrServer().commit();
        performanceEstimates = classifier.getPerformanceEstimates("urn:t/002");
        assertTrue(performanceEstimates.uptodate);
        assertEquals(0.76f, performanceEstimates.precision, 0.01);
        assertEquals(0.60f, performanceEstimates.recall, 0.01);
        assertEquals(0.67f, performanceEstimates.f1, 0.01);
        assertEquals(34, performanceEstimates.positiveSupport);
        assertEquals(32, performanceEstimates.negativeSupport);
        assertTrue(classifier.getBroaderConcepts("urn:t/002").contains("urn:t/001"));

        // accumulate other folds statistics and compute means of statistics
        classifier.updatePerformanceMetadata("urn:t/002", 0.79f, 0.63f, 10, 10, Arrays.asList("ex1", "ex5"),
            Arrays.asList("ex3", "ex10", "ex11"));
        classifier.getActiveSolrServer().commit();
        performanceEstimates = classifier.getPerformanceEstimates("urn:t/002");
        assertTrue(performanceEstimates.uptodate);
        assertEquals(0.775f, performanceEstimates.precision, 0.01);
        assertEquals(0.615f, performanceEstimates.recall, 0.01);
        assertEquals(0.695f, performanceEstimates.f1, 0.01);
        assertEquals(44, performanceEstimates.positiveSupport);
        assertEquals(42, performanceEstimates.negativeSupport);
    }

    @Test
    public void testCrossValidation() throws Exception {
        log.info(" --- testCrossValidation --- ");
        // seed a pseudo random number generator for reproducible tests
        Random rng = new Random(0);
        ClassificationReport performanceEstimates;

        // build an artificial data set used for training models and evaluation
        int numberOfTopics = 10;
        int numberOfDocuments = 100;
        int vocabSizeMin = 20;
        int vocabSizeMax = 30;
        initArtificialTrainingSet(numberOfTopics, numberOfDocuments, vocabSizeMin, vocabSizeMax, rng);

        // by default the reports are not computed
        performanceEstimates = classifier.getPerformanceEstimates("urn:t/001");
        assertFalse(performanceEstimates.uptodate);
        performanceEstimates = classifier.getPerformanceEstimates("urn:t/002");
        assertFalse(performanceEstimates.uptodate);
        performanceEstimates = classifier.getPerformanceEstimates("urn:t/003");
        assertFalse(performanceEstimates.uptodate);

        try {
            classifier.getPerformanceEstimates("urn:doesnotexist");
            fail("Should have raised a ClassifierException");
        } catch (ClassifierException e) {
            // expected
        }

        // launch an evaluation of the classifier according to the current state of the training set
        assertEquals(numberOfTopics, classifier.updatePerformanceEstimates(true));
        for (int i = 1; i <= numberOfTopics; i++) {
            String topic = String.format("urn:t/%03d", i);
            performanceEstimates = classifier.getPerformanceEstimates(topic);
            assertTrue(performanceEstimates.uptodate);
            assertGreater(performanceEstimates.precision, 0.45f);
            assertNotNull(performanceEstimates.falsePositiveExampleIds);
            assertNotNull(performanceEstimates.falseNegativeExampleIds);
            if (performanceEstimates.precision < 1) {
                assertFalse(performanceEstimates.falsePositiveExampleIds.isEmpty());
            }
            if (performanceEstimates.recall < 1) {
                assertFalse(performanceEstimates.falseNegativeExampleIds.isEmpty());
            }
            assertGreater(performanceEstimates.recall, 0.45f);
            assertGreater(performanceEstimates.f1, 0.55f);
            // very small support, hence the estimates are unstable, hence we set low min expectations, but we
            // need this test to run reasonably fast...
            assertGreater(performanceEstimates.positiveSupport, 4);
            assertGreater(performanceEstimates.negativeSupport, 4);
            assertNotNull(performanceEstimates.evaluationDate);
        }

        // TODO: test model invalidation by registering a sub topic manually
    }

    protected void assertGreater(float large, float small) {
        if (small > large) {
            throw new AssertionError(String.format("Expected %f to be greater than %f.", large, small));
        }
    }

    protected void initArtificialTrainingSet(int numberOfTopics,
                                             int numberOfDocuments,
                                             int vocabSizeMin,
                                             int vocabSizeMax,
                                             Random rng) throws ClassifierException, TrainingSetException {
        // define some artificial topics and register them to the classifier
        String[] stopWords = randomVocabulary(0, 50, 50, rng);
        String[] topics = new String[numberOfTopics];
        Map<String,String[]> vocabularies = new TreeMap<String,String[]>();
        for (int i = 0; i < numberOfTopics; i++) {
            String topic = String.format("urn:t/%03d", i + 1);
            topics[i] = topic;
            classifier.addConcept(topic, null);
            String[] terms = randomVocabulary(i, vocabSizeMin, vocabSizeMax, rng);
            vocabularies.put(topic, terms);
        }
        classifier.setTrainingSet(trainingSet);

        // build a random data where each example has a couple of dominating topics and other words
        for (int i = 0; i < numberOfDocuments; i++) {
            List<String> documentTerms = new ArrayList<String>();

            // add terms from some non-dominant topics that are used as classification target
            int numberOfDominantTopics = rng.nextInt(3) + 1; // between 1 and 3 topics
            List<String> documentTopics = new ArrayList<String>();
            for (int j = 0; j < numberOfDominantTopics; j++) {
                String topic = randomTopicAndTerms(topics, vocabularies, documentTerms, 100, 150, rng);
                documentTopics.add(topic);
            }
            // add terms from some non-dominant topics
            for (int j = 0; j < 5; j++) {
                randomTopicAndTerms(topics, vocabularies, documentTerms, 5, 10, rng);
            }
            // add some non discriminative terms not linked to any topic
            for (int k = 0; k < 100; k++) {
                documentTerms.add(String.valueOf(stopWords[rng.nextInt(stopWords.length)]));
            }
            // register the generated example in the training set
            String text = StringUtils.join(documentTerms, " ");
            trainingSet.registerExample(String.format("example_%03d", i), text, documentTopics);
        }
    }

    protected String[] randomVocabulary(int topicIndex, int vocabSizeMin, int vocabSizeMax, Random rng) {
        int vocSize = rng.nextInt(vocabSizeMax + 1 - vocabSizeMin) + vocabSizeMin;
        String[] terms = new String[vocSize];

        for (int j = 0; j < vocSize; j++) {
            // define some artificial vocabulary for each topic to automatically generate random examples
            // with some topic structure
            terms[j] = String.format("term_%03d_t%03d", j, topicIndex + 1);
        }
        return terms;
    }

    protected String randomTopicAndTerms(String[] topics,
                                         Map<String,String[]> vocabularies,
                                         List<String> documentTerms,
                                         int min,
                                         int max,
                                         Random rng) {
        String topic = topics[rng.nextInt(topics.length)];
        String[] terms = vocabularies.get(topic);
        int numberOfDominantTopicTerm = rng.nextInt(max + 1 - min) + min;
        for (int k = 0; k < numberOfDominantTopicTerm; k++) {
            documentTerms.add(terms[rng.nextInt(terms.length)]);
        }
        return topic;
    }

    protected Hashtable<String,Object> getDefaultClassifierConfigParams() {
        Hashtable<String,Object> config = new Hashtable<String,Object>();
        config.put(EnhancementEngine.PROPERTY_NAME, "test-engine");
        config.put(TopicClassificationEngine.SOLR_CORE, classifierSolrServer);
        //those are now optional properties
//        config.put(TopicClassificationEngine.ENTRY_ID_FIELD, "entry_id");
//        config.put(TopicClassificationEngine.ENTRY_TYPE_FIELD, "entry_type");
//        config.put(TopicClassificationEngine.MODEL_ENTRY_ID_FIELD, "model_entry_id");
//        config.put(TopicClassificationEngine.CONCEPT_URI_FIELD, "concept");
//        config.put(TopicClassificationEngine.PRIMARY_TOPIC_URI_FIELD, "primary_topic");
//        config.put(TopicClassificationEngine.SIMILARTITY_FIELD, "classifier_features");
//        config.put(TopicClassificationEngine.BROADER_FIELD, "broader");
//        config.put(TopicClassificationEngine.MODEL_UPDATE_DATE_FIELD, "last_update_dt");
//        config.put(TopicClassificationEngine.MODEL_EVALUATION_DATE_FIELD, "last_evaluation_dt");
//        config.put(TopicClassificationEngine.PRECISION_FIELD, "precision");
//        config.put(TopicClassificationEngine.RECALL_FIELD, "recall");
//        config.put(TopicClassificationEngine.POSITIVE_SUPPORT_FIELD, "positive_support");
//        config.put(TopicClassificationEngine.NEGATIVE_SUPPORT_FIELD, "negative_support");
//        config.put(TopicClassificationEngine.FALSE_POSITIVES_FIELD, "false_positives");
//        config.put(TopicClassificationEngine.FALSE_NEGATIVES_FIELD, "false_negatives");
        return config;
    }

    protected Hashtable<String,Object> getDefaultTrainingSetConfigParams() {
        Hashtable<String,Object> config = new Hashtable<String,Object>();
        config.put(SolrTrainingSet.SOLR_CORE, trainingSetSolrServer);
        config.put(SolrTrainingSet.TRAINING_SET_NAME, "test-training-set");
        config.put(SolrTrainingSet.EXAMPLE_ID_FIELD, "id");
        config.put(SolrTrainingSet.EXAMPLE_TEXT_FIELD, "text");
        config.put(SolrTrainingSet.TOPICS_URI_FIELD, "topics");
        config.put(SolrTrainingSet.MODIFICATION_DATE_FIELD, "modification_dt");
        return config;
    }

}

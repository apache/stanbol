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
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.core.CoreContainer;
import org.apache.stanbol.commons.solr.utils.StreamQueryRequest;
import org.apache.stanbol.enhancer.topic.TopicSuggestion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class TopicEngineTest {

    private static final Logger log = LoggerFactory.getLogger(TopicEngineTest.class);

    public static final String TEST_SOLR_CORE_ID = "test";

    EmbeddedSolrServer solrServer;

    File solrHome;

    @Before
    public void makeEmptyEmbeddedSolrServer() throws IOException, ParserConfigurationException, SAXException {
        solrHome = File.createTempFile("topicEngineTest_", "_solr_folder");
        solrHome.delete();
        solrHome.mkdir();

        // solr conf file
        File solrFile = new File(solrHome, "solr.xml");
        InputStream is = getClass().getResourceAsStream("/test_solr.xml");
        TestCase.assertNotNull("missing test solr.xml file", is);
        IOUtils.copy(is, new FileOutputStream(solrFile));

        // solr conf folder with schema
        File solrCoreFolder = new File(solrHome, TEST_SOLR_CORE_ID);
        solrCoreFolder.mkdir();
        File solrConfFolder = new File(solrCoreFolder, "conf");
        solrConfFolder.mkdir();
        File schemaFile = new File(solrConfFolder, "schema.xml");
        is = getClass().getResourceAsStream("/test_schema.xml");
        TestCase.assertNotNull("missing test solr schema.xml file", is);
        IOUtils.copy(is, new FileOutputStream(schemaFile));

        File solrConfigFile = new File(solrConfFolder, "solrconfig.xml");
        is = getClass().getResourceAsStream("/test_solrconfig.xml");
        TestCase.assertNotNull("missing test solrconfig.xml file", is);
        IOUtils.copy(is, new FileOutputStream(solrConfigFile));

        // create the embedded server
        CoreContainer coreContainer = new CoreContainer(solrHome.getAbsolutePath(), solrFile);
        solrServer = new EmbeddedSolrServer(coreContainer, TEST_SOLR_CORE_ID);
    }

    @After
    public void cleanupEmbeddedSolrServer() {
        FileUtils.deleteQuietly(solrHome);
        solrHome = null;
        solrServer = null;
    }

    protected void loadSampleTopicsFromTSV() throws IOException, SolrServerException {
        assertNotNull(solrHome);
        assertNotNull(solrServer);
        String topicSnippetsPath = "/topics_abstracts_snippet.tsv";
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
        QueryResponse response = new StreamQueryRequest(query).process(solrServer);
        assertNotNull(response);
        log.info(String.format("Indexed test topics in %dms", response.getElapsedTime()));
    }

    protected Hashtable<String,Object> getDefaultConfigParams() {
        Hashtable<String,Object> config = new Hashtable<String,Object>();
        config.put(TopicClassificationEngine.ENGINE_ID, "test-engine");
        config.put(TopicClassificationEngine.SOLR_CORE, solrServer);
        config.put(TopicClassificationEngine.TOPIC_URI_FIELD, "topic");
        config.put(TopicClassificationEngine.SIMILARTITY_FIELD, "text");
        config.put(TopicClassificationEngine.BROADER_FIELD, "broader");
        return config;
    }

    @Test
    public void testEngineConfiguation() throws ConfigurationException {
        Hashtable<String,Object> config = getDefaultConfigParams();
        TopicClassificationEngine engine = TopicClassificationEngine.fromParameters(config);
        assertNotNull(engine);
        assertEquals(engine.engineId, "test-engine");
        assertEquals(engine.getActiveSolrServer(), solrServer);
        assertEquals(engine.topicUriField, "topic");
        assertEquals(engine.similarityField, "text");
        assertEquals(engine.acceptedLanguages, new ArrayList<String>());

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
        engine = TopicClassificationEngine.fromParameters(configWithAcceptLangage);
        assertNotNull(engine);
        assertEquals(engine.acceptedLanguages, Arrays.asList("en", "fr"));
    }

    @Test
    public void testProgrammaticThesaurusConstruction() throws Exception {
        TopicClassificationEngine engine = TopicClassificationEngine.fromParameters(getDefaultConfigParams());

        // Register the roots of the taxonomy
        engine.addTopic("http://example.com/topics/root1", null);
        engine.addTopic("http://example.com/topics/root2", null);
        engine.addTopic("http://example.com/topics/root3", new ArrayList<String>());
        assertEquals(0, engine.getBroaderTopics("http://example.com/topics/root1").size());
        assertEquals(0, engine.getBroaderTopics("http://example.com/topics/root2").size());
        assertEquals(0, engine.getBroaderTopics("http://example.com/topics/root3").size());
        assertEquals(3, engine.getTopicRoots().size());

        // Register some non root nodes
        engine.addTopic("http://example.com/topics/node1",
            Arrays.asList("http://example.com/topics/root1", "http://example.com/topics/root2"));
        engine.addTopic("http://example.com/topics/node2", Arrays.asList("http://example.com/topics/root3"));
        engine.addTopic("http://example.com/topics/node3",
            Arrays.asList("http://example.com/topics/node1", "http://example.com/topics/node2"));

        // the root where not impacted
        assertEquals(0, engine.getBroaderTopics("http://example.com/topics/root1").size());
        assertEquals(0, engine.getBroaderTopics("http://example.com/topics/root2").size());
        assertEquals(0, engine.getBroaderTopics("http://example.com/topics/root3").size());
        assertEquals(3, engine.getTopicRoots().size());

        // the other nodes have the same broader topics as at creation time
        assertEquals(2, engine.getBroaderTopics("http://example.com/topics/node1").size());
        assertEquals(1, engine.getBroaderTopics("http://example.com/topics/node2").size());
        assertEquals(2, engine.getBroaderTopics("http://example.com/topics/node3").size());

        // check the induced narrower relationships
        assertEquals(1, engine.getNarrowerTopics("http://example.com/topics/root1").size());
        assertEquals(1, engine.getNarrowerTopics("http://example.com/topics/root2").size());
        assertEquals(1, engine.getNarrowerTopics("http://example.com/topics/root3").size());
        assertEquals(1, engine.getNarrowerTopics("http://example.com/topics/node1").size());
        assertEquals(1, engine.getNarrowerTopics("http://example.com/topics/node2").size());
        assertEquals(0, engine.getNarrowerTopics("http://example.com/topics/node3").size());
    }

    @Test
    public void testEmptyIndexTopicClassification() throws Exception {
        TopicClassificationEngine engine = TopicClassificationEngine.fromParameters(getDefaultConfigParams());
        List<TopicSuggestion> suggestedTopics = engine.suggestTopics("This is a test.");
        assertNotNull(suggestedTopics);
        assertEquals(suggestedTopics.size(), 0);
    }

    @Test
    public void testTopicClassification() throws Exception {
        loadSampleTopicsFromTSV();
        TopicClassificationEngine engine = TopicClassificationEngine.fromParameters(getDefaultConfigParams());
        List<TopicSuggestion> suggestedTopics = engine
                .suggestTopics("The Man Who Shot Liberty Valance is a 1962"
                               + " American Western film directed by John Ford,"
                               + " narrated by Charlton Heston and starring James"
                               + " Stewart, John Wayne and Vivien Leigh.");
        assertNotNull(suggestedTopics);
        assertEquals(suggestedTopics.size(), 10);
        TopicSuggestion bestSuggestion = suggestedTopics.get(0);
        assertEquals(bestSuggestion.uri, "Category:American_films");
    }
}

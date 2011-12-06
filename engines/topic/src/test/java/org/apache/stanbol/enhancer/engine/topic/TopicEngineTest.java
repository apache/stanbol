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
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.xml.sax.SAXException;

public class TopicEngineTest {

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
        File solrConfFolder = new File(solrHome, "conf");
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
        CoreContainer coreContainer = new CoreContainer(solrHome.getAbsolutePath());
        solrServer = new EmbeddedSolrServer(coreContainer, "test");
    }

    @After
    public void cleanupEmbeddedSolrServer() {
        FileUtils.deleteQuietly(solrHome);
    }

    protected Hashtable<String,Object> getDefaultConfigParams() {
        Hashtable<String,Object> config = new Hashtable<String,Object>();
        config.put(TopicClassificationEngine.ENGINE_ID, "test-engine");
        config.put(TopicClassificationEngine.SOLR_CORE, solrServer);
        config.put(TopicClassificationEngine.TOPIC_URI_FIELD, "topic");
        config.put(TopicClassificationEngine.SIMILARTITY_FIELD, "text");
        return config;
    }

    @Test
    public void testEngineConfiguation() throws ConfigurationException {
        Hashtable<String,Object> config = getDefaultConfigParams();
        TopicClassificationEngine engine = TopicClassificationEngine.fromParameters(config);
        assertNotNull(engine);
        assertEquals(engine.engineId, "test-engine");
        assertEquals(engine.solrServer, solrServer);
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
        configWithAcceptLangage.put(TopicClassificationEngine.LANGUAGE, "en, fr");
        engine = TopicClassificationEngine.fromParameters(configWithAcceptLangage);
        assertNotNull(engine);
        assertEquals(engine.acceptedLanguages, Arrays.asList("en", "fr"));
    }

    //@Test
    public void testClassificationTest() throws Exception {
        TopicClassificationEngine engine = TopicClassificationEngine.fromParameters(getDefaultConfigParams());
        List<TopicSuggestion> suggestedTopics = engine.suggestTopics("This is a test.");
        assertNotNull(suggestedTopics);
        // TODO implement me
    }
}

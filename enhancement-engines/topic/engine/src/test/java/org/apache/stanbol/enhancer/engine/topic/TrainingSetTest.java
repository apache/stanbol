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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.stanbol.enhancer.topic.EmbeddedSolrHelper;
import org.apache.stanbol.enhancer.topic.UTCTimeStamper;
import org.apache.stanbol.enhancer.topic.api.Batch;
import org.apache.stanbol.enhancer.topic.api.training.Example;
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
import org.xml.sax.SAXException;

public class TrainingSetTest extends EmbeddedSolrHelper {

    private static final Logger log = LoggerFactory.getLogger(TrainingSetTest.class);
    private static final String TEST_ROOT = 
            FilenameUtils.separatorsToSystem("/target/triningtest-files");
    private static String  userDir;
    private static File testRoot;
    private static int testCounter = 1;

    
    public static final String TOPIC_1 = "http://example.com/topics/topic1";

    public static final String TOPIC_2 = "http://example.com/topics/topic2";

    public static final String TOPIC_3 = "http://example.com/topics/topic3";

    protected EmbeddedSolrServer trainingsetSolrServer;

    protected File solrHome;

    protected SolrTrainingSet trainingSet;

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
        log.info("Topic TrainingSet Test Folder : "+testRoot);
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
    public void setup() throws IOException,
                       ParserConfigurationException,
                       SAXException,
                       ConfigurationException {
        solrHome = new File(testRoot, "test"+testCounter);
        testCounter++;
        assertTrue("Unable to create solr.home directory '"+solrHome+"'!",solrHome.mkdir());
        trainingsetSolrServer = makeEmbeddedSolrServer(solrHome, "trainingsetserver",
            "default-topic-trainingset", "default-topic-trainingset");
        trainingSet = new SolrTrainingSet();
        trainingSet.configure(getDefaultConfigParams());
        
    }

    @After
    public void cleanupEmbeddedSolrServer() {
        //FileUtils.deleteQuietly(solrHome);
        solrHome = null;
        trainingsetSolrServer = null;
    }

    @Test
    public void testDateSerialization() throws Exception {
        log.info(" --- testDateSerialization --- ");
        GregorianCalendar timeUtc = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        timeUtc.set(2012, 23, 12, 06, 43, 00);
        timeUtc.set(Calendar.MILLISECOND, 0);
        assertEquals("2013-12-12T06:43:00.000Z", UTCTimeStamper.utcIsoString(timeUtc.getTime()));

        GregorianCalendar timeCet = new GregorianCalendar(TimeZone.getTimeZone("CET"));
        timeCet.set(2012, 23, 12, 06, 43, 00);
        timeCet.set(Calendar.MILLISECOND, 0);
        assertEquals("2013-12-12T05:43:00.000Z", UTCTimeStamper.utcIsoString(timeCet.getTime()));
    }

    @Test
    public void testEmptyTrainingSet() throws TrainingSetException {
        log.info(" --- testEmptyTrainingSet --- ");
        Batch<Example> examples = trainingSet.getPositiveExamples(new ArrayList<String>(), null);
        assertEquals(examples.items.size(), 0);
        assertFalse(examples.hasMore);
        examples = trainingSet.getNegativeExamples(new ArrayList<String>(), null);
        assertEquals(examples.items.size(), 0);
        assertFalse(examples.hasMore);
        examples = trainingSet.getPositiveExamples(Arrays.asList(TOPIC_1), null);
        assertEquals(examples.items.size(), 0);
        assertFalse(examples.hasMore);
        examples = trainingSet.getPositiveExamples(Arrays.asList(TOPIC_1, TOPIC_2), null);
        assertEquals(examples.items.size(), 0);
        assertFalse(examples.hasMore);
        examples = trainingSet.getNegativeExamples(Arrays.asList(TOPIC_1, TOPIC_2), null);
        assertEquals(examples.items.size(), 0);
        assertFalse(examples.hasMore);
    }

    @Test
    public void testStoringExamples() throws ConfigurationException, TrainingSetException {
        log.info(" --- testStoringExamples --- ");
        trainingSet.registerExample("example1", "Text of example1.", Arrays.asList(TOPIC_1));
        trainingSet.registerExample("example2", "Text of example2.", Arrays.asList(TOPIC_1, TOPIC_2));
        trainingSet.registerExample("example3", "Text of example3.", new ArrayList<String>());

        Batch<Example> examples = trainingSet.getPositiveExamples(Arrays.asList(TOPIC_2), null);
        assertEquals(1, examples.items.size());
        assertEquals(examples.items.get(0).getContentString(), "Text of example2.");
        assertFalse(examples.hasMore);

        examples = trainingSet.getPositiveExamples(Arrays.asList(TOPIC_1, TOPIC_3), null);
        assertEquals(2, examples.items.size());
        assertEquals(examples.items.get(0).getContentString(), "Text of example1.");
        assertEquals(examples.items.get(1).getContentString(), "Text of example2.");
        assertFalse(examples.hasMore);

        examples = trainingSet.getNegativeExamples(Arrays.asList(TOPIC_1), null);
        assertEquals(1, examples.items.size());
        assertEquals(examples.items.get(0).getContentString(), "Text of example3.");
        assertFalse(examples.hasMore);

        // Test example update by adding topic3 to example1. The results of the previous query should remain
        // the same (inplace update).
        trainingSet.registerExample("example1", "Text of example1.", Arrays.asList(TOPIC_1, TOPIC_3));
        examples = trainingSet.getPositiveExamples(Arrays.asList(TOPIC_1, TOPIC_3), null);
        assertEquals(2, examples.items.size());
        assertEquals(examples.items.get(0).getContentString(), "Text of example1.");
        assertEquals(examples.items.get(1).getContentString(), "Text of example2.");
        assertFalse(examples.hasMore);

        // Test example removal
        trainingSet.registerExample("example1", null, Arrays.asList(TOPIC_1, TOPIC_3));
        examples = trainingSet.getPositiveExamples(Arrays.asList(TOPIC_1, TOPIC_3), null);
        assertEquals(1, examples.items.size());
        assertEquals(examples.items.get(0).getContentString(), "Text of example2.");
        assertFalse(examples.hasMore);

        trainingSet.registerExample("example2", null, Arrays.asList(TOPIC_1, TOPIC_3));
        examples = trainingSet.getPositiveExamples(Arrays.asList(TOPIC_1, TOPIC_3), null);
        assertEquals(0, examples.items.size());
        assertFalse(examples.hasMore);
    }

    @Test
    public void testBatchingPositiveExamples() throws ConfigurationException, TrainingSetException {
        log.info(" --- testBatchingPositiveExamples --- ");
        Set<String> expectedCollectedIds = new HashSet<String>();
        Set<String> expectedCollectedText = new HashSet<String>();
        Set<String> collectedIds = new HashSet<String>();
        Set<String> collectedText = new HashSet<String>();
        for (int i = 0; i < 28; i++) {
            String id = "example-" + i;
            String text = "Text of example" + i + ".";
            trainingSet.registerExample(id, text, Arrays.asList(TOPIC_1));
            expectedCollectedIds.add(id);
            expectedCollectedText.add(text);
        }
        trainingSet.setBatchSize(10);
        Batch<Example> examples = trainingSet.getPositiveExamples(Arrays.asList(TOPIC_1, TOPIC_2), null);
        assertEquals(10, examples.items.size());
        for (Example example : examples.items) {
            collectedIds.add(example.id);
            collectedText.add(example.getContentString());
        }
        assertTrue(examples.hasMore);

        examples = trainingSet.getPositiveExamples(Arrays.asList(TOPIC_1, TOPIC_2), examples.nextOffset);
        assertEquals(10, examples.items.size());
        for (Example example : examples.items) {
            collectedIds.add(example.id);
            collectedText.add(example.getContentString());
        }
        assertTrue(examples.hasMore);

        examples = trainingSet.getPositiveExamples(Arrays.asList(TOPIC_1, TOPIC_2), examples.nextOffset);
        assertEquals(8, examples.items.size());
        for (Example example : examples.items) {
            collectedIds.add(example.id);
            collectedText.add(example.getContentString());
        }
        assertFalse(examples.hasMore);

        assertEquals(expectedCollectedIds, collectedIds);
        assertEquals(expectedCollectedText, collectedText);
    }

    @Test
    public void testBatchingNegativeExamplesAndAutoId() throws ConfigurationException, TrainingSetException {
        log.info(" --- testBatchingNegativeExamplesAndAutoId --- ");
        Set<String> expectedCollectedIds = new HashSet<String>();
        Set<String> expectedCollectedText = new HashSet<String>();
        Set<String> collectedIds = new HashSet<String>();
        Set<String> collectedText = new HashSet<String>();
        for (int i = 0; i < 17; i++) {
            String text = "Text of example" + i + ".";
            String id = trainingSet.registerExample(null, text, Arrays.asList(TOPIC_1));
            expectedCollectedIds.add(id);
            expectedCollectedText.add(text);
        }
        trainingSet.setBatchSize(10);
        Batch<Example> examples = trainingSet.getNegativeExamples(Arrays.asList(TOPIC_2), null);
        assertEquals(10, examples.items.size());
        for (Example example : examples.items) {
            collectedIds.add(example.id);
            collectedText.add(example.getContentString());
        }
        assertTrue(examples.hasMore);

        examples = trainingSet.getNegativeExamples(Arrays.asList(TOPIC_2), examples.nextOffset);
        assertEquals(7, examples.items.size());
        for (Example example : examples.items) {
            collectedIds.add(example.id);
            collectedText.add(example.getContentString());
        }
        assertFalse(examples.hasMore);

        assertEquals(expectedCollectedIds, collectedIds);
        assertEquals(expectedCollectedText, collectedText);
    }

    @Test
    public void testHasChangedSince() throws Exception {
        log.info(" --- testHasChangedSince --- ");
        Date date0 = new Date();
        assertFalse(trainingSet.hasChangedSince(Arrays.asList(TOPIC_1), date0));
        assertFalse(trainingSet.hasChangedSince(Arrays.asList(TOPIC_2), date0));
        assertFalse(trainingSet.hasChangedSince(Arrays.asList(TOPIC_3), date0));
        assertFalse(trainingSet.hasChangedSince(Arrays.asList(TOPIC_1, TOPIC_2), date0));
        assertFalse(trainingSet.hasChangedSince(Arrays.asList(TOPIC_1, TOPIC_3), date0));

        trainingSet.registerExample("example1", "Text of example1.", Arrays.asList(TOPIC_1));
        trainingSet.registerExample("example2", "Text of example2.", Arrays.asList(TOPIC_1, TOPIC_2));

        assertTrue(trainingSet.hasChangedSince(Arrays.asList(TOPIC_1), date0));
        assertTrue(trainingSet.hasChangedSince(Arrays.asList(TOPIC_2), date0));
        assertFalse(trainingSet.hasChangedSince(Arrays.asList(TOPIC_3), date0));
        assertTrue(trainingSet.hasChangedSince(Arrays.asList(TOPIC_1, TOPIC_2), date0));
        assertTrue(trainingSet.hasChangedSince(Arrays.asList(TOPIC_1, TOPIC_3), date0));

        // check that the new registration look as compared to a new date (who are stored up to the
        // millisecond precision):
        Thread.sleep(10);

        Date date1 = new Date();
        assertFalse(trainingSet.hasChangedSince(Arrays.asList(TOPIC_1), date1));
        assertFalse(trainingSet.hasChangedSince(Arrays.asList(TOPIC_2), date1));
        assertFalse(trainingSet.hasChangedSince(Arrays.asList(TOPIC_3), date1));
        assertFalse(trainingSet.hasChangedSince(Arrays.asList(TOPIC_1, TOPIC_2), date1));
        assertFalse(trainingSet.hasChangedSince(Arrays.asList(TOPIC_1, TOPIC_3), date1));
    }

    protected Hashtable<String,Object> getDefaultConfigParams() {
        Hashtable<String,Object> config = new Hashtable<String,Object>();
        config.put(SolrTrainingSet.SOLR_CORE, trainingsetSolrServer);
        config.put(SolrTrainingSet.TRAINING_SET_NAME, "test-training-set");
        config.put(SolrTrainingSet.EXAMPLE_ID_FIELD, "id");
        config.put(SolrTrainingSet.EXAMPLE_TEXT_FIELD, "text");
        config.put(SolrTrainingSet.TOPICS_URI_FIELD, "topics");
        config.put(SolrTrainingSet.MODIFICATION_DATE_FIELD, "modification_dt");
        return config;
    }
}

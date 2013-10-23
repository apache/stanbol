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
package org.apache.stanbol.entityhub.indexing.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class IndexerTest {
    
    /**
     * The number of Entities added to the {@link #testData}<p>
     * Should be > 100000 to test printing of the Indexing statistics after
     * 100000 entities.<p>
     * Note that the source and the indexed entities are kept in memory!
     */
    private static final int NUM_ENTITIES = 101000;
    
    /**
     * Holds the test data as defined by a static{} block
     */
    protected static final Map<String,Representation> testData = new HashMap<String,Representation>();
    /**
     * Hold the results of the indexing process
     */
    protected static final Map<String,Representation> indexedData = new HashMap<String,Representation>();
    protected static Logger log = LoggerFactory.getLogger(IndexerTest.class);
    private static String rootDir;
    private static IndexerFactory factory;
    
    private static final String DC_TITLE = NamespaceEnum.dcTerms+"title";
    private static final String DC_CREATED = NamespaceEnum.dcTerms+"created";
    private static final String DC_CREATOR = NamespaceEnum.dcTerms+"creator";
    private static final String RDF_TYPE = NamespaceEnum.rdf+"type";
    private static final String ENTITY_RANK = RdfResourceEnum.entityRank.getUri();
    private static final Set<String> EXPECTED_LANGUAGES = Collections.unmodifiableSet(
        new HashSet<String>(Arrays.asList("en","de")));
    private static final float EXPECTED_MAX_RANK = 100;
    private static final float MAX_INCOMMING = 10000;

    private static final String CONFIG_ROOT = 
        FilenameUtils.separatorsToSystem("indexerTests/");
    /**
     * mvn copies the resources in "src/test/resources" to target/test-classes.
     * This folder is than used as classpath.<p>
     * "/target/test-files/" does not exist, but is created by the
     * {@link IndexingConfig}.
     */
    private static final String TEST_ROOT = 
        FilenameUtils.separatorsToSystem("/target/test-files");
    private static String  userDir;
    private static String testRoot;
    /**
     * The methods resets the "user.dir" system property
     */
    @BeforeClass
    public static void initTestRootFolder(){
        String baseDir = System.getProperty("basedir");
        if(baseDir == null){
            baseDir = System.getProperty("user.dir");
        }
        //store the current user.dir
        userDir = System.getProperty("user.dir");
        testRoot = baseDir+TEST_ROOT;
        log.info("ConfigTest Root : "+testRoot);
        //set the user.dir to the testRoot (needed to test loading of missing
        //configurations via classpath
        //store the current user.dir and reset it after the tests
        System.setProperty("user.dir", testRoot);
        factory = IndexerFactory.getInstance();
    }
    /**
     * resets the "user.dir" system property the the original value
     */
    @AfterClass
    public static void cleanup(){
        System.setProperty("user.dir", userDir);
        indexedData.clear();
    }
    
    @Test
    public void testDataInteratingMode(){
        Indexer indexer = factory.create(CONFIG_ROOT+"dataIterating",CONFIG_ROOT+"idIterating");
        indexer.index();
        //check that all entities have been indexed
        validateAllIndexed();
    }
    @Test
    public void testEntityIdIteratingMode(){
        Indexer indexer = factory.create(CONFIG_ROOT+"idIterating",CONFIG_ROOT+"idIterating");
        indexer.index();
        //check that all entities have been indexed
        validateAllIndexed();
        
    }
    /**
     * validate the all the indexed resources!<p>
     * NOTE: That the asserts expect a specific configuration as provided by the
     * directory used to create the {@link IndexerFactory} used to initialise
     * the test.
     */
    private void validateAllIndexed() {
        assertEquals("Number of Indexed Entities "+indexedData.size()+
            "!= the Number of Source Entities "+NUM_ENTITIES,
            NUM_ENTITIES,indexedData.size());
        log.info("Validate Indexing Results:");
        float maxRank = 0;
        float minRank = EXPECTED_MAX_RANK;
        double rankSum = 0;
        for(Entry<String,Representation> entry : indexedData.entrySet()){
            assertEquals(entry.getKey(), entry.getValue().getId());
            float rank = validateIndexed(entry.getValue());
            if(rank > maxRank){
                maxRank = rank;
            }
            if(rank < minRank){
                minRank = rank;
            }
            rankSum += rank;
        }
        log.info("Entity Rank:");
        log.info(String.format(" - maximum %8.5f",maxRank));
        log.info(String.format(" - minimum %8.5f",minRank));
        //expected
        double expectedAverage = Math.log1p(MAX_INCOMMING/2)*EXPECTED_MAX_RANK/Math.log1p(MAX_INCOMMING);
        double average = rankSum/NUM_ENTITIES;
        log.info(String.format(" - average %8.5f (expected %8.5f) ",
            average, expectedAverage));
        assertTrue(String.format(
            "average score %8.5f is more than 5 precent lower than the expeded average %8.5f",
            average,expectedAverage),
            average > expectedAverage-(0.05*EXPECTED_MAX_RANK));
        assertTrue(String.format(
            "average score %8.5f is more than 5 precent higher than the expeded average %8.5f",
            average,expectedAverage),
            average < expectedAverage+(0.05*EXPECTED_MAX_RANK));
    }
    
    
    private float validateIndexed(Representation rep) {
        //first check that the dc-element fields are mapped to dc-terms
        Object value = rep.getFirst(DC_CREATOR);
        assertTrue(value instanceof String);
        value = rep.getFirst(DC_CREATED);
        assertTrue(value instanceof Date);
        for(Iterator<Object> types = rep.get(RDF_TYPE);types.hasNext();){
            value = types.next();
            assertTrue(value instanceof Reference);
            assertFalse(((Reference)value).getReference().isEmpty());
        }
        for(Iterator<Object> types = rep.get(DC_TITLE);types.hasNext();){
            value = types.next();
            assertTrue(value instanceof Text);
            assertFalse(((Text)value).getText().isEmpty());
            assertTrue(EXPECTED_LANGUAGES.contains(((Text)value).getLanguage()));
        }
        Float rankObject = rep.getFirst(ENTITY_RANK,Float.class);
        assertNotNull(rankObject);
        float rank = rankObject.floatValue();
        assertTrue("Rank"+rank+" > expected maximum "+EXPECTED_MAX_RANK,
            rank <= EXPECTED_MAX_RANK);
        assertTrue("Rank"+rank+" < expected maximum "+0,
            rank >= 0);
        return rank;
    }
    /*
     * Initialisation of the Test data stored in testData
     */
    static{
        ValueFactory vf = InMemoryValueFactory.getInstance();
        for(int i=0;i<NUM_ENTITIES;i++){
            Collection<Text> names = new ArrayList<Text>();
            Collection<Reference> types = new ArrayList<Reference>();
            if(i%2==0){
                if(i%5==0){
                    names.add(vf.createText("City "+i, "en"));
                    names.add(vf.createText("Stadt "+i,"de"));
                    types.add(vf.createReference(NamespaceEnum.dbpediaOnt+"City"));
                } else if(i%3==0){
                    names.add(vf.createText("Village "+i,"en"));
                    names.add(vf.createText("Gemeinde "+i,"de"));
                    types.add(vf.createReference(NamespaceEnum.dbpediaOnt+"PopulatedPlace"));
                } else {
                    names.add(vf.createText("Location "+i, "en"));
                    names.add(vf.createText("Platz "+i,"de"));
                }
                types.add(vf.createReference(NamespaceEnum.dbpediaOnt+"Place"));
            } else if(i%3==0){
                names.add(vf.createText("Person "+i,"en"));
                names.add(vf.createText("Person "+i,"de"));
                types.add(vf.createReference(NamespaceEnum.dbpediaOnt+"Person"));
            } else if(i%5==0){
                names.add(vf.createText("Organisation "+i,"en"));
                names.add(vf.createText("Organisation "+i,"de"));
                types.add(vf.createReference(NamespaceEnum.dbpediaOnt+"Organisation"));
            } else {
                names.add(vf.createText("Event "+i,"en"));
                names.add(vf.createText("Event "+i,"de"));
                types.add(vf.createReference(NamespaceEnum.dbpediaOnt+"Event"));
            }
            Representation rep = vf.createRepresentation("http://www.example.com/entity/test#entity-"+i);
            rep.add(NamespaceEnum.dcElements+"title", names);
            rep.add(NamespaceEnum.rdf+"type", types);
            rep.add(NamespaceEnum.dcElements+"created", new Date());
            rep.add(NamespaceEnum.dcElements+"creator", IndexerTest.class.getSimpleName());
            //use a random between [0..{MAX_INCOMMING}] as score
            Integer incomming = Integer.valueOf((int)Math.round((Math.random()*MAX_INCOMMING)));
            rep.add(RdfResourceEnum.entityRank.getUri(), incomming);
            testData.put(rep.getId(), rep);
        }
    }
}

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
package org.apache.stanbol.entityhub.indexing.source.jenatdb;

import java.util.Iterator;

import org.apache.commons.io.FilenameUtils;
import org.apache.stanbol.entityhub.indexing.core.EntityDataIterable;
import org.apache.stanbol.entityhub.indexing.core.EntityDataIterator;
import org.apache.stanbol.entityhub.indexing.core.EntityDataProvider;
import org.apache.stanbol.entityhub.indexing.core.EntityIterator;
import org.apache.stanbol.entityhub.indexing.core.EntityIterator.EntityScore;
import org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.*;

public class RdfIndexingSourceTest {
    
    
    private static final Logger log = LoggerFactory.getLogger(RdfIndexingSourceTest.class);

    
    private static final long NUMBER_OF_ENTITIES_EXPECTED = 5;
    
    private static final String CONFIG_ROOT = 
        FilenameUtils.separatorsToSystem("testConfigs/");
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
    }
    /**
     * resets the "user.dir" system property the the original value
     */
    @AfterClass
    public static void cleanup(){
        System.setProperty("user.dir", userDir);
    }
    @Test
    public void testEntityDataIterable(){
        IndexingConfig config = new IndexingConfig(CONFIG_ROOT+"iterable");
        EntityDataIterable iterable = config.getDataInterable();
        assertNotNull(iterable);
        assertEquals(iterable.getClass(), RdfIndexingSource.class);
        assertTrue(iterable.needsInitialisation());
        iterable.initialise();
        EntityDataIterator it = iterable.entityDataIterator();
        long count = 0;
        while(it.hasNext()){
            String entity = it.next();
            log.info("validate Entity "+entity);
            assertNotNull(entity);
            validateRepresentation(it.getRepresentation(), entity);
            count++;
        }
        //check if all entities where indexed
        //this checks if more entities are indexed as listed by the
        //textEntityIDs.txt file
        assertTrue(String.format("> %s Entities expected but only %s processed!",
            NUMBER_OF_ENTITIES_EXPECTED,count), 
            NUMBER_OF_ENTITIES_EXPECTED <= count);
    }
    @Test
    public void testEntityDataProvider(){
        IndexingConfig config = new IndexingConfig(CONFIG_ROOT+"provider");
        EntityIterator entityIdIterator = config.getEntityIdIterator();
        assertNotNull("Unable to perform test whithout EntityIterator",entityIdIterator);
        if(entityIdIterator.needsInitialisation()){
            entityIdIterator.initialise();
        }
        EntityDataProvider dataProvider = config.getEntityDataProvider();
        assertNotNull(dataProvider);
        assertTrue(dataProvider.needsInitialisation());//there are test data to load
        dataProvider.initialise();
        assertEquals(dataProvider.getClass(), RdfIndexingSource.class);
        long count = 0;
        while(entityIdIterator.hasNext()){
            EntityScore entityScore = entityIdIterator.next();
            assertNotNull(entityScore);
            assertNotNull(entityScore.id);
            validateRepresentation(dataProvider.getEntityData(entityScore.id),
                entityScore.id);
            count++;
        }
        //check if all entities where found
        assertEquals(String.format("%s Entities expected but %s processed!",
            NUMBER_OF_ENTITIES_EXPECTED,count), 
            NUMBER_OF_ENTITIES_EXPECTED, count);
    }

    /**
     * @param it
     * @param entity
     */
    private void validateRepresentation(Representation rep, String id) {
        assertNotNull("Representation for Entity with ID "+id+" is null",rep);
        assertEquals(id, rep.getId());
        //check if multiple languages are parsed correctly
        testText(rep);
        //TODO: need to add XSD dataTypes to the test data
        //testValue(rep, Double.class);
        testReference(rep);
    }
    private void testText(Representation rep){
        for(Iterator<String> fields = rep.getFieldNames();fields.hasNext();){
            String field = fields.next();
            Iterator<Text> values = rep.getText(field);
//            assertTrue(values.hasNext());
            while(values.hasNext()){
                Text text = values.next();
                assertNotNull(text);
                String lang = text.getLanguage();
                //log.info(text.getText()+" | "+text.getLanguage()+" | "+text.getText().endsWith("@"+lang));
                //this texts that the text does not contain the @{lang} as added by
                //the toString method of the RDF Literal java class
                assertFalse("Labels MUST NOT end with the Language! value="+text.getText(),
                    text.getText().endsWith("@"+lang));
            }
        }
    }
    private <T> void testValue(Representation rep, Class<T> type){
        for(Iterator<String> fields = rep.getFieldNames();fields.hasNext();){
            String field = fields.next();
            Iterator<T> values = rep.get(field,type);
            assertTrue(values.hasNext());
            while(values.hasNext()){
                T value = values.next();
                assertNotNull(value);
            }
        }
    }
    private void testReference(Representation rep){
        for(Iterator<String> fields = rep.getFieldNames();fields.hasNext();){
            String field = fields.next();
            Iterator<Reference> values = rep.getReferences(field);
//            assertTrue(values.hasNext());
            while(values.hasNext()){
                Reference ref = values.next();
                assertNotNull(ref);
            }
        }
    }
}

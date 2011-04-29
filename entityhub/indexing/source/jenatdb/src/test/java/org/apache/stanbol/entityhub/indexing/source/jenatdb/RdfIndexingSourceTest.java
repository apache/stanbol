package org.apache.stanbol.entityhub.indexing.source.jenatdb;

import java.util.Iterator;

import org.apache.stanbol.entityhub.indexing.core.EntityDataIterable;
import org.apache.stanbol.entityhub.indexing.core.EntityDataIterator;
import org.apache.stanbol.entityhub.indexing.core.EntityDataProvider;
import org.apache.stanbol.entityhub.indexing.core.EntityIterator;
import org.apache.stanbol.entityhub.indexing.core.EntityIterator.EntityScore;
import org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.*;

public class RdfIndexingSourceTest {
    
    
    private static final Logger log = LoggerFactory.getLogger(RdfIndexingSourceTest.class);
    /**
     * mvn copies the resources in "src/test/resources" to target/test-classes
     */
    private static final String TEST_CONFIGS_ROOT = "/target/test-classes/testConfigs/";

    private static final String TEXT_TEST_FIELD = "http://www.geonames.org/ontology#alternateName";
    private static final String VALUE_TEST_FIELD = "http://www.w3.org/2003/01/geo/wgs84_pos#lat";
    private static final String REFERENCE_TEST_FIELD = "http://www.w3.org/2002/07/owl#sameAs";
    
    private static final long NUMBER_OF_ENTITIES_EXPECTED = 3;
    
    /**
     * The path to the folder used as root for the tests
     */
    private static String testRoot;
    @BeforeClass
    public static void init(){
        //initialise based on basedir or user.dir
        String baseDir = System.getProperty("basedir");
        if(baseDir == null){
            baseDir = System.getProperty("user.dir");
        }
        testRoot = baseDir+TEST_CONFIGS_ROOT;
        log.info("Test Root ="+testRoot);
    }
    @Test
    public void testEntityDataIterable(){
        IndexingConfig config = new IndexingConfig(testRoot+"iterable");
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
        //check if all entities where found
        assertEquals(String.format("%s Entities expected but %s processed!",
            NUMBER_OF_ENTITIES_EXPECTED,count), 
            NUMBER_OF_ENTITIES_EXPECTED, count);
    }
    @Test
    public void testEntityDataProvider(){
        IndexingConfig config = new IndexingConfig(testRoot+"provider");
        EntityIterator entityIdIterator = config.getEntityIdIterator();
        assertNotNull("Unable to perform test whithout EntityIterator",entityIdIterator);
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
        Iterator<Text> values = rep.getText(TEXT_TEST_FIELD);
        assertTrue(values.hasNext());
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
    private <T> void testValue(Representation rep, Class<T> type){
        Iterator<T> values = rep.get(VALUE_TEST_FIELD,type);
        assertTrue(values.hasNext());
        while(values.hasNext()){
            T value = values.next();
            assertNotNull(value);
        }
    }
    private void testReference(Representation rep){
        Iterator<Reference> values = rep.getReferences(REFERENCE_TEST_FIELD);
        assertTrue(values.hasNext());
        while(values.hasNext()){
            Reference ref = values.next();
            assertNotNull(ref);
        }
    }
}

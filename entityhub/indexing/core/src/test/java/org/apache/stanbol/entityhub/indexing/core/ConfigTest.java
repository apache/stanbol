package org.apache.stanbol.entityhub.indexing.core;

import java.util.HashMap;
import java.util.Map;

import org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig;
import org.apache.stanbol.entityhub.indexing.core.normaliser.MinScoreNormalizer;
import org.apache.stanbol.entityhub.indexing.core.normaliser.NaturalLogNormaliser;
import org.apache.stanbol.entityhub.indexing.core.normaliser.RangeNormaliser;
import org.apache.stanbol.entityhub.indexing.core.normaliser.ScoreNormaliser;
import org.apache.stanbol.entityhub.indexing.core.source.LineBasedEntityIterator;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigTest {
    private static final Logger log = LoggerFactory.getLogger(ConfigTest.class);
    /**
     * mvn copies the resources in "src/test/resources" to target/test-classes
     */
    private static final String TEST_CONFIGS_ROOT = "/target/test-classes/testConfigs/";
    private static String testRoot;
    @BeforeClass
    public static void initTestRootFolder(){
        String baseDir = System.getProperty("basedir");
        if(baseDir == null){
            baseDir = System.getProperty("user.dir");
        }
        testRoot = baseDir+TEST_CONFIGS_ROOT;
        log.info("ConfigTest Root ="+testRoot);
    }
    @Test(expected=IllegalArgumentException.class)
    public void missingRoot(){
        new IndexingConfig(); //there is no indexing folder in the user.dir
    }
    @Test(expected=IllegalArgumentException.class)
    public void missingConfigDir(){
        new IndexingConfig(testRoot+"missingconfig");
    }
    @Test
    public void loadSimpleConfigDir(){
        IndexingConfig config = new IndexingConfig(testRoot+"simple");
        //test the name
        assertEquals(config.getName(),"simple");
        assertEquals(config.getDescription(), "Simple Configuration");
        //test if the normaliser configuration was parsed correctly!
        final ScoreNormaliser normaliser = config.getNormaliser();
        ScoreNormaliser testNormaliser = normaliser;
        assertNotNull(testNormaliser);
        assertEquals(testNormaliser.getClass(), RangeNormaliser.class);
        testNormaliser = testNormaliser.getChained();
        assertNotNull(testNormaliser);
        assertEquals(testNormaliser.getClass(), NaturalLogNormaliser.class);
        testNormaliser = testNormaliser.getChained();
        assertNotNull(testNormaliser);
        assertEquals(testNormaliser.getClass(), MinScoreNormalizer.class);
        EntityIterator entityIterator = config.getEntityIdIterator();
        assertNotNull(entityIterator);
        assertEquals(entityIterator.getClass(), LineBasedEntityIterator.class);
        Map<String,Float> entityIds = new HashMap<String,Float>();
        //the values test if the normaliser configuration was readed correctly
        //the keys if the configured entiyScore file was configured correctly
        float boost = 10f/(float)Math.log1p(100);
        entityIds.put("http://www.example.org/entity/test", Float.valueOf(10));
        entityIds.put("http://www.example.org/entity/test2", Float.valueOf((float)(Math.log1p(10)*boost)));
        entityIds.put("http://www.example.org/entity/test3", Float.valueOf(-1));
        while(entityIterator.hasNext()){
            EntityIterator.EntityScore entityScore = entityIterator.next();
            Float expectedScore = entityIds.remove(entityScore.id);
            assertNotNull("Entity with ID "+entityScore.id+" not found!",expectedScore);
            Float score = normaliser.normalise(entityScore.score);
            assertTrue("Entity score "+score+" is not the expected "+expectedScore,expectedScore.compareTo(score)==0);
        }
        assertTrue(entityIds.isEmpty());
        EntityProcessor processor = config.getEntityProcessor();
        assertNotNull(processor);
    }
    

}

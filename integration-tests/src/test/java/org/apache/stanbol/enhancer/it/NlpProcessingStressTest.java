package org.apache.stanbol.enhancer.it;

import org.junit.Test;

public class NlpProcessingStressTest extends MultiThreadedTestBase {

    public static final String PROPER_NOUN_LINKING_CHAIN = "dbpedia-proper-noun";
    
    public NlpProcessingStressTest(){
        super();
    }

    @Test
    public void testProperNounLinking() throws Exception {
        TestSettings settings = new TestSettings();
        settings.setChain(PROPER_NOUN_LINKING_CHAIN);
        //use the default for the rest of the tests
        performTest(settings);
    }

}

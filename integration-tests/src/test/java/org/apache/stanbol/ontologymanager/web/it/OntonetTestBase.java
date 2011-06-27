package org.apache.stanbol.ontologymanager.web.it;

import static org.junit.Assert.fail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.stanbol.commons.testing.http.RetryLoop;
import org.apache.stanbol.commons.testing.stanbol.StanbolTestBase;
import org.junit.Before;

/** 
 * Inherit from this to wait for all to be up before running tests.
 */
public class OntonetTestBase extends StanbolTestBase {
    
    private final Logger log = LoggerFactory.getLogger(getClass());

    // TODO configurable via system properties??
    public static final int ENGINES_TIMEOUT_SECONDS = 60;
    public static final int WAIT_BETWEEN_TRIES_MSEC = 1000;
    
    static boolean enginesReady;
    static boolean timedOut;
    
    @Before
    public void checkEnginesReady() throws Exception {
    
        // Check only once per test run
        if(enginesReady) {
            return;
        }
        
        // If we timed out previously, don't waste time checking again
        if(timedOut) {
            fail("Timeout in previous check of ontonet engine, cannot run tests");
        }
        
        final RetryLoop.Condition c = new RetryLoop.Condition() {
            
            @Override
            public boolean isTrue() throws Exception {
                executor.execute(
                        builder.buildGetRequest("/ontonet")
                        .withHeader("Accept", "text/html")
                )
                .assertStatus(200)
                .assertContentType("text/html");
                
                log.info("Ontonet checked, engine is ready.");
                return true;
            }
            
            @Override
            public String getDescription() {
                return "Checking that Ontonet engine is ready";
            }
        };
        
        new RetryLoop(c, ENGINES_TIMEOUT_SECONDS, WAIT_BETWEEN_TRIES_MSEC) {
            @Override
            protected void reportException(Throwable t) {
                log.info("Exception in RetryLoop, will retry for up to " 
                        + getRemainingTimeSeconds() + " seconds: " + t);
            }
            
            protected void onTimeout() {
                timedOut = true;
            }
        };
        
        enginesReady = true;
    }
}

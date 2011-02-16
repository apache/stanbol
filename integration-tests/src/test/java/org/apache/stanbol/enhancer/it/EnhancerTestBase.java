/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.stanbol.enhancer.it;

import static org.junit.Assert.fail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.stanbol.commons.testing.http.RetryLoop;
import org.apache.stanbol.commons.testing.stanbol.StanbolTestBase;
import org.junit.Before;

/** Inherit from this to wait for all default enhancement 
 *  engines to be up before running tests.
 */
public class EnhancerTestBase extends StanbolTestBase {
    
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
            fail("Timeout in previous check of enhancement engines, cannot run tests");
        }
        
        // We'll retry the check for all engines to be ready
        // for up to ENGINES_TIMEOUT_SECONDS 
        final RetryLoop.Condition c = new RetryLoop.Condition() {
            
            @Override
            public boolean isTrue() throws Exception {
                /*  List of expected engines could be made configurable via system
                 *  properties, but we don't expect it to change often. 
                 */
                executor.execute(
                        builder.buildGetRequest("/engines")
                        .withHeader("Accept", "text/plain")
                )
                .assertStatus(200)
                .assertContentType("text/plain")
                .assertContentRegexp(
                    "org.apache.stanbol.*MetaxaEngine",
                    "org.apache.stanbol.*LangIdEnhancementEngine",
                    "org.apache.stanbol.*NamedEntityExtractionEnhancementEngine",
                    "org.apache.stanbol.*OpenCalaisEngine",
                    "org.apache.stanbol.*EntityMentionEnhancementEngine",
                    "org.apache.stanbol.*RelatedTopicEnhancementEngine",
                    "org.apache.stanbol.*CachingDereferencerEngine"
                );
                log.info("Enhancement engines checked, all present");
                return true;
            }
            
            @Override
            public String getDescription() {
                return "Checking that all enhancement engines are ready";
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

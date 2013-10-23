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
package org.apache.stanbol.commons.solr.web.it;

import static org.junit.Assert.fail;

import org.apache.stanbol.commons.testing.http.RetryLoop;
import org.apache.stanbol.commons.testing.stanbol.StanbolTestBase;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrDispatchFilterComponentTestBase extends StanbolTestBase {
    
    private final Logger log = LoggerFactory.getLogger(SolrDispatchFilterComponentTestBase.class);
    
    public static final int TIMEOUT_SECONDS = 60;
    public static final int WAIT_BETWEEN_TRIES_MSEC = 1000;
    
    private final String prefix;
    private final String coreName;
    private boolean coreReady;
    private boolean timedOut;
    

    protected SolrDispatchFilterComponentTestBase(String prefix,String coreName){
        if(prefix == null || prefix.isEmpty()){
            throw new IllegalArgumentException("The parsed prefix MUST NOT be NULL nor empty");
        }
        if(coreName == null || coreName.isEmpty()){
            throw new IllegalArgumentException("The parsed name of the SolrCore MUST NOT be NULL nor empty");
        }
        if(!prefix.endsWith("/")){
            prefix = prefix+'/';
        }
        this.prefix = prefix;
        this.coreName = coreName;
    }
    
    @Before
    public void checkEnginesReady() throws Exception {
    
        // Check only once per test run
        if(coreReady) {
            return;
        }
        
        // If we timed out previously, don't waste time checking again
        if(timedOut) {
            fail("Timeout in previous check of enhancement engines, cannot run tests");
        }
        
        // We'll retry to send ping requests for to the configured core 
        final RetryLoop.Condition c = new RetryLoop.Condition() {
            
            @Override
            public boolean isTrue() throws Exception {
                /*  List of expected engines could be made configurable via system
                 *  properties, but we don't expect it to change often. 
                 */
                executor.execute(
                        builder.buildGetRequest(getCorePath()+"admin/ping")
                )
                .assertStatus(200)
                .assertContentRegexp("<str name=\"status\">OK</str>");
                
                log.info("Solr Core {} is ready and mapped to {}",coreName,prefix);
                return true;
            }
            
            @Override
            public String getDescription() {
                return "Checking that SolrCore "+coreName+" is ready";
            }
        };
        
        new RetryLoop(c, TIMEOUT_SECONDS, WAIT_BETWEEN_TRIES_MSEC) {
            @Override
            protected void reportException(Throwable t) {
                log.info("Exception in RetryLoop, will retry for up to " 
                        + getRemainingTimeSeconds() + " seconds: " + t);
            }
            
            protected void onTimeout() {
                timedOut = true;
            }
        };
        
        coreReady = true;
    }
    /**
     * The path to the configured Solr core 
     * @return <code>prefix+coreName+'/'</code>
     */
    public String getCorePath(){
        return prefix+coreName+'/';
    }
}

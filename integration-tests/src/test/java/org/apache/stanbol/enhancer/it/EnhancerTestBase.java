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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

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
    
    boolean enginesReady;
    boolean timedOut;

    protected String endpoint;
    /*  List of expected engines could be made configurable via system
     *  properties, but we don't expect it to change often. 
     */
    protected final String[] assertEngines; 
    
    /**
     * The "/enhancer" endpoint"
     */
    public static final String ENHANCER_ENDPOINT = "/enhancer";
    /**
     * The default endpoint
     * @see #ENHANCER_ENDPOINT
     */
    public static final String DEFAULT_ENDPOINT = ENHANCER_ENDPOINT;
    /**
     * The "/engines" endpoint the only endpoint supported to enhance 
     * content items before STANBOL-431. This endpoint is still supported
     */
    public static final String ENGINES_ENDPOINT = "/engines";
    /**
     * The root for the endpoints of specific enhancement chains
     * @see #getChainEndpoint(String)
     */
    private static final String CHAINS_ROOT = "/enhancer/chain/";
    
    private static final String[] DEFAULT_ASSERT_ENGINES = 
            new String[]{
                //"metaxa","MetaxaEngine", deactivated see STANBOL-510
                "langdetect","LanguageDetectionEnhancementEngine",
                "ner","NamedEntityExtractionEnhancementEngine",
                "entityhubExtraction","NamedEntityExtractionEnhancementEngine",
                "dbpediaLinking","NamedEntityTaggingEngine",
                "tika","TikaEngine"                    
            };
    /**
     * Getter for the Endpoint for a specific enhancement chain
     * @param chainName the name of the chain
     * @return the endpoint
     * @throws IllegalArgumentException if the parsed chain is <code>null</code>
     * or invalid
     */
    public static final String getChainEndpoint(String chainName){
        if(chainName == null || chainName.isEmpty()){
            throw new IllegalArgumentException("The parsed Chain name MUST NOT BE NULL nor empty!");
        }
        if(chainName.charAt(0) == '/'){
            if(chainName.length()<2){
                throw new IllegalArgumentException("The parsed Chain name '/' is invalid!");
            }
            return CHAINS_ROOT+chainName.substring(1);
        } else {
            return CHAINS_ROOT+chainName;
        }
    }
    
    public EnhancerTestBase(){
        this(null, (String[])null);
    }
    public EnhancerTestBase(String endpoint){
        this(endpoint,(String[])null);
    }
    public EnhancerTestBase(String endpoint,String...assertEngines){
        super();
        setEndpoint(endpoint);
        if(assertEngines == null){
            this.assertEngines = DEFAULT_ASSERT_ENGINES;
        } else {
            this.assertEngines = assertEngines;
        }
    }

    /**
     * Setter for the endpoint. Keeps care of leading '/' and supports optional query parameter
     * @param endpoint the endpoint or <code>null</code> to use the default
     * @param params optional query parameter(s) [key,value,key,value,...]
     */
    protected void setEndpoint(String endpoint,String...params) {
        StringBuilder sb = new StringBuilder();
        if(endpoint == null){
            sb.append(DEFAULT_ENDPOINT);;
        } else if(endpoint.charAt(0) != '/')
            sb.append("/").append(endpoint);
        else{
            sb.append(endpoint);
        }
        if(params != null && params.length > 1){
            for(int i=0;i<params.length-1;i++){
                sb.append(i==0?'?':'&');
                sb.append(params[i]).append('=');
                i++;
                try {
                    sb.append(URLEncoder.encode(params[i], "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalStateException(e.getMessage(),e);
                }
            }
        }
        this.endpoint = sb.toString();
    }
    public String getEndpoint(){
        return endpoint;
    }
    @Before
    public void checkEnginesReady() throws Exception {
        log.debug("before {}#checkEngineReady", getClass().getSimpleName());
        // Check only once per test run
        if(enginesReady) {
            log.debug("   ... engines already marked as ready");
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
                log.debug("> checking for Enhancer services: ");
                executor.execute(
                    builder.buildGetRequest(endpoint)
                    .withHeader("Accept", "text/html")
                )
                .assertStatus(200)
                .assertContentType("text/html")
                .assertContentRegexp(assertEngines);
                log.info("  ... enpoint '{}' is ready", endpoint);
                /*  List of expected referencedSites could also be made 
                 *  configurable via system properties, but we don't expect it 
                 *  to change often. 
                 */
                executor.execute(
                        builder.buildGetRequest("/entityhub/sites/referenced")
                        .withHeader("Accept", "application/json")
                )
                .assertStatus(200)
                .assertContentType("application/json")
                .assertContentRegexp(
                    "http:\\\\/\\\\/.*\\\\/entityhub\\\\/site\\\\/dbpedia\\\\/"
                );
                log.debug("  ... entityhub DBpedia referencedSite is ready", endpoint);
                //also assert that the SolrYard for the dbpedia site is fully
                //initialized
                //No longer needed with STANBOL-996
//                executor.execute(
//                        builder.buildGetRequest("/entityhub/site/dbpedia" +
//                        		"/entity?id=urn:does:not:exist:f82js95xsig39s.23987")
//                        .withHeader("Accept", "application/json"))
//                .assertStatus(404);
                log.info("Enhancement engines checked for '{}', all present", endpoint);
                return true;
            }
            
            @Override
            public String getDescription() {
                return String.format("Checking that all enhancement engines for " +
                		"endpoint '%s' are ready",endpoint);
            }
        };
        
        new RetryLoop(c, ENGINES_TIMEOUT_SECONDS, WAIT_BETWEEN_TRIES_MSEC) {
            @Override
            protected void reportException(Throwable t) {
                log.info("Exception in RetryLoop, will retry for up to " 
                        + getRemainingTimeSeconds() + " seconds: ", t);
            }
            
            protected void onTimeout() {
                timedOut = true;
            }
        };
        
        enginesReady = true;
    }
}

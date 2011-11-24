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
package org.apache.stanbol.reasoners.it.concurrency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.stanbol.commons.jobs.api.JobInfo;
import org.apache.stanbol.reasoners.test.ReasonersTestBase;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * To test background jobs with parallel client requests.
 * 
 */
public class ReasonersConcurrencyTest extends ReasonersTestBase{

    private static final String TEST_URL = "http://xmlns.com/foaf/spec/index.rdf";

    // The number of jobs to start is = (services * tasks * SCALE)
    // Increase this to multiply the number of calls
    private static final int SCALE = 1;

    private ExecutorService executor;
    
    static Logger log = LoggerFactory.getLogger(ReasonersConcurrencyTest.class);
    
    static int counter = 0;
    
    @Before
    public void setUp() throws Exception {
        executor = Executors.newCachedThreadPool();
        counter = 0;
    }

    @After
    public void tearDown() throws Exception {
        executor.shutdown();
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            throw new Exception("Timeout while waiting for termination");
        }
        log.info("Done {} calls",counter);
        counter = 0;
    }
    
    /**
     * Execute a set of http calls to start jobs.
     * Then ping the jobs until they are done.
     * 
     * Both starts and pings are executed as set of parallel threads.
     * 
     * @throws Exception
     */
    @Test
    public void basicConcurrencyTest() throws Exception{
        log.info("basicConcurrencyTest()");
        // We start in parallel a set of background jobs
        List<JobClient> tasks = buildStarters();
        
        List<String> locations = new ArrayList<String>();
        List<Future<Result>> futures = executor.invokeAll(tasks);
        for (Future<Result> future : futures) {
            String location = future.get().assertResult().getResponse().getFirstHeader("Location").getValue();
            log.info("job created: {}",location);
            locations.add(location);
        }
        
        // We ping in parallel all jobs.
        // On each iteration, we prepare a new set of calls only on jobs
        // which are not terminated
        List<String> done = new ArrayList<String>();
        while((done.size() < locations.size())){
            // List of calls
            tasks = new ArrayList<JobClient>();
            // Prepare a Pinger on each unfinished job
            for(String l : locations){
                if(!done.contains(l)){
                    tasks.add(new Pinger(l));
                }
            }
            // Invoke all unfinished jobs in parallel
            futures = executor.invokeAll(tasks);
            // Query each response
            for (Future<Result> future : futures) {
                PingerResult pr = (PingerResult) future.get();
                String content = pr.assertResult().getContentString();
                // Explore JSON here
                log.info("Content:\n\n{}\n\n",content);
                JSONObject json = new JSONObject(content);
                String status = json.getString("status");
                
                String location = pr.location();
                if(status.equals(JobInfo.FINISHED)){
                    log.info("{} is done!", location);
                    done.add(location);
                }else{
                    log.info("{} is still working ... ", location);
                }
            }
        }
    }
    
    /**
     * Builds a list of calls which start jobs.
     * By default, returns a JobCLient for each service on each task.
     * Do this SCALE times.
     * 
     * @return
     */
    private List<JobClient> buildStarters(){
        List<JobClient> tasks = new ArrayList<JobClient>();
        for(int i= 0; i<SCALE; i++){
            for(String s : allServices()){
                for(String t: TASKS){
                    tasks.add(new Starter(s, t, "url", TEST_URL));
                }
            }
        }
        return tasks;
    }
    
    /***********************************************************
     * Utility classes & methods
     ***********************************************************/
    private abstract class JobClient implements Callable<Result> {
        abstract URI uri(String queryString);
        
        private List<Header> headers = new ArrayList<Header>();
        
        protected void addHeader(String key, String value){
            headers.add(new BasicHeader(key,value));
        }
        
        protected HttpResponse get() throws Exception{
            return get(new String[0]);
        }
        
        protected HttpResponse get(String... params) throws Exception{
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet();
            request.setURI(uri(buildQuerystring(params)));
            
            log.debug("Sending request[{}]: {} ",counter, request.getURI().toString());
            
            // Increment global counter
            counter++;
            
            return client.execute(request);
        }
        
        protected String buildQuerystring(String... params){
            StringBuilder qsb = new StringBuilder();
            
            for(int i=0; i<params.length; i=i+2){
                if(i==0){
                    qsb.append("?");
                }else{
                    qsb.append("&");
                }
                qsb.append(params[i]);
                qsb.append("=");
                qsb.append(params[i+1]);
            }
            return qsb.toString();
        }
    }
    
    private class Pinger extends JobClient {
        String location = null;
        
        Pinger(String location){
            this.location = location;
        }
        
        URI uri(String queryString){
            return URI.create(location + queryString);
        }
        
        public PingerResult call() throws Exception {
            // We ping the Job service with mime type application/json
            this.addHeader("Accept", "application/json");
            return new PingerResult(location, get());
        }
    }

    private class Starter extends JobClient {
        String service = null;
        String task = null;
        String[] queryString = null;
        
        Starter(String service, String task, String... queryParameters){
            this.service = service;
            this.task = task;
            this.queryString = queryParameters;
        }

        URI uri(String queryString) {
            return URI.create(ReasonersConcurrencyTest.this.builder.buildUrl(REASONERS_PATH + service
                                                                             + task + "/job" + queryString));
        }
        
        @Override
        public StarterResult call() throws Exception {
            return new StarterResult(get(queryString));
        }
    }

    abstract class Result {
        protected HttpResponse response = null;
        
        Result(HttpResponse response){
            this.response = response;
        }
        abstract Result assertResult();
         
        public HttpResponse getResponse(){
            return response;
        }
        public String getContentString() throws IllegalStateException, IOException{
            return IOUtils.toString(this.response.getEntity().getContent());
        }
    }

    private class PingerResult extends Result {
        
        private String location= null;
        
        PingerResult(String location, HttpResponse response){
            super(response);
            this.location = location;
        }
        
        String location(){
            return location;
        }
        
        /**
         * We assert that: 
         * - The job must exists (response code 200) 
         * - The Content-type header returns JSON 
         */
        @Override
        public Result assertResult() {
            assertNotNull("Response cannot be null", response);
            assertEquals("Result of a ping request must be 200 (Job must exists)", 200, response.getStatusLine().getStatusCode());
            assertEquals("Content type must be application/json", "application/json", response.getFirstHeader("Content-type").getValue());
            return this;
        }
    }

    private class StarterResult extends Result {
        
        StarterResult(HttpResponse response){
            super(response);
        }
        
        @Override
        public Result assertResult() {
            assertNotNull("Response cannot be null", response);
            // Response of a start request must be 201
            assertEquals(201, response.getStatusLine().getStatusCode());
            // Response must contain the Location header
            assertNotNull(response.getFirstHeader("Location"));
            // The location header must be unique
            assertTrue(response.getHeaders("Location").length == 1);
            // The location value must be a valid URL
            try {
                URI.create(response.getFirstHeader("Location").getValue()).toURL();
            } catch (MalformedURLException e) {
                assertTrue("Malformed url in location header",false);
            }
            return this;
        }
    }
}

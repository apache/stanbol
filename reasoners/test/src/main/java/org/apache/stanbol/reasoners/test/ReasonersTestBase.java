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
package org.apache.stanbol.reasoners.test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.stanbol.commons.jobs.api.JobInfo;
import org.apache.stanbol.commons.testing.http.Request;
import org.apache.stanbol.commons.testing.stanbol.StanbolTestBase;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mini framework for writing services tests for running services
 * 
 * @author enridaga
 *
 */
public class ReasonersTestBase extends StanbolTestBase{
    protected final String REASONERS_PATH = "/reasoners";
    protected final String JOBS_PATH = "/jobs";
    protected final String[] SERVICES = {"/owl", "/owlmini", "/rdfs"};
    protected final String[] TASKS = {"/check", "/classify", "/enrich"};

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected Request buildMultipartRequest(String path,MultipartEntity multiPart) {
        HttpPost httpPost = new HttpPost(builder.buildUrl(path));
        httpPost.setEntity(multiPart);
        /**
         * In case of Multipart requests, we must __NOT__ set the content-type header to multipart/form-data.
         * If we do it, we have a 400 response (bad request).
         */
        return this.builder.buildOtherRequest(httpPost);
    }

    protected List<String> allServices() {
        List<String> sl = new ArrayList<String>();
        sl.addAll(Arrays.asList(SERVICES));
        return sl;
    }
    

    /** 
     * Create a job, returns the location of the job.
     * We also assert that:
     * - response status code is 201
     * - Location header do exists, and is unique (no multiple values allowed)
     * - The value of the Location header muyst be a valid URI
     * 
     * @param request
     * @return
     * @throws Exception
     */
    protected String createJob(Request request) throws Exception{
        HttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(request.getRequest());
        // Response status code must be 201 Created
        assertEquals(201, response.getStatusLine().getStatusCode());
        // Job location must be in the Location: http header
        // Location header must exists and must be unique
        assertTrue(response.getHeaders("Location").length==1);
        // Analyze the location header
        Header locationHeader = response.getFirstHeader("Location");
        String location = locationHeader.getValue();
        // Location must be a valid URI
        URI locationURI = URI.create(location);
        // We do *not* check here if the body of the response contains a description
        return locationURI.toString();
    }
    
    /** 
     * Create the job and check for its status until it is complete
     * 
     * @param request
     * @throws Exception
     */
    protected void executeAndPingSingleJob(Request request) throws Exception{
        log.info("Executing: {}", request.getRequest().getURI());
        String location = createJob(request);
        log.info("Job location is {}", location);
        // Get the result and ping the jId
        pingSingleJob(location);
    }
    
    /** 
     * Check for the status of a job, pinging it each 0.5s until it is ready.
     * It does **not** invoke the result.
     * Asks for the application/json representation of the job.
     * 
     * We assert that:
     * - The job must exists (response code 200)
     * - The Content-type header returns JSON
     * - The content contains valid JSON
     * 
     * @param location
     * @throws Exception
     */
    protected void pingSingleJob(String location) throws Exception{
        log.info("Start pinging {} ... ", location);
        boolean waiting = true;
        while(waiting){
            Request req = builder.buildOtherRequest(new HttpGet(location));
            req.withHeader("Accept", "application/json");
            log.info("Ping method: {}", req.getRequest().getMethod());
            log.info("Ping location: {}", req.getRequest().getURI());
            req.getRequest().setHeader("Accept","application/json");
            log.info("headers:");
            for(Header h : req.getRequest().getAllHeaders()){
                log.info("{}: {}", h.getName(), h.getValue());
            }
            log.info("Request line:\n\n {} \n\n", req.getRequest().getRequestLine().toString());
            try{
                String content = executor.execute(req).assertStatus(200).assertContentType("application/json").getContent();
                log.info("JSON content:\n\n {} \n\n",content);
                JSONObject json = new JSONObject(content);
                String status = json.getString("status");
                
                if(status.equals(JobInfo.RUNNING)){
                    log.info(" ... still working (wait for 0.5s)");
                    Thread.sleep(500);
                }else{
                    waiting = false;
                    log.info(" ... done!");
                }
            }catch(Exception e){
                log.error("An error occurred",e);
                assertTrue(false);
            }
        }
    }
}

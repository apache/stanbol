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
package org.apache.stanbol.reasoners.it.offline;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.stanbol.commons.testing.http.Request;
import org.apache.stanbol.commons.testing.http.RequestDocumentor;
import org.apache.stanbol.commons.testing.stanbol.StanbolTestBase;
import org.apache.stanbol.reasoners.test.ReasonersTestBase;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * All these tests are based on POST with content-type multipart/form-data.
 * 
 * A file is sent to the service. This file does not contain owl:imports.
 * 
 */
public class ReasonersOfflineTest extends ReasonersTestBase {
	protected static String fileName = null;
	protected static String inconsistentFileName = null;
    protected String fileParam = "file";
    protected static File file = null;
    protected MultipartEntity multiPart = null;

    private final Logger log = LoggerFactory.getLogger(getClass());

    @BeforeClass
    public static void prepare() throws URISyntaxException {
        fileName = ReasonersOfflineTest.class.getResource(System.getProperty("file.separator") + "foaf.rdf")
                .toString();
        inconsistentFileName = ReasonersOfflineTest.class.getResource(System.getProperty("file.separator") + "inconsistent-types.rdf")
                .toString(); 
        // This is shared among all tests
        file = new File(URI.create(fileName));
        assertTrue(file.exists());
        assertTrue(file.canRead());
    }

    @Before
    public void setupMultipart() {
        FileBody bin = new FileBody(file);
        multiPart = new MultipartEntity();
        multiPart.addPart(fileParam, bin);
    }


    @Test
    public void testMethodNotAllowed() throws Exception {
        // POST is not allowed on the base path
        // No urlencoded
        executor.execute(
            builder.buildPostRequest(REASONERS_PATH).withHeader("Content-type",
                "application/x-www-form-urlencoded")).assertStatus(405);
        // No multipart
        executor.execute(buildMultipartRequest(REASONERS_PATH, multiPart)).assertStatus(405);

        // PUT is not allowed on services
        List<String> sl = allServices();
        for (String s : sl) {
            for(String t : TASKS){
                String url = builder.buildUrl(REASONERS_PATH + s + t);
                executor.execute(builder.buildOtherRequest(new HttpPut(url)).withContent("Nothing"))
                        .assertStatus(405);
                log.info("PUT requests are not supported by {}", url);
            }
        }

    }

    @Test
    public void testNotAcceptable() throws Exception {
     // Same for all requests to services without a defined task
        List<String> sl = allServices();
        for (String s : sl) {
            for(String t : TASKS){
                String url = REASONERS_PATH + s + t;
                executor.execute(builder.buildGetRequest(url).withHeader("Accept","NOT/ACCEPTABLE"))
                        .assertStatus(406);
                log.info("PUT requests are not supported by {}", url);
            }
        }
    }

    @Test
    public void testServiceNotFound() throws Exception {
        String url = REASONERS_PATH + "/service-not-found";
        executor.execute(builder.buildGetRequest(url)).assertStatus(404);
        log.info("Correct answer on unsupported service: {}", url);
    }

    @Test
    public void testTaskNotFound() throws Exception {
        List<String> sl = allServices();
        for (String s : sl) {
            String url = REASONERS_PATH + s + "/task-not-found";
            executor.execute(builder.buildGetRequest(url)).assertStatus(404);
            log.info("Correct answer on unsupported task: {}", url);
        }
    }

    @Test
    public void testPostMultipartOwlEnrichTurtle() throws ParseException,
                                                  ClientProtocolException,
                                                  IOException {
        executor.execute(
            buildMultipartRequest("/reasoners/owl/enrich", multiPart).withHeader("Accept", "application/turtle"))
                .assertStatus(200)
                .assertContentType("application/turtle")
                .assertContentContains("<http://www.w3.org/2002/07/owl#disjointWith>",
                    "<http://www.w3.org/2002/07/owl#sameAs>",
                    "<http://www.w3.org/2002/07/owl#intersectionOf>", " a ");

    }
    
    @Test
    public void testPostMultipartRdfsEnrichN3() throws ParseException, ClientProtocolException, IOException {
        executor.execute(
            buildMultipartRequest("/reasoners/rdfs/enrich", multiPart).withHeader("Accept", "text/n3"))
                .assertStatus(200)
                .assertContentType("text/n3")
                .assertContentContains("<http://xmlns.com/foaf/0.1/>");

    }
     
    @Test
    public void testPostMultipartOwlminiEnrichRDFXML() throws ParseException,
                                                      ClientProtocolException,
                                                      IOException {
        executor.execute(
            buildMultipartRequest("/reasoners/owlmini/enrich", multiPart).withHeader("Accept", "application/rdf+xml"))
                .assertStatus(200)
                .assertContentType("application/rdf+xml")
                .assertContentContains("rdf:RDF","rdf:Description","http://www.w3.org/2001/XMLSchema#time",
                    "http://www.w3.org/2000/01/rdf-schema#subClassOf",
                    "http://www.w3.org/2002/07/owl#equivalentClass");

    }
    
    @Test
    public void testPostMultipartConsistency200() throws Exception{
       	String[] services = {"/owl","/owlmini"};
    	// Consistent
    	for(String s : services){
    		executor.execute(buildMultipartRequest("/reasoners" + s + "/check", multiPart)).assertStatus(200);
    	}
    }
    
    @Test
    public void testPostMultipartConsistency409() throws Exception{
    	
        FileBody bin = new FileBody(new File(URI.create(inconsistentFileName)));
        MultipartEntity incMultiPart = new MultipartEntity();
        incMultiPart.addPart(fileParam, bin);

    	String[] services = {"/owl","/owlmini"};
   
    	// Not consistent
    	for(String s : services){
    		executor.execute(buildMultipartRequest("/reasoners" + s + "/check", incMultiPart)).assertStatus(409);
    	}
    }
    
    /**
     * We test here all acceptable return types
     */
    @Test
    public void testOutputFormats() throws Exception{
        log.info("testOutputFormats()");
        final String[] formats = {
                                  // Each group of 3 elements is: Accept header, Expected content-type,
                                  // Expected regexp
                                  "text/html",
                                  "text/html",
                                  ".*Reasoners: Result.*",

                                  "application/rdf+xml",
                                  "application/rdf+xml",
                                  "rdf:RDF",
/* FIXME This is not supported by the OWLApi JAX-RS writer
                                  "text/n3",
                                  "text/n3",
                                  "<.*www.w3.org/2000/01/rdf-schema.*>",
*/
                                  "application/turtle",
                                  "application/turtle",
                                  "<.*www.w3.org/2000/01/rdf-schema.*>",

/* FIXME This is not supported by the OWLApi JAX-RS writer
                                  "text/plain",
                                  "text/plain",
                                  "<.*www.w3.org/2000/01/rdf-schema.*>"
*/
        };
        List<String> sl = allServices();
        for (String s : sl) {
            for(String t : TASKS){
                String url = REASONERS_PATH + s + t;
                for (int i = 0; i < formats.length; i += 3) {
                    executor.execute(
                        buildMultipartRequest(url, multiPart).withHeader("Accept", formats[i]))
                            .assertStatus(200)
                            .assertContentType(formats[i + 1]);
                    // Only if task is not CHECK
                    if(t.equals("/check")){
                        // FIXME
                        // Check should not support these formats or it should return something compatible
                        // Now returns the sentence
                        executor.assertContentContains("The input is consistent");
                    }else{
                        executor
                                .assertContentRegexp(formats[i + 2]);
                    }
                    log.info("Service {} supports return type {}",url,formats[i]);
                }
            }
        }
    }
}

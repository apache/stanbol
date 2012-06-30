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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import javax.ws.rs.core.MediaType;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.core.serializedform.UnsupportedFormatException;
import org.apache.clerezza.rdf.jena.parser.JenaParserProvider;
import org.apache.clerezza.rdf.rdfjson.parser.RdfJsonParsingProvider;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.stanbol.commons.testing.http.Request;
import org.apache.stanbol.commons.testing.http.RequestExecutor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Test that the default chain is called by requesting the "/enhancer" endpoint. */
public class MultiThreadedTest extends EnhancerTestBase {
    
    
    public static final String PROPERTY_TEST_DATA = "stanbol.it.multithreadtest.data";
    public static final String PROPERTY_TEST_DATA_TYPE = "stanbol.it.multithreadtest.media-type";
    public static final String PROPERTY_TEST_DATA_PROPERTY = "stanbol.it.multithreadtest.data-property";
    public static final String PROPERTY_THREADS = "stanbol.it.multithreadtest.threads";
    public static final String PROPERTY_REQUESTS = "stanbol.it.multithreadtest.requests";
    
    private static final Logger log = LoggerFactory.getLogger(MultiThreadedTest.class);
    
    public final static int DEFAULT_NUM_THREADS = 5;
    public final static int DEFAULT_NUM_REQUESTS = 500;
    public final static String DEFAULT_TEST_DATA = "10k_long_abstracts_en.nt.bz2";
    public final static String DEFAULT_TEST_DATA_PROPERTY = "http://dbpedia.org/ontology/abstract";
    
    private static Parser rdfParser;
    private static Iterator<String> testDataIterator;
    /*
     * We need here a custom http client that uses a connection pool 
     */
    protected DefaultHttpClient pooledHttpClient;
    private BasicHttpParams httpParams;
    private PoolingClientConnectionManager connectionManager;
    
    public MultiThreadedTest(){
        super(null,new String[]{});
    }
    protected MultiThreadedTest(String endpoint){
        super(endpoint);
    }
    protected MultiThreadedTest(String endpoint,String...assertEngines){
        super(endpoint,assertEngines);
    }

    @BeforeClass
    public static void init() throws IOException {
        //init the RDF parser
        rdfParser = new Parser();
        rdfParser.bindParsingProvider(new JenaParserProvider());
        rdfParser.bindParsingProvider(new RdfJsonParsingProvider());
        //init theTestData
        initTestData();
    }
    
    
    @Before
    public void initialiseHttpClient() {
        if(this.pooledHttpClient == null){ //init for the first test
            httpParams = new BasicHttpParams();
            httpParams.setParameter(CoreProtocolPNames.USER_AGENT, "Stanbol Integration Test");
            httpParams.setBooleanParameter(ClientPNames.HANDLE_REDIRECTS,true);
            httpParams.setIntParameter(ClientPNames.MAX_REDIRECTS,3);
            httpParams.setBooleanParameter(CoreConnectionPNames.SO_KEEPALIVE,true);
    
            connectionManager = new PoolingClientConnectionManager();
            connectionManager.setMaxTotal(20);
            connectionManager.setDefaultMaxPerRoute(20);
    
            pooledHttpClient = new DefaultHttpClient(connectionManager,httpParams);
        }
    }
    
    @Test
    public void testMultipleParallelRequests() throws Exception {
        Integer maxRequests = Integer.getInteger(PROPERTY_REQUESTS,DEFAULT_NUM_REQUESTS);
        if(maxRequests.intValue() <= 0){
            maxRequests = Integer.MAX_VALUE;
        }
        Integer numThreads = Integer.getInteger(PROPERTY_THREADS,DEFAULT_NUM_THREADS);
        if(numThreads <= 0){
            numThreads = DEFAULT_NUM_THREADS;
        }
        log.info("Start Multi Thread testing of max. {} requests using {} threads",
            maxRequests,numThreads);
        ExcutionTracker tracker = new ExcutionTracker(Executors.newFixedThreadPool(numThreads));
        int testNum;
        for(testNum = 0;testDataIterator.hasNext() && testNum < maxRequests; testNum++){
            String test = testDataIterator.next();
            Request request = builder.buildPostRequest(getEndpoint())
                    .withHeader("Accept","text/rdf+nt")
                    .withContent(test);
            tracker.register(request);
            if(testNum%100 == 0){
                log.info("  ... sent {} Requests ({} finished, {} pending, {} failed",
                    new Object[]{testNum,tracker.getNumCompleted(),
                                 tracker.getPending().size(),tracker.getFailed().size()});
            }
        }
        log.info("> All {} requests sent!",testNum);
        log.info("  ... wait for all requests to complete");
        while(!tracker.getPending().isEmpty()){
            tracker.wait(3);
            log.info("  ... {} finished, {} pending, {} failed",
                new Object[]{tracker.getNumCompleted(),tracker.getPending().size(),tracker.getFailed().size()});
        }
        Assert.assertTrue(tracker.getFailed()+"/"+numThreads+" failed", tracker.getFailed().isEmpty());
    }
    
    /* -------------------------------------------------------------
     * Utilities for reading the Test Data from the defined source
     * -------------------------------------------------------------
     */
    
    private static void initTestData() throws IOException {
        String testData = System.getProperty(PROPERTY_TEST_DATA, DEFAULT_TEST_DATA);
        File testFile = new File(testData);
        InputStream is = null;
        if(testFile.isFile()){
            is = new FileInputStream(testFile);
        } 
        if(is == null) {
            is = MultiThreadedTest.class.getClassLoader().getResourceAsStream(testData);
        }
        if(is == null){
           is = ClassLoader.getSystemResourceAsStream(testData);
        }
        if(is == null){
            try {
              is = new URL(testData).openStream();
            }catch (MalformedURLException e) {
                //not a URL
            }
        }
        Assert.assertNotNull("Unable to load the parsed TestData '"
            +testData+"'!", is != null);
        
        String name = FilenameUtils.getName(testData);
        if ("gz".equalsIgnoreCase(FilenameUtils.getExtension(name))) {
            is = new GZIPInputStream(is);
            name = FilenameUtils.removeExtension(name);
            log.debug("   - from GZIP Archive");
        } else if ("bz2".equalsIgnoreCase(FilenameUtils.getExtension(name))) {
            is = new BZip2CompressorInputStream(is);
            name = FilenameUtils.removeExtension(name);
            log.debug("   - from BZip2 Archive");
        } else if ("zip".equalsIgnoreCase(FilenameUtils.getExtension(name))) {
            ZipArchiveInputStream zipin = new ZipArchiveInputStream(is);
            ArchiveEntry entry = zipin.getNextEntry();
            log.info("For ZIP archives only the 1st Entry will be processed!");
            name = FilenameUtils.getName(entry.getName());
            log.info("  - processed Entry: {}",entry.getName());
        } // else uncompressed data ...
        
        String mediaTypeString = System.getProperty(PROPERTY_TEST_DATA_TYPE);
        MediaType mediaType;
        if(mediaTypeString != null){
            mediaType = MediaType.valueOf(mediaTypeString);
        } else { //parse based on extension
            String ext = FilenameUtils.getExtension(name);
            if("txt".equalsIgnoreCase(ext)){
                mediaType = MediaType.TEXT_PLAIN_TYPE;
            } else if("rdf".equalsIgnoreCase(ext)){
                mediaType = MediaType.valueOf(SupportedFormat.RDF_XML);
            } else if("xml".equalsIgnoreCase(ext)){
                mediaType = MediaType.valueOf(SupportedFormat.RDF_XML);
            } else if("ttl".equalsIgnoreCase(ext)){
                mediaType = MediaType.valueOf(SupportedFormat.TURTLE);
            } else if("n3".equalsIgnoreCase(ext)){
                mediaType = MediaType.valueOf(SupportedFormat.N3);
            } else if("nt".equalsIgnoreCase(ext)){
                mediaType = MediaType.valueOf(SupportedFormat.N_TRIPLE);
            } else if("json".equalsIgnoreCase(ext)){
                mediaType = MediaType.valueOf(SupportedFormat.RDF_JSON);
            } else if(name.indexOf('.')<0){ //no extension
                mediaType = MediaType.TEXT_PLAIN_TYPE; //try plain text
            } else {
                log.info("Unkown File Extension {} for resource name {}",
                    ext,name);
                mediaType = null;
            }
        }
        Assert.assertNotNull("Unable to detect MediaType for Resource '"
            + name+"'. Please use the property '"+PROPERTY_TEST_DATA_TYPE
            + "' to manually parse the MediaType!", mediaType);
        
        //now init the iterator for the test data
        testDataIterator = mediaType.isCompatible(MediaType.TEXT_PLAIN_TYPE) ?
            createTextDataIterator(is, mediaType) :
            createRdfDataIterator(is,mediaType);
    }
    /**
     * @param is
     * @param mediaType
     */
    private static Iterator<String> createRdfDataIterator(InputStream is, MediaType mediaType) {
        final SimpleMGraph graph = new SimpleMGraph();
        try {
            rdfParser.parse(graph, is, mediaType.toString());
        } catch (UnsupportedFormatException e) {
            Assert.fail("The MimeType '"+mediaType+"' of the parsed testData "
                + "is not supported. This utility supports plain text files as "
                + "as well as the RDF formats "+rdfParser.getSupportedFormats()
                + "If your test data uses one of those formats but it was not "
                + "correctly detected you can use the System property '"
                + PROPERTY_TEST_DATA_TYPE + "' to manually parse the Media-Type!");
        }
        return new Iterator<String>() {
            Iterator<Triple> it = null;
            String next = null;
            private String getNext(){
                if(it == null){
                    UriRef property;
                    String propertyString = System.getProperty(PROPERTY_TEST_DATA_PROPERTY,DEFAULT_TEST_DATA_PROPERTY);
                    propertyString.trim();
                    if("*".equals(propertyString)){
                        property = null; //wildcard
                    } else {
                        property = new UriRef(propertyString);
                    }
                    it = graph.filter(null, property, null);
                }
                while(it.hasNext()){
                    Resource value = it.next().getObject();
                    if(value instanceof Literal){
                        return ((Literal)value).getLexicalForm();
                    }
                }
                return null; //no more data
            }
            
            @Override
            public boolean hasNext() {
                if(next == null){
                    next = getNext();
                }
                return next != null;
            }

            @Override
            public String next() {
                if(next == null){
                    next = getNext();
                }
                if(next == null){
                    throw new NoSuchElementException("No further testData available");
                } else {
                    String elem = next;
                    next = null;
                    return elem;
                }
                
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
            
        };
    }
    /**
     * @param is
     * @param mediaType
     */
    private static Iterator<String> createTextDataIterator(InputStream is, MediaType mediaType) {
        String charsetString = mediaType.getParameters().get("charset");
        Charset charset = Charset.forName(charsetString == null ? "UTF-8" : charsetString);
        log.info("  ... using charset {} for parsing Text data",charset);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is, charset));
        return new Iterator<String>() {
            String next = null;
            private String getNext(){
                String line;
                StringBuilder data = new StringBuilder();
                int emtptyLines = 0;
                try {
                    while((line = reader.readLine()) != null && emtptyLines < 2){
                        data.append(line).append('\n');
                        if(line.isEmpty()){
                            emtptyLines++;
                        } else {
                            emtptyLines = 0;
                        }
                    }
                } catch (IOException e) {
                    log.warn("IOException while reading from Stream",e);
                    Assert.fail("IOException while reading from Stream");
                }
                return data.length() == 0 ? null : data.toString();
            }
            @Override
            public boolean hasNext() {
                if(next == null){
                    next = getNext();
                }
                return next != null;
            }

            @Override
            public String next() {
                if(next == null){
                    next = getNext();
                }
                if(next == null){
                    throw new NoSuchElementException("No further testData available");
                } else {
                    String elem = next;
                    next = null;
                    return elem;
                }
                
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
            
        };
    }
    
    
    
    /* -------------------------------------------------------------
     * Utilities for executing and tracking the concurrent Requests
     * -------------------------------------------------------------
     */

    private class ExcutionTracker {
        
        private int completed = 0;
        private final Set<Request> registered = Collections.synchronizedSet(new HashSet<Request>());
        private final List<Request> failed = Collections.synchronizedList(new ArrayList<Request>());
        private ExecutorService executorService;
        
        public ExcutionTracker(ExecutorService executorService) {
            this.executorService = executorService;
        }
        
        public void register(Request request){
            registered.add(request);
            executorService.execute(new AsyncExecuter(request, this));
        }

        public void succeed(Request request) {
            if(registered.remove(request)){
                completed++;
            }
        }

        public void failed(Request request) {
            if(registered.remove(request)){
                completed++;
            }
            failed.add(request);
        }
        
        public Set<Request> getPending(){
            return registered;
        }
        
        public List<Request> getFailed(){
            return failed;
        }
        public int getNumCompleted(){
            return completed;
        }
        public void wait(int seconds){
            try {
                executorService.awaitTermination(seconds, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }
        }
        
    }
    private class AsyncExecuter implements Runnable{

        private Request request;
        private ExcutionTracker tracker;
        protected AsyncExecuter(Request request, ExcutionTracker tracker){
            this.request = request;
            this.tracker = tracker;
        }
        @Override
        public void run() {
            try {
                RequestExecutor executor = new RequestExecutor(pooledHttpClient);
                executor.execute(request).assertStatus(200);
                tracker.succeed(request);
            } catch (Throwable e) {
                log.warn("Error while sending Request ",e);
                tracker.failed(request);
            }
        }
        
    }

}

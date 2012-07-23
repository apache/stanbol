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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import javax.ws.rs.core.MediaType;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
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
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.commons.testing.http.Request;
import org.apache.stanbol.commons.testing.http.RequestExecutor;
import org.apache.stanbol.enhancer.servicesapi.helper.execution.Execution;
import org.apache.stanbol.enhancer.servicesapi.helper.execution.ExecutionMetadata;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Test that the default chain is called by requesting the "/enhancer" endpoint. */
public class MultiThreadedTest extends EnhancerTestBase {
    
    /**
     * The name of the Enhancement Chain this test runs against. If not defined
     * the default chain is used.
     */
    public static final String PROPERTY_CHAIN = "stanbol.it.multithreadtest.chain";
    /**
     * The reference to the test data. Can be a File, a Resource available via the
     * Classpath or an URL. This also supports compressed files. In case of ZIP
     * only the first entry is processed.
     */
    public static final String PROPERTY_TEST_DATA = "stanbol.it.multithreadtest.data";
    /**
     * Can be used to explicitly parse the Media-Type of the test data. If not set
     * the Media-Type is parsed based on the file extension.
     */
    public static final String PROPERTY_TEST_DATA_TYPE = "stanbol.it.multithreadtest.media-type";
    /**
     * The RDF property used to filter triples their values are used as texts for
     * Enhancer requests. Only used of test data are provided as RDF<p>
     * Note:<ul>
     * <li> Only triples where their Object are Literals are used
     * <li> the default property is "http://dbpedia.org/ontology/abstract"
     * <li> if set to "*" than all triples with literal values are used.
     * </ul>
     */
    public static final String PROPERTY_TEST_DATA_PROPERTY = "stanbol.it.multithreadtest.data-property";
    /**
     * The maximum number of concurrent requests
     */
    public static final String PROPERTY_THREADS = "stanbol.it.multithreadtest.threads";
    /**
     * The maximum number of requests. Can be used to limit the number of requests if
     * the provided data do contain more samples.
     */
    public static final String PROPERTY_REQUESTS = "stanbol.it.multithreadtest.requests";
    /**
     * The RDF serialisation used as Accept header for Stanbol Enhancer requests
     */
    public static final String PROPERTY_RDF_FORMAT = "stanbol.it.multithreadtest.rdf-format";
    
    private static final Logger log = LoggerFactory.getLogger(MultiThreadedTest.class);
    
    public final static int DEFAULT_NUM_THREADS = 5;
    public final static int DEFAULT_NUM_REQUESTS = 500;
    public final static String DEFAULT_RDF_FORMAT = SupportedFormat.RDF_JSON;
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
        this(System.getProperty(PROPERTY_CHAIN),new String[]{});
    }
    protected MultiThreadedTest(String chain){
        this(chain,new String[]{});
    }
    protected MultiThreadedTest(String chain,String...assertEngines){
        super(null,assertEngines);
        if(chain != null && !chain.isEmpty()){
            log.info("Testing with Enhancement Chain '{}'",chain);
            this.endpoint = endpoint+"/chain/"+chain;
        } else { //else no chain configured ... use default
            log.info("Testing default Enhancement Chain");
        }
        //add the parameter for the execution metadata
        this.endpoint = this.endpoint+"?executionmetadata=true";
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
        ExcutionTracker tracker = new ExcutionTracker(
            Executors.newFixedThreadPool(numThreads),
            Math.max(100, numThreads*5));
        String rdfFormat = System.getProperty(PROPERTY_RDF_FORMAT,DEFAULT_RDF_FORMAT);
        int testNum;
        for(testNum = 0;testDataIterator.hasNext() && testNum < maxRequests; testNum++){
            String test = testDataIterator.next();
            Request request = builder.buildPostRequest(getEndpoint())
                    .withHeader("Accept",rdfFormat)
                    .withContent(test);
            tracker.register(request);
            if(testNum%100 == 0){
                log.info("  ... sent {} Requests ({} finished, {} pending, {} failed",
                    new Object[]{testNum,tracker.getNumCompleted(),
                                 tracker.getNumPending(),tracker.getFailed().size()});
            }
        }
        log.info("> All {} requests sent!",testNum);
        log.info("  ... wait for all requests to complete");
        while(tracker.getNumPending() > 0){
            tracker.wait(3);
            log.info("  ... {} finished, {} pending, {} failed",
                new Object[]{tracker.getNumCompleted(),tracker.getNumPending(),tracker.getFailed().size()});
        }
        log.info("Multi Thread testing of {} requests (failed: {}) using {} threads completed",
            new Object[]{tracker.getNumCompleted(),tracker.getFailed().size(),numThreads});
        tracker.printStatistics();
        Assert.assertTrue(tracker.getFailed()+"/"+numThreads+" failed", tracker.getFailed().isEmpty());
        tracker = null;
    }
    
    @After
    public final void close(){
        httpParams = null;
        pooledHttpClient = null;
        connectionManager.shutdown();
        connectionManager = null;
    }
    
    @AfterClass
    public static final void cleanup(){
        testDataIterator = null;
    }
    
    /* -------------------------------------------------------------
     * Utilities for reading the Test Data from the defined source
     * -------------------------------------------------------------
     */
    
    private static void initTestData() throws IOException {
        String testData = System.getProperty(PROPERTY_TEST_DATA, DEFAULT_TEST_DATA);
        log.info("Read Testdata from '{}'",testData);
        File testFile = new File(testData);
        InputStream is = null;
        if(testFile.isFile()){
            log.info(" ... init from File");
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
              log.info(" ... init from URL");
            }catch (MalformedURLException e) {
                //not a URL
            }
        } else {
            log.info(" ... init via Classpath");
        }
        Assert.assertNotNull("Unable to load the parsed TestData '"
            +testData+"'!", is);
        log.info("  - InputStream: {}", is == null ? null: is.getClass().getSimpleName());
        
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
        } else { // else uncompressed data ...
            log.info("  - uncompressed source: {}",name);
        }
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
        
        log.info("  - Media-Type: {}", mediaType);
        //now init the iterator for the test data
        testDataIterator = mediaType.isCompatible(MediaType.TEXT_PLAIN_TYPE) ?
            createTextDataIterator(is, mediaType) :
            createRdfDataIterator(is, mediaType);
    }
    /**
     * Iterator implementation that parses an RDF graph from the parsed
     * {@link InputStream}. The RDF data are loaded in-memory. Because of this
     * only test data that fit in-memory can be used. <p>
     * Literal values (objects) of the {@link #PROPERTY_TEST_DATA_PROPERTY} are
     * used as data. If this property is not present {@link #DEFAULT_TEST_DATA_PROPERTY}
     * is used. If {@link #PROPERTY_TEST_DATA_PROPERTY} is set to '*' than all
     * Triples with Literal values are used.<p>
     * This supports all RDF-formats supported by the {@link JenaParserProvider} and
     * {@link RdfJsonParsingProvider}. The charset is expected to be UTF-8.
     * @param is the input stream providing the RDF test data.
     * @param mediaType the Media-Type of the stream. MUST BE supported by
     * the Apache Clerezza RDF parsers.
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
        IOUtils.closeQuietly(is);
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
                        log.info("Iterate over values of all Triples");
                    } else {
                        propertyString = NamespaceEnum.getFullName(propertyString);
                        property = new UriRef(propertyString);
                        log.info("Iterate over values of property {}", propertyString);
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
     * Iterator reading Content elements from the input stream. Two (ore more)
     * empty lines are used to separate multiple content items.<p>
     * NOTE: This iterator does not keep the whole text in-memory. Therefore
     * it can be possible used to process test data that would not fit
     * in-memory.
     * @param is The input stream to read the data from
     * @param mediaType the Media-Type - only used to parse the charset from. If
     * no charset is specified UTF-8 is uses as default.
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
                        if(line.isEmpty()){
                            if(data.length() != 0){
                                emtptyLines++;
                            } //do not count empty lines at the beginning!
                        } else {
                            emtptyLines = 0;
                        }
                        data.append(line).append('\n');
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
        
        
        
        private int maxRegistered;

        
        private int completed = 0;
        private final Set<Request> registered = new HashSet<Request>();
        private final List<HttpResponse> failed = Collections.synchronizedList(new ArrayList<HttpResponse>());
        
        private ExecutionStatistics statistics = new ExecutionStatistics();
        
        private ExecutorService executorService;
        
        protected ExcutionTracker(ExecutorService executorService){
            this(executorService,100);
        }
        public ExcutionTracker(ExecutorService executorService,int maxRegistered) {
            this.executorService = executorService;
            this.maxRegistered = maxRegistered <= 0 ? Integer.MAX_VALUE : maxRegistered;
        }
        
        public void register(Request request){
            synchronized (registered) {
                while(registered.size() >= maxRegistered){
                    try {
                        registered.wait();
                    } catch (InterruptedException e) {
                        //interrupped
                    }
                }
                registered.add(request);
                executorService.execute(new AsyncExecuter(request, this));
            }
        }

        void succeed(Request request, UriRef contentItemUri, TripleCollection results,Long rtt) {
            ExecutionMetadata em = ExecutionMetadata.parseFrom(results, (UriRef)contentItemUri);
            results.clear(); //we no longer need the results
            if(em != null){
                synchronized (statistics) {
                    statistics.addResult(em,rtt);
                }
            } //no executionData available ... unable to collect statistics
            synchronized (registered) {
                if(registered.remove(request)){
                    completed++;
                    registered.notifyAll();
                }
            }
        }

        void failed(Request request, RequestExecutor executor) {
            synchronized (registered) {
                failed.add(executor.getResponse());
                if(registered.remove(request)){
                    completed++;
                    registered.notifyAll();
                }
            }
        }
        
        public int getNumPending(){
            synchronized (registered) {
                return registered.size();
            }
        }
        /**
         * Live list of the failed requests. Non basic access MUST BE
         * syncronized on the list while the requests are still pending as newly
         * failed requests will modify this list
         * @return
         */
        public List<HttpResponse> getFailed(){
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
        public void printStatistics(){
            log.info("Statistics:");
            synchronized (statistics) {
                log.info("Chain:");
                log.info("  Round Trip Time (Server + Transfer + Client):");
                if(statistics.getNumRtt() < 1){
                    log.info("    - not available");
                } else {
                    log.info("     max: {}ms | min: {}ms | avr: {}ms over {} requests",
                        new Object[]{statistics.getMaxRtt(),
                                     statistics.getMinRtt(),
                                     statistics.getAverageRtt(),
                                     statistics.getNumRtt()});
                }
                log.info("  processing time (server side)");
                if(statistics.getNumSamples() < 1){
                    log.info("    - not available. Make shure the used "
                        + "EnhancementJobManager supports ExecutionMetadata!");
                } else {
                    log.info("     max: {}ms | min: {}ms | avr: {}ms over {} requests",
                        new Object[]{statistics.getMaxDuration(),
                                     statistics.getMinDuration(),
                                     statistics.getAverageDuration(),
                                     statistics.getNumSamples()});
                    log.info("Enhancement Engines");
                    for(String name :statistics.getEngineNames()){
                        log.info("  {}: max: {}ms | min: {}ms | avr: {}ms over {} requests",
                            new Object[]{name,
                                         statistics.getMaxDuration(name),
                                         statistics.getMinDuration(name),
                                         statistics.getAverage(name),
                                         statistics.getNumSamples(name)});
                    }
                }
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
            RequestExecutor executor = new RequestExecutor(pooledHttpClient);
            long start = System.currentTimeMillis();
            Long rtt;
            try {
                executor.execute(request).assertStatus(200);
                rtt = System.currentTimeMillis()-start;
            } catch (Throwable e) {
                log.warn("Error while sending Request ",e);
                tracker.failed(request,executor);
                rtt = null;
            }
            IndexedMGraph graph = new IndexedMGraph();
            try {
                rdfParser.parse(graph,executor.getStream(), executor.getContentType().getMimeType());
            }catch (Exception e) {
                Assert.fail("Unable to parse RDF data from Response with Content-Type "
                    + executor.getContentType().getMimeType()+" ( "+e.getClass().getSimpleName()
                    + ": "+e.getMessage()+")");
            }
//            log.info("Content:\n{}",executor.getContent());
//            
//            log.info("Triples");
//            for(Triple t : graph){
//                log.info(t.toString());
//            }
            Iterator<Triple> ciIt = graph.filter(null, Properties.ENHANCER_EXTRACTED_FROM, null);
            Assert.assertTrue("Enhancement Results do not caontain a single Enhancement",ciIt.hasNext());
            Resource contentItemUri = ciIt.next().getObject();
            Assert.assertTrue("ContentItem URI is not an UriRef but an instance of "
                    + contentItemUri.getClass().getSimpleName(), contentItemUri instanceof UriRef);
            tracker.succeed(request,(UriRef)contentItemUri,graph,rtt);
        }
    }

    private class ExecutionStatistics {
        private int numSamples;
        private long maxDuration = -1;
        private long minDuration = Long.MAX_VALUE;
        private long sumDuration = 0;
        private int numRtt;
        private long maxRtt = -1;
        private long minRtt = Long.MAX_VALUE;
        private long sumRtt = 0;
        
        private Map<String, long[]> engineStats = new TreeMap<String,long[]>();
        
        void addResult(ExecutionMetadata em,Long roundTripTime){
            Long durationNumber = em.getChainExecution().getDuration();
            long duration;
            if(durationNumber != null){
                duration = durationNumber.longValue();
                if(duration > maxDuration){
                    maxDuration = duration;
                }
                if(duration < minDuration){
                    minDuration = duration;
                }
                sumDuration = sumDuration+duration;
                numSamples++;
            }
            if(roundTripTime != null){
                long rtt = roundTripTime;
                if(rtt > maxRtt){
                    maxRtt = rtt;
                }
                if(rtt < minRtt){
                    minRtt = rtt;
                }
                sumRtt = sumRtt+rtt;
                numRtt++;
            }
            for(Entry<String,Execution> ex : em.getEngineExecutions().entrySet()){
                long[] stats = engineStats.get(ex.getKey());
                if(stats == null){
                    stats = new long[]{-1L,Long.MAX_VALUE,0L,0L};
                    engineStats.put(ex.getKey(), stats);
                }
                durationNumber = ex.getValue().getDuration();
                if(durationNumber != null){
                    duration = durationNumber.longValue();
                    if(duration > stats[0]){ //max duration
                        stats[0] = duration;
                    }
                    if(duration < stats[1]){ //min duration
                        stats[1] = duration;
                    }
                    stats[2] = stats[2]+duration; //sum duration
                    stats[3]++; //num Samples
                }
            }
        }
        
        
        public Set<String> getEngineNames(){
            return engineStats.keySet();
        }
        public Long getMaxDuration(){
            return maxDuration < 0 ? null : maxDuration;
        }
        public Long getMinDuration(){
            return minDuration == Long.MAX_VALUE ? null : minDuration;
        }
        public Long getAverageDuration(){
            return sumDuration <= 0 && numSamples <= 0 ? null : Math.round((double)sumDuration/(double)numSamples);
        }
        public int getNumSamples(){
            return numSamples;
        }
        public Long getMaxRtt(){
            return maxRtt < 0 ? null : maxRtt;
        }
        public Long getMinRtt(){
            return minRtt == Long.MAX_VALUE ? null : minRtt;
        }
        public Long getAverageRtt(){
            return sumRtt <= 0 && numRtt <= 0 ? null : Math.round((double)sumRtt/(double)numRtt);
        }
        public int getNumRtt(){
            return numRtt;
        }
        public Long getMaxDuration(String engine){
            long[] stats = engineStats.get(engine);
            return stats == null ? null : stats[0];
        }
        public Long getMinDuration(String engine){
            long[] stats = engineStats.get(engine);
            return stats == null ? null : stats[1];
        }
        public Long getAverage(String engine){
            long[] stats = engineStats.get(engine);
            return stats == null && stats[2] <= 0 && stats[3] <= 0 ? 
                    null : Math.round((double)stats[2]/(double)stats[3]);
        }
        public int getNumSamples(String engine){
            long[] stats = engineStats.get(engine);
            return stats == null ? null : (int)stats[3];
        }
        
    }
    
    
}

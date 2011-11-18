package org.apache.stanbol.reasoners.it.concurrency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.stanbol.reasoners.test.ReasonersTestBase;
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
        
        List<String> jids = new ArrayList<String>();
        List<Future<Result>> futures = executor.invokeAll(tasks);
        for (Future<Result> future : futures) {
            String j = future.get().assertResult().getContentString();
            log.info("Got job id: {}",j);
            jids.add(j);
        }
        
        // We ping in parallel all jobs.
        // On each iteration, we prepare a new set of calls only on jobs
        // which are not terminated
        List<String> done = new ArrayList<String>();
        while((done.size() < jids.size())){
            // List of calls
            tasks = new ArrayList<JobClient>();
            // Prepare a Pinger on each unfinished job
            for(String j : jids){
                if(!done.contains(j)){
                    tasks.add(new Pinger(j));
                }
            }
            // Invoke all unfinished jobs in parallel
            futures = executor.invokeAll(tasks);
            // Query each response
            for (Future<Result> future : futures) {
                PingerResult pr = (PingerResult) future.get();
                String r = pr.assertResult().getContentString();
                String jid = pr.jid();
                if(!r.equals("Job is still working")){
                    log.info("{} is done!", jid);
                    done.add(jid);
                }else{
                    log.info("{} is still working ... ",jid);
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
        String jid = null;
        
        Pinger(String jid){
            this.jid = jid;
        }
        
        URI uri(String queryString){
            return URI.create(ReasonersConcurrencyTest.this.builder.buildUrl(REASONERS_PATH+"/jobs/ping/"+jid+queryString));
        }
        
        public PingerResult call() throws Exception {
            return new PingerResult(jid, get());
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
        private String jid= null;
        PingerResult(String jid, HttpResponse response){
            super(response);
            this.jid = jid;
        }
        
        String jid(){
            return jid;
        }
        @Override
        public Result assertResult() {
            // Result of a ping request must be 200
            assertNotNull(this.toString(), response);
            assertEquals(200, response.getStatusLine().getStatusCode());
            return this;
        }
    }

    private class StarterResult extends Result {
        
        StarterResult(HttpResponse response){
            super(response);
        }
        
        @Override
        public Result assertResult() {
            // Result of a start request must be 200
            assertNotNull(this.toString(), response);
            assertEquals(200, response.getStatusLine().getStatusCode());
            return this;
        }
    }
}

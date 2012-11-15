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
package org.apache.stanbol.reasoners.jobs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.stanbol.commons.jobs.api.Job;
import org.apache.stanbol.commons.jobs.api.JobManager;
import org.apache.stanbol.commons.jobs.api.JobResult;
import org.apache.stanbol.commons.jobs.impl.JobManagerImpl;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests the {@see JobManagerImpl}
 * 
 * @author enridaga
 * 
 */
public class TestJobManagerImpl {
    private static final Logger log = LoggerFactory.getLogger(TestJobManagerImpl.class);

    // Number of jobs to add
    private static final int numberOfJobs = 20;
    // Each job counts until ...
    private static final int countUntil = 100;
    // Each count sleep ... ms
    private static final int jobsleepTime = 10;
    
    private static JobManager jobManager;
    private static List<String> jobs;
    private static List<String> terminated;

    @BeforeClass
    public static void setup() {
        log.info("Test initialized");
        jobs = new ArrayList<String>();
        terminated = new ArrayList<String>();
        jobManager = new JobManagerImpl();

        for (int x = 0; x < numberOfJobs; x++) {
            addJob();
        }
        log.info("Launched {} processes", numberOfJobs);
    }

    /**
     * Before each test method, we add some jobs
     */
    @Before
    public void init() {}

    private static void addJob() {
        final int max = countUntil;
        final int number = jobs.size() + 1;
        final int jst = jobsleepTime;
        jobs.add(jobManager.execute(new Job() {

            @Override
            public JobResult call() {
                final int num = number;
                for (int i = 0; i < max; i++) {
                    try {
                        //log.debug("Process " + Integer.toString(num) + " is working");
                        Thread.sleep(jst);
                    } catch (InterruptedException ie) {}
                }
                JobResult r = new JobResult(){

                    @Override
                    public String getMessage() {
                        return "This is process " + Integer.toString(num);
                    }

                    @Override
                    public boolean isSuccess() {
                        return true;
                    }
                };
                return r;
            }

            @Override
            public String buildResultLocation(String jobId) {
                return null;
            }
        }));
    }

    /**
     * Ensures that the tests are called in the correct order
     * @throws ExecutionException 
     * @throws InterruptedException 
     */
    @Test
    public void testJobManagerImpl() throws InterruptedException, ExecutionException{
        contains();
        future();
        interrupt();
        ping();
    }
    
    private void contains() {
        log.info("Testing hasJob(String id)");
        for (String id : jobs) {
            assertTrue(jobManager.hasJob(id));
        }
    }

    private void future() throws InterruptedException, ExecutionException {
        log.info("Testing monitoring");
        for (int i = 0; i < jobs.size(); i++) {
            assertNotNull(jobManager.ping(jobs.get(i)));
        }
    }

    private void ping() throws InterruptedException, ExecutionException {
        log.info("Testing ping(String id)");
        for (int i = 0; i < jobs.size(); i++) {
            log.info("Waiting 0.5 sec before checking status");
            Thread.sleep(500);
            boolean finished = ping(jobs);
            if (finished) {
                break;
            }
        }
    }

    /**
     * To test the interaction with the Future object, for interrupting jobs. Jobs are canceled, but they
     * persist in the manager.
     */
    private void interrupt() {
        log.info("Testing the Future object (for monitoring)");
        // We interrupt the first numberOfJobs/2 processes
        int numberToInterrupt = jobs.size() / 2;
        log.info("Going to interrupt {} jobs", numberToInterrupt);
        for (String id : jobs) {
            if (numberToInterrupt == 0) {
                break;
            } else {
                numberToInterrupt--;
            }
            Future<?> f = jobManager.ping(id);
            // We force the job to interrupt
            boolean success = f.cancel(true);
            boolean throwsOnget = false;
            if (success) {
                try {
                    f.get();
                } catch (InterruptedException e) {
                    // This should NOT happen
                    assertFalse(true);
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    // This should NOT happen
                    assertFalse(true);
                    e.printStackTrace();
                } catch (CancellationException e) {
                    // This exception SHOULD happen
                    throwsOnget = true;
                }
                assertTrue(throwsOnget);
                log.debug("Job {} interrupted", id);
            }
        }
    }

    private boolean ping(List<String> processes) throws InterruptedException, ExecutionException {
        int size = processes.size();
        for (String id : processes) {
            if (terminated.contains(id)) {
                assertFalse(jobManager.hasJob(id));
            } else {
                Future<?> f = jobManager.ping(id);
                assertNotNull(f);
                log.debug("Pinging id {}: {}", id, f.isDone());
                if (f.isCancelled()) {
                    log.info("{} - have been interrupted.", id);
                    terminated.add(id);
                    // We can remove this, since we have known it
                    jobManager.remove(id);
                    // There is no output
                    size = size - 1;
                } else if (f.isDone()) {
                    terminated.add(id);
                    jobManager.remove(id);
                    // The get method should return something, wince we know this is not a canceled job
                    log.info("{} completed, output is {}", id, f.get());
                    size = size - 1;
                }
            }

        }
        return (terminated.size() == processes.size());
    }
}

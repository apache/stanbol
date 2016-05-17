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
package org.apache.stanbol.enhancer.jobmanager.event.impl;

import static org.apache.stanbol.enhancer.jobmanager.event.Constants.PROPERTY_EXECUTION;
import static org.apache.stanbol.enhancer.jobmanager.event.Constants.PROPERTY_JOB_MANAGER;
import static org.apache.stanbol.enhancer.jobmanager.event.Constants.TOPIC_JOB_MANAGER;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.getEngine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;

import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngineManager;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.ExecutionMetadataHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.execution.ChainExecution;
import org.apache.stanbol.enhancer.servicesapi.helper.execution.Execution;
import org.apache.stanbol.enhancer.servicesapi.helper.execution.ExecutionMetadata;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnhancementJobHandler implements EventHandler {

    private EnhancementEngineManager engineManager;
    private EventAdmin eventAdmin;

    /*
     * NOTE on debug level Loggings
     * 
     *  ++ ... init some activity
     *  >> ... started some activity (thread has the requested lock)
     *  << ... completed some activity (thread has released the lock)
     *  
     *  n: ... no lock
     *  r: ... read lock
     *  w: ... write lock
     */
    private static Logger log = LoggerFactory.getLogger(EnhancementJobHandler.class);
    /**
     * Keys are {@link EnhancementJob}s currently asynchronously enhancing 
     * contentItems and the values are the objects used to interrupt the 
     * requesting thread as soon as the enhancement process has finished. 
     */
    private Map<EnhancementJob,EnhancementJobObserver> processingJobs;
    private final ReadWriteLock processingLock = new ReentrantReadWriteLock();
    private Thread observerDaemon;
    
    public EnhancementJobHandler(EventAdmin eventAdmin, 
                                 EnhancementEngineManager engineManager) {
        if(eventAdmin == null){
            throw new IllegalArgumentException("The parsed EventAdmin service MUST NOT be NULL!");
        }
        if(engineManager == null){
            throw new IllegalArgumentException("The parsed EnhancementEngineManager MUST NOT be NULL!");
        }
        this.eventAdmin = eventAdmin;
        this.engineManager = engineManager;
        processingLock.writeLock().lock();
        try {
            processingJobs = new LinkedHashMap<EnhancementJob,EnhancementJobObserver>();
        } finally{
            processingLock.writeLock().unlock();
        }
        observerDaemon = new Thread(new EnhancementJobObserverDaemon());
        observerDaemon.setName("Event Job Manager Observer Daemon");
        observerDaemon.setDaemon(true);
        observerDaemon.start();
        
    }
    /**
     * Closes this Handler and notifies all components that wait for still
     * running jobs
     */
    public void close(){
        log.info("deactivate {}",getClass().getName());
        processingLock.writeLock().lock();
        try {
            for(Object o : processingJobs.values()){
                synchronized (o) {
                    o.notifyAll();
                }
            }
            processingJobs = null;
        } finally {
            processingLock.writeLock().unlock();
        }
        observerDaemon = null;
    }
    
    /**
     * Registers an EnhancementJob and will start the enhancement process.
     * When the process is finished or this service is deactivated the
     * returned oject will be notified. Therefore callers that need to 
     * wait for the completion of the parsed job will want to
     * <code><pre>
     *   Object object = enhancementJobHandler.register();
     *   while(!job.isFinished() & enhancementJobHandler != null){
     *       synchronized (object) {
     *           try {
     *               object.wait();
     *           } catch (InterruptedException e) {}
     *       }
     *   }
     * </pre></code>
     * @param enhancementJob the enhancement job to register
     * @return An object that will get {@link Object#notifyAll()} as soon as
     * {@link EnhancementJob#isFinished()} or this instance is deactivated
     */
    public EnhancementJobObserver register(EnhancementJob enhancementJob){
        final boolean init;
        EnhancementJobObserver observer;
        processingLock.writeLock().lock();
        try {
            if(enhancementJob == null || processingJobs == null){
                return null;
            }
            observer = processingJobs.get(enhancementJob);
            if(observer == null){
                observer = new EnhancementJobObserver(enhancementJob);
                if(log.isDebugEnabled()){
                    logJobInfo(log, enhancementJob, "Add EnhancementJob:",log.isTraceEnabled());
                }
                processingJobs.put(enhancementJob, observer);
                init = true;
            } else {
                log.warn("Request to register an EnhancementJob for an ContentItem {} that is" +
                		"already registered "+enhancementJob.getContentItem().getUri());
                init = false;
            }
        } finally {
            processingLock.writeLock().unlock();
        }
        if(init){
            observer.acquire();
            enhancementJob.startProcessing();
            log.trace("++ w: {}","init execution");
            enhancementJob.getLock().writeLock().lock();
            try {
                log.trace(">> w: {}","init execution");
                if(!executeNextNodes(enhancementJob)){
                    String message = "Unable to start Execution of "+enhancementJob.getContentItem().getUri();
                    log.warn(message);
                    logJobInfo(log, enhancementJob, null, true);
                    log.warn("finishing job ...");
                    finish(enhancementJob);
                }
            } finally {
                log.trace("<< w: {}","init execution");
                enhancementJob.getLock().writeLock().unlock();
            }
        }
        return observer;
    }

    @Override
    public void handleEvent(Event event) {
        EnhancementJob job = (EnhancementJob)event.getProperty(PROPERTY_JOB_MANAGER);
        BlankNodeOrIRI execution = (BlankNodeOrIRI)event.getProperty(PROPERTY_EXECUTION);
        if(job == null || execution == null){
            log.warn("Unable to process EnhancementEvent where EnhancementJob " +
            		"{} or Execution node {} is null -> ignore",job,execution);
        }
        try {
            processEvent(job, execution);
        } catch (Throwable t) {
            String message = String.format("Unexpected Exception while processing " +
            		"ContentItem %s with EnhancementJobManager: %s",
                    job.getContentItem().getUri(),EventJobManagerImpl.class);
            //this ensures that an runtime exception does not 
           job.setFailed(execution, null, new IllegalStateException(message,t));
           log.error(message,t);
        }
        //(2) trigger the next actions
        log.trace("++ w: {}","check for next Executions");
        job.getLock().writeLock().lock();
        log.trace(">> w: {}","check for next Executions");
        try {
            if(job.isFinished()){
                finish(job);
            } else if(!job.isFailed()){
                if(!executeNextNodes(job) && job.getRunning().isEmpty()){
                    log.warn("Unexpected state in the Execution of ContentItem {}:"
                        + " Job is not finished AND no executions are running AND"
                        + " no further execution could be started! -> finishing"
                        + " this job :(");
                    finish(job);
                } //else execution started of other jobs are running
            } else {
                if(log.isInfoEnabled()){
                    Collection<String> running = new ArrayList<String>(3);
                    for(BlankNodeOrIRI runningNode : job.getRunning()){
                        running.add(getEngine(job.getExecutionPlan(), job.getExecutionNode(runningNode)));
                    }
                    log.info("Job {} failed, but {} still running!",
                        job.getContentItem().getUri(),running);
                }
            }
        } finally {
            log.trace("<< w: {}","check for next Executions");
            job.getLock().writeLock().unlock();
        }
    }
    /**
     * @param job
     * @param execution
     */
    private void processEvent(EnhancementJob job, BlankNodeOrIRI execution) {
        String engineName = getEngine(job.getExecutionPlan(), 
            job.getExecutionNode(execution));
        //(1) execute the parsed ExecutionNode
        EnhancementEngine engine = engineManager.getEngine(engineName);
        if(engine != null){
            //execute the engine
            Exception exception = null;
            int engineState;
            try {
                engineState = engine.canEnhance(job.getContentItem());
            } catch (EngineException e) {
                exception = e;
                log.warn("Unable to check if engine '" + engineName
                    + "'(type: " + engine.getClass() + ") can enhance ContentItem '"
                    + job.getContentItem().getUri()+ "'!",e);
                engineState = EnhancementEngine.CANNOT_ENHANCE;
            }
            if(engineState == EnhancementEngine.ENHANCE_SYNCHRONOUS){
                //ensure that this engine exclusively access the content item
                log.trace("++ w: {}: {}","start sync execution", engine.getName());
                job.getLock().writeLock().lock();
                log.trace(">> w: {}: {}","start sync execution", engine.getName());
                try {
                    engine.computeEnhancements(job.getContentItem());
                    job.setCompleted(execution);
                } catch (EngineException e){
                    log.warn(e.getMessage(),e);
                    job.setFailed(execution, engine, e);
                } catch (RuntimeException e){
                    log.warn(e.getMessage(),e);
                    job.setFailed(execution, engine, e);
                } finally{
                    log.trace("<< w: {}: {}","finished sync execution", engine.getName());
                    job.getLock().writeLock().unlock();
                }
            } else if(engineState == EnhancementEngine.ENHANCE_ASYNC){
                try {
                    log.trace("++ n: start async execution of Engine {}",engine.getName());
                    engine.computeEnhancements(job.getContentItem());
                    log.trace("++ n: finished async execution of Engine {}",engine.getName());
                    job.setCompleted(execution);
                } catch (EngineException e) {
                    log.warn(e.getMessage(),e);
                    job.setFailed(execution, engine, e);
                } catch (RuntimeException e) {
                    log.warn(e.getMessage(),e);
                    job.setFailed(execution, engine, e);
                }
            } else { //CANNOT_ENHANCE
                if(exception != null){
                    job.setFailed(execution,engine,exception);
                } else { //can not enhance is not an error
                    //it just says this engine can not enhance this content item
                    job.setCompleted(execution);
                }
            }
        } else { //engine with that name is not available
            job.setFailed(execution, null, null);
        }
    }
    /**
     * Removes a finished job from {@link #processingJobs} and notifies
     * all waiting components
     * @param job the finished job
     */
    private void finish(EnhancementJob job){
        processingLock.writeLock().lock();
        EnhancementJobObserver observer;
        try {
            observer = processingJobs.remove(job);
        } finally {
            processingLock.writeLock().unlock();
        }
        if(observer != null) {
            try {
                if(log.isDebugEnabled()){
                    logJobInfo(log, job, "Finished EnhancementJob:",log.isTraceEnabled());
                }
                log.trace("++ n: finished processing ContentItem {} with Chain {}",
                    job.getContentItem().getUri(),job.getChainName());
            } finally {
                //release the semaphore to send signal to the EventJobManager waiting
                //for the results
                observer.release();
            }
        } else {
            log.warn("EnhancementJob for ContentItem {} is not " +
                    "registered with {}. Will not send notification!",
                    job.getContentItem().getUri(), getClass().getName());
        }            
    }
    /**
     * triggers the execution of the next nodes or if 
     * {@link EnhancementJob#isFinished()} notifies the one who registered 
     * the {@link EnhancementJob} with this component.
     * @param job the enhancement job to process
     * @return if an Execution event was sent
     */
    protected boolean executeNextNodes(EnhancementJob job) {
        //getExecutable returns an snapshot so we do not need to lock
        boolean startedExecution = false;
        for(BlankNodeOrIRI executable : job.getExecutable()){
            if(log.isTraceEnabled()){
                log.trace("PREPARE execution of Engine {}",
                    getEngine(job.getExecutionPlan(), job.getExecutionNode(executable)));
            }
            Dictionary<String,Object> properties = new Hashtable<String,Object>();
            properties.put(PROPERTY_JOB_MANAGER, job);
            properties.put(PROPERTY_EXECUTION, executable);
            job.setRunning(executable);
            if(log.isTraceEnabled()){
                log.trace("SHEDULE execution of Engine {}",
                    getEngine(job.getExecutionPlan(), job.getExecutionNode(executable)));
            }
            eventAdmin.postEvent(new Event(TOPIC_JOB_MANAGER,properties));
            startedExecution = true;
        }
        return startedExecution;
    }
    /**
     * Helper method that logs the execution time for the Chain and all the
     * Enhancement Engines on DEBUG level
     * @param logger The logger used for logging
     * @param job the job to log. This expects that 
     * <code>{@link EnhancementJob#isFinished()} == true</code>
     */
    protected static void logExecutionTimes(Logger logger, EnhancementJob job){
    	if(logger.isInfoEnabled()){
    		try {
		    	ExecutionMetadata em = ExecutionMetadata.parseFrom(
		    			job.getExecutionMetadata(),job.getContentItem().getUri());
		    	ChainExecution ce = em.getChainExecution();
		    	long cd = ce.getDuration();
                StringBuilder message = new StringBuilder("> processed ContentItem ")
                .append(job.getContentItem().getUri()).append(" with Chain '")
                .append(ce.getChainName()).append("' in ").append(ce.getDuration()).append("ms | ");
		    	List<Execution> ees = new ArrayList<Execution>(em.getEngineExecutions().values());
		    	//sort by start date (execution order)
		    	Collections.sort(ees, new Comparator<Execution>() {
		    		@Override
		    		public int compare(Execution e1, Execution e2) {
		    			return e1.getStarted().compareTo(e2.getStarted());
		    		}
				});
		    	message.append("chain:[");
		    	long eds = 0;
		    	boolean first = true;
		    	for(Execution ee : ees){
                    if(first){
                        first = false;
                    } else {
                        message.append(", ");
                    }
		    		long ed = ee.getDuration();
		    		eds = eds + ed;
		    		int edp = Math.round(ed*100/(float)cd);
		    		message.append(ee.getExecutionNode().getEngineName())
		    		    .append(": ").append(ed).append("ms (").append(edp).append("%)");
		    	}
		    	float cf = eds/cd;
		    	int cfp = Math.round((cf-1)*100);
                message.append("], concurrency: ").append(cf).append(" (").append(cfp).append("%)");
                logger.info(message.toString());
    		} catch (RuntimeException e) {
    			log.warn("Exception while logging ExecutionTimes for Chain: '" +
    					job.getChainName() + " and ContentItem "+
    					job.getContentItem().getUri() +" to Logger " +
    					logger.getName(),e);
    		}
    	}
    }
    
    /**
     * Logs basic infos about the Job as INFO and detailed infos as DEBUG
     * @param job
     */
    protected static void logJobInfo(Logger log, EnhancementJob job, String header, boolean logExecutions) {
        if(header != null){
            log.info(header);
        }
        log.info("   finished:     {}",job.isFinished());
        log.info("   state:        {}",job.isFailed()?"failed":"processing");
        log.info("   chain:        {}",job.getChainName());
        log.info("   content-item: {}", job.getContentItem().getUri());
        if(logExecutions){
            log.info("  executions:");
            for(BlankNodeOrIRI completedExec : job.getCompleted()){
                log.info("    - {} completed",getEngine(job.getExecutionMetadata(), 
                    job.getExecutionNode(completedExec)));
            }
            for(BlankNodeOrIRI runningExec : job.getRunning()){
                log.info("    - {} running",getEngine(job.getExecutionMetadata(), 
                    job.getExecutionNode(runningExec)));
            }
            for(BlankNodeOrIRI executeable : job.getExecutable()){
                log.info("    - {} executeable",getEngine(job.getExecutionMetadata(), 
                    job.getExecutionNode(executeable)));
            }
        }
        if(job.getErrorMessage() != null){
            log.info("Error Message: {}",job.getErrorMessage());
        }
        if(job.getError() != null){
            log.info("Reported Exception:",job.getError());
        }
    }
    public final class EnhancementJobObserver{
        
        private static final int MIN_WAIT_TIME = 500;
        private final EnhancementJob enhancementJob;
        private final Semaphore semaphore;
        
        private EnhancementJobObserver(EnhancementJob job){
            if(job == null){
                throw new IllegalArgumentException("The parsed EnhancementJob MUST NOT be NULL!");
            }
            this.enhancementJob = job;
            this.semaphore = new Semaphore(1);
        }

        protected void acquire() {
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                log.warn("Interrupted while acquireing Semaphore for EnhancementJob "
                        + enhancementJob + "!",e);
            }
        }
        
        protected void release() {
            semaphore.release();
        }

        public boolean hasCompleted() {
            enhancementJob.getLock().readLock().lock();
            try {
                return enhancementJob.isFinished();
            } finally {
                enhancementJob.getLock().readLock().unlock();
            }
        }

        public boolean waitForCompletion(int maxEnhancementJobWaitTime) {
            boolean finished = false;
            if(semaphore.availablePermits() < 1){
                // The only permit is taken by the EnhancementJobHander
                try {
                    finished = semaphore.tryAcquire(1,
                        Math.max(MIN_WAIT_TIME, maxEnhancementJobWaitTime),TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    //interupted
                    finished = false;
                }
            } else if(!hasCompleted()){
                log.error("Unexpected {} permit(s) (expected = 0) available for "
                    + "Semaphore of  EnhancementJob of ContentItem {}. Please "
                    + "report this on dev@stanbol.apache.org and/or the Apache "
                    + "Stanbol Issue Tracker.", semaphore.availablePermits(),
                    enhancementJob.getContentItem().getUri());
                finished = false;
            } else { //already completed
                finished = true;
            }
            return finished;
        }
    }
    
    
    /**
     * Currently only used to debug the number of currently registered
     * Enhancements Jobs (if there are some)
     * @author Rupert Westenthaler
     */
    private class EnhancementJobObserverDaemon implements Runnable {

        /**
         * The logger of the Observer. Can be used to configure Loglevel specificly
         * 
         */
        private Logger observerLog = LoggerFactory.getLogger(EnhancementJobObserverDaemon.class);
        
        @Override
        public void run() {
            observerLog.debug(" ... init EnhancementJobObserver");
            while(processingJobs != null){
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                }
                Collection<EnhancementJob> jobs;
                Lock readLock = processingLock.readLock();
                readLock.lock();
                try {
                    if(processingJobs != null){
                        jobs = new ArrayList<EnhancementJob>(processingJobs.keySet());
                    } else {
                        jobs = Collections.emptyList();
                    }
                } finally {
                    readLock.unlock();
                }
                if(!jobs.isEmpty()){
                    observerLog.debug(" -- {} active Enhancement Jobs",jobs.size());
                    if(observerLog.isDebugEnabled()){
                        for(EnhancementJob job : jobs){
                            Lock jobLock = job.getLock().readLock();
                            jobLock.lock();
                            try {
                                logJobInfo(observerLog,job,null,true);
                            } finally {
                                jobLock.unlock();
                            }
                        }
                    }
                } else {
                    log.debug(" -- No active Enhancement Jobs");
                }
            }
            
        }
        
    }
    
    
}

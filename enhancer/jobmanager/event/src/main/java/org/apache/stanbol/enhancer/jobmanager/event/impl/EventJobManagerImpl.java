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

import static org.apache.stanbol.enhancer.jobmanager.event.Constants.TOPIC_JOB_MANAGER;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.jobmanager.event.impl.EnhancementJobHandler.EnhancementJobObserver;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ChainException;
import org.apache.stanbol.enhancer.servicesapi.ChainManager;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngineManager;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.execution.Execution;
import org.apache.stanbol.enhancer.servicesapi.helper.execution.ExecutionMetadata;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate=true,metatype=true)
@Service
@Properties(value={
    //register with a ranking lower than 0 to allow easy overriding by specific
    @Property(name=Constants.SERVICE_RANKING,intValue=EventJobManagerImpl.DEFAULT_SERVICE_RANKING),
    @Property(name=EventJobManagerImpl.MAX_ENHANCEMENT_JOB_WAIT_TIME,intValue=EventJobManagerImpl.DEFAULT_MAX_ENHANCEMENT_JOB_WAIT_TIME)
})
public class EventJobManagerImpl implements EnhancementJobManager {

    private final Logger log = LoggerFactory.getLogger(EventJobManagerImpl.class);
    
    public static final int DEFAULT_SERVICE_RANKING = 0;

    public static final String MAX_ENHANCEMENT_JOB_WAIT_TIME = "stanbol.maxEnhancementJobWaitTime";

    /**
     * default max wait time is 60sec (similar to the http timeout)
     */
    public static final int DEFAULT_MAX_ENHANCEMENT_JOB_WAIT_TIME = 60 * 1000;
    
    @Reference
    protected ChainManager chainManager;
    @Reference
    protected EnhancementEngineManager engineManager;
    @Reference
    protected EventAdmin eventAdmin;

    private ServiceRegistration jobHandlerRegistration;
    private EnhancementJobHandler jobHandler;
    private int maxEnhancementJobWaitTime = DEFAULT_MAX_ENHANCEMENT_JOB_WAIT_TIME;
    
    
    /**
     * Instantiates and registers the {@link EnhancementJobHandler} as
     * {@link EventHandler} for the topic 
     * {@link org.apache.stanbol.enhancer.jobmanager.event.Constants#TOPIC_JOB_MANAGER}
     * @param ctx
     */
    @Activate
    protected void activate(ComponentContext ctx){
        log.info("activate {}",getClass().getName());
        jobHandler = new EnhancementJobHandler(eventAdmin,engineManager);
        Dictionary<String,Object> properties = new Hashtable<String,Object>();
        properties.put(org.osgi.service.event.EventConstants.EVENT_TOPIC, TOPIC_JOB_MANAGER);
        jobHandlerRegistration = ctx.getBundleContext().registerService(
            EventHandler.class.getName(), jobHandler, properties);
        
        Object maxWaitTime = ctx.getProperties().get(MAX_ENHANCEMENT_JOB_WAIT_TIME);
        if (maxWaitTime instanceof Integer) {
            this.maxEnhancementJobWaitTime = (Integer) maxWaitTime;
        }
    }
    /**
     * Unregisters the {@link EnhancementJobHandler}
     * @param ctx
     */
    @Deactivate
    protected void deactivate(ComponentContext ctx){
        log.info("deactivate {}",getClass().getName());
        EnhancementJobHandler jobHandler = this.jobHandler;
        //set first the field to null
        this.jobHandler = null;
        //and than close the instance to ensure that running jobs are shut down
        //correctly
        jobHandler.close();
        jobHandlerRegistration.unregister();
        jobHandlerRegistration = null;
    }
    
    @Override
    public void enhanceContent(ContentItem ci) throws EngineException, ChainException {
        Chain defaultChain = chainManager.getDefault();
        if(defaultChain == null){
            throw new ChainException("Unable to enhance ContentItem '"+ci.getUri()+
                "' because currently no enhancement chain is active. Please" +
                "configure a Chain or enable the default chain");
        }
        enhanceContent(ci, defaultChain);
    }

    @Override
    public void enhanceContent(ContentItem ci, Chain chain) throws EngineException, ChainException {
        if(ci == null) {
            throw new IllegalArgumentException("The parsed contentItem MUST NOT be NULL!");
        }
        if(chain == null){
            throw new IllegalArgumentException("Unable to enhance ContentItem '"+ci.getUri()+
                "' because NULL was passed as enhancement chain");
        }
        long start = System.currentTimeMillis();
        boolean isDefaultChain = chain.equals(chainManager.getDefault());
        EnhancementJob job = new EnhancementJob(ci, chain.getName(), chain.getExecutionPlan(),isDefaultChain);
        //start the execution
        //wait for the results
        EnhancementJobObserver observer = jobHandler.register(job);
        //now wait for the execution to finish for the configured maximum time
        boolean completed = observer.waitForCompletion(maxEnhancementJobWaitTime);
        if(!completed){ //throw timeout exception
            StringBuilder sb = new StringBuilder("Status:\n");
            ExecutionMetadata em = ExecutionMetadata.parseFrom(job.getExecutionMetadata(), ci.getUri());
            for(Entry<String,Execution> ex : em.getEngineExecutions().entrySet()){
                sb.append("  -").append(ex.getKey()).append(": ").append(ex.getValue().getStatus()).append('\n');
            }
            throw new ChainException("Execution timeout after "
                    +((System.currentTimeMillis()-start)/1000f)+"sec (timeout:"+(maxEnhancementJobWaitTime/1000)
                + "sec) for ContentItem "+ci.getUri()+"\n"+sb.toString()
                + " \n To change the timeout change value of property '"+
                    MAX_ENHANCEMENT_JOB_WAIT_TIME+"' for the service "+getClass());
        }
        log.info("Execution of Chain {} {} after {}ms for ContentItem {}",
            new Object[]{ chain.getName(), job.isFailed() ? "failed" : "finished",
                    System.currentTimeMillis()-start,
                    job.getContentItem().getUri()});
        //NOTE: ExecutionMetadata are not added to the metadata of the ContentItem
        //      by the EnhancementJobManager.
        //      However one could add this as an optional feature to the
        //      RESTful interface of the Enhancer!
        //ci.getMetadata().addAll(job.getExecutionMetadata());
        if(job.isFailed()){
        	Exception e = job.getError();
        	if (e instanceof SecurityException) {
        		throw (SecurityException)e;
        	} else {
        		throw new ChainException(job.getErrorMessage(), e);
        	}
        }
        if(!job.isFinished()){
            log.warn("Execution finished, but Job is not finished!");
            EnhancementJobHandler.logJobInfo(log, job, null, true);
            log.warn("ExecutionMetadata: ");
            for(Iterator<Triple> it = job.getExecutionMetadata().iterator();
                    it.hasNext();
                    log.warn(it.next().toString()));
            throw new ChainException("EnhancementJobManager was deactivated while" +
            		" enhancing the passed ContentItem "+job.getContentItem()+
            		" (EnhancementJobManager type: "+getClass()+")");
        }
    }

    @Override
    public List<EnhancementEngine> getActiveEngines() {
        //This implementation return the list of active engined for the default
        //Chain in the order they would be executed
        Chain defaultChain = chainManager.getDefault();
        if(defaultChain == null){
            throw new IllegalStateException("Currently no enhancement chain is " +
                "active. Please configure a Chain or enable the default chain");
        }
        Graph ep;
        try {
            ep = defaultChain.getExecutionPlan();
        } catch (ChainException e) {
            throw new IllegalStateException("Unable to get Execution Plan for " +
            		"default enhancement chain (name: '"+defaultChain.getName()+
            		"'| class: '"+defaultChain.getClass()+"')!",e);
        }
        return ExecutionPlanHelper.getActiveEngines(engineManager,ep);
    }


}

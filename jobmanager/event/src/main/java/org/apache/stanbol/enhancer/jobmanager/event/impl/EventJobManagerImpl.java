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
import java.util.List;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ChainException;
import org.apache.stanbol.enhancer.servicesapi.ChainManager;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngineManager;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper;
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
    @Property(name=Constants.SERVICE_RANKING,intValue=EventJobManagerImpl.DEFAULT_SERVICE_RANKING)
})
public class EventJobManagerImpl implements EnhancementJobManager {

    private final Logger log = LoggerFactory.getLogger(EventJobManagerImpl.class);
    
    public static final int DEFAULT_SERVICE_RANKING = 0;
    
    @Reference
    protected ChainManager chainManager;
    @Reference
    protected EnhancementEngineManager engineManager;
    @Reference
    protected EventAdmin eventAdmin;

    private ServiceRegistration jobHandlerRegistration;
    private EnhancementJobHandler jobHandler;
    
    
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
                "' because NULL was parsed as enhancement chain");
        }
        long start = System.currentTimeMillis();
        boolean isDefaultChain = chain.equals(chainManager.getDefault());
        EnhancementJob job = new EnhancementJob(ci, chain.getName(), chain.getExecutionPlan(),isDefaultChain);
        //start the execution
        //wait for the results
        Object object = jobHandler.register(job);
        while(!job.isFinished() & jobHandler != null){
            synchronized (object) {
                try {
                    object.wait();
                } catch (InterruptedException e) {
                    log.debug("Interupped for EnhancementJob if ContentItem {}",
                        job.getContentItem().getUri());
                }
            }
        }
        log.info("{} EnhancementJob for ContentItem {} after {}ms",
            new Object[]{ job.isFailed() ? "Failed" : "Finished",
                    job.getContentItem().getUri(),
                    System.currentTimeMillis()-start});
        //NOTE: ExecutionMetadata are not added to the metadata of the ContentItem
        //      by the EnhancementJobManager.
        //      However one could add this as an optional feature to the
        //      RESTful interface of the Enhancer!
        //ci.getMetadata().addAll(job.getExecutionMetadata());
        if(job.isFailed()){
            throw new ChainException(job.getErrorMessage(), job.getError());
        }
        if(!job.isFinished()){
            throw new ChainException("EnhancementJobManager was deactivated while" +
            		"enhancing the parsed ContentItem "+job.getContentItem()+
            		"(EnhancementJobManager type: "+getClass()+")!");
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

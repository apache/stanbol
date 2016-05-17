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
package org.apache.stanbol.enhancer.chain.allactive.impl;

import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.calculateExecutionPlan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ChainException;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.impl.EnginesTracker;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Chain implementation that uses all currently active {@link EnhancementEngine}s
 * to build the execution plan based on the 
 * {@link ServiceProperties#ENHANCEMENT_ENGINE_ORDERING ordering} values of such
 * engines. <p>
 * The execution plan produced by this Chain is the same as used by the
 * Stanbol Enhancer before the introduction of enhancement {@link Chain}s.<p>
 * An instance of this chain is registerd under:<ul>
 * <li> {@link EnhancementEngine#PROPERTY_NAME} = "default"
 * <li> {@link Constants#SERVICE_RANKING} = {@link Integer#MIN_VALUE}
 * </ul>
 * by the {@link DefaultChain} component. This ensures that the behaviour of
 * the Stanbol Enhancer - to enhance parsed {@link ContentItem}s by using all
 * currently active {@link EnhancementEngine}s - is still the default after the
 * introduction of {@link Chain}s. See the documentation of the Stanbol Enhancer
 * for details.
 * 
 * @author Rupert Westenthaler
 *
 */
public class AllActiveEnginesChain implements ServiceTrackerCustomizer, Chain {

    private String name;
    private final Object lock = new Object();
    private BundleContext context;
    private ImmutableGraph executionPlan;
    private Set<String> engineNames;
    private EnginesTracker tracker;
    
    public AllActiveEnginesChain(BundleContext context, String name) {
        if(context == null){
            throw new IllegalArgumentException("The parsed BundleContext MUST NOT be NULL!");
        }
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException("The parsed Chain name MUST NOT be NULL!");
        }
        this.context = context;
        this.name = name;
        Set<String> trackAll = Collections.emptySet();
        this.tracker = new EnginesTracker(context, 
            trackAll, //empty set to track all engines
            this);
        this.tracker.open();
    }
        
    /**
     * This internally used an {@link EnginesTracker} to track currently active
     * {@link EnhancementEngine}. This will {@link EnginesTracker#close() close}
     * this tracker and also clear other member variables
     */
    public void close(){
        synchronized (lock) {
            this.executionPlan = null;
            this.engineNames = null;
        }
        this.tracker.close();
        this.tracker = null;
        this.name = null;
    }
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
    
    @Override
    public String getName() {
        return name;
    }
    @Override
    public ImmutableGraph getExecutionPlan() throws ChainException {
        synchronized (lock) {
            if(executionPlan == null){
                update();
            }
            return executionPlan;
        }
    }

    @Override
    public Set<String> getEngines() throws ChainException {
        synchronized (lock) {
            if(engineNames == null){
                update();
            }
            return engineNames;
        }
    }

    @Override
    public Object addingService(ServiceReference reference) {
        invalidateExecutionPlan();
        return context.getService(reference);
    }

    @Override
    public void modifiedService(ServiceReference reference, Object service) {
        invalidateExecutionPlan();
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {
        invalidateExecutionPlan();
        context.ungetService(reference);
    }
    private void invalidateExecutionPlan(){
        synchronized (lock) {
            this.executionPlan = null;
            this.engineNames = null;
        }
    }
    
    private void update() throws ChainException {
        Set<String> activeEngineNames = new HashSet<String>(tracker.getActiveEngineNames());
        if(activeEngineNames.isEmpty()){
            throw new ChainException("Currently there are no active EnhancementEngines available");
        }
        List<EnhancementEngine> activeEngines = new ArrayList<EnhancementEngine>(activeEngineNames.size());
        Iterator<String> names = activeEngineNames.iterator();
        while(names.hasNext()){
            String name = names.next();
            EnhancementEngine engine = tracker.getEngine(name);
            if(engine != null){
                activeEngines.add(engine);
            } else { //looks like the config has changed in the meantime
                names.remove();
            }
        }
        Set<String> emptySet = Collections.emptySet();
        executionPlan = calculateExecutionPlan(
            getName(),activeEngines, 
            emptySet,//this Chain does not support optional engines
            emptySet); //only active meaning that no engines are missing
        engineNames = Collections.unmodifiableSet(activeEngineNames);
    }

}
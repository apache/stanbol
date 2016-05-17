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
package org.apache.stanbol.enhancer.chain.weighted.impl;

import static org.apache.stanbol.enhancer.servicesapi.helper.ConfigUtils.getState;
import static org.apache.stanbol.enhancer.servicesapi.helper.ConfigUtils.parseConfig;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.calculateExecutionPlan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ChainException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.helper.ConfigUtils;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractChain;
import org.apache.stanbol.enhancer.servicesapi.impl.EnginesTracker;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Chain implementation takes a list of engines names as input 
 * and uses the "org.apache.stanbol.enhancer.engine.order " metadata provided by 
 * such engines to calculate the ExecutionGraph.<p>
 * 
 * Similar the current WeightedJobManager implementation Engines would be
 * dependent to each other based on decreasing order values. Engines with the 
 * same order value would could be executed in parallel.<p>
 * 
 * This implementation is targeted for easy configuration - just a list of the 
 * engine names contained within a chain - but has limited possibilities to 
 * control the execution order within an chain. However it is expected 
 * that it provides enough flexibility for most of the usage scenarios.<p>
 * 
 * This engine also supports the definition of additional parameters for
 * Enhancement Engines. The syntax is the same as used by BND tools: 
 * <pre><code>
 *     &lt;engineName&gt;;&ltparam-name&gt=&ltparam-value&gt
 *     &lt;engineName&gt;;&ltparam-name&gt
 * </code></pre>
 * Parameter without value are interpreted as enabled boolean switch<p>
 * 
 * Currently this Chain implementation supports the following Parameter: <ul>
 * <li> optional: Boolean switch that allows to define that the execution of this
 * engine is not required.
 * </ul>
 * 
 * <i>NOTE:</i> Since <code>0.12.1</code> this supports EnhancementProperties
 * as described by <a href="https://issues.apache.org/jira/browse/STANBOL-488"></a>
 * 
 * @author Rupert Westenthaler
 *
 */
@Component(configurationFactory=true,metatype=true,
policy=ConfigurationPolicy.REQUIRE)
@Properties(value={
    @Property(name=Chain.PROPERTY_NAME),
    @Property(name=WeightedChain.PROPERTY_CHAIN, cardinality=1000),
    @Property(name=AbstractChain.PROPERTY_CHAIN_PROPERTIES,cardinality=1000),
    @Property(name=Constants.SERVICE_RANKING, intValue=0)
})
@Service(value=Chain.class)
public class WeightedChain extends AbstractChain implements Chain, ServiceTrackerCustomizer {
    
    private final Logger log = LoggerFactory.getLogger(WeightedChain.class);

    /**
     * The list of Enhancement Engine names used to build the Execution Plan 
     * based on there weights. 
     */
    public static final String PROPERTY_CHAIN = "stanbol.enhancer.chain.weighted.chain";
    /**
     * the Chain configuration as parsed in the {@link #activate(ComponentContext)} method
     */
    private Map<String,Map<String,List<String>>> chain;
    /**
     * Do hold chain scope EnhancementProperties of the configured chain.
     */
    private Map<String,Map<String,Object>> chainScopedEnhProps;
    /**
     * Tracks the engines defined in the {@link #chain}
     */
    private EnginesTracker tracker;
    
    /**
     * Used to sync access to the {@link #executionPlan}
     */
    private Object epLock = new Object();
    
    private ImmutableGraph executionPlan = null;
    
    @Override
    protected void activate(ComponentContext ctx) throws ConfigurationException {
        super.activate(ctx);
        Object value = ctx.getProperties().get(PROPERTY_CHAIN);
        Set<String> configuredChain = new HashSet<String>();
        if(value instanceof String[]){
            configuredChain.addAll(Arrays.asList((String[])value));
        } else if (value instanceof Collection<?>){
            for(Object o : (Collection<?>) value){
                if(o instanceof String){
                    configuredChain.add((String)o);
                }
            }
        } else {
            throw new ConfigurationException(PROPERTY_CHAIN, 
                "The engines of a Weigted Chain MUST BE configured as a Array or " +
                "Collection of Strings (parsed: "+
                        (value != null?value.getClass():"null")+")");
        }
        //now parse the configured chain
        try {
            chain = Collections.unmodifiableMap(parseConfig(configuredChain));
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(PROPERTY_CHAIN, "Unable to parse Chain Configuraiton (message: '"+
                    e.getMessage()+"')!",e);
        }
        if(chain.isEmpty()){
            throw new ConfigurationException(PROPERTY_CHAIN, 
                "The configured chain MUST at least contain a single valid entry!");
        }
        //init the chain scoped enhancement properties
        chainScopedEnhProps = new HashMap<String,Map<String,Object>>();
        if(getChainProperties() != null){
            chainScopedEnhProps.put(null, getChainProperties());
        }
        for(Entry<String,Map<String,List<String>>> entry : chain.entrySet()){
            Map<String,Object> enhProp = ConfigUtils.getEnhancementProperties(entry.getValue());
            if(enhProp != null){
                chainScopedEnhProps.put(entry.getKey(), enhProp);
            }
        }
        //start tracking the engines of the configured chain
        tracker = new EnginesTracker(ctx.getBundleContext(), chain.keySet(),this);
        tracker.open();
    }

    @Override
    protected void deactivate(ComponentContext ctx) {
        tracker.close();
        tracker = null;
        chainScopedEnhProps = null;
        chain = null;
        super.deactivate(ctx);
    }
    @Override
    public ImmutableGraph getExecutionPlan() throws ChainException {
        synchronized (epLock) {
            if(executionPlan == null){
                executionPlan = createExecutionPlan();
            }
            return executionPlan;
        }
    }

    @Override
    public Set<String> getEngines() {
        return chain.keySet();
    }
    /**
     * Creates a new execution plan based on the configured {@link #chain} and
     * the currently available {@link EnhancementEngine}s. If required
     * {@link EnhancementEngine}s are missing a {@link ChainException} will be
     * thrown.
     * @return the execution plan
     * @throws ChainException if a required {@link EnhancementEngine} of the
     * configured {@link #chain} is not active.
     */
    private ImmutableGraph createExecutionPlan() throws ChainException {
        List<EnhancementEngine> availableEngines = new ArrayList<EnhancementEngine>(chain.size());
        Set<String> optionalEngines = new HashSet<String>();
        Set<String> missingEngines = new HashSet<String>();
        for(Entry<String,Map<String,List<String>>> entry : chain.entrySet()){
            boolean optional = getState(entry.getValue(),"optional");
            EnhancementEngine engine = tracker.getEngine(entry.getKey());
            if(engine != null){
                availableEngines.add(engine);
            } else {
                missingEngines.add(entry.getKey());
            }
            if(optional){
                optionalEngines.add(entry.getKey());
            }
        }
        return calculateExecutionPlan(getName(),availableEngines,optionalEngines, 
            missingEngines,chainScopedEnhProps);
    }

    @Override
    public Object addingService(ServiceReference reference) {
        invalidateExecutionPlan();
        ComponentContext context = this.context;
        if(context != null){
            return context.getBundleContext().getService(reference);
        } else {
            log.warn("Unable to get EnhancementEngine for Reference {} because" +
            		"this {} seams already be deactivated -> return null",
            		reference.getProperty(EnhancementEngine.PROPERTY_NAME),
            		toString());
            return null;
        }
    }

    @Override
    public void modifiedService(ServiceReference reference, Object service) {
        invalidateExecutionPlan();
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {
        invalidateExecutionPlan();
        ComponentContext context = this.context;
        if(context != null){
            context.getBundleContext().ungetService(reference);
        }
    }
    private void invalidateExecutionPlan(){
        synchronized (epLock) {
            this.executionPlan = null;
        }
    }

}

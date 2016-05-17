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
package org.apache.stanbol.enhancer.chain.list.impl;

import static org.apache.stanbol.enhancer.servicesapi.helper.ConfigUtils.getEnhancementProperties;
import static org.apache.stanbol.enhancer.servicesapi.helper.ConfigUtils.getState;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.createExecutionPlan;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.writeExecutionNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ChainException;
import org.apache.stanbol.enhancer.servicesapi.helper.ConfigUtils;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractChain;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
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
@Component(configurationFactory=true,metatype=true, policy=ConfigurationPolicy.REQUIRE)
@Properties(value={
    @Property(name=Chain.PROPERTY_NAME),
    @Property(name=ListChain.PROPERTY_ENGINE_LIST, cardinality=1000),
    @Property(name=AbstractChain.PROPERTY_CHAIN_PROPERTIES,cardinality=1000),
    @Property(name=Constants.SERVICE_RANKING, intValue=0)
})
@Service(value=Chain.class)
public class ListChain extends AbstractChain implements Chain {
    
    private final Logger log = LoggerFactory.getLogger(ListChain.class);

    /**
     * The list of Enhancement Engine names used to build the Execution Plan
     */
    public static final String PROPERTY_ENGINE_LIST = "stanbol.enhancer.chain.list.enginelist";

    private Set<String> engineNames;
    
    private ImmutableGraph executionPlan;
        
    
    @Override
    protected void activate(ComponentContext ctx) throws ConfigurationException {
        super.activate(ctx);
        Object value = ctx.getProperties().get(PROPERTY_ENGINE_LIST);
        List<String> configuredChain = new ArrayList<String>();
        if(value instanceof String[]){
            configuredChain.addAll(Arrays.asList((String[])value));
        } else if(value instanceof List<?>){
            for(Object o : (List<?>)value){
                if(o != null){
                    configuredChain.add(o.toString());
                }
            }
        } else {
            throw new ConfigurationException(PROPERTY_ENGINE_LIST, 
                "The engines of a List Chain MUST BE configured as Array/List of " +
                "Strings (parsed: "+
                        (value != null?value.getClass():"null")+")");
        }
        Set<String> engineNames = new HashSet<String>(configuredChain.size());
        BlankNodeOrIRI last = null;
        Graph ep = new SimpleGraph();
        BlankNodeOrIRI epNode = createExecutionPlan(ep, getName(), getChainProperties());
        log.debug("Parse ListChain config:");
        for(String line : configuredChain){
            try {
                Entry<String,Map<String,List<String>>> parsed = ConfigUtils.parseConfigEntry(line);
                if(!engineNames.add(parsed.getKey())){
                    throw new ConfigurationException(PROPERTY_ENGINE_LIST, 
                        "The EnhancementEngine '"+parsed.getKey()+"' is mentioned"
                        + "twice in the configured list!");
                }
                boolean optional = getState(parsed.getValue(), "optional");
                log.debug(" > Engine: {} ({})",parsed.getKey(),optional? "optional" : "required");
                last = writeExecutionNode(ep, epNode, parsed.getKey(), optional, 
                    last == null ? null : Collections.singleton(last),
                    getEnhancementProperties(parsed.getValue()));
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException(PROPERTY_ENGINE_LIST, "Unable to parse Chain Configuraiton (message: '"+
                        e.getMessage()+"')!",e);
            }
        }
        if(engineNames.isEmpty()){
            throw new ConfigurationException(PROPERTY_ENGINE_LIST, 
                "The configured chain MUST at least contain a single valid entry!");
        }
        this.engineNames = Collections.unmodifiableSet(engineNames);
        this.executionPlan = ep.getImmutableGraph();
    }

    @Override
    protected void deactivate(ComponentContext ctx) {
        this.engineNames = null;
        this.executionPlan = null;
        super.deactivate(ctx);
    }
    @Override
    public ImmutableGraph getExecutionPlan() throws ChainException {
        return executionPlan;
    }

    @Override
    public Set<String> getEngines() {
        return engineNames;
    }

}

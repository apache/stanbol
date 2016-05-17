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
package org.apache.stanbol.enhancer.servicesapi.impl;

import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.createExecutionPlan;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.writeExecutionNode;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ChainException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;

/**
 * Intended to be used if one needs to wrap a single {@link EnhancementEngine}
 * with a {@link Chain} to execute it by using the
 * {@link EnhancementJobManager#enhanceContent(org.apache.stanbol.enhancer.servicesapi.ContentItem, String)}
 * method.<p>
 * This Chain implementation is NOT intended to be registered as OSGI service.
 * The intension is that it is instantiated by the component (e.g. the implementation
 * of a RESTful service) for an {@link EnhancementEngine} and directly parsed
 * to the {@link EnhancementJobManager}.
 * 
 * @author Rupert Westenthaler 
 *
 */
public class SingleEngineChain implements Chain {

    private final ImmutableGraph executionPlan;
    private final EnhancementEngine engine;
    private final String name;
    
    /**
     * Creates a {@link Chain} for a single {@link EnhancementEngine}
     * @param engine the engine
     */
    public SingleEngineChain(EnhancementEngine engine){
        this(engine,null);
    }
    /**
     * Creates a {@link Chain} for a single {@link EnhancementEngine} including
     * optional chain scoped enhancement properties
     * @param engine the engine
     * @param enhProps chain scoped enhancement properties or <code>null</code>
     * if none.
     * @since 0.12.1
     */
    public SingleEngineChain(EnhancementEngine engine, Map<String,Object> enhProps){
        if(engine == null){
            throw new IllegalArgumentException("The parsed EnhancementEngine MUST NOT be NULL!");
        }
        this.engine = engine;
        this.name = engine.getName()+"Chain";
        Graph graph = new IndexedGraph();
        writeExecutionNode(graph, createExecutionPlan(graph, name, null),
            engine.getName(), false, null, enhProps);
        executionPlan = graph.getImmutableGraph();
    }
    
    @Override
    public ImmutableGraph getExecutionPlan() throws ChainException {
        return executionPlan;
    }

    @Override
    public Set<String> getEngines() throws ChainException {
        return Collections.singleton(engine.getName());
    }

    @Override
    public String getName() {
        return name;
    }

}

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
package org.apache.stanbol.enhancer.servicesapi.helper;

import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.createExecutionPlan;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.writeExecutionNode;

import java.util.Collections;
import java.util.Set;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
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

    private final Graph executionPlan;
    private final EnhancementEngine engine;
    private final String name;
    
    public SingleEngineChain(EnhancementEngine engine){
        if(engine == null){
            throw new IllegalArgumentException("The parsed EnhancementEngine MUST NOT be NULL!");
        }
        this.engine = engine;
        this.name = engine.getName()+"Chain";
        MGraph graph = new IndexedMGraph();
        writeExecutionNode(graph, createExecutionPlan(graph, name),
            engine.getName(), false, null);
        executionPlan = graph.getGraph();
    }
    
    @Override
    public Graph getExecutionPlan() throws ChainException {
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

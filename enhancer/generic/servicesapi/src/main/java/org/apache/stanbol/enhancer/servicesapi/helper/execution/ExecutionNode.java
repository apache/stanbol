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
package org.apache.stanbol.enhancer.servicesapi.helper.execution;

import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper;

/**
 * An ExecutionNode of the ExecutionPlan.
 * @author Rupert Westenthaler
 *
 */
public class ExecutionNode {
    
    final BlankNodeOrIRI node;
    private final Graph ep;
    private final boolean optional;
    private final String engineName;
    
    public ExecutionNode(Graph executionPlan, BlankNodeOrIRI node) {
        this.node = node;
        this.ep = executionPlan;
        this.optional = ExecutionPlanHelper.isOptional(ep, node);
        this.engineName = ExecutionPlanHelper.getEngine(ep, node);
    }
    /**
     * If the execution of this node is optional
     * @return
     */
    public boolean isOptional() {
        return optional;
    }
    /**
     * The name of the Engine to be executed by this node
     * @return
     */
    public String getEngineName() {
        return engineName;
    }
    
    @Override
    public int hashCode() {
        return node.hashCode();
    }
    @Override
    public boolean equals(Object o) {
        return o instanceof ExecutionNode && ((ExecutionNode)o).node.equals(node);
    }
}
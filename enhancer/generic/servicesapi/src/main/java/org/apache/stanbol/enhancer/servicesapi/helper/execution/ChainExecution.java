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
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.ExecutionMetadataHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionPlan;


/**
 * Parses the Chain
 * @author westei
 *
 */
public class ChainExecution extends Execution {
    
    private final String chainName;
    
    public ChainExecution(Graph graph, BlankNodeOrIRI node) {
        super(null,graph,node);
        BlankNodeOrIRI ep = ExecutionMetadataHelper.getExecutionPlanNode(graph, node);
        if(ep != null){
            chainName = EnhancementEngineHelper.getString(graph, ep, ExecutionPlan.CHAIN);
        } else {
            chainName = null;
        }
    }
    
    public String getChainName(){
        return chainName;
    }
}
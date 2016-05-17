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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.enhancer.servicesapi.helper.ExecutionMetadataHelper;

public final class ExecutionMetadata {

    
    private final ChainExecution chainExecution;
    private final Map<String,Execution> engineExecutions;

    public static ExecutionMetadata parseFrom(Graph executionMetadata, IRI contentItemUri){
        BlankNodeOrIRI ce = ExecutionMetadataHelper.getChainExecution(executionMetadata, contentItemUri);
        ExecutionMetadata em;
        if(ce != null){
            em = new ExecutionMetadata(executionMetadata, contentItemUri,ce);
        } else {
            em = null;
        }
        return em;
    }
    
    private ExecutionMetadata(Graph executionMetadata, IRI contentItemUri, BlankNodeOrIRI ce){
        chainExecution = new ChainExecution(executionMetadata, ce);
        engineExecutions = new HashMap<String,Execution>();
        for(BlankNodeOrIRI ex : ExecutionMetadataHelper.getExecutions(executionMetadata, ce)){
            Execution execution = new Execution(chainExecution,executionMetadata, ex);
            engineExecutions.put(execution.getExecutionNode().getEngineName(),execution);
        }
    }
    
    public ChainExecution getChainExecution(){
        return chainExecution;
    }
    
    public Map<String,Execution> getEngineExecutions(){
        return engineExecutions;
    }
}

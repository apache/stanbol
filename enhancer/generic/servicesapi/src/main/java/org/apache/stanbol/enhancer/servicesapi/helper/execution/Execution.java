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

import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.getReference;

import java.util.Date;

import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.enhancer.servicesapi.helper.ExecutionMetadataHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata;

/**
 * The Execution of an EnhancementEngine as defined by the {@link #getExecutionNode()}
 * @author Rupert Westenthaler
 *
 */
public class Execution implements Comparable<Execution>{
    
    protected final BlankNodeOrIRI node;
    private final ExecutionNode executionNode;
    private final IRI status;
    protected final Graph graph;
    private final Date started;
    private final Date completed;
    private final Long duration;
    private final ChainExecution chain;
    public Execution(ChainExecution parent, Graph graph, BlankNodeOrIRI node) {
        this.chain = parent;
        this.graph = graph;
        this.node = node;
        BlankNodeOrIRI executionNode = ExecutionMetadataHelper.getExecutionNode(graph, node);
        if(executionNode != null){
            this.executionNode = new ExecutionNode(graph, executionNode);
        } else {
            this.executionNode = null;
        }
        this.status = getReference(graph, node, ExecutionMetadata.STATUS);
        this.started = ExecutionMetadataHelper.getStarted(graph, node);
        this.completed = ExecutionMetadataHelper.getCompleted(graph, node);
        if(started != null && completed != null){
            this.duration = completed.getTime() - started.getTime();
        } else {
            this.duration = null;
        }
    }
    
    /**
     * The Status of the execution
     * @return the status
     */
    public final IRI getStatus() {
        return status;
    }

    /**
     * The start date of the Execution
     * @return the started
     */
    public final Date getStarted() {
        return started;
    }

    /**
     * The duration of the Execution in milliseconds.
     * @return the duration
     */
    public final Long getDuration() {
        return duration;
    }
    /**
     * @return the executionNode
     */
    public ExecutionNode getExecutionNode() {
        return executionNode;
    }

    public Date getCompleted(){
        return completed;
    }
    public boolean isFailed(){
        return ExecutionMetadata.STATUS_FAILED.equals(status);
    }
    public boolean isCompleted(){
        return ExecutionMetadata.STATUS_COMPLETED.equals(status);
    }

    @Override
    public int hashCode() {
        return node.hashCode();
    }
    @Override
    public boolean equals(Object o) {
        return o instanceof ExecutionNode && ((ExecutionNode)o).node.equals(node);
    }
    @Override
    public int compareTo(Execution e2) {
        if(started != null && e2.started != null){
            int result = started.compareTo(e2.started);
            if(result == 0){
                if(completed != null && e2.completed != null){
                    result = started.compareTo(e2.completed);
                    if(result == 0){
                        return node.toString().compareTo(e2.toString());
                    } else {
                        return result;
                    }
                } else if (completed == null && e2.completed == null){
                    return node.toString().compareTo(e2.toString());
                } else {
                    return completed == null ? -1 : 1;
                }
            } else {
                return result;
            }
        } else if (started == null && e2.started == null){
            return node.toString().compareTo(e2.toString());
        } else {
            return started == null ? -1 : 1;
        }
    }

    /**
     * @return the chain
     */
    public ChainExecution getChain() {
        return chain;
    }
}
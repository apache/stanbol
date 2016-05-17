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
package org.apache.stanbol.enhancer.servicesapi.rdf;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ChainManager;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;


/**
 * Defined constants for Classes and Properties defined by the
 * {@link NamespaceEnum#em Execution Metadata ontology} used by the Stanbol
 * Enhancer to describe metadata about the enhancement process.
 *
 */
public final class ExecutionMetadata {
    
    private ExecutionMetadata(){};
    
    /**
     * Class representing an execution of a {@link Chain} or an {@link EnhancementEngine}.
     * This is considered an abstract concept. Use {@link #CHAIN_EXECUTION} or
     * {@link #ENGINE_EXECUTION} depending on the type of the executed component.
     */
    public static final IRI EXECUTION = new IRI(NamespaceEnum.em+"Execution");

    /**
     * Property that links {@link #EXECUTION} to its parent 
     * {@link #CHAIN_EXECUTION}.
     */
    public static final IRI EXECUTION_PART = new IRI(NamespaceEnum.em+"executionPart");
    
    /**
     * The current status of an {@link #EXECUTION}. Values are expected to be
     * one of {@link #EXECUTION_STATUS}.
     */
    public static final IRI STATUS = new IRI(NamespaceEnum.em+"status");

    /**
     * The 'xsd:startTime' when an {@link #EXECUTION} started
     */
    public static final IRI STARTED = new IRI(NamespaceEnum.em+"started");

    /**
     * The 'xsd:dateTime' when an {@link #EXECUTION} execution completed or
     * failed.
     */
    public static final IRI COMPLETED = new IRI(NamespaceEnum.em+"completed");

    /**
     * Allows to add a status message to a {@link #EXECUTION} node.
     */
    public static final IRI STATUS_MESSAGE = new IRI(NamespaceEnum.em+"statusMessage");
    
    /**
     * Class representing the execution of a {@link Chain}. This class is a 
     * sub-class of {@link #EXECUTION}
     */
    public static final IRI CHAIN_EXECUTION = new IRI(NamespaceEnum.em+"ChainExecution");

    /**
     * Property indicating if the {@link ExecutionPlan#EXECUTION_PLAN} executed
     * by a {@link #CHAIN_EXECUTION} was the {@link ChainManager#getDefault()}
     * {@link Chain} at that time. Values are expected to be of data type
     * 'xsd:boolean'.
     */
    public static final IRI IS_DEFAULT_CHAIN = new IRI(NamespaceEnum.em+"defualtChain");

    /**
     * Property that links from the {@link #CHAIN_EXECUTION} to the
     * {@link ExecutionPlan#EXECUTION_PLAN}
     */
    public static final IRI EXECUTION_PLAN = new IRI(NamespaceEnum.em+"executionPlan");

    /**
     * Property that links from the {@link #CHAIN_EXECUTION} node to the
     * enhanced {@link ContentItem#getUri()}
     */
    public static final IRI ENHANCES = new IRI(NamespaceEnum.em+"enhances");

    /**
     * Property that links from {@link ContentItem#getUri()} to the 
     * {@link #CHAIN_EXECUTION} defining the root node of the execution metadata
     */
    public static final IRI ENHANCED_BY = new IRI(NamespaceEnum.em+"enhancedBy");

    /**
     * Class that represents the execution of an {@link EnhancementEngine}.
     *  This is a sub-class of {@link #EXECUTION}.
     */
    public static final IRI ENGINE_EXECUTION = new IRI(NamespaceEnum.em+"EngineExecution");

    /**
     * Property that links from the {@link #ENGINE_EXECUTION} to the
     * {@link ExecutionPlan#EXECUTION_NODE}
     */
    public static final IRI EXECUTION_NODE = new IRI(NamespaceEnum.em+"executionNode");

    /**
     * Type for all ExecutionStatus values: {@link #STATUS_SCHEDULED},
     * {@link #STATUS_IN_PROGRESS}, {@link #STATUS_COMPLETED}, {@link #STATUS_SKIPPED},
     * {@link #STATUS_FAILED}.
     */
    public static final IRI EXECUTION_STATUS = new IRI(NamespaceEnum.em+"ExecutionStatus");

    /**
     * em:ExecutionStatus indicating that the execution is scheduled, but has not yet started
     */
    public static final IRI STATUS_SCHEDULED = new IRI(NamespaceEnum.em+"StatusSheduled");
    /**
     * em:ExecutionStatus indicating that the execution was skipped 
     */
    public static final IRI STATUS_SKIPPED = new IRI(NamespaceEnum.em+"StatusSkipped");
    /**
     * em:ExecutionStatus indicating that the execution is in progress
     */
    public static final IRI STATUS_IN_PROGRESS = new IRI(NamespaceEnum.em+"StatusInProgress");
    /**
     * em:ExecutionStatus indicating that the execution has completed successfully
     */
    public static final IRI STATUS_COMPLETED = new IRI(NamespaceEnum.em+"StatusCompleted");
    /**
     * em:ExecutionStatus indicating that the execution has failed
     */
    public static final IRI STATUS_FAILED = new IRI(NamespaceEnum.em+"StatusFailed");

    
}

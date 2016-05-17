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
package org.apache.stanbol.enhancer.jobmanager.event.impl;

import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.getReference;
import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.getString;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionMetadataHelper.getChainExecution;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionMetadataHelper.getExecutionPlanNode;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionMetadataHelper.initExecutionMetadata;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionMetadataHelper.initExecutionMetadataContentPart;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionMetadataHelper.isExecutionFailed;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionMetadataHelper.setExecutionCompleted;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionMetadataHelper.setExecutionFaild;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionMetadataHelper.setExecutionInProgress;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.getDependend;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.getEngine;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.isOptional;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata.STATUS;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata.STATUS_IN_PROGRESS;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionPlan.CHAIN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.NoSuchPartException;
import org.apache.stanbol.enhancer.servicesapi.helper.ExecutionMetadataHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the enhancement of a {@link ContentItem} by following the
 * execution plan provided by a {@link Chain} as parsed to an
 * {@link EnhancementJobManager}.<p>
 * TODO: This might become part of the Stanbol Enhancer Services API.
 * 
 * @author Rupert Westenthaler
 */
public class EnhancementJob {

    /*
     * NOTE on debug level Loggings
     * 
     *  ++ ... init some activity
     *  >> ... started some activity (thread has the requested lock)
     *  << ... completed some activity (thread has released the lock)
     *  
     *  n: ... no lock
     *  r: ... read lock
     *  w: ... write lock
     */
    private final Logger log = LoggerFactory.getLogger(EnhancementJob.class);

    private final Lock readLock;
    private final Lock writeLock;
    /**
     * The read only executionPlan
     */
    private final ImmutableGraph executionPlan;
    /**
     * The read/write able execution metadata. Also accessible via
     * {@link ContentItem#getPart(IRI, Class)} with the URI
     * {@link ExecutionMetadata#CHAIN_EXECUTION}
     */
    private final Graph executionMetadata;
    /**
     * Map with the em:Execution nodes of the em:ChainExecution for this
     * ContentItem. Values are are ep:ExecutionNodes of the ep:ExecutionPlan
     */
    private final BidiMap executionsMap;
    /**
     * The em:ChainExecution for this {@link ContentItem}
     */
    private final BlankNodeOrIRI chainExecutionNode;
    /**
     * The ep:ExecutionPlan for this {@link ContentItem}
     */
    private final BlankNodeOrIRI executionPlanNode;
    /**
     * The name of the {@link Chain} used to enhance this {@link ContentItem}.
     */
    private final String chain;
    /**
     * The ContentItem
     */
    private final ContentItem contentItem;

    /**
     * The completed ep:ExecutionPlan nodes. <p>
     * NOTE: This contains ep:ExecutionNodes and NOT em:Exetution instances!
     */
    private final Set<BlankNodeOrIRI> completed = new HashSet<BlankNodeOrIRI>();
    /**
     * Unmodifiable and final set of completed executables. Replaced by a new
     * instance every time {@link #completed} changes
     */
    private Set<BlankNodeOrIRI> completedExec = Collections.emptySet();
    /**
     * The running ep:ExecutionPlan nodes <p>
     * NOTE: This contains ep:ExecutionNodes and NOT em:Exetution instances!
     */
    private final Set<BlankNodeOrIRI> running = new HashSet<BlankNodeOrIRI>();
    /**
     * Unmodifiable and final set of running executables. Replaced by a new
     * instance every time {@link #running} changes.
     */
    private Set<BlankNodeOrIRI> runningExec = Collections.emptySet();

    /**
     * Unmodifiable and final set of executable em:Execution nodes. 
     * Replaced by a new instance every time {@link #running} or 
     * {@link #completed} changes.
     */
    private Set<BlankNodeOrIRI> executable;
    /**
     * Used to store any {@link Exception} parsed with the call to
     * {@link #setFailed(BlankNodeOrIRI, EnhancementEngine, Exception)} causing the
     * enhancement process to fail. This Exception is typically re-thrown by the
     * {@link EnhancementJobManager#enhanceContent(ContentItem, Chain)} method.
     * @see #getError()
     */
    private Exception error = null;
    /**
     * Constructor used to create and initialise a new enhancement job. This
     * will create the initial set of ExecutionMetadata and add them as
     * ContentPart with the URI {@link ExecutionMetadata#CHAIN_EXECUTION} to the
     * ContentItem.
     * @param contentItem
     * @param chainName
     * @param executionPlan
     * @param isDefaultChain
     */
    public EnhancementJob(ContentItem contentItem, String chainName, ImmutableGraph executionPlan, boolean isDefaultChain) {
        if (contentItem == null || chainName == null || executionPlan == null) {
            throw new IllegalArgumentException("The parsed contentItem and executionPlan MUST NOT be NULL");
        }
        this.readLock = contentItem.getLock().readLock();
        this.writeLock = contentItem.getLock().writeLock();
        executionMetadata = initExecutionMetadataContentPart(contentItem);
        if(executionMetadata.isEmpty()){
            //if we init from scratch 
            this.executionsMap = new DualHashBidiMap(initExecutionMetadata(executionMetadata, executionPlan, 
                contentItem.getUri(), chainName, isDefaultChain));
            chainExecutionNode = getChainExecution(executionMetadata, contentItem.getUri());
            executionPlanNode = getExecutionPlanNode(executionMetadata, chainExecutionNode);
            executionMetadata.addAll(executionPlan);
        } else {
            throw new IllegalArgumentException("Unable to create EnhancementJob for "
                    + "a parsed execution plan if the ContentItem already contains "
                    + "some Execution Metadata!");
        }
        this.contentItem = contentItem;
        this.executionPlan = executionPlan;
        this.chain = chainName;
        //check the first engines to execute
        checkExecutable();
    }
    /**
     * Creates an EnhancemenJob based on already existing execution metadata present
     * for a ContentItem.
     * @param contentItem the ContentItem with an already existing content part
     * containing an {@link Graph} with all required execution metadata and the 
     * execution plan.
     * @throws IllegalArgumentException if the parsed {@link ContentItem} does
     * not provide the required data to (re)initialise the EnhancementJob.
     */
    public EnhancementJob(ContentItem contentItem){
        if (contentItem == null){
            throw new IllegalArgumentException("The parsed ContentItem MUST NOT be NULL!");
        }
        this.contentItem = contentItem;
        this.readLock = contentItem.getLock().readLock();
        this.writeLock = contentItem.getLock().writeLock();
        try {
            contentItem.getPart(ExecutionMetadata.CHAIN_EXECUTION, Graph.class);
        } catch (NoSuchPartException e) {
            throw new IllegalArgumentException("Cannot (re)initialise an EnhancementJob" +
                    "without existing execution metadata content part!",e);
        }
        executionMetadata = initExecutionMetadataContentPart(contentItem);
        this.executionPlan = executionMetadata.getImmutableGraph();
        chainExecutionNode = getChainExecution(executionMetadata, contentItem.getUri());
        if(chainExecutionNode == null){
            throw new IllegalArgumentException("Cannot (re)initialise an EnhancementJob" +
                    "because the ExecutionMetadata do not contain an em:ChainExecution" +
                    "for the given ContentItem '"+contentItem.getUri()+"'!");
        }
        executionPlanNode = getExecutionPlanNode(executionMetadata, chainExecutionNode);
        if(executionPlanNode == null){
            throw new IllegalArgumentException("Cannot (re)initialise an EnhancementJob" +
                    "because the ExecutionMetadata do not contain an ep:ExecutionPlan" +
                    "for the given ContentItem '"+contentItem.getUri()+"'!");
        }
        this.chain = getString(executionPlan, executionPlanNode, CHAIN);
        if(chain == null || chain.isEmpty()){
            throw new IllegalArgumentException("Cannot (re)initialise an EnhancementJob " +
                    "because the ExecutionMetadata do not define a valid chain name for " +
                    "the ep:ExecutionPlan node '" + executionPlanNode+"' as used to " +
                    "enhance  ContentItem '"+contentItem.getUri()+"'!");
        }
        //the executionPlan is part of the execution metadata
        Map<BlankNodeOrIRI,BlankNodeOrIRI> executionsMap = initExecutionMetadata(executionMetadata, 
            executionPlan, contentItem.getUri(), null, null);
        for(Entry<BlankNodeOrIRI,BlankNodeOrIRI> executionEntry : executionsMap.entrySet()){
            IRI status = getReference(executionMetadata, executionEntry.getKey(), STATUS);
            if(status == null){
                throw new IllegalArgumentException("The ex:Execution '"
                        + executionEntry.getKey()+"' of the ex:ChainExecution for ContentItme '"
                        + contentItem.getUri()+"' is missing a required value for the property '"
                        + STATUS+"'!");
            }
            if(status.equals(STATUS_IN_PROGRESS)){
                //re-schedule unfinished enhancement jobs
                ExecutionMetadataHelper.setExecutionScheduled(executionMetadata, executionEntry.getKey());
            } else if(status.equals(ExecutionMetadata.STATUS_COMPLETED) ||
                    status.equals(ExecutionMetadata.STATUS_FAILED)){
               completed.add(executionEntry.getValue());
            }
        }
        this.executionsMap = new DualHashBidiMap(executionsMap);
        //check the first engines to execute after continuation
        checkExecutable();
    }

    /**
     * Getter for the ep:ExecutionNode linked to a em:Execution
     * @return the ep:ExecutionNode instance
     * @throws IllegalArgumentException if the parsed em:Execution is not
     * part of the execution metadata of this enhancement job
     */
    public BlankNodeOrIRI getExecutionNode(BlankNodeOrIRI execution){
        BlankNodeOrIRI node = (BlankNodeOrIRI)executionsMap.get(execution);
        if(node == null){
            throw new IllegalArgumentException("Unknown sp:ExecutionNode instance "+node);
        }
        return node;
    }
    /**
     * Getter for the em:Execution linked to a ep:ExecutionNode
     * @return the em:Execution instance 
     * @throws IllegalArgumentException if the parsed ep:ExecutionNode is not
     * part of the execution plan of this enhancement job
     */
    public BlankNodeOrIRI getExecution(BlankNodeOrIRI executionNode){
        BlankNodeOrIRI execution = (BlankNodeOrIRI)executionsMap.getKey(executionNode);
        if(execution == null){
            throw new IllegalArgumentException("Unknown em:Execution instance "+executionNode);
        }
        return execution;
    }

    /**
     * The used execution plan for processing the {@link ContentItem}
     * 
     * @return the executionPlan
     */
    public final ImmutableGraph getExecutionPlan() {
        return executionPlan;
    }

    /**
     * The ContentItem enhanced by this job.
     * 
     * @return
     */
    public final ContentItem getContentItem() {
        return contentItem;
    }

    /**
     * This returns the name of the Chain used to initialise the execution plan for this enhancement job.
     * <p>
     * NOTE that because chains can change (activate, change, deactivate) at any moment there is no guarantee
     * that the a chain with that name is still available nor that calling {@link Chain#getExecutionPlan()}
     * will be equals to the {@link #getExecutionPlan()} used by this enhancement job. This is the reason why
     * this method just returns the name of the chain and not the {@link Chain} instance.
     * 
     * @return
     */
    public final String getChainName() {
        return chain;
    }

    /**
     * Getter for the lock used to synchronise read/write to this enhancement job. This returns the lock
     * provided by {@link ContentItem#getLock()}
     * 
     * @return the read/write lock for this enhancement job
     */
    public final ReadWriteLock getLock() {
        return contentItem.getLock();
    }


    /**
     * Getter for a read only view over the currently running executions.
     * 
     * @return the currently running executions.
     */
    public Set<BlankNodeOrIRI> getRunning() {
        log.trace("++ r: {}","getRunning");
        readLock.lock();
        try {
            log.trace(">> r: {}","getRunning");
            return runningExec;
        } finally {
            log.trace("<< r: {}","getRunning");
            readLock.unlock();
        }
    }

    /**
     * Getter for a read only view over the completed execution.
     * 
     * @return the completed execution nodes
     */
    public Set<BlankNodeOrIRI> getCompleted() {
        log.trace("++ r: {}","getCompleted");
        readLock.lock();
        try {
            log.trace(">> r: {}","getCompleted");
            return completedExec;
        } finally {
            log.trace("<< r: {}","getCompleted");
            readLock.unlock();
        }
    }

    /**
     * Sets the state of the parsed executionNode to completed. This also validates if the new state
     * confirms to the ExectionPlan (e.g. if all nodes the parsed node depends on are also marked as
     * completed).
     * 
     * @param execution
     *            the exection to be marked as running
     * @throws IllegalArgumentException
     *             if <code>null</code> is parsed as execution node
     * @throws IllegalStateException
     *             if the parsed execution node can not be marked as completed because some of its
     *             depended nodes are not yet marked as completed.
     */
    public void setCompleted(BlankNodeOrIRI execution) {
        if(execution == null) {
            throw new IllegalArgumentException("The parsed em:Execution instance MUST NOT be NULL!");
        }
        writeLock.lock();
        BlankNodeOrIRI executionNode = getExecutionNode(execution);
        log.trace("++ w: {}: {}","setCompleted",getEngine(executionPlan, executionNode));
        try {
            log.trace(">> w: {}: {}","setCompleted",getEngine(executionPlan, executionNode));
            setNodeCompleted(executionNode);
            setExecutionCompleted(executionMetadata, execution, null);
        } finally {
            log.trace("<< w: {}: {}","setCompleted",getEngine(executionPlan, executionNode));
            writeLock.unlock();
        }
    }
    /**
     * Internally used to update the state kept in {@link #completed} and
     * {@link #running} and {@link #executable} after an execution was set to
     * {@link #setCompleted(BlankNodeOrIRI) completed} or 
     * {@link #setFailed(BlankNodeOrIRI, EnhancementEngine, Exception) failed}.<p>
     * This method expects to be called within an active {@link #writeLock}.
     * @param executionNode the ep:ExecutionNode linked to the em:Execution that
     * finished. 
     */
    private void setNodeCompleted(BlankNodeOrIRI executionNode) {
        String engine = getEngine(executionPlan, executionNode);
        boolean optional = isOptional(executionPlan, executionNode);
        Set<BlankNodeOrIRI> dependsOn = getDependend(executionPlan, executionNode);
        if (completed.contains(executionNode)) {
            log.warn("Execution of Engine '{}' for ContentItem {} already "
                     + "marked as completed(chain: {}, node: {}, optional {})."
                     + " -> call ignored", 
                     new Object[] {engine, contentItem.getUri().getUnicodeString(),
                                   chain, executionNode, optional});
            return;
        }
        if (!completed.containsAll(dependsOn)) {
            // TODO maybe define an own Exception for such cases
            throw new IllegalStateException("Unable to set state of ExectionNode '"
                    + executionNode+ "' (chain '"+ chain
                    + "' | contentItem '"+ contentItem.getUri()
                    + "') to completed, because some of its depended "
                    + "nodes are not marked completed yet. This indicates an Bug in the "
                    + "implementation of the JobManager used to execute the ExecutionPlan. "
                    + "(this.dependsOn=" + dependsOn + "| chain.completed " + completed
                    + " | chain.running " + running + ")!");
        }
        if (running.remove(executionNode)) {
            log.trace(
                "Execution of '{}' for ContentItem {} completed "
                + "(chain: {}, node: {}, optional {})",
                new Object[] {engine, contentItem.getUri().getUnicodeString(), 
                              chain, executionNode, optional});
        }
        completed.add(executionNode);
        //update the set with the completed and running executables
        updateCompletedExec();
        updateRunningExec();
        // update the executables ... this will also recognise if finished 
        checkExecutable();
    }

    /**
     * Sets the state of the parsed execution to running. This also validates if the new state
     * confirms to the ExectionPlan (e.g. if all nodes the parsed node depends on are already marked as
     * completed).
     * 
     * @param execution
     *            the execution to be marked as running
     * @throws IllegalArgumentException
     *             if <code>null</code> is parsed as execution node
     * @throws IllegalStateException
     *             if the parsed execution node can not be marked as running because some of its depended
     *             nodes are not yet marked as completed.
     */
    public void setRunning(BlankNodeOrIRI execution) {
        if(execution == null) {
            throw new IllegalArgumentException("The parsed em:Execution instance MUST NOT be NULL!");
        }
        BlankNodeOrIRI executionNode = getExecutionNode(execution);
        String engine = getEngine(executionPlan, executionNode);
        boolean optional = isOptional(executionPlan, executionNode);
        Set<BlankNodeOrIRI> dependsOn = getDependend(executionPlan, executionNode);
        log.trace("++ w: {}: {}","setRunning",ExecutionPlanHelper.getEngine(executionPlan, executionNode));
        writeLock.lock();
        try {
            log.trace(">> w: {}: {}","setRunning",ExecutionPlanHelper.getEngine(executionPlan, executionNode));
            if (completed.contains(executionNode)) {
                String message = "Unable to set state of ExectionNode '" + executionNode + "'(chain '"
                                 + chain + "' | contentItem '" + contentItem.getUri()
                                 + "') to running, because"
                                 + "it is already marked as completed. This indicates "
                                 + "an Bug in the implementation of the JobManager "
                                 + "used to execute the ExecutionPlan (chain state: " + "completed "
                                 + completed + " | running " + running + ")!";
                log.error(message);
                throw new IllegalStateException(message);
            }
            if (!completed.containsAll(dependsOn)) {
                // TODO maybe define an own Exception for such cases
                String message = "Unable to set state of ExectionNode '" + executionNode + "' (chain '"
                                 + chain + "' | contentItem '" + contentItem.getUri()
                                 + "') to running, because " + "some of its depended nodes are not marked "
                                 + "completed yet. This indicates an Bug in the "
                                 + "implementation of the JobManager used to execute "
                                 + "the ExecutionPlan (this.dependsOn=" + dependsOn + "| chain.completed "
                                 + completed + " | chain.running " + running + ")!";
                log.error(message);
                throw new IllegalStateException(message);
            }
            if (!running.add(executionNode)) {
                log.warn("Execution of Engine '{}' for ContentItem {} already "
                         + "marked as running(chain: {}, node: {}, optional {})."
                         + " -> call ignored", 
                         new Object[] {engine, contentItem.getUri().getUnicodeString(),
                                       chain, executionNode, optional});
                return;
            } else { //added an engine to running
                log.trace("Started Execution of '{}' for ContentItem {} "
                         + "(chain: {}, node: {}, optional {})",
                    new Object[] {engine, contentItem.getUri().getUnicodeString(), chain,
                                  executionNode, optional});
                //set the status of the execution to be in progress
                ExecutionMetadataHelper.setExecutionInProgress(executionMetadata, execution);
                // update the executables ... this will also recognise if finished
                updateRunningExec();
                //update executables
                checkExecutable();
            }
        } finally {
            log.trace("<< w: {}: {}","setRunning",ExecutionPlanHelper.getEngine(executionPlan, executionNode));
            writeLock.unlock();
        }
    }
    /**
     * updates the {@link #runningExec} based on {@link #running}
     */
    private void updateRunningExec() {
        Set<BlankNodeOrIRI> runningExec = new HashSet<BlankNodeOrIRI>(running.size());
        for(BlankNodeOrIRI node : running){
            runningExec.add(getExecution(node));
        }
        this.runningExec = Collections.unmodifiableSet(runningExec);
    }
    /**
     * updates the {@link #runningExec} based on {@link #running}
     */
    private void updateCompletedExec() {
        Set<BlankNodeOrIRI> completedExec = new HashSet<BlankNodeOrIRI>(completed.size());
        for(BlankNodeOrIRI node : completed){
            completedExec.add(getExecution(node));
        }
        this.completedExec = Collections.unmodifiableSet(completedExec);
    }
    /**
     * updated the {@link #executable} and also checks for {@link #finished}<p>
     * Assumed to be called within a write lock!
     */
    private void checkExecutable(){
        Set<BlankNodeOrIRI> executeableNodes = 
                ExecutionPlanHelper.getExecutable(executionPlan, completed);
        //a Chain finishes if no engine is running and no more nodes are executable
        if(!ExecutionMetadata.STATUS_FAILED.equals(
                getReference(executionMetadata, chainExecutionNode, STATUS))) { 
            executeableNodes.removeAll(running);
            if(log.isDebugEnabled()){
                Collection<String> engines = new ArrayList<String>(executeableNodes.size());
                for(BlankNodeOrIRI node : executeableNodes){
                    engines.add(getEngine(executionPlan, node));
                }
                log.trace("MARK {} as executeable",engines);
            }
            //we need to get the em:Executables for the ep:ExecutionNodes ...
            if(executeableNodes.isEmpty()){
                this.executable = Collections.emptySet();
            } else if( executeableNodes.size() == 1){
                this.executable = Collections.singleton(getExecution(executeableNodes.iterator().next()));
            } else {
                Set<BlankNodeOrIRI> executable = new HashSet<BlankNodeOrIRI>(executeableNodes.size());
                for(BlankNodeOrIRI exeutableNode : executeableNodes){
                    executable.add(getExecution(exeutableNode));
                }
                this.executable = Collections.unmodifiableSet(executable);
            }
        } else {
            //do not mark engines as executeable if chain already failed
            this.executable = Collections.emptySet();
        }
        if(isFinished() && !isFailed()){
            //mark the execution process as completed
            setExecutionCompleted(executionMetadata, chainExecutionNode, null);
        }
    }
    /**
     * Getter for the executable nodes.
     * @return the nodes that can be executed next based on the completed and
     * currently running engines.
     */
    public Set<BlankNodeOrIRI> getExecutable(){
        log.trace("++ r: {}","getExecutable");
        readLock.lock();
        log.trace(">> r: {}","getExecutable");
        try {
            return executable;
        } finally {
            log.trace("<< r: {}:{}","getExecutable",executable);
            readLock.unlock();  
        }
    }
    /**
     * Returns true if this chain has finished.
     * @return if this enhancement job is finished.
     */
    public boolean isFinished(){
        log.trace("++ r: {}","isFinished");
        readLock.lock();
        try {
            log.trace(">> r: {}","isFinished");
            return running.isEmpty() && // wait for running engine (regard if failed or not)
                    (executable.isEmpty() || isFailed()); //no more engines or already failed
        } finally {
            log.trace("<< r: {}","isFinished");
            readLock.unlock();
        }
    }

    public void setFailed(BlankNodeOrIRI execution, EnhancementEngine engine, Exception exception) {
        if(execution == null) {
            throw new IllegalArgumentException("The parsed em:Execution instance MUST NOT be NULL!");
        }
        BlankNodeOrIRI executionNode = getExecutionNode(execution);
        final boolean optional = isOptional(executionPlan, executionNode);
        final String engineName = getEngine(executionPlan, executionNode);
        log.trace("++ w: {}: {}","setFailed",ExecutionPlanHelper.getEngine(executionPlan, executionNode));
        writeLock.lock();
        try {
            log.trace(">> w: {}: {}","setFailed",ExecutionPlanHelper.getEngine(executionPlan, executionNode));
            StringBuilder message = new StringBuilder();
            message.append(String.format("Unable to process ContentItem '%s' with " +
            		"Enhancement Engine '%s' because the engine ", 
            		contentItem.getUri(), engineName));
            if(engine == null){
                message.append("is currently not active");
            } else {
                message.append(String.format("was unable to process the content " +
                		"(Engine class: %s)",engine.getClass().getName()));
            }
            if(exception != null){
                message.append("(Reason: ").append(exception.getMessage()).append(')');
            }
            message.append('!');
            setNodeCompleted(executionNode); //update the internal state
            //set this execution to failed
            setExecutionFaild(executionMetadata, execution, message.toString());
            //if not optional and the chain is not yet failed
            if(!optional && !isExecutionFailed(executionMetadata, chainExecutionNode)){
                //set also the whole chain to faild!
                String chainMessage = String.format(
                    "Enhancement Chain failed because of required Engine '%s' failed " +
                    "with Message: %s", engineName, message);
                setExecutionFaild(executionMetadata, chainExecutionNode, chainMessage);
                error = exception; //this member stores the exception to allow
                //re-throwing by the EnhancementJobManager.
            }
        } finally {
            log.trace("<< w: {}: {}","setFailed",ExecutionPlanHelper.getEngine(executionPlan, executionNode));
            writeLock.unlock();
        }

    }
    /**
     * Getter for the failed state. Note that EnhancementJobs might be already
     * failed but not yet finished.
     * @return if the EnhancementJob has failed or not.
     */
    public boolean isFailed() {
        log.trace("++ r: {}","isFailed");
        readLock.lock();
        try {
            log.trace(">> r: {}","isFailed");
            return isExecutionFailed(executionMetadata, chainExecutionNode);
        } finally {
            log.trace("<< r: {}","isFailed");
            readLock.unlock();
        }
    }
// NOTE: use default implementations of hashCode and equals for now as we need
//       to support the concurrent enhancement of ContentItems with the same
//       URI. Also two ContentItems with the same URI might still have other
//       content (as users can manually parse the URI in the request). 
//    @Override
//    public int hashCode() {
//        return contentItem.getUri().hashCode();
//    }
//    @Override
//    public boolean equals(Object o) {
//        return o instanceof EnhancementJob && 
//                contentItem.getUri().equals(((EnhancementJob)o).contentItem.getUri());
//    }
    @Override
    public String toString() {
        return "EnhancementJob for ContentItem "+contentItem.getUri();
    }

    /**
     * if {@link #isFailed()} this may contain the {@link Exception} that caused
     * the enhancement job to fail. 
     * @return The Exception or <code>null</code> if no exception is available
     */
    public Exception getError(){
        return error;
    }
    public String getErrorMessage() {
        readLock.lock();
        try {
            return getString(executionMetadata, chainExecutionNode, ExecutionMetadata.STATUS_MESSAGE);
        } finally {
            readLock.unlock();
        }
    }
    /**
     * Getter for the ExecutionMetadata.
     * @return the execution metadata.
     */
    public Graph getExecutionMetadata() {
        return executionMetadata;
    }
    /**
     * Marks the execution of the enhancement process as started. In other
     * words this sets the status of the 'em:ChainExecution' instance that
     * 'em:enhances' the {@link ContentItem} to 
     * {@link ExecutionMetadata#STATUS_IN_PROGRESS}
     */
    public void startProcessing() {
        writeLock.lock();
        try {
            setExecutionInProgress(executionMetadata, chainExecutionNode);
        } finally {
            writeLock.unlock();
        }
        
    }
}

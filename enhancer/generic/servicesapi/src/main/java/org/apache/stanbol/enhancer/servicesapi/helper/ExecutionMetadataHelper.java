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

import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.get;
import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.getReference;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.getEngine;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.getExecutionNodes;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata.CHAIN_EXECUTION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata.COMPLETED;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata.ENGINE_EXECUTION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata.ENHANCED_BY;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata.ENHANCES;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata.EXECUTION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata.EXECUTION_NODE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata.EXECUTION_PART;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata.EXECUTION_PLAN;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata.IS_DEFAULT_CHAIN;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata.STARTED;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata.STATUS;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata.STATUS_COMPLETED;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata.STATUS_FAILED;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata.STATUS_IN_PROGRESS;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata.STATUS_MESSAGE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata.STATUS_SCHEDULED;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata.STATUS_SKIPPED;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionPlan.CHAIN;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.commons.rdf.BlankNode;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.NoSuchPartException;
import org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata;
import org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionPlan;
/**
 * This class defines utility methods for writing and updating 
 * Execution Metadata. This will be usually needed by 
 * {@link EnhancementJobManager} implementations.
 * @author Rupert Westenthaler
 *
 */
public final class ExecutionMetadataHelper {

    /**
     * Restrict instantiation
     */
    private ExecutionMetadataHelper() {}

    private static final LiteralFactory lf = LiteralFactory.getInstance();
    
    public static BlankNodeOrIRI createChainExecutionNode(Graph graph, BlankNodeOrIRI executionPlan, 
                                        IRI ciUri, boolean defaultChain){
        BlankNodeOrIRI node = new BlankNode();
        graph.add(new TripleImpl(node, RDF_TYPE, EXECUTION));
        graph.add(new TripleImpl(node, RDF_TYPE, CHAIN_EXECUTION));
        graph.add(new TripleImpl(node, ENHANCES, ciUri));
        graph.add(new TripleImpl(ciUri, ENHANCED_BY, node));
        graph.add(new TripleImpl(node, STATUS, STATUS_SCHEDULED));
        graph.add(new TripleImpl(node, EXECUTION_PLAN, executionPlan));
        graph.add(new TripleImpl(node, IS_DEFAULT_CHAIN, 
            lf.createTypedLiteral(defaultChain)));
        return node;
    }
    
    public static BlankNodeOrIRI createEngineExecution(Graph graph, BlankNodeOrIRI chainExecution,
                                     BlankNodeOrIRI executionNode){
        
        BlankNodeOrIRI node = new BlankNode();
        graph.add(new TripleImpl(node, RDF_TYPE, EXECUTION));
        graph.add(new TripleImpl(node, RDF_TYPE, ENGINE_EXECUTION));
        graph.add(new TripleImpl(node, EXECUTION_PART, chainExecution));
        graph.add(new TripleImpl(node, EXECUTION_NODE, executionNode));
        graph.add(new TripleImpl(node, STATUS, STATUS_SCHEDULED));
        return node;
    }
    /**
     * Sets the state of the ExecutionNode to completed
     * @param graph
     * @param execution
     * @param message An optional message
     */
    public static void setExecutionCompleted(Graph graph,BlankNodeOrIRI execution,String message){
        Literal dateTime = lf.createTypedLiteral(new Date());
        setStatus(graph, execution,STATUS_COMPLETED);
        graph.add(new TripleImpl(execution, COMPLETED, dateTime));
        if(message != null){
            graph.add(new TripleImpl(execution, STATUS_MESSAGE, new PlainLiteralImpl(message)));
        }
    }
    /**
     * Sets the state of the ExecutionNode to scheduled and deletes any started,
     * completed times
     * @param graph the graph holding the execution metadata
     * @param execution the execution node
     */
    public static void setExecutionScheduled(Graph graph,BlankNodeOrIRI execution){
        setStatus(graph, execution,STATUS_SCHEDULED);
        Iterator<Triple> it = graph.filter(execution, STARTED, null);
        while(it.hasNext()){
            it.next();
            it.remove();
        }
        it = graph.filter(execution, COMPLETED, null);
        while(it.hasNext()){
            it.next();
            it.remove();
        }
    }
    /**
     * Set the parsed execution node to failed.
     * @param graph
     * @param execution
     * @param message An message describing why the execution failed
     */
    public static void setExecutionFaild(Graph graph,BlankNodeOrIRI execution,String message){
        Literal dateTime = lf.createTypedLiteral(new Date());
        setStatus(graph, execution,STATUS_FAILED);
        graph.add(new TripleImpl(execution, COMPLETED, dateTime));
        if(message != null){
            graph.add(new TripleImpl(execution, STATUS_MESSAGE, new PlainLiteralImpl(message)));
        } else {
            throw new IllegalArgumentException("For faild Execution a STATUS message is required!");
        }
    }
    /**
     * Sets an execution node to skipped. This sets both the start and the completed
     * time to the current time.
     * @param graph
     * @param execution
     * @param message An optional message why this execution was skipped
     */
    public static void setExecutionSkipped(Graph graph,BlankNodeOrIRI execution,String message){
        Literal dateTime = lf.createTypedLiteral(new Date());
        setStatus(graph, execution,STATUS_SKIPPED);
        graph.add(new TripleImpl(execution, STARTED, dateTime));
        graph.add(new TripleImpl(execution, COMPLETED, dateTime));
        if(message != null){
            graph.add(new TripleImpl(execution, STATUS_MESSAGE, new PlainLiteralImpl(message)));
        }
    }
    /**
     * Sets an execution node to in-progress. This also sets the start time to
     * the current time
     * @param graph
     * @param execution
     */
    public static void setExecutionInProgress(Graph graph,BlankNodeOrIRI execution){
        Literal dateTime = lf.createTypedLiteral(new Date());
        setStatus(graph, execution,STATUS_IN_PROGRESS);
        graph.add(new TripleImpl(execution, STARTED, dateTime));
    }
    
    /**
     * Removes the current value of {@link ExecutionMetadata#STATUS} and set it
     * to the parsed value.
     * @param graph
     * @param execution
     */
    private static void setStatus(Graph graph, BlankNodeOrIRI execution, IRI status) {
        Iterator<Triple> it = graph.filter(execution, STATUS, null);
        while(it.hasNext()){
            it.next();
            it.remove();
        }
        if(status != null){
            graph.add(new TripleImpl(execution, STATUS, status));
        }
    }
    
    /**
     * Getter for the {@link ExecutionMetadata#CHAIN_EXECUTION} node of an 
     * graph containing ChainExecution metadata for the parsed {@link Chain#getName()}.
     * If both the execution metadata and the execution plan are in the same
     * graph one need to parse the same triple collection instance for both
     * the execution metadata and the execution plan.
     * @param em the triple collection containing execution metadata
     * @param ep the triple collection containing the execution plan
     * @param chainName the name of the executed chain
     * @return the node or <code>null</code> if not found.
     */
    public static final BlankNodeOrIRI getChainExecutionForChainName(Graph em, Graph ep, String chainName){
        final BlankNodeOrIRI executionPlanNode = ExecutionPlanHelper.getExecutionPlan(ep, chainName);
        if(executionPlanNode == null){
            return null;
        } else {
            return getChainExecutionForExecutionPlan(em, executionPlanNode);
        }
    }

    /**
     * Getter for the {@link ExecutionMetadata#CHAIN_EXECUTION} node of an 
     * graph containing ChainExecution metadata for the parsed 
     * {@link ExecutionPlan#EXECUTION_PLAN} node
     * @param graph the graph containing the Execution Metadata
     * @param executionPlanNode the {@link ExecutionPlan#EXECUTION_PLAN} node
     * @return the {@link ExecutionMetadata#CHAIN_EXECUTION} node
     */
    public static BlankNodeOrIRI getChainExecutionForExecutionPlan(Graph graph, final BlankNodeOrIRI executionPlanNode) {
        if(graph == null){
            throw new IllegalArgumentException("The parsed graph with the execution metadata MUST NOT be NULL!");
        }
        if(executionPlanNode == null){
            throw new IllegalArgumentException("The parsed execution plan node MUST NOT be NULL!");
        }
        Iterator<Triple> it = graph.filter(null, ExecutionMetadata.EXECUTION_PLAN, executionPlanNode);
        if(it.hasNext()){
            return it.next().getSubject();
        } else {
            return null;
        }
    }
    /**
     * Getter for the execution metadata content part.
     * @param contentItem the content item
     * @return the content part
     * @throws NoSuchPartException if no execution metadata are present in the
     * content part
     * @since 0.12.1
     */
    public static Graph getExecutionMetadata(ContentItem contentItem) {
        if(contentItem == null) {
            throw new IllegalArgumentException("The parsed ContentItme MUST NOT be NULL!");
        }
        contentItem.getLock().readLock().lock();
        try{
            return contentItem.getPart(CHAIN_EXECUTION, Graph.class);
        }finally{
            contentItem.getLock().readLock().unlock();
        }
    }
    
    /**
     * Getter/Initialiser for the execution metadata content part of the parsed
     * content item. This part is expected to be registered with the URI
     * {@link ExecutionMetadata#CHAIN_EXECUTION}. If it does not already exist
     * this method creates an empty graph and register it with the parsed
     * content item otherwise it returns the existing part registered under that
     * URI.<p>
     * Typically users will also want to use 
     * {@link #initExecutionMetadata(Graph, Graph, IRI, String, boolean)}
     * to initialise the state based on the grpah returned by this method.
     * NOTES:<ul>
     * <li> If a content part is registered under the URI 
     * {@link ExecutionMetadata#CHAIN_EXECUTION} that is not of type
     * {@link Graph} this method will replace it with an empty {@link Graph}.
     * <li> This method acquires a write lock on the content item while checking
     * for the content part.
     * </ul>
     * @param contentItem the contentItem
     * @return the {@link Graph} with the execution metadata as registered as
     * content part with the URI {@link ExecutionMetadata#CHAIN_EXECUTION} to 
     * the {@link ContentItem}
     * @throws IllegalArgumentException if the parsed content itme is <code>null</code>.
     */
    public static Graph initExecutionMetadataContentPart(ContentItem contentItem) {
        if(contentItem == null){
          throw new IllegalArgumentException("The parsed ContentItme MUST NOT be NULL!");  
        }
        Graph executionMetadata;
        contentItem.getLock().writeLock().lock();
        try {
            try {
                executionMetadata = contentItem.getPart(CHAIN_EXECUTION, Graph.class);
            } catch (NoSuchPartException e) {
                executionMetadata = new IndexedGraph();
                contentItem.addPart(CHAIN_EXECUTION, executionMetadata);
            }
        } finally {
            contentItem.getLock().writeLock().unlock();
        }
        return executionMetadata;
    }
        
    /**
     * Initialises execution metadata based on the parsed parameter. If the parsed
     * graph with the execution metadata is empty it will initialise the metadata
     * based on the execution plan. If there are already metadata in the graph
     * it will initialise the returned map based on the existing data.<p>
     * This method can be therefore used to both:<ul>
     * <li> create a new set of execution metadata: as needed before a
     * {@link EnhancementJobManager} implementation can start processing a 
     * {@link ContentItem} by using an {@link Chain} 
     * <li> read existing executionMetadata allowing to let an 
     * {@link EnhancementJobManager} to continue from an uncompleted enhancement.
     * </ul><p>
     * If both the execution metadata and the execution plan are stored within the
     * same graph users need to base this graph as both the first and second
     * parameter
     * @param em The graph containing the execution metadata. MUST NOT be NULL
     * @param ep The graph containing the execution plan. MUST NOT be NULL
     * @param ciUri the URI of the content item. MUST NOT be NULL
     * @param chainName the name of the chain to execute. May be NULL if
     * initialising from existing metadata. MUST NOT be NULL if initialising from
     * empty execution metadata
     * @param isDefaultChain if the chain to execute is the default chain. Will be
     * ignored if initialising from existing execution metadata. MUST NOT be NULL
     * if initialising from empty execution metadata
     * @return A map containing all em:Execution nodes as key and the according
     * ep:ExecutionNode of the execution plan as values.
     * @throws IllegalArgumentException if any of the requirements stated in the
     * documentation for the parameters is not fulfilled.
     */
    public static final Map<BlankNodeOrIRI,BlankNodeOrIRI> initExecutionMetadata(Graph em, Graph ep, IRI ciUri, String chainName, Boolean isDefaultChain){
        if(em == null){
            throw new IllegalArgumentException("The parsed ExecutionMetadata graph MUST NOT be NULL!");
        }
        if(ciUri == null){
            throw new IllegalArgumentException("The parsed URI of the contentItem MUST NOT be NULL!");
        }
        //1. check for the ChainExecution node for the parsed content item
        final BlankNodeOrIRI executionPlanNode;
        BlankNodeOrIRI chainExecutionNode = getChainExecutionForExecutionPlan(em, ciUri);
        if(chainExecutionNode != null){ //init from existing executin metadata
            // -> chainName and isDefaultChain may be null
            //init from existing
            executionPlanNode = getExecutionPlanNode(em, chainExecutionNode);
            if(executionPlanNode == null){
                throw new IllegalArgumentException("The em:ChainExecution '"
                        + chainExecutionNode+"'that enhances ContentItem '"
                        + ciUri +"' does not define a link to an valid ExecutionPlan");
            }
            isDefaultChain = get(em, chainExecutionNode, IS_DEFAULT_CHAIN, Boolean.class, lf);
            String extractedChainName = EnhancementEngineHelper.getString(ep, executionPlanNode, CHAIN);
            if(extractedChainName == null){
                throw new IllegalArgumentException("The em:ChainExecution '"
                        + chainExecutionNode + "'that enhances ContentItem '"
                        + ciUri + "' links to the ep:ExecutionPlan '"
                        + executionPlanNode+"' that does not define a ChainName (property: "
                        + CHAIN + ")!");
            }
            if(chainName == null){
                chainName = extractedChainName;
            } else if(!chainName.equals(extractedChainName)){
                throw new IllegalArgumentException("The em:ChainExecution '"
                        + chainExecutionNode + "'that enhances ContentItem '"
                        + ciUri + "' links to the ep:ExecutionPlan '"
                        + executionPlanNode + "' with the chain name '"
                        + extractedChainName + "' but '"+chainName+"' was parsed "
                        + "as expected chain name!");
            }
        } else { //create a new one 
            // -> in that case chainName and isDefaultChain are required
            executionPlanNode = ExecutionPlanHelper.getExecutionPlan(ep, chainName);
            if(executionPlanNode == null){
                throw new IllegalArgumentException("The parsed ExectuonPlan graph does not contain an" +
                        "ExecutionPlan for a Chain with the name '"+chainName+"'!");
            }
            if(isDefaultChain == null){
                throw new IllegalArgumentException("The isDefaultChain parameter MUST NOT" +
                		"be NULL if initialising from empty ExecutionMetadata!");
            }
            chainExecutionNode = createChainExecutionNode(em, executionPlanNode, ciUri, isDefaultChain);
        }
        //2. check/init the EngineExecution nodes for for the ExecutionNodes of the ExecutionPlan
        Map<BlankNodeOrIRI,BlankNodeOrIRI> executionsMap = new HashMap<BlankNodeOrIRI,BlankNodeOrIRI>();
        Set<BlankNodeOrIRI> executionNodes = getExecutionNodes(ep, executionPlanNode);
        Set<BlankNodeOrIRI> executions = getExecutions(em, chainExecutionNode);
        for(BlankNodeOrIRI en : executionNodes) {
            Iterator<Triple> it = em.filter(null, EXECUTION_NODE, en);
            BlankNodeOrIRI execution;
            if(it.hasNext()){
                execution = it.next().getSubject();
                if(!executions.contains(execution)){
                    throw new IllegalStateException("Execution '"+execution
                        + "' for ExecutionNode '"+en+"' (engine: '"+getEngine(ep, en)
                        + "') is not part of ChainExecution '"
                        + chainExecutionNode +"' (chain: '"+chainName+")!");
                }
            } else {
                execution = createEngineExecution(em, chainExecutionNode, en);
                executions.add(execution);
            }
            executionsMap.put(execution,en);
        }
        //3. check that there are no executions that are not part of the
        //   parsed ExecutionPlan
        for(BlankNodeOrIRI e : executions){
            if(!executionsMap.containsKey(e)){
                BlankNodeOrIRI en = getExecutionNode(em, e);
                throw new IllegalStateException("ChainExecution '"
                        + chainExecutionNode +"' (chain: '"+chainName+") contains"
                        + "Execution '"+e+"' for ExecutionNode '" + en
                        + "' (engine: '"+ExecutionPlanHelper.getEngine(ep, en)
                        + "') that is not part of the pased ExecutionPlan '"
                        + executionPlanNode +"'(chain; '"+chainName+"')!");
            }
        }
        return executionsMap;
    }
    /**
     * Getter for the ep:ExecutionNode for a given em:Execution.
     * @param graph the graph containing the execution metadata
     * @param execution the em:Execution node
     * @return the ep:ExecutionNode node
     */
    public static BlankNodeOrIRI getExecutionNode(Graph graph, BlankNodeOrIRI execution){
        Iterator<Triple> it = graph.filter(execution, EXECUTION_NODE, null);
        if(it.hasNext()){
            Triple t = it.next();
            RDFTerm o = t.getObject();
            if(o instanceof BlankNodeOrIRI){
                return (BlankNodeOrIRI)o;
            } else {
                throw new IllegalStateException("Value of property "+ EXECUTION_NODE
                    + "MUST BE of type BlankNodeOrIRI (triple: '"+t+"')!");
            }
        } else {
            //maybe an em:ChainExecution
            return null;
        }
    }
    
    /**
     * Get {@link ExecutionMetadata#EXECUTION} nodes that are 
     * {@link ExecutionMetadata#EXECUTION_PART} of the parsed 
     * {@link ExecutionMetadata#CHAIN_EXECUTION} node
     * @param em the graph with the execution metadata
     * @param chainExecutionNode the chain execution node
     * @return the Set with all execution part of the chain execution
     */
    public static Set<BlankNodeOrIRI> getExecutions(Graph em, BlankNodeOrIRI chainExecutionNode) {
        if(em == null){
            throw new IllegalArgumentException("The parsed graph with the Execution metadata MUST NOT be NULL!");
        }
        if(chainExecutionNode == null){
            throw new IllegalArgumentException("The parsed chain execution plan node MUST NOT be NULL!");
        }
        Set<BlankNodeOrIRI> executionNodes = new HashSet<BlankNodeOrIRI>();
        Iterator<Triple> it = em.filter(null, ExecutionMetadata.EXECUTION_PART, chainExecutionNode);
        while(it.hasNext()){
            executionNodes.add(it.next().getSubject());
        }
        return executionNodes;
    }

    /**
     * Getter for the ep:ExecutionPlan node for the parsed em:ChainExecution node
     * @param em the execution metadata
     * @param chainExecutionNode the chain execution node
     * @return the execution plan node
     */
    public static BlankNodeOrIRI getExecutionPlanNode(Graph em, BlankNodeOrIRI chainExecutionNode){
        Iterator<Triple> it = em.filter(chainExecutionNode, EXECUTION_PLAN, null);
        if(it.hasNext()){
            Triple t = it.next();
            RDFTerm r = t.getObject();
            if(r instanceof BlankNodeOrIRI){
                return (BlankNodeOrIRI)r;
            } else {
                throw new IllegalStateException("Value of the property "+EXECUTION_PLAN
                    + " MUST BE a BlankNodeOrIRI (triple: '"+t+"')!");
            }
        } else {
            return null;
        }
        
    }
    /**
     * Getter for the ChainExecution used to enhance the content item
     * @param em the graph with the execution metadata
     * @param ciUri the ID of the content item
     * @return the node that {@link ExecutionMetadata#ENHANCES} the {@link ContentItem}
     */
    public static BlankNodeOrIRI getChainExecution(Graph em, IRI ciUri){
        Iterator<Triple> it = em.filter(null, ENHANCES, ciUri);
        if(it.hasNext()){
            return it.next().getSubject();
        } else {
            return null;
        }
    }
    /**
     * Tests if the {@link ExecutionMetadata#STATUS status} if an 
     * {@link ExecutionMetadata#EXECUTION execution} is
     * {@link ExecutionMetadata#STATUS_FAILED failed}.
     * @param graph the graph with the execution metadata
     * @param execution the execution node
     * @return <code>true</code> if the status is faild. Otherwise <code>false</code>.
     */
    public static boolean isExecutionFailed(Graph graph, BlankNodeOrIRI execution){
        return STATUS_FAILED.equals(getReference(graph,execution,STATUS));
    }
    /**
     * Tests if the {@link ExecutionMetadata#STATUS status} if an 
     * {@link ExecutionMetadata#EXECUTION execution} has already finished. This
     * includes the states {@link ExecutionMetadata#STATUS_COMPLETED completed} 
     * and {@link ExecutionMetadata#STATUS_FAILED failed}.
     * @param graph the graph with the execution metadata
     * @param execution the execution node
     * @return <code>true</code> if the execution has already finished
     */
    public static boolean isExecutionFinished(Graph graph, BlankNodeOrIRI execution){
        IRI status = getReference(graph,execution,STATUS);
        return STATUS_FAILED.equals(status) || STATUS_COMPLETED.equals(status);
    }
    /**
     * Getter for the started dateTime of an 'em:Execution'
     * @param graph the graph
     * @param execution the execution instance
     * @return the time or <code>null</code> if not present
     */
    public static Date getStarted(Graph graph, BlankNodeOrIRI execution){
        return get(graph, execution, ExecutionMetadata.STARTED, Date.class, lf);
    }
    /**
     * Getter for the completed dateTime of an 'em:Execution'
     * @param graph the graph
     * @param execution the execution instance
     * @return the time or <code>null</code> if not present
     */
    public static Date getCompleted(Graph graph, BlankNodeOrIRI execution){
        return get(graph, execution, ExecutionMetadata.COMPLETED, Date.class, lf);
    }
}

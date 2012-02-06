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

import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.EXECUTION_ORDER_COMPARATOR;
import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.get;
import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.getEngineOrder;
import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.getString;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.getExecutable;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.writeExecutionNode;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionPlan.CHAIN;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionPlan.DEPENDS_ON;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionPlan.ENGINE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionPlan.EXECUTION_NODE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionPlan.EXECUTION_PLAN;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionPlan.HAS_EXECUTION_NODE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionPlan.OPTIONAL;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;

import java.awt.peer.LightweightPeer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.enhancer.servicesapi.ChainException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngineManager;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionPlan;

public final class ExecutionPlanHelper {
    
    private static LiteralFactory lf = LiteralFactory.getInstance();
    
    private ExecutionPlanHelper(){/* Do not allow instances of utility classes*/}
    /**
     * Writes all triples for an ep:ExecutionNode to the parsed {@link MGraph}.
     * An {@link BNode} is use for representing the execution node resource.
     * @param graph the graph to write the triples. MUST NOT be empty
     * @param epNode the NonLiteral representing the ep:ExecutionPlan
     * @param engineName the name of the engine. MUST NOT be <code>null</code> nor empty
     * @param optional if the execution of this node is optional or required
     * @param dependsOn other nodes that MUST BE executed before this one. Parse 
     * <code>null</code> or an empty set if none.
     * @return the resource representing the added ep:ExecutionNode.
     */
    public static NonLiteral writeExecutionNode(MGraph graph,NonLiteral epNode, String engineName, boolean optional, Set<NonLiteral> dependsOn){
        if(graph == null){
            throw new IllegalArgumentException("The parsed MGraph MUST NOT be NULL!");
        }
        if(engineName == null || engineName.isEmpty()){
            throw new IllegalArgumentException("The parsed Engine name MUST NOT be NULL nor empty!");
        }
        if(epNode == null){
            throw new IllegalArgumentException("The ep:ExecutionPlan instance MUST NOT be NULL!");
        }
        NonLiteral node = new BNode();
        graph.add(new TripleImpl(epNode, HAS_EXECUTION_NODE, node));
        graph.add(new TripleImpl(node, RDF_TYPE, EXECUTION_NODE));
        graph.add(new TripleImpl(node,ENGINE,new PlainLiteralImpl(engineName)));
        if(dependsOn != null){
            for(NonLiteral dependend : dependsOn){
                if(dependend != null){
                    graph.add(new TripleImpl(node, DEPENDS_ON, dependend));
                }
            }
        }
        graph.add(new TripleImpl(node, OPTIONAL, lf.createTypedLiteral(optional)));
        return node;
    }
    /**
     * Creates an ExecutionPlan for the parsed chainName in the parsed Graph
     * @param graph the graph
     * @param chainName the chain name
     * @return the node representing the ex:ExecutionPlan
     */
    public static NonLiteral createExecutionPlan(MGraph graph,String chainName){
        if(graph == null){
            throw new IllegalArgumentException("The parsed MGraph MUST NOT be NULL!");
        }
        if(chainName == null || chainName.isEmpty()){
            throw new IllegalArgumentException("The parsed Chain name MUST NOT be NULL nor empty!");
        }
        NonLiteral node = new BNode();
        graph.add(new TripleImpl(node, RDF_TYPE, EXECUTION_PLAN));
        graph.add(new TripleImpl(node, CHAIN,new PlainLiteralImpl(chainName)));
        return node;
    }
    
    /**
     * Evaluates the parsed {@link Graph execution plan} and the set of already executed
     * {@link ExecutionPlan#EXECUTION_NODE ep:ExecutionNode}s to find the next
     * nodes that can be executed. 
     * @param executionPlan the execution plan
     * @param executed the already executed {@link ExecutionPlan#EXECUTION_NODE node}s
     * or an empty set to determine the nodes to start the execution.
     * @return the set of nodes that can be executed next or an empty set if
     * there are no more nodes to execute.
     */
    public static Set<NonLiteral>getExecutable(TripleCollection executionPlan, Set<NonLiteral> executed){
        Set<NonLiteral> executeable = new HashSet<NonLiteral>();
        for(Iterator<Triple> nodes = executionPlan.filter(null, RDF_TYPE, EXECUTION_NODE);nodes.hasNext();){
            NonLiteral node = nodes.next().getSubject();
            if(!executed.contains(node)){
                Iterator<Triple> dependsIt = executionPlan.filter(node, DEPENDS_ON, null);
                boolean dependendExecuted = true;
                while(dependsIt.hasNext() && dependendExecuted){
                    dependendExecuted = executed.contains(dependsIt.next().getObject());
                }
                if(dependendExecuted){
                    executeable.add(node);
                }
            }
        }
        return executeable;
    }
    /**
     * Creates an execution plan based on the 
     * {@link ServiceProperties#ENHANCEMENT_ENGINE_ORDERING} of the parsed
     * EnhancementEngines. NOTE that the parsed list is modified as it is sroted by
     * using the {@link EnhancementEngineHelper#EXECUTION_ORDER_COMPARATOR}.<p>
     * A second parameter with the set of optional engines can be used to define
     * what {@link ExecutionPlan#EXECUTION_NODE} in the execution plan should be 
     * marked as {@link ExecutionPlan#OPTIONAL}.
     * @param chainName the name of the Chain to build the execution plan for
     * @param availableEngines the list of engines
     * @param the names of optional engines.
     * @return the execution plan
     */
    public static Graph calculateExecutionPlan(String chainName, List<EnhancementEngine> availableEngines, Set<String> optional, Set<String> missing) {
        if(chainName == null || chainName.isEmpty()){
            throw new IllegalArgumentException("The parsed ChainName MUST NOT be empty!");
        }
        Collections.sort(availableEngines,EXECUTION_ORDER_COMPARATOR);
        //now we have all required and possible also optional engines
        //  -> build the execution plan
        MGraph ep = new IndexedMGraph();
        NonLiteral epNode = createExecutionPlan(ep, chainName);
        Integer prevOrder = null;
        Set<NonLiteral> prev = null;
        Set<NonLiteral> current = new HashSet<NonLiteral>();
        for(String name : missing){
            boolean optionalMissing = optional.contains(name);
            NonLiteral node = writeExecutionNode(ep, epNode, name, optionalMissing, null);
            if(!optionalMissing){
                current.add(node);
            } // else add missing optional engines without any dependsOn restrictions
        }
        for(EnhancementEngine engine : availableEngines){
            String name = engine.getName();
            Integer order = getEngineOrder(engine);
            if(prevOrder == null || !prevOrder.equals(order)){
                prev = current;
                current = new HashSet<NonLiteral>();
                prevOrder = order;
            }
            current.add(writeExecutionNode(ep, epNode, name, optional.contains(name), prev));
        }
        return ep.getGraph();
    }
    /**
     * Utility that checks if the parsed graph contains a valid execution
     * plan. This method is intended to be used by components that need to
     * ensure that an parsed graph contains a valid execution plan.<p>
     * This especially checks: <ul>
     * <li> if for all {@link ExecutionPlan#EXECUTION_NODE}s
     * <li> if they define a unary and valid value for the
     * {@link ExecutionPlan#ENGINE} property and
     * <li> if all {@link ExecutionPlan#DEPENDS_ON} values do actually point
     * to an other execution node in the parsed graph
     * <ul><p>
     * This method does not modify the parsed graph. Therefore it is save
     * to parse a {@link Graph} object.<p>
     * TODO: There is no check for cycles implemented yet.
     * @param the graph to check
     * @return the engine names referenced by the validated execution plan-
     * @throws ChainException
     */
    public static Set<String> validateExecutionPlan(TripleCollection executionPlan) throws ChainException {
        Iterator<Triple> executionNodeIt = executionPlan.filter(null, RDF_TYPE, EXECUTION_NODE);
        Set<String> engineNames = new HashSet<String>();
        Map<NonLiteral, Collection<NonLiteral>> nodeDependencies = new HashMap<NonLiteral,Collection<NonLiteral>>();
        //1. check the ExecutionNodes
        while(executionNodeIt.hasNext()){
            NonLiteral node = executionNodeIt.next().getSubject();
            Iterator<String> engines = EnhancementEngineHelper.getStrings(executionPlan, node,ENGINE);
            if(!engines.hasNext()){
                throw new ChainException("Execution Node "+node+" does not define " +
                        "the required property "+ENGINE+"!");
            }
            String engine = engines.next();
            if(engines.hasNext()){
                throw new ChainException("Execution Node "+node+" does not define " +
                        "multiple values for the property "+ENGINE+"!");
            }
            if(engine.isEmpty()){
                throw new ChainException("Execution Node "+node+" does not define " +
                        "an empty String as engine name (property "+ENGINE+")!");
            }
            engineNames.add(engine);
            Collection<NonLiteral> dependsOn = new HashSet<NonLiteral>();
            for(Iterator<Triple> t = executionPlan.filter(node, DEPENDS_ON, null);t.hasNext();){
                Resource o = t.next().getObject();
                if(o instanceof NonLiteral){
                    dependsOn.add((NonLiteral)o);
                } else {
                    throw new ChainException("Execution Node "+node+" defines the literal '" +
                        o+"' as value for the "+DEPENDS_ON +" property. However this" +
                        "property requires values to be bNodes or URIs.");
                }
            }
            nodeDependencies.put(node, dependsOn);
        }
        //2. now check the dependency graph
        for(Entry<NonLiteral,Collection<NonLiteral>> entry : nodeDependencies.entrySet()){
            if(entry.getValue() != null){
                for(NonLiteral dependent : entry.getValue()){
                    if(!nodeDependencies.containsKey(dependent)){
                        throw new ChainException("Execution Node "+entry.getKey()+
                            " defines a dependency to an non existent ex:ExectutionNode "+
                            dependent+"!");
                    } //else the dependency is valid
                }
            } //no dependencies
        }
        //done ... the parsed graph survived all consistency checks :)
        return engineNames;
    }
    
    public static Set<NonLiteral> getDependend(TripleCollection executionPlan, NonLiteral executionNode){
        Set<NonLiteral> dependend = new HashSet<NonLiteral>();
        addDependend(dependend, executionPlan, executionNode);
        return dependend;
    }
    public static void addDependend(Collection<NonLiteral> collection, TripleCollection executionPlan, NonLiteral executionNode){
        for(Iterator<Triple> it = executionPlan.filter(executionNode, DEPENDS_ON, null);
                it.hasNext();collection.add((NonLiteral)it.next().getObject()));
    }
    public static boolean isOptional(TripleCollection executionPlan, NonLiteral executionNode) {
        Boolean optional = get(executionPlan,executionNode,OPTIONAL,Boolean.class,lf);
        return optional == null ? false : optional.booleanValue();
    }
    public static String getEngine(TripleCollection executionPlan, NonLiteral executionNode) {
        return getString(executionPlan, executionNode, ENGINE);
    }

    /**
     * Calculates a sorted list of active EnhancementEngines form the given
     * ExecutinPlan
     * @param engineManager The engine manager (OSGI service or {@link EnginesTracker})
     * @param ep the execution plan
     * @return
     */
    public static List<EnhancementEngine> getActiveEngines(EnhancementEngineManager engineManager, TripleCollection ep) {
        List<EnhancementEngine> engines = new ArrayList<EnhancementEngine>();
        Set<NonLiteral> visited = new HashSet<NonLiteral>();
        Set<NonLiteral> executeable;
        do {
            executeable = getExecutable(ep, visited);
            for(NonLiteral node : executeable){
                String engineName = getString(ep, node, ENGINE);
                EnhancementEngine engine = engineManager.getEngine(engineName);
                if(engine != null){
                    engines.add(engine);
                }
                visited.add(node);
            }
        } while(!executeable.isEmpty());
        return engines;
    }
    
    /**
     * Getter for the {@link ExecutionPlan#EXECUTION_PLAN} node of an execution
     * plan for the given chainNmame. This method is handy for components that
     * need to get an execution plan for a graph that might potentially contain
     * more than a single execution plan.
     * @param graph the graph
     * @param chainName the chain name
     * @return the node or <code>null</code> if not found
     */
    public static NonLiteral getExecutionPlan(TripleCollection graph, String chainName){
        if(graph == null){
            throw new IllegalArgumentException("The parsed graph MUST NOT be NULL!");
        }
        if(chainName == null || chainName.isEmpty()){
            throw new IllegalArgumentException("The parsed chain name MUST NOT be NULL nor empty!");
        }
        Iterator<Triple> it = graph.filter(null, CHAIN, new PlainLiteralImpl(chainName));
        if(it.hasNext()){
            return it.next().getSubject();
        } else {
            return null;
        }

    }
    /**
     * Getter for the set of ExecutionNodes part of an execution plan.
     * @param ep the execution plan graph
     * @param executionPlanNode the execution plan node
     */
    public static Set<NonLiteral> getExecutionNodes(TripleCollection ep, final NonLiteral executionPlanNode) {
        if(ep == null){
            throw new IllegalArgumentException("The parsed graph with the Executionplan MUST NOT be NULL!");
        }
        if(executionPlanNode == null){
            throw new IllegalArgumentException("The parsed execution plan node MUST NOT be NULL!");
        }
        Set<NonLiteral> executionNodes = new HashSet<NonLiteral>();
        Iterator<Triple> it = ep.filter(executionPlanNode, HAS_EXECUTION_NODE, null);
        while(it.hasNext()){
            Triple t = it.next();
            Resource node = t.getObject();
            if(node instanceof NonLiteral){
                executionNodes.add((NonLiteral)node);
            } else {
                throw new IllegalStateException("The value of the "+HAS_EXECUTION_NODE
                    + " property MUST BE a NonLiteral (triple: "+t+")!");
            }
        }
        return executionNodes;
    }
}

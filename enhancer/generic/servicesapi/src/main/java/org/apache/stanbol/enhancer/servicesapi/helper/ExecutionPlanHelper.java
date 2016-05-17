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
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionPlan.CHAIN;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionPlan.DEPENDS_ON;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionPlan.ENGINE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionPlan.EXECUTION_NODE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionPlan.EXECUTION_PLAN;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionPlan.HAS_EXECUTION_NODE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionPlan.OPTIONAL;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.clerezza.commons.rdf.BlankNode;
import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.rdf.core.NoConvertorException;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.enhancer.servicesapi.ChainException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngineManager;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.impl.EnginesTracker;
import org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionPlan;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExecutionPlanHelper {
    
    private final static Logger log = LoggerFactory.getLogger(ExecutionPlanHelper.class);
    
    private static LiteralFactory lf = LiteralFactory.getInstance();
    
    private ExecutionPlanHelper(){/* Do not allow instances of utility classes*/}

    /**
     * Writes all triples for an ep:ExecutionNode to the parsed {@link Graph}.
     * An {@link BlankNode} is use for representing the execution node resource.
     * @param graph the graph to write the triples. MUST NOT be empty
     * @param epNode the BlankNodeOrIRI representing the ep:ExecutionPlan
     * @param engineName the name of the engine. MUST NOT be <code>null</code> nor empty
     * @param optional if the execution of this node is optional or required
     * @param dependsOn other nodes that MUST BE executed before this one. Parse 
     * <code>null</code> or an empty set if none.
     * @return the resource representing the added ep:ExecutionNode.
     * @deprecated use {@link #writeExecutionNode(Graph, BlankNodeOrIRI, String, boolean, Set, Map)}
     * with <code>null</code> as last parameter
     */
    @Deprecated
    public static BlankNodeOrIRI writeExecutionNode(Graph graph,BlankNodeOrIRI epNode, 
            String engineName, boolean optional, Set<BlankNodeOrIRI> dependsOn){
        return writeExecutionNode(graph,epNode,engineName,optional,dependsOn, null);
    }
    /**
     * Writes all triples for an ep:ExecutionNode to the parsed {@link Graph}.
     * An {@link BlankNode} is use for representing the execution node resource.
     * @param graph the graph to write the triples. MUST NOT be empty
     * @param epNode the BlankNodeOrIRI representing the ep:ExecutionPlan
     * @param engineName the name of the engine. MUST NOT be <code>null</code> nor empty
     * @param optional if the execution of this node is optional or required
     * @param dependsOn other nodes that MUST BE executed before this one. Parse 
     * <code>null</code> or an empty set if none.
     * @param enhProps the EnhancementProperties for this ExecutionNode or
     * <code>null</code> if none
     * @return the resource representing the added ep:ExecutionNode.
     * @since 0.12.1
     */
    public static BlankNodeOrIRI writeExecutionNode(Graph graph,BlankNodeOrIRI epNode, 
            String engineName, boolean optional, Set<BlankNodeOrIRI> dependsOn, 
            Map<String,Object> enhProps){
        if(graph == null){
            throw new IllegalArgumentException("The parsed Graph MUST NOT be NULL!");
        }
        if(engineName == null || engineName.isEmpty()){
            throw new IllegalArgumentException("The parsed Engine name MUST NOT be NULL nor empty!");
        }
        if(epNode == null){
            throw new IllegalArgumentException("The ep:ExecutionPlan instance MUST NOT be NULL!");
        }
        BlankNodeOrIRI node = new BlankNode();
        graph.add(new TripleImpl(epNode, HAS_EXECUTION_NODE, node));
        graph.add(new TripleImpl(node, RDF_TYPE, EXECUTION_NODE));
        graph.add(new TripleImpl(node,ENGINE,new PlainLiteralImpl(engineName)));
        if(dependsOn != null){
            for(BlankNodeOrIRI dependend : dependsOn){
                if(dependend != null){
                    graph.add(new TripleImpl(node, DEPENDS_ON, dependend));
                }
            }
        }
        graph.add(new TripleImpl(node, OPTIONAL, lf.createTypedLiteral(optional)));
        writeEnhancementProperties(graph, node, engineName, enhProps);
        return node;
    }
    /**
     * Creates an ExecutionPlan for the parsed chainName in the parsed ImmutableGraph
     * @param graph the graph
     * @param chainName the chain name
     * @return the node representing the ex:ExecutionPlan
     * @deprecated use {@link #createExecutionPlan(Graph, String, Map)} with
     * parsing <code>null</code> as last parameter
     */
    @Deprecated
    public static BlankNodeOrIRI createExecutionPlan(Graph graph,String chainName){
        return createExecutionPlan(graph, chainName, null);
    }
    
    /**
     * Creates an ExecutionPlan for the parsed chainName in the parsed ImmutableGraph
     * @param graph the graph
     * @param chainName the chain name
     * @param enhProps the map with the enhancement properties defined for the
     * chain or <code>null</code> if none
     * @return the node representing the ex:ExecutionPlan
     * @since 0.12.1
     */
    public static BlankNodeOrIRI createExecutionPlan(Graph graph,String chainName, Map<String,Object> enhProps){
        if(graph == null){
            throw new IllegalArgumentException("The parsed Graph MUST NOT be NULL!");
        }
        if(chainName == null || chainName.isEmpty()){
            throw new IllegalArgumentException("The parsed Chain name MUST NOT be NULL nor empty!");
        }
        BlankNodeOrIRI node = new BlankNode();
        graph.add(new TripleImpl(node, RDF_TYPE, EXECUTION_PLAN));
        graph.add(new TripleImpl(node, CHAIN,new PlainLiteralImpl(chainName)));
        writeEnhancementProperties(graph, node, null, enhProps);
        return node;
    }
    
    /**
     * Evaluates the parsed {@link ImmutableGraph execution plan} and the set of already executed
     * {@link ExecutionPlan#EXECUTION_NODE ep:ExecutionNode}s to find the next
     * nodes that can be executed. 
     * @param executionPlan the execution plan
     * @param executed the already executed {@link ExecutionPlan#EXECUTION_NODE node}s
     * or an empty set to determine the nodes to start the execution.
     * @return the set of nodes that can be executed next or an empty set if
     * there are no more nodes to execute.
     */
    public static Set<BlankNodeOrIRI>getExecutable(Graph executionPlan, Set<BlankNodeOrIRI> executed){
        Set<BlankNodeOrIRI> executeable = new HashSet<BlankNodeOrIRI>();
        for(Iterator<Triple> nodes = executionPlan.filter(null, RDF_TYPE, EXECUTION_NODE);nodes.hasNext();){
            BlankNodeOrIRI node = nodes.next().getSubject();
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
     * EnhancementEngines. NOTE that the parsed list is modified as it is sorted by
     * using the {@link EnhancementEngineHelper#EXECUTION_ORDER_COMPARATOR}.<p>
     * A second parameter with the set of optional engines can be used to define
     * what {@link ExecutionPlan#EXECUTION_NODE} in the execution plan should be 
     * marked as {@link ExecutionPlan#OPTIONAL}.
     * @param chainName the name of the Chain to build the execution plan for
     * @param availableEngines the list of engines
     * @param optional the names of optional engines.
     * @param missing the names of missing engines
     * @return the execution plan
     * @deprecated use {@link #calculateExecutionPlan(String, List, Set, Set, Map)}
     * with <code>null</code> as last argument instead
     */
    @Deprecated
    public static ImmutableGraph calculateExecutionPlan(String chainName, List<EnhancementEngine> availableEngines, 
            Set<String> optional, Set<String> missing) {
        return calculateExecutionPlan(chainName, availableEngines, optional, missing, null);
    }
    /**
     * Creates an execution plan based on the 
     * {@link ServiceProperties#ENHANCEMENT_ENGINE_ORDERING} of the parsed
     * EnhancementEngines. NOTE that the parsed list is modified as it is sorted by
     * using the {@link EnhancementEngineHelper#EXECUTION_ORDER_COMPARATOR}.<p>
     * A second parameter with the set of optional engines can be used to define
     * what {@link ExecutionPlan#EXECUTION_NODE} in the execution plan should be 
     * marked as {@link ExecutionPlan#OPTIONAL}.
     * @param chainName the name of the Chain to build the execution plan for
     * @param availableEngines the list of engines
     * @param optional the names of optional engines.
     * @param missing the names of missing engines
     * @param enhProps chain scoped enhancement properties. The key of the outer
     * map are the name of the engine or <code>null</code> for the chain. The
     * inner map uses the property as key and the value(s) as value. Multiple
     * values can be parsed as {@link Collection}. Single values will be
     * converted to RDF {@link TypedLiteral}s by using the {@link LiteralFactory}.
     * For types not supported by the LiteralFactory the <code>toString()</code>
     * method will be used. <code>null</code> can be parsed if no enhancement
     * properties are present.
     * @return the execution plan
     * @since 0.12.1
     */
    public static ImmutableGraph calculateExecutionPlan(String chainName, List<EnhancementEngine> availableEngines, 
            Set<String> optional, Set<String> missing, Map<String,Map<String,Object>> enhProps) {
        if(chainName == null || chainName.isEmpty()){
            throw new IllegalArgumentException("The parsed ChainName MUST NOT be empty!");
        }
        Collections.sort(availableEngines,EXECUTION_ORDER_COMPARATOR);
        //now we have all required and possible also optional engines
        //  -> build the execution plan
        Graph ep = new IndexedGraph();
        BlankNodeOrIRI epNode = createExecutionPlan(ep, chainName,
            enhProps != null ? enhProps.get(null) : null);
        Integer prevOrder = null;
        Set<BlankNodeOrIRI> prev = null;
        Set<BlankNodeOrIRI> current = new HashSet<BlankNodeOrIRI>();
        for(String name : missing){
            boolean optionalMissing = optional.contains(name);
            BlankNodeOrIRI node = writeExecutionNode(ep, epNode, name, optionalMissing, null,
                enhProps == null ? null : enhProps.get(name));
            if(!optionalMissing){
                current.add(node);
            } // else add missing optional engines without any dependsOn restrictions
        }
        for(EnhancementEngine engine : availableEngines){
            String name = engine.getName();
            Integer order = getEngineOrder(engine);
            if(prevOrder == null || !prevOrder.equals(order)){
                prev = current;
                current = new HashSet<BlankNodeOrIRI>();
                prevOrder = order;
            }
            try {
                BlankNodeOrIRI executionNode = writeExecutionNode(ep, epNode, name, 
                    optional.contains(name), prev, 
                    enhProps == null ? null : enhProps.get(name));
                current.add(executionNode);
            } catch (RuntimeException e){
                //add the engine and class to ease debugging in such cases
                log.error("Exception while writing ExecutionNode for Enhancement Eninge: "
                    + engine +"(class: "+engine.getClass()+")",e);
                throw e; //rethrow it
            }
        }
        return ep.getImmutableGraph();
    }
    /**
     * Writes the enhancementProperties for an engine/chain to the parsed 
     * ExecutionNode
     * @param ep The RDF graph holding the execution plan
     * @param node the execution node of the engine (or chain) to add the
     * enhancement properties
     * @param engineName the name of the engine or <code>null</code> in case
     * of the chain
     * @param enhProps the chain scoped enhancement properties or <code>null</code>
     * if none
     * @since 0.12.1
     */
    private static void writeEnhancementProperties(Graph ep, BlankNodeOrIRI node, String engineName,
            Map<String,Object> enhProps) {
        if(enhProps == null){ //no enhancement properties for this engine
            return;
        }
        for(Entry<String,Object> enhprop : enhProps.entrySet()){
            if(enhprop.getKey() == null || enhprop.getValue() == null){
                log.warn("Invalid Enhancement Property {} for {} {}", new Object[]{
                        enhprop, engineName == null ? "Chain" : "engine",
                        engineName == null ? "" : engineName});
            } else {
                writeEnhancementProperty(ep, node,
                    new IRI(NamespaceEnum.ehp + enhprop.getKey()),
                    enhprop.getValue());
            }
        }
    }
    
    /**
     * Writes enhancement property value(s) for the parsed node, property to the
     * execution plan graph.
     * @param ep the RDF graph holding the execution plan
     * @param epNode the execution node
     * @param property the property
     * @param value the value(s). {@link Collection} and <code>Object[]</code> are
     * supported for multiple values.
     * @throws NullPointerException if any of the parsed parameter is <code>null</code>
     */
    @SuppressWarnings("unchecked")
    private static void writeEnhancementProperty(Graph ep, BlankNodeOrIRI epNode, 
            IRI property, Object value) {
        Collection<Object> values;
        if(value instanceof Collection<?>){
            values = (Collection<Object>)value;
        } else if(value instanceof Object[]){
            values = Arrays.asList((Object[])value);
        } else {
            values = Collections.singleton(value);
        }
        for(Object v : values){
            if(v != null){
                Literal literal;
                if(v instanceof String){
                    literal = new PlainLiteralImpl((String)v);
                } else {
                    try {
                        literal = lf.createTypedLiteral(v);
                    } catch (NoConvertorException e){
                        log.warn("Use toString() value '{}' for EnhancementProperty "
                            + "'{}' as no TypedLiteral converter is registered for "
                            + "class {}", new Object[]{ v, property, v.getClass().getName()});
                        literal = new PlainLiteralImpl(v.toString());
                    }
                }
                ep.add(new TripleImpl(epNode, property, literal));
            }
        }
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
     * to parse a {@link ImmutableGraph} object.<p>
     * TODO: There is no check for cycles implemented yet.
     * @param the graph to check
     * @return the engine names referenced by the validated execution plan-
     * @throws ChainException
     */
    public static Set<String> validateExecutionPlan(Graph executionPlan) throws ChainException {
        Iterator<Triple> executionNodeIt = executionPlan.filter(null, RDF_TYPE, EXECUTION_NODE);
        Set<String> engineNames = new HashSet<String>();
        Map<BlankNodeOrIRI, Collection<BlankNodeOrIRI>> nodeDependencies = new HashMap<BlankNodeOrIRI,Collection<BlankNodeOrIRI>>();
        //1. check the ExecutionNodes
        while(executionNodeIt.hasNext()){
            BlankNodeOrIRI node = executionNodeIt.next().getSubject();
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
            Collection<BlankNodeOrIRI> dependsOn = new HashSet<BlankNodeOrIRI>();
            for(Iterator<Triple> t = executionPlan.filter(node, DEPENDS_ON, null);t.hasNext();){
                RDFTerm o = t.next().getObject();
                if(o instanceof BlankNodeOrIRI){
                    dependsOn.add((BlankNodeOrIRI)o);
                } else {
                    throw new ChainException("Execution Node "+node+" defines the literal '" +
                        o+"' as value for the "+DEPENDS_ON +" property. However this" +
                        "property requires values to be bNodes or URIs.");
                }
            }
            nodeDependencies.put(node, dependsOn);
        }
        //2. now check the dependency graph
        for(Entry<BlankNodeOrIRI,Collection<BlankNodeOrIRI>> entry : nodeDependencies.entrySet()){
            if(entry.getValue() != null){
                for(BlankNodeOrIRI dependent : entry.getValue()){
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
    
    public static Set<BlankNodeOrIRI> getDependend(Graph executionPlan, BlankNodeOrIRI executionNode){
        Set<BlankNodeOrIRI> dependend = new HashSet<BlankNodeOrIRI>();
        addDependend(dependend, executionPlan, executionNode);
        return dependend;
    }
    public static void addDependend(Collection<BlankNodeOrIRI> collection, Graph executionPlan, BlankNodeOrIRI executionNode){
        for(Iterator<Triple> it = executionPlan.filter(executionNode, DEPENDS_ON, null);
                it.hasNext();collection.add((BlankNodeOrIRI)it.next().getObject()));
    }
    public static boolean isOptional(Graph executionPlan, BlankNodeOrIRI executionNode) {
        Boolean optional = get(executionPlan,executionNode,OPTIONAL,Boolean.class,lf);
        return optional == null ? false : optional.booleanValue();
    }
    public static String getEngine(Graph executionPlan, BlankNodeOrIRI executionNode) {
        return getString(executionPlan, executionNode, ENGINE);
    }

    /**
     * Calculates a sorted list of active EnhancementEngines form the given
     * ExecutinPlan
     * @param engineManager The engine manager (OSGI service or {@link EnginesTracker})
     * @param ep the execution plan
     * @return
     */
    public static List<EnhancementEngine> getActiveEngines(EnhancementEngineManager engineManager, Graph ep) {
        List<EnhancementEngine> engines = new ArrayList<EnhancementEngine>();
        Set<BlankNodeOrIRI> visited = new HashSet<BlankNodeOrIRI>();
        Set<BlankNodeOrIRI> executeable;
        do {
            executeable = getExecutable(ep, visited);
            for(BlankNodeOrIRI node : executeable){
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
    public static BlankNodeOrIRI getExecutionPlan(Graph graph, String chainName){
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
    public static Set<BlankNodeOrIRI> getExecutionNodes(Graph ep, final BlankNodeOrIRI executionPlanNode) {
        if(ep == null){
            throw new IllegalArgumentException("The parsed graph with the Executionplan MUST NOT be NULL!");
        }
        if(executionPlanNode == null){
            throw new IllegalArgumentException("The parsed execution plan node MUST NOT be NULL!");
        }
        Set<BlankNodeOrIRI> executionNodes = new HashSet<BlankNodeOrIRI>();
        Iterator<Triple> it = ep.filter(executionPlanNode, HAS_EXECUTION_NODE, null);
        while(it.hasNext()){
            Triple t = it.next();
            RDFTerm node = t.getObject();
            if(node instanceof BlankNodeOrIRI){
                executionNodes.add((BlankNodeOrIRI)node);
            } else {
                throw new IllegalStateException("The value of the "+HAS_EXECUTION_NODE
                    + " property MUST BE a BlankNodeOrIRI (triple: "+t+")!");
            }
        }
        return executionNodes;
    }
}

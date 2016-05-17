/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.stanbol.enhancer.jersey.utils;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Enhancer.ENHANCEMENT_ENGINE;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ChainManager;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngineManager;
import org.apache.stanbol.enhancer.servicesapi.rdf.Enhancer;
import org.osgi.framework.ServiceReference;

public final class EnhancerUtils {

    private EnhancerUtils(){};
    
    
    /**
     * Uses the parsed {@link EnhancementEngineManager} to build a Map
     * representing the current snapshot of the active enhancement engines.
     * 
     * @param engineManager The engine manager used to build the snapshot
     * @return the map with the names as key and an Entry with the {@link ServiceReference}
     * and the {@link EnhancementEngine} instance as value.
     */
    public static Map<String, Entry<ServiceReference,EnhancementEngine>> buildEnginesMap(EnhancementEngineManager engineManager) {
        Map<String, Entry<ServiceReference,EnhancementEngine>> engines = new HashMap<String,Map.Entry<ServiceReference,EnhancementEngine>>();
        for(String engineName : engineManager.getActiveEngineNames()){
            ServiceReference engineRef = engineManager.getReference(engineName);
            if(engineRef != null){
                EnhancementEngine engine = engineManager.getEngine(engineRef);
                if(engine != null){
                    Map<ServiceReference,EnhancementEngine> m = Collections.singletonMap(engineRef, engine);
                    engines.put(engineName, m.entrySet().iterator().next());
                }
            }
        }
        return engines;
    }
    /**
     * Uses the parsed {@link ChainManager} to build a Map
     * representing the current snapshot of the active enhancement chains.
     * 
     * @param chainManager The chain manager used to build the snapshot
     * @return the map with the names as key and an Entry with the {@link ServiceReference}
     * and the {@link Chain} instance as value.
     */
    public static Map<String,Map.Entry<ServiceReference,Chain>> buildChainsMap(ChainManager chainManager) {
        Map<String,Map.Entry<ServiceReference,Chain>> chains = new HashMap<String,Map.Entry<ServiceReference,Chain>>();
        for(String chainName : chainManager.getActiveChainNames()){
            ServiceReference chainRef = chainManager.getReference(chainName);
            if(chainRef != null){
                Chain chain = chainManager.getChain(chainRef);
                if(chain != null){
                    Map<ServiceReference,Chain> m = Collections.singletonMap(chainRef, chain);
                    chains.put(chainName, m.entrySet().iterator().next());
                }
            }
        }
        return chains;
    }
    /**
     * Create the RDF data for the currently active EnhancementEngines.<p>
     * Note the the parsed rootUrl MUST already consider offsets configured
     * for the Stanbol RESTful service. When called from within a
     * {@link BaseStanbolResource} the following code segment should be used:<p>
     * <code><pre>
     *     String rootUrl = uriInfo.getBaseUriBuilder().path(getRootUrl()).build().toString();
     * </pre></code>
     * @param engineManager the enhancement engine manager
     * @param graph the RDF graph to add the triples
     * @param rootUrl the root URL used by the current request
     */
    public static void addActiveEngines(EnhancementEngineManager engineManager,Graph graph, String rootUrl) {
        addActiveEngines(buildEnginesMap(engineManager).values(), graph, rootUrl);
    }
    /**
     * Create the RDF data for the currently active EnhancementEngines.<p>
     * Note the the parsed rootUrl MUST already consider offsets configured
     * for the Stanbol RESTful service. When called from within a
     * {@link BaseStanbolResource} the following code segment should be used:<p>
     * <code><pre>
     *     String rootUrl = uriInfo.getBaseUriBuilder().path(getRootUrl()).build().toString();
     * </pre></code>
     * @param activeEngines the active enhancement engines as {@link Entry entries}.
     * @param graph the RDF graph to add the triples
     * @param rootUrl the root URL used by the current request
     * @see EnhancerUtils#buildEnginesMap(EnhancementEngineManager)
     */
    public static void addActiveEngines(Iterable<Entry<ServiceReference,EnhancementEngine>> activeEngines,Graph graph, String rootUrl) {
        IRI enhancerResource = new IRI(rootUrl+"enhancer");
        graph.add(new TripleImpl(enhancerResource, RDF.type, Enhancer.ENHANCER));
        for(Entry<ServiceReference,EnhancementEngine> entry : activeEngines){
            IRI engineResource = new IRI(rootUrl+"enhancer/engine/"+entry.getValue().getName());
            graph.add(new TripleImpl(enhancerResource, Enhancer.HAS_ENGINE, engineResource));
            graph.add(new TripleImpl(engineResource, RDF.type, ENHANCEMENT_ENGINE));
            graph.add(new TripleImpl(engineResource, RDFS.label, new PlainLiteralImpl(entry.getValue().getName())));
        }
    }
    
    /**
     * Create the RDF data for the currently active Enhancement {@link Chain}s.<p>
     * Note the the parsed rootUrl MUST already consider offsets configured
     * for the Stanbol RESTful service. When called from within a
     * {@link BaseStanbolResource} the following code segment should be used:<p>
     * <code><pre>
     *     String rootUrl = uriInfo.getBaseUriBuilder().path(getRootUrl()).build().toString();
     * </pre></code>
     * @param chainManager the enhancement chain manager.
     * @param graph the RDF graph to add the triples
     * @param rootUrl the root URL used by the current request
     */
    public static void addActiveChains(ChainManager chainManager, Graph graph, String rootUrl) {
        addActiveChains(buildChainsMap(chainManager).values(), chainManager.getDefault(), graph, rootUrl);
    }
    /**
     * Create the RDF data for the currently active Enhancement {@link Chain}s.<p>
     * Note the the parsed rootUrl MUST already consider offsets configured
     * for the Stanbol RESTful service. When called from within a
     * {@link BaseStanbolResource} the following code segment should be used:<p>
     * <code><pre>
     *     String rootUrl = uriInfo.getBaseUriBuilder().path(getRootUrl()).build().toString();
     * </pre></code>
     * @param activeChains the active enhancement chains as {@link Entry entries}.
     * @param defaultChain the default chain
     * @param graph the RDF graph to add the triples
     * @param rootUrl the root URL used by the current request
     */
    public static void addActiveChains(Iterable<Entry<ServiceReference,Chain>> activeChains, Chain defaultChain, Graph graph, String rootUrl) {
        IRI enhancer = new IRI(rootUrl+"enhancer");
        graph.add(new TripleImpl(enhancer, RDF.type, Enhancer.ENHANCER));
        for(Entry<ServiceReference,Chain> entry : activeChains){
            IRI chainResource = new IRI(rootUrl+"enhancer/chain/"+entry.getValue().getName());
            graph.add(new TripleImpl(enhancer, Enhancer.HAS_CHAIN, chainResource));
            if(entry.getValue().equals(defaultChain)){
                graph.add(new TripleImpl(enhancer, Enhancer.HAS_DEFAULT_CHAIN, chainResource));
            }
            graph.add(new TripleImpl(chainResource, RDF.type, Enhancer.ENHANCEMENT_CHAIN));
            graph.add(new TripleImpl(chainResource, RDFS.label, new PlainLiteralImpl(entry.getValue().getName())));
        }
    }
}

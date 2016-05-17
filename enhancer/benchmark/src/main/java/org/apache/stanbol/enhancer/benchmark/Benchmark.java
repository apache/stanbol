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
package org.apache.stanbol.enhancer.benchmark;

import java.util.List;

import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EnhancementException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;

/** A Benchmark is a named List of {@link TripleMatcherGroup} */
public interface Benchmark extends List<TripleMatcherGroup >{
    /** Benchmark name */
    String getName();
    
    /** Benchmark input text */
    String getInputText();
    
    /** The enhancement chain used to execute the test */
    Chain getChain();
    
    /** Execute the benchmark and return results 
     *  @return null */
    List<BenchmarkResult> execute(EnhancementJobManager jobManager, 
                                  ContentItemFactory ciFactory) throws EnhancementException;
    
    /** Return the enhanced ImmutableGraph of our input text */
    ImmutableGraph getGraph(EnhancementJobManager jobManager, 
                   ContentItemFactory ciFactory) throws EnhancementException;

    /**
     * Setter for the chain
     * @param chain the chain
     */
    void setChain(Chain chain);
}

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
package org.apache.stanbol.enhancer.benchmark.impl;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.stanbol.enhancer.benchmark.Benchmark;
import org.apache.stanbol.enhancer.benchmark.BenchmarkResult;
import org.apache.stanbol.enhancer.benchmark.TripleMatcherGroup;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EnhancementException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;

@SuppressWarnings("serial")
public class BenchmarkImpl extends LinkedList<TripleMatcherGroup> implements Benchmark {
    
    private String name;
    private String inputText;
    private ImmutableGraph graph;
    private ContentItemFactory ciFactory;
    private Chain chain;
    
    /** Not public: meant to be constructed by parsing */
    BenchmarkImpl() {
    }
    
    void setName(String name) {
        this.name = name;
    }

    /** @inheritDoc */
    @Override
    public String getName() {
        return name;
    }

    void setInputText(String inputText) {
        this.inputText = inputText;
    }
    
    /** @inheritDoc */
    @Override
    public String getInputText() {
        return inputText;
    }

    /** @inheritDoc */
    @Override
    public Chain getChain(){
        return chain;
    }
    
    /** @inheritDoc */
    @Override
    public void setChain(Chain chain){
        this.chain = chain;
    }
    
    @Override
    public List<BenchmarkResult> execute(EnhancementJobManager jobManager, ContentItemFactory ciFactory) throws EnhancementException {
        if(isEmpty()) {
            return null;
        }
        if(inputText == null || inputText.length() == 0) {
            throw new IllegalStateException("inputText is null or empty, cannot run benchmark");
        }
      
        final List<BenchmarkResult> result = new LinkedList<BenchmarkResult>();
        for(TripleMatcherGroup g :  this) {
            result.add(new BenchmarkResultImpl(g, getGraph(jobManager,ciFactory)));
        }

        return result;
    }
    
    /** @inheritDoc */
    public ImmutableGraph getGraph(EnhancementJobManager jobManager, 
                          ContentItemFactory ciFactory) throws EnhancementException {
        if(graph == null) {
            ContentItem ci;
            try {
                ci = ciFactory.createContentItem(new StringSource(inputText));
            } catch (IOException e) {
                throw new IllegalStateException("Unable to create a ContentItem" +
                		"using '"+ciFactory.getClass().getSimpleName()+"'!",e);
            }
            if(chain == null){
                jobManager.enhanceContent(ci);
            } else { //parsing null as chain does not work!
                jobManager.enhanceContent(ci,chain);
            }
            graph = ci.getMetadata().getImmutableGraph();
        }
        return graph;
    }
}

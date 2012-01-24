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

import java.util.LinkedList;
import java.util.List;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.stanbol.enhancer.benchmark.Benchmark;
import org.apache.stanbol.enhancer.benchmark.BenchmarkResult;
import org.apache.stanbol.enhancer.benchmark.TripleMatcherGroup;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.helper.InMemoryContentItem;

@SuppressWarnings("serial")
public class BenchmarkImpl extends LinkedList<TripleMatcherGroup> implements Benchmark {
    
    private String name;
    private String inputText;
    private Graph graph;
    
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

    @Override
    public List<BenchmarkResult> execute(EnhancementJobManager jobManager) throws EnhancementException {
        if(isEmpty()) {
            return null;
        }
        if(inputText == null || inputText.length() == 0) {
            throw new IllegalStateException("inputText is null or empty, cannot run benchmark");
        }
        
        final List<BenchmarkResult> result = new LinkedList<BenchmarkResult>();
        for(TripleMatcherGroup g :  this) {
            result.add(new BenchmarkResultImpl(g, getGraph(jobManager)));
        }

        return result;
    }
    
    /** @inheritDoc */
    public Graph getGraph(EnhancementJobManager jobManager) throws EnhancementException {
        if(graph == null) {
            final ContentItem ci = new InMemoryContentItem(inputText.getBytes(), "text/plain");
            jobManager.enhanceContent(ci);
            graph = ci.getMetadata().getGraph();
        }
        return graph;
    }
}

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

import java.util.Set;

import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.enhancer.benchmark.BenchmarkResult;
import org.apache.stanbol.enhancer.benchmark.TripleMatcherGroup;

public class BenchmarkResultImpl implements BenchmarkResult {
    
    private final TripleMatcherGroup tmg;
    private final boolean successful;
    private String info;
    private final Set<IRI> matchingSubjects;
    
    BenchmarkResultImpl(TripleMatcherGroup tmg, ImmutableGraph graph) {
        this.tmg = tmg;
        matchingSubjects = tmg.getMatchingSubjects(graph);
        
        if(tmg.isExpectGroup()) {
            if(matchingSubjects.size() > 0) {
                successful = true;
                info = "EXPECT OK";
            } else {
                successful = false;
                info = "EXPECT FAILED";
            }
        } else {
            if(matchingSubjects.size() > 0) {
                successful = false;
                info = "COMPLAINT TRIGGERED";
            } else {
                successful = true;
                info = "NO COMPLAINT";
            }
        } 
        info += ", matchingSubjects=" + matchingSubjects.size();
    }
    
    @Override
    public String toString() {
        return BenchmarkResult.class.getSimpleName()
            + " "
            + (successful ? "SUCCESSFUL" : "**FAILED**")
            + " ("
            + info
            + "): "
            + tmg.getDescription()
            ;
    }
    
    @Override
    public TripleMatcherGroup getTripleMatcherGroup() {
        return tmg;
    }

    @Override
    public boolean successful() {
        return successful;
    }

    @Override
    public String getInfo() {
        return info;
    }
    
    @Override
    public Set<IRI> getMatchingSubjects() {
        return matchingSubjects;
    }
}

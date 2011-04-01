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

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.List;
import java.util.Set;

import javax.management.RuntimeErrorException;
import javax.servlet.ServletException;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.benchmark.Benchmark;
import org.apache.stanbol.enhancer.benchmark.BenchmarkEngine;
import org.apache.stanbol.enhancer.benchmark.BenchmarkParser;
import org.apache.stanbol.enhancer.benchmark.TripleMatcherGroup;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.helper.InMemoryContentItem;

@Component(immediate=false)
@Service
public class BenchmarkEngineImpl implements BenchmarkEngine {

    public static final String ENCODING = "UTF-8";
    
    @Reference
    private EnhancementJobManager jobManager;
    
    @Reference 
    private BenchmarkParser parser;

    @Override
    public void runBenchmark(String benchmarkText, Writer output) throws Exception {
        final PrintWriter pw = new PrintWriter(output);
        
        // TODO better formatting
        pw.println("<pre>");
        
        try {
            final List<? extends Benchmark> benchmarks = 
                parser.parse(new StringReader(benchmarkText));
            
            if(benchmarks.isEmpty()) {
                throw new ServletException(
                        "No valid benchmarks found in input [" 
                        + benchmarkText + "]"); 
            }
            
            for(Benchmark b : benchmarks) {
                // TODO move this to benchmark class
                // TODO Benchmark should validate itself
                final ContentItem ci = new InMemoryContentItem(b.getInputText().getBytes(), "text/plain");
                jobManager.enhanceContent(ci);
                pw.println("BENCHMARK:" + b.getName());
                for(TripleMatcherGroup g :  b) {
                    final Set<UriRef> s = g.getMatchingSubjects(ci.getMetadata().getGraph());
                    if(g.isExpectGroup()) {
                        if(s.size() == 1) {
                            pw.print("EXPECT OK: " + g.getDescription());
                        } else {
                            pw.print("**ERROR** EXPECT FAILED: " + g.getDescription());
                        }
                    } else {
                        if(s.size() > 0) {
                            pw.print("**ERROR** COMPLAIN: " + s.size() + " matches for group " + g.getDescription()); 
                        } else {
                            pw.print("NO COMPLAINTS: " + g.getDescription()); 
                        }
                    } 
                    pw.println(" (" + s.size() + " matches)");
                }
                pw.println("END OF BENCHMARK:" + b.getName());
                pw.flush();
            }

        } finally {
            pw.println("</pre>");
            pw.flush();
        }
    }
}

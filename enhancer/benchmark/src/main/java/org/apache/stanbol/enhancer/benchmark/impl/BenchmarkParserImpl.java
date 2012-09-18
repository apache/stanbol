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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.benchmark.Benchmark;
import org.apache.stanbol.enhancer.benchmark.BenchmarkParser;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ChainManager;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;

/** BenchmarkParser implementation */
@Component
@Service
public class BenchmarkParserImpl implements BenchmarkParser {

    
    /**
     * Needed to lookup active enhancement changes as parsed by {@link ChainState}
     */
    @Reference
    private ChainManager chainManager;
        
    private static class ParserContext {
        List<BenchmarkImpl> benchmarks;
        BenchmarkImpl currentBenchmark;
        TripleMatcherGroupImpl currentGroup;
    }
    
    private abstract class State {
        protected final ParserContext ctx;
        
        State(ParserContext ctx) {
            this.ctx = ctx;
        }
        
        State read(String line) throws IOException {
            if(line != null) {
                line = line.trim();
                if(line.length() > 0 && !line.startsWith(COMMENT_MARKER)) {
                    // Switch states on marker lines, or let current state
                    // parse current line
                    if(INPUT_SECTION_MARKER.equals(line)) {
                        return new InputState(ctx);
                    } else if(EXPECT_SECTION_MARKER.equals(line)) {
                        return new MatcherGroupState(ctx, true);
                    } else if(COMPLAIN_SECTION_MARKER.equals(line)) {
                        return new MatcherGroupState(ctx, false);
                    } else if (ENHANCEMENT_CHAIN.equals(line)) {
                        return new ChainState(ctx);
                    } else {
                        return consume(line);
                    }
                }
            }
            return this;
        }
        
        /** Get field value if current line is in the form "KEY: value here",
         *  null if not.
         */
        protected String getField(String line, String fieldName) {
            String value = null;
            if(line.startsWith(fieldName + FIELD_SEPARATOR)) {
                value = line.substring(line.indexOf(FIELD_SEPARATOR) + 1).trim();
            }
            return value;
        }
        
        /** Consume supplied line and return the next state 
         *  (which might this one if no state change is needed) 
         */
        protected abstract State consume(String line) throws IOException;
    }

    private class InitState extends State {
        InitState(ParserContext ctx) {
            super(ctx);
        }
        
        protected State consume(String line) {
            return this;
        }
    }

    private class InputState extends State {
        InputState(ParserContext ctx) {
            super(ctx);
            ctx.currentBenchmark = new BenchmarkImpl();
            ctx.benchmarks.add(ctx.currentBenchmark);
        }
        
        protected State consume(String line) {
            // Add all lines to the benchmark's input text,
            // separated by one space
            final String cur = ctx.currentBenchmark.getInputText();
            if(cur == null) {
                ctx.currentBenchmark.setInputText(line);
            } else {
                ctx.currentBenchmark.setInputText(cur + " " + line);
            }
            return this;
        }
    }
    /* not a static class because its needs the #chainManager !*/
    private class ChainState extends State {
        ChainState(ParserContext ctx) {
            super(ctx);
        }
        
        @Override
        protected State consume(String line) throws IOException {
            if(ctx.currentBenchmark.getChain() == null){
                Chain chain = chainManager.getChain(line);
                if(chain != null){
                    ctx.currentBenchmark.setChain(chain);
                } //defined chain not active
            } //do not override
            return this;
        }
        
    }
    private class MatcherGroupState extends State {
        private final boolean isExpect;
        
        MatcherGroupState(ParserContext ctx, boolean isExpect) {
            super(ctx);
            this.isExpect = isExpect;
        }
        
        protected State consume(String line) throws IOException {
            // Description field starts a new group
            final String desc = getField(line, DESCRIPTION_FIELD);
            if(desc != null) {
                ctx.currentGroup = new TripleMatcherGroupImpl(isExpect, desc);
                ctx.currentBenchmark.add(ctx.currentGroup);
            } else {
                // Each line is a new matcher
                ctx.currentGroup.addMatcher(new TripleMatcherImpl(line));
            }
            return this;
        }
    }
    
    @Override
    public List<? extends Benchmark> parse(Reader r) throws IOException {
        final BufferedReader br = new BufferedReader(r);
        final ParserContext ctx = new ParserContext();
        ctx.benchmarks = new ArrayList<BenchmarkImpl>();
        State state = new InitState(ctx);
        
        try {
            String line = null;
            while( (line = br.readLine()) != null) {
                state = state.read(line);
            }
        } finally {
            br.close();
        }
        
        return ctx.benchmarks;
    }
}

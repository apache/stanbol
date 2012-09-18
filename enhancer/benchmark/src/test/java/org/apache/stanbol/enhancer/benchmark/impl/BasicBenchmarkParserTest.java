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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;

import org.apache.stanbol.enhancer.benchmark.Benchmark;
import org.apache.stanbol.enhancer.benchmark.TripleMatcherGroup;
import org.junit.Before;
import org.junit.Test;

public class BasicBenchmarkParserTest {

    public static final String RESOURCE_ENCODING = "UTF-8";
    private List<? extends Benchmark> benchmarks;
    
    @Before
    public void parse() throws IOException {
        benchmarks = new BenchmarkParserImpl().parse(getTestBenchmark("/benchmarks/benchmark1.txt"));
    }
    
    private Reader getTestBenchmark(String path) {
        final InputStream is = BasicBenchmarkParserTest.class.getResourceAsStream(path);
        if(is == null) {
            throw new IllegalArgumentException("Cannot read resource [" + path + "]");
        }
        
        try {
            return new InputStreamReader(is, RESOURCE_ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new Error("Unsupported encoding?? " + RESOURCE_ENCODING, e);
        }
    }
    
    @Test
    public void testStructureAndDescriptions() throws Exception {
        assertEquals("Number of benchmarks", 2, benchmarks.size());
        
        final Iterator<? extends Benchmark> bit = benchmarks.iterator();
        {
            final Benchmark b = bit.next();
            assertEquals("Bob Marley was born in Kingston, Jamaica.", b.getInputText()); 
            assertEquals("First benchmark group count", 2, b.size());
            final Iterator<TripleMatcherGroup> git = b.iterator();
            assertEquals("Kingston must be found", git.next().getDescription());
            assertEquals("Bob Marley must be found as a musical artist", git.next().getDescription());
            assertFalse(git.hasNext());
        }
        
        {
            final Benchmark b = bit.next();
            assertEquals("Paris Hilton might live in Paris, but she prefers New York.", b.getInputText()); 
            assertEquals("Second benchmark group count", 5, b.size());
            final Iterator<TripleMatcherGroup> git = b.iterator();
            for(int i=1; i <= 5; i++) {
                assertEquals("Second benchmark group " + i, git.next().getDescription());
            }
            assertFalse(git.hasNext());
        }
    }

    @Test
    public void testExpectComplain() throws Exception {
        assertEquals("Number of benchmarks", 2, benchmarks.size());
        
        final Iterator<? extends Benchmark> bit = benchmarks.iterator();
        {
            // First benchmark has two expect groups and no complains
            final Iterator<TripleMatcherGroup> git = bit.next().iterator();
            assertTrue(git.next().isExpectGroup());
            assertTrue(git.next().isExpectGroup());
            assertFalse(git.hasNext());
        }
        {
            // Second benchmark has 3 expect groups and 2 complains
            final Iterator<TripleMatcherGroup> git = bit.next().iterator();
            assertTrue(git.next().isExpectGroup());
            assertTrue(git.next().isExpectGroup());
            assertTrue(git.next().isExpectGroup());
            assertFalse(git.next().isExpectGroup());
            assertFalse(git.next().isExpectGroup());
            assertFalse(git.hasNext());
        }
    }
    
    @Test
    public void testMatcherCount() throws Exception {
        assertEquals("Number of benchmarks", 2, benchmarks.size());
        
        final Iterator<? extends Benchmark> bit = benchmarks.iterator();
        
        // Number of matchers in each group of each benchmark
        final int [][] matchersCount = {
                { 2, 3 },
                { 1, 2, 3, 2, 1 }
        };
        
        for(int [] counts : matchersCount) {
            final Iterator<TripleMatcherGroup> git = bit.next().iterator();
            for(int count : counts) {
                assertTrue("Iterator has more data at count=" + count, git.hasNext());
                final TripleMatcherGroup g = git.next();
                assertEquals("Matchers count matches for " + g, count, g.getMatchers().size());
            }
        }
    }
}

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

import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TripleMatcherGroupImplTest {
    private Graph graph;
    
    @Before
    public void createGraph() {
        graph = new SimpleGraph();
        graph.add(TripleUtil.uriTriple("S1", "P1", "01"));
        graph.add(TripleUtil.uriTriple("S1", "P1", "02"));
        graph.add(TripleUtil.uriTriple("S2", "P1", "01"));
        graph.add(TripleUtil.uriTriple("S2", "P1", "02"));
        graph.add(TripleUtil.uriTriple("S3", "P1", "01"));
        graph.add(TripleUtil.uriTriple("S4", "P1", "02"));
    }
    
    @Test
    public void basicTest() throws Exception {
        final TripleMatcherGroupImpl group = new TripleMatcherGroupImpl(true, "no description");
        
        assertEquals(
                "Empty matcher group should find nothing",
                0,
                group.getMatchingSubjects(graph.getImmutableGraph()).size());
        
        // Add two matchers, only S1 and S2 match all of them
        group.addMatcher(new TripleMatcherImpl("P1 URI 01"));
        group.addMatcher(new TripleMatcherImpl("P1 URI 02"));
        
        final Set<IRI> actual = group.getMatchingSubjects(graph.getImmutableGraph());
        final Set<IRI> expected = TripleUtil.uriRefSet("S1", "S2");
        
        assertEquals("Size of results " + actual + " matches " + expected, expected.size(), actual.size());
        assertTrue("Content of results " + actual + " matches " + expected, expected.containsAll(actual));
    }
}

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.enhancer.benchmark.TripleMatcher;
import org.apache.stanbol.enhancer.benchmark.TripleMatcherGroup;

public class TripleMatcherGroupImpl implements TripleMatcherGroup {

    private List<TripleMatcher> matchers = new ArrayList<TripleMatcher>();
    private boolean isExpect;
    private String description;
    
    TripleMatcherGroupImpl(boolean isExpect, String description) {
        this.isExpect = isExpect;
        this.description = description;
    }
     
    void addMatcher(TripleMatcher m) {
        matchers.add(m);
    }

    @Override
    public String toString() {
         return getClass().getSimpleName() + " (" + description + ")";
    }

    @Override
    public boolean isExpectGroup() {
        return isExpect;
    }

    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public Set<UriRef> getMatchingSubjects(Graph g) {
        if(matchers.isEmpty()) {
            return new HashSet<UriRef>();
        }

        // For all matchers, find the set of subjects that match
        // and compute the intersection of those sets
        Set<UriRef> intersection = null;
        for(TripleMatcher m : matchers) {
            final Set<UriRef> s = new HashSet<UriRef>();
            final Iterator<Triple> it = g.iterator();
            while(it.hasNext()) {
                final Triple t = it.next();
                if(m.matches(t)) {
                    final NonLiteral n = t.getSubject();
                    if(n instanceof UriRef) {
                        s.add((UriRef)n);
                    } else {
                        // TODO do we need to handle non-UriRef subjects?
                    }
                }
            }
            
            if(intersection == null) {
                intersection = s;
            } else {
                intersection.retainAll(s);
            }
        }
        
        return intersection;
    }

    @Override
    public Collection<TripleMatcher> getMatchers() {
        return matchers;
    }
}
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

import java.util.HashSet;
import java.util.Set;

import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;

public class TripleUtil {
    static Triple uriTriple(String subject, String predicate, String object) {
        return new TripleImpl(new IRI(subject), new IRI(predicate), new IRI(object));
    }
    
    static Set<IRI> uriRefSet(String...uri) {
        final Set<IRI> result = new HashSet<IRI>();
        for(String str : uri) {
            result.add(new IRI(str));
        }
        return result;
    }
}

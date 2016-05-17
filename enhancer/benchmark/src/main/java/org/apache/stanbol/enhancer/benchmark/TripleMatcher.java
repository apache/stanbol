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

import org.apache.clerezza.commons.rdf.Triple;

/** TripleMatcher is used to count how many Triples
 *  match a given statement in the benchmark tool.
 *  
 *  TripleMatchers are usually parsed from expressions like
 *  <pre>
 *  http://somePredicate URI http://someURI
 *  </pre>
 *  or
 *  <pre>
 *  http://somePredicate REGEXP someRegularExpression
 *  </pre>
 *  
 *  Which look for triples which have the specified predicate
 *  and an Object that matches the rest of the expression using
 *  the specified matching operator (URI, REGEXP etc).
 *  
 *  The parsing is not part of this interface, it's an implementation
 *  concern.
 */
public interface TripleMatcher {
    /** True if this matches suppplied Triple */
    boolean matches(Triple t);
    
    /** Get the expression used to build this matcher */
    String getExpression();
}

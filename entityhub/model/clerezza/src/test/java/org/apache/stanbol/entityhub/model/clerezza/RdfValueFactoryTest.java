/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stanbol.entityhub.model.clerezza;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.test.model.ValueFactoryTest;
import org.junit.Before;
import org.junit.Test;


public class RdfValueFactoryTest extends ValueFactoryTest {
    
    protected RdfValueFactory valueFactory;
    
    @Before
    public void init(){
        this.valueFactory = RdfValueFactory.getInstance();
    }
    
    @Override
    protected Object getUnsupportedReferenceType() {
        return null; //all references are supported (no test for valid IRIs are done by Clerezza)
    }
    
    @Override
    protected Object getUnsupportedTextType() {
        return null; //all Types are supported
    }
    
    @Override
    protected ValueFactory getValueFactory() {
        return valueFactory;
    }
    @Test(expected=IllegalArgumentException.class)
    public void testNullNodeRepresentation() {
        Graph graph = new IndexedGraph();
        valueFactory.createRdfRepresentation(null, graph);
    }
    @Test(expected=IllegalArgumentException.class)
    public void testNullGraphRepresentation() {
        IRI rootNode = new IRI("urn:test.rootNode");
        valueFactory.createRdfRepresentation(rootNode, null);
    }
    
}

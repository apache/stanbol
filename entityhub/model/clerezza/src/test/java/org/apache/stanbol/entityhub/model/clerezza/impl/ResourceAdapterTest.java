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
package org.apache.stanbol.entityhub.model.clerezza.impl;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.junit.Assert;
import org.junit.Test;

public class ResourceAdapterTest {

    /**
     * Test related to STANBOL-698
     */
    @Test
    public void testDouble(){
        Graph graph = new IndexedGraph();
        IRI id = new IRI("http://www.example.org/test");
        IRI doubleTestField = new IRI("http://www.example.org/field/double");
        LiteralFactory lf = LiteralFactory.getInstance();
        graph.add(new TripleImpl(id, doubleTestField, lf.createTypedLiteral(Double.NaN)));
        graph.add(new TripleImpl(id, doubleTestField, lf.createTypedLiteral(Double.POSITIVE_INFINITY)));
        graph.add(new TripleImpl(id, doubleTestField, lf.createTypedLiteral(Double.NEGATIVE_INFINITY)));
        
        RdfValueFactory vf = new RdfValueFactory(graph);
        Representation r = vf.createRepresentation(id.getUnicodeString());
        Set<Double> expected = new HashSet<Double>(Arrays.asList(
            Double.NaN, Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY));
        Iterator<Double> dit = r.get(doubleTestField.getUnicodeString(), Double.class);
        while(dit.hasNext()){
            Double val = dit.next();
            Assert.assertNotNull(val);
            Assert.assertTrue(expected.remove(val));
        }
        Assert.assertTrue(expected.isEmpty());
    }
    
    @Test
    public void testFloat(){
        Graph graph = new IndexedGraph();
        IRI id = new IRI("http://www.example.org/test");
        IRI doubleTestField = new IRI("http://www.example.org/field/double");
        LiteralFactory lf = LiteralFactory.getInstance();
        graph.add(new TripleImpl(id, doubleTestField, lf.createTypedLiteral(Float.NaN)));
        graph.add(new TripleImpl(id, doubleTestField, lf.createTypedLiteral(Float.POSITIVE_INFINITY)));
        graph.add(new TripleImpl(id, doubleTestField, lf.createTypedLiteral(Float.NEGATIVE_INFINITY)));
        
        RdfValueFactory vf = new RdfValueFactory(graph);
        Representation r = vf.createRepresentation(id.getUnicodeString());
        Set<Float> expected = new HashSet<Float>(Arrays.asList(
            Float.NaN, Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY));
        Iterator<Float> dit = r.get(doubleTestField.getUnicodeString(), Float.class);
        while(dit.hasNext()){
            Float val = dit.next();
            Assert.assertNotNull(val);
            Assert.assertTrue(expected.remove(val));
        }
        Assert.assertTrue(expected.isEmpty());
    }
// TODO: how to create NAN, POSITIVE_INFINITY, NEGATIVE_INVINITY instances for BigDecimal
//    @Test
//    public void testBigDecimal(){
//        Graph graph = new IndexedGraph();
//        IRI id = new IRI("http://www.example.org/test");
//        IRI doubleTestField = new IRI("http://www.example.org/field/double");
//        LiteralFactory lf = LiteralFactory.getInstance();
//        graph.add(new TripleImpl(id, doubleTestField, lf.createTypedLiteral(BigDecimal.valueOf(Double.NaN))));
//        graph.add(new TripleImpl(id, doubleTestField, lf.createTypedLiteral(BigDecimal.valueOf(Double.POSITIVE_INFINITY))));
//        graph.add(new TripleImpl(id, doubleTestField, lf.createTypedLiteral(BigDecimal.valueOf(Double.NEGATIVE_INFINITY))));
//        
//        RdfValueFactory vf = new RdfValueFactory(graph);
//        Representation r = vf.createRepresentation(id.getUnicodeString());
//        Set<BigDecimal> expected = new HashSet<BigDecimal>(Arrays.asList(
//            BigDecimal.valueOf(Double.NaN), BigDecimal.valueOf(Double.POSITIVE_INFINITY),
//            BigDecimal.valueOf(Double.NEGATIVE_INFINITY)));
//        Iterator<BigDecimal> dit = r.get(doubleTestField.getUnicodeString(), BigDecimal.class);
//        while(dit.hasNext()){
//            BigDecimal val = dit.next();
//            Assert.assertNotNull(val);
//            Assert.assertTrue(expected.remove(val));
//        }
//        Assert.assertTrue(expected.isEmpty());
//    }
}

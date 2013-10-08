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
package org.apache.stanbol.entityhub.model.sesame.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.stanbol.entityhub.model.sesame.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.junit.Assert;
import org.junit.Test;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.TreeModel;
import org.openrdf.model.impl.ValueFactoryImpl;

public class ResourceAdapterTest {

    
    private ValueFactory vf = ValueFactoryImpl.getInstance();
    
    /**
     * Test related to STANBOL-698
     */
    @Test
    public void testDouble(){
        Model graph = new TreeModel();
        URI id = vf.createURI("http://www.example.org/test");
        URI doubleTestField = vf.createURI("http://www.example.org/field/double");
        graph.add(id, doubleTestField, vf.createLiteral(Double.NaN));
        graph.add(id, doubleTestField, vf.createLiteral(Double.POSITIVE_INFINITY));
        graph.add(id, doubleTestField, vf.createLiteral(Double.NEGATIVE_INFINITY));
        
        RdfValueFactory valueFactory = new RdfValueFactory(graph,vf);
        Representation r = valueFactory.createRepresentation(id.stringValue());
        Set<Double> expected = new HashSet<Double>(Arrays.asList(
            Double.NaN, Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY));
        Iterator<Double> dit = r.get(doubleTestField.stringValue(), Double.class);
        while(dit.hasNext()){
            Double val = dit.next();
            Assert.assertNotNull(val);
            Assert.assertTrue(expected.remove(val));
        }
        Assert.assertTrue(expected.isEmpty());
    }
    
    @Test
    public void testFloat(){
        Model graph = new TreeModel();
        URI id = vf.createURI("http://www.example.org/test");
        URI floatTestField = vf.createURI("http://www.example.org/field/float");
        graph.add(id, floatTestField, vf.createLiteral(Float.NaN));
        graph.add(id, floatTestField, vf.createLiteral(Float.POSITIVE_INFINITY));
        graph.add(id, floatTestField, vf.createLiteral(Float.NEGATIVE_INFINITY));
        
        RdfValueFactory valueFactory = new RdfValueFactory(graph,vf);
        Representation r = valueFactory.createRepresentation(id.stringValue());
        Set<Float> expected = new HashSet<Float>(Arrays.asList(
            Float.NaN, Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY));
        Iterator<Float> dit = r.get(floatTestField.stringValue(), Float.class);
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
//        MGraph graph = new IndexedMGraph();
//        UriRef id = new UriRef("http://www.example.org/test");
//        UriRef doubleTestField = new UriRef("http://www.example.org/field/double");
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

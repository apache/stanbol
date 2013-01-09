package org.apache.stanbol.entityhub.model.clerezza.impl;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
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
        MGraph graph = new IndexedMGraph();
        UriRef id = new UriRef("http://www.example.org/test");
        UriRef doubleTestField = new UriRef("http://www.example.org/field/double");
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
        MGraph graph = new IndexedMGraph();
        UriRef id = new UriRef("http://www.example.org/test");
        UriRef doubleTestField = new UriRef("http://www.example.org/field/double");
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

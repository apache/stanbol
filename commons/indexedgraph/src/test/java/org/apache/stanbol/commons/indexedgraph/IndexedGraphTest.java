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
package org.apache.stanbol.commons.indexedgraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.clerezza.commons.rdf.BlankNode;
import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.test.GraphTest;
import org.apache.clerezza.rdf.ontologies.FOAF;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexedGraphTest  extends GraphTest {

    protected static final Logger log = LoggerFactory.getLogger(IndexedGraphTest.class);
    
    private IRI uriRef1 = new IRI("http://example.org/foo");
    private IRI uriRef2 = new IRI("http://example.org/bar");
    private IRI uriRef3 = new IRI("http://example.org/test");
    private Triple triple1 = new TripleImpl(uriRef1, uriRef2, uriRef3);
    private Triple triple2 = new TripleImpl(uriRef2, uriRef2, uriRef1);
    private Triple triple3 = new TripleImpl(uriRef3, uriRef1, uriRef3);
    private Triple triple4 = new TripleImpl(uriRef1, uriRef3, uriRef2);
    private Triple triple5 = new TripleImpl(uriRef2, uriRef3, uriRef2);
    
    @Override
    protected Graph getEmptyGraph() {
        return new IndexedGraph();
    }
    @Test
    public void bNodeConsitency() {
        Graph mGraph = getEmptyGraph();
        final BlankNode bNode = new BlankNode() {

            @Override
            public int hashCode() {
                return -1;
            }

            @Override
            public boolean equals(Object o) {
                return o instanceof BlankNode;
            }
            
        
        };
        
        final BlankNode bNodeClone = new BlankNode() {

            @Override
            public int hashCode() {
                return -1;
            }

            @Override
            public boolean equals(Object o) {
                return o instanceof BlankNode; 
            }
            
        
        };

        mGraph.add(new TripleImpl(bNode, uriRef1, uriRef2));
        mGraph.add(new TripleImpl(bNodeClone, uriRef2, uriRef3));
        BlankNodeOrIRI bNodeBack = mGraph.filter(null, uriRef1, uriRef2).next().getSubject();
        Assert.assertEquals("The bnode we get back is not equals to the one we added", bNode, bNodeBack);
        BlankNodeOrIRI bNodeBack2 = mGraph.filter(null, uriRef2, uriRef3).next().getSubject();
        Assert.assertEquals("The returnned bnodes are no longer equals", bNodeBack, bNodeBack2);
        Assert.assertTrue("Not finding a triple when searching with equal bNode", mGraph.filter(bNodeBack, uriRef2, null).hasNext());
    }
    @Test
    public void iteratorRemove() {
        Graph itc = new IndexedGraph();
        itc.add(triple1);
        itc.add(triple2);
        itc.add(triple3);
        itc.add(triple4);
        itc.add(triple5);
        Iterator<Triple> iter = itc.iterator();
        while (iter.hasNext()) {
            iter.next();
            iter.remove();
        }
        Assert.assertEquals(0, itc.size());
    }

    @Test
    public void removeAll() {
        Graph itc = new IndexedGraph();
        itc.add(triple1);
        itc.add(triple2);
        itc.add(triple3);
        itc.add(triple4);
        itc.add(triple5);
        Graph itc2 = new IndexedGraph();
        itc2.add(triple1);
        itc2.add(triple3);
        itc2.add(triple5);
        itc.removeAll(itc2);
        Assert.assertEquals(2, itc.size());
    }
    
    @Test
    public void filterIteratorRemove() {
        Graph itc = new IndexedGraph();
        itc.add(triple1);
        itc.add(triple2);
        itc.add(triple3);
        itc.add(triple4);
        itc.add(triple5);       
        Iterator<Triple> iter = itc.filter(uriRef1, null, null);
        while (iter.hasNext()) {
            iter.next();
            iter.remove();
        }
        Assert.assertEquals(3, itc.size());
    }

    @Test(expected=ConcurrentModificationException.class)
    public void remove() {
        Graph itc = new IndexedGraph();
        itc.add(triple1);
        itc.add(triple2);
        itc.add(triple3);
        itc.add(triple4);
        itc.add(triple5);
        Iterator<Triple> iter = itc.filter(uriRef1, null, null);
        while (iter.hasNext()) {
            Triple triple = iter.next();
            itc.remove(triple);
        }
        Assert.assertEquals(3, itc.size());
    }
    /**
     * Holds the test data to perform 
     * {@link Graph#filter(BlankNodeOrIRI, IRI, RDFTerm)}
     * tests on {@link Graph} implementations
     * @author rwesten
     */
    public static final class TestCase {
        public final List<BlankNodeOrIRI> subjects;
        public final List<RDFTerm> objects;
        public final List<IRI> predicates;

        /**
         * Create a new Test with a maximum number of subjects, predicates and
         * objects based on data in the parsed triple collection
         * @param tc the data
         * @param sNum the maximum number of subjects
         * @param pNum the maximum number of predicates
         * @param oNum the maximum number of objects
         */
        public TestCase(Graph tc,int sNum, int pNum, int oNum){
            Set<BlankNodeOrIRI> subjects = new LinkedHashSet<BlankNodeOrIRI>();
            Set<RDFTerm> objects = new LinkedHashSet<RDFTerm>();
            Set<IRI> predicates = new LinkedHashSet<IRI>();
            for(Iterator<Triple> it = tc.iterator();it.hasNext();){
                Triple t = it.next();
                if(subjects.size() < 100){
                    subjects.add(t.getSubject());
                }
                if(predicates.size() < 5){
                    predicates.add(t.getPredicate());
                }
                if(objects.size() < 100){
                    objects.add(t.getObject());
                }
            }
            this.subjects = Collections.unmodifiableList(
                new ArrayList<BlankNodeOrIRI>(subjects));
            this.predicates = Collections.unmodifiableList(
                new ArrayList<IRI>(predicates));
            this.objects = Collections.unmodifiableList(
                new ArrayList<RDFTerm>(objects));
        }
    }
    @Test
    public void testPerformance(){
        //Reduced values to fix STANBOL-
        Set<Triple> graph = new HashSet<Triple>();
        int iterations = 100; //reduced from 1000
        int graphsize = 100000;
        Long seed = System.currentTimeMillis();
        log.info("Test Seed: {}",seed);
        createGraph(graph, graphsize, seed);
        log.info("Load Time ({} triples)", graph.size());
        long start = System.currentTimeMillis();
        Graph sg = new SimpleGraph(graph);
        log.info("  ... {}: {}",sg.getClass().getSimpleName(), System.currentTimeMillis()-start);
        start = System.currentTimeMillis();
        Graph ig = new IndexedGraph(graph);
        log.info("  ... {}: {}",ig.getClass().getSimpleName(), System.currentTimeMillis()-start);
        //Simple ImmutableGraph reference test
        TestCase testCase = new TestCase(sg, 20, 5, 20); //reduced form 100,5,100
        log.info("Filter Performance Test (graph size {} triples, iterations {})",graphsize,iterations);
        log.info(" --- TEST {} with {} triples ---",sg.getClass().getSimpleName(),sg.size());
        start = System.currentTimeMillis();
        List<Long> sgr = executeTest(sg, testCase, iterations);
        log.info(" --- TEST completed in {}ms",System.currentTimeMillis()-start);

        log.info(" --- TEST {} {} triples ---",ig.getClass().getSimpleName(),sg.size());
        start = System.currentTimeMillis();
        List<Long> igr = executeTest(ig, testCase, iterations);
        log.info(" --- TEST completed in {}ms",System.currentTimeMillis()-start);
        Assert.assertEquals(sgr, igr); //validate filter implementation
    }
    
    public List<Long> executeTest(Graph graph, TestCase test, int testCount){
        List<Long> testResults = new ArrayList<Long>();
        long start;
        long resultCount;
        //[S,P,O]
        start = System.currentTimeMillis();
        resultCount = testSPO(graph, test, testCount);
        testResults.add(new Long(resultCount));
        log.info("... run [S,P,O] in {}ms with {} results",System.currentTimeMillis()-start,resultCount);
        //[S,P,n]
        start = System.currentTimeMillis();
        resultCount = testSPn(graph, test, testCount);
        testResults.add(new Long(resultCount));
        log.info("... run [S,P,n] in {}ms with {} results",System.currentTimeMillis()-start,resultCount);
        //[S,n,O]
        start = System.currentTimeMillis();
        resultCount = testSnO(graph, test, testCount);
        testResults.add(new Long(resultCount));
        log.info("... run [S,n,O] in {}ms with {} results",System.currentTimeMillis()-start,resultCount);
        //[n,P,O]
        start = System.currentTimeMillis();
        resultCount = testnPO(graph, test, testCount);
        testResults.add(new Long(resultCount));
        log.info("... run [n,P,O] in {}ms with {} results",System.currentTimeMillis()-start,resultCount);
        //[S,n,n]
        start = System.currentTimeMillis();
        resultCount = testSnn(graph, test, testCount);
        testResults.add(new Long(resultCount));
        log.info("... run [S,n,n] in {}ms with {} results",System.currentTimeMillis()-start,resultCount);
        //[n,P,n]
        start = System.currentTimeMillis();
        resultCount = testnPn(graph, test, testCount);
        testResults.add(new Long(resultCount));
        log.info("... run [n,P,n] in {}ms with {} results",System.currentTimeMillis()-start,resultCount);
        //[n,n,O]
        start = System.currentTimeMillis();
        resultCount = testnnO(graph, test, testCount);
        testResults.add(new Long(resultCount));
        log.info("... run [n,n,O] in {}ms with {} results",System.currentTimeMillis()-start,resultCount);
        return testResults;
    }

    private long testSPO(Graph graph, TestCase test, int testCount) {
        Iterator<Triple> it;
        long count = 0;
        int si = -1;
        int pi = -1;
        int oi;
        for(int num = 0;num < testCount;num++){
            oi = num%test.objects.size();
            if(oi == 0) {
                pi++;
                pi = pi%test.predicates.size();
            }
            if(pi == 0) {
                si++;
                si = si%test.subjects.size();
            }
            it = graph.filter(test.subjects.get(si), test.predicates.get(pi), test.objects.get(oi));
            while(it.hasNext()){
                it.next();
                count++;
            }
        }
        return count;
    }
    
    private long testSPn(Graph graph, TestCase test, int testCount) {
        Iterator<Triple> it;
        long count = 0;
        int si = -1;
        int pi = 0;
        for(int num = 0;num < testCount;num++){
            pi = num%test.predicates.size();
            if(pi == 0) {
                si++;
                si = si%test.subjects.size();
            }
            it = graph.filter(test.subjects.get(si), test.predicates.get(pi), null);
            while(it.hasNext()){
                it.next();
                count++;
            }
        }
        return count;
    }
    
    private long testSnO(Graph graph, TestCase test, int testCount) {
        Iterator<Triple> it;
        long count = 0;
        int si = -1;
        int oi;
        for(int num = 0;num < testCount;num++){
            oi = num%test.objects.size();
            if(oi == 0) {
                si++;
                si = si%test.subjects.size();
            }
            it = graph.filter(test.subjects.get(si), null, test.objects.get(oi));
            while(it.hasNext()){
                it.next();
                count++;
            }
        }
        return count;
    }
    
    private long testnPO(Graph graph, TestCase test, int testCount) {
        Iterator<Triple> it;
        long count = 0;
        int pi = -1;
        int oi;
        for(int num = 0;num < testCount;num++){
            oi = num%test.objects.size();
            if(oi == 0) {
                pi++;
                pi = pi%test.predicates.size();
            }
            it = graph.filter(null, test.predicates.get(pi), test.objects.get(oi));
            while(it.hasNext()){
                it.next();
                count++;
            }
        }
        return count;
    }
    private long testSnn(Graph graph, TestCase test, int testCount) {
        Iterator<Triple> it;
        long count = 0;
        int si = 0;
        for(int num = 0;num < testCount;num++){
            si = num%test.subjects.size();
            it = graph.filter(test.subjects.get(si), null, null);
            while(it.hasNext()){
                it.next();
                count++;
            }
        }
        return count;
    }
    private long testnPn(Graph graph, TestCase test, int testCount) {
        Iterator<Triple> it;
        long count = 0;
        int pi;
        for(int num = 0;num < testCount;num++){
            pi = num%test.predicates.size();
            it = graph.filter(null, test.predicates.get(pi), null);
            while(it.hasNext()){
                it.next();
                count++;
            }
        }
        return count;
    }
    private long testnnO(Graph graph, TestCase test, int testCount) {
        Iterator<Triple> it;
        long count = 0;
        int oi;
        for(int num = 0;num < testCount;num++){
            oi = num%test.objects.size();
            it = graph.filter(null, null, test.objects.get(oi));
            while(it.hasNext()){
                it.next();
                count++;
            }
        }
        return count;
    }
    
    private static void createGraph(Collection<Triple> tc, int triples, Long seed){
        Random rnd = new Random();
        if(seed != null){
             rnd.setSeed(seed);
        }
        LiteralFactory lf = LiteralFactory.getInstance();
        //randoms are in the range [0..3]
        double l = 1.0; //literal
        double i = l / 3; //int
        double d = l * 2 / 3;//double
        double b = 2.0;//bNode
        double nb = b - (l * 2 / 3); //create new bNode
        double random;
        BlankNodeOrIRI subject = null;
        IRI predicate = null;
        List<IRI> predicateList = new ArrayList<IRI>();
        predicateList.add(RDF.first);
        predicateList.add(RDF.rest);
        predicateList.add(RDF.type);
        predicateList.add(RDFS.label);
        predicateList.add(RDFS.comment);
        predicateList.add(RDFS.range);
        predicateList.add(RDFS.domain);
        predicateList.add(FOAF.name);
        predicateList.add(FOAF.nick);
        predicateList.add(FOAF.homepage);
        predicateList.add(FOAF.age);
        predicateList.add(FOAF.depiction);
        String URI_PREFIX = "http://www.test.org/bigGraph/ref";
        Language DE = new Language("de");
        Language EN = new Language("en");
        Iterator<IRI> predicates = predicateList.iterator();
        List<BlankNode> bNodes = new ArrayList<BlankNode>();
        bNodes.add(new BlankNode());
        for (int count = 0; tc.size() < triples; count++) {
            random = rnd.nextDouble() * 3;
            if (random >= 2.5 || count == 0) {
                if (random <= 2.75) {
                    subject = new IRI(URI_PREFIX + count);
                } else {
                    int rndIndex = (int) ((random - 2.75) * bNodes.size() / (3.0 - 2.75));
                    subject = bNodes.get(rndIndex);
                }
            }
            if (random > 2.0 || count == 0) {
                if (!predicates.hasNext()) {
                    Collections.shuffle(predicateList,rnd);
                    predicates = predicateList.iterator();
                }
                predicate = predicates.next();
            }
            if (random <= l) { //literal
                if (random <= i) {
                    tc.add(new TripleImpl(subject, predicate, lf.createTypedLiteral(count)));
                } else if (random <= d) {
                    tc.add(new TripleImpl(subject, predicate, lf.createTypedLiteral(random)));
                } else {
                    Literal text;
                    if (random <= i) {
                        text = new PlainLiteralImpl("Literal for " + count);
                    } else if (random <= d) {
                        text = new PlainLiteralImpl("An English literal for " + count, EN);
                    } else {
                        text = new PlainLiteralImpl("Ein Deutsches Literal für " + count, DE);
                    }
                    tc.add(new TripleImpl(subject, predicate, text));
                }
            } else if (random <= b) { //bnode
                BlankNode bnode;
                if (random <= nb) {
                    bnode = new BlankNode();
                    bNodes.add(bnode);
                } else { //>nb <b
                    int rndIndex = (int) ((random - nb) * bNodes.size() / (b - nb));
                    bnode = bNodes.get(rndIndex);
                }
                tc.add(new TripleImpl(subject, predicate, bnode));
            } else { //IRI
                tc.add(new TripleImpl(subject, predicate,
                        new IRI(URI_PREFIX + count * random)));
            }
        }        
    }
    
}

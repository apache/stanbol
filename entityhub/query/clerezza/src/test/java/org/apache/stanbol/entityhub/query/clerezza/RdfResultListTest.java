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
package org.apache.stanbol.entityhub.query.clerezza;

import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.entityhub.core.query.FieldQueryImpl;
import org.apache.stanbol.entityhub.model.clerezza.RdfRepresentation;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RdfResultListTest {
    private final static Logger log = LoggerFactory.getLogger(RdfResultListTest.class);
    /**
     * Providing a sorted Iteration over query results stored in an RDF
     * graph is not something trivial. Therefore this test
     */
    @Test
    public void testRdfResultSorting(){
        SortedMap<Double,RdfRepresentation> sorted = new TreeMap<Double,RdfRepresentation>();
        Graph resultGraph = new IndexedGraph();
        RdfValueFactory vf = new RdfValueFactory(resultGraph);
        IRI resultListNode = new IRI(RdfResourceEnum.QueryResultSet.getUri());
        IRI resultProperty = new IRI(RdfResourceEnum.queryResult.getUri());
        for(int i=0;i<100;i++){
            Double rank;
            do { //avoid duplicate keys
                rank = Math.random();
            } while (sorted.containsKey(rank));
            RdfRepresentation r = vf.createRepresentation("urn:sortTest:rep."+i);
            //link the representation with the query result set
            resultGraph.add(new TripleImpl(resultListNode,resultProperty,r.getNode()));
            r.set(RdfResourceEnum.resultScore.getUri(), rank);
            sorted.put(rank, r);
        }
        RdfQueryResultList resultList = new RdfQueryResultList(new FieldQueryImpl(),
            resultGraph);
        if(log.isDebugEnabled()){
            log.debug("---DEBUG Sorting ---");
            for(Iterator<Representation> it = resultList.iterator();it.hasNext();){
                Representation r = it.next();
                log.debug("{}: {}",r.getFirst(RdfResourceEnum.resultScore.getUri()),r.getId());
            }
        }
        log.debug("---ASSERT Sorting ---");
        for(Iterator<Representation> it = resultList.iterator();it.hasNext();){
            Representation r = it.next();
            Double lastkey = sorted.lastKey();
            Representation last = sorted.get(lastkey);
            Assert.assertEquals("score: "+r.getFirst(RdfResourceEnum.resultScore.getUri())+
                " of Representation "+r.getId()+" is not as expected "+
                last.getFirst(RdfResourceEnum.resultScore.getUri())+" of Representation "+
                last.getId()+"!",r, last);
            sorted.remove(lastkey);
        }
        Assert.assertTrue(sorted.isEmpty());
    }

}

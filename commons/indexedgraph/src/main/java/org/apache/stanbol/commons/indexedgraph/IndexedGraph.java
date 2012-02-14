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

import java.util.Iterator;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.AbstractGraph;
/**
 * {@link Graph} implementation that internally uses a {@link IndexedTripleCollection}
 * to hold the RDF graph.
 * @author rwesten
 *
 */
public class IndexedGraph extends AbstractGraph implements Graph {

    private final TripleCollection tripleCollection;
    
    /**
     * Creates a graph with the triples in tripleCollection
     * 
     * @param tripleCollection the collection of triples this Graph shall consist of
     */
    public IndexedGraph(TripleCollection tripleCollection) {
        this.tripleCollection = new IndexedTripleCollection(tripleCollection);
    }

    /**
     * Create a graph with the triples provided by the Iterator
     * @param tripleIter the iterator over the triples
     */
    public IndexedGraph(Iterator<Triple> tripleIter) {
        this.tripleCollection = new IndexedTripleCollection(tripleIter);
    }
//    /**
//     * Create a read-only {@link Graph} wrapper over the provided 
//     * {@link TripleCollection}
//     * @param tripleCollection the indexed triple collection create a read-only
//     * wrapper around
//     */
//    protected IndexedGraph(IndexedTripleCollection tripleCollection){
//        this.tripleCollection = tripleCollection;
//    }
    
    @Override
    protected Iterator<Triple> performFilter(NonLiteral subject, UriRef predicate, Resource object) {
        return tripleCollection.filter(subject, predicate, object);
    }

    
    @Override
    public int size() {
        return tripleCollection.size();
    }

}

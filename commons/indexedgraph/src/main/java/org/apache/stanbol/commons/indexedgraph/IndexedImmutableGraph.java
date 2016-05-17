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

import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.AbstractImmutableGraph;
/**
 * {@link ImmutableGraph} implementation that internally uses a {@link IndexedGraph}
 * to hold the RDF graph.
 * @author rwesten
 *
 */
public class IndexedImmutableGraph extends AbstractImmutableGraph implements ImmutableGraph {

    private final Graph tripleCollection;
    
    /**
     * Creates a graph with the triples in tripleCollection
     * 
     * @param tripleCollection the collection of triples this ImmutableGraph shall consist of
     */
    public IndexedImmutableGraph(Graph tripleCollection) {
        this.tripleCollection = new IndexedGraph(tripleCollection);
    }

    /**
     * Create a graph with the triples provided by the Iterator
     * @param tripleIter the iterator over the triples
     */
    public IndexedImmutableGraph(Iterator<Triple> tripleIter) {
        this.tripleCollection = new IndexedGraph(tripleIter);
    }
//    /**
//     * Create a read-only {@link public class IndexedImmutableGraph extends AbstractImmutableGraph implements ImmutableGraph {} wrapper over the provided 
//     * {@link Graph}
//     * @param tripleCollection the indexed triple collection create a read-only
//     * wrapper around
//     */
//    protected IndexedGraph(IndexedGraph tripleCollection){
//        this.tripleCollection = tripleCollection;
//    }
    
    @Override
    protected Iterator<Triple> performFilter(BlankNodeOrIRI subject, IRI predicate, RDFTerm object) {
        return tripleCollection.filter(subject, predicate, object);
    }

    
    @Override
    public int performSize() {
        return tripleCollection.size();
    }

}

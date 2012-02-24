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

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.AbstractTripleCollection;
import org.apache.clerezza.rdf.core.impl.TripleImpl;

/**
 * {@link TripleCollection} implementation that uses indexes for <ul>
 * <li> subject, predicate, object [SPO]
 * <li> predicate, object, subject [POS]
 * <li> object, subject, predicate [OSP]
 * </ul>
 * Indexes are maintained in {@link TreeSet}s with according {@link Comparator}
 * instances ({@link #SPO_COMPARATOR}, {@link #POS_COMPARATOR} ,
 * {@link #OSP_COMPARATOR}). {@link Resource}s are compared first using the
 * {@link Resource#hashCode()} and only if this matches by using
 * {@link Resource}{@link #toString()}.<p>
 * The {@link #filter(NonLiteral, UriRef, Resource)} implementation is based
 * on {@link TreeSet#subSet(Object, Object)}. All Iterators returned directly
 * operate on top of one of the internal indexes.
 * <p>
 * This class is not public, implementations should use {@link IndexedGraph} or
 * {@link IndexedMGraph}.
 *
 * @author rwesten
 */
class IndexedTripleCollection extends AbstractTripleCollection implements TripleCollection {

    private final NavigableSet<Triple> spo = new TreeSet<Triple>(SPO_COMPARATOR);
    private final NavigableSet<Triple> pos = new TreeSet<Triple>(POS_COMPARATOR);
    private final NavigableSet<Triple> osp = new TreeSet<Triple>(OSP_COMPARATOR);
    
    /**
     * Creates an empty {@link IndexedTripleCollection}
     */
    public IndexedTripleCollection() { 
        super();
    }

    /**
     * Creates a {@link IndexedTripleCollection} using the passed iterator, the iterator 
     * is consumed before the constructor returns
     * 
     * @param iterator
     */
    public IndexedTripleCollection(Iterator<Triple> iterator) {
        super();
        while (iterator.hasNext()) {
            Triple triple = iterator.next();
            performAdd(triple);
        }
    }

    /**
     * Creates a {@link IndexedTripleCollection} for the specified collection of triples,
     * subsequent modification of baseSet do not affect the created instance.
     *
     * @param iterable over triples
     */
    public IndexedTripleCollection(Collection<Triple> baseCollection) {
        super();
        spo.addAll(baseCollection);
        pos.addAll(baseCollection);
        osp.addAll(baseCollection);
    }
    
    @Override
    protected Iterator<Triple> performFilter(final NonLiteral subject, final UriRef predicate, final Resource object) {
        if(subject == null && predicate == null && object == null){ //[n,n,n]
            return createIterator(spo, spo.iterator());
        }
        final Triple low = new TripleImpl(
            subject == null ? MIN : subject, 
                    predicate == null ? MIN : predicate, 
                            object == null ? MIN : object);
        final Triple high = new TripleImpl(
            subject == null ? MAX : subject, 
                    predicate == null ? MAX : predicate, 
                            object == null ? MAX : object);
        if(subject != null && predicate != null && object != null){ // [S,P,O]
            //NOTE: low.equals(high) in that case!
            return createIterator(spo, spo.subSet(low, true, low, true).iterator());
        } else if(subject != null && object == null){ //[S,n,n], [S,P,n] 
            return createIterator(spo, spo.subSet(low, high).iterator());
        } else if (predicate != null) { //[n,P,n], [n,P,O]
            return createIterator(pos,pos.subSet(low, high).iterator());
        } else { //[n,n,O] , [S,n,O]
            return createIterator(osp,osp.subSet(low, high).iterator());
        }
    }

    @Override
    protected boolean performAdd(Triple triple) {
        if(spo.add(triple)){
            osp.add(triple);
            return pos.add(triple);
        }
        return false;
    }
    
    @Override
    protected boolean performRemove(Triple triple) {
        if(spo.remove(triple)){
            osp.remove(triple);
            return pos.remove(triple);
        } 
        return false;
    }
    
    @Override
    public int size() {
        return spo.size();
    }
//    @Override
//    public Iterator<Triple> iterator() {
//        return createIterator(spo, spo.iterator());
//    }

    /**
     * Returns an Iterator that ensures that calls to {@link Iterator#remove()}
     * remove items from all three indexes
     * @param index
     * @param base
     * @return
     */
    private Iterator<Triple> createIterator(final SortedSet<Triple> index,final Iterator<Triple> base){
        return new Iterator<Triple>() {
            Triple current = null;
            @Override
            public boolean hasNext() {
                return base.hasNext();
            }

            @Override
            public Triple next() {
                current = base.next();
                return current;
            }

            @Override
            public void remove() {
                base.remove();
                if(current != null){
                    if(!(index == spo)){
                        spo.remove(current);
                    } 
                    if(!(index == pos)){
                        pos.remove(current);
                    }
                    if(!(index == osp)){
                        osp.remove(current);
                    }
                }
            }
        };
        
    }

    
    /**
     * Compares Triples based on Subject, Predicate, Object
     */
    public static final Comparator<Triple> SPO_COMPARATOR = new Comparator<Triple>() {

        @Override
        public int compare(Triple a, Triple b) {
            int c = IndexedTripleCollection.compare(a.getSubject(), b.getSubject());
            if(c == 0){
                c = IndexedTripleCollection.compare(a.getPredicate(), b.getPredicate());
                if(c == 0){
                    c =  IndexedTripleCollection.compare(a.getObject(), b.getObject());
                }
            }
            return c;
        }
    };
    /**
     * Compares Triples based on Predicate, Object, Subject
     */
    public static final Comparator<Triple> POS_COMPARATOR = new Comparator<Triple>() {

        @Override
        public int compare(Triple a, Triple b) {
            int c = IndexedTripleCollection.compare(a.getPredicate(), b.getPredicate());
            if(c == 0){
                c = IndexedTripleCollection.compare(a.getObject(), b.getObject());
                if(c == 0){
                    c =  IndexedTripleCollection.compare(a.getSubject(), b.getSubject());
                }
            }
            return c;
        }
    };
    protected static UriRef MIN = new UriRef("") {
        @Override
        public int hashCode() {
            return Integer.MIN_VALUE;
        };
    };
    protected static UriRef MAX = new UriRef("") {
        @Override
        public int hashCode() {
            return Integer.MAX_VALUE;
        };
    };
    

    /**
     * Compares Triples based on Object, Subject, Predicate
     */
    public static final Comparator<Triple> OSP_COMPARATOR = new Comparator<Triple>() {

        @Override
        public int compare(Triple a, Triple b) {
            int c = IndexedTripleCollection.compare(a.getObject(), b.getObject());
            if(c == 0){
                c = IndexedTripleCollection.compare(a.getSubject(), b.getSubject());
                if(c == 0){
                    c =  IndexedTripleCollection.compare(a.getPredicate(), b.getPredicate());
                }
            }
            return c;
        }
    };
    /**
     * Compares two resources with special support for {@link #MIN} and
     * {@link #MAX} to allow building {@link SortedSet#subSet(Object, Object)}
     * for <code>null</code> values parsed to 
     * {@link #filter(NonLiteral, UriRef, Resource)}
     * @param a
     * @param b
     * @return
     */
    protected static int compare(Resource a, Resource b) {
        int hashA = a.hashCode();
        int hashB = b.hashCode();
        if (hashA != hashB) {
            return hashA > hashB ? 1 : -1;
        }
        return a == MIN || b == MAX ? -1 :
            a == MAX || b == MIN ? 1 : a.toString().compareTo(b.toString());
    }

    
}

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
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.PlainLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.TypedLiteral;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.AbstractTripleCollection;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link TripleCollection} implementation that uses indexes for <ul>
 * <li> subject, predicate, object [SPO]
 * <li> predicate, object, subject [POS]
 * <li> object, subject, predicate [OSP]
 * </ul>
 * Indexes are maintained in {@link TreeSet}s with according {@link Comparator}
 * instances ({@link #spoComparator}, {@link #posComparator} ,
 * {@link #ospComparator}). {@link Resource}s are compared first using the
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

    private static final Logger log = LoggerFactory.getLogger(IndexedTripleCollection.class);
    
    /**
     * This map is used to ensure constant ordering for {@link BNode} that do
     * have the same hashcode (and therefore result to have the same
     * {@link BNode#toString()} value.
     */
    private final Map<Integer,List<Resource>> hashCodeConflictMap = new HashMap<Integer,List<Resource>>();
    /**
     * Compares Triples based on Subject, Predicate, Object
     */
    private final Comparator<Triple> spoComparator = new Comparator<Triple>() {

        @Override
        public int compare(Triple a, Triple b) {
            int c = IndexedTripleCollection.compare(a.getSubject(), b.getSubject(), hashCodeConflictMap);
            if(c == 0){
                c = IndexedTripleCollection.compare(a.getPredicate(), b.getPredicate(), hashCodeConflictMap);
                if(c == 0){
                    c =  IndexedTripleCollection.compare(a.getObject(), b.getObject(), hashCodeConflictMap);
                }
            }
            return c;
        }
    };
    /**
     * The SPO index
     */
    private final NavigableSet<Triple> spo = new TreeSet<Triple>(spoComparator);
    /**
     * Compares Triples based on Predicate, Object, Subject
     */
    private final Comparator<Triple> posComparator = new Comparator<Triple>() {

        @Override
        public int compare(Triple a, Triple b) {
            int c = IndexedTripleCollection.compare(a.getPredicate(), b.getPredicate(), hashCodeConflictMap);
            if(c == 0){
                c = IndexedTripleCollection.compare(a.getObject(), b.getObject(), hashCodeConflictMap);
                if(c == 0){
                    c =  IndexedTripleCollection.compare(a.getSubject(), b.getSubject(), hashCodeConflictMap);
                }
            }
            return c;
        }
    };
    /**
     * The POS index
     */
    private final NavigableSet<Triple> pos = new TreeSet<Triple>(posComparator);
    /**
     * Compares Triples based on Object, Subject, Predicate
     */
    private final Comparator<Triple> ospComparator = new Comparator<Triple>() {

        @Override
        public int compare(Triple a, Triple b) {
            int c = IndexedTripleCollection.compare(a.getObject(), b.getObject(), hashCodeConflictMap);
            if(c == 0){
                c = IndexedTripleCollection.compare(a.getSubject(), b.getSubject(), hashCodeConflictMap);
                if(c == 0){
                    c =  IndexedTripleCollection.compare(a.getPredicate(), b.getPredicate(), hashCodeConflictMap);
                }
            }
            return c;
        }
    };
    /**
     * The OSP index
     */
    private final NavigableSet<Triple> osp = new TreeSet<Triple>(ospComparator);
    
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
        //use internal index to fill the other indexes, because the parsed
        //collection might be slow
        pos.addAll(spo); 
        osp.addAll(spo);
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

//    /**
//     * Compares two resources with special support for {@link #MIN} and
//     * {@link #MAX} to allow building {@link SortedSet#subSet(Object, Object)}
//     * for <code>null</code> values parsed to 
//     * {@link #filter(NonLiteral, UriRef, Resource)}
//     * @param a
//     * @param b
//     * @return
//     */
//    protected static int compareHash(Resource a, Resource b, Map<Integer,List<Resource>> confictsMap) {
//        int hashA = a.hashCode();
//        int hashB = b.hashCode();
//        if (hashA != hashB) {
//            return hashA > hashB ? 1 : -1;
//        }
//        //those resources might be equals
//        //(1) Check for MIN, MAX (used to build sub-sets). Other resources might
//        //    have a similar hasCode
//        int state = a == MIN || b == MAX ? -1 :
//            a == MAX || b == MIN ? 1 : 0;
//        if(state == 0){
//            if(a.equals(b)){ //check of the resources are equals
//                return 0; //return zero
//            } else if(//we need to care about HashCode conflicts 
//                a instanceof BNode && b instanceof BNode){ // of BNodes
//                log.info("HashCode conflict for {} and {}",a,b); //we have a conflict
//                return resolveBNodeHashConflict(a, b, confictsMap);
//            } else { //same hashCode but not equals
//                //use the String representation of the Resources to sort them
//                String as = resource2String(a);
//                String bs = resource2String(b);
//                log.info("same hash code {} - compare Strings a: {}, b: {}",
//                    new Object[]{a.hashCode(),as,bs});
//                return as.compareTo(bs);
//            }
//        }
//       return state;
//    }

    /**
     * Resolved BNode hasConflics, by storing the correct order for the affected
     * {@link Integer} in a {@link List} of Resource instances.
     * @param a the first {@link BNode}
     * @param b the second {@link BNode}
     * @param confictsMap the Map used to store the order of BNodes with conflicts
     * @return the decision taken based on the confictsMap.
     */
    private static int resolveBNodeHashConflict(Resource a, Resource b,
            Map<Integer,List<Resource>> confictsMap) {
        //This is not a bad thing. We need just to ensure constant ordering
        //and as there is nothing we can use to distinguish we need to keep
        //this information in a list.
        Integer hash = Integer.valueOf(a.hashCode());
        List<Resource> resources = confictsMap.get(hash);
        if(resources == null){ //new conflict ... just add and return
            resources = new ArrayList<Resource>(2);
            confictsMap.put(hash, resources);
            resources.add(a);
            resources.add(b);
            return -1;
        }
        //already conflicting resource for this hash present
        int aIndex=-1;
        int bIndex=-1;
        for(int i = 0; i<resources.size() && (aIndex < 0 || bIndex < 0);i++){
            Resource r = resources.get(i);
            if(aIndex < 0 && r.equals(a)){
                aIndex = i;
            }
            if(bIndex < 0 && r.equals(b)){
                bIndex = i;
            }
        }
        if(aIndex < 0){ //a not found
            aIndex = resources.size();
            resources.add(a);
        }
        if(bIndex < 0){ //b not found
            bIndex = resources.size();
            resources.add(b);
        }
        return aIndex < bIndex ? -1 : 1;
    }
    /**
     * Compares Resources to correctly sort them within the index.<p>
     * Sort criteria are:<ol>
     * <li> URIs are sorted by the {@link UriRef#getUnicodeString()} unicode string)
     * <li> Literals 
     *  <ol>
     *      <li> sort by the {@link Literal#getLexicalForm() lixical form}
     *      <li> sort by {@link PlainLiteral#getLanguage() language} (<code>null</code> value first)
     *      <li> sort by {@link TypedLiteral#getDataType() type} (<code>null</code> value fist
     *  </ol>
     * <li> BNode 
     *  <ol>
     *      <li> sorted by their {@link System#identityHashCode(Object) Object hasCode}
     *      <li> on hasCode conflicts (same hasCode but not equals) a random order is chosen
     *      and kept in the parsed conflictsMap
     *  </ol> 
     * </ol>
     * <b>NOTEs</b><ul>
     * <li> parsed {@link Resource} are not required to correctly implement 
     * {@link Object#hashCode() hashCode} and {@link Object#equals(Object) equals}
     * <li> parsed {@link UriRef} and {@link BNode} and {@link Literal} MUST NOT
     * extend/implement any of the other classes/interfaces. This means that an
     * {@link UriRef} MUST NOT implement {@link BNode} nor {@link Literal}
     * <li> parsed {@link Literal}s MAY implement {@link PlainLiteral} AND
     * {@link TypedLiteral}. This allows wrappers over frameworks that do not
     * distinguish between those two literal types to be used with the
     * {@link IndexedTripleCollection}.
     * </ul>
     * 
     * @param a the first resource to compare
     * @param b the second resource to compare
     * @param confictsMap the map used to resolve BNodes with hasCode conflicts 
     * @return 
     */
    protected static int compare(Resource a, Resource b, Map<Integer,List<Resource>> confictsMap){
        //Handle special cases for MAX and MIN values
        if(a == MIN || b == MAX) {
            return -1 ;
        } else if(a == MAX || b == MIN){
            return 1;
        }
        //sort (0) UriRefs < (1) Literals (PlainLiterals & TypedLiterals) < (3) BNodes
        int at = a instanceof UriRef ? 0 : a instanceof Literal ? 1 : 2;
        int bt = b instanceof UriRef ? 0 : b instanceof Literal ? 1 : 2;
        if(at == bt){ //same type sort the different types
            if(at < 2){ //no BNode
                //sort in alphabetic order of the string representation
                String as = at == 0 ? ((UriRef)a).getUnicodeString() :
                    ((Literal)a).getLexicalForm();
                String bs = bt == 0 ? ((UriRef)b).getUnicodeString() :
                    ((Literal)b).getLexicalForm();
                int sc = as.compareTo(bs);
                if(sc == 0 && at == 1){ //same string value and Literals
                    //check if the language and types are the same
                    Language al = a instanceof PlainLiteral ? ((PlainLiteral)a).getLanguage() : null;
                    Language bl = b instanceof PlainLiteral ? ((PlainLiteral)b).getLanguage() : null;
                    //first try to sort by language
                    if(al == null){
                        sc = bl == null ? 0 : -1;
                    } else if(bl == null){
                        sc = 1;
                    } else {
                        sc = al.toString().compareTo(bl.toString());
                    }
                    if(sc == 0){
                        //if still equals look at the dataType
                        UriRef adt = a instanceof TypedLiteral ? ((TypedLiteral)a).getDataType() : null;
                        UriRef bdt = b instanceof TypedLiteral ? ((TypedLiteral)b).getDataType() : null;
                        if(adt == null){
                            sc = bdt == null ? 0 : -1;
                        } else if(bdt == null){
                            sc = 1;
                        } else {
                            sc = adt.getUnicodeString().compareTo(bdt.getUnicodeString());
                        }
                    }
                    return sc;
                } else { //for UriRefs return the string compare
                    return sc;
                }
            } else { //handle BNodes
                //sort BNodes based on hashCode
                int ah = System.identityHashCode(a);
                int bh = System.identityHashCode(b);
                if(ah == bh){
                    if(!a.equals(b)){
                        return resolveBNodeHashConflict(a, b, confictsMap);
                    } else { //same hash and equals
                        return 0;
                    }
                } else { //sort by hash
                    return ah < bh ? -1 : 1;
                }
            }
        } else {
            return at < bt ? -1 : 1;
        }
    }
    
}

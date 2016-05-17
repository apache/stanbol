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
package org.apache.stanbol.enhancer.nlp.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.enhancer.nlp.model.impl.SectionImpl;
import org.apache.stanbol.enhancer.nlp.model.impl.SpanImpl;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.NoSuchPartException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.SubstituteLoggerFactory;

import com.ibm.icu.lang.UCharacter.SentenceBreak;

public final class AnalysedTextUtils {

    /**
     * Restrict instantiation
     */
    private AnalysedTextUtils() {}

    private static final Logger log = LoggerFactory.getLogger(AnalysedTextUtils.class);

    /**
     * Getter for the {@link AnalysedText} content part of the parsed
     * ContentItem.<p>
     * This assumes that the AnalysedText is registered by using
     * {@link AnalysedText#ANALYSED_TEXT_URI}. Otherwise it will not find it.
     * @param ci The {@link ContentItem}
     * @return the {@link AnalysedText} or <code>null</code> if not present.
     * @throws ClassCastException if a content part is registered with
     * {@link AnalysedText#ANALYSED_TEXT_URI} but its type is not compatible
     * to {@link AnalysedText}.
     */
    public static AnalysedText getAnalysedText(ContentItem ci){
        ci.getLock().readLock().lock();
        try {
            return ci.getPart(AnalysedText.ANALYSED_TEXT_URI, AnalysedText.class);
        } catch (NoSuchPartException e) {
            return null;
        } finally {
            ci.getLock().readLock().unlock();
        }
    }

    /**
     * Copies the elements of the parsed iterator to a list.
     * @param iterator the iterator
     * @return the List with all spans of the Iterators
     */
    public static <T extends Span> List<T> asList(Iterator<T> it){
        if(it == null || !it.hasNext()){
            return Collections.emptyList();
        } else {
            List<T> spans = new ArrayList<T>();
            appandToList(it, spans);
            return spans;
        }
    }
    /**
     * Appends the elements provided by the parsed Iterator to the list.
     * @param it the Iterator
     * @param list the List
     * @throws NullPointerException if the parsed List is <code>null</code>
     */
    public static <T extends Span> void appandToList(Iterator<T> it, List<? super T> list){
        if(it != null){
            while(it.hasNext()){
                list.add(it.next());
            }
        }
    }
    
    /**
     * Copies the elements of the parsed iterator(s) to a {@link SortedSet}. As
     * {@link Span} implements {@link Comparable} the Spans within the resulting
     * set will have the same order as returned by the methods of {@link AnalysedText}
     * @param it the iterator(s)
     * @return the {@link SortedSet} containing all Spans of the iterators
     */
    public static <T extends Span> SortedSet<T> asSet(Iterator<T> it){
        SortedSet<T> spans = new TreeSet<T>();
        addToSet(it, spans);
        return spans;
    }
    /**
     * Adds the Spans of the parsed Iterator to the parsed Set
     * @param it the Iterator
     * @param set the set
     * @throws NullPointerException if the parsed List is <code>null</code>
     */
    public static <T extends Span> void addToSet(Iterator<T> it,Set<? super T> set){
        if(it != null){
            while(it.hasNext()){
                set.add(it.next());
            }
        }
    }
    /**
     * Iterates over two levels of the Span hierarchy (e.g. all Tokens of a
     * Sentence that are within a Chunk). The returned Iterator is a live
     * view on the {@link AnalysedText} (being the context of the enclosing
     * Span).<p>
     * Usage Example
     * <code><pre>
     *     Sentence sentence; //The currently processed Sentence
     *     Iterator&lt;Span&gt; tokens = AnalysedTextUtils.getSpansInSpans(
     *         sentence,
     *         {@link SpanTypeEnum#Chunk SpanTypeEnum.Chunk}
     *         {@link SpanTypeEnum#Token SpanTypeEnum.Token}
     *     while(tokens.hasNext()){
     *         Token token = (Token)tokens.next();
     *         // process only tokens within a chunk
     *     }
     * </pre></code>
     * @param section 
     * @param level1 the {@link SpanTypeEnum} for the first Level. MUST be
     * a Type that is a {@link Section} (e.g. Chunk or Sentence).
     * @param level2
     * @return
     * @throws IllegalArgumentException if {@link SpanTypeEnum#Token} is parsed
     * as <code>level1</code> span type.
     */
    public static Iterator<Span> getSpansInSpans(Section section, SpanTypeEnum level1, final SpanTypeEnum level2){
        if(level1 == SpanTypeEnum.Token){
            throw new IllegalArgumentException("The SpanType for level1 MUST refer to a Section "
                + "(Chunk, Sentence, TextSection or Text)");
        }
        final Iterator<Span> level1It = section.getEnclosed(EnumSet.of(level1));
        return new Iterator<Span>(){
            Iterator<Span> level2It = null;
            @Override
            public boolean hasNext() {
                if(level2It != null && level2It.hasNext()) {
                    return true;
                } else {
                    while(level1It.hasNext()){
                        level2It = ((Section)level1It.next()).getEnclosed(EnumSet.of(level2));
                        if(level2It.hasNext()){
                            return true;
                        }
                    }
                }
                return false;
            }

            @Override
            public Span next() {
                hasNext(); //ensure hasNext is called on multiple calls to next()
                return level2It.next();
            }

            @Override
            public void remove() {
                level2It.remove();
            }
        };
    }
// NOTE: No longer used ... keep for now in case that we need this functionality.
//    public static Set<Span> getEnclosed(SortedSet<Span> sortedSet, Span span){
//        if(span.getType() == SpanTypeEnum.Token){
//            log.warn("Span {} with SpanType {} parsed to getEnclosing(..). Returned Set will " 
//                    + "contain the parsed span!");
//        }
//        return sortedSet.subSet(new SubSetHelperSpan(span.getStart(), span.getEnd()), 
//            new SubSetHelperSpan(span.getEnd()));
//    }
//    public static <T> Map<Span,T> getEnclosed(SortedMap<Span,T> sortedSet, Span span){
//        if(span.getType() == SpanTypeEnum.Token){
//            log.warn("Span {} with SpanType {} parsed to getEnclosing(..). Returned Set will " 
//                    + "contain the parsed span!");
//        }
//        return sortedSet.subMap(new SubSetHelperSpan(span.getStart(), span.getEnd()), 
//            new SubSetHelperSpan(span.getEnd()));
//    }
//    
//    /**
//     * Internal helper class used for building {@link SortedSet#subSet(Object, Object)}.
//     * 
//     * @author Rupert Westenthaler
//     *
//     */
//    private static class SubSetHelperSpan extends SpanImpl implements Span {
//        /**
//         * Create the start constraint for {@link SortedSet#subSet(Object, Object)}
//         * @param start
//         * @param end
//         */
//        protected SubSetHelperSpan(int start,int end){
//            super(SpanTypeEnum.Text, //lowest pos type
//                start,end);
//        }
//        /**
//         * Creates the end constraint for {@link SortedSet#subSet(Object, Object)}
//         * @param pos
//         */
//        protected SubSetHelperSpan(int pos){
//            super(SpanTypeEnum.Token, //highest pos type,
//                pos,Integer.MAX_VALUE);
//        }
//    }
}

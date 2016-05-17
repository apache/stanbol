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
package org.apache.stanbol.enhancer.engines.nlp2rdf.engine;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.commons.io.IOUtils;
import org.apache.stanbol.enhancer.nlp.NlpAnnotations;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Chunk;
import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.SpanTypeEnum;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.annotation.Annotated;
import org.apache.stanbol.enhancer.nlp.model.annotation.Annotation;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.ner.NerTag;
import org.apache.stanbol.enhancer.nlp.nif.Nif20;
import org.apache.stanbol.enhancer.nlp.phrase.PhraseTag;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.Pos;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;

public final class Nif20Helper {
    
    private static final LiteralFactory lf = LiteralFactory.getInstance();

    private Nif20Helper(){}
    
    public static final Map<SpanTypeEnum,IRI> SPAN_TYPE_TO_SSO_TYPE;
    static {
        Map<SpanTypeEnum,IRI> mapping = new EnumMap<SpanTypeEnum,IRI>(SpanTypeEnum.class);
        //mapping.put(SpanTypeEnum.Text, null);
        //mapping.put(SpanTypeEnum.TextSection, null);
        mapping.put(SpanTypeEnum.Sentence, Nif20.Sentence.getUri());
        mapping.put(SpanTypeEnum.Chunk, Nif20.Phrase.getUri());
        mapping.put(SpanTypeEnum.Token, Nif20.Word.getUri());
        SPAN_TYPE_TO_SSO_TYPE = Collections.unmodifiableMap(mapping);
    }
    
    /**
     * Read-only map that maps from the {@link LexicalCategory} to the OLIA
     * Concept representing the Phrase (e.g. {@link LexicalCategory#Noun} maps
     * to "<code>http://purl.org/olia/olia.owl#NounPhrase</code>").
     */
    public static final Map<LexicalCategory,IRI> LEXICAL_TYPE_TO_PHRASE_TYPE;
    static {
        String olia = "http://purl.org/olia/olia.owl#";
        Map<LexicalCategory,IRI> mapping = new EnumMap<LexicalCategory,IRI>(LexicalCategory.class);
        mapping.put(LexicalCategory.Noun, new IRI(olia+"NounPhrase"));
        mapping.put(LexicalCategory.Verb, new IRI(olia+"VerbPhrase"));
        mapping.put(LexicalCategory.Adjective, new IRI(olia+"AdjectivePhrase"));
        mapping.put(LexicalCategory.Adverb, new IRI(olia+"AdverbPhrase"));
        mapping.put(LexicalCategory.Conjuction, new IRI(olia+"ConjuctionPhrase"));
        LEXICAL_TYPE_TO_PHRASE_TYPE = Collections.unmodifiableMap(mapping);
    }    
    /**
     * Creates a NIF2.0 Fragment URI using the parsed base URI and the start/end
     * indexes.
     * @param base the base URI
     * @param start the start position. If <code>&lt; 0</code> than zero is added.
     * @param end the end position or values &lt; 1 when open ended.
     * @return the NIF 2.0 Fragment URI
     * @throws IllegalArgumentException if <code>null</code> is parsed as base
     * {@link IRI} or the end position is &gt;=0 but &lt= the parsed start
     * position.
     */
    public static final IRI getNifFragmentURI(IRI base, int start,int end){
        if(base == null){
            throw new IllegalArgumentException("Base URI MUST NOT be NULL!");
        }
        StringBuilder sb = new StringBuilder(base.getUnicodeString());
        sb.append("#char=");
        sb.append(start >= 0 ? start : 0).append(',');
        if(end >= 0){
            if(end < start){
                throw new IllegalArgumentException("End index '"+end+"' < start '"+start+"'!");
            }
            sb.append(end);
        } //else open ended ...
        return new IRI(sb.toString());
    }
 
    public static final IRI getNifRFC5147URI(IRI base, int start, int end){
        if(base == null){
            throw new IllegalArgumentException("Base URI MUST NOT be NULL!");
        }
        assert start >= 0;
        assert end < 0 || end >= start;
        StringBuilder sb = new StringBuilder(base.getUnicodeString());
        sb.append("#char=");
        sb.append(start >= 0 ? start : 0);
        if(end >= 0){
            sb.append(',').append(end);
        } //else select the whole string ...
        return new IRI(sb.toString());
    }
    
    public static final int NIF_HASH_CONTEXT_LENGTH = 10;
    public static final int NIF_HASH_MAX_STRING_LENGTH = 20;
    
    public static final Charset UTF8 = Charset.forName("UTF8");
    
    public static final IRI getNifHashURI(IRI base, int start, int end, String text){
        if(base == null){
            throw new IllegalArgumentException("Base URI MUST NOT be NULL!");
        }
        start = start < 0 ? 0 : start;
        end = end < 0 ? start : end;
        if(end < start){
            throw new IllegalArgumentException("End index '"+end+"' < start '"+start+"'!");
        }
        if(end >= text.length()){
            throw new IllegalArgumentException("The End index '"+end+"' exeeds the "
                + "length of the text '"+text.length()+"'!");
        }
        int contextStart = Math.max(0, start-NIF_HASH_CONTEXT_LENGTH);
        int contextEnd = Math.min(text.length(), end+NIF_HASH_CONTEXT_LENGTH);
        StringBuilder sb = new StringBuilder(base.getUnicodeString());
        sb.append("#hash_");
        sb.append(NIF_HASH_CONTEXT_LENGTH);
        sb.append('_');
        sb.append(end-start);
        sb.append('_');
        sb.append(getContextDigest(text, contextStart, start, end, contextEnd));
        sb.append('_');
        sb.append(text.substring(start, 
            Math.min(end,start+NIF_HASH_MAX_STRING_LENGTH)));
        return new IRI(sb.toString());
    }

    /**
     * Creates the UTF8 byte representation for the '{prefix}({selected}){suffix}'
     * calculated based on the parsed parameters
     * @param text the text
     * @param contextStart the start index of the prefix
     * @param start the start index of the selected text part
     * @param end the end index of the selecte text part
     * @param contextEnd the end index of the suffix
     * @return the HASH string representation of the MD5 over 
     *  <code>'{prefix}({selected}){suffix}'</code> (NOTE the brackets that are
     *  added at the start/end of the selected text)
     */
    private static String getContextDigest(String text, int contextStart, int start, int end, int contextEnd) {
        ByteArrayOutputStream contextOs = new ByteArrayOutputStream();
        Writer contextWriter = new OutputStreamWriter(contextOs, UTF8);
        try {
            if(contextStart<start){
                contextWriter.append(text, contextStart, start);
            }
            contextWriter.append('(');
            if(start < end){
                contextWriter.append(text, start, end);
            }
            contextWriter.append(')');
            if(end < contextEnd){
                contextWriter.append(text,end,contextEnd);
            }
            contextWriter.flush();
            return ContentItemHelper.streamDigest(
                new ByteArrayInputStream(contextOs.toByteArray()),
                null, "MD5");
        } catch (IOException e) {
            //NO IOExceptions in in-memory stream implementations
            throw new IllegalStateException(e);
        } finally {
            IOUtils.closeQuietly(contextOs);
        }
    }

    
    /**
     * Writes the {@link NlpAnnotations#POS_ANNOTATION} as NIF 1.0 to the parsed
     * RDF graph by using the parsed segmentUri as subject
     * @param graph the graph
     * @param annotated the annotated element (e.g. a {@link Token})
     * @param segmentUri the URI of the resource representing the parsed 
     * annotated element in the graph
     */
    public static void writePos(Graph graph, Annotated annotated, IRI segmentUri) {
        Value<PosTag> posTag = annotated.getAnnotation(NlpAnnotations.POS_ANNOTATION);
        if(posTag != null){
            if(posTag.value().isMapped()){
                for(Pos pos : posTag.value().getPos()){
                    graph.add(new TripleImpl(segmentUri, Nif20.oliaCategory.getUri(), 
                        pos.getUri()));
                }
                for(LexicalCategory cat : posTag.value().getCategories()){
                    graph.add(new TripleImpl(segmentUri, Nif20.oliaCategory.getUri(), 
                        cat.getUri()));
                }
            }
            graph.add(new TripleImpl(segmentUri, Nif20.posTag.getUri(), 
                lf.createTypedLiteral(posTag.value().getTag())));
            //set the oliaConf
            //remove existing conf values (e.g. for a single word phrase)
            setOliaConf(graph, segmentUri, posTag);
        }
    }
    /**
     * Sets the {@link Nif20#oliaConf} value. Note this also deletes existing
     * values. This mans that in the case of multiple Olia annotation (e.g. 
     * single word phrases together with word level annotation) the last
     * confidence will win (still better as having two confidence values)
     * @param graph
     * @param segmentUri
     * @param value
     */
    private static void setOliaConf(Graph graph, IRI segmentUri,
            Value<?> value) {
        Iterator<Triple> existingConfValues = graph.filter(segmentUri, Nif20.oliaConf.getUri(), null);
        while(existingConfValues.hasNext()){
            existingConfValues.next();
            existingConfValues.remove();
        }
        if(value.probability() != Value.UNKNOWN_PROBABILITY){
            graph.add(new TripleImpl(segmentUri, Nif20.oliaConf.getUri(), 
                lf.createTypedLiteral(value.probability())));
        }
    }    
    
    /**
     * Writes a {@link NlpAnnotations#PHRASE_ANNOTATION} as NIF 1.0 to the
     * parsed RDF graph by using the segmentUri as subject
     * @param graph the graph
     * @param annotated the annotated element (e.g. a {@link Chunk})
     * @param segmentUri the URI of the resource representing the parsed 
     * annotated element in the graph
     */
    public static void writePhrase(Graph graph, Annotated annotated, IRI segmentUri) {
        Value<PhraseTag> phraseTag = annotated.getAnnotation(NlpAnnotations.PHRASE_ANNOTATION);
        if(phraseTag != null){
            IRI phraseTypeUri = LEXICAL_TYPE_TO_PHRASE_TYPE.get(phraseTag.value().getCategory());
            if(phraseTypeUri != null){ //add the oliaLink for the Phrase
                graph.add(new TripleImpl(segmentUri, Nif20.oliaCategory.getUri(), phraseTypeUri));
                setOliaConf(graph, segmentUri, phraseTag);
            }
        }
    }

}

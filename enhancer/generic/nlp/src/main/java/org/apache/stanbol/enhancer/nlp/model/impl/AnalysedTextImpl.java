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
package org.apache.stanbol.enhancer.nlp.model.impl;

import java.util.Iterator;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Chunk;
import org.apache.stanbol.enhancer.nlp.model.Sentence;
import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.SpanTypeEnum;
import org.apache.stanbol.enhancer.servicesapi.Blob;

/**
 * The Class added as ContentPart to the contentItem
 * @author westei
 *
 */
public class AnalysedTextImpl extends SectionImpl implements AnalysedText {

        
    private final Blob blob;
    /**
     * The analysed text
     */
    private String text;
    
    protected NavigableMap<Span,Span> spans = new TreeMap<Span,Span>();
    
    public AnalysedTextImpl(Blob blob, String text){
        super(SpanTypeEnum.Text,0,text.length());
        this.setContext(this); //the the context to itself
        this.blob = blob;
        this.text = text;
    }
    
    @Override
    public SpanTypeEnum getType() {
        return SpanTypeEnum.Text;
    }
    
    /* (non-Javadoc)
     * @see org.apache.stanbol.enhancer.nlp.model.impl.AnalyzedText#addSentence(int, int)
     */
    @Override
    public SentenceImpl addSentence(int start, int end){
        return register(new SentenceImpl(context, this, start, end));
    }
    
    /* (non-Javadoc)
     * @see org.apache.stanbol.enhancer.nlp.model.impl.AnalyzedText#getSentences()
     */
    @Override
    public Iterator<Sentence> getSentences(){
        return filter(Sentence.class);
    }
        
    /* (non-Javadoc)
     * @see org.apache.stanbol.enhancer.nlp.model.impl.Sentence#addChunk(int, int)
     */
    @Override
    public ChunkImpl addChunk(int start, int end){
        return register(new ChunkImpl(context, this, start, end));
    }
    
    /* (non-Javadoc)
     * @see org.apache.stanbol.enhancer.nlp.model.impl.Sentence#getChunks()
     */
    @Override
    public Iterator<Chunk> getChunks(){
        return filter(Chunk.class);
    }
    /**
     * Reference to the Blob those data got analysed
     * @return
     */
    public final Blob getAnalysedBlob(){
        return blob;
    }

    @Override
    public CharSequence getText() {
        return text;
    }

    @Override
    public Blob getBlob() {
        return blob;
    }
}

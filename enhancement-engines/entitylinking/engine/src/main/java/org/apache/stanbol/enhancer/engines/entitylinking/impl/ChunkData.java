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
package org.apache.stanbol.enhancer.engines.entitylinking.impl;

import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.PHRASE_ANNOTATION;

import org.apache.stanbol.enhancer.engines.entitylinking.config.LanguageProcessingConfig;
import org.apache.stanbol.enhancer.nlp.NlpAnnotations;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Chunk;
import org.apache.stanbol.enhancer.nlp.model.Sentence;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.phrase.PhraseTag;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;

/** 
 * Represents a Chunk (group of tokens) used as context for EntityLinking.
 * Typically a single {@link ChunkData#chunk} is used, but in case of
 * overlapping and {@link ChunkData#isProcessable processable} chunks
 * multiple {@link Chunk}s might be merged to a single {@link ChunkData}
 * instance. In such cases {@link ChunkData#chunk} represents the
 * first and {@link ChunkData#merged} the last of the merged chunks.<p>
 * {@link ChunkData#startToken} and {@link ChunkData#endToken} represent
 * the covered [start,end) {@link Token} indices relative to the current
 * sections (typically a {@link Sentence}). {@link ChunkData#getStartChar()}
 * and {@link ChunkData#getEndChar()} are the absolute [start,end) character
 * indices within the {@link AnalysedText#getSpan()}
 */
public class ChunkData {
    protected final static boolean DEFAULT_PROCESSABLE_STATE = true;
    /** if this Chunk represents a Named Entity **/
    protected final boolean isNamedEntity;
    /** if the Chunk is processable */
    public final boolean isProcessable;
    /** the Chunk */
    public final Chunk chunk;
    /** the start token index relative to the current section (sentence) */
    int startToken;
    /** the end token index relative to the current section (sentence) */
    int endToken;
    /**
     * If this chunk has a linkable token
     */
    boolean hasLinkable = false;
    /**
     * The number of matchable Tokens enclosed by this Chunk
     */
    int matchableCount;
    /**
     * The start position of the first matchable {@link Token} within this
     * chunk
     */
    int matchableStart = -1;
    /**
     * The start char offset of the first matchable {@link Token} within this chunk
     */
    int matchableStartCharIndex = -1;
    /**
     * The end position of the last matchable {@link Token} within this chunk
     */
    int matchableEnd = -1;
    /**
     * The end char offset of the last matchable {@link Token} within this chunk
     */
    int matchableEndCharIndex = -1;
    /**
     * constructs and initializes the meta data for the parsed {@link Chunk}
     * @param chunk
     */
    public ChunkData(LanguageProcessingConfig tpc, Chunk chunk){
        this.chunk = chunk;
        Boolean process = null;
        for (Value<PhraseTag> phraseAnnotation : chunk.getAnnotations(PHRASE_ANNOTATION)) {
            if (tpc.getProcessedPhraseCategories().contains(phraseAnnotation.value().getCategory())
                || tpc.getProcessedPhraseTags().contains(phraseAnnotation.value().getTag())) {
                if (phraseAnnotation.probability() == Value.UNKNOWN_PROBABILITY ||
                        phraseAnnotation.probability() >= tpc.getMinPhraseAnnotationProbability()) {
                    process = true;
                    break;
                } // else probability to low for inclusion
            } else if (phraseAnnotation.probability() == Value.UNKNOWN_PROBABILITY ||
                    phraseAnnotation.probability() >= tpc.getMinExcludePhraseAnnotationProbability()) {
                process = false;
                break;
            } // else probability to low for exclusion
        }
        //fallback for NER chunks in case Noun Phrases are processible and a NER
        //annotation is present for the parsed chunk.
        isNamedEntity = chunk.getAnnotation(NlpAnnotations.NER_ANNOTATION) != null;
        if(process == null && isNamedEntity &&
        		tpc.getProcessedPhraseCategories().contains(LexicalCategory.Noun)){
        	process = true;
        }
        isProcessable = process == null ? DEFAULT_PROCESSABLE_STATE : process;
    }
    /**
     * Getter for the start character position
     * @return the start character position of the selected text span.
     */
    public int getStartChar(){
        return chunk.getStart();
    }
    /**
     * Getter for the end character position of the text
     * @return the end character position
     */
    public int getEndChar(){
        return chunk.getEnd();
    }
    /**
     * If this chunk is processable
     * @return the state
     */
    public boolean isProcessable() {
        return isProcessable;
    }
    
    public boolean isNamedEntity() {
    	return isNamedEntity;
    }
    /**
     * If this chunk covers a linkable token
     * @return
     */
    public boolean hasLinkable(){
        return hasLinkable;
    }
    
    /**
     * Getter for the number of matchable tokens contained in this chunk
     * @return The number of matchable tokens contained in this chunk
     */
    public int getMatchableCount() {
        return matchableCount;
    }
    public int getStartTokenIndex() {
        return startToken;
    }
    public int getEndTokenIndex() {
        return endToken;
    }
    /**
     * The index of the first matchable Token within the {@link Chunk} or
     * <code>-1</code> if none
     * @return
     */
    public int getMatchableStart() {
        return matchableStart;
    }
    /**
     * The index of the last matchable Token within the {@link Chunk} or
     * <code>-1</code> if none
     * @return
     */
    public int getMatchableEnd() {
        return matchableEnd;
    }
    /**
     * The char index of the start character of the first matchable {@link Token}
     * within the {@link Chunk} or <code>-1</code> if none.
     * @return
     */
    public int getMatchableStartChar() {
        return matchableStartCharIndex;
    }
    /**
     * the char indes of the end character of the last matchable {@link Token}
     * within the {@link Chunk} or <code>-1</code> if none
     * @return
     */
    public int getMatchableEndChar() {
        return matchableEndCharIndex;
    }
    
}
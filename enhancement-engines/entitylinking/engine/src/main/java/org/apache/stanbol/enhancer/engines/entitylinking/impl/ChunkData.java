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
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Chunk;
import org.apache.stanbol.enhancer.nlp.model.Sentence;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.phrase.PhraseTag;

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
    /** if the Chunk is processable */
    public final boolean isProcessable;
    /** the Chunk */
    public final Chunk chunk;
    /** 
     * In case multiple overlapping and processable {@link Chunk}s the
     * section selected by the chunks are merged. While {@link #chunk}
     * holds the original chunk (the first) this variable holds the
     * last merged one. Enclosed chunks (in case more than two are
     * merged) are not available via this class, but can be retrieved
     * by iterating over the {@link AnalysedText} content part.
     */
    Chunk merged;
    /** the start token index relative to the current section (sentence) */
    int startToken;
    /** the end token index relative to the current section (sentence) */
    int endToken;
    /**
     * The number of matchable Tokens enclosed by this Chunk
     */
    int matchableCount;
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
     * Getter for the end character position of the text selected by
     * possible multiple {@link #merged} chunks.
     * @return the end character position considering possible {@link #merged}
     * chunks.
     */
    public int getEndChar(){
        return merged == null ? chunk.getEnd() : merged.getEnd();
    }
    /**
     * If this chunk is processable
     * @return the state
     */
    public boolean isProcessable() {
        return isProcessable;
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
}
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
/**
 * 
 */
package org.apache.stanbol.enhancer.engines.keywordextraction.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.stanbol.commons.opennlp.TextAnalyzer.AnalysedText;
import org.apache.stanbol.commons.opennlp.TextAnalyzer.AnalysedText.Chunk;
import org.apache.stanbol.commons.opennlp.TextAnalyzer.AnalysedText.Token;

public class ProcessingState {

    private final Iterator<AnalysedText> sentences;
    /**
     * The sentence currently processed
     */
    private AnalysedText sentence;
    /**
     * The index of the current token needed to be linked
     */
    private int tokenIndex = -1;
    /**
     * The current token
     */
    private Token token;
    /**
     * The iterator over the chunks of the current {@link #sentence}  
     * or <code>null</code> if no {@link Chunk}s are available.
     */
    private Iterator<Chunk> chunks;
    /**
     * The current {@link Chunk}
     */
    private Chunk chunk;
    private static final int MAX_TEXT_CACHE_SIZE = 32;
    /**
     * This is a cache over the last {@link #MAX_TEXT_CACHE_SIZE} token texts
     * requested by {@link #getTokenText(int, int)}
     */
    private Map<String,String> textCache = new LinkedHashMap<String,String>(
            MAX_TEXT_CACHE_SIZE, 0.75f, true){
        private static final long serialVersionUID = 1L;
        protected boolean removeEldestEntry(Map.Entry<String,String> eldest) {
            return size() > MAX_TEXT_CACHE_SIZE;
        };
    };
    /**
     * The position for the next token
     */
    private int nextToken = -1;
    /**
     * The position of the last consumed position
     */
    private int consumedIndex = -1;

    public ProcessingState(Iterator<AnalysedText> sentences){
        this.sentences = sentences;
        if(!sentences.hasNext()){
            throw new IllegalArgumentException("The parsed AnalysedContent MUST NOT have an empty AnalysedText iterator!");
        }
    }
    /**
     * Getter for the current Sentence
     * @return the sentence
     */
    public final AnalysedText getSentence() {
        return sentence;
    }
    /**
     * Getter for the index of the current active token within the current
     * active {@link #getSentence() sentence}
     * @return the tokenPos the index of the token
     */
    public final int getTokenIndex() {
        return tokenIndex;
    }
    /**
     * Getter for the last consumed index
     * @return the index of the last consumed token
     */
    public final int getConsumedIndex() {
        return consumedIndex;
    }
    /**
     * The currently active token
     * @return the token
     */
    public final Token getToken() {
        return token;
    }
    /**
     * Getter for the language of the current Token (based on the current
     * sentence)
     * @return the language
     */
    public final String getLanguage() {
        return sentence.getLanguage();
    }
    /**
     * The currently active chunk or <code>null</code> if no chunks are
     * available. If chunks are present this can not be <code>null</code>
     * because {@link Token}s outside of chunks are skiped.
     * @return the chunk the current {@link Chunk} or <code>null</code> if
     * no chunks are present.
     */
    public final Chunk getChunk() {
        return chunk;
    }
    /**
     * Getter for the next {@link Token} to be processed. Calling {@link #next()}
     * is guaranteed to skip all tokens in between {@link #getTokenIndex()}
     * and {@link #getNextToken()}, but it might even skip more tokens (e.g.
     * in case that the token referenced by {@link #getNextToken()} is not
     * within a {@link Chunk}
     * @return the nextToken
     */
    public final int getNextToken() {
        return nextToken;
    }
//    /**
//     * Allows to manually set to position of the next token to process.
//     * This can be used to skip some tokens within (e.g. if a Concept
//     * matching multiple Tokens where found.<p>
//     * The set token may be greater than the number of tokens in 
//     * {@link #sentence}. This will simple cause the next sentence to be
//     * activated on the next call to {@link #next()}
//     * @param pos the position of the next token to process. 
//     */
//    public void setNextToken(int pos){
//        if(pos > tokenIndex){
//            this.nextToken = pos;
//        } else {
//            throw new IllegalArgumentException("The nextTokenPos "+pos+
//                " MUST BE greater than the current "+tokenIndex);
//        }
//    }
    /**
     * The index of an consumed Token. The consumed index MUST BE equals or
     * greater as {@link #getTokenIndex()}. If the consumed index is set to a
     * value greater that {@link #getTokenIndex()} than consumed tokens are
     * skipped on the next call to {@link #next()}
     * @param pos the position of the last consumed token.
     */
    public void setConsumed(int pos){
        if(pos >= tokenIndex){
            this.consumedIndex = pos;
            this.nextToken = pos+1;
        } else {
            throw new IllegalArgumentException("The lastConsumedPos "+pos+
                " MUST BE equals or gerater than the current Pos "+tokenIndex);
        }
    }
    /**
     * Moves the state to #nextToken this may switch to the next Chunk or
     * sentence.
     * @return <code>true</code> if there are further elements to process or
     * <code>false</code> if there are no further elements to process.
     */
    public boolean next() {
        //switch to the next token
        if(nextToken > tokenIndex){
            tokenIndex = nextToken;
        } else {
            tokenIndex++;
            nextToken = tokenIndex;
        }
        //now init the next element
        final boolean hasNext;
        if(chunk != null){ //if chunks are present
            //get next chunk (may be the current if chunk.getEnd() > tokenPos
            for(;tokenIndex > chunk.getEnd() && chunks.hasNext();chunk = chunks.next());
            if(tokenIndex <= chunk.getEnd()){ //found valid chunk
                if(chunk.getStart() > tokenIndex) { //skip tokens outside chunks
                    tokenIndex = chunk.getStart();
                }
                if(chunk.getStart() > consumedIndex){
                    consumedIndex = chunk.getStart()-1;
                }
                hasNext = true;
            } else { //no more valid chunks in this sentence
                hasNext = initNextSentence();
            }
        } else { //no chunks ... use tokens only
            if(sentence == null){ //first sentence
                hasNext = initNextSentence();
            } else if(tokenIndex >= sentence.getTokens().size()){
                hasNext = initNextSentence();
            } else { //more tokens in the sentence
                //set the token
                hasNext = true;
            }
        }
        if(hasNext){ //set the Token
            token = sentence.getTokens().get(tokenIndex);
        }
        return hasNext;
    }

    /**
     * Correctly initialise {@link #sentence}, {@link #chunks}, {@link #chunk}
     * and {@link #tokenIndex} for the next element of {@link #sentences}. If
     * no further sentences are to process it simple sets {@link #sentence}, 
     * {@link #chunks}, {@link #chunk} and {@link #tokenIndex} to <code>null</code>
     */
    private boolean initNextSentence() {
        textCache.clear();
        sentence = null;
        while(sentence == null && sentences.hasNext()){
            sentence = sentences.next();
            if(sentence.getChunks() != null){
                chunks = sentence.getChunks().iterator();
                if(chunks.hasNext()){
                    chunk = chunks.next();
                    tokenIndex = chunk.getStart();
                    consumedIndex = tokenIndex-1;
                    nextToken = tokenIndex;
                } else { //no chunks in this sentence
                    sentence = null; //skip this sentence
                }
            } else {
                if(sentence.getTokens().isEmpty()){ //no tokens in this sentence
                    sentence = null; //skip this one
                } else {
                    chunks = null;
                    chunk = null;
                    tokenIndex = 0;
                    consumedIndex = -1;
                    nextToken = 0;
                }
            }
        }
        return sentence != null;
    }
    /**
     * Getter for the text covered by the next tokenCount tokens relative to
     * {@link #token}. It uses the {@link #textCache} to lookup/store such texts.
     * Given the Tokens
     * <pre>
     *    [This, is, an, Example]
     * </pre>
     * and the parameter <code>3</code> this method will return
     * <pre>
     *     This is an
     * </pre>
     * @param tokenCount the number of tokens to be included relative to 
     * {@link #tokenIndex}
     * @return the text covered by the span start of {@link #token} to end of
     * token at <code>{@link #tokenIndex}+tokenCount</code>.
     */
    public String getTokenText(int start, int tokenCount){
        String pos = start+","+tokenCount;
        String text = textCache.get(pos);
        if(text == null){
            text = sentence.getText().substring(
                sentence.getTokens().get(start).getStart(),
                sentence.getTokens().get(start+tokenCount-1).getEnd());
            textCache.put(pos, text);
        }
        return text;
    }
    @Override
    public String toString() {
        return "["+tokenIndex+","+token+"] chunk: " +
            (chunk == null?null:chunk.getText())+"| sentence: "+
            (sentence == null?null:sentence.getText());
    }
}
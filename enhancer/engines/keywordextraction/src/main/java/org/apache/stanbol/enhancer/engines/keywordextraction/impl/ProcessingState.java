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

import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.PHRASE_ANNOTATION;
import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.POS_ANNOTATION;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import org.apache.stanbol.enhancer.engines.keywordextraction.linking.EntityLinkerConfig;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.TextProcessingConfig;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Chunk;
import org.apache.stanbol.enhancer.nlp.model.Section;
import org.apache.stanbol.enhancer.nlp.model.Sentence;
import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.Span.SpanTypeEnum;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.phrase.PhraseTag;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessingState {

    private final Logger log = LoggerFactory.getLogger(ProcessingState.class);
    
    /**
     * Iterator over the sentences (might be
     * the whole {@link AnalysedText} if no sentences are
     * defined).
     */
    private final Iterator<? extends Section> sections;
    /**
     * The sentence currently processed
     */
    private Section section;
//    /**
//     * The index of the current token needed to be linked
//     */
//    private int tokenIndex = -1;
    /**
     * Holds the {@link Token}s of the current {@link #sentence} 
     * to allow fast index based access.
     */
    private List<TokenData> tokens = new ArrayList<TokenData>(64);
    
    @SuppressWarnings("unchecked")
    private Iterator<TokenData> processableTokensIterator = Collections.EMPTY_LIST.iterator();
    
    private final EnumSet<SpanTypeEnum> enclosedSpanTypes;
    /**
     * The current token
     */
    private TokenData token;
//    /**
//     * The position for the next token
//     */
//    private int nextToken = -1;
    /**
     * The position of the last consumed position
     */
    private int consumedIndex = -1;
    /**
     * The language of the text
     */
    private String language;

    private final TextProcessingConfig tpc;
    private final EntityLinkerConfig elc;

    public ProcessingState(AnalysedText at, String language, TextProcessingConfig tpc, EntityLinkerConfig elc){
        if(at == null){
            throw new IllegalArgumentException("The parsed AnalysedText MUST NOT be NULL!");
        }
        if(language == null || language.isEmpty()){
            throw new IllegalArgumentException("The parsed Language MUST NOT be NULL nor empty!");
        }
        if(tpc == null){
            throw new IllegalArgumentException("The parsed TextProcessingConfig MUST NOT be NULL!");
        }
        if(elc == null){
            throw new IllegalArgumentException("The parsed EntityLinkerConfig MUST NOT be NULL!");
        }
        this.tpc = tpc;
        this.elc = elc;
        enclosedSpanTypes = EnumSet.of(SpanTypeEnum.Token);
        
        if(!tpc.isIgnoreChunks()){
            enclosedSpanTypes.add(SpanTypeEnum.Chunk);
        }
        
        this.language = language;
        //prefer to iterate over sentences
        Iterator<Sentence> sentences = at.getSentences();
        this.sections = sentences.hasNext() ? sentences : Collections.singleton(at).iterator();
        //init the first sentence
        initNextSentence();
    }
    /**
     * Getter for the current Sentence
     * @return the sentence
     */
    public final Section getSentence() {
        return section;
    }
//    /**
//     * Getter for the index of the current active token within the current
//     * active {@link #getSentence() sentence}
//     * @return the tokenPos the index of the token
//     */
//    public final int getTokenIndex() {
//        return tokenIndex;
//    }
    
    public TokenData getToken(){
        return token;
    }
    
    public List<TokenData> getTokens(){
        return tokens;
    }
    
    /**
     * Getter for the last consumed index
     * @return the index of the last consumed token
     */
    public final int getConsumedIndex() {
        return consumedIndex;
    }

    
    /**
     * Getter for the language of the current Token (based on the current
     * sentence)
     * @return the language
     */
    public final String getLanguage() {
        return language;
    }
//    /**
//     * Getter for the next {@link Token} to be processed. Calling {@link #next()}
//     * is guaranteed to skip all tokens in between {@link #getTokenIndex()}
//     * and {@link #getNextToken()}, but it might even skip more tokens (e.g.
//     * in case that the token referenced by {@link #getNextToken()} is not
//     * within a {@link Chunk}
//     * @return the nextToken
//     */
//    public final int getNextToken() {
//        return nextToken;
//    }

    /**
     * The index of an consumed Token. The consumed index MUST BE equals or
     * greater as {@link #getTokenIndex()}. If the consumed index is set to a
     * value greater that {@link #getTokenIndex()} than consumed tokens are
     * skipped on the next call to {@link #next()}
     * @param pos the position of the last consumed token.
     */
    public void setConsumed(int pos){
        if(pos >= token.index){
            this.consumedIndex = pos;
//            this.nextToken = pos+1;
        } else {
            throw new IllegalArgumentException("The lastConsumedPos "+pos+
                " MUST BE equals or gerater than the current Pos "+token.index);
        }
    }
    
    /**
     * Moves the state to next processable token after the index #nextToken
     * @return <code>true</code> if there are further elements to process or
     * <code>false</code> if there are no further elements to process.
     */
    public boolean next() {
        //switch to the next token
//        if(nextToken > tokenIndex){
//            tokenIndex = nextToken;
//        } else {
//            tokenIndex++;
//            nextToken = tokenIndex;
//        }
        while(processableTokensIterator.hasNext() || initNextSentence()){
            TokenData token = processableTokensIterator.next();
            if(token.index > consumedIndex){
//                tokenIndex = token.index;
                this.token = token;
                return true;
            }
        }
        return false;
    }

    /**
     * Correctly initialise {@link #sentence}, {@link #chunks}, {@link #chunk}
     * and {@link #tokenIndex} for the next element of {@link #sections}. If
     * no further sentences are to process it simple sets {@link #sentence}, 
     * {@link #chunks}, {@link #chunk} and {@link #tokenIndex} to <code>null</code>
     */
    private boolean initNextSentence() {
        section = null;
        tokens.clear();
        processableTokensIterator = null;
        consumedIndex = -1;
        List<TokenData> processableTokens = new ArrayList<TokenData>(64);
        while(processableTokens.isEmpty() && sections.hasNext()){
            section = sections.next();
            Iterator<Span> enclosed = section.getEnclosed(enclosedSpanTypes);
            ChunkData activeChunk = null;
            while(enclosed.hasNext()){
                Span span = enclosed.next();
                if(span.getType() == SpanTypeEnum.Chunk){
                    if(isProcesableChunk((Chunk)span)){
                        if(activeChunk != null){ //write the end position of the previous Chunk
                            //TODO: maybe we need to support overlapping chunks ...
                            //for now just close the previous if the next starts
                            log.warn(" ... Overlapping processible Chunks {} <-> {}",
                                activeChunk.chunk,span);
                            if(activeChunk.chunk.getEnd() < span.getEnd()){
                                activeChunk.endToken = tokens.size();
                                activeChunk = null;
                            } //else the current encloses this one
                        }
                        if(activeChunk == null){
                            activeChunk = new ChunkData((Chunk)span);
                            activeChunk.startToken = tokens.size();
                        } // else ignore chunkes enclosed by others
                    } //else ignore chunks that are not processed
                } else if(span.getType() == SpanTypeEnum.Token){
                    TokenData tokenData = new TokenData(tokens.size(),(Token)span,activeChunk);
                    if(log.isDebugEnabled()){
                        log.debug(" >> Token {}: {} (pos:{}) chunk: {}",
                            new Object[]{tokenData.index,tokenData.token, 
                                         tokenData.token.getAnnotations(POS_ANNOTATION),
                                         tokenData.inChunk != null ? 
                                                 (tokenData.inChunk.chunk + " "+ tokenData.inChunk.chunk.getSpan()) : 
                                                     "none"});
                    }
                    tokens.add(tokenData);
                    if(tokenData.isProcessable){
                        processableTokens.add(tokenData);
                    }
                    if(activeChunk != null && span.getEnd() >= activeChunk.chunk.getEnd()){
                        //this is the last token in the current chunk
                        activeChunk.endToken = tokens.size()-1;
                        activeChunk = null;
                    }
                }
            }
            if(activeChunk != null) { //close the last chunk (if not done)
                activeChunk.endToken = tokens.size()-1;
            }
        }
        processableTokensIterator = processableTokens.iterator();
        return processableTokensIterator.hasNext();
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
        int offset = section.getStart();
        return section.getSpan().substring(
            tokens.get(start).token.getStart()-offset,
            tokens.get(start+(tokenCount-1)).token.getEnd()-offset);
    }
    
    /**
     * Checks if the parsed {@link Token} is processable. This
     * decision is taken first based on the POS annotation (
     * Lexical Category, POS tag)and second on the 
     * {@link EntityLinkerConfig#getMinSearchTokenLength()}
     * if no POS annotations are available or the probability of
     * the POS annotations is to low.<p>
     * Since STANBOL-685two POS Probabilities are used<ul>
     * <li> {@link TextProcessingConfig#getMinPosAnnotationProbability()} for
     * accepting POS tags that are processed - included in 
     * {@link TextProcessingConfig#getProcessedLexicalCategories()} or
     * {@link TextProcessingConfig#getProcessedPosTags()}.
     * <li> {@link TextProcessingConfig#getMinExcludePosAnnotationProbability()}
     * for those that are not processed. By default the exclusion probability
     * is set to half of the inclusion one.
     * </ul>
     * Assuming that the <code>minPosTypePropb=0.667</code> a<ul>
     * <li> noun with the prop 0.8 would result in returning <code>true</code>
     * <li> noun with prop 0.5 would return <code>null</code>
     * <li> verb with prop 0.4 would return <code>false</code>
     * <li> verb with prop 0.3 would return <code>null</code>
     * </ul>
     * This algorithm makes it less likely that the  
     * {@link EntityLinkerConfig#getMinSearchTokenLength()} needs to 
     * be used as fallback for Tokens (what typically still provides better
     * estimations as the token length).<p>
     * (see also STANBOL-685 even that this Issue refers a version of this
     * Engine that has not yet used the Stanbol NLP processing chain)
     * @param token the {@link Token} to check.
     * @return <code>true</code> if the parsed token needs to be processed.
     * Otherwise <code>false</code>
     */
    protected boolean isProcessableToken(Token token) {
        for(Value<PosTag> posAnnotation : token.getAnnotations(POS_ANNOTATION)){
            // check three possible match
            //  1. the LexicalCategory matches
            //  2. the Pos matches
            //  3. the String tag matches
            PosTag posTag = posAnnotation.value();
//            log.debug("   ... check PosAnntation {} (lc:{}|pos:{}|tag:{}",
//                new Object[]{posAnnotation,posTag.getCategories(),
//                             posTag.getPosHierarch(),posTag.getTag()});
            if((!Collections.disjoint(tpc.getProcessedLexicalCategories(), 
                    posTag.getCategories())) ||
                (!Collections.disjoint(tpc.getProcessedPos(),
                    posTag.getPosHierarchy())) ||
                tpc.getProcessedPosTags().contains(
                    posTag.getTag())){
                if(posAnnotation.probability() >= tpc.getMinPosAnnotationProbability()){
                    return true;
                } // else probability to low for inclusion
            } else if(posAnnotation.probability() >= tpc.getMinExcludePosAnnotationProbability()){
                return false;
            } // else probability to low for exclusion
        }
        return token.getSpan().length() >= elc.getMinSearchTokenLength();
    }
    
    protected boolean isMatchableToken(Token token){
        for(Value<PosTag> posAnnotation : token.getAnnotations(POS_ANNOTATION)){
            PosTag posTag = posAnnotation.value();
            if(posTag.isMapped()){
                if(!Collections.disjoint(tpc.getMatchableLexicalCategories(), 
                    posTag.getCategories())){
                    if(posAnnotation.probability() >= tpc.getMinPosAnnotationProbability()){
                        return true;
                    } // else probability to low for inclusion
                } else if(posAnnotation.probability() >= tpc.getMinExcludePosAnnotationProbability()){
                    return false;
                } // else probability to low for exclusion
            } //else not matched ... search next one
        }
        return token.getSpan().length() >= elc.getMinSearchTokenLength();        
    }
    
    
    protected boolean isProcesableChunk(Chunk chunk){
        for(Value<PhraseTag> phraseAnnotation : chunk.getAnnotations(PHRASE_ANNOTATION)){
            if(tpc.getProcessedPhraseCategories().contains(
                phraseAnnotation.value().getCategory()) ||
                tpc.getProcessedPhraseTags().contains(
                    phraseAnnotation.value().getTag())){
                if(phraseAnnotation.probability() >= tpc.getMinPhraseAnnotationProbability()){
                    return true;
                } // else probability to low for inclusion
            } else if(phraseAnnotation.probability() >= tpc.getMinExcludePhraseAnnotationProbability()){
                return false;
            } // else probability to low for exclusion
        }
        //neither a clear accept/reject ...
        return true;
    }
    
    @Override
    public String toString() {
        return "["+token.index+","+token.token+"] chunk: " +
            (token.inChunk == null?"none":token.inChunk.chunk)+"| sentence: "+
            (section == null?null:section.getSpan());
    }
    
    /**
     * Internally used to store additional Metadata for Tokens of the current Sentence
     */
    class TokenData {
        
        final Token token;
        final int index;
        final boolean isProcessable;
        final boolean isMatchable;
        final boolean hasAlphaNumeric;
        final ChunkData inChunk;

        TokenData(int index,Token token, ChunkData chunk) {
            this.token = token;
            this.index = index;
            this.isProcessable = isProcessableToken(token);
            this.isMatchable = isMatchableToken(token);
            this.hasAlphaNumeric = Utils.hasAlphaNumericChar(token.getSpan());
            this.inChunk = chunk;
        }
    }

    class ChunkData {
        
        ChunkData(Chunk chunk){
            this.chunk = chunk;
        }
        final Chunk chunk;
        int startToken;
        int endToken;
    }
    
}
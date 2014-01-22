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
package org.apache.stanbol.enhancer.engines.entitylinking.impl;

import static org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig.UNICASE_SCRIPT_LANUAGES;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.iterators.FilterIterator;
import org.apache.stanbol.enhancer.engines.entitylinking.config.LanguageProcessingConfig;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Section;
import org.apache.stanbol.enhancer.nlp.model.Sentence;
import org.apache.stanbol.enhancer.nlp.model.SpanTypeEnum;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.pos.Pos;
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
    /**
     * The position of the last consumed position
     */
    private int consumedIndex = -1;
    /**
     * Ensures that Tokens are not processed twice in case of multiple
     * overlapping Sentence Annotations (e.g. if two NLP frameworks contributing
     * Sentences do not agree with each other).
     */
    private int consumedSectionIndex = -1;
    /**
     * The language of the text
     */
    private String language;
    
    protected final LanguageProcessingConfig tpc;
    //protected final EntityLinkerConfig elc;

    //private AnalysedText at;
    /**
     * If the language uses a unicase script and therefore upper case specific
     * processing rules can not be used (see STANBOL-1049)
     */
    private boolean isUnicaseLanguage;

    private static final Predicate PROCESSABLE_TOKEN_OREDICATE = new Predicate() {
        @Override
        public boolean evaluate(Object object) {
            return ((TokenData)object).isLinkable;
        }
    };

    public static final Collection<Pos> SUB_SENTENCE_START_POS = EnumSet.of(
        Pos.Quote);
    
    public ProcessingState(AnalysedText at, String language, LanguageProcessingConfig tpc){
        if(at == null){
            throw new IllegalArgumentException("The parsed AnalysedText MUST NOT be NULL!");
        }
        if(language == null || language.isEmpty()){
            throw new IllegalArgumentException("The parsed Language MUST NOT be NULL nor empty!");
        }
        if(tpc == null){
            throw new IllegalArgumentException("The parsed TextProcessingConfig MUST NOT be NULL!");
        }
        this.tpc = tpc;
        enclosedSpanTypes = EnumSet.of(SpanTypeEnum.Token);
        
        if(!tpc.isIgnoreChunks()){
            enclosedSpanTypes.add(SpanTypeEnum.Chunk);
        }
        //this.at = at; //store as field (just used for logging)
        this.language = language;
        //STANBOL-1049: we need now to know if a language uses a unicase script
        //ensure lower case and only use the language part 
        String lookupLang = language.toLowerCase(Locale.ROOT).split("[_-]")[0];
        this.isUnicaseLanguage = UNICASE_SCRIPT_LANUAGES.contains(lookupLang);
        //prefer to iterate over sentences
        Iterator<Sentence> sentences = at.getSentences();
        this.sections = sentences.hasNext() ? sentences : Collections.singleton(at).iterator();
        //init the first sentence
        //initNextSentence();
    }
    /**
     * Getter for the current section. This is typically a {@link Sentence}
     * but might also be the whole {@link AnalysedText} in case no sentence
     * annotations are available
     * @return the currently processed {@link Section}
     */
    public final Section getSentence() {
        return section;
    }
    /**
     * Getter for the current token
     * @return the token for the currently processed word
     */
    public TokenData getToken(){
        return token;
    }
    /**
     * Getter for the Tokens of the currently processed section
     * @return the Tokens of the currently processed section
     */
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
        while(processableTokensIterator.hasNext() || initNextSentence()){
            TokenData token = processableTokensIterator.next();
            if(token.index > consumedIndex){
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
        processableTokensIterator = null;
        consumedIndex = -1;
        boolean foundLinkableToken = false;
        while(!foundLinkableToken && sections.hasNext()){
            section = sections.next();
            if(consumedSectionIndex > section.getStart()){
                log.debug(" > skipping {} because an other section until Index {} " +
                		"was already processed. This is not an error, but indicates that" +
                		"multiple NLP framewords do contribute divergating Sentence annotations",
                		section, consumedSectionIndex);
                continue; //ignore this section
            }
            consumedSectionIndex = section.getEnd();
            SectionData sectionData = new SectionData(tpc, section, enclosedSpanTypes, isUnicaseLanguage);
            //TODO: It would be better to use a SectionData field instead
            tokens = sectionData.getTokens();
            section = sectionData.section;
            foundLinkableToken = sectionData.hasLinkableToken();
        }
        processableTokensIterator = new FilterIterator(tokens.iterator(), PROCESSABLE_TOKEN_OREDICATE);
        return foundLinkableToken;
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
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(token.index).append(',').append(token.token);
        sb.append("] chunk: ");
        if(token.inChunk == null){
            sb.append("none");
        } else {
            sb.append(token.inChunk.chunk);
        }
        sb.append("| sentence: ");
        if(section == null){
            sb.append("none");
        } else if(section.getSpan().length() > 45){
            sb.append(section.getSpan().substring(0, 45)).append(" ...");
        } else {
            sb.append(section.getSpan());
        }
        return sb.toString();
    }
    
}
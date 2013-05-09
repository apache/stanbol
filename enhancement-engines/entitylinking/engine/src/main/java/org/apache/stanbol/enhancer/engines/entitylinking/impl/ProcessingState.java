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

import static java.util.Collections.disjoint;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig.UNICASE_SCRIPT_LANUAGES;
import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.PHRASE_ANNOTATION;
import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.POS_ANNOTATION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.iterators.FilterIterator;
import org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.config.LanguageProcessingConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig;
import org.apache.stanbol.enhancer.nlp.NlpAnnotations;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Chunk;
import org.apache.stanbol.enhancer.nlp.model.Section;
import org.apache.stanbol.enhancer.nlp.model.Sentence;
import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.Span.SpanTypeEnum;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.morpho.MorphoFeatures;
import org.apache.stanbol.enhancer.nlp.phrase.PhraseTag;
import org.apache.stanbol.enhancer.nlp.pos.Pos;
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
     * The language of the text
     */
    private String language;
    
    protected final LanguageProcessingConfig tpc;
    //protected final EntityLinkerConfig elc;

    private AnalysedText at;
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
        this.at = at; //store as field (just used for logging)
        this.language = language;
        //STANBOL-1049: we need now to know if a language uses a unicase script
        //ensure lower case and only use the language part 
        String lookupLang = language.toLowerCase(Locale.ROOT).split("[_-]")[0];
        this.isUnicaseLanguage = UNICASE_SCRIPT_LANUAGES.contains(lookupLang);
        //prefer to iterate over sentences
        Iterator<Sentence> sentences = at.getSentences();
        this.sections = sentences.hasNext() ? sentences : Collections.singleton(at).iterator();
        //init the first sentence
        initNextSentence();
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
            tokens.clear(); //clear token for each section (STANBOL-818)
            Iterator<Span> enclosed = section.getEnclosed(enclosedSpanTypes);
            ChunkData activeChunk = null;
            while(enclosed.hasNext()){
                Span span = enclosed.next();
                if(span.getStart() >= span.getEnd()){ //save guard against empty spans
                    log.warn("Detected Empty Span {} in section {} of Blob {}",
                        new Object[]{span,section, at.getBlob()});
                }
                if(span.getType() == SpanTypeEnum.Chunk){
                    ChunkData chunkData = new ChunkData((Chunk)span);
                    if(chunkData.isProcessable()){
                        if(activeChunk != null){ //current Chunk not yet closed -> overlapping chunks!
                            if(activeChunk.getEndChar() < span.getEnd()){ //merge partly overlapping chunks
                                log.info("   - merge overlapping and processable Chunks {} <-> {}",
                                    activeChunk.merged == null? activeChunk.chunk : activeChunk.merged,span);
                                activeChunk.merged = (Chunk)span; //set this one as last merged
                            } //ignore completely covered chunks
                        } else { // a new Chunk starts
                            activeChunk = chunkData;
                            activeChunk.startToken = tokens.size();
                            if(log.isDebugEnabled()){
                                log.debug(">> Chunk: (type:{}, startPos: {}) text: '{}'",
                                    new Object []{
                                        activeChunk.chunk.getType(),
                                        activeChunk.startToken,
                                        activeChunk.chunk.getSpan()
                                    });
                            }
                        } 
                    } //else ignore chunks that are not processable
                } else if(span.getType() == SpanTypeEnum.Token){
                    TokenData tokenData = new TokenData(tokens.size(),(Token)span,activeChunk);
                    if(log.isDebugEnabled()){
                        log.debug("  > {}: {} {}(pos:{}) chunk: '{}'",
                            new Object[]{tokenData.index,tokenData.token,
                                tokenData.morpho != null ? ("(lemma: "+tokenData.morpho.getLemma()+") ") : "",
                                tokenData.token.getAnnotations(POS_ANNOTATION),
                                tokenData.inChunk != null ? tokenData.inChunk.chunk.getSpan() : "none"});
                    }
                    if(!tokenData.hasAlphaNumeric){
                        tokenData.isLinkable = false;
                        tokenData.isMatchable = false;
                    } else {
                        // (1) apply basic rules for linkable/processable tokens
                        //determine if the token should be linked/matched
                        tokenData.isLinkable = tokenData.isLinkablePos != null ? tokenData.isLinkablePos : false;
                        //matchabel := linkable OR has matchablePos
                        tokenData.isMatchable = tokenData.isLinkable || 
                                (tokenData.isMatchablePos != null && tokenData.isMatchablePos);
                        
                        //(2) for non linkable tokens check for upper case rules
                        if(!tokenData.isLinkable && tokenData.upperCase && 
                                tokenData.index > 0 && //not a sentence or sub-sentence start
                                !tokens.get(tokenData.index-1).isSubSentenceStart){
                            //We have an upper case token!
                            if(tpc.isLinkUpperCaseTokens()){
                                if(tokenData.isMatchable) { //convert matchable to 
                                    tokenData.isLinkable = true; //linkable
                                    tokenData.isMatchable = true;
                                } else { // and other tokens to
                                    tokenData.isMatchable = true; //matchable
                                }
                            } else { 
                                //finally we need to convert other Tokens to matchable
                                //if MatchUpperCaseTokens is active
                                if(!tokenData.isMatchable && tpc.isMatchUpperCaseTokens()){
                                    tokenData.isMatchable = true;
                                }
                            }
                        } //else not an upper case token
                        
                        //(3) Unknown POS tag Rules (see STANBOL-1049)
                        if(!tokenData.isLinkable && (tokenData.isLinkablePos == null || 
                                tokenData.isMatchablePos == null)){
                            if(isUnicaseLanguage || !tpc.isLinkOnlyUpperCaseTokensWithUnknownPos()){
                                if(tokenData.isLinkablePos == null && tokenData.hasSearchableLength){
                                    tokenData.isLinkable = true;
                                    tokenData.isMatchable = true;
                                } //else no need to change the state
                            } else { //non unicase language and link only upper case tokens enabled
                                if(tokenData.upperCase && // upper case token
                                        tokenData.index > 0 && //not a sentence or sub-sentence start
                                        !tokens.get(tokenData.index-1).isSubSentenceStart){
                                    if(tokenData.hasSearchableLength && tokenData.isLinkablePos == null){
                                        tokenData.isLinkable = true;
                                        tokenData.isMatchable = true;
                                    } else if(tokenData.isMatchablePos == null){
                                        tokenData.isMatchable = true;
                                    }
                                } else if(tokenData.hasSearchableLength &&  //lower case and long token
                                        tokenData.isMatchablePos == null){ 
                                    tokenData.isMatchable = true;
                                } //else lower case and short word 
                            }
                        } //else already linkable or POS tag present
                    }
                    log.debug("    - {}",tokenData); 
                    //add the token to the list
                    tokens.add(tokenData);
                    if(!foundLinkableToken){
                        foundLinkableToken = tokenData.isLinkable;
                    }
                    if(activeChunk != null){
                        if (tokenData.isLinkable){
                            //ignore matchableCount in Chunks with linkable Tokens
                            activeChunk.matchableCount = -10; //by setting the count to -10
                        } else if(tokenData.isMatchable){
                            activeChunk.matchableCount++;
                        }
                        if (span.getEnd() >= activeChunk.getEndChar()){
                            //this is the last token in the current chunk
                            activeChunk.endToken = tokens.size()-1;
                            log.debug("   - end Chunk@pos: {}", activeChunk.endToken);
                            if(tpc.isLinkMultiMatchableTokensInChunk() && 
                                    activeChunk.getMatchableCount() > 1 ){
                                log.debug("   - multi-matchable Chunk:");
                                //mark the last of two immediate following matchable
                                //tokens as processable
                                for(int i = activeChunk.endToken-1;i >= activeChunk.startToken+1;i--){
                                    TokenData ct = tokens.get(i);
                                    TokenData pt = tokens.get(i-1);
                                    if(ct.isMatchable && pt.isMatchable){
                                        if(!ct.isLinkable) { //if not already processable
                                            log.debug("     > convert Token {}: {} (pos:{}) from matchable to processable",
                                                new Object[]{i,ct.token.getSpan(),ct.token.getAnnotations(POS_ANNOTATION)});
                                            ct.isLinkable = true;
                                            if(!foundLinkableToken){
                                                foundLinkableToken = true;
                                            }
                                        }
                                        i--;//mark both (ct & pt) as processed
                                    }
                                }
                            }
                            activeChunk = null;
                        }
                    }
                }
            }
            if(activeChunk != null) { //close the last chunk (if not done)
                activeChunk.endToken = tokens.size()-1;
            }
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
    
//    /**

//     */
//    protected boolean getProcessablePosTag(Token token) {
//        for(Value<PosTag> posAnnotation : token.getAnnotations(POS_ANNOTATION)){
//            // check three possible match
//            //  1. the LexicalCategory matches
//            //  2. the Pos matches
//            //  3. the String tag matches
//            PosTag posTag = posAnnotation.value();
////            log.debug("   ... check PosAnntation {} (lc:{}|pos:{}|tag:{}",
////                new Object[]{posAnnotation,posTag.getCategories(),
////                             posTag.getPosHierarch(),posTag.getTag()});
//            if((!Collections.disjoint(tpc.getProcessedLexicalCategories(), 
//                    posTag.getCategories())) ||
//                (!Collections.disjoint(tpc.getProcessedPos(),
//                    posTag.getPosHierarchy())) ||
//                tpc.getProcessedPosTags().contains(
//                    posTag.getTag())){
//                if(posAnnotation.probability() >= tpc.getMinPosAnnotationProbability()){
//                    return true;
//                } // else probability to low for inclusion
//            } else if(posAnnotation.probability() >= tpc.getMinExcludePosAnnotationProbability()){
//                return false;
//            } // else probability to low for exclusion
//        }
//        return token.getSpan().length() >= elc.getMinSearchTokenLength();
//    }

// Both
//    protected boolean isMatchableToken(Token token){
//        for(Value<PosTag> posAnnotation : token.getAnnotations(POS_ANNOTATION)){
//            PosTag posTag = posAnnotation.value();
//            if(posTag.isMapped()){
//                if(!Collections.disjoint(tpc.getMatchableLexicalCategories(), 
//                    posTag.getCategories())){
//                    if(posAnnotation.probability() >= tpc.getMinPosAnnotationProbability()){
//                        return true;
//                    } // else probability to low for inclusion
//                } else if(posAnnotation.probability() >= tpc.getMinExcludePosAnnotationProbability()){
//                    return false;
//                } // else probability to low for exclusion
//            } //else not matched ... search next one
//        }
//        return token.getSpan().length() >= elc.getMinSearchTokenLength();        
//    }
//    
//    
//    protected boolean isProcesableChunk(Chunk chunk){
//        for(Value<PhraseTag> phraseAnnotation : chunk.getAnnotations(PHRASE_ANNOTATION)){
//            if(tpc.getProcessedPhraseCategories().contains(
//                phraseAnnotation.value().getCategory()) ||
//                tpc.getProcessedPhraseTags().contains(
//                    phraseAnnotation.value().getTag())){
//                if(phraseAnnotation.probability() >= tpc.getMinPhraseAnnotationProbability()){
//                    return true;
//                } // else probability to low for inclusion
//            } else if(phraseAnnotation.probability() >= tpc.getMinExcludePhraseAnnotationProbability()){
//                return false;
//            } // else probability to low for exclusion
//        }
//        //neither a clear accept/reject ...
//        return true;
//    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(token.index).append(',').append(token.token);
        sb.append("] chunk: ");
        if(token.inChunk == null){
            sb.append("none");
        } else {
            sb.append(token.inChunk.chunk);
            if(token.inChunk.merged != null){
                sb.append("(merged with ").append(token.inChunk.merged).append(')');
            }
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
    
    /**
     * Internally used to store additional Metadata for Tokens of the current Sentence
     * <p>
     * Checks if the parsed {@link Token} is processable. This decision is taken first based on the POS
     * annotation ( Lexical Category, POS tag) and second on the
     * {@link EntityLinkerConfig#getMinSearchTokenLength()} if no POS annotations are available or the
     * probability of the POS annotations is to low.
     * <p>
     * Since STANBOL-685two POS Probabilities are used <ul>
     * <li> {@link LanguageProcessingConfig#getMinPosAnnotationProbability()} for accepting POS tags that are
     * processed - included in {@link LanguageProcessingConfig#getLinkedLexicalCategories()} or
     * {@link LanguageProcessingConfig#getLinkedPosTags()}.
     * <li> {@link LanguageProcessingConfig#getMinExcludePosAnnotationProbability()} for those that are not
     * processed. By default the exclusion probability is set to half of the inclusion one.
     * </ul>
     * Assuming that the <code>minPosTypePropb=0.667</code> a
     * <ul>
     * <li>noun with the prop 0.8 would result in returning <code>true</code>
     * <li>noun with prop 0.5 would return <code>null</code>
     * <li>verb with prop 0.4 would return <code>false</code>
     * <li>verb with prop 0.3 would return <code>null</code>
     * </ul>
     * This algorithm makes it less likely that the {@link EntityLinkerConfig#getMinSearchTokenLength()} needs
     * to be used as fallback for Tokens (what typically still provides better estimations as the token
     * length).
     * <p>
     * (see also STANBOL-685 even that this Issue refers a version of this Engine that has not yet used the
     * Stanbol NLP processing chain)
     * 
     * @param token
     *            the {@link Token} to check.
     * @return <code>true</code> if the parsed token needs to be processed. Otherwise <code>false</code>
     */
    public class TokenData {
        /** The Token */
        public final Token token;
        /** The index of the Token within the current Section (Sentence) */
        public final int index;
        /** If this Token should be linked with the Vocabulary */
        public boolean isLinkable;
        /** If this Token should be used for multi word searches in the Vocabulary */
        public boolean isMatchable;
        /** if this Token has an alpha or numeric char */
        public final boolean hasAlphaNumeric;
        /** the chunk of this Token */
        public final ChunkData inChunk;
        /** the morphological features of the Token (selected based on the POS Tag) */
        public final MorphoFeatures morpho;
        /**
         * if this token starts with an upperCase letter
         */
        public final boolean upperCase;
        /**
         * if the length of the token is &gt;= {@link LanguageProcessingConfig#getMinSearchTokenLength()}
         */
        public boolean hasSearchableLength;
        /**
         * If the POS type of this word matches a linkable category
         */
        public final Boolean isLinkablePos;
        /**
         * if the POS type of this word matches a matchable category
         */
        public final Boolean isMatchablePos;
        /**
         * if this Token represents the start of an sub-sentence such as an 
         * starting ending quote 
         * @see ProcessingState#SUB_SENTENCE_START_POS
         */
        public final boolean isSubSentenceStart;
        /**
         * Constructs and initializes meta data needed for linking based 
         * on the current tokens (and its NLP annotation)
         * @param index the index of the Token within the current section
         * @param token the token
         * @param chunk the current chunk or <code>null</code> if none
         */
        TokenData(int index,Token token, ChunkData chunk) {
            //(0) init fields
            this.token = token;
            this.index = index;
            this.inChunk = chunk;
            this.hasAlphaNumeric = Utils.hasAlphaNumericChar(token.getSpan());
            this.hasSearchableLength = token.getSpan().length() >= tpc.getMinSearchTokenLength();
            PosTag selectedPosTag = null;
            boolean matchedPosTag = false; //matched any of the POS annotations
            
            //(1) check if this Token should be linked against the Vocabulary (isProcessable)
            upperCase = token.getEnd() > token.getStart() && //not an empty token
                    Character.isUpperCase(token.getSpan().codePointAt(0)); //and upper case
            boolean isLinkablePos = false;
            boolean isMatchablePos = false;
            boolean isSubSentenceStart = false;
            List<Value<PosTag>> posAnnotations = token.getAnnotations(POS_ANNOTATION);
            for(Value<PosTag> posAnnotation : posAnnotations){
                // check three possible match
                //  1. the LexicalCategory matches
                //  2. the Pos matches
                //  3. the String tag matches
                PosTag posTag = posAnnotation.value();
                if((!disjoint(tpc.getLinkedLexicalCategories(), posTag.getCategories())) ||
                        (!disjoint(tpc.getLinkedPos(), posTag.getPosHierarchy())) ||
                        tpc.getLinkedPosTags().contains(posTag.getTag())){
                    if(posAnnotation.probability() == Value.UNKNOWN_PROBABILITY ||
                            posAnnotation.probability() >= tpc.getMinPosAnnotationProbability()){
                        selectedPosTag = posTag;
                        isLinkablePos = true;
                        isMatchablePos = true;
                        matchedPosTag = true;
                        break;
                    } // else probability to low for inclusion
                } else if(posAnnotation.probability() == Value.UNKNOWN_PROBABILITY ||
                        posAnnotation.probability() >= tpc.getMinExcludePosAnnotationProbability()){
                    selectedPosTag = posTag; //also rejected PosTags are selected
                    matchedPosTag = true;
                    isLinkablePos = false;
                    break;
                } // else probability to low for exclusion
            }
            if(!matchedPosTag) { //not matched against a POS Tag ...
                this.isLinkablePos = null;
            } else {
                this.isLinkablePos = isLinkablePos;
            }
            
            //(2) check if this token should be considered to match labels of suggestions
            if(this.isLinkablePos != null && this.isLinkablePos){ //processable tokens are also matchable
                this.isMatchablePos = true;
            } else { //check POS and length to see if token is matchable
                matchedPosTag = false; //reset to false!
                for(Value<PosTag> posAnnotation : posAnnotations){
                    PosTag posTag = posAnnotation.value();
                    if(posTag.isMapped()){
                        if(!Collections.disjoint(tpc.getMatchedLexicalCategories(), 
                            posTag.getCategories())){
                            if(posAnnotation.probability() == Value.UNKNOWN_PROBABILITY ||
                                    posAnnotation.probability() >= tpc.getMinPosAnnotationProbability()){
                                //override selectedPosTag if present
                                selectedPosTag = posTag; //mark the matchable as selected PosTag
                                isMatchablePos = true;
                                matchedPosTag = true;
                                break;
                            } // else probability to low for inclusion
                        } else if(posAnnotation.probability() == Value.UNKNOWN_PROBABILITY ||
                                posAnnotation.probability() >= tpc.getMinExcludePosAnnotationProbability()){
                            if(selectedPosTag == null){ //do not override existing values
                                selectedPosTag = posTag; //also rejected PosTags are selected
                            }
                            isMatchablePos = false;
                            matchedPosTag = true;
                            break;
                        } // else probability to low for exclusion
                    } //else not matched ... search next one
                }
                if(!matchedPosTag){ //not matched against POS tag ...
                    //fall back to the token length
                    this.isMatchablePos = null;
                    //this.isMatchablePos = token.getSpan().length() >= tpc.getMinSearchTokenLength();    
                } else {
                    this.isMatchablePos = isMatchablePos;
                }
            }
            //(3) check if the POS tag indicates the start/end of an sub-sentence
            for(Value<PosTag> posAnnotation : posAnnotations){
                PosTag posTag = posAnnotation.value();
                if((!disjoint(SUB_SENTENCE_START_POS,posTag.getPosHierarchy()))){
                    if(posAnnotation.probability() == Value.UNKNOWN_PROBABILITY ||
                            posAnnotation.probability() >= tpc.getMinPosAnnotationProbability()){
                        isSubSentenceStart = true;
                    } // else probability to low for inclusion
                } else if(posAnnotation.probability() == Value.UNKNOWN_PROBABILITY ||
                        posAnnotation.probability() >= tpc.getMinExcludePosAnnotationProbability()){
                    isSubSentenceStart = false;
                }
            }
            this.isSubSentenceStart = isSubSentenceStart;
            
            //(4) check for morpho analyses
            if(selectedPosTag == null){ //token is not processable or matchable
                //we need to set the selectedPoas tag to the first POS annotation
                Value<PosTag> posAnnotation = token.getAnnotation(POS_ANNOTATION);
                if(posAnnotation != null) {
                    selectedPosTag = posAnnotation.value();
                }
            }
            List<Value<MorphoFeatures>> morphoAnnotations = token.getAnnotations(NlpAnnotations.MORPHO_ANNOTATION);
            if(selectedPosTag == null){ //no POS information ... use the first morpho annotation
                morpho = morphoAnnotations.isEmpty() ? null : morphoAnnotations.get(0).value();
            } else { //select the correct morpho annotation based on the POS tag
                MorphoFeatures mf = null;
                selectMorphoFeature : 
                for(Value<MorphoFeatures> morphoAnnotation : morphoAnnotations){
                    for(PosTag posTag : morphoAnnotation.value().getPosList()){
                        if(!disjoint(selectedPosTag.getCategories(),posTag.getCategories())){
                            mf = morphoAnnotation.value();
                            break selectMorphoFeature; //stop after finding the first one
                        }
                    }
                }
                morpho = mf;
            }
        }
        
        /**
         * Getter for token text
         * @return the text of the token
         */
        public String getTokenText(){
            return token.getSpan();
        }
        /**
         * Getter for the Lemma of the token. 
         * @return the Lemma of the Token or <code>null</code> if not available
         */
        public String getTokenLemma(){
            return morpho != null ? morpho.getLemma() : null;
        }
        @Override
        public String toString() {
            return new StringBuilder("TokenData: '").append(getTokenText())
                    .append("'[linkable=").append(isLinkable).append("(linkabkePos=").append(isLinkablePos)
                    .append(")| matchable=").append(isMatchable).append("(matchablePos=").append(isMatchablePos)
                    .append(")| alpha=").append(hasAlphaNumeric).append("| seachLength=")
                    .append(hasSearchableLength).append("| upperCase=").append(upperCase)
                    .append("]").toString();
        }  
    }
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
        private Chunk merged;
        /** the start token index relative to the current section (sentence) */
        private int startToken;
        /** the end token index relative to the current section (sentence) */
        private int endToken;
        /**
         * The number of matchable Tokens enclosed by this Chunk
         */
        int matchableCount;
        /**
         * constructs and initializes the meta data for the parsed {@link Chunk}
         * @param chunk
         */
        ChunkData(Chunk chunk){
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
    
}
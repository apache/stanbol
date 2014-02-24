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

import static java.util.Collections.disjoint;
import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.POS_ANNOTATION;

import java.util.Collections;
import java.util.List;

import org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.config.LanguageProcessingConfig;
import org.apache.stanbol.enhancer.nlp.NlpAnnotations;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.morpho.MorphoFeatures;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;

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
    public TokenData(LanguageProcessingConfig tpc, int index,Token token, ChunkData chunk) {
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
                    if((!Collections.disjoint(tpc.getMatchedLexicalCategories(), posTag.getCategories())) ||
                            (!Collections.disjoint(tpc.getMatchedPos(), posTag.getPosHierarchy())) ||
                            tpc.getMatchedPosTags().contains(posTag.getTag())){
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
            if((!disjoint(ProcessingState.SUB_SENTENCE_START_POS,posTag.getPosHierarchy()))){
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
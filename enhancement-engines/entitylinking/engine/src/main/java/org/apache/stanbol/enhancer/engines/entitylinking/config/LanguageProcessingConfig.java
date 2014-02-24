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
package org.apache.stanbol.enhancer.engines.entitylinking.config;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.apache.stanbol.enhancer.engines.entitylinking.EntitySearcher;
import org.apache.stanbol.enhancer.nlp.model.Chunk;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.phrase.PhraseTag;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.Pos;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;

public class LanguageProcessingConfig implements Cloneable{
    
    /**
     * The linked Phrase types. Includes {@link LexicalCategory#Noun} phrases
     */
    public static final Set<LexicalCategory> DEFAULT_PROCESSED_PHRASE_CATEGORIES = 
            EnumSet.of(LexicalCategory.Noun);
        
    /**
     * The default set of {@link LexicalCategory LexicalCategories} used to
     * lookup (link) Entities within the {@link EntitySearcher}
     */
    public static final Set<LexicalCategory> DEFAULT_LINKED_LEXICAL_CATEGORIES = 
            EnumSet.of(LexicalCategory.Noun, LexicalCategory.Residual);

    /**
     * The default set of {@link LexicalCategory LexicalCategories} used to
     * match (and search) for Entities.<p>
     * Matched Tokens are not used for linking, but are considered when matching
     * label tokens of Entities with the Text.
     */
    public static final Set<LexicalCategory> DEFAULT_MATCHED_LEXICAL_CATEGORIES =
            EnumSet.of(LexicalCategory.Noun, LexicalCategory.Quantifier,LexicalCategory.Residual);
    
    /**
     * The default set of {@link Pos} used to match (and search) for Entities <p>
     * Matched Tokens are not used for linking, but are considered when matching
     * label tokens of Entities with the Text.
     */
    public static final Set<Pos> DEFAULT_MATCHED_POS = EnumSet.of(Pos.Gerund);
    
    /**
     * The default set of {@link Pos} types that are used to lookup (link) Entities.
     * By defualt only {@link Pos#ProperNoun}s and two 
     * {@link LexicalCategory#Residual} acronyms and
     * words marked as foreign material.
     */
    public static final Set<Pos> DEFAULT_LINKED_POS = 
            EnumSet.of(Pos.ProperNoun, Pos.Foreign, Pos.Acronym);

    /**
     * Default value for POS annotation confidence required for processed POS tags.
     * Used for <ul>
     * <li> {@link #getLinkedLexicalCategories()}
     * <li> {@link #getLinkedPosTags()} and
     * <li> {@link #getMatchedLexicalCategories()}
     * <ul>
     */
    public static final double DEFAULT_MIN_POS_ANNOTATION_PROBABILITY = 0.75;

    /**
     * Default {@link LexicalCategory LexicalCategories} that allow the EntityLinker 
     * to step-over non matchable tokens when determining search tokens for 
     * Entityhub lookups (Defaults: {@link LexicalCategory#Noun}, 
     * {@link LexicalCategory#Punctuation} and {@link LexicalCategory#Adposition}).
     */
    public static final Set<LexicalCategory> DEFAULT_CHUNKABLE_CATEGORIES = EnumSet.of(
        LexicalCategory.Noun, LexicalCategory.Punctuation, LexicalCategory.Conjuction);
    
    /**
     * Default {@link Pos} tags that allow the EntityLinker to step-over non matchable 
     * tokens when determining search tokens for Entityhub lookups (default: empty).
     */
    private static final Set<Pos> DEFAULT_CHUNKABLE_POS = EnumSet.of(Pos.Preposition);
    /**
     * Default string tags that allow the EntityLinker to step-over non matchable 
     * tokens when determining search tokens for Entityhub lookups (default: empty).
     */
    private static final Set<String> DEFAULT_CHUNKABKE_TAGS = Collections.emptySet();
    
    /**
     * Default value for POS annotation confidence required for not-processed POS tags
     * (not contained in both {@link #getLinkedLexicalCategories()} and 
     * {@link #getLinkedPosTags()}). <br> The default is 
     * <code>{@link #DEFAULT_MIN_POS_ANNOTATION_PROBABILITY}/2</code>
     */
    public static final double DEFAULT_MIN_EXCLUDE_POS_ANNOTATION_PROBABILITY = DEFAULT_MIN_POS_ANNOTATION_PROBABILITY/2;

    /**
     * By default {@link Chunk}s are considered
     */
    public static final boolean DEFAULT_IGNORE_CHUNK_STATE = false;
    /**
     * the minimum probability so that a phrase in processed based on the Phrase Annotation
     */
    public static final double DEFAULT_MIN_PHRASE_ANNOTATION_PROBABILITY = 0.75;
    /**
     * the minimum probability so that a phrase is rejected based on the Phrase Annotation
     */
    public static final double DEFAULT_MIN_EXCLUDE_PHRASE_ANNOTATION_PROBABILITY = 
            DEFAULT_MIN_PHRASE_ANNOTATION_PROBABILITY/2;
    /**
     * The default for linking upper case tokens (regardless of length and POS)
     * The default is <code>false</code> as some languages (like German) use upper
     * case for Nouns and so this would also affect configurations that only
     * link {@link Pos#ProperNoun}s
     */
    public static final boolean DEFAULT_LINK_UPPER_CASE_TOKEN_STATE = false;
    /**
     * The default for matching upper case tokens (regardless of length and POS)
     * is <code>true</code>
     */
    public static final boolean DEFAULT_MATCH_UPPER_CASE_TOKEN_STATE = true;
    /**
     * By default linking of chunks with multiple matchable tokens is enabled.
     * This is useful to link Entities represented by two common nouns.  
     */
    public static final boolean DEFAULT_LINK_MULTIPLE_MATCHABLE_TOKENS_IN_CHUNKS_STATE = true;
    
    /**
     * The set of {@link PosTag#getCategory()} considered for EntityLinking
     * @see #DEFAULT_LINKED_LEXICAL_CATEGORIES
     */
    private Set<LexicalCategory> linkedLexicalCategories = DEFAULT_LINKED_LEXICAL_CATEGORIES;

    private Set<LexicalCategory> matchedLexicalCategories = DEFAULT_MATCHED_LEXICAL_CATEGORIES;

    private Set<Pos> matchedPos = DEFAULT_MATCHED_POS;
    
    private Set<String> matchedPosTags = Collections.emptySet();
    
    /**
     * The linked {@link Pos} categories
     */
    private Set<Pos> linkedPos = DEFAULT_LINKED_POS;
    /**
     * The set of {@link PosTag#getTag()} values that are processed
     */
    private Set<String> linkedPosTags = Collections.emptySet();
    /**
     * The minimum confidence of POS annotations for {@link #getLinkedLexicalCategories()}
     * and {@link #getLinkedPosTags()}
     */
    private double minPosAnnotationProbability = DEFAULT_MIN_POS_ANNOTATION_PROBABILITY;

    /**
     * The minimum confidence that a POS annotation 
     */
    private double minExcludePosAnnotationProbability = DEFAULT_MIN_EXCLUDE_POS_ANNOTATION_PROBABILITY;

    private boolean ignoreChunksState = DEFAULT_IGNORE_CHUNK_STATE;

    private Set<LexicalCategory> chunkableCategories = DEFAULT_CHUNKABLE_CATEGORIES;
    private Set<Pos> chunkablePos = DEFAULT_CHUNKABLE_POS;
    private Set<String> chunkableTags = DEFAULT_CHUNKABKE_TAGS;

    private double minPhraseAnnotationProbability = DEFAULT_MIN_PHRASE_ANNOTATION_PROBABILITY;

    private double minExcludePhraseAnnotationProbability = DEFAULT_MIN_EXCLUDE_PHRASE_ANNOTATION_PROBABILITY;

    private Set<LexicalCategory> processedPhraseCategories = DEFAULT_PROCESSED_PHRASE_CATEGORIES;

    private Set<String> processedPhraseTags = Collections.emptySet();
    /**
     * If upper case tokens are linked (and matched)
     */
    private boolean linkUpperCaseTokensState = DEFAULT_LINK_UPPER_CASE_TOKEN_STATE;
    /**
     * If upper case tokens are matched
     */
    private boolean matchUpperCaseTokensState = DEFAULT_MATCH_UPPER_CASE_TOKEN_STATE;
    /**
     * If for {@link Chunk}s with multiple matchable Tokens those should be
     * linked.
     */
    private boolean linkMultiMatchableTokensInChunkState = DEFAULT_LINK_MULTIPLE_MATCHABLE_TOKENS_IN_CHUNKS_STATE;
    private int minSearchTokenLength;
    private boolean linkOnlyUpperCaseTokenWithUnknownPos;


    /**
     * The language or <code>null</code> for the default configuration
     * @param language
     */
    public LanguageProcessingConfig(){
    }    
    
    public final boolean isIgnoreChunks() {
        return ignoreChunksState;
    }    
    
    /**
     * Setter for the ignore {@link Chunk} state.
     * @param state the state or <code>null</code> to set the 
     * {@link #DEFAULT_IGNORE_CHUNK_STATE}
     */
    public final void setIgnoreChunksState(Boolean state){
        if(state == null){
            this.ignoreChunksState = DEFAULT_IGNORE_CHUNK_STATE;
        } else {
            this.ignoreChunksState = state;
        }
    }
    
    /**
     * Getter for the set of {@link LexicalCategory LexicalCategories} used 
     * to link Entities in the configured Vocabulary.
     * @return the set of {@link LexicalCategory LexicalCategories} used 
     * for linking.
     * @see #DEFAULT_LINKED_LEXICAL_CATEGORIES
     */
    public final Set<LexicalCategory> getLinkedLexicalCategories() {
        return linkedLexicalCategories;
    }
    /**
     * Getter for the set of {@link LexicalCategory LexicalCategories} used
     * to match label tokens of suggested Entities.
     * @return the set of {@link LexicalCategory LexicalCategories} used for
     * matching
     */
    public final Set<LexicalCategory> getMatchedLexicalCategories(){
        return matchedLexicalCategories;
    }
    /**
     * Setter for the matched lexical categories
     * @param matchedLexicalCategories the set or <code>null</code>
     * to set the {@link #DEFAULT_MATCHED_LEXICAL_CATEGORIES}
     */
    public void setMatchedLexicalCategories(Set<LexicalCategory> matchedLexicalCategories) {
        if(matchedLexicalCategories == null){
            this.matchedLexicalCategories = DEFAULT_MATCHED_LEXICAL_CATEGORIES;
        } else {
            this.matchedLexicalCategories = EnumSet.noneOf(LexicalCategory.class);
            this.matchedLexicalCategories.addAll(matchedLexicalCategories);
        }
    }
    /**
     * Getter for the set of {@link Pos} tags used to match label tokens of
     * suggested Entities
     * @return the set of {@link Pos} tags used for matching
     */
    public Set<Pos> getMatchedPos(){
        return matchedPos;
    }
    /**
     * Setter for the matched {@link Pos} tags
     * @param pos the set or <code>null</code>
     * to set the {@link #DEFAULT_MATCHED_POS}
     */
    public void setMatchedPos(Set<Pos> pos) {
        if(pos == null){
            this.matchedPos = DEFAULT_MATCHED_POS;
        } else {
            this.matchedPos = EnumSet.noneOf(Pos.class);
            this.matchedPos.addAll(pos);
        }
    }
    public Set<String> getMatchedPosTags(){
        return matchedPosTags;
    }
    
    public void setMatchedPosTags(Set<String> matchedPosTags){
        if(matchedPosTags == null){
            this.matchedPosTags = Collections.emptySet();
        } else if(matchedPosTags.contains(null)){
            throw new IllegalArgumentException("The parsed set with matched POS tags MUST NOT contain the NULL element!");
        } else {
            this.matchedPosTags = matchedPosTags;
        }

    }
    
    /**
     * The set of tags used for linking. This is useful if the string tags
     * used by the POS tagger are not mapped to {@link LexicalCategory} nor
     * {@link Pos} enum members. 
     * @return the set of pos tags used for linking entities
     */
    public final Set<String> getLinkedPosTags() {
        return linkedPosTags;
    }
    
    /**
     * Getter for the minimum probability of POS annotations for 
     * {@link #getLinkedLexicalCategories()} or {@link #getLinkedPosTags()}
     * @return the probability
     */
    public final double getMinPosAnnotationProbability() {
        return minPosAnnotationProbability ;
    }
    
    
    /**
     * Getter for the minimum probability of POS annotations not included in 
     * {@link #getLinkedLexicalCategories()} or {@link #getLinkedPosTags()}
     * @return the probability
     */
    public final double getMinExcludePosAnnotationProbability() {
        return minExcludePosAnnotationProbability;
    }
    
    /**
     * Setter for the minimum probability of POS annotations for 
     * {@link #getLinkedLexicalCategories()} or {@link #getLinkedPosTags()}
     * @param minPosAnnotationProbability the probability or <code>null</code> to set
     * {@value #DEFAULT_MIN_POS_ANNOTATION_PROBABILITY}
     */
    public final void setMinPosAnnotationProbability(Double minPosAnnotationProbability) {
        if(minPosAnnotationProbability == null){
            this.minPosAnnotationProbability = DEFAULT_MIN_POS_ANNOTATION_PROBABILITY;
        } else if(minPosAnnotationProbability >= 0 && minPosAnnotationProbability <= 1) {
            this.minPosAnnotationProbability = minPosAnnotationProbability;
        } else {
            throw new IllegalArgumentException("parsed value MUST BE in the range 0..1 or NULL to set the default");
        }
    }
    
    /**
     * Setter for the minimum probability of POS annotations not included in 
     * {@link #getLinkedLexicalCategories()} or {@link #getLinkedPosTags()}
     * @param minExcludePosAnnotationProbability the probability or <code>null</code> to set
     * {@value #DEFAULT_MIN_EXCLUDE_POS_ANNOTATION_PROBABILITY}
     */
    public final void setMinExcludePosAnnotationProbability(Double minExcludePosAnnotationProbability){
        if(minExcludePosAnnotationProbability == null){
            this.minExcludePosAnnotationProbability = DEFAULT_MIN_EXCLUDE_POS_ANNOTATION_PROBABILITY;
        } else if(minExcludePosAnnotationProbability >= 0 && minExcludePosAnnotationProbability <= 1) {
            this.minExcludePosAnnotationProbability = minExcludePosAnnotationProbability;
        } else {
            throw new IllegalArgumentException("parsed value MUST BE in the range 0..1 or NULL to set the default");
        }
    }
    /**
     * Setter for the linked {@link LexicalCategory LexicalCategories}
     * @param linkedLexicalCategories the set or <code>null</code> to set
     * the {@link #DEFAULT_LINKED_LEXICAL_CATEGORIES}.
     */
    public final void setLinkedLexicalCategories(Set<LexicalCategory> linkedLexicalCategories) {
        if(linkedLexicalCategories == null){
            this.linkedLexicalCategories = DEFAULT_LINKED_LEXICAL_CATEGORIES;
        } else if(linkedLexicalCategories.contains(null)){
            throw new IllegalArgumentException("The parsed set with linked LexicalCategories MUST NOT contain the NULL element!");
        } else {
            this.linkedLexicalCategories = linkedLexicalCategories;
        }
    }
    /**
     * Setter for the linked {@link Pos} types.
     * @param linkedLexicalCategories the set of linked {@link Pos} types or <code>null</code>
     * to set the {@link #DEFAULT_LINKED_POS} types
     */
    public final void setLinkedPos(Set<Pos> linkedPos) {
        if(linkedPos == null){
            this.linkedPos = DEFAULT_LINKED_POS;
        } else if(linkedPos.contains(null)){
            throw new IllegalArgumentException("The parsed set with linked LexicalCategories MUST NOT contain the NULL element!");
        } else {
            this.linkedPos = linkedPos;
        }
    }
    /**
     * Setter for the linked Pos Tags. This should only be used of the 
     * used POS tagger uses {@link PosTag}s that are not mapped to
     * {@link LexicalCategory LexicalCategories} nor {@link Pos} types.
     * @param processedPosTags the linked Pos tags. if <code>null</code>
     * the value is set to an empty set.
     */
    public final void setLinkedPosTags(Set<String> processedPosTags) {
        if(processedPosTags == null){
            this.linkedPosTags = Collections.emptySet();
        } else if(processedPosTags.contains(null)){
            throw new IllegalArgumentException("The parsed set with processed POS tags MUST NOT contain the NULL element!");
        } else {
            this.linkedPosTags = processedPosTags;
        }
    }
    /**
     * Getter for the processed phrase categories.
     * {@link Chunk}s of other types will be ignored.
     * @return
     */
    public Set<LexicalCategory> getProcessedPhraseCategories() {
        return processedPhraseCategories;
    }
    /**
     * Setter for the processable phrase categories. 
     * @param processablePhraseCategories the processable categories or
     * <code>null</code> to set the {@link #DEFAULT_PROCESSED_PHRASE_CATEGORIES}.
     */
    public void setProcessedPhraseCategories(Set<LexicalCategory> processablePhraseCategories){
        if(processablePhraseCategories == null){
            this.processedPhraseCategories = DEFAULT_PROCESSED_PHRASE_CATEGORIES;
        } else {
            this.processedPhraseCategories = EnumSet.noneOf(LexicalCategory.class);
            this.processedPhraseCategories.addAll(processablePhraseCategories);
        }
    }
    /**
     * Getter for the prococessed phrase Tags. This should be only
     * used if the {@link PhraseTag}s used by the Chunker are not
     * mapped to {@link LexicalCategory LexicalCategories}.
     * @return the processed phrase tags
     */
    public Set<String> getProcessedPhraseTags() {
        return processedPhraseTags;
    }
    /**
     * Setter for the Processed Phrase Tags
     * @param processedPhraseTags the set with the tags. If <code>null</code>
     * the value is set to an empty set.
     */
    public void setProcessedPhraseTags(Set<String> processedPhraseTags) {
        if(processedPhraseTags == null || processedPhraseTags.isEmpty()){
            this.processedPhraseTags = Collections.emptySet();
        } else {
            this.processedPhraseTags = new HashSet<String>(processedPhraseTags);
        }
    }
    /**
     * Getter for the minimum required probability so that {@link PhraseTag}s
     * are accepted.
     * @return the probability [0..1)
     */
    public double getMinPhraseAnnotationProbability() {
        return minPhraseAnnotationProbability;
    }
    /**
     * Getter for the minimum required probability so that {@link PhraseTag}s
     * are considered for rejecting (e.g. to skip a VerbPhrase if 
     * {@link LexicalCategory#Verb} is not present in 
     * {@link #getProcessedPhraseCategories()}). Typically this value is
     * lower as {@link #getMinPhraseAnnotationProbability()}
     * @return the probability [0..1)
     */
    public double getMinExcludePhraseAnnotationProbability() {
        return minExcludePhraseAnnotationProbability;
    }
    /**
     * Setter for the minimum phrase annotation probability [0..1)
     * @param prob the probability [0..1) or <code>null</code> to set
     * the {@value #DEFAULT_MIN_PHRASE_ANNOTATION_PROBABILITY}
     * @throws IllegalArgumentException if the parsed value is not
     * in the range [0..1).
     */
    public void setMinPhraseAnnotationProbability(Double prob) {
        if(prob == null){
            this.minPhraseAnnotationProbability = DEFAULT_MIN_PHRASE_ANNOTATION_PROBABILITY;
        } else if (prob >= 1 || prob < 0){
            throw new IllegalArgumentException("The parsed minimum phrase annotation probability '"
                + prob +" MUST be in the range [0..1)!");
        } else {
            this.minPhraseAnnotationProbability = prob;
        }
    }

    /**
     * Setter for the minimum excluded phrase annotation probability [0..1)
     * @param prob the probability [0..1) or <code>null</code> to set
     * the {@value #DEFAULT_MIN_EXCLUDE_PHRASE_ANNOTATION_PROBABILITY}
     * @throws IllegalArgumentException if the parsed value is not
     * in the range [0..1).
     */
    public void setMinExcludePhraseAnnotationProbability(Double prob) {
        if(prob == null){
            this.minExcludePhraseAnnotationProbability = DEFAULT_MIN_EXCLUDE_PHRASE_ANNOTATION_PROBABILITY;
        } else if (prob >= 1 || prob < 0){
            throw new IllegalArgumentException("The parsed minimum exclude phrase annotation probability '"
                + prob +" MUST be in the range [0..1)!");
        } else {
            this.minExcludePhraseAnnotationProbability = prob;
        }
    }
    /**
     * Getter for the set of {@link Pos} types used for linking Entities
     * @return the linked {@link Pos} types
     */
    public Set<Pos> getLinkedPos() {
        return linkedPos;
    }
    
    /**
     * If upper case Tokens should be linked regardless
     * of the POS type and length
     * @return
     */
    public boolean isLinkUpperCaseTokens(){
        return linkUpperCaseTokensState;
    }
    /**
     * Setter for the state if upper case token should be
     * linked regardless of the POS type and length
     * @param linkUpperCaseTokensState the state or <code>null</code>
     * to set the {@link #DEFAULT_LINK_UPPER_CASE_TOKEN_STATE}
     */
    public void setLinkUpperCaseTokensState(Boolean linkUpperCaseTokensState) {
        if(linkUpperCaseTokensState == null){
            this.linkUpperCaseTokensState = DEFAULT_LINK_UPPER_CASE_TOKEN_STATE;
        } else {
            this.linkUpperCaseTokensState = linkUpperCaseTokensState;
        }
    }
    /**
     * If upper case Tokens should be matched regardless
     * of the POS type and length
     * @return
     */
    public boolean isMatchUpperCaseTokens(){
        return matchUpperCaseTokensState;
    }
    /**
     * Setter for the state if upper case token should be
     * matched regardless of the POS type and length
     * @param matchUpperCaseTokensState the state or <code>null</code>
     * to set the {@link #DEFAULT_MATCH_UPPER_CASE_TOKEN_STATE}
     */
    public void setMatchUpperCaseTokensState(Boolean matchUpperCaseTokensState) {
        if(matchUpperCaseTokensState == null){
            this.matchUpperCaseTokensState = DEFAULT_MATCH_UPPER_CASE_TOKEN_STATE;
        } else {
            this.matchUpperCaseTokensState = matchUpperCaseTokensState;
        }
    }
    /**
     * If {@link #isIgnoreChunks()} is disabled than this allows
     * to convert matchable {@link Token}s to linked one in 
     * case a {@link Chunk} contains more than one matchable
     * Token. <p>
     * This is especially useful in cases where only
     * {@link Pos#ProperNoun}s are processed to also detect
     * Entities that are named by using multiple Common Nouns.
     * In cases where all {@link LexicalCategory#Noun}s are
     * processed this option has usually no influence on the
     * results.
     * @return the state
     */
    public boolean isLinkMultiMatchableTokensInChunk() {
        return linkMultiMatchableTokensInChunkState;
    }
    /**
     * Setter for state if for {@link Chunk}s with multiple 
     * matchable {@link Token}s those Tokens should be treated
     * as linkable.<p>
     * This is especially useful in cases where only
     * {@link Pos#ProperNoun}s are linked to also detect
     * Entities that are named by using multiple Common Nouns.
     * In cases where all {@link LexicalCategory#Noun}s are
     * processed this option has usually no influence on the
     * results.
     * @param state the state or <code>null</code> to reset to the
     * the {@link #DEFAULT_LINK_MULTIPLE_MATCHABLE_TOKENS_IN_CHUNKS_STATE default}
     */
    public void setLinkMultiMatchableTokensInChunkState(Boolean state){
        if(state == null){
            this.linkMultiMatchableTokensInChunkState = DEFAULT_LINK_MULTIPLE_MATCHABLE_TOKENS_IN_CHUNKS_STATE;
        } else {
            this.linkMultiMatchableTokensInChunkState = state;
        }
    }
    /**
     * The minimum number of character a {@link Token} (word) must have to be
     * used {@link EntitySearcher#lookup(java.util.List, String...) lookup} concepts
     * in the taxonomy. Note that this parameter is only used of no POS (Part-
     * of-speech) tags are available in the {@link AnalysedText}.
     * @param minSearchTokenLength the minSearchTokenLength to set
     */
    public void setMinSearchTokenLength(int minSearchTokenLength) {
        this.minSearchTokenLength = minSearchTokenLength;
    }
    
    /**
     * The minimum number of character a {@link Token} (word) must have to be
     * used {@link EntitySearcher#lookup(java.util.List, String...) lookup} concepts
     * in the taxonomy. Note that this parameter is only used of no POS (Part-
     * of-speech) tags are available in the {@link AnalysedText}.
     * @return the minSearchTokenLength
     */
    public int getMinSearchTokenLength() {
        return minSearchTokenLength;
    }

    /**
     * This returns the state if only upper case tokens should be marked as 
     * 'linkable' if they do not have a POS tag
     * @return the state
     */
    public boolean isLinkOnlyUpperCaseTokensWithUnknownPos(){
        return linkOnlyUpperCaseTokenWithUnknownPos;
    }
 
    /**
     * This returns the state if only upper case tokens should be marked as 
     * 'linkable' if they do not have a POS tag
     * @param linkOnlyUpperCaseTokenWithUnknownPos the state
     */
    public void setLinkOnlyUpperCaseTokenWithUnknownPos(boolean linkOnlyUpperCaseTokenWithUnknownPos) {
        this.linkOnlyUpperCaseTokenWithUnknownPos = linkOnlyUpperCaseTokenWithUnknownPos;
    }
    
    /**
     * Getter for the chunkable {@link LexicalCategory LexicalCategories}. Those
     * allow the EntityLinker to step-over non matchable tokens when determining 
     * search tokens for Entityhub lookups.
     * @return
     */
    public Set<LexicalCategory> getChunkableCategories(){
        return chunkableCategories;
    }
    
    /**
     * Setter for the chunkable {@link LexicalCategory LexicalCategories}. Those
     * allow the EntityLinker to step-over non matchable tokens when determining 
     * search tokens for Entityhub lookups.
     * @param categories The list of {@link LexicalCategory LexicalCategories} 
     * considered as chunkable or <code>null</code> to reset to the default
     */
    public void setChunkableCategories(Set<LexicalCategory> categories){
        if(categories == null){
            this.chunkableCategories = DEFAULT_CHUNKABLE_CATEGORIES;
        } else {
            this.chunkableCategories = categories;
        }
    }

    /**
     * Setter for the {@link Pos} tags considered by the EntityLinker to step-over 
     * non matchable tokens when determining search tokens for Entityhub lookups
     * @param pos The list of {@link Pos} tags considered as chunkable or 
     * <code>null</code> to reset to the default
     */
    public void setChunkablePos(Set<Pos> pos){
        if(pos == null){
            this.chunkablePos = DEFAULT_CHUNKABLE_POS;
        } else {
            this.chunkablePos = pos;
        }
    }
    
    /**
     * Setter for the String tags considered by the EntityLinker to step-over 
     * non matchable tokens when determining search tokens for Entityhub lookups
     * @param tags The list of String tags considered as chunkable or 
     * <code>null</code> to reset to the default
     */
    public void setChunkableTags(Set<String> tags){
        if(tags == null){
            this.chunkableTags = DEFAULT_CHUNKABKE_TAGS;
        } else {
            this.chunkableTags = tags;
        }
    }
    /**
     * Getter for the {@link Pos} tags considered by the EntityLinker to step-over 
     * non matchable tokens when determining search tokens for Entityhub lookups
     * @return
     */
    public Set<Pos> getChunkablePos(){
        return chunkablePos;
    }
    
    /**
     * Getter for the String tags considered by the EntityLinker to step-over 
     * non matchable tokens when determining search tokens for Entityhub lookups
     * @return the String tags considered as chunkable
     */
    public Set<String> getChunkableTags(){
        return chunkableTags;
    }
    
    /**
     * Clones the {@link LanguageProcessingConfig}. Intended to be used
     * to create language specific configs based on the default one.
     */
    @Override
    public LanguageProcessingConfig clone() {
        LanguageProcessingConfig c = new LanguageProcessingConfig();
        c.ignoreChunksState = ignoreChunksState;
        c.minExcludePhraseAnnotationProbability = minExcludePhraseAnnotationProbability;
        c.minExcludePosAnnotationProbability = minExcludePosAnnotationProbability;
        c.minPhraseAnnotationProbability = minPhraseAnnotationProbability;
        c.minPosAnnotationProbability = minPosAnnotationProbability;
        c.linkedLexicalCategories = linkedLexicalCategories;
        c.processedPhraseCategories = processedPhraseCategories;
        c.processedPhraseTags = processedPhraseTags;
        c.linkedPos = linkedPos;
        c.linkedPosTags = linkedPosTags;
        c.linkUpperCaseTokensState = linkUpperCaseTokensState;
        c.matchUpperCaseTokensState = matchUpperCaseTokensState;
        c.linkMultiMatchableTokensInChunkState = linkMultiMatchableTokensInChunkState;
        c.matchedLexicalCategories = matchedLexicalCategories;
        c.minSearchTokenLength = minSearchTokenLength;
        c.linkOnlyUpperCaseTokenWithUnknownPos = linkOnlyUpperCaseTokenWithUnknownPos;
        c.chunkableCategories = chunkableCategories;
        c.chunkablePos = chunkablePos;
        c.chunkableTags = chunkableTags;
        return c;
    }


}

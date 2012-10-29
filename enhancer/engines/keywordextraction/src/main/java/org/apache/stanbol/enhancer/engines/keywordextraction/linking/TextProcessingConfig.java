package org.apache.stanbol.enhancer.engines.keywordextraction.linking;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.apache.stanbol.enhancer.nlp.model.Chunk;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.Pos;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;

public class TextProcessingConfig implements Cloneable{
    
    /**
     * The processed Phrase types. Includes {@link LexicalCategory#Noun} phrases
     */
    private static final Set<LexicalCategory> DEFAULT_PROCESSED_PHRASE_CATEGORIES = 
            EnumSet.of(LexicalCategory.Noun);
    /**
     * The default set of {@link LexicalCategory LexicalCategories} used to
     * lookup Entities within the {@link EntitySearcher}
     */
    public static final Set<LexicalCategory> DEFAULT_PROCESSED_LEXICAL_CATEGORIES = 
            EnumSet.of(LexicalCategory.Noun, LexicalCategory.Residual);

    /**
     * The default set of {@link LexicalCategory LexicalCategories} used to
     * match (and search) for Entities
     */
    public static final Set<LexicalCategory> DEFAULT_MATCHED_LEXICAL_CATEGORIES =
            EnumSet.of(LexicalCategory.Noun, LexicalCategory.Quantifier,LexicalCategory.Residual);
            
    
    /**
     * The default set of {@link Pos} types that are used to lookup Entities.
     * By defualt only {@link Pos#ProperNoun}s and two 
     * {@link LexicalCategory#Residual} acronyms and
     * words marked as foreign material.
     */
    public static final Set<Pos> DEFAULT_PROCESSED_POS = 
            EnumSet.of(Pos.ProperNoun, Pos.Foreign, Pos.Acronym);

    /**
     * Default value for POS annotation confidence required for processed POS tags
     * (both {@link #getProcessedLexicalCategories()} and {@link #getProcessedPosTags()})
     */
    public static final double DEFAULT_MIN_POS_ANNOTATION_PROBABILITY = 0.75;

    /**
     * Default value for POS annotation confidence required for not-processed POS tags
     * (not contained in both {@link #getProcessedLexicalCategories()} and 
     * {@link #getProcessedPosTags()}). <br> The default is 
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
    private static final double DEFAULT_MIN_PHRASE_ANNOTATION_PROBABILITY = 0.75;
    /**
     * the minimum probability so that a phrase is rejected based on the Phrase Annotation
     */
    private static final double DEFAULT_MIN_EXCLUDE_PHRASE_ANNOTATION_PROBABILITY = 
            DEFAULT_MIN_PHRASE_ANNOTATION_PROBABILITY/2;
    
    /**
     * The set of {@link PosTag#getCategory()} considered for EntityLinking
     * @see #DEFAULT_PROCESSED_LEXICAL_CATEGORIES
     */
    private Set<LexicalCategory> processedLexicalCategories = DEFAULT_PROCESSED_LEXICAL_CATEGORIES;

    private Set<LexicalCategory> matchedLexicalCategories = DEFAULT_MATCHED_LEXICAL_CATEGORIES;

    /**
     * The processed {@link Pos} categories
     */
    private Set<Pos> processedPos = DEFAULT_PROCESSED_POS;
    /**
     * The set of {@link PosTag#getTag()} values that are processed
     */
    private Set<String> processedPosTags = Collections.emptySet();
    /**
     * The minimum confidence of POS annotations for {@link #getProcessedLexicalCategories()}
     * and #getProcessedPosTags()
     */
    private double minPosAnnotationProbability = DEFAULT_MIN_POS_ANNOTATION_PROBABILITY;

    /**
     * The minimum confidence that a POS annotation 
     */
    private double minExcludePosAnnotationProbability = DEFAULT_MIN_EXCLUDE_POS_ANNOTATION_PROBABILITY/2;

    private boolean ignoreChunksState = DEFAULT_IGNORE_CHUNK_STATE;


    private double minPhraseAnnotationProbability = DEFAULT_MIN_PHRASE_ANNOTATION_PROBABILITY;

    private double minExcludePhraseAnnotationProbability = DEFAULT_MIN_EXCLUDE_PHRASE_ANNOTATION_PROBABILITY;

    private Set<LexicalCategory> processedPhraseCategories = DEFAULT_PROCESSED_PHRASE_CATEGORIES;

    private Set<String> processedPhraseTags = Collections.emptySet();


    /**
     * The language or <code>null</code> for the default configuration
     * @param language
     */
    public TextProcessingConfig(){
    }    
    
    public final boolean isIgnoreChunks() {
        return ignoreChunksState;
    }    
    
    /**
     * Setter for the ignore {@link Chunk} state.
     * @param state the state or <code>null</code> to set the 
     * {@link #DEFAULT_IGNORE_CHUNK_STATE}
     */
    public final void setIgnoreChunks(Boolean state){
        if(state == null){
            this.ignoreChunksState = DEFAULT_IGNORE_CHUNK_STATE;
        } else {
            this.ignoreChunksState = state;
        }
    }
    
    /**
     * Getter for the set of {@link LexicalCategory LexicalCategories} used for EntityLinking
     * @see #DEFAULT_PROCESSED_LEXICAL_CATEGORIES
     */
    public final Set<LexicalCategory> getProcessedLexicalCategories() {
        return processedLexicalCategories;
    }

    public final Set<LexicalCategory> getMatchableLexicalCategories(){
        return matchedLexicalCategories;
    }
    
    public final Set<String> getProcessedPosTags() {
        return processedPosTags;
    }
    
    /**
     * Getter for the minimum probability of POS annotations for 
     * {@link #getProcessedLexicalCategories()} or {@link #getProcessedPosTags()}
     * @return the probability
     */
    public final double getMinPosAnnotationProbability() {
        return minPosAnnotationProbability ;
    }
    
    
    /**
     * Getter for the minimum probability of POS annotations not included in 
     * {@link #getProcessedLexicalCategories()} or {@link #getProcessedPosTags()}
     * @return the probability
     */
    public final double getMinExcludePosAnnotationProbability() {
        return minExcludePosAnnotationProbability;
    }
    
    /**
     * Setter for the minimum probability of POS annotations for 
     * {@link #getProcessedLexicalCategories()} or {@link #getProcessedPosTags()}
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
     * {@link #getProcessedLexicalCategories()} or {@link #getProcessedPosTags()}
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
     * 
     * @param processedLexicalCategories
     */
    public final void setProcessedLexicalCategories(Set<LexicalCategory> processedLexicalCategories) {
        if(processedLexicalCategories == null){
            this.processedLexicalCategories = DEFAULT_PROCESSED_LEXICAL_CATEGORIES;
        } else if(processedLexicalCategories.contains(null)){
            throw new IllegalArgumentException("The parsed set with processed LexicalCategories MUST NOT contain the NULL element!");
        } else {
            this.processedLexicalCategories = processedLexicalCategories;
        }
    }
    /**
     * 
     * @param processedLexicalCategories
     */
    public final void setProcessedPos(Set<Pos> processedPos) {
        if(processedPos == null){
            this.processedPos = DEFAULT_PROCESSED_POS;
        } else if(processedPos.contains(null)){
            throw new IllegalArgumentException("The parsed set with processed LexicalCategories MUST NOT contain the NULL element!");
        } else {
            this.processedPos = processedPos;
        }
    }
    /**
     * 
     * @param processedPosTags
     */
    public final void setProcessedPosTags(Set<String> processedPosTags) {
        if(processedPosTags == null){
            this.processedPosTags = Collections.emptySet();
        } else if(processedPosTags.contains(null)){
            throw new IllegalArgumentException("The parsed set with processed POS tags MUST NOT contain the NULL element!");
        } else {
            this.processedPosTags = processedPosTags;
        }
    }

    public Set<LexicalCategory> getProcessedPhraseCategories() {
        return processedPhraseCategories;
    }

    public Set<String> getProcessedPhraseTags() {
        return processedPhraseTags;
    }
    
    public double getMinPhraseAnnotationProbability() {
        return minPhraseAnnotationProbability;
    }

    public double getMinExcludePhraseAnnotationProbability() {
        return minExcludePhraseAnnotationProbability;
    }

    public Set<Pos> getProcessedPos() {
        return processedPos;
    }
    /**
     * Clones the {@link TextProcessingConfig}. Intended to be used
     * to create language specific configs based on the default one.
     */
    @Override
    public TextProcessingConfig clone() {
        TextProcessingConfig c = new TextProcessingConfig();
        c.ignoreChunksState = ignoreChunksState;
        c.minExcludePhraseAnnotationProbability = minExcludePhraseAnnotationProbability;
        c.minExcludePosAnnotationProbability = minExcludePosAnnotationProbability;
        c.minPhraseAnnotationProbability = minPhraseAnnotationProbability;
        c.minPosAnnotationProbability = minPosAnnotationProbability;
        c.processedLexicalCategories = processedLexicalCategories;
        c.processedPhraseCategories = processedPhraseCategories;
        c.processedPhraseTags = processedPhraseTags;
        c.processedPos = processedPos;
        c.processedPosTags = processedPosTags;
        return c;
    }

}

package org.apache.stanbol.enhancer.engines.poschunker;

import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.PHRASE_ANNOTATION;
import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.POS_ANNOTATION;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.stanbol.enhancer.nlp.NlpAnnotations;
import org.apache.stanbol.enhancer.nlp.model.Chunk;
import org.apache.stanbol.enhancer.nlp.model.Section;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.phrase.PhraseTag;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;

public class PhraseBuilder {
    
    /**
     * Just a fallback in case Pos annotations do not provide probabilities. 
     * In most cases the value of this will not have any effect as typically 
     * Pos Taggers that do not provide probabilities only emit a
     * single POS tag per Token. In such cases this tag will be always accepted 
     * regardless of the configured value. <p>
     * The value is only important if some Pos annotation for a Token do have 
     * probabilities while others have not. In such cases those without are rated 
     * against other that have by using this value. Such Situations should only
     * occur if a chain uses several POS taggers - a setting that should be
     * avoided<p>
     */
    private static final double DEFAULT_SCORE = 0.1;
    
    private final PhraseTypeDefinition phraseType;
    
    private final ChunkFactory chunkFactory;
    
    private final double minPosSocre;
    /**
     * The {@link PhraseTag} added to all {@link Chunk}s created by this
     * {@link PhraseBuilder}
     */
    private final PhraseTag phraseTag;
        
    /**
     * Holds Tokens of a current phrase. Empty if no phrase is building.
     */
    private List<Token> current = new ArrayList<Token>();
    /**
     * If {@link #current} contains a Tokens matching 
     * {@link PhraseTypeDefinition#getRequiredType()}
     */
    boolean valid;
    
    public PhraseBuilder(PhraseTypeDefinition phraseType, ChunkFactory chunkFactory, double minPosSocre) {
        if(phraseType == null){
            throw new IllegalArgumentException("The parsed PhraseTypeDefinition MUST NOT be NULL!");
        }
        this.phraseType = phraseType;
        this.phraseTag = new PhraseTag(phraseType.getPhraseType().name(), 
            phraseType.getPhraseType());
        if(chunkFactory == null){
            throw new IllegalArgumentException("The parsed ChunkFactory MUST NOT be NULL");
        }
        this.chunkFactory = chunkFactory;
        if(minPosSocre < 0 || minPosSocre > 1){
            throw new IllegalArgumentException("The parsed minPosScore '" + minPosSocre 
                + "' MUST BE within the ranve [0..1]!");
        }
        this.minPosSocre = minPosSocre;
    }
    
    
    public void nextToken(Token token){
        if(current.isEmpty()){ //check for start
            checkStart(token);
        } else if(!checkContinuation(token)){ //check for continuation
            buildPhrase(token);
        }
        
    }
    
    public void nextSection(Section section){
        buildPhrase(null);
    }
    

    @SuppressWarnings("unchecked") //varargs with generic types
    private void checkStart(Token token){
        boolean[] states = checkCategories(token, phraseType.getStartType(), 
            phraseType.getRequiredType());
        if(states[0]){
            current.add(token);
            valid = states[1];
        }
    }

    @SuppressWarnings("unchecked") //varargs with generic types
    private boolean checkContinuation(Token token){
        final boolean[] states;
        if(!valid){
            states = checkCategories(token, phraseType.getContinuationType(),
                phraseType.getRequiredType());
        } else {
            states = checkCategories(token, phraseType.getContinuationType());
        }
        if(states[0]){
            current.add(token);
        }
        if(states.length > 1){
            valid = states[1];
        }
        return states[0];
    }
    
    @SuppressWarnings("unchecked") //varargs with generic types
    private void buildPhrase(Token token) {
        Token lastConsumedToken = null;
        if(valid){
            //search backwards for the first token matching an allowed end
            //category
            int endIndex = current.size()-1;
            while(endIndex > 0 && !checkCategories(current.get(endIndex), 
                phraseType.getEndType())[0]){
                endIndex--;
            }
            lastConsumedToken = current.get(endIndex);
            //NOTE: ignore phrases with a single token
            if(endIndex > 0){
                Chunk chunk = chunkFactory.createChunk(current.get(0), lastConsumedToken);
                //TODO: add support for confidence
                chunk.addAnnotation(PHRASE_ANNOTATION, Value.value(phraseTag));
            }
        }
        //cleanup
        current.clear();
        valid = false;
        if(token != null && !token.equals(lastConsumedToken)){
            //the current token might be the start of a new phrase
            checkStart(token);
        }
    }
    
    /**
     * Checks if the a the {@link NlpAnnotations#POS_ANNOTATION POS Annotations}
     * of a {@link Token} matches the parsed categories. This method supports
     * to check against multiple sets of categories to allow checking e.g. if a token
     * is suitable for {@link PhraseTypeDefinition#getStartType()} and
     * {@link PhraseTypeDefinition#getRequiredType()}.
     * @param token the Token
     * @param categories the list of categories to check
     * @return if the sum of matching annotations compared to the score of all
     * POS annotations is higher or equals the configured {@link #minPosSocre}.
     * For each parsed categories set a boolean state is returned.
     */
    private boolean[] checkCategories(Token token, Set<LexicalCategory>...categories) {
        //there are different ways NLP frameworks do assign scores. For some the
        //sum of all categories would sum up to 1.0, but as only the top three
        //categories are included the sum would be < 1
        //Others assign scores so that each score is < 1, but the sum of all
        //is higher as 1.0.
        //There is also the possibility that no scores are present.
        
        //Because of that this sums up all scores and normalizes with the 
        //Match.max(1.0,sumScore).
        //POS tags without score are assigned a #DEFAULT_SCORE. If not a single
        //POS tag with a score is present the sumScore is NOT normalized to 1.0
        boolean scorePresent = false;
        double sumScore = 0;
        double[] matchScores = new double[categories.length];
        for(Value<PosTag> pos : token.getAnnotations(POS_ANNOTATION)){
            double score = pos.probability();
            if(score == Value.UNKNOWN_PROBABILITY){
                score = DEFAULT_SCORE;
            } else {
                scorePresent = true;
            }
            sumScore = sumScore + pos.probability();
            Set<LexicalCategory> tokenCategories = pos.value().getCategories();
            for(int i = 0; i < categories.length; i++){
                Set<LexicalCategory> category = categories[i];
                if(!Collections.disjoint(tokenCategories, category)){
                    matchScores[i] = matchScores[i] + pos.probability();
                }
            }
        }
        boolean[] matches = new boolean[matchScores.length];
        //the score used to normalize annotations. See comments at method start
        double normScore = scorePresent ? Math.max(1.0,sumScore) : sumScore;
        for(int i = 0; i < matchScores.length ; i++){
            matches[i] = matchScores[i]/normScore >= minPosSocre;
        }
        return matches;
    }

    public static interface ChunkFactory {
        
        Chunk createChunk(Token start, Token end);
    }
    
}

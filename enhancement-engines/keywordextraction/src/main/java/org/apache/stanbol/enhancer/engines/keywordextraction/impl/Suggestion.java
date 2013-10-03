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

import java.util.Comparator;
import java.util.Iterator;

import org.apache.stanbol.enhancer.engines.keywordextraction.linking.EntitySearcher;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;

/**
 * A suggestion of an entity in the {@link EntitySearcher} for a part of the
 * text. This class does not include the actual position within the Text, 
 * because it is intended to be used in combination with {@link LinkedEntity}.<p>
 * This class also manages redirected entities and a state if redirects where
 * already processed for this suggestion.<p>
 * In addition this class also defines a set of {@link Comparator}s that are 
 * used to sort suggestions base on how well the fit the text.
 * @author Rupert Westenthaler
 *
 */
public class Suggestion implements Comparable<Suggestion>{
    private MATCH match = MATCH.NONE;
    private int start = 0;
    private int span = 0;
    private int matchCount = 0;
    private Text label;
    private int labelTokenCount = 0;
    private final Representation result;
    private Representation redirectsTo;
    private boolean redirectProcessed;
    
    private double score;
    /**
     * The score of the matches (e.g. when a match is based on stemming or some
     * oder kind of fuzziness, than matchers might assign a match score than
     * 1.0.
     */
    private float matchScore;
    public static enum MATCH {
        /**
         * No match (to less tokens, wrong oder ...)
         */
        NONE,
        /**
         * Not all tokens but sufficient to suggest (with lower score)
         */
        PARTIAL,
        /**
         * All requested Tokens match, but it is no exact match e.g. because
         * the label defines some additional tokens
         */
        FULL,
        /**
         * The label of the suggested Entity is exactly the requested string
         */
        EXACT,
    }
    protected Suggestion(Representation result){
        if(result == null){
            throw new IllegalArgumentException("The parsed Result MUST NOT be NULL!");
        }
        this.result = result;
        //TODO Do no longer use the resultScore as the score. We need to provide an
        //own algorithm to calculate scores!
//        this.resultScore = result.getFirst(RdfResourceEnum.resultScore.getUri(), Float.class);
    }
    /**
     * Updates this suggestion 
     * @param match the math type
     * @param start the start position of this suggestion
     * @param span the number of token this suggestion spans
     * @param count the number of token that match with the suggestion within the span
     * @param matchScore the score of the match. MUST BE in the range between 
     * <code>[0..1]</code>. For {@link MATCH#EXACT} and {@link MATCH#NONE} this
     * parameter is ignored and the value is set to <code>1</code>, <code>0</code>
     * respectively.
     * @param label the label that matches the tokens
     * @param labelTokenCount the number of tokens of the label
     */
    protected void updateMatch(MATCH match,int start, int span,int count,float matchScore,Text label,int labelTokenCount){
        this.match = match;
        //check the validity of the parameters to avoid later errors that are
        //than hard to debug
        if(match == MATCH.NONE){
            this.span = 0;
            this.matchCount = 0;
            this.matchScore = 0f;
            this.label = null;
        } else {
            if(span < 1 || count < 1){
                throw new IllegalArgumentException("For "+match+" matches the token span and count MUST BE > 0");
            }
            if(match == MATCH.PARTIAL){
                if(span <= count && labelTokenCount <= count){
                    throw new IllegalArgumentException("For "+match+" matches the (token span OR label token count) MUST BE > than the token count!");
                }
            } else {
                if(span != count){
                    throw new IllegalArgumentException("For "+match+" matches the token span '"
                            +span+"' MUST BE equals to the token count '"+count+"' (label: '"
                            +label.getText()+"')!");
                }
            }
        }
        this.start = start;
        this.span = span;
        this.label = label;
        if(match == MATCH.EXACT){ //for exact matches the matchScore needs to be
            this.matchScore = 1f; // ignored and set to 1.0f
            this.matchCount = span; //and the match count needs to be equals to the span
            this.labelTokenCount = span;
        } else {
            if(matchScore > 1f){
                throw new IllegalArgumentException("The matchScore MUST NOT be greater than one (parsed value = "+matchScore+")");
            }
            this.matchScore = matchScore;
            this.matchCount = count;
            this.labelTokenCount = labelTokenCount;
        }
    }
    /**
     * Getter for the number of Tokens of the label. Usually needed to calculate
     * the score (how good the label matches)
     * @return the labelTokenCount
     */
    public final int getLabelTokenCount() {
        return labelTokenCount;
    }
    /**
     * Setter for the {@link MATCH} type of this suggestion
     * @param match the match type
     */
    protected void setMatch(MATCH match) {
        this.match = match;
    }

    /**
     * Getter for the the type of the match
     * @return The type of the match
     */
    public final MATCH getMatch() {
        return match;
    }
    /**
     * Getter for the matching score. This is a modifier in the range
     * between [0..1] that tells about the quality of the matches for the
     * {@link #getMatchCount() matched} tokens. <p>
     * As an example if a match is based on stemming a word a label matcher
     * implementation might want to assign a matching score below <code>1</code>.
     * Score calculations that use the {@link #getMatchCount()} should use
     * <code>{@link #getMatchCount()} * {@link #getMatchScore()}</code> as a
     * bases.
     * @return the matchScore
     */
    public final float getMatchScore() {
        return matchScore;
    }
    /**
     * Getter for the start index of this Suggestion
     * @return the start token index for this suggestion
     */
    public int getStart() {
        return start;
    }
    /**
     * Getter for the number of the token matched by this suggestion
     * @return The number of the token matched by this suggestion
     */
    public final int getSpan() {
        return span;
    }
    /**
     * Getter for the he number of matching tokens.
     * @return The number of matching tokens.
     */
    public final int getMatchCount(){
        return matchCount;
    }
    /**
     * The actual label of the {@link #getResult() result} that produced the
     * based match for the given search tokens.
     * @return the label
     */
    public final Text getMatchedLabel() {
        return label;
    }
    protected final void setMatchedLabel(Text label){
        this.label = label;
    }
    /**
     * Getter for the best label in the given language
     * @param suggestion the suggestion
     * @param nameField the field used to search for labels
     * @param language the language
     * @return the best match or {@link Suggestion#getMatchedLabel()} if non is found
     */
    public Text getBestLabel(String nameField, String language){
        Representation rep = getRepresentation();
        //start with the matched label -> so if we do not find a better one
        //we will use the matched!
        Text label = this.label;
        // 1. check if the returned Entity does has a label -> if not return null
        // add labels (set only a single label. Use "en" if available!
        Iterator<Text> labels = rep.getText(nameField);
        boolean matchFound = false;
        while (labels.hasNext() && !matchFound) {
            Text actLabel = labels.next();
            if (label == null) { //take any label at first
                label = actLabel;
            }
            //now we have already a label check the language
            String actLang = actLabel.getLanguage();
            //use startWith to match also en-GB and en-US ...
            if (actLang != null && actLang.startsWith(language)) {
                //prefer labels with the correct language
                label = actLabel;
                if(this.label.getText().equalsIgnoreCase(label.getText())){
                    //found label in that language that exactly matches the
                    //label used to match the text
                    matchFound = true; 
                }
            }
        }
        if (label == null) { //if no label was found ... return the one used for the match
            label = getMatchedLabel();
        }
        return label;

    }
    public final Representation getResult(){
        return result;
    }
    @Override
    public String toString() {
        return label+"[m="+match+(match != MATCH.NONE ? ",c="+matchCount+",s="+span+']':"]");
    }
    /**
     * The {@link RdfResourceEnum#entityRank entity rank} of the {@link #getResult() result}.
     * The entity rank is the relative importance of an entity within an
     * Collection of Entities (ReferencedSite, Thesaurus, Taxonomy ...).<p>
     * This method returns the rank of the entity returned by
     * {@link #getRepresentation()}. Therefore if an redirect is active it will
     * be the rank of the redirected entity and not of the suggested result.
     * @return the rank of the entity or <code>null</code> if not available
     */
    public Float getEntityRank() {
        return getRepresentation().getFirst(RdfResourceEnum.entityRank.getUri(), Float.class);
    }
    /**
     * @param score the score to set
     */
    public void setScore(double score) {
        this.score = score;
    }
    /**
     * @return the score
     */
    public double getScore() {
        return score;
    }
    /**
     * Returns <code>true</code> if the result has a registered redirect
     * @return <code>true</code> if a redirect is present. Otherwise <code>false</code>
     */
    public boolean isRedirect(){
        return redirectsTo != null;
    }
    /**
     * Setter for Entity the {@link #getResult() result} of this match redirects
     * to. Also sets {@link #setRedirectProcessed(boolean)} to <code>true</code>
     * @param redirect the redirected entity or <code>null</code> if no redirect
     * is present
     */
    protected void setRedirect(Representation redirect){
        this.redirectsTo = redirect;
        setRedirectProcessed(true);
    }
    /**
     * Setter for the state if the redirects for this resultMatch where already
     * processed. Calling {@link #setRedirect(Representation)} will set this
     * automatically to <code>true</code>
     * @param state the state.
     */
    protected void setRedirectProcessed(boolean state){
        this.redirectProcessed = state;
    }
    /**
     * Getter for the state if the redirect was processed for this ResultMatch
     * @return the state
     */
    protected boolean isRedirectedProcessed(){
        return redirectProcessed;
    }
    /**
     * Getter for the Entity the {@link #getResult()} of this Entity redirects
     * to. Returns <code>null</code> if there is no redirect. 
     * @return the entity the {@link #getResult()} redirects to or <code>null</code>
     * if there is no redirect
     */
    public Representation getRedirect(){
        return redirectsTo;
    }
    
    /**
     * getter for the Representation of this result. In case of 
     * <code>{@link #isRedirect()} == true</code> it returns the the 
     * {@link #getRedirect()} otherwise it returns the {@link #getResult()}.<p>
     * To check explicitly for the result of the redirect one needs to use
     * {@link #getRedirect()} and {@link #getRedirect()} instead.
     * @return The representation for this match. might be directly the 
     * {@link #getResult() result} or if present the 
     * {@link #getRedirect() redirected} resource. 
     */
    public final Representation getRepresentation(){
        return redirectsTo == null ? result : redirectsTo;
    }
    
    /**
     * Compares {@link Suggestion} first based on the {@link Suggestion#getMatch()} value
     * and secondly based on the {@link RdfResourceEnum#entityRank}.
     */
    public static final Comparator<Suggestion> MATCH_TYPE_SUGGESTION_COMPARATOR = new Comparator<Suggestion>() {
        @Override
        public int compare(Suggestion arg0, Suggestion arg1) {
            if(arg0.match != arg1.match){
                return arg1.match.ordinal() - arg0.match.ordinal(); //higher ordinal first
            } else if(arg0.match == MATCH.NONE){
                return 0; //do not further sort entries that do not match
            } else {
                Float arg0Rank = arg0.getEntityRank();
                if(arg0Rank == null){
                    arg0Rank = Float.valueOf(0);
                }
                Float arg1Rank = arg1.getEntityRank();
                if(arg1Rank == null){
                    arg1Rank = Float.valueOf(0);
                }
                return arg1Rank.compareTo(arg0Rank); //higher ranks first
            }
        }
    };
    /**
     * Compares {@link Suggestion}s based on the {@link Suggestion#getScore()}.
     * In case the scores are equals the call is forwarded to the
     * {@link Suggestion#DEFAULT_SUGGESTION_COMPARATOR}.<p>
     * This is NOT the default {@link Comparator} because score values are
     * usually only calculated relative to the best matching suggestions and
     * therefore only available later.
     */
    public static final Comparator<Suggestion> SCORE_COMPARATOR = new Comparator<Suggestion>() {
        @Override
        public int compare(Suggestion arg0, Suggestion arg1) {
            return arg0.getScore() > arg1.getScore() ? -1 : //bigger score first
                arg0.getScore() < arg1.getScore() ? 1 : 
                    DEFAULT_SUGGESTION_COMPARATOR.compare(arg0, arg1);
        }
    };
    /**
     * Compares {@link Suggestion} first based on the {@link Suggestion#getMatchCount()} 
     * number of matched tokens. If the number of the matched tokens is equals or
     * any of the parsed {@link Suggestion} instances has {@link MATCH#NONE} it
     * forwards the request to the {@link #MATCH_TYPE_SUGGESTION_COMPARATOR}.
     */
    public static final Comparator<Suggestion> DEFAULT_SUGGESTION_COMPARATOR = new Comparator<Suggestion>() {
        @Override
        public int compare(Suggestion arg0, Suggestion arg1) {
            if(arg0.match == MATCH.NONE || arg1.match == MATCH.NONE ||
                    arg0.matchCount == arg1.matchCount){
                return MATCH_TYPE_SUGGESTION_COMPARATOR.compare(arg0, arg1);
            } else {
                return arg1.matchCount - arg0.matchCount; //bigger should be first
            }
        }
    };
    /**
     * Implementation of the {@link Comparable} interface using
     * {@link #MATCH_TYPE_SUGGESTION_COMPARATOR}.
     */
    @Override
    public int compareTo(Suggestion other) {
        return DEFAULT_SUGGESTION_COMPARATOR.compare(this, other);
    }
}
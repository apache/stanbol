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

import static org.apache.stanbol.enhancer.engines.entitylinking.impl.LabelMatch.DEFAULT_LABEL_TOKEN_COMPARATOR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.stanbol.enhancer.engines.entitylinking.Entity;
import org.apache.stanbol.enhancer.engines.entitylinking.EntitySearcher;

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
public class Suggestion {
    
    private List<LabelMatch> labelMatches = new ArrayList<LabelMatch>();
    private boolean labelMatchesSorted = true;
    private final Entity entity;
    private Entity redirectsTo;
    private boolean redirectProcessed;
    private double score;
    /**
     * used to allow overriding the MATCH of this suggestion
     */
    private MATCH match;
    
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
    protected Suggestion(Entity entity){
        if(entity == null){
            throw new IllegalArgumentException("The parsed Result MUST NOT be NULL!");
        }
        this.entity = entity;
        //TODO Do no longer use the resultScore as the score. We need to provide an
        //own algorithm to calculate scores!
//        this.resultScore = result.getFirst(RdfResourceEnum.resultScore.getUri(), Float.class);
    }
    /**
     * Adds an new LabelMatch to this suggestion
     * @param labelMatch the labelMatch
     */
    public void addLabelMatch(LabelMatch labelMatch){
        if(labelMatch == null || labelMatch.getMatch() == MATCH.NONE){
            return; //ignore null an MATCH.NONE entries
        }
        labelMatches.add(labelMatch);
        if(labelMatches.size() > 1){
            labelMatchesSorted = false;
        }
    }
    
    /**
     * Getter for the best label in the given language
     * @param suggestion the suggestion
     * @param nameField the field used to search for labels
     * @param language the language
     * @return the best match or {@link Suggestion#getMatchedLabel()} if non is found
     */
    public Literal getBestLabel(IRI nameField, String language){
        Entity rep = getEntity();
        //start with the matched label -> so if we do not find a better one
        //we will use the matched!
        Literal matchedLabel = getMatchedLabel();
        Literal label = matchedLabel;
        // 1. check if the returned Entity does has a label -> if not return null
        // add labels (set only a single label. Use "en" if available!
        Iterator<Literal> labels = rep.getText(nameField);
        boolean matchFound = false;
        while (labels.hasNext() && !matchFound) {
            Literal actLabel = labels.next();
            if(label == null){
                label = actLabel;
            }
            //now we have already a label check the language
            Language actLang = actLabel.getLanguage();
            //use startWith to match also en-GB and en-US ...
            if (actLang != null && actLang.toString().startsWith(language)) {
                //prefer labels with the correct language
                label = actLabel;
                if(matchedLabel != null && matchedLabel.getLexicalForm().equalsIgnoreCase(label.getLexicalForm())){
                    //found label in that language that exactly matches the
                    //label used to match the text
                    matchFound = true; 
                }
            }
        }
        return label;

    }

    /**
     * Shorthand for {@link #getLabelMatch()}.getMatchedLabel()
     * @return the label or <code>null</code> if {@link MATCH#NONE}
     */
    public Literal getMatchedLabel() {
        return getLabelMatch().getMatchedLabel();
    }
    protected void setMatch(MATCH matchType) {
        this.match = matchType;
    }
    /**
     * Getter for the {@link MATCH}. If not manually set
     * this forwards to {@link #getLabelMatch()}.getMatch()
     * @return the {@link MATCH} of this suggestion
     */
    public MATCH getMatch() {
        return match != null ? match : getLabelMatch().getMatch();
    }

    public final Entity getResult(){
        return entity;
    }
    /**
     * Getter for the EntityRank of the suggested Entity. In case of a 
     * redirected Entity it will return the maximum value
     */
    public Float getEntityRank() {
        final Float ranking = entity.getEntityRanking();
        final Float rdRanking = redirectsTo == null ? null : redirectsTo.getEntityRanking();
        return rdRanking != null ? 
                ranking == null || rdRanking.compareTo(ranking) > 0 ? 
                        rdRanking : ranking : ranking;
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
    protected void setRedirect(Entity redirect){
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
    public Entity getRedirect(){
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
    public final Entity getEntity(){
        return redirectsTo == null ? entity : redirectsTo;
    }
    /**
     * Getter for the top ranked LabelMatch.
     * @return the top ranked {@link LabelMatch} or {@link LabelMatch#NONE}
     * if no match is present.
     */
    public final LabelMatch getLabelMatch(){
        if(!labelMatchesSorted){
            Collections.sort(labelMatches, LabelMatch.DEFAULT_LABEL_TOKEN_COMPARATOR);
        }
        return labelMatches.isEmpty() ? LabelMatch.NONE : labelMatches.get(0);
    }
    /**
     * Getter for the sorted list with all {@link LabelMatch}s of this Suggestion
     * @return the sorted LabelMatches. Guaranteed NOT <code>null</code> and
     * NOT empty. In case no match is present a singleton list containing
     * {@link LabelMatch#NONE} is returned.
     */
    public final List<LabelMatch> getLabelMatches(){
        if(!labelMatchesSorted){
            Collections.sort(labelMatches, LabelMatch.DEFAULT_LABEL_TOKEN_COMPARATOR);
        }
        if(labelMatches.isEmpty()){
            return Collections.singletonList(LabelMatch.NONE);
        } else {
            return labelMatches;
        }
    }
    @Override
    public String toString() {
        return labelMatches.isEmpty() ? "no match" :labelMatches.get(0)
                + " for "+entity.getId()
                + (redirectsTo != null ? " (redirects: "+redirectsTo.getId()+") " : "")
                + " ranking: "+getEntityRank();
    }

    /**
     * Compares {@link Suggestion}s based on the {@link Suggestion#getScore()}.
     * In case the scores are equals the call is forwarded to the
     * {@link Suggestion#DEFAULT_LABEL_TOKEN_COMPARATOR}.<p>
     * This is NOT the default {@link Comparator} because score values are
     * usually only calculated relative to the best matching suggestions and
     * therefore only available later.
     */
    public static final Comparator<Suggestion> SCORE_COMPARATOR = new Comparator<Suggestion>() {
        @Override
        public int compare(Suggestion arg0, Suggestion arg1) {
            return arg0.getScore() > arg1.getScore() ? -1 : //bigger score first
                arg0.getScore() < arg1.getScore() ? 1 : 
                    DEFAULT_LABEL_TOKEN_COMPARATOR.compare(arg0.getLabelMatch(), arg1.getLabelMatch());
        }
    };
    /**
     * Compares {@link Suggestion}s based on the {@link Suggestion#getEntityRank()}.
     * <code>null</code> values are assumed to be the smallest.
     */
    public static final Comparator<Suggestion> ENTITY_RANK_COMPARATOR = new Comparator<Suggestion>(){
        @Override
        public int compare(Suggestion arg0, Suggestion arg1) {
            Float r1 = arg0.getEntityRank();
            Float r2 = arg1.getEntityRank();
            return r2 == null ? r1 == null ? 0 : -1 : r1 == null ? 1 : r2.compareTo(r1);
        }
    };
    /**
     * Compares {@link Suggestion} first based on the {@link Suggestion#getMatch()} value
     * and secondly based on the {@link RdfResourceEnum#entityRank}.
     */
    public static final Comparator<Suggestion> MATCH_TYPE_SUGGESTION_COMPARATOR = new Comparator<Suggestion>() {
        @Override
        public int compare(Suggestion arg0, Suggestion arg1) {
            int labelMatch = DEFAULT_LABEL_TOKEN_COMPARATOR.compare(arg0.getLabelMatch(), arg1.getLabelMatch());
            if(labelMatch == 0){
                Float arg0Rank = arg0.getEntityRank();
                if(arg0Rank == null){
                    arg0Rank = Float.valueOf(0);
                }
                Float arg1Rank = arg1.getEntityRank();
                if(arg1Rank == null){
                    arg1Rank = Float.valueOf(0);
                }
                return arg1Rank.compareTo(arg0Rank); //higher ranks first
            } else {
                return labelMatch;
            }
        }
    };

    
}
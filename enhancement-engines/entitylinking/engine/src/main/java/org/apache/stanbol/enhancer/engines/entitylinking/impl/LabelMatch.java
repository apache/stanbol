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

import java.util.Comparator;
import org.apache.clerezza.commons.rdf.Literal;

import org.apache.stanbol.enhancer.engines.entitylinking.impl.Suggestion.MATCH;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LabelMatch {
    
    private final Logger log = LoggerFactory.getLogger(LabelMatch.class);
    /**
     * To be used in case no match is present
     */
    public static final LabelMatch NONE = new LabelMatch();
    
    private MATCH match = MATCH.NONE;
    private int start = 0;
    private int span = 0;
    private int processableMatchCount = 0;
    private Literal label;
    private int labelTokenCount = 0;
    private double score;
    /**
     * The score of the matches (e.g. when a match is based on stemming or some
     * oder kind of fuzziness, than matchers might assign a match score than
     * 1.0.
     */
    private float tokenMatchScore;
    private double textScore;
    private double labelScore;
    
    private LabelMatch(){
        //internally used to create the NONE instance
    }
    
    /**
     * Creates an {@link MATCH#EXACT} label match
     * @param start
     * @param span
     */
    protected LabelMatch(int start, int span, Literal label){
        this(start,span,span,span,1f,label,span,span);
    }
    
    protected LabelMatch(int start, int span,int processableMatchCount, int matchCount, float tokenMatchScore,Literal label,int labelTokenCount, int coveredLabelTokenCount){
        if(start < 0){
            throw new IllegalArgumentException("parsed start position MUST BE >= 0!");
        }
        this.start = start;
        if(span <= 0){
            throw new IllegalArgumentException("parsed span MUST be > 0!");
        }
        this.span = span;
        if(label == null){
            throw new NullPointerException("parsed Label MUST NOT be NULL!");
        }
        this.label = label;
        if(processableMatchCount <= 0){
            match = MATCH.NONE;
        } else if(processableMatchCount == span && matchCount == coveredLabelTokenCount){
            match = MATCH.FULL;
        } else {
            match = MATCH.PARTIAL;
        }
        if(tokenMatchScore > 1f){
            throw new IllegalArgumentException("The matchScore MUST NOT be greater than one (parsed value = "+tokenMatchScore+")");
        }
        this.tokenMatchScore = tokenMatchScore;
        this.processableMatchCount = processableMatchCount;
        this.labelTokenCount = labelTokenCount;
        //init scores();
        double suggestionMatchScore = matchCount*this.tokenMatchScore;
        textScore = suggestionMatchScore/(double)this.span;
        labelScore = suggestionMatchScore/(double)this.labelTokenCount;
        score = textScore * labelScore;
        if(span < processableMatchCount){
            throw new IllegalArgumentException("The span '" + span
                + "' MUST BE >= then number of matched processable tokens'"
                + processableMatchCount+"': "+toString()+"!");
        }
        if(span < matchCount){
            log.warn("The span '{}' MUST BE >= then number of matched tokens '{}"
                    + "': {}! Set span to {}.", new Object[]{
                    span, matchCount, toString(), matchCount});
            span = matchCount;
        }
        if(processableMatchCount > matchCount){
            throw new IllegalArgumentException("The number of matched processable tokens '"
                + processableMatchCount+"' MUST BE <= the number of matched tokens '"
                + matchCount+"': "+toString()+"!");
        }
    }


    /**
     * How well matches the label matches the text span.
     * Only considers matched tokens of the label. This
     * value gets low if matches are not exact AND if
     * some words are not matched at all.
     * @return
     */
    public double getTextScore() {
        return textScore;
    }
    /**
     * How well matches the label. Sets the tokens of the
     * Label in relation to the matched tokens in the text. Also
     * considers that tokens might not match perfectly.<p>
     * This score get low if the labels defines a lot of additional
     * tokens that are not present in the Text.
     * @return
     */
    public double getLabelScore() {
        return labelScore;
    }
    /**
     * The actual label of the {@link #getResult() result} that produced the
     * based match for the given search tokens.
     * @return the label
     */
    public Literal getMatchedLabel() {
        return label;
    }
    /**
     * Getter for the number of Tokens of the label. Usually needed to calculate
     * the score (how good the label matches)
     * @return the labelTokenCount
     */
    public int getLabelTokenCount() {
        return labelTokenCount;
    }
    /**
     * Getter for the the type of the match
     * @return The type of the match
     */
    public MATCH getMatch() {
        return match;
    }
    /**
     * The overall score how well the label matches the text.
     * This is the product of the {@link #getLabelScore() labelScore} 
     * with the {@link #getTextScore()}
     * @return the overall score [0..1]
     */
    public double getMatchScore() {
        return score;
    }
    /**
     * Getter for the number of the token matched by this suggestion
     * @return The number of the token matched by this suggestion
     */
    public int getSpan() {
        return span;
    }
    /**
     * Getter for the start index of this Suggestion
     * @return the start token index for this suggestion
     */
    public int getStart() {
        return start;
    }
    /**
     * Getter for the he number of matching tokens.
     * @return The number of matching tokens.
     */
    public int getMatchCount() {
        return processableMatchCount;
    }
    
    @Override
    public String toString() {
        if(match == MATCH.NONE){
            return "no match";
        }
        StringBuilder sb = new StringBuilder(label.getLexicalForm());
        sb.append("[m=").append(match);
        sb.append(",s=").append(span);
        sb.append(",c=").append(processableMatchCount).append('(').append(tokenMatchScore).append(")/").append(labelTokenCount);
        sb.append("] score=").append(score).append("[l=").append(labelScore).append(",t=").append(textScore).append(']');
        return sb.toString();
    }

    /**
     * Compares {@link LabelMatch} first based on the {@link LabelMatch#getMatchCount()} 
     * number of matched tokens. If the number of the matched tokens is equals or
     * any of the parsed {@link Suggestion} instances has {@link MATCH#NONE} it
     * forwards the request to the {@link #MATCH_TYPE_SUGGESTION_COMPARATOR}.
     */
    public static final Comparator<LabelMatch> DEFAULT_LABEL_TOKEN_COMPARATOR = new Comparator<LabelMatch>() {
        @Override
        public int compare(LabelMatch arg0, LabelMatch arg1) {
            if(arg0.match.ordinal() >= MATCH.FULL.ordinal() && //for FULL or EXACT matches
                    arg1.match.ordinal() >= MATCH.FULL.ordinal()){
                return arg1.processableMatchCount - arg0.processableMatchCount; //bigger should be first
            } else if(arg0.match == arg1.match){ //also if the MATCH type is equals
                return arg1.processableMatchCount - arg0.processableMatchCount; //bigger should be first
            } else { //sort by MATCH type
                return arg1.match.ordinal() - arg0.match.ordinal(); //higher ordinal first
            }
// OLD IMPLEMENTATION
//            if(arg0.match == MATCH.NONE || arg1.match == MATCH.NONE ||
//                    arg0.processableMatchCount == arg1.processableMatchCount){
//                return arg1.match.ordinal() - arg0.match.ordinal(); //higher ordinal first
//            } else {
//                return arg1.processableMatchCount - arg0.processableMatchCount; //bigger should be first
//            }
        }
    };

}

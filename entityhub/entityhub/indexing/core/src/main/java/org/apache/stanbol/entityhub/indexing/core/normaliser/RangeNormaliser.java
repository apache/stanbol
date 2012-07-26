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
package org.apache.stanbol.entityhub.indexing.core.normaliser;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Normalises scores to the range [0..max]. it supports forwarding the parsed
 * scores to an other {@link ScoreNormaliser}.
 * @author Rupert Westenthaler
 *
 */
public class RangeNormaliser implements ScoreNormaliser {

    public static final String KEY_UPPER_BOUND = "upper-bound";
    public static final String KEY_MAX_EXPECTED_SCORE = "max-expected-score";
    
    private static final Logger log = LoggerFactory.getLogger(RangeNormaliser.class);

    private double upperBound;
    private ScoreNormaliser normaliser;
    private double maxScore = -1;
    /**
     * Normalises values parsed to {@link #normalise(float)} to [0..1] assuming
     * that the first call to {@link #normalise(float)} will parsed the higest
     * value.
     */
    public RangeNormaliser(){
        this(null,null,null);
    }
    /**
     * Uses the parsed {@link ScoreNormaliser} and further normalises results to
     * [0..1] assuming that the first call to {@link #normalise(float)} will 
     * parsed the highest value. 
     * @param normaliser the normaliser used to normalise parsed scores before
     * putting them to the range [0..1]
     */
    public RangeNormaliser(ScoreNormaliser normaliser){
        this(normaliser,null,null);
    }
    /**
     * Constructs an RangeNormalizer that forwards to the parsed normaliser but
     * keeps results within the range [0..{upperBound}] based on the provided
     * {maxScore} expected to be parsed to {@link #normalise(float)};
     * @param normaliser The normaliser called to process scores parsed to 
     * {@link #normalise(float)}. If <code>null</code> than parsed scores are
     * only normalised to the range [0..{upperBound}]
     * @param upperBound the upper bound for the range. If <code>null</code> is
     * parsed the range [0..1] will be used.
     * @param maxExpectedScore the maximum expected score. If this value is &lt;
     * 0 or <code>null</code> than the first score parsed to 
     * {@link #normalise(float)} will be used instead. This feature is useful if
     * entities are already sorted by there score.
     */
    public RangeNormaliser(ScoreNormaliser normaliser,Float upperBound,Float maxExpectedScore) {
        if(normaliser == null){
            this.normaliser = new DefaultNormaliser();
        } else {
            this.normaliser = normaliser;
        }
        if(upperBound == null){
            this.upperBound = 1;
        } else if(upperBound > 0){
            this.upperBound = upperBound;
        } else {
            throw new IllegalArgumentException("The parsed upperBound MUST NOT be <= 0. Parse NULL (to use the default) or values > 0!");
        }
        if(maxExpectedScore != null && maxExpectedScore > 0){
            normalise(maxExpectedScore); //call normalise for initialisation of maxScore
        } else if(maxExpectedScore != null){
            throw new IllegalArgumentException("The parsed maxExpectedScore MUST NOT be <= 0. Parse NULL (to use the first value parsed to normalise(..)) or values > 0!");
        } //else maxExpectedScore == null -> will use the first call to init maxExpectedScore!
    }  
    
    @Override
    public Float normalise(Float parsed) {
        parsed = normaliser.normalise(parsed);
        if(parsed == null || parsed.compareTo(ZERO) < 0){
            return parsed;
        }
        double score = parsed.doubleValue();
        if(maxScore<0){ //set based on the first call
            maxScore = score;
        } else if(score > maxScore){
            //print a warning if the first call does not parse the higest score
            log.warn("Found higer Score than of the first parsed value. This will cause all scores to exeed the range [0..1]");
        }
        return Float.valueOf((float)(upperBound*(score/maxScore)));
    }
    @Override
    public void setConfiguration(Map<String,Object> config) {
        Object value = config.get(CHAINED_SCORE_NORMALISER);
        if(value != null){
            normaliser = (ScoreNormaliser)value;
        }
        value = config.get(KEY_UPPER_BOUND);
        if(value != null) {
            upperBound = Double.valueOf(value.toString());
            if(upperBound <= 0){
                throw new IllegalArgumentException("The upper bound '"+upperBound+"' MUST BE > zero!");
            }
        } //else [0..1]
        value = config.get(KEY_MAX_EXPECTED_SCORE);
        if(value != null){
            Float maxExpected = Float.valueOf(value.toString());
            normalise(maxExpected);
        } // else none
    }
    @Override
    public ScoreNormaliser getChained() {
        return normaliser;
    }

}

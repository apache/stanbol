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


/**
 * This normaliser will return -1 for scores lower than the minimum parsed to the
 * constructor. Because of Entities with score &lt;0 are typically not indexed
 * this can be used to filter Entities based on there score.<p>
 * This normaliser also supports forwarding the score to an other {@link ScoreNormaliser}.
 * The filtering is calculated based on the results of this normaliser. To
 * perform the minimum score on the original scores one should not parse an
 * {@link ScoreNormaliser} in the constructor
 * @author Rupert Westenthaler
 *
 */
public class MinScoreNormalizer implements ScoreNormaliser {

    public static final String KEY_INCLUSIVE = "inclusive";
    public static final String KEY_MIN_SCORE = "min-score";
    
    private Float minScore;
    private ScoreNormaliser normaliser;
    private boolean inclusive;
    public MinScoreNormalizer(){
        this(0,true,null);
    }
    /**
     * Constructs a normaliser that returns -1 for scores lower (if inclusive is 
     * <code>false</code> lower equals) to the minimum required score. In case
     * an other normaliser is parsed than scores parsed to {@link #normalise(float)}
     * are first processed by this normaliser
     * @param minimumRequiredScore the minimum required score. MUST BE &gt; 0
     * @param inclusive if scores equals to the required minimum are accepted
     * @param normaliser the normaliser used to process parsed scores or
     * <code>null</code> to use none.
     */
    public MinScoreNormalizer(float minimumRequiredScore, boolean inclusive,ScoreNormaliser normaliser) {
        if(minimumRequiredScore < 0){
            throw new IllegalArgumentException("The parsed minimum required score MUST BE >= 0");
        }
        this.inclusive = inclusive;
        this.minScore = minimumRequiredScore;
        this.normaliser = normaliser;
    }
    
    @Override
    public void setConfiguration(Map<String,Object> config) {
        Object value = config.get(KEY_INCLUSIVE);
        if(value != null){
            inclusive = Boolean.parseBoolean(value.toString());
        } //else default true
        value = config.get(KEY_MIN_SCORE);
        if(value != null){
            minScore = Float.valueOf(value.toString());
            if(minScore.floatValue() <= 0){
                throw new IllegalArgumentException("The parsed minScore value '"+value+"'MUST BE greater than 0");
            }
        } //else default null
        value = config.get(CHAINED_SCORE_NORMALISER);
        if(value != null){
            normaliser = (ScoreNormaliser)value;
        }
    }
    /**
     * Constructs an normaliser that returns -1 for all scores lower than the
     * minimum required score 
     * @param minimumRequiredScore the minimum required score. MUST BE &gt; 0
     */
    public MinScoreNormalizer(float minimumRequiredScore){
        this(minimumRequiredScore,true, null);
    }
    
    @Override
    public Float normalise(Float score) {
        if(normaliser != null){
            score = normaliser.normalise(score);
        }
        if(score == null || score.compareTo(ZERO) < 0){
            return score;
        }
        int compare = score.compareTo(minScore);
        return (inclusive && compare < 0) || //score == minScore is OK
                (!inclusive && compare <= 0)? //score == minScore is not OK
                        MINUS_ONE:score; 
    }
    @Override
    public ScoreNormaliser getChained() {
        return normaliser;
    }

}

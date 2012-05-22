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
package org.apache.stanbol.enhancer.engines.entitytagging.impl;

import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Text;

/**
 * A suggestion of an {@link Entity} for a fise:TextAnnotation processed
 * by the NamedEntityTaggingEngine
 * @author Rupert Westenthaler
 */
public class Suggestion implements Comparable<Suggestion>{
    private final Entity entity;
    private double levenshtein = -1;
    private Double score;
    private Text matchedLabel;

    protected Suggestion(Entity entity){
        this.entity = entity;
    }
    
    
    /**
     * @return the levenshtein
     */
    public final double getLevenshtein() {
        return levenshtein;
    }


    /**
     * @param levenshtein the levenshtein to set
     */
    protected final void setLevenshtein(double levenshtein) {
        this.levenshtein = levenshtein;
    }


    /**
     * @return the score
     */
    public final Double getScore() {
        return score;
    }


    /**
     * @param score the score to set
     */
    protected final void setScore(Double score) {
        this.score = score;
    }


    /**
     * @return the matchedLabel
     */
    public final Text getMatchedLabel() {
        return matchedLabel;
    }


    /**
     * @param matchedLabel the matchedLabel to set
     */
    protected final void setMatchedLabel(Text matchedLabel) {
        this.matchedLabel = matchedLabel;
    }


    /**
     * @return the entity
     */
    public final Entity getEntity() {
        return entity;
    }


    @Override
    public int compareTo(Suggestion other) {
        return other.score.compareTo(score);
    }
    
    
}
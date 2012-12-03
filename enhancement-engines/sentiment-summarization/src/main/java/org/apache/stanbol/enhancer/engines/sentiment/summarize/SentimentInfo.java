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
package org.apache.stanbol.enhancer.engines.sentiment.summarize;

import org.apache.stanbol.enhancer.nlp.model.Section;

/**
 * Holds the information about the Sentiment tags found for a
 * {@link Section}
 * @author Rupert Westenthaler
 *
 */
public final class SentimentInfo {

    private Section section;
    private double positive = 0;
    private double negative = 0;

    SentimentInfo(Section section){
        if(section == null){
            throw new IllegalArgumentException("The parsed Section MUST NOT be NULL");
        }
         this.section = section;
    }
    
    public Double getPositive(){
        return positive > 0.0 ? positive : null;
    }
    
    public Double getNegative(){
        return negative < 0.0 ? negative : null;
    }
    
    public Double getSentiment(){
        return negative+positive;
    }
    
    public boolean hasSentiment(){
        return positive != 0 || negative != 0;
    }
    
    void addSentiment(Double sentiment){
        if(sentiment == null){ //ignore null
            return;
        }
        if(sentiment > 0){
            positive = positive + sentiment;
        } else if (sentiment < 0){
            negative = negative + sentiment;
        }
    }
    
    public Section getSection() {
        return section;
    }
    
}

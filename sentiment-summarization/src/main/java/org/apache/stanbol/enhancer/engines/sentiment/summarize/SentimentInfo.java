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

import java.util.ArrayList;
import java.util.List;

import org.apache.stanbol.enhancer.nlp.model.Section;

/**
 * Holds the information about the Sentiment tags found for a
 * {@link Section}.
 * Values returned by the getters are the geometric mean normalised by the
 * number of tags with an sentiment value. By this words with an high sentiment
 * value will have much more influence as those with lower values
 * @author Rupert Westenthaler
 *
 */
public final class SentimentInfo {

    private Section section;
    private List<Sentiment> sentiments;

    SentimentInfo(Section section){
        if(section == null){
            throw new IllegalArgumentException("The parsed Section MUST NOT be NULL");
        }
         this.section = section;
    }
    
    public Double getPositive(){
        if(sentiments == null){
            return null;
        }
        double positive = 0;
        int num = 0;
        for(Sentiment s : sentiments){
            double v = s.getValue();
            if(v > 0){
                positive = positive+(v*v);
                num++;
            }
        }
        return positive > 0.0 ? Math.sqrt(positive/(double)num) : null;
    }
    
    public Double getNegative(){
        if(sentiments == null){
            return null;
        }
        double negative = 0;
        int num = 0;
        for(Sentiment s : sentiments){
            double v = s.getValue();
            if(v < 0){
                negative = negative+(v*v);
                num++;
            }
        }
        return negative > 0.0 ? Math.sqrt(negative/(double)num)*-1 : null;
    }
    
    public Double getSentiment(){
        if(sentiments == null){
            return null;
        }
        Double pos = getPositive();
        Double neg = getNegative();
        if(pos == null && neg == null){
            return null;
        }
        double sum = pos != null ? pos : 0.0;
        sum = neg != null ? sum+neg : sum;
        return sum;
    }
    
    public boolean hasSentiment(){
        return sentiments != null;
    }
        
    void addSentiment(Sentiment sentiment){
        if(sentiments == null){
            sentiments = new ArrayList<Sentiment>(4);
        }
        sentiments.add(sentiment);
    }
    
    public Section getSection() {
        return section;
    }
    
    public List<Sentiment> getSentiments() {
        return sentiments;
    }
    
}

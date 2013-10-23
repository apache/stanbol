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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.stanbol.enhancer.nlp.model.Sentence;
import org.apache.stanbol.enhancer.nlp.model.Token;

/**
 * Represents phrases in a sentence that do hold a Sentiment value.
 * Phrases are defined by collecting {@link Sentiment}s that refer the same 
 * {@link Sentiment#getAboutness()}
 * @author Rupert Westenthaler
 */
public class SentimentPhrase {
    
    private Set<Token> nouns = new HashSet<Token>();
    int start = Integer.MAX_VALUE;
    int end = Integer.MIN_VALUE;    
    private List<Sentiment> sentiments = new ArrayList<Sentiment>(4);
    /**
     * lazzy initialised on the first call of 
     */
    private Double[] __sentiment;
    
    /**
     * Creates a single Noun sentiment Phrase
     * @param noun the noun
     * @param index the index of the word relative to the sentence
     */
    public SentimentPhrase(Sentiment sentiment) {
        addSentiment(sentiment);
    }
    /**
     * Adds a Sentiment to the Phrase
     * @param sentiment the sentiment to add
     */
    public void addSentiment(Sentiment sentiment){
        sentiments.add(sentiment);
        nouns.addAll(sentiment.getAboutness());
        if(start > sentiment.getStart()){
            start = sentiment.getStart();
        }
        if(end < sentiment.getEnd()){
            end = sentiment.getEnd();
        }
    }
    /**
     * Getter for the positive sentiment value
     * @return the sentiment of <code>0.0</code> if no positive sentiment is present
     */
    public Double getPositiveSentiment(){
        if(__sentiment == null){
            summarizeSentimentValues();
        }
        return __sentiment[1];
    }
    /**
     * Getter for the negative sentiment value
     * @return the sentiment of <code>0.0</code> if no negative sentiment is present
     */
    public Double getNegativeSentiment(){
        if(__sentiment == null){
            summarizeSentimentValues();
        }
        return __sentiment[0];
    }
    /**
     * Getter for the average Sentiment value 
     * @return
     */
    public Double getSentiment(){
        if(__sentiment == null){
            summarizeSentimentValues();
        }
        return __sentiment[2];
    }
    /**
     * The Sentence containing this phrase or <code>null</code> if no
     * {@link Sentiment} was yet added
     * @return the sentence
     */
    public Sentence getSentence(){
        return sentiments.isEmpty() ? null : sentiments.get(0).getSentence();
    }
    
    private void summarizeSentimentValues(){
        double positive = 0;
        double negative = 0;
        for(Sentiment sentiment : sentiments){
            double value = sentiment.getValue();
            if(value < 0){
                if(negative == 0){
                    negative = Math.abs(value);
                } else {
                    negative = tnorm(negative, Math.abs(value));
                }
            } else if(value > 0){
                if(positive == 0){
                    positive = value;
                } else {
                    positive = tnorm(positive, value);
                }
            }
        }
        __sentiment = new Double[]{
                negative > 0 ? Double.valueOf(negative*-1) : null,
                positive > 0 ? Double.valueOf(positive) : null,
                Double.valueOf(positive-negative)};
    }
    
    /**
     * Combines two sentiment values of the same phrase
     * @param a
     * @param b
     * @return
     */
    private double tnorm(double a, double b){
        return (a+b)-(a*b);
        //return (a+b)/(1+(a*b));
    }
    
    /**
     * Start (char) index of this phrase
     * @return
     */
    public int getStartIndex() {
        return start;
    }
    
    /**
     * End (char) index of this phrase
     * @return
     */
    public int getEndIndex() {
        return end;
    }
    /**
     * The {@link Sentiment}s contained in this Phrase
     */
    public List<Sentiment> getSentiments() {
        return sentiments;
    }
    @Override
    public String toString() {
        return new StringBuilder("SentimentPhrase[").append(start).append(',').append(end).append(']')
                .append(" positive: ").append(sentiments.get(0)).append("| negative: ").append(sentiments.get(1)).toString();
    }
}

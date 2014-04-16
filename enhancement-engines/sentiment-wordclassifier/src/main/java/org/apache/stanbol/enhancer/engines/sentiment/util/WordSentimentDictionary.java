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

package org.apache.stanbol.enhancer.engines.sentiment.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.stanbol.enhancer.engines.sentiment.api.SentimentClassifier;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;

/**
 * <code>{Word,Category} -&gt; {Sentiment}</code> Dictionary intended to be
 * used by {@link SentimentClassifier} implementation to hold the dictionary.<p>
 * This implementation is thread save.
 * 
 * @author Rupert Westenthaler
 *
 */
public class WordSentimentDictionary {

    
    private final ReadWriteLock lock;
    private final Map<String,Map<LexicalCategory,Double>> wordMap;
    private final Locale locale;
    private int sentCount; //the number of wordSentiments

    /**
     * Create a word sentiment directory for the given locale.
     * @param locale the locale used to convert words to lower case. If
     * <code>null</code> {@link Locale#ROOT} will be used.
     */
    public WordSentimentDictionary(Locale locale){
        this.wordMap = new HashMap<String,Map<LexicalCategory,Double>>();
        this.lock = new ReentrantReadWriteLock();
        this.locale = locale == null ? Locale.ROOT : locale;
    }
    
    /**
     * Puts (adds/updates) a word (with unknown {@link LexicalCategory})
     * to the dictionary
     * @param word the word.
     * @param sentiment the sentiment value
     * @return the old sentiment value or <code>null</code> if none.
     */
    public Double updateSentiment(String word, Double sentiment){
        return updateSentiment(null, word, sentiment);
    }
    /**
     * Puts (adds/updates) a word with {@link LexicalCategory} to the dictionary.
     * @param cat the {@link LexicalCategory} of the word or <code>null</code> if not known
     * @param word the word 
     * @param sentiment the sentiment value or <code>null</code> to remove this
     *     mapping.
     * @return the old sentiment value or <code>null</code> if none.
     */
    public Double updateSentiment(LexicalCategory cat, String word, Double sentiment){
        word = word.toLowerCase(locale);
        Double old = null;
        lock.writeLock().lock();
        try {
            Map<LexicalCategory,Double> entry = wordMap.get(word);
            //most elements (99%) will only have a single value.
            //so we use a singleton map as default and create a HashMap for those
            //that do have more elements (to save memory)
            boolean replace = false;
            if(entry == null && sentiment != null){
                entry = Collections.singletonMap(cat, sentiment);
                replace = true;
            } else if(entry != null){
                if(entry.size() == 1){ //special case
                    if(sentiment == null) {
                        old = entry.get(cat);
                        if(old != null){ //remove
                            entry = null;
                            replace = true;
                        } //not found -> do nothing
                    } else { //about to add 2nd element
                        //create a normal HashMap and add the existing value;
                        entry = new HashMap<LexicalCategory,Double>(entry);
                        replace = true;
                    }
                }
                if(sentiment == null){
                    if(entry != null && entry.size() > 1){
                        old = entry.remove(cat);
                        if(old != null && entry.size() == 1){ //only one entry left
                            //switch back to a singletonMap
                            Entry<LexicalCategory,Double> lastEntry = entry.entrySet().iterator().next();
                            entry = Collections.singletonMap(lastEntry.getKey(), lastEntry.getValue());
                            replace = true;
                        }
                    } //else already processed by special case size == 1
                } else {
                    old = entry.put(cat, sentiment);
                }
            } //else entry == null and sentiment == null ... nothing to do
            if(replace){ //we have changed the entry instance and need to put the word
                if(entry == null){
                    wordMap.remove(word);
                } else {
                    wordMap.put(word, entry);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
        if(old == null && sentiment != null){
            sentCount++; //we added a new sentiment
        } else if(old != null && sentiment == null){
            sentCount--;
        } //else no change
        return old;
    }

    /**
     * Getter for the sentiment value for the word. If multiple sentiments
     * for different {@link LexicalCategory lexical categories} are registered
     * for the word this will return the average of those.
     * @param word the word
     * @return the sentiment or <code>null</code> if not in the dictionary.
     */
    public Double getSentiment(String word){
        return getSentiment(null, word);
    }
    /**
     * Getter for the sentiment for the parsed word and {@link LexicalCategory}.
     * In case the category is <code>null</code> this method might parse an
     * average over different sentiments registered for different lexical
     * categories.
     * @param cat the category
     * @param word the word
     * @return the sentiment or <code>null</code> if the not in the dictionary.
     */
    public Double getSentiment(LexicalCategory cat, String word){
        lock.readLock().lock();
        try {
            Map<LexicalCategory,Double> sentiments = wordMap.get(word.toLowerCase(locale));
            if(sentiments != null){
                Double sentiment = sentiments.get(cat);
                if(sentiment == null && cat == null && !sentiments.isEmpty()){
                    if(sentiments.size() == 1) {
                        sentiment = sentiments.values().iterator().next();
                    } else {
                        double avgSent = 0;
                        for(Double sent : sentiments.values()){
                            avgSent = avgSent + sent;
                        }
                        sentiment = Double.valueOf(avgSent/(double)sentiments.size());
                    }
                }
                return sentiment;
            } else {
                return null;
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /** 
     * The number of words in the dictionary. NOTE that a single word
     * might have multiple sentiments for different {@link LexicalCategory}.
     * So this value might be lower to {@link #size()} 
     **/
    public int getWordCount() {
        lock.readLock().lock();
        try {
            return wordMap.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    /**
     * The number of word sentiments in the dictionary
     * @return
     */
    public int size(){
        return sentCount;
    }

    /**
     * removes all entries of this dictionary.
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            wordMap.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    
}

/*
 * Copyright (c) 2012 Sebastian Schaffert
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.stanbol.enhancer.engines.sentiment.api;

import java.util.Set;

import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
import org.osgi.framework.BundleContext;

/**
 * A simple classifier that determines for a given word a positive or negative 
 * value between -1 and 1. Unknown words will return the value 0.
 * <p/>
 * This Interface need to be implemented by Sentiment frameworks so that they
 * can be used with this engine. Implementations need to be 
 * {@link BundleContext#registerService(String, Object, java.util.Dictionary)
 * registered as OSGI service}.
 * @see LexicalCategoryClassifier
 * 
 * @author Sebastian Schaffert
 * @author Rupert Westenthaler
 */
public interface SentimentClassifier {

    /**
     * Given the word passed as argument, return a value between -1 and 1 indicating its sentiment value from
     * very negative to very positive. Unknown words should return the value 0.
     *
     * @param cat the lexical category of the word (see 
     * <a href="https://issues.apache.org/jira/browse/STANBOL-1151">STANBOL-1151</a>)
     * @param word the word
     * @return
     */
    public double classifyWord(LexicalCategory cat, String word);


    /**
     * Getter for the LexicalCategories for the parsed {@link PosTag}. Used
     * to lookup the lexical categories for the 
     * {@link #classifyWord(LexicalCategory, String)} lookups.<p>
     * Simple implementations might return {@link PosTag#getCategories()}. But
     * as some {@link PosTag} instances might only define the literal
     * {@link PosTag#getTag()} value this method might also implement its own
     * mappings.
     * @param posTag the posTag
     * @return the categories 
     */
    public Set<LexicalCategory> getCategories(PosTag posTag);
    
    /**
     * The language of this WordClassifier
     * @return the language
     */
    public String getLanguage();
}

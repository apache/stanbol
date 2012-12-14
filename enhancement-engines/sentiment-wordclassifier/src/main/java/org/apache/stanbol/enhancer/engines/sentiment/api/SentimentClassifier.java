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
 * @author Sebastian Schaffert
 */
public interface SentimentClassifier {

    /**
     * Given the word passed as argument, return a value between -1 and 1 indicating its sentiment value from
     * very negative to very positive. Unknown words should return the value 0.
     *
     * @param word
     * @return
     */
    public double classifyWord(String word);


    /**
     * Helper method. Return true if the given POS tag indicates an adjective in the language implemented by
     * this classifier.
     *
     * @param posTag
     * @return
     */
    public boolean isAdjective(PosTag posTag);

    /**
     * Helper method. Return true if the given POS tag indicates a noun in the language implemented by this
     * classifier.
     *
     * @param posTag
     * @return
     */
    public boolean isNoun(PosTag posTag);
    
    /**
     * The language of this WordClassifier
     * @return the language
     */
    public String getLanguage();
}

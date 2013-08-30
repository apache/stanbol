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

/**
 * Implements the {@link #isAdjective(PosTag)} and {@link #isNoun(PosTag)}
 * methods by using the {@link LexicalCategory} of the parsed {@link PosTag}.<p>
 * This should be sufficient for all POS TagSets that are mapped to 
 * {@link LexicalCategory}. For other TagSets users will need to manually
 * check {@link PosTag#getTag()}.
 * 
 * @author Rupert Westenthaler
 *
 */
public abstract class LexicalCategoryClassifier implements SentimentClassifier {

    public abstract double classifyWord(LexicalCategory cat, String word);

    @Override
    public Set<LexicalCategory> getCategories(PosTag posTag) {
        return posTag.getCategories();
    }

}

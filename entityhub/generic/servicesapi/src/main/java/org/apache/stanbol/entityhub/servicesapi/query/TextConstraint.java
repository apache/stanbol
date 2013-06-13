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
package org.apache.stanbol.entityhub.servicesapi.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


public class TextConstraint extends Constraint {

    public enum PatternType {
        /**
         * Simple checks if the parsed constraint equals the value
         */
        none,
        /**
         * All kind of REGEX Patterns
         */
        regex,
        /**
         * WildCard based queries using * and ?
         */
        wildcard
        //TODO maybe add Prefix as additional type
    }
    private final PatternType wildcardType;
    private final Set<String> languages;
    private final boolean caseSensitive;
    private final List<String> texts;
    /**
     * If enabled the proximity of query terms will be used for ranking the 
     * results.
     */
    private boolean proximityRanking;
    /**
     * Creates a TextConstraint for multiple texts and languages. Parsed texts
     * are connected using OR and may appear in any of the parsed languages.
     * @param text the texts or <code>null</code> to search for any text in active languages
     * @param languages the set of active languages
     */
    public TextConstraint(List<String> text,String...languages) {
        this(text,PatternType.none,false,languages);
    }
    /**
     * Creates a TextConstraint for a text and languages.
     * @param text the text or <code>null</code> to search for any text in active languages
     * @param languages the set of active languages.
     */
    public TextConstraint(String text,String...languages) {
        this(text == null || text.isEmpty() ? null : Collections.singletonList(text),
                PatternType.none,false,languages);
    }
    public TextConstraint(List<String> text,boolean caseSensitive,String...languages) {
        this(text,PatternType.none,caseSensitive,languages);
    }
    public TextConstraint(String text,boolean caseSensitive,String...languages) {
        this(text == null || text.isEmpty() ? null : Collections.singletonList(text),
                PatternType.none,caseSensitive,languages);
    }
    public TextConstraint(String text,PatternType wildcardType,boolean caseSensitive,String...languages) {
        this(text == null || text.isEmpty() ? null : Collections.singletonList(text),
                wildcardType,caseSensitive,languages);
    }
    public TextConstraint(List<String> text,PatternType wildcardType,boolean caseSensitive,String...languages) {
        super(ConstraintType.text);
        //create a local copy and filter null and empty elements
        if(text == null || text.isEmpty()){
            this.texts = null;
        } else {
            List<String> processedText = new ArrayList<String>(text);
            for(Iterator<String> constraints = processedText.iterator();constraints.hasNext();){
                String constraint = constraints.next();
                if(constraint == null || constraint.isEmpty()){
                    constraints.remove(); //remove null and empty elements
                }
            }
            if(processedText.isEmpty()){
                this.texts = null;
            } else {
                this.texts = Collections.unmodifiableList(processedText);
            }
        }
        //check that we have at least a text or a language
        if(this.texts == null && (languages == null || languages.length<1)){
            throw new IllegalArgumentException("Text Constraint MUST define a non empty text OR a non empty list of language constraints");
        }
        if(wildcardType == null){
            this.wildcardType = PatternType.none;
        } else {
            this.wildcardType = wildcardType;
        }
        if(languages==null){
            this.languages = Collections.emptySet();
        } else {
            /*
             * Implementation NOTE:
             *   We need to use a LinkedHashSet here to
             *    1) ensure that there are no duplicates and
             *    2) ensure ordering of the parsed constraints
             *   Both is important: Duplicates might result in necessary calculations
             *   and ordering might be important for users that expect that the
             *   language parsed first is used as the preferred one
             */
            this.languages = Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(languages)));
        }
        this.caseSensitive = caseSensitive;

    }
    /**
     * The pattern type to be used for this query.
     * @return the wildcardType
     */
    public final PatternType getPatternType() {
        return wildcardType;
    }
    /**
     * The set of languages for this query.
     * @return the languages
     */
    public final Set<String> getLanguages() {
        return languages;
    }
    /**
     * If the query is case sensitive
     * @return the caseSensitive state
     */
    public final boolean isCaseSensitive() {
        return caseSensitive;
    }
    /**
     * Getter for the text constraints. Multiple constraints need to be connected
     * with OR. For AND simple post all required words in a single String.
     * @return the text constraints
     */
    public final List<String> getTexts() {
        return texts;
    }
        
    /**
     * Getter for the first text constraint. If multiple constrains are set only
     * the first one will be returned.
     * @return the fist text constraint (of possible multiple text constraints)
     * @deprecated 
     */
    @Deprecated
    public final String getText(){
        return texts == null || texts.isEmpty() ? null : texts.get(0);
    }
    /**
     * Getter for the Term Proximity state. If enabled the proximity of the
     * parsed terms should be used to rank search results.
     * @return the termProximity or <code>null</code> if not specified
     */
    public Boolean isProximityRanking() {
        return proximityRanking;
    }
    /**
     * Setter for the proximity ranking state. If enabled the proximity of the
     * parsed terms should be used to rank search results.
     * @param state the proximity ranking state to set
     */
    public void setProximityRanking(boolean state) {
        this.proximityRanking = state;
    }
    @Override
    public String toString() {
        return String.format("TextConstraint[value=%s|%s|case %sensitive|languages:%s]",
                texts,wildcardType.name(),caseSensitive?"":"in",languages);
    }

}

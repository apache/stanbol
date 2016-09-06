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

package org.apache.stanbol.enhancer.engines.poschunker;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.Pos;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;

/**
 * Definition of a phrase type<p>
 * 
 * Phrases are defined by a set of POS tags that can <ul>
 * <li> required Tokens - typically noun for noun phrases, verbs for verb phrases.
 * <li> start types - types that can start a new phrase
 * <li> prefix types - types that can continue a phrase not yet containing a
 * required token
 * <li> continuation types - types that can continue a phrase already containing
 * a required token
 * <li> end types - types that can end a phrase. Used to remove tailing tokens
 * from a phrase (typically punctations).
 * </ul>
 * 
 * <b>TODO:</b> Add support for {@link Pos} and String tags in addition to
 * {@link LexicalCategory}.
 * 
 * @author Rupert Westenthaler
 *
 */
public class PhraseTypeDefinition {

    protected final LexicalCategory phraseType;
    
    private final TokenTypeDefinition startTypeDefinition;
    private final TokenTypeDefinition prefixTypeDefinition;
    private final TokenTypeDefinition continuationTypeDefinition;
    private final TokenTypeDefinition requiredTypeDefinition;
    private final TokenTypeDefinition endTypeDefinition;
    
    public PhraseTypeDefinition(LexicalCategory phraseType) {
        if(phraseType == null){
            throw new IllegalArgumentException("The parsed PhraseType MUST NOT be NULL!");
        }
        this.phraseType = phraseType;
        startTypeDefinition = new TokenTypeDefinition(phraseType);
        prefixTypeDefinition = new TokenTypeDefinition(phraseType);
        continuationTypeDefinition = new TokenTypeDefinition(phraseType);
        requiredTypeDefinition = new TokenTypeDefinition(phraseType);
        endTypeDefinition = new TokenTypeDefinition(phraseType);
    }
    
    /**
     * Getter for the type of this phrase definition
     * @return
     */
    public LexicalCategory getPhraseType(){
        return phraseType;
    }
    
    /**
     * Getter for the read only set with the start types.
     * @return the read only set with {@link LexicalCategory LexicalCategories}
     * that can start a phrase of that type
     */
    public TokenTypeDefinition getStartType(){
        return startTypeDefinition;
    }
    /**
     * Getter for the read only set with the prefix types
     * @return the read only set with {@link LexicalCategory LexicalCategories}
     * that can continue a phrase that does not yet include a token classified
     * with a {@link #getRequiredType() required type}. A typical Example are
     * {@link LexicalCategory#Adjective} in Noun Phrases that need to be
     * considered in prefixes (e.g. "A nice weekend") but excluded after the
     * first noun (e.g. "the trip last week"). 
     */
    public TokenTypeDefinition getPrefixType(){
        return prefixTypeDefinition;
    }
    
    /**
     * Getter for the read only set with the continuation types
     * @return the read only set with {@link LexicalCategory LexicalCategories}
     * that can continue a phrase that does already include a token classified
     * with a {@link #getRequiredType() required type}. A typical Example are
     * {@link LexicalCategory#Adjective} in Noun Phrases that need to be
     * considered in prefixes (e.g. "A nice weekend") but excluded after the
     * first noun (e.g. "the trip last week"). 
     */
    public TokenTypeDefinition getContinuationType(){
        return continuationTypeDefinition;
    }
    
    /**
     * Getter for the read only set with the required types
     * @return the read only set with {@link LexicalCategory LexicalCategories}
     * that MUST occur within a phrase of that type
     */
    public TokenTypeDefinition getRequiredType(){
        return requiredTypeDefinition;
    }
    
    /**
     * Getter for the read only set with the end types.
     * @return the read only set with {@link LexicalCategory LexicalCategories}
     * that can end a phrase of that type
     */
    public TokenTypeDefinition getEndType(){
        return endTypeDefinition;
    }
    
    @Override
    public String toString() {
    	return phraseType.name();
    }
    
    public static class TokenTypeDefinition {
        
        private final Set<LexicalCategory> categories = EnumSet.noneOf(LexicalCategory.class);
        private Set<Pos> posTags = EnumSet.noneOf(Pos.class);
        private Set<Pos> excludedPosTags = EnumSet.noneOf(Pos.class);
        private Set<String> tags = new HashSet<String>();
        
        /**
         * Used by the constructor of the {@link PhraseTypeDefinition} class
         * @param lc
         */
        private TokenTypeDefinition(LexicalCategory lc){
            this(Collections.singleton(lc),null);
        }
        
        public TokenTypeDefinition(Set<LexicalCategory> categories, Set<Pos> posTags, String...tags) {
            if(categories != null){
                for(LexicalCategory lc : categories){
                    if(lc != null){
                        this.categories.add(lc);
                    }
                }
            }
            if(posTags != null){
                for(Pos pos : posTags){
                    if(pos != null){
                        this.posTags.add(pos);
                    }
                }
            }
            if(tags != null){
                for(String tag : tags){
                    if(tag != null){
                        this.tags.add(tag);
                    }
                }
            }
        }
        /**
         * Read-/writeable set of {@link LexicalCategory LexicalCategories}
         * @return the set of lexical categories
         */
        public Set<LexicalCategory> getCategories() {
            return categories;
        }
        /**
         * Adds the parsed {@link LexicalCategory LexicalCategories}
         * @param categories the LexicalCategories
         * @return if the {@link TokenTypeDefinition} was updated by this operation
         */
        public boolean addCategories(LexicalCategory...categories){
            return add(this.categories, categories);
        }
        
        /**
         * Removes the parsed {@link LexicalCategory LexicalCategories}
         * @param categories the LexicalCategories
         * @return if the {@link TokenTypeDefinition} was updated by this operation
         */
        public boolean removeCategories(LexicalCategory...categories){
            return remove(this.categories, categories);
        }
        
        /**
         * Read-/writeable set of {@link Pos} tags
         * @return the set of POS tags
         */
        public Set<Pos> getPosTags() {
            return posTags;
        }
        
        /**
         * Adds the parsed {@link Pos} tags
         * @param pos the {@link Pos} tags
         * @return if the {@link TokenTypeDefinition} was updated by this operation
         */
        public boolean addPosTags(Pos...pos){
            return add(this.posTags, pos);
        }
        
        /**
         * Removes the parsed {@link Pos} tags
         * @param pos the {@link Pos} tags
         * @return if the {@link TokenTypeDefinition} was updated by this operation
         */
        public boolean removePosTags(Pos...pos){
            return remove(this.posTags, pos);
        }
        
        /**
         * Read-/writeable set of excluded {@link Pos} tags. This allows to
         * include a {@link LexicalCategory} but to exclude some specific 
         * {@link Pos} member of this category.
         * @return the set of excluded POS tags
         */
        public Set<Pos> getExcludedPosTags() {
            return excludedPosTags;
        }
        
        /**
         * Adds the parsed {@link Pos} tags to the set of excluded {@link Pos} tags
         * @param pos the {@link Pos} tags
         * @return if the {@link TokenTypeDefinition} was updated by this operation
         */
        public boolean addExcludedPosTags(Pos...pos){
            return add(this.excludedPosTags, pos);
        }
        
        /**
         * Removes the parsed {@link Pos} tags to the set of excluded {@link Pos} tags
         * @param pos the {@link Pos} tags
         * @return if the {@link TokenTypeDefinition} was updated by this operation
         */
        public boolean removeExcludedPosTags(Pos...pos){
            return remove(this.excludedPosTags, pos);
        }
        /**
         * Read-/writeable set of string tags (as provided by the POS tagger)
         * @return the set of String tags
         */
        public Set<String> getTags() {
            return tags;
        }
        /**
         * Adds the parsed tags
         * @param tag the tags
         * @return if the {@link TokenTypeDefinition} was updated by this operation
         */
        public boolean addTags(String...tag){
            return add(this.tags, tag);
        }
        
        /**
         * Removes the parsed tags
         * @param tag the tags
         * @return if the {@link TokenTypeDefinition} was updated by this operation
         */
        public boolean removeTags(String...tag){
            return remove(this.tags, tag);
        }
        
        /**
         * Checks if a posTag matches against this TokenTypeDefinition
         * @param posTag the posTag to check
         * @return <code>true</code> in case of a match. Otherwise <code>false</code>
         * @throws NullPointerException if the parsed posTag is <code>null</code>
         */
        public boolean matches(PosTag posTag){
            //check against incldues categories, posTags and tags
            boolean matches = 
                    (!Collections.disjoint(posTag.getCategories(), categories)) ||
                    (!Collections.disjoint(posTag.getPosHierarchy(), posTags)) ||
                    tags.contains(posTag.getTag());
            //if there is a match we need still to check for excluded POS tags
            return matches ? Collections.disjoint(posTag.getPosHierarchy(),excludedPosTags) :
                false;
        }
        
        private <T> boolean add(Set<T> set, T...types){
            boolean changed = false;
            if(types != null){
                for(T type : types){
                    if(type != null){
                        if(set.add(type)){
                            changed = true;
                        }
                    }
                }
            }
            return changed;
        }
        
        private <T> boolean remove(Set<T> set, T...types){
            boolean changed = false;
            if(types != null){
                for(T type : types){
                    if(type != null){
                        if(set.remove(type)){
                            changed = true;
                        }
                    }
                }
            }
            return changed;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if(!categories.isEmpty()){
                sb.append("Cat: ");
                boolean first = true;
                for(LexicalCategory lc : categories){
                    if(first){
                        first = false;
                    } else {
                        sb.append(", ");
                    }
                    sb.append(lc.name());
                }
            }
            if(!posTags.isEmpty()){
                if(sb.length() > 0){
                    sb.append(" | ");
                }
                sb.append("Pos: ");
                boolean first = true;
                for(Pos pos : posTags){
                    if(first){
                        first = false;
                    } else {
                        sb.append(", ");
                    }
                    sb.append(pos.name());
                }
            }
            if(!tags.isEmpty()){
                if(sb.length() > 0){
                    sb.append(" | ");
                }
                sb.append("Tags: ");
                boolean first = true;
                for(String tag : tags){
                    if(first){
                        first = false;
                    } else {
                        sb.append(", ");
                    }
                    sb.append(tag);
                }
            }
            if(!excludedPosTags.isEmpty()){
                if(sb.length() > 0){
                    sb.append(" | ");
                }
                sb.append("Excluded: ");
                boolean first = true;
                for(Pos pos : excludedPosTags){
                    if(first){
                        first = false;
                    } else {
                        sb.append(", ");
                    }
                    sb.append(pos.name());
                }
            }
            return sb.toString();
        }
    }
    
}

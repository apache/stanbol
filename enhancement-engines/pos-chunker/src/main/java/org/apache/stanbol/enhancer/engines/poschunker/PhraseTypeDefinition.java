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
import java.util.Set;

import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.Pos;

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
    
    private final Set<LexicalCategory> startTypes;
    protected final Set<LexicalCategory> readOnlyStartTypes;
    private final Set<LexicalCategory> prefixTypes;
    protected final Set<LexicalCategory> readOnlyPrefixTypes;
    private final Set<LexicalCategory> continuationTypes;
    protected final Set<LexicalCategory> readOnlyContinuationTypes;
    private final Set<LexicalCategory> requiredTypes;
    protected final Set<LexicalCategory> readOnlyRequiredTypes;
    private final Set<LexicalCategory> endTypes;
    protected final Set<LexicalCategory> readOnlyEndTypes;
    
    public PhraseTypeDefinition(LexicalCategory phraseType) {
        if(phraseType == null){
            throw new IllegalArgumentException("The parsed PhraseType MUST NOT be NULL!");
        }
        this.phraseType = phraseType;
        startTypes = EnumSet.of(phraseType);
        readOnlyStartTypes = Collections.unmodifiableSet(startTypes);
        prefixTypes = EnumSet.of(phraseType);
        readOnlyPrefixTypes = Collections.unmodifiableSet(prefixTypes);
        continuationTypes = EnumSet.of(phraseType);
        readOnlyContinuationTypes = Collections.unmodifiableSet(continuationTypes);
        requiredTypes = EnumSet.of(phraseType);
        readOnlyRequiredTypes = Collections.unmodifiableSet(requiredTypes);
        endTypes = EnumSet.of(phraseType);
        readOnlyEndTypes = Collections.unmodifiableSet(startTypes);
    }
    
    public boolean addStartType(LexicalCategory...types){
        return add(startTypes,types);
    }

    public boolean addPrefixType(LexicalCategory...types){
        return add(prefixTypes,types);
    }
    
    public boolean addContinuationType(LexicalCategory...types){
        return add(continuationTypes,types);
    }
    
    public boolean addRequiredType(LexicalCategory...types){
        return add(requiredTypes,types);
    }
    public boolean addEndType(LexicalCategory...types){
        return add(endTypes,types);
    }
    
    public boolean removeStartType(LexicalCategory...types){
        return remove(startTypes,types);
    }
    
    public boolean removePrefixType(LexicalCategory...types){
        return remove(prefixTypes,types);
    }
    
    public boolean removeContinuationType(LexicalCategory...types){
        return remove(continuationTypes,types);
    }
    
    public boolean removeRequiredType(LexicalCategory...types){
        return remove(requiredTypes,types);
    }

    public boolean removeEndType(LexicalCategory...types){
        return remove(endTypes,types);
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
    public Set<LexicalCategory> getStartType(){
        return readOnlyStartTypes;
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
    public Set<LexicalCategory> getPrefixType(){
        return readOnlyPrefixTypes;
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
    public Set<LexicalCategory> getContinuationType(){
        return readOnlyContinuationTypes;
    }
    
    /**
     * Getter for the read only set with the required types
     * @return the read only set with {@link LexicalCategory LexicalCategories}
     * that MUST occur within a phrase of that type
     */
    public Set<LexicalCategory> getRequiredType(){
        return readOnlyRequiredTypes;
    }
    
    /**
     * Getter for the read only set with the end types.
     * @return the read only set with {@link LexicalCategory LexicalCategories}
     * that can end a phrase of that type
     */
    public Set<LexicalCategory> getEndType(){
        return readOnlyEndTypes;
    }

    private boolean add(Set<LexicalCategory> set, LexicalCategory...types){
        boolean changed = false;
        if(types != null){
            for(LexicalCategory type : types){
                if(type != null){
                    if(set.add(type)){
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }
    
    private boolean remove(Set<LexicalCategory> set, LexicalCategory...types){
        boolean changed = false;
        if(types != null){
            for(LexicalCategory type : types){
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
    	return phraseType.name();
    }
}

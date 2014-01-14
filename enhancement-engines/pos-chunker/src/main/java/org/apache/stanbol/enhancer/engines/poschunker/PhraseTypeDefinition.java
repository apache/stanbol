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

public class PhraseTypeDefinition {

    protected final LexicalCategory phraseType;
    
    private final Set<LexicalCategory> startTypes;
    protected final Set<LexicalCategory> readOnlyStartTypes;
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
     * Getter for the read only set with the start types
     * @return the read only set with {@link LexicalCategory LexicalCategories}
     * that can start a phrase of that type
     */
    public Set<LexicalCategory> getStartType(){
        return readOnlyStartTypes;
    }
    
    /**
     * Getter for the read only set with the continuation types
     * @return the read only set with {@link LexicalCategory LexicalCategories}
     * that can continue a phrase of that type
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
}

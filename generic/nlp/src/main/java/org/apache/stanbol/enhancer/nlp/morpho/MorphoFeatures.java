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
package org.apache.stanbol.enhancer.nlp.morpho;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a morphological interpretation of a {@link Token word}. 
 * Words might have different interpretations (typically depending on the POS)
 * so this Tag allows to add information about all possible interpretations
 * to a single word.<br>
 * Note that the oder of added values via the different add** methods is preserved
 * for lists returned by the get**List() methods. The get**() methods will return
 * the first element of the list.
 * @author Rupert Westenthaler
 * @author Alessio Bosca
 */
public class MorphoFeatures {
    
    
    private final String lemma;
    /*
     * NOTE: uses internally Objects to represent the different morphological
     * features. This is because the assumption is that for most words most of
     * the features will not be set AND that even if set most of the time there
     * will be a single value.
     * As this data structure will potentially instantiated for each word of an
     * text this lazy initialization can save a lot of heap memory!
     */
    private Object posTags;
    private Object genderTags;
    private Object numberTags;
    private Object caseFeatureTags;
    private Object personValue;
    private Object definitnessValue;
    private Object verbFormTags;
    private Object tenseTags;
    

    public MorphoFeatures(String lemma){
        if(lemma == null || lemma.isEmpty()){
            throw new IllegalArgumentException("The parsed lemma MUST NOT be NULL nor empty!");
        }
        this.lemma = lemma;
    }
    

    
    public final String getLemma() {
        return lemma;
    }
    
    public final void addCase(CaseTag caseFeature) {
        caseFeatureTags = addTo(caseFeatureTags,caseFeature,CaseTag.class);
    }
    
    public final CaseTag getCase(){
        return getValue(caseFeatureTags, CaseTag.class);
    }
    
    public final List<CaseTag> getCaseList(){
        return getValues(caseFeatureTags, CaseTag.class);
    }

    public final void addDefinitness(Definitness definitness) {
        definitnessValue = addTo(definitnessValue,definitness,Definitness.class);
    }
    public final Definitness getDefinitness(){
        return getValue(definitnessValue, Definitness.class);
    }
    
    public final List<Definitness> getDefinitnessList(){
        return getValues(definitnessValue, Definitness.class);
    }

    public final void addGender(GenderTag gender) {
        genderTags = addTo(genderTags,gender,GenderTag.class);
    }

    public final GenderTag getGender(){
        return getValue(genderTags, GenderTag.class);
    }
    
    public final List<GenderTag> getGenderList(){
        return getValues(genderTags, GenderTag.class);
    }

    public final void addNumber(NumberTag number) {
        numberTags = addTo(numberTags,number,NumberTag.class);
    }

    public final NumberTag getNumber(){
        return getValue(numberTags, NumberTag.class);
    }
    
    public final List<NumberTag> getNumberList(){
        return getValues(numberTags, NumberTag.class);
    }

    public void addPerson(Person person) {
        personValue = addTo(personValue,person,Person.class);
    }

    public final Person getPerson(){
        return getValue(personValue, Person.class);
    }
    
    public final List<Person> getPersonList(){
        return getValues(personValue, Person.class);
    }

    public void addPos(PosTag pos) {
        posTags = addTo(posTags,pos,PosTag.class);
    }

    public final PosTag getPos(){
        return getValue(posTags, PosTag.class);
    }
    
    public final List<PosTag> getPosList(){
        return getValues(posTags, PosTag.class);
    }

    public void addTense(TenseTag tense) {
        tenseTags = addTo(tenseTags,tense,TenseTag.class);
    }

    public final TenseTag getTense(){
        return getValue(tenseTags, TenseTag.class);
    }
    
    public final List<TenseTag> getTenseList(){
        return getValues(tenseTags, TenseTag.class);
    }
    
    public void addVerbForm(VerbMoodTag verbForm) {
        verbFormTags = addTo(verbFormTags,verbForm,VerbMoodTag.class);
    }

    public final VerbMoodTag getVerbMood(){
        return getValue(verbFormTags, VerbMoodTag.class);
    }
    
    public final List<VerbMoodTag> getVerbMoodList(){
        return getValues(verbFormTags, VerbMoodTag.class);
    }

    @Override
    public int hashCode() {
        return lemma.hashCode() + 
                (posTags != null ? posTags.hashCode() : 0) + 
                (genderTags != null ? genderTags.hashCode() : 0) + 
                (personValue != null ? personValue.hashCode() : 0) + 
                (caseFeatureTags != null ? caseFeatureTags.hashCode() : 0) + 
                (definitnessValue != null ? definitnessValue.hashCode() : 0) + 
                (verbFormTags != null ? verbFormTags.hashCode() : 0) + 
                (tenseTags != null ? tenseTags.hashCode() : 0);
    }
    @Override
    public boolean equals(Object o) {
        if (o instanceof MorphoFeatures && lemma.equals(((MorphoFeatures) o).lemma)) {
            MorphoFeatures lt = (MorphoFeatures) o;
            return ((genderTags != null && genderTags.equals(lt.genderTags)) || (genderTags == null && lt.genderTags == null)) && ((caseFeatureTags != null && caseFeatureTags.equals(lt.caseFeatureTags)) || (caseFeatureTags == null && lt.caseFeatureTags == null))
                    && ((tenseTags != null && tenseTags.equals(lt.tenseTags)) || (tenseTags == null && lt.tenseTags == null)) && ((numberTags != null && numberTags.equals(lt.numberTags)) || (numberTags == null && lt.numberTags == null))
                    && ((definitnessValue != null && definitnessValue.equals(lt.definitnessValue)) || (definitnessValue == null && lt.definitnessValue == null)) && ((personValue != null && personValue.equals(lt.personValue)) || (personValue == null && lt.personValue == null))
                    && ((verbFormTags != null && verbFormTags.equals(lt.verbFormTags)) || (verbFormTags == null && lt.verbFormTags == null));
        } else {
            return false;
        }
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("MorphoTag( lemma:");
        sb.append(lemma);
        if(posTags != null){
            sb.append("| ").append(posTags);
        }
        if(genderTags != null){
            sb.append("| ").append(genderTags);
        }
        if(numberTags != null){
            sb.append("| ").append(numberTags);
        }
        if(personValue != null){
            sb.append("| ").append(personValue);
        }
        if(definitnessValue != null){
            sb.append("| ").append(definitnessValue);
        }
        if(caseFeatureTags != null){
            sb.append("| ").append(caseFeatureTags);
        }
        if(verbFormTags != null){
            sb.append("| ").append(verbFormTags);
        }
        if(tenseTags != null){
            sb.append("|tense:").append(tenseTags);
        }
        sb.append(')');
        return sb.toString();
    }
    /* ------------------------------------------------------------
     * Utility methods to read/write data to the Object fields
     * by using lazzy initialization of single or multiple (List)
     * values.
     * ------------------------------------------------------------
     */
    @SuppressWarnings("unchecked")
    private static <T> Object addTo(Object field,T value,Class<T> clazz){
        if(value == null){
            return field;
        } else if(field == null){
            return value;
        } else if(field instanceof List<?>){
            ((Collection<T>)field).add(value);
            return field;
        } else {
            List<T> list = new ArrayList<T>(3);
            list.add((T)field);
            list.add(value);
            return list;
        }
    }
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> T getValue(Object field, Class<T> clazz){
        if(field == null){
            return null;
        } else if(field instanceof List<?>){
            return (T)((List) field).get(0);
        } else {
            return (T)field;
        }
    }
    
    @SuppressWarnings("unchecked")
    private static <T> List<T> getValues(Object field, Class<T> clazz){
        if(field == null){
            return Collections.EMPTY_LIST;
        } else if(field instanceof List<?>){
            return (List<T>) field;
        } else {
            return Collections.singletonList((T)field);
        }
    }
}

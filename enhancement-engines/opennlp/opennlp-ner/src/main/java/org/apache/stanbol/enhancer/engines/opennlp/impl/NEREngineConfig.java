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
package org.apache.stanbol.enhancer.engines.opennlp.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.commons.opennlp.OpenNLP;
import org.apache.stanbol.enhancer.nlp.model.tag.TagSet;
import org.apache.stanbol.enhancer.nlp.ner.NerTag;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;

public class NEREngineConfig {
    /**
     * Default mapping for Concept types to dc:type values added for
     * TextAnnotations.
     */
    public static final Map<String,IRI> DEFAULT_ENTITY_TYPE_MAPPINGS;
    
    static { //the default mappings for the default NER types
        Map<String,IRI> mappings = new TreeMap<String,IRI>();
        mappings.put("person", OntologicalClasses.DBPEDIA_PERSON);
        mappings.put("location", OntologicalClasses.DBPEDIA_PLACE);
        mappings.put("organization", OntologicalClasses.DBPEDIA_ORGANISATION);
        DEFAULT_ENTITY_TYPE_MAPPINGS = Collections.unmodifiableMap(mappings);
    }
    
    /**
     * Holds the configured {@link NerTag}s - the mappings from the
     * named entity name to the {@link IRI} type used for the
     * <code>dc:type</code> value for <code>fise:TextAnnotation</code>s
     */
    private TagSet<NerTag> nerTagSet = new TagSet<NerTag>("NER TagSet");
    
    private Map<String,Collection<String>> additionalNerModels = new HashMap<String,Collection<String>>();
    /**
     * The default model types
     */
    private Set<String> defaultModelTypes = new HashSet<String>(DEFAULT_ENTITY_TYPE_MAPPINGS.keySet());
    /**
     * TODO: replace with Language as soon as STANBOL-733 is re-integrated with
     * the Stanbol trunk
     */
    private Set<String> processedLanguages = new HashSet<String>();
    
    private String defaultLanguage;
    
    public NEREngineConfig(){
        for(Entry<String,IRI> mapping : DEFAULT_ENTITY_TYPE_MAPPINGS.entrySet()){
            nerTagSet.addTag(new NerTag(mapping.getKey(), mapping.getValue()));
        }
    }
    
    public synchronized void addCustomNameFinderModel(String lang, String modelFileName){
        if(lang == null || lang.isEmpty()){
            throw new IllegalArgumentException("The parsed lanaguage MUST NOT be NULL or empty!");
        }
        if(modelFileName == null || modelFileName.isEmpty()){
            throw new IllegalArgumentException("The parsed NER model name MUST NOT be NULL or empty!");
        }
        Collection<String> currentModels = additionalNerModels.get(lang);
        if(currentModels == null){
            currentModels = new CopyOnWriteArrayList<String>();
            additionalNerModels.put(lang, currentModels);
        }
        currentModels.add(modelFileName);
    }
    
    public synchronized void removeCustomNameFinderModel(String lang, String modelFileName){
        if(lang == null || lang.isEmpty()){
            throw new IllegalArgumentException("The parsed lanaguage MUST NOT be NULL or empty!");
        }
        if(modelFileName == null || modelFileName.isEmpty()){
            throw new IllegalArgumentException("The parsed NER model name MUST NOT be NULL or empty!");
        }
        Collection<String> currentModels = additionalNerModels.get(lang);
        if(currentModels != null && //if models for that language are present
                currentModels.remove(modelFileName) && // and the model was actually remove
                currentModels.isEmpty()){ //no other models present for this language
            additionalNerModels.remove(lang);
        }
    }
        
    public Set<String> getProcessedLanguages() {
        return processedLanguages;
    }
    /**
     * Checks if the parsed language is enabled for processing.
     * If <code>null</code> is parsed as language this returns <code>false</code>
     * even if processing of all languages is enabled. <p>
     * NOTE: If this Method returns <code>true</code> this does
     * not mean that text with this language can be actually processed because this
     * also requires that the NER model for this language are available via the
     * parsed {@link OpenNLP} instance.
     * @param lang the language
     * @return the state
     */
    public boolean isProcessedLangage(String lang){
        return lang != null && (processedLanguages.isEmpty() || processedLanguages.contains(lang));
    }
    
    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }
    
    public String getDefaultLanguage() {
        return defaultLanguage;
    }
        
    public Set<String> getDefaultModelTypes() {
        return defaultModelTypes;
    }
    
    @SuppressWarnings("unchecked")
    public Collection<String> getSpecificNerModles(String lang){
        Collection<String> modelNames = additionalNerModels.get(lang);
        return modelNames == null ? Collections.EMPTY_LIST : modelNames;
    }
    /**
     * Getter for the {@link NerTag} of the parsed Named Entity
     * name. If not yet present a new {@link NerTag} (with no
     * <code>dc:type</code> mapping) is created and added to the
     * configuration.
     * @param namedEntityType the NamedEntity name.
     * @return the NerTag. Guaranteed to be not <code>null</code>
     * @throws IllegalArgumentException if the parsed NamedEntity
     * type is <code>null</code> or an empty String.
     */
    public NerTag getNerTag(String namedEntityType){
        if(namedEntityType == null || namedEntityType.isEmpty()){
            throw new IllegalArgumentException("The parsed NamedEntity string MUST NOT be NULL nor empty!");
        }
        NerTag tag = nerTagSet.getTag(namedEntityType);
        if(tag == null){
            tag = new NerTag(namedEntityType);
            nerTagSet.addTag(tag);
        }
        return tag;
    }
    /**
     * Setter for a NamedEntity name &gt; <code>dc:tyoe</code>
     * mapping.
     * @param namedEntityType the Named Entity type (as
     * used by the OpenNLP NameFinder model)
     * @param dcType the <code>dc:Type</code> used for the
     * NamedEntity or <code>nulll</code> if non
     * @throws IllegalArgumentException if the parsed NamedEntity
     * type is <code>null</code> or an empty String.
     */
    public void setMappedType(String namedEntityType,IRI dcType){
        if(namedEntityType != null && !namedEntityType.isEmpty()){
            nerTagSet.addTag(new NerTag(namedEntityType, dcType));
        } else {
            throw new IllegalArgumentException("The parsed NamedEntity type MUST NOT be NULL nor empty!");
        }
    }
}

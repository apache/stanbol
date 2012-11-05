package org.apache.stanbol.enhancer.engines.opennlp.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.commons.opennlp.OpenNLP;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;

public class NEREngineConfig {
    /**
     * Default mapping for Concept types to dc:type values added for
     * TextAnnotations.
     */
    public static final Map<String,UriRef> DEFAULT_ENTITY_TYPE_MAPPINGS;
    
    static { //the default mappings for the default NER types
        Map<String,UriRef> mappings = new TreeMap<String,UriRef>();
        mappings.put(OntologicalClasses.DBPEDIA_ORGANISATION.getUnicodeString(), OntologicalClasses.DBPEDIA_ORGANISATION);
        mappings.put("person", OntologicalClasses.DBPEDIA_PERSON);
        mappings.put("location", OntologicalClasses.DBPEDIA_PLACE);
        mappings.put("organization", OntologicalClasses.DBPEDIA_ORGANISATION);
        DEFAULT_ENTITY_TYPE_MAPPINGS = Collections.unmodifiableMap(mappings);
    }
    
    /**
     * Holds the mappings of rdf:type used by concepts to dc:type values used
     * by TextAnnotations. 
     */
    private Map<String,UriRef> typeMappings = new HashMap<String,UriRef>(DEFAULT_ENTITY_TYPE_MAPPINGS);
    
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
    
    public UriRef getMappedType(String namedEntityType){
        return typeMappings.get(namedEntityType);
    }
    public void setMappedType(String namedEntityType,UriRef dcType){
        if(namedEntityType != null && !namedEntityType.isEmpty()){
            if(dcType == null){
                typeMappings.remove(namedEntityType);
            } else {
                typeMappings.put(namedEntityType, dcType);
            }
        } else {
            throw new IllegalArgumentException("The parsed NamedEntity type MUST NOT be NULL nor empty!");
        }
    }
}

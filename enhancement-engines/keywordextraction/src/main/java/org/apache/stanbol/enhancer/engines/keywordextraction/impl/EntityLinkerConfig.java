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
package org.apache.stanbol.enhancer.engines.keywordextraction.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import opennlp.tools.chunker.Chunker;
import opennlp.tools.postag.POSTagger;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.commons.opennlp.TextAnalyzer.AnalysedText;
import org.apache.stanbol.commons.opennlp.TextAnalyzer.AnalysedText.Chunk;
import org.apache.stanbol.commons.opennlp.TextAnalyzer.AnalysedText.Token;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.EntitySearcher;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;

/**
 * The configuration for the {@link EntityLinker}. Typically this
 * configuration does not change often. Therefore it will be used for
 * several {@link EntityLinker} instances processing different 
 * contents.
 * @author Rupert Westenthaler
 *
 */
public class EntityLinkerConfig {
    /**
     * The minimum length of Token to be used for searches in case no
     * POS (Part of Speech) tags are available.
     */
    public static final int DEFAULT_MIN_SEARCH_TOKEN_LENGTH = 3;
    /**
     * The default number for the maximum number of terms suggested for a word
     */
    public static final int DEFAULT_SUGGESTIONS = 3;
    /**
     * Default value for the number of tokens that must be contained in
     * suggested terms.
     */
    public static final int DEFAULT_MIN_FOUND_TOKENS = 2;
    /**
     * Multiple Tokens can be sent to the {@link EntitySearcher} service. The
     * service uses this as optional parameters for the search. Therefore
     * returned Concepts MUST contain at least a single of the parsed 
     * tokens. <p>
     * The default value of <code>2</code> should be enough for nearly all
     * Taxonomies to sufficiently reduce the number of results.<p>
     * NOTE that the labels (nameField) of the results are compared as a
     * whole. So even if only 2 Tokens are used for the search there may be
     * more mapped to the actual label of an result.
     */
    public static final int DEFAULT_MAX_SEARCH_TOKENS = 2;

    /**
     * Default value for {@link #getNameField()} (rdfs:label)
     */
    public static final String DEFAULT_NAME_FIELD = "http://www.w3.org/2000/01/rdf-schema#label";
    /**
     * Default value for {@link #getTypeField()} (rdf:type)
     */
    public static final String DEFAULT_TYPE_FIELD = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    /**
     * Default value for {@link #getRedirectField()} (rdf:seeAlso)
     */
    public static final String DEFAULT_REDIRECT_FIELD = "http://www.w3.org/2000/01/rdf-schema#seeAlso";
    /**
     * The default language used to search for labels regardless of the language
     * of the text. The default value is <code>null</code> causing to include
     * labels that do not have a language assigned.
     */
    public static final String DEFAULT_LANGUAGE = null;
    /**
     * The default for case sensitive matching is set to <code>false</code>
     */
    public static final boolean DEFAULT_CASE_SENSITIVE_MATCHING_STATE = false;
    /**
     * Default mapping for Concept types to dc:type values added for
     * TextAnnotations.
     */
    public static final Map<String,IRI> DEFAULT_ENTITY_TYPE_MAPPINGS;
    
    static { //the default mappings for the three types used by the Stanbol Enhancement Structure
        Map<String,IRI> mappings = new HashMap<String,IRI>();
        mappings.put(OntologicalClasses.DBPEDIA_ORGANISATION.getUnicodeString(), 
            OntologicalClasses.DBPEDIA_ORGANISATION);
        mappings.put("http://dbpedia.org/ontology/Newspaper", OntologicalClasses.DBPEDIA_ORGANISATION);
        mappings.put("http://schema.org/Organization", OntologicalClasses.DBPEDIA_ORGANISATION);
//        mappings.put(NamespaceEnum.dailymed+"organization",OntologicalClasses.DBPEDIA_ORGANISATION);
        
        mappings.put(OntologicalClasses.DBPEDIA_PERSON.getUnicodeString(),
            OntologicalClasses.DBPEDIA_PERSON);
        mappings.put("http://xmlns.com/foaf/0.1/Person", OntologicalClasses.DBPEDIA_PERSON);
        mappings.put("http://schema.org/Person", OntologicalClasses.DBPEDIA_PERSON);

        mappings.put(OntologicalClasses.DBPEDIA_PLACE.getUnicodeString(), 
            OntologicalClasses.DBPEDIA_PLACE);
        mappings.put("http://schema.org/Place", OntologicalClasses.DBPEDIA_PLACE);
        mappings.put("http://www.opengis.net/gml/_Feature", OntologicalClasses.DBPEDIA_PLACE);

        mappings.put(OntologicalClasses.SKOS_CONCEPT.getUnicodeString(),
            OntologicalClasses.SKOS_CONCEPT);

        mappings.put(OntologicalClasses.DBPEDIA_ORGANISATION.getUnicodeString(), 
            OntologicalClasses.DBPEDIA_ORGANISATION);
//        IRI DRUG = new IRI(NamespaceEnum.drugbank+"drugs");
//        mappings.put(DRUG.getUnicodeString(), DRUG);
//        mappings.put(NamespaceEnum.dbpediaOnt+"Drug", DRUG);
//        mappings.put(NamespaceEnum.dailymed+"drugs", DRUG);
//        mappings.put(NamespaceEnum.sider+"drugs", DRUG);
//        mappings.put(NamespaceEnum.tcm+"Medicine", DRUG);
//        
//        IRI DISEASE = new IRI(NamespaceEnum.diseasome+"diseases");
//        mappings.put(DISEASE.getUnicodeString(), DISEASE);
//        mappings.put(NamespaceEnum.linkedct+"condition", DISEASE);
//        mappings.put(NamespaceEnum.tcm+"Disease", DISEASE);
//
//        IRI SIDE_EFFECT = new IRI(NamespaceEnum.sider+"side_effects");
//        mappings.put(SIDE_EFFECT.getUnicodeString(), SIDE_EFFECT);
//        
//        IRI INGREDIENT = new IRI(NamespaceEnum.dailymed+"ingredients");
//        mappings.put(INGREDIENT.getUnicodeString(), INGREDIENT);
                
        DEFAULT_ENTITY_TYPE_MAPPINGS = Collections.unmodifiableMap(mappings);
    }
    /**
     * Enumeration over the different possibilities on how to deal with
     * redirects (similar to Browsers following HTTP status 303 and RDF defining
     * the "rdf:seeAlso" relation. 
     * @author Rupert Westenthaler
     */
    public static enum RedirectProcessingMode {
        /**
         * Ignore redirects
         */
        IGNORE,
        /**
         * Follow redirects, but only add the values (e.g. labels, types) such
         * entities to the original one.
         */
        ADD_VALUES,
        /**
         * Follow the redirect.
         */
        FOLLOW
    }
    /**
     * The default value for how to process redirect is set to
     * {@link RedirectProcessingMode#IGNORE}
     */
    public static RedirectProcessingMode DEFAULT_REDIRECT_PROCESSING_MODE = 
        RedirectProcessingMode.IGNORE;
    /**
     * The minimum length of labels that are looked-up in the directory
     */
    private int minSearchTokenLength = DEFAULT_MIN_SEARCH_TOKEN_LENGTH;
    /**
     * The the maximum number of terms suggested for a word
     */
    private int maxSuggestions = DEFAULT_SUGGESTIONS;
    /**
     * If several words are selected from the text to search for an Entity in the
     * Dictionary (e.g. if a {@link Chunker} is used or if the {@link POSTagger}
     * detects several connected nouns) that entities found for the such chunks
     * MUST define a label (with no or the correct lanugage) that contains at
     * least this number of tokens to be accepted.<p>
     * TODO: make configurable
     */
    private int minFoundTokens = DEFAULT_MIN_FOUND_TOKENS;
    /**
     * The maximum numbers of Tokens sent to the {@link EntitySearcher} to search
     * for concepts. <p>
     * NOTE that the labels (nameField) of the results are compared as a
     * whole. So even if only e.g. 2 tokens are used for the search there may be
     * more mapped to the actual label of an result.
     */
    private int maxSearchTokens = DEFAULT_MAX_SEARCH_TOKENS;
    
    private boolean caseSensitiveMatchingState = DEFAULT_CASE_SENSITIVE_MATCHING_STATE;
    /**
     * Holds the mappings of rdf:type used by concepts to dc:type values used
     * by TextAnnotations. 
     */
    private Map<String,IRI> typeMappings;
    private Map<String, IRI> unmodTypeMappings;
    /**
     * The mode on how to process redirect for Entities. 
     */
    private RedirectProcessingMode redirectProcessingMode;
    /**
     * the default DC Type
     */
    private IRI defaultDcType;
    private String nameField;
    private String redirectField;
    private String typeField;
    private Set<String> selectedFields = new HashSet<String>();
    /**
     * The language always included in searches (regardless of the language
     * detected for the text.
     */
    private String defaultLanguage = DEFAULT_LANGUAGE;
    
    /**
     * Default for the maximum number of non-processable tokens that are 
     * allowed to not match before no further tokens are matched against a label 
     * of an Entity. <p>
     * This allows e.g. to match "Dr. Richard Dogles" with "Dr Richard Dogles"
     * as '.' is a non-processable token in the text that is missing in the
     * label.<p>
     * The default is set to <code>1</code>
     */
    public final static int DEFAULT_MAX_NOT_FOUND = 1; 
    /**
     * Value of the maximum number of non-processable tokens that are 
     * allowed to not match before no further tokens are matched against a label 
     * of an Entity. <p>
     * This allows e.g. to match "Dr. Richard Dogles" with "Dr Richard Dogles"
     * as '.' is a non-processable token in the text that is missing in the
     * label.
    */
    private int maxNotFound;
    /**
     * Default value for the minimum token match factor.
     * If Tokens match is determined by comparing them using some algorithm.
     * Results need to be in the range [0..1]. This factor defines the minimum
     * similarity value so that a match is assumed. Not that this factor only
     * is used for filtering out non-matching tokens. The similarity value will
     * still used for calculating the confidence.<p>
     * The default is set to <code>0.7</code>.
     */
    public final static float DEFAULT_MIN_TOKEN_MATCH_FACTOR = 0.7f;
    /**
     * If Tokens match is determined by comparing them using some algorithm.
     * Results need to be in the range [0..1]. This factor defines the minimum
     * similarity value so that a match is assumed. Not that this factor only
     * is used for filtering out non-matching tokens. The similarity value will
     * still used for calculating the confidence
     */
    private float minTokenMatchFactor;
    
    /**
     * Default constructor the initialises the configuration with the 
     * default values
     */
    public EntityLinkerConfig(){
        setMinSearchTokenLength(DEFAULT_MIN_SEARCH_TOKEN_LENGTH);
        setMaxSuggestions(DEFAULT_SUGGESTIONS);
        setMaxSearchTokens(DEFAULT_MAX_SEARCH_TOKENS);
        setRedirectProcessingMode(DEFAULT_REDIRECT_PROCESSING_MODE);
        typeMappings = new HashMap<String,IRI>(DEFAULT_ENTITY_TYPE_MAPPINGS);
        unmodTypeMappings = Collections.unmodifiableMap(typeMappings);
        setDefaultDcType(typeMappings.remove(null));
        setNameField(DEFAULT_NAME_FIELD);
        setRedirectField(DEFAULT_REDIRECT_FIELD);
        setTypeField(DEFAULT_TYPE_FIELD);
        setMaxNotFound(DEFAULT_MAX_NOT_FOUND);
        setMinTokenMatchFactor(DEFAULT_MIN_TOKEN_MATCH_FACTOR);
    }
    /**
     * Getter for the uri of the field used for the names in the taxonomy
     * (e.g. rdfs:label, skos:prefLabel). Needs to return the full URI
     * @return the field used for the names of in the Taxonomy.
     */
    public final String getNameField() {
        return nameField;
    }
    /**
     * Setter for the uri of the field used for the entities in the vocabulary<p>
     * <b>NOTE</b>: does NOT support the convertion of short to full URIs
     * {@link NamespaceEnum}.
     * @param nameField the nameField to set
     */
    public final void setNameField(String nameField) {
        this.nameField = nameField;
        updateSelectedFields();
    }
    /**
     * internally used to update the selected fields on changes to
     * {@link #setNameField(String)}, {@link #setRedirectField(String)} or
     * {@link #setTypeField(String)}
     */
    private void updateSelectedFields() {
        selectedFields.clear();
        selectedFields.add(nameField);
        selectedFields.add(redirectField);
        selectedFields.add(typeField);
    }
    /**
     * Getter for the selected fields. A set that includes the current
     * {@link #getNameField()}, {@link #getTypeField()} and {@link #getRedirectField()}.
     * @return the selectedFields
     */
    public final Set<String> getSelectedFields() {
        return selectedFields;
    }
    /**
     * The field used to follow redirects (typically rdf:seeAlso)
     * @return the redirect field
     */
    public final String getRedirectField() {
        return redirectField;
    }
    /**
     * The field used to follow redirects (typically rdf:seeAlso)<p>
     * <b>NOTE</b>: does NOT support the convertion of short to full URIs
     * @param redirectField the redirectField to set
     */
    public final void setRedirectField(String redirectField) {
        this.redirectField = redirectField;
        updateSelectedFields();
    }
    /**
     * The field used to lookup the types (typically rdf:type)
     * @return the field name used to lookup types
     */
    public final String getTypeField() {
        return typeField;
    }
    /**
     * The field used to lookup the types (typically rdf:type)<p>
     * <b>NOTE</b>: does NOT support the convertion of short to full URIs
     * @param typeField the typeField to set
     */
    public final void setTypeField(String typeField) {
        this.typeField = typeField;
        updateSelectedFields();
    }
    /**
     * The minimum number of character a {@link Token} (word) must have to be
     * used {@link EntitySearcher#lookup(java.util.List, String...) lookup} concepts
     * in the taxonomy. Note that this parameter is only used of no POS (Part-
     * of-speech) tags are available in the {@link AnalysedText}.
     * @param minSearchTokenLength the minSearchTokenLength to set
     */
    public void setMinSearchTokenLength(int minSearchTokenLength) {
        this.minSearchTokenLength = minSearchTokenLength;
    }
    /**
     * The minimum number of character a {@link Token} (word) must have to be
     * used {@link EntitySearcher#lookup(java.util.List, String...) lookup} concepts
     * in the taxonomy. Note that this parameter is only used of no POS (Part-
     * of-speech) tags are available in the {@link AnalysedText}.
     * @return the minSearchTokenLength
     */
    public int getMinSearchTokenLength() {
        return minSearchTokenLength;
    }
    /**
     * Setter for the maximum number of suggestion returned. 
     * @param maxSuggestions the maxSuggestions to set
     */
    public void setMaxSuggestions(int maxSuggestions) {
        this.maxSuggestions = maxSuggestions;
    }
    /**
     * Getter for the maximum number of suggestion returned. 
     * @return the maxSuggestions
     */
    public int getMaxSuggestions() {
        return maxSuggestions;
    }
    /**
     * Setter for the minimum number of Tokens (of the content) that MUST match
     * with a {@link EntitySearcher#getNameField() label} of a 
     * {@link EntitySearcher#lookup(java.util.List, String...) concept of the taxonomy}
     * so that it is {@link Suggestion suggested} even if the match is only
     * {@link MATCH#PARTIAL}. Entities that match less than that are only included
     * if a label is an {@link MATCH#EXACT EXACT} match with the current position
     * in the text. 
     * @param minFoundTokens the minFoundTokens to set
     */
    public void setMinFoundTokens(int minFoundTokens) {
        this.minFoundTokens = minFoundTokens;
    }
    /**
     * Getter for the minimum number of Tokens (of the content) that MUST match
     * with a {@link EntitySearcher#getNameField() label} of a 
     * {@link EntitySearcher#lookup(java.util.List, String...) concept of the taxonomy}
     * so that it is {@link Suggestion suggested} even if the match is only
     * {@link MATCH#PARTIAL}. Entities that match less than that are only included
     * if a label is an {@link MATCH#EXACT EXACT} match with the current position
     * in the text. 
     * @return the minFoundTokens
     */
    public int getMinFoundTokens() {
        return minFoundTokens;
    }
    /**
     * Getter for the  maximum number of tokens parsed to 
     * {@link EntitySearcher#lookup(java.util.List, String...)}
     * @return the maxSearchTokens
     */
    public final int getMaxSearchTokens() {
        return maxSearchTokens;
    }
    /**
     * The maximum number of tokens parsed to 
     * {@link EntitySearcher#lookup(java.util.List, String...)}. This is NOT the
     * maximum number of Tokens mapped for Entities returned by such queries.<p>
     * In case {@link Chunk}s are available in the parsed {@link AnalysedText}
     * searches can be scoped by such chunks. However if no chunks are available,
     * than this value is used to collect this number of words in the text.<p>
     * The {@link #DEFAULT_MAX_SEARCH_TOKENS default value} of <code>2</code>
     * should be ok in most cases.  
     * @param maxSearchTokens the maxSearchTokens to set
     */
    public final void setMaxSearchTokens(int maxSearchTokens) {
        this.maxSearchTokens = maxSearchTokens;
    }
    /**
     * Getter for the case sensitive matching state
     * @return the state
     */
    public boolean isCaseSensitiveMatching() {
        return caseSensitiveMatchingState;
    }
    /**
     * Setter for the case sensitive matching state
     * @param caseSensitiveMatchingState the state
     */
    public void setCaseSensitiveMatchingState(boolean state) {
        this.caseSensitiveMatchingState = state;
    }
    /**
     * Removes the mapping for the parsed concept type
     * @param conceptType the concept type to remove the mapping
     * @return the previously mapped dc:type value or <code>null</code> if
     * no mapping for the parsed concept type was present
     */
    public IRI removeTypeMapping(String conceptType){
        return typeMappings.remove(conceptType);
    }
    /**
     * 
     * @param conceptType the type of the concept or <code>null</code> to
     * add the default dc:type mapping. See also {@link #setDefaultDcType(IRI)}
     * @param dcType the dc:type for the parsed concept type
     * @return the previously mapped dc:type value if an existing mapping
     * was updated or <code>null</code> if a new mapping was added.
     */
    public IRI setTypeMapping(String conceptType, IRI dcType){
        if(dcType == null) {
            throw new IllegalArgumentException("The parsed dc:type URI MUST NOT be NULL!");
        }
        if(conceptType == null){ //handle setting of the default dc:type value
            IRI oldDefault = getDefaultDcType();
            setDefaultDcType(dcType);
            return oldDefault;
        }
        return typeMappings.put(conceptType, dcType);
    }
    
    /**
     * Setter for the default dc:type of linked entities if for none of the
     * types of the suggestions a {@link #getTypeMappings()} exists. Set this
     * to <code>null</code> to specify that no dc:type should be set in such
     * cases.
     * @param defaultDcType the defaultDcType to set
     */
    public void setDefaultDcType(IRI defaultDcType) {
        this.defaultDcType = defaultDcType;
    }
    /**
     * The default type for Entities if no {@link #getTypeMappings() type mapping}
     * is present. <code>null</code> means that no type should be set if no
     * explicit mapping exists
     * @return the defaultDcType
     */
    public IRI getDefaultDcType() {
        return defaultDcType;
    }
    /**
     * Setter for the mode on how to deal with redirects
     * @param redirectProcessingMode the redirectProcessingMode to set
     */
    public void setRedirectProcessingMode(RedirectProcessingMode redirectProcessingMode) {
        this.redirectProcessingMode = redirectProcessingMode;
    }
    /**
     * Getter for the mode how to deal with redirects
     * @return the redirectProcessingMode
     */
    public RedirectProcessingMode getRedirectProcessingMode() {
        return redirectProcessingMode;
    }    
    /**
     * Getter for the read only mappings of type mappings
     * @return the type mappings (read only)
     */
    public Map<String,IRI> getTypeMappings() {
        return unmodTypeMappings;
    }
    /**
     * Setter for the language of labels searched in addition to the current
     * language of the text. Setting this to <code>null</code> (also the default)
     * will cause to search labels without any defined language.<p>
     * Changing this makes only sense if a dataset (such as dbpedia.org) adds
     * language tags to labels even if they are typically used in any language.
     * @param defaultLanguage the default language
     */
    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }
    /**
     * Getter for the language of labels searched in addition to the current
     * language of the text.
     * @return the default language 
     */
    public String getDefaultLanguage() {
        return defaultLanguage;
    }
    /**
     * Getter for the maximum number of non-processable tokens that are 
     * allowed to not match before no further tokens are matched against a label 
     * of an Entity. <p>
     * This allows e.g. to match "Dr. Richard Dogles" with "Dr Richard Dogles"
     * as '.' is a non-processable token in the text that is missing in the
     * label.
     * @return the maxNotFound
     */
    public int getMaxNotFound() {
        return maxNotFound;
    }
    /**
     * Setter for the maximum number of non-processable tokens that are 
     * allowed to not match before no further tokens are matched against a label 
     * of an Entity. <p>
     * This allows e.g. to match "Dr. Richard Dogles" with "Dr Richard Dogles"
     * as '.' is a non-processable token in the text that is missing in the
     * label.
     * @param maxNotFound the maxNotFound to set
     */
    public void setMaxNotFound(int maxNotFound) {
        if(maxNotFound < 0){
            this.maxNotFound = DEFAULT_MAX_NOT_FOUND;
        } else {
            this.maxNotFound = maxNotFound;
        }
    }
    /**
     * Getter for the minimum token match Factor.
     * If Tokens match is determined by comparing them using some algorithm.
     * Results need to be in the range [0..1]. This factor defines the minimum
     * similarity value so that a match is assumed. Not that this factor only
     * is used for filtering out non-matching tokens. The similarity value will
     * still used for calculating the confidence
     * @return the minTokenMatchFactor
     */
    public float getMinTokenMatchFactor() {
        return minTokenMatchFactor;
    }
    /**
     * Setter for the minimum token match Factor.
     * If Tokens match is determined by comparing them using some algorithm.
     * Results need to be in the range [0..1]. This factor defines the minimum
     * similarity value so that a match is assumed. Not that this factor only
     * is used for filtering out non-matching tokens. The similarity value will
     * still used for calculating the confidence
     * @param minTokenMatchFactor the minTokenMatchFactor to set
     */
    public void setMinTokenMatchFactor(float minTokenMatchFactor) {
        if(minTokenMatchFactor < 0 ){
            this.minTokenMatchFactor = DEFAULT_MIN_TOKEN_MATCH_FACTOR;
        } else if(minTokenMatchFactor == 0 || minTokenMatchFactor > 1){
            throw new IllegalArgumentException("minimum Token Match Facter MUST be > 0 <= 1 (parsed: "+minTokenMatchFactor+")!");
        } else {
            this.minTokenMatchFactor = minTokenMatchFactor;
        }
    }
}
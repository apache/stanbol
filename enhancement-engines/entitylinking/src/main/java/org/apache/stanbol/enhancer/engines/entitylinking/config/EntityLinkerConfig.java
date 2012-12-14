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
package org.apache.stanbol.enhancer.engines.entitylinking.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.commons.namespaceprefix.NamespaceMappingUtils;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.enhancer.engines.entitylinking.EntitySearcher;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.EntityLinker;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.Suggestion;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.Suggestion.MATCH;
import org.apache.stanbol.enhancer.nlp.NlpAnnotations;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.morpho.MorphoFeatures;
import org.apache.stanbol.enhancer.nlp.pos.Pos;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The configuration for the {@link EntityLinker}. Typically this
 * configuration does not change often. Therefore it will be used for
 * several {@link EntityLinker} instances processing different 
 * contents.
 * @author Rupert Westenthaler
 *
 */
public class EntityLinkerConfig {
    
    private static final  Logger log = LoggerFactory.getLogger(EntityLinkerConfig.class);
    
    /**
     * The field used to search for labels in the vocabulary linked against
     */
    public static final String NAME_FIELD = "enhancer.engines.linking.labelField";
    /**
     * The field used as types for entities. While the type does not influence the
     * suggestions it is used for the <code>fise:entity-type</code> value of 
     * <code>fise:EntityAnnotation</code>s and also to determine the
     * <code>dc:type</code> value of <code>fise:TextAnnotation</code>s via the
     * configured {@link #TYPE_MAPPINGS}.
     */
    public static final String TYPE_FIELD = "enhancer.engines.linking.typeField";
    /**
     * Allows to enable/disable case sensitive matching
     */
    public static final String CASE_SENSITIVE = "enhancer.engines.linking.caseSensitive";
    /**
     * The field used to lookup redirects
     */
    public static final String REDIRECT_FIELD = "enhancer.engines.linking.redirectField";
    /**
     * If/how redirects (provided by the {@link #REDIRECT_FIELD}) are processed.
     */
    public static final String REDIRECT_MODE = "enhancer.engines.linking.redirectMode";
    /**
     * The maximum number of fise:EntityAnnotations created as suggestion for a fise:TextAnnotation
     */
    public static final String SUGGESTIONS = "enhancer.engines.linking.suggestions";
    /**
     * If enabled {@link MorphoFeatures#getLemma()} values are used instead of the {@link Token#getSpan()} to
     * search/match Entities within the Vocabulary linked against.
     * @see EntityLinkerConfig#isLemmaMatching()
     * @see EntityLinkerConfig#DEFAULT_LEMMA_MATCHING_STATE
     */
    public static final String LEMMA_MATCHING_STATE = "enhancer.engines.linking.lemmaMatching";
    /**
     * Can be used to that the "default language" from <code>null</code>
     * (labels without language tag) to an other value (e.g. "en").<p>
     * The "default language" is used in addition to the language of the
     * processed text to search for labels.
     */
    public static final String DEFAULT_MATCHING_LANGUAGE = "enhancer.engines.linking.defaultMatchingLanguage";
    /**
     * Allows to configure entity type -> dc:type mappings as used for created
     * fise:TextAnnotations
     */
    public static final String TYPE_MAPPINGS = "enhancer.engines.linking.typeMappings";
    /**
     * How well single tokens of the Label needs to match a token of the Text so that they
     * are considered to match. Matching does only allow differences at the end of the
     * token (e.g. "London" -> "Londons major")
     */
    public static final String MIN_TOKEN_SCORE = "enhancer.engines.linking.minTokenScore";
    /**
     * The minimum number of matching tokens. Only "matchable" tokens are counted.
     * For full matches (where all tokens of the Label do match tokens in the text)
     * this parameter is ignored.<p>
     * This parameter is strongly related with the {@link #MIN_LABEL_SCORE}.
     * Typical setting are<ul>
     * <li> <code>{@link #MIN_FOUND_TOKENS}=1</code> and <code>{@link #MIN_LABEL_SCORE} > 0.5</code> (e.g. 0.75)
     * <li> <code>{@link #MIN_FOUND_TOKENS}=2</code> and <code>{@link #MIN_LABEL_SCORE} <= 0.5</code> (e.g. 0.5)
     * </ul>
     * as both settings will ensures that Labels with two tokens where only a single one
     * does match with the text are not suggested.<p>
     * If used in combination with an disambiguation Engine one might want to consider
     * Entities where their labels do match only a single token is such cases a
     * <code>{@link #MIN_FOUND_TOKENS}=1</code> and <code>{@link #MIN_LABEL_SCORE} <= 0.5</code>
     * might be also a meaningful configuration. In such cases users will also want to set the
     * <code>{@link #SUGGESTIONS} > 10</code>.
     */
    public static final String MIN_FOUND_TOKENS = "enhancer.engines.linking.minFoundTokens";
    /**
     * The "Label Score" [0..1] represents how much of the
     * Label of an Entity matches with the Text. It compares the number
     * of Tokens of the Label with the number of Tokens matched to the
     * Text. Not exact matches for Tokens, or if the Tokens within the 
     * label do appear in an other order than in the text do also 
     * reduce this score. <p>
     * The default is {@link EntityLinkerConfig#DEFAULT_MIN_LABEL_SCORE}
     * (value: {@value EntityLinkerConfig#DEFAULT_MIN_LABEL_SCORE})
     */
    public static final String MIN_LABEL_SCORE = "enhancer.engines.linking.minLabelScore";
    /**
     * The "Text Score" [0..1] represents how well the
     * Label of an Entity matches to the selected Span in the Text.
     * It compares the number of matched {@link Token} from
     * the label with the number of Tokens enclosed by the Span
     * in the Text an Entity is suggested for. Not exact matches 
     * for Tokens, or if the Tokens within the label do appear in
     * an other order than in the text do also reduce this score.<p>
     * The default is {@link EntityLinkerConfig#DEFAULT_MIN_TEXT_SCORE}
     * (value: {@value EntityLinkerConfig#DEFAULT_MIN_TEXT_SCORE})
     */
    public static final String MIN_TEXT_SCORE = "enhancer.engines.linking.minTextScore";
    /**
     * Defined as the product of the "Text Score" with the
     * "Label Score" - meaning that this value represents
     * both how well the label matches the text and how much of the
     * label is matched with the text.<p>
     * The default is {@link EntityLinkerConfig#DEFAULT_MIN_MATCH_SCORE}
     * (value: {@value EntityLinkerConfig#DEFAULT_MIN_MATCH_SCORE})
     * @see #MIN_TEXT_SCORE
     * @see #MIN_LABEL_SCORE
     */
    public static final String MIN_MATCH_FACTOR = "enhancer.engines.linking.minMatchScore";
    /**
     * Used as fallback in case a {@link Token} does not have a {@link PosTag} or 
     * {@link NlpAnnotations#POS_ANNOTATION POS annotations} do have a low confidence.
     * In such cases only words that are longer than  this value will be considerd for
     * linking
     */
    public static final String MIN_SEARCH_TOKEN_LENGTH = "enhancer.engines.linking.minSearchTokenLength";
    /**
     * The maximum number of {@link Token} used as search terms with the 
     * {@link EntitySearcher#lookup(String, Set, java.util.List, String[], Integer)}
     * method
     */
    public static final String MAX_SEARCH_TOKENS = "enhancer.engines.linking.maxSearchTokens";
    /**
     * The maximum number of {@link Token} searched around a linkable Token for
     * additional search tokens.<p>
     * As an Example in the text section "at the University of Munich a new procedure to"
     * only "Munich" would be classified as {@link Pos#ProperNoun} and considered as
     * linkable. However for searching it makes sence to use additional Tokens to
     * reduce (or correctly rank) the expected high number of results for "Munich".
     * Because of that "matchable" words surrounding the linkable are considered as
     * included for searches.<p>
     * This parameter allows to configure the maximum distance surounding the current
     * linkable Token other linkable tokens can be included in searches.
     */
    public static final String MAX_SEARCH_TOKEN_DISTANCE = "enhancer.engines.linking.maxSearchTokenDistance";
    /**
     * Adds the dereference feature (STANBOL-333) also to this engine.
     * This will be replaced by STANBOL-336. 
     */
    public static final String DEREFERENCE_ENTITIES = "enhancer.engines.linking.dereference";
    /**
     * Allows to add a list of fields that are included when dereferencing Entities
     */
    public static final String DEREFERENCE_ENTITIES_FIELDS = "enhancer.engines.linking.dereferenceFields";

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
     * suggested terms. The default is <code>1</code>
     */
    public static final int DEFAULT_MIN_FOUND_TOKENS = 1;
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
     * Default value for the maximum distance tokens are
     * considered to be used (in addition to the currently processed on)
     * for searches of Entities.<p>
     * The default is set to <code>3</code> 
     */
    public static final int DEFAULT_MAX_SEARCH_DISTANCE = 3;

    /**
     * Default value for {@link #getNameField()} (rdfs:label)
     */
    public static final UriRef DEFAULT_NAME_FIELD = new UriRef(
        "http://www.w3.org/2000/01/rdf-schema#label");
    /**
     * Default value for {@link #getTypeField()} (rdf:type)
     */
    public static final UriRef DEFAULT_TYPE_FIELD = new UriRef(
        "http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
    /**
     * Default value for {@link #getRedirectField()} (rdf:seeAlso)
     */
    public static final UriRef DEFAULT_REDIRECT_FIELD = new UriRef(
        "http://www.w3.org/2000/01/rdf-schema#seeAlso");
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
     * By default Lemma based matching is deactivated.
     */
    public static final boolean DEFAULT_LEMMA_MATCHING_STATE = false;
    public static final double DEFAULT_MIN_LABEL_SCORE = 0.75;
    public static final double DEFAULT_MIN_TEXT_SCORE = 0.4;
    public static final double DEFAULT_MIN_MATCH_SCORE = 0.3;
    /**
     * Default mapping for Concept types to dc:type values added for
     * TextAnnotations.
     */
    public static final Map<UriRef,UriRef> DEFAULT_ENTITY_TYPE_MAPPINGS;
    
    static { //the default mappings for the three types used by the Stanbol Enhancement Structure
        Map<UriRef,UriRef> mappings = new HashMap<UriRef,UriRef>();
        mappings.put(OntologicalClasses.DBPEDIA_ORGANISATION, OntologicalClasses.DBPEDIA_ORGANISATION);
        mappings.put(new UriRef("http://dbpedia.org/ontology/Newspaper"), OntologicalClasses.DBPEDIA_ORGANISATION);
        mappings.put(new UriRef("http://schema.org/Organization"), OntologicalClasses.DBPEDIA_ORGANISATION);
//        mappings.put(NamespaceEnum.dailymed+"organization",OntologicalClasses.DBPEDIA_ORGANISATION);
        
        mappings.put(OntologicalClasses.DBPEDIA_PERSON, OntologicalClasses.DBPEDIA_PERSON);
        mappings.put(new UriRef("http://xmlns.com/foaf/0.1/Person"), OntologicalClasses.DBPEDIA_PERSON);
        mappings.put(new UriRef("http://schema.org/Person"), OntologicalClasses.DBPEDIA_PERSON);

        mappings.put(OntologicalClasses.DBPEDIA_PLACE, OntologicalClasses.DBPEDIA_PLACE);
        mappings.put(new UriRef("http://schema.org/Place"), OntologicalClasses.DBPEDIA_PLACE);
        mappings.put(new UriRef("http://www.opengis.net/gml/_Feature"), OntologicalClasses.DBPEDIA_PLACE);

        mappings.put(OntologicalClasses.SKOS_CONCEPT, OntologicalClasses.SKOS_CONCEPT);

//        UriRef DRUG = new UriRef(NamespaceEnum.drugbank+"drugs");
//        mappings.put(DRUG.getUnicodeString(), DRUG);
//        mappings.put(NamespaceEnum.dbpediaOnt+"Drug", DRUG);
//        mappings.put(NamespaceEnum.dailymed+"drugs", DRUG);
//        mappings.put(NamespaceEnum.sider+"drugs", DRUG);
//        mappings.put(NamespaceEnum.tcm+"Medicine", DRUG);
//        
//        UriRef DISEASE = new UriRef(NamespaceEnum.diseasome+"diseases");
//        mappings.put(DISEASE.getUnicodeString(), DISEASE);
//        mappings.put(NamespaceEnum.linkedct+"condition", DISEASE);
//        mappings.put(NamespaceEnum.tcm+"Disease", DISEASE);
//
//        UriRef SIDE_EFFECT = new UriRef(NamespaceEnum.sider+"side_effects");
//        mappings.put(SIDE_EFFECT.getUnicodeString(), SIDE_EFFECT);
//        
//        UriRef INGREDIENT = new UriRef(NamespaceEnum.dailymed+"ingredients");
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
     * The dereferenceEntitiesState as set in {@link #activateEntityDereference(Dictionary)}
     */
    private boolean dereferenceEntitiesState;
    /**
     * The minimum length of labels that are looked-up in the directory
     */
    private int minSearchTokenLength = DEFAULT_MIN_SEARCH_TOKEN_LENGTH;
    /**
     * The the maximum number of terms suggested for a word
     */
    private int maxSuggestions = DEFAULT_SUGGESTIONS;
    /**
     * The minimum number of Tokens in the text that must match with 
     * a label of the Entity so that also non-exact matches are
     * used for suggestions
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
    /**
     * Defines the maximum distance tokens are
     * considered to be used (in addition to the currently processed on)
     * for searches of Entities.<p>
     */
    private int maxSearchDistance = DEFAULT_MAX_SEARCH_DISTANCE;
    
    private boolean caseSensitiveMatchingState = DEFAULT_CASE_SENSITIVE_MATCHING_STATE;
    /**
     * Holds the mappings of rdf:type used by concepts to dc:type values used
     * by TextAnnotations. 
     */
    private Map<UriRef,UriRef> typeMappings;
    private Map<UriRef, UriRef> unmodTypeMappings;
    /**
     * The mode on how to process redirect for Entities. 
     */
    private RedirectProcessingMode redirectProcessingMode;
    /**
     * the default DC Type
     */
    private UriRef defaultDcType;
    private UriRef nameField;
    private UriRef redirectField;
    private UriRef typeField;
    private Set<UriRef> dereferencedFields = new HashSet<UriRef>();

    private Set<UriRef> __selectedFields;
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
    public final static float DEFAULT_MIN_TOKEN_SCORE = 0.7f;

    /**
     * By default Entities are dereferenced
     */
    public static final boolean DEFAULT_DEREFERENCE_ENTITIES_STATE = true;
    /**
     * If Tokens match is determined by comparing them using some algorithm.
     * Results need to be in the range [0..1]. This factor defines the minimum
     * similarity value so that a match is assumed. Not that this factor only
     * is used for filtering out non-matching tokens. The similarity value will
     * still used for calculating the confidence
     */
    private float minTokenMatchFactor;
    /**
     * If lemmas are used instead of the Tokens as present in the text to search
     * and match Entities within the linked vocabulary
     */
    private boolean lemmaMatchingState = DEFAULT_LEMMA_MATCHING_STATE;
    private double minLabelScore = DEFAULT_MIN_LABEL_SCORE;
    private double minTextScore = DEFAULT_MIN_TEXT_SCORE;
    private double minMatchScore = DEFAULT_MIN_MATCH_SCORE;

    /**
     * Default constructor the initializes the configuration with the 
     * default values
     */
    public EntityLinkerConfig(){
        setMinSearchTokenLength(DEFAULT_MIN_SEARCH_TOKEN_LENGTH);
        setMaxSuggestions(DEFAULT_SUGGESTIONS);
        setMaxSearchTokens(DEFAULT_MAX_SEARCH_TOKENS);
        setRedirectProcessingMode(DEFAULT_REDIRECT_PROCESSING_MODE);
        typeMappings = new HashMap<UriRef,UriRef>(DEFAULT_ENTITY_TYPE_MAPPINGS);
        unmodTypeMappings = Collections.unmodifiableMap(typeMappings);
        setDefaultDcType(typeMappings.remove(null));
        setNameField(DEFAULT_NAME_FIELD);
        setRedirectField(DEFAULT_REDIRECT_FIELD);
        setTypeField(DEFAULT_TYPE_FIELD);
        setMaxNotFound(DEFAULT_MAX_NOT_FOUND);
        setMinTokenMatchFactor(DEFAULT_MIN_TOKEN_SCORE);
        setDereferenceEntitiesState(DEFAULT_DEREFERENCE_ENTITIES_STATE);
    }
    
    /**
     * Creates a new {@link EntityLinkerConfig} based on the properties
     * in the parsed {@link Dictionary}
     * @param configuration the configuration
     * @param prefixService Optionally a namespace prefix service used to
     * convert '{prefix}:{localname}' parameters in the configuration to URIs.
     * If <code>null</code> is parsed this feature is not supported and parameters
     * are not changed.
     * @return the configured {@link EntityLinkerConfig}
     * @throws ConfigurationException if the parsed configuration is not valid
     */
    public static EntityLinkerConfig createInstance(Dictionary<String,Object> configuration, 
                                                    NamespacePrefixService prefixService) throws ConfigurationException {
        EntityLinkerConfig elc = new EntityLinkerConfig();
        setConfiguration(elc, configuration, prefixService);
        return elc;
    }
    /**
     * Sets the configuration as parsed by the {@link Dictionary} to the
     * parsed {@link EntityLinkerConfig}.
     * @param linkerConfig the instance to apply the configuration to
     * @param configuration the configuration
     * @param prefixService Optionally a namespace prefix service used to
     * convert '{prefix}:{localname}' parameters in the configuration to URIs.
     * If <code>null</code> is parsed this feature is not supported and parameters
     * are not changed.
     * @throws ConfigurationException in case the configuration is invalid
     */
    public static void setConfiguration(EntityLinkerConfig linkerConfig,Dictionary<String,Object> configuration,NamespacePrefixService prefixService) throws ConfigurationException {
        Object value;
        value = configuration.get(NAME_FIELD);
        if(value != null){
            if(value.toString().isEmpty()){
                throw new ConfigurationException(NAME_FIELD,"The configured name field MUST NOT be empty");
            }
            linkerConfig.setNameField(new UriRef(
                getFullName(prefixService,NAME_FIELD,value.toString())));
        }
        
        //init case sensitivity
        value = configuration.get(CASE_SENSITIVE);
        if(value instanceof Boolean){
            linkerConfig.setCaseSensitiveMatchingState((Boolean)value);
        } else if(value != null && !value.toString().isEmpty()){
            linkerConfig.setCaseSensitiveMatchingState(Boolean.valueOf(value.toString()));
        } //if NULL or empty use default
        
        //init TYPE_FIELD
        value = configuration.get(TYPE_FIELD);
        if(value != null){
            if(value.toString().isEmpty()){
                throw new ConfigurationException(TYPE_FIELD,"The configured name field MUST NOT be empty");
            }
            linkerConfig.setTypeField(new UriRef(
                getFullName(prefixService, TYPE_FIELD, value.toString())));
        }
        
        //init REDIRECT_FIELD
        value = configuration.get(REDIRECT_FIELD);
        if(value != null){
            if(value.toString().isEmpty()){
                throw new ConfigurationException(NAME_FIELD,"The configured name field MUST NOT be empty");
            }
            linkerConfig.setRedirectField(new UriRef(
                getFullName(prefixService,REDIRECT_FIELD,value.toString())));
        }
        
        //init MAX_SUGGESTIONS
        value = configuration.get(SUGGESTIONS);
        Integer maxSuggestions;
        if(value instanceof Integer){
            maxSuggestions = (Integer)value;
        } else if (value != null){
            try {
                maxSuggestions = Integer.valueOf(value.toString());
            } catch(NumberFormatException e){
                throw new ConfigurationException(SUGGESTIONS, "Values MUST be valid Integer values > 0",e);
            }
        } else {
            maxSuggestions = null;
        }
        if(maxSuggestions != null){
            if(maxSuggestions < 1){
                throw new ConfigurationException(SUGGESTIONS, "Values MUST be valid Integer values > 0");
            }
            linkerConfig.setMaxSuggestions(maxSuggestions);
        }
        
        //init MIN_FOUND_TOKENS
        value = configuration.get(MIN_FOUND_TOKENS);
        Integer minFoundTokens;
        if(value instanceof Integer){
            minFoundTokens = (Integer)value;
        } else if(value != null){
            try {
                minFoundTokens = Integer.valueOf(value.toString());
            } catch(NumberFormatException e){
                throw new ConfigurationException(MIN_FOUND_TOKENS, "Values MUST be valid Integer values > 0",e);
            }
        } else {
            minFoundTokens = null;
        }
        if(minFoundTokens != null){
            if(minFoundTokens < 1){
                throw new ConfigurationException(MIN_FOUND_TOKENS, "Values MUST be valid Integer values > 0");
            }
            linkerConfig.setMinFoundTokens(minFoundTokens);
        }
        
        //init Label Score parameters
        value = configuration.get(MIN_LABEL_SCORE);
        Double minLabelMatchFactor = null;
        if(value instanceof Number){
            minLabelMatchFactor = Double.valueOf(((Number)value).doubleValue());
        } else if(value != null){
            try {
                minLabelMatchFactor = Double.valueOf(value.toString());
            } catch (NumberFormatException e) {
                throw new ConfigurationException(MIN_LABEL_SCORE, "Parsed value '"
                        +value+"' is not an valid double!");
            }
        }
        try {
            linkerConfig.setMinLabelScore(minLabelMatchFactor);
        } catch (IllegalArgumentException e){
            throw new ConfigurationException(MIN_LABEL_SCORE, e.getMessage());
        }
        value = configuration.get(MIN_TEXT_SCORE);
        Double minTextMatchFactor = null;
        if(value instanceof Number){
            minTextMatchFactor = Double.valueOf(((Number)value).doubleValue());
        } else if(value != null){
            try {
                minTextMatchFactor = Double.valueOf(value.toString());
            } catch (NumberFormatException e) {
                throw new ConfigurationException(MIN_TEXT_SCORE, "Parsed value '"
                        +value+"' is not an valid double!");
            }
        }
        try {
            linkerConfig.setMinTextScore(minTextMatchFactor);
        } catch (IllegalArgumentException e){
            throw new ConfigurationException(MIN_TEXT_SCORE, e.getMessage());
        }
        value = configuration.get(MIN_MATCH_FACTOR);
        Double minMatchFactor = null;
        if(value instanceof Number){
            minMatchFactor = Double.valueOf(((Number)value).doubleValue());
        } else if(value != null){
            try {
                minMatchFactor = Double.valueOf(value.toString());
            } catch (NumberFormatException e) {
                throw new ConfigurationException(MIN_MATCH_FACTOR, "Parsed value '"
                        +value+"' is not an valid double!");
            }
        }
        try {
            linkerConfig.setMinMatchScore(minMatchFactor);
        } catch (IllegalArgumentException e){
            throw new ConfigurationException(MIN_MATCH_FACTOR, e.getMessage());
        }
        
        // init MIN_SEARCH_TOKEN_LENGTH
        value = configuration.get(MIN_SEARCH_TOKEN_LENGTH);
        Integer minSearchTokenLength;
        if(value instanceof Integer){
            minSearchTokenLength = (Integer)value;
        } else if (value != null){
            try {
                minSearchTokenLength = Integer.valueOf(value.toString());
            } catch(NumberFormatException e){
                throw new ConfigurationException(MIN_SEARCH_TOKEN_LENGTH, "Values MUST be valid Integer values > 0",e);
            }
        } else {
            minSearchTokenLength = null;
        }
        if(minSearchTokenLength != null){
            if(minSearchTokenLength < 1){
                throw new ConfigurationException(MIN_SEARCH_TOKEN_LENGTH, "Values MUST be valid Integer values > 0");
            }
            linkerConfig.setMinSearchTokenLength(minSearchTokenLength);
        }
        
        //init LEMMA_MATCHING_STATE
        value = configuration.get(LEMMA_MATCHING_STATE);
        if(value instanceof Boolean){
            linkerConfig.setLemmaMatchingState((Boolean)value);
        } else if (value != null){
            linkerConfig.setLemmaMatchingState(Boolean.parseBoolean(value.toString()));
        }
        
        //init MAX_SEARCH_TOKENS
        value = configuration.get(MAX_SEARCH_TOKENS);
        Integer maxSearchTokens;
        if(value instanceof Integer){
            maxSearchTokens = (Integer)value;
        } else if (value != null){
            try {
                maxSearchTokens = Integer.valueOf(value.toString());
            } catch(NumberFormatException e){
                throw new ConfigurationException(MAX_SEARCH_TOKENS, "Values MUST be valid Integer values > 0",e);
            }
        } else {
            maxSearchTokens = null;
        }
        if(maxSearchTokens != null){
            if(maxSearchTokens < 1){
                throw new ConfigurationException(MAX_SEARCH_TOKENS, "Values MUST be valid Integer values > 0");
            }
            linkerConfig.setMaxSearchTokens(maxSearchTokens);
        }
        
        //init the MAX_SEARCH_TOKEN_DISTANCE
        value = configuration.get(MAX_SEARCH_TOKEN_DISTANCE);
        Integer maxSearchDistance;
        if(value instanceof Integer){
            maxSearchDistance = (Integer)value;
        } else if (value != null){
            try {
                maxSearchDistance = Integer.valueOf(value.toString());
            } catch(NumberFormatException e){
                throw new ConfigurationException(MAX_SEARCH_TOKEN_DISTANCE, "Values MUST be valid Integer values > 0",e);
            }
        } else {
            maxSearchDistance = null;
        }
        if(maxSearchDistance != null){
            if(maxSearchDistance < 1){
                throw new ConfigurationException(MAX_SEARCH_TOKEN_DISTANCE, "Values MUST be valid Integer values > 0");
            }
            linkerConfig.setMaxSearchDistance(maxSearchDistance);
        }

        //init the REDIRECT_PROCESSING_MODE
        value = configuration.get(REDIRECT_MODE);
        if(value != null){
            try {
                linkerConfig.setRedirectProcessingMode(RedirectProcessingMode.valueOf(value.toString()));
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException(REDIRECT_MODE, "Values MUST be one of "+
                    Arrays.toString(RedirectProcessingMode.values()));
            }
        }
        
        //init the DEFAULT_LANGUAGE
        value = configuration.get(DEFAULT_MATCHING_LANGUAGE);
        if(value != null){
            String defaultLang = value.toString().trim();
            if(defaultLang.isEmpty()){
                linkerConfig.setDefaultLanguage(null);
            } else if(defaultLang.length() == 1){
                throw new ConfigurationException(DEFAULT_MATCHING_LANGUAGE, "Illegal language code '"+
                    defaultLang+"'! Language Codes MUST BE at least 2 chars long.");
            } else {
                linkerConfig.setDefaultLanguage(defaultLang);
            }
        }
        
        // init MIN_TOKEN_MATCH_FACTOR
        value=configuration.get(MIN_TOKEN_SCORE);
        float minTokenMatchFactor;
        if(value instanceof Number){
            minTokenMatchFactor = ((Number)value).floatValue();
        } else if(value != null){
            try {
                minTokenMatchFactor = Float.valueOf(value.toString());
            } catch (NumberFormatException e) {
                throw new ConfigurationException(MIN_TOKEN_SCORE, 
                    "Unable to parse the minimum token match factor from the parsed value "+value,e);
            }
            if(minTokenMatchFactor < 0){
                minTokenMatchFactor = EntityLinkerConfig.DEFAULT_MIN_TOKEN_SCORE;
            }
        } else {
            minTokenMatchFactor = EntityLinkerConfig.DEFAULT_MIN_TOKEN_SCORE;
        }
        if(minTokenMatchFactor == 0 || minTokenMatchFactor > 1){
            throw new ConfigurationException(MIN_TOKEN_SCORE, 
                "The minimum token match factor MUST be > 0 and <= 1 (negative values for the default)");
        }
        linkerConfig.setMinTokenMatchFactor(minTokenMatchFactor);

        //init type mappings
        value = configuration.get(TYPE_MAPPINGS);
        if(value instanceof String[]){ //support array
            value = Arrays.asList((String[])value);
        } else if(value instanceof String) { //single value
            value = Collections.singleton(value);
        }
        if(value instanceof Collection<?>){ //and collection
            log.info("Init Type Mappings");
            configs :
            for(Object o : (Iterable<?>)value){
                if(o != null){
                    StringBuilder usage = new StringBuilder("useages: ");
                    usage.append("a: '{uri}' short for {uri} > {uri} | ");
                    usage.append("b: '{source1};{source2};..;{sourceN} > {target}'");
                    String[] config = o.toString().split(">");
                    if(config[0].isEmpty()){
                        log.warn("Invalid Type Mapping Config '{}': Missing Source Type ({}) -> ignore this config",
                            o,usage);
                        continue configs;
                    }
                    String[] sourceTypes = config[0].split(";");
                    if(sourceTypes.length > 1 && (config.length < 2 || config[1].isEmpty())){
                        log.warn("Invalid Type Mapping Config '{}': Missing Target Type '{}' ({}) -> ignore this config",
                            o,usage);
                        continue configs;
                    }
                    String targetType = config.length < 2 ? sourceTypes[0] : config[1];
                    targetType = getFullName(prefixService,TYPE_MAPPINGS,targetType.trim()); //support for ns:localName
                    try { //validate
                        new URI(targetType);
                    } catch (URISyntaxException e) {
                        log.warn("Invalid URI '{}' in Type Mapping Config '{}' -> ignore this config",
                            sourceTypes[0],o);
                        continue configs;
                    }
                    UriRef targetUri = new UriRef(targetType);
                    for(String sourceType : sourceTypes){
                        if(!sourceType.isEmpty()){
                            sourceType = getFullName(prefixService,TYPE_MAPPINGS,sourceType.trim()); //support for ns:localName
                            try { //validate
                                new URI(sourceType);
                                UriRef old = linkerConfig.setTypeMapping(sourceType, targetUri);
                                if(old == null){
                                    log.info(" > add type mapping {} > {}", sourceType,targetType);
                                } else {
                                    log.info(" > set type mapping {} > {} (old: {})", 
                                        new Object[]{sourceType,targetType,old.getUnicodeString()});
                                }
                            } catch (URISyntaxException e) {
                                log.warn("Invalid URI '{}' in Type Mapping Config '{}' -> ignore this source type",
                                    sourceTypes[0],o);
                            }
                        }
                    }
                }
            }
        } else {
            log.debug("No Type mappings configured");
        }
        //dereference entities
        value = configuration.get(DEREFERENCE_ENTITIES);
        if(value instanceof Boolean){
            linkerConfig.setDereferenceEntitiesState(((Boolean)value).booleanValue());
        } else if(value != null && !value.toString().isEmpty()){
            linkerConfig.setDereferenceEntitiesState(Boolean.parseBoolean(value.toString()));
        }
        if(linkerConfig.isDereferenceEntitiesEnabled()){
            value = configuration.get(DEREFERENCE_ENTITIES_FIELDS);
            if(value instanceof String[]){
                for(String field : (String[])value){
                    if(field != null && !field.isEmpty()){
                        linkerConfig.getDereferencedFields().add(
                            new UriRef(getFullName(prefixService,DEREFERENCE_ENTITIES_FIELDS,field)));
                    }
                }
            } else if(value instanceof Collection<?>){
                for(Object field : (Collection<?>)value){
                    if(field != null && !field.toString().isEmpty()){
                        linkerConfig.getDereferencedFields().add(
                            new UriRef(getFullName(prefixService,DEREFERENCE_ENTITIES_FIELDS,field.toString())));
                    }
                }
            } else if(value instanceof String){
                if(!value.toString().isEmpty()){
                    linkerConfig.getDereferencedFields().add(
                        new UriRef(getFullName(prefixService,DEREFERENCE_ENTITIES_FIELDS,value.toString())));
                }
            } else if(value != null){
                throw new ConfigurationException(DEREFERENCE_ENTITIES_FIELDS, 
                    "Dereference Entities_Fields MUST BE parsed as String[], Collection<String> or "
                    + "String (single value). The actual value '"+value+"'(type: '"+value.getClass() 
                    + "') is NOT supported");
            }else { //value == null 
            	log.debug("No deference fields for entity configured");
            }
        }

        
    }
    
    private static String getFullName(NamespacePrefixService prefixService, String property,String value) throws ConfigurationException {
        String prefix = NamespaceMappingUtils.getPrefix(value);
        if(prefixService == null){
            if(prefix != null){
                throw new ConfigurationException(property, "'{prefix}:{localname}' tpye configurations "
                    + "are not supported if no "+NamespacePrefixService.class.getSimpleName()
                    + "is present (configured value='"+value+"')!");
            } else {
                return value;
            }
        } else {
            String uri = prefixService.getFullName(value);
            if(uri == null){
                throw new ConfigurationException(property, "The prefix '"+prefix
                        + "' as used by the configured value '"+value+"' is unknow to the"
                        + NamespacePrefixService.class.getSimpleName());
            }
            log.debug("mapped '{}' -> '{}'",value,uri);
            return uri;
        }
    }
        
    /**
     * Getter for the uri of the field used for the names in the taxonomy
     * (e.g. rdfs:label, skos:prefLabel). Needs to return the full URI
     * @return the field used for the names of in the Taxonomy.
     */
    public final UriRef getNameField() {
        return nameField;
    }
    /**
     * Setter for the uri of the field used for the names in the taxonomy
     * (e.g. rdfs:label, skos:prefLabel).
     * @param nameField the nameField to set
     */
    public final void setNameField(UriRef nameField) {
        this.nameField = nameField;
        __selectedFields = null;
    }
    /**
     * Getter for the dereferencedFields. This is a read- and write-able
     * set that allows to configure the fields that should be dereferenced
     * @return
     */
    public final Set<UriRef> getDereferencedFields(){
        return dereferencedFields;
    }
    /**
     * The field used to follow redirects (typically rdf:seeAlso)
     * @return the redirect field
     */
    public final UriRef getRedirectField() {
        return redirectField;
    }
    /**
     * The field used to follow redirects (typically rdf:seeAlso)
     * @param redirectField the redirectField to set
     */
    public final void setRedirectField(UriRef redirectField) {
        this.redirectField = redirectField;
        __selectedFields = null;
    }
    /**
     * The field used to lookup the types (typically rdf:type)
     * @return the field name used to lookup types
     */
    public final UriRef getTypeField() {
        return typeField;
    }
    /**
     * The field used to lookup the types (typically rdf:type)
     * @param typeField the typeField to set
     */
    public final void setTypeField(UriRef typeField) {
        this.typeField = typeField;
        __selectedFields = null;
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
        if(maxSearchTokens == 0){
            this.maxSearchTokens = DEFAULT_MAX_SEARCH_TOKENS;
        } else if (maxSearchTokens < 0){
            throw new IllegalArgumentException("The maxSearchToken value MUST BE >= 0 (0 for setting the default)");
        } else {
            this.maxSearchTokens = maxSearchTokens;
        }
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
    /* REMOVED because getTypemappings.remove(conceptType) can be used anyway
     * Removes the mapping for the parsed concept type
     * @param conceptType the concept type to remove the mapping
     * @return the previously mapped dc:type value or <code>null</code> if
     * no mapping for the parsed concept type was present
    public UriRef removeTypeMapping(UriRef conceptType){
        return typeMappings.remove(conceptType);
    }
     */
    /**
     * 
     * @param conceptType the type of the concept or <code>null</code> to
     * add the default dc:type mapping. See also {@link #setDefaultDcType(UriRef)}
     * @param dcType the dc:type for the parsed concept type
     * @return the previously mapped dc:type value if an existing mapping
     * was updated or <code>null</code> if a new mapping was added.
     */
    public UriRef setTypeMapping(String conceptType, UriRef dcType){
        if(dcType == null) {
            throw new IllegalArgumentException("The parsed dc:type URI MUST NOT be NULL!");
        }
        if(conceptType == null){ //handle setting of the default dc:type value
            UriRef oldDefault = getDefaultDcType();
            setDefaultDcType(dcType);
            return oldDefault;
        }
        return typeMappings.put(new UriRef(conceptType), dcType);
    }
    
    /**
     * Setter for the default dc:type of linked entities if for none of the
     * types of the suggestions a {@link #getTypeMappings()} exists. Set this
     * to <code>null</code> to specify that no dc:type should be set in such
     * cases.
     * @param defaultDcType the defaultDcType to set
     */
    public void setDefaultDcType(UriRef defaultDcType) {
        this.defaultDcType = defaultDcType;
    }
    /**
     * The default type for Entities if no {@link #getTypeMappings() type mapping}
     * is present. <code>null</code> means that no type should be set if no
     * explicit mapping exists
     * @return the defaultDcType
     */
    public UriRef getDefaultDcType() {
        return defaultDcType;
    }
    /**
     * Setter for the mode on how to deal with redirects
     * @param redirectProcessingMode the redirectProcessingMode to set
     */
    public void setRedirectProcessingMode(RedirectProcessingMode redirectProcessingMode) {
        this.redirectProcessingMode = redirectProcessingMode;
        __selectedFields = null;
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
    public Map<UriRef,UriRef> getTypeMappings() {
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
            this.minTokenMatchFactor = DEFAULT_MIN_TOKEN_SCORE;
        } else if(minTokenMatchFactor == 0 || minTokenMatchFactor > 1){
            throw new IllegalArgumentException("minimum Token Match Facter MUST be > 0 <= 1 (parsed: "+minTokenMatchFactor+")!");
        } else {
            this.minTokenMatchFactor = minTokenMatchFactor;
        }
    }
    /**
     * Getter for the maximum distance tokens are
     * considered to be used (in addition to the currently processed on)
     * for searches of Entities.
     * @return the maximum search token distance
     */
    public int getMaxSearchDistance() {
        return maxSearchDistance;
    }
    /**
    /**
     * Getter for the maximum distance tokens are
     * considered to be used (in addition to the currently processed on)
     * for searches of Entities.
     * @param maxSearchDistance the maximum search token distance. If
     * values &lt;= 0 are parsed the value is set to
     *  {@link #DEFAULT_MAX_SEARCH_DISTANCE}
     */
    public void setMaxSearchDistance(int maxSearchDistance) {
        if(maxSearchDistance <= 0){
            maxSearchDistance = DEFAULT_MAX_SEARCH_DISTANCE;
        } else {
            this.maxSearchDistance = maxSearchDistance;
        }
    }
    public boolean isLemmaMatching() {
        return lemmaMatchingState;
    }
    
    public void setLemmaMatchingState(Boolean lemmaMatchingState) {
        if(lemmaMatchingState == null){
            this.lemmaMatchingState = DEFAULT_LEMMA_MATCHING_STATE;
        } else {
            this.lemmaMatchingState = lemmaMatchingState;
        }
    }
    /**
     * The minimum LabelScore required to suggest an Entity.<p>
     * The "Label Score" [0..1] represents how much of the
     * Label of an Entity matches with the Text. It compares the number
     * of Tokens of the Label with the number of Tokens matched to the
     * Text. Not exact matches for Tokens, or if the Tokens within the 
     * label do appear in an other order than in the text do also 
     * reduce this score.
     * @return the minimum required LabelScore
     */
    public double getMinLabelScore() {
        return minLabelScore;
    }
    /**
     * Setter for the minimum label score for suggested entities
     * @param score the score [0..1] or <code>null</code> to reset
     * to the default.
     */
    public void setMinLabelScore(Double score){
        if(score == null){
            minLabelScore = DEFAULT_MIN_LABEL_SCORE;
        } else if(score > 1 || score < 0) {
            throw new IllegalArgumentException("The parsed MinLabelScore '"
                + score + "' MUST BE in the range [0..1]!");
        } else {
            minLabelScore = score;
        }
    }
    /**
     * The minimum Text Score required to suggest an Entity.<p>
     * The "Text Score" [0..1] represents how well the
     * Label of an Entity matches to the selected Span in the Text.
     * It compares the number of matched {@link Token} from
     * the label with the number of Tokens enclosed by the Span
     * in the Text an Entity is suggested for. Not exact matches 
     * for Tokens, or if the Tokens within the label do appear in
     * an other order than in the text do also reduce this score
     * @return the minimum required Text Score for labels of suggested
     * Entities
     */
    public double getMinTextScore() {
        return minTextScore;
    }
    /**
     * Setter for the minimum text score for suggested entities
     * @param score the score [0..1] or <code>null</code> to reset
     * to the default.
     */
    public void setMinTextScore(Double score){
        if(score == null){
            minTextScore = DEFAULT_MIN_TEXT_SCORE;
        } else if(score > 1 || score < 0) {
            throw new IllegalArgumentException("The parsed MinTextScore '"
                + score + "' MUST BE in the range [0..1]!");
        } else {
            minTextScore = score;
        }
    }    
    /**
     * Getter for the minimum match Score of Entity labels against the
     * Text.<p>
     * This is the product of the {@link #getMinLabelScore()} with the
     * {@link #getMinTextScore()} - meaning that this value represents
     * both how well the label matches the text and how much of the
     * label is matched with the text.
     * @return
     */
    public double getMinMatchScore() {
        return minMatchScore;
    }
    /**
     * Setter for the minimum text score for suggested entities
     * @param score the score [0..1] or <code>null</code> to reset
     * to the default.
     */
    public void setMinMatchScore(Double score){
        if(score == null){
            minMatchScore = DEFAULT_MIN_MATCH_SCORE;
        } else if(score > 1 || score < 0) {
            throw new IllegalArgumentException("The parsed MinMatchScore '"
                + score + "' MUST BE in the range [0..1]!");
        } else {
            minMatchScore = score;
        }
    }
    /**
     * Setter for the dereference entities state.
     * @param state the state or <code>null</code> to set the
     * default.
     */
    public void setDereferenceEntitiesState(Boolean state) {
        if(state == null){
            this.dereferenceEntitiesState = DEFAULT_DEREFERENCE_ENTITIES_STATE;
        } else {
            this.dereferenceEntitiesState = state;
        }
        __selectedFields = null;
    }
    /**
     * Getter for the dereference entities state
     * @return <code>true</code> if enabled otherwise <code>false</code>
     */
    public boolean isDereferenceEntitiesEnabled(){
        return dereferenceEntitiesState;
    }

    /**
     * Getter for all fields that need to be selected based on the
     * current EntityLinker configuration. This includes<ul>
     * <li> {@link #getNameField()}
     * <li> {@link #getTypeField()}
     * <li> {@link #getRedirectField()} if {@link #getRedirectProcessingMode()} 
     * != {@link RedirectProcessingMode#IGNORE}
     * <li> {@link #getDereferencedFields()} if {@link #isDereferenceEntitiesEnabled()}
     * </ul>
     * @return the selected fields for queries against the linked vocabulary.
     */
    public Set<UriRef> getSelectedFields() {
        if(__selectedFields == null){
            Set<UriRef> fields = new HashSet<UriRef>();
            fields.add(nameField);
            fields.add(typeField);
            if(redirectProcessingMode != RedirectProcessingMode.IGNORE){
                fields.add(redirectField);
            }
            if(dereferenceEntitiesState){
                fields.addAll(dereferencedFields);
            }
            __selectedFields = Collections.unmodifiableSet(fields);
            return __selectedFields;
        } else {
            return __selectedFields;
        }
    }
}
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
package org.apache.stanbol.enhancer.engines.keywordextraction.engine;

import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.getAnalysedText;
import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.getLanguage;
import static org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum.getFullName;

import java.lang.Integer; //preserve this!

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.stanboltools.offline.OfflineMode;
import org.apache.stanbol.enhancer.engines.keywordextraction.impl.EntityLinker;
import org.apache.stanbol.enhancer.engines.keywordextraction.impl.LinkedEntity;
import org.apache.stanbol.enhancer.engines.keywordextraction.impl.Suggestion;
import org.apache.stanbol.enhancer.engines.keywordextraction.impl.LinkedEntity.Occurrence;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.EntityLinkerConfig;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.EntityLinkerConfig.RedirectProcessingMode;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.EntitySearcher;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.LabelTokenizer;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.LabelTokenizerManager;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.TextProcessingConfig;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.impl.EntityhubSearcher;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.impl.ReferencedSiteSearcher;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.impl.TrackingEntitySearcher;
import org.apache.stanbol.enhancer.nlp.NlpAnnotations;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.Pos;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
import org.apache.stanbol.enhancer.nlp.utils.LanguageConfiguration;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * TODO: Split "Engine" and "EngineConfiguration" in two classes
 * @author Rupert Westenthaler
 *
 */
@Component(
    configurationFactory = true, 
    policy = ConfigurationPolicy.REQUIRE, // the baseUri is required!
    specVersion = "1.1", 
    metatype = true, 
    immediate = true,
    inherit = true)
@Service
@org.apache.felix.scr.annotations.Properties(value={
    @Property(name=EnhancementEngine.PROPERTY_NAME),
    @Property(name=KeywordLinkingEngine.REFERENCED_SITE_ID),
    @Property(name=KeywordLinkingEngine.NAME_FIELD,value=EntityLinkerConfig.DEFAULT_NAME_FIELD),
    @Property(name=KeywordLinkingEngine.CASE_SENSITIVE,boolValue=EntityLinkerConfig.DEFAULT_CASE_SENSITIVE_MATCHING_STATE),
    @Property(name=KeywordLinkingEngine.TYPE_FIELD,value=EntityLinkerConfig.DEFAULT_TYPE_FIELD),
    @Property(name=KeywordLinkingEngine.REDIRECT_FIELD,value=EntityLinkerConfig.DEFAULT_REDIRECT_FIELD),
    @Property(name=KeywordLinkingEngine.REDIRECT_PROCESSING_MODE,options={
        @PropertyOption(
            value='%'+KeywordLinkingEngine.REDIRECT_PROCESSING_MODE+".option.ignore",
            name="IGNORE"),
        @PropertyOption(
            value='%'+KeywordLinkingEngine.REDIRECT_PROCESSING_MODE+".option.addValues",
            name="ADD_VALUES"),
        @PropertyOption(
                value='%'+KeywordLinkingEngine.REDIRECT_PROCESSING_MODE+".option.follow",
                name="FOLLOW")
        },value="IGNORE"),
    @Property(name=KeywordLinkingEngine.MIN_SEARCH_TOKEN_LENGTH,
        intValue=EntityLinkerConfig.DEFAULT_MIN_SEARCH_TOKEN_LENGTH),
    @Property(name=KeywordLinkingEngine.MIN_TOKEN_MATCH_FACTOR,floatValue=
            EntityLinkerConfig.DEFAULT_MIN_TOKEN_MATCH_FACTOR),
    //Can no longer be supported with the new NLP chain!
    //@Property(name=KeywordLinkingEngine.KEYWORD_TOKENIZER,boolValue=false),
    @Property(name=KeywordLinkingEngine.MAX_SUGGESTIONS,
        intValue=EntityLinkerConfig.DEFAULT_SUGGESTIONS),
    @Property(name=KeywordLinkingEngine.PROCESS_ONLY_PROPER_NOUNS_STATE,
        boolValue=KeywordLinkingEngine.DEFAULT_PROCESS_ONLY_PROPER_NOUNS_STATE),
    @Property(name=KeywordLinkingEngine.PROCESSED_LANGUAGES,
        cardinality=Integer.MAX_VALUE,
        value={"*"}),
    @Property(name=KeywordLinkingEngine.DEFAULT_MATCHING_LANGUAGE,value=""),
    @Property(name=KeywordLinkingEngine.TYPE_MAPPINGS,cardinality=Integer.MAX_VALUE),
    @Property(name=KeywordLinkingEngine.DEREFERENCE_ENTITIES,
        boolValue=KeywordLinkingEngine.DEFAULT_DEREFERENCE_ENTITIES_STATE),
    @Property(name=Constants.SERVICE_RANKING,intValue=0)
})
public class KeywordLinkingEngine 
        extends AbstractEnhancementEngine<RuntimeException,RuntimeException> 
        implements EnhancementEngine, ServiceProperties {

    private final Logger log = LoggerFactory.getLogger(KeywordLinkingEngine.class);
    /**
     * This is used to check the content type of parsed {@link ContentItem}s for
     * plain text
     */
    protected static final String TEXT_PLAIN_MIMETYPE = "text/plain";
    /**
     * Contains the only supported mime type {@link #TEXT_PLAIN_MIMETYPE}
     */
    protected static final Set<String> SUPPORTED_MIMETYPES = Collections.singleton(TEXT_PLAIN_MIMETYPE);
    /**
     * The default value for the Execution of this Engine.
     * This Engine creates TextAnnotations that should not be processed by other Engines.
     * Therefore it uses a lower rank than {@link ServiceProperties#ORDERING_DEFAULT}
     * to ensure that other engines do not get confused
     */
    public static final Integer DEFAULT_ORDER = ServiceProperties.ORDERING_DEFAULT - 10;

    
    public static final String REFERENCED_SITE_ID = "org.apache.stanbol.enhancer.engines.keywordextraction.referencedSiteId";
    public static final String NAME_FIELD = "org.apache.stanbol.enhancer.engines.keywordextraction.nameField";
    public static final String TYPE_FIELD = "org.apache.stanbol.enhancer.engines.keywordextraction.typeField";
    public static final String CASE_SENSITIVE = "org.apache.stanbol.enhancer.engines.keywordextraction.caseSensitive";
    public static final String REDIRECT_FIELD = "org.apache.stanbol.enhancer.engines.keywordextraction.redirectField";
    public static final String REDIRECT_PROCESSING_MODE = "org.apache.stanbol.enhancer.engines.keywordextraction.redirectMode";
    public static final String MAX_SUGGESTIONS = "org.apache.stanbol.enhancer.engines.keywordextraction.maxSuggestions";
    public static final String MIN_FOUND_TOKENS= "org.apache.stanbol.enhancer.engines.keywordextraction.minFoundTokens";
    public static final String DEFAULT_MATCHING_LANGUAGE = "org.apache.stanbol.enhancer.engines.keywordextraction.defaultMatchingLanguage";
    public static final String TYPE_MAPPINGS = "org.apache.stanbol.enhancer.engines.keywordextraction.typeMappings";
    //public static final String KEYWORD_TOKENIZER = "org.apache.stanbol.enhancer.engines.keywordextraction.keywordTokenizer";
    public static final String MIN_TOKEN_MATCH_FACTOR = "org.apache.stanbol.enhancer.engines.keywordextraction.minTokenMatchFactor";
//  public static final String ENABLE_CHUNKER = "org.apache.stanbol.enhancer.engines.keywordextraction.enableChunker";
    //Search parameters
    /**
     * Used as fallback in case a {@link Token} does not have a {@link PosTag} or 
     * {@link NlpAnnotations#POS_ANNOTATION POS annotations} do have a low confidence.
     * In such cases only words that are longer than  this value will be considerd for
     * linking
     */
    public static final String MIN_SEARCH_TOKEN_LENGTH = "org.apache.stanbol.enhancer.engines.keywordextraction.minSearchTokenLength";
    /**
     * The maximum number of {@link Token} used as search terms with the 
     * {@link EntitySearcher#lookup(String, Set, java.util.List, String[], Integer)}
     * method
     */
    public static final String MAX_SEARCH_TOKENS = "org.apache.stanbol.enhancer.engines.keywordextraction.masSearchTokens";
    /**
     * The maximum number of {@link Token} searched around a "processable" Token for
     * additional search tokens.<p>
     * As an Example in the text section "at the University of Munich a new procedure to"
     * only "Munich" would be classified as {@link Pos#ProperNoun} and considered as
     * "processible". However for searching it makes sence to use additional Tokens to
     * reduce (or correctly rank) the expected high number of results for "Munich".
     * Because of that "matchable" words suronding the "processable" are considered as
     * included for searches.<p>
     * This parameter allows to configure the maximum distance surounding the current
     * "processable" Token other "processable" tokens can be included in searches.
     */
    public static final String MAX_SEARCH_TOKEN_DISTANCE = "org.apache.stanbol.enhancer.engines.keywordextraction.maxSearchTokenDistance";
    
    /**
     * {@link NlpAnnotations#POS_ANNOTATION POS annotations} with a lower
     * confidence than this value will be ignored.
     */
    public static final String MIN_POS_TAG_PROBABILITY = "org.apache.stanbol.enhancer.engines.keywordextraction.minPosTagProbability";
    /**
     * If enabled only {@link Pos#ProperNoun}, {@link Pos#Foreign} and {@link Pos#Acronym} are Matched. If
     * deactivated all Tokens with the category {@link LexicalCategory#Noun} and 
     * {@link LexicalCategory#Residual} are considered for matching.<p>
     * This property allows an easy configuration of the matching that is sufficient for most usage scenarios.
     * Users that need to have more control can configure language specific mappings by using
     * {@link #PARAM_LEXICAL_CATEGORIES}, {@link #PARAM_POS_TYPES}, {@link #PARAM_POS_TAG} and
     * {@link #PARAM_POS_PROBABILITY} in combination with the {@link #PROCESSED_LANGUAGES}
     * configuration.<p>
     * The {@link #DEFAULT_PROCESS_ONLY_PROPER_NOUNS_STATE default} if this is <code>false</code>
     */
    public static final String PROCESS_ONLY_PROPER_NOUNS_STATE = "org.apache.stanbol.enhancer.engines.keywordextraction.properNounsState";
    public static final boolean DEFAULT_PROCESS_ONLY_PROPER_NOUNS_STATE = false;
    public static Set<Pos> DEFAULT_PROCESSED_POS_TYPES = TextProcessingConfig.DEFAULT_PROCESSED_POS;
    public static Set<LexicalCategory> DEFAULT_PROCESSED_LEXICAL_CATEGORIES = TextProcessingConfig.DEFAULT_PROCESSED_LEXICAL_CATEGORIES;
    /**
     * Allows to configure the processed languages by using the syntax supported by {@link LanguageConfiguration}.
     * In addition this engine supports language specific configurations for matched {@link LexicalCategory}
     * {@link Pos} and String POS tags as well as Pos annotation probabilities by using the parameters
     * {@link #PARAM_LEXICAL_CATEGORIES}, {@link #PARAM_POS_TYPES}, {@link #PARAM_POS_TAG} and
     * {@link #PARAM_POS_PROBABILITY}.<p>
     * See the documentation of {@link LanguageConfiguration} for details of the Syntax.
     */
    public static final String PROCESSED_LANGUAGES = "org.apache.stanbol.enhancer.engines.keywordextraction.processedLanguages";
    /*
     * Parameters used for language specific text processing configurations
     */
    public static final String PARAM_LEXICAL_CATEGORIES = "lc";
    public static final String PARAM_POS_TYPES = "pos";
    public static final String PARAM_POS_TAG = "tag";
    public static final String PARAM_POS_PROBABILITY = "prob";
    /**
     * Adds the dereference feature (STANBOL-333) also to this engine.
     * This will be replaced by STANBOL-336. 
     */
    public static final String DEREFERENCE_ENTITIES = "org.apache.stanbol.enhancer.engines.keywordextraction.dereference";
    /**
     * The default state to dereference entities set to <code>true</code>.
     */
    public static final boolean DEFAULT_DEREFERENCE_ENTITIES_STATE = true;
    /**
     * Allows to add a list of fields that are included when dereferencing Entities
     */
    public static final String DEREFERENCE_ENTITIES_FIELDS = "org.apache.stanbol.enhancer.engines.keywordextraction.dereferenceFields";
    /**
     * Additional fields added for dereferenced entities
     */
    private static final Collection<String> DEREFERENCE_FIELDS = Arrays.asList(
        getFullName("rdfs:comment"),
        getFullName("geo:lat"),
        getFullName("geo:long"),
        getFullName("foaf:depiction"),
        getFullName("dbp-ont:thumbnail"));
    /**
     * The dereferenceEntitiesState as set in {@link #activateEntityDereference(Dictionary)}
     */
    private boolean dereferenceEntitiesState;
    /**
     * Default set of languages. This is an empty set indicating that texts in any
     * language are processed. 
     */
    public static final Set<String> DEFAULT_LANGUAGES = Collections.emptySet();
    public static final double DEFAULT_MIN_POS_TAG_PROBABILITY = 0.6667;
    /**
     * The languages this engine is configured to enhance. An empty List is
     * considered as active for any language
     */
    private LanguageConfiguration languages = new LanguageConfiguration(PROCESSED_LANGUAGES, new String[]{"*"});
    /**
     * The literal representing the LangIDEngine as creator.
     */
    public static final Literal LANG_ID_ENGINE_NAME = LiteralFactory.getInstance().createTypedLiteral("org.apache.stanbol.enhancer.engines.langid.LangIdEnhancementEngine");

    /**
     * The default value for the LIMIT of the {@link EntitySearcher}
     */
    private static final int DEFAULT_ENTITY_SEARCHER_LIMIT = 10;

    /**
     * The entitySearcher used for linking
     */
    protected EntitySearcher entitySearcher;
    private EntityLinkerConfig linkerConfig;
    
    private TextProcessingConfig defaultTextProcessingConfig;
    private Map<String,TextProcessingConfig> textProcessingConfigs = new HashMap<String,TextProcessingConfig>();
    
    //NOTE as I want to inject an instance of LabelTokenizerManager I need to implement my own
    //bind/unbind methods as the generated methods would expect a field 
    // "LabelTokenizerManager labelTokenizer" and not "LabelTokenizer labelTokenizer"
    @org.apache.felix.scr.annotations.Reference(referenceInterface=LabelTokenizerManager.class,
            bind="bindLabelTokenizer",unbind="unbindLabelTokenizer")
    protected LabelTokenizer labelTokenizer;

    protected void bindLabelTokenizer(LabelTokenizerManager ltm){
        labelTokenizer = ltm;
    }
    
    protected void unbindLabelTokenizer(LabelTokenizerManager ltm){
        labelTokenizer = null;
    }
    
    
    
    /**
     * The literalFactory used to create typed literals
     */
    private LiteralFactory literalFactory = LiteralFactory.getInstance();
    
    /**
     * The {@link OfflineMode} is used by Stanbol to indicate that no external service should be referenced.
     * For this engine that means it is necessary to check if the used {@link ReferencedSite} can operate
     * offline or not.
     * 
     * @see #enableOfflineMode(OfflineMode)
     * @see #disableOfflineMode(OfflineMode)
     */
    @org.apache.felix.scr.annotations.Reference(
        cardinality = ReferenceCardinality.OPTIONAL_UNARY, 
        policy = ReferencePolicy.DYNAMIC, 
        bind = "enableOfflineMode", 
        unbind = "disableOfflineMode", 
        strategy = ReferenceStrategy.EVENT)
    private OfflineMode offlineMode;
    /**
     * The name of the reference site ('local' or 'entityhub') if the
     * Entityhub is used for enhancing
     */
    protected String referencedSiteName;

    /**
     * Called by the ConfigurationAdmin to bind the {@link #offlineMode} if the service becomes available
     * 
     * @param mode
     */
    protected final void enableOfflineMode(OfflineMode mode) {
        this.offlineMode = mode;
    }

    /**
     * Called by the ConfigurationAdmin to unbind the {@link #offlineMode} if the service becomes unavailable
     * 
     * @param mode
     */
    protected final void disableOfflineMode(OfflineMode mode) {
        this.offlineMode = null;
    }

    /**
     * Returns <code>true</code> only if Stanbol operates in {@link OfflineMode}.
     * 
     * @return the offline state
     */
    protected final boolean isOfflineMode() {
        return offlineMode != null;
    }
    
    /**
     * Default constructor as used by OSGI. This expects that 
     * {@link #activate(ComponentContext)} is called before usage
     */
    public KeywordLinkingEngine() {
    }
    /**
     * Internal Constructor used by {@link #createInstance(EntitySearcher, TextProcessingConfig, EntityLinkerConfig)}
     * @param entitySearcher The component used to lookup Entities
     * @param textProcessingConfig The configuration on how to use the {@link AnalysedText} content part of
     * processed {@link ContentItem}s
     * @param linkingConfig the configuration for the EntityLinker
     */
    protected KeywordLinkingEngine(EntitySearcher entitySearcher,TextProcessingConfig textProcessingConfig, 
                                   EntityLinkerConfig linkingConfig, LabelTokenizer labelTokenizer){
        this.linkerConfig = linkingConfig != null ? linkingConfig : new EntityLinkerConfig();
        this.defaultTextProcessingConfig = textProcessingConfig != null ? textProcessingConfig : new TextProcessingConfig();
        this.textProcessingConfigs = Collections.emptyMap();
        this.entitySearcher = entitySearcher;
        this.labelTokenizer = labelTokenizer;
    }
    /**
     * Allows to create an instance that can be used outside of an OSGI
     * environment. This is mainly intended for unit tests.
     * @param entitySearcher The component used to lookup Entities
     * @param textProcessingConfig The configuration on how to use the {@link AnalysedText} content part of
     * processed {@link ContentItem}s
     * @param linkingConfig the configuration for the EntityLinker
     * @return the created engine instance
     */
    public static KeywordLinkingEngine createInstance(EntitySearcher entitySearcher,
                                                      TextProcessingConfig textProcessingConfig,
                                                      EntityLinkerConfig linkingConfig,
                                                      LabelTokenizer labelTokenizer){
        return new KeywordLinkingEngine(entitySearcher,textProcessingConfig,linkingConfig,labelTokenizer);
    }

    @Override
    public Map<String,Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(
            ENHANCEMENT_ENGINE_ORDERING,
            (Object) DEFAULT_ORDER));
    }

    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        log.info("canEnhancer {}",ci.getUri());
        if(isOfflineMode() && !entitySearcher.supportsOfflineMode()){
            log.warn("{} '{}' is inactive because EntitySearcher does not support Offline mode!",
                getClass().getSimpleName(),getName());
            return CANNOT_ENHANCE;
        }
        String language = getLanguage(this, ci, false);
        if(language == null || !languages.isLanguage(language)){
            log.debug("Engine {} ignores ContentItem {} becuase language {} is not condigured.",
                new Object[]{ getName(), ci.getUri(), language});
            return CANNOT_ENHANCE;
        }
        //we need a detected language, the AnalyzedText contentPart with
        //Tokens.
        AnalysedText at = getAnalysedText(this, ci, false);
        return at != null && at.getTokens().hasNext() ?
                ENHANCE_ASYNC : CANNOT_ENHANCE;
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        log.info(" enhance ci {}",ci.getUri());
        if(isOfflineMode() && !entitySearcher.supportsOfflineMode()){
            throw new EngineException(this,ci,"Offline mode is not supported by the used EntitySearcher!",null);
        }
        AnalysedText at = getAnalysedText(this, ci, true);
        log.info("  > AnalysedText {}",at);
        String language = getLanguage(this, ci, true);
        if(log.isDebugEnabled()){
            log.debug("computeEnhancements for ContentItem {} language {} text={}", 
                new Object []{ci.getUri().getUnicodeString(), language, StringUtils.abbreviate(at.getSpan(), 100)});
        }
        log.info("  > Language {}",language);
        TextProcessingConfig tpc = textProcessingConfigs.get(language);
        if(tpc == null){
            tpc = defaultTextProcessingConfig;
            log.info("    ... with default TextProcessingConfig");
        } else {
            log.info("    ... with language specific TextProcessingConfig");
        }
        EntityLinker entityLinker = new EntityLinker(at,language, 
            defaultTextProcessingConfig, entitySearcher, linkerConfig, labelTokenizer);
        //process
        entityLinker.process();
        //write results (requires a write lock)
        ci.getLock().writeLock().lock();
        try {
            writeEnhancements(ci, entityLinker.getLinkedEntities().values(), language);
        } finally {
            ci.getLock().writeLock().unlock();
        }
    }

    /**
     * Writes the Enhancements for the {@link LinkedEntity LinkedEntities}
     * extracted from the parsed ContentItem
     * @param ci
     * @param linkedEntities
     * @param language
     */
    private void writeEnhancements(ContentItem ci, Collection<LinkedEntity> linkedEntities, String language) {
        Language languageObject = null;
        if(language != null && !language.isEmpty()){
            languageObject = new Language(language);
        }
        MGraph metadata = ci.getMetadata();
        for(LinkedEntity linkedEntity : linkedEntities){
            Collection<UriRef> textAnnotations = new ArrayList<UriRef>(linkedEntity.getOccurrences().size());
            //first create the TextAnnotations for the Occurrences
            for(Occurrence occurrence : linkedEntity.getOccurrences()){
                UriRef textAnnotation = EnhancementEngineHelper.createTextEnhancement(ci, this);
                textAnnotations.add(textAnnotation);
                metadata.add(new TripleImpl(textAnnotation, 
                    Properties.ENHANCER_START, 
                    literalFactory.createTypedLiteral(occurrence.getStart())));
                metadata.add(new TripleImpl(textAnnotation, 
                    Properties.ENHANCER_END, 
                    literalFactory.createTypedLiteral(occurrence.getEnd())));
                metadata.add(new TripleImpl(textAnnotation, 
                    Properties.ENHANCER_SELECTION_CONTEXT, 
                    new PlainLiteralImpl(occurrence.getContext(),languageObject)));
                metadata.add(new TripleImpl(textAnnotation, 
                    Properties.ENHANCER_SELECTED_TEXT, 
                    new PlainLiteralImpl(occurrence.getSelectedText(),languageObject)));
                metadata.add(new TripleImpl(textAnnotation, 
                    Properties.ENHANCER_CONFIDENCE, 
                    literalFactory.createTypedLiteral(linkedEntity.getScore())));
                for(UriRef dcType : linkedEntity.getTypes()){
                    metadata.add(new TripleImpl(
                        textAnnotation, Properties.DC_TYPE, dcType));
                }
            }
            //now the EntityAnnotations for the Suggestions
            for(Suggestion suggestion : linkedEntity.getSuggestions()){
                UriRef entityAnnotation = EnhancementEngineHelper.createEntityEnhancement(ci, this);
                //should we use the label used for the match, or search the
                //representation for the best label ... currently its the matched one
                Text label = suggestion.getBestLabel(linkerConfig.getNameField(),language);
                metadata.add(new TripleImpl(entityAnnotation, 
                    Properties.ENHANCER_ENTITY_LABEL, 
                    label.getLanguage() == null ?
                            new PlainLiteralImpl(label.getText()) :
                                new PlainLiteralImpl(label.getText(),
                                    new Language(label.getLanguage()))));
                metadata.add(new TripleImpl(entityAnnotation, 
                    Properties.ENHANCER_ENTITY_REFERENCE, 
                    new UriRef(suggestion.getRepresentation().getId())));
                Iterator<Reference> suggestionTypes = suggestion.getRepresentation().getReferences(linkerConfig.getTypeField());
                while(suggestionTypes.hasNext()){
                    metadata.add(new TripleImpl(entityAnnotation, 
                        Properties.ENHANCER_ENTITY_TYPE, new UriRef(suggestionTypes.next().getReference())));
                }
                metadata.add(new TripleImpl(entityAnnotation,
                    Properties.ENHANCER_CONFIDENCE, literalFactory.createTypedLiteral(suggestion.getScore())));
                for(UriRef textAnnotation : textAnnotations){
                    metadata.add(new TripleImpl(entityAnnotation, 
                        Properties.DC_RELATION, textAnnotation));
                }
                //add the name of the ReferencedSite providing this suggestion
                metadata.add(new TripleImpl(entityAnnotation, 
                    new UriRef(RdfResourceEnum.site.getUri()), 
                    new PlainLiteralImpl(referencedSiteName)));
                //in case dereferencing of Entities is enabled we need also to
                //add the RDF data for entities
                if(dereferenceEntitiesState){
                    metadata.addAll(
                        RdfValueFactory.getInstance().toRdfRepresentation(
                            suggestion.getRepresentation()).getRdfGraph());
                }
            }
        }
    }

    
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     * Methods for activate() and deactivate() the properties configureable via
     * OSGI.
     * 
     * NOTEs:
     * Directly calling super.activate and super.deactivate
     * is possible but might not be applicable in all cases.
     * The activate**(...) and deactivate**() Methods are intended to be
     * called by subclasses that need more control over the initialisation
     * process.
     * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     */
    /**
     * Activates this Engine. Subclasses should not call this method but rather
     * call<ul>
     * <li> {@link #activateEntitySearcher(ComponentContext, Dictionary)}
     * <li> {@link #initEntityLinkerConfig(Dictionary, EntityLinkerConfig)} and
     * <li> {@link #activateTextProcessingConfig(Dictionary)}
     * <li> {@link #dereferenceEntitiesState} (needs to be called after 
     * {@link #initEntityLinkerConfig(Dictionary, EntityLinkerConfig)})
     * </ul>
     * if applicable.
     * @param context the Component context
     * @throws ConfigurationException if the required {@link #REFERENCED_SITE_ID}
     * configuration is missing or any of the other properties has an illegal value
     */
    @Activate
    @SuppressWarnings("unchecked")
    protected void activate(ComponentContext context) throws ConfigurationException {
        super.activate(context);
        Dictionary<String,Object> properties = context.getProperties();
        activateTextProcessingConfig(properties);
        activateEntitySearcher(context, properties);
        activateEntityLinkerConfig(properties);
        activateEntityDereference(properties);
    }

    /**
     * Inits the {@link #dereferenceEntitiesState} based on the
     * {@link #DEREFERENCE_ENTITIES} configuration.
     * @param properties the configuration
     */
    protected final void activateEntityDereference(Dictionary<String,Object> properties) throws ConfigurationException {
        Object value = properties.get(DEREFERENCE_ENTITIES);
        if(value instanceof Boolean){
            dereferenceEntitiesState = ((Boolean)value).booleanValue();
        } else if(value != null && !value.toString().isEmpty()){
            dereferenceEntitiesState = Boolean.parseBoolean(value.toString());
        } else {
            dereferenceEntitiesState = DEFAULT_DEREFERENCE_ENTITIES_STATE;
        }
        if(dereferenceEntitiesState){
            value = properties.get(DEREFERENCE_ENTITIES_FIELDS);
            if(value instanceof String[]){
                for(String field : (String[])value){
                    if(field != null && !field.isEmpty()){
                        linkerConfig.getSelectedFields().add(field);
                    }
                }
            } else if(value instanceof Collection<?>){
                for(Object field : (Collection<?>)value){
                    if(field != null && !field.toString().isEmpty()){
                        linkerConfig.getSelectedFields().add(field.toString());
                    }
                }
            } else if(value instanceof String){
                if(!value.toString().isEmpty()){
                    linkerConfig.getSelectedFields().add(value.toString());
                }
            } else if(value != null){
                throw new ConfigurationException(DEREFERENCE_ENTITIES_FIELDS, 
                    "Dereference Entities_Fields MUST BE parsed as String[], Collection<String> or "
                    + "String (single value). The actual value '"+value+"'(type: '"+value.getClass() 
                    + "') is NOT supported");
            } else { //value == null -> add the default fields
                linkerConfig.getSelectedFields().addAll(DEREFERENCE_FIELDS);
            }
        }    }

    /**
     * Initialise the {@link TextAnalyzer} component.<p>
     * Currently this includes the following configurations: <ul>
     * <li>{@link #PROCESSED_LANGUAGES}: If no configuration is present the
     * default (process all languages) is used.
     * <li> {@value #MIN_POS_TAG_PROBABILITY}: If no configuration is
     * present the #DEFAULT_MIN_POS_TAG_PROBABILITY is used
     * languages based on the value of the
     * 
     * @param configuration the OSGI component configuration
     */
    protected final void activateTextProcessingConfig(Dictionary<String,Object> configuration) throws ConfigurationException {
        //Parse the default text processing configuration
        defaultTextProcessingConfig = new TextProcessingConfig();
        Object value = configuration.get(MIN_POS_TAG_PROBABILITY);
        double minPosTagProb;
        if(value instanceof Number){
            minPosTagProb = ((Number)value).doubleValue();
        } else if(value != null && !value.toString().isEmpty()){
            try {
                minPosTagProb = Double.valueOf(value.toString());
            } catch (NumberFormatException e) {
                throw new ConfigurationException(MIN_POS_TAG_PROBABILITY, 
                    "Unable to parse the min POS tag probability from the parsed value "+value,e);
            }
        } else {
            minPosTagProb = DEFAULT_MIN_POS_TAG_PROBABILITY;
        }
        if(minPosTagProb > 1){
            throw new ConfigurationException(MIN_POS_TAG_PROBABILITY, 
                "The configured min POS tag probability MUST BE in the range [0..1] " +
                "or < 0 to deactivate this feature (parsed value "+value+")!");
        }
        defaultTextProcessingConfig.setMinPosAnnotationProbability(minPosTagProb);
        defaultTextProcessingConfig.setMinExcludePosAnnotationProbability(minPosTagProb/2d);
        //set the default LexicalTypes
        value = configuration.get(PROCESS_ONLY_PROPER_NOUNS_STATE);
        boolean properNounState;
        if(value instanceof Boolean){
            properNounState = ((Boolean)value).booleanValue();
        } else if (value != null){
            properNounState = Boolean.parseBoolean(value.toString());
        } else {
            properNounState = DEFAULT_PROCESS_ONLY_PROPER_NOUNS_STATE;
        }
        if(properNounState){
            defaultTextProcessingConfig.setProcessedLexicalCategories(Collections.EMPTY_SET);
            defaultTextProcessingConfig.setProcessedPos(DEFAULT_PROCESSED_POS_TYPES);
            log.debug("> ProperNoun matching activated (matched Pos: {})",
                defaultTextProcessingConfig.getProcessedPos());
        } else {
            defaultTextProcessingConfig.setProcessedLexicalCategories(DEFAULT_PROCESSED_LEXICAL_CATEGORIES);
            defaultTextProcessingConfig.setProcessedPos(Collections.EMPTY_SET);
            log.debug("> Noun matching activated (matched LexicalCategories: {})",
                defaultTextProcessingConfig.getProcessedLexicalCategories());
        }
        //parse the language configuration
        value = configuration.get(PROCESSED_LANGUAGES);
        if(value instanceof String){
            throw new ConfigurationException(PROCESSED_LANGUAGES, "Unable to configure "
                + getClass().getSimpleName()+" '"+getName()+": 'Comma separated String "
                + "is not supported for configurung the processed languages for the because "
                + "the comma is used as separator for values of the parameters '"
                + PARAM_LEXICAL_CATEGORIES+"', '"+ PARAM_POS_TYPES+"'and'"+PARAM_POS_TAG
                + "! Users need to use String[] or Collection<?> instead!");
        }
        languages.setConfiguration(configuration);
        Map<String,String> defaultConfig = languages.getDefaultParameters();
        if(!defaultConfig.isEmpty()){
            applyLanguageParameter(defaultTextProcessingConfig,null,defaultConfig);
        }
        for(String lang : languages.getExplicitlyIncluded()){
            TextProcessingConfig tpc = defaultTextProcessingConfig.clone();
            applyLanguageParameter(tpc, lang, languages.getParameters(lang));
            this.textProcessingConfigs.put(lang, tpc);
        }
    }

    private void applyLanguageParameter(TextProcessingConfig tpc, String language, Map<String,String> config) throws ConfigurationException {
        Set<LexicalCategory> lexCats = parseEnumParam(config, PROCESSED_LANGUAGES, language, PARAM_LEXICAL_CATEGORIES, LexicalCategory.class);
        Set<Pos> pos = parseEnumParam(config, PROCESSED_LANGUAGES, language,PARAM_POS_TYPES, Pos.class);
        Set<String> tags = parsePosTags(config.get(PARAM_POS_TAG));
        Double prob = null;
        String paramVal = config.get(PARAM_POS_PROBABILITY);
        if(paramVal != null && !paramVal.trim().isEmpty()){
            try {
                prob = Double.parseDouble(paramVal.trim());
            } catch (NumberFormatException e) {
                throw new ConfigurationException(PROCESSED_LANGUAGES, "Unable to parse parameter '"
                    + PARAM_POS_PROBABILITY+"="+paramVal.trim()
                    + "' from the "+(language == null ? "default" : language)
                    + " language configuration", e);
            }
        }
        if(!lexCats.isEmpty() || !pos.isEmpty() || !tags.isEmpty()){
            log.info(" > use spefic language Configuration for language {}",
                getClass().getSimpleName(),getName());
            log.info("   - LexCat: {}",lexCats);
            log.info("   - Pos: {}",pos);
            log.info("   - Tags: {}",tags);
            tpc.setProcessedLexicalCategories(lexCats);
            tpc.setProcessedPos(pos);
            tpc.setProcessedPosTags(tags);
        }
        if(prob != null){
            tpc.setMinPosAnnotationProbability(prob);
            tpc.setMinExcludePosAnnotationProbability(prob/2d);
        }
    }
    private Set<String> parsePosTags(String value) {
        if(value == null || value.isEmpty()){
            return Collections.EMPTY_SET;
        } else {
            Set<String> tags = new HashSet<String>();
            for(String entry : value.split(",")){
                entry = entry.trim();
                if(!entry.isEmpty()){
                    tags.add(entry);
                }
            }
            return tags;
        }
    }

    /**
     * Utility to parse Enum members out of a comma separated string
     * @param config the config
     * @param property the property (only used for error handling)
     * @param param the key of the config used to obtain the config
     * @param enumClass the {@link Enum} class
     * @return the configured members of the Enum or an empty set if none 
     * @throws ConfigurationException if a configured value was not part of the enum
     */
    private <T extends Enum<T>> Set<T> parseEnumParam(Map<String,String> config,
        String property, String language, //params used for logging
        String param,Class<T> enumClass) throws ConfigurationException {
        Set<T> enumSet;
        String val = config.get(param);
        if(val == null){
            enumSet = Collections.emptySet();
        } else {
            enumSet = EnumSet.noneOf(enumClass);
            for(String entry : val.split(",")){
                entry = entry.trim();
                if(!entry.isEmpty()){
                    try {
                        enumSet.add(Enum.valueOf(enumClass,entry.toString()));
                    } catch (IllegalArgumentException e) {
                        throw new ConfigurationException(property, 
                            "'"+entry +"' of param '"+param+"' for language '"
                            + (language == null ? "default" : language)
                            + "'is not a member of the enum "+ enumClass.getSimpleName()
                            + "(configured : '"+val+"')!" ,e);
                    }
                }
            }
        }
        return enumSet;
    }
    
    /**
     * Configures the parsed {@link EntityLinkerConfig} with the values of the
     * following properties:<ul>
     * <li>{@link #NAME_FIELD}
     * <li>{@link #TYPE_FIELD}
     * <li>{@link #REDIRECT_FIELD}
     * <li>{@link #REDIRECT_PROCESSING_MODE}
     * <li>{@link #MAX_SUGGESTIONS}
     * <li>{@link #MIN_SEARCH_TOKEN_LENGTH}
     * <li>{@link #MIN_FOUND_TOKENS}
     * <li> {@link #MIN_TOKEN_MATCH_FACTOR}
     * </ul>
     * This Method create an new {@link EntityLinkerConfig} instance only if
     * <code>{@link #linkerConfig} == null</code>. If the instance is already initialised
     * that all current values for keys missing in the parsed configuration are
     * preserved.
     * @param configuration the configuration
     * @throws ConfigurationException In case of an illegal value in the parsed configuration.
     * Note that all configuration are assumed as optional, therefore missing values will not
     * case a ConfigurationException.
     */
    protected void activateEntityLinkerConfig(Dictionary<String,Object> configuration) throws ConfigurationException {
        if(linkerConfig == null){
            this.linkerConfig = new EntityLinkerConfig();
        }
        Object value;
        value = configuration.get(NAME_FIELD);
        if(value != null){
            if(value.toString().isEmpty()){
                throw new ConfigurationException(NAME_FIELD,"The configured name field MUST NOT be empty");
            }
            linkerConfig.setNameField(value.toString());
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
            linkerConfig.setTypeField(value.toString());
        }
        //init REDIRECT_FIELD
        value = configuration.get(REDIRECT_FIELD);
        if(value != null){
            if(value.toString().isEmpty()){
                throw new ConfigurationException(NAME_FIELD,"The configured name field MUST NOT be empty");
            }
            linkerConfig.setRedirectField(value.toString());
        }
        //init MAX_SUGGESTIONS
        value = configuration.get(MAX_SUGGESTIONS);
        Integer maxSuggestions;
        if(value instanceof Integer){
            maxSuggestions = (Integer)value;
        } else if (value != null){
            try {
                maxSuggestions = Integer.valueOf(value.toString());
            } catch(NumberFormatException e){
                throw new ConfigurationException(MAX_SUGGESTIONS, "Values MUST be valid Integer values > 0",e);
            }
        } else {
            maxSuggestions = null;
        }
        if(maxSuggestions != null){
            if(maxSuggestions < 1){
                throw new ConfigurationException(MAX_SUGGESTIONS, "Values MUST be valid Integer values > 0");
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
        value = configuration.get(REDIRECT_PROCESSING_MODE);
        if(value != null){
            try {
                linkerConfig.setRedirectProcessingMode(RedirectProcessingMode.valueOf(value.toString()));
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException(REDIRECT_PROCESSING_MODE, "Values MUST be one of "+
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
        value=configuration.get(MIN_TOKEN_MATCH_FACTOR);
        float minTokenMatchFactor;
        if(value instanceof Number){
            minTokenMatchFactor = ((Number)value).floatValue();
        } else if(value != null){
            try {
                minTokenMatchFactor = Float.valueOf(value.toString());
            } catch (NumberFormatException e) {
                throw new ConfigurationException(MIN_TOKEN_MATCH_FACTOR, 
                    "Unable to parse the minimum token match factor from the parsed value "+value,e);
            }
            if(minTokenMatchFactor < 0){
                minTokenMatchFactor = EntityLinkerConfig.DEFAULT_MIN_TOKEN_MATCH_FACTOR;
            }
        } else {
            minTokenMatchFactor = EntityLinkerConfig.DEFAULT_MIN_TOKEN_MATCH_FACTOR;
        }
        if(minTokenMatchFactor == 0 || minTokenMatchFactor > 1){
            throw new ConfigurationException(MIN_TOKEN_MATCH_FACTOR, 
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
                    targetType = getFullName(targetType.trim()); //support for ns:localName
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
                            sourceType = getFullName(sourceType.trim()); //support for ns:localName
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
    }

    /**
     * Initialise the {@link #entitySearcher} based on the value of the
     * {@link #REFERENCED_SITE_ID} property in the parsed configuration
     * @param context
     * @param configuration
     * @throws ConfigurationException
     */
    protected void activateEntitySearcher(ComponentContext context, Dictionary<String,Object> configuration) throws ConfigurationException {
        Object value = configuration.get(REFERENCED_SITE_ID);
        //init the EntitySource
        if (value == null) {
            throw new ConfigurationException(REFERENCED_SITE_ID,
                    "The ID of the Referenced Site is a required Parameter and MUST NOT be NULL!");
        }
        referencedSiteName = value.toString();
        if (referencedSiteName.isEmpty()) {
            throw new ConfigurationException(REFERENCED_SITE_ID,
                    "The ID of the Referenced Site is a required Parameter and MUST NOT be an empty String!");
        }
        //TODO: make limit configurable!
        if(Entityhub.ENTITYHUB_IDS.contains(referencedSiteName.toLowerCase())){
            entitySearcher = new EntityhubSearcher(context.getBundleContext(),DEFAULT_ENTITY_SEARCHER_LIMIT);
        } else {
            entitySearcher = new ReferencedSiteSearcher(context.getBundleContext(),referencedSiteName,DEFAULT_ENTITY_SEARCHER_LIMIT);
        }
    }
    /**
     * Deactivates this Engine. Subclasses should not call this method but rather
     * call<ul>
     * <li> {@link #deactivateEntitySearcher()}
     * <li> {@link #deactivateEntityLinkerConfig()} and
     * <li> {@link #deactivateProcessedLanguages())}
     * </ul>
     * @param context the context (not used)
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {
        super.deactivate(context);
        deactivateEntitySearcher();
        deactivateTextProcessingConfig();
        deactivateEntityLinkerConfig();
        deactivateEntityDereference();
    }
    /**
     * Resets the {@link #dereferenceEntitiesState} to 
     * {@link #DEFAULT_DEREFERENCE_ENTITIES_STATE}
     */
    protected final void deactivateEntityDereference() {
        dereferenceEntitiesState = DEFAULT_DEREFERENCE_ENTITIES_STATE;
    }

    /**
     * Deactivates the {@link TextAnalyzer} as well as resets the set of languages
     * to process to {@link #DEFAULT_LANGUAGES}
     */
    protected void deactivateTextProcessingConfig() {
        this.languages.setDefault(); //reset to the default
        this.textProcessingConfigs.clear();
    }

    /**
     * sets the {@link EntityLinkerConfig} to <code>null</code>
     */
    protected void deactivateEntityLinkerConfig() {
        linkerConfig = null;
    }

    /**
     * Closes and resets the EntitySearcher. Also calls
     * {@link TrackingEntitySearcher#close()} if applicable. 
     */
    protected void deactivateEntitySearcher() {
        if(entitySearcher instanceof TrackingEntitySearcher<?>){
            //close tracking EntitySearcher
            ((TrackingEntitySearcher<?>)entitySearcher).close();
        }
        entitySearcher = null;
        referencedSiteName = null;
    }
}

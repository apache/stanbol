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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
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
import org.apache.stanbol.commons.namespaceprefix.NamespaceMappingUtils;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.commons.opennlp.OpenNLP;
import org.apache.stanbol.commons.opennlp.TextAnalyzer;
import org.apache.stanbol.commons.opennlp.TextAnalyzer.TextAnalyzerConfig;
import org.apache.stanbol.commons.stanboltools.offline.OfflineMode;
import org.apache.stanbol.enhancer.engines.keywordextraction.impl.EntityLinker;
import org.apache.stanbol.enhancer.engines.keywordextraction.impl.EntityLinkerConfig;
import org.apache.stanbol.enhancer.engines.keywordextraction.impl.LinkedEntity;
import org.apache.stanbol.enhancer.engines.keywordextraction.impl.Suggestion;
import org.apache.stanbol.enhancer.engines.keywordextraction.impl.EntityLinkerConfig.RedirectProcessingMode;
import org.apache.stanbol.enhancer.engines.keywordextraction.impl.LinkedEntity.Occurrence;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.AnalysedContent;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.EntitySearcher;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.impl.EntityhubSearcher;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.impl.OpenNlpAnalysedContentFactory;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.impl.ReferencedSiteSearcher;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.impl.TrackingEntitySearcher;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
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
    @Property(name=KeywordLinkingEngine.NAME_FIELD,value="rdfs:label"),
    @Property(name=KeywordLinkingEngine.CASE_SENSITIVE,boolValue=EntityLinkerConfig.DEFAULT_CASE_SENSITIVE_MATCHING_STATE),
    @Property(name=KeywordLinkingEngine.TYPE_FIELD,value="rdf:type"),
    @Property(name=KeywordLinkingEngine.REDIRECT_FIELD,value="rdfs:seeAlso"),
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
    @Property(name=KeywordLinkingEngine.KEYWORD_TOKENIZER,boolValue=false),
    @Property(name=KeywordLinkingEngine.MAX_SUGGESTIONS,
        intValue=EntityLinkerConfig.DEFAULT_SUGGESTIONS),
    @Property(name=KeywordLinkingEngine.PROCESSED_LANGUAGES,value=""),
    @Property(name=KeywordLinkingEngine.DEFAULT_MATCHING_LANGUAGE,value=""),
    @Property(name=KeywordLinkingEngine.TYPE_MAPPINGS,cardinality=1000),
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
    public static final String MIN_SEARCH_TOKEN_LENGTH = "org.apache.stanbol.enhancer.engines.keywordextraction.minSearchTokenLength";
    public static final String MAX_SUGGESTIONS = "org.apache.stanbol.enhancer.engines.keywordextraction.maxSuggestions";
    public static final String PROCESSED_LANGUAGES = "org.apache.stanbol.enhancer.engines.keywordextraction.processedLanguages";
    public static final String MIN_FOUND_TOKENS= "org.apache.stanbol.enhancer.engines.keywordextraction.minFoundTokens";
    public static final String DEFAULT_MATCHING_LANGUAGE = "org.apache.stanbol.enhancer.engines.keywordextraction.defaultMatchingLanguage";
    public static final String MIN_POS_TAG_PROBABILITY = "org.apache.stanbol.enhancer.engines.keywordextraction.minPosTagProbability";
    public static final String TYPE_MAPPINGS = "org.apache.stanbol.enhancer.engines.keywordextraction.typeMappings";
    public static final String KEYWORD_TOKENIZER = "org.apache.stanbol.enhancer.engines.keywordextraction.keywordTokenizer";
    public static final String MIN_TOKEN_MATCH_FACTOR = "org.apache.stanbol.enhancer.engines.keywordextraction.minTokenMatchFactor";
//  public static final String ENABLE_CHUNKER = "org.apache.stanbol.enhancer.engines.keywordextraction.enableChunker";
    /**
     * Adds the dereference feature (STANBOL-333) also to this engine.
     * @deprecated Use a Dereference Engine instead (STANBOL-336)
     */
    public static final String DEREFERENCE_ENTITIES = "org.apache.stanbol.enhancer.engines.keywordextraction.dereference";
    /**
     * The default state to dereference entities set to <code>false</code> as
     * this is now a deprecated feature.
     * @deprecated Use a Dereference Engine instead (STANBOL-336)
     */
    public static final boolean DEFAULT_DEREFERENCE_ENTITIES_STATE = false;
    /**
     * Allows to add a list of fields that are included when dereferencing Entities
     * @deprecated Use a Dereference Engine instead (STANBOL-336)
     */
    public static final String DEREFERENCE_ENTITIES_FIELDS = "org.apache.stanbol.enhancer.engines.keywordextraction.dereferenceFields";
    /**
     * Additional fields added for dereferenced entities
     */
    private static final Collection<String> DEREFERENCE_FIELDS = Arrays.asList(
        "http://www.w3.org/2000/01/rdf-schema#comment",
        "http://www.w3.org/2003/01/geo/wgs84_pos#lat",
        "http://www.w3.org/2003/01/geo/wgs84_pos#long",
        "http://xmlns.com/foaf/0.1/depiction",
        "http://dbpedia.org/ontology/thumbnail");
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
    private Set<String> languages = DEFAULT_LANGUAGES;
    /**
     * The literal representing the LangIDEngine as creator.
     */
    public static final Literal LANG_ID_ENGINE_NAME = LiteralFactory.getInstance().createTypedLiteral("org.apache.stanbol.enhancer.engines.langid.LangIdEnhancementEngine");
    
    private EntitySearcher entitySearcher;
    private EntityLinkerConfig linkerConfig;
    private TextAnalyzerConfig nlpConfig;
    
    /**
     * The reference to the OpenNLP component
     */
    @org.apache.felix.scr.annotations.Reference
    private OpenNLP openNLP;
    
    @org.apache.felix.scr.annotations.Reference(cardinality=ReferenceCardinality.OPTIONAL_UNARY)
    protected NamespacePrefixService nsPrefixService;

    //TextAnalyzer was changed to have a scope of a single request ( call to
    //#computeEnhancement!
    //private TextAnalyzer textAnalyser;
    /**
     * Used to create {@link AnalysedContent} instances for parsed content items
     */
    private OpenNlpAnalysedContentFactory analysedContentFactory;
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
     * Internal Constructor used by {@link #createInstance(OpenNLP, EntitySearcher, EntityLinkerConfig)}
     * @param openNLP
     * @param entitySearcher
     * @param config
     */
    protected KeywordLinkingEngine(OpenNLP openNLP,EntitySearcher entitySearcher,
                                   TextAnalyzerConfig nlpConfig,EntityLinkerConfig linkingConfig){
        this.openNLP = openNLP;
        this.linkerConfig = linkingConfig != null ? linkingConfig : new EntityLinkerConfig();
        this.nlpConfig = nlpConfig != null ? nlpConfig : new TextAnalyzerConfig();
        this.analysedContentFactory = OpenNlpAnalysedContentFactory.getInstance(openNLP,nlpConfig);
        this.entitySearcher = entitySearcher;
    }
    /**
     * Allows to create an instance that can be used outside of an OSGI
     * environment. This is mainly intended for unit tests.
     * @param openNLP The {@link OpenNLP} instance used for natural language processing
     * @param entitySearcher the searcher used to lookup terms
     * @param config the configuration or <code>null</code> to use the defaults
     * @return the created engine instance
     */
    public static KeywordLinkingEngine createInstance(OpenNLP openNLP,
                                                      EntitySearcher entitySearcher,
                                                      TextAnalyzerConfig nlpConfig,
                                                      EntityLinkerConfig linkingConfig){
        return new KeywordLinkingEngine(openNLP,entitySearcher,nlpConfig,linkingConfig);
    }


    /**
     * Checks if the parsed language is enabled for processing.
     * @param language The language to process
     * @return the processing state for the parsed language.
     */
    protected boolean isProcessableLanguages(String language) {
        return languages.isEmpty() || languages.contains(language);
    }
    
    @Override
    public Map<String,Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(
            ENHANCEMENT_ENGINE_ORDERING,
            (Object) DEFAULT_ORDER));
    }

    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        if(ContentItemHelper.getBlob(ci, SUPPORTED_MIMETYPES) != null){
            return ENHANCE_ASYNC; //KeywordLinking now supports async processing
        } else {
            return CANNOT_ENHANCE;
        }
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        if(isOfflineMode() && !entitySearcher.supportsOfflineMode()){
            throw new EngineException("Offline mode is not supported by the Component used to lookup Entities");
        }
        Entry<IRI,Blob> contentPart = ContentItemHelper.getBlob(ci, SUPPORTED_MIMETYPES);
        if(contentPart == null){
            throw new IllegalStateException("No ContentPart with a supported Mime Type"
                    + "found for ContentItem "+ci.getUri()+"(supported: '"
                    + SUPPORTED_MIMETYPES+"') -> this indicates that canEnhance was" 
                    + "NOT called and indicates a bug in the used EnhancementJobManager!");
        }
        String text;
        try {
            text = ContentItemHelper.getText(contentPart.getValue());
        } catch (IOException e) {
            throw new InvalidContentException(String.format("Unable to extract "
                +" text from ContentPart %s of ContentItem %s!",
                contentPart.getKey(),ci.getUri()),e);
        }
        if (text.trim().length() == 0) {
            // TODO: make the length of the data a field of the ContentItem
            // interface to be able to filter out empty items in the canEnhance
            // method
            log.warn("ContentPart {} of ContentItem does not contain any Text to extract knowledge from",
                contentPart.getKey(), ci);
            return;
        }
        //Determine the language
        String language;
        ci.getLock().readLock().lock();
        try {
         language = extractLanguage(ci);
        } finally {
            ci.getLock().readLock().unlock();
        }
        if(isProcessableLanguages(language)){
            log.debug("computeEnhancements for ContentItem {} language {} text={}", 
                new Object []{ci.getUri().getUnicodeString(), language, StringUtils.abbreviate(text, 100)});
            
            EntityLinker entityLinker = new EntityLinker(
                analysedContentFactory.create(text, language),
                entitySearcher, linkerConfig);
            //process
            entityLinker.process();
            //write results (requires a write lock)
            ci.getLock().writeLock().lock();
            try {
                writeEnhancements(ci, entityLinker.getLinkedEntities().values(), language);
            } finally {
                ci.getLock().writeLock().unlock();
            }
        } else {
            log.debug("ignore ContentItem {} because language '{}' is not configured to" +
            		"be processed by this engine.",ci.getUri().getUnicodeString(),language);
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
        Graph metadata = ci.getMetadata();
        for(LinkedEntity linkedEntity : linkedEntities){
            Collection<IRI> textAnnotations = new ArrayList<IRI>(linkedEntity.getOccurrences().size());
            //first create the TextAnnotations for the Occurrences
            for(Occurrence occurrence : linkedEntity.getOccurrences()){
                IRI textAnnotation = EnhancementEngineHelper.createTextEnhancement(ci, this);
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
                for(IRI dcType : linkedEntity.getTypes()){
                    metadata.add(new TripleImpl(
                        textAnnotation, Properties.DC_TYPE, dcType));
                }
            }
            //now the EntityAnnotations for the Suggestions
            for(Suggestion suggestion : linkedEntity.getSuggestions()){
                IRI entityAnnotation = EnhancementEngineHelper.createEntityEnhancement(ci, this);
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
                    new IRI(suggestion.getRepresentation().getId())));
                Iterator<Reference> suggestionTypes = suggestion.getRepresentation().getReferences(linkerConfig.getTypeField());
                while(suggestionTypes.hasNext()){
                    metadata.add(new TripleImpl(entityAnnotation, 
                        Properties.ENHANCER_ENTITY_TYPE, new IRI(suggestionTypes.next().getReference())));
                }
                metadata.add(new TripleImpl(entityAnnotation,
                    Properties.ENHANCER_CONFIDENCE, literalFactory.createTypedLiteral(suggestion.getScore())));
                for(IRI textAnnotation : textAnnotations){
                    metadata.add(new TripleImpl(entityAnnotation, 
                        Properties.DC_RELATION, textAnnotation));
                }
                //add the name of the ReferencedSite providing this suggestion
                metadata.add(new TripleImpl(entityAnnotation, 
                    new IRI(RdfResourceEnum.site.getUri()), 
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
    /**
     * Extracts the language of the parsed ContentItem by using
     * {@link EnhancementEngineHelper#getLanguage(ContentItem)} and "en" as
     * default.
     * @param ci the content item
     * @return the language
     */
    private String extractLanguage(ContentItem ci) {
        String lang = EnhancementEngineHelper.getLanguage(ci);
//        if(lang != null){
//        Graph metadata = ci.getMetadata();
//        Iterator<Triple> langaugeEnhancementCreatorTriples = 
//            metadata.filter(null, Properties.DC_CREATOR, LANG_ID_ENGINE_NAME);
//        if(langaugeEnhancementCreatorTriples.hasNext()){
//            String lang = EnhancementEngineHelper.getString(metadata, 
//                langaugeEnhancementCreatorTriples.next().getSubject(), 
//                Properties.DC_LANGUAGE);
        if(lang != null){
            return lang;
        } else {
            log.warn("Unable to extract language for ContentItem %s! The Enhancement of the %s is missing the %s property",
                new Object[]{ci.getUri().getUnicodeString(),LANG_ID_ENGINE_NAME.getLexicalForm(),Properties.DC_LANGUAGE});
            log.warn(" ... return 'en' as default");
            return "en";
        }
//        } else {
//            log.warn("Unable to extract language for ContentItem %s! Is the %s active?",
//                ci.getUri().getUnicodeString(),LANG_ID_ENGINE_NAME.getLexicalForm());
//            log.warn(" ... return 'en' as default");
//            return "en";
//        }
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
     * <li> {@link #activateTextAnalyzerConfig(Dictionary)}
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
        activateTextAnalyzerConfig(properties);
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
            log.warn("DereferenceEntities is deprecated. Please use the Entityhub"
                + "DereferenceEngine instead (see STANBOL-1223 for details)");
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
    protected final void activateTextAnalyzerConfig(Dictionary<String,Object> configuration) throws ConfigurationException {
        nlpConfig = new TextAnalyzerConfig();
        Object value;
        value = configuration.get(PROCESSED_LANGUAGES);
        if(value == null){
            this.languages = DEFAULT_LANGUAGES;
        } else if (value.toString().trim().isEmpty()){
            this.languages = Collections.emptySet();
        } else {
            String[] languageArray = value.toString().split(",");
            languages = new HashSet<String>();
            for(String language : languageArray){
                if(language != null){
                    language = language.trim();
                    if(!language.isEmpty()){
                        languages.add(language);
                    }
                }
            }
        }
        value = configuration.get(MIN_POS_TAG_PROBABILITY);
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
        nlpConfig.setMinPosTagProbability(minPosTagProb);
        value = configuration.get(KEYWORD_TOKENIZER);
        //the keyword tokenizer config
        if(value instanceof Boolean){
            nlpConfig.forceKeywordTokenizer((Boolean)value);
        } else if(value != null && !value.toString().isEmpty()){
            nlpConfig.forceKeywordTokenizer(Boolean.valueOf(value.toString()));
        }
        //nlpConfig.enablePosTypeChunker(false);
        //nlpConfig.enableChunker(false);
        analysedContentFactory = OpenNlpAnalysedContentFactory.getInstance(openNLP,nlpConfig);
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
            
            linkerConfig.setNameField(NamespaceMappingUtils.getConfiguredUri(
                nsPrefixService, NAME_FIELD, value.toString()));
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
            linkerConfig.setTypeField(NamespaceMappingUtils.getConfiguredUri(
                nsPrefixService, TYPE_FIELD, value.toString()));
        }
        //init REDIRECT_FIELD
        value = configuration.get(REDIRECT_FIELD);
        if(value != null){
            if(value.toString().isEmpty()){
                throw new ConfigurationException(NAME_FIELD,"The configured name field MUST NOT be empty");
            }
            linkerConfig.setRedirectField(NamespaceMappingUtils.getConfiguredUri(
                nsPrefixService, REDIRECT_FIELD, value.toString()));
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
                    targetType = NamespaceMappingUtils.getConfiguredUri(
                        nsPrefixService,TYPE_MAPPINGS,targetType.trim()); //support for ns:localName
                    try { //validate
                        new URI(targetType);
                    } catch (URISyntaxException e) {
                        log.warn("Invalid URI '{}' in Type Mapping Config '{}' -> ignore this config",
                            sourceTypes[0],o);
                        continue configs;
                    }
                    IRI targetUri = new IRI(targetType);
                    for(String sourceType : sourceTypes){
                        if(!sourceType.isEmpty()){
                            sourceType = NamespaceMappingUtils.getConfiguredUri(
                                nsPrefixService,TYPE_MAPPINGS,sourceType.trim()); //support for ns:localName
                            try { //validate
                                new URI(sourceType);
                                IRI old = linkerConfig.setTypeMapping(sourceType, targetUri);
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
            entitySearcher = new EntityhubSearcher(context.getBundleContext(),10);
        } else {
            entitySearcher = new ReferencedSiteSearcher(context.getBundleContext(),referencedSiteName,10);
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
        deactivateTextAnalyzerConfig();
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
    protected void deactivateTextAnalyzerConfig() {
        this.nlpConfig = null;
        this.analysedContentFactory = null;
        languages = DEFAULT_LANGUAGES;
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

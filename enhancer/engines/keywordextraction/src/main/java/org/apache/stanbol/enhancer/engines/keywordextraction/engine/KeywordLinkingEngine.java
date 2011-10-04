package org.apache.stanbol.enhancer.engines.keywordextraction.engine;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.NIE_PLAINTEXTCONTENT;
import static org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum.getFullName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.commons.io.IOUtils;
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
import org.apache.stanbol.commons.opennlp.OpenNLP;
import org.apache.stanbol.commons.opennlp.TextAnalyzer;
import org.apache.stanbol.commons.stanboltools.offline.OfflineMode;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.AnalysedContent;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.EntityLinker;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.EntityLinkerConfig;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.EntitySearcher;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.LinkedEntity;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.Suggestion;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.EntityLinkerConfig.RedirectProcessingMode;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.LinkedEntity.Occurrence;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.impl.EntityhubSearcher;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.impl.OpenNlpAnalysedContentFactory;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.impl.ReferencedSiteSearcher;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.impl.TrackingEntitySearcher;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSite;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(configurationFactory = true, policy = ConfigurationPolicy.REQUIRE, // the baseUri is required!
    specVersion = "1.1", metatype = true, immediate = true)
@Service
@org.apache.felix.scr.annotations.Properties(value={
    @Property(name=KeywordLinkingEngine.REFERENCED_SITE_ID),
    @Property(name=KeywordLinkingEngine.NAME_FIELD,value=EntityLinkerConfig.DEFAULT_NAME_FIELD),
    @Property(name=KeywordLinkingEngine.TYPE_FIELD,value=EntityLinkerConfig.DEFAULT_TYPE_FIELD),
    @Property(name=KeywordLinkingEngine.REDIRECT_FIELD,value=EntityLinkerConfig.DEFAULT_REDIRECT_FIELD),
    //@Property(name=TaxonomyLinkingEngine2.SIMPLE_TOKENIZER,boolValue=true),
    //@Property(name=TaxonomyLinkingEngine2.ENABLE_CHUNKER,boolValue=false),
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
        },value="FOLLOW"),
    @Property(name=KeywordLinkingEngine.MIN_SEARCH_TOKEN_LENGTH,
        intValue=EntityLinkerConfig.DEFAULT_MIN_SEARCH_TOKEN_LENGTH),
    @Property(name=KeywordLinkingEngine.MAX_SUGGESTIONS,
        intValue=EntityLinkerConfig.DEFAULT_SUGGESTIONS),
    @Property(name=KeywordLinkingEngine.PROCESSED_LANGUAGES,value=""),
    @Property(name=KeywordLinkingEngine.DEFAULT_MATCHING_LANGUAGE,value=""),
    @Property(name=KeywordLinkingEngine.DEREFERENCE_ENTITIES,
        boolValue=KeywordLinkingEngine.DEFAULT_DEREFERENCE_ENTITIES_STATE)
})
public class KeywordLinkingEngine implements EnhancementEngine, ServiceProperties{

    private final Logger log = LoggerFactory.getLogger(KeywordLinkingEngine.class);
    /**
     * This is used to check the content type of parsed {@link ContentItem}s for
     * plain text
     */
    protected static final String TEXT_PLAIN_MIMETYPE = "text/plain";
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
    public static final String REDIRECT_FIELD = "org.apache.stanbol.enhancer.engines.keywordextraction.redirectField";
    public static final String REDIRECT_PROCESSING_MODE = "org.apache.stanbol.enhancer.engines.keywordextraction.redirectMode";
    public static final String MIN_SEARCH_TOKEN_LENGTH = "org.apache.stanbol.enhancer.engines.keywordextraction.minSearchTokenLength";
    public static final String MAX_SUGGESTIONS = "org.apache.stanbol.enhancer.engines.keywordextraction.maxSuggestions";
    public static final String PROCESSED_LANGUAGES = "org.apache.stanbol.enhancer.engines.keywordextraction.processedLanguages";
    public static final String MIN_FOUND_TOKENS= "org.apache.stanbol.enhancer.engines.keywordextraction.minFoundTokens";
    public static final String DEFAULT_MATCHING_LANGUAGE = "org.apache.stanbol.enhancer.engines.keywordextraction.defaultMatchingLanguage";
    public static final String MIN_POS_TAG_PROBABILITY = "org.apache.stanbol.enhancer.engines.keywordextraction.minPosTagProbability";
//  public static final String SIMPLE_TOKENIZER = "org.apache.stanbol.enhancer.engines.keywordextraction.simpleTokenizer";
//  public static final String ENABLE_CHUNKER = "org.apache.stanbol.enhancer.engines.keywordextraction.enableChunker";
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
    public static final double DEFAULT_MIN_POS_TAG_PROBABILITY = 0.8;
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
    private EntityLinkerConfig config;
    
    /**
     * The reference to the OpenNLP component
     */
    @org.apache.felix.scr.annotations.Reference
    private OpenNLP openNLP;
    /**
     * Used for natural language processing of parsed content
     */
    private TextAnalyzer textAnalyser;
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
                                     EntityLinkerConfig config){
        this.openNLP = openNLP;
        this.textAnalyser = new TextAnalyzer(openNLP);
        this.analysedContentFactory = OpenNlpAnalysedContentFactory.getInstance(textAnalyser);
        this.entitySearcher = entitySearcher;
        this.config = config != null ? config : new EntityLinkerConfig();
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
                                                        EntityLinkerConfig config){
        return new KeywordLinkingEngine(openNLP,entitySearcher,config);
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
        String mimeType = ci.getMimeType().split(";", 2)[0];
        if (TEXT_PLAIN_MIMETYPE.equalsIgnoreCase(mimeType)) {
            return ENHANCE_SYNCHRONOUS;
        }
        // check for existence of textual content in metadata
        UriRef subj = new UriRef(ci.getId());
        Iterator<Triple> it = ci.getMetadata().filter(subj, NIE_PLAINTEXTCONTENT, null);
        if (it.hasNext()) {
            return ENHANCE_SYNCHRONOUS;
        }
        return CANNOT_ENHANCE;
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        if(isOfflineMode() && !entitySearcher.supportsOfflineMode()){
            throw new EngineException("Offline mode is not supported by the Component used to lookup Entities");
        }
        String mimeType = ci.getMimeType().split(";", 2)[0];
        String text = extractText(ci, mimeType);
        if (text.trim().length() == 0) {
            // TODO: make the length of the data a field of the ContentItem
            // interface to be able to filter out empty items in the canEnhance
            // method
            log.warn("nothing to extract knowledge from in ContentItem {}", ci);
            return;
        }
        //Determine the language
        String language = extractLanguage(ci);
        if(isProcessableLanguages(language)){
            log.debug("computeEnhancements for ContentItem {} language {} text={}", 
                new Object []{ci.getId(), language, StringUtils.abbreviate(text, 100)});
            
            EntityLinker taxonomyLinker = new EntityLinker(
                analysedContentFactory.create(text, language),
                entitySearcher, config);
            //process
            taxonomyLinker.process();
            //write results
            writeEnhancements(ci, taxonomyLinker.getLinkedEntities().values(), language);
        } else {
            log.debug("ignore ContentItem {} because language '{}' is not configured to" +
            		"be processed by this engine.",ci.getId(),language);
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
                    literalFactory.createTypedLiteral(occurrence.getContext())));
                metadata.add(new TripleImpl(textAnnotation, 
                    Properties.ENHANCER_SELECTED_TEXT, 
                    literalFactory.createTypedLiteral(occurrence.getSelectedText())));
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
                Text label = suggestion.getBestLabel(config.getNameField(),language);
                metadata.add(new TripleImpl(entityAnnotation, 
                    Properties.ENHANCER_ENTITY_LABEL, 
                    label.getLanguage() == null ?
                            new PlainLiteralImpl(label.getText()) :
                                new PlainLiteralImpl(label.getText(),
                                    new Language(label.getLanguage()))));
                metadata.add(new TripleImpl(entityAnnotation, 
                    Properties.ENHANCER_ENTITY_REFERENCE, 
                    new UriRef(suggestion.getRepresentation().getId())));
                Iterator<Reference> suggestionTypes = suggestion.getRepresentation().getReferences(config.getTypeField());
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
     * Extracts the language of the parsed ContentItem from the metadata
     * @param ci the content item
     * @return the language
     */
    private String extractLanguage(ContentItem ci) {
        MGraph metadata = ci.getMetadata();
        Iterator<Triple> langaugeEnhancementCreatorTriples = 
            metadata.filter(null, Properties.DC_CREATOR, LANG_ID_ENGINE_NAME);
        if(langaugeEnhancementCreatorTriples.hasNext()){
            String lang = EnhancementEngineHelper.getString(metadata, 
                langaugeEnhancementCreatorTriples.next().getSubject(), 
                Properties.DC_LANGUAGE);
            if(lang != null){
                return lang;
            } else {
                log.warn("Unable to extract language for ContentItem %s! The Enhancement of the %s is missing the %s property",
                    new Object[]{ci.getId(),LANG_ID_ENGINE_NAME.getLexicalForm(),Properties.DC_LANGUAGE});
                log.warn(" ... return 'en' as default");
                return "en";
            }
        } else {
            log.warn("Unable to extract language for ContentItem %s! Is the %s active?",
                ci.getId(),LANG_ID_ENGINE_NAME.getLexicalForm());
            log.warn(" ... return 'en' as default");
            return "en";
        }
    }

    /**
     * Extracts the text from the parsed contentItem. In case the content type is
     * plain text, it directly reads the text from the stream. In other cases it
     * tries to read the string representation from the metadata by looking for
     * values of the {@link org.apache.stanbol.enhancer.servicesapi.rdf.Properties#NIE_PLAINTEXTCONTENT}
     * property.<p>
     * TODO: This is a Workaround for the currently not implemented Adapter
     * Pattern for the Stanbol Enhancer.
     * @param ci
     * @param mimeType
     * @return
     * @throws InvalidContentException
     */
    private String extractText(ContentItem ci, String mimeType) throws InvalidContentException {
        String text;
        if (TEXT_PLAIN_MIMETYPE.equals(mimeType)) {
            try {
                text = IOUtils.toString(ci.getStream(),"UTF-8");
            } catch (IOException e) {
                throw new InvalidContentException(this, ci, e);
            }
        } else {
            //TODO: change that as soon the Adapter Pattern is used for multiple
            // mimetype support.
            StringBuilder textBuilder = new StringBuilder();
            Iterator<Triple> it = ci.getMetadata().filter(new UriRef(ci.getId()), NIE_PLAINTEXTCONTENT, null);
            while (it.hasNext()) {
                textBuilder.append(it.next().getObject());
            }
            text = textBuilder.toString();
        }
        return text;
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
     * <li> {@link #activateTextAnalyzer(Dictionary)}
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
        Dictionary<String,Object> properties = context.getProperties();
        activateTextAnalyzer(properties);
        activateEntitySearcher(context, properties);
        activateEntityLinkerConfig(properties);
        activateEntityDereference(properties);
    }

    /**
     * Inits the {@link #dereferenceEntitiesState} based on the
     * {@link #DEREFERENCE_ENTITIES} configuration.
     * @param properties the configuration
     */
    protected final void activateEntityDereference(Dictionary<String,Object> properties) {
        Object value = properties.get(DEREFERENCE_ENTITIES);
        if(value instanceof Boolean){
            dereferenceEntitiesState = ((Boolean)value).booleanValue();
        } else if(value != null && !value.toString().isEmpty()){
            dereferenceEntitiesState = Boolean.parseBoolean(value.toString());
        } else {
            dereferenceEntitiesState = DEFAULT_DEREFERENCE_ENTITIES_STATE;
        }
        if(dereferenceEntitiesState){
            config.getSelectedFields().addAll(DEREFERENCE_FIELDS);
        }
    }

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
    protected final void activateTextAnalyzer(Dictionary<String,Object> configuration) throws ConfigurationException {
        textAnalyser = new TextAnalyzer(openNLP);
        analysedContentFactory = OpenNlpAnalysedContentFactory.getInstance(textAnalyser);
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
        textAnalyser.setMinPosTagProbability(minPosTagProb);
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
     * </ul>
     * This Method create an new {@link EntityLinkerConfig} instance only if
     * <code>{@link #config} == null</code>. If the instance is already initialised
     * that all current values for keys missing in the parsed configuration are
     * preserved.
     * @param configuration the configuration
     * @throws ConfigurationException In case of an illegal value in the parsed configuration.
     * Note that all configuration are assumed as optional, therefore missing values will not
     * case a ConfigurationException.
     */
    protected void activateEntityLinkerConfig(Dictionary<String,Object> configuration) throws ConfigurationException {
        if(config == null){
            this.config = new EntityLinkerConfig();
        }
        Object value;
        value = configuration.get(NAME_FIELD);
        if(value != null){
            if(value.toString().isEmpty()){
                throw new ConfigurationException(NAME_FIELD,"The configured name field MUST NOT be empty");
            }
            config.setNameField(value.toString());
        }
        //init TYPE_FIELD
        value = configuration.get(TYPE_FIELD);
        if(value != null){
            if(value.toString().isEmpty()){
                throw new ConfigurationException(TYPE_FIELD,"The configured name field MUST NOT be empty");
            }
            config.setTypeField(value.toString());
        }
        //init REDIRECT_FIELD
        value = configuration.get(REDIRECT_FIELD);
        if(value != null){
            if(value.toString().isEmpty()){
                throw new ConfigurationException(NAME_FIELD,"The configured name field MUST NOT be empty");
            }
            config.setRedirectField(value.toString());
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
            config.setMaxSuggestions(maxSuggestions);
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
            config.setMinFoundTokens(minFoundTokens);
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
            config.setMaxSuggestions(minSearchTokenLength);
        }
        //init the REDIRECT_PROCESSING_MODE
        value = configuration.get(REDIRECT_PROCESSING_MODE);
        if(value != null){
            try {
                config.setRedirectProcessingMode(RedirectProcessingMode.valueOf(value.toString()));
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
                config.setDefaultLanguage(null);
            } else if(defaultLang.length() == 1){
                throw new ConfigurationException(DEFAULT_MATCHING_LANGUAGE, "Illegal language code '"+
                    defaultLang+"'! Language Codes MUST BE at least 2 chars long.");
            } else {
                config.setDefaultLanguage(defaultLang);
            }
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
        String refSiteId = value.toString();
        if (refSiteId.isEmpty()) {
            throw new ConfigurationException(REFERENCED_SITE_ID,
                    "The ID of the Referenced Site is a required Parameter and MUST NOT be an empty String!");
        }
        if(Entityhub.ENTITYHUB_IDS.contains(refSiteId.toLowerCase())){
            entitySearcher = new EntityhubSearcher(context.getBundleContext());
        } else {
            entitySearcher = new ReferencedSiteSearcher(context.getBundleContext(),refSiteId);
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
        deactivateEntitySearcher();
        deactivateTextAnalyzer();
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
    protected void deactivateTextAnalyzer() {
        this.textAnalyser = null;
        this.analysedContentFactory = null;
        languages = DEFAULT_LANGUAGES;
    }

    /**
     * sets the {@link EntityLinkerConfig} to <code>null</code>
     */
    protected void deactivateEntityLinkerConfig() {
        config = null;
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
    }
}

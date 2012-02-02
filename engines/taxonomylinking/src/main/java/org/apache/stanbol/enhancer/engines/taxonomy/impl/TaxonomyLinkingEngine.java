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
package org.apache.stanbol.enhancer.engines.taxonomy.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import opennlp.tools.chunker.Chunker;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.Span;

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
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.opennlp.OpenNLP;
import org.apache.stanbol.commons.stanboltools.offline.OfflineMode;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.AbstractEnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.EntityhubException;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSite;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteManager;
//removed annotations until engine actually does something
//@Component(configurationFactory = true, policy = ConfigurationPolicy.REQUIRE, // the baseUri is required!
//    specVersion = "1.1", metatype = true, immediate = true)
//@Service
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the first try of an EnhancementEngine that finds concepts present within 
 * an taxonomy (controlled vocabulary) within content.<p>
 * Currently users should not use this engine but use the KeywordLinkingEngine
 * (org.apache.stanbol.enhancer.engine.keywordextraction bundle) instead.<p>
 * It is planed to re-introduce this engine with additional features specific to
 * taxonomies (such as support for concept hierarchies).
 * @deprecated
 * @author Rupert Westenthaler
 *
 */
@Component(configurationFactory = true, policy = ConfigurationPolicy.REQUIRE, // the baseUri is required!
    specVersion = "1.1", metatype = true, immediate = true, inherit = true)
@Service
@Property(name=EnhancementEngine.PROPERTY_NAME)
@Deprecated
public class TaxonomyLinkingEngine 
        extends AbstractEnhancementEngine<RuntimeException,RuntimeException> 
        implements EnhancementEngine, ServiceProperties {

    private static Logger log = LoggerFactory.getLogger(TaxonomyLinkingEngine.class);

    private static final boolean DEFAULT_SIMPLE_TOKENIZER_STATE = true;
    private static final int DEFAULT_MIN_SEARCH_TOKEN_LENGTH = 3;
    private static final boolean DEFAULT_USE_CHUNKER_STATE = false;
    private static final String DEFAULT_NAME_FIELD = "rdfs:label";
    /**
     * The default number for the maximum number of terms suggested for a word
     */
    private static final int DEFAULT_SUGGESTIONS = 3;
    /**
     * Default value for the number of tokens that must be contained in
     * suggested terms.
     */
    private static final int DEFAULT_MIN_FOUND_TOKENS = 2;
    @Property
    public static final String REFERENCED_SITE_ID = "org.apache.stanbol.enhancer.engines.taxonomy.referencedSiteId";
    @Property(value = DEFAULT_NAME_FIELD)
    public static final String NAME_FIELD = "org.apache.stanbol.enhancer.engines.taxonomy.nameField";
    @Property(boolValue=DEFAULT_SIMPLE_TOKENIZER_STATE)
    public static final String SIMPLE_TOKENIZER = "org.apache.stanbol.enhancer.engines.taxonomy.simpleTokenizer";
    @Property(intValue=DEFAULT_MIN_SEARCH_TOKEN_LENGTH)
    public static final String MIN_SEARCH_TOKEN_LENGTH = "org.apache.stanbol.enhancer.engines.taxonomy.minSearchTokenLength";
    @Property(boolValue=DEFAULT_USE_CHUNKER_STATE)
    public static final String ENABLE_CHUNKER = "org.apache.stanbol.enhancer.engines.taxonomy.enableChunker";
    @Property(intValue=DEFAULT_SUGGESTIONS)
    public static final String MAX_SUGGESTIONS = "org.apache.stanbol.enhancer.engines.taxonomy.maxSuggestions";
    @Property(intValue=DEFAULT_MIN_FOUND_TOKENS)
    public static final String MIN_FOUND_TOKENS= "org.apache.stanbol.enhancer.engines.taxonomy.minFoundTokens";
    @Property(intValue=0)
    public static final String SERVICE_RANKING = Constants.SERVICE_RANKING;
    
    protected static final String TEXT_PLAIN_MIMETYPE = "text/plain";
    /**
     * Contains the only supported mime type {@link #TEXT_PLAIN_MIMETYPE}
     */
    protected static final Set<String> SUPPORTED_MIMETYPES = Collections.singleton(TEXT_PLAIN_MIMETYPE);
    /**
     * The default value for the Execution of this Engine. Currently set to
     * {@link ServiceProperties#ORDERING_EXTRACTION_ENHANCEMENT} + 10. It should run after Metaxa and LangId.
     */
    public static final Integer defaultOrder = ServiceProperties.ORDERING_EXTRACTION_ENHANCEMENT + 10;

    public static final Map<String,UriRef> DEFAULT_ENTITY_TYPE_MAPPINGS;
    static { //the default mappings for the three types used by the Stanbol Enhancement Structure
        Map<String,UriRef> mappings = new HashMap<String,UriRef>();
        mappings.put(OntologicalClasses.DBPEDIA_ORGANISATION.getUnicodeString(), OntologicalClasses.DBPEDIA_ORGANISATION);
        mappings.put(NamespaceEnum.dbpediaOnt+"Newspaper", OntologicalClasses.DBPEDIA_ORGANISATION);
        mappings.put(NamespaceEnum.schema+"Organization", OntologicalClasses.DBPEDIA_ORGANISATION);
        
        mappings.put(OntologicalClasses.DBPEDIA_PERSON.getUnicodeString(), OntologicalClasses.DBPEDIA_PERSON);
        mappings.put(NamespaceEnum.foaf+"Person", OntologicalClasses.DBPEDIA_PERSON);
        mappings.put(NamespaceEnum.schema+"Person", OntologicalClasses.DBPEDIA_PERSON);

        mappings.put(OntologicalClasses.DBPEDIA_PLACE.getUnicodeString(), OntologicalClasses.DBPEDIA_PLACE);
        mappings.put(NamespaceEnum.schema+"Place", OntologicalClasses.DBPEDIA_PLACE);

        mappings.put(OntologicalClasses.SKOS_CONCEPT.getUnicodeString(), OntologicalClasses.SKOS_CONCEPT);
        DEFAULT_ENTITY_TYPE_MAPPINGS = Collections.unmodifiableMap(mappings);
    }
    
    @org.apache.felix.scr.annotations.Reference
    private OpenNLP openNLP;

    /**
     * Allow to force the use of the {@link SimpleTokenizer}
     */
    private boolean useSimpleTokenizer = DEFAULT_SIMPLE_TOKENIZER_STATE;

    /**
     * The minimum length of labels that are looked-up in the directory
     */
    private int minSearchTokenLength = DEFAULT_MIN_SEARCH_TOKEN_LENGTH;

    /**
     * Allows to activate/deactivate the use of an {@link Chunker}
     */
    private boolean useChunker = DEFAULT_USE_CHUNKER_STATE;

    /**
     * The field used to search for the names of entities part of the dictionary
     */
    private String nameField = NamespaceEnum.getFullName(DEFAULT_NAME_FIELD);
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
     * Service of the Entityhub that manages all the active referenced Site. This Service is used to lookup the
     * configured Referenced Site when we need to enhance a content item.
     */
    @org.apache.felix.scr.annotations.Reference
    protected ReferencedSiteManager siteManager;

    /**
     * Used to lookup Entities if the {@link #REFERENCED_SITE_ID} property is
     * set to "entityhub" or "local"
     */
    @org.apache.felix.scr.annotations.Reference
    protected Entityhub entityhub;
    
    /**
     * This holds the id of the {@link ReferencedSite} used to lookup Entities
     * or <code>null</code> if the {@link Entityhub} is used. 
     */
    protected String referencedSiteID;

    /**
     * Default constructor used by OSGI
     */
    public TaxonomyLinkingEngine(){}
    
    /**
     * The RDF LiteralFactory used for typed literals
     */
    private LiteralFactory literalFactory = LiteralFactory.getInstance();
    
    /**
     * Constructor used for unit tests outside of an OSGI environment
     * @param openNLP
     */
    protected TaxonomyLinkingEngine(OpenNLP openNLP){
        if(openNLP == null){
            throw new IllegalArgumentException("The parsed OpenNLP instance MUST NOT be NULL");
        }
        this.openNLP = openNLP;
    }
    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) throws ConfigurationException {
        super.activate(context);
        Dictionary<String,Object> config = context.getProperties();
        //lookup the referenced site used as dictionary
        Object referencedSiteID = config.get(REFERENCED_SITE_ID);
        if (referencedSiteID == null) {
            throw new ConfigurationException(REFERENCED_SITE_ID,
                    "The ID of the Referenced Site is a required Parameter and MUST NOT be NULL!");
        }

        this.referencedSiteID = referencedSiteID.toString();
        if (this.referencedSiteID.isEmpty()) {
            throw new ConfigurationException(REFERENCED_SITE_ID,
                    "The ID of the Referenced Site is a required Parameter and MUST NOT be an empty String!");
        }
        if(Entityhub.ENTITYHUB_IDS.contains(this.referencedSiteID.toLowerCase())){
            log.info("Init NamedEntityTaggingEngine instance for the Entityhub");
            this.referencedSiteID = null;
        }
        //parse the other configurations
        Object value = config.get(ENABLE_CHUNKER);
        if(value instanceof Boolean){
            useChunker = ((Boolean)value).booleanValue();
        } else if(value != null){
            useChunker = Boolean.parseBoolean(value.toString());
        }
        value = config.get(MIN_SEARCH_TOKEN_LENGTH);
        if(value instanceof Number){
            minSearchTokenLength = ((Number)value).intValue();
        } else if(value != null){
            try {
                minSearchTokenLength = Integer.parseInt(value.toString());
            }catch (NumberFormatException e) {
                log.warn("Unable to parse value for the minimum search token length." +
                		"Use the default value "+minSearchTokenLength,e);
            }
        }
        value = config.get(SIMPLE_TOKENIZER);
        if(value instanceof Boolean){
            useSimpleTokenizer = ((Boolean)value).booleanValue();
        } else if(value != null){
            useSimpleTokenizer = Boolean.parseBoolean(value.toString());
        }
        value = config.get(NAME_FIELD);
        if(value != null && !value.toString().isEmpty()){
            this.nameField = NamespaceEnum.getFullName(value.toString());
        }

    }
    @Deactivate
    protected void deactivate(ComponentContext context) {
        super.deactivate(context);
        referencedSiteID = null;
        //reset optional properties to the default
        nameField = DEFAULT_NAME_FIELD;
        useChunker = DEFAULT_USE_CHUNKER_STATE;
        minSearchTokenLength = DEFAULT_MIN_SEARCH_TOKEN_LENGTH;
        useSimpleTokenizer = DEFAULT_SIMPLE_TOKENIZER_STATE;
        minFoundTokens = DEFAULT_MIN_FOUND_TOKENS;
        maxSuggestions = DEFAULT_SUGGESTIONS;
    }
    
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

    private RedirectProcessingState redirectState = RedirectProcessingState.FOLLOW;

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
    
    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        if(ContentItemHelper.getBlob(ci, SUPPORTED_MIMETYPES) != null){
            return ENHANCE_SYNCHRONOUS;
        } else {
            return ENHANCE_ASYNC;
        }
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        final ReferencedSite site;
        if(referencedSiteID != null) { //lookup the referenced site
            site = siteManager.getReferencedSite(referencedSiteID);
            //ensure that it is present
            if (site == null) {
                String msg = String.format(
                    "Unable to enhance %s because Referenced Site %s is currently not active!", ci.getUri().getUnicodeString(),
                    referencedSiteID);
                log.warn(msg);
                // TODO: throwing Exceptions is currently deactivated. We need a more clear
                // policy what do to in such situations
                // throw new EngineException(msg);
                return;
            }
            //and that it supports offline mode if required
            if (isOfflineMode() && !site.supportsLocalMode()) {
                log.warn("Unable to enhance ci {} because OfflineMode is not supported by ReferencedSite {}.",
                    ci.getUri().getUnicodeString(), site.getId());
                return;
            }
        } else { // null indicates to use the Entityhub to lookup Entities
            site = null;
        }
        Entry<UriRef,Blob> contentPart = ContentItemHelper.getBlob(ci, SUPPORTED_MIMETYPES);
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
            throw new InvalidContentException(this, ci, e);
        }
        if (text.trim().length() == 0) {
            // TODO: make the length of the data a field of the ContentItem
            // interface to be able to filter out empty items in the canEnhance
            // method
            log.warn("ContentPart {} of ContentItem {} does not contain any text to extract knowledge from.",
                contentPart.getKey(),ci.getUri());
            return;
        }
        //TODO: determine the language
        String language = "en";
        log.debug("computeEnhancements for ContentPart {} of ContentItem {} language {} text={}", 
            new Object [] { contentPart.getKey(),ci.getUri().getUnicodeString(), 
                            language, StringUtils.abbreviate(text, 100) });
        
        //first get the models
        Tokenizer tokenizer = initTokenizer(language);
        SentenceDetector sentenceDetector = initSentence(language);
        POSTaggerME posTagger;
        if(sentenceDetector != null){ //sentence detection is requirement
            posTagger = initTagger(language);
        } else {
            posTagger = null;
        }
        ChunkerME chunker;
        if(posTagger != null && useChunker ){ //pos tags requirement
            chunker = initChunker(language);
        } else {
            chunker = null;
        }
        Map<String,Suggestion> suggestionCache = new TreeMap<String,Suggestion>();
        if(sentenceDetector != null){
            //add dots for multiple line breaks
            text = text.replaceAll("\\n\\n", ".\n");
            Span[] sentenceSpans = sentenceDetector.sentPosDetect(text);
            for (int i = 0; i < sentenceSpans.length; i++) {
                String sentence = sentenceSpans[i].getCoveredText(text).toString();
                Span[] tokenSpans = tokenizer.tokenizePos(sentence);
                String[] tokens = getTokensForSpans(sentence, tokenSpans);
                String[] pos;
                double[] posProbs;
                if(posTagger != null){
                    pos = posTagger.tag(tokens);
                    posProbs = posTagger.probs();
                } else {
                    pos = null;
                    posProbs = null;
                }
                Span[] chunkSpans;
                double[] chunkProps;
                if(chunker != null){
                    chunkSpans = chunker.chunkAsSpans(tokens, pos);
                    chunkProps = chunker.probs();
                } else {
                    chunkSpans = null;
                    chunkProps = null;
                }
                enhance(suggestionCache,site,ci,language, //the site, metadata and lang
                    sentenceSpans[i].getStart(),sentence, //offset and sentence
                    tokenSpans,tokens, //the tokens
                    pos,posProbs, // the pos tags (might be null)
                    chunkSpans,chunkProps); //the chunks (might be null)
            }
        } else {
            Span[] tokenSpans = tokenizer.tokenizePos(text);
            String[] tokens = getTokensForSpans(text, tokenSpans);
            enhance(suggestionCache,site,ci,language,0,text,tokenSpans,tokens,
                null,null,null,null);
        }
        //finally write the entity enhancements
        this.wirteEntityEnhancements(suggestionCache, ci, nameField,language);
    }

    /**
     * @param sentence
     * @param tokenSpans
     * @return
     */
    private String[] getTokensForSpans(String sentence, Span[] tokenSpans) {
        String[] tokens = new String[tokenSpans.length];
        for(int ti = 0; ti<tokenSpans.length;ti++) {
            tokens[ti] = tokenSpans[ti].getCoveredText(sentence).toString();
        }
        return tokens;
    }

    private void enhance(Map<String,Suggestion> suggestionCache,
                         ReferencedSite site,
                         ContentItem ci,
                         String language,
                         int offset,
                         String sentence,
                         Span[] tokenSpans,
                         String[] tokens,
                         String[] pos,
                         double[] posProbs,
                         Span[] chunkSpans,
                         double[] chunkProps) throws EngineException {
        //Iterate over tokens. Note that a single iteration may consume multiple
        //tokens in case a suggestion is found for a chunk.
        int consumed = -1;
        int chunkPos = 0;
        for(int currentToken = 0; currentToken < tokenSpans.length;currentToken++){
            Span current; //the current chunk to be processed
            //in case POS tags are available process only tokens with
            //specific types. If no POS tags are available process all tokens
            if(pos == null || includePOS(pos[currentToken])){
                //process this token
                if(chunkSpans != null && chunkPos < chunkSpans.length){
                    //consume unused chunks (chunks use token index as start/end)
                    for(;chunkSpans[chunkPos].getEnd() < currentToken;chunkPos++);
                    current = chunkSpans[chunkPos]; //use the current chunk
                    chunkPos++;
                } else if (pos != null){ //if no Chunker is used
                    //build chunks based on POS tags. For that we have a list
                    //of tags that are followed (backwards and forwards)
                    int start = currentToken;
                    while(start-1 > consumed && followPOS(pos[start-1])){
                        start--; //follow backwards until consumed
                    }
                    int end = currentToken;
                    while(end+1 < tokens.length && followPOS(pos[end+1])){
                        end++; //follow forwards until consumed
                    }
                    current = new Span(start,end);
                } else { //if no chunker and no POS tags just use the current token
                    current = new Span(currentToken,currentToken);
                }
            } else { //ignore tokens with POS tags that are not included
                current = null; 
            }
            if(current != null){
                consumed = currentToken; //set consumed to the current token
                //calculate the search string and search tokens for the currently
                //active chunk
                StringBuilder labelBuilder = null;
                boolean first = true;
                int startIndex = current.getStart();
                int endIndex = current.getEnd();
                //we need also the tokens to filter results that may be included
                //because of the use of Tokenizers, Stemmers ...
                List<String> searchTokens = new ArrayList<String>(current.length()+1);
                for(int j = current.getStart();j<=current.getEnd();j++){
                    if((pos == null && tokens[j].length() >= minSearchTokenLength) || 
                            (pos != null && includePOS(pos[j]))){
                        if(!first){
                            labelBuilder.append(' ');
                            endIndex = j; //update end
                        } else {
                            labelBuilder = new StringBuilder();
                            startIndex = j; //set start
                            endIndex = j; //set end
                        }
                        labelBuilder.append(tokens[j]);
                        searchTokens.add(tokens[j]);
                        first = false;
                    }
                }
                String searchString = labelBuilder != null ? labelBuilder.toString() : null;
                if(searchString != null && !suggestionCache.containsKey(searchString)){
                    Suggestion suggestion = suggestionCache.get(searchString);
                    if(suggestion != null){
                        //create TextAnnotation for this selection and add it to
                        //the suggestions.
                        suggestion.addLinked(createTextAnnotation(
                            offset, sentence, tokenSpans, ci, 
                            startIndex, endIndex, suggestion.getTextAnnotationTypes()));
                        log.debug("processed: Entity {} is now mentioned {} times",
                            searchString,suggestion.getLinkedTextAnnotations().size());
                    } else { //new word without an suggestion (suggestion == null)
                        List<Representation> suggestions = searchSuggestions(site, ci, searchTokens, searchString,
                            language, sentence, tokenSpans, offset, startIndex, endIndex);
                        if(!suggestions.isEmpty()){
                            //we need to parse the types to get the dc:type
                            //values for the TextAnnotations
                            Set<UriRef> textAnnotationTypes = new HashSet<UriRef>();
                            for(Representation rep : suggestions){
                                Iterator<Reference> types = rep.getReferences(NamespaceEnum.rdf+"type");
                                log.info(" > Entity {}"+rep.getId());
                                while(types.hasNext()){
                                    Reference type = types.next();
                                    log.info("  - type {}",type.toString());
                                    UriRef textAnnotationType = DEFAULT_ENTITY_TYPE_MAPPINGS.get(type.getReference());
                                    if(textAnnotationType != null){
                                        textAnnotationTypes.add(textAnnotationType);
                                    }
                                }
                            }
                            UriRef textAnnotation = createTextAnnotation(
                                offset, sentence, tokenSpans, ci, startIndex, endIndex,
                                textAnnotationTypes);
                            //create a new suggestion
                            suggestion = new Suggestion(
                                searchString, textAnnotation, suggestions, 
                                textAnnotationTypes);
                            //mark the current selection as "consumed"
                            consumed = current.getEnd(); 
                            //also set the current token to the last consumed
                            //to prevent processing of consumed tokens
                            currentToken = current.getEnd();
                            log.debug("processed: First mention of Entity {} ",searchString);
                        } else {
                            log.debug("processed: No suggestion for Entity {} ",searchString);
                            //will add NULL to the suggestion cache and therefore
                            //blacklist this "searchString"
                        }
                        //NULL values are added to blacklist "searchStrings"
                        suggestionCache.put(searchString, suggestion);
                    }
                } else if(log.isDebugEnabled()){ //ignore but do some debugging
                    if(searchString != null){
                        log.debug("ignore: {} already processed with no suggestions",searchString);
                    } else {
                        log.debug("ignore {}",
                            new Span(tokenSpans[current.getStart()].getStart(),
                                tokenSpans[current.getEnd()].getEnd()).getCoveredText(sentence));
                    }
                }
            } else {
                log.debug("ignore '{}'{}",tokens[currentToken],(pos!=null?'_'+pos[currentToken]:""));
            }
        }
    }
    private void wirteEntityEnhancements(Map<String,Suggestion> suggestionsCache,ContentItem ci,String nameField,String language){
        for(Suggestion suggestion : suggestionsCache.values()){
            if(suggestion != null){ //this map contains NULL values -> ignore them
                //create EntityAnnotations for all the suggested Representations
                Collection<UriRef> related;
                if(suggestion.getLinkedTextAnnotations().isEmpty()){
                    related = Collections.singleton((UriRef)suggestion.getTextAnnotation());
                } else {
                    related = new ArrayList<UriRef>(suggestion.getLinkedTextAnnotations().size()+1);
                    related.add(suggestion.getTextAnnotation());
                    related.addAll(suggestion.getLinkedTextAnnotations());
                }
                for(Representation rep : suggestion.getSuggestions()){
                    EnhancementRDFUtils.writeEntityAnnotation(
                        this, literalFactory, ci, related,rep, nameField, language);
                }
            }
        }
    }
    /**
     * Searches the {@link ReferencedSite} or the {@link Entityhub} (depending
     * on the configuration) for Entities corresponding to the search string.
     * Results are compaired to the search tokens (to avoid false positives
     * based on tokenizers and stemming).
     * @param site
     * @param ci
     * @param searchTokens
     * @param searchString
     * @param language
     * @param sentence
     * @param tokenSpans
     * @param offset
     * @param startIndex
     * @param endIndex
     * @param ciId
     * @return The Entities suggested for the parsed searchString. An empty list
     * indicates that no entities where found
     * @throws EngineException
     */
    private List<Representation> searchSuggestions(ReferencedSite site,
                                         ContentItem ci,
                                         List<String> searchTokens,
                                         String searchString,
                                         String language,
                                         String sentence,
                                         Span[] tokenSpans,
                                         int offset,
                                         int startIndex,
                                         int endIndex) throws EngineException {
        List<Representation> processedResults;
        FieldQuery query = site != null ? 
                site.getQueryFactory().createFieldQuery() :
                    entityhub.getQueryFactory().createFieldQuery();
        query.addSelectedField(nameField);
        query.addSelectedField(NamespaceEnum.rdfs+"comment");
        query.addSelectedField(NamespaceEnum.rdf+"type");
        query.addSelectedField(NamespaceEnum.rdfs+"seeAlso");
        query.setConstraint(nameField, new TextConstraint(searchString));//,language));
        //select 5 times the number of suggestion to allow some post
        //filtering
        //TODO: convert this to additional queries with offset
        query.setLimit(Integer.valueOf(maxSuggestions*5)); 
        QueryResultList<Representation> result;
        try {
            result = site != null ? site.find(query): entityhub.find(query);
        } catch (EntityhubException e) {
            throw new EngineException(this,ci,String.format(
                "Unable to search for Entity wiht label '%s@%s'",
                searchString,language),e);
        }
        if(!result.isEmpty()){
            processedResults = new ArrayList<Representation>(maxSuggestions);
            for(Iterator<Representation> it = result.iterator();it.hasNext() && processedResults.size()<maxSuggestions;){
                Representation rep = it.next();
                if(checkLabels(rep.getText(nameField),language,searchTokens)){
                    //based on the configuration we might need to do things for
                    //redirects (rdfs:seeAlso links)
                    rep = processRedirects(site, rep, query.getSelectedFields());
                    processedResults.add(rep);
                } //else ignore this result
            }
        } else {
            processedResults = Collections.emptyList();
        }
        return processedResults;
    }
    
    public static enum RedirectProcessingState {
        IGNORE,ADD_VALUES,FOLLOW
    }
    /**
     * @param site
     * @param rep
     * @param fields
     */
    private Representation processRedirects(ReferencedSite site, Representation rep, Collection<String> fields) {
        Iterator<Reference> redirects = rep.getReferences(NamespaceEnum.rdfs+"seeAlso");
        switch (redirectState == null ? RedirectProcessingState.IGNORE : redirectState) {
            case ADD_VALUES:
                while(redirects.hasNext()){
                    Reference redirect = redirects.next();
                    if(redirect != null){
                        try {
                            Entity redirectedEntity = site != null ? 
                                    site.getEntity(redirect.getReference()) : 
                                        entityhub.getEntity(redirect.getReference());
                            if(redirectedEntity != null){
                                for(String field: fields){
                                    rep.add(field, redirectedEntity.getRepresentation().get(field));
                                }
                            }
                        } catch (EntityhubException e) {
                            log.info(String.format("Unable to follow redirect to '%s' for Entity '%s'",
                                redirect.getReference(),rep.getId()),e);
                        }
                    }
                }
                return rep;
            case FOLLOW:
                while(redirects.hasNext()){
                    Reference redirect = redirects.next();
                    if(redirect != null){
                        try {
                            Entity redirectedEntity = site != null ? 
                                    site.getEntity(redirect.getReference()) : 
                                        entityhub.getEntity(redirect.getReference());
                            if(redirectedEntity != null){
                                return redirectedEntity.getRepresentation();
                            }
                        } catch (EntityhubException e) {
                            log.info(String.format("Unable to follow redirect to '%s' for Entity '%s'",
                                redirect.getReference(),rep.getId()),e);
                        }
                    }
                }
                return rep; //no redirect found
            default:
                return rep;
        }

    }
    /**
     * Checks if the labels of an Entity confirm to the searchTokens. Because of
     * Stemming an Tokenizers might be used for indexing the Dictionary this need
     * to be done on the client side.
     * @param labels the labels to check
     * @param language the language
     * @param searchTokens the required tokens
     * @return <code>true</code> if a label was acceptable or <code>false</code>
     * if no label was found
     */
    private boolean checkLabels(Iterator<Text> labels, String language, List<String> searchTokens) {
        while(labels.hasNext()){
            Text label = labels.next();
            //NOTE: I use here startWith language because I want 'en-GB' labels accepted for 'en'
            if(label.getLanguage() == null || label.getLanguage().startsWith(language)){
                String text = label.getText().toLowerCase();
                if(searchTokens.size() > 1){
                    int foundTokens = 0;
                    for(String token : searchTokens){
                        if(text.indexOf(token.toLowerCase())>=0){
                            foundTokens++;
                        }
                    }
                    if(foundTokens == searchTokens.size() || foundTokens >= minFoundTokens ){
                        return true;
                    }
                } else {
                    //for single searchToken queries there are often results with 
                    //multiple words. We need to filter those
                    //e.g. for persons only referenced by the given or family name
                    if(text.equalsIgnoreCase(searchTokens.get(0))){
                        return true;
                    }
                }
            }
        }
        return false;
    }
    /**
     * @param sentenceOffset
     * @param sentence
     * @param tokenSpans
     * @param metadata
     * @param contentItemId
     * @param startTokenIndex
     * @param endTokenIndex
     * @param dcTypes
     * @return
     */
    private UriRef createTextAnnotation(int sentenceOffset,
                                        String sentence,
                                        Span[] tokenSpans,
                                        ContentItem ci,
                                        int startTokenIndex,
                                        int endTokenIndex,
                                        Set<UriRef> dcTypes) {
        MGraph metadata = ci.getMetadata();
        UriRef contentItemId = ci.getUri();
        UriRef textAnnotation = EnhancementEngineHelper.createTextEnhancement(metadata, this, contentItemId);
        int startChar =  tokenSpans[startTokenIndex].getStart();
        int endChar = tokenSpans[endTokenIndex].getEnd();
        metadata.add(new TripleImpl(
            textAnnotation,
            Properties.ENHANCER_START,
            literalFactory.createTypedLiteral(sentenceOffset+startChar)));
        metadata.add(new TripleImpl(
            textAnnotation,
            Properties.ENHANCER_END,
            literalFactory.createTypedLiteral(sentenceOffset+endChar)));
        metadata.add(new TripleImpl(
            textAnnotation,
            Properties.ENHANCER_SELECTED_TEXT,
            new PlainLiteralImpl(new Span(startChar, endChar).getCoveredText(sentence).toString())));
        metadata.add(new TripleImpl(
            textAnnotation,
            Properties.ENHANCER_SELECTION_CONTEXT,
            new PlainLiteralImpl(sentence)));
        for(UriRef type : dcTypes){
            metadata.add(new TripleImpl(textAnnotation, Properties.DC_TYPE, type));
        }
        return textAnnotation;
    }

    /**
     * Set of POS tags used to build chunks of no {@link Chunker} is used.
     * NOTE that all tags starting with 'N' (Nouns) are included anyway
     */
    public static final Set<String> followPosSet = Collections.unmodifiableSet(
        new TreeSet<String>(Arrays.asList(
            "#","$"," ","(",")",",",".",":","``","POS","CD","IN","FW")));//,"''")));
    /**
     * Set of POS tags used for searches.
     * NOTE that all tags starting with 'N' (Nouns) are included anyway
     */
    public static final Set<String> searchPosSet = Collections.unmodifiableSet(
        new TreeSet<String>(Arrays.asList(
            "FW")));//,"''")));
    /**
     * TODO: This might be language specific!
     * @param pos
     * @return
     */
    private boolean followPOS(String pos){
        return pos.charAt(0) == 'N' || followPosSet.contains(pos);
    }
    private boolean includePOS(String pos){
        return pos.charAt(0) == 'N' || searchPosSet.contains(pos);
    }
    
    /**
     * @param language
     * @return
     */
    private Tokenizer initTokenizer(String language) {
        Tokenizer tokenizer;
        if(useSimpleTokenizer ){
            tokenizer = SimpleTokenizer.INSTANCE;
        } else {
            tokenizer = openNLP.getTokenizer(language);
        }
        return tokenizer;
    }

    /**
     * @param language
     * @return
     */
    private POSTaggerME initTagger(String language) {
        POSTaggerME posTagger;
        try {
            POSModel posModel = openNLP.getPartOfSpeachModel(language);
            if(posModel != null){
                posTagger = new POSTaggerME(posModel);
            } else {
                log.debug("No POS Model for language {}",language);
                posTagger = null;
            }
        } catch (IOException e) {
            log.info("Unable to load POS Model for language "+language,e);
            posTagger = null;
        }
        return posTagger;
    }
    /**
     * @param language
     * @return
     */
    private SentenceDetector initSentence(String language) {
        SentenceDetector sentDetect;
        try {
            SentenceModel sentModel = openNLP.getSentenceModel(language);
            if(sentModel != null){
                sentDetect = new SentenceDetectorME(sentModel);
            } else {
                log.debug("No Sentence Detection Model for language {}",language);
                sentDetect = null;
            }
        } catch (IOException e) {
            log.info("Unable to load Sentence Detection Model for language "+language,e);
            sentDetect = null;
        }
        return sentDetect;
    }
    /**
     * @param language
     */
    private ChunkerME initChunker(String language) {
        ChunkerME chunker;
        try {
            ChunkerModel chunkerModel = openNLP.getChunkerModel(language);
            if(chunkerModel != null){
                chunker = new ChunkerME(chunkerModel);
            } else {
                log.debug("No Chunker Model for language {}",language);
                chunker = null;
            }
        } catch (IOException e) {
            log.info("Unable to load Chunker Model for language "+language,e);
            chunker = null;
        }
        return chunker;
    }
        

    @Override
    public Map<String,Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(
            ENHANCEMENT_ENGINE_ORDERING,
            (Object) defaultOrder));
    }

}

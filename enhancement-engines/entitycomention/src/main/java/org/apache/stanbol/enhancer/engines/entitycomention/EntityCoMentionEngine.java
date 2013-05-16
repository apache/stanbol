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
package org.apache.stanbol.enhancer.engines.entitycomention;

import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.CASE_SENSITIVE;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.DEFAULT_CASE_SENSITIVE_MATCHING_STATE;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.DEFAULT_DEREFERENCE_ENTITIES_STATE;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.DEFAULT_MATCHING_LANGUAGE;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.DEFAULT_MIN_TOKEN_SCORE;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.DEFAULT_SUGGESTIONS;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.DEREFERENCE_ENTITIES;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.DEREFERENCE_ENTITIES_FIELDS;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.MIN_TOKEN_SCORE;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.NAME_FIELD;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.REDIRECT_FIELD;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.REDIRECT_MODE;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.SUGGESTIONS;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.TYPE_FIELD;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.TYPE_MAPPINGS;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig.DEFAULT_MIN_SEARCH_TOKEN_LENGTH;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig.DEFAULT_PROCESS_ONLY_PROPER_NOUNS_STATE;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig.MIN_SEARCH_TOKEN_LENGTH;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig.PROCESSED_LANGUAGES;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig.PROCESS_ONLY_PROPER_NOUNS_STATE;
import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.getAnalysedText;
import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.getLanguage;
import static org.apache.stanbol.enhancer.servicesapi.EnhancementEngine.PROPERTY_NAME;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_CONTRIBUTOR;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_RELATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_END;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_TEXTANNOTATION;
import static org.osgi.framework.Constants.SERVICE_RANKING;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.PlainLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
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
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.enhancer.engines.entitycomention.impl.ContentItemMentionBuilder;
import org.apache.stanbol.enhancer.engines.entitycomention.impl.InMemoryEntityIndex;
import org.apache.stanbol.enhancer.engines.entitylinking.Entity;
import org.apache.stanbol.enhancer.engines.entitylinking.EntitySearcher;
import org.apache.stanbol.enhancer.engines.entitylinking.EntitySearcherException;
import org.apache.stanbol.enhancer.engines.entitylinking.LabelTokenizer;
import org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.RedirectProcessingMode;
import org.apache.stanbol.enhancer.engines.entitylinking.config.LanguageProcessingConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.EntityLinker;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.LinkedEntity;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.Suggestion;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.LinkedEntity.Occurrence;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * The Entity Co-Mentiaon Engine builds a local knowledge base already extracted
 * <code>fise:TextAnnotation</code>s and suggested 
 * <code>fise:EntityAnnotation</code>s. This information are then used to perform
 * an entity linking process. By doing so this engine will be able to detect
 * Co-Mentions of Entities within the processed document. <p>
 * 
 * 
 * 
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
@org.apache.felix.scr.annotations.Properties(value={
    @Property(name=PROPERTY_NAME),
    @Property(name=CASE_SENSITIVE,boolValue=DEFAULT_CASE_SENSITIVE_MATCHING_STATE),
    @Property(name=MIN_SEARCH_TOKEN_LENGTH, intValue=DEFAULT_MIN_SEARCH_TOKEN_LENGTH),
    @Property(name=PROCESS_ONLY_PROPER_NOUNS_STATE, boolValue=DEFAULT_PROCESS_ONLY_PROPER_NOUNS_STATE),
    @Property(name=PROCESSED_LANGUAGES,
        cardinality=Integer.MAX_VALUE,
        value={"*;lmmtip;uc=LINK;prop=0.75;pprob=0.75", // link multiple matchable tokens in chunks; link upper case words
               "de;uc=MATCH", //in German all Nouns are upper case
               "es;lc=Noun", //the OpenNLP POS tagger for Spanish does not support ProperNouns
               "nl;lc=Noun"}), //same for Dutch 
    @Property(name=DEFAULT_MATCHING_LANGUAGE,value=""),
    @Property(name=TYPE_MAPPINGS,cardinality=Integer.MAX_VALUE, value={
        "dbp-ont:Organisation; dbp-ont:Newspaper; schema:Organization > dbp-ont:Organisation",
        "dbp-ont:Person; foaf:Person; schema:Person > dbp-ont:Person",
        "dbp-ont:Place; schema:Place > dbp-ont:Place",
        "dbp-ont:Work; schema:CreativeWork > dbp-ont:Work",
        "dbp-ont:Event; schema:Event > dbp-ont:Event",
        "schema:Product > schema:Product",
        "skos:Concept > skos:Concept"}),
    @Property(name=SERVICE_RANKING,intValue=0)
})
@Service(value=EnhancementEngine.class)
public class EntityCoMentionEngine extends AbstractEnhancementEngine<RuntimeException,RuntimeException> implements ServiceProperties {

    private static final Integer ENGINE_ORDERING = ServiceProperties.ORDERING_POST_PROCESSING - 90;
    private static final Map<String,Object> SERVICE_PROPERTIES = 
            Collections.unmodifiableMap(Collections.singletonMap(
                ServiceProperties.ENHANCEMENT_ENGINE_ORDERING, 
                (Object)ENGINE_ORDERING));


    private final Logger log = LoggerFactory.getLogger(EntityCoMentionEngine.class);

    private final LiteralFactory literalFactory = LiteralFactory.getInstance();
    
    @Reference
    protected NamespacePrefixService prefixService;
    
    @Reference 
    protected LabelTokenizer labelTokenizer; 

    private BundleContext bundleContext;
    /**
     * EntityLinking configuration used for Co-Mention extractions
     */
    private EntityLinkerConfig linkerConfig;
    /**
     * TextProcessingConfig used for Co-Mention extraction
     */
    private TextProcessingConfig textProcessingConfig;

    /**
     * Default constructor as used by OSGI. This expects that 
     * {@link #activate(ComponentContext)} is called before usage
     */
    public EntityCoMentionEngine() {
    }

    @Activate
    @SuppressWarnings("unchecked")
    protected void activate(ComponentContext ctx) throws ConfigurationException {
        super.activate(ctx);
        log.info("activate {}[name:{}]",getClass().getSimpleName(),getName());
        Dictionary<String,Object> properties = ctx.getProperties();
        bundleContext = ctx.getBundleContext();
        //extract TextProcessing and EnityLinking config from the provided properties
        textProcessingConfig = TextProcessingConfig.createInstance(properties);
        linkerConfig = EntityLinkerConfig.createInstance(properties,prefixService);
        //some of the confiugration is predefined
        linkerConfig.setNameField(CoMentionConstants.CO_MENTION_LABEL_FIELD);
        linkerConfig.setTypeField(CoMentionConstants.CO_MENTION_TYPE_FIELD);
        linkerConfig.setMaxSuggestions(5); //there should not be more as 5 suggestions
        linkerConfig.setMinFoundTokens(1); //a single token is enough
        linkerConfig.setMinLabelScore(0.24); //1/4 of the tokens
        linkerConfig.setMinMatchScore( //labelScore * token match factor
            linkerConfig.getMinLabelScore()*linkerConfig.getMinTokenMatchFactor());
        linkerConfig.setRedirectProcessingMode(RedirectProcessingMode.IGNORE);
        //get the metadata later set to the enhancement engine
    }
    /**
     * Deactivates this components. 
     */
    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        log.info("deactivate {}[name:{}]",getClass().getSimpleName(),getName());
        textProcessingConfig = null;
        linkerConfig = null;
        super.deactivate(ctx);
    }
    
    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        String language = getLanguage(this, ci, false);
        if(language == null || textProcessingConfig.getConfiguration(language) == null){
            log.debug("Engine {} ignores ContentItem {} becuase language {} is not condigured.",
                new Object[]{ getName(), ci.getUri(), language});
            return CANNOT_ENHANCE;
        }
        //we need a detected language, the AnalyzedText contentPart with Tokens.
        AnalysedText at = getAnalysedText(this, ci, false);
        return at != null && at.getTokens().hasNext() ?
                ENHANCE_ASYNC : CANNOT_ENHANCE;
    }
    
    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        AnalysedText at = getAnalysedText(this, ci, true);
        String language = getLanguage(this, ci, true);
        LanguageProcessingConfig languageConfig = textProcessingConfig.getConfiguration(language);
        if(languageConfig == null){
            throw new IllegalStateException("The language '"+language+"' is not configured "
                    + "to be processed by this Engine. As this is already checked within the "
                    + "canEnhance(..) method this may indicate an bug in the used "
                    + "EnhanceemntJobManager implementation!");
        }
        if(log.isDebugEnabled()){
            log.debug("compute co-mentions for ContentItem {} language {}  text={}", 
                new Object []{ci.getUri().getUnicodeString(), language, StringUtils.abbreviate(at.getSpan(), 100)});
        }
        //create the in-memory database for the mentioned Entities
        ContentItemMentionBuilder entityMentionIndex = new ContentItemMentionBuilder(ci, 
            labelTokenizer, language, linkerConfig.getDefaultLanguage());
        EntityLinker entityLinker = new EntityLinker(at,language, 
            languageConfig, entityMentionIndex, linkerConfig, labelTokenizer,entityMentionIndex);
        //process
        try {
            entityLinker.process();
        } catch (EntitySearcherException e) {
            log.error("Unable to link Entities with "+entityLinker,e);
            throw new EngineException(this, ci, "Unable to link Entities with "+entityLinker, e);
        }
        //TODO: write results
        ci.getLock().writeLock().lock();
        try {
            writeComentions(ci,entityLinker.getLinkedEntities().values(), language);
        } finally {
            ci.getLock().writeLock().unlock();
        }
        log.info("Found co-mentions:");
        for(LinkedEntity linkedEntity : entityLinker.getLinkedEntities().values()){
            log.info(" > {}",linkedEntity);
        }
    }

    private void writeComentions(ContentItem ci,Collection<LinkedEntity> comentions, String language) {
        Language languageObject = null;
        if(language != null && !language.isEmpty()){
            languageObject = new Language(language);
        }
        
        MGraph metadata = ci.getMetadata();
                
        for(LinkedEntity comention : comentions){
            //URIs of TextAnnotations for the initial mention of this co-mention
            Collection<UriRef> initialMentions = new ArrayList<UriRef>(comention.getOccurrences().size());
            for(Suggestion suggestion : comention.getSuggestions()){
                Entity entity = suggestion.getEntity();
                if(entity.getData().filter(entity.getUri(),RDF_TYPE,ENHANCER_TEXTANNOTATION).hasNext()){
                    //this is a textAnnotation
                    initialMentions.add(entity.getUri());
                } //else TODO support also Entities!!
            }
            //first create the TextAnnotations for the co-mention
            for(Occurrence occurrence : comention.getOccurrences()){
                Literal startLiteral = literalFactory.createTypedLiteral(occurrence.getStart());
                Literal endLiteral = literalFactory.createTypedLiteral(occurrence.getEnd());
                //search for existing text annotation
                boolean ignore = false;
                //search for textAnnotations with the same end
                UriRef textAnnotation = null;
                Iterator<Triple> it = metadata.filter(null, ENHANCER_START, startLiteral);
                while(it.hasNext()){
                    Triple t = it.next();
                    Integer end = EnhancementEngineHelper.get(metadata, t.getSubject(), ENHANCER_END, Integer.class, literalFactory);
                    if(end != null &&
                            metadata.filter(t.getSubject(), RDF_TYPE, ENHANCER_TEXTANNOTATION).hasNext()){
                        textAnnotation = (UriRef)t.getSubject();
                        if(end > occurrence.getEnd()){
                            // there is an other TextAnnotation selecting a bigger Span
                            //so we should ignore this Occurrence
                            ignore = true;
                        }
                    }
                }
                it = metadata.filter(null, ENHANCER_END, endLiteral);
                while(it.hasNext()){
                    Triple t = it.next();
                    Integer start = EnhancementEngineHelper.get(metadata, t.getSubject(), ENHANCER_START, Integer.class, literalFactory);
                    if(start != null &&
                            metadata.filter(t.getSubject(), RDF_TYPE, ENHANCER_TEXTANNOTATION).hasNext()){
                        textAnnotation = (UriRef)t.getSubject();
                        if(start < occurrence.getStart()){
                            // there is an other TextAnnotation selecting a bigger Span
                            //so we should ignore this Occurrence
                            ignore = true;
                        }
                    }
                }
                if(!ignore){
                    //collect confidence values of co-mentions
                    Double maxConfidence = null;
                    if(textAnnotation == null){ //not found ... create a new TextAnnotation for the co-mention
                        textAnnotation = EnhancementEngineHelper.createTextEnhancement(ci, this);
                        metadata.add(new TripleImpl(textAnnotation, 
                            Properties.ENHANCER_START, 
                            startLiteral));
                        metadata.add(new TripleImpl(textAnnotation, 
                            Properties.ENHANCER_END, 
                            endLiteral));
                        metadata.add(new TripleImpl(textAnnotation, 
                            Properties.ENHANCER_SELECTION_CONTEXT, 
                            new PlainLiteralImpl(occurrence.getContext(),languageObject)));
                        metadata.add(new TripleImpl(textAnnotation, 
                            Properties.ENHANCER_SELECTED_TEXT, 
                            new PlainLiteralImpl(occurrence.getSelectedText(),languageObject)));
                    } else { //if existing add this engine as contributor
                        metadata.add(new TripleImpl(textAnnotation, DC_CONTRIBUTOR, 
                            new PlainLiteralImpl(this.getClass().getName())));
                        //consider the confidence value of the existing TextAnnotation
                        maxConfidence = EnhancementEngineHelper.get(metadata, textAnnotation, 
                            ENHANCER_CONFIDENCE, Double.class, literalFactory);
                    }
                    //now process initial mention(s) for the co-mention
                    for(UriRef initialMention : initialMentions){
                        //link the co-mentation with the initial one
                        metadata.add(new TripleImpl(textAnnotation, DC_RELATION, initialMention));
                        //check confidence of the initial one
                        Double confidnece = EnhancementEngineHelper.get(metadata, initialMention, 
                            ENHANCER_CONFIDENCE, Double.class, literalFactory);
                        if(confidnece != null){
                            if(maxConfidence == null){
                                maxConfidence = confidnece;
                            } else if(maxConfidence.compareTo(confidnece) <= 0){
                                maxConfidence = confidnece;
                            }
                        }
                        //add suggestions of the initial mention
                        Set<Resource> values = new HashSet<Resource>();
                        for(Iterator<Triple> suggestions = metadata.filter(initialMention, DC_TYPE, null); suggestions.hasNext();){
                            values.add(suggestions.next().getObject());
                        }
                        for(Resource dcType : values){
                            metadata.add(new TripleImpl(textAnnotation, DC_TYPE, dcType));
                        }
                        values.clear();
                        //add the suggestions of the initial mention to this one
                        for(Iterator<Triple> suggestions = metadata.filter(null, DC_RELATION, initialMention); suggestions.hasNext();){
                            values.add(suggestions.next().getSubject());
                        }
                        for(Resource suggestion : values){
                            metadata.add(new TripleImpl((NonLiteral)suggestion, DC_RELATION, textAnnotation));
    
                        }
                    }
                    //TODO: support also Entities
                    if(maxConfidence != null){ //set the confidence value (if known)
                        EnhancementEngineHelper.set(metadata, textAnnotation, ENHANCER_CONFIDENCE, maxConfidence, literalFactory);
                    }
                } //else ignore this occurence
            }
        }
    }
    @Override
    public Map<String,Object> getServiceProperties() {
        return SERVICE_PROPERTIES;
    }
    
}

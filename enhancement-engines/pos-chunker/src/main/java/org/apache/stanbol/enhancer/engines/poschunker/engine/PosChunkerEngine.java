/*
 * Copyright (c) 2012 Sebastian Schaffert
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.stanbol.enhancer.engines.poschunker.engine;

import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.POS_ANNOTATION;
import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.PHRASE_ANNOTATION;
import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.getAnalysedText;
import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.getLanguage;
import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.isLangaugeConfigured;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.engines.poschunker.PhraseBuilder;
import org.apache.stanbol.enhancer.engines.poschunker.PhraseBuilder.ChunkFactory;
import org.apache.stanbol.enhancer.engines.poschunker.PhraseTypeDefinition;
import org.apache.stanbol.enhancer.nlp.NlpAnnotations;
import org.apache.stanbol.enhancer.nlp.NlpProcessingRole;
import org.apache.stanbol.enhancer.nlp.NlpServiceProperties;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Chunk;
import org.apache.stanbol.enhancer.nlp.model.Section;
import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.SpanTypeEnum;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.model.tag.TagSet;
import org.apache.stanbol.enhancer.nlp.phrase.PhraseTag;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.Pos;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
import org.apache.stanbol.enhancer.nlp.utils.LanguageConfiguration;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A noun phrase detector (chunker) for English and German language base on OpenNLP. Uses the following chunker
 * models for OpenNLP:
 * <ul>
 *     <li>English: http://opennlp.sourceforge.net/models-1.5/en-chunker.bin</li>
 *     <li>German: http://gromgull.net/blog/2010/01/noun-phrase-chunking-for-the-awful-german-language/</li>
 * </ul>
 * The noun phrase detector requires a {@link org.apache.stanbol.enhancer.engines.opennlp.pos.model.POSContentPart} to
 * be present in the content item and will extend each {@link org.apache.stanbol.enhancer.engines.opennlp.pos.model.POSSentence}
 * with an array of chunks.
 * 
 * @author Sebastian Schaffert
 */
@Component(immediate = true, metatype = true, 
    configurationFactory = true, //allow multiple instances to be configured
    policy = ConfigurationPolicy.OPTIONAL) //create the default instance with the default config
@Service
@Properties(value={
        @Property(name=EnhancementEngine.PROPERTY_NAME,value="pos-chunker"),
        @Property(name=PosChunkerEngine.CONFIG_LANGUAGES, 
        	cardinality=Integer.MAX_VALUE, value = {"*"}),
        @Property(name=PosChunkerEngine.MIN_POS_SCORE, 
            doubleValue=PosChunkerEngine.DEFAULT_MIN_POS_SCORE),
        @Property(name=PosChunkerEngine.NOUN_PHRASE_STATE, 
            boolValue=PosChunkerEngine.DEFAULT_NOUN_PHRASE_STATE),
        @Property(name=PosChunkerEngine.VERB_PHRASE_STATE, 
            boolValue=PosChunkerEngine.DEFAULT_VERB_PHRASE_STATE),
        @Property(name=Constants.SERVICE_RANKING,intValue=-100) //give the default instance a ranking < 0
})
public class PosChunkerEngine extends AbstractEnhancementEngine<RuntimeException,RuntimeException> implements ServiceProperties {

    private static final Map<String,Object> SERVICE_PROPERTIES;
    static {
        Map<String,Object> props = new HashMap<String,Object>();
        props.put(ServiceProperties.ENHANCEMENT_ENGINE_ORDERING, 
            ServiceProperties.ORDERING_NLP_CHUNK);
        props.put(NlpServiceProperties.ENHANCEMENT_ENGINE_NLP_ROLE, 
            NlpProcessingRole.Chunking);
        SERVICE_PROPERTIES = Collections.unmodifiableMap(props);
    }
    /**
     * Language configuration. Takes a list of ISO language codes of supported languages. Currently supported
     * are the languages given as default value.
     */
    public static final String CONFIG_LANGUAGES = "enhancer.engine.poschunker.languages";

    public static final String MIN_POS_SCORE = "enhancer.engine.poschunker.minPosScore";
    public static final double DEFAULT_MIN_POS_SCORE = 0.5;
    
    public static final String NOUN_PHRASE_STATE = "enhancer.engine.poschunker.nounPhrase";
    public static final boolean DEFAULT_NOUN_PHRASE_STATE = true;
    public static final String VERB_PHRASE_STATE = "enhancer.engine.poschunker.verbPhrase";
    public static final boolean DEFAULT_VERB_PHRASE_STATE = false;
    
    private static final PhraseTypeDefinition NOUN_PHRASE_TYPE;
    private static final PhraseTypeDefinition VERB_PHRASE_TYPE;

    //TODO: maybe move this to PhraseTypeDefinition
    //TODO: this might be language specific
    //TODO: make configurable
    static {
        PhraseTypeDefinition nounPD = new PhraseTypeDefinition(LexicalCategory.Noun);
        //NOTE: Pos.Acronym, Pos.Abbreviation, Pos.Foreign are also considered as
        //      nouns by this definition.
        nounPD.getRequiredType().addPosTags(Pos.Acronym, Pos.Abbreviation, Pos.Foreign);
        //start types noun (automatically included) pronoun or determiners, adjectives 
        nounPD.getStartType().addCategories(LexicalCategory.PronounOrDeterminer, LexicalCategory.Adjective);
        nounPD.getStartType().addPosTags(Pos.Acronym, Pos.Abbreviation, Pos.Foreign);
        //prefix types are the same as start types (e.g. "the nice trip")
        nounPD.getPrefixType().addCategories(LexicalCategory.PronounOrDeterminer, LexicalCategory.Adjective);
        nounPD.getPrefixType().addPosTags(Pos.Acronym, Pos.Abbreviation, Pos.Foreign);
        //continuation types are nouns and punctations. 
        //NOTE: Adverbs are excluded to avoid phrases like "the nice trip last week"
        nounPD.getContinuationType().addCategories(LexicalCategory.Punctuation);
        nounPD.getContinuationType().addPosTags(Pos.Acronym, Pos.Abbreviation, Pos.Foreign);
        //end types are the same as start terms
        nounPD.getEndType().addCategories(LexicalCategory.PronounOrDeterminer, LexicalCategory.Adjective);
        nounPD.getEndType().addPosTags(Pos.Acronym, Pos.Abbreviation, Pos.Foreign);
        //and required types do include a Noun (what is actually included by default)
        NOUN_PHRASE_TYPE = nounPD;

        PhraseTypeDefinition verbPD = new PhraseTypeDefinition(LexicalCategory.Verb);
        verbPD.getStartType().addCategories(LexicalCategory.Adverb);
        verbPD.getContinuationType().addCategories(LexicalCategory.Adverb,LexicalCategory.Punctuation);
        verbPD.getEndType().addCategories(LexicalCategory.Adverb);
        //and required types do include a Verbs (what is actually included by default)
        VERB_PHRASE_TYPE = verbPD;
    }
    
    private static Logger log = LoggerFactory.getLogger(PosChunkerEngine.class);

    private LanguageConfiguration languageConfiguration = new LanguageConfiguration(CONFIG_LANGUAGES, 
        new String []{"*"});
    

    private double minPosScore = -1;

    private List<PhraseTypeDefinition> phraseTypeDefinitions;
    
    /**
     * Indicate if this engine can enhance supplied ContentItem, and if it
     * suggests enhancing it synchronously or asynchronously. The
     * {@link org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager} can force sync/async mode if desired, it is
     * just a suggestion from the engine.
     * <p/>
     * Returns CANNOT_ENHANCE if the content item does not have a POSContentPart, the language of the content is not
     * available or no chunker for the language is available.
     *
     * @throws org.apache.stanbol.enhancer.servicesapi.EngineException
     *          if the introspecting process of the content item
     *          fails
     */
    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        if(phraseTypeDefinitions.isEmpty()){
            return CANNOT_ENHANCE; //Nothing to do
        }
        String language = getLanguage(this, ci,false);
        if(language == null){
            return CANNOT_ENHANCE;
        }
        if(!isLangaugeConfigured(this,languageConfiguration,language,false)){
           return CANNOT_ENHANCE; 
        }
        if(getAnalysedText(this,ci,false) == null) {
            return CANNOT_ENHANCE;
        }

        // default enhancement is synchronous enhancement
        return ENHANCE_ASYNC;

    }

    /**
     * Compute enhancements for supplied ContentItem. The results of the process
     * are expected to be stored in the metadata of the content item.
     * <p/>
     * The client (usually an {@link org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager}) should take care of
     * persistent storage of the enhanced {@link org.apache.stanbol.enhancer.servicesapi.ContentItem}.
     *
     * @throws org.apache.stanbol.enhancer.servicesapi.EngineException
     *          if the underlying process failed to work as
     *          expected
     */
    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        AnalysedText at = getAnalysedText(this, ci, true);
        String language = getLanguage(this, ci, true);
        isLangaugeConfigured(this, languageConfiguration, language, true);
        //init the PhraseBuilder
        ChunkFactory chunkFactory = new ChunkFactoryImpl(at, ci.getLock());
        List<PhraseBuilder> phraseBuilders = new ArrayList<PhraseBuilder>(phraseTypeDefinitions.size());
        for(PhraseTypeDefinition ptd : phraseTypeDefinitions){
            phraseBuilders.add(new PhraseBuilder(ptd, chunkFactory, minPosScore));
        }
        Iterator<? extends Section> sentences = at.getSentences();
        if(!sentences.hasNext()){ //no sentences ... iterate over the whole text
            sentences = Collections.singleton(at).iterator();
        }
        while(sentences.hasNext()){
            // (1) get Tokens and POS information for the sentence
            Section sentence = sentences.next();
            for(PhraseBuilder pb : phraseBuilders){
                pb.nextSection(sentence);
            }
            Iterator<Token> tokens = sentence.getTokens();
            while(tokens.hasNext()){
                Token token = tokens.next();
                for(PhraseBuilder pb : phraseBuilders){
                    pb.nextToken(token);
                }
            }
        }
        //signal the end of the document
        for(PhraseBuilder pb : phraseBuilders){
            pb.nextSection(null);
        }
//        if(log.isTraceEnabled()){
//            logChunks(at);
//        }
    }
    
    @Override
    public Map<String,Object> getServiceProperties() {
        return SERVICE_PROPERTIES;
    }

//logging is now done by the PhraseBuilder
//    private void logChunks(AnalysedText at){
//        Iterator<Span> it = at.getEnclosed(EnumSet.of(SpanTypeEnum.Sentence, SpanTypeEnum.Chunk));
//        while(it.hasNext()){
//            Span span = it.next();
//            if(span.getType() == SpanTypeEnum.Chunk){
//                Value<PhraseTag> phraseAnno = span.getAnnotation(PHRASE_ANNOTATION);
//                log.trace(" > {} Phrase: {} {}", new Object[]{
//                    phraseAnno != null ? phraseAnno.value().getTag() : "unknown",
//                    span, span.getSpan()});
//                log.trace("  Tokens: ");
//                int i = 1;
//                for(Iterator<Token> tokens = ((Chunk)span).getTokens(); tokens.hasNext();i++){
//                    Token token = tokens.next();
//                    log.trace("    {}. {}{}", new Object[]{i,token.getSpan(),
//                            token.getAnnotations(NlpAnnotations.POS_ANNOTATION)});
//                }
//            } else {
//                log.trace("--- {}",span);
//            }
//        }
//    }

    /**
     * Activate and read the properties. Configures and initialises a ChunkerHelper for each language configured in
     * CONFIG_LANGUAGES.
     *
     * @param ce the {@link org.osgi.service.component.ComponentContext}
     */
    @Activate
    protected void activate(ComponentContext ce) throws ConfigurationException {
        log.info("activating POS tagging engine");
        super.activate(ce);
        @SuppressWarnings("unchecked")
        Dictionary<String, Object> properties = ce.getProperties();
        
        //read the min chunk score
        Object value = properties.get(MIN_POS_SCORE);
        Double minPosScore;
        if(value instanceof Number){
            minPosScore = ((Number)value).doubleValue();
        } else if (value != null && !value.toString().isEmpty()){
            try {
                minPosScore = Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                throw new ConfigurationException(MIN_POS_SCORE, 
                    "The configured minumum chunk score MUST BE a floating point"
                    + "number in the range > 0 < 1.",e);
            }
        } else {
            minPosScore = null;
        }
        if(minPosScore != null && (minPosScore.doubleValue() >= 1d ||
                minPosScore.doubleValue() < 0d)){
            throw new ConfigurationException(MIN_POS_SCORE, 
                "The configured minumum chunk score '"+minPosScore+"' MUST BE a "
                + "floating point number in the range > 0 < 1.");
        } else if(minPosScore == null){
            this.minPosScore = DEFAULT_MIN_POS_SCORE; //set to default
        } else {
            this.minPosScore = minPosScore.doubleValue();
        }
        log.info(" > set minimum POS score to {} (Engine: {})",
            this.minPosScore, getName());
        
        //read the language configuration
        languageConfiguration.setConfiguration(properties);
        
        //configure the PhraseType definitions
        phraseTypeDefinitions = new ArrayList<PhraseTypeDefinition>(2);
        value = properties.get(NOUN_PHRASE_STATE);
        if((value != null && Boolean.parseBoolean(value.toString())) ||
                (value == null && DEFAULT_NOUN_PHRASE_STATE)){
            phraseTypeDefinitions.add(NOUN_PHRASE_TYPE);
        }
        value = properties.get(VERB_PHRASE_STATE);
        if((value != null && Boolean.parseBoolean(value.toString())) ||
                (value == null && DEFAULT_VERB_PHRASE_STATE)){
            phraseTypeDefinitions.add(VERB_PHRASE_TYPE);
        }
        
    }
    
    @Deactivate
    protected void deactivate(ComponentContext context){
        this.languageConfiguration.setDefault();
        this.minPosScore = -1;
        super.deactivate(context);
    }
   

}

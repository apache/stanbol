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

package org.apache.stanbol.enhancer.engines.sentiment.services;

import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.SENTIMENT_ANNOTATION;
import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.getAnalysedText;
import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.getLanguage;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.engines.sentiment.api.SentimentClassifier;
import org.apache.stanbol.enhancer.nlp.NlpAnnotations;
import org.apache.stanbol.enhancer.nlp.NlpProcessingRole;
import org.apache.stanbol.enhancer.nlp.NlpServiceProperties;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.annotation.Annotation;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
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
 * A Stanbol engine that associates sentiment values with the tokens created by the POS tagging engine.
 * Sentiment values are added to the POSContentPart of the content item and can by further analysed by other
 * engines, e.g. to compute sentiment values for the whole content item or in relation to certain nouns.
 * <p/>
 * The configuration allows specifying whether to analyse all words or only adjectives and nouns (a typical case).
 * <p/>
 * Currently, sentiment analysis is available for English and for German language. It uses the following word lists:
 * <ul>
 *     <li>English: SentiWordNet (http://wordnet.princeton.edu/), license allows commercial use</li>
 *     <li>German: SentiWS (http://wortschatz.informatik.uni-leipzig.de/download/), license does NOT allow commercial use</li>
 * </ul>
 * <p/>
 * Author: Sebastian Schaffert
 */
@Component(immediate = true, metatype = true, 
    configurationFactory = true, //allow multiple instances
    policy = ConfigurationPolicy.OPTIONAL) //create a default instance with the default configuration
@Service
@Properties(value={
        @Property(name= EnhancementEngine.PROPERTY_NAME,value="sentiment-wordclassifier"),
        @Property(name=SentimentEngine.CONFIG_LANGUAGES,value={SentimentEngine.DEFAULT_LANGUAGE_CONFIG}),
        @Property(name=SentimentEngine.CONFIG_ADJECTIVES,
            boolValue=SentimentEngine.DEFAULT_PROCESS_ADJECTIVES_ONLY),
        @Property(name=SentimentEngine.CONFIG_MIN_POS_CONFIDENCE,
            doubleValue = SentimentEngine.DEFAULT_MIN_POS_CONFIDNECE),
        @Property(name=Constants.SERVICE_RANKING,intValue=-100) //give the default instance a ranking < 0
})
public class SentimentEngine  extends AbstractEnhancementEngine<RuntimeException,RuntimeException> implements ServiceProperties {

    /**
     * Language configuration. Takes a list of ISO language codes of supported languages. Currently supported
     * are the languages given as default value.
     */
    public static final String CONFIG_LANGUAGES = "org.apache.stanbol.enhancer.sentiment.languages";

    /**
     * When set to true, only adjectives and nouns will be considered in sentiment analysis.
     */
    public static final String CONFIG_ADJECTIVES = "org.apache.stanbol.enhancer.sentiment.adjectives";
    /**
     * POS tags that are not selected by {@link SentimentClassifier#isAdjective(PosTag)}
     * or {@link SentimentClassifier#isNoun(PosTag)} are ignored if their confidence
     * is &gt= the configured values. If there are multiple POS tag suggestions, 
     * that Words that do have a suitable TAG are still considered if the
     * confidence of the fitting tag is &gt;= {min-pos-confidence}/2
     */
    public static final String CONFIG_MIN_POS_CONFIDENCE = "org.apache.stanbol.enhancer.sentiment.min-pos-confidence";

    boolean debugSentiments;
    
    public static final String DEFAULT_LANGUAGE_CONFIG = "*";
    private LanguageConfiguration langaugeConfig = 
            new LanguageConfiguration(CONFIG_LANGUAGES, new String[]{DEFAULT_LANGUAGE_CONFIG});

    /**
     * The minimum confidence of POS tags so that a token is NOT processed if
     * the {@link LexicalCategory} is NOT {@link LexicalCategory#Adjective} (or
     * {@link LexicalCategory#Noun Noun} if {@link #CONFIG_ADJECTIVES} is
     * deactivated) - default: 0.8<p>
     */
    public static final double DEFAULT_MIN_POS_CONFIDNECE = 0.8;

    public static final boolean DEFAULT_PROCESS_ADJECTIVES_ONLY = false;

    /**
     * Service Properties used by this Engine
     */
    private static final Map<String,Object> SERVICE_PROPERTIES;
    static {
        Map<String,Object> props = new HashMap<String,Object>();
        props.put(ServiceProperties.ENHANCEMENT_ENGINE_ORDERING, 
            ServiceProperties.ORDERING_NLP_POS - 1); //after POS tagging
        props.put(NlpServiceProperties.ENHANCEMENT_ENGINE_NLP_ROLE, 
            NlpProcessingRole.SentimentTagging);
        SERVICE_PROPERTIES = Collections.unmodifiableMap(props);
    }


    private static Logger log = LoggerFactory.getLogger(SentimentEngine.class);

    /**
     * {@link SentimentClassifier} are now OSGI services and injected via events
     * (calls to {@link #bindClassifier(SentimentClassifier)} and 
     * {@link #unbindClassifier(SentimentClassifier)}) as soon as they become
     * (un)available.
     */
    @Reference(referenceInterface=SentimentClassifier.class,
        cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
        bind="bindClassifier",
        unbind="unbindClassifier",
        policy=ReferencePolicy.DYNAMIC,
        strategy=ReferenceStrategy.EVENT)
    private Map<String,SentimentClassifier> classifiers = Collections.synchronizedMap(
        new HashMap<String,SentimentClassifier>());
    /** bind method for {@link #classifiers} */
    protected void bindClassifier(SentimentClassifier classifier){
        log.info("  ... bind Sentiment Classifier {} for language {}",
            classifier.getClass().getSimpleName(),classifier.getLanguage());
        synchronized (classifiers) {
            SentimentClassifier old = classifiers.put(classifier.getLanguage(), classifier);
            if(old != null){
                log.warn("Replaced Sentiment Classifier for language {} (old: {}, new: {}",
                    new Object[]{old.getLanguage(),old,classifier});
            }
        }
    }
    /** unbind method for {@link #classifiers} */
    protected void unbindClassifier(SentimentClassifier classifier){
        String lang = classifier.getLanguage();
        synchronized (classifiers) {
            SentimentClassifier current = classifiers.remove(lang);
            if(!classifier.equals(current) //the current is not the parsed one
                    && current != null){
                classifiers.put(lang,current); //re-add the value
            } else {
                log.info("  ... unbind Sentiment Classifier {} for language {}",
                    classifier.getClass().getSimpleName(),lang);
            }
        }
    }
    
    /**
     * The processed {@link LexicalCategory LexicalCategories}.
     */
    boolean adjectivesOnly = DEFAULT_PROCESS_ADJECTIVES_ONLY;
    
    /**
     * The minimum {@link PosTag} value {@link Value#probability() confidence}.<p>
     * This means that if the {@link Value#probability() confidence} of a
     * {@link NlpAnnotations#POS_ANNOTATION}s (returned by
     * {@link Token#getAnnotations(Annotation)}) is greater than 
     * {@link #minPOSConfidence} that the result of 
     * {@link SentimentClassifier#isAdjective(PosTag)} (and 
     * {@link SentimentClassifier#isNoun(PosTag)}  - if #CONFIG_ADJECTIVES is 
     * deactivated) is used to decide if a Token needs to be processed or not.
     * Otherwise further {@link NlpAnnotations#POS_ANNOTATION}s are analysed for
     * processable POS tags. Processable POS tags are accepted until
     * <code>{@link #minPOSConfidence}/2</code>.  
     */
    private double minPOSConfidence = DEFAULT_MIN_POS_CONFIDNECE;

    /**
     * Indicate if this engine can enhance supplied ContentItem, and if it
     * suggests enhancing it synchronously or asynchronously. The
     * {@link org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager} can force sync/async mode if desired, it is
     * just a suggestion from the engine.
     * <p/>
     * Returns {@link EnhancementEngine}#ENHANCE_ASYNC if <ul>
     * <li> the {@link AnalysedText} content part is present
     * <li> the language of the content is known
     * <li> the language is active based on the language configuration and
     * <li> a sentiment classifier is available for the language
     * </ul>
     *
     * @throws org.apache.stanbol.enhancer.servicesapi.EngineException
     *          if the introspecting process of the content item
     *          fails
     */
    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        if(getAnalysedText(this,ci, false) == null){
            return CANNOT_ENHANCE;
        }
        String language = getLanguage(this, ci,false);

        if(language == null) {
            return CANNOT_ENHANCE;
        }
        if(classifiers.containsKey(language)){
            return ENHANCE_ASYNC;
        } else {
            return CANNOT_ENHANCE;
        }
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
        AnalysedText analysedText = getAnalysedText(this,ci, true);
        String language = getLanguage(this, ci, true);
        SentimentClassifier classifier = classifiers.get(language);
        if(classifier == null){
            throw new IllegalStateException("Sentiment Classifier for language '"
                + language +"' not available. As this is also checked in "
                + " canEnhance this may indicate an Bug in the used "
                + "EnhancementJobManager!");
        }
        //TODO: locking for AnalysedText not yet defined
//        ci.getLock().writeLock().lock();
//        try {
        Iterator<Token> tokens = analysedText.getTokens();
        while(tokens.hasNext()){
            Token token = tokens.next();
            Set<LexicalCategory> cats = null;
            boolean process = false;
            if(!adjectivesOnly){
                process = true;
                Value<PosTag> posTag = token.getAnnotation(NlpAnnotations.POS_ANNOTATION);
                if(posTag != null && posTag.probability() == Value.UNKNOWN_PROBABILITY
                        || posTag.probability() >= (minPOSConfidence/2.0)){
                    cats = classifier.getCategories(posTag.value());
                } else { //no POS tags or probability to low
                    cats = Collections.emptySet();
                }
            } else { //check PosTags if we need to lookup this word
                Iterator<Value<PosTag>> posTags = token.getAnnotations(NlpAnnotations.POS_ANNOTATION).iterator();
                boolean ignore = false;
                while(!ignore && !process && posTags.hasNext()) {
                    Value<PosTag> value = posTags.next();
                    PosTag tag = value.value();
                    cats = classifier.getCategories(tag);
                    boolean state = cats.contains(LexicalCategory.Adjective) 
                            || cats.contains(LexicalCategory.Noun);
                    ignore = !state && (value.probability() == Value.UNKNOWN_PROBABILITY ||
                            value.probability() >= minPOSConfidence);
                    process = state && (value.probability() == Value.UNKNOWN_PROBABILITY ||
                            value.probability() >= (minPOSConfidence/2.0));
                }
            } //else process all tokens ... no POS tag checking needed
            if(process){
                String word = token.getSpan();
                double sentiment = 0.0;
                if(cats.isEmpty()){
                    sentiment = classifier.classifyWord(null, word);
                } else { //in case of multiple Lexical Cats
                    //we build the average over NOT NULL sentiments for the word
                    int catSentNum = 0;
                    for(LexicalCategory cat : cats){
                        double catSent = classifier.classifyWord(cat, word);
                        if(catSent != 0.0){
                            catSentNum++;
                            sentiment = sentiment + catSent;
                        }
                    }
                    if(catSentNum > 0){
                        sentiment = sentiment / (double) catSentNum;
                    }
                }
                if(sentiment != 0.0){
                    token.addAnnotation(SENTIMENT_ANNOTATION, new Value<Double>(sentiment));
                } //else do not set sentiments with 0.0
            } // else do not process
        }
//        } finally {
//            ci.getLock().writeLock().unlock();
//        }
    }


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

        //parse the configured languages
        langaugeConfig.setConfiguration(properties);
        
        //set the processed lexical categories
        Object value = properties.get(CONFIG_ADJECTIVES);
        adjectivesOnly = value instanceof Boolean ? (Boolean)value :
            value != null ? Boolean.parseBoolean(value.toString()) : 
                DEFAULT_PROCESS_ADJECTIVES_ONLY;
        
        //set minimum POS confidence
        value = properties.get(CONFIG_MIN_POS_CONFIDENCE);
        if(value instanceof Number){
            minPOSConfidence = ((Number)value).doubleValue();
        } else if(value != null){
            try {
                minPOSConfidence = Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                throw new ConfigurationException(CONFIG_MIN_POS_CONFIDENCE, 
                    "Unable to parsed minimum POS confidence value from '"
                    + value +"'!",e);
            }
        } else {
            minPOSConfidence = DEFAULT_MIN_POS_CONFIDNECE;
        }
        if(minPOSConfidence <= 0 || minPOSConfidence >= 1){
            throw new ConfigurationException(CONFIG_MIN_POS_CONFIDENCE, 
                "The configured minimum POS confidence value '"
                +minPOSConfidence+"' MUST BE > 0 and < 1!");
        }
    }
    
    @Deactivate
    protected void deactivate(ComponentContext ctx){
        //remove remaining classifiers
        this.classifiers.clear();
        langaugeConfig.setDefault();
        super.deactivate(ctx);
    }
    @Override
    public Map<String,Object> getServiceProperties() {
        return SERVICE_PROPERTIES;
    }

}

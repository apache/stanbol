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

package org.apache.stanbol.enhancer.engines.opennlp.pos.services;

import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.POS_ANNOTATION;
import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.getLanguage;
import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.initAnalysedText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.Sequence;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.opennlp.OpenNLP;
import org.apache.stanbol.enhancer.engines.opennlp.pos.model.PosTagSetRegistry;
import org.apache.stanbol.enhancer.nlp.NlpProcessingRole;
import org.apache.stanbol.enhancer.nlp.NlpServiceProperties;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextFactory;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextUtils;
import org.apache.stanbol.enhancer.nlp.model.Section;
import org.apache.stanbol.enhancer.nlp.model.Sentence;
import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.SpanTypeEnum;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.model.tag.TagSet;
import org.apache.stanbol.enhancer.nlp.phrase.PhraseTag;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
import org.apache.stanbol.enhancer.nlp.utils.LanguageConfiguration;
import org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.Blob;
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
 * A german language POS tagger. Requires that the content item has a text/plain part and a
 * language id of "de". Adds a POSContentPart to the content item that can be used for further
 * processing by other modules.
 * 
 * @author Sebastian Schaffert
 */

@Component(immediate = true, metatype = true, 
    configurationFactory = true, //allow multiple instances
    policy = ConfigurationPolicy.OPTIONAL) //create a default instance with the default configuration
@Service
@Properties(value={
        @Property(name= EnhancementEngine.PROPERTY_NAME,value="opennlp-pos"),
        @Property(name=OpenNlpPosTaggingEngine.CONFIG_LANGUAGES, value = {"*"},cardinality=Integer.MAX_VALUE),
        @Property(name=Constants.SERVICE_RANKING,intValue=-100) //give the default instance a ranking < 0
})
public class OpenNlpPosTaggingEngine extends AbstractEnhancementEngine<RuntimeException,RuntimeException> implements ServiceProperties {

    private static final Map<String,Object> SERVICE_PROPERTIES;
    static {
        Map<String,Object> props = new HashMap<String,Object>();
        props.put(ServiceProperties.ENHANCEMENT_ENGINE_ORDERING, 
            ServiceProperties.ORDERING_NLP_POS);
        props.put(NlpServiceProperties.ENHANCEMENT_ENGINE_NLP_ROLE, 
            NlpProcessingRole.PartOfSpeachTagging);
        SERVICE_PROPERTIES = Collections.unmodifiableMap(props);
    }

    /**
     * Language configuration. Takes a list of ISO language codes of supported languages. Currently supported
     * are the languages given as default value.
     */
    public static final String CONFIG_LANGUAGES = "org.apache.stanbol.enhancer.pos.languages";

    /**
     * The parameter name used to configure the name of the OpenNLP model used for pos tagging
     */
    private static final String MODEL_NAME_PARAM = "model";


    private static Logger log = LoggerFactory.getLogger(OpenNlpPosTaggingEngine.class);

    //Langauge configuration
    private LanguageConfiguration languageConfig = new LanguageConfiguration(CONFIG_LANGUAGES,new String[]{"*"});
//    private Set<String> configuredLanguages;
//    private Set<String> excludedLanguages;
//    private boolean allowAll;

    @Reference
    private OpenNLP openNLP;
    /**
     * Provides known {@link TagSet}s used by OpenNLP
     */
    private PosTagSetRegistry tagSetRegistry = PosTagSetRegistry.getInstance();
    
    @Reference
    private AnalysedTextFactory analysedTextFactory;

    /**
     * Holds as key the languages and as values the ad-hoc (unmapped) phrase tags
     * for that languages.<p>
     * NOTE: Not synchronised as concurrent execution caused multiple adds will
     * only create some additional {@link PhraseTag} instances and not actual
     * problems.
     */
    private Map<String,Map<String,PosTag>> languageAdhocTags = new HashMap<String,Map<String,PosTag>>();
     
    /**
     * Indicate if this engine can enhance supplied ContentItem, and if it
     * suggests enhancing it synchronously or asynchronously. The
     * {@link org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager} can force sync/async mode if desired, it is
     * just a suggestion from the engine.
     * <p/>
     * Returns ENHANCE_ASYNC in case there is a text/plain content part and a tagger for the language identified for
     * the content item, CANNOT_ENHANCE otherwise.
     *
     * @throws org.apache.stanbol.enhancer.servicesapi.EngineException
     *          if the introspecting process of the content item
     *          fails
     */
    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        // check if content is present
        Map.Entry<IRI,Blob> entry = NlpEngineHelper.getPlainText(this, ci, false);
        if(entry == null || entry.getValue() == null) {
            return CANNOT_ENHANCE;
        }

        String language = getLanguage(this,ci,false);
        if(language == null) {
            return CANNOT_ENHANCE;
        }
        if(!languageConfig.isLanguage(language)){
            log.trace(" > can NOT enhance ContentItem {} because language {} is "
                + "not enabled by this engines configuration",ci,language);
            return CANNOT_ENHANCE;
        }

        if(getPOSTagger(language) == null) {
            log.trace(" > can NOT enhance ContentItem {} because no POSTagger is"
                    + "is present for language {}",ci,language);
            return CANNOT_ENHANCE;
        }

        log.trace(" > can enhance ContentItem {} with language {}",ci,language);
        return ENHANCE_ASYNC;
    }

    /**
     * Compute enhancements for supplied ContentItem. The results of the process
     * are expected to be stored in the metadata of the content item.
     * <p/>
     * The client (usually an {@link org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager}) should take care of
     * persistent storage of the enhanced {@link org.apache.stanbol.enhancer.servicesapi.ContentItem}.
     * <p/>
     * This method creates a new POSContentPart using {@link org.apache.stanbol.enhancer.engines.pos.api.POSTaggerHelper#createContentPart} from a text/plain part and
     * stores it as a new part in the content item. The metadata is not changed.
     *
     * @throws org.apache.stanbol.enhancer.servicesapi.EngineException
     *          if the underlying process failed to work as
     *          expected
     */
    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        AnalysedText at = initAnalysedText(this,analysedTextFactory,ci);
        String language = getLanguage(this, ci, true);
        
        POSTagger posTagger = getPOSTagger(language);
        if(posTagger == null){
            //this means that the POS tagger became unavailable in-between
            //the call to canEnhance and computeEnhancement
            throw new EngineException("PosTagger for langauge '"+language
                + "is not available."); 
        }
        TagSet<PosTag> tagSet = tagSetRegistry.getTagSet(language);
        if(tagSet == null){
            log.warn("No POS TagSet registered for Language '{}'. Will build an "
                    + "adhoc set based on encountered Tags!",language);
            //for now only created to avoid checks for tagSet == null
            //TODO: in future we might want to automatically create posModels based
            //on tagged texts. However this makes no sense as long we can not
            //persist TagSets.
            tagSet = new TagSet<PosTag>("dummy", language);
        }
        //holds PosTags created for POS tags that where not part of the posModel
        //(will hold all PosTags in case tagSet is NULL
        Map<String,PosTag> adhocTags = languageAdhocTags.get(language);
        if(adhocTags == null){
                adhocTags =  new HashMap<String,PosTag>();
                languageAdhocTags.put(language, adhocTags);
        }
        //(1) Sentence detection
        
        //Try to read existing Sentence Annotations
        Iterator<Sentence> sentences = at.getSentences();
        List<Section> sentenceList;
        if(!sentences.hasNext()){
            //if non try to detect sentences
            log.trace(" > detect sentences for {}",at);
            sentenceList = detectSentences(at,language);
        }
        if(sentences.hasNext()){ //check if we have detected sentences
            log.trace(" > use existing Sentence annotations for {}",at);
            sentenceList = new ArrayList<Section>();
            AnalysedTextUtils.appandToList(sentences, sentenceList);
        } else {
            //no sentence detected ... treat the whole text as a single sentence
            //TODO: maybe apply here a limit to the text size!
            log.trace(" > unable to detect Sentences for {} (langauge: {})",at,language);
            sentenceList = Collections.singletonList((Section)at);
        }
        
        //for all sentences (or the whole Text - if no sentences available)
        for(Section sentence : sentenceList){

            //(2) Tokenize Sentences
            
            List<Token> tokenList;
            //check if there are already tokens
            Iterator<Token> tokens = sentence.getTokens();
            if(!tokens.hasNext()){ //no tokens present -> tokenize
                log.trace(" > tokenize {}",sentence);
                tokenList = tokenize(sentence,language);
            } else { //use existing
                log.trace(" > use existing Tokens for {}",sentence);
                tokenList = new ArrayList<Token>(); //ensure an ArrayList is used
                AnalysedTextUtils.appandToList(tokens, tokenList);
            }
            
            //(3) POS Tagging
            posTag(tokenList, posTagger,tagSet,adhocTags,language);
            
        }
        if(log.isTraceEnabled()){
            logAnnotations(at);
        }
    }
    
    @Override
    public Map<String,Object> getServiceProperties() {
        return SERVICE_PROPERTIES;
    }
    
    
    private void logAnnotations(AnalysedText at){
        Iterator<Span> it = at.getEnclosed(EnumSet.of(SpanTypeEnum.Sentence, SpanTypeEnum.Token));
        while(it.hasNext()){
            Span span = it.next();
            log.trace(" > {}",span);
            for(Value<PosTag> value : span.getAnnotations(POS_ANNOTATION)){
                log.trace("   - {}",value);
            }
        }
    }
    /**
     * POS tags the parsed tokens by using the pos tagger. Annotations are
     * added based on the posModel and already created adhoc tags.
     * @param tokenList
     * @param posTagger
     * @param posModel
     * @param adhocTags
     * @param language
     */
    private void posTag(List<Token> tokenList,
                        POSTagger posTagger,
                        TagSet<PosTag> posModel,
                        Map<String,PosTag> adhocTags, 
                        String language) {
        String[] tokenTexts = new String[tokenList.size()];
        for(int i=0;i<tokenList.size(); i++){
            tokenTexts[i] = tokenList.get(i).getSpan();
        }
        //get the topK POS tags and props and copy it over to the 2dim Arrays
        Sequence[] posSequences = posTagger.topKSequences(tokenTexts);
        //extract the POS tags and props for the current token from the
        //posSequences.
        //NOTE: Sequence includes always POS tags for all Tokens. If
        //      less then posSequences.length are available it adds the
        //      best match for all followings.
        //      We do not want such copies.
        PosTag[] actPos = new PosTag[posSequences.length];
        double[] actProp = new double[posSequences.length];
        for(int i=0;i<tokenTexts.length;i++){
            Token token = tokenList.get(i);
            boolean done = false;
            int j = 0;
            while( j < posSequences.length && !done){
                String p = posSequences[j].getOutcomes().get(i);
                done = j > 0 && p.equals(actPos[0].getTag());
                if(!done){
                    actPos[j] = getPosTag(posModel,adhocTags,p,language);
                    actProp[j] = posSequences[j].getProbs()[i];
                    j++;
                }
            }
            //create the POS values
            token.addAnnotations(POS_ANNOTATION, Value.values(actPos, actProp,j));
        }

    }

    private PosTag getPosTag(TagSet<PosTag> model, Map<String,PosTag> adhocTags, String tag, String language) {
        PosTag posTag = model.getTag(tag);
        if(posTag != null){
            return posTag;
        }
        posTag = adhocTags.get(tag);
        if(posTag != null){
            return posTag;
        }
        posTag = new PosTag(tag);
        adhocTags.put(tag, posTag);
        log.info("Encountered umapped POS tag '{}' for langauge '{}'",tag,language);
        return posTag;
    }

    private List<Token> tokenize(Section section,String langauge) {
        Tokenizer tokenizer = getTokenizer(langauge);
        String text = section.getSpan();
        List<Token> tokens = new ArrayList<Token>(text.length()/5); //assume avr. token length is 5
        opennlp.tools.util.Span[] tokenSpans = tokenizer.tokenizePos(section.getSpan());
        for(int i=0;i<tokenSpans.length;i++){
            Token token = section.addToken(tokenSpans[i].getStart(), tokenSpans[i].getEnd());
            log.trace(" > add {}",token);
            tokens.add(token);
        }
        return tokens;
    }

    private List<Section> detectSentences(AnalysedText at, String language) {
        SentenceDetector sentenceDetector = getSentenceDetector(language);
        List<Section> sentences;
        if(sentenceDetector != null){
            sentences = new ArrayList<Section>();
            for(opennlp.tools.util.Span sentSpan : sentenceDetector.sentPosDetect(at.getSpan())) {
                Sentence sentence = at.addSentence(sentSpan.getStart(), sentSpan.getEnd());
                log.trace(" > add {}",sentence);
                sentences.add(sentence);
            }
        } else {
            sentences = null;
        }
        return sentences;
    }

    /**
     * Activate and read the properties. Configures and initialises a POSTagger for each language configured in
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

        languageConfig.setConfiguration(properties);
    }
    
    @Deactivate
    protected void deactivate(ComponentContext context) {
        languageConfig.setDefault();
        super.deactivate(context);
    }
    
    
    private SentenceDetector getSentenceDetector(String language) {
        try {
            SentenceModel model = openNLP.getSentenceModel(language);
            if(model != null) {
                log.debug("Sentence Detection Model {} for lanugage '{}' version: {}",
                    new Object[]{model.getClass().getSimpleName(), 
                                 model.getLanguage(), 
                                 model.getVersion() != null ? model.getVersion() : "undefined"});
                return new SentenceDetectorME(model);
            }
        } catch (Exception e) {
        }
        log.debug("Sentence Detection Model for Language '{}' not available.", language);
        return null;
    }
    private POSTagger getPOSTagger(String language) {
        String modelName = languageConfig.getParameter(language,MODEL_NAME_PARAM);
        try {
            POSModel model;
            if(modelName == null){ //use the default
                model = openNLP.getPartOfSpeechModel(language);
            } else {
                model = openNLP.getModel(POSModel.class, modelName, null);
            }
            if(model != null) {
                log.debug("POS Tagger Model {} for lanugage '{}' version: {}",
                    new Object[]{model.getClass().getSimpleName(), 
                                 model.getLanguage(), 
                                 model.getVersion() != null ? model.getVersion() : "undefined"});
                return new POSTaggerME(model);
            }
        } catch (Exception e) {
            log.warn("Unable to load POS model for language '"+language+"'!",e);
        }
        log.debug("POS tagging Model for Language '{}' not available.", language);
        return null;
    }
    
    private Tokenizer getTokenizer(String language){
        return openNLP.getTokenizer(language);
    }
    
}

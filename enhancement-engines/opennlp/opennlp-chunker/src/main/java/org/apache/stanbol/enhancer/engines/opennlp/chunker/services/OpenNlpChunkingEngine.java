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

package org.apache.stanbol.enhancer.engines.opennlp.chunker.services;

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

import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.opennlp.OpenNLP;
import org.apache.stanbol.enhancer.engines.opennlp.chunker.model.PhraseTagSetRegistry;
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
        @Property(name=EnhancementEngine.PROPERTY_NAME,value="opennlp-chunker"),
        @Property(name=OpenNlpChunkingEngine.CONFIG_LANGUAGES,
            value = {"de;model=OpenNLP_1.5.1-German-Chunker-TigerCorps07.zip","*"}),
        @Property(name=OpenNlpChunkingEngine.MIN_CHUNK_SCORE),
        @Property(name=Constants.SERVICE_RANKING,intValue=-100) //give the default instance a ranking < 0
})
public class OpenNlpChunkingEngine extends AbstractEnhancementEngine<RuntimeException,RuntimeException> implements ServiceProperties {

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
    public static final String CONFIG_LANGUAGES = "org.apache.stanbol.enhancer.chunker.languages";

    public static final String MIN_CHUNK_SCORE = "org.apache.stanbol.enhancer.chunker.minScore";
    
    private static final String MODEL_PARAM_NAME = "model";

    private static Logger log = LoggerFactory.getLogger(OpenNlpChunkingEngine.class);

    private LanguageConfiguration languageConfiguration = new LanguageConfiguration(CONFIG_LANGUAGES, 
        new String []{"de;"+MODEL_PARAM_NAME+"=OpenNLP_1.5.1-German-Chunker-TigerCorps07.zip","*"});
    
    @Reference
    private OpenNLP openNLP;
    
    /**
     * The registry used to lookup predefined {@link TagSet}s
     */
    private PhraseTagSetRegistry tagSetRegistry = PhraseTagSetRegistry.getInstance();

    private Double minChunkScore;

    /**
     * Holds as key the languages and as values the ad-hoc (unmapped) phrase tags
     * for that languages.<p>
     * NOTE: Not synchronised as concurrent execution caused multiple adds will
     * only create some additional {@link PhraseTag} instances and not actual
     * problems.
     */
    private Map<String,Map<String,PhraseTag>> languageAdhocTags = new HashMap<String,Map<String,PhraseTag>>();
    
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
        ChunkerME chunker = initChunker(language);
        if(chunker == null){
            return;
        }
        //init the Phrase TagSet
        TagSet<PhraseTag> tagSet = tagSetRegistry.getTagSet(language);
        if(tagSet == null){ 
        }
        if(tagSet == null){
            log.warn("No Phrase TagSet registered for Language '{}'. Will build an "
                + "adhoc set based on encountered Tags!",language);
            //for now only created to avoid checks for tagSet == null
            //TODO: in future we might want to automatically create posModels based
            //on tagged texts. However this makes no sense as long we can not
            //persist TagSets.
            tagSet = new TagSet<PhraseTag>("dummy", language);
        }
        //holds PosTags created for POS tags that where not part of the posModel
        //(will hold all PosTags in case tagSet is NULL
        Map<String,PhraseTag> adhocTags = languageAdhocTags.get(language);
        if(adhocTags == null){
            adhocTags = new HashMap<String,PhraseTag>();
            languageAdhocTags.put(language, adhocTags);
        }        
        ci.getLock().writeLock().lock();
        try {
            Iterator<? extends Section> sentences = at.getSentences();
            if(!sentences.hasNext()){ //no sentences ... iterate over the whole text
                sentences = Collections.singleton(at).iterator();
            }
            List<String> tokenTextList = new ArrayList<String>(64);
            List<String> posList = new ArrayList<String>(64);
            List<Token> tokenList = new ArrayList<Token>(64);
            //process each sentence seperatly
            while(sentences.hasNext()){
                // (1) get Tokens and POS information for the sentence
                Section sentence = sentences.next();
                Iterator<Token> tokens = sentence.getTokens();
                while(tokens.hasNext()){
                    Token token = tokens.next();
                    tokenList.add(token);
                    tokenTextList.add(token.getSpan());
                    Value<PosTag> posValue = token.getAnnotation(POS_ANNOTATION);
                    if(posValue == null){
                        throw new EngineException("Missing POS value for Token '"
                            + token.getSpan()+"' of ContentItem "+ci.getUri()
                            + "(Sentence: '"+sentence.getSpan()+"'). This may "
                            + "indicate that a POS tagging Engine is missing in "
                            + "the EnhancementChain or that the used POS tagging "
                            + "does not provide POS tags for each token!");
                    } else {
                        posList.add(posValue.value().getTag());
                    }
                }
                String[] tokenStrings = tokenTextList.toArray(new String[tokenTextList.size()]);
                String[] tokenPos = posList.toArray(new String[tokenTextList.size()]);
                if(log.isTraceEnabled()){
                    log.trace("Tokens: {}"+Arrays.toString(tokenStrings));
                }
                tokenTextList.clear(); //free memory
                posList.clear(); //free memory
                
                // (2) Chunk the sentence
                
                String[] chunkTags = chunker.chunk(tokenStrings, tokenPos);
                double[] chunkProb = chunker.probs();
                if(log.isTraceEnabled()){
                    log.trace("Chunks: {}"+Arrays.toString(chunkTags));
                }
                tokenStrings = null; //free memory
                tokenPos = null; //free memory
                
                // (3) Process the results and write the Annotations
                double chunkProps = 0;
                int chunkTokenCount = 0;
                PhraseTag tag = null;
                int i;
                /*
                 * This assumes:
                 *  - 'B-{tag}' ... for start of a new chunk
                 *  - '???' ... anything other for continuing the current chunk
                 *  - 'O' ... no chunk (ends current chunk)
                 */
                for(i=0;i<tokenList.size();i++){
                    boolean start = chunkTags[i].charAt(0) == 'B';
                    boolean end = tag != null && (start || chunkTags[i].charAt(0) == 'O');
                    if(end){ //add the current phrase
                        //add at AnalysedText level, because offsets are absolute
                        //NOTE we are already at the next token when we detect the end
                        Chunk chunk = at.addChunk( 
                            tokenList.get(i-chunkTokenCount).getStart(), 
                            tokenList.get(i-1).getEnd());
                        chunk.addAnnotation(PHRASE_ANNOTATION, 
                            new Value<PhraseTag>(tag,
                                    chunkProps/(double)chunkTokenCount));
                        //reset the state
                        tag = null;
                        chunkTokenCount = 0;
                        chunkProps = 0;
                    }
                    if(start){ //create the new tag
                        tag = getPhraseTag(tagSet,adhocTags,
                            chunkTags[i].substring(2), language); //skip 'B-'
                        
                    }
                    if(tag != null){ //count this token for the current chunk
                        chunkProps = chunkProps + chunkProb[i];
                        chunkTokenCount++;
                    }
                }
                if(tag != null){
                    Chunk chunk = at.addChunk( 
                        tokenList.get(i-chunkTokenCount).getStart(), 
                        tokenList.get(i-1).getEnd());
                    chunk.addAnnotation(PHRASE_ANNOTATION, 
                        new Value<PhraseTag>(tag,
                                chunkProps/(double)chunkTokenCount));
                    
                }
                // (4) clean up
                tokenList.clear();
            }

        } finally {
            ci.getLock().writeLock().unlock();
        }
        if(log.isTraceEnabled()){
            logChunks(at);
        }
    }
    
    @Override
    public Map<String,Object> getServiceProperties() {
        return SERVICE_PROPERTIES;
    }
    
    private void logChunks(AnalysedText at){
        Iterator<Span> it = at.getEnclosed(EnumSet.of(SpanTypeEnum.Sentence, SpanTypeEnum.Chunk));
        while(it.hasNext()){
            Span span = it.next();
            if(span.getType() == SpanTypeEnum.Chunk){
                log.trace(" > {} {}",span,span.getSpan());
            } else {
                log.trace(" > {}",span);
            }
            for(Value<PhraseTag> value : span.getAnnotations(PHRASE_ANNOTATION)){
                log.trace("   - {}",value);
            }
        }
    }

    private PhraseTag getPhraseTag(TagSet<PhraseTag> model, Map<String,PhraseTag> adhocTags, String tag, String language) {
        PhraseTag phraseTag = model.getTag(tag);
        if(phraseTag != null){
            return phraseTag;
        }
        phraseTag = adhocTags.get(tag);
        if(phraseTag != null){
            return phraseTag;
        }
        phraseTag = new PhraseTag(tag);
        adhocTags.put(tag, phraseTag);
        log.info("Encountered unknown POS tag '{}' for langauge '{}'",tag,language);
        return phraseTag;
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
        
        //read the min chunk score
        Object value = properties.get(MIN_CHUNK_SCORE);
        Double minScore;
        if(value instanceof Number){
            minScore = ((Number)value).doubleValue();
        } else if (value != null && !value.toString().isEmpty()){
            try {
                minScore = Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                throw new ConfigurationException(MIN_CHUNK_SCORE, 
                    "The configured minumum chunk score MUST BE a floating point"
                    + "number in the range > 0 < 1. Values >= 0 will deactivate "
                    + "this feature.",e);
            }
        } else {
            minScore = null;
        }
        if(minScore != null && minScore.doubleValue() >= 1d){
            throw new ConfigurationException(MIN_CHUNK_SCORE, 
                "The configured minumum chunk score '"+minScore+"' MUST BE a "
                + "floating point number in the range > 0 < 1. Values >= 0 will "
                + "deactivate this feature.");
        } else if(minScore == null || minScore.doubleValue() <= 0){
            this.minChunkScore = null;
        } else {
            log.info(" > set minimum chunk score to {} (Engine: {})",
                minScore, getName());
            this.minChunkScore = minScore;
        }
        
        //read the language configuration
        languageConfiguration.setConfiguration(properties);
    }
    
    @Deactivate
    protected void deactivate(ComponentContext context){
        this.languageConfiguration.setDefault();
        this.minChunkScore = null;
        this.languageAdhocTags.clear();
        super.deactivate(context);
    }
    
   
    private ChunkerME initChunker(String language) {
        isLangaugeConfigured(this,languageConfiguration,language, true); //check if the parsed language is ok
        String modelName = languageConfiguration.getParameter(language, MODEL_PARAM_NAME);
        ChunkerModel model;
        try {
            if(modelName == null){ // the default model
                model = openNLP.getChunkerModel(language);
            } else {
                model = openNLP.getModel(ChunkerModel.class, modelName, null);
            }
        }catch (IOException e) {
            log.warn("Unable to load Chunker model for language '"+language
                + "' (model: "+(modelName == null ? "default" : modelName)+")",
                e);
            return null;
        } catch (RuntimeException e){
            log.warn("Error while creating ChunkerModel for language '"+language
                + "' (model: "+(modelName == null ? "default" : modelName)+")",
                e);
            return null;
        }
        if(model == null){
            log.trace("no Chunker Model for language {}",language);
            return null;
        } else {
            return new ChunkerME(model);
        }
    }

}

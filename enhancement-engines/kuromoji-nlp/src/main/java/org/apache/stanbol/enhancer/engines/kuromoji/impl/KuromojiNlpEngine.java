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

package org.apache.stanbol.enhancer.engines.kuromoji.impl;

import static org.apache.stanbol.enhancer.engines.kuromoji.Constants.NER_TAG_SET;
import static org.apache.stanbol.enhancer.engines.kuromoji.Constants.POS_TAG_SET;
import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.MORPHO_ANNOTATION;
import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.POS_ANNOTATION;
import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.getLanguage;
import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.initAnalysedText;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_END;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTED_TEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTION_CONTEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.commons.io.input.CharSequenceReader;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ja.JapaneseBaseFormFilterFactory;
import org.apache.lucene.analysis.ja.JapaneseKatakanaStemFilterFactory;
import org.apache.lucene.analysis.ja.JapanesePartOfSpeechStopFilterFactory;
import org.apache.lucene.analysis.ja.JapaneseTokenizerFactory;
import org.apache.lucene.analysis.ja.tokenattributes.BaseFormAttribute;
import org.apache.lucene.analysis.ja.tokenattributes.InflectionAttribute;
import org.apache.lucene.analysis.ja.tokenattributes.PartOfSpeechAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.Version;
import org.apache.sling.installer.core.impl.OsgiInstallerImpl;
import org.apache.stanbol.commons.solr.utils.StanbolResourceLoader;
import org.apache.stanbol.enhancer.nlp.NlpAnnotations;
import org.apache.stanbol.enhancer.nlp.NlpProcessingRole;
import org.apache.stanbol.enhancer.nlp.NlpServiceProperties;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextFactory;
import org.apache.stanbol.enhancer.nlp.model.Chunk;
import org.apache.stanbol.enhancer.nlp.model.Sentence;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.morpho.MorphoFeatures;
import org.apache.stanbol.enhancer.nlp.ner.NerTag;
import org.apache.stanbol.enhancer.nlp.pos.Pos;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
import org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sentence detection and word tokenizer for Chinese based on the Solr/Lucene
 * smartcn analysers.
 * 
 * @author Rupert Westenthaler
 */

@Component(immediate = true, metatype = true, 
    policy = ConfigurationPolicy.OPTIONAL) //create a default instance with the default configuration
@Service
@Properties(value={
        @Property(name= EnhancementEngine.PROPERTY_NAME,value="kuromoji-nlp"),
        @Property(name=Constants.SERVICE_RANKING,intValue=0) //give the default instance a ranking < 0
})
public class KuromojiNlpEngine extends AbstractEnhancementEngine<IOException,RuntimeException> implements ServiceProperties {

    private static final Version LUCENE_VERSION = Version.LUCENE_44;
    private static final String TOKENIZER_MODE = "search"; //normal, extended
    private static final Map<String,Object> SERVICE_PROPERTIES;
    private static final Map<String,String> TOKENIZER_FACTORY_CONFIG = new HashMap<String,String>();
	private static final Map<String, String> BASE_FORM_FILTER_CONFIG = new HashMap<String,String>();
	private static final Map<String, String> POS_FILTER_CONFIG = new HashMap<String,String>();
	private static final Map<String, String> STEMM_FILTER_CONFIG = new HashMap<String,String>();
    static {
        Map<String,Object> props = new HashMap<String,Object>();
        props.put(ServiceProperties.ENHANCEMENT_ENGINE_ORDERING, 
            ServiceProperties.ORDERING_NLP_TOKENIZING);
        props.put(NlpServiceProperties.ENHANCEMENT_ENGINE_NLP_ROLE, 
            NlpProcessingRole.Tokenizing);
        SERVICE_PROPERTIES = Collections.unmodifiableMap(props);

        TOKENIZER_FACTORY_CONFIG.put("luceneMatchVersion", LUCENE_VERSION.toString());
        TOKENIZER_FACTORY_CONFIG.put("mode",TOKENIZER_MODE);
        //we want to have tokens for punctations
        TOKENIZER_FACTORY_CONFIG.put("discardPunctuation", "false");
        
        BASE_FORM_FILTER_CONFIG.put("luceneMatchVersion", LUCENE_VERSION.toString());
        
        POS_FILTER_CONFIG.put("luceneMatchVersion", LUCENE_VERSION.toString());
        POS_FILTER_CONFIG.put("tags", "nostoptags.txt");
        POS_FILTER_CONFIG.put("enablePositionIncrements","true");
        
        STEMM_FILTER_CONFIG.put("luceneMatchVersion", LUCENE_VERSION.toString());
        STEMM_FILTER_CONFIG.put("minimumLength","4");
    }


    private static Logger log = LoggerFactory.getLogger(KuromojiNlpEngine.class);
    
    @Reference(cardinality=ReferenceCardinality.OPTIONAL_UNARY)
    protected ResourceLoader parentResourceLoader;

    protected ResourceLoader resourceLoader;

    //private MappingCharFilterFactory charFilterFactory;
    private TokenizerFactory tokenizerFactory;
    
    private List<TokenFilterFactory> filterFactories = new ArrayList<TokenFilterFactory>();
    
    @Reference
    protected AnalysedTextFactory analysedTextFactory;
    
    protected LiteralFactory lf = LiteralFactory.getInstance();
    /**

     * holds {@link PosTag}s that are not contained in the 
     * {@link org.apache.stanbol.enhancer.engines.kuromoji.Constants#POS_TAG_SET}
     */
    private Map<String,PosTag> adhocTags = new HashMap<String,PosTag>();
    
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
        if("ja".equals(language) || (language != null && language.startsWith("ja-"))) {
            log.trace(" > can enhance ContentItem {} with language {}",ci,language);
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
        final AnalysedText at = initAnalysedText(this,analysedTextFactory,ci);

        String language = getLanguage(this,ci,false);
        if(!("ja".equals(language) || (language != null && language.startsWith("ja-")))) {
            throw new IllegalStateException("The detected language is NOT 'ja'! "
                + "As this is also checked within the #canEnhance(..) method this "
                + "indicates an Bug in the used EnhancementJobManager implementation. "
                + "Please report this on the dev@apache.stanbol.org or create an "
                + "JIRA issue about this.");
        }
        //start with the Tokenizer
        TokenStream tokenStream = tokenizerFactory.create(new CharSequenceReader(at.getText()));
        //build the analyzing chain by adding all TokenFilters
        for(TokenFilterFactory filterFactory : filterFactories){
            tokenStream = filterFactory.create(tokenStream);
        }

        //Try to extract sentences based on POS tags ...
        int sentStartOffset = -1;
        //NER data
        List<NerData> nerList = new ArrayList<NerData>();
        int nerSentIndex = 0; //the next index where the NerData.context need to be set
        NerData ner = null;
        OffsetAttribute offset = null;
        try {
        	tokenStream.reset(); //required with Solr 4
            while (tokenStream.incrementToken()){
                offset = tokenStream.addAttribute(OffsetAttribute.class);
                Token token = at.addToken(offset.startOffset(), offset.endOffset());
                //Get the POS attribute and init the PosTag
                PartOfSpeechAttribute posAttr = tokenStream.addAttribute(PartOfSpeechAttribute.class);
                PosTag posTag = POS_TAG_SET.getTag(posAttr.getPartOfSpeech());
                if(posTag == null){
                    posTag = adhocTags.get(posAttr.getPartOfSpeech());
                    if(posTag == null){
                        posTag = new PosTag(posAttr.getPartOfSpeech());
                        adhocTags.put(posAttr.getPartOfSpeech(), posTag);
                        log.warn(" ... missing PosTag mapping for {}",posAttr.getPartOfSpeech());
                    }
                }
                //Sentence detection by POS tag
                if(sentStartOffset < 0){ //the last token was a sentence ending
                	sentStartOffset = offset.startOffset();
                }
                if(posTag.hasPos(Pos.Point)) { 
                    Sentence sent = at.addSentence(sentStartOffset, offset.startOffset());
                    //add the sentence as context to the NerData instances
                    while(nerSentIndex < nerList.size()){
                        nerList.get(nerSentIndex).context = sent.getSpan();
                        nerSentIndex++;
                    }
                    sentStartOffset = -1;
                }
                //POS
                token.addAnnotation(POS_ANNOTATION, Value.value(posTag));
                //NER
                NerTag nerTag = NER_TAG_SET.getTag(posAttr.getPartOfSpeech());
                if(ner != null && (nerTag == null || !ner.tag.getType().equals(nerTag.getType()))){
                    //write NER annotation
                    Chunk chunk = at.addChunk(ner.start, ner.end);
                    chunk.addAnnotation(NlpAnnotations.NER_ANNOTATION, Value.value(ner.tag));
                    //NOTE that the fise:TextAnnotation are written later based on the nerList
                    //clean up
                    ner = null;
                }
                if(nerTag != null){
                    if(ner == null){
                        ner = new NerData(nerTag, offset.startOffset());
                        nerList.add(ner);
                    }
                    ner.end = offset.endOffset();
                }
                BaseFormAttribute baseFormAttr = tokenStream.addAttribute(BaseFormAttribute.class);
                MorphoFeatures morpho = null;
                if(baseFormAttr != null && baseFormAttr.getBaseForm() != null){
                	morpho = new MorphoFeatures(baseFormAttr.getBaseForm());
                	morpho.addPos(posTag); //and add the posTag
                }
                InflectionAttribute inflectionAttr = tokenStream.addAttribute(InflectionAttribute.class);
                inflectionAttr.getInflectionForm();
                inflectionAttr.getInflectionType();
                if(morpho != null){ //if present add the morpho
                	token.addAnnotation(MORPHO_ANNOTATION, Value.value(morpho));
                }
            }
            //we still need to write the last sentence
            Sentence lastSent = null;
            if(offset != null && sentStartOffset >= 0 && offset.endOffset() > sentStartOffset){
                lastSent = at.addSentence(sentStartOffset, offset.endOffset());
            }
            //and set the context off remaining named entities
            while(nerSentIndex < nerList.size()){
                if(lastSent != null){
                    nerList.get(nerSentIndex).context = lastSent.getSpan();
                } else { //no sentence detected
                    nerList.get(nerSentIndex).context = at.getSpan();
                }
                nerSentIndex++;
            }
        } catch (IOException e) {
            throw new EngineException(this, ci, "Exception while reading from "
                + "AnalyzedText contentpart",e);
        } finally {
            try {
                tokenStream.close();
            } catch (IOException e) {/* ignore */}
        }
        //finally write the NER annotations to the metadata of the ContentItem
        final Graph metadata = ci.getMetadata();
        ci.getLock().writeLock().lock();
        try {
            Language lang = new Language("ja");
            for(NerData nerData : nerList){
                IRI ta = EnhancementEngineHelper.createTextEnhancement(ci, this);
                metadata.add(new TripleImpl(ta, ENHANCER_SELECTED_TEXT, new PlainLiteralImpl(
                    at.getSpan().substring(nerData.start, nerData.end),lang)));
                metadata.add(new TripleImpl(ta, DC_TYPE, nerData.tag.getType()));
                metadata.add(new TripleImpl(ta, ENHANCER_START, lf.createTypedLiteral(nerData.start)));
                metadata.add(new TripleImpl(ta, ENHANCER_END, lf.createTypedLiteral(nerData.end)));
                metadata.add(new TripleImpl(ta, ENHANCER_SELECTION_CONTEXT, 
                    new PlainLiteralImpl(nerData.context, lang)));
            }
        } finally{
            ci.getLock().writeLock().unlock();
        }
    }

    @Override
    public Map<String,Object> getServiceProperties() {
        return SERVICE_PROPERTIES;
    }
    /**
     * Activate and read the properties. Configures and initialises a POSTagger for each language configured in
     * CONFIG_LANGUAGES.
     *
     * @param ce the {@link org.osgi.service.component.ComponentContext}
     */
    @Activate
    protected void activate(ComponentContext ce) throws ConfigurationException, IOException {
        log.info("activating smartcn tokenizing engine");
        super.activate(ce);
        //init the Solr ResourceLoader used for initialising the components
        //first a ResourceLoader for this classloader, 2nd one using the commons.solr.core classloader
        //and third the parentResourceLoader (if present).
        resourceLoader = new StanbolResourceLoader(KuromojiNlpEngine.class.getClassLoader(), 
            new StanbolResourceLoader(parentResourceLoader));
        tokenizerFactory = new JapaneseTokenizerFactory(TOKENIZER_FACTORY_CONFIG);
        ((ResourceLoaderAware) tokenizerFactory).inform(resourceLoader);
        //base form filter
        TokenFilterFactory baseFormFilterFactory =  new JapaneseBaseFormFilterFactory(BASE_FORM_FILTER_CONFIG);
        filterFactories.add(baseFormFilterFactory);
        //POS filter
        TokenFilterFactory posFilterFactory = new JapanesePartOfSpeechStopFilterFactory(POS_FILTER_CONFIG);
        ((ResourceLoaderAware) posFilterFactory).inform(resourceLoader);
        filterFactories.add(posFilterFactory);
        //Stemming
        TokenFilterFactory stemmFilterFactory = new JapaneseKatakanaStemFilterFactory(STEMM_FILTER_CONFIG);
        filterFactories.add(stemmFilterFactory);
    }
    
    @Deactivate
    protected void deactivate(ComponentContext context) {
    	tokenizerFactory = null;
    	filterFactories.clear();
    	filterFactories = null;
        super.deactivate(context);
    }

    /**
     * This is an internal helper class that avoids to execute sentences
     * using the {@link SentenceTokenizer} twice.
     * @author Rupert Westenthaler
     *
     */
    protected final class AnalyzedTextSentenceTokenizer extends Tokenizer {
        private final AnalysedText at;
        private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
        private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
        private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);
        private Iterator<Sentence> sentences;
        private Sentence sentence = null;

        protected AnalyzedTextSentenceTokenizer(AnalysedText at) {
            super(new StringReader(at.getText().toString()));
            this.at = at;
            sentences = at.getSentences();
        }

        @Override
        public boolean incrementToken() throws IOException {
            if(sentences.hasNext()){
                sentence = sentences.next();
                termAtt.setEmpty().append(sentence.getSpan());
                offsetAtt.setOffset(sentence.getStart(),sentence.getEnd());
                typeAtt.setType("sentence");
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void end() throws IOException {
          // set final offset
          offsetAtt.setOffset(at.getEnd(), at.getEnd());
        }
        @Override
        public void reset() throws IOException {
            super.reset();
            sentences = at.getSentences();
            termAtt.setEmpty();
            offsetAtt.setOffset(0, 0);
            typeAtt.setType(null);
        }
    }
}

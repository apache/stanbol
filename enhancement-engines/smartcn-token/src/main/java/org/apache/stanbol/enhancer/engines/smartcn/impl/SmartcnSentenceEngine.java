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

package org.apache.stanbol.enhancer.engines.smartcn.impl;

import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.getLanguage;
import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.initAnalysedText;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.commons.io.input.CharSequenceReader;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.cn.smart.SentenceTokenizer;
import org.apache.lucene.analysis.cn.smart.WordTokenFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.stanbol.enhancer.nlp.NlpProcessingRole;
import org.apache.stanbol.enhancer.nlp.NlpServiceProperties;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextFactory;
import org.apache.stanbol.enhancer.nlp.model.Sentence;
import org.apache.stanbol.enhancer.nlp.model.Token;
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
 * Sentence detection and word tokenizer for Chinese based on the Solr/Lucene
 * smartcn analysers.
 * 
 * @author Rupert Westenthaler
 */

@Component(immediate = true, metatype = true, 
    policy = ConfigurationPolicy.OPTIONAL) //create a default instance with the default configuration
@Service
@Properties(value={
        @Property(name= EnhancementEngine.PROPERTY_NAME,value="smartcn-sentence"),
        @Property(name=Constants.SERVICE_RANKING,intValue=0) //give the default instance a ranking < 0
})
public class SmartcnSentenceEngine extends AbstractEnhancementEngine<RuntimeException,RuntimeException> implements ServiceProperties {

    private static final Map<String,Object> SERVICE_PROPERTIES;
    static {
        Map<String,Object> props = new HashMap<String,Object>();
        props.put(ServiceProperties.ENHANCEMENT_ENGINE_ORDERING, 
            ServiceProperties.ORDERING_NLP_SENTENCE_DETECTION);
        props.put(NlpServiceProperties.ENHANCEMENT_ENGINE_NLP_ROLE, 
            NlpProcessingRole.SentenceDetection);
        SERVICE_PROPERTIES = Collections.unmodifiableMap(props);
    }


    private static Logger log = LoggerFactory.getLogger(SmartcnSentenceEngine.class);
    
    @Reference
    private AnalysedTextFactory analysedTextFactory;
    
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
        if("zh".equals(language) || (language != null && language.startsWith("zh-"))) {
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
        if(!("zh".equals(language) || (language != null && language.startsWith("zh-")))) {
            throw new IllegalStateException("The detected language is NOT 'zh'! "
                + "As this is also checked within the #canEnhance(..) method this "
                + "indicates an Bug in the used EnhancementJobManager implementation. "
                + "Please report this on the dev@apache.stanbol.org or create an "
                + "JIRA issue about this.");
        }
        //first the sentences
        TokenStream sentences = new SentenceTokenizer(new CharSequenceReader(at.getText()));
        try {
        	sentences.reset();
            while(sentences.incrementToken()){
                OffsetAttribute offset = sentences.addAttribute(OffsetAttribute.class);
                Sentence s = at.addSentence(offset.startOffset(), offset.endOffset());
                if(log.isTraceEnabled()) {
                    log.trace("detected {}:{}",s,s.getSpan());
                }
            }
        } catch (IOException e) {
            String message = String.format("IOException while reading from "
                +"CharSequenceReader of AnalyzedText for ContentItem %s",ci.getUri());
            log.error(message,e);
            throw new EngineException(this, ci, message, e);
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
    protected void activate(ComponentContext ce) throws ConfigurationException {
        log.info("activating smartcn tokenizing engine");
        super.activate(ce);
    }
    
    @Deactivate
    protected void deactivate(ComponentContext context) {
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

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

package org.apache.stanbol.enhancer.engines.opennlp.token.impl;

import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.getLanguage;
import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.initAnalysedText;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

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
import org.apache.stanbol.enhancer.nlp.NlpProcessingRole;
import org.apache.stanbol.enhancer.nlp.NlpServiceProperties;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextFactory;
import org.apache.stanbol.enhancer.nlp.model.Section;
import org.apache.stanbol.enhancer.nlp.model.Token;
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
        @Property(name=EnhancementEngine.PROPERTY_NAME,value="opennlp-token"),
        @Property(name=OpenNlpTokenizerEngine.CONFIG_LANGUAGES, value = {"*"},cardinality=Integer.MAX_VALUE),
        @Property(name=Constants.SERVICE_RANKING,intValue=-100) //give the default instance a ranking < 0
})
public class OpenNlpTokenizerEngine extends AbstractEnhancementEngine<RuntimeException,RuntimeException> implements ServiceProperties {

    /**
     * Language configuration. Takes a list of ISO language codes of supported languages. Currently supported
     * are the languages given as default value.
     */
    public static final String CONFIG_LANGUAGES = "org.apache.stanbol.enhancer.token.languages";

    /**
     * The parameter name used to configure the name of the OpenNLP model used for pos tagging
     */
    private static final String MODEL_NAME_PARAM = "model";

    /**
     * Configuring {@value #SIMPLE_MODEL_NAME} as value for the {@link #MODEL_NAME_PARAM}
     * will cause the {@link SimpleTokenizer#INSTANCE} to be used for
     * Tokenizing the language.<p>
     * This might be useful to force the usage of this tokenizer even if a
     * language specific model is available via the {@link OpenNLP} service.
     */
    private static final String SIMPLE_MODEL_NAME = "SIMPLE";

    private static final Map<String,Object> SERVICE_PROPERTIES;
    static {
        Map<String,Object> props = new HashMap<String,Object>();
        props.put(ServiceProperties.ENHANCEMENT_ENGINE_ORDERING, 
            ServiceProperties.ORDERING_NLP_TOKENIZING);
        props.put(NlpServiceProperties.ENHANCEMENT_ENGINE_NLP_ROLE, 
            NlpProcessingRole.Tokenizing);
        SERVICE_PROPERTIES = Collections.unmodifiableMap(props);
    }


    private static Logger log = LoggerFactory.getLogger(OpenNlpTokenizerEngine.class);

    //Langauge configuration
    private LanguageConfiguration languageConfig = new LanguageConfiguration(CONFIG_LANGUAGES,new String[]{"*"});

    @Reference
    private OpenNLP openNLP;
    
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
        if(language == null) {
            return CANNOT_ENHANCE;
        }
        if(!languageConfig.isLanguage(language)){
            log.trace(" > can NOT enhance ContentItem {} because language {} is "
                + "not enabled by this engines configuration",ci,language);
            return CANNOT_ENHANCE;
        }
        if(getTokenizer(language) == null){
            log.trace(" > can NOT tokenize plain text of {} because the tokenizer "
                + "for language {} is not available.",ci,language);
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
        
        Tokenizer tokenizer = getTokenizer(language);
        if(tokenizer == null){
            log.warn("Tokenizer for language {} is no longer available. "
                    + "This might happen if the model becomes unavailable during enhancement. "
                    + "If this happens more often it might also indicate an bug in the used "
                    + "EnhancementJobManager implementation as the availability is also checked "
                    + "in the canEnhance(..) method of this Enhancement Engine.");
            return;
        }
        //Try to use sentences for tokenizing
        Iterator<? extends Section> sections = at.getSentences();
        if(!sections.hasNext()){
            //if no sentences are annotated
            sections = Collections.singleton(at).iterator();
        }
        
        //for all sentences (or the whole Text - if no sentences available)
        while(sections.hasNext()){
            Section section = sections.next();
            //Tokenize section
            opennlp.tools.util.Span[] tokenSpans = tokenizer.tokenizePos(section.getSpan());
            for(int i=0;i<tokenSpans.length;i++){
                Token token = section.addToken(tokenSpans[i].getStart(), tokenSpans[i].getEnd());
                log.trace(" > add {}",token);
            }
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
    /**
     * Getter for the Tokenizer. This uses the {@link #languageConfig} to
     * check if a specific configuration for the given language is present
     * by checking for the {@link #MODEL_NAME_PARAM}.
     * @param language the language
     * @return the {@link Tokenizer} guaranteed to be not <code>null</code>.
     * @throws EngineException in case a custom configured model is not
     * available or an error occurred during loading.
     */
    private Tokenizer getTokenizer(String language) throws EngineException {
        String modelName = languageConfig.getParameter(language, MODEL_NAME_PARAM);
        if(modelName == null){
            return openNLP.getTokenizer(language);
        } else if(SIMPLE_MODEL_NAME.equals(modelName)){
            return SimpleTokenizer.INSTANCE;
        } else { //try to load the configured model
            TokenizerModel model;
            try {
                model = openNLP.getModel(TokenizerModel.class, modelName, null);
            } catch (Exception e) {
                throw new EngineException("Error while loading the configured OpenNLP "
                    + "TokenizerModel '"+modelName+"' ("+getClass().getSimpleName()+" | name="
                    + getName() + ")!",e);
            }
            if(model == null){
                throw new EngineException("The configured OpenNLP TokenizerModel '"
                        + modelName +" is not available' ("+getClass().getSimpleName()
                        + " | name=" + getName() + ")!");
            }
            return new TokenizerME(model);
        }
    }
    
}

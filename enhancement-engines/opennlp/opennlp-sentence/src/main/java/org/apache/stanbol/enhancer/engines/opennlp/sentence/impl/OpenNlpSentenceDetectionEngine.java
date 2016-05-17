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
package org.apache.stanbol.enhancer.engines.opennlp.sentence.impl;

import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.getLanguage;
import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.initAnalysedText;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

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
import org.apache.stanbol.enhancer.nlp.model.Sentence;
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
 * EnhancementEngine that uses the OpenNLP {@link SentenceDetector} to
 * add {@link Sentence} annotations to the {@link AnalysedText}
 * content part of the parsed {@link ContentItem}.<p>
 * While the opennlp-pos engine does also support adding of {@link Sentence}
 * annotations this engine can be used in cases where no POS tagging is
 * needed. In addition this engine also allows to configure custom
 * {@link SentenceModel}s with by using the {@link #MODEL_NAME_PARAM}
 * with the language configuration
 * <code><pre>
 *     {lang};model={model-name}
 * </pre></code>
 */
@Component(immediate = true, metatype = true, 
    configurationFactory = true, //allow multiple instances
    policy = ConfigurationPolicy.OPTIONAL) //create a default instance with the default configuration
@Service
@Properties(value={
        @Property(name= EnhancementEngine.PROPERTY_NAME,value="opennlp-sentence"),
        @Property(name=OpenNlpSentenceDetectionEngine.CONFIG_LANGUAGES, value = {"*"},cardinality=Integer.MAX_VALUE),
        @Property(name=Constants.SERVICE_RANKING,intValue=-100) //give the default instance a ranking < 0
})
public class OpenNlpSentenceDetectionEngine extends AbstractEnhancementEngine<RuntimeException,RuntimeException> implements ServiceProperties {


    private static final Map<String,Object> SERVICE_PROPERTIES;
    static {
        Map<String,Object> props = new HashMap<String,Object>();
        props.put(ServiceProperties.ENHANCEMENT_ENGINE_ORDERING, 
            ServiceProperties.ORDERING_NLP_SENTENCE_DETECTION);
        props.put(NlpServiceProperties.ENHANCEMENT_ENGINE_NLP_ROLE, 
            NlpProcessingRole.SentenceDetection);
        SERVICE_PROPERTIES = Collections.unmodifiableMap(props);
    }

    /**
     * Language configuration. Takes a list of ISO language codes of supported languages. Currently supported
     * are the languages given as default value.
     */
    public static final String CONFIG_LANGUAGES = "org.apache.stanbol.enhancer.sentence.languages";

    /**
     * The parameter name used to configure the name of the OpenNLP model used for pos tagging
     */
    private static final String MODEL_NAME_PARAM = "model";


    private static Logger log = LoggerFactory.getLogger(OpenNlpSentenceDetectionEngine.class);

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
        if(getSentenceDetector(language) == null){
            log.trace(" > can NOT enhance ContentItem {} because no sentence "
                    + "deteciton model for language {} is available.",ci,language);
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
        SentenceDetector sentenceDetector = getSentenceDetector(language);
        if(sentenceDetector != null){
            for(opennlp.tools.util.Span sentSpan : sentenceDetector.sentPosDetect(at.getSpan())) {
                //detect sentences and add it to the AnalyzedText.
                Sentence sentence = at.addSentence(sentSpan.getStart(), sentSpan.getEnd());
                log.trace(" > add {}",sentence);
            }
        } else {
            log.warn("SentenceDetector model for language {} is no longer available. "
                + "This might happen if the model becomes unavailable during enhancement. "
                + "If this happens more often it might also indicate an bug in the used "
                + "EnhancementJobManager implementation as the availability is also checked "
                + "in the canEnhance(..) method of this Enhancement Engine.");
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
     * Obtains the {@link SentenceDetectorME} model for the given
     * language form the {@link #openNLP} service. If a custom
     * model is configured for the parsed language than it is
     * loaded by using {@link OpenNLP#getModel(Class, String, Map)}
     * otherwise the default model {@link OpenNLP#getSentenceDetector(String)}
     * is retrieved
     * @param language the language
     * @return the model of <code>null</code> if non is available or
     * an exception was encountered while loading
     */
    private SentenceDetector getSentenceDetector(String language) {
        SentenceModel model;
        String modelName = languageConfig.getParameter(language, MODEL_NAME_PARAM);
        if(modelName == null){
            try {
                model = openNLP.getSentenceModel(language);
            } catch (Exception e) {
                log.warn("Unable to load default Sentence Detection model for language '"+language+"'!",e);
                return null;
            }
        } else {
            try {
                model = openNLP.getModel(SentenceModel.class, modelName, null);
            } catch (Exception e) {
                log.warn("Unable to load Sentence Detection model for language '"
                        +language+"' from the configured model '"+modelName+"'!",e);
                return null;
            }
        }
        if(model != null) {
            log.debug("Sentence Detection Model {} for lanugage '{}' version: {}",
                new Object[]{model.getClass().getSimpleName(), 
                             model.getLanguage(), 
                             model.getVersion() != null ? model.getVersion() : "undefined"});
            return new SentenceDetectorME(model);
        }
        log.debug("Sentence Detection Model for Language '{}' not available.", language);
        return null;
    }
        
}

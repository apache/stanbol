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
package org.apache.stanbol.enhancer.engines.celi.lemmatizer.impl;

import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.getAnalysedText;
import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.getLanguage;
import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.isLangaugeConfigured;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.soap.SOAPException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.stanboltools.offline.OnlineMode;
import org.apache.stanbol.enhancer.engines.celi.CeliConstants;
import org.apache.stanbol.enhancer.engines.celi.CeliMorphoFeatures;
import org.apache.stanbol.enhancer.engines.celi.utils.Utils;
import org.apache.stanbol.enhancer.nlp.NlpAnnotations;
import org.apache.stanbol.enhancer.nlp.NlpProcessingRole;
import org.apache.stanbol.enhancer.nlp.NlpServiceProperties;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.morpho.MorphoFeatures;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
import org.apache.stanbol.enhancer.nlp.utils.LanguageConfiguration;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

@Component(immediate = true, metatype = true)
@Service
@Properties(value = { 
    @Property(name = EnhancementEngine.PROPERTY_NAME, value = "celiLemmatizer"), 
    @Property(name = CeliConstants.CELI_LICENSE), 
    @Property(name = CeliConstants.CELI_TEST_ACCOUNT, boolValue = false) ,
    @Property(name = CeliConstants.CELI_CONNECTION_TIMEOUT, intValue=CeliConstants.DEFAULT_CONECTION_TIMEOUT)
})
public class CeliAnalyzedTextLemmatizerEngine extends AbstractEnhancementEngine<IOException, RuntimeException> implements EnhancementEngine, ServiceProperties {

    private static final Map<String,Object> SERVICE_PROPERTIES;
    static {
        Map<String,Object> props = new HashMap<String,Object>();
        props.put(ServiceProperties.ENHANCEMENT_ENGINE_ORDERING, 
            ServiceProperties.ORDERING_NLP_LEMMATIZE);
        props.put(NlpServiceProperties.ENHANCEMENT_ENGINE_NLP_ROLE, 
            NlpProcessingRole.Lemmatize);
        SERVICE_PROPERTIES = Collections.unmodifiableMap(props);
    }
    
    /**
     * This ensures that no connections to external services are made if Stanbol is started in offline mode as the OnlineMode service will only be available if OfflineMode is deactivated.
     */
    @SuppressWarnings("unused")
    @Reference
    private OnlineMode onlineMode;
    
    @Property(value = "http://linguagrid.org/LSGrid/ws/morpho-analyser")
    public static final String SERVICE_URL = "org.apache.stanbol.enhancer.engines.celi.lemmatizer.url";

    @Property(value={"it", "da", "de", "ru", "ro", "sv"})
    public static final String PROPERTY_SUPPORTED_LANGUAGES = "org.apache.stanbol.enhancer.engines.celi.lemmatizer.languages";
    
    private LanguageConfiguration languageConfig = new LanguageConfiguration(
        PROPERTY_SUPPORTED_LANGUAGES, new String[]{"it", "da", "de", "ru","ro","sv"});
    
    private String licenseKey;
    private URL serviceURL;

    private LemmatizerClientHTTP client;
    
    @Override
    @Activate
    protected void activate(ComponentContext ctx) throws IOException, ConfigurationException {
        super.activate(ctx);
        Dictionary<String, Object> properties = ctx.getProperties();
        this.licenseKey = Utils.getLicenseKey(properties, ctx.getBundleContext());
        String url = (String) properties.get(SERVICE_URL);
        if (url == null || url.isEmpty()) {
            throw new ConfigurationException(SERVICE_URL, String.format("%s : please configure the URL of the CELI Web Service (e.g. by" + "using the 'Configuration' tab of the Apache Felix Web Console).", getClass().getSimpleName()));
        }
        this.serviceURL = new URL(url);
        //parse the parsed language configuration
        languageConfig.setConfiguration(properties);
        int conTimeout = Utils.getConnectionTimeout(properties, ctx.getBundleContext());
        this.client = new LemmatizerClientHTTP(this.serviceURL, this.licenseKey,conTimeout);
    }

    @Override
    @Deactivate
    protected void deactivate(ComponentContext ce) {
        languageConfig.setDefault();
        super.deactivate(ce);
    }
    
    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        String language = getLanguage(this, ci,false);
        if(language == null){
            return CANNOT_ENHANCE;
        }
        if(!isLangaugeConfigured(this,languageConfig,language,false)){
           return CANNOT_ENHANCE; 
        }
        if(getAnalysedText(this,ci,false) == null) {
            return CANNOT_ENHANCE;
        }
        // default enhancement is synchronous enhancement
        return ENHANCE_ASYNC;
    }
    
    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        AnalysedText at = getAnalysedText(this, ci, true);
        String language = getLanguage(this, ci, true);
        isLangaugeConfigured(this, languageConfig, language, true);
        List<LexicalEntry> terms;
        try {
            terms = this.client.performMorfologicalAnalysis(at.getSpan(), language);
        } catch (IOException e) {
            throw new EngineException("Error while calling the CELI Lemmatizer" 
                    + " service (configured URL: " + serviceURL + ")!", e);
        } catch (SOAPException e) {
            throw new EngineException("Error wile encoding/decoding the request/" + 
                    "response to the CELI lemmatizer service!", e);
        }
        Map<LexicalCategory,Double> tokenLexCats = new EnumMap<LexicalCategory,Double>(LexicalCategory.class);
        for(LexicalEntry term : terms){
            if(term.getTermReadings().isEmpty()){
                //TODO: maybe still add them and use the Lemmatizer as Tokenizer
                continue; //ignore terms without readings
            }
            //Add the LexicalEntry as Token to the Text. NOTE that if a
            //Token with the same start/end positions already exist this
            //Method returns the existing instance
            Token token = at.addToken(term.getFrom(), term.getTo());
            //Now try to get POS annotations for the Token
            for(Value<PosTag> posAnno : token.getAnnotations(NlpAnnotations.POS_ANNOTATION)){
                if(posAnno.value().isMapped()){
                    for(LexicalCategory cat :posAnno.value().getCategories()){
                        if(!tokenLexCats.containsKey(cat)){ //do not override with lover prob
                            tokenLexCats.put(cat, posAnno.probability());
                        }
                    }
                }
            }
            for(Reading reading : term.getTermReadings()){
                MorphoFeatures mf = CeliMorphoFeatures.parseFrom(reading, language);
                //add the readings (MorphoFeatures)
                if(mf != null){
                    //use the POS tags of the morpho analysis and compare it
                    //with existing POS tags.
                    double posProbability = -1;
                    Set<LexicalCategory> mfCats = EnumSet.noneOf(LexicalCategory.class);
                    for(PosTag mfPos : mf.getPosList()){
                        mfCats.addAll(mfPos.getCategories());
                    }
                    for(LexicalCategory mfCat : mfCats){
                        Double prob = tokenLexCats.get(mfCat);
                        if(prob != null && posProbability < prob){
                            posProbability = prob;
                        }
                    }
                    //add the morpho features with the posProbabiliy
                    Value<MorphoFeatures> value = Value.value(mf,
                        posProbability < 0 ? Value.UNKNOWN_PROBABILITY : posProbability);
                    token.addAnnotation(NlpAnnotations.MORPHO_ANNOTATION, value);
                }
            }
        }
    }
    
    @Override
    public Map<String, Object> getServiceProperties() {
        return SERVICE_PROPERTIES;
    }

}

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
package org.apache.stanbol.enhancer.engines.celi.sentimentanalysis.impl;

import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.getAnalysedText;
import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.getLanguage;
import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.isLangaugeConfigured;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.apache.stanbol.enhancer.engines.celi.utils.Utils;
import org.apache.stanbol.enhancer.nlp.NlpAnnotations;
import org.apache.stanbol.enhancer.nlp.NlpProcessingRole;
import org.apache.stanbol.enhancer.nlp.NlpServiceProperties;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
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
    @Property(name = EnhancementEngine.PROPERTY_NAME, value = "celiSentiment"),
    @Property(name = CeliConstants.CELI_LICENSE),
    @Property(name = CeliConstants.CELI_TEST_ACCOUNT,boolValue=false),
    @Property(name = CeliConstants.CELI_CONNECTION_TIMEOUT, intValue=CeliConstants.DEFAULT_CONECTION_TIMEOUT)
})
public class CeliAnalyzedTextSentimentAnalysisEngine extends AbstractEnhancementEngine<IOException, RuntimeException> implements EnhancementEngine, ServiceProperties {
	
	private static final Map<String,Object> SERVICE_PROPERTIES;
    static {
        Map<String,Object> props = new HashMap<String,Object>();
        props.put(ServiceProperties.ENHANCEMENT_ENGINE_ORDERING, ServiceProperties.ORDERING_POST_PROCESSING);
        props.put(NlpServiceProperties.ENHANCEMENT_ENGINE_NLP_ROLE, NlpProcessingRole.SentimentTagging);
        SERVICE_PROPERTIES = Collections.unmodifiableMap(props);
    }
    
    /**
     * This ensures that no connections to external services are made if Stanbol is started in offline mode as the OnlineMode service will only be available if OfflineMode is deactivated.
     */
    @SuppressWarnings("unused")
    @Reference
    private OnlineMode onlineMode;
	
	@Property(value = "http://linguagrid.org/LSGrid/ws/sentiment-analysis")
	public static final String SERVICE_URL = "org.apache.stanbol.enhancer.engines.celi.celiSentiment.url";

    @Property(value={"it", "fr"})
    public static final String PROPERTY_SUPPORTED_LANGUAGES = "org.apache.stanbol.enhancer.engines.celi.celiSentiment.languages";
    
    private LanguageConfiguration languageConfig = new LanguageConfiguration(
            PROPERTY_SUPPORTED_LANGUAGES, new String[]{"it", "fr"});
    
    private String licenseKey;
    private URL serviceURL;

    private SentimentAnalysisServiceClientHttp client;
    
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
        int connectionTimeout = Utils.getConnectionTimeout(properties, ctx.getBundleContext());
        this.client = new SentimentAnalysisServiceClientHttp(this.serviceURL, this.licenseKey,connectionTimeout);
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
        List<SentimentExpression> seList;
        try {
        	seList = this.client.extractSentimentExpressions(at.getSpan(), language);
        } catch (IOException e) {
            throw new EngineException("Error while calling the CELI Sentiment Analysis service (configured URL: " + serviceURL + ")!", e);
        } catch (SOAPException e) {
            throw new EngineException("Error wile encoding/decoding the request/response to the CELI Sentiment Analysis service!", e);
        }
        
        for(SentimentExpression se : seList){
            //Add the Sentiment Expression as Token to the Text. NOTE that if a Token with the same start/end positions already exist this
            //Method returns the existing instance
            Token token = at.addToken(se.getStartSnippet(),se.getEndSnippet());
            token.addAnnotation(NlpAnnotations.SENTIMENT_ANNOTATION, new Value<Double>(se.getSentimentPolarityAsDoubleValue()) );
        }
    }
    
    @Override
    @Deactivate
    protected void deactivate(ComponentContext ce) {
        languageConfig.setDefault();
        super.deactivate(ce);
    }
    
    @Override
    public Map<String, Object> getServiceProperties() {
        return SERVICE_PROPERTIES;
    }
	
	
}

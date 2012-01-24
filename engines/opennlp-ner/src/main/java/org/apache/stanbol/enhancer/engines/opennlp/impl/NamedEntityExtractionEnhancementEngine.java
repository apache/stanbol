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
package org.apache.stanbol.enhancer.engines.opennlp.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.opennlp.OpenNLP;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.AbstractEnhancementEngine;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

/**
 * Apache Stanbol Enhancer Named Entity Recognition enhancement engine based on opennlp's Maximum Entropy
 * models.
 */
@Component(
    metatype = true, 
    immediate = true,
    inherit = true,
    configurationFactory = true, 
    policy = ConfigurationPolicy.REQUIRE, // the baseUri is required!
    specVersion = "1.1", 
    label = "%stanbol.NamedEntityExtractionEnhancementEngine.name", 
    description = "%stanbol.NamedEntityExtractionEnhancementEngine.description")
@Service
@org.apache.felix.scr.annotations.Properties(value={
    @Property(name=EnhancementEngine.PROPERTY_NAME,value="ner"),
    @Property(name=NamedEntityExtractionEnhancementEngine.PROCESSED_LANGUAGES,value=""),
    @Property(name=NamedEntityExtractionEnhancementEngine.DEFAULT_LANGUAGE,value=""),
    @Property(name=Constants.SERVICE_RANKING,intValue=0)
})
public class NamedEntityExtractionEnhancementEngine 
        extends AbstractEnhancementEngine<IOException,RuntimeException> 
        implements EnhancementEngine, ServiceProperties {

    private EnhancementEngine engineCore;
    
    public static final String DEFAULT_DATA_OPEN_NLP_MODEL_LOCATION = "org/apache/stanbol/defaultdata/opennlp";

    /**
     * Allows to define the default language assumed for parsed Content if no language
     * detection is available. If <code>null</code> or empty this engine will not
     * process content with an unknown language
     */
    public static final String DEFAULT_LANGUAGE = "stanbol.NamedEntityExtractionEnhancementEngine.defaultLanguage";
    /**
     * Allows to restrict the list of languages processed by this engine. if
     * <code>null</code> or empty content of any language where a NER model is
     * available via {@link OpenNLP} will be processed.<p>
     * This property allows to configure multiple instances of this engine that
     * do only process specific languages. The default is a single instance that
     * processes all languages.
     */
    public static final String PROCESSED_LANGUAGES = "stanbol.NamedEntityExtractionEnhancementEngine.processedLanguages";

    /**
     * The default value for the Execution of this Engine. Currently set to
     * {@link ServiceProperties#ORDERING_CONTENT_EXTRACTION}
     */
    public static final Integer defaultOrder = ORDERING_CONTENT_EXTRACTION;

    private ServiceRegistration dfpServiceRegistration;
    
    @Reference
    private OpenNLP openNLP;
    
    protected void activate(ComponentContext ctx) throws IOException, ConfigurationException {
        super.activate(ctx);
        // Need to register the default data before loading the models
        Object value = ctx.getProperties().get(DEFAULT_LANGUAGE);
        final String defaultLanguage;
        if(value != null && !value.toString().isEmpty()){
            defaultLanguage = value.toString();
        } else {
            defaultLanguage = null;
        }
        value = ctx.getProperties().get(PROCESSED_LANGUAGES);
        final Set<String> processedLanguages;
        if(value instanceof String[]){
            processedLanguages = new HashSet<String>(Arrays.asList((String[]) value));
            processedLanguages.remove(null); //remove null
            processedLanguages.remove(""); //remove empty
        } else if (value instanceof Collection<?>){
            processedLanguages = new HashSet<String>();
            for(Object o : ((Collection<?>)value)){
                if(o != null){
                    processedLanguages.add(o.toString());
                }
            }
            processedLanguages.remove(""); //remove empty
        } else if(value != null && !value.toString().isEmpty()){
            //if a single String is parsed we support ',' as seperator
            String[] languageArray = value.toString().split(",");
            processedLanguages = new HashSet<String>(Arrays.asList(languageArray));
            processedLanguages.remove(null); //remove null
            processedLanguages.remove(""); //remove empty
        } else { //no configuration
            processedLanguages = Collections.emptySet();
        }
        if(!processedLanguages.isEmpty() && defaultLanguage != null &&
                !processedLanguages.contains(defaultLanguage)){
            throw new ConfigurationException(PROCESSED_LANGUAGES, "The list of" +
            		"processed Languages "+processedLanguages+" MUST CONTAIN the" +
            		"configured default language '"+defaultLanguage+"'!");
        }
        engineCore = new NEREngineCore(openNLP, defaultLanguage, processedLanguages);
    }

    protected void deactivate(ComponentContext ctx) {
        super.deactivate(ctx);
        if(dfpServiceRegistration != null) {
            dfpServiceRegistration.unregister();
            dfpServiceRegistration = null;
        }
        engineCore = null;
    }
    
    @Override
    public Map<String,Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(ENHANCEMENT_ENGINE_ORDERING,
            (Object) defaultOrder));
    }

    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        checkCore();
        return engineCore.canEnhance(ci);
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        checkCore();
        engineCore.computeEnhancements(ci);
    }
    
    private void checkCore() {
        if(engineCore == null) {
            throw new IllegalStateException("EngineCore not initialized");
        }
    }

}
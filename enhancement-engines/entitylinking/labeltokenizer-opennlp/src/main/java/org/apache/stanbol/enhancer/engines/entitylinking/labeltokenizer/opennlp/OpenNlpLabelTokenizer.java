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
package org.apache.stanbol.enhancer.engines.entitylinking.labeltokenizer.opennlp;


import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.opennlp.OpenNLP;
import org.apache.stanbol.enhancer.engines.entitylinking.LabelTokenizer;
import org.apache.stanbol.enhancer.nlp.utils.LanguageConfiguration;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a LabelTokenizer based on OpenNLP that
 * allows to configure custom Tokenizer models for specific
 * languages.<p>
 * <b>NOTE:</b> This component requires the optional dependency
 * to <code>o.a.stanbol.commons.opennlp</code> as it dependes
 * on the the {@link OpenNLP} service to retrieve {@link Tokenizer} 
 * and load {@link TokenizerModel}s.<p>
 * This component registers itself with a service ranking of <code>-100</code>
 * @author Rupert Westenthaler
 *
 */
@Component(immediate=true)
@Service
@Properties(value={
    @Property(name=Constants.SERVICE_RANKING,intValue=-100),
    @Property(name=LabelTokenizer.SUPPORTED_LANUAGES,value="*")})
public class OpenNlpLabelTokenizer implements LabelTokenizer {

    private final Logger log = LoggerFactory.getLogger(OpenNlpLabelTokenizer.class);
    
    public static final String PARAM_MODEL = "model";
    
    @Reference
    protected OpenNLP openNlp;

    public OpenNlpLabelTokenizer(){}
    
    public OpenNlpLabelTokenizer(OpenNLP openNLP){
        this.openNlp = openNLP;
    }
    
    private LanguageConfiguration languageConfig = new LanguageConfiguration(
        LabelTokenizer.SUPPORTED_LANUAGES, new String[]{"*"});
    
    @Activate
    protected void activate(ComponentContext ctx) throws ConfigurationException {
        languageConfig.setConfiguration(ctx.getProperties());
    }
    @Deactivate
    protected void deactivate(ComponentContext ctx){
        languageConfig.setDefault();
    }
    
    @Override
    public String[] tokenize(String label, String language) {
        if(label == null){
            throw new IllegalArgumentException("The parsed Label MUST NOT be NULL!");
        }
        if(languageConfig.isLanguage(language)){
            String modelName = languageConfig.getParameter(language, PARAM_MODEL);
            if(modelName != null){
                try {
                    TokenizerModel model = openNlp.getModel(TokenizerModel.class, modelName, null);
                    return new TokenizerME(model).tokenize(label);
                } catch (Exception e) {
                    log.warn("Unable to load configured TokenizerModel '"+modelName
                        + "' for language '"+language
                        + "! Fallback to default Tokenizers",e);
                }
            }
            //fallback to the defaults
            return openNlp.getTokenizer(language).tokenize(label);
        } else { //language not configured
            return null;
        }
    }

}

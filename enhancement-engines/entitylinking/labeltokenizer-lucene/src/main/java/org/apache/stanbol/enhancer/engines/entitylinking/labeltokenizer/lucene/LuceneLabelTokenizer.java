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
package org.apache.stanbol.enhancer.engines.entitylinking.labeltokenizer.lucene;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.lucene.analysis.CharFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.util.AbstractAnalysisFactory;
import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.Version;
import org.apache.solr.common.SolrException;
import org.apache.stanbol.commons.solr.utils.StanbolResourceLoader;
import org.apache.stanbol.enhancer.engines.entitylinking.LabelTokenizer;
import org.apache.stanbol.enhancer.nlp.utils.LanguageConfiguration;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Component(
    configurationFactory=true,
    policy=ConfigurationPolicy.REQUIRE,
    metatype=true)
@Properties(value={
        @Property(name=LuceneLabelTokenizer.PROPERTY_CHAR_FILTER_FACTORY,value=LuceneLabelTokenizer.DEFAULT_CLASS_NAME_CONFIG),
        @Property(name=LuceneLabelTokenizer.PROPERTY_TOKENIZER_FACTORY,value=LuceneLabelTokenizer.DEFAULT_CLASS_NAME_CONFIG),
        @Property(name=LuceneLabelTokenizer.PROPERTY_TOKEN_FILTER_FACTORY,cardinality=Integer.MAX_VALUE,value=LuceneLabelTokenizer.DEFAULT_CLASS_NAME_CONFIG),
        @Property(name=LabelTokenizer.SUPPORTED_LANUAGES,value="{lang1},{lang2},!{lang3},{*}"),
        @Property(name=Constants.SERVICE_RANKING,intValue=0)
})
public class LuceneLabelTokenizer implements LabelTokenizer {

    private Logger log = LoggerFactory.getLogger(LuceneLabelTokenizer.class);

    private static final String[] EMPTY = new String[]{};

    @Reference(cardinality=ReferenceCardinality.OPTIONAL_UNARY)
    private ResourceLoader parentResourceLoader;

    protected ResourceLoader resourceLoader;


    public static final String PROPERTY_CHAR_FILTER_FACTORY = "enhancer.engine.linking.labeltokenizer.lucene.charFilterFactory";
    public static final String PROPERTY_TOKENIZER_FACTORY = "enhancer.engine.linking.labeltokenizer.lucene.tokenizerFactory";
    public static final String PROPERTY_TOKEN_FILTER_FACTORY = "enhancer.engine.linking.labeltokenizer.lucene.tokenFilterFactory";

    static final String DEFAULT_CLASS_NAME_CONFIG = "{full-qualified-class-name}";

    private CharFilterFactory charFilterFactory;
    private TokenizerFactory tokenizerFactory;
    private List<TokenFilterFactory> filterFactories = new ArrayList<TokenFilterFactory>();
    private LanguageConfiguration langConf = new LanguageConfiguration(SUPPORTED_LANUAGES, new String[]{});

    @Activate
    protected void activate(ComponentContext ctx) throws ConfigurationException {
        //init the Solr ResourceLoader used for initialising the components
        resourceLoader = new StanbolResourceLoader(parentResourceLoader);
        //init the Solr CharFilterFactory (optional)
        Object value = ctx.getProperties().get(PROPERTY_CHAR_FILTER_FACTORY);
        if(value != null && !value.toString().isEmpty() && !DEFAULT_CLASS_NAME_CONFIG.equals(value)){
            Entry<String,Map<String,String>> charFilterConfig = parseConfigLine(
                PROPERTY_CHAR_FILTER_FACTORY, value.toString());
            Object factoryObject;
            try {
                factoryObject = resourceLoader.newInstance(charFilterConfig.getKey(), Object.class);
            } catch (SolrException e) {
                throw new ConfigurationException(PROPERTY_CHAR_FILTER_FACTORY, "Unable to instantiate the "
                        + "class '"+charFilterConfig.getKey()+"'!", e);
            }

            if(factoryObject instanceof CharFilterFactory){
                charFilterFactory = (CharFilterFactory)factoryObject;
                Map<String,String> config = charFilterConfig.getValue();
                addLuceneMatchVersionIfNotPresent(config, charFilterFactory);
                charFilterFactory.init(config);
                if(factoryObject instanceof ResourceLoaderAware){
                    try {
                        ((ResourceLoaderAware)factoryObject).inform(resourceLoader);
                    } catch (IOException e) {
                        throw new ConfigurationException(PROPERTY_CHAR_FILTER_FACTORY, "Could not load configuration");
                    }
                }
            } else {
                throw new ConfigurationException(PROPERTY_CHAR_FILTER_FACTORY, "The parsed class '"
                        + charFilterConfig.getKey() +"' is not assignable to "+CharFilterFactory.class);
            }
        } else {
            charFilterFactory = null;
        }

        //now initialise the TokenizerFactory (required)
        value = ctx.getProperties().get(PROPERTY_TOKENIZER_FACTORY);
        if(value == null || value.toString().isEmpty() || DEFAULT_CLASS_NAME_CONFIG.equals(value)){
            throw new ConfigurationException(PROPERTY_TOKENIZER_FACTORY,"The class name of the Lucene Tokemizer MUST BE configured");
        }
        Entry<String,Map<String,String>> tokenizerConfig = parseConfigLine(
            PROPERTY_CHAR_FILTER_FACTORY, value.toString());

        Object factoryObject;
        try {
            factoryObject = resourceLoader.newInstance(tokenizerConfig.getKey(), Object.class);
        } catch (SolrException e) {
            throw new ConfigurationException(PROPERTY_TOKENIZER_FACTORY, "Unable to instantiate the "
                    + "class '"+tokenizerConfig.getKey()+"'!", e);
        }

        if(factoryObject instanceof TokenizerFactory){
            tokenizerFactory = (TokenizerFactory)factoryObject;
            Map<String,String> config = tokenizerConfig.getValue();
            addLuceneMatchVersionIfNotPresent(config, tokenizerFactory);
            tokenizerFactory.init(config);
        } else {
            throw new ConfigurationException(PROPERTY_TOKENIZER_FACTORY, "The instance "
                    + factoryObject + "of the parsed parsed class '" + tokenizerConfig.getKey()
                    + "' is not assignable to "+TokenizerFactory.class);
        }
        if(factoryObject instanceof ResourceLoaderAware){
            try {
                ((ResourceLoaderAware)factoryObject).inform(resourceLoader);
            } catch (IOException e) {
                throw new ConfigurationException(PROPERTY_TOKENIZER_FACTORY, "Could not load configuration");
            }
        }

        //initialise the list of Token Filters
        Collection<String> values;
        value = ctx.getProperties().get(PROPERTY_TOKEN_FILTER_FACTORY);
        if(value == null){
            values = Collections.emptyList();
        } else if(value instanceof Collection<?>){
            values = new ArrayList<String>(((Collection<?>)value).size());
            for(Object v : (Collection<Object>)value){
                values.add(v.toString());
            }
        } else if(value instanceof String[]){
            values = Arrays.asList((String[])value);
        } else if(value instanceof String){
            values = Collections.singleton((String)value);
        } else {
            throw new ConfigurationException(PROPERTY_TOKEN_FILTER_FACTORY, "The type '"
                + value.getClass()+"' of the parsed value is not supported (supported are "
                + "Collections, String[] and String values)!");
        }
        for(String filterConfigLine : values){
            if(filterConfigLine == null || filterConfigLine.isEmpty() || DEFAULT_CLASS_NAME_CONFIG.equals(value)){
                continue; //ignore null, empty and the default value
            }
            Entry<String,Map<String,String>> filterConfig = parseConfigLine(
                PROPERTY_CHAR_FILTER_FACTORY, filterConfigLine);
            Object filterFactoryObject;
            try {
                filterFactoryObject = resourceLoader.newInstance(filterConfig.getKey(), Object.class);
            } catch (SolrException e) {
                throw new ConfigurationException(PROPERTY_TOKEN_FILTER_FACTORY, "Unable to instantiate the "
                        + "class '"+filterConfig.getKey()+"'!", e);
            }

            if(filterFactoryObject instanceof TokenFilterFactory){
                TokenFilterFactory tff = (TokenFilterFactory)filterFactoryObject;
                Map<String,String> config = filterConfig.getValue();
                addLuceneMatchVersionIfNotPresent(config,tff);
                tff.init(config);
                filterFactories.add(tff);
            } else {
                throw new ConfigurationException(PROPERTY_TOKEN_FILTER_FACTORY, "The parsed class '"
                        + filterConfig.getKey() +"' is not assignable to "+TokenFilterFactory.class);
            }
            if(filterFactoryObject instanceof ResourceLoaderAware){
                try {
                    ((ResourceLoaderAware)filterFactoryObject).inform(resourceLoader);
                } catch (IOException e) {
                    throw new ConfigurationException(PROPERTY_TOKEN_FILTER_FACTORY, "Could not load configuration");
                }
            }
        }
        //init the language configuration
        value = ctx.getProperties().get(LabelTokenizer.SUPPORTED_LANUAGES);
        if(value == null){
            throw new ConfigurationException(LabelTokenizer.SUPPORTED_LANUAGES, "The language "
                + "configuration MUST BE present!");
        }
        langConf.setConfiguration(ctx.getProperties());
    }

	private void addLuceneMatchVersionIfNotPresent(Map<String, String> config, AbstractAnalysisFactory factory) {
		if(!config.containsKey("luceneMatchVersion")){
		    config.put("luceneMatchVersion", Version.LUCENE_41.toString());
		}
		if(factory.getLuceneMatchVersion() == null){
			factory.setLuceneMatchVersion(Version.LUCENE_41);
		}
	}

    @Deactivate
    protected void deactivate(ComponentContext ctx){
        resourceLoader = null;
        charFilterFactory = null;
        tokenizerFactory = null;
        filterFactories.clear();
        langConf.setDefault();
    }

    @Override
    public String[] tokenize(String label, String language) {
        if(label == null){
            throw new IllegalArgumentException("The parsed label MUST NOT be NULL!");
        }
        if((language == null && langConf.useWildcard()) ||
                langConf.isLanguage(language)){
            if(label.isEmpty()){
                return EMPTY;
            }
            Reader reader = new StringReader(label);
            TokenStream tokenizer;
            if(charFilterFactory != null){
                tokenizer = tokenizerFactory.create(charFilterFactory.create(
                		reader));
            } else {
                tokenizer = tokenizerFactory.create(reader);
            }
            //build the analysing chain
            for(TokenFilterFactory filterFactory : filterFactories){
                tokenizer = filterFactory.create(tokenizer);
            }
            List<String> tokens = new ArrayList<String>(8);
            try {
                tokenizer.reset();
                while(tokenizer.incrementToken()){
                    OffsetAttribute offset = tokenizer.addAttribute(OffsetAttribute.class);
                    tokens.add(label.substring(offset.startOffset(), offset.endOffset()));
                }
                tokenizer.end();
                tokenizer.close();
            } catch (IOException e) {
                log.error("IOException while reading from a StringReader :(",e);
                return null;
            }
            return tokens.toArray(new String[tokens.size()]);
        } else {
            log.trace("Language {} not configured to be supported",language);
            return null;
        }

    }
    /**
     * Parses the configured component including parameters formatted like
     * <code><pre>
     *     {component};{param}={value};{param1}={value1};
     * </pre></code>
     * This can be used to parse the same configuration as within the XML schema
     *
     * @param property
     * @param line
     * @return
     * @throws ConfigurationException
     */
    private Entry<String,Map<String,String>> parseConfigLine(String property, String line) throws ConfigurationException {
        line = line.trim();
        int sepIndex = line.indexOf(';');
        String component = sepIndex < 0 ? line : line.substring(0, sepIndex).trim();
        if(component.isEmpty()){
            throw new ConfigurationException(property, "The component name MUST NOT be NULL "
                + "(illegal formatted line: '"+line+"')!");
        }
        return Collections.singletonMap(component,sepIndex >= 0 && sepIndex < line.length()-2 ?
                        parseParameters(property,line.substring(sepIndex+1, line.length()).trim()) :
                            new HashMap<String,String>()).entrySet().iterator().next();
    }

    /**
     * Parses optional parameters <code>{key}[={value}];{key2}[={value2}]</code>. Using
     * the same key multiple times will override the previouse value
     * @param property the proeprty (used for throwing {@link ConfigurationException})
     * @param paramString the parameter string
     * @return
     * @throws ConfigurationException
     */
    private Map<String,String> parseParameters(String property,String paramString) throws ConfigurationException {
        Map<String,String> params = new HashMap<String,String>();
        for(String param : paramString.split(";")){
            param = param.trim();
            int equalsPos = param.indexOf('=');
            if(equalsPos == 0){
                throw new ConfigurationException(property,
                    "Parameter '"+param+"' has empty key!");
            }
            String key = equalsPos > 0 ? param.substring(0, equalsPos).trim() : param;
            String value;
            if(equalsPos > 0){
                if(equalsPos < param.length()-2) {
                    value = param.substring(equalsPos+1).trim();
                } else {
                    value = "";
                }
            } else {
                value = null;
            }
            params.put(key, value);
        }
        return params.isEmpty() ? new HashMap<String,String>() : params;
    }
}

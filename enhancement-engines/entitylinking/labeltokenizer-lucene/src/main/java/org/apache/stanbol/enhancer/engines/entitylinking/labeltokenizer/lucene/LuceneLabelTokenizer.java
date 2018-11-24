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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
            charFilterFactory = initAnalyzer(PROPERTY_CHAR_FILTER_FACTORY, 
                charFilterConfig.getKey(), CharFilterFactory.class, 
                charFilterConfig.getValue());
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
        tokenizerFactory = initAnalyzer(PROPERTY_TOKENIZER_FACTORY, 
            tokenizerConfig.getKey(), TokenizerFactory.class, 
            tokenizerConfig.getValue());

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
            if(filterConfigLine == null || filterConfigLine.isEmpty() || DEFAULT_CLASS_NAME_CONFIG.equals(filterConfigLine)){
                continue; //ignore null, empty and the default value
            }
            Entry<String,Map<String,String>> filterConfig = parseConfigLine(
                PROPERTY_CHAR_FILTER_FACTORY, filterConfigLine);
            TokenFilterFactory tff = initAnalyzer(PROPERTY_TOKEN_FILTER_FACTORY, 
                filterConfig.getKey(), TokenFilterFactory.class, 
                filterConfig.getValue());
            filterFactories.add(tff);
        }
        //init the language configuration
        value = ctx.getProperties().get(LabelTokenizer.SUPPORTED_LANUAGES);
        if(value == null){
            throw new ConfigurationException(LabelTokenizer.SUPPORTED_LANUAGES, "The language "
                + "configuration MUST BE present!");
        }
        langConf.setConfiguration(ctx.getProperties());
    }

	private static void addLuceneMatchVersionIfNotPresent(Map<String, String> config) {
		if(!config.containsKey("luceneMatchVersion")){
		    config.put("luceneMatchVersion", Version.LUCENE_44.toString());
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
            } catch (IOException e) {
                log.error("IOException while reading from a StringReader :(",e);
                return null;
            } finally {
                try {
                    if (tokenizer == null) {
                        return null;
                    }
                    tokenizer.close();
                } catch (IOException e) {
                    log.error("IOException while closing a StringReader :(",e);
                    return null;
                }
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
    
    private <T> T initAnalyzer(String property, String analyzerName, Class<T> type, Map<String,String> config)
        throws ConfigurationException {
        Class<? extends T> analyzerClass;
        try {
            analyzerClass = resourceLoader.findClass(analyzerName, type);
        } catch (SolrException e) {
            throw new ConfigurationException(PROPERTY_CHAR_FILTER_FACTORY, "Unable find "
                + type.getSimpleName()+ " '" + analyzerName+"'!", e);
        }
        Constructor<? extends T> constructor;
        try {
            constructor = analyzerClass.getConstructor(Map.class);
        } catch (NoSuchMethodException e1) {
            throw new ConfigurationException(PROPERTY_CHAR_FILTER_FACTORY, "Unable find "
                + type.getSimpleName()+ "constructor with parameter Map<String,String> "
                + "for class " + analyzerClass +" (analyzer: '"+analyzerName+"') !");
        }
        addLuceneMatchVersionIfNotPresent(config);
        T analyzer;
        try {
            analyzer = constructor.newInstance(config);
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(property, "Unable to instantiate "
                +type.getSimpleName()+' '+ analyzerClass +" (analyzer: "+analyzerName+"') !",e);
        } catch (InstantiationException e) {
            throw new ConfigurationException(property, "Unable to instantiate "
                    +type.getSimpleName()+' '+ analyzerClass +" (analyzer: "+analyzerName+"') !",e);
        } catch (IllegalAccessException e) {
            throw new ConfigurationException(property, "Unable to instantiate "
                    +type.getSimpleName()+' '+ analyzerClass +" (analyzer: "+analyzerName+"') !",e);
        } catch (InvocationTargetException e) {
            throw new ConfigurationException(property, "Unable to instantiate "
                    +type.getSimpleName()+' '+ analyzerClass +" (analyzer: "+analyzerName+"') !",e);
        }
        if(analyzer instanceof ResourceLoaderAware){
            try {
                ((ResourceLoaderAware)analyzer).inform(resourceLoader);
            } catch (IOException e) {
                throw new ConfigurationException(PROPERTY_CHAR_FILTER_FACTORY, "Could not load configuration");
            }
        }
        return analyzer;
    }
    
    
}

package org.apache.stanbol.enhancer.engines.entityhublinking.labeltokenizer.lucene;


import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.util.Version;
import org.apache.solr.analysis.TokenFilterFactory;
import org.apache.solr.analysis.TokenizerFactory;
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
        @Property(name=LuceneLabelTokenizer.PROPERTY_TOKENIZER_FACTORY,value="{full-qualified-class-name}"),
        @Property(name=LuceneLabelTokenizer.PROPERTY_TOKEN_FILTER_FACTORY,cardinality=Integer.MAX_VALUE,value=""),
        @Property(name=LabelTokenizer.SUPPORTED_LANUAGES,value="{lang1},{lang2},!{lang3},{*}"),
        @Property(name=Constants.SERVICE_RANKING,intValue=0)
})
public class LuceneLabelTokenizer implements LabelTokenizer {
    
    Logger log = LoggerFactory.getLogger(LuceneLabelTokenizer.class);

    private static final String[] EMPTY = new String[]{};
    
    public static final String PROPERTY_TOKENIZER_FACTORY = "enhancer.engine.linking.labeltokenizer.lucene.tokenizerFactory";
    public static final String PROPERTY_TOKEN_FILTER_FACTORY = "enhancer.engine.linking.labeltokenizer.lucene.tokenFilterFactory";
    private TokenizerFactory tokenizerFactory;
    private List<TokenFilterFactory> filterFactories = new ArrayList<TokenFilterFactory>();
    private LanguageConfiguration langConf = new LanguageConfiguration(SUPPORTED_LANUAGES, new String[]{});
    
    @Activate
    protected void activate(ComponentContext ctx) throws ConfigurationException {
        //init the Solr TokenizerFactory
        Object value = ctx.getProperties().get(PROPERTY_TOKENIZER_FACTORY);
        if(value == null || value.toString().isEmpty()){
            throw new ConfigurationException(PROPERTY_TOKENIZER_FACTORY,"The class name of the Lucene Tokemizer MUST BE configured");
        }
        Class<?> tokenizerFactoryClass;
        try {
            tokenizerFactoryClass = getClass().getClassLoader().loadClass(value.toString());
            log.info(" ... adding {}",tokenizerFactoryClass.getSimpleName());
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException(PROPERTY_TOKENIZER_FACTORY, "Unable to load the "
                + "class for the parsed name '"+value+"'!");
        }
        Object factoryObject;
        try {
            factoryObject = tokenizerFactoryClass.newInstance();
        } catch (InstantiationException e) {
            throw new ConfigurationException(PROPERTY_TOKENIZER_FACTORY, "Unable to instantiate the "
                    + "class '"+tokenizerFactoryClass+"'!", e);
        } catch (IllegalAccessException e) {
            throw new ConfigurationException(PROPERTY_TOKENIZER_FACTORY, "Unable to instantiate the "
                    + "class '"+tokenizerFactoryClass+"'!", e);
        }
        
        if(factoryObject instanceof TokenizerFactory){
            tokenizerFactory = (TokenizerFactory)factoryObject;
            tokenizerFactory.init(Collections.singletonMap("luceneMatchVersion", Version.LUCENE_36.toString()));
        } else {
            throw new ConfigurationException(PROPERTY_TOKENIZER_FACTORY, "The parsed class '"
                    + tokenizerFactoryClass +"' is not assignable to "+TokenizerFactory.class);
        }
        Collection<String> values;
        value = ctx.getProperties().get(PROPERTY_TOKEN_FILTER_FACTORY);
        if(value == null){
            values = Collections.emptyList();
        } else if(value instanceof Collection<?>){
            values = new ArrayList<String>(((Collection<?>)value).size());
            for(Object v : (Collection<Object>)value){
                if(v != null && !v.toString().isEmpty()){
                    values.add(v.toString());
                }
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
        for(String filterClassName : values){
            Class<?> tokenFilterFactoryClass;
            try {
                tokenFilterFactoryClass = getClass().getClassLoader().loadClass(filterClassName);
                log.info(" ... adding {}",tokenFilterFactoryClass.getSimpleName());
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException(PROPERTY_TOKEN_FILTER_FACTORY, "Unable to load the "
                    + "class for the parsed name '"+filterClassName+"'!");
            }
            Object filterFactoryObject;
            try {
                filterFactoryObject = tokenFilterFactoryClass.newInstance();
            } catch (InstantiationException e) {
                throw new ConfigurationException(PROPERTY_TOKEN_FILTER_FACTORY, "Unable to instantiate the "
                        + "class '"+tokenFilterFactoryClass+"'!", e);
            } catch (IllegalAccessException e) {
                throw new ConfigurationException(PROPERTY_TOKEN_FILTER_FACTORY, "Unable to instantiate the "
                        + "class '"+tokenFilterFactoryClass+"'!", e);
            }
            
            if(filterFactoryObject instanceof TokenFilterFactory){
                TokenFilterFactory tff = (TokenFilterFactory)filterFactoryObject;
                tff.init(Collections.singletonMap("luceneMatchVersion", Version.LUCENE_36.toString()));
                filterFactories.add(tff);
            } else {
                throw new ConfigurationException(PROPERTY_TOKEN_FILTER_FACTORY, "The parsed class '"
                        + tokenFilterFactoryClass +"' is not assignable to "+TokenFilterFactory.class);
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
    
    @Deactivate
    protected void deactivate(ComponentContext ctx){
        tokenizerFactory = null;
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
            //build the analysing chain
            TokenStream tokenizer = tokenizerFactory.create(new StringReader(label));
            for(TokenFilterFactory filterFactory : filterFactories){
                tokenizer = filterFactory.create(tokenizer); 
            }
            List<String> tokens = new ArrayList<String>(8);
            try {
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

}

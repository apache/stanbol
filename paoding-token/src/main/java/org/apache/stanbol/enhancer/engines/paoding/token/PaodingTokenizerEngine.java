package org.apache.stanbol.enhancer.engines.paoding.token;

import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.getLanguage;
import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.initAnalysedText;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.paoding.analysis.analyzer.PaodingAnalyzer;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.commons.io.input.CharSequenceReader;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.util.Version;
import org.apache.stanbol.enhancer.nlp.NlpProcessingRole;
import org.apache.stanbol.enhancer.nlp.NlpServiceProperties;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextFactory;
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

@Component(immediate = true, metatype = true, 
policy = ConfigurationPolicy.OPTIONAL) //create a default instance with the default configuration
@Service
@Properties(value={
    @Property(name= EnhancementEngine.PROPERTY_NAME,value="paoding-token"),
    @Property(name=Constants.SERVICE_RANKING,intValue=0) //give the default instance a ranking < 0
})
public class PaodingTokenizerEngine extends AbstractEnhancementEngine<RuntimeException,RuntimeException> implements ServiceProperties {

    private Logger log = LoggerFactory.getLogger(PaodingTokenizerEngine.class);

    /*
     * Analyzer configuration constants
     */
    private static final String LUCENE_VERSION = Version.LUCENE_36.toString();
    private static final Map<String,String> CHAR_FILTER_FACTORY_CONFIG = new HashMap<String,String>();
    private static final Map<String,String> TOKENIZER_FACTORY_CONFIG = new HashMap<String,String>();
    static {
        CHAR_FILTER_FACTORY_CONFIG.put("luceneMatchVersion", LUCENE_VERSION);
        CHAR_FILTER_FACTORY_CONFIG.put("mapping", "gosen-mapping-japanese.txt");
        TOKENIZER_FACTORY_CONFIG.put("luceneMatchVersion", LUCENE_VERSION);
    }
    
    /**
     * Service Properties of this Engine
     */
    private static final Map<String,Object> SERVICE_PROPERTIES;
    static {
        Map<String,Object> props = new HashMap<String,Object>();
        props.put(ServiceProperties.ENHANCEMENT_ENGINE_ORDERING, 
            ServiceProperties.ORDERING_NLP_TOKENIZING);
        props.put(NlpServiceProperties.ENHANCEMENT_ENGINE_NLP_ROLE, 
            NlpProcessingRole.Tokenizing);
        SERVICE_PROPERTIES = Collections.unmodifiableMap(props);
    }
    
    @Reference
    protected AnalysedTextFactory analysedTextFactory;
    
    @Override
    protected void activate(ComponentContext ctx) throws ConfigurationException {
        super.activate(ctx);
        //init the Solr ResourceLoader used for initialising the components
    }

    @Override
    protected void deactivate(ComponentContext ctx) {
        super.deactivate(ctx);
    }
    
    
    
    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        // check if content is present
        Map.Entry<UriRef,Blob> entry = NlpEngineHelper.getPlainText(this, ci, false);
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
        PaodingAnalyzer pa;
        try {
            pa = AccessController.doPrivileged(new PrivilegedExceptionAction<PaodingAnalyzer>() {
                public PaodingAnalyzer run() throws Exception {
                    return new PaodingAnalyzer();
                }
            });
        } catch (PrivilegedActionException pae){
            Exception e = pae.getException();
            log.error("Unable to initialise PoadingAnalyzer",e);
            throw new EngineException("Unable to initialise PoadingAnalyzer",e);
        }
        TokenStream ts = pa.tokenStream("dummy", new CharSequenceReader(at.getText()));
        int lastEnd = 0;
        try {
            while(ts.incrementToken()){
                OffsetAttribute offset = ts.addAttribute(OffsetAttribute.class);
                //when tokenizing labels we need to preserve all chars
                if(offset.startOffset() > lastEnd){ //add token for stopword
                    at.addToken(lastEnd,offset.startOffset());
                }
                at.addToken(offset.startOffset(), offset.endOffset());
                lastEnd = offset.endOffset();
            }
        } catch (IOException e) {
            log.warn("IOException while reading the parsed Text",e);
            throw new EngineException("IOException while reading the parsed Text",e);
        }
    }

    @Override
    public Map<String,Object> getServiceProperties() {
        return SERVICE_PROPERTIES;
    }
    
}

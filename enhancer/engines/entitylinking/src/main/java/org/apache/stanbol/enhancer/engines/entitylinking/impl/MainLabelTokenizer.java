package org.apache.stanbol.enhancer.engines.entitylinking.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.engines.entitylinking.LabelTokenizer;
import org.apache.stanbol.enhancer.nlp.utils.LanguageConfiguration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate=true)
@Service
@Properties(
    value={ //Ensure that his LabelTokenizer is highest priority
           @Property(name=Constants.SERVICE_RANKING,intValue=Integer.MAX_VALUE)
})
public class MainLabelTokenizer implements LabelTokenizer {

    private static final String[] DEFAULT_LANG_CONF = new String[]{"*"};

    private final Logger log = LoggerFactory.getLogger(MainLabelTokenizer.class);
    
    private ServiceTracker labelTokenizerTracker;
    
    private static final Comparator<ServiceReference> RANKING_COMPARATOR = new Comparator<ServiceReference>() {
        
        public int compare(ServiceReference ref1, ServiceReference ref2) {
            int r1,r2;
            Object tmp = ref1.getProperty(Constants.SERVICE_RANKING);
            r1 = tmp != null ? ((Integer)tmp).intValue() : 0;
            tmp = (Integer)ref2.getProperty(Constants.SERVICE_RANKING);
            r2 = tmp != null ? ((Integer)tmp).intValue() : 0;
            if(r1 == r2){
                tmp = (Long)ref1.getProperty(Constants.SERVICE_ID);
                long id1 = tmp != null ? ((Long)tmp).longValue() : Long.MAX_VALUE;
                tmp = (Long)ref2.getProperty(Constants.SERVICE_ID);
                long id2 = tmp != null ? ((Long)tmp).longValue() : Long.MAX_VALUE;
                //the lowest id must be first -> id1 < id2 -> [id1,id2] -> return -1
                return id1 < id2 ? -1 : id2 == id1 ? 0 : 1; 
            } else {
                //the highest ranking MUST BE first -> r1 < r2 -> [r2,r1] -> return 1
                return r1 < r2 ? 1:-1;
            }
        }        
    };
    
    private Map<ServiceReference,LanguageConfiguration> ref2LangConfig = 
            Collections.synchronizedMap(new HashMap<ServiceReference,LanguageConfiguration>());
    
    /**
     * Lazily initialized keys based on requested languages.
     * Cleared every time when {@link #ref2LangConfig} changes.
     */
    private Map<String,List<ServiceReference>> langTokenizers = 
            Collections.synchronizedMap(new HashMap<String,List<ServiceReference>>());
    
    
    @Activate
    protected void activate(ComponentContext ctx){
        final BundleContext bundleContext = ctx.getBundleContext();
        final String managerServicePid = (String)ctx.getProperties().get(Constants.SERVICE_PID);
        labelTokenizerTracker = new ServiceTracker(bundleContext, 
            LabelTokenizer.class.getName(), 
            new ServiceTrackerCustomizer() {
                
                @Override
                public Object addingService(ServiceReference reference) {
                    if(managerServicePid.equals(reference.getProperty(Constants.SERVICE_PID))){
                        return null; //do not track this manager!
                    }
                    LanguageConfiguration langConf = new LanguageConfiguration(SUPPORTED_LANUAGES, DEFAULT_LANG_CONF);
                    try {
                        langConf.setConfiguration(reference);
                    } catch (ConfigurationException e) {
                        log.error("Unable to track ServiceReference {} becuase of invalid LanguageConfiguration("
                            + SUPPORTED_LANUAGES+"="+reference.getProperty(SUPPORTED_LANUAGES)+")!",e);
                        return null;
                    }
                    Object service = bundleContext.getService(reference);
                    if(service != null){
                        ref2LangConfig.put(reference, langConf);
                        langTokenizers.clear();
                    }
                    return service;
                }


                @Override
                public void modifiedService(ServiceReference reference, Object service) {
                    if(managerServicePid.equals(reference.getProperty(Constants.SERVICE_PID))){
                        return; //ignore this service!
                    }
                    LanguageConfiguration langConf = new LanguageConfiguration(SUPPORTED_LANUAGES, DEFAULT_LANG_CONF);
                    try {
                        langConf.setConfiguration(reference);
                        ref2LangConfig.put(reference, langConf);
                        langTokenizers.clear();
                    } catch (ConfigurationException e) {
                        log.error("Unable to track ServiceReference {} becuase of invalid LanguageConfiguration("
                            + SUPPORTED_LANUAGES+"="+reference.getProperty(SUPPORTED_LANUAGES)+")!",e);
                        if(ref2LangConfig.remove(reference) != null){
                            langTokenizers.clear();
                        }
                    }
                }


                @Override
                public void removedService(ServiceReference reference, Object service) {
                    if(managerServicePid.equals(reference.getProperty(Constants.SERVICE_PID))){
                        return; //ignore this service
                    }
                    bundleContext.ungetService(reference);
                    if(ref2LangConfig.remove(reference) != null){
                        langTokenizers.clear();
                    }
                }
            });
        labelTokenizerTracker.open();
    }
    
    
    @Deactivate
    protected void deactivate(ComponentContext ctx){
        if(labelTokenizerTracker != null){
            labelTokenizerTracker.close();
            labelTokenizerTracker = null;
        }
    }
    /**
     * Getter for the Servcice based on a Service Refernece
     * @param ref
     * @return
     */
    public LabelTokenizer getService(ServiceReference ref){
        return (LabelTokenizer) labelTokenizerTracker.getService();
    }
    /**
     * Getter for the list of {@link ServiceReference}s for all
     * tracked {@link LabelTokenizer} supporting the parsed language.
     * Entries in the List are sorted by "service.ranking"
     * @param language
     * @return
     */
    public List<ServiceReference> getTokenizers(String language){
        List<ServiceReference> langTokenizers = this.langTokenizers.get(language);
        if(langTokenizers == null ){
            langTokenizers = initTokenizers(language);
        }
        return langTokenizers;
    }

    
    private List<ServiceReference> initTokenizers(String language) {
        List<ServiceReference> tokenizers = new ArrayList<ServiceReference>();
        if(labelTokenizerTracker.getServiceReferences() != null){
            for(ServiceReference ref : labelTokenizerTracker.getServiceReferences()){
                LanguageConfiguration langConf = ref2LangConfig.get(ref);
                if(langConf != null && langConf.isLanguage(language)){
                    tokenizers.add(ref);
                }
            }
        }
        if(tokenizers.size() > 1){
            Collections.sort(tokenizers,RANKING_COMPARATOR);
        }
        this.langTokenizers.put(language, tokenizers);
        return tokenizers;
    }
    
    /* (non-Javadoc)
     * @see org.apache.stanbol.enhancer.engines.keywordextraction.impl.LabelTokenizerManager#tokenize(java.lang.String, java.lang.String)
     */
    @Override
    public String[] tokenize(String label,String language){
        for(ServiceReference ref : getTokenizers(language)){
            LabelTokenizer tokenizer = (LabelTokenizer)labelTokenizerTracker.getService(ref);
            if(tokenizer != null){
                log.trace(" > use Tokenizer {} for language {}",tokenizer.getClass(),language);
                String[] tokens = tokenizer.tokenize(label, language);
                if(tokens != null){
                    if(log.isTraceEnabled()){
                        log.trace("   - tokenized {} -> {}",label, Arrays.toString(tokens));
                    }
                    return tokens;
                }
            }
        }
        log.warn("No LabelTokenizer availabel for language {} -> return null",language);
        return null;
    }
    
}

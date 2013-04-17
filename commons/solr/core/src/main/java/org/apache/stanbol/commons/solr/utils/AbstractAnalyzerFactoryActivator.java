package org.apache.stanbol.commons.solr.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceConfigurationError;

import org.apache.lucene.analysis.util.AbstractAnalysisFactory;
import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.SPIClassIterator;
import org.apache.lucene.util.Version;
import org.apache.solr.core.CoreContainer;
import org.apache.stanbol.commons.solr.SolrConstants;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link BundleActivator} that initialises all {@link CharFilterFactory},
 * {@link TokenizerFactory} and {@link TokenFilterFactory} implementations
 * present in the current module as OSGI services. <p>
 * Users need to extend this class, but do not need to provide any additional
 * functionality.
 * @author Rupert Westenthaler
 *
 */
public abstract class AbstractAnalyzerFactoryActivator implements BundleActivator {

    private static Logger log = LoggerFactory.getLogger(AbstractAnalyzerFactoryActivator.class);

    public static final Map<Class<? extends AbstractAnalysisFactory>, String[]> SUFFIXES;
    
    static { //Defaults are based on the source code of Solr 4.1
        Map<Class<? extends AbstractAnalysisFactory>, String[]> suffixes = 
                new HashMap<Class<? extends AbstractAnalysisFactory>,String[]>();
        suffixes.put(TokenFilterFactory.class, new String[] { 
            TokenFilterFactory.class.getSimpleName(), "FilterFactory" });
        SUFFIXES = Collections.unmodifiableMap(suffixes);
    }

    private ClassLoader classLoader;
    private List<ServiceRegistration> charFilterFactoryRegistrations;
    private List<ServiceRegistration> tokenizerFactoryRegistrations;
    private List<ServiceRegistration> tokenFilterFactoryRegistrations;
    
    
    protected AbstractAnalyzerFactoryActivator(){
        this.classLoader = getClass().getClassLoader();
    }
    
    @Override
    public void start(BundleContext context) throws Exception {
        //we need to reset the context ClassLoader to avoid leaking of Solr
        //versions present in the System (when Stanbol is running in an embedded
        //OSGI environment)
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(CoreContainer.class.getClassLoader());
            charFilterFactoryRegistrations = registerAnalyzerFactories(context, 
                classLoader, CharFilterFactory.class);
            tokenizerFactoryRegistrations = registerAnalyzerFactories(context, 
                classLoader, TokenizerFactory.class);
            tokenFilterFactoryRegistrations = registerAnalyzerFactories(context, 
                classLoader, TokenFilterFactory.class);
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        for(ServiceRegistration sr : charFilterFactoryRegistrations){
            sr.unregister();
        }
        for(ServiceRegistration sr : tokenizerFactoryRegistrations){
            sr.unregister();
        }
        for(ServiceRegistration sr : tokenFilterFactoryRegistrations){
            sr.unregister();
        }

    }
    
    /**
     * Helper class that registers Lucene 4 {@link AbstractAnalysisFactory} instances
     * with the OSGI service registry.
     * <p>
     * It uses the {@link SPIClassIterator} to load instances from the provided
     * {@link ClassLoader}. Note that only factories noted in <code>META-INF/serivces</code>
     * files embedded within the current module will be found and registered.
     * This means that this code needs typically be used in the Bundle Activator for 
     * all modules providing Solr analyzer factories.     
     * @param bc The BundleContext used to register the services
     * @param classloader the classloader of the current modlue
     * @param type the type of the Factories to register
     * @return the ServiceRegistrations for the found factories
     */
    protected <S extends AbstractAnalysisFactory> List<ServiceRegistration> registerAnalyzerFactories(BundleContext bc, ClassLoader classloader, Class<S> type){
        //this code is based on org.apache.lucene.analysis.util.AnalysisSPILoader
        //but registers the loaded classes as services to the OSGI environment
        SPIClassIterator<S> loader = SPIClassIterator.get(type, classloader);
        String[] suffixes = SUFFIXES.get(type);
        if(suffixes == null){
            suffixes = new String[]{type.getSimpleName()};
        }
        List<ServiceRegistration> registrations = new ArrayList<ServiceRegistration>();
        log.debug("Register {} for Bundle {}", type.getSimpleName(), bc.getBundle().getSymbolicName());
        while (loader.hasNext()) {
            final Class<? extends S> service = loader.next();
            final String clazzName = service.getSimpleName();
            String name = null;
            for (String suffix : suffixes) {
              if (clazzName.endsWith(suffix)) {
                name = clazzName.substring(0, clazzName.length() - suffix.length()).toLowerCase(Locale.ROOT);
                break;
              }
            }
            if (name == null) {
              throw new ServiceConfigurationError("The class name " + service.getName() +
                " has wrong suffix, allowed are: " + Arrays.toString(suffixes));
            }
            AbstractAnalysisFactory factory;
            try {
                factory = service.newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException("SPI class of type "+ type.getName()
                    + " with name '"+name+"' cannot be instantiated. This is likely "
                    + "due to a misconfiguration of the java class '" 
                    + service.getName() + "': ", e);
            }
            Dictionary<String,Object> prop = new Hashtable<String,Object>();
            prop.put(SolrConstants.PROPERTY_ANALYZER_FACTORY_NAME,name);
            Version version = factory.getLuceneMatchVersion();
            if(version != null){
                prop.put(SolrConstants.PROPERTY_LUCENE_MATCH_VERSION, version.name());
            }
            //use 0 - bundle id as service ranking. This ensures that if two
            //factories do use the same name the one provided by the bundle with the
            //lower id is used by default
            int serviceRanking = 0 - (int)bc.getBundle().getBundleId();
            prop.put(Constants.SERVICE_RANKING, serviceRanking);
            log.debug(" ... {} (name={})",service.getName(),name);
            registrations.add(bc.registerService(type.getName(), factory, prop));
        }
        return registrations;
    }
}

#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.rdf.ontologies.DCTERMS;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(configurationFactory = true, //allow multiple service instances (false for a  singelton instance)
    policy = ConfigurationPolicy.OPTIONAL, //use REQUIRE if a non default option is present
    immediate = true, //activate service instances on startup 
    metatype = true, inherit = true, specVersion = "1.1")
@Service //this will register the engine as an OSGI service
@Properties(value = { //Configuration properties included in the config form
    @Property(name = EnhancementEngine.PROPERTY_NAME, value = "${artifactId}-example"),
    @Property(name = Constants.SERVICE_RANKING, intValue = 0)
})
public class ExampleEnhancer extends AbstractEnhancementEngine<RuntimeException,RuntimeException>
        implements EnhancementEngine, ServiceProperties {

    /**
     * Using slf4j for logging
     */
    private static final Logger log = LoggerFactory.getLogger(ExampleEnhancer.class);

    /**
     * Configuration property for option 1
     */
    @Property(value=ExampleEnhancer.DEFAULT_OPTION1_VALUE)
    public static final String EXAMPLE_CONFIG_OPTION1 = "${package}.option1";
    /**
     * The default value for EXAMPLE_CONFIG_OPTION1
     */
    public static final String DEFAULT_OPTION1_VALUE = "value1";
    /**
     * The value of option1
     */
    private String option1;

    /**
     * TODO: change to fit your engine. See constants defined in the 
     * ServiceProperties class
     */
    protected static final Integer ENGINE_ORDERING = ServiceProperties.ORDERING_PRE_PROCESSING;
    
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
     * OSGI lifecycle methods
     * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
     */
    /**
     * Activate and read the properties. Configures and initialises a POSTagger for each language configured in
     * CONFIG_LANGUAGES.
     *
     * @param ce the {@link org.osgi.service.component.ComponentContext}
     */
    @Activate
    protected void activate(ComponentContext ce) throws ConfigurationException {
        super.activate(ce);
        log.info("activating {}: {}", getClass().getSimpleName(), getName());
        @SuppressWarnings("unchecked")
        Dictionary<String, Object> properties = ce.getProperties();
        //TODO: parse custom properties

        //As Example we parse EXAMPLE_CONFIG_OPTION1 form the config
        Object value = properties.get(EXAMPLE_CONFIG_OPTION1);
        if(value == null || value.toString().isEmpty()){
            option1 = DEFAULT_OPTION1_VALUE;
        } else {
            option1 = value.toString();
        }
    }
    
    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("deactivating {}: {}", getClass().getSimpleName(), getName());
        //TODO: reset fields to default, close resources ...
        option1 = null;
        
        super.deactivate(context); //call deactivate on the super class
    }
    
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
     * ServiceProperties interface method
     * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
     */
    
    /**
     * ServiceProperties are currently only used for automatic ordering of the 
     * execution of EnhancementEngines (e.g. by the WeightedChain implementation).
     * Default ordering means that the engine is called after all engines that
     * use a value < {@link ServiceProperties#ORDERING_CONTENT_EXTRACTION}
     * and >= {@link ServiceProperties#ORDERING_EXTRACTION_ENHANCEMENT}.
     */
    public Map<String,Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.<String,Object>singletonMap(
                ENHANCEMENT_ENGINE_ORDERING, ENGINE_ORDERING));
    }
    
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
     * EnhancementEngine interface methods
     * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
     */

    /**
     * @return if and how (asynchronously) we can enhance a ContentItem
     */
    public int canEnhance(ContentItem ci) throws EngineException {
        // check if a Content in the supported type is available
        //NOTE: you can parse multiple content types
        Entry<IRI,Blob> textBlob = ContentItemHelper.getBlob(
            ci, Collections.singleton("text/plain"));
        if(textBlob == null) {
            return CANNOT_ENHANCE;
        }
        //TODO: test additional requirements for this EnhancementEngine

        // no reason why we should require to be executed synchronously
        return ENHANCE_ASYNC;
    }

    public void computeEnhancements(ContentItem ci) throws EngineException {
        //(1) retrieve the required data from the ConentItem
        String content;
        try {
            //get the (generated or submitted) text version of the ContentItem
            Blob textBlob = ContentItemHelper.getBlob(ci,
                    Collections.singleton("text/plain")).getValue();
            //Blob provides an InputStream. Use the Utility to load the String
            content = ContentItemHelper.getText(textBlob);
        } catch (IOException ex) {
            log.error("Exception reading content item.", ex);
            throw new InvalidContentException("Exception reading content item.", ex);
        }
        
        //(2) compute the enhancements
        int contentLength = content.length();
        
        //(3) write the enhancement results        
        // get the metadata graph
        Graph metadata = ci.getMetadata();
        //NOTE: as we allow synchronous calls we need to use read/write
        // locks on the ContentItem
        ci.getLock().writeLock().lock();
        try {
            // TODO: replace this with real enhancements
            IRI textAnnotation = EnhancementEngineHelper.createTextEnhancement(ci, this);
            metadata.add(new TripleImpl(textAnnotation, DCTERMS.type, 
                    new IRI("http://example.org/ontology/LengthEnhancement")));
            metadata.add(new TripleImpl(textAnnotation, RDFS.comment,
                    new PlainLiteralImpl("A text of " + contentLength + " charaters")));
        } finally {
            ci.getLock().writeLock().unlock();
        }
    }
}

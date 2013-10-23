#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.ontologies.DCTERMS;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.felix.scr.annotations.Component;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, metatype = true, inherit = true)
@Service
@Properties(value = {
    @Property(name = EnhancementEngine.PROPERTY_NAME, value = "${artifactId}-example")
})
public class ExampleEnhancer extends AbstractEnhancementEngine
        implements EnhancementEngine, ServiceProperties {

    /**
     * Using slf4j for logging
     */
    private static final Logger log = LoggerFactory.getLogger(ExampleEnhancer.class);

    /**
     * ServiceProperties are currently only used for automatic ordering of the 
     * execution of EnhancementEngines (e.g. by the WeightedChain implementation).
     * Default ordering means that the engine is called after all engines that
     * use a value < {@link ServiceProperties#ORDERING_CONTENT_EXTRACTION}
     * and >= {@link ServiceProperties#ORDERING_EXTRACTION_ENHANCEMENT}.
     */
    public Map getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(
                ENHANCEMENT_ENGINE_ORDERING, ORDERING_DEFAULT));
    }

    /**
     * @return if and how (asynchronously) we can enhance a ContentItem
     */
    public int canEnhance(ContentItem ci) throws EngineException {
        // check if content is present
        try {
            if ((ci.getBlob() == null)
                    || (ci.getBlob().getStream().read() == -1)) {
                return CANNOT_ENHANCE;
            }
        } catch (IOException e) {
            log.error("Failed to get the text for "
                    + "enhancement of content: " + ci.getUri(), e);
            throw new InvalidContentException(this, ci, e);
        }
        // no reason why we should require to be executed synchronously
        return ENHANCE_ASYNC;
    }

    public void computeEnhancements(ContentItem ci) throws EngineException {
        try {
            //get the (generated or submitted) text version of the ContentItem
            Blob textBlob =
                    ContentItemHelper.getBlob(ci,
                    Collections.singleton("text/plain")).getValue();
            String content = ContentItemHelper.getText(textBlob);
            // get the metadata graph
            MGraph metadata = ci.getMetadata();
            // update some sample data
            UriRef textAnnotation = EnhancementEngineHelper.createTextEnhancement(ci, this);
            metadata.add(new TripleImpl(textAnnotation, DCTERMS.type, 
                    new UriRef("http://example.org/ontology/LengthEnhancement")));
            metadata.add(new TripleImpl(textAnnotation, RDFS.comment,
                    new PlainLiteralImpl("A text of " + content.length() + " charaters")));
        } catch (IOException ex) {
            log.error("Exception reading content item.", ex);
            throw new InvalidContentException("Exception reading content item.", ex);
        }
    }
}

package org.apache.stanbol.rules.web;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.apache.stanbol.commons.web.base.writers.GraphWriter;
import org.apache.stanbol.commons.web.base.writers.ResultSetWriter;
import org.apache.stanbol.owl.web.OWLOntologyWriter;
import org.apache.stanbol.rules.web.resources.RefactorerResource;
import org.apache.stanbol.rules.web.resources.RestRecipe;
import org.apache.stanbol.rules.web.resources.RestRule;
import org.apache.stanbol.rules.web.resources.RuleStoreResource;

/**
 * Statically define the list of available resources and providers to be used by the KReS JAX-RS Endpoint.
 * 
 * The jersey auto-scan mechanism does not seem to work when deployed through OSGi's HttpService
 * initialization.
 * 
 * In the future this class might get refactored as an OSGi service to allow for dynamic configuration and
 * deployment of additional JAX-RS resources and providers.
 * 
 * @author andrea.nuzzolese
 */

public class JerseyEndpointApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();

        // Rules manager
        classes.add(RuleStoreResource.class);
        classes.add(RestRecipe.class);
        classes.add(RestRule.class);
        classes.add(RefactorerResource.class);
        //classes.add(OntologyStorageResource.class);
        
        // message body writers
        classes.add(GraphWriter.class);
        classes.add(ResultSetWriter.class);
        // classes.add(OwlModelWriter.class);
        classes.add(OWLOntologyWriter.class);
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> singletons = new HashSet<Object>();
        // view processors
//        singletons.add(new ViewProcessorImpl());
        return singletons;
    }

}

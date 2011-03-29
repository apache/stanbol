package org.apache.stanbol.ontologymanager.web;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.apache.stanbol.kres.jersey.processors.ViewProcessorImpl;
import org.apache.stanbol.kres.jersey.writers.GraphWriter;
import org.apache.stanbol.kres.jersey.writers.OWLOntologyWriter;
import org.apache.stanbol.kres.jersey.writers.ResultSetWriter;
import org.apache.stanbol.ontologymanager.web.resources.GraphsResource;
import org.apache.stanbol.ontologymanager.web.resources.ONMOntResource;
import org.apache.stanbol.ontologymanager.web.resources.ONMRootResource;
import org.apache.stanbol.ontologymanager.web.resources.ONMScopeOntologyResource;
import org.apache.stanbol.ontologymanager.web.resources.ONMScopeResource;
import org.apache.stanbol.ontologymanager.web.resources.SessionIDResource;
import org.apache.stanbol.ontologymanager.web.resources.SessionResource;

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

        classes.add(GraphsResource.class);
        
        classes.add(ONMRootResource.class);
        classes.add(ONMScopeResource.class);
        classes.add(ONMScopeOntologyResource.class);
        classes.add(ONMOntResource.class);
        
        classes.add(SessionResource.class);
        classes.add(SessionIDResource.class);
       
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
        singletons.add(new ViewProcessorImpl());
        return singletons;
    }

}

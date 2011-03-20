package org.apache.stanbol.kres.jersey;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.apache.stanbol.kres.jersey.processors.KReSViewProcessor;
import org.apache.stanbol.kres.jersey.resource.DocumentationResource;
import org.apache.stanbol.kres.jersey.resource.GraphsResource;
import org.apache.stanbol.kres.jersey.resource.KReSResource;
import org.apache.stanbol.kres.jersey.resource.KReSSessionIDResource;
import org.apache.stanbol.kres.jersey.resource.KReSSessionResource;
import org.apache.stanbol.kres.jersey.resource.RESTfulResource;
import org.apache.stanbol.kres.jersey.writers.GraphWriter;
import org.apache.stanbol.kres.jersey.writers.OWLOntologyWriter;
import org.apache.stanbol.kres.jersey.writers.ResultSetWriter;

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

        // resources
        classes.add(KReSResource.class);
        classes.add(KReSSessionResource.class);
        classes.add(KReSSessionIDResource.class);
        classes.add(GraphsResource.class);
        classes.add(DocumentationResource.class);

        /* REST services */
        classes.add(RESTfulResource.class);
        /* end rest services */
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
        singletons.add(new KReSViewProcessor());
        return singletons;
    }

}

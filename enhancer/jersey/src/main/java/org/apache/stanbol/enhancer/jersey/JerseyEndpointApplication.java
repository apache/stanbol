package org.apache.stanbol.enhancer.jersey;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.apache.stanbol.enhancer.jersey.processors.FreemarkerViewProcessor;
import org.apache.stanbol.enhancer.jersey.resource.EnginesRootResource;
import org.apache.stanbol.enhancer.jersey.resource.EnhancerRootResource;
import org.apache.stanbol.enhancer.jersey.resource.SparqlQueryResource;
import org.apache.stanbol.enhancer.jersey.resource.StoreRootResource;
import org.apache.stanbol.enhancer.jersey.writers.GraphWriter;
import org.apache.stanbol.enhancer.jersey.writers.ResultSetWriter;


/**
 * Statically define the list of available resources and providers to be used by
 * the Stanbol Enhancer JAX-RS Endpoint.
 * <p>
 * The jersey auto-scan mechanism does not seem to work when deployed through
 * OSGi's HttpService initialization.
 * <p>
 * In the future this class might get refactored as an OSGi service to allow for
 * dynamic configuration and deployment of additional JAX-RS resources and
 * providers.
 */
public class JerseyEndpointApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        // resources
        classes.add(EnhancerRootResource.class);
        classes.add(EnginesRootResource.class);
        classes.add(StoreRootResource.class);
        classes.add(SparqlQueryResource.class);

        // message body writers
        classes.add(GraphWriter.class);
        classes.add(ResultSetWriter.class);
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> singletons = new HashSet<Object>();
        // view processors
        singletons.add(new FreemarkerViewProcessor());
        return singletons;
    }

}

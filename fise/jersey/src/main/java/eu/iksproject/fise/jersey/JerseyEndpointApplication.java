package eu.iksproject.fise.jersey;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import eu.iksproject.fise.jersey.processors.FreemarkerViewProcessor;
import eu.iksproject.fise.jersey.resource.EnginesRootResource;
import eu.iksproject.fise.jersey.resource.FiseRootResource;
import eu.iksproject.fise.jersey.resource.SparqlQueryResource;
import eu.iksproject.fise.jersey.resource.StoreRootResource;
import eu.iksproject.fise.jersey.writers.GraphWriter;
import eu.iksproject.fise.jersey.writers.ResultSetWriter;

/**
 * Statically define the list of available resources and providers to be used by
 * the FISE JAX-RS Endpoint.
 *
 * The jersey auto-scan mechanism does not seem to work when deployed through
 * OSGi's HttpService initialization.
 *
 * In the future this class might get refactored as an OSGi service to allow for
 * dynamic configuration and deployment of additional JAX-RS resources and
 * providers.
 */
public class JerseyEndpointApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        // resources
        classes.add(FiseRootResource.class);
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

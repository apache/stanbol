package org.apache.stanbol.commons.web;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.apache.stanbol.commons.web.processor.FreemarkerViewProcessor;
import org.apache.stanbol.commons.web.resource.StanbolRootResource;
import org.apache.stanbol.commons.web.writers.GraphWriter;
import org.apache.stanbol.commons.web.writers.ResultSetWriter;

/**
 * Statically define the list of available resources and providers to be used by the Stanbol JAX-RS Endpoint.
 * <p>
 * The jersey auto-scan mechanism does not seem to work when deployed through OSGi's HttpService
 * initialization.
 * <p>
 * In the future this class might get refactored as an OSGi service to allow for dynamic configuration and
 * deployment of additional JAX-RS resources and providers.
 */
public class JerseyEndpointApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        // resources
        classes.add(StanbolRootResource.class);

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

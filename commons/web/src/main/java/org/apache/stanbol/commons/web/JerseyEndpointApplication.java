package org.apache.stanbol.commons.web;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.apache.stanbol.commons.web.processor.FreemarkerViewProcessor;
import org.apache.stanbol.commons.web.writers.GraphWriter;
import org.apache.stanbol.commons.web.writers.ResultSetWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Define the list of available resources and providers to be used by the Stanbol JAX-RS Endpoint.
 */
public class JerseyEndpointApplication extends Application {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(JerseyEndpointApplication.class);

    public final Set<Class<?>> contributedClasses = new HashSet<Class<?>>();

    public final Set<Object> contributedSingletons = new HashSet<Object>();

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();

        classes.addAll(contributedClasses);

        // message body writers, hard-coded for now
        classes.add(GraphWriter.class);
        classes.add(ResultSetWriter.class);
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> singletons = new HashSet<Object>();
        singletons.addAll(contributedSingletons);

        // view processors, hard-coded for now
        // TODO make it possible to pass the template loaders from the OSGi context here:
        singletons.add(new FreemarkerViewProcessor());
        return singletons;
    }

    public void contributeClasses(Set<Class<?>> classes) {
        contributedClasses.addAll(classes);
    }

    public void contributeSingletons(Set<Object> singletons) {
        contributedSingletons.addAll(singletons);
    }
}

package org.apache.stanbol.commons.web;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    private static final Logger log = LoggerFactory.getLogger(JerseyEndpointApplication.class);
    
    List<JaxrsResource> jaxrsResources = new ArrayList<JaxrsResource>();
    
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        
        // resources
        for (JaxrsResource jr : this.jaxrsResources) {
            classes.add(jr.getClass());
        }

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
    
    
    public void bindJaxrsResource(JaxrsResource jr) {
        synchronized (this.jaxrsResources) {
            this.jaxrsResources.add(jr);
        }
        if (log.isInfoEnabled()) {
            log.info("JaxrsResource {} added to list: {}", jr, this.jaxrsResources);
        }
    }

    public void unbindEnhancementEngine(JaxrsResource jr) {
        synchronized (this.jaxrsResources) {
            this.jaxrsResources.remove(jr);
        }
        if (log.isInfoEnabled()) {
            log.info("JaxrsResource {} removed to list: {}", jr, this.jaxrsResources);
        }
    }
}

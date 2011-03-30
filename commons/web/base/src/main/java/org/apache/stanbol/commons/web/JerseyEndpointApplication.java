package org.apache.stanbol.commons.web;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.apache.stanbol.commons.web.processor.FreemarkerViewProcessor;
import org.apache.stanbol.commons.web.resource.StanbolRootResource;
import org.apache.stanbol.commons.web.writers.GraphWriter;
import org.apache.stanbol.commons.web.writers.ResultSetWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;

/**
 * Define the list of available resources and providers to be used by the Stanbol JAX-RS Endpoint.
 */
public class JerseyEndpointApplication extends Application {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(JerseyEndpointApplication.class);

    protected final Set<Class<?>> contributedClasses = new HashSet<Class<?>>();

    protected final Set<Object> contributedSingletons = new HashSet<Object>();

    protected List<TemplateLoader> templateLoaders = new ArrayList<TemplateLoader>();

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        // hardcoded root resource class for now (to be externalized into a stanbol.commons.web.home package
        // for instance)
        classes.add(StanbolRootResource.class);

        // resources contributed buy other bundles
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

        TemplateLoader[] loadersArray = templateLoaders.toArray(new TemplateLoader[templateLoaders.size()]);
        MultiTemplateLoader templateLoader = new MultiTemplateLoader(loadersArray);
        singletons.add(new FreemarkerViewProcessor(templateLoader));
        return singletons;
    }

    public void contributeClasses(Set<Class<?>> classes) {
        contributedClasses.addAll(classes);
    }

    public void contributeSingletons(Set<Object> singletons) {
        contributedSingletons.addAll(singletons);
    }

    public void contributeTemplateLoader(TemplateLoader templateLoader) {
        this.templateLoaders.add(templateLoader);
    }
}

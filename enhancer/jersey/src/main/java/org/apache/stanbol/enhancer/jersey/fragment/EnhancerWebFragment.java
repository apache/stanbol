package org.apache.stanbol.enhancer.jersey.fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.LinkResource;
import org.apache.stanbol.commons.web.ScriptResource;
import org.apache.stanbol.commons.web.WebFragment;
import org.apache.stanbol.enhancer.jersey.resource.EnginesRootResource;
import org.apache.stanbol.enhancer.jersey.resource.SparqlQueryResource;
import org.apache.stanbol.enhancer.jersey.resource.StoreRootResource;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;

/**
 * Statically define the list of available resources and providers to be contributed to the the Stanbol JAX-RS
 * Endpoint.
 */
@Component(immediate = true, metatype = true)
@Service
public class EnhancerWebFragment implements WebFragment {

    private static final String NAME = "enhancer";

    private static final String STATIC_RESOURCE_PATH = "/org/apache/stanbol/enhancer/jersey/static";

    private static final String TEMPLATE_PATH = "/org/apache/stanbol/enhancer/jersey/templates";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Set<Class<?>> getJaxrsResourceClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        // resources
        classes.add(EnginesRootResource.class);
        classes.add(StoreRootResource.class);
        classes.add(SparqlQueryResource.class);
        return classes;
    }

    @Override
    public Set<Object> getJaxrsResourceSingletons() {
        return Collections.emptySet();
    }

    @Override
    public String getStaticResourceClassPath() {
        return STATIC_RESOURCE_PATH;
    }

    @Override
    public TemplateLoader getTemplateLoader() {
        return new ClassTemplateLoader(getClass(), TEMPLATE_PATH);
    }

    @Override
    public List<LinkResource> getLinkResources() {
        List<LinkResource> resources = new ArrayList<LinkResource>();
        resources.add(new LinkResource("stylesheet", "openlayers-2.9/theme/default/style.css", this));
        resources.add(new LinkResource("stylesheet", "scripts/prettify/prettify.css", this));
        return resources;
    }

    @Override
    public List<ScriptResource> getScriptResources() {
        List<ScriptResource> resources = new ArrayList<ScriptResource>();
        resources.add(new ScriptResource("text/javascript", "openlayers-2.9/OpenLayers.js", this));
        resources.add(new ScriptResource("text/javascript", "scripts/prettify/prettify.js", this));
        resources.add(new ScriptResource("text/javascript", "scripts/jquery-1.4.2.js", this));
        return resources;
    }

}

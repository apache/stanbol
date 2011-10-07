package org.apache.stanbol.commons.web.vie.fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.base.LinkResource;
import org.apache.stanbol.commons.web.base.NavigationLink;
import org.apache.stanbol.commons.web.base.ScriptResource;
import org.apache.stanbol.commons.web.base.WebFragment;
import org.apache.stanbol.commons.web.vie.resource.EnhancerVieRootResource;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.Store;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;

/**
 * Statically define the list of available resources and providers to be contributed to the the Stanbol JAX-RS
 * Endpoint.
 */
@Component(immediate = true, metatype = true)
@Service
public class EnhancerVieWebFragment implements WebFragment {

    private static final String NAME = "enhancervie";

    private static final String STATIC_RESOURCE_PATH = "/org/apache/stanbol/commons/web/vie/static";

    private static final String TEMPLATE_PATH = "/org/apache/stanbol/commons/web/vie/templates";

    private BundleContext bundleContext;
    
    @Reference
    TcManager tcManager;

    @Reference
    Store store;

    @Reference
    EnhancementJobManager jobManager;

    @Reference
    Serializer serializer;

    @Override
    public String getName() {
        return NAME;
    }

    @Activate
    protected void activate(ComponentContext ctx) {
        this.bundleContext = ctx.getBundleContext();
    }

    @Override
    public Set<Class<?>> getJaxrsResourceClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(EnhancerVieRootResource.class);
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
//    	resources.add(new LinkResource("stylesheet", "lib/Aristo/jquery-ui-1.8.7.custom.css", this, 10));
    	resources.add(new LinkResource("stylesheet", "lib/Smoothness/jquery.ui.all.css", this, 10)); // version 1.8.13
//    	resources.add(new LinkResource("stylesheet", "lib/Aristo/jquery.ui.menu.css", this, 10));
        return resources;
    }

    @Override
    public List<ScriptResource> getScriptResources() {
        List<ScriptResource> resources = new ArrayList<ScriptResource>();
        resources.add(new ScriptResource("text/javascript", "lib/jquery-1.5.1.js", this, 10));
        resources.add(new ScriptResource("text/javascript", "lib/jquery-ui.1.9m5.js", this, 10));
        resources.add(new ScriptResource("text/javascript", "lib/underscore-min.js", this, 10));
        resources.add(new ScriptResource("text/javascript", "lib/backbone.js", this, 10));

        resources.add(new ScriptResource("text/javascript", "lib/jquery.rdfquery.debug.js", this, 10));
        resources.add(new ScriptResource("text/javascript", "lib/vie/vie-latest.debug.js", this, 10));

        resources.add(new ScriptResource("text/javascript", "lib/hallo/hallo.js", this, 10));
        resources.add(new ScriptResource("text/javascript", "lib/hallo/format.js", this, 10));

        resources.add(new ScriptResource("text/javascript", "lib/annotate.js", this, 10));

        return resources;
    }

    @Override
    public List<NavigationLink> getNavigationLinks() {
        List<NavigationLink> links = new ArrayList<NavigationLink>();
        links.add(new NavigationLink("enhancervie", "/enhancer VIE", "/imports/enhancervieDescription.ftl", 20));
        return links;
    }

    @Override
    public BundleContext getBundleContext() {
        return bundleContext;
    }

}

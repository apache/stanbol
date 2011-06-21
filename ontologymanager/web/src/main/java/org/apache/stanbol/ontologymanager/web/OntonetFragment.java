package org.apache.stanbol.ontologymanager.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.base.LinkResource;
import org.apache.stanbol.commons.web.base.NavigationLink;
import org.apache.stanbol.commons.web.base.ScriptResource;
import org.apache.stanbol.commons.web.base.WebFragment;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.web.resources.DocumentationResource;
import org.apache.stanbol.ontologymanager.web.resources.GraphsResource;
import org.apache.stanbol.ontologymanager.web.resources.ONMOntResource;
import org.apache.stanbol.ontologymanager.web.resources.ONMRootResource;
import org.apache.stanbol.ontologymanager.web.resources.ONMScopeOntologyResource;
import org.apache.stanbol.ontologymanager.web.resources.ONMScopeResource;
import org.apache.stanbol.ontologymanager.web.resources.RESTfulResource;
import org.apache.stanbol.ontologymanager.web.resources.RootResource;
import org.apache.stanbol.ontologymanager.web.resources.SessionIDResource;
import org.apache.stanbol.ontologymanager.web.resources.SessionResource;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;


/**
 * Implementation of WebFragment for the Stanbol Ontonet end-point.
 * 
 * @author alberto musetti
 *
 */

@Component(immediate = true, metatype = true)
@Service(WebFragment.class)
public class OntonetFragment implements WebFragment{

    private static final String NAME = "ontonet";

    private static final String STATIC_RESOURCE_PATH = "/org/apache/stanbol/ontologymanager/web/static";

    private static final String TEMPLATE_PATH = "/org/apache/stanbol/ontologymanager/web/templates";

    private BundleContext bundleContext;

    @Reference
    ONManager onm;

    @Override
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    @Override
    public Set<Class<?>> getJaxrsResourceClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        // Temporary resources
        classes.add(RootResource.class);
        classes.add(DocumentationResource.class);
        classes.add(RESTfulResource.class);

        classes.add(GraphsResource.class);

        classes.add(ONMRootResource.class);
        classes.add(ONMScopeResource.class);
        classes.add(ONMScopeOntologyResource.class);
        classes.add(ONMOntResource.class);

        classes.add(SessionResource.class);
        classes.add(SessionIDResource.class);

        return classes;
    }

    @Override
    public Set<Object> getJaxrsResourceSingletons() {
        return Collections.emptySet();
    }

    @Override
    public List<LinkResource> getLinkResources() {
        List<LinkResource> resources = new ArrayList<LinkResource>();
        resources.add(new LinkResource("stylesheet", "css/ontonet.css", this, 10));
        return resources;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<NavigationLink> getNavigationLinks() {
        List<NavigationLink> links = new ArrayList<NavigationLink>();
        links.add(new NavigationLink("ontonet", "/ontonet", "/imports/ontonetDescription.ftl", 50));
        return links;
    }

    @Override
    public List<ScriptResource> getScriptResources() {
        List<ScriptResource> resources = new ArrayList<ScriptResource>();
        resources.add(new ScriptResource("text/javascript", "actions/actions.js", this, 10));
        return resources;
    }

    @Override
    public String getStaticResourceClassPath() {
        return STATIC_RESOURCE_PATH;
    }

    @Override
    public TemplateLoader getTemplateLoader() {
        return new ClassTemplateLoader(getClass(), TEMPLATE_PATH);
    }

    @Activate
    protected void activate(ComponentContext ctx) {
        this.bundleContext = ctx.getBundleContext();
    }

}

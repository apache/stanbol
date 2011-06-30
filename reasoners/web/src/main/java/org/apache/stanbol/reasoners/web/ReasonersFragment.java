package org.apache.stanbol.reasoners.web;

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
import org.apache.stanbol.reasoners.base.api.Reasoner;
import org.apache.stanbol.reasoners.web.resources.Classify;
import org.apache.stanbol.reasoners.web.resources.ConsistencyCheck;
import org.apache.stanbol.reasoners.web.resources.ConsistentRefactoring;
import org.apache.stanbol.reasoners.web.resources.Enrichment;
import org.apache.stanbol.reasoners.web.resources.ReasonersResource;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;


/**
 * Implementation of WebFragment for the Stanbol Reasoner end-point.
 * 
 * @author Alberto Musetti
 *
 */

@Component(immediate = true, metatype = true)
@Service(WebFragment.class)
public class ReasonersFragment implements WebFragment{
    
    private static final String NAME = "reasoners";

    private static final String STATIC_RESOURCE_PATH = "/org/apache/stanbol/reasoners/web/static";

    private static final String TEMPLATE_PATH = "/org/apache/stanbol/reasoners/web/templates";
    
    @Reference
    Reasoner reasoners;
    
    @Reference
    ONManager onm;
    
    @Reference
    RuleStore kresRuleStore;

    private BundleContext bundleContext;
    
    @Override
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    @Override
    public Set<Class<?>> getJaxrsResourceClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        // Reasoner
        classes.add(ReasonersResource.class);
        classes.add(ConsistencyCheck.class);
        classes.add(ConsistentRefactoring.class);
        classes.add(Classify.class);
        classes.add(Enrichment.class);
        
        return classes;
    }

    @Override
    public Set<Object> getJaxrsResourceSingletons() {
        return Collections.emptySet();
    }

    @Override
    public List<LinkResource> getLinkResources() {
        List<LinkResource> resources = new ArrayList<LinkResource>();
        resources.add(new LinkResource("stylesheet", "css/reasoners.css", this, 10));
        return resources;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<NavigationLink> getNavigationLinks() {
        List<NavigationLink> links = new ArrayList<NavigationLink>();
        links.add(new NavigationLink("reasoners", "/reasoners", "/imports/reasonersDescription.ftl", 50));
        return links;
    }

    @Override
    public List<ScriptResource> getScriptResources() {
        return Collections.emptyList();
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

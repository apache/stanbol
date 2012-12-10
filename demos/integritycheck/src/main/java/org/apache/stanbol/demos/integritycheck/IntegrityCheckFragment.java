package org.apache.stanbol.demos.integritycheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.base.LinkResource;
import org.apache.stanbol.commons.web.base.NavigationLink;
import org.apache.stanbol.commons.web.base.ScriptResource;
import org.apache.stanbol.commons.web.base.WebFragment;
import org.apache.stanbol.demos.integritycheck.resources.IntegrityCheckResource;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;

/**
 * 
 * @author enridaga
 *
 */
@Component(immediate = true, metatype = true)
@Service(WebFragment.class)
public class IntegrityCheckFragment implements WebFragment{
    public static final String NAME = "integritycheck";

    private BundleContext bundleContext;

    @Activate
    protected void activate(ComponentContext ctx) {
        this.bundleContext = ctx.getBundleContext();
    }

	@Override
	public String getName() {
		return NAME;
	}


	@Override
	public Set<Class<?>> getJaxrsResourceClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(IntegrityCheckResource.class);
        return classes;
	}

	@Override
	public Set<Object> getJaxrsResourceSingletons() {
        return Collections.emptySet();
	}

	@Override
	public TemplateLoader getTemplateLoader() {
        return new ClassTemplateLoader(getClass(), TEMPLATE_PATH);
	}

	@Override
	public List<LinkResource> getLinkResources() {
        List<LinkResource> resources = new ArrayList<LinkResource>();
        resources.add(new LinkResource("stylesheet", "integritycheck.css", this, 10));
        return resources;
	}

	@Override
	public List<ScriptResource> getScriptResources() {
        List<ScriptResource> resources = new ArrayList<ScriptResource>();
        resources.add(new ScriptResource("text/javascript", "jquery.rdfquery.core-1.0.js", this, 10));
        resources.add(new ScriptResource("text/javascript", "jquery.cookie.js", this, 10));
        resources.add(new ScriptResource("text/javascript", "integritycheck.js", this, 20));

        return resources;
	}

	@Override
	public List<NavigationLink> getNavigationLinks() {
        List<NavigationLink> links = new ArrayList<NavigationLink>();
        links.add(new NavigationLink("integritycheck", "/integritycheck", "/imports/integritycheckDescription.ftl", 50));
        return links;
	}

	@Override
	public BundleContext getBundleContext() {
		return bundleContext;
	}

}
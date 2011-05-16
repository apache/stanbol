package org.apache.stanbol.owl.web;

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
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;

/**
 * Implementation of WebFragment for the OWL API.
 * 
 * @author andrea.nuzzolese
 *
 */

@Component(immediate = true, metatype = true)
@Service(WebFragment.class)
public class OWLFragment implements WebFragment {

	private static final String NAME = "owl";

    private static final String STATIC_RESOURCE_PATH = "/org/apache/stanbol/owl/web/static";

    private static final String TEMPLATE_PATH = "/org/apache/stanbol/owl/web/templates";

    private BundleContext bundleContext;
	
	
	@Override
	public String getName() {
		return NAME;
	}
	
	@Activate
    protected void activate(ComponentContext ctx) {
        this.bundleContext = ctx.getBundleContext();
    }

	@Override
	public String getStaticResourceClassPath() {
		return STATIC_RESOURCE_PATH;
	}

	@Override
	public Set<Class<?>> getJaxrsResourceClasses() {
		Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(OWLOntologyWriter.class);
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
		return resources;
	}

	@Override
	public List<ScriptResource> getScriptResources() {
		List<ScriptResource> resources = new ArrayList<ScriptResource>();
		return resources;
	}

	@Override
	public List<NavigationLink> getNavigationLinks() {
		List<NavigationLink> resources = new ArrayList<NavigationLink>();
		return resources;
	}

	@Override
	public BundleContext getBundleContext() {
		return bundleContext;
	}

}

/**
 * 
 */
package org.apache.stanbol.demos.crawler.web.fragment;

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
import org.apache.stanbol.demos.crawler.web.resources.CNNCrawlerResource;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;

/**
 * @author anil.sinaci
 * 
 */
@Component(immediate = true, metatype = true)
@Service
public class CrawlerWebFragment implements WebFragment {

	private static final String NAME = "crawler";

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
		classes.add(CNNCrawlerResource.class);
		return classes;
	}

	@Override
	public Set<Object> getJaxrsResourceSingletons() {
		return Collections.emptySet();
	}

	@Override
	public List<LinkResource> getLinkResources() {
		List<LinkResource> resources = new ArrayList<LinkResource>();
		resources.add(new LinkResource("stylesheet", "style/contenthub.css",
				this, 0));
		resources.add(new LinkResource("stylesheet",
				"style/jquery-ui-1.8.11.custom.css", this, 1));
		return resources;
	}

	@Override
	public List<ScriptResource> getScriptResources() {
		List<ScriptResource> resources = new ArrayList<ScriptResource>();
		resources.add(new ScriptResource("text/javascript",
				"scripts/prettify/prettify.js", this, 0));
		resources.add(new ScriptResource("text/javascript", "scripts/jit.js",
				this, 1));
		resources.add(new ScriptResource("text/javascript",
				"scripts/jquery-1.5.1.min.js", this, 2));
		resources.add(new ScriptResource("text/javascript",
				"scripts/jquery-ui-1.8.11.custom.min.js", this, 3));
		return resources;
	}

	@Override
	public List<NavigationLink> getNavigationLinks() {
		return Collections.emptyList();
	}

	@Override
	public BundleContext getBundleContext() {
		return bundleContext;
	}

}

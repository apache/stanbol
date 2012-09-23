package org.apache.stanbol.commons.ldpathtemplate;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;

import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.ldpath.clerezza.ClerezzaBackend;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.newmedialab.ldpath.api.backend.RDFBackend;
import at.newmedialab.ldpath.template.engine.TemplateEngine;
import freemarker.cache.TemplateLoader;
import freemarker.template.TemplateException;

/**
 * This service renders a GraphNode to a Writer given the 
 * path of an ldpath template
 *
 */
@Component
@Service(LdRenderer.class)
public class LdRenderer {
	
	private static final String TEMPLATES_PATH_IN_BUNDLES = "templates/";

	private static final Logger log = LoggerFactory.getLogger(LdRenderer.class);
	
	private final Collection<Bundle> bundles = new HashSet<Bundle>();
	
	private BundleListener bundleListener = new BundleListener() {
		
		@Override
		public void bundleChanged(BundleEvent event) {
			if ((event.getType() == BundleEvent.STARTED) && containsTemplates(event.getBundle())) {
				bundles.add(event.getBundle());
			} else {
				bundles.remove(event.getBundle());
			}
		}
	};
	
	private TemplateLoader templateLoader = new TemplateLoader() {
		
		@Override
		public Reader getReader(Object templateSource, String encoding)
				throws IOException {
			URL templateUrl = (URL) templateSource;
			return new InputStreamReader(templateUrl.openStream(), encoding);
		}
		
		@Override
		public long getLastModified(Object templateSource) {
			// not known
			return -1;
		}
		
		@Override
		public Object findTemplateSource(String name) throws IOException {
			for (Bundle bundle : bundles) {
				URL res = bundle.getResource(TEMPLATES_PATH_IN_BUNDLES+name);
				if (res != null) {
					return res;
				}
			}
			log.warn("Template "+name+" not known");
			return null;
		}
		
		@Override
		public void closeTemplateSource(Object templateSource) throws IOException {

			
		}
	};
	
	@Activate
	protected void activate(final ComponentContext context) {
		final Bundle[] registeredBundles = context.getBundleContext().getBundles();
		for (int i = 0; i < registeredBundles.length; i++) {
			if ((registeredBundles[i].getState() == Bundle.ACTIVE) 
					&& containsTemplates(registeredBundles[i])) {
				bundles.add(registeredBundles[i]);
			}
		}	
		context.getBundleContext().addBundleListener(bundleListener);
	}

	@Deactivate
	protected void deactivate(final ComponentContext context) {
		context.getBundleContext().removeBundleListener(bundleListener);
	}
	
	private boolean containsTemplates(Bundle bundle) {
		return bundle.getResource(TEMPLATES_PATH_IN_BUNDLES) != null;
	}

	/**
	 * Renders a GraphNode with a template located in the templates
	 * folder of any active bundle
	 * 
	 * @param node the GraphNode to be rendered
	 * @param templatePath the freemarker path to the template
	 * @param out where the result is written to
	 */
	public void render(GraphNode node, final String templatePath, Writer out) {	
		//A GraphNode backend could be graph unspecific, so the same engine could be
		//reused, possibly being signifantly more performant (caching, etc.)
		RDFBackend<Resource> backend = new ClerezzaBackend(node.getGraph());
		Resource context = node.getNode();
		TemplateEngine<Resource> engine = new TemplateEngine<Resource>(backend);
		engine.setTemplateLoader(templateLoader);
		try {
			engine.processFileTemplate(context, templatePath, null, out);
			out.flush();
			out.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (TemplateException e) {
			throw new RuntimeException(e);
		}
	}
}

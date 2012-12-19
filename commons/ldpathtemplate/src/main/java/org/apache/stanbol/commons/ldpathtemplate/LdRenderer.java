/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
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
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
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
			if (!name.endsWith(".ftl")) {
				name = name +".ftl";
			}
			final String path = TEMPLATES_PATH_IN_BUNDLES+name;
			for (Bundle bundle : bundles) {
				URL res = bundle.getResource(path);
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
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (TemplateException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Old school classical freemarker rendering, no LD here
	 */
	public void renderPojo(Object pojo, final String templatePath, Writer out) {	
		Configuration freemarker= new Configuration();
		freemarker.setDefaultEncoding("utf-8");
		freemarker.setOutputEncoding("utf-8");
		freemarker.setLocalizedLookup(false);
	    freemarker.setObjectWrapper(new DefaultObjectWrapper());
		freemarker.setTemplateLoader(templateLoader);
		try {
			//should root be a map instead?
			freemarker.getTemplate(templatePath).process(pojo, out);
			out.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (TemplateException e) {
			throw new RuntimeException(e);
		}
	}
}

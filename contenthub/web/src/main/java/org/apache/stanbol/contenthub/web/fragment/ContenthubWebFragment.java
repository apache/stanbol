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

package org.apache.stanbol.contenthub.web.fragment;

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
import org.apache.stanbol.contenthub.web.resources.FeaturedSearchResource;
import org.apache.stanbol.contenthub.web.resources.RelatedKeywordResource;
import org.apache.stanbol.contenthub.web.resources.RootResource;
import org.apache.stanbol.contenthub.web.resources.SemanticIndexManagerResource;
import org.apache.stanbol.contenthub.web.resources.StoreResource;
import org.apache.stanbol.contenthub.web.writers.LDProgramCollectionWriter;
import org.apache.stanbol.contenthub.web.writers.SearchResultWriter;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

/**
 * Statically define the list of available resources and providers to be
 * contributed to the the Stanbol JAX-RS Endpoint.
 */
@Component(immediate = true, metatype = true)
@Service
public class ContenthubWebFragment implements WebFragment {

	private static final String NAME = "contenthub";


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
	public Set<Class<?>> getJaxrsResourceClasses() {
		Set<Class<?>> classes = new HashSet<Class<?>>();
		classes.add(RootResource.class);
		classes.add(StoreResource.class);
		classes.add(FeaturedSearchResource.class);
		classes.add(SemanticIndexManagerResource.class);
		classes.add(RelatedKeywordResource.class);

		classes.add(LDProgramCollectionWriter.class);
		classes.add(SearchResultWriter.class);

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
		List<NavigationLink> links = new ArrayList<NavigationLink>();
		links.add(new NavigationLink("contenthub/contenthub/store",
				"/contenthub", "/imports/contenthubDescription.ftl", 20));
		return links;
	}
}

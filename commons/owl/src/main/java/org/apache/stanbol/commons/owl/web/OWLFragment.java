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
package org.apache.stanbol.commons.owl.web;

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


/**
 * Implementation of WebFragment for the OWL API.
 * 
 * @author andrea.nuzzolese
 *
 */

@Component(immediate = true, metatype = false)
@Service(WebFragment.class)
public class OWLFragment implements WebFragment {

	private static final String NAME = "owl";

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
        classes.add(OWLOntologyWriter.class);
        return classes;
	}

	@Override
	public Set<Object> getJaxrsResourceSingletons() {
		return Collections.emptySet();
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


}

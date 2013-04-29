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
package org.apache.stanbol.commons.web.vie.fragment;

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
import org.apache.stanbol.commons.web.vie.resource.EnhancerVieRootResource;
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
	private static final String htmlDescription = 
			"This is a frontend to the enhancer featuring VIE.js";


    @Override
    public String getName() {
        return NAME;
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
    public List<LinkResource> getLinkResources() {
    	List<LinkResource> resources = new ArrayList<LinkResource>();
//    	resources.add(new LinkResource("stylesheet", "lib/Aristo/jquery-ui-1.8.7.custom.css", this, 10));
    	resources.add(new LinkResource("stylesheet", "lib/jquery/jquery-ui.min.css", this, 10));
    	resources.add(new LinkResource("stylesheet", "lib/Smoothness/jquery.ui.all.css", this, 10));
    	resources.add(new LinkResource("stylesheet", "annotate.css", this, 10));
//    	resources.add(new LinkResource("stylesheet", "lib/Aristo/jquery.ui.menu.css", this, 10));
        return resources;
    }

    @Override
    public List<ScriptResource> getScriptResources() {
        List<ScriptResource> resources = new ArrayList<ScriptResource>();
        resources.add(new ScriptResource("text/javascript", "lib/jquery/jquery-1.8.2.js", this, 10));
        resources.add(new ScriptResource("text/javascript", "lib/jqueryui/jquery-ui.1.10.2.min.js", this, 10));
        resources.add(new ScriptResource("text/javascript", "lib/underscore/underscore-min.js", this, 10));
        resources.add(new ScriptResource("text/javascript", "lib/backboneJS/backbone.js", this, 10));

        resources.add(new ScriptResource("text/javascript", "lib/jquery.rdfquery.debug.js", this, 10));
        resources.add(new ScriptResource("text/javascript", "lib/vie/vie.js", this, 10));

        resources.add(new ScriptResource("text/javascript", "lib/hallo/hallo.js", this, 10));
        resources.add(new ScriptResource("text/javascript", "lib/hallo/format.js", this, 10));

        resources.add(new ScriptResource("text/javascript", "lib/vie.entitypreview.js", this, 10));
        resources.add(new ScriptResource("text/javascript", "lib/annotate.js", this, 10));

        return resources;
    }

    @Override
    public List<NavigationLink> getNavigationLinks() {
        List<NavigationLink> links = new ArrayList<NavigationLink>();
        links.add(new NavigationLink("enhancervie", "/enhancer VIE", htmlDescription, 21));
        return links;
    }

}

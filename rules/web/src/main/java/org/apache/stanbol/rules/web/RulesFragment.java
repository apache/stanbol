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
package org.apache.stanbol.rules.web;

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
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.refactor.api.Refactorer;
import org.apache.stanbol.rules.web.resources.RefactorResource;
import org.apache.stanbol.rules.web.resources.RecipeResource;
import org.apache.stanbol.rules.web.resources.RuleResource;
import org.apache.stanbol.rules.web.resources.RuleStoreResource;
import org.apache.stanbol.rules.web.resources.RulesRootResource;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;

/**
 * Implementation of WebFragment for the Stanbol Rules end-point.
 * 
 * @author andrea.nuzzolese
 *
 */

@Component(immediate = true, metatype = true)
@Service(WebFragment.class)
public class RulesFragment implements WebFragment{
	
	private static final String NAME = "rules";

    private static final String STATIC_RESOURCE_PATH = "/org/apache/stanbol/rules/web/static";

    private static final String TEMPLATE_PATH = "/org/apache/stanbol/rules/web/templates";

    private BundleContext bundleContext;
    
    @Reference
    Refactorer refactorer;
    
    @Reference
    RuleStore ruleStore;

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
        // resources
        classes.add(RefactorResource.class);
        classes.add(RecipeResource.class);
        classes.add(RuleResource.class);
        classes.add(RuleStoreResource.class);
        classes.add(RulesRootResource.class);
        return classes;
    }

    @Override
    public Set<Object> getJaxrsResourceSingletons() {
        return Collections.emptySet();
    }

    @Override
    public String getStaticResourceClassPath() {
        return STATIC_RESOURCE_PATH;
    }

    @Override
    public TemplateLoader getTemplateLoader() {
        return new ClassTemplateLoader(getClass(), TEMPLATE_PATH);
    }

	@Override
	public List<LinkResource> getLinkResources() {
		List<LinkResource> resources = new ArrayList<LinkResource>();
		resources.add(new LinkResource("stylesheet", "css/rules.css", this, 10));
        return resources;
	}

	@Override
	public List<ScriptResource> getScriptResources() {
		List<ScriptResource> resources = new ArrayList<ScriptResource>();
		resources.add(new ScriptResource("text/javascript", "actions/actions.js", this, 10));
		resources.add(new ScriptResource("text/javascript", "actions/tutorial.js", this, 10));
        return resources;
	}

	@Override
	public List<NavigationLink> getNavigationLinks() {
		List<NavigationLink> links = new ArrayList<NavigationLink>();
        links.add(new NavigationLink("rules", "/rules", "/imports/rulesDescription.ftl", 50));
        return links;
	}

	@Override
	public BundleContext getBundleContext() {
		return bundleContext;
	}
    
}

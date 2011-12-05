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
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionManager;
import org.apache.stanbol.reasoners.web.resources.JobsResource;
import org.apache.stanbol.reasoners.web.resources.ReasoningServiceTaskResource;
import org.apache.stanbol.reasoners.web.resources.ReasoningServicesResource;
import org.apache.stanbol.reasoners.web.writers.JenaModelWriter;
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
public class ReasonersFragment implements WebFragment {

    private static final String NAME = "reasoners";

    private static final String STATIC_RESOURCE_PATH = "/org/apache/stanbol/reasoners/web/static";

    private static final String TEMPLATE_PATH = "/org/apache/stanbol/reasoners/web/templates";

    @Reference
    ONManager onm;

    @Reference
    SessionManager sessionManager;

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
        // classes.add(ReasonersResource.class);
        classes.add(ReasoningServicesResource.class);
        classes.add(ReasoningServiceTaskResource.class);
        classes.add(JobsResource.class);

        // Writer
        classes.add(JenaModelWriter.class);
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

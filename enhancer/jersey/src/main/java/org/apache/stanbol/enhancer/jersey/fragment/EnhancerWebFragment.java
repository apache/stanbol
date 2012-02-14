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
package org.apache.stanbol.enhancer.jersey.fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.base.LinkResource;
import org.apache.stanbol.commons.web.base.NavigationLink;
import org.apache.stanbol.commons.web.base.ScriptResource;
import org.apache.stanbol.commons.web.base.WebFragment;
import org.apache.stanbol.enhancer.jersey.reader.ContentItemReader;
import org.apache.stanbol.enhancer.jersey.resource.ChainsRootResource;
import org.apache.stanbol.enhancer.jersey.resource.EnginesRootResource;
import org.apache.stanbol.enhancer.jersey.resource.EnhancementEngineResource;
import org.apache.stanbol.enhancer.jersey.resource.EnhancementEnginesRootResource;
import org.apache.stanbol.enhancer.jersey.resource.EnhancerRootResource;
import org.apache.stanbol.enhancer.jersey.resource.EnhancementChainResource;
import org.apache.stanbol.enhancer.jersey.writers.ContentItemWriter;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
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
public class EnhancerWebFragment implements WebFragment {

    private static final String NAME = "enhancer";

    private static final String STATIC_RESOURCE_PATH = "/org/apache/stanbol/enhancer/jersey/static";

    private static final String TEMPLATE_PATH = "/org/apache/stanbol/enhancer/jersey/templates";

    private BundleContext bundleContext;

    @Reference
    EnhancementJobManager jobManager;

    @Reference
    TcManager tcManager;

    @Reference
    Serializer serializer;

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
        classes.add(EnginesRootResource.class);
        classes.add(EnhancerRootResource.class);
        classes.add(EnhancementChainResource.class);
        classes.add(ChainsRootResource.class);
        classes.add(EnhancementEnginesRootResource.class);
        classes.add(EnhancementEngineResource.class);
        //Reader/Writer for ContentItems
        classes.add(ContentItemReader.class);
        classes.add(ContentItemWriter.class);
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
        resources.add(new LinkResource("stylesheet", "openlayers-2.9/theme/default/style.css", this, 10));
        resources.add(new LinkResource("stylesheet", "scripts/prettify/prettify.css", this, 20));
        return resources;
    }

    @Override
    public List<ScriptResource> getScriptResources() {
        List<ScriptResource> resources = new ArrayList<ScriptResource>();
        resources.add(new ScriptResource("text/javascript", "openlayers-2.9/OpenLayers.js", this, 10));
        resources.add(new ScriptResource("text/javascript", "scripts/prettify/prettify.js", this, 20));
        return resources;
    }

    @Override
    public List<NavigationLink> getNavigationLinks() {
        List<NavigationLink> links = new ArrayList<NavigationLink>();
        links.add(new NavigationLink("enhancer", "/enhancer", "/imports/enginesDescription.ftl", 10));
        return links;
    }

    @Override
    public BundleContext getBundleContext() {
        return bundleContext;
    }

}

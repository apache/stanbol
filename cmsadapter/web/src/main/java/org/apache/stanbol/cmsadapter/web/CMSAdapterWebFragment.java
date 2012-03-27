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
package org.apache.stanbol.cmsadapter.web;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.cmsadapter.web.resources.BridgeDefinitionsResource;
import org.apache.stanbol.cmsadapter.web.resources.ContenthubFeedResource;
import org.apache.stanbol.cmsadapter.web.resources.NotifyResource;
import org.apache.stanbol.cmsadapter.web.resources.ObjectTypesResource;
import org.apache.stanbol.cmsadapter.web.resources.RDFMapperResource;
import org.apache.stanbol.cmsadapter.web.resources.RootResource;
import org.apache.stanbol.cmsadapter.web.resources.SessionResource;
import org.apache.stanbol.commons.web.base.LinkResource;
import org.apache.stanbol.commons.web.base.NavigationLink;
import org.apache.stanbol.commons.web.base.ScriptResource;
import org.apache.stanbol.commons.web.base.WebFragment;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;

@Component(immediate = true, metatype = true)
@Service
public class CMSAdapterWebFragment implements WebFragment {

    private static final Logger logger = LoggerFactory.getLogger(CMSAdapterWebFragment.class);

    private static final String NAME = "cmsadapter";

    private static final String STATIC_RESOURCE_PATH = "/org/apache/stanbol/cmsadapter/web/static";
    private static final String TEMPLATE_PATH = "/org/apache/stanbol/cmsadapter/web/templates";

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
    public String getStaticResourceClassPath() {
        return STATIC_RESOURCE_PATH;
    }

    @Override
    public Set<Class<?>> getJaxrsResourceClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(BridgeDefinitionsResource.class);
        classes.add(NotifyResource.class);
        classes.add(ObjectTypesResource.class);
        classes.add(RootResource.class);
        classes.add(RDFMapperResource.class);
        classes.add(ContenthubFeedResource.class);
        classes.add(SessionResource.class);
        return classes;
    }

    @Override
    public Set<Object> getJaxrsResourceSingletons() {
        Set<Object> singletons = new HashSet<Object>();
        try {
            singletons.add(new JAXBProvider());
        } catch (Exception e) {
            logger.warn("Error in creating JAXB provider, ", e);
        }
        return singletons;
    }

    @Override
    public TemplateLoader getTemplateLoader() {
        return new ClassTemplateLoader(getClass(), TEMPLATE_PATH);
    }

    @Override
    public List<LinkResource> getLinkResources() {
        return new ArrayList<LinkResource>();
    }

    @Override
    public List<ScriptResource> getScriptResources() {
        return new ArrayList<ScriptResource>();
    }

    @Override
    public List<NavigationLink> getNavigationLinks() {
        List<NavigationLink> links = new ArrayList<NavigationLink>();
        links.add(new NavigationLink("cmsadapter", "/cmsadapter", "/imports/cmsadapterDescription.ftl", 80));
        return links;
    }

    @Override
    public BundleContext getBundleContext() {
        return bundleContext;
    }

}
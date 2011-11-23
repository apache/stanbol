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
package org.apache.stanbol.commons.jobs.web;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.jobs.web.resources.JobsResource;
import org.apache.stanbol.commons.jobs.web.writers.JobInfoWriter;
import org.apache.stanbol.commons.web.base.LinkResource;
import org.apache.stanbol.commons.web.base.NavigationLink;
import org.apache.stanbol.commons.web.base.ScriptResource;
import org.apache.stanbol.commons.web.base.WebFragment;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;

@Component(immediate = true, metatype = true)
@Service(WebFragment.class)
public class JobsFragment implements WebFragment{

    private static final String NAME = "jobs";
    private static final String STATIC_RESOURCE_PATH = "/org/apache/stanbol/commons/jobs/web/static";
    private static final String TEMPLATE_PATH = "/org/apache/stanbol/commons/jobs/web/templates";
    
    private BundleContext bundleContext;
    
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
        classes.add(JobsResource.class);
        // Writer
        classes.add(JobInfoWriter.class);
        return classes;
    }

    @Override
    public Set<Object> getJaxrsResourceSingletons() {
        return Collections.emptySet();
    }

    @Override
    public TemplateLoader getTemplateLoader() {
        return new ClassTemplateLoader(getClass(), TEMPLATE_PATH);
    }

    @Override
    public List<LinkResource> getLinkResources() {
        return Collections.emptyList();
    }

    @Override
    public List<ScriptResource> getScriptResources() {
        return Collections.emptyList();
    }

    @Override
    public List<NavigationLink> getNavigationLinks() {
        return Collections.emptyList();
    }

    @Override
    public BundleContext getBundleContext() {
        return this.bundleContext;
    }

    @Activate
    protected void activate(ComponentContext ctx) {
        this.bundleContext = ctx.getBundleContext();
    }
}

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
package org.apache.stanbol.factstore.web;

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
import org.apache.stanbol.factstore.api.FactStore;
import org.apache.stanbol.factstore.web.resource.FactStoreRootResource;
import org.apache.stanbol.factstore.web.resource.FactsResource;
import org.apache.stanbol.factstore.web.resource.QueryResource;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;

/**
 * The FactStore web fragment registers the FactStore documentation and REST
 * resources at the global Stanbol Jersey module.
 * 
 * @author Fabian Christ
 */
@Component(immediate = true, metatype = true)
@Service
public class FactStoreWebFragment implements WebFragment {

    public static final String NAME = "factstore";
    
    private static final String STATIC_RESOURCE_PATH = "/org/apache/stanbol/factstore/web/static";

    private static final String TEMPLATE_PATH = "/org/apache/stanbol/factstore/web/templates";

    private BundleContext bundleContext;
    
    @Reference
    FactStore factStore;
    
    @Activate
    protected void activate(ComponentContext ctx) {
        this.bundleContext = ctx.getBundleContext();
    }

    @Override
    public BundleContext getBundleContext() {
        return this.bundleContext;
    }

    @Override
    public Set<Class<?>> getJaxrsResourceClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(FactStoreRootResource.class);
        classes.add(FactsResource.class);
        classes.add(QueryResource.class);
        return classes;
    }

    @Override
    public Set<Object> getJaxrsResourceSingletons() {
        return Collections.emptySet();
    }
    
    @Override
    public List<LinkResource> getLinkResources() {
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return NAME;
    }
    
    @Override
    public List<NavigationLink> getNavigationLinks() {
        List<NavigationLink> navList = new ArrayList<NavigationLink>();
        navList.add(new NavigationLink("factstore", "/factstore", "/imports/abstract.ftl", 30));
        return navList;
    }
    
    @Override
    public List<ScriptResource> getScriptResources() {
        List<ScriptResource> scripts = new ArrayList<ScriptResource>();
        scripts.add(new ScriptResource("text/javascript", "scripts/json2.js", this, 10));
        return scripts;
    }

    @Override
    public String getStaticResourceClassPath() {
        return STATIC_RESOURCE_PATH;
    }

    @Override
    public TemplateLoader getTemplateLoader() {
        return new ClassTemplateLoader(getClass(), TEMPLATE_PATH);
    }

}

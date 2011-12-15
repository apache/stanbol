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
package org.apache.stanbol.entityhub.jersey.fragment;

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
import org.apache.stanbol.entityhub.jersey.parsers.FieldQueryReader;
import org.apache.stanbol.entityhub.jersey.parsers.RepresentationReader;
import org.apache.stanbol.entityhub.jersey.resource.EntityhubRootResource;
import org.apache.stanbol.entityhub.jersey.resource.ReferencedSiteRootResource;
import org.apache.stanbol.entityhub.jersey.resource.SiteManagerRootResource;
import org.apache.stanbol.entityhub.jersey.writers.JettisonWriter;
import org.apache.stanbol.entityhub.jersey.writers.QueryResultListWriter;
import org.apache.stanbol.entityhub.jersey.writers.RepresentationWriter;
import org.apache.stanbol.entityhub.jersey.writers.SignWriter;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;

@Component(immediate = true, metatype = true)
@Service
public class EntityhubWebFragment implements WebFragment {
    
    private static final String NAME = "entityhub";
    
    private static final String STATIC_RESOURCE_PATH = "/org/apache/stanbol/entityhub/jersey/static";

    private static final String TEMPLATE_PATH = "/org/apache/stanbol/entityhub/jersey/templates";

    private BundleContext bundleContext;

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
        // resources
        classes.add(EntityhubRootResource.class);
        //classes.add(EntityMappingResource.class);
        classes.add(ReferencedSiteRootResource.class);
        classes.add(SiteManagerRootResource.class);
        //classes.add(SymbolResource.class);
        // message body readers and writers
        classes.add(FieldQueryReader.class);
        classes.add(QueryResultListWriter.class);
        classes.add(SignWriter.class);
        classes.add(RepresentationWriter.class);
        classes.add(FieldQueryReader.class);
        classes.add(RepresentationReader.class);
        //TODO: somehow writing of Json has not worked because of
        //      A message body writer for Java class org.codehaus.jettison.json.JSONArray,
        //     and Java type class org.codehaus.jettison.json.JSONArray, and MIME media
        //     type application/json was not found
        //     As a workaround I have implemented this small workaround!
        classes.add(JettisonWriter.class);
        
        return classes;
    }
    
    @Override
    public Set<Object> getJaxrsResourceSingletons() {
        return Collections.emptySet();
    }
    
    @Override
    public List<LinkResource> getLinkResources() {
        List<LinkResource> resources = new ArrayList<LinkResource>();
        resources.add(new LinkResource("stylesheet", "style/entityhub.css", this, 0));
        return resources;
    }
    
    @Override
    public String getName() {
        return NAME;
    }
    
    @Override
    public List<NavigationLink> getNavigationLinks() {
        List<NavigationLink> navList = new ArrayList<NavigationLink>();
        navList.add(new NavigationLink("entityhub", "/entityhub", null, 30));
        return navList;
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
}

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
package org.apache.stanbol.ontologymanager.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.base.LinkResource;
import org.apache.stanbol.commons.web.base.NavigationLink;
import org.apache.stanbol.commons.web.base.ScriptResource;
import org.apache.stanbol.commons.web.base.WebFragment;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.registry.api.RegistryManager;
import org.apache.stanbol.ontologymanager.servicesapi.OfflineConfiguration;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.servicesapi.session.SessionManager;
import org.apache.stanbol.ontologymanager.web.resources.OntoNetRootResource;
import org.apache.stanbol.ontologymanager.web.resources.RegistryManagerResource;
import org.apache.stanbol.ontologymanager.web.resources.ScopeManagerResource;
import org.apache.stanbol.ontologymanager.web.resources.ScopeResource;
import org.apache.stanbol.ontologymanager.web.resources.SessionManagerResource;
import org.apache.stanbol.ontologymanager.web.resources.SessionResource;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;


/**
 * Implementation of WebFragment for the Stanbol Ontology Manager endpoint.
 * 
 * @author alberto musetti
 * 
 */

@Component(immediate = true, metatype = false)
@Service(WebFragment.class)
public class OntologyManagerWebFragment implements WebFragment {

    private static final String NAME = "ontonet";

    private BundleContext bundleContext;

    @Reference
    OfflineConfiguration offline;

    @Reference
    OntologyProvider<?> ontologyProvider;

    @Reference
    RegistryManager regMgr;

    @Reference
    ONManager scopeMgr;

    @Reference
    SessionManager sessionMgr;

    @Reference
    TcManager tcManager;

    @Activate
    protected void activate(ComponentContext ctx) {
        this.bundleContext = ctx.getBundleContext();
    }

    @Override
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    @Override
    public Set<Class<?>> getJaxrsResourceClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();

        // Core resources
        classes.add(OntoNetRootResource.class);

        // Registry resources
        classes.add(RegistryManagerResource.class);

        // Scope resources
        classes.add(ScopeManagerResource.class);
        classes.add(ScopeResource.class);

        // Session resources
        classes.add(SessionManagerResource.class);
        classes.add(SessionResource.class);

        return classes;
    }

    @Override
    public Set<Object> getJaxrsResourceSingletons() {
        return Collections.emptySet();
    }

    @Override
    public List<LinkResource> getLinkResources() {
        List<LinkResource> resources = new ArrayList<LinkResource>();
        resources.add(new LinkResource("stylesheet", "style/ontonet.css", this, 10));
        return resources;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<NavigationLink> getNavigationLinks() {
        List<NavigationLink> links = new ArrayList<NavigationLink>();
        links.add(new NavigationLink(NAME, "/" + NAME, "/imports/ontonetDescription.ftl", 50));
        return links;
    }

    @Override
    public List<ScriptResource> getScriptResources() {
        List<ScriptResource> resources = new ArrayList<ScriptResource>();
        resources.add(new ScriptResource("text/javascript", "scripts/actions.js", this, 10));
        return resources;
    }


}

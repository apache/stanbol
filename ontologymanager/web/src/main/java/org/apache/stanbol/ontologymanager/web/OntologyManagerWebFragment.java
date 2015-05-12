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
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.base.LinkResource;
import org.apache.stanbol.commons.web.base.NavigationLink;
import org.apache.stanbol.commons.web.base.ScriptResource;
import org.apache.stanbol.commons.web.base.WebFragment;


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

    private static final String htmlDescription = 
            "A <strong>controlled environment</strong> for managing Web ontologies, "+
            "<strong>ontology networks</strong> and user sessions that put them to use.";

    @Override
    public Set<Class<?>> getJaxrsResourceClasses() {
        //Set<Class<?>> classes = new HashSet<Class<?>>();

        // Core resources
        //classes.add(OntoNetRootResource.class);

        // Registry resources
        //classes.add(RegistryManagerResource.class);

        // Scope resources
        //classes.add(ScopeManagerResource.class);
        //classes.add(ScopeResource.class);

        // Session resources
        //classes.add(SessionManagerResource.class);
        //classes.add(SessionResource.class);

        //return classes;
        return Collections.emptySet();
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
        return Collections.emptyList();
    }

    @Override
    public List<ScriptResource> getScriptResources() {
        List<ScriptResource> resources = new ArrayList<ScriptResource>();
        resources.add(new ScriptResource("text/javascript", "scripts/actions.js", this, 10));
        return resources;
    }


}

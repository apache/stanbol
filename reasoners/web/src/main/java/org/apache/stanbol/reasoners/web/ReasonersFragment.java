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
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.base.LinkResource;
import org.apache.stanbol.commons.web.base.NavigationLink;
import org.apache.stanbol.commons.web.base.ScriptResource;
import org.apache.stanbol.commons.web.base.WebFragment;


/**
 * Implementation of WebFragment for the Stanbol Reasoner end-point.
 * 
 * @author Alberto Musetti
 * 
 */

@Component(immediate = true, metatype = false)
@Service(WebFragment.class)
public class ReasonersFragment implements WebFragment {

    private static final String NAME = "reasoners";

    private static final String htmlDescription = 
            "The entry point to multiple <strong>reasoning services</strong> that are used for"+
            "obtaining unexpressed additional knowledge from the explicit axioms in an ontology."+
            "Multiple reasoning profiles are available, each with its expressive power and computational cost.";

    @Override
    public Set<Class<?>> getJaxrsResourceClasses() {
        //Set<Class<?>> classes = new HashSet<Class<?>>();
        // Reasoner
        // classes.add(ReasonersResource.class);
        //classes.add(ReasoningServicesResource.class);
        //classes.add(ReasoningServiceTaskResource.class);
        //classes.add(JobsResource.class);

        // Writer
        //classes.add(JenaModelWriter.class);
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
        resources.add(new LinkResource("stylesheet", "css/reasoners.css", this, 10));
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
        return Collections.emptyList();
    }

}

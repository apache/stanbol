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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.base.LinkResource;
import org.apache.stanbol.commons.web.base.NavigationLink;
import org.apache.stanbol.commons.web.base.ScriptResource;
import org.apache.stanbol.commons.web.base.WebFragment;

/**
 * Statically define the list of available resources and providers to be contributed to the the Stanbol JAX-RS
 * Endpoint.
 */
@Component(immediate = true)
@Service
public class EnhancerWebFragment implements WebFragment {

    private static final String NAME = "enhancer";


	private static final String htmlDescription = 
			"This is a <strong>stateless interface</strong> to allow clients to submit"+
			"content to <strong>analyze</strong> by the <code>EnhancementEngine</code>s"+
			"and get the resulting <strong>RDF enhancements</strong> at once without"+
			"storing anything on the server-side.";


    @Override
    public String getName() {
        return NAME;
    }


    @Override
    public Set<Class<?>> getJaxrsResourceClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        // resources
        //classes.add(EnginesRootResource.class);
        //classes.add(EnhancerRootResource.class);
        //classes.add(EnhancementChainResource.class);
        //classes.add(ChainsRootResource.class);
        //classes.add(EnhancementEnginesRootResource.class);
        //classes.add(EnhancementEngineResource.class);
        //Reader/Writer for ContentItems
        //classes.add(ContentItemReader.class);
        //classes.add(ContentItemWriter.class);
        return classes;
    }

    @Override
    public Set<Object> getJaxrsResourceSingletons() {
        return Collections.emptySet();
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
        return Collections.emptyList();
    }

}

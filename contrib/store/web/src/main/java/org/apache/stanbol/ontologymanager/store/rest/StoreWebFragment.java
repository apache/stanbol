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
package org.apache.stanbol.ontologymanager.store.rest;

import java.util.ArrayList;
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
import org.apache.stanbol.ontologymanager.store.rest.resources.Ontologies;
import org.apache.stanbol.ontologymanager.store.rest.resources.OntologyClasses;
import org.apache.stanbol.ontologymanager.store.rest.resources.OntologyDatatypeProperties;
import org.apache.stanbol.ontologymanager.store.rest.resources.OntologyImports;
import org.apache.stanbol.ontologymanager.store.rest.resources.OntologyIndividuals;
import org.apache.stanbol.ontologymanager.store.rest.resources.OntologyObjectProperties;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularClass;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularClassDisjointClasses;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularClassEquivalentClasses;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularClassSuperClasses;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularDatatypeProperty;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularDatatypePropertyDomains;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularDatatypePropertyRanges;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularDatatypePropertySuperProperties;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularIndividual;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularIndividualPropertyAssertions;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularIndividualTypes;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularObjectProperty;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularObjectPropertyDomains;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularObjectPropertyRanges;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularObjectPropertySuperProperties;
import org.apache.stanbol.ontologymanager.store.rest.resources.ParticularOntology;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;

@Component(immediate = true, metatype = true)
@Service
public class StoreWebFragment implements WebFragment {
    private static final Logger log = LoggerFactory.getLogger(StoreWebFragment.class);
    private static final String NAME = "ontology";
    
    private BundleContext bundleContext;

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
        classes.add(Ontologies.class);
        classes.add(OntologyClasses.class);
        classes.add(OntologyDatatypeProperties.class);
        classes.add(OntologyObjectProperties.class);
        classes.add(OntologyIndividuals.class);
        classes.add(OntologyImports.class);
        classes.add(ParticularClass.class);
        classes.add(ParticularDatatypeProperty.class);
        classes.add(ParticularOntology.class);
        classes.add(ParticularObjectProperty.class);
        classes.add(ParticularIndividual.class);
        classes.add(ParticularClassSuperClasses.class);
        classes.add(ParticularClassDisjointClasses.class);
        classes.add(ParticularClassEquivalentClasses.class);
        classes.add(ParticularObjectPropertyRanges.class);
        classes.add(ParticularObjectPropertyDomains.class);
        classes.add(ParticularObjectPropertySuperProperties.class);
        classes.add(ParticularDatatypePropertyDomains.class);
        classes.add(ParticularDatatypePropertyRanges.class);
        classes.add(ParticularDatatypePropertySuperProperties.class);
        classes.add(ParticularIndividualTypes.class);
        classes.add(ParticularIndividualPropertyAssertions.class);

        return classes;
    }

    @Override
    public Set<Object> getJaxrsResourceSingletons() {
        Set<Object> singletons = new HashSet<Object>();
        try {
            singletons.add(new JAXBProvider());
        } catch (Exception e) {
            log.warn("Error in creating JAXB provider, ", e);
        }
        return singletons;
    }


    @Override
    public List<LinkResource> getLinkResources() {
        List<LinkResource> resources = new ArrayList<LinkResource>();
        resources.add(new LinkResource("stylesheet", "style/store.css", this, 0));
        return resources;
    }

    @Override
    public List<ScriptResource> getScriptResources() {
        List<ScriptResource> resources = new ArrayList<ScriptResource>();
        resources.add(new ScriptResource("text/javascript", "scripts/paging.js", this, 0));
        resources.add(new ScriptResource("text/javascript", "scripts/propertyUpdater.js", this, 1));
        resources.add(new ScriptResource("text/javascript", "scripts/requestResponse.js", this, 2));
        resources.add(new ScriptResource("text/javascript", "scripts/individualUpdater.js", this, 3));
        return resources;
    }

    @Override
    public List<NavigationLink> getNavigationLinks() {
        List<NavigationLink> links = new ArrayList<NavigationLink>();
        links.add(new NavigationLink("ontology", "/ontology", "/imports/storeDescription.ftl", 70));
        return links;
    }

}

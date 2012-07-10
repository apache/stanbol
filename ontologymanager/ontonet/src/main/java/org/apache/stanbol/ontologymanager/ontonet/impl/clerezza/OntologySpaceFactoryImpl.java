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
package org.apache.stanbol.ontologymanager.ontonet.impl.clerezza;

import java.io.IOException;
import java.util.Dictionary;

import org.apache.clerezza.rdf.core.access.TcProvider;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.OntologyCollectorListener;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.CoreOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.CustomOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologySpace.SpaceType;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologySpaceFactory;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link OntologySpaceFactory} based on Clerezza.
 * 
 * @author alexdma
 * 
 */
@Component(immediate = true, metatype = false)
@Service(OntologySpaceFactory.class)
public class OntologySpaceFactoryImpl implements OntologySpaceFactory {

    protected Logger log = LoggerFactory.getLogger(getClass());

    protected IRI namespace;

    @Reference
    private OntologyProvider<TcProvider> ontologyProvider;

    public OntologySpaceFactoryImpl() {
        super();
    }

    public OntologySpaceFactoryImpl(OntologyProvider<TcProvider> provider,
                                    Dictionary<String,Object> configuration) {
        this.ontologyProvider = provider;
        try {
            activate(configuration);
        } catch (IOException e) {
            log.error("Unable to access servlet context.", e);
        }
    }

    /**
     * Used to configure an instance within an OSGi container.
     * 
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) throws IOException {
        log.info("in " + OntologySpaceFactoryImpl.class + " activate with context " + context);
        if (context == null) {
            throw new IllegalStateException("No valid" + ComponentContext.class + " parsed in activate!");
        }
        activate((Dictionary<String,Object>) context.getProperties());
    }

    /**
     * Called within both OSGi and non-OSGi environments.
     * 
     * @param configuration
     * @throws IOException
     */
    protected void activate(Dictionary<String,Object> configuration) throws IOException {
        log.debug(OntologySpaceFactoryImpl.class + " activated.");
    }

    /**
     * Utility method for configuring ontology spaces after creating them.
     * 
     * @param s
     * @param scopeID
     * @param rootSource
     */
    private void configureSpace(OntologySpace s, String scopeID, OntologyInputSource<?,?>... ontologySources) {
        // // FIXME: ensure that this is not null AND convert to using Strings for scope IDs
        // OntologyScope parentScope = registry.getScope(scopeID);
        //
        // if (parentScope != null && parentScope instanceof OntologyCollectorListener) s
        // .addListener((OntologyCollectorListener) parentScope);

        // Make sure the ontology provider listens to ontology additions before core ontologies are added.
        if (ontologyProvider instanceof OntologyCollectorListener) s
                .addOntologyCollectorListener((OntologyCollectorListener) ontologyProvider);

        // Set the supplied ontology's parent as the root for this space.
        if (ontologySources != null) try {
            for (OntologyInputSource<?,?> src : ontologySources)
                s.addOntology(src);
        } catch (UnmodifiableOntologyCollectorException e) {
            log.error("Ontology space " + s.getID() + " was found locked at creation time!", e);
        }
        // s.setUp();
    }

    @Override
    public CoreOntologySpace createCoreOntologySpace(String scopeId, OntologyInputSource<?,?>... coreSources) {
        CoreOntologySpace s = new CoreSpaceImpl(scopeId, namespace, ontologyProvider);
        configureSpace(s, scopeId, coreSources);
        return s;
    }

    @Override
    public CustomOntologySpace createCustomOntologySpace(String scopeId,
                                                         OntologyInputSource<?,?>... customSources) {
        CustomOntologySpace s = new CustomSpaceImpl(scopeId, namespace, ontologyProvider);
        configureSpace(s, scopeId, customSources);
        return s;
    }

    @Override
    public OntologySpace createOntologySpace(String scopeId,
                                             SpaceType type,
                                             OntologyInputSource<?,?>... ontologySources) {
        switch (type) {
            case CORE:
                return createCoreOntologySpace(scopeId, ontologySources);
            case CUSTOM:
                return createCustomOntologySpace(scopeId, ontologySources);
            default:
                return null;
        }
    }

    /**
     * Deactivation of the ONManagerImpl resets all its resources.
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {
        namespace = null;
        log.info("in " + OntologySpaceFactoryImpl.class + " deactivate with context " + context);
    }

    @Override
    public String getID() {
        return this.toString();
    }

    @Override
    public IRI getNamespace() {
        return this.namespace;
    }

    @Override
    public void setNamespace(IRI namespace) {
        this.namespace = namespace;
    }

}

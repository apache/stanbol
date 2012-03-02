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
package org.apache.stanbol.ontologymanager.ontonet.impl.owlapi;

import org.apache.stanbol.commons.owl.OWLOntologyManagerFactory;
import org.apache.stanbol.ontologymanager.ontonet.api.OfflineConfiguration;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.OntologyCollectorListener;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.CoreOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.CustomOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologySpaceFactory;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.ScopeRegistry;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.SessionOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologySpace.SpaceType;
import org.semanticweb.owlapi.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class that generates default implementations of the three types of ontology scope.
 * 
 */
public class OntologySpaceFactoryImpl implements OntologySpaceFactory {

    protected Logger log = LoggerFactory.getLogger(getClass());

    protected IRI namespace;

    protected OfflineConfiguration offline;

    protected ScopeRegistry registry;

    public OntologySpaceFactoryImpl(ScopeRegistry registry,

    OfflineConfiguration offline, IRI namespace) {
        this.registry = registry;

        this.offline = offline;
        this.namespace = namespace;
    }

    /**
     * Utility method for configuring ontology spaces after creating them.
     * 
     * @param s
     * @param scopeID
     * @param rootSource
     */
    private void configureSpace(OntologySpace s, String scopeID, OntologyInputSource<?,?>... ontologySources) {
        // FIXME: ensure that this is not null AND convert to using Strings for scope IDs
        OntologyScope parentScope = registry.getScope(scopeID);

        if (parentScope != null && parentScope instanceof OntologyCollectorListener) s
                .addListener((OntologyCollectorListener) parentScope);
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
        CoreOntologySpace s = new CoreOntologySpaceImpl(scopeId, namespace, /* storage, */
        OWLOntologyManagerFactory.createOWLOntologyManager(offline.getOntologySourceLocations().toArray(
            new IRI[0])));
        configureSpace(s, scopeId, coreSources);
        return s;
    }

    @Override
    public CustomOntologySpace createCustomOntologySpace(String scopeId,
                                                         OntologyInputSource<?,?>... customSources) {
        CustomOntologySpace s = new CustomOntologySpaceImpl(scopeId, namespace, /* storage, */
        OWLOntologyManagerFactory.createOWLOntologyManager(offline.getOntologySourceLocations().toArray(
            new IRI[0])));
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
            case SESSION:
                return createSessionOntologySpace(scopeId, ontologySources);
            default:
                return null;
        }
    }

    @Override
    public SessionOntologySpace createSessionOntologySpace(String scopeId,
                                                           OntologyInputSource<?,?>... sessionSources) {
        SessionOntologySpace s = new SessionOntologySpaceImpl(scopeId, namespace, /* storage, */
        OWLOntologyManagerFactory.createOWLOntologyManager(offline.getOntologySourceLocations().toArray(
            new IRI[0])));
        for (OntologyInputSource<?,?> src : sessionSources)
            try {
                s.addOntology(src);
            } catch (UnmodifiableOntologyCollectorException e) {
                // Should never happen anyway...
                continue;
            }
        // s.setUp();
        return s;
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

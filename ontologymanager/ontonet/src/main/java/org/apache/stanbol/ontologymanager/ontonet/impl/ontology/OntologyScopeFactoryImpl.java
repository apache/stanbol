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
package org.apache.stanbol.ontologymanager.ontonet.impl.ontology;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.stanbol.ontologymanager.ontonet.api.collector.DuplicateIDException;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologyScopeFactory;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologySpaceFactory;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.ScopeEventListener;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.ScopeRegistry;
import org.semanticweb.owlapi.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class that instantiates default implementations of ontology scope.
 * 
 * @author alexdma
 * 
 */
public class OntologyScopeFactoryImpl implements OntologyScopeFactory {

    private Set<ScopeEventListener> listeners = new HashSet<ScopeEventListener>();
    private Logger log = LoggerFactory.getLogger(getClass());
    protected IRI namespace;

    protected ScopeRegistry registry;
    protected OntologySpaceFactory spaceFactory;

    public OntologyScopeFactoryImpl(ScopeRegistry registry, IRI namespace, OntologySpaceFactory spaceFactory) {

        this.registry = registry;
        this.spaceFactory = spaceFactory;
        this.namespace = namespace;

        if (!getNamespace().equals(spaceFactory.getNamespace())) log
                .warn(
                    "Scope factory namespace {} differs from space factory namespace {} . This is not illegal but strongly discouraged.",
                    getNamespace(), spaceFactory.getNamespace());
    }

    @Override
    public void addScopeEventListener(ScopeEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void clearScopeEventListeners() {
        listeners.clear();
    }

    @Override
    public OntologyScope createOntologyScope(String scopeID, OntologyInputSource<?,?>... coreSources) throws DuplicateIDException {

        if (registry.containsScope(scopeID)) throw new DuplicateIDException(scopeID,
                "Scope registry already contains ontology scope with ID " + scopeID);
        OntologyScope scope = new OntologyScopeImpl(scopeID, getNamespace(), spaceFactory, coreSources);
        // scope.addOntologyScopeListener(ONManager.get().getOntologyIndex());
        // TODO : manage scopes with null core ontologies
        fireScopeCreated(scope);
        return scope;
    }

    protected void fireScopeCreated(OntologyScope scope) {
        for (ScopeEventListener l : listeners)
            l.scopeCreated(scope);
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
    public Collection<ScopeEventListener> getScopeEventListeners() {
        return listeners;
    }

    @Override
    public void removeScopeEventListener(ScopeEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void setNamespace(IRI namespace) {
        this.namespace = namespace;
    }

}

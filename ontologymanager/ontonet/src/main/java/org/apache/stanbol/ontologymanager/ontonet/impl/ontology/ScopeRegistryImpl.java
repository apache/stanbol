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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.stanbol.ontologymanager.ontonet.api.scope.NoSuchScopeException;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.ScopeEventListener;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.ScopeRegistry;

/**
 * Default implementation of an ontology scope registry.
 * 
 * @author alexdma
 * 
 */
public class ScopeRegistryImpl implements ScopeRegistry {

    private Set<String> activeScopeIRIs;

    private Set<ScopeEventListener> scopeListeners;

    private Map<String,OntologyScope> scopeMap;

    public ScopeRegistryImpl() {
        scopeMap = new HashMap<String,OntologyScope>();
        activeScopeIRIs = new HashSet<String>();
        scopeListeners = new HashSet<ScopeEventListener>();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * eu.iksproject.kres.api.manager.ontology.ScopeRegistry#addScopeRegistrationListener(eu.iksproject.kres
     * .api.manager.ontology.ScopeEventListener)
     */
    @Override
    public void addScopeRegistrationListener(ScopeEventListener listener) {
        scopeListeners.add(listener);

    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.iksproject.kres.api.manager.ontology.ScopeRegistry#clearScopeRegistrationListeners()
     */
    @Override
    public void clearScopeRegistrationListeners() {
        scopeListeners.clear();

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * eu.iksproject.kres.api.manager.ontology.ScopeRegistry#containsScope(org.semanticweb.owlapi.model.IRI)
     */
    @Override
    public boolean containsScope(String scopeID) {
        // containsKey() is not reliable enough
        return scopeMap.get(scopeID) != null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * eu.iksproject.kres.api.manager.ontology.ScopeRegistry#deregisterScope(eu.iksproject.kres.api.manager
     * .ontology.OntologyScope)
     */
    @Override
    public synchronized void deregisterScope(OntologyScope scope) {
        String id = scope.getID();
        if (!containsScope(id)) throw new NoSuchScopeException(id);
        // For sure it is deactivated...
        setScopeActive(id, false);
        // activeScopeIRIs.remove(id);
        scopeMap.remove(id);
        fireScopeDeregistered(scope);
    }

    protected void fireScopeActivationChange(String scopeID, boolean activated) {
        OntologyScope scope = scopeMap.get(scopeID);
        if (activated) for (ScopeEventListener l : scopeListeners)
            l.scopeActivated(scope);
        else for (ScopeEventListener l : scopeListeners)
            l.scopeDeactivated(scope);
    }

    /**
     * Notifies all registered scope listeners that an ontology scope has been removed.
     * 
     * @param scope
     *            the scope that was removed.
     */
    protected void fireScopeDeregistered(OntologyScope scope) {
        for (ScopeEventListener l : scopeListeners)
            l.scopeDeregistered(scope);
    }

    /**
     * Notifies all registered scope listeners that an ontology scope has been added.
     * 
     * @param scope
     *            the scope that was added.
     */
    protected void fireScopeRegistered(OntologyScope scope) {
        for (ScopeEventListener l : scopeListeners)
            l.scopeRegistered(scope);
    }

    @Override
    public Set<OntologyScope> getActiveScopes() {
        Set<OntologyScope> scopes = new HashSet<OntologyScope>();
        for (String id : activeScopeIRIs)
            scopes.add(scopeMap.get(id));
        return scopes;
    }

    @Override
    public synchronized Set<OntologyScope> getRegisteredScopes() {
        return new HashSet<OntologyScope>(scopeMap.values());
    }

    @Override
    public OntologyScope getScope(String scopeID) {
        return scopeMap.get(scopeID);
    }

    @Override
    public Set<ScopeEventListener> getScopeRegistrationListeners() {
        return scopeListeners;
    }

    @Override
    public boolean isScopeActive(String scopeID) {
        if (!containsScope(scopeID)) throw new NoSuchScopeException(scopeID);
        return activeScopeIRIs.contains(scopeID);
    }

    @Override
    public synchronized void registerScope(OntologyScope scope) {
        registerScope(scope, false);
    }

    @Override
    public synchronized void registerScope(OntologyScope scope, boolean activate) {
        scopeMap.put(scope.getID(), scope);
        setScopeActive(scope.getID(), activate);
        fireScopeRegistered(scope);
    }

    @Override
    public void removeScopeRegistrationListener(ScopeEventListener listener) {
        scopeListeners.remove(listener);
    }

    @Override
    public void setScopeActive(String scopeID, boolean active) {
        if (!containsScope(scopeID)) throw new NoSuchScopeException(scopeID);
        // Prevent no-changes from firing events.
        boolean previousStatus = isScopeActive(scopeID);
        OntologyScope scope = getScope(scopeID);
        if (active == previousStatus) return;
        if (active) {
            scope.setUp();
            activeScopeIRIs.add(scopeID);
        } else {
            scope.tearDown();
            activeScopeIRIs.remove(scopeID);
        }
        fireScopeActivationChange(scopeID, active);
    }
}

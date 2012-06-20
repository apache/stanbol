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

    protected Set<String> activeScopeIRIs;

    protected Set<ScopeEventListener> listeners;

    protected Map<String,OntologyScope> scopeMap;

    public ScopeRegistryImpl() {
        scopeMap = new HashMap<String,OntologyScope>();
        activeScopeIRIs = new HashSet<String>();
        listeners = new HashSet<ScopeEventListener>();
    }

    @Override
    public void addScopeRegistrationListener(ScopeEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void clearScopeRegistrationListeners() {
        listeners.clear();
    }

    @Override
    public boolean containsScope(String scopeID) {
        // containsKey() is not reliable enough
        return scopeMap.get(scopeID) != null;
    }

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
        if (activated) for (ScopeEventListener l : listeners)
            l.scopeActivated(scope);
        else for (ScopeEventListener l : listeners)
            l.scopeDeactivated(scope);
    }

    /**
     * Notifies all registered scope listeners that an ontology scope has been removed.
     * 
     * @param scope
     *            the scope that was removed.
     */
    protected void fireScopeDeregistered(OntologyScope scope) {
        for (ScopeEventListener l : listeners)
            l.scopeDeregistered(scope);
    }

    /**
     * Notifies all registered scope listeners that an ontology scope has been added.
     * 
     * @param scope
     *            the scope that was added.
     */
    protected void fireScopeRegistered(OntologyScope scope) {
        for (ScopeEventListener l : listeners)
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
        return listeners;
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
        listeners.remove(listener);
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

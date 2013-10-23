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
package org.apache.stanbol.ontologymanager.registry.impl.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.stanbol.ontologymanager.registry.api.IllegalRegistryCycleException;
import org.apache.stanbol.ontologymanager.registry.api.RegistryContentException;
import org.apache.stanbol.ontologymanager.registry.api.RegistryContentListener;
import org.apache.stanbol.ontologymanager.registry.api.RegistryOperation;
import org.apache.stanbol.ontologymanager.registry.api.model.RegistryItem;
import org.semanticweb.owlapi.model.IRI;

/**
 * Abstract default implementation of registry items.
 * 
 * @author alexdma
 * 
 */
public abstract class AbstractRegistryItem implements RegistryItem {

    /* Two-way adjacency index TODO use maps instead? */
    protected Map<IRI,RegistryItem> children = new HashMap<IRI,RegistryItem>(),
            parents = new HashMap<IRI,RegistryItem>();

    private IRI iri;

    protected Set<RegistryContentListener> listeners = new HashSet<RegistryContentListener>();

    private String name;

    public AbstractRegistryItem(IRI iri) {
        this.iri = iri;
    }

    public AbstractRegistryItem(IRI iri, String name) {
        this(iri);
        setName(name);
    }

    @Override
    public void addChild(RegistryItem child) throws RegistryContentException {
        if (this.equals(child) || parents.values().contains(child)) throw new IllegalRegistryCycleException(
                this, child, RegistryOperation.ADD_CHILD);
        if (!children.values().contains(child)) {
            children.put(child.getIRI(), child);
            try {
                child.addParent(this);
            } catch (RegistryContentException e) {
                // Shouldn't happen. null is always legal.
            }
        }
    }

    @Override
    public void addParent(RegistryItem parent) throws RegistryContentException {
        if (this.equals(parent) || children.values().contains(parent)) throw new IllegalRegistryCycleException(
                this, parent, RegistryOperation.ADD_PARENT);
        if (!parents.values().contains(parent)) {
            parents.put(parent.getIRI(), parent);
            try {
                parent.addChild(this);
            } catch (RegistryContentException e) {
                // Shouldn't happen. null is always legal.
            }
        }
    }

    @Override
    public void addRegistryContentListener(RegistryContentListener listener) {
        listeners.add(listener);
    }

    @Override
    public void clearChildren() {
        for (RegistryItem child : children.values())
            removeChild(child);
    }

    @Override
    public void clearParents() {
        for (RegistryItem parent : parents.values())
            removeParent(parent);
    }

    @Override
    public void clearRegistryContentListeners() {
        listeners.clear();
    }

    protected void fireContentRequested(RegistryItem item) {
        for (RegistryContentListener listener : getRegistryContentListeners())
            listener.registryContentRequested(item);
    }

    @Override
    public RegistryItem getChild(IRI id) {
        return children.get(id);
    }

    @Override
    public RegistryItem[] getChildren() {
        return children.values().toArray(new RegistryItem[children.size()]);
    }

    @Override
    public IRI getIRI() {
        return iri;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public RegistryItem getParent(IRI id) {
        return parents.get(id);
    }

    @Override
    public RegistryItem[] getParents() {
        return parents.values().toArray(new RegistryItem[parents.size()]);
    }

    @Override
    public Set<RegistryContentListener> getRegistryContentListeners() {
        return listeners;
    }

    @Override
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    @Override
    public boolean hasParents() {
        return !parents.isEmpty();
    }

    @Override
    public void prune() {
        clearChildren();
        clearParents();
    }

    @Override
    public void removeChild(RegistryItem child) {
        if (children.values().contains(child)) {
            children.remove(child.getIRI());
            child.removeParent(this);
        }
    }

    @Override
    public void removeParent(RegistryItem parent) {
        if (parents.values().contains(parent)) {
            parents.remove(parent.getIRI());
            parent.removeChild(this);
        }
    }

    @Override
    public void removeRegistryContentListener(RegistryContentListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void setName(String string) {
        this.name = string;
    }

    @Override
    public String toString() {
        return getName();
    }
}

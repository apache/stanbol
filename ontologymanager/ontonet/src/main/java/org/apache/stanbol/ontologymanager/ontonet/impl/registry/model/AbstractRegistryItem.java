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
package org.apache.stanbol.ontologymanager.ontonet.impl.registry.model;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.stanbol.ontologymanager.ontonet.api.registry.RegistryContentException;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.RegistryContentListener;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.RegistryItem;
import org.semanticweb.owlapi.model.IRI;

public abstract class AbstractRegistryItem implements RegistryItem {

    /* Two-way adjacency TODO use maps instead? */
    protected Set<RegistryItem> children = new HashSet<RegistryItem>(),
            parents = new HashSet<RegistryItem>();

    private IRI iri;

    protected Set<RegistryContentListener> listeners = new HashSet<RegistryContentListener>();

    private String name;

    public AbstractRegistryItem(String name) {
        setName(name);
    }

    public AbstractRegistryItem(String name, URL url) throws URISyntaxException {
        this(name);
        setURL(url);
    }

    protected void fireContentRequested(RegistryItem item) {
        for (RegistryContentListener listener : getRegistryContentListeners())
            listener.registryContentRequested(item);
    }

    @Override
    public void addChild(RegistryItem child) throws RegistryContentException {
        if (parents.contains(child)) throw new RegistryContentException("Cannot add parent item " + child
                                                                        + " as a child.");
        if (!children.contains(child)) {
            children.add(child);
            try {
                child.addContainer(this);
            } catch (RegistryContentException e) {
                // Shouldn't happen. null is always legal.
            }
        }
    }

    @Override
    public void addContainer(RegistryItem container) throws RegistryContentException {
        if (children.contains(container)) throw new RegistryContentException("Cannot set child item "
                                                                             + container + " as a parent.");
        if (!parents.contains(container)) {
            parents.add(container);
            container.addChild(this);
        }
    }

    // private RegistryItem parent;

    @Override
    public void addRegistryContentListener(RegistryContentListener listener) {
        listeners.add(listener);
    }

    @Override
    public void clearChildren() {
        for (RegistryItem child : children)
            removeChild(child);
    }

    @Override
    public void clearRegistryContentListeners() {
        listeners.clear();
    }

    @Override
    public RegistryItem[] getChildren() {
        return children.toArray(new RegistryItem[children.size()]);
    }

    @Override
    public RegistryItem[] getContainers() {
        return parents.toArray(new RegistryItem[parents.size()]);
    }

    public String getName() {
        return this.name;
    }

    @Override
    public Set<RegistryContentListener> getRegistryContentListeners() {
        return listeners;
    }

    public URL getURL() {
        try {
            return iri.toURI().toURL();
        } catch (MalformedURLException e) {
            // Will be obsolete once we replace URLs with IRIs
            return null;
        }
    }

    @Override
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    @Override
    public boolean isLibrary() {
        return Type.LIBRARY.equals(getType());
    }

    @Override
    public boolean isOntology() {
        return Type.ONTOLOGY.equals(getType());
    }

    @Override
    public void removeChild(RegistryItem child) {
        if (children.contains(child)) {
            children.remove(child);
            child.removeContainer(this);
        }
    }

    @Override
    public void removeContainer(RegistryItem container) {
        if (parents.contains(container)) {
            parents.remove(container);
            container.removeChild(this);
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
    public void setURL(URL url) throws URISyntaxException {
        this.iri = IRI.create(url);
    }

    @Override
    public String toString() {
        return getName();
    }
}

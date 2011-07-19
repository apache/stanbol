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

import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.RegistryContentException;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.RegistryItem;
import org.semanticweb.owlapi.model.IRI;

public abstract class AbstractRegistryItem implements RegistryItem {

    protected Set<RegistryItem> children = new HashSet<RegistryItem>();

    private IRI iri;

    private String name;

    private RegistryItem parent;

    public AbstractRegistryItem(String name) {
        setName(name);
    }

    public AbstractRegistryItem(String name, URL url) throws URISyntaxException {
        this(name);
        setURL(url);
    }

    @Override
    public void addChild(RegistryItem child) throws RegistryContentException {
        if (child.equals(parent)) throw new RegistryContentException("Cannot add parent item " + parent
                                                                     + " as a child.");
        children.add(child);
        try {
            child.setParent(this);
        } catch (RegistryContentException e) {
            // Shouldn't happen. null is always legal.
        }
    }

    @Override
    public void clearChildren() {
        for (RegistryItem child : children)
            removeChild(child);
    }

    @Override
    public RegistryItem[] getChildren() {
        return children.toArray(new RegistryItem[children.size()]);
    }

    public String getName() {
        return this.name;
    }

    @Override
    public RegistryItem getParent() {
        return parent;
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
    public void removeChild(RegistryItem child) {
        children.remove(child);
        try {
            child.setParent(null);
        } catch (RegistryContentException e) {
            // Shouldn't happen. null is always legal.
        }
    }

    @Override
    public void setName(String string) {
        this.name = string;
    }

    @Override
    public void setParent(RegistryItem parent) throws RegistryContentException {
        if (children.contains(parent)) throw new RegistryContentException("Cannot set child item " + parent
                                                                          + " as a parent.");
        this.parent = parent;
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

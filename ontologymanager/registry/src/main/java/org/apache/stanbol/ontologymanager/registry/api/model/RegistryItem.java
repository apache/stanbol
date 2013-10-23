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
package org.apache.stanbol.ontologymanager.registry.api.model;

import java.util.Set;

import org.apache.stanbol.ontologymanager.registry.api.RegistryContentException;
import org.apache.stanbol.ontologymanager.registry.api.RegistryContentListener;
import org.semanticweb.owlapi.model.IRI;

/**
 * A member of an ontology registry, possibly even the registry itself.
 * 
 * @author alexdma
 */
public interface RegistryItem {

    /**
     * The allowed types of registry item that a registry manager can handle.
     */
    public enum Type {
        /**
         * An ontology library. Contains ontologies, is contained by ontology registries.
         */
        LIBRARY,
        /**
         * An ontology. Contained by libraries.
         */
        ONTOLOGY,
        /**
         * An ontology registry. Contains ontology libraries.
         */
        REGISTRY;
    }

    /**
     * Sets a registry item as a child of this registry item. Also sets itself as a parent of the supplied
     * item. Note that cycles are not allowed.
     * 
     * @param child
     *            the registry item to be added as a child.
     * @throws RegistryContentException
     *             if a cycle is detected or one of the registry items is invalid.
     */
    void addChild(RegistryItem child) throws RegistryContentException;

    /**
     * Sets a registry item as a parent of this registry item. Also sets itself as a child of the supplied
     * item. Note that cycles are not allowed.
     * 
     * @param parent
     *            the registry item to be added as a parent.
     * @throws RegistryContentException
     *             if a cycle is detected or one of the registry items is invalid.
     */
    void addParent(RegistryItem parent) throws RegistryContentException;

    /**
     * Adds the supplied listener to the set of registry content listeners. If the listener is already
     * registered with this registry item, this method has no effect.
     * 
     * @param listener
     *            the listener to be added.
     */
    void addRegistryContentListener(RegistryContentListener listener);

    /**
     * Removes all children from this item. Also remove this item from the parents of all its former children.
     */
    void clearChildren();

    /**
     * Removes all parents from this item. Also remove this item from the children of all its former parents.
     */
    void clearParents();

    /**
     * Clears the set of registry content listeners.
     */
    void clearRegistryContentListeners();

    /**
     * Returns the child items of this registry item that has the supplied it if present. Note that this
     * method will return null if an item with this id does not exist, or exists but is not registered as a
     * child of this item, even if it is registered as its parent.
     * 
     * @return the child item, or null if not present or not a child.
     */
    RegistryItem getChild(IRI id);

    /**
     * Returns the child items of this registry item if present.
     * 
     * @return the child items, or an empty array if there are none.
     */
    RegistryItem[] getChildren();

    /**
     * Returns the unique identifier of this registry item. In some cases, such as for ontologies, this also
     * denotes their physical locations.
     * 
     * @return the identifier of this registry item.
     */
    IRI getIRI();

    /**
     * Returns the short name of this registry item. It may or may not be a suffix of its ID.
     * 
     * @return the short name of this registry item.
     */
    String getName();

    /**
     * Returns the parent items of this registry item that has the supplied it if present. Note that this
     * method will return null if an item with this id does not exist, or exists but is not registered as a
     * parent of this item, even if it is registered as its child.
     * 
     * @return the parent item, or null if not present or not a parent.
     */
    RegistryItem getParent(IRI id);

    /**
     * Returns the parent items of this registry item if present.
     * 
     * @return the parent items, or an empty array if there are none.
     */
    RegistryItem[] getParents();

    /**
     * Clears the set of objects to be notified any changes to this registry item.
     * 
     * @return the set of registry content listeners registered with this item.
     */
    Set<RegistryContentListener> getRegistryContentListeners();

    /**
     * Returns the type of this registry item.
     * 
     * @return the type of this registry item.
     */
    Type getType();

    /**
     * Determines if this registry item has any child items. It is a shortcut to {@link #getChildren()}
     * <code>.isEmpty()</code>.
     * 
     * @return true if this registry item has children, false otherwise.
     */
    boolean hasChildren();

    /**
     * Determines if this registry item has any parent items. It is a shortcut to {@link #getParents()}
     * <code>.isEmpty()</code>.
     * 
     * @return true if this registry item has parents, false otherwise.
     */
    boolean hasParents();

    /**
     * Releases all the parent and child references of this item. If no objects other than the former parents
     * and children are referencing it, this object is left stranded for garbage collection.
     */
    void prune();

    /**
     * Removes a registry item from the children of this registry item. Also removes itself from the parents
     * of the supplied item. Note that cycles will result in no effect.
     * 
     * @param child
     *            the child registry item to be removed.
     */
    void removeChild(RegistryItem child);

    /**
     * Removes a registry item from the parents of this registry item. Also removes itself from the children
     * of the supplied item. Note that cycles will result in no effect.
     * 
     * @param parent
     *            the parent registry item to be removed.
     */
    void removeParent(RegistryItem parent);

    /**
     * Removes the supplied listener from the set of registry content listeners. If the listener was not
     * previously registered with this registry item, this method has no effect.
     * 
     * @param listener
     *            the listener to be removed.
     */
    void removeRegistryContentListener(RegistryContentListener listener);

    /**
     * Sets the name of this registry item.
     * 
     * @param name
     *            the name of this registry item.
     */
    void setName(String name);

}
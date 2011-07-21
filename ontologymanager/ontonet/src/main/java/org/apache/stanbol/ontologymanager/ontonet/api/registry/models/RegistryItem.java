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
package org.apache.stanbol.ontologymanager.ontonet.api.registry.models;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;

import org.apache.stanbol.ontologymanager.ontonet.api.registry.RegistryContentException;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.RegistryContentListener;

public interface RegistryItem {

    /**
     * The allowed types of registry item that a registry manager can handle.
     */
    public enum Type {
        LIBRARY,
        ONTOLOGY,
        REGISTRY;
    }

    void addChild(RegistryItem child) throws RegistryContentException;

    void addContainer(RegistryItem container) throws RegistryContentException;

    void addRegistryContentListener(RegistryContentListener listener);

    void clearChildren();

    void clearRegistryContentListeners();

    RegistryItem[] getChildren();

    RegistryItem[] getContainers();

    String getName();

    Set<RegistryContentListener> getRegistryContentListeners();

    Type getType();

    URL getURL();

    boolean hasChildren();

    boolean isLibrary();

    boolean isOntology();

    void removeChild(RegistryItem child);

    void removeContainer(RegistryItem container);

    void removeRegistryContentListener(RegistryContentListener listener);

    void setName(String string);

    void setURL(URL url) throws URISyntaxException;

    String toString();

}
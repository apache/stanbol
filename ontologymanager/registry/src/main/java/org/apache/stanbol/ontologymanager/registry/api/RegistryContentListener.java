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
package org.apache.stanbol.ontologymanager.registry.api;

import org.apache.stanbol.ontologymanager.registry.api.model.RegistryItem;

/**
 * Objects that react to direct content requests to ontology registry items will implement this interface.
 * 
 * @author alexdma
 */
public interface RegistryContentListener {

    /**
     * Called when a registry item receives a request for some resource in its content, regardless whether the
     * actual resource has been previously loaded.
     * 
     * @param requestTarget
     *            the registry item that was requested.
     */
    void registryContentRequested(RegistryItem requestTarget);

}

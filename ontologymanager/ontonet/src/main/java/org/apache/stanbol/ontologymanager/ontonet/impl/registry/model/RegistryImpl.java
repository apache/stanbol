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

import java.net.URISyntaxException;
import java.net.URL;

import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.Registry;

public class RegistryImpl extends AbstractRegistryItem implements Registry {

    private String message = "";

    public RegistryImpl(String name) {
        super(name);
    }

    public RegistryImpl(String name, URL url) throws URISyntaxException {
        super(name, url);
    }

    public String getError() {
        return this.message;
    }

    public String getName() {
        return super.getName() + getError();
    }

    public boolean isError() {
        return !isOK();
    }

    @Override
    public boolean isLibrary() {
        return false;
    }

    public boolean isOK() {
        return this.getError().equals("");
    }

    @Override
    public boolean isOntology() {
        return false;
    }

    public void setError(String message) {
        this.message = message;
    }
}

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
package org.apache.stanbol.cmsadapter.core.mapping;

import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingConfiguration;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.BridgeDefinitions;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.AdapterMode;

import com.hp.hpl.jena.ontology.OntModel;

public class MappingConfigurationImpl implements MappingConfiguration {
    private OntModel ontModel;
    private BridgeDefinitions bridgeDefinitions;
    private AdapterMode adapterMode;
    private String ontologyURI;
    private List<Object> objects;
    private ConnectionInfo connectionInfo;

    @Override
    public void setOntModel(OntModel ontModel) {
        this.ontModel = ontModel;
    }

    @Override
    public OntModel getOntModel() {
        return this.ontModel;
    }

    @Override
    public void setBridgeDefinitions(BridgeDefinitions bridgeDefinitions) {
        this.bridgeDefinitions = bridgeDefinitions;
    }

    @Override
    public BridgeDefinitions getBridgeDefinitions() {
        return this.bridgeDefinitions;
    }

    @Override
    public void setAdapterMode(AdapterMode adapterMode) {
        this.adapterMode = adapterMode;
    }

    @Override
    public AdapterMode getAdapterMode() {
        return this.adapterMode;
    }

    @Override
    public void setOntologyURI(String ontologyURI) {
        this.ontologyURI = ontologyURI;
    }

    @Override
    public String getOntologyURI() {
        return this.ontologyURI;
    }

    @Override
    public void setObjects(List<Object> objects) {
        this.objects = objects;
    }

    @Override
    public List<Object> getObjects() {
        return this.objects;
    }

    @Override
    public void setConnectionInfo(ConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
    }

    @Override
    public ConnectionInfo getConnectionInfo() {
        return this.connectionInfo;
    }

}

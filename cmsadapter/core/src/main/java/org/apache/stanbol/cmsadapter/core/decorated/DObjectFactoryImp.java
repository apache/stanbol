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
package org.apache.stanbol.cmsadapter.core.decorated;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.Property;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.AdapterMode;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectAdapter;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DProperty;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DPropertyDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccess;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;

public class DObjectFactoryImp implements DObjectAdapter {

    private RepositoryAccess access;
    private RepositoryAccess offlineAccess;
    private Object session;
    private AdapterMode mode;

    public DObjectFactoryImp(RepositoryAccess access, Object session) {
        this.access = access;
        this.session = session;
        this.mode = AdapterMode.ONLINE;
    }

    public DObjectFactoryImp(RepositoryAccess access, ConnectionInfo connectionInfo) throws RepositoryAccessException {
        this.access = access;
        this.session = access.getSession(connectionInfo);
        this.mode = AdapterMode.ONLINE;
    }

    public DObjectFactoryImp(RepositoryAccess access, Object session, AdapterMode mode) {
        this.access = access;
        this.session = session;
        this.mode = mode;
    }

    public DObjectFactoryImp(RepositoryAccess access, ConnectionInfo connectionInfo, AdapterMode mode) throws RepositoryAccessException {
        this.access = access;
        this.session = access.getSession(connectionInfo);
        this.mode = mode;
    }

    public DObjectFactoryImp(RepositoryAccess access,
                             RepositoryAccess offlineAccess,
                             Object onlineSession,
                             AdapterMode mode) {
        this.access = access;
        this.offlineAccess = offlineAccess;
        this.session = onlineSession;
        this.mode = mode;
    }

    @Override
    public DObject wrapAsDObject(CMSObject node) {
        return new DObjectImp(node, this, access, offlineAccess);
    }

    @Override
    public DObjectType wrapAsDObjectType(ObjectTypeDefinition definition) {
        return new DObjectTypeImp(definition, this, access);
    }

    @Override
    public DPropertyDefinition wrapAsDPropertyDefinition(PropertyDefinition propertyDefinition) {
        return new DPropertyDefinitionImp(propertyDefinition);
    }

    @Override
    public DProperty wrapAsDProperty(Property property) {
        return new DPropertyImp(property, this, access);
    }

    @Override
    public Object getSession() {
        return session;
    }

    @Override
    public void setMode(AdapterMode mode) {
        this.mode = mode;
    }

    @Override
    public AdapterMode getMode() {
        return mode;
    }

}

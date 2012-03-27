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

import java.util.ArrayList;
import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.Property;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectAdapter;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DProperty;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccess;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DObjectImp implements DObject {

    private static final Logger log = LoggerFactory.getLogger(DObjectImp.class);

    private CMSObject instance;
    private DObjectAdapter factory;
    private RepositoryAccess access;
    private RepositoryAccess offlineAccess;
    private List<DObject> children = null;
    private List<DProperty> properties = null;
    private DObject parent = null;
    private DObjectType objectType = null;

    public DObjectImp(CMSObject instance,
                      DObjectAdapter factory,
                      RepositoryAccess access,
                      RepositoryAccess offlineAccess) {
        this.instance = instance;
        this.factory = factory;
        this.access = access;
        this.offlineAccess = offlineAccess;
    }

    @Override
    public String getID() {
        return instance.getUniqueRef();
    }

    @Override
    public String getPath() {
        return instance.getPath();
    }

    @Override
    public String getName() {
        return instance.getLocalname();
    }

    @Override
    public String getNamespace() {
        return instance.getNamespace();
    }

    @Override
    public List<DObject> getChildren() throws RepositoryAccessException {
        if (children == null) {
            switch (factory.getMode()) {
                case ONLINE:
                    children = getChildrenOnline();
                    break;
                case TOLERATED_OFFLINE:
                    children = getChildrenTOffline();
                    break;
                case STRICT_OFFLINE:
                    children = getChildrenSOffline();
                    break;
            }
        }

        return children;

    }

    private List<DObject> getChildrenOnline() throws RepositoryAccessException {
        List<CMSObject> nodes = access.getChildren(instance, factory.getSession());
        return wrapAsDObject(nodes);
    }

    private List<DObject> getChildrenTOffline() {
        try {
            return getChildrenOnline();
        } catch (RepositoryAccessException e) {
            log.debug("Error accesing repository at fetching children of {}. Tyring offline",
                instance.getPath());
            return getChildrenSOffline();
        }
    }

    private List<DObject> getChildrenSOffline() {
        return wrapAsDObject(instance.getChildren());
    }

    @Override
    public DObject getParent() throws RepositoryAccessException {
        if (parent == null) {
            switch (factory.getMode()) {
                case ONLINE:
                    parent = factory.wrapAsDObject(access.getParentByNode(instance, factory.getSession()));
                    break;
                case TOLERATED_OFFLINE:
                    try {
                        parent = factory
                                .wrapAsDObject(access.getParentByNode(instance, factory.getSession()));
                    } catch (RepositoryAccessException e) {
                        log.debug("Can not access repository at fetching parent of {}.", instance.getPath());
                        parent = factory.wrapAsDObject(offlineAccess.getParentByNode(instance, null));
                    }
                    break;
                case STRICT_OFFLINE:
                    parent = factory.wrapAsDObject(access.getParentByNode(instance, null));
                    break;
            }
        }

        return parent;
    }

    @Override
    public DObjectType getObjectType() throws RepositoryAccessException {
        if (objectType == null) {
            String typeRef = instance.getObjectTypeRef();
            switch (factory.getMode()) {
                case ONLINE:
                    objectType = factory.wrapAsDObjectType(access.getObjectTypeDefinition(typeRef,
                        factory.getSession()));
                    break;
                case TOLERATED_OFFLINE:
                    try {
                        objectType = factory.wrapAsDObjectType(access.getObjectTypeDefinition(typeRef,
                            factory.getSession()));
                    } catch (RepositoryAccessException e) {
                        log.debug("Can not access repository at fetching object type of {}.",
                            instance.getPath());
                    }
                    break;
                case STRICT_OFFLINE:
                    break;
            }
        }

        return objectType;
    }

    @Override
    public List<DProperty> getProperties() throws RepositoryAccessException {
        if (properties == null) {
            switch (factory.getMode()) {
                case ONLINE:
                    properties = getPropertiesOnline();
                    break;
                case TOLERATED_OFFLINE:
                    try {
                        properties = getPropertiesOnline();
                    } catch (RepositoryAccessException e) {
                        log.debug("Can not access repository at fetching properties of {}.",
                            instance.getPath());
                        properties = wrapAsDProperty(instance.getProperty());
                    }
                    break;
                case STRICT_OFFLINE:
                    properties = wrapAsDProperty(instance.getProperty());
                    break;
            }
        }
        return properties;
    }

    @Override
    public CMSObject getInstance() {
        return this.instance;
    }

    private List<DProperty> getPropertiesOnline() throws RepositoryAccessException {
        List<Property> props = access.getProperties(instance, factory.getSession());
        return wrapAsDProperty(props);
    }

    private List<DObject> wrapAsDObject(List<CMSObject> cmsObjects) {
        List<DObject> wrappeds = new ArrayList<DObject>(cmsObjects.size());
        for (CMSObject node : cmsObjects) {
            wrappeds.add(this.factory.wrapAsDObject(node));
        }
        return wrappeds;
    }

    private List<DProperty> wrapAsDProperty(List<Property> props) {
        List<DProperty> properties = new ArrayList<DProperty>(props.size());

        for (Property prop : props) {
            properties.add(factory.wrapAsDProperty(prop));
        }
        return properties;
    }
}

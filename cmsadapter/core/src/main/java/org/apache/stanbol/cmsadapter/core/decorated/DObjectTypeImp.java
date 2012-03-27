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

import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectAdapter;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DPropertyDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccess;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DObjectTypeImp implements DObjectType {
    private static final Logger log = LoggerFactory.getLogger(DObjectTypeImp.class);

    private ObjectTypeDefinition instance;
    private DObjectAdapter factory;
    private RepositoryAccess access;
    private List<DPropertyDefinition> propertyDefinitions;
    private List<DObjectType> parentDefinitions;
    private List<DObjectType> childDefinitions;

    public DObjectTypeImp(ObjectTypeDefinition instance, DObjectAdapter factory, RepositoryAccess access) {
        this.instance = instance;
        this.factory = factory;
        this.access = access;
    }

    @Override
    public String getID() {
        return instance.getUniqueRef();
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
    public List<DPropertyDefinition> getPropertyDefinitions() throws RepositoryAccessException {
        if (propertyDefinitions != null) {
            return propertyDefinitions;
        } else {
            List<PropertyDefinition> propDefinitions = access.getPropertyDefinitions(instance,
                factory.getSession());

            propertyDefinitions = new ArrayList<DPropertyDefinition>(propDefinitions.size());
            for (PropertyDefinition propDefinition : propDefinitions) {
                propertyDefinitions.add(factory.wrapAsDPropertyDefinition(propDefinition));
            }

            return propertyDefinitions;
        }
    }

    @Override
    public List<DObjectType> getParentDefinitions() throws RepositoryAccessException {
        if (parentDefinitions != null) {
            return parentDefinitions;
        }

        List<ObjectTypeDefinition> parDefinitions = access.getParentTypeDefinitions(instance,
            factory.getSession());
        propertyDefinitions = new ArrayList<DPropertyDefinition>(parDefinitions.size());
        for (ObjectTypeDefinition parentTypeRef : parDefinitions) {
            parentDefinitions.add(factory.wrapAsDObjectType(parentTypeRef));
        }

        return parentDefinitions;
    }

    @Override
    public List<DObjectType> getChildDefinitions() throws RepositoryAccessException {
        if (childDefinitions == null) {
            switch (factory.getMode()) {
                case ONLINE:
                    childDefinitions = getChildDefinitionsOnline();
                    break;
                case TOLERATED_OFFLINE:
                    try {
                        childDefinitions = getChildDefinitionsOnline();
                    } catch (RepositoryAccessException e) {
                        log.debug("Can not access repository while fetching childs definitions of type {}",
                            instance.getUniqueRef());
                        childDefinitions = getChildDefinitionsOffline();
                    }
                    break;
                case STRICT_OFFLINE:
                    childDefinitions = getChildDefinitionsOffline();
                    break;
            }
        }

        return childDefinitions;
    }

    private List<DObjectType> getChildDefinitionsOnline() throws RepositoryAccessException {
        List<ObjectTypeDefinition> childDefs = access.getChildObjectTypeDefinitions(instance,
            factory.getSession());
        return wrapChildObjectDefinitions(childDefs);
    }

    private List<DObjectType> getChildDefinitionsOffline() {
        return wrapChildObjectDefinitions(instance.getObjectTypeDefinition());
    }

    private List<DObjectType> wrapChildObjectDefinitions(List<ObjectTypeDefinition> childDefs) {
        List<DObjectType> childDefinitions = new ArrayList<DObjectType>(childDefs.size());
        for (ObjectTypeDefinition childDef : childDefs) {
            childDefinitions.add(factory.wrapAsDObjectType(childDef));
        }
        return childDefinitions;
    }

    @Override
    public ObjectTypeDefinition getInstance() {
        return instance;
    }

}

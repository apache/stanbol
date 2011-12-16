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
package org.apache.stanbol.cmsadapter.core.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.Property;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccess;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;

/**
 * This class is used for mocking a {@link RepositoryAccess} service when there is not possible to access a
 * remote CMS Repository. Instead when initialized with a list of CMS Objects, Offline Access acts as a
 * repository, providing an implementation of {@link RepositoryAccess} so that there no separate interface for
 * offline processing.
 * 
 * 
 * @author cihan
 * 
 */
public class OfflineAccess implements RepositoryAccess {

    MultiValueMap nameMap = new MultiValueMap();
    Map<String,CMSObject> cmsObjectMap = new HashMap<String,CMSObject>();
    Map<String,PropertyDefinition> propDefinitionMap = new HashMap<String,PropertyDefinition>();
    Map<String,ObjectTypeDefinition> typeDefinitionMap = new HashMap<String,ObjectTypeDefinition>();
    // TODO find a more suitable solution
    Map<Property,CMSObject> propertyMap = new HashMap<Property,CMSObject>();

    public OfflineAccess(List<Object> repository) {
        for (Object cmsObject : repository) {
            if (cmsObject instanceof CMSObject) {
                processCMSOBject((CMSObject) cmsObject);
            } else if (cmsObject instanceof ObjectTypeDefinition) {
                processObjectTypeDefinition((ObjectTypeDefinition) cmsObject);
            } else if (cmsObject instanceof PropertyDefinition) {
                processPropertyDefinition((PropertyDefinition) cmsObject);
            } else {
                throw new IllegalArgumentException("Offline accessor can not handle type "
                                                   + cmsObject.getClass().getName());
            }
        }
    }

    private void processPropertyDefinition(PropertyDefinition propDef) {
        propDefinitionMap.put(propDef.getUniqueRef(), propDef);
    }

    private void processObjectTypeDefinition(ObjectTypeDefinition typeDef) {
        typeDefinitionMap.put(typeDef.getUniqueRef(), typeDef);
        for (PropertyDefinition propDef : typeDef.getPropertyDefinition()) {
            propDefinitionMap.put(propDef.getUniqueRef(), propDef);
        }
    }

    private void processCMSOBject(CMSObject cmsObject) {
        cmsObjectMap.put(cmsObject.getUniqueRef(), cmsObject);
        if (!nameMap.containsValue(cmsObject.getLocalname(), cmsObject)) {
            nameMap.put(cmsObject.getLocalname(), cmsObject);
        }

        for (Property prop : cmsObject.getProperty()) {
            propertyMap.put(prop, cmsObject);
            propDefinitionMap.put(prop.getPropertyDefinition().getUniqueRef(), prop.getPropertyDefinition());
        }

        for (CMSObject child : cmsObject.getChildren()) {
            processCMSOBject(child);
        }
    }

    @Override
    public Object getSession(ConnectionInfo connectionInfo) throws RepositoryAccessException {
        throw new UnsupportedOperationException("Offline Repository Access does not handle connections");
    }

    @Override
    public List<CMSObject> getNodeByPath(String path, Object session) throws RepositoryAccessException {
        checkNullArgument(session);
        return getNodeByPath(path);

    }

    @Override
    public List<CMSObject> getNodeByPath(String path, ConnectionInfo connectionInfo) throws RepositoryAccessException {
        checkNullArgument(connectionInfo);
        return getNodeByPath(path);
    }

    private List<CMSObject> getNodeByPath(String path) {
        List<CMSObject> nodes = new ArrayList<CMSObject>();
        for (CMSObject cmsObject : cmsObjectMap.values()) {
            if (matches(path, cmsObject.getPath())) {
                nodes.add(cmsObject);
            }
        }

        return nodes;
    }

    @Override
    public List<CMSObject> getNodeById(String id, ConnectionInfo connectionInfo) throws RepositoryAccessException {
        checkNullArgument(connectionInfo);
        return getNodeById(id);
    }

    @Override
    public List<CMSObject> getNodeById(String id, Object session) throws RepositoryAccessException {
        checkNullArgument(session);
        return getNodeById(id);
    }

    private List<CMSObject> getNodeById(String id) throws RepositoryAccessException {
        CMSObject cmsObject = cmsObjectMap.get(id);
        if (cmsObject == null) {
            throw new RepositoryAccessException("No object with id=" + id);
        } else {
            return Arrays.asList(new CMSObject[] {cmsObject});
        }
    }

    @Override
    public List<CMSObject> getNodeByName(String name, Object session) throws RepositoryAccessException {
        checkNullArgument(session);
        return getNodeByName(name);
    }

    @Override
    public List<CMSObject> getNodeByName(String name, ConnectionInfo connectionInfo) throws RepositoryAccessException {
        checkNullArgument(connectionInfo);
        return getNodeByName(name);
    }

    private List<CMSObject> getNodeByName(String name) {
        List<CMSObject> nodes = (List<CMSObject>) nameMap.get(name);
        if (nodes == null) {
            nodes = Collections.emptyList();
        }
        return nodes;
    }

    @Override
    public CMSObject getFirstNodeByPath(String path, ConnectionInfo connectionInfo) throws RepositoryAccessException {
        checkNullArgument(connectionInfo);
        return getFirstNodeByPath(path);
    }

    @Override
    public CMSObject getFirstNodeByPath(String path, Object session) throws RepositoryAccessException {
        checkNullArgument(session);
        return getFirstNodeByPath(path);
    }

    private CMSObject getFirstNodeByPath(String path) {
        for (CMSObject cmsObject : cmsObjectMap.values()) {
            if (matches(path, cmsObject.getPath())) {
                return cmsObject;
            }
        }
        return null;
    }

    @Override
    public CMSObject getFirstNodeById(String id, ConnectionInfo connectionInfo) throws RepositoryAccessException {
        checkNullArgument(connectionInfo);
        return getFirstNodeById(id);
    }

    @Override
    public CMSObject getFirstNodeById(String id, Object session) throws RepositoryAccessException {
        checkNullArgument(session);
        return getFirstNodeById(id);
    }

    private CMSObject getFirstNodeById(String id) throws RepositoryAccessException {
        return getNodeById(id).get(0);
    }

    @Override
    public CMSObject getFirstNodeByName(String name, Object session) throws RepositoryAccessException {
        checkNullArgument(session);
        return getFirstNodeByName(name);
    }

    @Override
    public CMSObject getFirstNodeByName(String name, ConnectionInfo connectionInfo) throws RepositoryAccessException {
        checkNullArgument(connectionInfo);
        return getFirstNodeByName(name);
    }

    private CMSObject getFirstNodeByName(String name) {
        List<CMSObject> nodes = (List<CMSObject>) nameMap.get(name);
        if (nodes == null || nodes.size() < 1) {
            return null;
        } else {
            return nodes.get(0);
        }
    }

    @Override
    public List<CMSObject> getChildren(CMSObject node, Object session) throws RepositoryAccessException {
        checkNullArgument(session);
        return node.getChildren();
    }

    @Override
    public ObjectTypeDefinition getObjectTypeDefinition(String typeRef, Object session) throws RepositoryAccessException {
        checkNullArgument(session);
        if (typeDefinitionMap.containsKey(typeRef)) {
            return typeDefinitionMap.get(typeRef);
        } else {
            throw new RepositoryAccessException("No object type definition with id " + typeRef);
        }
    }

    @Override
    public List<Property> getProperties(CMSObject node, Object session) throws RepositoryAccessException {
        checkNullArgument(session);
        return node.getProperty();
    }

    @Override
    public List<PropertyDefinition> getPropertyDefinitions(ObjectTypeDefinition instance, Object session) throws RepositoryAccessException {
        checkNullArgument(session);
        return instance.getPropertyDefinition();
    }

    @Override
    public List<ObjectTypeDefinition> getParentTypeDefinitions(ObjectTypeDefinition instance, Object session) throws RepositoryAccessException {
        // TODO Check if this interpreted correctly
        checkNullArgument(session);
        List<ObjectTypeDefinition> parentTypeDefinitions = new ArrayList<ObjectTypeDefinition>();
        for (String parentRef : instance.getParentRef()) {
            if (cmsObjectMap.containsKey(parentRef)) {
                String parentTypeRef = cmsObjectMap.get(parentRef).getObjectTypeRef();
                if (typeDefinitionMap.containsKey(parentTypeRef)) {
                    parentTypeDefinitions.add(typeDefinitionMap.get(parentTypeRef));
                }
            }
        }

        return parentTypeDefinitions;
    }

    @Override
    public List<ObjectTypeDefinition> getChildObjectTypeDefinitions(ObjectTypeDefinition instance,
                                                                    Object session) throws RepositoryAccessException {
        checkNullArgument(session);
        return instance.getObjectTypeDefinition();
    }

    @Override
    public CMSObject getContainerObject(Property instance, Object session) throws RepositoryAccessException {
        checkNullArgument(session);
        if (propertyMap.containsKey(instance)) {
            return propertyMap.get(instance);
        } else {
            throw new RepositoryAccessException("Container object for property not found");
        }
    }

    @Override
    public PropertyDefinition getPropertyDefinition(Property instance, Object session) throws RepositoryAccessException {
        return instance.getPropertyDefinition();
    }

    @Override
    public String getNamespaceURI(String prefix, Object session) throws RepositoryAccessException {
        // TODO find a good solution here.
        // throw new UnsupportedOperationException(
        // "Offline Repository Access does not handle connections, thus prefixes can not be accessible");
        return null;
    }

    @Override
    public boolean canRetrieve(ConnectionInfo connectionInfo) {
        throw new UnsupportedOperationException("Offline Repository Access does not handle connections");
    }

    @Override
    public boolean canRetrieve(Object session) {
        throw new UnsupportedOperationException("Offline Repository Access does not handle connections");
    }

    @Override
    public CMSObject getParentByNode(CMSObject instance, Object session) throws RepositoryAccessException {
        checkNullArgument(session);
        String parentRef = instance.getParentRef();
        if (cmsObjectMap.containsKey(parentRef)) {
            return cmsObjectMap.get(parentRef);
        } else {
            throw new RepositoryAccessException("No object with id=" + parentRef);
        }
    }

    private void checkNullArgument(Object argument) {
        if (argument != null) {
            throw new IllegalArgumentException(
                    "Offline accessor can not handle non-null Session or Connection Info");
        }
    }

    private boolean matches(String pathExpr, String realPath) {
        // TODO implement
        if (pathExpr.endsWith("%")) {
            return realPath.contains(pathExpr.substring(0, pathExpr.length() - 1));
        } else {
            return pathExpr.contentEquals(realPath);
        }
    }

    @Override
    public boolean isSessionValid(Object session) {
        throw new UnsupportedOperationException("Offline Repository Access does not handle connections");
    }
}
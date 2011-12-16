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

import static org.apache.stanbol.cmsadapter.core.decorated.NamingHelper.UNIQUE_REF;

import java.util.Arrays;
import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectFactory;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.Property;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccess;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;

public class MockOnlineAccess implements RepositoryAccess {

    private ObjectFactory of = new ObjectFactory();

    public MockOnlineAccess() {

    }

    @Override
    public Object getSession(ConnectionInfo connectionInfo) throws RepositoryAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<CMSObject> getNodeByPath(String path, ConnectionInfo connectionInfo) throws RepositoryAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<CMSObject> getNodeById(String id, ConnectionInfo connectionInfo) throws RepositoryAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<CMSObject> getNodeByPath(String path, Object session) throws RepositoryAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<CMSObject> getNodeById(String id, Object session) throws RepositoryAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<CMSObject> getNodeByName(String name, Object session) throws RepositoryAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<CMSObject> getNodeByName(String name, ConnectionInfo connectionInfo) throws RepositoryAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CMSObject getFirstNodeByPath(String path, ConnectionInfo connectionInfo) throws RepositoryAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CMSObject getFirstNodeById(String id, ConnectionInfo connectionInfo) throws RepositoryAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CMSObject getFirstNodeByPath(String path, Object session) throws RepositoryAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CMSObject getFirstNodeById(String id, Object session) throws RepositoryAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CMSObject getFirstNodeByName(String name, Object session) throws RepositoryAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CMSObject getFirstNodeByName(String name, ConnectionInfo connectionInfo) throws RepositoryAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<CMSObject> getChildren(CMSObject node, Object session) throws RepositoryAccessException {
        if (node == null || !node.getUniqueRef().equals(DobjectFactoryImpTest.PREFIX_ROOT + UNIQUE_REF)) {
            return null;

        } else {
            CMSObject child1 = new CMSObjectBuilder(DobjectFactoryImpTest.PREFIX_CHILD_1).namespace().build();
            CMSObject child2 = new CMSObjectBuilder(DobjectFactoryImpTest.PREFIX_CHILD_2).namespace().build();
            return Arrays.asList(new CMSObject[] {child1, child2});
        }
    }

    @Override
    public ObjectTypeDefinition getObjectTypeDefinition(String typeRef, Object session) throws RepositoryAccessException {
        // TODO Auto-generated method stub
        if (typeRef == null) {
            throw new RepositoryAccessException("Null typeRef", null);
        }
        return null;
    }

    @Override
    public List<Property> getProperties(CMSObject node, Object session) throws RepositoryAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<PropertyDefinition> getPropertyDefinitions(ObjectTypeDefinition instance, Object session) throws RepositoryAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<ObjectTypeDefinition> getParentTypeDefinitions(ObjectTypeDefinition instance, Object session) throws RepositoryAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<ObjectTypeDefinition> getChildObjectTypeDefinitions(ObjectTypeDefinition instance,
                                                                    Object session) throws RepositoryAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CMSObject getContainerObject(Property instance, Object session) throws RepositoryAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PropertyDefinition getPropertyDefinition(Property instance, Object session) throws RepositoryAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getNamespaceURI(String prefix, Object session) throws RepositoryAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean canRetrieve(ConnectionInfo connectionInfo) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canRetrieve(Object session) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public CMSObject getParentByNode(CMSObject instance, Object session) throws RepositoryAccessException {
        // TODO Auto-generated method stub
        if (instance.getParentRef() == null) {
            throw new RepositoryAccessException("No parent", null);
        } else {
            String ref = instance.getParentRef();
            CMSObject parent = of.createCMSObject();
            parent.setLocalname("localname" + ref);
            parent.setNamespace("namespace" + ref);
            parent.setParentRef("parent" + ref);
            parent.setUniqueRef(ref);
            return parent;
        }
    }

    @Override
    public boolean isSessionValid(Object session) {
        // TODO Auto-generated method stub
        return false;
    }

}
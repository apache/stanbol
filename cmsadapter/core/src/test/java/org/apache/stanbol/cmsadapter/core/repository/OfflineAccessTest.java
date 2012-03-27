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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.stanbol.cmsadapter.core.decorated.CMSObjectBuilder;
import org.apache.stanbol.cmsadapter.core.decorated.ObjectTypeBuilder;
import org.apache.stanbol.cmsadapter.core.decorated.PropertyBuilder;
import org.apache.stanbol.cmsadapter.core.decorated.PropertyDefinitionBuilder;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.Property;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class OfflineAccessTest {

    private static final String PX_ROOT = "root";
    private static final String PX_O_11 = "child11";
    private static final String PX_O_12 = "child12";
    private static final String PX_O_21 = "child21";
    private static final String PX_O_13 = "child13";
    private static final String PX_PD_1 = "propdef1";
    private static final String PX_PD_2 = "propdef2";
    private static final String PX_PD_3 = "propdef3";
    private static final String PX_P_1 = "prop1";
    private static final String PX_P_2 = "prop2";
    // private static final String PX_P_3 = "prop3";
    private static final String PX_OT_1 = "objectype1";
    private static final String PX_OT_2 = "objectype2";
    private static final String PX_OT_3 = "objectype3";
    // Simulate an input repository(will be submitted through rest services)
    private static List<Object> repository;
    private static RepositoryAccessManagerImpl accessManager;
    private static OfflineAccess offlineAccess;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // initialize a dummy repository
        repository = new ArrayList<Object>();
        PropertyDefinition pd1 = new PropertyDefinitionBuilder(PX_PD_1).build();
        PropertyDefinition pd2 = new PropertyDefinitionBuilder(PX_PD_2).build();
        PropertyDefinition pd3 = new PropertyDefinitionBuilder(PX_PD_3).build();
        Property p1 = new PropertyBuilder(PX_P_1).propertyDefinition(pd1).build();
        Property p2 = new PropertyBuilder(PX_P_2).propertyDefinition(pd2).build();
        CMSObject root = new CMSObjectBuilder(PX_ROOT).build();
        CMSObject child11 = new CMSObjectBuilder(PX_O_11).build();
        CMSObject child12 = new CMSObjectBuilder(PX_O_12).build();
        CMSObject child21 = new CMSObjectBuilder(PX_O_21).build();
        CMSObject child13 = new CMSObjectBuilder(PX_O_13).build();

        ObjectTypeDefinition type1 = new ObjectTypeBuilder(PX_OT_1).propertyDefinition(pd1).build();
        ObjectTypeDefinition type2 = new ObjectTypeBuilder(PX_OT_2).propertyDefinition(pd2).build();
        ObjectTypeDefinition type3 = new ObjectTypeBuilder(PX_OT_3).propertyDefinition(pd3).build();

        root.setObjectTypeRef(type1.getUniqueRef());
        child11.setObjectTypeRef(type1.getUniqueRef());
        child12.setObjectTypeRef(type2.getUniqueRef());
        child13.setObjectTypeRef(type2.getUniqueRef());
        child21.setObjectTypeRef(type3.getUniqueRef());

        p1.setContainerObjectRef(root.getUniqueRef());
        root.getProperty().add(p1);
        root.getProperty().add(p2);

        root.getChildren().add(child11);
        child11.setParentRef(root.getUniqueRef());

        root.getChildren().add(child12);
        child12.setParentRef(root.getUniqueRef());

        child11.getChildren().add(child21);
        child21.setParentRef(child11.getUniqueRef());

        // child13 has the same name as child11
        root.getChildren().add(child13);
        child13.setParentRef(root.getUniqueRef());
        child13.setLocalname(child11.getLocalname());

        processPaths(root);
        repository.addAll(Arrays.asList(new Object[] {root, child11, child12, child13, child21, type1, type2,
                                                      type3, pd1, pd2}));

    }

    @Test
    public void testOfflineAccess() {
        try {
            offlineAccess = new OfflineAccess(repository);
        } catch (Exception e) {
            fail("Exception in constructor: " + e.getMessage());
        }
    }

    @Test
    public void testGetSession() throws RepositoryAccessException {
        expectedException.expect(UnsupportedOperationException.class);
        offlineAccess.getSession(new ConnectionInfo());
    }

    @Test
    public void testGetNodeByPath() throws RepositoryAccessException {
        List<CMSObject> real = offlineAccess.getNodeByPath("/", null);
        // Check only the root is returned
        assertEquals(1, real.size());
        assertSame(repository.get(0), real.get(0));

        List<CMSObject> all = offlineAccess.getNodeByPath("/%", null);
        assertEquals(5, all.size());

        expectedException.expect(IllegalArgumentException.class);
        offlineAccess.getNodeByPath("/", new Object());
    }

    @Test
    public void testGetNodeById() throws RepositoryAccessException {
        CMSObject expected = (CMSObject) repository.get(0);
        List<CMSObject> real = offlineAccess.getNodeById(expected.getUniqueRef(), null);

        assertEquals(1, real.size());
        assertSame(expected, real.get(0));

        expectedException.expect(RepositoryAccessException.class);
        real = offlineAccess.getNodeById(UUID.randomUUID().toString(), null);
        assertEquals(0, real.size());

    }

    @Test
    public void testGetNodeByName() throws RepositoryAccessException {
        CMSObject c11 = (CMSObject) repository.get(1);
        CMSObject c13 = (CMSObject) repository.get(3);

        List<CMSObject> real = offlineAccess.getNodeByName(c11.getLocalname(), null);
        assertEquals(2, real.size());
        assertSame(c11, real.get(0));
        assertSame(c13, real.get(1));

        expectedException.expect(IllegalArgumentException.class);
        offlineAccess.getNodeByName(UUID.randomUUID().toString(), new Object());
    }

    @Test
    public void testGetFirstNodeByPath() throws RepositoryAccessException {
        CMSObject real = offlineAccess.getFirstNodeByPath("/", null);
        // Check only the root is returned
        assertSame(repository.get(0), real);

        CMSObject all = offlineAccess.getFirstNodeByPath("/%", null);
        assertNotNull(all);

        expectedException.expect(IllegalArgumentException.class);
        offlineAccess.getFirstNodeByPath("/", new Object());
    }

    @Test
    public void testGetFirstNodeById() throws RepositoryAccessException {
        CMSObject expected = (CMSObject) repository.get(0);
        CMSObject real = offlineAccess.getFirstNodeById(expected.getUniqueRef(), null);

        assertSame(expected, real);

        expectedException.expect(RepositoryAccessException.class);
        real = offlineAccess.getFirstNodeById(UUID.randomUUID().toString(), null);

    }

    @Test
    public void testGetFirstNodeByName() throws RepositoryAccessException {
        CMSObject c11 = (CMSObject) repository.get(1);
        CMSObject c13 = (CMSObject) repository.get(3);

        CMSObject real = offlineAccess.getFirstNodeByName(c11.getLocalname(), null);
        assertTrue(c11 == real || c13 == real);

        expectedException.expect(IllegalArgumentException.class);
        offlineAccess.getNodeByName(UUID.randomUUID().toString(), new Object());
    }

    @Test
    public void testGetObjectTypeDefinition() throws RepositoryAccessException {
        CMSObject root = (CMSObject) repository.get(0);
        ObjectTypeDefinition expected = (ObjectTypeDefinition) repository.get(5);
        ObjectTypeDefinition typeDef = offlineAccess.getObjectTypeDefinition(root.getObjectTypeRef(), null);
        assertSame(expected, typeDef);

        expectedException.expect(IllegalArgumentException.class);
        offlineAccess.getObjectTypeDefinition(UUID.randomUUID().toString(), new Object());

    }

    /*
     * TODO Implement when the semantics gets clear*
     * 
     * @Test public void testGetParentTypeDefinitions() {}
     * 
     * @Test public void testGetChildObjectTypeDefinitions() {}
     * 
     * @Test public void testGetAllowableTypeDef() {}
     */
    @Test
    public void testGetContainerObject() throws RepositoryAccessException {
        CMSObject root = (CMSObject) repository.get(0);
        Property p = root.getProperty().get(0);
        CMSObject real = offlineAccess.getContainerObject(p, null);
        assertSame(root, real);

        expectedException.expect(IllegalArgumentException.class);
        offlineAccess.getContainerObject(p, new Object());
    }

    @Test
    public void testGetPropertyDefinition() throws RepositoryAccessException {
        CMSObject root = (CMSObject) repository.get(0);
        PropertyDefinition expected = (PropertyDefinition) repository.get(8);
        Property p = root.getProperty().get(0);
        PropertyDefinition propDef = offlineAccess.getPropertyDefinition(p, null);
        assertSame(expected, propDef);

    }

    @Test
    public void testGetNamespaceURI() throws RepositoryAccessException {
        String nsURI = offlineAccess.getNamespaceURI(null, null);
        assertTrue(nsURI == null);
    }

    @Test
    public void testCanRetrieveConnectionInfo() {
        expectedException.expect(UnsupportedOperationException.class);
        offlineAccess.canRetrieve((ConnectionInfo) null);
    }

    @Test
    public void testCanRetrieveObject() {
        expectedException.expect(UnsupportedOperationException.class);
        offlineAccess.canRetrieve(null);
    }

    @Test
    public void testGetParentByNode() throws RepositoryAccessException {
        CMSObject child11 = (CMSObject) repository.get(1);
        CMSObject root = (CMSObject) repository.get(0);
        CMSObject parent = offlineAccess.getParentByNode(child11, null);
        assertSame(root, parent);

        expectedException.expect(IllegalArgumentException.class);
        offlineAccess.getParentByNode(child11, new Object());
    }

    // Helper for assigning CMSOBject paths
    private static void processPaths(CMSObject root) {
        String path = "/";
        root.setPath(path);

        processCMSObjectPath(root.getChildren(), path);
    }

    private static void processCMSObjectPath(List<CMSObject> cmsObjects, String path) {
        for (CMSObject cmsObject : cmsObjects) {
            String newPath = path + cmsObject.getLocalname();
            cmsObject.setPath(newPath);
            processCMSObjectPath(cmsObject.getChildren(), newPath + "/");
        }
    }
}

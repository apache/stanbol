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

import static org.apache.stanbol.cmsadapter.core.decorated.NamingHelper.LOCAL_NAME;
import static org.apache.stanbol.cmsadapter.core.decorated.NamingHelper.NAMESPACE;
import static org.apache.stanbol.cmsadapter.core.decorated.NamingHelper.PATH;
import static org.apache.stanbol.cmsadapter.core.decorated.NamingHelper.UNIQUE_REF;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.AdapterMode;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectAdapter;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DobjectFactoryImpTest {

    public static final String PREFIX_ROOT = "root_";
    public static final String PREFIX_CHILD_1 = "child_1_";
    public static final String PREFIX_CHILD_2 = "child_2_";

    static DObjectAdapter tOfflineAdapter;
    static DObjectAdapter sOfflineAdapter;
    static DObjectAdapter onlineAdapter;
    CMSObject rootStripped;
    CMSObject root;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        tOfflineAdapter = new DObjectFactoryImp(new MockOfflineAccess(), null, AdapterMode.TOLERATED_OFFLINE);
        sOfflineAdapter = new DObjectFactoryImp(new MockOfflineAccess(), null, AdapterMode.STRICT_OFFLINE);
        onlineAdapter = new DObjectFactoryImp(new MockOnlineAccess(), null);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {}

    @Before
    public void setUp() {
        rootStripped = new CMSObjectBuilder(PREFIX_ROOT).namespace().build();
        CMSObject child1 = new CMSObjectBuilder(PREFIX_CHILD_1).build();
        CMSObject child2 = new CMSObjectBuilder(PREFIX_CHILD_2).build();
        root = new CMSObjectBuilder(PREFIX_ROOT).namespace().child(child1).child(child2).build();
    }

    @Test
    public void testDObjectFieldsOnline() {
        DObject root = onlineAdapter.wrapAsDObject(rootStripped);
        assertEquals(PREFIX_ROOT + LOCAL_NAME, root.getName());
        assertEquals(PREFIX_ROOT + NAMESPACE, root.getNamespace());
        assertEquals(PREFIX_ROOT + PATH, root.getPath());
        assertEquals(PREFIX_ROOT + UNIQUE_REF, root.getID());
    }

    @Test
    public void testDObjectFieldsOffline() {
        DObject root = sOfflineAdapter.wrapAsDObject(rootStripped);
        assertEquals(PREFIX_ROOT + LOCAL_NAME, root.getName());
        assertEquals(PREFIX_ROOT + NAMESPACE, root.getNamespace());
        assertEquals(PREFIX_ROOT + PATH, root.getPath());
        assertEquals(PREFIX_ROOT + UNIQUE_REF, root.getID());

    }

    @Test
    public void testDObjectParentOnline() throws RepositoryAccessException {
        DObject root = onlineAdapter.wrapAsDObject(rootStripped);
        expectedException.expect(RepositoryAccessException.class);
        root.getParent();
    }

    @Test
    public void testDObjectParentOffline() throws RepositoryAccessException {
        DObject root = sOfflineAdapter.wrapAsDObject(rootStripped);
        expectedException.expect(RepositoryAccessException.class);
        root.getParent();
    }

    @Test
    public void testDObjectChildsOnline() throws RepositoryAccessException {
        DObject root = onlineAdapter.wrapAsDObject(rootStripped);
        List<DObject> children = root.getChildren();
        assertEquals(2, children.size());
    }

    @Test
    public void testDObjectChildsOffline() throws RepositoryAccessException {
        DObject rootd = sOfflineAdapter.wrapAsDObject(root);
        List<DObject> children = rootd.getChildren();
        assertEquals(2, children.size());
    }

    @Test
    public void testDObjectChildsOfflineStripped() throws RepositoryAccessException {
        DObject rootd = sOfflineAdapter.wrapAsDObject(rootStripped);
        List<DObject> children = rootd.getChildren();
        assertEquals(0, children.size());
    }

    @Test
    public void testDObjectChildsTOfflineStripped() throws RepositoryAccessException {
        DObject rootd = tOfflineAdapter.wrapAsDObject(root);
        List<DObject> children = rootd.getChildren();
        assertEquals(2, children.size());
    }

    @Test
    public void testDObjectTypeSOffline() throws RepositoryAccessException {
        DObject rootd = sOfflineAdapter.wrapAsDObject(root);
        assertNull(rootd.getObjectType());
    }

    @Test
    public void testDObjectTypeOnline() throws RepositoryAccessException {
        DObject rootd = onlineAdapter.wrapAsDObject(rootStripped);
        expectedException.expect(RepositoryAccessException.class);
        rootd.getObjectType();
    }

}

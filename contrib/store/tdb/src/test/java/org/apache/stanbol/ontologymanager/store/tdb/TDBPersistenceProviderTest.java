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
package org.apache.stanbol.ontologymanager.store.tdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.stanbol.ontologymanager.store.api.JenaPersistenceProvider;
import org.apache.stanbol.ontologymanager.store.jena.tdb.TDBPersistenceProvider;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.tdb.TDB;

public class TDBPersistenceProviderTest {

    private static JenaPersistenceProvider TDBProvider;
    private static String[] testURIs = {"http://www.co-ode.org/ontologies/pizza/pizza.owl",
                                        "http://protege.cim3.net/file/pub/ontologies/camera/camera.owl",
                                        "http://protege.cim3.net/file/pub/ontologies/koala/koala.owl",
                                        "http://www.w3.org/TR/owl-guide/wine.rdf"};

    @BeforeClass
    public static void setUpCase() throws Exception {
        TDBProvider = new TDBPersistenceProvider();
        ((TDBPersistenceProvider) TDBProvider).activate(new DummyComponentContext());

    }

    public static void createAndLoadOntology(String ontologyURI) {
        Model model = TDBProvider.createModel(ontologyURI);
        model.read(ontologyURI);
    }

    @AfterClass
    public static void tearDownCase() throws Exception {
        for (String model : TDBProvider.listModels()) {
            TDB.sync(TDBProvider.getModel(model));
        }
        TDB.closedown();
        File file = new File("target/testDir");
        if (file.isDirectory()) {
            try {
                FileUtils.deleteDirectory(file);
            } catch (Exception e) {}
        }
    }

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {}

    @Test
    public final void testCreateModel() {
        for (String uri : testURIs) {
            createAndLoadOntology(uri);
        }
    }

    @Test
    public final void testGetModel() {
        for (String uri : testURIs) {
            Model model = TDBProvider.getModel(uri);
            assertNotNull(model);
            assertFalse(model.isEmpty());
            assertFalse(model.listStatements().toList().size() == 0);
        }
    }

    @Test
    public final void testGetResourceManager() {

    }

    @Test
    public final void testHasModel() {
        for (String uri : testURIs) {
            assertTrue(TDBProvider.hasModel(uri));
        }
        assertFalse(TDBProvider.hasModel("http://somedummyURI"));
        assertFalse(TDBProvider.hasModel("http://anotherDummyURI"));
    }

    @Test
    public final void testListModels() {
        List<String> includedURIs = TDBProvider.listModels();
        assertEquals(includedURIs.size(), testURIs.length);
        for (String uri : testURIs) {
            assertTrue(includedURIs.contains(uri));
        }
    }

    @Test
    public final void removeModel() {
        for (int i = 0; i < testURIs.length; i++) {
            String uri = testURIs[i];
            TDBProvider.removeModel(uri);
            assertEquals(testURIs.length - i - 1, TDBProvider.listModels().size());
            assertNull(TDBProvider.getModel(uri));
        }
    }

    @Test
    public final void clean() {
        TDBProvider.clear();

        for (String uri : testURIs) {
            assertFalse(TDBProvider.hasModel(uri));
        }
        for (String uri : testURIs) {
            Model model = TDBProvider.getModel(uri);
            assertNull(model);
        }
    }
}

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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.apache.stanbol.rules.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerImpl;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.manager.changes.RuleStoreImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.AutoIRIMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author elvio
 */
public class RuleStoreTest {

    private Logger log = LoggerFactory.getLogger(getClass());

    public RuleStoreTest() {}

    @BeforeClass
    public static void setUpClass() throws Exception {}

    @AfterClass
    public static void tearDownClass() throws Exception {}

    @Before
    public void setUp() {
        Dictionary<String,Object> configuration = new Hashtable<String,Object>();
        store = new RuleStoreImpl(configuration, "./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
        blankStore = new RuleStoreImpl(configuration, "");
    }

    @After
    public void tearDown() {
        store = null;
        blankStore = null;
    }

    public RuleStore store = null, blankStore = null;

    @Test
    public void testKReSRuleStore() throws Exception {
        OWLOntology owlmodel = store.getOntology();
        log.debug("Path for default store config is " + blankStore.getFilePath());
        assertNotNull(owlmodel);
        OWLOntologyManager owlmanager = OWLManager.createOWLOntologyManager();
        owlmanager.addIRIMapper(new AutoIRIMapper(new File("./src/main/resources/RuleOntology/"), false));
        String src = "";
        try {
            src = "./src/main/resources/RuleOntology/TestKReSOntologyRules.owl";
            assertEquals(owlmodel, owlmanager.loadOntologyFromOntologyDocument(new File(src)));
        } catch (Exception e) {
            try {
                src = "./src/main/resources/RuleOntology/OffLineKReSOntologyRules.owl";
                assertEquals(owlmodel, owlmanager.loadOntologyFromOntologyDocument(new File(src)));
            } catch (OWLOntologyCreationException ex) {
                fail("OWLOntologyCreationException caught when loading from " + src);
            }
        }
    }

    @Test
    public void testKReSRuleStore_2() throws Exception {
        OWLOntology owlmodel = blankStore.getOntology();
        log.debug("Path for default store config is " + blankStore.getFilePath());
        assertNotNull(owlmodel);
        assertTrue(!owlmodel.isEmpty());
    }

}
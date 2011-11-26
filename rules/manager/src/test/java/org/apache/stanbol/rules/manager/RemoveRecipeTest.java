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
import static org.junit.Assert.fail;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerImpl;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.manager.changes.LoadRuleFile;
import org.apache.stanbol.rules.manager.changes.RemoveRecipe;
import org.apache.stanbol.rules.manager.changes.RuleStoreImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

/**
 * 
 * @author elvio
 */
public class RemoveRecipeTest {

    public RemoveRecipeTest() {}

    @BeforeClass
    public static void setUpClass() throws Exception {}

    @AfterClass
    public static void tearDownClass() throws Exception {}

    @Before
    public void setUp() {
        Dictionary<String,Object> configuration = new Hashtable<String,Object>();
        store = new RuleStoreImpl(configuration,
                "./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
    }

    @After
    public void tearDown() {
        store = null;
    }

    public RuleStore store = null;

    /**
     * Test of removeRule method, of class RemoveRecipe.
     */
    @Test
    public void testRemoveRule_String() throws OWLOntologyCreationException {
        // RuleStore store = new RuleStoreImpl("./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
        String owlID = store.getOntology().getOntologyID().toString().replace("<", "").replace(">", "") + "#";
        // Load the example file
        LoadRuleFile load = new LoadRuleFile("./src/main/resources/RuleOntology/TestRuleFileExample.txt",
                store);
        OWLOntology owlstart = load.getStore().getOntology();
        String recipeName = owlID + "MyRecipe";
        RemoveRecipe instance = new RemoveRecipe(load.getStore());

        boolean expResult = true;
        boolean result = instance.removeRecipe(IRI.create(recipeName));
        OWLOntology owlend = instance.getStore().getOntology();

        if (result) {
            assertEquals(expResult, (owlstart.getAxiomCount() > owlend.getAxiomCount()));
            // TODO review the generated test code and remove the default call to fail.
        } else {
            fail("Some errors occur with removeRule of KReSRemoveRule.");
        }
    }

    /**
     * Test of removeRule method, of class RemoveRecipe.
     */
    @Test
    public void testRemoveRuleName_String() throws OWLOntologyCreationException {
        // RuleStore store = new RuleStoreImpl("./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
        // Load the example file
        LoadRuleFile load = new LoadRuleFile("./src/main/resources/RuleOntology/TestRuleFileExample.txt",
                store);
        OWLOntology owlstart = load.getStore().getOntology();
        String recipeName = "MyRecipe";
        RemoveRecipe instance = new RemoveRecipe(load.getStore());

        boolean expResult = true;
        boolean result = instance.removeRecipe(recipeName);
        OWLOntology owlend = instance.getStore().getOntology();

        if (result) {
            assertEquals(expResult, (owlstart.getAxiomCount() > owlend.getAxiomCount()));
            // TODO review the generated test code and remove the default call to fail.
        } else {
            fail("Some errors occur with removeRule of KReSRemoveRule.");
        }
    }

}
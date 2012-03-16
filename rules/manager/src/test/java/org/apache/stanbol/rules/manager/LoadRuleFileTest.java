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
import java.util.Iterator;
import java.util.Set;

import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.manager.changes.LoadRuleFile;
import org.apache.stanbol.rules.manager.changes.RuleStoreImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author elvio
 */
public class LoadRuleFileTest {
    
    private Logger log = LoggerFactory.getLogger(getClass());

    public LoadRuleFileTest() {}

    @BeforeClass
    public static void setUpClass() throws Exception {}

    @AfterClass
    public static void tearDownClass() throws Exception {}

    @Before
    public void setUp() {}

    @After
    public void tearDown() {}

    /**
     * Test of LoadRuleFile method, of class LoadRuleFile.
     */
    @Test
    public void testKReSLoadRuleFile() throws OWLOntologyStorageException {
        Dictionary<String,Object> configuration = new Hashtable<String,Object>();
        Dictionary<String,Object> configuration2 = new Hashtable<String,Object>();
        // configuration2.put(RuleStoreImpl.RULE_ONTOLOGY, "");
        configuration2.put(RuleStore.RULE_NAMESPACE, "http://kres.iks-project.eu/ontology/meta/rmi.owl#");
        RuleStore store = new RuleStoreImpl(configuration2,
                "./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
        RuleStore newstore = new RuleStoreImpl(configuration2, store.getOntology());
        // Load the example file
        LoadRuleFile load = new LoadRuleFile("./src/main/resources/RuleOntology/TestRuleFileExample.txt",
                store);
        OWLOntology result = load.getStore().getOntology();

        // //////////////////////////////////////////////////////////////////
        // Create ontology
        OWLOntologyManager owlmanager = OWLManager.createOWLOntologyManager();
        OWLOntology owlmodel = newstore.getOntology();
        OWLDataFactory factory = owlmanager.getOWLDataFactory();
        String ID = owlmodel.getOntologyID().toString().replace("<", "").replace(">", "") + "#";
        
        log.debug(ID);
        
        // KReSRule
        OWLClass ontocls = factory.getOWLClass(IRI.create(ID + "KReSRule"));

        // MyRuleX
        String rule = "PREFIX var http://kres.iksproject.eu/rules# ."
                      + "PREFIX dbs http://andriry.altervista.org/tesiSpecialistica/dbs_l1.owl# ."
                      + "PREFIX lmm http://www.ontologydesignpatterns.org/ont/lmm/LMM_L1.owl# ."
                      + "rule1[dbs:Table(?x) -> lmm:Meaning(?x)]";
        OWLNamedIndividual ontoind = factory.getOWLNamedIndividual(IRI.create(ID + "MyRuleX"));
        OWLClassAssertionAxiom classAssertion = factory.getOWLClassAssertionAxiom(ontocls, ontoind);
        owlmanager.addAxiom(owlmodel, classAssertion);
        OWLDataProperty dataprop = factory.getOWLDataProperty(IRI.create(ID + "hasDescription"));
        OWLDataPropertyAssertionAxiom dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(dataprop,
            ontoind, "My comment to the rule X");
        owlmanager.addAxiom(owlmodel, dataPropAssertion);
        dataprop = factory.getOWLDataProperty(IRI.create(ID + "hasBodyAndHead"));
        dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(dataprop, ontoind, rule);
        owlmanager.addAxiom(owlmodel, dataPropAssertion);

        // MyRuleA
        ontoind = factory.getOWLNamedIndividual(IRI.create(ID + "MyRuleA"));
        classAssertion = factory.getOWLClassAssertionAxiom(ontocls, ontoind);
        owlmanager.addAxiom(owlmodel, classAssertion);
        dataprop = factory.getOWLDataProperty(IRI.create(ID + "hasDescription"));
        dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(dataprop, ontoind,
            "My comment to the rule A");
        owlmanager.addAxiom(owlmodel, dataPropAssertion);
        dataprop = factory.getOWLDataProperty(IRI.create(ID + "hasBodyAndHead"));
        dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(dataprop, ontoind,
            "MyRuleABody -> MyRuleAHead");
        owlmanager.addAxiom(owlmodel, dataPropAssertion);

        // MyRuleB
        ontoind = factory.getOWLNamedIndividual(IRI.create(ID + "MyRuleB"));
        classAssertion = factory.getOWLClassAssertionAxiom(ontocls, ontoind);
        owlmanager.addAxiom(owlmodel, classAssertion);
        dataprop = factory.getOWLDataProperty(IRI.create(ID + "hasDescription"));
        dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(dataprop, ontoind,
            "My comment to the rule B");
        owlmanager.addAxiom(owlmodel, dataPropAssertion);
        dataprop = factory.getOWLDataProperty(IRI.create(ID + "hasBodyAndHead"));
        dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(dataprop, ontoind,
            "MyRuleBBody -> MyRuleBHead");
        owlmanager.addAxiom(owlmodel, dataPropAssertion);

        // MyRuleC
        ontoind = factory.getOWLNamedIndividual(IRI.create(ID + "MyRuleC"));
        classAssertion = factory.getOWLClassAssertionAxiom(ontocls, ontoind);
        owlmanager.addAxiom(owlmodel, classAssertion);
        dataprop = factory.getOWLDataProperty(IRI.create(ID + "hasDescription"));
        dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(dataprop, ontoind,
            "My comment to the rule C");
        owlmanager.addAxiom(owlmodel, dataPropAssertion);
        dataprop = factory.getOWLDataProperty(IRI.create(ID + "hasBodyAndHead"));
        dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(dataprop, ontoind,
            "MyRuleCBody -> MyRuleCHead");
        owlmanager.addAxiom(owlmodel, dataPropAssertion);

        // MyRuleD
        ontoind = factory.getOWLNamedIndividual(IRI.create(ID + "MyRuleD"));
        classAssertion = factory.getOWLClassAssertionAxiom(ontocls, ontoind);
        owlmanager.addAxiom(owlmodel, classAssertion);
        dataprop = factory.getOWLDataProperty(IRI.create(ID + "hasDescription"));
        dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(dataprop, ontoind,
            "My comment to the rule D");
        owlmanager.addAxiom(owlmodel, dataPropAssertion);
        dataprop = factory.getOWLDataProperty(IRI.create(ID + "hasBodyAndHead"));
        dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(dataprop, ontoind,
            "MyRuleDBody -> MyRuleDHead");
        owlmanager.addAxiom(owlmodel, dataPropAssertion);

        // MyRuleE
        ontoind = factory.getOWLNamedIndividual(IRI.create(ID + "MyRuleE"));
        classAssertion = factory.getOWLClassAssertionAxiom(ontocls, ontoind);
        owlmanager.addAxiom(owlmodel, classAssertion);
        dataprop = factory.getOWLDataProperty(IRI.create(ID + "hasDescription"));
        dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(dataprop, ontoind,
            "My comment to the rule E");
        owlmanager.addAxiom(owlmodel, dataPropAssertion);
        dataprop = factory.getOWLDataProperty(IRI.create(ID + "hasBodyAndHead"));
        dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(dataprop, ontoind,
            "MyRuleEBody -> MyRuleEHead");
        owlmanager.addAxiom(owlmodel, dataPropAssertion);

        // MyRuleF
        ontoind = factory.getOWLNamedIndividual(IRI.create(ID + "MyRuleF"));
        classAssertion = factory.getOWLClassAssertionAxiom(ontocls, ontoind);
        owlmanager.addAxiom(owlmodel, classAssertion);
        dataprop = factory.getOWLDataProperty(IRI.create(ID + "hasDescription"));
        dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(dataprop, ontoind,
            "My comment to the rule F");
        owlmanager.addAxiom(owlmodel, dataPropAssertion);
        dataprop = factory.getOWLDataProperty(IRI.create(ID + "hasBodyAndHead"));
        dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(dataprop, ontoind,
            "MyRuleFBody -> MyRuleFHead");
        owlmanager.addAxiom(owlmodel, dataPropAssertion);

        // Recipe
        ontocls = factory.getOWLClass(IRI.create(ID + "Recipe"));

        // Add sequence
        ontoind = factory.getOWLNamedIndividual(IRI.create(ID + "MyRecipe"));
        dataprop = factory.getOWLDataProperty(IRI.create(ID + "hasDescription"));
        dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(dataprop, ontoind,
            "My comment to the recipe");
        owlmanager.addAxiom(owlmodel, dataPropAssertion);

        OWLObjectProperty objprop = factory.getOWLObjectProperty(IRI.create(ID + "hasRule"));

        OWLNamedIndividual ruleind = factory.getOWLNamedIndividual(IRI.create(ID + "MyRuleC"));
        OWLObjectPropertyAssertionAxiom objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(
            objprop, ontoind, ruleind);
        owlmanager.addAxiom(owlmodel, objectPropAssertion);

        ruleind = factory.getOWLNamedIndividual(IRI.create(ID + "MyRuleB"));
        objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(objprop, ontoind, ruleind);
        owlmanager.addAxiom(owlmodel, objectPropAssertion);

        ruleind = factory.getOWLNamedIndividual(IRI.create(ID + "MyRuleA"));
        objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(objprop, ontoind, ruleind);
        owlmanager.addAxiom(owlmodel, objectPropAssertion);

        objprop = factory.getOWLObjectProperty(IRI.create(ID + "startWith"));
        ruleind = factory.getOWLNamedIndividual(IRI.create(ID + "MyRuleC"));
        objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(objprop, ontoind, ruleind);
        owlmanager.addAxiom(owlmodel, objectPropAssertion);

        objprop = factory.getOWLObjectProperty(IRI.create(ID + "endWith"));
        ruleind = factory.getOWLNamedIndividual(IRI.create(ID + "MyRuleA"));
        objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(objprop, ontoind, ruleind);
        owlmanager.addAxiom(owlmodel, objectPropAssertion);

        objprop = factory.getOWLObjectProperty(IRI
                .create("http://www.ontologydesignpatterns.org/cp/owl/sequence.owl#directlyPrecedes"));
        OWLNamedIndividual ruleindp = factory.getOWLNamedIndividual(IRI.create(ID + "MyRuleC"));
        OWLNamedIndividual ruleindf = factory.getOWLNamedIndividual(IRI.create(ID + "MyRuleB"));
        objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(objprop, ruleindp, ruleindf);
        owlmanager.addAxiom(owlmodel, objectPropAssertion);

        ruleindp = factory.getOWLNamedIndividual(IRI.create(ID + "MyRuleB"));
        ruleindf = factory.getOWLNamedIndividual(IRI.create(ID + "MyRuleA"));
        objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(objprop, ruleindp, ruleindf);
        owlmanager.addAxiom(owlmodel, objectPropAssertion);

        ontoind = factory.getOWLNamedIndividual(IRI.create(ID + "MyRecipe"));
        dataprop = factory.getOWLDataProperty(IRI.create(ID + "hasSequence"));
        dataPropAssertion = factory
                .getOWLDataPropertyAssertionAxiom(
                    dataprop,
                    ontoind,
                    "http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleC, http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleB, http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleA");
        owlmanager.addAxiom(owlmodel, dataPropAssertion);

        // Add sequence
        ontoind = factory.getOWLNamedIndividual(IRI.create(ID + "MyRecipe2"));
        dataprop = factory.getOWLDataProperty(IRI.create(ID + "hasDescription"));
        dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(dataprop, ontoind,
            "My comment to the recipe 2");
        owlmanager.addAxiom(owlmodel, dataPropAssertion);

        objprop = factory.getOWLObjectProperty(IRI.create(ID + "hasRule"));

        ruleind = factory.getOWLNamedIndividual(IRI.create(ID + "MyRuleD"));
        objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(objprop, ontoind, ruleind);
        owlmanager.addAxiom(owlmodel, objectPropAssertion);

        ruleind = factory.getOWLNamedIndividual(IRI.create(ID + "MyRuleE"));
        objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(objprop, ontoind, ruleind);
        owlmanager.addAxiom(owlmodel, objectPropAssertion);

        objprop = factory.getOWLObjectProperty(IRI.create(ID + "startWith"));
        ruleind = factory.getOWLNamedIndividual(IRI.create(ID + "MyRuleE"));
        objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(objprop, ontoind, ruleind);
        owlmanager.addAxiom(owlmodel, objectPropAssertion);

        objprop = factory.getOWLObjectProperty(IRI.create(ID + "endWith"));
        ruleind = factory.getOWLNamedIndividual(IRI.create(ID + "MyRuleD"));
        objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(objprop, ontoind, ruleind);
        owlmanager.addAxiom(owlmodel, objectPropAssertion);

        objprop = factory.getOWLObjectProperty(IRI
                .create("http://www.ontologydesignpatterns.org/cp/owl/sequence.owl#directlyPrecedes"));
        ruleindp = factory.getOWLNamedIndividual(IRI.create(ID + "MyRuleE"));
        ruleindf = factory.getOWLNamedIndividual(IRI.create(ID + "MyRuleD"));
        objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(objprop, ruleindp, ruleindf);
        owlmanager.addAxiom(owlmodel, objectPropAssertion);

        ontoind = factory.getOWLNamedIndividual(IRI.create(ID + "MyRecipe2"));
        dataprop = factory.getOWLDataProperty(IRI.create(ID + "hasSequence"));
        dataPropAssertion = factory
                .getOWLDataPropertyAssertionAxiom(
                    dataprop,
                    ontoind,
                    "http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleE, http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleD");
        owlmanager.addAxiom(owlmodel, dataPropAssertion);

        // Add sequence
        ontoind = factory.getOWLNamedIndividual(IRI.create(ID + "MyRecipe3"));
        dataprop = factory.getOWLDataProperty(IRI.create(ID + "hasDescription"));
        dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(dataprop, ontoind,
            "My comment to the recipe 3");
        owlmanager.addAxiom(owlmodel, dataPropAssertion);

        objprop = factory.getOWLObjectProperty(IRI.create(ID + "hasRule"));

        ruleind = factory.getOWLNamedIndividual(IRI.create(ID + "MyRuleF"));
        objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(objprop, ontoind, ruleind);
        owlmanager.addAxiom(owlmodel, objectPropAssertion);

        objprop = factory.getOWLObjectProperty(IRI.create(ID + "startWith"));
        ruleind = factory.getOWLNamedIndividual(IRI.create(ID + "MyRuleF"));
        objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(objprop, ontoind, ruleind);
        owlmanager.addAxiom(owlmodel, objectPropAssertion);

        ontoind = factory.getOWLNamedIndividual(IRI.create(ID + "MyRecipe3"));
        dataprop = factory.getOWLDataProperty(IRI.create(ID + "hasSequence"));
        dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(dataprop, ontoind,
            "http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleF");
        owlmanager.addAxiom(owlmodel, dataPropAssertion);
        // //////////////////////////////////////////////////////////////////

        // Get axiom
        int numexp = owlmodel.getAxiomCount();
        Set<OWLAxiom> expaxiom = owlmodel.getAxioms();
        int numres = result.getAxiomCount();
        if (result != null) {
            int num = 0;
            Iterator<OWLAxiom> axiom = result.getAxioms().iterator();

            while (axiom.hasNext()) {

                OWLAxiom ax = axiom.next();
                if (expaxiom.contains(ax)) num++;
            }
            log.debug(numexp + " " + numres + " " + num + " " + numres);
            assertEquals(numexp - numres, num - numres);
            // assertEquals(numexp,(numres+12));
            // TODO review the generated test code and remove the default call to fail.
        } else {
            fail("Some problem accours");
        }
    }

}
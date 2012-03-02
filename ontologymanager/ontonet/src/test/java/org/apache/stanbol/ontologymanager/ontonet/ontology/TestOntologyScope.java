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
package org.apache.stanbol.ontologymanager.ontonet.ontology;

import static org.apache.stanbol.ontologymanager.ontonet.MockOsgiContext.*;
import static org.junit.Assert.*;

import org.apache.stanbol.commons.owl.OWLOntologyManagerFactory;
import org.apache.stanbol.ontologymanager.ontonet.Constants;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.DuplicateIDException;
import org.apache.stanbol.ontologymanager.ontonet.api.io.BlankOntologySource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.RootOntologySource;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologyScopeFactory;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.ScopeRegistry;
import org.apache.stanbol.ontologymanager.ontonet.impl.ontology.OntologyScopeFactoryImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.owlapi.CoreOntologySpaceImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.owlapi.CustomOntologySpaceImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class TestOntologyScope {

    public static IRI baseIri = IRI.create(Constants.PEANUTS_MAIN_BASE), baseIri2 = IRI
            .create(Constants.PEANUTS_MINOR_BASE);

    /**
     * An ontology scope that initially contains no ontologies, and is rebuilt from scratch before each test
     * method.
     */
    private static OntologyScope blankScope;

    private static OntologyScopeFactory factory = null;

    public static String scopeIdBlank = "WackyRaces", scopeId1 = "Peanuts", scopeId2 = "CalvinAndHobbes";

    private static OntologyInputSource src1 = null, src2 = null;

    @BeforeClass
    public static void setup() {
        factory = onManager.getOntologyScopeFactory();
        if (factory == null) fail("Could not instantiate ontology space factory");
        OWLOntologyManager mgr = OWLOntologyManagerFactory.createOWLOntologyManager(null);
        try {
            src1 = new RootOntologySource(mgr.createOntology(baseIri), null);
            src2 = new RootOntologySource(mgr.createOntology(baseIri2), null);
        } catch (OWLOntologyCreationException e) {
            fail("Could not setup ontology with base IRI " + Constants.PEANUTS_MAIN_BASE);
        }
    }

    @After
    public void cleanup() {
        reset();
    }

    @Before
    public void cleanupScope() throws DuplicateIDException {
        if (factory != null) blankScope = factory.createOntologyScope(scopeIdBlank);
    }

    /**
     * Tests that a scope is generated with the expected identifiers for both itself and its core and custom
     * spaces.
     */
    @Test
    public void testIdentifiers() throws Exception {

        OntologyScope shouldBeNull = null, shouldBeNotNull = null;

        /* First test scope identifiers. */

        // Null identifier (invalid)
        try {
            shouldBeNull = factory.createOntologyScope(null, new BlankOntologySource());
            fail("Expected IllegalArgumentException not thrown despite null scope identifier.");
        } catch (IllegalArgumentException ex) {}
        assertNull(shouldBeNull);

        // Slash in identifier (invalid)
        try {
            shouldBeNull = factory.createOntologyScope("a0/b1", new BlankOntologySource());
            fail("Expected IllegalArgumentException not thrown despite slash in scope identifier.");
        } catch (IllegalArgumentException ex) {}
        assertNull(shouldBeNull);

        /* Now test namespaces. */

        // Null namespace (invalid).
        factory.setNamespace(null);
        try {
            shouldBeNull = factory.createOntologyScope(scopeIdBlank, new BlankOntologySource());
            fail("Expected IllegalArgumentException not thrown despite null OntoNet namespace.");
        } catch (IllegalArgumentException ex) {}
        assertNull(shouldBeNull);

        // Namespace with query (invalid).
        factory.setNamespace(IRI.create("http://stanbol.apache.org/ontology/?query=true"));
        try {
            shouldBeNull = factory.createOntologyScope(scopeIdBlank, new BlankOntologySource());
            fail("Expected IllegalArgumentException not thrown despite query in OntoNet namespace.");
        } catch (IllegalArgumentException ex) {}
        assertNull(shouldBeNull);

        // Namespace with fragment (invalid).
        factory.setNamespace(IRI.create("http://stanbol.apache.org/ontology#fragment"));
        try {
            shouldBeNull = factory.createOntologyScope(scopeIdBlank, new BlankOntologySource());
            fail("Expected IllegalArgumentException not thrown despite fragment in OntoNet namespace.");
        } catch (IllegalArgumentException ex) {}
        assertNull(shouldBeNull);

        // Namespace ending with hash (invalid).
        factory.setNamespace(IRI.create("http://stanbol.apache.org/ontology#"));
        try {
            shouldBeNull = factory.createOntologyScope(scopeIdBlank, new BlankOntologySource());
            fail("Expected IllegalArgumentException not thrown despite fragment in OntoNet namespace.");
        } catch (IllegalArgumentException ex) {}
        assertNull(shouldBeNull);

        // Namespace ending with slash (valid).
        factory.setNamespace(IRI.create("http://stanbol.apache.org/ontology/"));
        shouldBeNotNull = factory.createOntologyScope(scopeIdBlank, new BlankOntologySource());
        assertNotNull(shouldBeNotNull);

        shouldBeNotNull = null;

        // Namespace ending with neither (valid, should automatically add slash).
        factory.setNamespace(IRI.create("http://stanbol.apache.org/ontology"));
        shouldBeNotNull = factory.createOntologyScope(scopeIdBlank, new BlankOntologySource());
        assertNotNull(shouldBeNotNull);
        assertTrue(shouldBeNotNull.getNamespace().toString().endsWith("/"));

        // Now set again the correct namespace.
        factory.setNamespace(IRI.create(onManager.getOntologyNetworkNamespace()));
        shouldBeNotNull = null;
        try {
            shouldBeNotNull = factory.createOntologyScope(scopeId1, src1, src2);
            shouldBeNotNull.setUp();
        } catch (DuplicateIDException e) {
            fail("Unexpected DuplicateIDException caught when creating scope "
                 + "with non-null parameters in a non-registered environment.");
        }
        boolean condition = shouldBeNotNull.getID().equals(scopeId1);
        condition &= shouldBeNotNull.getCoreSpace().getID()
                .equals(scopeId1 + "/" + CoreOntologySpaceImpl.SUFFIX);
        condition &= shouldBeNotNull.getCustomSpace().getID()
                .equals(scopeId1 + "/" + CustomOntologySpaceImpl.SUFFIX);
        assertTrue(condition);
    }

    /**
     * Tests that creating an ontology scope with null identifier fails to generate the scope at all.
     */
    @Test
    public void testNullScopeCreation() {
        OntologyScope scope = null;
        try {
            scope = factory.createOntologyScope(null, (OntologyInputSource) null);
        } catch (DuplicateIDException e) {
            fail("Unexpected DuplicateIDException caught while testing scope creation"
                 + " with null parameters.");
        } catch (IllegalArgumentException ex) {
            // Expected behaviour.
        }
        assertNull(scope);
    }

    /**
     * Tests that an ontology scope is correctly generated with both a core space and a custom space. The
     * scope is set up but not registered.
     */
    @Test
    public void testScopeSetup() {
        OntologyScope scope = null;
        try {
            scope = factory.createOntologyScope(scopeId1, src1, src2);
            scope.setUp();
        } catch (DuplicateIDException e) {
            fail("Unexpected DuplicateIDException was caught while testing scope " + e.getDuplicateID());
        }
        assertNotNull(scope);
    }

    /**
     * Tests that an ontology scope is correctly generated even when missing a custom space. The scope is set
     * up but not registered.
     */
    @Test
    public void testScopeSetupNoCustom() {
        OntologyScope scope = null;
        try {
            scope = factory.createOntologyScope(scopeId2, src1);
            scope.setUp();
        } catch (DuplicateIDException e) {
            fail("Duplicate ID exception caught for scope iri " + src1);
        }

        assertTrue(scope != null && scope.getCoreSpace() != null && scope.getCustomSpace() != null);
    }

    @Test
    public void testScopesRendering() {
        ScopeRegistry reg = onManager.getScopeRegistry();
        OntologyScopeFactoryImpl scf = new OntologyScopeFactoryImpl(reg, onManager.getOntologyScopeFactory()
                .getNamespace(), onManager.getOntologySpaceFactory());
        OntologyScope scope = null, scope2 = null;
        try {
            scope = scf.createOntologyScope(scopeId1, src1, src2);
            scope2 = scf.createOntologyScope(scopeId2, src2);
            scope.setUp();
            reg.registerScope(scope);
            scope2.setUp();
            reg.registerScope(scope2);
        } catch (DuplicateIDException e) {
            fail("Duplicate ID exception caught on " + e.getDuplicateID());
        }
        // System.err.println(new ScopeSetRenderer().getScopesAsRDF(reg
        // .getRegisteredScopes()));
        assertTrue(true);
    }

}

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
package org.apache.stanbol.ontologymanager.ontonet.registry;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.stanbol.ontologymanager.ontonet.api.DuplicateIDException;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManagerConfiguration;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.ParentPathInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.CoreOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.SessionOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.UnmodifiableOntologySpaceException;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.RegistryLoader;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.io.OntologyRegistryIRISource;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerConfigurationImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerImpl;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.AutoIRIMapper;

public class TestRegistry {
	private static OWLOntologyManager ontologyManager;
	private static RegistryLoader loader;
	private static OntologyRegistryIRISource ontologySource;
	private static ONManagerConfiguration configuration;
	private static ONManager onm;

	private static IRI testRegistryIri = IRI
			.create("http://www.ontologydesignpatterns.org/registry/krestest.owl");

	@SuppressWarnings("unused")
	private static IRI submissionsIri = IRI
			.create("http://www.ontologydesignpatterns.org/registry/submissions.owl");

	@BeforeClass
	public static void setup() {
		// An ONManagerImpl with no store and default settings
		configuration = new ONManagerConfigurationImpl(
				new Hashtable<String, Object>());
		onm = new ONManagerImpl(null, null, configuration,
				new Hashtable<String, Object>());
		ontologyManager = onm.getOwlCacheManager();
		loader = onm.getRegistryLoader();
		

	}

	private static boolean mapperIsSet = false;

	public void setupOfflineMapper() {
		if (mapperIsSet) {
		} else {
			ontologySource = new OntologyRegistryIRISource(testRegistryIri,
					ontologyManager, loader);
			mapperIsSet = true;
		}
	}

	@Test
	public void testAddRegistryToSessionSpace() {
		setupOfflineMapper();
		IRI scopeIri = IRI.create("http://fise.iks-project.eu/scopone");
		SessionOntologySpace space = null;
		space = onm.getOntologySpaceFactory().createSessionOntologySpace(
				scopeIri);
		space.setUp();
		try {
			// space.addOntology(new
			// OntologyRegistryIRISource(testRegistryIri,onm.getOwlCacheManager(),onm.getRegistryLoader()));
			space.addOntology(ontologySource);
		} catch (UnmodifiableOntologySpaceException e) {
			fail("Adding libraries to session space failed. "
					+ "This should not happen for active session spaces.");
		}

		assertTrue(space.getTopOntology() != null);
		assertTrue(space.getOntologies().contains(space.getTopOntology()));
	}

	@Test
	public void testScopeCreationWithRegistry() {
		setupOfflineMapper();
		IRI scopeIri = IRI.create("http://fise.iks-project.eu/scopone");
		OntologyScope scope = null;
		// The factory call also invokes loadRegistriesEager() and
		// gatherOntologies() so no need to test them individually.
		try {
			scope = onm.getOntologyScopeFactory().createOntologyScope(scopeIri,
					ontologySource);
		} catch (DuplicateIDException e) {
			fail("DuplicateID exception caught when creating test scope.");
		}

		assertTrue(scope != null
				&& scope.getCoreSpace().getTopOntology() != null);
		// OntologyUtils.printOntology(scope.getCoreSpace().getTopOntology(),
		// System.err);
	}

	@Test
	public void testSpaceCreationWithRegistry() {
		setupOfflineMapper();
		IRI scopeIri = IRI.create("http://fise.iks-project.eu/scopone");
		CoreOntologySpace space = null;
		// The factory call also invokes loadRegistriesEager() and
		// gatherOntologies() so no need to test them individually.
		space = onm.getOntologySpaceFactory().createCoreOntologySpace(scopeIri,
				ontologySource);
		assertTrue(space != null && space.getTopOntology() != null);
	}

}

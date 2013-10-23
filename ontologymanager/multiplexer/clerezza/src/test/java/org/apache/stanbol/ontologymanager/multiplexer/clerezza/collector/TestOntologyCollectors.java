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
package org.apache.stanbol.ontologymanager.multiplexer.clerezza.collector;

import static org.apache.stanbol.ontologymanager.multiplexer.clerezza.MockOsgiContext.ontologyProvider;
import static org.apache.stanbol.ontologymanager.multiplexer.clerezza.MockOsgiContext.reset;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import org.apache.stanbol.commons.owl.PhonyIRIMapper;
import org.apache.stanbol.ontologymanager.multiplexer.clerezza.impl.CustomSpaceImpl;
import org.apache.stanbol.ontologymanager.servicesapi.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.servicesapi.scope.OntologySpace;
import org.apache.stanbol.ontologymanager.sources.owlapi.ParentPathInputSource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyLoaderListener;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestOntologyCollectors {

    private IRI scopeNs = IRI.create("http://stanbol.apache.org/ontologies/");

    private Logger log = LoggerFactory.getLogger(getClass());

    @After
    public void cleanup() {
        reset();
    }

    @Test
    public void sessionMergesOntologies() throws Exception {
        // TODO after merging is implemented
        assertTrue(true);
    }

    @Test
    public void sessionPreservesImports() throws Exception {

    }

    @Test
    public void spaceMergesOntologies() throws Exception {
        // TODO after merging is implemented
        assertTrue(true);
    }

    @Test
    public void spacePreservesImports() throws Exception {
        InputStream content = getClass().getResourceAsStream("/ontologies/characters_all.owl");
        URL url = getClass().getResource("/ontologies/characters_all.owl");
        OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
        mgr.addOntologyLoaderListener(new OWLOntologyLoaderListener() {

            @Override
            public void startedLoadingOntology(LoadingStartedEvent arg0) {}

            @Override
            public void finishedLoadingOntology(LoadingFinishedEvent arg0) {
                log.info((arg0.isSuccessful() ? "Loaded" : "Failed")
                         + (arg0.isImported() ? " imported " : " ") + "ontology " + arg0.getDocumentIRI());
            }
        });

        mgr.addIRIMapper(new PhonyIRIMapper(null));

        File f = new File(url.toURI());
        OntologyInputSource<OWLOntology> src = new ParentPathInputSource(f, mgr);
        // OntologyInputSource<OWLOntology> src = new RootOntologyIRISource(IRI.create(f), mgr);

        // OntologyInputSource<OWLOntology> src = new OntologyContentInputSource(content,mgr);

        OWLOntology original = src.getRootOntology();
        Assert.assertNotNull(original);
        OntologySpace spc = new CustomSpaceImpl("Test", scopeNs, ontologyProvider);
        spc.addOntology(src);

    }

}

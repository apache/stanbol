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
package org.apache.stanbol.ontologymanager.sources.owlapi;

import java.io.InputStream;

import org.apache.stanbol.commons.owl.util.OWLUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An input source that tries to parse an in-memory {@link OWLOntology} object from an input stream. This
 * ontology input source will try to resolve imports and fail if one cannot be resolved. To setup a custom
 * configuration, such as adding IRI mappers, the constructor
 * {@link #OntologyContentInputSource(InputStream, OWLOntologyManager)} can be used with a configured
 * {@link OWLOntologyManager}.
 * 
 * @author alexdma
 * 
 */
public class OntologyContentInputSource extends AbstractOWLOntologyInputSource {

    private Logger log = LoggerFactory.getLogger(getClass());

    public OntologyContentInputSource(InputStream content) throws OWLOntologyCreationException {
        this(content, OWLManager.createOWLOntologyManager());
    }

    public OntologyContentInputSource(InputStream content, OWLOntologyManager manager) throws OWLOntologyCreationException {
        long before = System.currentTimeMillis();
        bindPhysicalOrigin(null);
        bindRootOntology(manager.loadOntologyFromOntologyDocument(content));
        log.debug("Input source initialization completed in {} ms.", (System.currentTimeMillis() - before));
    }

    @Override
    public String toString() {
        return "<ONTOLOGY_CONTENT>" + OWLUtils.extractOntologyID(getRootOntology());
    }

}

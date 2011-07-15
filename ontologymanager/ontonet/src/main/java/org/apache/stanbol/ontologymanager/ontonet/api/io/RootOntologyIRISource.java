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
package org.apache.stanbol.ontologymanager.ontonet.api.io;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * An input source that provides the OWL Ontology loaded from the supplied physical IRI, as well as the
 * physical IRI itself for consumers that need to load the ontology themselves.<br>
 * <br>
 * For convenience, an existing OWL ontology manager can be supplied for loading the ontology.
 * 
 * @author alessandro
 * 
 */
public class RootOntologyIRISource extends AbstractOntologyInputSource {

    public RootOntologyIRISource(IRI rootPhysicalIri) throws OWLOntologyCreationException {
        this(rootPhysicalIri, OWLManager.createOWLOntologyManager());
    }

    public RootOntologyIRISource(IRI rootPhysicalIri, OWLOntologyManager manager) throws OWLOntologyCreationException {
        bindPhysicalIri(rootPhysicalIri);
        bindRootOntology(manager.loadOntologyFromOntologyDocument(rootPhysicalIri));
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.iksproject.kres.manager.io.AbstractOntologyInputSource#toString()
     */
    @Override
    public String toString() {
        return "ROOT_ONT_IRI<" + getPhysicalIRI() + ">";
    }

}

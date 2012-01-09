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

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * An input source that provides the supplied OWL ontology straight away. The physical IRI is either obtained
 * from the default document IRI in the ontology, or supplied manually using the appropriate constructor (e.g.
 * retrieved from the ontology manager that actually loaded the ontology).
 */
public class RootOntologySource extends AbstractOWLOntologyInputSource {

    public RootOntologySource(OWLOntology rootOntology) {
        bindRootOntology(rootOntology);
        try {
            bindPhysicalIri(rootOntology.getOntologyID().getDefaultDocumentIRI());
            bindTriplesProvider(rootOntology.getOWLOntologyManager());
        } catch (Exception e) {
            // Ontology might be anonymous, no physical IRI then...
            bindPhysicalIri(null);
        }

    }

    /**
     * This constructor can be used to hijack ontologies using a physical IRI other than their default one.
     * 
     * @param rootOntology
     * @param phyicalIRI
     */
    public RootOntologySource(OWLOntology rootOntology, IRI phyicalIRI) {
        this(rootOntology);
        bindPhysicalIri(phyicalIRI);
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.iksproject.kres.manager.io.AbstractOntologyInputSource#toString()
     */
    @Override
    public String toString() {
        return "ROOT_ONT<" + rootOntology.getOntologyID() + ">";
    }

}

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

import org.apache.stanbol.ontologymanager.servicesapi.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.servicesapi.io.Origin;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * An input source that provides the supplied OWL ontology straight away or from its physical resource. The
 * physical IRI is either obtained from the default document IRI in the ontology, or supplied manually using
 * the appropriate constructor (e.g. retrieved from the ontology manager that actually loaded the ontology).<br>
 * <br>
 * Note that, no matter what constructor is used, the expected behavior of {@link OntologyInputSource}
 * consumers remains the same, i.e. the root ontology is checked first, and then its physical origin.
 * 
 * @author alexdma
 */
public class RootOntologySource extends AbstractOWLOntologyInputSource {

    /**
     * This constructor can be used when the physical IRI of the ontology is known and one wants Stanbol to
     * obtain the ontology/ies from it. Any failure to do so, including parse errors and unresolved imports,
     * will cause an {@link OWLOntologyCreationException} to be thrown.
     * 
     * @param rootPhysicalIri
     *            the physical IRI where the ontology is located.
     * @throws OWLOntologyCreationException
     *             if no ontology could be obtained by resolving the root physical IRI.
     */
    public RootOntologySource(IRI rootPhysicalIri) throws OWLOntologyCreationException {
        this(rootPhysicalIri, OWLManager.createOWLOntologyManager());
    }

    /**
     * This constructor can be used when the physical IRI of the ontology is known and one wants Stanbol to
     * obtain the ontology/ies from it, but one needs a special configuration for the ontology manager that
     * should be used for doing so, e.g. custom IRI mappers or import resolution policies. This custom
     * ontology manager can be passed as the second argument.<br>
     * <br>
     * Any failure to obtain the ontology/ies, including parse errors and unresolved imports (if set to do
     * so), will cause an {@link OWLOntologyCreationException} to be thrown.
     * 
     * @param rootPhysicalIri
     *            the physical IRI where the ontology is located.
     * @param manager
     *            the ontology manager to be used for resolving the IRI.
     * @throws OWLOntologyCreationException
     *             if no ontology could be obtained by resolving the root physical IRI.
     */
    public RootOntologySource(IRI rootPhysicalIri, OWLOntologyManager manager) throws OWLOntologyCreationException {
        bindPhysicalOrigin(Origin.create(rootPhysicalIri));
        bindRootOntology(manager.loadOntology(rootPhysicalIri));
    }

    /**
     * This constructor can be used if an {@link OWLOntology} object was obtained prior to creating this input
     * source. The {@link OWLOntology} passed in as an argument is set as the root ontology. No import check
     * or resolution is performed.
     * 
     * @param rootOntology
     *            the ontology object that will be returned by {@link #getRootOntology()}.
     */
    public RootOntologySource(OWLOntology rootOntology) {
        this(rootOntology, rootOntology.getOWLOntologyManager().getOntologyDocumentIRI(rootOntology));
    }

    /**
     * This constructor can be used if an {@link OWLOntology} object was obtained prior to creating this input
     * source, but one needs to specify that the source for that ontology is at a different IRI than the one
     * recorded by its ontology manager. The {@link OWLOntology} passed in as an argument is set as the root
     * ontology. No import check or resolution is performed.
     * 
     * @param rootOntology
     *            the ontology object that will be returned by {@link #getRootOntology()}.
     * @param physicalIriOverride
     *            the new physical location of the ontology.
     */
    public RootOntologySource(OWLOntology rootOntology, IRI physicalIriOverride) {
        if (rootOntology == null) throw new IllegalArgumentException(
                "Root ontology cannot be null. "
                        + "To submit a dummy ontology input source, please use class "
                        + BlankOntologySource.class.getCanonicalName() + " instead.");
        bindRootOntology(rootOntology);
        // Never bind logical IDs as physical IRIs, as they risk overwriting previous bindings.
        bindPhysicalOrigin(Origin.create(physicalIriOverride));
    }

    @Override
    public String toString() {
        return "ROOT_ONT<" + rootOntology.getOntologyID() + ">";
    }

}

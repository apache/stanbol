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

import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Abstract implementation of {@link OntologyInputSource} with the basic methods for obtaining root ontologies
 * and their physical IRIs where applicable.<br/>
 * </br> Implementations should either invoke abstract methods {@link #bindPhysicalIri(IRI)} and
 * {@link #bindRootOntology(OWLOntology)} in their constructors, or override them.
 * 
 */
public abstract class AbstractOntologyInputSource implements OntologyInputSource {

    protected IRI physicalIri = null;

    protected OWLOntology rootOntology = null;

    /**
     * This method is used to remind developers to bind a physical IRI to the {@link OntologyInputSource} if
     * intending to do so.
     * 
     * Implementation should assign a value to {@link #physicalIri}.
     * 
     * @param iri
     *            the physical ontology IRI.
     */
    protected void bindPhysicalIri(IRI iri) {
        this.physicalIri = iri;
    }

    /**
     * This method is used to remind developers to bind a root ontology to the {@link OntologyInputSource} if
     * intending to do so.
     * 
     * Implementation should assign a value to {@link #rootOntology}.
     * 
     * @param ontology
     *            the root ontology.
     */
    protected void bindRootOntology(OWLOntology ontology) {
        this.rootOntology = ontology;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof OntologyInputSource)) return false;
        OntologyInputSource src = (OntologyInputSource) obj;
        return this.physicalIri.equals(src.getPhysicalIRI())
               && this.rootOntology.equals(src.getRootOntology());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource#getClosure()
     */
    @Override
    public Set<OWLOntology> getClosure() {
        return rootOntology.getOWLOntologyManager().getImportsClosure(rootOntology);
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.iksproject.kres.api.manager.ontology.OntologyInputSource#getPhysicalIRI()
     */
    @Override
    public IRI getPhysicalIRI() {
        return physicalIri;
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.iksproject.kres.api.manager.ontology.OntologyInputSource#getRootOntology()
     */
    @Override
    public OWLOntology getRootOntology() {
        return rootOntology;
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.iksproject.kres.api.manager.ontology.OntologyInputSource#hasPhysicalIRI()
     */
    @Override
    public boolean hasPhysicalIRI() {
        return physicalIri != null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.iksproject.kres.api.manager.ontology.OntologyInputSource#hasRootOntology()
     */
    @Override
    public boolean hasRootOntology() {
        return rootOntology != null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public abstract String toString();

}

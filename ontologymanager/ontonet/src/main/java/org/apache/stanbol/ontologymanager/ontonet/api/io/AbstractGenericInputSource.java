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

/**
 * 
 * @author alexdma
 * 
 * @param <O>
 *            the ontologuy returned by this input source.
 */
public abstract class AbstractGenericInputSource<O,P> implements OntologyInputSource<O,P> {

    protected String key;

    protected IRI physicalIri = null;

    private P provider;

    protected O rootOntology = null;

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
    protected void bindRootOntology(O ontology) {
        this.rootOntology = ontology;
    }

    protected void bindStorageKey(String key) {
        this.key = key;
    }

    protected void bindTriplesProvider(P provider) {
        this.provider = provider;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof OntologyInputSource<?,?>)) return false;
        OntologyInputSource<?,?> src = (OntologyInputSource<?,?>) obj;
        return this.physicalIri.equals(src.getPhysicalIRI())
               && this.rootOntology.equals(src.getRootOntology());
    }

    @Override
    public IRI getPhysicalIRI() {
        return physicalIri;
    }

    @Override
    public O getRootOntology() {
        return rootOntology;
    }

    @Override
    public String getStorageKey() {
        return key;
    }

    @Override
    public P getTriplesProvider() {
        return provider;
    }

    @Override
    public boolean hasPhysicalIRI() {
        return physicalIri != null;
    }

    @Override
    public boolean hasRootOntology() {
        return rootOntology != null;
    }

    @Override
    public abstract String toString();

}

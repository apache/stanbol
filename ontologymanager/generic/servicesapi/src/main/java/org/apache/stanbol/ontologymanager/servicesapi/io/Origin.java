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
package org.apache.stanbol.ontologymanager.servicesapi.io;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.access.TcProvider;
import org.semanticweb.owlapi.model.OWLOntologyID;

/**
 * A wrapper class for whatever can be used for physically referencing a resource (typically an ontology).
 * Currently the supported types are:
 * <ul>
 * <li> {@link IRI}, which is interpreted as the physical location of the resource.
 * <li> {@link OWLOntologyID}, which is interpreted as the public key of an ontology already stored by Stanbol.
 * <li> {@link IRI}, which is interpreted as the name of a graph to be retrieved from an underlying Clerezza
 * store (typically a {@link TcProvider}).
 * </ul>
 * 
 * @author alexdma
 * 
 * @param <R>
 *            the resource reference.
 */
public class Origin<R> {

    /**
     * Creates a new Origin for a resource that can be retrieved by dereferencing the given IRI as an URL.
     * 
     * @param physicalURL
     *            the physical location of the resource
     * @return the origin that wraps this IRI.
     */
    public static Origin<org.semanticweb.owlapi.model.IRI> create(org.semanticweb.owlapi.model.IRI physicalURL) {
        return new Origin<org.semanticweb.owlapi.model.IRI>(physicalURL);
    }

    /**
     * Creates a new Origin for a resource whose public key is known. What a "public key" is interpreted to be
     * is implementation-dependent.
     * 
     * @param publicKey
     *            the public key
     * @return the origin that wraps this IRI.
     */
    public static Origin<OWLOntologyID> create(OWLOntologyID publicKey) {
        return new Origin<OWLOntologyID>(publicKey);
    }

    /**
     * Creates a new Origin for a resource that can be retrieved by querying a Clerezza store for a graph with
     * the given name.
     * 
     * @param graphName
     *            the graph name
     * @return the origin that wraps this graph name.
     */
    public static Origin<IRI> create(IRI graphName) {
        return new Origin<IRI>(graphName);
    }

    private R ref;

    /**
     * Creates a new instance of {@link Origin}.
     * 
     * @param reference
     *            the physical reference. Cannot be null
     * @throws IllegalArgumentException
     *             if a null value was supplied for <code>reference</code>.
     */
    protected Origin(R reference) {
        if (reference == null) throw new IllegalArgumentException(
                "Class " + getClass().getCanonicalName() + " does not allow a null reference object." + " "
                        + "If a null object is needed, developers should use a null Origin instead.");
        ref = reference;
    }

    @Override
    public boolean equals(Object arg0) {
        if (arg0 == null) return false;
        if (!(arg0 instanceof Origin<?>)) return false;
        return this.getReference().equals(((Origin<?>) arg0).getReference());
    }

    /**
     * Returns the actual reference object that was wrapped by this Origin.
     * 
     * @return the reference object.
     */
    public R getReference() {
        return ref;
    }

    @Override
    public String toString() {
        return "Origin(" + ref.toString() + ")";
    }

}

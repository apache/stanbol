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

/**
 * The abstract implementation of the {@link OntologyInputSource} interface which is inherited by all concrete
 * implementations.
 * 
 * @author alexdma
 * 
 * @param <O>
 *            the ontology returned by this input source.
 */
public abstract class AbstractGenericInputSource<O> implements
        org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource<O> {

    /**
     * Where the ontology object was retrieved from.
     */
    protected Origin<?> origin = null;

    protected O rootOntology = null;

    /**
     * This method is used to remind developers to bind a physical reference to the
     * {@link OntologyInputSource} if intending to do so.
     * 
     * Implementations should assign a value to {@link #origin}.
     * 
     * @param origin
     *            where the ontology object was obtained from.
     */
    protected void bindPhysicalOrigin(Origin<?> origin) {
        this.origin = origin;
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

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof OntologyInputSource<?>)) return false;
        OntologyInputSource<?> src = (OntologyInputSource<?>) obj;
        return this.origin.equals(src.getOrigin()) && this.rootOntology.equals(src.getRootOntology());
    }

    @Override
    public Origin<?> getOrigin() {
        return origin;
    }

    @Override
    public O getRootOntology() {
        return rootOntology;
    }

    @Override
    public boolean hasOrigin() {
        return origin != null;
    }

    @Override
    public boolean hasRootOntology() {
        return rootOntology != null;
    }

    @Override
    public abstract String toString();

}

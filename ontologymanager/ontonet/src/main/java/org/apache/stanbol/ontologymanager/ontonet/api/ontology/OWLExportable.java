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
package org.apache.stanbol.ontologymanager.ontonet.api.ontology;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * An object that can be represented as an {@link OWLOntology} instance.
 * 
 * @author alexdma
 * 
 */
public interface OWLExportable {

    /**
     * Returns the OWL ontology form of this object.
     * 
     * @deprecated use the method {@link #export(Class, boolean)} instead, with the first argument set as
     *             {@link OWLOntology.class}.
     * 
     * @param merge
     *            if true, all imported ontologies will be merged and no import statements will appear.
     * @return the OWL ontology that represents this object.
     * 
     */
    OWLOntology asOWLOntology(boolean merge);

    /**
     * Returns an ontological form of this object of the specified return type, if supported. If the supplied
     * class is not a supported return type, an {@link UnsupportedOperationException} is thrown. <br>
     * <br>
     * TODO replace merge parameter with integer for merge level (-1 for infinite).
     * 
     * @param returnType
     *            the desired class of the returned object.
     * @param merge
     *            if true, all imported ontologies will be merged and no import statements will appear.
     * @return the ontology that represents this object.
     */
    <O> O export(Class<O> returnType, boolean merge);

    IRI getDocumentIRI();

}

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
package org.apache.stanbol.ontologymanager.servicesapi.collector;

import org.semanticweb.owlapi.model.IRI;

/**
 * Thrown whenever an illegal attempt at removing an ontology from an ontology space is detected. This can
 * happen e.g. if the ontology is the space root or not a direct child thereof.
 * 
 * @author alexdma
 * 
 */
public class IrremovableOntologyException extends OntologyCollectorModificationException {

    protected IRI ontologyId;

    /**
	 * 
	 */
    private static final long serialVersionUID = -3301398666369788964L;

    /**
     * Constructs a new instance of <code>IrremovableOntologyException</code>.
     * 
     * @param space
     *            the space that holds the ontology.
     * @param ontologyId
     *            the logical IRI of the ontology whose removal was denied.
     */
    public IrremovableOntologyException(OntologyCollector collector, IRI ontologyId) {
        super(collector);
        this.ontologyId = ontologyId;
    }

    /**
     * Returns the unique identifier of the ontology whose removal was denied.
     * 
     * @return the ID of the ontology that was not removed.
     */
    public IRI getOntologyId() {
        return ontologyId;
    }

}

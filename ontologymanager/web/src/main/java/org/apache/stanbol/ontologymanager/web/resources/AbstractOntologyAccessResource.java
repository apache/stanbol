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
package org.apache.stanbol.ontologymanager.web.resources;

import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.ontologymanager.servicesapi.util.OntologyUtils;
import org.semanticweb.owlapi.model.OWLOntologyID;

/**
 * RESTful resources intended for performing CRUD operations on ontologies with respect to their storage
 * facilities (i.e. operations that manipulate the content of one ontology at a time) should specialize this
 * class.
 * 
 * @author alexdma
 * 
 */
public abstract class AbstractOntologyAccessResource extends BaseStanbolResource {

    /**
     * The ontology this resource was created after, and represents.
     */
    protected OWLOntologyID submitted;

    /**
     * Returns a public key of the ontology this resource was created after.
     * 
     * @return the key of the ontology represented by this resource.
     */
    public OWLOntologyID getRepresentedOntologyKey() {
        return submitted;
    }

    /**
     * Returns a canonicalized string form of a public key.
     * 
     * @param ontologyID
     *            the public key
     * @return the canonical form of the submitted public key.
     */
    public String stringForm(OWLOntologyID ontologyID) {
        return OntologyUtils.encode(ontologyID);
    }

}

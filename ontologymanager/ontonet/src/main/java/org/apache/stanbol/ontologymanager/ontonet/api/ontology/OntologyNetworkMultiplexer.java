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

import java.util.Set;

import org.apache.stanbol.ontologymanager.ontonet.api.collector.OntologyCollectorListener;
import org.semanticweb.owlapi.model.OWLOntologyID;

/**
 * The object that "knows" the relationships between stored graphs and their usage in ontology spaces or
 * sessions.
 * 
 * @author alexdma.
 * 
 */
public interface OntologyNetworkMultiplexer extends OntologyCollectorListener {

    /**
     * Returns the public keys of all the stored ontologies it is aware of.
     * 
     * @return the public key set.
     */
    Set<OWLOntologyID> getPublicKeys();

    /**
     * Returns the size in triples of the ontology with the supplied public key. Depending on the
     * implementation, the size can be cached or computed on-the-fly.
     * 
     * @param publicKey
     *            the public key of the ontology
     * @return the size in triples of the ontology.
     */
    int getSize(OWLOntologyID publicKey);

    /**
     * An utility method that computes the public key of an ontology given a canonical string form. The result
     * also depends on the stored ontologies, hence the inclusion of this non-static method with this class.
     * 
     * @param stringForm
     *            the string form of the public key.
     * @return the public key.
     */
    OWLOntologyID getPublicKey(String stringForm);

}

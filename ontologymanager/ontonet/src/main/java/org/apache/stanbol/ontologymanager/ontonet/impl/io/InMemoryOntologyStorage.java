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
package org.apache.stanbol.ontologymanager.ontonet.impl.io;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.stanbol.ontologymanager.ontonet.impl.ontology.NoSuchStoreException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * A hack that stores ontologies in volatile memory (e.g. for unit tests) but prevents the incessant logging
 * of related error messages.
 * 
 * @author alexdma
 * 
 */
public class InMemoryOntologyStorage extends ClerezzaOntologyStorage {

    private Map<IRI,OWLOntology> store;

    public InMemoryOntologyStorage() {
        store = new HashMap<IRI,OWLOntology>();
    }

    public void clear() {
        store.clear();
    }

    public void delete(IRI arg0) {
        store.remove(arg0);
    }

    public void deleteAll(Set<IRI> arg0) {
        for (IRI iri : arg0)
            delete(iri);
    }

    public OWLOntology getGraph(IRI arg0) throws NoSuchStoreException {
        return store.get(arg0);
    }

    public Set<IRI> listGraphs() {
        return store.keySet();
    }

    public OWLOntology load(IRI arg0) {
        return store.get(arg0);
    }

    public OWLOntology sparqlConstruct(String arg0, String arg1) {
        return super.sparqlConstruct(arg0, arg1);
    }

    public void store(OWLOntology arg0) {
        try {
            store.put(arg0.getOntologyID().getOntologyIRI(), arg0);
        } catch (Exception ex) {
            store.put(arg0.getOWLOntologyManager().getOntologyDocumentIRI(arg0), arg0);
        }
    }

    public void store(OWLOntology arg0, IRI arg1) {
        store.put(arg1, arg0);
    }

}

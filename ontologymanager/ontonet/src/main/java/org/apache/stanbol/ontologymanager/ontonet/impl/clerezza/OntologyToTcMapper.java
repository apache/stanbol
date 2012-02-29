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
package org.apache.stanbol.ontologymanager.ontonet.impl.clerezza;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.EntityAlreadyExistsException;
import org.apache.clerezza.rdf.core.access.TcProvider;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.semanticweb.owlapi.model.IRI;

/**
 * A map from logical/physical ontology IDs to Clerezza graph names, stored using the supplied TcProvider.
 * 
 * @author alexdma
 * 
 */
public class OntologyToTcMapper {

    /**
     * The basic terms to use for the mapping graph.
     * 
     * @author alexdma
     * 
     */
    private class Vocabulary {

        static final String _BASE_VOCAB = "http://stanbol.apache.org/ontology/ontonet/meta#";

        static final String MAPS_TO_GRAPH = _BASE_VOCAB + "mapsToGraph";

    }

    private MGraph graph;

    private UriRef graphId = new UriRef(getClass().getCanonicalName());

    private TcProvider store;

    public OntologyToTcMapper(TcProvider store) {
        if (store == null) throw new IllegalArgumentException("TcProvider cannot be null");
        this.store = store;
        try {
            graph = store.createMGraph(graphId);
        } catch (EntityAlreadyExistsException e) {
            graph = store.getMGraph(graphId);
        }
    }

    public void addMapping(IRI ontologyReference, UriRef graphName) {
        graph.add(new TripleImpl(new UriRef(ontologyReference.toString()), new UriRef(
                Vocabulary.MAPS_TO_GRAPH), graphName));
    }

    public void clearMappings() {
        graph.clear();
    }

    public UriRef getMapping(IRI ontologyReference) {
        Iterator<Triple> it = graph.filter(new UriRef(ontologyReference.toString()), new UriRef(
                Vocabulary.MAPS_TO_GRAPH), null);
        while (it.hasNext()) {
            Resource obj = it.next().getObject();
            if (obj instanceof UriRef) return (UriRef) obj;
        }
        return null;
    }

    public Set<IRI> keys() {
        Set<IRI> result = new HashSet<IRI>();
        Iterator<Triple> it = graph.filter(null, new UriRef(Vocabulary.MAPS_TO_GRAPH), null);
        while (it.hasNext()) {
            NonLiteral subj = it.next().getSubject();
            if (subj instanceof UriRef) result.add(IRI.create(((UriRef) subj).getUnicodeString()));
        }
        return result;
    }

    public void removeMapping(IRI ontologyReference) {
        Iterator<Triple> it = graph.filter(new UriRef(ontologyReference.toString()), new UriRef(
                Vocabulary.MAPS_TO_GRAPH), null);
        // I expect a concurrent modification exception here, but we'll deal with it later.
        while (it.hasNext())
            graph.remove(it.next());
    }

    public void setMapping(IRI ontologyReference, UriRef graphName) {
        removeMapping(ontologyReference);
        addMapping(ontologyReference, graphName);
    }

    public Set<String> stringValues() {
        Set<String> result = new HashSet<String>();
        Iterator<Triple> it = graph.filter(null, new UriRef(Vocabulary.MAPS_TO_GRAPH), null);
        while (it.hasNext()) {
            Resource obj = it.next().getObject();
            if (obj instanceof UriRef) result.add(((UriRef) obj).getUnicodeString());
        }
        return result;
    }
}

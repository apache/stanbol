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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A set of utilities for handling OWL2 ontologies in Clerezza.
 * 
 * @author alexdma
 * 
 */
public class ClerezzaOWLUtils {

    private static Logger log = LoggerFactory.getLogger(ClerezzaOWLUtils.class);

    public static MGraph createOntology(String id, TcManager tcm) {
        UriRef name = new UriRef(id);
        MGraph ont = tcm.createMGraph(name);
        ont.add(new TripleImpl(name, RDF.type, OWL.Ontology));
        return ont;
    }

    public static MGraph createOntology(String id) {
        return createOntology(id, TcManager.getInstance());
    }

    public static OWLOntologyID guessOntologyID(InputStream content, Parser parser, String format) throws IOException {
        int limit = 100 * 1024; // 100kB lookahead size
        BufferedInputStream bIn = new BufferedInputStream(content);
        bIn.mark(limit); // set an appropriate limit
        OntologyLookaheadMGraph graph = new OntologyLookaheadMGraph();
        try {
            parser.parse(graph, bIn, format);
        } catch (RuntimeException e) {}
        if (graph.getOntologyIRI() != null) {
            // bIn.reset(); // reset set the stream to the start
            return new OWLOntologyID(IRI.create(graph.getOntologyIRI().getUnicodeString()));
        } else { // No OntologyID found
            // do some error handling
            log.warn("No ontologyID found after {} bytes, ontology has a chance of being anonymous.", limit);
            return null;
        }
    }

}

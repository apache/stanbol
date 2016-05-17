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
package org.apache.stanbol.commons.owl.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.stanbol.commons.owl.OntologyLookaheadGraph;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A set of utility methods for the manipulation of OWL API objects.
 */
public final class OWLUtils {

    /**
     * Restrict instantiation
     */
    private OWLUtils() {}

    private static int _LOOKAHEAD_LIMIT_DEFAULT = 1024;

    private static Logger log = LoggerFactory.getLogger(OWLUtils.class);

    public static final String NS_STANBOL = "http://stanbol.apache.org/";

    /**
     * If the ontology is named, this method will return its logical ID, otherwise it will return the location
     * it was retrieved from (which is still unique).
     * 
     * @param o
     * @return
     */
    public static OWLOntologyID extractOntologyID(OWLOntology o) {
        String oiri;
        IRI viri = null;
        // For named OWL ontologies it is their ontology ID.
        // For anonymous ontologies, it is the URI they were fetched from, if any.
        if (o.isAnonymous()) oiri = o.getOWLOntologyManager().getOntologyDocumentIRI(o).toString();
        else {
            OWLOntologyID id = o.getOntologyID();
            oiri = id.getOntologyIRI().toString();
            viri = id.getVersionIRI();
        }
        // Strip fragment or query tokens. TODO do proper URL Encoding.
        while (oiri.endsWith("#") || oiri.endsWith("?"))
            oiri = oiri.substring(0, oiri.length() - 1);
        if (viri != null) return new OWLOntologyID(IRI.create(oiri), viri);
        else return new OWLOntologyID(IRI.create(oiri));
    }

    /**
     * Returns the logical identifier of the supplied RDF graph, which is interpreted as an OWL ontology.
     * 
     * @param graph
     *            the RDF graph
     * @return the OWL ontology ID of the supplied graph, or null if it denotes an anonymous ontology.
     */
    public static OWLOntologyID extractOntologyID(Graph graph) {
        IRI ontologyIri = null, versionIri = null;
        Iterator<Triple> it = graph.filter(null, RDF.type, OWL.Ontology);
        if (it.hasNext()) {
            BlankNodeOrIRI subj = it.next().getSubject();
            if (it.hasNext()) {
                log.warn("Multiple OWL ontology definitions found.");
                log.warn("Ignoring all but {}", subj);
            }
            if (subj instanceof org.apache.clerezza.commons.rdf.IRI) {
                ontologyIri = IRI.create(((org.apache.clerezza.commons.rdf.IRI) subj).getUnicodeString());
                Iterator<Triple> it2 = graph.filter(subj, new org.apache.clerezza.commons.rdf.IRI(OWL2Constants.OWL_VERSION_IRI),
                    null);
                if (it2.hasNext()) versionIri = IRI.create(((org.apache.clerezza.commons.rdf.IRI) it2.next().getObject())
                        .getUnicodeString());
            }
        }
        if (ontologyIri == null) {
            // Note that OWL 2 does not allow ontologies with a version IRI and no ontology IRI.
            log.debug("Ontology is anonymous. Returning null ID.");
            return null;
        }
        if (versionIri == null) return new OWLOntologyID(ontologyIri);
        else return new OWLOntologyID(ontologyIri, versionIri);
    }

    /**
     * Performs lookahead with a 100 kB limit.
     * 
     * @param content
     * @param parser
     * @param format
     * @return
     * @throws IOException
     */
    public static OWLOntologyID guessOntologyID(InputStream content, Parser parser, String format) throws IOException {
        return guessOntologyID(content, parser, format, _LOOKAHEAD_LIMIT_DEFAULT);
    }

    public static OWLOntologyID guessOntologyID(InputStream content, Parser parser, String format, int limit) throws IOException {
        return guessOntologyID(content, parser, format, limit, Math.max(10, limit / 10));
    }

    public static OWLOntologyID guessOntologyID(InputStream content,
                                                Parser parser,
                                                String format,
                                                int limit,
                                                int versionIriOffset) throws IOException {
        long before = System.currentTimeMillis();
        log.info("Guessing ontology ID. Read limit = {} triples; offset = {} triples.", limit,
            versionIriOffset);
        BufferedInputStream bIn = new BufferedInputStream(content);
        bIn.mark(limit * 512); // set an appropriate limit
        OntologyLookaheadGraph graph = new OntologyLookaheadGraph(limit, versionIriOffset);
        try {
            parser.parse(graph, bIn, format);
        } catch (RuntimeException e) {
            log.error("Parsing failed for format {}. Returning null.", format);
        }
        OWLOntologyID result;

        if (graph.getOntologyIRI() == null) { // No Ontology ID found
            log.warn(" *** No ontology ID found, ontology has a chance of being anonymous.");
            result = new OWLOntologyID();
        } else {
            // bIn.reset(); // reset set the stream to the start
            IRI oiri = IRI.create(graph.getOntologyIRI().getUnicodeString());
            result = graph.getVersionIRI() == null ? new OWLOntologyID(oiri) : new OWLOntologyID(oiri,
                    IRI.create(graph.getVersionIRI().getUnicodeString()));
            log.info(" *** Guessed ID : {}", result);
        }
        log.info(" ... Triples scanned : {}; filtered in : {}", graph.getScannedTripleCount(), graph.size());
        log.info(" ... Time : {} ms", System.currentTimeMillis() - before);
        return result;
    }

}

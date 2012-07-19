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

import java.util.Iterator;

import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A set of utility methods for the manipulation of OWL API objects.
 */
public class OWLUtils {

    private static Logger log = LoggerFactory.getLogger(OWLUtils.class);

    public static final String NS_STANBOL = "http://stanbol.apache.org/";

    /**
     * If the ontology is named, this method will return its logical ID, otherwise it will return the location
     * it was retrieved from (which is still unique).
     * 
     * @param o
     * @return
     */
    public static OWLOntologyID guessOntologyIdentifier(OWLOntology o) {
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
    public static OWLOntologyID guessOntologyIdentifier(TripleCollection graph) {
        IRI ontologyIri = null, versionIri = null;
        Iterator<Triple> it = graph.filter(null, RDF.type, OWL.Ontology);
        if (it.hasNext()) {
            NonLiteral subj = it.next().getSubject();
            if (it.hasNext()) {
                log.warn("Multiple OWL ontology definitions found.");
                log.warn("Ignoring all but {}", subj);
            }
            if (subj instanceof UriRef) {
                ontologyIri = IRI.create(((UriRef) subj).getUnicodeString());
                Iterator<Triple> it2 = graph.filter((UriRef) subj, new UriRef(OWL2Constants.OWL_VERSION_IRI),
                    null);
                if (it2.hasNext()) versionIri = IRI.create(((UriRef) it2.next().getObject())
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

}

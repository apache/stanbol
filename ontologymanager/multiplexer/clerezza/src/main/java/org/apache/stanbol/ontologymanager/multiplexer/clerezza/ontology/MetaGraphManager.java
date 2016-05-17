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
package org.apache.stanbol.ontologymanager.multiplexer.clerezza.ontology;

import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.ENTRY_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.HAS_ONTOLOGY_IRI_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.HAS_VERSION_IRI_URIREF;

import java.util.Iterator;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.stanbol.ontologymanager.servicesapi.util.OntologyUtils;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Note that this class is not public.
 * 
 * @author alexdma
 * 
 */
class MetaGraphManager {

    private Graph graph;

    private Logger log = LoggerFactory.getLogger(getClass());

    private TcManager tcManager;

    public MetaGraphManager(TcManager tcManager, Graph graph) {
        this.tcManager = tcManager;
        this.graph = graph;
    }

    protected IRI buildResource(final OWLOntologyID publicKey) {
        if (publicKey == null) throw new IllegalArgumentException(
                "Cannot build a IRI resource on a null public key!");
        // The IRI is of the form ontologyIRI[:::versionIRI] (TODO use something less conventional?)
        // XXX should versionIRI also include the version IRI set by owners? Currently not

        // Remember not to sanitize logical identifiers.
        org.semanticweb.owlapi.model.IRI ontologyIri = publicKey.getOntologyIRI(), versionIri = publicKey.getVersionIRI();
        if (ontologyIri == null) throw new IllegalArgumentException(
                "Cannot build a IRI resource on an anonymous public key!");
        log.debug("Searching for a meta graph entry for public key:");
        log.debug(" -- {}", publicKey);
        IRI match = null;
        LiteralFactory lf = LiteralFactory.getInstance();
        Literal oiri = lf.createTypedLiteral(new IRI(ontologyIri.toString()));
        Literal viri = versionIri == null ? null : lf.createTypedLiteral(new IRI(versionIri
                .toString()));
        for (Iterator<Triple> it = graph.filter(null, HAS_ONTOLOGY_IRI_URIREF, oiri); it.hasNext();) {
            RDFTerm subj = it.next().getSubject();
            log.debug(" -- Ontology IRI match found. Scanning");
            log.debug(" -- RDFTerm : {}", subj);
            if (!(subj instanceof IRI)) {
                log.debug(" ---- (uncomparable: skipping...)");
                continue;
            }
            if (viri != null) {
                // Must find matching versionIRI
                if (graph.contains(new TripleImpl((IRI) subj, HAS_VERSION_IRI_URIREF, viri))) {
                    log.debug(" ---- Version IRI match!");
                    match = (IRI) subj;
                    break; // Found
                } else {
                    log.debug(" ---- Expected version IRI match not found.");
                    continue; // There could be another with the right versionIRI.
                }

            } else {
                // Must find unversioned resource
                if (graph.filter((IRI) subj, HAS_VERSION_IRI_URIREF, null).hasNext()) {
                    log.debug(" ---- Unexpected version IRI found. Skipping.");
                    continue;
                } else {
                    log.debug(" ---- Unversioned match!");
                    match = (IRI) subj;
                    break; // Found
                }
            }
        }
        log.debug("Matching IRI in graph : {}", match);
        if (match == null) return new IRI(OntologyUtils.encode(publicKey));
        else return match;

    }

    public boolean exists(final OWLOntologyID publicKey) {
        IRI publicKeyIRI = new IRI(OntologyUtils.encode(publicKey));
        if (graph.filter(publicKeyIRI, RDF.type, ENTRY_URIREF).hasNext()) return true;
        if (graph.filter(publicKeyIRI, OWL.sameAs, null).hasNext()) return true;
        return false;
    }

    public void updateAddAlias(OWLOntologyID subject, OWLOntologyID object) {
        // For now add both owl:sameAs statements
        IRI suben = buildResource(subject), oben = buildResource(object);
        synchronized (graph) {
            graph.add(new TripleImpl(suben, OWL.sameAs, oben));
            graph.add(new TripleImpl(oben, OWL.sameAs, suben));
        }
        // XXX add the Entry typing later on, but if so give up on using owl:sameAs.
    }

    public void updateCreateEntry(OWLOntologyID publicKey) {
        if (publicKey == null || publicKey.isAnonymous()) throw new IllegalArgumentException(
                "An anonymous ontology cannot be mapped. A non-anonymous ontology ID must be forged in these cases.");
        Triple tType, tHasOiri = null, tHasViri = null;
        org.semanticweb.owlapi.model.IRI ontologyIRI = publicKey.getOntologyIRI(), versionIri = publicKey.getVersionIRI();
        IRI entry = buildResource(publicKey);
        tType = new TripleImpl(entry, RDF.type, ENTRY_URIREF);
        LiteralFactory lf = LiteralFactory.getInstance();
        tHasOiri = new TripleImpl(entry, HAS_ONTOLOGY_IRI_URIREF, lf.createTypedLiteral(new IRI(
                ontologyIRI.toString())));
        if (versionIri != null) tHasViri = new TripleImpl(entry, HAS_VERSION_IRI_URIREF,
                lf.createTypedLiteral(new IRI(versionIri.toString())));
        synchronized (graph) {
            graph.add(tType);
            if (tHasViri != null) graph.add(tHasViri);
            graph.add(tHasOiri);
        }
    }

}

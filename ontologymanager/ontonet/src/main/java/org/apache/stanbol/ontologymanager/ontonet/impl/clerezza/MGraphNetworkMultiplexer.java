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

import static org.apache.stanbol.ontologymanager.ontonet.api.Vocabulary.APPENDED_TO_URIREF;
import static org.apache.stanbol.ontologymanager.ontonet.api.Vocabulary.ENTRY_URIREF;
import static org.apache.stanbol.ontologymanager.ontonet.api.Vocabulary.HAS_APPENDED_URIREF;
import static org.apache.stanbol.ontologymanager.ontonet.api.Vocabulary.HAS_ONTOLOGY_IRI_URIREF;
import static org.apache.stanbol.ontologymanager.ontonet.api.Vocabulary.HAS_VERSION_IRI_URIREF;
import static org.apache.stanbol.ontologymanager.ontonet.api.Vocabulary.IS_MANAGED_BY_URIREF;
import static org.apache.stanbol.ontologymanager.ontonet.api.Vocabulary.MANAGES_URIREF;
import static org.apache.stanbol.ontologymanager.ontonet.api.Vocabulary.SESSION_URIREF;
import static org.apache.stanbol.ontologymanager.ontonet.api.Vocabulary.SIZE_IN_TRIPLES_URIREF;
import static org.apache.stanbol.ontologymanager.ontonet.api.Vocabulary._NS_STANBOL_INTERNAL;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TypedLiteral;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.OntologyCollector;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyNetworkMultiplexer;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.session.Session;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionEvent;
import org.apache.stanbol.ontologymanager.ontonet.impl.util.OntologyUtils;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO make this object update its sizes as a graph changes.
 * 
 * @author alexdma
 * 
 */
public class MGraphNetworkMultiplexer implements OntologyNetworkMultiplexer {

    private class InvalidMetaGraphStateException extends RuntimeException {

        /**
         * 
         */
        private static final long serialVersionUID = 3915817349833358738L;

        @SuppressWarnings("unused")
        InvalidMetaGraphStateException() {
            super();
        }

        InvalidMetaGraphStateException(String message) {
            super(message);
        }

    }

    private Logger log = LoggerFactory.getLogger(getClass());

    private MGraph meta;

    public MGraphNetworkMultiplexer(MGraph metaGraph) {
        this.meta = metaGraph;
    }

    /**
     * Creates an {@link OWLOntologyID} object by combining the ontologyIRI and the versionIRI, where
     * applicable, of the stored graph.
     * 
     * @param resource
     *            the ontology
     * @return
     */
    protected OWLOntologyID buildPublicKey(final UriRef resource) {
        // TODO desanitize?
        IRI oiri = null, viri = null;
        Iterator<Triple> it = meta.filter(resource, HAS_ONTOLOGY_IRI_URIREF, null);
        if (it.hasNext()) {
            Resource obj = it.next().getObject();
            if (obj instanceof UriRef) oiri = IRI.create(((UriRef) obj).getUnicodeString());
            else if (obj instanceof Literal) oiri = IRI.create(((Literal) obj).getLexicalForm());
        } else {
            // Anonymous ontology? Decode the resource itself (which is not null)
            return OntologyUtils.decode(resource.getUnicodeString());
        }
        it = meta.filter(resource, HAS_VERSION_IRI_URIREF, null);
        if (it.hasNext()) {
            Resource obj = it.next().getObject();
            if (obj instanceof UriRef) viri = IRI.create(((UriRef) obj).getUnicodeString());
            else if (obj instanceof Literal) viri = IRI.create(((Literal) obj).getLexicalForm());
        }
        if (viri == null) return new OWLOntologyID(oiri);
        else return new OWLOntologyID(oiri, viri);
    }

    /**
     * Creates an {@link UriRef} out of an {@link OWLOntologyID}, so it can be used as an identifier. This
     * does NOT necessarily correspond to the UriRef that identifies the stored graph. In order to obtain
     * that, check the objects of any MAPS_TO_GRAPH assertions.
     * 
     * @param publicKey
     * @return
     */
    protected UriRef buildResource(final OWLOntologyID publicKey) {
        if (publicKey == null) throw new IllegalArgumentException(
                "Cannot build a UriRef resource on a null public key!");
        // The UriRef is of the form ontologyIRI[:::versionIRI] (TODO use something less conventional?)
        // XXX should versionIRI also include the version IRI set by owners? Currently not

        // Remember not to sanitize logical identifiers.
        IRI ontologyIri = publicKey.getOntologyIRI(), versionIri = publicKey.getVersionIRI();
        if (ontologyIri == null) throw new IllegalArgumentException(
                "Cannot build a UriRef resource on an anonymous public key!");
        log.debug("Searching for a meta graph entry for public key:");
        log.debug(" -- {}", publicKey);
        UriRef match = null;
        LiteralFactory lf = LiteralFactory.getInstance();
        TypedLiteral oiri = lf.createTypedLiteral(new UriRef(ontologyIri.toString()));
        TypedLiteral viri = versionIri == null ? null : lf.createTypedLiteral(new UriRef(versionIri
                .toString()));
        for (Iterator<Triple> it = meta.filter(null, HAS_ONTOLOGY_IRI_URIREF, oiri); it.hasNext();) {
            Resource subj = it.next().getSubject();
            log.debug(" -- Ontology IRI match found. Scanning");
            log.debug(" -- Resource : {}", subj);
            if (!(subj instanceof UriRef)) {
                log.debug(" ---- (uncomparable: skipping...)");
                continue;
            }
            if (viri != null) {
                // Must find matching versionIRI
                if (meta.contains(new TripleImpl((UriRef) subj, HAS_VERSION_IRI_URIREF, viri))) {
                    log.debug(" ---- Version IRI match!");
                    match = (UriRef) subj;
                    break; // Found
                } else {
                    log.debug(" ---- Expected version IRI match not found.");
                    continue; // There could be another with the right versionIRI.
                }

            } else {
                // Must find unversioned resource
                if (meta.filter((UriRef) subj, HAS_VERSION_IRI_URIREF, null).hasNext()) {
                    log.debug(" ---- Unexpected version IRI found. Skipping.");
                    continue;
                } else {
                    log.debug(" ---- Unversioned match!");
                    match = (UriRef) subj;
                    break; // Found
                }
            }
        }
        log.debug("Matching UriRef in graph : {}", match);
        if (match == null) return new UriRef(OntologyUtils.encode(publicKey));
        else return match;

    }

    private UriRef getIRIforScope(String scopeId) {
        // Use the Stanbol-internal namespace, so that the whole configuration can be ported.
        return new UriRef(_NS_STANBOL_INTERNAL + OntologyScope.shortName + "/" + scopeId);
    }

    private UriRef getIRIforSession(Session session) {
        // Use the Stanbol-internal namespace, so that the whole configuration can be ported.
        return new UriRef(_NS_STANBOL_INTERNAL + Session.shortName + "/" + session.getID());
    }

    @Override
    public OWLOntologyID getPublicKey(String stringForm) {
        if (stringForm == null || stringForm.trim().isEmpty()) throw new IllegalArgumentException(
                "String form must not be null or empty.");
        return buildPublicKey(new UriRef(stringForm));
    }

    @Override
    public Set<OWLOntologyID> getPublicKeys() {
        Set<OWLOntologyID> result = new HashSet<OWLOntologyID>();
        Iterator<Triple> it = meta.filter(null, RDF.type, ENTRY_URIREF);
        while (it.hasNext()) {
            Resource obj = it.next().getSubject();
            if (obj instanceof UriRef) result.add(buildPublicKey((UriRef) obj));
        }
        return result;
    }

    @Override
    public int getSize(OWLOntologyID publicKey) {
        UriRef subj = buildResource(publicKey);
        Iterator<Triple> it = meta.filter(subj, SIZE_IN_TRIPLES_URIREF, null);
        if (it.hasNext()) {
            Resource obj = it.next().getObject();
            if (obj instanceof Literal) {
                String s = ((Literal) obj).getLexicalForm();
                try {
                    return Integer.parseInt(s);
                } catch (Exception ex) {
                    log.warn("Not a valid integer value {} for size of {}", s, publicKey);
                    return -1;
                }
            }
        }
        return 0;
    }

    @Override
    public void onOntologyAdded(OntologyCollector collector, OWLOntologyID addedOntology) {
        // When the ontology provider hears an ontology has been added to a collector, it has to register this
        // into the metadata graph.

        // log.info("Heard addition of ontology {} to collector {}", addedOntology, collector.getID());
        // log.info("This ontology is stored as {}", getKey(addedOntology));

        String colltype = "";
        if (collector instanceof OntologyScope) colltype = OntologyScope.shortName + "/"; // Cannot be
        else if (collector instanceof OntologySpace) colltype = OntologySpace.shortName + "/";
        else if (collector instanceof Session) colltype = Session.shortName + "/";
        UriRef c = new UriRef(_NS_STANBOL_INTERNAL + colltype + collector.getID());
        UriRef u =
        // new UriRef(prefix + "::" + keymap.buildResource(addedOntology).getUnicodeString());
        // keymap.getMapping(addedOntology);
        buildResource(addedOntology);

        // TODO OntologyProvider should not be aware of scopes, spaces or sessions. Move elsewhere.
        boolean hasValues = false;
        log.debug("Ontology {}", addedOntology);
        log.debug("-- is already managed by the following collectors :");
        for (Iterator<Triple> it = meta.filter(u, IS_MANAGED_BY_URIREF, null); it.hasNext();) {
            hasValues = true;
            log.debug("-- {}", it.next().getObject());
        }
        for (Iterator<Triple> it = meta.filter(null, MANAGES_URIREF, u); it.hasNext();) {
            hasValues = true;
            log.debug("-- {} (inverse)", it.next().getSubject());
        }
        if (!hasValues) log.debug("-- <none>");

        // Add both inverse triples. This graph has to be traversed efficiently, no need for reasoners.
        UriRef predicate1 = null, predicate2 = null;
        if (collector instanceof OntologySpace) {
            predicate1 = MANAGES_URIREF;
            predicate2 = IS_MANAGED_BY_URIREF;
        } else if (collector instanceof Session) {
            // TODO implement model for sessions.
            predicate1 = MANAGES_URIREF;
            predicate2 = IS_MANAGED_BY_URIREF;
        } else {
            log.error("Unrecognized ontology collector type {} for \"{}\". Aborting.", collector.getClass(),
                collector.getID());
            return;
        }
        if (u != null) synchronized (meta) {
            Triple t;
            if (predicate1 != null) {
                t = new TripleImpl(c, predicate1, u);
                boolean b = meta.add(t);
                log.debug((b ? "Successful" : "Redundant") + " addition of meta triple");
                log.debug("-- {} ", t);
            }
            if (predicate2 != null) {
                t = new TripleImpl(u, predicate2, c);
                boolean b = meta.add(t);
                log.debug((b ? "Successful" : "Redundant") + " addition of meta triple");
                log.debug("-- {} ", t);
            }
        }
    }

    @Override
    public void onOntologyRemoved(OntologyCollector collector, OWLOntologyID removedOntology) {
        log.info("Heard removal of ontology {} from collector {}", removedOntology, collector.getID());

        String colltype = "";
        if (collector instanceof OntologyScope) colltype = OntologyScope.shortName + "/"; // Cannot be
        else if (collector instanceof OntologySpace) colltype = OntologySpace.shortName + "/";
        else if (collector instanceof Session) colltype = Session.shortName + "/";
        UriRef c = new UriRef(_NS_STANBOL_INTERNAL + colltype + collector.getID());
        UriRef u =
        // new UriRef(prefix + "::" + keymap.buildResource(removedOntology).getUnicodeString());
        // keymap.getMapping(removedOntology);
        buildResource(removedOntology);
        // XXX condense the following code
        boolean badState = true;

        log.debug("Checking ({},{}) pattern", c, u);
        for (Iterator<Triple> it = meta.filter(c, null, u); it.hasNext();) {
            UriRef property = it.next().getPredicate();
            if (collector instanceof OntologySpace || collector instanceof Session) {
                if (property.equals(MANAGES_URIREF)) badState = false;
            }
        }

        log.debug("Checking ({},{}) pattern", u, c);
        for (Iterator<Triple> it = meta.filter(u, null, c); it.hasNext();) {
            UriRef property = it.next().getPredicate();
            if (collector instanceof OntologySpace || collector instanceof Session) {
                if (property.equals(IS_MANAGED_BY_URIREF)) badState = false;
            }
        }

        if (badState) throw new InvalidMetaGraphStateException(
                "No relationship found for ontology-collector pair {" + u + " , " + c + "}");

        synchronized (meta) {
            if (collector instanceof OntologySpace) {
                meta.remove(new TripleImpl(c, MANAGES_URIREF, u));
                meta.remove(new TripleImpl(u, IS_MANAGED_BY_URIREF, c));
            }
        }
    }

    @Override
    public void scopeAppended(Session session, String scopeId) {
        final UriRef sessionur = getIRIforSession(session), scopeur = getIRIforScope(scopeId);
        if (sessionur == null || scopeur == null) throw new IllegalArgumentException(
                "UriRefs for scope and session cannot be null.");
        if (meta instanceof MGraph) synchronized (meta) {
            meta.add(new TripleImpl(sessionur, HAS_APPENDED_URIREF, scopeur));
            meta.add(new TripleImpl(scopeur, APPENDED_TO_URIREF, sessionur));
        }
    }

    @Override
    public void scopeDetached(Session session, String scopeId) {
        final UriRef sessionur = getIRIforSession(session), scopeur = getIRIforScope(scopeId);
        if (sessionur == null || scopeur == null) throw new IllegalArgumentException(
                "UriRefs for scope and session cannot be null.");
        if (meta instanceof MGraph) synchronized (meta) {
            // TripleImpl implements equals() and hashCode() ...
            meta.remove(new TripleImpl(sessionur, HAS_APPENDED_URIREF, scopeur));
            meta.remove(new TripleImpl(scopeur, APPENDED_TO_URIREF, sessionur));
        }
    }

    @Override
    public void sessionChanged(SessionEvent event) {
        switch (event.getOperationType()) {
            case CREATE:
                updateSessionRegistration(event.getSession());
                break;
            case KILL:
                updateSessionUnregistration(event.getSession());
                break;
            default:
                break;
        }
    }

    private void updateSessionRegistration(Session session) {
        final UriRef sesur = getIRIforSession(session);
        // If this method was called after a session rebuild, the following will have little to no effect.
        synchronized (meta) {
            // The only essential triple to add is typing
            meta.add(new TripleImpl(sesur, RDF.type, SESSION_URIREF));
        }
        log.debug("Ontology collector information triples added for session \"{}\".", sesur);
    }

    private void updateSessionUnregistration(Session session) {
        long before = System.currentTimeMillis();
        boolean removable = false, conflict = false;
        final UriRef sessionur = getIRIforSession(session);
        Set<Triple> removeUs = new HashSet<Triple>();
        for (Iterator<Triple> it = meta.filter(sessionur, null, null); it.hasNext();) {
            Triple t = it.next();
            if (RDF.type.equals(t.getPredicate())) {
                if (SESSION_URIREF.equals(t.getObject())) removable = true;
                else conflict = true;
            }
            removeUs.add(t);
        }
        if (!removable) {
            log.error("Cannot write session deregistration to persistence:");
            log.error("-- resource {}", sessionur);
            log.error("-- is not typed as a {} in the meta-graph.", SESSION_URIREF);
        } else if (conflict) {
            log.error("Conflict upon session deregistration:");
            log.error("-- resource {}", sessionur);
            log.error("-- has incompatible types in the meta-graph.");
        } else {
            log.debug("Removing all triples for session \"{}\".", session.getID());
            Iterator<Triple> it;
            for (it = meta.filter(null, null, sessionur); it.hasNext();)
                removeUs.add(it.next());
            for (it = meta.filter(sessionur, null, null); it.hasNext();)
                removeUs.add(it.next());
            meta.removeAll(removeUs);
            log.debug("Done; removed {} triples in {} ms.", removeUs.size(), System.currentTimeMillis()
                                                                             - before);
        }
    }

    @Override
    public Set<OntologyCollector> getHandles(OWLOntologyID publicKey) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

}

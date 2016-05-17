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
package org.apache.stanbol.ontologymanager.multiplexer.clerezza.collector;

import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.APPENDED_TO_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.DEPENDS_ON_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.ENTRY_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.HAS_APPENDED_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.HAS_DEPENDENT_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.HAS_ONTOLOGY_IRI_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.HAS_SPACE_CORE_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.HAS_SPACE_CUSTOM_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.HAS_VERSION_IRI_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.IS_MANAGED_BY_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.IS_SPACE_CORE_OF_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.IS_SPACE_CUSTOM_OF_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.MANAGES_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.SCOPE_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.SESSION_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.SIZE_IN_TRIPLES_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.SPACE_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary._NS_STANBOL_INTERNAL;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.stanbol.ontologymanager.core.scope.ScopeManagerImpl;
import org.apache.stanbol.ontologymanager.multiplexer.clerezza.session.SessionManagerImpl;
import org.apache.stanbol.ontologymanager.servicesapi.collector.OntologyCollector;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.Multiplexer;
import org.apache.stanbol.ontologymanager.servicesapi.scope.OntologySpace;
import org.apache.stanbol.ontologymanager.servicesapi.scope.OntologySpace.SpaceType;
import org.apache.stanbol.ontologymanager.servicesapi.scope.Scope;
import org.apache.stanbol.ontologymanager.servicesapi.scope.ScopeManager;
import org.apache.stanbol.ontologymanager.servicesapi.session.Session;
import org.apache.stanbol.ontologymanager.servicesapi.session.SessionEvent;
import org.apache.stanbol.ontologymanager.servicesapi.session.SessionManager;
import org.apache.stanbol.ontologymanager.servicesapi.util.OntologyUtils;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO make this object update its sizes as a graph changes.
 * 
 * @author alexdma
 * 
 */
public class GraphMultiplexer implements Multiplexer {

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

    private Graph meta;

    public GraphMultiplexer(Graph metaGraph) {
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
    protected OWLOntologyID buildPublicKey(final IRI resource) {
        // TODO desanitize?
        org.semanticweb.owlapi.model.IRI oiri = null, viri = null;
        Iterator<Triple> it = meta.filter(resource, HAS_ONTOLOGY_IRI_URIREF, null);
        if (it.hasNext()) {
            RDFTerm obj = it.next().getObject();
            if (obj instanceof IRI) oiri = org.semanticweb.owlapi.model.IRI.create(((IRI) obj).getUnicodeString());
            else if (obj instanceof Literal) oiri = org.semanticweb.owlapi.model.IRI.create(((Literal) obj).getLexicalForm());
        } else {
            // Anonymous ontology? Decode the resource itself (which is not null)
            return OntologyUtils.decode(resource.getUnicodeString());
        }
        it = meta.filter(resource, HAS_VERSION_IRI_URIREF, null);
        if (it.hasNext()) {
            RDFTerm obj = it.next().getObject();
            if (obj instanceof IRI) viri = org.semanticweb.owlapi.model.IRI.create(((IRI) obj).getUnicodeString());
            else if (obj instanceof Literal) viri = org.semanticweb.owlapi.model.IRI.create(((Literal) obj).getLexicalForm());
        }
        if (viri == null) return new OWLOntologyID(oiri);
        else return new OWLOntologyID(oiri, viri);
    }

    /**
     * Creates an {@link IRI} out of an {@link OWLOntologyID}, so it can be used as an identifier. This
     * does NOT necessarily correspond to the IRI that identifies the stored graph. In order to obtain
     * that, check the objects of any MAPS_TO_GRAPH assertions.
     * 
     * @param publicKey
     * @return
     */
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
        for (Iterator<Triple> it = meta.filter(null, HAS_ONTOLOGY_IRI_URIREF, oiri); it.hasNext();) {
            RDFTerm subj = it.next().getSubject();
            log.debug(" -- Ontology IRI match found. Scanning");
            log.debug(" -- RDFTerm : {}", subj);
            if (!(subj instanceof IRI)) {
                log.debug(" ---- (uncomparable: skipping...)");
                continue;
            }
            if (viri != null) {
                // Must find matching versionIRI
                if (meta.contains(new TripleImpl((IRI) subj, HAS_VERSION_IRI_URIREF, viri))) {
                    log.debug(" ---- Version IRI match!");
                    match = (IRI) subj;
                    break; // Found
                } else {
                    log.debug(" ---- Expected version IRI match not found.");
                    continue; // There could be another with the right versionIRI.
                }

            } else {
                // Must find unversioned resource
                if (meta.filter((IRI) subj, HAS_VERSION_IRI_URIREF, null).hasNext()) {
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

    private void checkHandle(IRI candidate, Set<OntologyCollector> handles) {

        /*
         * We have to do it like this because we cannot make this class a Component and reference ONManager
         * and SessionManager, otherwise an activation cycle will occur.
         */
        ScopeManager scopeManager = ScopeManagerImpl.get(); // FIXME get rid of this.
        SessionManager sessionManager = SessionManagerImpl.get();
        String prefix_scope = _NS_STANBOL_INTERNAL + Scope.shortName + "/", prefix_session = _NS_STANBOL_INTERNAL
                                                                                             + Session.shortName
                                                                                             + "/";

        // TODO check when not explicitly typed.
        SpaceType spaceType;
        if (meta.contains(new TripleImpl(candidate, RDF.type, SPACE_URIREF))) {
            RDFTerm rScope;
            Iterator<Triple> parentSeeker = meta.filter(candidate, IS_SPACE_CORE_OF_URIREF, null);
            if (parentSeeker.hasNext()) {
                rScope = parentSeeker.next().getObject();
                spaceType = SpaceType.CORE;
            } else {
                parentSeeker = meta.filter(candidate, IS_SPACE_CUSTOM_OF_URIREF, null);
                if (parentSeeker.hasNext()) {
                    rScope = parentSeeker.next().getObject();
                    spaceType = SpaceType.CUSTOM;
                } else {
                    parentSeeker = meta.filter(null, HAS_SPACE_CORE_URIREF, candidate);
                    if (parentSeeker.hasNext()) {
                        rScope = parentSeeker.next().getSubject();
                        spaceType = SpaceType.CORE;
                    } else {
                        parentSeeker = meta.filter(null, HAS_SPACE_CUSTOM_URIREF, candidate);
                        if (parentSeeker.hasNext()) {
                            rScope = parentSeeker.next().getSubject();
                            spaceType = SpaceType.CUSTOM;
                        } else throw new InvalidMetaGraphStateException("Ontology space " + candidate
                                                                        + " does not declare a parent scope.");
                    }
                }
            }
            if (!(rScope instanceof IRI)) throw new InvalidMetaGraphStateException(
                    rScope + " is not a legal scope identifier.");
            String scopeId = ((IRI) rScope).getUnicodeString().substring(prefix_scope.length());
            Scope scope = scopeManager.getScope(scopeId);
            switch (spaceType) {
                case CORE:
                    handles.add(scope.getCoreSpace());
                    break;
                case CUSTOM:
                    handles.add(scope.getCustomSpace());
                    break;
            }
        } else if (meta.contains(new TripleImpl(candidate, RDF.type, SESSION_URIREF))) {
            String sessionId = candidate.getUnicodeString().substring(prefix_session.length());
            handles.add(sessionManager.getSession(sessionId));
        }
    }

    @Override
    public void clearDependencies(OWLOntologyID dependent) {
        if (dependent == null) throw new IllegalArgumentException("dependent cannot be null");
        log.debug("Clearing dependencies for {}", dependent);

        Set<Triple> dependencies = new HashSet<Triple>();
        synchronized (meta) {
            Set<OWLOntologyID> aliases = listAliases(dependent);
            aliases.add(dependent);
            for (OWLOntologyID depalias : aliases) {
                IRI dep = buildResource(depalias);
                Iterator<Triple> it = meta.filter(dep, DEPENDS_ON_URIREF, null);
                while (it.hasNext()) {
                    Triple t = it.next();
                    dependencies.add(t);
                    log.debug(" ... Set {} as a dependency to remove.", t.getObject());
                }
                it = meta.filter(null, HAS_DEPENDENT_URIREF, dep);
                while (it.hasNext()) {
                    Triple t = it.next();
                    dependencies.add(t);
                    log.debug(" ... Set {} as a dependency to remove.", t.getSubject());
                }
            }
            meta.removeAll(dependencies);
        }
        log.debug(" ... DONE clearing dependencies.");
    }

    @Override
    public Set<OWLOntologyID> getDependencies(OWLOntologyID dependent) {
        Set<OWLOntologyID> dependencies = new HashSet<OWLOntologyID>();
        log.debug("Getting dependencies for {}", dependent);
        synchronized (meta) {
            Set<OWLOntologyID> aliases = listAliases(dependent);
            aliases.add(dependent);
            for (OWLOntologyID depalias : aliases) {
                IRI dep = buildResource(depalias);
                Iterator<Triple> it = meta.filter(dep, DEPENDS_ON_URIREF, null);
                while (it.hasNext()) {
                    RDFTerm obj = it.next().getObject();
                    log.debug(" ... found {} (inverse).", obj);
                    if (obj instanceof IRI) dependencies.add(buildPublicKey((IRI) obj));
                    else log.warn(" ... Unexpected literal value!");
                }
                it = meta.filter(null, HAS_DEPENDENT_URIREF, dep);
                while (it.hasNext()) {
                    RDFTerm sub = it.next().getSubject();
                    log.debug(" ... found {} (inverse).", sub);
                    if (sub instanceof IRI) dependencies.add(buildPublicKey((IRI) sub));
                    else log.warn(" ... Unexpected literal value!");
                }
            }
        }
        return dependencies;
    }

    @Override
    public Set<OWLOntologyID> getDependents(OWLOntologyID dependency) {
        Set<OWLOntologyID> dependents = new HashSet<OWLOntologyID>();
        IRI dep = buildResource(dependency);
        log.debug("Getting depents for {}", dependency);
        synchronized (meta) {
            Iterator<Triple> it = meta.filter(null, DEPENDS_ON_URIREF, dep);
            while (it.hasNext()) {
                RDFTerm sub = it.next().getSubject();
                log.debug(" ... found {} (inverse).", sub);
                if (sub instanceof IRI) dependents.add(buildPublicKey((IRI) sub));
                else log.warn(" ... Unexpected literal value!");
            }
            it = meta.filter(dep, HAS_DEPENDENT_URIREF, null);
            while (it.hasNext()) {
                RDFTerm obj = it.next().getObject();
                log.debug(" ... found {} (inverse).", obj);
                if (obj instanceof IRI) dependents.add(buildPublicKey((IRI) obj));
                else log.warn(" ... Unexpected literal value!");
            }
        }
        return dependents;
    }

    @Override
    public Set<OntologyCollector> getHandles(OWLOntologyID publicKey) {
        Set<OntologyCollector> handles = new HashSet<OntologyCollector>();
        Set<OWLOntologyID> aliases = listAliases(publicKey);
        aliases.add(publicKey);
        for (OWLOntologyID alias : aliases) {
            IRI ontologyId = buildResource(alias);

            for (Iterator<Triple> it = meta.filter(null, MANAGES_URIREF, ontologyId); it.hasNext();) {
                BlankNodeOrIRI sub = it.next().getSubject();
                if (sub instanceof IRI) checkHandle((IRI) sub, handles);
                else throw new InvalidMetaGraphStateException(
                        sub + " is not a valid ontology collector identifer.");
            }

            for (Iterator<Triple> it = meta.filter(ontologyId, IS_MANAGED_BY_URIREF, null); it.hasNext();) {
                RDFTerm obj = it.next().getObject();
                if (obj instanceof IRI) checkHandle((IRI) obj, handles);
                else throw new InvalidMetaGraphStateException(
                        obj + " is not a valid ontology collector identifer.");
            }
        }
        return handles;
        // throw new UnsupportedOperationException("Not implemented yet.");
    }

    private IRI getIRIforScope(String scopeId) {
        // Use the Stanbol-internal namespace, so that the whole configuration can be ported.
        return new IRI(_NS_STANBOL_INTERNAL + Scope.shortName + "/" + scopeId);
    }

    private IRI getIRIforSession(Session session) {
        // Use the Stanbol-internal namespace, so that the whole configuration can be ported.
        return new IRI(_NS_STANBOL_INTERNAL + Session.shortName + "/" + session.getID());
    }

    private IRI getIRIforSpace(OntologySpace space) {
        // Use the Stanbol-internal namespace, so that the whole configuration can be ported.
        return new IRI(_NS_STANBOL_INTERNAL + OntologySpace.shortName + "/" + space.getID());
    }

    @Override
    public OWLOntologyID getPublicKey(String stringForm) {
        if (stringForm == null || stringForm.trim().isEmpty()) throw new IllegalArgumentException(
                "String form must not be null or empty.");
        return buildPublicKey(new IRI(stringForm));
    }

    @Override
    public Set<OWLOntologyID> getPublicKeys() {
        Set<OWLOntologyID> result = new HashSet<OWLOntologyID>();
        Iterator<Triple> it = meta.filter(null, RDF.type, ENTRY_URIREF);
        while (it.hasNext()) {
            RDFTerm obj = it.next().getSubject();
            if (obj instanceof IRI) result.add(buildPublicKey((IRI) obj));
        }
        return result;
    }

    @Override
    public int getSize(OWLOntologyID publicKey) {
        IRI subj = buildResource(publicKey);
        Iterator<Triple> it = meta.filter(subj, SIZE_IN_TRIPLES_URIREF, null);
        if (it.hasNext()) {
            RDFTerm obj = it.next().getObject();
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

    /*
     * XXX see if we can use reasoners, either live or by caching materialisations.
     */
    protected Set<OWLOntologyID> listAliases(OWLOntologyID publicKey) {
        if (publicKey == null || publicKey.isAnonymous()) throw new IllegalArgumentException(
                "Cannot locate aliases for null or anonymous public keys.");
        Set<OWLOntologyID> aliases = new HashSet<OWLOntologyID>();
        IRI ont = buildResource(publicKey);
        // Forwards
        for (Iterator<Triple> it = meta.filter(ont, OWL.sameAs, null); it.hasNext();) {
            RDFTerm r = it.next().getObject();
            if (r instanceof IRI) aliases.add(buildPublicKey((IRI) r));
        }
        // Backwards
        for (Iterator<Triple> it = meta.filter(null, OWL.sameAs, ont); it.hasNext();) {
            RDFTerm r = it.next().getSubject();
            if (r instanceof IRI) aliases.add(buildPublicKey((IRI) r));
        }
        return aliases;
    }

    @Override
    public void onOntologyAdded(OntologyCollector collector, OWLOntologyID addedOntology) {
        // When the ontology provider hears an ontology has been added to a collector, it has to register this
        // into the metadata graph.

        // log.info("Heard addition of ontology {} to collector {}", addedOntology, collector.getID());
        // log.info("This ontology is stored as {}", getKey(addedOntology));

        String colltype = "";
        if (collector instanceof Scope) colltype = Scope.shortName + "/"; // Cannot be
        else if (collector instanceof OntologySpace) colltype = OntologySpace.shortName + "/";
        else if (collector instanceof Session) colltype = Session.shortName + "/";
        IRI c = new IRI(_NS_STANBOL_INTERNAL + colltype + collector.getID());
        IRI u =
        // new IRI(prefix + "::" + keymap.buildResource(addedOntology).getUnicodeString());
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
        IRI predicate1 = null, predicate2 = null;
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
        if (collector instanceof Scope) colltype = Scope.shortName + "/"; // Cannot be
        else if (collector instanceof OntologySpace) colltype = OntologySpace.shortName + "/";
        else if (collector instanceof Session) colltype = Session.shortName + "/";
        IRI c = new IRI(_NS_STANBOL_INTERNAL + colltype + collector.getID());
        Set<OWLOntologyID> aliases = listAliases(removedOntology);
        aliases.add(removedOntology);
        boolean badState = true;
        for (OWLOntologyID alias : aliases) {
            IRI u = buildResource(alias);
            // XXX condense the following code

            log.debug("Checking ({},{}) pattern", c, u);
            for (Iterator<Triple> it = meta.filter(c, null, u); it.hasNext();) {
                IRI property = it.next().getPredicate();
                if (collector instanceof OntologySpace || collector instanceof Session) {
                    if (property.equals(MANAGES_URIREF)) badState = false;
                }
            }

            log.debug("Checking ({},{}) pattern", u, c);
            for (Iterator<Triple> it = meta.filter(u, null, c); it.hasNext();) {
                IRI property = it.next().getPredicate();
                if (collector instanceof OntologySpace || collector instanceof Session) {
                    if (property.equals(IS_MANAGED_BY_URIREF)) badState = false;
                }
            }

            synchronized (meta) {
                if (collector instanceof OntologySpace || collector instanceof Session) {
                    meta.remove(new TripleImpl(c, MANAGES_URIREF, u));
                    meta.remove(new TripleImpl(u, IS_MANAGED_BY_URIREF, c));
                }
            }
        }
        if (badState) throw new InvalidMetaGraphStateException(
                "No relationship found between ontology collector " + c + " and stored ontology "
                        + removedOntology + " (or its aliases).");

    }

    @Override
    public void removeDependency(OWLOntologyID dependent, OWLOntologyID dependency) {
        if (dependent == null) throw new IllegalArgumentException("dependent cannot be null");
        if (dependency == null) throw new IllegalArgumentException("dependency cannot be null");
        log.debug("Removing dependency.");
        log.debug(" ... dependent : {}", dependent);
        log.debug(" ... dependency : {}", dependency);
        IRI depy = buildResource(dependency);
        synchronized (meta) {
            Set<OWLOntologyID> aliases = listAliases(dependent);
            aliases.add(dependent);
            for (OWLOntologyID depalias : aliases) {
                IRI dep = buildResource(depalias);
                Triple t = new TripleImpl(dep, DEPENDS_ON_URIREF, depy);
                boolean found = false;
                if (meta.contains(t)) {
                    found = true;
                    meta.remove(t);
                }
                t = new TripleImpl(depy, HAS_DEPENDENT_URIREF, dep);
                if (meta.contains(t)) {
                    found = true;
                    meta.remove(t);
                }
                if (!found) log.warn("No such dependency found.");
                else log.debug("DONE removing dependency.");
            }
        }
    }

    @Override
    public void scopeActivated(Scope scope) {}

    @Override
    public void scopeAppended(Session session, String scopeId) {
        final IRI sessionur = getIRIforSession(session), scopeur = getIRIforScope(scopeId);
        if (sessionur == null || scopeur == null) throw new IllegalArgumentException(
                "IRIs for scope and session cannot be null.");
        if (meta instanceof Graph) synchronized (meta) {
            meta.add(new TripleImpl(sessionur, HAS_APPENDED_URIREF, scopeur));
            meta.add(new TripleImpl(scopeur, APPENDED_TO_URIREF, sessionur));
        }
    }

    @Override
    public void scopeCreated(Scope scope) {}

    @Override
    public void scopeDeactivated(Scope scope) {}

    @Override
    public void scopeDetached(Session session, String scopeId) {
        final IRI sessionur = getIRIforSession(session), scopeur = getIRIforScope(scopeId);
        if (sessionur == null || scopeur == null) throw new IllegalArgumentException(
                "IRIs for scope and session cannot be null.");
        if (meta instanceof Graph) synchronized (meta) {
            // TripleImpl implements equals() and hashCode() ...
            meta.remove(new TripleImpl(sessionur, HAS_APPENDED_URIREF, scopeur));
            meta.remove(new TripleImpl(scopeur, APPENDED_TO_URIREF, sessionur));
        }
    }

    @Override
    public void scopeRegistered(Scope scope) {
        updateScopeRegistration(scope);
    }

    @Override
    public void scopeUnregistered(Scope scope) {
        updateScopeUnregistration(scope);
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

    @Override
    public void setDependency(OWLOntologyID dependent, OWLOntologyID dependency) {
        if (dependent == null) throw new IllegalArgumentException("dependent cannot be null");
        if (dependency == null) throw new IllegalArgumentException("dependency cannot be null");
        log.debug("Setting dependency.");
        log.debug(" ... dependent : {}", dependent);
        log.debug(" ... dependency : {}", dependency);
        IRI dep = buildResource(dependent), depy = buildResource(dependency);
        // TODO check for the actual resource!
        synchronized (meta) {
            meta.add(new TripleImpl(dep, DEPENDS_ON_URIREF, depy));
        }
        log.debug("DONE setting dependency.");
    }

    /**
     * Write registration info for a new ontology scope and its spaces.
     * 
     * @param scope
     *            the scope whose information needs to be updated.
     */
    private void updateScopeRegistration(Scope scope) {
        final IRI scopeur = getIRIforScope(scope.getID());
        final IRI coreur = getIRIforSpace(scope.getCoreSpace());
        final IRI custur = getIRIforSpace(scope.getCustomSpace());
        // If this method was called after a scope rebuild, the following will have little to no effect.
        synchronized (meta) {
            // Spaces are created along with the scope, so it is safe to add their triples.
            meta.add(new TripleImpl(scopeur, RDF.type, SCOPE_URIREF));
            meta.add(new TripleImpl(coreur, RDF.type, SPACE_URIREF));
            meta.add(new TripleImpl(custur, RDF.type, SPACE_URIREF));
            meta.add(new TripleImpl(scopeur, HAS_SPACE_CORE_URIREF, coreur));
            meta.add(new TripleImpl(scopeur, HAS_SPACE_CUSTOM_URIREF, custur));
            // Add inverse predicates so we can traverse the graph in both directions.
            meta.add(new TripleImpl(coreur, IS_SPACE_CORE_OF_URIREF, scopeur));
            meta.add(new TripleImpl(custur, IS_SPACE_CUSTOM_OF_URIREF, scopeur));
        }
        log.debug("Ontology collector information triples added for scope \"{}\".", scope);
    }

    /**
     * Remove all information on a deregistered ontology scope and its spaces.
     * 
     * @param scope
     *            the scope whose information needs to be updated.
     */
    private void updateScopeUnregistration(Scope scope) {
        long before = System.currentTimeMillis();
        boolean removable = false, conflict = false;
        final IRI scopeur = getIRIforScope(scope.getID());
        final IRI coreur = getIRIforSpace(scope.getCoreSpace());
        final IRI custur = getIRIforSpace(scope.getCustomSpace());
        Set<Triple> removeUs = new HashSet<Triple>();
        for (Iterator<Triple> it = meta.filter(scopeur, null, null); it.hasNext();) {
            Triple t = it.next();
            if (RDF.type.equals(t.getPredicate())) {
                if (SCOPE_URIREF.equals(t.getObject())) removable = true;
                else conflict = true;
            }
            removeUs.add(t);
        }
        if (!removable) {
            log.error("Cannot write scope deregistration to persistence:");
            log.error("-- resource {}", scopeur);
            log.error("-- is not typed as a {} in the meta-graph.", SCOPE_URIREF);
        } else if (conflict) {
            log.error("Conflict upon scope deregistration:");
            log.error("-- resource {}", scopeur);
            log.error("-- has incompatible types in the meta-graph.");
        } else {
            log.debug("Removing all triples for scope \"{}\".", scope.getID());
            Iterator<Triple> it;
            for (it = meta.filter(null, null, scopeur); it.hasNext();)
                removeUs.add(it.next());
            for (it = meta.filter(null, null, coreur); it.hasNext();)
                removeUs.add(it.next());
            for (it = meta.filter(coreur, null, null); it.hasNext();)
                removeUs.add(it.next());
            for (it = meta.filter(null, null, custur); it.hasNext();)
                removeUs.add(it.next());
            for (it = meta.filter(custur, null, null); it.hasNext();)
                removeUs.add(it.next());
            meta.removeAll(removeUs);
            log.debug("Done; removed {} triples in {} ms.", removeUs.size(), System.currentTimeMillis()
                                                                             - before);
        }
    }

    private void updateSessionRegistration(Session session) {
        final IRI sesur = getIRIforSession(session);
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
        final IRI sessionur = getIRIforSession(session);
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

}

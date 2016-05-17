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

import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.APPENDED_TO_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.ENTRY_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.HAS_APPENDED_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.HAS_ONTOLOGY_IRI_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.HAS_SPACE_CORE_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.HAS_SPACE_CUSTOM_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.HAS_VERSION_IRI_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.IS_MANAGED_BY_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.IS_SPACE_CORE_OF_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.IS_SPACE_CUSTOM_OF_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.MANAGES_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.MAPS_TO_GRAPH_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.RETRIEVED_FROM_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.SCOPE_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.SESSION_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary.SIZE_IN_TRIPLES_URIREF;
import static org.apache.stanbol.ontologymanager.servicesapi.Vocabulary._NS_STANBOL_INTERNAL;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.access.EntityAlreadyExistsException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.access.TcProvider;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.UnsupportedFormatException;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.commons.owl.OWLOntologyManagerFactory;
import org.apache.stanbol.commons.owl.PhonyIRIMapper;
import org.apache.stanbol.commons.owl.transformation.OWLAPIToClerezzaConverter;
import org.apache.stanbol.commons.owl.util.OWLUtils;
import org.apache.stanbol.commons.owl.util.URIUtils;
import org.apache.stanbol.commons.stanboltools.offline.OfflineMode;
import org.apache.stanbol.ontologymanager.multiplexer.clerezza.collector.GraphMultiplexer;
import org.apache.stanbol.ontologymanager.ontonet.api.OntologyNetworkConfiguration;
import org.apache.stanbol.ontologymanager.servicesapi.OfflineConfiguration;
import org.apache.stanbol.ontologymanager.servicesapi.collector.ImportManagementPolicy;
import org.apache.stanbol.ontologymanager.servicesapi.io.Origin;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.Multiplexer;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OntologyHandleException;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OrphanOntologyKeyException;
import org.apache.stanbol.ontologymanager.servicesapi.scope.Scope;
import org.apache.stanbol.ontologymanager.servicesapi.session.Session;
import org.apache.stanbol.ontologymanager.servicesapi.util.OntologyUtils;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clerezza-based ontology provider implementation. Whether it is persistent or in-memory depends on the
 * {@link TcProvider} used.<br>
 * <br>
 * NOTE: in this implementation, the <code>preferredFormat</code> argument of the
 * {@link #loadInStore(InputStream, String, boolean)} and {@link #loadInStore(IRI, String, boolean)} methods
 * is not the only one to be tried when parsing an ontology, but merely the first one: should it fail, all
 * other supported formats will be tried as a fallback.
 * 
 * @author alexdma
 * 
 */
@Component(immediate = true, metatype = true)
@Service({OntologyProvider.class,
          org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyProvider.class})
public class ClerezzaOntologyProvider implements
        org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyProvider<TcProvider> {

    /**
     * Internally, the Clerezza ontology provider uses a reserved graph to store the associations between
     * ontology IDs/physical IRIs and graph names. This graph is wrapped into an {@link OntologyToTcMapper}
     * object.
     * 
     * @author alexdma
     * 
     */
    private class OntologyToTcMapper {

        private Graph graph;

        OntologyToTcMapper() {
            if (store == null) throw new IllegalArgumentException("TcProvider cannot be null");
            IRI graphId = new IRI(metaGraphId);
            try {
                graph = store.createGraph(graphId);
            } catch (EntityAlreadyExistsException e) {
                graph = store.getGraph(graphId);
            }
        }

        void addMapping(OWLOntologyID ontologyReference, IRI graphName) {
            if (ontologyReference == null || ontologyReference.isAnonymous()) throw new IllegalArgumentException(
                    "An anonymous ontology cannot be mapped. A non-anonymous ontology ID must be forged in these cases.");
            Triple tType, tMaps, tHasOiri = null, tHasViri = null;
            org.semanticweb.owlapi.model.IRI ontologyIRI = ontologyReference.getOntologyIRI(), versionIri = ontologyReference
                    .getVersionIRI();
            IRI entry = buildResource(ontologyReference);
            tType = new TripleImpl(entry, RDF.type, ENTRY_URIREF);
            tMaps = new TripleImpl(entry, MAPS_TO_GRAPH_URIREF, graphName);
            LiteralFactory lf = LiteralFactory.getInstance();
            tHasOiri = new TripleImpl(entry, HAS_ONTOLOGY_IRI_URIREF, lf.createTypedLiteral(new IRI(
                    ontologyIRI.toString())));
            if (versionIri != null) tHasViri = new TripleImpl(entry, HAS_VERSION_IRI_URIREF,
                    lf.createTypedLiteral(new IRI(versionIri.toString())));
            synchronized (graph) {
                graph.add(tType);
                graph.add(tMaps);
                if (tHasViri != null) graph.add(tHasViri);
                graph.add(tHasOiri);
            }
        }

        OWLOntologyID buildPublicKey(final IRI resource) {
            // TODO desanitize?
            LiteralFactory lf = LiteralFactory.getInstance();
            org.semanticweb.owlapi.model.IRI oiri = null, viri = null;
            Iterator<Triple> it = graph.filter(resource, HAS_ONTOLOGY_IRI_URIREF, null);
            if (it.hasNext()) {
                IRI s = null;
                RDFTerm obj = it.next().getObject();
                if (obj instanceof IRI) s = ((IRI) obj);
                else if (obj instanceof Literal) s = lf.createObject(IRI.class, (Literal) obj);
                oiri = org.semanticweb.owlapi.model.IRI.create(s.getUnicodeString());
            } else {
                // Anonymous ontology? Decode the resource itself (which is not null)
                return OntologyUtils.decode(resource.getUnicodeString());
            }
            it = graph.filter(resource, HAS_VERSION_IRI_URIREF, null);
            if (it.hasNext()) {
                IRI s = null;
                RDFTerm obj = it.next().getObject();
                if (obj instanceof IRI) s = ((IRI) obj);
                else if (obj instanceof Literal) s = lf.createObject(IRI.class, (Literal) obj);
                viri = org.semanticweb.owlapi.model.IRI.create(s.getUnicodeString());
            }
            if (viri == null) return new OWLOntologyID(oiri);
            else return new OWLOntologyID(oiri, viri);
        }

        /**
         * Creates an {@link IRI} out of an {@link OWLOntologyID}, so it can be used as a storage key for
         * the graph.
         * 
         * @param ontologyReference
         * @return
         */
        IRI buildResource(OWLOntologyID publicKey) {
            /*
             * The IRI is of the form ontologyIRI[:::versionIRI] (TODO use something less conventional e.g.
             * the string form of OWLOntologyID objects?)
             */
            Graph meta = getMetaGraph(Graph.class);
            if (publicKey == null) throw new IllegalArgumentException(
                    "Cannot build a IRI resource on a null public key!");

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
            if (match == null) {
                return new IRI(OntologyUtils.encode(publicKey));
            } else {
                return match;
            }
        }

        IRI getMapping(OWLOntologyID reference) {
            Set<IRI> aliases = new HashSet<IRI>();
            aliases.add(buildResource(reference));
            for (OWLOntologyID alias : listAliases(reference))
                aliases.add(buildResource(alias));
            for (IRI alias : aliases) {
                // Logical mappings first.
                Iterator<Triple> it = graph.filter(alias, MAPS_TO_GRAPH_URIREF, null);
                while (it.hasNext()) {
                    RDFTerm obj = it.next().getObject();
                    if (obj instanceof IRI) return (IRI) obj;
                }
                Literal litloc = LiteralFactory.getInstance().createTypedLiteral(
                    new IRI(alias.getUnicodeString()));
                // Logical mappings failed, try physical mappings.
                it = graph.filter(null, RETRIEVED_FROM_URIREF, litloc);
                while (it.hasNext()) {
                    RDFTerm obj = it.next().getSubject();
                    if (obj instanceof IRI) return (IRI) obj;
                }
            }
            return null;
        }

        OWLOntologyID getReverseMapping(IRI graphName) {
            // Logical mappings first.
            Iterator<Triple> it = graph.filter(null, MAPS_TO_GRAPH_URIREF, graphName);
            while (it.hasNext()) {
                RDFTerm obj = it.next().getSubject();
                if (obj instanceof IRI) return buildPublicKey((IRI) obj);
            }
            Literal litloc = LiteralFactory.getInstance().createTypedLiteral(
                new IRI(graphName.getUnicodeString()));
            // Logical mappings failed, try physical mappings.
            it = graph.filter(null, RETRIEVED_FROM_URIREF, litloc);
            while (it.hasNext()) {
                RDFTerm subj = it.next().getSubject();
                if (subj instanceof IRI) return buildPublicKey((IRI) subj);

            }
            return null;
        }

        Set<OWLOntologyID> getVersions(org.semanticweb.owlapi.model.IRI ontologyIri) {
            if (ontologyIri == null) throw new IllegalArgumentException("Cannot get versions for a null IRI.");
            Set<OWLOntologyID> keys = new HashSet<OWLOntologyID>();
            LiteralFactory lf = LiteralFactory.getInstance();
            Literal iri = lf.createTypedLiteral(new IRI(ontologyIri.toString()));
            // Exclude aliases.
            for (Iterator<Triple> it = graph.filter(null, HAS_ONTOLOGY_IRI_URIREF, iri); it.hasNext();) {
                RDFTerm sub = it.next().getSubject();
                if (sub instanceof IRI) keys.add(buildPublicKey((IRI) sub));
            }
            // Also check for physical locations
            for (Iterator<Triple> it = graph.filter(null, RETRIEVED_FROM_URIREF, iri); it.hasNext();) {
                RDFTerm sub = it.next().getSubject();
                if (sub instanceof IRI) keys.add(buildPublicKey((IRI) sub));
            }
            return keys;
        }

        void mapLocator(org.semanticweb.owlapi.model.IRI locator, IRI graphName) {
            if (graphName == null) throw new IllegalArgumentException("A null graph name is not allowed.");
            // Null locator is a legal argument, will remove all locator mappings from the supplied graph
            Set<Triple> remove = new HashSet<Triple>();
            for (Iterator<Triple> nodes = graph.filter(graphName, null, null); nodes.hasNext();) {
                Triple t = nodes.next();
                // isOntology |= RDF.type.equals(t.getPredicate()) && OWL.Ontology.equals(t.getObject());
                if (RETRIEVED_FROM_URIREF.equals(t.getPredicate())) remove.add(t);
            }
            graph.removeAll(remove);
            if (locator != null) {
                Literal litloc = LiteralFactory.getInstance().createTypedLiteral(
                    new IRI(locator.toString()));
                graph.add(new TripleImpl(graphName, RETRIEVED_FROM_URIREF, litloc));
            }
        }

        void registerOntologyDeletion(OWLOntologyID publicKey) {
            Set<Triple> toRemove = new HashSet<Triple>();
            Set<OWLOntologyID> aliases = listAliases(publicKey);
            aliases.add(publicKey);
            for (OWLOntologyID alias : aliases) {
                IRI ontologyId = buildResource(alias);
                // Also removes aliases and dependencies.
                // XXX Too extreme?
                for (Iterator<Triple> it = graph.filter(ontologyId, null, null); it.hasNext();)
                    toRemove.add(it.next());
                for (Iterator<Triple> it = graph.filter(null, null, ontologyId); it.hasNext();)
                    toRemove.add(it.next());
            }
            graph.removeAll(toRemove);
        }

        void removeMapping(OWLOntologyID ontologyReference) {
            Iterator<Triple> it = graph.filter(buildResource(ontologyReference), MAPS_TO_GRAPH_URIREF, null);
            // I expect a concurrent modification exception here, but we can deal with it later.
            while (it.hasNext())
                graph.remove(it.next());
        }

        void setMapping(OWLOntologyID ontologyReference, IRI graphName) {
            removeMapping(ontologyReference);
            addMapping(ontologyReference, graphName);
        }

    }

    private static final String _GRAPH_PREFIX_DEFAULT = "ontonet";

    private static final ImportManagementPolicy _IMPORT_POLICY_DEFAULT = ImportManagementPolicy.PRESERVE;

    private static final String _META_GRAPH_ID_DEFAULT = "urn:x-localinstance:/ontologymanager.graph";

    private static final boolean _RESOLVE_IMPORTS_DEFAULT = true;

    protected Multiplexer descriptor = null;

    @Property(name = OntologyProvider.IMPORT_POLICY, options = {
                                                                @PropertyOption(value = '%'
                                                                                        + OntologyProvider.IMPORT_POLICY
                                                                                        + ".option.merge", name = "MERGE"),
                                                                @PropertyOption(value = '%'
                                                                                        + OntologyProvider.IMPORT_POLICY
                                                                                        + ".option.flatten", name = "FLATTEN"),
                                                                @PropertyOption(value = '%'
                                                                                        + OntologyProvider.IMPORT_POLICY
                                                                                        + ".option.preserve", name = "PRESERVE")}, value = "PRESERVE")
    private String importPolicyString;

    /**
     * Maps ontology IRIs (logical or physical if the ontology is anonymous) to Clerezza storage keys i.e.
     * graph names.
     */
    private OntologyToTcMapper keymap = null;

    private Logger log = LoggerFactory.getLogger(getClass());

    private List<OWLOntologyIRIMapper> mappers = new ArrayList<OWLOntologyIRIMapper>();

    @Property(name = OntologyProvider.META_GRAPH_ID, value = _META_GRAPH_ID_DEFAULT)
    protected String metaGraphId = _META_GRAPH_ID_DEFAULT;

    @Reference
    OfflineConfiguration offlineConfig;

    /**
     * The {@link OfflineMode} is used by Stanbol to indicate that no external service should be referenced.
     * For this engine that means it is necessary to check if the used {@link ReferencedSite} can operate
     * offline or not.
     * 
     * @see #enableOfflineMode(OfflineMode)
     * @see #disableOfflineMode(OfflineMode)
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC, bind = "enableOfflineMode", unbind = "disableOfflineMode", strategy = ReferenceStrategy.EVENT)
    private OfflineMode offlineMode;

    @Reference
    Parser parser;

    @Property(name = OntologyProvider.GRAPH_PREFIX, value = _GRAPH_PREFIX_DEFAULT)
    protected String prefix = _GRAPH_PREFIX_DEFAULT;

    @Property(name = OntologyProvider.RESOLVE_IMPORTS, boolValue = _RESOLVE_IMPORTS_DEFAULT)
    protected boolean resolveImports = _RESOLVE_IMPORTS_DEFAULT;

    /*
     * Do not use SCR reference here: this might be different from the registered WeightedTcProvider services
     * : when supplied, it overrides TcManager
     */
    private TcProvider store = null;

    private Class<?>[] supported = null;

    @Reference
    private TcManager tcManager;

    /**
     * This default constructor is <b>only</b> intended to be used by the OSGI environment with Service
     * Component Runtime support.
     * <p>
     * DO NOT USE to manually create instances - the ClerezzaOntologyProvider instances do need to be
     * configured! YOU NEED TO USE {} or its overloads, to parse the configuration and then initialise the
     * rule store if running outside an OSGI environment.
     */
    public ClerezzaOntologyProvider() {
        supported = new Class<?>[] {Graph.class, Graph.class, OWLOntology.class};
    }

    public ClerezzaOntologyProvider(TcProvider store, OfflineConfiguration offline, Parser parser) {
        this();

        this.offlineConfig = offline;
        // Re-assign the TcManager if no store is supplied
        if (store == null) store = TcManager.getInstance();
        this.store = store;
        if (this.tcManager == null) this.tcManager = TcManager.getInstance();
        // Same for the parser
        if (parser == null) this.parser = Parser.getInstance();
        else this.parser = parser;

        activate(new Hashtable<String,Object>());
    }

    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) {
        log.info("in {} activate with context {}", getClass(), context);
        if (context == null) {
            throw new IllegalStateException("No valid" + ComponentContext.class + " parsed in activate!");
        }
        activate((Dictionary<String,Object>) context.getProperties());
    }

    protected void activate(Dictionary<String,Object> configuration) {

        // Check if the TcManager should be set as the store
        if (store == null) store = tcManager;

        // Parse configuration.
        metaGraphId = (String) (configuration.get(OntologyProvider.META_GRAPH_ID));
        if (metaGraphId == null) metaGraphId = _META_GRAPH_ID_DEFAULT; // Should be already assigned though

        // This call will also create the metadata graph.
        keymap = new OntologyToTcMapper();
        descriptor = new GraphMultiplexer(keymap.graph);

        // Parse configuration.
        prefix = (String) (configuration.get(OntologyProvider.GRAPH_PREFIX));
        if (prefix == null) prefix = _GRAPH_PREFIX_DEFAULT; // Should be already assigned though

        try {
            resolveImports = (Boolean) (configuration.get(OntologyProvider.RESOLVE_IMPORTS));
        } catch (Exception ex) {
            resolveImports = _RESOLVE_IMPORTS_DEFAULT; // Should be already assigned though
        }

        Object importPolicy = configuration.get(OntologyProvider.IMPORT_POLICY);
        if (importPolicy == null) {
            this.importPolicyString = _IMPORT_POLICY_DEFAULT.name();
        } else {
            this.importPolicyString = importPolicy.toString();
        }

        // TODO replace with DataFileProvider ?
        final org.semanticweb.owlapi.model.IRI[] offlineResources;
        if (this.offlineConfig != null) {
            List<org.semanticweb.owlapi.model.IRI> paths = offlineConfig.getOntologySourceLocations();
            if (paths != null) offlineResources = paths.toArray(new org.semanticweb.owlapi.model.IRI[0]);
            // There are no offline paths.
            else offlineResources = new org.semanticweb.owlapi.model.IRI[0];
        }
        // There's no offline configuration at all.
        else offlineResources = new org.semanticweb.owlapi.model.IRI[0];
        this.mappers = OWLOntologyManagerFactory.getMappers(offlineResources);

    }

    @Override
    public boolean addAlias(OWLOntologyID primaryKey, OWLOntologyID alias) {
        log.info("Adding alias for ontology entry.");
        log.info(" ... Primary key : {}", primaryKey);
        log.info(" ... Alias : {}", alias);
        // Check that they do not map to two different ontologies.
        OWLOntologyID already = checkAlias(primaryKey, alias);
        /* if (already != null) throw new IllegalArgumentException */log
                .warn(alias + " is already an alias for primary key " + already);
        // XXX a SPARQL query could come in handy.
        // Nothing to do but defer to the meta graph,
        new MetaGraphManager(tcManager, keymap.graph).updateAddAlias(primaryKey, alias);
        log.info(" ... DONE.");
        return true;
    }

    protected OWLOntologyID checkAlias(OWLOntologyID primaryKey, OWLOntologyID alias) {
        for (OWLOntologyID primary : listPrimaryKeys())
            if (listAliases(primary).contains(alias) && !alias.equals(primary)) return primary;
        return null;
    }

    @Override
    public OWLOntologyID createBlankOntologyEntry(OWLOntologyID publicKey) {
        log.info("Creating new orphan entry.");
        log.info(" ... Public key : {}", publicKey);

        if (getStatus(publicKey) != Status.NO_MATCH) throw new IllegalArgumentException(
                "Public key " + publicKey + "is already registered");

        /*
         * TODO keep this object from being created on every call once we get totally rid of the
         * OntologyToTcMapper class.
         */
        MetaGraphManager metaMgr = new MetaGraphManager(tcManager, keymap.graph);
        metaMgr.updateCreateEntry(publicKey);
        log.info(" ... DONE.");
        return publicKey;
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("in {} deactivate with context {}", getClass(), context);
    }

    /**
     * Called by the ConfigurationAdmin to unbind the {@link #offlineMode} if the service becomes unavailable
     * 
     * @param mode
     */
    protected final void disableOfflineMode(OfflineMode mode) {
        this.offlineMode = null;
    }

    /**
     * Called by the ConfigurationAdmin to bind the {@link #offlineMode} if the service becomes available
     * 
     * @param mode
     */
    protected final void enableOfflineMode(OfflineMode mode) {
        this.offlineMode = mode;
    }

    /**
     * Fills a reverse stack of import targets for the graph identified by key <tt>importing</tt>. The import
     * tree is visited in <i>pre-order</i> and the stack is filled accordingly. Optionally, a second stack can
     * be supplied to store only the level 1 imports. This can be used for preserving the original import tree
     * structure.<br>
     * <br>
     * TODO there should be a more space-efficient implementation.
     * 
     * @param importing
     *            the key of the root graph, which will be at the bottom of every list.
     * @param reverseImports
     *            the list that will store all import target keys in pre-order.
     * @param level1Imports
     *            a second list that will store the level 1 import target keys, and is not passed to recursive
     *            calls. Will be ignored if null.
     */
    private void fillImportsReverse(OWLOntologyID importing,
                                    List<OWLOntologyID> reverseImports,
                                    List<OWLOntologyID> level1Imports) {
        log.debug("Filling reverse imports for {}", importing);

        // Add the importing ontology first
        reverseImports.add(importing);
        if (level1Imports != null) level1Imports.add(importing);

        // Get the graph and explore its imports
        Graph graph // store.getTriples(importing);
        = getStoredOntology(/* getPublicKey */(importing), Graph.class, false);
        Iterator<Triple> it = graph.filter(null, RDF.type, OWL.Ontology);
        if (!it.hasNext()) return;
        Iterator<Triple> it2 = graph.filter(it.next().getSubject(), OWL.imports, null);
        while (it2.hasNext()) {
            // obj is the *original* import target
            RDFTerm obj = it2.next().getObject();
            if (obj instanceof IRI) {
                // Right now getKey() is returning the "private" storage ID
                String key = getKey(org.semanticweb.owlapi.model.IRI.create(((IRI) obj).getUnicodeString()));
                // TODO this will not be needed when getKey() and getPublicKey() return the proper public key.
                OWLOntologyID oid = keymap.getReverseMapping(new IRI(key));
                // Check used for breaking cycles in the import graph.
                // (Unoptimized, should not use contains() for stacks.)
                if (!reverseImports.contains(oid)) {
                    if (level1Imports != null) level1Imports.add(oid);
                    fillImportsReverse(oid, reverseImports, null);
                }
            }
        }
    }

    @Override
    public ImportManagementPolicy getImportManagementPolicy() {
        try {
            return ImportManagementPolicy.valueOf(importPolicyString);
        } catch (IllegalArgumentException e) {
            log.warn("The value \""
                     + importPolicyString
                     + "\" configured as default ImportManagementPolicy does not match any value of the Enumeration! "
                     + "Return the default policy as defined by the " + ImportManagementPolicy.class + ".");
            return _IMPORT_POLICY_DEFAULT;
        }
    }

    @Override
    @Deprecated
    public String getKey(org.semanticweb.owlapi.model.IRI ontologyIri) {
        // ontologyIri = URIUtils.sanitizeID(ontologyIri);
        return getPublicKey(new OWLOntologyID(ontologyIri));
    }

    @Override
    @Deprecated
    public String getKey(OWLOntologyID ontologyId) {
        return getPublicKey(ontologyId);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <O extends Graph> O getMetaGraph(Class<O> returnType) {
        if (!Graph.class.isAssignableFrom(returnType)) throw new IllegalArgumentException(
                "Only subtypes of " + Graph.class + " are allowed.");
        return (O) store.getGraph(new IRI(metaGraphId));
    }

    @Override
    @Deprecated
    public OWLOntologyID getOntologyId(String storageKey) {
        return keymap.getReverseMapping(new IRI(storageKey));
    }

    public OntologyNetworkConfiguration getOntologyNetworkConfiguration() {
        Map<String,Collection<OWLOntologyID>> coreOntologies = new HashMap<String,Collection<OWLOntologyID>>(), customOntologies = new HashMap<String,Collection<OWLOntologyID>>();
        Map<String,Collection<String>> attachedScopes = new HashMap<String,Collection<String>>();
        final Graph meta = store.getGraph(new IRI(metaGraphId));

        // Scopes first
        for (Iterator<Triple> it = meta.filter(null, RDF.type, SCOPE_URIREF); it.hasNext();) { // for each
                                                                                               // scope
            Triple ta = it.next();
            BlankNodeOrIRI sub = ta.getSubject();
            if (sub instanceof IRI) {
                String s = ((IRI) sub).getUnicodeString(), prefix = _NS_STANBOL_INTERNAL + Scope.shortName
                                                                       + "/";
                if (s.startsWith(prefix)) {
                    String scopeId = s.substring(prefix.length());
                    log.info("Rebuilding scope \"{}\".", scopeId);
                    coreOntologies.put(scopeId, new TreeSet<OWLOntologyID>());
                    customOntologies.put(scopeId, new TreeSet<OWLOntologyID>());
                    IRI core_ur = null, custom_ur = null;
                    RDFTerm r;
                    // Check core space
                    Iterator<Triple> it2 = meta.filter(sub, HAS_SPACE_CORE_URIREF, null);
                    if (it2.hasNext()) {
                        r = it2.next().getObject();
                        if (r instanceof IRI) core_ur = (IRI) r;
                    } else {
                        it2 = meta.filter(null, IS_SPACE_CORE_OF_URIREF, sub);
                        if (it2.hasNext()) {
                            r = it2.next().getSubject();
                            if (r instanceof IRI) core_ur = (IRI) r;
                        }
                    }

                    // Check custom space
                    it2 = meta.filter(sub, HAS_SPACE_CUSTOM_URIREF, null);
                    if (it2.hasNext()) {
                        r = it2.next().getObject();
                        if (r instanceof IRI) custom_ur = (IRI) r;
                    } else {
                        it2 = meta.filter(null, IS_SPACE_CUSTOM_OF_URIREF, sub);
                        if (it2.hasNext()) {
                            r = it2.next().getSubject();
                            if (r instanceof IRI) custom_ur = (IRI) r;
                        }
                    }

                    // retrieve the ontologies
                    if (core_ur != null) {
                        for (it2 = meta.filter(core_ur, null, null); it2.hasNext();) {
                            Triple t = it2.next();
                            IRI predicate = t.getPredicate();
                            if (predicate.equals(MANAGES_URIREF)) {
                                if (t.getObject() instanceof IRI) coreOntologies.get(scopeId).add(
                                    keymap.buildPublicKey((IRI) t.getObject()) // FIXME must be very
                                                                                  // temporary!
                                        // ((IRI) t.getObject()).getUnicodeString()
                                        );
                            }
                        }
                        for (it2 = meta.filter(null, null, core_ur); it2.hasNext();) {
                            Triple t = it2.next();
                            IRI predicate = t.getPredicate();
                            if (predicate.equals(IS_MANAGED_BY_URIREF)) {
                                if (t.getSubject() instanceof IRI) coreOntologies.get(scopeId).add(
                                    keymap.buildPublicKey((IRI) t.getSubject()) // FIXME must be very
                                                                                   // temporary!
                                        // ((IRI) t.getSubject()).getUnicodeString()
                                        );
                            }
                        }
                    }
                    if (custom_ur != null) {
                        for (it2 = meta.filter(custom_ur, null, null); it2.hasNext();) {
                            Triple t = it2.next();
                            IRI predicate = t.getPredicate();
                            if (predicate.equals(MANAGES_URIREF)) {
                                if (t.getObject() instanceof IRI) customOntologies.get(scopeId).add(
                                    keymap.buildPublicKey((IRI) t.getObject()) // FIXME must be very
                                                                                  // temporary!
                                        // ((IRI) t.getObject()).getUnicodeString()
                                        );
                            }
                        }
                        for (it2 = meta.filter(null, null, custom_ur); it2.hasNext();) {
                            Triple t = it2.next();
                            IRI predicate = t.getPredicate();
                            if (predicate.equals(IS_MANAGED_BY_URIREF)) {
                                if (t.getSubject() instanceof IRI) customOntologies.get(scopeId).add(
                                    keymap.buildPublicKey((IRI) t.getSubject()) // FIXME must be very
                                                                                   // temporary!
                                        // ((IRI) t.getSubject()).getUnicodeString()
                                        );
                            }
                        }
                    }

                }
            }
        }

        // Sessions next
        Map<String,Collection<OWLOntologyID>> sessionOntologies = new HashMap<String,Collection<OWLOntologyID>>();
        for (Iterator<Triple> it = meta.filter(null, RDF.type, SESSION_URIREF); it.hasNext();) { // for each
                                                                                                 // scope
            Triple ta = it.next();
            BlankNodeOrIRI sub = ta.getSubject();
            if (sub instanceof IRI) {
                IRI ses_ur = (IRI) sub;
                String s = ((IRI) sub).getUnicodeString();
                String prefix = _NS_STANBOL_INTERNAL + Session.shortName + "/";
                if (s.startsWith(prefix)) {
                    String sessionId = s.substring(prefix.length());
                    log.info("Rebuilding session \"{}\".", sessionId);
                    sessionOntologies.put(sessionId, new TreeSet<OWLOntologyID>());
                    attachedScopes.put(sessionId, new TreeSet<String>());
                    // retrieve the ontologies
                    if (ses_ur != null) {
                        for (Iterator<Triple> it2 = meta.filter(ses_ur, MANAGES_URIREF, null); it2.hasNext();) {
                            RDFTerm obj = it2.next().getObject();
                            if (obj instanceof IRI) sessionOntologies.get(sessionId).add(
                                keymap.buildPublicKey((IRI) obj) // FIXME must be very temporary!
                                    // ((IRI) obj).getUnicodeString()
                                    );

                        }
                        for (Iterator<Triple> it2 = meta.filter(null, IS_MANAGED_BY_URIREF, ses_ur); it2
                                .hasNext();) {
                            RDFTerm subj = it2.next().getSubject();
                            if (subj instanceof IRI) sessionOntologies.get(sessionId).add(
                                keymap.buildPublicKey((IRI) subj) // FIXME must be very temporary!
                                    // ((IRI) subj).getUnicodeString()
                                    );

                        }
                        for (Iterator<Triple> it2 = meta.filter(null, APPENDED_TO_URIREF, ses_ur); it2
                                .hasNext();) {
                            RDFTerm subj = it2.next().getSubject();
                            if (subj instanceof IRI) {
                                String s1 = ((IRI) subj).getUnicodeString();
                                String prefix1 = _NS_STANBOL_INTERNAL + Scope.shortName + "/";
                                if (s1.startsWith(prefix1)) {
                                    String scopeId = s1.substring(prefix1.length());
                                    attachedScopes.get(sessionId).add(scopeId);
                                }
                            }
                        }
                        for (Iterator<Triple> it2 = meta.filter(ses_ur, HAS_APPENDED_URIREF, null); it2
                                .hasNext();) {
                            RDFTerm obj = it2.next().getObject();
                            if (obj instanceof IRI) {
                                String s1 = ((IRI) obj).getUnicodeString();
                                String prefix1 = _NS_STANBOL_INTERNAL + Scope.shortName + "/";
                                if (s1.startsWith(prefix1)) {
                                    String scopeId = s1.substring(prefix1.length());
                                    attachedScopes.get(sessionId).add(scopeId);
                                }
                            }
                        }
                    }
                }
            }
        }

        return new OntologyNetworkConfiguration(coreOntologies, customOntologies, sessionOntologies,
                attachedScopes);
    }

    @Override
    public Multiplexer getOntologyNetworkDescriptor() {
        return descriptor;
    }

    @Override
    @Deprecated
    public String getPublicKey(OWLOntologyID ontologyId) {
        IRI ur = keymap.getMapping(ontologyId);
        log.debug("key for {} is {}", ontologyId, ur);
        return (ur == null) ? null : ur.getUnicodeString();
    }

    @Override
    @Deprecated
    public Set<OWLOntologyID> getPublicKeys() {
        return descriptor.getPublicKeys();
    }

    @Override
    public TcProvider getStore() {
        return store;
    }

    @Override
    @Deprecated
    public <O> O getStoredOntology(org.semanticweb.owlapi.model.IRI reference, Class<O> returnType) {
        // reference = URIUtils.sanitizeID(reference);
        return getStoredOntology(new OWLOntologyID(reference), returnType);
    }

    @Override
    @Deprecated
    public <O> O getStoredOntology(org.semanticweb.owlapi.model.IRI reference, Class<O> returnType, boolean merge) {
        // reference = URIUtils.sanitizeID(reference);
        return getStoredOntology(new OWLOntologyID(reference), returnType, merge);
    }

    @Override
    public <O> O getStoredOntology(OWLOntologyID reference, Class<O> returnType) {
        return getStoredOntology(reference, returnType, false);
    }

    @Override
    public <O> O getStoredOntology(OWLOntologyID reference, Class<O> returnType, boolean merge) {
        switch (getStatus(reference)) {
            case NO_MATCH:
                throw new IllegalArgumentException("No ontology with public key " + reference + " found.");
            case UNCHARTED:
                log.warn("No key found for IRI {}", reference);
                return null;
            case ORPHAN:
                throw new OrphanOntologyKeyException(reference);
            default:
                String key = /* getKey(reference); */
                keymap.getMapping(reference).getUnicodeString();
                return getStoredOntology(key, returnType, merge);
        }
    }

    @Override
    @Deprecated
    public <O> O getStoredOntology(String key, Class<O> returnType) {
        return getStoredOntology(key, returnType, false);
    }

    /**
     * In this implementation the identifier is the ImmutableGraph Name (e.g. ontonet::blabla)
     */
    @SuppressWarnings("unchecked")
    @Override
    @Deprecated
    public <O> O getStoredOntology(String identifier, Class<O> returnType, boolean forceMerge) {
        if (identifier == null || identifier.isEmpty()) throw new IllegalArgumentException(
                "Identifier cannot be null or empty.");
        if (returnType == null) {
            // Defaults to OWLOntology
            returnType = (Class<O>) OWLOntology.class;
            log.warn("No return type given for the ontology. Will return a {}", returnType.getCanonicalName());
        }
        boolean canDo = false;
        for (Class<?> clazz : getSupportedReturnTypes())
            if (clazz.isAssignableFrom(returnType)) {
                canDo = true;
                break;
            }
        if (!canDo) throw new UnsupportedOperationException(
                "Return type " + returnType.getCanonicalName()
                        + " is not allowed in this implementation. Only allowed return types are "
                        + supported);

        Graph tc = store.getGraph(new IRI(identifier));
        if (tc == null) return null;
        /*
         * The ontology provider itself does not wrap the returned object into an in-memory graph, therefore
         * any direct modifications will be propagated. Collectors should wrap them, though. To change this
         * behaviour, uncomment the line below.
         */
        // tc = new SimpleGraph(tc);

        if (Graph.class.equals(returnType) || Graph.class.isAssignableFrom(returnType)) {
            return returnType.cast(tc);
        } else if (OWLOntology.class.isAssignableFrom(returnType)) {
            try {
                return (O) toOWLOntology(new IRI(identifier), forceMerge);
            } catch (OWLOntologyCreationException e) {
                log.error(
                    "Failed to return stored ontology " + identifier + " as type "
                            + returnType.getCanonicalName(), e);
            }
        }

        return null;
    }

    @Override
    public Class<?>[] getSupportedReturnTypes() {
        return supported;
    }

    @Override
    public boolean hasOntology(org.semanticweb.owlapi.model.IRI ontologyIri) {
        // ontologyIri = URIUtils.sanitizeID(ontologyIri);
        return hasOntology(new OWLOntologyID(ontologyIri));
    }

    @Override
    public boolean hasOntology(OWLOntologyID id) {
        Status stat = getStatus(id);
        if (stat == Status.ORPHAN) throw new OrphanOntologyKeyException(id);
        return getStatus(id) == Status.MATCH;
    }

    @Override
    public Status getStatus(OWLOntologyID publicKey) {
        if (publicKey == null || publicKey.isAnonymous()) throw new IllegalArgumentException(
                "Cannot check for an anonymous ontology.");
        if (!new MetaGraphManager(tcManager, keymap.graph).exists(publicKey)) return Status.NO_MATCH;
        IRI graphName = keymap.getMapping(publicKey);
        if (graphName == null) return Status.UNCHARTED;
        if (store.listGraphs().contains(graphName)) return Status.MATCH;
        else return Status.ORPHAN;
    }

    /**
     * Returns <code>true</code> only if Stanbol operates in {@link OfflineMode}.
     * 
     * @return the offline state
     */
    protected final boolean isOfflineMode() {
        return offlineMode != null;
    }

    /*
     * (non-Javadoc) XXX see if we can use reasoners, either live or by caching materialisations.
     * 
     * @see
     * org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyProvider#listAliases(org.semanticweb
     * .owlapi.model.OWLOntologyID)
     */
    @Override
    public Set<OWLOntologyID> listAliases(OWLOntologyID publicKey) {
        if (publicKey == null || publicKey.isAnonymous()) throw new IllegalArgumentException(
                "Cannot locate aliases for null or anonymous public keys.");
        final Set<OWLOntologyID> aliases = new HashSet<OWLOntologyID>();
        computeAliasClosure(publicKey, aliases);
        aliases.remove(publicKey);
        return aliases;
    }

    protected void computeAliasClosure(OWLOntologyID publicKey, Set<OWLOntologyID> target) {
        target.add(publicKey);
        Graph meta = getMetaGraph(Graph.class);
        IRI ont = keymap.buildResource(publicKey);
        Set<RDFTerm> resources = new HashSet<RDFTerm>();
        // Forwards
        for (Iterator<Triple> it = meta.filter(ont, OWL.sameAs, null); it.hasNext();)
            resources.add(it.next().getObject());
        // Backwards
        for (Iterator<Triple> it = meta.filter(null, OWL.sameAs, ont); it.hasNext();)
            resources.add(it.next().getSubject());
        for (RDFTerm r : resources)
            if (r instanceof IRI) {
                OWLOntologyID newKey = keymap.buildPublicKey((IRI) r);
                if (!target.contains(newKey)) computeAliasClosure(newKey, target);
            }
    }

    @Override
    public Set<OWLOntologyID> listAllRegisteredEntries() {
        Set<OWLOntologyID> result = new TreeSet<OWLOntologyID>();
        // Add aliases
        for (OWLOntologyID key : descriptor.getPublicKeys()) {
            result.add(key);
            for (OWLOntologyID alias : listAliases(key))
                result.add(alias);
        }
        return result;
    }

    @Override
    public SortedSet<OWLOntologyID> listOrphans() {
        SortedSet<OWLOntologyID> result = new TreeSet<OWLOntologyID>();
        for (OWLOntologyID key : descriptor.getPublicKeys()) {
            IRI graphName = keymap.getMapping(key);
            if (graphName == null || !store.listGraphs().contains(graphName)) result.add(key);
        }
        return result;
    }

    @Override
    public SortedSet<OWLOntologyID> listPrimaryKeys() {
        // XXX here we should decide if non-registered Clerezza graphs should also be in the list.
        SortedSet<OWLOntologyID> result = new TreeSet<OWLOntologyID>();
        for (OWLOntologyID key : descriptor.getPublicKeys())
            if (keymap.getMapping(key) != null) result.add(key);
        return result;
    }

    @Override
    public Set<OWLOntologyID> listVersions(org.semanticweb.owlapi.model.IRI ontologyIri) {
        return keymap.getVersions(ontologyIri);
    }

    @Override
    public OWLOntologyID loadInStore(InputStream data,
                                     String formatIdentifier,
                                     boolean force,
                                     Origin<?>... references) {
        if (data == null) throw new IllegalArgumentException("No data to load ontologies from.");
        if (formatIdentifier == null || formatIdentifier.trim().isEmpty()) throw new IllegalArgumentException(
                "A non-null, non-blank format identifier is required for parsing the data stream.");
        checkReplaceability(references);

        // This method only tries the supplied format once.
        log.debug("Trying to parse data stream with format {}", formatIdentifier);
        Graph rdfData = parser.parse(data, formatIdentifier);
        log.debug("SUCCESS format {}.", formatIdentifier);
        return loadInStore(rdfData, force, references);
    }

    @Override
    public OWLOntologyID loadInStore(final org.semanticweb.owlapi.model.IRI ontologyIri,
                                     String formatIdentifier,
                                     boolean force,
                                     Origin<?>... origins) throws IOException {
        log.debug("Loading {}", ontologyIri);
        if (ontologyIri == null) throw new IllegalArgumentException("Ontology IRI cannot be null.");

        org.semanticweb.owlapi.model.IRI location = null;
        if (force) location = null;
        else for (OWLOntologyIRIMapper mapper : mappers) {
            location = mapper.getDocumentIRI(ontologyIri);
            if (location != null) break;
        }
        if (location == null) {
            if (isOfflineMode()) throw new IllegalStateException(
                    "Cannot retrieve " + ontologyIri + " while Stanbol is in offline mode. "
                            + "No resource with that identifier was found locally.");
            else location = ontologyIri;
        }

        log.info("found {} in {}", ontologyIri, location);

        // Add the physical IRI to the origins.
        origins = Arrays.copyOf(origins, origins.length + 1);
        origins[origins.length - 1] = Origin.create(ontologyIri);
        checkReplaceability(origins);

        // Get ordered list of preferred/supported formats, or use the specified one.
        List<String> supported = OntologyUtils.getPreferredSupportedFormats(parser.getSupportedFormats());
        List<String> formats;
        if (formatIdentifier == null || "".equals(formatIdentifier.trim())) formats = supported;
        else {
            formats = new LinkedList<String>();
            // Pre-check supported format
            if (supported.contains(formatIdentifier)) formats.add(formatIdentifier);
            for (String sup : supported)
                if (sup != null && !formats.contains(sup)) formats.add(sup);
        }

        for (String currentFormat : formats) {
            try {
                final URLConnection con = location.toURI().toURL().openConnection();
                con.setRequestProperty("Accept", currentFormat);
                final InputStream is = con.getInputStream();
                if (is != null) {
                    /*
                     * We provide the current format, so the recursive call won't be trying to sort preferred
                     * formats again. Also, we provide the ontologyIRI as the preferred key, since we already
                     * know it.
                     */
                    OWLOntologyID key = loadInStore(is, currentFormat, force, origins);
                    // If parsing failed, an exception will be thrown before getting here, so no risk.
                    // if (key != null && !key.isEmpty()) setLocatorMapping(ontologyIri, key);
                    return key;
                }
            } catch (UnsupportedFormatException e) {
                log.debug("FAILURE format {} (unsupported). Trying next one.", currentFormat);
                continue;
            } catch (Exception e) {
                log.debug("FAILURE format {} (parse error). Will try next one.", currentFormat);
                continue;
            }
        }

        // No parser worked, return null.
        log.error("All parsers failed, giving up.");
        return null;
    }

    @Override
    public OWLOntologyID loadInStore(Object ontology, final boolean force, Origin<?>... origins) {

        if (ontology == null) throw new IllegalArgumentException("No ontology supplied.");
        checkReplaceability(origins);
        long before = System.currentTimeMillis();

        Graph targetGraph; // The final graph
        Graph rdfData; // The supplied ontology converted to Graph

        if (ontology instanceof OWLOntology) {
            // This will be in memory!
            rdfData = OWLAPIToClerezzaConverter.owlOntologyToClerezzaGraph((OWLOntology) ontology);
        } else if (ontology instanceof Graph) {
            // This might be in memory or in persistent storage.
            rdfData = (Graph) ontology;
        } else throw new UnsupportedOperationException(
                "This ontology provider can only accept objects assignable to " + Graph.class
                        + " or " + OWLOntology.class);

        // XXX Force is ignored for the content, but the imports?

        // Now we proceed to assign the primary key to the ontology.
        OWLOntologyID primaryKey = null;

        /*
         * Compute aliases
         */
        IRI graphName = null;
        List<OWLOntologyID> overrides = new ArrayList<OWLOntologyID>(); // Priority aliases.
        List<org.semanticweb.owlapi.model.IRI> sources = new ArrayList<org.semanticweb.owlapi.model.IRI>(); // Second-choice aliases.

        // Scan origins ONCE.
        for (int i = 0; i < origins.length; i++) {
            Origin<?> origin = origins[i];
            log.debug("Found origin at index {}", i);
            if (origin == null) {
                log.warn("Null origin at index {}. Skipping.", i);
                continue;
            }
            Object ref = origin.getReference();
            if (ref == null) {
                log.warn("Null reference at index {}. Skipping.", i);
                continue;
            }
            log.debug(" ... Reference is a {}", ref.getClass().getCanonicalName());
            log.debug(" ... Value : {}", ref);
            if (ref instanceof OWLOntologyID) {
                OWLOntologyID key = (OWLOntologyID) ref;
                if (primaryKey == null) {
                    primaryKey = key;
                    log.debug(" ... assigned as primary key.");
                } else if (primaryKey.equals(key)) {
                    log.debug(" ... matches primary key. Skipping.");
                } else {
                    overrides.add(key);
                    log.debug(" ... assigned as a priority alias for {}", primaryKey);
                }
            } else if (ref instanceof org.semanticweb.owlapi.model.IRI) {
                sources.add((org.semanticweb.owlapi.model.IRI) ref);
                log.debug(" ... assigned as a secondary alias (source) for {}", primaryKey);
            } else if (ref instanceof IRI) {
                if (graphName != null) log.warn("ImmutableGraph name already assigned as {}. Skipping.", graphName);
                else {
                    graphName = (IRI) ref;
                    log.debug(" ... assigned as a graph name for {}", primaryKey);
                }
            } else {
                log.warn("Unhandled type for origin at index {} : {}. Skipping.", i, ref.getClass());
            }
        }

        // The actual logical ID will be dereferenceable no matter what.
        OWLOntologyID extractedId = OWLUtils.extractOntologyID(rdfData);
        if (primaryKey == null) primaryKey = extractedId; // Not overridden: set as primary key.
        else overrides.add(extractedId); // Overridden: must be an alias anyway.

        if (primaryKey == null) // No overrides, no extracted ID.
        {
            org.semanticweb.owlapi.model.IRI z;
            // The first IRI found becomes the primary key.
            if (!sources.isEmpty()) z = sources.iterator().next();
            else // Try the graph name
            if (graphName != null) z = org.semanticweb.owlapi.model.IRI.create(graphName.getUnicodeString());
            else // Extrema ratio : compute a timestamped primary key.
            z = org.semanticweb.owlapi.model.IRI.create(getClass().getCanonicalName() + "-time:" + System.currentTimeMillis());
            primaryKey = new OWLOntologyID(z);
        }

        // Check if it is possible to avoid reloading the ontology content from its source.
        boolean mustLoad = true;
        if (!force && graphName != null && store.listGraphs().contains(graphName)) {
            boolean condition = true; // Any failed check will abort the scan.
            // Check if the extracted ontology ID matches that of the supplied graph.
            // XXX note that anonymous ontologies should be considered a match... or should they not?
            Graph tc = store.getGraph(graphName);
            OWLOntologyID idFromStore = OWLUtils.extractOntologyID(tc);
            condition &= (extractedId == null && idFromStore == null) || extractedId.equals(idFromStore);
            // Finally, a size check
            // FIXME not a good policy for graphs that change without altering the size.
            if (condition && rdfData instanceof Graph) condition &= tc.size() == rdfData.size();
            mustLoad &= !condition;
        }

        if (!mustLoad && graphName != null) {
            log.debug("ImmutableGraph with ID {} already in store. Default action is to skip storage.", graphName);
            targetGraph = store.getGraph(graphName);
        } else {
            String iri = null;
            if (primaryKey.getOntologyIRI() != null) iri = primaryKey.getOntologyIRI().toString();
            if (primaryKey.getVersionIRI() != null) iri += ":::" + primaryKey.getVersionIRI().toString();
            // s will become the graph name
            String s = (iri.startsWith(prefix + "::")) ? "" : (prefix + "::");
            s += iri;
            graphName = new IRI(URIUtils.sanitize(s));
            log.debug("Storing ontology with graph ID {}", graphName);
            try {
                targetGraph = store.createGraph(graphName);
            } catch (EntityAlreadyExistsException e) {
                if (graphName.equals(e.getEntityName())) targetGraph = store.getGraph(graphName);
                else targetGraph = store.createGraph(graphName);
            }
            targetGraph.addAll(rdfData);
        }

        // All is already sanitized by the time we get here.

        // Now do the mappings
        String mappedIds = "";
        // Discard unconventional ontology IDs with only the version IRI
        if (primaryKey != null && primaryKey.getOntologyIRI() != null) {
            // Versioned or not, the real ID mapping is always added
            keymap.setMapping(primaryKey, graphName);
            mappedIds += primaryKey;
            // TODO map unversioned ID as well?
            Triple t = new TripleImpl(keymap.buildResource(primaryKey), SIZE_IN_TRIPLES_URIREF,
                    LiteralFactory.getInstance().createTypedLiteral(Integer.valueOf(rdfData.size())));
            getMetaGraph(Graph.class).add(t);
        }

        // Add aliases.
        for (org.semanticweb.owlapi.model.IRI source : sources)
            if (source != null) overrides.add(new OWLOntologyID(source));
        for (OWLOntologyID alias : overrides)
            if (alias != null && !alias.equals(primaryKey)) {
                addAlias(primaryKey, alias);
                mappedIds += " , " + alias;
            }

        // Do this AFTER registering the ontology, otherwise import cycles will cause infinite loops.
        if (resolveImports) {
            // Scan resources of type owl:Ontology, but only get the first.
            Iterator<Triple> it = targetGraph.filter(null, RDF.type, OWL.Ontology);
            if (it.hasNext()) {
                // Scan import statements for the one owl:Ontology considered.
                Iterator<Triple> it2 = targetGraph.filter(it.next().getSubject(), OWL.imports, null);
                while (it2.hasNext()) {
                    RDFTerm obj = it2.next().getObject();
                    log.info("Resolving import target {}", obj);
                    if (obj instanceof IRI) try {
                        // TODO try locals first
                        IRI target = (IRI) obj;
                        OWLOntologyID id = new OWLOntologyID(org.semanticweb.owlapi.model.IRI.create(target.getUnicodeString()));
                        if (keymap.getMapping(id) == null) { // Check if it's not there already.
                            if (isOfflineMode()) throw new RuntimeException(
                                    "Cannot load imported ontology " + obj
                                            + " while Stanbol is in offline mode.");
                            // TODO manage origins for imported ontologies too?
                            OWLOntologyID id2 = loadInStore(org.semanticweb.owlapi.model.IRI.create(((IRI) obj).getUnicodeString()),
                                null, false);
                            if (id2 != null) id = id2;
                            log.info("Import {} resolved.", obj);
                            log.debug("");
                        } else {
                            log.info("Requested import already stored. Setting dependency only.");
                        }
                        descriptor.setDependency(primaryKey, id);
                    } catch (UnsupportedFormatException e) {
                        log.warn("Failed to parse format for resource " + obj, e);
                        // / XXX configure to continue?
                    } catch (IOException e) {
                        log.warn("Failed to load ontology from resource " + obj, e);
                        // / XXX configure to continue?
                    }
                }
            }
        }

        log.debug(" Ontology {}", mappedIds);
        if (targetGraph != null) log.debug(" ... ({} triples)", targetGraph.size());
        log.debug(" ... primary public key : {}", primaryKey);
        // log.debug("--- {}", URIUtils.sanitize(s));
        log.debug("Time: {} ms", (System.currentTimeMillis() - before));
        // return URIUtils.sanitize(s);
        return primaryKey;
    }

    @Override
    public boolean removeOntology(OWLOntologyID publicKey) throws OntologyHandleException {

        if (descriptor.getDependents(publicKey).isEmpty() && descriptor.getHandles(publicKey).isEmpty()) {

            IRI graphName = keymap.getMapping(publicKey);

            // TODO propagate everything to the descriptor
            descriptor.clearDependencies(publicKey); // release dependencies
            keymap.registerOntologyDeletion(publicKey); // remove metadata

            // Now the actual deletion
            store.deleteGraph(graphName);

            return true;
        } else throw new OntologyHandleException("There are ontologies or collectors depending on "
                                                 + publicKey + "Those handles must be released first.");
    }

    @Override
    public void setImportManagementPolicy(ImportManagementPolicy policy) {
        if (policy == null) throw new IllegalArgumentException("Import management policy cannot be null.");
        importPolicyString = policy.toString();
    }

    @Override
    public void setLocatorMapping(org.semanticweb.owlapi.model.IRI locator, OWLOntologyID publicKey) {
        if (publicKey == null || publicKey.isAnonymous()) throw new IllegalArgumentException(
                "key must be non-null and non-anonymous.");
        log.info("Setting {} as the resource locator for ontology {}", locator, publicKey);
        new MetaGraphManager(tcManager, keymap.graph).updateAddAlias(new OWLOntologyID(locator), publicKey);
    }

    @Override
    public void setLocatorMapping(org.semanticweb.owlapi.model.IRI locator, String key) {
        if (key == null || key.isEmpty()) throw new IllegalArgumentException(
                "key must be non-null and non-empty.");
        if (!store.listGraphs().contains(new IRI(key))) throw new IllegalArgumentException(
                "No ontology found with storage key " + key);
        if (locator == null) log
                .warn(
                    "Setting null locator for {}. This will remove all physical mappings for the corresponding graph.",
                    key);
        else log.info("Setting {} as the resource locator for ontology {}", locator, key);
        keymap.mapLocator(locator, new IRI(key));
    }

    protected void checkReplaceability(Origin<?>... origins) {
        for (Origin<?> or : origins) {
            if (or == null || !(or.getReference() instanceof OWLOntologyID)) continue;
            OWLOntologyID key = (OWLOntologyID) or.getReference();
            if (getStatus(key) == Status.MATCH) throw new IllegalStateException(
                    "Public key " + key + " matches an existing non-orphan entry. Cannot replace.");
        }
    }

    /**
     * 
     * @param graphName
     * @param forceMerge
     *            if set to false, the selected import management policy will be applied.
     * @return
     * @throws OWLOntologyCreationException
     */
    protected OWLOntology toOWLOntology(IRI graphName, boolean forceMerge) throws OWLOntologyCreationException {

        log.debug("Exporting graph to OWLOntology");
        log.debug(" -- ImmutableGraph name : {}", graphName);
        OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
        // Never try to import
        mgr.addIRIMapper(new PhonyIRIMapper(Collections.<org.semanticweb.owlapi.model.IRI> emptySet()));

        Set<OWLOntologyID> loaded = new HashSet<OWLOntologyID>();
        Graph graph = store.getGraph(graphName);
        IRI ontologyId = null;

        // Get the id of this ontology.
        Iterator<Triple> itt = graph.filter(null, RDF.type, OWL.Ontology);
        if (itt.hasNext()) {
            BlankNodeOrIRI nl = itt.next().getSubject();
            if (nl instanceof IRI) ontologyId = (IRI) nl;
        }
        List<OWLOntologyID> revImps = new Stack<OWLOntologyID>();
        List<OWLOntologyID> lvl1 = new Stack<OWLOntologyID>();

        fillImportsReverse(keymap.getReverseMapping(graphName), revImps, lvl1);

        // If not set to merge (either by policy of by force), adopt the set import policy.
        if (!forceMerge && !ImportManagementPolicy.MERGE.equals(getImportManagementPolicy())) {
            OWLOntology o = OWLAPIToClerezzaConverter.clerezzaGraphToOWLOntology(graph, mgr);
            // TODO make it not flat.
            // Examining the reverse imports stack will flatten all imports.
            List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
            OWLDataFactory df = OWLManager.getOWLDataFactory();

            List<OWLOntologyID> listToUse;
            switch (getImportManagementPolicy()) {
                case FLATTEN:
                    listToUse = revImps;
                    break;
                case PRESERVE:
                    listToUse = lvl1;
                    break;
                default:
                    listToUse = lvl1;
                    break;
            }

            for (OWLOntologyID ref : listToUse)
                if (!loaded.contains(ref) && !ref.equals(keymap.getReverseMapping(graphName))) {
                    changes.add(new AddImport(o, df.getOWLImportsDeclaration(ref.getOntologyIRI())));
                    loaded.add(ref);
                }
            o.getOWLOntologyManager().applyChanges(changes);
            return o;
        } else {
            // Merge

            // If there is just the root ontology, convert it straight away.
            if (revImps.size() == 1 && revImps.contains(graphName)) {
                OWLOntology o = OWLAPIToClerezzaConverter.clerezzaGraphToOWLOntology(graph, mgr);
                return o;
            }

            // FIXME when there's more than one ontology, this way of merging them seems inefficient...
            Graph tempGraph = new IndexedGraph();
            // The set of triples that will be excluded from the merge
            Set<Triple> exclusions = new HashSet<Triple>();
            // Examine all reverse imports
            for (OWLOntologyID ref : revImps)
                if (!loaded.contains(ref)) {
                    // Get the triples
                    Graph imported =
                    // store.getTriples(ref);
                    getStoredOntology(getKey(ref), Graph.class, false);
                    // For each owl:Ontology
                    Iterator<Triple> remove = imported.filter(null, RDF.type, OWL.Ontology);
                    while (remove.hasNext()) {
                        BlankNodeOrIRI subj = remove.next().getSubject();
                        /*
                         * If it's not the root ontology, trash all its triples. If the root ontology is
                         * anonymous, all ontology annotations are to be trashed without distinction.
                         */
                        if (ontologyId == null || !subj.equals(ontologyId)) {
                            Iterator<Triple> it = imported.filter(subj, null, null);
                            while (it.hasNext()) {
                                Triple t = it.next();
                                exclusions.add(t);
                            }
                        }
                    }

                    Iterator<Triple> it = imported.iterator();
                    while (it.hasNext()) {
                        Triple t = it.next();
                        if (!exclusions.contains(t)) tempGraph.add(t);
                    }

                    loaded.add(ref);
                }
            // Since they are all merged and import statements removed, there should be no risk of going
            // online.
            return OWLAPIToClerezzaConverter.clerezzaGraphToOWLOntology(tempGraph, mgr);
        }
    }

}

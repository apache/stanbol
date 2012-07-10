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

import static org.apache.stanbol.ontologymanager.ontonet.api.Vocabulary.HAS_SPACE_CORE;
import static org.apache.stanbol.ontologymanager.ontonet.api.Vocabulary.HAS_SPACE_CUSTOM;
import static org.apache.stanbol.ontologymanager.ontonet.api.Vocabulary.IS_MANAGED_BY;
import static org.apache.stanbol.ontologymanager.ontonet.api.Vocabulary.IS_SPACE_CORE_OF;
import static org.apache.stanbol.ontologymanager.ontonet.api.Vocabulary.IS_SPACE_CUSTOM_OF;
import static org.apache.stanbol.ontologymanager.ontonet.api.Vocabulary.MANAGES;
import static org.apache.stanbol.ontologymanager.ontonet.api.Vocabulary.SCOPE;
import static org.apache.stanbol.ontologymanager.ontonet.api.Vocabulary.SESSION;
import static org.apache.stanbol.ontologymanager.ontonet.api.Vocabulary.SPACE;
import static org.apache.stanbol.ontologymanager.ontonet.api.Vocabulary._NS_ONTONET;
import static org.apache.stanbol.ontologymanager.ontonet.api.Vocabulary._NS_STANBOL_INTERNAL;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
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
import java.util.Stack;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.EntityAlreadyExistsException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.access.TcProvider;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
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
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.commons.owl.OWLOntologyManagerFactory;
import org.apache.stanbol.commons.owl.PhonyIRIMapper;
import org.apache.stanbol.commons.owl.transformation.OWLAPIToClerezzaConverter;
import org.apache.stanbol.commons.owl.util.OWLUtils;
import org.apache.stanbol.commons.owl.util.URIUtils;
import org.apache.stanbol.commons.stanboltools.offline.OfflineMode;
import org.apache.stanbol.ontologymanager.ontonet.api.OfflineConfiguration;
import org.apache.stanbol.ontologymanager.ontonet.api.OntologyNetworkConfiguration;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.ImportManagementPolicy;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.OntologyCollector;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.OntologyCollectorListener;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.ScopeEventListener;
import org.apache.stanbol.ontologymanager.ontonet.api.session.Session;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionEvent;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionEvent.OperationType;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionListener;
import org.apache.stanbol.ontologymanager.ontonet.impl.util.OntologyUtils;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
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
@Service(OntologyProvider.class)
public class ClerezzaOntologyProvider implements OntologyProvider<TcProvider>, ScopeEventListener,
        SessionListener, OntologyCollectorListener {

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

    /**
     * Internally, the Clerezza ontology provider uses a reserved graph to store the associations between
     * ontology IDs/physical IRIs and graph names. This graph is wrapped into an {@link OntologyToTcMapper}
     * object.
     * 
     * @author alexdma
     * 
     */
    private class OntologyToTcMapper {

        /**
         * The basic terms to use for the mapping graph.
         * 
         * @author alexdma
         * 
         */
        private class Vocabulary {

            static final String ENTRY = _NS_ONTONET + "Entry";

            static final String HAS_ONTOLOGY_IRI = _NS_ONTONET + "hasOntologyIRI";

            static final String HAS_VERSION_IRI = _NS_ONTONET + "hasVersionIRI";

            static final String MAPS_TO_GRAPH = _NS_ONTONET + "mapsToGraph";

            static final String RETRIEVED_FROM = _NS_ONTONET + "retrievedFrom";

        }

        private MGraph graph;

        private UriRef graphId = new UriRef(metaGraphId);

        OntologyToTcMapper() {
            if (store == null) throw new IllegalArgumentException("TcProvider cannot be null");
            try {
                graph = store.createMGraph(graphId);
            } catch (EntityAlreadyExistsException e) {
                graph = store.getMGraph(graphId);
            }
        }

        void addMapping(OWLOntologyID ontologyReference, UriRef graphName) {
            if (ontologyReference == null || ontologyReference.isAnonymous()) throw new IllegalArgumentException(
                    "An anonymous ontology cannot be mapped. A non-anonymous ontology ID must be forged in these cases.");
            Triple tType, tMaps, tHasOiri = null, tHasViri = null;
            IRI ontologyIRI = ontologyReference.getOntologyIRI(), versionIri = ontologyReference
                    .getVersionIRI();
            UriRef entry = buildResource(ontologyReference);
            tType = new TripleImpl(entry, RDF.type, new UriRef(Vocabulary.ENTRY));
            tMaps = new TripleImpl(entry, new UriRef(Vocabulary.MAPS_TO_GRAPH), graphName);
            tHasOiri = new TripleImpl(entry, new UriRef(Vocabulary.HAS_ONTOLOGY_IRI), new UriRef(
                    ontologyIRI.toString()));
            if (versionIri != null) tHasViri = new TripleImpl(entry, new UriRef(Vocabulary.HAS_VERSION_IRI),
                    new UriRef(versionIri.toString()));
            graph.add(tType);
            graph.add(tMaps);
            if (tHasViri != null) graph.add(tHasViri);
            graph.add(tHasOiri);
        }

        /**
         * Creates an {@link OWLOntologyID} object by combining the ontologyIRI and the versionIRI, where
         * applicable, of the stored graph.
         * 
         * @param resource
         *            the ontology
         * @return
         */
        private OWLOntologyID buildOntologyId(UriRef resource) {
            // TODO desanitize?
            IRI oiri = null, viri = null;
            Iterator<Triple> it = graph.filter(resource, new UriRef(Vocabulary.HAS_ONTOLOGY_IRI), null);
            if (it.hasNext()) {
                Resource obj = it.next().getObject();
                if (obj instanceof UriRef) oiri = IRI.create(((UriRef) obj).getUnicodeString());
            } else return null; // Anonymous but versioned ontologies are not acceptable.
            it = graph.filter(resource, new UriRef(Vocabulary.HAS_VERSION_IRI), null);
            if (it.hasNext()) {
                Resource obj = it.next().getObject();
                if (obj instanceof UriRef) viri = IRI.create(((UriRef) obj).getUnicodeString());
            }
            if (viri == null) return new OWLOntologyID(oiri);
            else return new OWLOntologyID(oiri, viri);
        }

        /**
         * Creates an {@link UriRef} out of an {@link OWLOntologyID}, so it can be used as a storage key for
         * the graph.
         * 
         * @param ontologyReference
         * @return
         */
        UriRef buildResource(OWLOntologyID ontologyReference) {
            // The UriRef is of the form ontologyIRI[:::versionIRI] (TODO use something less conventional?)
            IRI ontologyIRI = ontologyReference.getOntologyIRI(), versionIri = ontologyReference
                    .getVersionIRI();
            UriRef entry = new UriRef(URIUtils.sanitizeID(ontologyIRI).toString()
                                      + ((versionIri == null) ? "" : (":::" + URIUtils.sanitizeID(versionIri)
                                              .toString())));
            return entry;
        }

        void clearMappings() {
            graph.clear();
        }

        UriRef getMapping(OWLOntologyID ontologyReference) {
            UriRef res = buildResource(ontologyReference);
            // Logical mappings first.
            Iterator<Triple> it = graph.filter(res, new UriRef(Vocabulary.MAPS_TO_GRAPH), null);
            while (it.hasNext()) {
                Resource obj = it.next().getObject();
                if (obj instanceof UriRef) return (UriRef) obj;
            }
            Literal litloc = LiteralFactory.getInstance().createTypedLiteral(
                new UriRef(res.getUnicodeString()));
            // Logical mappings failed, try physical mappings.
            it = graph.filter(null, new UriRef(Vocabulary.RETRIEVED_FROM), litloc);
            while (it.hasNext()) {
                Resource obj = it.next().getSubject();
                if (obj instanceof UriRef) return (UriRef) obj;
            }
            return null;
        }

        Set<OWLOntologyID> keys() {
            Set<OWLOntologyID> result = new HashSet<OWLOntologyID>();
            Iterator<Triple> it = graph.filter(null, new UriRef(Vocabulary.MAPS_TO_GRAPH), null);
            while (it.hasNext()) {
                NonLiteral subj = it.next().getSubject();
                if (subj instanceof UriRef) result.add(buildOntologyId((UriRef) subj));
            }
            it = graph.filter(null, new UriRef(Vocabulary.RETRIEVED_FROM), null);
            while (it.hasNext()) {
                Resource subj = it.next().getObject();
                if (subj instanceof UriRef) result.add(buildOntologyId((UriRef) subj));
                else if (subj instanceof Literal) System.out.println(((Literal) subj).getLexicalForm());
            }
            return result;
        }

        void mapLocator(IRI locator, UriRef graphName) {
            if (graphName == null) throw new IllegalArgumentException("A null graph name is not allowed.");
            // Null locator is a legal argument, will remove all locator mappings from the supplied graph
            UriRef retrieved_from = new UriRef(Vocabulary.RETRIEVED_FROM);
            Set<Triple> remove = new HashSet<Triple>();
            for (Iterator<Triple> nodes = graph.filter(graphName, null, null); nodes.hasNext();) {
                Triple t = nodes.next();
                // isOntology |= RDF.type.equals(t.getPredicate()) && OWL.Ontology.equals(t.getObject());
                if (retrieved_from.equals(t.getPredicate())) remove.add(t);
            }
            graph.removeAll(remove);
            if (locator != null) {
                Literal litloc = LiteralFactory.getInstance().createTypedLiteral(
                    new UriRef(locator.toString()));
                graph.add(new TripleImpl(graphName, retrieved_from, litloc));
            }
        }

        void removeMapping(OWLOntologyID ontologyReference) {
            Iterator<Triple> it = graph.filter(buildResource(ontologyReference), new UriRef(
                    Vocabulary.MAPS_TO_GRAPH), null);
            // I expect a concurrent modification exception here, but we'll deal with it later.
            while (it.hasNext())
                graph.remove(it.next());
        }

        void setMapping(OWLOntologyID ontologyReference, UriRef graphName) {
            removeMapping(ontologyReference);
            addMapping(ontologyReference, graphName);
        }

        Set<String> stringValues() {
            Set<String> result = new HashSet<String>();
            Iterator<Triple> it = graph.filter(null, new UriRef(Vocabulary.MAPS_TO_GRAPH), null);
            while (it.hasNext()) {
                Resource obj = it.next().getObject();
                if (obj instanceof UriRef) result.add(((UriRef) obj).getUnicodeString());
            }
            return result;
        }
    }

    private static final String _GRAPH_PREFIX_DEFAULT = "ontonet";

    private static final ImportManagementPolicy _IMPORT_POLICY_DEFAULT = ImportManagementPolicy.PRESERVE;

    private static final String _META_GRAPH_ID_DEFAULT = "org.apache.stanbol.ontologymanager.ontonet";

    private static final boolean _RESOLVE_IMPORTS_DEFAULT = true;

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
    private OfflineConfiguration offlineConfig;

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
    private Parser parser;

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
        supported = new Class<?>[] {MGraph.class, OWLOntology.class};
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
        final IRI[] offlineResources;
        if (this.offlineConfig != null) {
            List<IRI> paths = offlineConfig.getOntologySourceLocations();
            if (paths != null) offlineResources = paths.toArray(new IRI[0]);
            // There are no offline paths.
            else offlineResources = new IRI[0];
        }
        // There's no offline configuration at all.
        else offlineResources = new IRI[0];
        this.mappers = OWLOntologyManagerFactory.getMappers(offlineResources);

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
    private void fillImportsReverse(UriRef importing, List<UriRef> reverseImports, List<UriRef> level1Imports) {
        log.debug("Filling reverse imports for {}", importing);
        // Add the importing ontology first
        reverseImports.add(importing);
        if (level1Imports != null) level1Imports.add(importing);
        // Get the graph and explore its imports
        TripleCollection graph = store.getTriples(importing);
        Iterator<Triple> it = graph.filter(null, RDF.type, OWL.Ontology);
        if (!it.hasNext()) return;
        Iterator<Triple> it2 = graph.filter(it.next().getSubject(), OWL.imports, null);
        while (it2.hasNext()) {
            Resource obj = it2.next().getObject();
            if (obj instanceof UriRef) {
                UriRef key = new UriRef(getKey(IRI.create(((UriRef) obj).getUnicodeString())));
                // Check used for breaking cycles in the import graph.
                // (Unoptimized, should not use contains() for stacks.)
                if (!reverseImports.contains(key)) {
                    if (level1Imports != null) level1Imports.add(key);
                    fillImportsReverse(key, reverseImports, null);
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

    private UriRef getIRIforScope(OntologyScope scope) {
        // Use the Stanbol-internal namespace, so that the whole configuration can be ported.
        return new UriRef(_NS_STANBOL_INTERNAL + OntologyScope.shortName + "/" + scope.getID());
    }

    private UriRef getIRIforSession(Session session) {
        // Use the Stanbol-internal namespace, so that the whole configuration can be ported.
        return new UriRef(_NS_STANBOL_INTERNAL + Session.shortName + "/" + session.getID());
    }

    private UriRef getIRIforSpace(OntologySpace space) {
        // Use the Stanbol-internal namespace, so that the whole configuration can be ported.
        return new UriRef(_NS_STANBOL_INTERNAL + OntologySpace.shortName + "/" + space.getID());
    }

    @Override
    public String getKey(IRI ontologyIri) {
        ontologyIri = URIUtils.sanitizeID(ontologyIri);
        return getKey(new OWLOntologyID(ontologyIri));
    }

    @Override
    public String getKey(OWLOntologyID ontologyId) {
        UriRef ur = keymap.getMapping(ontologyId);
        log.debug("key for {} is {}", ontologyId, ur);
        return (ur == null) ? null : ur.getUnicodeString();
    }

    @Override
    public Set<String> getKeys() {
        return keymap.stringValues();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <O extends TripleCollection> O getMetaGraph(Class<O> returnType) {
        if (!TripleCollection.class.isAssignableFrom(returnType)) throw new IllegalArgumentException(
                "Only subtypes of " + TripleCollection.class + " are allowed.");
        return (O) store.getTriples(new UriRef(metaGraphId));
    }

    public OntologyNetworkConfiguration getOntologyNetworkConfiguration() {
        Map<String,Collection<String>> coreOntologies = new HashMap<String,Collection<String>>(), customOntologies = new HashMap<String,Collection<String>>();
        final TripleCollection meta = store.getTriples(new UriRef(metaGraphId));

        // Scopes first
        for (Iterator<Triple> it = meta.filter(null, RDF.type, SCOPE); it.hasNext();) { // for each scope
            Triple ta = it.next();
            NonLiteral sub = ta.getSubject();
            if (sub instanceof UriRef) {
                String s = ((UriRef) sub).getUnicodeString(), prefix = _NS_STANBOL_INTERNAL
                                                                       + OntologyScope.shortName + "/";
                if (s.startsWith(prefix)) {
                    String scopeId = s.substring(prefix.length());
                    log.info("Rebuilding scope \"{}\".", scopeId);
                    coreOntologies.put(scopeId, new LinkedList<String>());
                    customOntologies.put(scopeId, new LinkedList<String>());
                    UriRef core_ur = null, custom_ur = null;
                    Resource r;
                    // Check core space
                    Iterator<Triple> it2 = meta.filter(sub, HAS_SPACE_CORE, null);
                    if (it2.hasNext()) {
                        r = it2.next().getObject();
                        if (r instanceof UriRef) core_ur = (UriRef) r;
                    } else {
                        it2 = meta.filter(null, IS_SPACE_CORE_OF, sub);
                        if (it2.hasNext()) {
                            r = it2.next().getSubject();
                            if (r instanceof UriRef) core_ur = (UriRef) r;
                        }
                    }

                    // Check custom space
                    it2 = meta.filter(sub, HAS_SPACE_CUSTOM, null);
                    if (it2.hasNext()) {
                        r = it2.next().getObject();
                        if (r instanceof UriRef) custom_ur = (UriRef) r;
                    } else {
                        it2 = meta.filter(null, IS_SPACE_CUSTOM_OF, sub);
                        if (it2.hasNext()) {
                            r = it2.next().getSubject();
                            if (r instanceof UriRef) custom_ur = (UriRef) r;
                        }
                    }

                    // retrieve the ontologies
                    if (core_ur != null) {
                        for (it2 = meta.filter(core_ur, null, null); it2.hasNext();) {
                            Triple t = it2.next();
                            UriRef predicate = t.getPredicate();
                            if (predicate.equals(MANAGES)) {
                                if (t.getObject() instanceof UriRef) coreOntologies.get(scopeId).add(
                                    ((UriRef) t.getObject()).getUnicodeString());
                            }
                        }
                        for (it2 = meta.filter(null, null, core_ur); it2.hasNext();) {
                            Triple t = it2.next();
                            UriRef predicate = t.getPredicate();
                            if (predicate.equals(IS_MANAGED_BY)) {
                                if (t.getSubject() instanceof UriRef) coreOntologies.get(scopeId).add(
                                    ((UriRef) t.getSubject()).getUnicodeString());
                            }
                        }
                    }
                    if (custom_ur != null) {
                        for (it2 = meta.filter(custom_ur, null, null); it2.hasNext();) {
                            Triple t = it2.next();
                            UriRef predicate = t.getPredicate();
                            if (predicate.equals(MANAGES)) {
                                if (t.getObject() instanceof UriRef) customOntologies.get(scopeId).add(
                                    ((UriRef) t.getObject()).getUnicodeString());
                            }
                        }
                        for (it2 = meta.filter(null, null, custom_ur); it2.hasNext();) {
                            Triple t = it2.next();
                            UriRef predicate = t.getPredicate();
                            if (predicate.equals(IS_MANAGED_BY)) {
                                if (t.getSubject() instanceof UriRef) customOntologies.get(scopeId).add(
                                    ((UriRef) t.getSubject()).getUnicodeString());
                            }
                        }
                    }

                }
            }
        }

        // Sessions next
        Map<String,Collection<String>> sessionOntologies = new HashMap<String,Collection<String>>();
        for (Iterator<Triple> it = meta.filter(null, RDF.type, SESSION); it.hasNext();) { // for each scope
            Triple ta = it.next();
            NonLiteral sub = ta.getSubject();
            if (sub instanceof UriRef) {
                UriRef ses_ur = (UriRef) sub;
                String s = ((UriRef) sub).getUnicodeString();
                String prefix = _NS_STANBOL_INTERNAL + Session.shortName + "/";
                if (s.startsWith(prefix)) {
                    String sessionId = s.substring(prefix.length());
                    log.info("Rebuilding session \"{}\".", sessionId);
                    sessionOntologies.put(sessionId, new HashSet<String>());
                    // retrieve the ontologies
                    if (ses_ur != null) {
                        for (Iterator<Triple> it2 = meta.filter(ses_ur, null, null); it2.hasNext();) {
                            Triple t = it2.next();
                            UriRef predicate = t.getPredicate();
                            if (predicate.equals(MANAGES)) {
                                if (t.getObject() instanceof UriRef) sessionOntologies.get(sessionId).add(
                                    ((UriRef) t.getObject()).getUnicodeString());
                            }
                        }
                        for (Iterator<Triple> it2 = meta.filter(null, null, ses_ur); it2.hasNext();) {
                            Triple t = it2.next();
                            UriRef predicate = t.getPredicate();
                            if (predicate.equals(IS_MANAGED_BY)) {
                                if (t.getSubject() instanceof UriRef) sessionOntologies.get(sessionId).add(
                                    ((UriRef) t.getSubject()).getUnicodeString());
                            }
                        }
                    }
                }
            }
        }

        return new OntologyNetworkConfiguration(coreOntologies, customOntologies, sessionOntologies);
    }

    @Override
    public Set<String> getOntologyVersionKeys(IRI ontologyIRI) {
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public TcProvider getStore() {
        return store;
    }

    @Override
    public <O> O getStoredOntology(IRI reference, Class<O> returnType) {
        return getStoredOntology(reference, returnType, false);
    }

    @Override
    public <O> O getStoredOntology(IRI reference, Class<O> returnType, boolean forceMerge) {
        String key = getKey(reference);
        if (key == null || key.isEmpty()) {
            log.warn("No key found for IRI {}", reference);
            return null;
        } else return getStoredOntology(key, returnType, forceMerge);
    }

    @Override
    public <O> O getStoredOntology(String key, Class<O> returnType) {
        return getStoredOntology(key, returnType, false);
    }

    /**
     * In this implementation the identifier is the Graph Name (e.g. ontonet::blabla)
     */
    @SuppressWarnings("unchecked")
    @Override
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

        TripleCollection tc = store.getTriples(new UriRef(identifier));
        if (tc == null) return null;

        if (MGraph.class.isAssignableFrom(returnType)) {
            return returnType.cast(tc);
        } else if (OWLOntology.class.isAssignableFrom(returnType)) {
            try {
                return (O) toOWLOntology(new UriRef(identifier), forceMerge);
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
    public boolean hasOntology(IRI ontologyIri) {
        return hasOntology(new OWLOntologyID(ontologyIri));
    }

    @Override
    public boolean hasOntology(OWLOntologyID id) {
        if (id == null || id.isAnonymous()) throw new IllegalArgumentException(
                "Cannot check for an anonymous ontology.");
        return keymap.getMapping(id) != null;
    }

    @Override
    public boolean hasOntology(String key) {
        return store.listTripleCollections().contains(new UriRef(key));
    }

    /**
     * Returns <code>true</code> only if Stanbol operates in {@link OfflineMode}.
     * 
     * @return the offline state
     */
    protected final boolean isOfflineMode() {
        return offlineMode != null;
    }

    @Override
    public String loadInStore(InputStream data, String formatIdentifier, boolean force) {
        if (data == null) throw new IllegalArgumentException("No data to load ontologies from.");
        if (formatIdentifier == null || formatIdentifier.trim().isEmpty()) throw new IllegalArgumentException(
                "A non-null, non-blank format identifier is required for parsing the data stream.");

        // This method only tries the supplied format once.
        log.debug("Trying to parse data stream with format {}", formatIdentifier);
        TripleCollection rdfData = parser.parse(data, formatIdentifier);
        log.debug("SUCCESS format {}.", formatIdentifier);
        return loadInStore(rdfData, force);
    }

    @Override
    public String loadInStore(final IRI ontologyIri, String formatIdentifier, boolean force) throws IOException {
        log.debug("Loading {}", ontologyIri);
        if (ontologyIri == null) throw new IllegalArgumentException("Ontology IRI cannot be null.");

        IRI location = null;
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
                    String key = loadInStore(is, currentFormat, force);
                    // If parsing failed, an exception will be thrown before getting here, so no risk.
                    if (key != null && !key.isEmpty()) setLocatorMapping(ontologyIri, key);
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
    public String loadInStore(Object ontology, boolean force) {

        if (ontology == null) throw new IllegalArgumentException("No ontology supplied.");
        long before = System.currentTimeMillis();

        TripleCollection graph; // The final graph
        TripleCollection rdfData; // The supplied ontology converted to TripleCollection

        if (ontology instanceof OWLOntology) {
            rdfData = OWLAPIToClerezzaConverter.owlOntologyToClerezzaMGraph((OWLOntology) ontology);
        } else if (ontology instanceof TripleCollection) {
            rdfData = (TripleCollection) ontology;
        } else throw new UnsupportedOperationException(
                "This ontology provider can only accept objects assignable to " + TripleCollection.class
                        + " or " + OWLOntology.class);

        // Force is ignored for the content, but the imports?

        String s = prefix + "::"; // This will become the graph name
        IRI ontologyIri = null;

        // FIXME Profile this method. Are we getting rid of rdfData after adding its triples?

        // preferredKey should be the "guessed" ontology id
        String iri = null;
        OWLOntologyID realId = OWLUtils.guessOntologyIdentifier(rdfData);

        // String alternateId = OWLUtils.guessOntologyIdentifier(rdfData).getUnicodeString();
        if ((iri == null || iri.isEmpty()) && realId != null) {
            if (realId.getOntologyIRI() != null) iri = realId.getOntologyIRI().toString();
            if (realId.getVersionIRI() != null) iri += ":::" + realId.getVersionIRI().toString();
        }
        // else try {
        // new UriRef(iri); // Can I make a UriRef from it?
        // } catch (Exception ex) {
        // iri = OWLUtils.guessOntologyIdentifier(rdfData).getUnicodeString();
        // }

        ontologyIri = IRI.create(iri);
        while (s.endsWith("#"))
            s = s.substring(0, s.length() - 1);
        ontologyIri = URIUtils.sanitizeID(ontologyIri);
        s += ontologyIri;
        // if (s.endsWith("#")) s = s.substring(0, s.length() - 1);
        /*
         * rdfData should be an in-memory graph, so we shouldn't have a problem creating one with the
         * TcProvider and adding triples there, so that the in-memory graph is garbage-collected.
         * 
         * TODO this occupies twice as much space, which should not be necessary if the provider is the same
         * as the one used by the input source.
         */
        UriRef uriref = new UriRef(s);
        log.debug("Storing ontology with graph ID {}", uriref);

        // The policy here is to avoid copying the triples from a graph already in the store.
        // FIXME not a good policy for graphs that change
        if (!getStore().listTripleCollections().contains(uriref) || force) {
            try {
                graph = store.createMGraph(uriref);
            } catch (EntityAlreadyExistsException e) {
                if (uriref.equals(e.getEntityName())) graph = store.getMGraph(uriref);
                else graph = store.createMGraph(uriref);
            }
            graph.addAll(rdfData);
        } else {
            log.debug("Graph with ID {} already in store. Default action is to skip storage.", uriref);
            graph = store.getTriples(uriref);
        }

        // All is already sanitized by the time we get here.

        // Now do the mappings
        String mappedIds = "";
        // Discard unconventional ontology IDs with only the version IRI
        if (realId != null && realId.getOntologyIRI() != null) {
            // Versioned or not, the real ID mapping is always added
            keymap.setMapping(realId, uriref);
            mappedIds += realId;
            if (realId.getVersionIRI() != null) {
                // If the unversioned variant of a versioned ID wasn't mapped, map it too.
                OWLOntologyID unvId = new OWLOntologyID(realId.getOntologyIRI());
                if (keymap.getMapping(unvId) == null) {
                    keymap.setMapping(unvId, uriref);
                    mappedIds += realId;
                }
            }
        }
        /*
         * Make an ontology ID out of the originally supplied IRI (which might be the physical one and differ
         * from the logical one!)
         * 
         * If we find out that it differs from the "real ID", we map this one too.
         * 
         * TODO how safe is this if there was a mapping earlier?
         */
        OWLOntologyID unv = new OWLOntologyID(ontologyIri);
        if (!unv.equals(realId)) {
            keymap.setMapping(unv, uriref);
            mappedIds += " , " + unv;
        }

        // Do this AFTER registering the ontology, otherwise import cycles will cause infinite loops.
        if (resolveImports) {
            // Scan resources of type owl:Ontology, but only get the first.
            Iterator<Triple> it = graph.filter(null, RDF.type, OWL.Ontology);
            if (it.hasNext()) {
                // Scan import statements for the one owl:Ontology considered.
                Iterator<Triple> it2 = graph.filter(it.next().getSubject(), OWL.imports, null);
                while (it2.hasNext()) {
                    Resource obj = it2.next().getObject();
                    log.info("Resolving import {}", obj);
                    if (obj instanceof UriRef) try {
                        // TODO try locals first
                        if (isOfflineMode()) throw new RuntimeException(
                                "Cannot load imported ontology " + obj + " while Stanbol is in offline mode.");
                        else {
                            UriRef target = (UriRef) obj;
                            OWLOntologyID id = new OWLOntologyID(IRI.create(target.getUnicodeString()));
                            if (keymap.getMapping(id) == null) {
                                loadInStore(IRI.create(((UriRef) obj).getUnicodeString()), null, false);
                            }
                        }
                    } catch (UnsupportedFormatException e) {
                        log.warn("Failed to parse format for resource " + obj, e);
                    } catch (IOException e) {
                        log.warn("Failed to load ontology from resource " + obj, e);
                    }
                }
            }
        }

        log.debug("Ontology");
        log.debug("--- {}", mappedIds);
        log.debug("--- stored with key");
        log.debug("--- {}", s);
        log.debug("Time: {} ms", (System.currentTimeMillis() - before));
        return s;

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
        UriRef u = new UriRef(prefix + "::" + keymap.buildResource(addedOntology).getUnicodeString());

        // TODO OntologyProvider should not be aware of scopes, spaces or sessions. Move elsewhere.
        MGraph meta = getMetaGraph(MGraph.class);
        boolean hasValues = false;
        log.debug("Ontology {}", addedOntology);
        log.debug("-- is already managed by the following collectors :");
        for (Iterator<Triple> it = meta.filter(u, IS_MANAGED_BY, null); it.hasNext();) {
            hasValues = true;
            log.debug("-- {}", it.next().getObject());
        }
        for (Iterator<Triple> it = meta.filter(null, MANAGES, u); it.hasNext();) {
            hasValues = true;
            log.debug("-- {} (inverse)", it.next().getSubject());
        }
        if (!hasValues) log.debug("-- <none>");

        // Add both inverse triples. This graph has to be traversed efficiently, no need for reasoners.
        UriRef predicate1 = null, predicate2 = null;
        if (collector instanceof OntologySpace) {
            predicate1 = MANAGES;
            predicate2 = IS_MANAGED_BY;
        } else if (collector instanceof Session) {
            // TODO implement model for sessions.
            predicate1 = MANAGES;
            predicate2 = IS_MANAGED_BY;
        } else {
            log.error("Unrecognized ontology collector type {} for \"{}\". Aborting.", collector.getClass(),
                collector.getID());
            return;
        }
        synchronized (meta) {
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
        UriRef u = new UriRef(prefix + "::" + keymap.buildResource(removedOntology).getUnicodeString());

        // XXX condense the following code
        MGraph meta = getMetaGraph(MGraph.class);
        boolean badState = true;

        log.debug("Checking ({},{}) pattern", c, u);
        for (Iterator<Triple> it = meta.filter(c, null, u); it.hasNext();) {
            UriRef property = it.next().getPredicate();
            if (collector instanceof OntologySpace || collector instanceof Session) {
                if (property.equals(MANAGES)) badState = false;
            }
        }

        log.debug("Checking ({},{}) pattern", u, c);
        for (Iterator<Triple> it = meta.filter(u, null, c); it.hasNext();) {
            UriRef property = it.next().getPredicate();
            if (collector instanceof OntologySpace || collector instanceof Session) {
                if (property.equals(IS_MANAGED_BY)) badState = false;
            }
        }

        if (badState) throw new InvalidMetaGraphStateException(
                "No relationship found for ontology-collector pair {" + u + " , " + c + "}");

        synchronized (meta) {
            if (collector instanceof OntologySpace) {
                meta.remove(new TripleImpl(c, MANAGES, u));
                meta.remove(new TripleImpl(u, IS_MANAGED_BY, c));
            }
        }
    }

    @Override
    public void scopeActivated(OntologyScope scope) {}

    @Override
    public void scopeCreated(OntologyScope scope) {}

    @Override
    public void scopeDeactivated(OntologyScope scope) {}

    @Override
    public void scopeRegistered(OntologyScope scope) {
        updateScopeRegistration(scope);
    }

    @Override
    public void scopeUnregistered(OntologyScope scope) {
        updateScopeUnregistration(scope);
    }

    @Override
    public void sessionChanged(SessionEvent event) {
        if (event.getOperationType() == OperationType.CREATE) {
            updateSessionRegistration(event.getSession());
        } else if (event.getOperationType() == OperationType.KILL) {
            updateSessionUnregistration(event.getSession());
        }
    }

    @Override
    public void setImportManagementPolicy(ImportManagementPolicy policy) {
        if (policy == null) throw new IllegalArgumentException("Import management policy cannot be null.");
        importPolicyString = policy.toString();
    }

    @Override
    public void setLocatorMapping(IRI locator, String key) {
        if (key == null || key.isEmpty()) throw new IllegalArgumentException(
                "key must be non-null and non-empty.");
        if (!store.listTripleCollections().contains(new UriRef(key))) throw new IllegalArgumentException(
                "No ontology found with storage key " + key);
        if (locator == null) log
                .warn(
                    "Setting null locator for {}. This will remove all physical mappings for the corresponding graph.",
                    key);
        else log.info("Setting {} as the resource locator for ontology {}", locator, key);
        keymap.mapLocator(locator, new UriRef(key));
    }

    /**
     * 
     * @param graphName
     * @param forceMerge
     *            if set to false, the selected import management policy will be applied.
     * @return
     * @throws OWLOntologyCreationException
     */
    protected OWLOntology toOWLOntology(UriRef graphName, boolean forceMerge) throws OWLOntologyCreationException {

        OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
        // Never try to import
        mgr.addIRIMapper(new PhonyIRIMapper(Collections.<IRI> emptySet()));

        Set<UriRef> loaded = new HashSet<UriRef>();

        TripleCollection graph = store.getTriples(graphName);

        UriRef ontologyId = null;

        // Get the id of this ontology.
        Iterator<Triple> itt = graph.filter(null, RDF.type, OWL.Ontology);
        if (itt.hasNext()) {
            NonLiteral nl = itt.next().getSubject();
            if (nl instanceof UriRef) ontologyId = (UriRef) nl;
        }
        List<UriRef> revImps = new Stack<UriRef>();
        List<UriRef> lvl1 = new Stack<UriRef>();
        fillImportsReverse(graphName, revImps, lvl1);

        // If not set to merge (either by policy of by force), adopt the set import policy.
        if (!forceMerge && !ImportManagementPolicy.MERGE.equals(getImportManagementPolicy())) {
            OWLOntology o = OWLAPIToClerezzaConverter.clerezzaGraphToOWLOntology(graph, mgr);
            // TODO make it not flat.
            // Examining the reverse imports stack will flatten all imports.
            List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
            OWLDataFactory df = OWLManager.getOWLDataFactory();

            List<UriRef> listToUse;
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

            for (UriRef ref : listToUse)
                if (!loaded.contains(ref) && !ref.equals(graphName)) {
                    changes.add(new AddImport(o, df.getOWLImportsDeclaration(IRI.create(ref
                            .getUnicodeString()))));
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
            TripleCollection tempGraph = new IndexedMGraph();
            // The set of triples that will be excluded from the merge
            Set<Triple> exclusions = new HashSet<Triple>();
            // Examine all reverse imports
            for (UriRef ref : revImps)
                if (!loaded.contains(ref)) {
                    // Get the triples
                    TripleCollection imported = store.getTriples(ref);
                    // For each owl:Ontology
                    Iterator<Triple> remove = imported.filter(null, RDF.type, OWL.Ontology);
                    while (remove.hasNext()) {
                        NonLiteral subj = remove.next().getSubject();
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

    /**
     * Write registration info for a new ontology scope and its spaces.
     * 
     * @param scope
     *            the scope whose information needs to be updated.
     */
    private void updateScopeRegistration(OntologyScope scope) {
        final TripleCollection meta = store.getTriples(new UriRef(metaGraphId));
        final UriRef scopeur = getIRIforScope(scope);
        final UriRef coreur = getIRIforSpace(scope.getCoreSpace());
        final UriRef custur = getIRIforSpace(scope.getCustomSpace());
        // If this method was called after a scope rebuild, the following will have little to no effect.
        synchronized (meta) {
            // Spaces are created along with the scope, so it is safe to add their triples.
            meta.add(new TripleImpl(scopeur, RDF.type, SCOPE));
            meta.add(new TripleImpl(coreur, RDF.type, SPACE));
            meta.add(new TripleImpl(custur, RDF.type, SPACE));
            meta.add(new TripleImpl(scopeur, HAS_SPACE_CORE, coreur));
            meta.add(new TripleImpl(scopeur, HAS_SPACE_CUSTOM, custur));
            // Add inverse predicates so we can traverse the graph in both directions.
            meta.add(new TripleImpl(coreur, IS_SPACE_CORE_OF, scopeur));
            meta.add(new TripleImpl(custur, IS_SPACE_CUSTOM_OF, scopeur));
        }
        log.debug("Ontology collector information triples added for scope \"{}\".", scope);
    }

    /**
     * Remove all information on a deregistered ontology scope and its spaces.
     * 
     * @param scope
     *            the scope whose information needs to be updated.
     */
    private void updateScopeUnregistration(OntologyScope scope) {
        long before = System.currentTimeMillis();
        final TripleCollection meta = store.getTriples(new UriRef(metaGraphId));
        boolean removable = false, conflict = false;
        final UriRef scopeur = getIRIforScope(scope);
        final UriRef coreur = getIRIforSpace(scope.getCoreSpace());
        final UriRef custur = getIRIforSpace(scope.getCustomSpace());
        Set<Triple> removeUs = new HashSet<Triple>();
        for (Iterator<Triple> it = meta.filter(scopeur, null, null); it.hasNext();) {
            Triple t = it.next();
            if (RDF.type.equals(t.getPredicate())) {
                if (SCOPE.equals(t.getObject())) removable = true;
                else conflict = true;
            }
            removeUs.add(t);
        }
        if (!removable) {
            log.error("Cannot write scope deregistration to persistence:");
            log.error("-- resource {}", scopeur);
            log.error("-- is not typed as a {} in the meta-graph.", SCOPE);
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
        final TripleCollection meta = store.getTriples(new UriRef(metaGraphId));
        final UriRef sesur = getIRIforSession(session);
        // If this method was called after a session rebuild, the following will have little to no effect.
        synchronized (meta) {
            // The only essential triple to add is typing
            meta.add(new TripleImpl(sesur, RDF.type, SESSION));
        }
        log.debug("Ontology collector information triples added for session \"{}\".", sesur);
    }

    private void updateSessionUnregistration(Session session) {
        long before = System.currentTimeMillis();
        boolean removable = false, conflict = false;
        final TripleCollection meta = store.getTriples(new UriRef(metaGraphId));
        final UriRef sessionur = getIRIforSession(session);
        Set<Triple> removeUs = new HashSet<Triple>();
        for (Iterator<Triple> it = meta.filter(sessionur, null, null); it.hasNext();) {
            Triple t = it.next();
            if (RDF.type.equals(t.getPredicate())) {
                if (SESSION.equals(t.getObject())) removable = true;
                else conflict = true;
            }
            removeUs.add(t);
        }
        if (!removable) {
            log.error("Cannot write session deregistration to persistence:");
            log.error("-- resource {}", sessionur);
            log.error("-- is not typed as a {} in the meta-graph.", SESSION);
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

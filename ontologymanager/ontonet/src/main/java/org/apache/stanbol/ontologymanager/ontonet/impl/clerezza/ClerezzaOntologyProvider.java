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

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

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
import org.apache.stanbol.ontologymanager.ontonet.api.collector.ImportManagementPolicy;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyProvider;
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
 * {@link TcProvider} used.
 * 
 * @author alexdma
 * 
 */
@Component(immediate = true, metatype = true)
@Service(OntologyProvider.class)
public class ClerezzaOntologyProvider implements OntologyProvider<TcProvider> {

    /**
     * Internally, the Clerezza ontology provider uses a reserved graph to store the associations between
     * ontology IDs/physical IRIs and graph names.
     * 
     * @author alessandro
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

            static final String _BASE_VOCAB = "http://stanbol.apache.org/ontology/ontonet/meta#";

            static final String ENTRY = _BASE_VOCAB + "Entry";

            static final String HAS_ONTOLOGY_IRI = _BASE_VOCAB + "hasOntologyIRI";

            static final String HAS_VERSION_IRI = _BASE_VOCAB + "hasVersionIRI";

            static final String MAPS_TO_GRAPH = _BASE_VOCAB + "mapsToGraph";

        }

        private MGraph graph;

        private UriRef graphId = new UriRef(OntologyToTcMapper.class.getCanonicalName());

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

        private UriRef buildResource(OWLOntologyID ontologyReference) {
            IRI ontologyIRI = ontologyReference.getOntologyIRI(), versionIri = ontologyReference
                    .getVersionIRI();
            UriRef entry = new UriRef(ontologyIRI.toString()
                                      + ((versionIri == null) ? "" : ("/" + versionIri.toString())));
            return entry;
        }

        private OWLOntologyID buildOntologyId(UriRef resource) {
            IRI oiri = null, viri = null;
            Iterator<Triple> it = graph.filter(resource, new UriRef(Vocabulary.HAS_ONTOLOGY_IRI), null);
            if (it.hasNext()) {
                Resource obj = it.next().getObject();
                if (obj instanceof UriRef) oiri = IRI.create(((UriRef) obj).getUnicodeString());
            }
            it = graph.filter(resource, new UriRef(Vocabulary.HAS_VERSION_IRI), null);
            if (it.hasNext()) {
                Resource obj = it.next().getObject();
                if (obj instanceof UriRef) viri = IRI.create(((UriRef) obj).getUnicodeString());
            }
            if (viri == null) return new OWLOntologyID(oiri);
            else return new OWLOntologyID(oiri, viri);
        }

        void clearMappings() {
            graph.clear();
        }

        UriRef getMapping(OWLOntologyID ontologyReference) {
            Iterator<Triple> it = graph.filter(buildResource(ontologyReference), new UriRef(
                    Vocabulary.MAPS_TO_GRAPH), null);
            while (it.hasNext()) {
                Resource obj = it.next().getObject();
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
            return result;
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
        reverseImports.add(importing);
        if (level1Imports != null) level1Imports.add(importing);

        // Get the graph and explore its imports
        TripleCollection graph = store.getTriples(importing);
        Iterator<Triple> it = graph.filter(null, RDF.type, OWL.Ontology);
        if (it.hasNext()) {
            Iterator<Triple> it2 = graph.filter(it.next().getSubject(), OWL.imports, null);
            while (it2.hasNext()) {
                Resource obj = it2.next().getObject();
                if (obj instanceof UriRef) {
                    UriRef key = new UriRef(getKey(IRI.create(((UriRef) obj).getUnicodeString())));
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

    @Override
    public String getKey(IRI ontologyIri) {
        ontologyIri = URIUtils.sanitizeID(ontologyIri);
        UriRef ur = keymap.getMapping(new OWLOntologyID(ontologyIri));
        log.debug("key for {} is {}", ontologyIri, ur);
        return (ur == null) ? null : ur.getUnicodeString();
    }

    @Override
    public Set<String> getKeys() {
        return keymap.stringValues();
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
    public String loadInStore(InputStream data, String formatIdentifier, String preferredKey, boolean force) {

        if (data == null) throw new IllegalArgumentException("No data to load ontologies from.");

        // Force is ignored for the content, but the imports?

        // Get sorted list of supported formats, or use specified one.
        Collection<String> formats;
        if (formatIdentifier == null || "".equals(formatIdentifier.trim())) formats = OntologyUtils
                .getPreferredSupportedFormats(parser.getSupportedFormats());
        else formats = Collections.singleton(formatIdentifier);

        // Try each format, return on the first one that was parsed.
        for (String format : formats) {
            try {
                TripleCollection rdfData = parser.parse(data, format);
                return loadInStore(rdfData, preferredKey, force);
            } catch (UnsupportedFormatException e) {
                log.debug("Unsupported format format {}. Trying next one.", format);
                continue;
            } catch (Exception e) {
                log.debug("Parsing format " + format + " failed. Trying next one.", e);
                continue;
            }
        }
        // No parser worked, return null.
        log.error("All parsers failed, giving up.");
        return null;
    }

    @Override
    public String loadInStore(final IRI ontologyIri,
                              String formatIdentifier,
                              String preferredKey,
                              boolean force) throws IOException, UnsupportedFormatException {
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

        Collection<String> formats;
        if (formatIdentifier == null || "".equals(formatIdentifier.trim())) formats = OntologyUtils
                .getPreferredSupportedFormats(parser.getSupportedFormats());
        else formats = Collections.singleton(formatIdentifier);
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
                    return loadInStore(is, currentFormat, ontologyIri.toString(), force);
                }
            } catch (UnsupportedFormatException e) {
                log.debug("Unsupported format format {}. Trying next one.", currentFormat);
                continue;
            } catch (Exception e) {
                log.debug("Parsing format " + currentFormat + " failed. Trying next one.", e);
                continue;
            }
        }

        // No parser worked, return null.
        log.error("All parsers failed, giving up.");
        return null;
    }

    @Override
    public String loadInStore(Object ontology, String preferredKey, boolean force) {

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
        String iri = preferredKey;
        OWLOntologyID realId = OWLUtils.guessOWLOntologyID(rdfData);

        // String alternateId = OWLUtils.guessOntologyIdentifier(rdfData).getUnicodeString();
        if ((iri == null || iri.isEmpty()) && realId != null) {
            if (realId.getOntologyIRI() != null) iri = realId.getOntologyIRI().toString();
            if (realId.getVersionIRI() != null) iri += ":::" + realId.getVersionIRI().toString();
        } else try {
            new UriRef(iri); // Can I make a UriRef from it?
        } catch (Exception ex) {
            iri = OWLUtils.guessOntologyIdentifier(rdfData).getUnicodeString();
        }

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

        if (resolveImports) {
            // Scan resources of type owl:Ontology, but only get the first.
            Iterator<Triple> it = graph.filter(null, RDF.type, OWL.Ontology);
            if (it.hasNext()) {
                // Scan import statements for the one owl:Ontology considered.
                Iterator<Triple> it2 = graph.filter(it.next().getSubject(), OWL.imports, null);
                while (it2.hasNext()) {
                    Resource obj = it2.next().getObject();
                    if (obj instanceof UriRef) try {
                        loadInStore(IRI.create(((UriRef) obj).getUnicodeString()), null, null, false);
                    } catch (UnsupportedFormatException e) {
                        log.warn("Failed to parse format for resource " + obj, e);
                    } catch (IOException e) {
                        log.warn("Failed to load ontology from resource " + obj, e);
                    }
                }
            }
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
        log.debug("Ontology \n\t\t{}\n\tstored with key\n\t\t{}", mappedIds, s);
        log.debug("Time: {} ms", (System.currentTimeMillis() - before));
        return s;

    }

    @Override
    public void setImportManagementPolicy(ImportManagementPolicy policy) {
        if (policy == null) throw new IllegalArgumentException("Import management policy cannot be null.");
        importPolicyString = policy.toString();
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

    @Override
    public boolean hasOntology(IRI ontologyIri) {
        return hasOntology(new OWLOntologyID(ontologyIri));
    }
}

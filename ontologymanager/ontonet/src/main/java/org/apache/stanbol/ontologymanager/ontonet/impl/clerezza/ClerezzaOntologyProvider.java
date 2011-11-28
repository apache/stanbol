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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.UnsupportedFormatException;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.stanboltools.offline.OfflineMode;
import org.apache.stanbol.ontologymanager.ontonet.api.OfflineConfiguration;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.ontonet.impl.util.OntologyUtils;
import org.apache.stanbol.owl.OWLOntologyManagerFactory;
import org.apache.stanbol.owl.PhonyIRIMapper;
import org.apache.stanbol.owl.transformation.OWLAPIToClerezzaConverter;
import org.apache.stanbol.owl.util.OWLUtils;
import org.apache.stanbol.owl.util.URIUtils;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clerezza-based ontology cache implementation. Whether it is persistent or in-memory depends on the
 * {@link TcProvider} used.
 * 
 * @author alexdma
 * 
 */
@Component(immediate = true, metatype = false)
@Service(OntologyProvider.class)
public class ClerezzaOntologyProvider implements OntologyProvider<TcProvider> {

    private static final String _GRAPH_PREFIX_DEFAULT = "ontonet";

    private static final boolean _RESOLVE_IMPORTS_DEFAULT = true;

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

    /**
     * Maps ontology IRIs (logical or physical if the ontology is anonymous) to Clerezza storage keys i.e.
     * graph names.
     */
    private Map<IRI,String> ontologyIdsToKeys;

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
        ontologyIdsToKeys = new HashMap<IRI,String>();
    }

    public ClerezzaOntologyProvider(TcProvider store, OfflineConfiguration offline, Parser parser) {
        this();

        this.offlineConfig = offline;
        // Re-assign the TcManager if no store is supplied
        if (store == null) store = TcManager.getInstance();
        this.store = store;
        if (this.tcManager == null) this.tcManager = TcManager.getInstance();
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
        prefix = (String) (configuration.get(OntologyProvider.GRAPH_PREFIX));
        if (prefix == null) prefix = _GRAPH_PREFIX_DEFAULT; // Should be already assigned though

        try {
            resolveImports = (Boolean) (configuration.get(OntologyProvider.RESOLVE_IMPORTS));
        } catch (Exception ex) {
            resolveImports = _RESOLVE_IMPORTS_DEFAULT; // Should be already assigned though
        }

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

    private void fillImportsReverse(UriRef importing, List<UriRef> reverseImports) {
        log.debug("Filling reverse imports for {}", importing);
        reverseImports.add(importing);
        TripleCollection graph = store.getTriples(importing);
        Iterator<Triple> it = graph.filter(null, RDF.type, OWL.Ontology);
        if (it.hasNext()) {
            Iterator<Triple> it2 = graph.filter(it.next().getSubject(), OWL.imports, null);
            while (it2.hasNext()) {
                Resource obj = it2.next().getObject();
                if (obj instanceof UriRef) fillImportsReverse(
                    new UriRef(getKey(IRI.create(((UriRef) obj).getUnicodeString()))
                    // prefix + "::" + ((UriRef) obj).getUnicodeString()
                    ), reverseImports);
            }
        }
    }

    @Override
    public String getKey(IRI ontologyIri) {
        ontologyIri = URIUtils.sanitizeID(ontologyIri);
        log.debug("key for {} is {}", ontologyIri, ontologyIdsToKeys.get(ontologyIri));
        return ontologyIdsToKeys.get(ontologyIri);
    }

    @Override
    public Set<String> getKeys() {
        return new HashSet<String>(ontologyIdsToKeys.values());
    }

    @Override
    public TcProvider getStore() {
        return store;
    }

    @Override
    public <O> O getStoredOntology(IRI reference, Class<O> returnType) {
        return getStoredOntology(getKey(reference), returnType);
    }

    @Override
    public <O> O getStoredOntology(IRI reference, Class<O> returnType, boolean merge) {
        return getStoredOntology(getKey(reference), returnType, merge);
    }

    @Override
    public <O> O getStoredOntology(String key, Class<O> returnType) {
        // TODO default to false? Or by set policy?
        return getStoredOntology(key, returnType, false);
    }

    /**
     * In this implementation the identifier is the Graph Name (e.g. ontonet::blabla)
     */
    @SuppressWarnings("unchecked")
    @Override
    public <O> O getStoredOntology(String identifier, Class<O> returnType, boolean merge) {
        if (identifier == null) throw new IllegalArgumentException("Identifier cannot be null");
        if (returnType == null) {
            returnType = (Class<O>) OWLOntology.class;
            log.warn("No return type given for ontologies. Will return a {}", returnType);
        }
        boolean canDo = false;
        for (Class<?> clazz : getSupportedReturnTypes())
            if (clazz.isAssignableFrom(returnType)) {
                canDo = true;
                break;
            }
        if (!canDo) throw new UnsupportedOperationException(
                "Return type " + returnType
                        + " is not allowed in this implementation. Only allowed return types are "
                        + supported);

        TripleCollection tc = store.getTriples(new UriRef(identifier));

        if (MGraph.class.isAssignableFrom(returnType)) {
            return returnType.cast(tc);
        } else if (OWLOntology.class.isAssignableFrom(returnType)) {
            try {
                return (O) toOWLOntology(new UriRef(identifier), merge);
            } catch (OWLOntologyCreationException e) {
                log.error("Failed to return stored ontology " + identifier + " as type " + returnType, e);
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<?>[] getSupportedReturnTypes() {
        return supported;
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

        MGraph graph;
        TripleCollection rdfData;

        if (ontology instanceof OWLOntology) {
            rdfData = OWLAPIToClerezzaConverter.owlOntologyToClerezzaMGraph((OWLOntology) ontology);
        } else if (ontology instanceof TripleCollection) {
            rdfData = (TripleCollection) ontology;
        } else throw new UnsupportedOperationException(
                "This ontology provider can only accept objects assignable to " + TripleCollection.class
                        + " or " + OWLOntology.class);

        // Force is ignored for the content, but the imports?

        String s = prefix + "::";
        IRI ontologyIri = null;

        boolean loaded = false;

        // FIXME are we getting rid of rdfData after adding its triples?
        String iri = preferredKey;
        if (iri == null || iri.isEmpty()) iri = OWLUtils.guessOntologyIdentifier(rdfData).getUnicodeString();
        else try {
            new UriRef(iri);
        } catch (Exception ex) {
            iri = OWLUtils.guessOntologyIdentifier(rdfData).getUnicodeString();
        }

        ontologyIri = IRI.create(iri);
        ontologyIri = URIUtils.sanitizeID(ontologyIri);
        s += ontologyIri;
        // if (s.endsWith("#")) s = s.substring(0, s.length() - 1);
        /*
         * rdfData should be a SimpleGraph, so we shouldn't have a problem creating one with the TcProvider
         * and adding triples there, so that the SimpleGraph is garbage-collected.
         */
        {
            UriRef uriref = new UriRef(s);
            try {
                graph = store.createMGraph(uriref);
            } catch (EntityAlreadyExistsException e) {
                if (uriref.equals(e.getEntityName())) graph = store.getMGraph(uriref);
                else graph = store.createMGraph(uriref);
            }
            graph.addAll(rdfData);
        }
        if (resolveImports) {
            Iterator<Triple> it = graph.filter(null, RDF.type, OWL.Ontology);
            if (it.hasNext()) {
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

        loaded = true;

        if (loaded) {
            // All is already sanitized by the time we get here.
            ontologyIdsToKeys.put(ontologyIri, s);
            log.debug("Ontology \n\t\t{}\n\tstored with key\n\t\t{}", ontologyIri, s);
            log.debug("Time: {} ms", (System.currentTimeMillis() - before));
            return s;
        } else return null;
    }

    protected OWLOntology toOWLOntology(UriRef graphName, boolean merge) throws OWLOntologyCreationException {

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

        fillImportsReverse(graphName, revImps);

        if (!merge) {
            OWLOntology o = OWLAPIToClerezzaConverter.clerezzaGraphToOWLOntology(graph, mgr);
            // TODO make it not flat.
            // Examining the reverse imports stack will flatten all imports.
            List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
            OWLDataFactory df = OWLManager.getOWLDataFactory();
            for (UriRef ref : revImps)
                if (!loaded.contains(ref) && !ref.equals(graphName)) {
                    changes.add(new AddImport(o, df.getOWLImportsDeclaration(IRI.create(ref
                            .getUnicodeString()))));
                    loaded.add(ref);
                }
            o.getOWLOntologyManager().applyChanges(changes);
            return o;
        } else {
            // More efficient / brutal implementation.

            // If there is just the root ontology, convert it straight away.
            if (revImps.size() == 1 && revImps.contains(graphName)) {
                OWLOntology o = OWLAPIToClerezzaConverter.clerezzaGraphToOWLOntology(graph, mgr);
                return o;
            }

            // FIXME when there's more than one ontology, this way of merging them seems inefficient...
            TripleCollection tempGraph = new SimpleMGraph();
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
}

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
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.EntityAlreadyExistsException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.access.TcProvider;
import org.apache.clerezza.rdf.core.access.WeightedTcProvider;
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
import org.apache.stanbol.owl.OWLOntologyManagerFactory;
import org.apache.stanbol.owl.PhonyIRIMapper;
import org.apache.stanbol.owl.transformation.OWLAPIToClerezzaConverter;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clerezza-based ontology cache implementation. Whether it is persistent or in-memory depends on the
 * {@link WeightedTcProvider} used.
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
    private OfflineConfiguration offline;

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

        this.offline = offline;
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
        // Parse configuration.
        prefix = (String) (configuration.get(OntologyProvider.GRAPH_PREFIX));
        if (prefix == null) prefix = _GRAPH_PREFIX_DEFAULT; // Should be already assigned though

        try {
            resolveImports = (Boolean) (configuration.get(OntologyProvider.RESOLVE_IMPORTS));
        } catch (Exception ex) {
            resolveImports = _RESOLVE_IMPORTS_DEFAULT; // Should be already assigned though
        }

        final IRI[] offlineResources;
        if (this.offline != null) {
            List<IRI> paths = offline.getOntologySourceLocations();
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
        reverseImports.add(importing);
        TripleCollection graph = store.getTriples(importing);
        Iterator<Triple> it = graph.filter(null, RDF.type, OWL.Ontology);
        if (it.hasNext()) {
            Iterator<Triple> it2 = graph.filter(it.next().getSubject(), OWL.imports, null);
            while (it2.hasNext()) {
                Resource obj = it2.next().getObject();
                if (obj instanceof UriRef) fillImportsReverse(
                    new UriRef(prefix + "::" + ((UriRef) obj).getUnicodeString()), reverseImports);
            }
        }
    }

    @Override
    public Set<String> getOntologyReferences() {
        Set<String> result = new HashSet<String>();
        for (UriRef u : store.listTripleCollections())
            result.add(u.getUnicodeString());
        return result;
    }

    @Override
    public TcProvider getStore() {
        return store;
    }

    @Override
    public Object getStoredOntology(String identifier, Class<?> returnType) {
        if (returnType == null) {
            returnType = OWLOntology.class;
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
            return toOWLOntology(new UriRef(identifier));
        }

        return null;
    }

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
    public String loadInStore(InputStream data, String formatIdentifier, boolean force) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String loadInStore(IRI ontologyIri, String formatIdentifier, boolean force) throws IOException,
                                                                                      UnsupportedFormatException {
        log.debug("Loading {}", ontologyIri);
        if (ontologyIri == null) throw new IllegalArgumentException("Ontology IRI cannot be null.");

        String s = prefix + "::" + ontologyIri.toString();

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

        boolean loaded = false;

        Set<String> formats;
        if (formatIdentifier == null || "".equals(formatIdentifier.trim())) formats = parser
                .getSupportedFormats();
        else formats = Collections.singleton(formatIdentifier);
        for (String format : formats) {
            try {
                final URLConnection con = location.toURI().toURL().openConnection();
                con.setRequestProperty("Accept", format);
                final InputStream is = con.getInputStream();
                if (is != null) {
                    MGraph graph;
                    TripleCollection rdfData = parser.parse(is, format);
                    if (rdfData instanceof MGraph) {
                        graph = (MGraph) rdfData;
                    } else {
                        UriRef uriref = new UriRef(s);
                        try {
                            graph = store.createMGraph(uriref);
                            // graph = new SimpleMGraph();
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
                                if (obj instanceof UriRef) loadInStore(
                                    IRI.create(((UriRef) obj).getUnicodeString()), null, false);
                            }
                        }

                    }

                    loaded = true;
                    break;
                }
            } catch (UnsupportedFormatException e) {
                log.debug("Parsing format {} failed.", format);
                continue;
            } catch (Exception e) {
                log.debug("Parsing format {} failed.", format);
                continue;
            }
        }
        if (loaded) return s;
        else return null;
    }

    protected OWLOntology toOWLOntology(UriRef graphName) {

        OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
        // Never try to import
        mgr.addIRIMapper(new PhonyIRIMapper(Collections.<IRI> emptySet()));
        List<UriRef> revImps = new Stack<UriRef>();
        fillImportsReverse(graphName, revImps);
        Set<UriRef> loaded = new HashSet<UriRef>();

        for (UriRef ref : revImps) {
            if (!loaded.contains(ref)) {
                TripleCollection tc = store.getTriples(ref);
                OWLAPIToClerezzaConverter.clerezzaGraphToOWLOntology(tc, mgr);
                loaded.add(ref);
            }
        }

        TripleCollection graph = store.getTriples(graphName);
        return OWLAPIToClerezzaConverter.clerezzaGraphToOWLOntology(graph, mgr);
    }
}

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
package org.apache.stanbol.ontologymanager.ontonet.impl.registry;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.RegistryContentException;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.RegistryContentListener;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.RegistryItemFactory;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.RegistryManager;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.Library;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.Registry;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.RegistryItem;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.RegistryItem.Type;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.RegistryOntology;
import org.apache.stanbol.ontologymanager.ontonet.impl.registry.cache.RegistryUtils;
import org.apache.stanbol.ontologymanager.ontonet.xd.vocabulary.CODOVocabulary;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLAxiomVisitor;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLAxiomVisitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of the registry manager, that listens to requests on its referenced resources and
 * issues loading requests accordingly.
 */
@Component(immediate = true, metatype = true)
@Service(RegistryManager.class)
public class RegistryManagerImpl implements RegistryManager, RegistryContentListener {

    private static final boolean _LAZY_LOADING_DEFAULT = false;

    private static final OWLClass cRegistryLibrary, cOntology;

    private static final OWLObjectProperty hasPart, hasOntology, isPartOf, isOntologyOf;

    static {
        OWLDataFactory factory = OWLManager.getOWLDataFactory();
        cOntology = factory.getOWLClass(IRI.create(CODOVocabulary.CODK_Ontology));
        cRegistryLibrary = factory.getOWLClass(IRI.create(CODOVocabulary.CODD_OntologyLibrary));
        isPartOf = factory.getOWLObjectProperty(IRI.create(CODOVocabulary.PARTOF_IsPartOf));
        isOntologyOf = factory.getOWLObjectProperty(IRI.create(CODOVocabulary.ODPM_IsOntologyOf));
        hasPart = factory.getOWLObjectProperty(IRI.create(CODOVocabulary.PARTOF_HasPart));
        hasOntology = factory.getOWLObjectProperty(IRI.create(CODOVocabulary.ODPM_HasOntology));
    }

    @Property(name = RegistryManager.CACHING_POLICY, options = {
                                                                @PropertyOption(value = '%'
                                                                                        + RegistryManager.CACHING_POLICY
                                                                                        + ".option.registry", name = "registry"),
                                                                @PropertyOption(value = '%'
                                                                                        + RegistryManager.CACHING_POLICY
                                                                                        + ".option.all", name = "all")}, value = "all")
    private String cachingPolicyString;

    @Property(name = RegistryManager.LAZY_LOADING, boolValue = _LAZY_LOADING_DEFAULT)
    private boolean lazyLoading = _LAZY_LOADING_DEFAULT;

    /* Maps registries to libraries */
    private Map<IRI,Set<IRI>> libraryIndex = new HashMap<IRI,Set<IRI>>();

    @Property(name = RegistryManager.REGISTRY_LOCATIONS, cardinality = 1000)
    private String[] locations;

    private Logger log = LoggerFactory.getLogger(getClass());

    /* Maps libraries to ontologies */
    private Map<IRI,Set<IRI>> ontologyIndex = new HashMap<IRI,Set<IRI>>();

    private Map<IRI,RegistryItem> population = new TreeMap<IRI,RegistryItem>();

    private Set<IRI> registries = new HashSet<IRI>();

    private RegistryItemFactory riFactory;

    /**
     * This default constructor is <b>only</b> intended to be used by the OSGI environment with Service
     * Component Runtime support.
     * <p>
     * DO NOT USE to manually create instances - the RegistryManagerImpl instances do need to be configured!
     * YOU NEED TO USE {@link #RegistryManagerImpl(Dictionary)} or its overloads, to parse the configuration
     * and then initialise the rule store if running outside an OSGI environment.
     */
    public RegistryManagerImpl() {
        riFactory = new RegistryItemFactoryImpl();
    }

    /**
     * To be invoked by non-OSGi environments.
     * 
     * @param configuration
     */
    public RegistryManagerImpl(Dictionary<String,Object> configuration) {
        this();
        activate(configuration);
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
        try {
            lazyLoading = (Boolean) (configuration.get(RegistryManager.LAZY_LOADING));
        } catch (Exception ex) {
            lazyLoading = _LAZY_LOADING_DEFAULT;
        }
        locations = (String[]) configuration.get(RegistryManager.REGISTRY_LOCATIONS);
        if (locations == null) locations = new String[] {};
        // TODO manage enum constants for caching policy.

        OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
        // Load registries
        Set<OWLOntology> regOnts = new HashSet<OWLOntology>();
        for (String loc : locations) {
            try {
                regOnts.add(mgr.loadOntology(IRI.create(loc)));
            } catch (OWLOntologyAlreadyExistsException e) {
                log.info("Skipping cached ontology {}.", e.getOntologyID());
                continue;
            } catch (OWLOntologyCreationException e) {
                log.warn("Failed to load ontology " + loc + " - Skipping...", e);
                continue;
            }
        }
        // Build the model!
        createModel(regOnts);
    }

    @Override
    public void addRegistry(Registry registry) {
        try {
            population.put(registry.getIRI(), registry);
            registries.add(registry.getIRI());
            updateLocations();
        } catch (Exception e) {
            log.error("Failed to add ontology registry.", e);
        }
    }

    @Override
    public void clearRegistries() {
        for (IRI id : registries)
            if (registries.remove(id)) population.remove(id);
        updateLocations();
    }

    @Override
    public Set<Registry> createModel(Set<OWLOntology> registryOntologies) {

        Set<Registry> results = new HashSet<Registry>();
        // Reset population
        population.clear();

        // Build the transitive imports closure of the union.
        Set<OWLOntology> closure = new HashSet<OWLOntology>();
        for (OWLOntology rego : registryOntologies)
            closure.addAll(rego.getOWLOntologyManager().getImportsClosure(rego));

        final Map<IRI,int[]> candidateTypes = new HashMap<IRI,int[]>();

        OWLAxiomVisitor v = new OWLAxiomVisitorAdapter() {

            private int[] checkScores(IRI key) {
                int[] scores;
                if (candidateTypes.containsKey(key)) scores = candidateTypes.get(key);
                else {
                    scores = new int[] {0, 0};
                    candidateTypes.put(key, scores);
                }
                return scores;
            }

            @Override
            public void visit(OWLClassAssertionAxiom axiom) {
                OWLIndividual ind = axiom.getIndividual();
                if (ind.isAnonymous()) return;
                IRI iri = ind.asOWLNamedIndividual().getIRI();
                int[] scores = checkScores(iri);

                OWLClassExpression type = axiom.getClassExpression();
                if (cRegistryLibrary.equals(type)) {
                    scores[0]++;
                } else if (cOntology.equals(type)) {
                    scores[1]++;
                }

            }

            @Override
            public void visit(OWLObjectPropertyAssertionAxiom axiom) {
                OWLObjectPropertyExpression prop = axiom.getProperty();

                if (hasOntology.equals(prop)) {
                    IRI iri;
                    OWLIndividual ind = axiom.getSubject();
                    if (!ind.isAnonymous()) {
                        iri = ind.asOWLNamedIndividual().getIRI();
                        checkScores(iri)[0]++;
                    }
                    ind = axiom.getObject();
                    if (!ind.isAnonymous()) {
                        iri = ind.asOWLNamedIndividual().getIRI();
                        checkScores(iri)[1]++;
                    }
                } else if (isOntologyOf.equals(prop)) {
                    IRI iri;
                    OWLIndividual ind = axiom.getSubject();
                    if (!ind.isAnonymous()) {
                        iri = ind.asOWLNamedIndividual().getIRI();
                        checkScores(iri)[1]++;
                    }
                    ind = axiom.getObject();
                    if (!ind.isAnonymous()) {
                        iri = ind.asOWLNamedIndividual().getIRI();
                        checkScores(iri)[0]++;
                    }
                }

            }

        };

        // First pass to determine the types.
        for (OWLOntology o : closure)
            for (OWLAxiom ax : o.getAxioms())
                ax.accept(v);

        // Then populate on the registry
        OWLDataFactory df = OWLManager.getOWLDataFactory();
        for (IRI iri : candidateTypes.keySet()) {
            int[] scores = candidateTypes.get(iri);
            if (scores != null && (scores[0] > 0 || scores[1] > 0)) {
                if (scores[0] > 0 && scores[1] == 0) population.put(iri,
                    riFactory.createLibrary(df.getOWLNamedIndividual(iri)));
                else if (scores[0] == 0 && scores[1] > 0) population.put(iri,
                    riFactory.createRegistryOntology(df.getOWLNamedIndividual(iri)));
            } else log.warn("Unable to determine type for registry item {}", iri);
        }

        for (OWLOntology oReg : registryOntologies) {
            try {
                results.add(populateRegistry(oReg));
            } catch (RegistryContentException e) {
                log.error("An error occurred while populating an ontology registry.", e);
            }
        }
        return results;
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        lazyLoading = _LAZY_LOADING_DEFAULT;
        locations = null;
        log.info("in {} deactivate with context {}", getClass(), context);
    }

    @Override
    public Set<Library> getLibraries(IRI ontologyID) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<Registry> getRegistries() {
        Set<Registry> results = new HashSet<Registry>();
        for (IRI key : population.keySet()) {
            RegistryItem item = population.get(key);
            if (item instanceof Registry) results.add((Registry) item);
        }
        return results;
    }

    @Override
    public Set<Registry> getRegistries(IRI libraryID) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Registry getRegistry(IRI id) {
        RegistryItem item = population.get(id);
        return item != null && item instanceof Registry ? (Registry) item : null;
    }

    @Override
    public boolean isLazyLoading() {
        return lazyLoading;
    }

    protected Library populateLibrary(OWLNamedIndividual ind, Set<OWLOntology> registries) throws RegistryContentException {
        IRI id = ind.getIRI();
        RegistryItem lib = null;
        if (population.containsKey(id)) {
            // We are not allowing multityping either.
            lib = population.get(id);
            if (!(lib instanceof Library)) throw new RegistryContentException(
                    "Inconsistent multityping: for item " + id + " : {" + Library.class + ", "
                            + lib.getClass() + "}");
        } else {
            lib = riFactory.createLibrary(ind.asOWLNamedIndividual());
            try {
                population.put(lib.getIRI(), lib);
            } catch (Exception e) {
                log.error("Invalid identifier for library item " + lib, e);
                return null;
            }
        }
        // EXIT nodes.
        Set<OWLIndividual> ronts = new HashSet<OWLIndividual>();
        for (OWLOntology o : registries)
            ronts.addAll(ind.getObjectPropertyValues(hasOntology, o));
        for (OWLIndividual iont : ronts) {
            if (iont.isNamed()) lib.addChild(populateOntology(iont.asOWLNamedIndividual(), registries));
        }
        return (Library) lib;
    }

    protected RegistryOntology populateOntology(OWLNamedIndividual ind, Set<OWLOntology> registries) throws RegistryContentException {
        IRI id = ind.getIRI();
        RegistryItem ront = null;
        if (population.containsKey(id)) {
            // We are not allowing multityping either.
            ront = population.get(id);
            if (!(ront instanceof RegistryOntology)) throw new RegistryContentException(
                    "Inconsistent multityping: for item " + id + " : {" + RegistryOntology.class + ", "
                            + ront.getClass() + "}");
        } else {
            ront = riFactory.createRegistryOntology(ind);
            try {
                population.put(ront.getIRI(), ront);
            } catch (Exception e) {
                log.error("Invalid identifier for library item " + ront, e);
                return null;
            }
        }
        // EXIT nodes.
        Set<OWLIndividual> libs = new HashSet<OWLIndividual>();
        for (OWLOntology o : registries)
            libs.addAll(ind.getObjectPropertyValues(isOntologyOf, o));
        for (OWLIndividual ilib : libs) {
            if (ilib.isNamed()) ront.addParent(populateLibrary(ilib.asOWLNamedIndividual(), registries));
        }
        return (RegistryOntology) ront;
    }

    protected Registry populateRegistry(OWLOntology registry) throws RegistryContentException {

        Registry reg = riFactory.createRegistry(registry);
        Set<OWLOntology> closure = registry.getOWLOntologyManager().getImportsClosure(registry);

        // Just scan all individuals. Recurse in case the registry imports more registries.
        for (OWLIndividual ind : registry.getIndividualsInSignature(true)) {
            // We do not allow anonymous registry items.
            if (ind.isAnonymous()) continue;
            RegistryItem item = null;
            // IRI id = ind.asOWLNamedIndividual().getIRI();
            Type t = RegistryUtils.getType(ind, closure);
            if (t == null) {
                log.warn("Undetermined type for registry ontology individual {}", ind);
                continue;
            }
            switch (t) {
                case LIBRARY:
                    // Create the library and attach to parent and children
                    item = populateLibrary(ind.asOWLNamedIndividual(), closure);
                    reg.addChild(item);
                    item.addRegistryContentListener(this);
                    break;
                case ONTOLOGY:
                    // Create the ontology and attach to parent
                    item = populateOntology(ind.asOWLNamedIndividual(), closure);
                    item.addRegistryContentListener(this);
                    // We don't know where to attach it within this method.
                    break;
                default:
                    break;
            }
        }
        try {
            reg.addRegistryContentListener(this);
            population.put(reg.getIRI(), reg);
        } catch (Exception e) {
            log.error("Invalid identifier for library item " + reg, e);
            return null;
        }
        return reg;
    }

    @Override
    public void registryContentRequested(RegistryItem requestTarget) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeRegistry(IRI registryId) {
        registries.remove(registryId);
        updateLocations();
    }

    @Override
    public void setLazyLoading(boolean lazy) {
        this.lazyLoading = lazy;
    }

    protected synchronized void updateLocations() {
        Set<IRI> locations = Collections.unmodifiableSet(registries);
        this.locations = locations.toArray(new String[0]);
    }

}

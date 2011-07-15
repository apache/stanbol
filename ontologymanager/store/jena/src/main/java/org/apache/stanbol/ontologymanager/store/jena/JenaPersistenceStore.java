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
package org.apache.stanbol.ontologymanager.store.jena;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.ontologymanager.store.api.JenaPersistenceProvider;
import org.apache.stanbol.ontologymanager.store.api.PersistenceStore;
import org.apache.stanbol.ontologymanager.store.api.ResourceManager;
import org.apache.stanbol.ontologymanager.store.api.StoreSynchronizer;
import org.apache.stanbol.ontologymanager.store.jena.util.JenaUtil;
import org.apache.stanbol.ontologymanager.store.model.AdministeredOntologies;
import org.apache.stanbol.ontologymanager.store.model.BuiltInResource;
import org.apache.stanbol.ontologymanager.store.model.ClassConstraint;
import org.apache.stanbol.ontologymanager.store.model.ClassContext;
import org.apache.stanbol.ontologymanager.store.model.ClassMetaInformation;
import org.apache.stanbol.ontologymanager.store.model.ClassesForOntology;
import org.apache.stanbol.ontologymanager.store.model.ConstraintType;
import org.apache.stanbol.ontologymanager.store.model.ContainerClasses;
import org.apache.stanbol.ontologymanager.store.model.DatatypePropertiesForOntology;
import org.apache.stanbol.ontologymanager.store.model.DatatypePropertyContext;
import org.apache.stanbol.ontologymanager.store.model.DisjointClasses;
import org.apache.stanbol.ontologymanager.store.model.Domain;
import org.apache.stanbol.ontologymanager.store.model.EquivalentClasses;
import org.apache.stanbol.ontologymanager.store.model.EquivalentProperties;
import org.apache.stanbol.ontologymanager.store.model.ImportsForOntology;
import org.apache.stanbol.ontologymanager.store.model.IndividualContext;
import org.apache.stanbol.ontologymanager.store.model.IndividualMetaInformation;
import org.apache.stanbol.ontologymanager.store.model.IndividualsForOntology;
import org.apache.stanbol.ontologymanager.store.model.ObjectFactory;
import org.apache.stanbol.ontologymanager.store.model.ObjectPropertiesForOntology;
import org.apache.stanbol.ontologymanager.store.model.ObjectPropertyContext;
import org.apache.stanbol.ontologymanager.store.model.OntologyImport;
import org.apache.stanbol.ontologymanager.store.model.OntologyMetaInformation;
import org.apache.stanbol.ontologymanager.store.model.PropertyAssertions;
import org.apache.stanbol.ontologymanager.store.model.PropertyAssertions.PropertyAssertion;
import org.apache.stanbol.ontologymanager.store.model.PropertyMetaInformation;
import org.apache.stanbol.ontologymanager.store.model.Range;
import org.apache.stanbol.ontologymanager.store.model.ResourceMetaInformationType;
import org.apache.stanbol.ontologymanager.store.model.SuperProperties;
import org.apache.stanbol.ontologymanager.store.model.Superclasses;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.owllink.OWLlinkHTTPXMLReasoner;
import org.semanticweb.owlapi.owllink.OWLlinkHTTPXMLReasonerFactory;
import org.semanticweb.owlapi.owllink.OWLlinkReasonerConfiguration;
import org.semanticweb.owlapi.owllink.OWLlinkReasonerRuntimeException;
import org.semanticweb.owlapi.owllink.builtin.requests.ReleaseKB;
import org.semanticweb.owlapi.reasoner.ConsoleProgressMonitor;
import org.semanticweb.owlapi.reasoner.IndividualNodeSetPolicy;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.InferredAxiomGenerator;
import org.semanticweb.owlapi.util.InferredClassAssertionAxiomGenerator;
import org.semanticweb.owlapi.util.InferredDataPropertyCharacteristicAxiomGenerator;
import org.semanticweb.owlapi.util.InferredDisjointClassesAxiomGenerator;
import org.semanticweb.owlapi.util.InferredEquivalentClassAxiomGenerator;
import org.semanticweb.owlapi.util.InferredEquivalentDataPropertiesAxiomGenerator;
import org.semanticweb.owlapi.util.InferredEquivalentObjectPropertyAxiomGenerator;
import org.semanticweb.owlapi.util.InferredInverseObjectPropertiesAxiomGenerator;
import org.semanticweb.owlapi.util.InferredObjectPropertyCharacteristicAxiomGenerator;
import org.semanticweb.owlapi.util.InferredPropertyAssertionGenerator;
import org.semanticweb.owlapi.util.InferredSubClassAxiomGenerator;
import org.semanticweb.owlapi.util.InferredSubDataPropertyAxiomGenerator;
import org.semanticweb.owlapi.util.InferredSubObjectPropertyAxiomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.AllValuesFromRestriction;
import com.hp.hpl.jena.ontology.ComplementClass;
import com.hp.hpl.jena.ontology.ConversionException;
import com.hp.hpl.jena.ontology.DataRange;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.EnumeratedClass;
import com.hp.hpl.jena.ontology.HasValueRestriction;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.IntersectionClass;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.ontology.SomeValuesFromRestriction;
import com.hp.hpl.jena.ontology.UnionClass;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.shared.ReificationStyle;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

@Component(immediate = true, metatype = true)
@Service
public class JenaPersistenceStore implements PersistenceStore {

    @org.apache.felix.scr.annotations.Property(value = "")
    private static final String REASONER_URL_PROP = "org.apache.stanbol.ontologymanager.store.reasonerURL";

    @org.apache.felix.scr.annotations.Property(name = JenaPersistenceStore.REASONER_INFERRED_AXIOM_GENERATOR, cardinality = 1000, value = {"InferredClassAssertionAxiomGenerator"}, options = {
                                                                                                                                                                                               @PropertyOption(name = "InferredClassAssertionAxiomGenerator", value = "InferredClassAssertionAxiomGenerator"),
                                                                                                                                                                                               @PropertyOption(name = "InferredObjectPropertyCharacteristicAxiomGenerator", value = "InferredObjectPropertyCharacteristicAxiomGenerator"),
                                                                                                                                                                                               @PropertyOption(name = "InferredDataPropertyCharacteristicAxiomGenerator", value = "InferredDataPropertyCharacteristicAxiomGenerator"),
                                                                                                                                                                                               @PropertyOption(name = "InferredDisjointClassesAxiomGenerator", value = "InferredDisjointClassesAxiomGenerator"),
                                                                                                                                                                                               @PropertyOption(name = "InferredEquivalentClassesAxiomGenerator", value = "InferredEquivalentClassesAxiomGenerator"),
                                                                                                                                                                                               @PropertyOption(name = "InferredEquivalentDataPropertiesAxiomGenerator", value = "InferredEquivalentDataPropertiesAxiomGenerator"),
                                                                                                                                                                                               @PropertyOption(name = "InferredEquivalentObjectPropertyAxiomGenerator", value = "InferredEquivalentObjectPropertyAxiomGenerator"),
                                                                                                                                                                                               @PropertyOption(name = "InferredInverseObjectPropertiesGenerator", value = "InferredInverseObjectPropertiesGenerator"),
                                                                                                                                                                                               @PropertyOption(name = "InferredObjectPropertyCharacteristicAxiomGenerator", value = "InferredObjectPropertyCharacteristicAxiomGenerator"),
                                                                                                                                                                                               @PropertyOption(name = "InferredPropertyAssertionGenerator", value = "InferredPropertyAssertionGenerator"),
                                                                                                                                                                                               @PropertyOption(name = "InferredSubClassAxiomGenerator", value = "InferredSubClassAxiomGenerator"),
                                                                                                                                                                                               @PropertyOption(name = "InferredSubDataPropertyAxiomGenerator", value = "InferredSubDataPropertyAxiomGenerator"),
                                                                                                                                                                                               @PropertyOption(name = "InferredSubObjectPropertyAssertionAxiomGenerator", value = "InferredSubObjectPropertyAssertionAxiomGenerator")})
    private static final String REASONER_INFERRED_AXIOM_GENERATOR = "org.apache.stanbol.ontologymanager.store.reasonerInferredAxiomGenerators";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private JenaPersistenceProvider persistenceProvider;

    @Reference
    private ResourceManager resourceManager;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, target = "(component.factory=org.apache.stanbol.ontologymanager.store.StoreSynchronizerFactory)")
    private ComponentFactory componentFactory;

    private SynchronizerThread synchronizerThread;

    private URL REASONER_URL;

    private boolean useReasoner;

    private List<InferredAxiomGenerator> inferredAxiomGenerators;

    /** Logger instance **/
    private static Logger logger = LoggerFactory.getLogger(JenaPersistenceStore.class);

    /** Singleton instance **/
    private static JenaPersistenceStore persistenceStoreInstance = null;

    static {
        JenaUtil.initialConf();
    }

    @Deactivate
    public void deactivate(ComponentContext cc) {
        this.synchronizerThread.done();

    }

    @Activate
    @SuppressWarnings("unchecked")
    public void activate(ComponentContext ce) throws Exception {

        Dictionary<String,Object> properties = ce.getProperties();
        String reasonerUrl = properties.get(REASONER_URL_PROP).toString();
        Object axiomGenerators = properties.get(REASONER_INFERRED_AXIOM_GENERATOR);
        List<String> inferenceGenerators;
        if (axiomGenerators instanceof String) {
            inferenceGenerators = new ArrayList<String>();
            inferenceGenerators.add((String) axiomGenerators);
        } else {
            inferenceGenerators = Arrays.asList((String[]) properties.get(REASONER_INFERRED_AXIOM_GENERATOR));
        }
        inferredAxiomGenerators = createAxiomGenerators(inferenceGenerators);
        if (reasonerUrl == null || "".equals(reasonerUrl)) {
            logger.error("Reasoner URL not found, If inferred axioms are requested "
                         + OntModelSpec.OWL_DL_MEM_TRANS_INF + " will be used");
        } else {
            try {
                REASONER_URL = new URL(reasonerUrl);
                this.useReasoner = true;
            } catch (MalformedURLException e) {
                logger.warn("Invalid URL for reasoner {} ", reasonerUrl);
            }
        }
        // Get StoreSycnhronizer from component factory
        if (this.componentFactory != null) {
            final Dictionary props = new Hashtable();
            props.put(ResourceManager.class.getName(), resourceManager);
            props.put(PersistenceStore.class.getName(), this);
            ComponentInstance componentInstance = this.componentFactory.newInstance(props);
            StoreSynchronizer storeSynchronizer = (StoreSynchronizer) componentInstance.getInstance();

            this.synchronizerThread = new SynchronizerThread(storeSynchronizer, componentInstance);
            synchronizerThread.start();
        } else {
            logger.info("No synchronizer factory found");
        }

        persistenceStoreInstance = this;

    }

    private List<InferredAxiomGenerator> createAxiomGenerators(List<String> inferenceGenerators) {
        List<InferredAxiomGenerator> generators = new ArrayList<InferredAxiomGenerator>();
        for (String generator : inferenceGenerators) {
            if (generator.equalsIgnoreCase(InferredClassAssertionAxiomGenerator.class.getSimpleName())) {
                generators.add(new InferredClassAssertionAxiomGenerator());
            } else if (generator.equalsIgnoreCase(InferredDataPropertyCharacteristicAxiomGenerator.class
                    .getSimpleName())) {
                generators.add(new InferredDataPropertyCharacteristicAxiomGenerator());
            } else if (generator
                    .equalsIgnoreCase(InferredDisjointClassesAxiomGenerator.class.getSimpleName())) {
                generators.add(new InferredDisjointClassesAxiomGenerator());
            } else if (generator
                    .equalsIgnoreCase(InferredEquivalentClassAxiomGenerator.class.getSimpleName())) {
                generators.add(new InferredEquivalentClassAxiomGenerator());
            } else if (generator.equalsIgnoreCase(InferredEquivalentDataPropertiesAxiomGenerator.class
                    .getSimpleName())) {
                generators.add(new InferredEquivalentDataPropertiesAxiomGenerator());
            } else if (generator.equalsIgnoreCase(InferredEquivalentObjectPropertyAxiomGenerator.class
                    .getSimpleName())) {
                generators.add(new InferredEquivalentDataPropertiesAxiomGenerator());
            } else if (generator.equalsIgnoreCase(InferredInverseObjectPropertiesAxiomGenerator.class
                    .getSimpleName())) {
                generators.add(new InferredInverseObjectPropertiesAxiomGenerator());
            } else if (generator.equalsIgnoreCase(InferredObjectPropertyCharacteristicAxiomGenerator.class
                    .getSimpleName())) {
                generators.add(new InferredObjectPropertyCharacteristicAxiomGenerator());
            } else if (generator.equalsIgnoreCase(InferredDataPropertyCharacteristicAxiomGenerator.class
                    .getSimpleName())) {
                generators.add(new InferredDataPropertyCharacteristicAxiomGenerator());
            } else if (generator.equalsIgnoreCase(InferredPropertyAssertionGenerator.class.getSimpleName())) {
                generators.add(new InferredPropertyAssertionGenerator());
            } else if (generator.equalsIgnoreCase(InferredSubClassAxiomGenerator.class.getSimpleName())) {
                generators.add(new InferredClassAssertionAxiomGenerator());
            } else if (generator
                    .equalsIgnoreCase(InferredSubDataPropertyAxiomGenerator.class.getSimpleName())) {
                generators.add(new InferredClassAssertionAxiomGenerator());
            } else if (generator.equalsIgnoreCase(InferredSubObjectPropertyAxiomGenerator.class
                    .getSimpleName())) {
                generators.add(new InferredClassAssertionAxiomGenerator());
            }
        }
        return generators;

    }

    public void bindComponentFactory(ComponentFactory componentFactory) {
        this.componentFactory = componentFactory;
    }

    public void unbindComponentFactory(ComponentFactory componentFactory) {
        this.synchronizerThread.done();
        this.componentFactory = null;
    }

    public void bindPersistenceProvider(JenaPersistenceProvider persistenceProvider) {
        this.persistenceProvider = persistenceProvider;
    }

    public void unbindPersistenceProvider(JenaPersistenceProvider persistenceProvider) {
        this.synchronizerThread.done();
        this.persistenceProvider = null;
    }

    public URL getREASONER_URL() {
        return REASONER_URL;
    }

    public ModelMaker getModelMaker() {
        ModelMaker m_maker = ModelFactory.createMemModelMaker(ReificationStyle.Minimal);
        return m_maker;
    }

    public OntModelSpec getOntModelSpec(boolean attachReasoner) {
        OntModelSpec oms;
        if (attachReasoner) {
            oms = new OntModelSpec(OntModelSpec.OWL_DL_MEM_TRANS_INF);
        } else {
            oms = new OntModelSpec(OntModelSpec.OWL_DL_MEM);
        }
        return oms;
    }

    public static JenaPersistenceStore getInstance() {
        if (persistenceStoreInstance == null) {
            persistenceStoreInstance = new JenaPersistenceStore();
        }
        return persistenceStoreInstance;
    }

    /** Interface Functions **/

    public boolean clearPersistenceStore() {
        resourceManager.clearResourceManager();
        return persistenceProvider.clear();
    }

    public AdministeredOntologies retrieveAdministeredOntologies() {
        ObjectFactory objectFactory = new ObjectFactory();
        AdministeredOntologies administeredOntologies = objectFactory.createAdministeredOntologies();
        List<String> ontologyURIs = getSavedOntologyURIs();
        Iterator<String> ontologyURIsItr = ontologyURIs.iterator();
        while (ontologyURIsItr.hasNext()) {
            String curOntologyURI = ontologyURIsItr.next();
            String curOntologyPath = resourceManager.getOntologyPath(curOntologyURI);
            if (curOntologyPath != null) {
                OntologyMetaInformation ontologyMetaInformation = retrieveOntologyMetaInformation(curOntologyURI);
                administeredOntologies.getOntologyMetaInformation().add(ontologyMetaInformation);
            }
        }
        return administeredOntologies;
    }

    @Override
    public OntologyMetaInformation saveOntology(String ontologyContent, String ontologyURI, String encoding) throws Exception {
        InputStream is = new ByteArrayInputStream(ontologyContent.getBytes(encoding));
        return saveOntology(is, ontologyURI, encoding);
    }

    @Override
    public OntologyMetaInformation saveOntology(URL ontologyContent, String ontologyURI, String encoding) throws Exception {
        InputStream is = ontologyContent.openStream();
        return saveOntology(is, ontologyURI, encoding);
    }

    public OntologyMetaInformation saveOntology(InputStream content, String ontologyURI, String encoding) throws UnsupportedEncodingException {
        long t1 = System.currentTimeMillis();
        OntologyMetaInformation ontMetaInformation = null;
        OntModel om = null;
        long t2 = System.currentTimeMillis();
        if (ontologyURI == null || ontologyURI.isEmpty()) {
            ontologyURI = UUID.randomUUID().toString();
        }
        try {
            if (persistenceProvider.hasModel(ontologyURI)) {
                logger.info("Ontology store for {}   already exists: Updating ontology", ontologyURI);
                deleteOntology(ontologyURI);
            }
            long st1 = System.currentTimeMillis();
            logger.info("Creating a new ontology store for: {} ", ontologyURI);
            long st2 = System.currentTimeMillis();
            OntModelSpec oms = getOntModelSpec(true);
            long st3 = System.currentTimeMillis();
            long st4 = System.currentTimeMillis();
            Model base = persistenceProvider.createModel(ontologyURI);
            long st5 = System.currentTimeMillis();
            om = ModelFactory.createOntologyModel(oms, base);
            long st6 = System.currentTimeMillis();
            /** FIXME:: instead of try/catch, use a parameter **/
            Model createdModel = null;
            resourceManager.registerOntology(ontologyURI);
            try {
                createdModel = om.read(content, ontologyURI, "RDF/XML");
            } catch (Exception e) {
                resourceManager.registerOntology(ontologyURI);
                logger.warn("Unable to read ontology {} ", ontologyURI);
            }
            logger.info("{} read as RDF/XML", ontologyURI);
            long st7 = System.currentTimeMillis();
            logger.info(" Create input stream: {} ms", (st2 - st1));
            logger.info(" Get Model Spec: {} ms", (st3 - st2));
            logger.info(" Create Base Model: {} ms", (st5 - st4));
            logger.info(" Create Ont Model: {} ms", (st6 - st5));
            logger.info(" Read Model: {} ms", (st7 - st6));
            logger.info(" Total Creation: {} ms", (st7 - st1));

            long t3 = System.currentTimeMillis();

            long t4 = System.currentTimeMillis();

            if (createdModel != null) {
                ExtendedIterator ontClassesItr = om.listClasses();
                while (ontClassesItr.hasNext()) {
                    OntClass curOntClass = (OntClass) ontClassesItr.next();
                    String curOntClassURI = curOntClass.getURI();
                    if (curOntClassURI != null) {
                        resourceManager.registerClass(ontologyURI, curOntClassURI);
                    }
                }
                long t5 = System.currentTimeMillis();

                ExtendedIterator ontDatatypePropertiesItr = om.listDatatypeProperties();
                while (ontDatatypePropertiesItr.hasNext()) {
                    DatatypeProperty curDatatypeProperty = (DatatypeProperty) ontDatatypePropertiesItr.next();
                    String curDatatypePropertyURI = curDatatypeProperty.getURI();
                    if (curDatatypePropertyURI != null) {
                        resourceManager.registerDatatypeProperty(ontologyURI, curDatatypePropertyURI);
                    }
                }

                long t6 = System.currentTimeMillis();
                ExtendedIterator ontObjectPropertiesItr = om.listObjectProperties();
                while (ontObjectPropertiesItr.hasNext()) {
                    ObjectProperty curObjectProperty = (ObjectProperty) ontObjectPropertiesItr.next();
                    String curObjectPropertyURI = curObjectProperty.getURI();
                    if (curObjectPropertyURI != null) {
                        resourceManager.registerObjectProperty(ontologyURI, curObjectPropertyURI);
                    }
                }
                long t7 = System.currentTimeMillis();
                ExtendedIterator ontIndividualsItr = om.listIndividuals();
                while (ontIndividualsItr.hasNext()) {
                    Individual curIndividual = (Individual) ontIndividualsItr.next();
                    String curIndividualURI = curIndividual.getURI();
                    if (curIndividualURI != null) {
                        resourceManager.registerIndividual(ontologyURI, curIndividualURI);
                    }
                }

                long t8 = System.currentTimeMillis();
                Set imports = om.listImportedOntologyURIs();
                Iterator importsItr = imports.iterator();
                while (importsItr.hasNext()) {
                    String importedOntologyURI = (String) importsItr.next();
                    try {

                        resourceManager.registerOntology(importedOntologyURI);
                        Model baseModel = persistenceProvider.createModel(importedOntologyURI);

                        OntModel imported_om = ModelFactory.createOntologyModel(getOntModelSpec(false),
                            baseModel);
                        // FIXME Test this case
                        imported_om.read(importedOntologyURI);
                        ExtendedIterator importedOntClassesItr = imported_om.listClasses();
                        while (importedOntClassesItr.hasNext()) {
                            OntClass curOntClass = (OntClass) importedOntClassesItr.next();
                            String curOntClassURI = curOntClass.getURI();
                            if (curOntClassURI != null) {
                                resourceManager.registerClass(importedOntologyURI, curOntClassURI);
                            }
                        }
                        ExtendedIterator importedDatatypePropertiesItr = imported_om.listDatatypeProperties();
                        while (importedDatatypePropertiesItr.hasNext()) {
                            DatatypeProperty curDatatypeProperty = (DatatypeProperty) importedDatatypePropertiesItr
                                    .next();
                            String curDatatypePropertyURI = curDatatypeProperty.getURI();
                            if (curDatatypePropertyURI != null) {
                                resourceManager.registerDatatypeProperty(importedOntologyURI,
                                    curDatatypePropertyURI);
                            }
                        }
                        ExtendedIterator importedObjectPropertiesItr = imported_om.listObjectProperties();
                        while (importedObjectPropertiesItr.hasNext()) {
                            ObjectProperty curObjectProperty = (ObjectProperty) importedObjectPropertiesItr
                                    .next();
                            String curObjectPropertyURI = curObjectProperty.getURI();
                            if (curObjectPropertyURI != null) {
                                resourceManager.registerObjectProperty(importedOntologyURI,
                                    curObjectPropertyURI);
                            }
                        }
                        ExtendedIterator importedOntIndividualsItr = imported_om.listIndividuals();
                        while (importedOntIndividualsItr.hasNext()) {
                            Individual curIndividual = (Individual) importedOntIndividualsItr.next();
                            String curIndividualURI = curIndividual.getURI();
                            if (curIndividualURI != null) {
                                resourceManager.registerIndividual(importedOntologyURI, curIndividualURI);
                            }
                        }
                        persistenceProvider.commit(imported_om);
                    } catch (Exception e) {
                        logger.warn("Error at importing ontology " + importedOntologyURI, e);
                    }
                }
                long t9 = System.currentTimeMillis();
                ontMetaInformation = retrieveOntologyMetaInformation(ontologyURI);
                long t10 = System.currentTimeMillis();
                logger.info(" Get Connection: {} ms", (t2 - t1));
                logger.info(" Create Ontology Model: {} ms", (t3 - t2));
                logger.info(" Classes: {} ms", (t5 - t4));
                logger.info(" Datatype Properties: {} ms", (t6 - t5));
                logger.info(" Object Props: {} ms ", (t7 - t6));
                logger.info(" Individuals: {} ms ", (t8 - t7));
                logger.info(" Imports: {} ms", (t9 - t8));
                logger.info(" MetaInf: {} ms", (t10 - t9));
                logger.info(" Total Save Time {} ms ", (t10 - t1));

            }
        } catch (Exception e) {
            logger.error("Error ", e);
        } finally {
            persistenceProvider.commit(om);
        }
        return ontMetaInformation;
    }

    public String retrieveOntology(String ontologyURI, String language, boolean withInferredAxioms) {

        try {
            if (persistenceProvider.hasModel(ontologyURI)) {
                Model baseModel = persistenceProvider.getModel(ontologyURI);
                OntModel ontModel = ModelFactory.createOntologyModel(getOntModelSpec(withInferredAxioms),
                    baseModel);
                if (this.useReasoner && withInferredAxioms) {
                    ontModel = addInferencesToModel(ontModel, ontologyURI, InferenceScope.Class);
                }
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                RDFWriter rdfWriter = ontModel.getWriter("RDF/XML");
                rdfWriter.setProperty("xmlbase", ontologyURI);
                rdfWriter.write(ontModel.getBaseModel(), bos, ontologyURI);

                byte[] ontologyContentAsByteArray = bos.toByteArray();
                String ontologyContentAsString = new String(ontologyContentAsByteArray);
                return ontologyContentAsString;
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return null;
    }

    @Override
    public String mergeOntology(String ontologyURI,
                                String targetOntology,
                                String targetOntologyURI,
                                boolean withInferredAxioms) throws Exception {
        try {
            if (persistenceProvider.hasModel(ontologyURI)) {
                Model baseModel = persistenceProvider.getModel(ontologyURI);
                OntModel ontModel = ModelFactory.createOntologyModel(getOntModelSpec(withInferredAxioms),
                    baseModel);
                if (this.useReasoner && withInferredAxioms) {
                    ontModel = addInferencesToModel(ontModel, ontologyURI, InferenceScope.Class);
                }

                InputStream is = new ByteArrayInputStream(targetOntology.getBytes());
                ontModel.read(is, targetOntologyURI);

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                RDFWriter rdfWriter = ontModel.getWriter("RDF/XML");
                rdfWriter.setProperty("xmlbase", ontologyURI);
                rdfWriter.write(ontModel.getBaseModel(), bos, ontologyURI);

                byte[] ontologyContentAsByteArray = bos.toByteArray();
                String ontologyContentAsString = new String(ontologyContentAsByteArray);
                return ontologyContentAsString;
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return null;
    }

    public ClassMetaInformation generateClassForOntology(String ontologyURI, String classURI) {
        OntModel ontModel = null;
        try {
            
            if (persistenceProvider.hasModel(ontologyURI)) {
                Model baseModel = persistenceProvider.getModel(ontologyURI);
                ontModel = ModelFactory.createOntologyModel(getOntModelSpec(false), baseModel);
                OntClass classForURI = ontModel.getOntClass(ontologyURI);
                if (classForURI == null) {
                    // the class does not exist, this is what we desire
                    ontModel.createClass(classURI);
                    resourceManager.registerClass(ontologyURI, classURI);
                    return generateClassMetaInformation(classURI);
                }
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        } finally {
            persistenceProvider.commit(ontModel);
        }
        return null;
    }

    public PropertyMetaInformation generateDatatypePropertyForOntology(String ontologyURI,
                                                                       String datatypePropertyURI) {

        try {

            if (persistenceProvider.hasModel(ontologyURI)) {
                Model baseModel = persistenceProvider.getModel(ontologyURI);
                OntModel ontModel = ModelFactory.createOntologyModel(getOntModelSpec(false), baseModel);

                OntProperty ontPropertyForURI = ontModel.getOntProperty(datatypePropertyURI);
                if (ontPropertyForURI == null) {
                    // the datatypeProperty does not exist, this is what we
                    // desire
                    ontModel.createDatatypeProperty(datatypePropertyURI);
                    resourceManager.registerDatatypeProperty(ontologyURI, datatypePropertyURI);
                    persistenceProvider.commit(ontModel);
                    return generatePropertyMetaInformation(datatypePropertyURI);
                }
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return null;
    }

    public PropertyMetaInformation generateObjectPropertyForOntology(String ontologyURI,
                                                                     String objectPropertyURI) {

        try {
            if (persistenceProvider.hasModel(ontologyURI)) {
                Model baseModel = persistenceProvider.getModel(ontologyURI);
                OntModel ontModel = ModelFactory.createOntologyModel(getOntModelSpec(false), baseModel);
                OntProperty ontPropertyForURI = ontModel.getOntProperty(objectPropertyURI);
                if (ontPropertyForURI == null) {
                    // the objectProperty does not exist, this is what we desire
                    ontModel.createObjectProperty(objectPropertyURI);
                    resourceManager.registerObjectProperty(ontologyURI, objectPropertyURI);
                    persistenceProvider.commit(ontModel);
                    return generatePropertyMetaInformation(objectPropertyURI);
                }
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return null;
    }

    public boolean makeSubClassOf(String subClassURI, String superClassURI) {

        try {
            // FIXME:: What happens when the superClassURI does not exist?
            String subClassOntologyURI = resourceManager.resolveOntologyURIFromResourceURI(subClassURI);
            String superClassOntologyURI = resourceManager.resolveOntologyURIFromResourceURI(superClassURI);
            if (subClassOntologyURI != null && superClassOntologyURI != null) {
                Model subClassModel = persistenceProvider.getModel(subClassOntologyURI);
                OntModel subClassOntModel = ModelFactory.createOntologyModel(getOntModelSpec(false),
                    subClassModel);
                OntClass subClass = subClassOntModel.getOntClass(subClassURI);

                Model superClassModel = persistenceProvider.getModel(superClassOntologyURI);
                OntModel superClassOntModel = ModelFactory.createOntologyModel(getOntModelSpec(false),
                    superClassModel);
                OntClass superClass = superClassOntModel.getOntClass(superClassURI);

                if (subClass != null && superClass != null) {
                    subClass.addSuperClass(superClass);
                    superClass.addSubClass(subClass);
                    persistenceProvider.commit(subClassOntModel);
                    persistenceProvider.commit(superClassOntModel);
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return false;
    }

    public boolean deleteSuperClass(String subClassURI, String superClassURI) {

        try {
            // FIXME:: What happens when the superClassURI does not exist?
            String subClassOntologyURI = resourceManager.resolveOntologyURIFromResourceURI(subClassURI);
            String superClassOntologyURI = resourceManager.resolveOntologyURIFromResourceURI(superClassURI);
            if (subClassOntologyURI != null && superClassOntologyURI != null) {
                Model subClassModel = persistenceProvider.getModel(subClassOntologyURI);
                OntModel subClassOntModel = ModelFactory.createOntologyModel(getOntModelSpec(false),
                    subClassModel);
                OntClass subClass = subClassOntModel.getOntClass(subClassURI);

                Model superClassModel = persistenceProvider.getModel(superClassOntologyURI);
                OntModel superClassOntModel = ModelFactory.createOntologyModel(getOntModelSpec(false),
                    superClassModel);
                OntClass superClass = superClassOntModel.getOntClass(superClassURI);

                if (subClass != null && superClass != null) {
                    subClass.removeSuperClass(superClass);
                    superClass.removeSubClass(subClass);
                    persistenceProvider.commit(subClassOntModel);
                    persistenceProvider.commit(superClassOntModel);
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return false;
    }

    public boolean setDomain(String propertyURI, List<String> domainURIs) {

        try {
            String ontologyForPropertyURI = resourceManager.resolveOntologyURIFromResourceURI(propertyURI);
            Model modelForProperty = persistenceProvider.getModel(ontologyForPropertyURI);
            OntModel ontModelForProperty = ModelFactory.createOntologyModel(getOntModelSpec(false),
                modelForProperty);
            OntProperty ontProperty = ontModelForProperty.getOntProperty(propertyURI);
            for (OntResource domainResource : ontProperty.listDomain().toList()) {
                ontProperty.removeDomain(domainResource);
            }
            for (String domainURI : domainURIs) {
                String ontologyForDomainURI = resourceManager.resolveOntologyURIFromResourceURI(domainURI);
                if (ontologyForPropertyURI != null && ontologyForDomainURI != null) {
                    Model modelForDomain = persistenceProvider.getModel(ontologyForDomainURI);
                    OntModel ontModelForDomain = ModelFactory.createOntologyModel(getOntModelSpec(false),
                        modelForDomain);
                    OntClass ontClassForDomain = ontModelForDomain.getOntClass(domainURI);
                    if (ontProperty != null && ontClassForDomain != null) {
                        ontProperty.addDomain(ontClassForDomain);
                        persistenceProvider.commit(ontModelForProperty);
                        persistenceProvider.commit(ontModelForDomain);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return false;
    }

    public boolean addDomain(String propertyURI, String domainURI) {

        try {
            String ontologyForPropertyURI = resourceManager.resolveOntologyURIFromResourceURI(propertyURI);
            Model modelForProperty = persistenceProvider.getModel(ontologyForPropertyURI);
            OntModel ontModelForProperty = ModelFactory.createOntologyModel(getOntModelSpec(false),
                modelForProperty);
            OntProperty ontProperty = ontModelForProperty.getOntProperty(propertyURI);
            String ontologyForDomainURI = resourceManager.resolveOntologyURIFromResourceURI(domainURI);
            if (ontologyForPropertyURI != null && ontologyForDomainURI != null) {
                Model modelForDomain = persistenceProvider.getModel(ontologyForDomainURI);
                OntModel ontModelForDomain = ModelFactory.createOntologyModel(getOntModelSpec(false),
                    modelForDomain);
                OntClass ontClassForDomain = ontModelForDomain.getOntClass(domainURI);
                if (ontProperty != null && ontClassForDomain != null) {
                    ontProperty.addDomain(ontClassForDomain);
                    persistenceProvider.commit(ontModelForProperty);
                    persistenceProvider.commit(ontModelForDomain);
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return false;
    }

    public boolean deleteDomain(String propertyURI, String domainURI) {

        try {
            String ontologyForPropertyURI = resourceManager.resolveOntologyURIFromResourceURI(propertyURI);
            Model modelForProperty = persistenceProvider.getModel(ontologyForPropertyURI);
            OntModel ontModelForProperty = ModelFactory.createOntologyModel(getOntModelSpec(false),
                modelForProperty);
            OntProperty ontProperty = ontModelForProperty.getOntProperty(propertyURI);
            String ontologyForDomainURI = resourceManager.resolveOntologyURIFromResourceURI(domainURI);
            if (ontologyForPropertyURI != null && ontologyForDomainURI != null) {
                Model modelForDomain = persistenceProvider.getModel(ontologyForDomainURI);
                OntModel ontModelForDomain = ModelFactory.createOntologyModel(getOntModelSpec(false),
                    modelForDomain);
                OntClass ontClassForDomain = ontModelForDomain.getOntClass(domainURI);
                if (ontProperty != null && ontClassForDomain != null) {
                    ontProperty.removeDomain(ontClassForDomain);
                    persistenceProvider.commit(ontModelForProperty);
                    persistenceProvider.commit(ontModelForDomain);
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return false;
    }

    public boolean setRange(String propertyURI, List<String> rangeURIs) {
        try {
            String ontologyForPropertyURI = resourceManager.resolveOntologyURIFromResourceURI(propertyURI);
            Model modelForProperty = persistenceProvider.getModel(ontologyForPropertyURI);
            OntModel ontModelForProperty = ModelFactory.createOntologyModel(getOntModelSpec(false),
                modelForProperty);
            OntProperty ontProperty = ontModelForProperty.getOntProperty(propertyURI);
            for (OntResource rangeResource : ontProperty.listRange().toList()) {
                ontProperty.removeRange(rangeResource);
            }
            for (String rangeURI : rangeURIs) {
                String ontologyForRangeURI = resourceManager.resolveOntologyURIFromResourceURI(rangeURI);
                if (ontologyForPropertyURI != null) {
                    if (JenaUtil.isBuiltInType(rangeURI)) {
                        Resource rangeResource = JenaUtil.getResourceForBuiltInType(rangeURI);
                        if (ontProperty != null && rangeResource != null) {
                            ontProperty.addRange(rangeResource);
                            persistenceProvider.commit(ontModelForProperty);
                        }
                    } else if (ontologyForRangeURI != null) {
                        Model modelForRange = persistenceProvider.getModel(ontologyForRangeURI);
                        OntModel ontModelForRange = ModelFactory.createOntologyModel(getOntModelSpec(false),
                            modelForRange);
                        OntClass ontClassForRange = ontModelForRange.getOntClass(rangeURI);

                        if (ontProperty != null && ontClassForRange != null) {
                            ontProperty.addRange(ontClassForRange);
                            persistenceProvider.commit(ontModelForProperty);
                            persistenceProvider.commit(ontModelForRange);
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return false;
    }

    public boolean addRange(String propertyURI, String rangeURI) {
        try {
            String ontologyForPropertyURI = resourceManager.resolveOntologyURIFromResourceURI(propertyURI);
            Model modelForProperty = persistenceProvider.getModel(ontologyForPropertyURI);
            OntModel ontModelForProperty = ModelFactory.createOntologyModel(getOntModelSpec(false),
                modelForProperty);
            OntProperty ontProperty = ontModelForProperty.getOntProperty(propertyURI);

            String ontologyForRangeURI = resourceManager.resolveOntologyURIFromResourceURI(rangeURI);
            if (ontologyForPropertyURI != null) {
                if (JenaUtil.isBuiltInType(rangeURI)) {
                    Resource rangeResource = JenaUtil.getResourceForBuiltInType(rangeURI);
                    if (ontProperty != null && rangeResource != null) {
                        ontProperty.addRange(rangeResource);
                        persistenceProvider.commit(ontModelForProperty);
                    }
                } else if (ontologyForRangeURI != null) {
                    Model modelForRange = persistenceProvider.getModel(ontologyForRangeURI);
                    OntModel ontModelForRange = ModelFactory.createOntologyModel(getOntModelSpec(false),
                        modelForRange);
                    OntClass ontClassForRange = ontModelForRange.getOntClass(rangeURI);

                    if (ontProperty != null && ontClassForRange != null) {
                        ontProperty.addRange(ontClassForRange);
                        persistenceProvider.commit(ontModelForProperty);
                        persistenceProvider.commit(ontModelForRange);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return false;
    }

    public boolean deleteRange(String propertyURI, String rangeURI) {
        try {
            String ontologyForPropertyURI = resourceManager.resolveOntologyURIFromResourceURI(propertyURI);
            Model modelForProperty = persistenceProvider.getModel(ontologyForPropertyURI);
            OntModel ontModelForProperty = ModelFactory.createOntologyModel(getOntModelSpec(false),
                modelForProperty);
            OntProperty ontProperty = ontModelForProperty.getOntProperty(propertyURI);

            String ontologyForRangeURI = resourceManager.resolveOntologyURIFromResourceURI(rangeURI);
            if (ontologyForPropertyURI != null) {
                if (JenaUtil.isBuiltInType(rangeURI)) {
                    Resource rangeResource = JenaUtil.getResourceForBuiltInType(rangeURI);
                    if (ontProperty != null && rangeResource != null) {
                        ontProperty.removeRange(rangeResource);
                        persistenceProvider.commit(ontModelForProperty);
                    }
                } else if (ontologyForRangeURI != null) {
                    Model modelForRange = persistenceProvider.getModel(ontologyForRangeURI);
                    OntModel ontModelForRange = ModelFactory.createOntologyModel(getOntModelSpec(false),
                        modelForRange);
                    OntClass ontClassForRange = ontModelForRange.getOntClass(rangeURI);

                    if (ontProperty != null && ontClassForRange != null) {
                        ontProperty.removeRange(ontClassForRange);
                        persistenceProvider.commit(ontModelForProperty);
                        persistenceProvider.commit(ontModelForRange);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return false;
    }

    public boolean makeSubPropertyOf(String subPropertyURI, String superPropertyURI) {
        try {
            String ontologyForSubPropertyURI = resourceManager
                    .resolveOntologyURIFromResourceURI(subPropertyURI);
            String ontologyForSuperClassURI = resourceManager
                    .resolveOntologyURIFromResourceURI(superPropertyURI);
            if (ontologyForSubPropertyURI != null && ontologyForSuperClassURI != null) {
                Model modelForSubProperty = persistenceProvider.getModel(ontologyForSubPropertyURI);
                OntModel ontModelForSubProperty = ModelFactory.createOntologyModel(getOntModelSpec(false),
                    modelForSubProperty);
                OntProperty subProperty = ontModelForSubProperty.getOntProperty(subPropertyURI);

                Model modelForSuperProperty = persistenceProvider.getModel(ontologyForSuperClassURI);
                OntModel ontModelForSuperProperty = ModelFactory.createOntologyModel(getOntModelSpec(false),
                    modelForSuperProperty);
                OntProperty superProperty = ontModelForSuperProperty.getOntProperty(superPropertyURI);

                if (subProperty != null && superProperty != null) {
                    subProperty.addSuperProperty(superProperty);
                    superProperty.addSubProperty(subProperty);
                    persistenceProvider.commit(ontModelForSubProperty);
                    persistenceProvider.commit(ontModelForSuperProperty);
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return false;
    }

    public boolean deleteSuperPropertyAssertion(String subPropertyURI, String superPropertyURI) {
        try {
            String ontologyForSubPropertyURI = resourceManager
                    .resolveOntologyURIFromResourceURI(subPropertyURI);
            String ontologyForSuperClassURI = resourceManager
                    .resolveOntologyURIFromResourceURI(superPropertyURI);
            if (ontologyForSubPropertyURI != null && ontologyForSuperClassURI != null) {
                Model modelForSubProperty = persistenceProvider.getModel(ontologyForSubPropertyURI);
                OntModel ontModelForSubProperty = ModelFactory.createOntologyModel(getOntModelSpec(false),
                    modelForSubProperty);
                OntProperty subProperty = ontModelForSubProperty.getOntProperty(subPropertyURI);

                Model modelForSuperProperty = persistenceProvider.getModel(ontologyForSuperClassURI);
                OntModel ontModelForSuperProperty = ModelFactory.createOntologyModel(getOntModelSpec(false),
                    modelForSuperProperty);
                OntProperty superProperty = ontModelForSuperProperty.getOntProperty(superPropertyURI);

                if (subProperty != null && superProperty != null) {
                    subProperty.removeSuperProperty(superProperty);
                    superProperty.removeSubProperty(subProperty);
                    persistenceProvider.commit(ontModelForSubProperty);
                    persistenceProvider.commit(ontModelForSuperProperty);
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return false;
    }

    public boolean setPropertyAttributes(String propertyURI,
                                         Boolean isFunctional,
                                         Boolean isTransitive,
                                         Boolean isSymmetric,
                                         Boolean isInverseFunctional) throws SQLException {
        OntModel ontModelForProperty = null;
        try {
            String ontologyForPropertyURI = resourceManager.resolveOntologyURIFromResourceURI(propertyURI);
            if (ontologyForPropertyURI != null) {
                Model modelForProperty = persistenceProvider.getModel(ontologyForPropertyURI);
                ontModelForProperty = ModelFactory.createOntologyModel(getOntModelSpec(false),
                    modelForProperty);
                OntProperty property = ontModelForProperty.getOntProperty(propertyURI);
                if (isFunctional != null && isFunctional.booleanValue()) {
                    property.convertToFunctionalProperty();
                } else {
                    List<Statement> stmts = ontModelForProperty.listStatements(property, RDF.type,
                        OWL.FunctionalProperty).toList();
                    ontModelForProperty.remove(stmts);
                }
                if (isTransitive != null && isTransitive.booleanValue()) {
                    property.convertToTransitiveProperty();
                } else {
                    List<Statement> stmts = ontModelForProperty.listStatements(property, RDF.type,
                        OWL.TransitiveProperty).toList();
                    ontModelForProperty.remove(stmts);

                }
                if (isSymmetric != null && isSymmetric.booleanValue()) {
                    property.convertToSymmetricProperty();
                } else {
                    List<Statement> stmts = ontModelForProperty.listStatements(property, RDF.type,
                        OWL.SymmetricProperty).toList();
                    ontModelForProperty.remove(stmts);
                }
                if (isInverseFunctional != null && isInverseFunctional.booleanValue()) {
                    property.convertToInverseFunctionalProperty();
                } else {
                    List<Statement> stmts = ontModelForProperty.listStatements(property, RDF.type,
                        OWL.InverseFunctionalProperty).toList();
                    ontModelForProperty.remove(stmts);
                }
                persistenceProvider.commit(ontModelForProperty);
                return true;
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return false;

    }

    public boolean assertPropertyValue(String individualURI,
                                       String propertyURI,
                                       String individualAsValueURI,
                                       String literalAsValue) {
        try {
            String ontologyFor_arg0 = resourceManager.resolveOntologyURIFromResourceURI(individualURI);
            String ontologyFor_arg1 = resourceManager.resolveOntologyURIFromResourceURI(propertyURI);

            if (ontologyFor_arg0 != null && ontologyFor_arg1 != null) {
                Model modelForIndividual = persistenceProvider.getModel(ontologyFor_arg0);
                OntModel ontModelForIndividual = ModelFactory.createOntologyModel(getOntModelSpec(false),
                    modelForIndividual);
                Individual individual = ontModelForIndividual.getIndividual(individualURI);

                Model modelForProperty = persistenceProvider.getModel(ontologyFor_arg1);
                OntModel ontModelForProperty = ModelFactory.createOntologyModel(getOntModelSpec(false),
                    modelForProperty);
                OntProperty ontProperty = ontModelForProperty.getOntProperty(propertyURI);

                if (individualAsValueURI != null) {
                    String ontologyFor_arg2 = resourceManager
                            .resolveOntologyURIFromResourceURI(individualAsValueURI);
                    if (ontologyFor_arg2 != null) {
                        Model modelForValue = persistenceProvider.getModel(ontologyFor_arg2);
                        OntModel ontModelForValue = ModelFactory.createOntologyModel(getOntModelSpec(false),
                            modelForValue);
                        Individual value = ontModelForValue.getIndividual(individualAsValueURI);
                        individual.addProperty(ontProperty, value);
                        persistenceProvider.commit(ontModelForIndividual);
                        persistenceProvider.commit(ontModelForProperty);
                        persistenceProvider.commit(ontModelForValue);
                        return true;
                    }
                } else if (literalAsValue != null) {
                    Literal literal = ontModelForIndividual.createLiteral(literalAsValue);
                    individual.addProperty(ontProperty, literal);
                    persistenceProvider.commit(ontModelForIndividual);
                    persistenceProvider.commit(ontModelForProperty);
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return false;
    }

    public boolean deletePropertyAssertion(String individualURI,
                                           String propertyURI,
                                           String individualAsValueURI,
                                           String literalAsValue) {
        try {
            String ontologyFor_arg0 = resourceManager.resolveOntologyURIFromResourceURI(individualURI);
            String ontologyFor_arg1 = resourceManager.resolveOntologyURIFromResourceURI(propertyURI);

            if (ontologyFor_arg0 != null && ontologyFor_arg1 != null) {
                Model modelForIndividual = persistenceProvider.getModel(ontologyFor_arg0);
                OntModel ontModelForIndividual = ModelFactory.createOntologyModel(getOntModelSpec(false),
                    modelForIndividual);
                Individual individual = ontModelForIndividual.getIndividual(individualURI);

                Model modelForProperty = persistenceProvider.getModel(ontologyFor_arg1);
                OntModel ontModelForProperty = ModelFactory.createOntologyModel(getOntModelSpec(false),
                    modelForProperty);
                OntProperty ontProperty = ontModelForProperty.getOntProperty(propertyURI);

                if (individualAsValueURI != null) {
                    String ontologyFor_arg2 = resourceManager
                            .resolveOntologyURIFromResourceURI(individualAsValueURI);
                    if (ontologyFor_arg2 != null) {
                        Model modelForValue = persistenceProvider.getModel(ontologyFor_arg2);
                        OntModel ontModelForValue = ModelFactory.createOntologyModel(getOntModelSpec(false),
                            modelForValue);
                        Individual value = ontModelForValue.getIndividual(individualAsValueURI);
                        individual.removeProperty(ontProperty, value);
                        persistenceProvider.commit(ontModelForIndividual);
                        persistenceProvider.commit(ontModelForProperty);
                        persistenceProvider.commit(ontModelForValue);
                        return true;
                    }
                } else if (literalAsValue != null) {
                    Literal literal = ontModelForIndividual.createLiteral(literalAsValue);
                    individual.removeProperty(ontProperty, literal);
                    persistenceProvider.commit(ontModelForIndividual);
                    persistenceProvider.commit(ontModelForProperty);
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return false;
    }

    public boolean addContainerClassForIndividual(String individualURI, String classURI) {
        try {

            String ontologyForIndividual = resourceManager.resolveOntologyURIFromResourceURI(individualURI);
            String ontologyForClass = resourceManager.resolveOntologyURIFromResourceURI(classURI);

            if (ontologyForIndividual != null && ontologyForClass != null) {
                Model modelForIndividual = persistenceProvider.getModel(ontologyForIndividual);
                OntModel ontModelForIndividual = ModelFactory.createOntologyModel(getOntModelSpec(false),
                    modelForIndividual);
                Individual individual = ontModelForIndividual.getIndividual(individualURI);

                Model modelForClass = persistenceProvider.getModel(ontologyForClass);
                OntModel ontModelForClass = ModelFactory.createOntologyModel(getOntModelSpec(false),
                    modelForClass);
                OntClass ontClass = ontModelForClass.getOntClass(classURI);

                individual.addOntClass(ontClass);
                persistenceProvider.commit(ontModelForClass);
                persistenceProvider.commit(ontModelForIndividual);
                return true;
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return false;
    }

    public boolean deleteContainerClassForIndividual(String individualURI, String classURI) {
        try {
            String ontologyForIndividual = resourceManager.resolveOntologyURIFromResourceURI(individualURI);
            String ontologyForClass = resourceManager.resolveOntologyURIFromResourceURI(classURI);
            if (ontologyForIndividual != null && ontologyForClass != null) {
                Model modelForIndividual = persistenceProvider.getModel(ontologyForIndividual);
                OntModel ontModelForIndividual = ModelFactory.createOntologyModel(getOntModelSpec(false),
                    modelForIndividual);
                Individual individual = ontModelForIndividual.getIndividual(individualURI);

                Model modelForClass = persistenceProvider.getModel(ontologyForClass);
                OntModel ontModelForClass = ModelFactory.createOntologyModel(getOntModelSpec(false),
                    modelForClass);
                OntClass ontClass = ontModelForClass.getOntClass(classURI);

                individual.removeOntClass(ontClass);
                persistenceProvider.commit(ontModelForClass);
                persistenceProvider.commit(ontModelForIndividual);
                return true;
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return false;
    }

    public IndividualMetaInformation generateIndividualForOntology(String ontologyURI,
                                                                   String classURI,
                                                                   String individualURI) {
        try {

            if (persistenceProvider.hasModel(ontologyURI)) {
                Model baseModel = persistenceProvider.getModel(ontologyURI);
                OntModel ontModel = ModelFactory.createOntologyModel(getOntModelSpec(false), baseModel);
                Individual individualForURI = ontModel.getIndividual(individualURI);
                OntClass ontClassForURI = ontModel.getOntClass(classURI);
                if (individualForURI == null && ontClassForURI != null) {

                    ontModel.createIndividual(individualURI, ontClassForURI);
                    resourceManager.registerIndividual(ontologyURI, individualURI);
                    persistenceProvider.commit(ontModel);
                    return generateIndividualMetaInformation(individualURI);
                }
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return null;
    }

    public OntologyMetaInformation retrieveOntologyMetaInformation(String ontologyURI) {

        if (ontologyURI != null) {
            ObjectFactory objectFactory = new ObjectFactory();
            OntologyMetaInformation ontologyMetaInformation = objectFactory.createOntologyMetaInformation();
            ontologyMetaInformation.setURI(ontologyURI);
            ontologyMetaInformation.setHref(resourceManager.getOntologyFullPath(ontologyURI));
            /** FIXME: Descriptions have to be administerable **/
            ontologyMetaInformation.setDescription("");
            return ontologyMetaInformation;
        } else {
            return null;
        }
    }

    public ClassesForOntology retrieveClassesOfOntology(String ontologyURI) {

        try {
            if (persistenceProvider.hasModel(ontologyURI)) {
                ObjectFactory objectFactory = new ObjectFactory();

                ClassesForOntology classesForOntology = objectFactory.createClassesForOntology();
                classesForOntology.setOntologyMetaInformation(retrieveOntologyMetaInformation(ontologyURI));

                Model baseModel = persistenceProvider.getModel(ontologyURI);
                OntModel ontModel = ModelFactory.createOntologyModel(getOntModelSpec(false), baseModel);
                ExtendedIterator ontClassesItr = ontModel.listClasses();
                while (ontClassesItr.hasNext()) {
                    OntClass curOntClass = (OntClass) ontClassesItr.next();
                    if (curOntClass.getURI() != null) {
                        ClassMetaInformation classMetaInformation = generateClassMetaInformation(curOntClass
                                .getURI());
                        if (classMetaInformation != null) classesForOntology.getClassMetaInformation().add(
                            classMetaInformation);
                    } else {

                    }
                }
                return classesForOntology;
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        } finally {

        }
        return null;
    }

    public DatatypePropertiesForOntology retrieveDatatypePropertiesOfOntology(String ontologyURI) {

        try {
            if (persistenceProvider.hasModel(ontologyURI)) {
                ObjectFactory objectFactory = new ObjectFactory();

                DatatypePropertiesForOntology datatypePropertiesForOntology = objectFactory
                        .createDatatypePropertiesForOntology();
                datatypePropertiesForOntology
                        .setOntologyMetaInformation(retrieveOntologyMetaInformation(ontologyURI));

                Model baseModel = persistenceProvider.getModel(ontologyURI);
                OntModel ontModel = ModelFactory.createOntologyModel(getOntModelSpec(false), baseModel);
                ExtendedIterator ontDatatypePropertiesItr = ontModel.listDatatypeProperties();
                while (ontDatatypePropertiesItr.hasNext()) {
                    DatatypeProperty curDatatypeProperty = (DatatypeProperty) ontDatatypePropertiesItr.next();
                    if (curDatatypeProperty.getURI() != null) {
                        PropertyMetaInformation datatypePropertyMetaInformation = generatePropertyMetaInformation(curDatatypeProperty
                                .getURI());
                        datatypePropertiesForOntology.getPropertyMetaInformation().add(
                            datatypePropertyMetaInformation);
                    }
                }
                return datatypePropertiesForOntology;
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        } finally {

        }
        return null;
    }

    public ObjectPropertiesForOntology retrieveObjectPropertiesOfOntology(String ontologyURI) {

        try {
            if (persistenceProvider.hasModel(ontologyURI)) {
                ObjectFactory objectFactory = new ObjectFactory();

                ObjectPropertiesForOntology objectPropertiesForOntology = objectFactory
                        .createObjectPropertiesForOntology();
                objectPropertiesForOntology
                        .setOntologyMetaInformation(retrieveOntologyMetaInformation(ontologyURI));

                Model baseModel = persistenceProvider.getModel(ontologyURI);
                OntModel ontModel = ModelFactory.createOntologyModel(getOntModelSpec(false), baseModel);
                ExtendedIterator ontObjectPropertiesItr = ontModel.listObjectProperties();
                while (ontObjectPropertiesItr.hasNext()) {
                    ObjectProperty curObjectProperty = (ObjectProperty) ontObjectPropertiesItr.next();
                    if (curObjectProperty.getURI() != null) {
                        PropertyMetaInformation propertyMetaInformation = generatePropertyMetaInformation(curObjectProperty
                                .getURI());
                        if (propertyMetaInformation != null) {
                            objectPropertiesForOntology.getPropertyMetaInformation().add(
                                propertyMetaInformation);
                        }
                    }
                }
                return objectPropertiesForOntology;
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        } finally {

        }
        return null;
    }

    public IndividualsForOntology retrieveIndividualsOfOntology(String ontologyURI) {

        try {
            if (persistenceProvider.hasModel(ontologyURI)) {
                ObjectFactory objectFactory = new ObjectFactory();

                IndividualsForOntology individualsForOntology = objectFactory.createIndividualsForOntology();
                individualsForOntology
                        .setOntologyMetaInformation(retrieveOntologyMetaInformation(ontologyURI));

                Model baseModel = persistenceProvider.getModel(ontologyURI);
                OntModel ontModel = ModelFactory.createOntologyModel(getOntModelSpec(false), baseModel);
                ExtendedIterator ontIndividualsItr = ontModel.listIndividuals();
                while (ontIndividualsItr.hasNext()) {
                    Individual individual = (Individual) ontIndividualsItr.next();
                    if (individual.getURI() != null) {
                        IndividualMetaInformation individualMetaInformation = generateIndividualMetaInformation(individual
                                .getURI());
                        individualsForOntology.getIndividualMetaInformation().add(individualMetaInformation);
                    }
                }
                return individualsForOntology;
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        } finally {

        }
        return null;
    }

    public boolean deleteOntology(String ontologyURI) {

        try {
            if (resourceManager.hasOntology(ontologyURI)) {
                persistenceProvider.removeModel(ontologyURI);
                resourceManager.removeOntology(ontologyURI);

                return true;
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        } finally {

        }
        return false;
    }

    public boolean deleteResource(String resourceURI) {
        OntModel ontModel = null;
        try {
            String ontologyURI = resourceManager.resolveOntologyURIFromResourceURI(resourceURI);
            if (resourceURI != null && ontologyURI != null) {
                Model baseModel = persistenceProvider.getModel(ontologyURI);
                ontModel = ModelFactory.createOntologyModel(getOntModelSpec(false), baseModel);
                OntResource ontResource = ontModel.getOntResource(resourceURI);
                if (ontResource != null) {
                    ontResource.remove();
                    OntResource xCheck = ontModel.getOntResource(resourceURI);
                    if (xCheck == null) {
                        resourceManager.removeResource(resourceURI);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        } finally {
            persistenceProvider.commit(ontModel);
        }
        return false;
    }

    public ClassContext generateClassContext(String classURI, boolean withInferredAxioms) {

        try {
            ObjectFactory objectFactory = new ObjectFactory();
            String ontologyURI = resourceManager.resolveOntologyURIFromResourceURI(classURI);
            Model baseModel = persistenceProvider.getModel(ontologyURI);
            OntModel ontModel = ModelFactory.createOntologyModel(getOntModelSpec(withInferredAxioms),
                baseModel);
            OntClass ontClass = ontModel.getOntClass(classURI);
            if (this.useReasoner && withInferredAxioms) {
                OWLOntology ontology = jenaToOWlApi(ontModel);
                OWLlinkHTTPXMLReasoner reasoner = getOWLLinkReasoner(ontology);
                OWLDataFactory factory = ontology.getOWLOntologyManager().getOWLDataFactory();
                IRI classIRI = IRI.create(classURI);
                OWLClass owlClass = factory.getOWLClass(classIRI);
                if (ontology.containsClassInSignature(classIRI)) {
                    ClassContext classContext = objectFactory.createClassContext();
                    classContext.setClassMetaInformation(generateClassMetaInformation(classURI));

                    // EquivalentClasses
                    EquivalentClasses equivalentClasses = generateEquivalentClasses(objectFactory, owlClass,
                        reasoner);
                    classContext.setEquivalentClasses(equivalentClasses);

                    // Superclasses
                    Superclasses superclasses = generateSuperClasses(objectFactory, owlClass, reasoner);
                    classContext.setSuperclasses(superclasses);

                    // Disjoint Classes
                    DisjointClasses disjointClasses = generateDisjointClasses(objectFactory, owlClass,
                        reasoner);
                    classContext.setDisjointClasses(disjointClasses);
                    classContext.getClassConstraint().addAll(resolveOntClass(ontClass));

                    // Class Constraints
                    reasoner.answer(new ReleaseKB(reasoner.getDefaultKB()));
                    return classContext;
                }
            } else {
                if (ontClass.getURI() != null) {
                    ClassContext classContext = objectFactory.createClassContext();
                    classContext.setClassMetaInformation(generateClassMetaInformation(classURI));

                    // EquivalentClasses
                    EquivalentClasses equivalentClasses = generateEquivalentClasses(objectFactory, ontClass);
                    classContext.setEquivalentClasses(equivalentClasses);

                    // Superclasses
                    Superclasses superclasses = generateSuperClasses(objectFactory, ontClass);
                    classContext.setSuperclasses(superclasses);

                    // DisjointClasses
                    DisjointClasses disjointClasses = generateDisjointClasses(objectFactory, ontClass);

                    classContext.setDisjointClasses(disjointClasses);
                    classContext.getClassConstraint().addAll(resolveOntClass(ontClass));
                    return classContext;
                }
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return null;
    }

    private Superclasses generateSuperClasses(ObjectFactory objectFactory,
                                              OWLClass owlClass,
                                              OWLReasoner reasoner) {
        Superclasses superclasses = objectFactory.createSuperclasses();
        Set<OWLClass> supClases = reasoner.getSuperClasses(owlClass, false).getFlattened();
        for (OWLClass klazz : supClases) {
            ClassMetaInformation classMetaInformation = generateClassMetaInformation(klazz.getIRI()
                    .toString());
            if (classMetaInformation != null) superclasses.getClassMetaInformation()
                    .add(classMetaInformation);
        }
        return superclasses;
    }

    private Superclasses generateSuperClasses(ObjectFactory objectFactory, OntClass ontClass) {
        Superclasses superclasses = objectFactory.createSuperclasses();
        ExtendedIterator superClassesItr = ontClass.listSuperClasses();
        while (superClassesItr.hasNext()) {
            OntClass curClass = (OntClass) superClassesItr.next();
            String curClass_classURI = curClass.getURI();
            if (curClass_classURI != null) {
                ClassMetaInformation classMetaInformation = generateClassMetaInformation(curClass_classURI);
                if (classMetaInformation != null) {
                    superclasses.getClassMetaInformation().add(classMetaInformation);
                }
            } else {
                logger.debug("super class without uri, localName {}", curClass.getLocalName());
            }
        }
        return superclasses;
    }

    private EquivalentClasses generateEquivalentClasses(ObjectFactory objectFactory, OntClass ontClass) {
        EquivalentClasses equivalentClasses = objectFactory.createEquivalentClasses();
        ExtendedIterator equivalentClassesItr = ontClass.listEquivalentClasses();
        while (equivalentClassesItr.hasNext()) {
            OntClass curClass = (OntClass) equivalentClassesItr.next();
            String curClass_classURI = curClass.getURI();
            if (curClass_classURI != null) {
                ClassMetaInformation classMetaInformation = generateClassMetaInformation(curClass_classURI);
                equivalentClasses.getClassMetaInformation().add(classMetaInformation);
            } else {
                logger.debug("equivalent class without uri, localName {}", curClass.getLocalName());
            }
        }
        return equivalentClasses;
    }

    private EquivalentClasses generateEquivalentClasses(ObjectFactory objectFactory,
                                                        OWLClass owlClass,
                                                        OWLReasoner reasoner) {
        EquivalentClasses equivalentClasses = objectFactory.createEquivalentClasses();
        Set<OWLClass> eqClasses = reasoner.getEquivalentClasses(owlClass).getEntities();
        for (OWLClass klazz : eqClasses) {
            ClassMetaInformation classMetaInformation = generateClassMetaInformation(klazz.getIRI()
                    .toString());
            if (classMetaInformation != null) {
                equivalentClasses.getClassMetaInformation().add(classMetaInformation);
            }
        }
        return equivalentClasses;
    }

    private DisjointClasses generateDisjointClasses(ObjectFactory objectFactory, OntClass ontClass) {
        DisjointClasses disjointClasses = objectFactory.createDisjointClasses();
        ExtendedIterator disjointClassesItr = ontClass.listDisjointWith();
        while (disjointClassesItr.hasNext()) {
            OntClass curClass = (OntClass) disjointClassesItr.next();
            String curClass_classURI = curClass.getURI();
            if (curClass_classURI != null) {
                ClassMetaInformation classMetaInformation = generateClassMetaInformation(curClass_classURI);
                if (classMetaInformation != null) {
                    disjointClasses.getClassMetaInformation().add(classMetaInformation);
                }
            } else {
                logger.debug("disjoint class without uri, localName {}", curClass.getLocalName());
            }
        }
        return disjointClasses;
    }

    private DisjointClasses generateDisjointClasses(ObjectFactory objectFactory,
                                                    OWLClass owlClass,
                                                    OWLReasoner reasoner) {
        DisjointClasses disjointClasses = objectFactory.createDisjointClasses();
        Set<OWLClass> disjClasses = reasoner.getDisjointClasses(owlClass).getFlattened();
        for (OWLClass klazz : disjClasses) {
            ClassMetaInformation classMetaInformation = generateClassMetaInformation(klazz.getIRI()
                    .toString());
            // FIXME URI of OWLNothing is not resolved to any ontologyURI
            if (classMetaInformation != null) {
                disjointClasses.getClassMetaInformation().add(classMetaInformation);
            }
        }
        return disjointClasses;
    }

    public IndividualContext generateIndividualContext(String individualURI, boolean withInferredAxioms) {

        try {
            ObjectFactory objectFactory = new ObjectFactory();
            String ontologyURI = resourceManager.resolveOntologyURIFromResourceURI(individualURI);
            Model baseModel = persistenceProvider.getModel(ontologyURI);
            OntModel ontModel = ModelFactory.createOntologyModel(getOntModelSpec(withInferredAxioms),
                baseModel);

            Individual individual = ontModel.getIndividual(individualURI);
            IndividualContext individualContext = objectFactory.createIndividualContext();
            if (this.useReasoner && withInferredAxioms) {
                OWLOntology ontology = jenaToOWlApi(ontModel);
                OWLlinkHTTPXMLReasoner reasoner = getOWLLinkReasoner(ontology);
                OWLDataFactory factory = ontology.getOWLOntologyManager().getOWLDataFactory();
                IRI individualIRI = IRI.create(individualURI);
                if (ontology.containsIndividualInSignature(individualIRI)) {
                    OWLNamedIndividual owlIndividual = factory.getOWLNamedIndividual(individualIRI);
                    // Container Classes
                    ContainerClasses containerClasses = generateContainerClasses(objectFactory,
                        owlIndividual, reasoner);
                    individualContext.setContainerClasses(containerClasses);

                    // Property Assertions
                    PropertyAssertions propertyAssertions = generatePropertyAssertions(objectFactory,
                        ontology, owlIndividual, reasoner);
                    individualContext.setPropertyAssertions(propertyAssertions);
                    reasoner.answer(new ReleaseKB(reasoner.getDefaultKB()));
                    reasoner = null;
                    return individualContext;
                }
                reasoner.answer(new ReleaseKB(reasoner.getDefaultKB()));
                reasoner = null;
            }
            if (individual.getURI() != null) {

                individualContext
                        .setIndividualMetaInformation(generateIndividualMetaInformation(individualURI));

                // Container Classes
                ContainerClasses containerClasses = generateContainerClasses(objectFactory, individual);
                individualContext.setContainerClasses(containerClasses);

                // Property Assertions
                PropertyAssertions propertyAssertions = generatePropertyAssertions(objectFactory, individual);
                individualContext.setPropertyAssertions(propertyAssertions);
                return individualContext;
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        } finally {

        }
        return null;
    }

    private ContainerClasses generateContainerClasses(ObjectFactory objectFactory, Individual individual) {
        ContainerClasses containerClasses = objectFactory.createContainerClasses();
        ExtendedIterator containerOntClassesItr = individual.listOntClasses(false);
        while (containerOntClassesItr.hasNext()) {
            try {
                Object obj = containerOntClassesItr.next();
                OntClass curClass = (OntClass) obj;
                String curClass_classURI = curClass.getURI();
                if (curClass_classURI != null) {
                    ClassMetaInformation classMetaInformation = generateClassMetaInformation(curClass_classURI);
                    if (classMetaInformation != null) {
                        containerClasses.getClassMetaInformation().add(classMetaInformation);
                    }
                } else {
                    logger.debug("equivalent class without uri, localName {}", curClass.getLocalName());
                }
            } catch (ConversionException ce) {
                logger.warn(ce.getMessage());
            }
        }
        return containerClasses;
    }

    private ContainerClasses generateContainerClasses(ObjectFactory objectFactory,
                                                      OWLNamedIndividual individual,
                                                      OWLReasoner reasoner) {
        ContainerClasses containerClasses = objectFactory.createContainerClasses();
        Set<OWLClass> contClasses = reasoner.getTypes(individual, false).getFlattened();
        for (OWLClass klazz : contClasses) {
            ClassMetaInformation classMetaInformation = generateClassMetaInformation(klazz.getIRI()
                    .toString());
            if (classMetaInformation != null) {
                containerClasses.getClassMetaInformation().add(classMetaInformation);
            }
        }
        return containerClasses;
    }

    private PropertyAssertions generatePropertyAssertions(ObjectFactory objectFactory, Individual individual) {
        PropertyAssertions propertyAssertions = objectFactory.createPropertyAssertions();
        StmtIterator propertiesItr = individual.listProperties();
        Set<String> knownProperties = new HashSet<String>();
        while (propertiesItr.hasNext()) {
            Statement curStmt = (Statement) propertiesItr.next();
            Property curProperty = curStmt.getPredicate();
            PropertyMetaInformation propertyMetaInformation;
            if (curProperty.getURI() != null) {
                if (knownProperties.contains(curProperty.getURI())) {
                    continue;
                } else {
                    propertyMetaInformation = generatePropertyMetaInformation(curProperty.getURI());
                    knownProperties.add(curProperty.getURI());
                }
                if (propertyMetaInformation != null) {
                    PropertyAssertion propertyAssertion = objectFactory
                            .createPropertyAssertionsPropertyAssertion();
                    propertyAssertion.setPropertyMetaInformation(propertyMetaInformation);
                    NodeIterator propertyValuesItr = individual.listPropertyValues(curProperty);
                    while (propertyValuesItr.hasNext()) {
                        RDFNode curValue = (RDFNode) propertyValuesItr.next();
                        if (curValue.isLiteral()) {
                            propertyAssertion.getIndividualMetaInformationOrLiteral()
                                    .add(curValue.toString());
                        } else {
                            IndividualMetaInformation individualMetaInformation = generateIndividualMetaInformation(curValue
                                    .toString());
                            if (individualMetaInformation != null) {
                                propertyAssertion.getIndividualMetaInformationOrLiteral().add(
                                    individualMetaInformation);
                            } else {
                                logger.debug("Unable to resolve property value {}", curValue.toString());
                            }
                        }
                    }
                    propertyAssertions.getPropertyAssertion().add(propertyAssertion);
                }
            }
        }
        return propertyAssertions;
    }

    private PropertyAssertions generatePropertyAssertions(ObjectFactory objectFactory,
                                                          OWLOntology ontology,
                                                          OWLNamedIndividual individual,
                                                          OWLReasoner reasoner) {
        PropertyAssertions propertyAssertions = objectFactory.createPropertyAssertions();
        for (OWLDataProperty dataProp : ontology.getDataPropertiesInSignature()) {
            PropertyMetaInformation propertyMetaInformation = generatePropertyMetaInformation(dataProp
                    .getIRI().toString());
            PropertyAssertion propertyAssertion = null;
            for (OWLLiteral literal : reasoner.getDataPropertyValues(individual, dataProp)) {
                if (propertyAssertion == null) {
                    propertyAssertion = objectFactory.createPropertyAssertionsPropertyAssertion();
                    propertyAssertion.setPropertyMetaInformation(propertyMetaInformation);
                }
                propertyAssertion.getIndividualMetaInformationOrLiteral().add(literal.getLiteral());
            }
            if (propertyAssertion != null) {
                propertyAssertions.getPropertyAssertion().add(propertyAssertion);
            }
        }

        for (OWLObjectProperty objetProp : ontology.getObjectPropertiesInSignature()) {
            PropertyMetaInformation propertyMetaInformation = generatePropertyMetaInformation(objetProp
                    .getIRI().toString());
            PropertyAssertion propertyAssertion = null;
            for (OWLNamedIndividual ind : reasoner.getObjectPropertyValues(individual, objetProp)
                    .getFlattened()) {
                if (propertyAssertion == null) {
                    propertyAssertion = objectFactory.createPropertyAssertionsPropertyAssertion();
                    propertyAssertion.setPropertyMetaInformation(propertyMetaInformation);
                }
                propertyAssertion.getIndividualMetaInformationOrLiteral().add(
                    generateIndividualMetaInformation(ind.getIRI().toString()));
            }
            if (propertyAssertion != null) {
                propertyAssertions.getPropertyAssertion().add(propertyAssertion);
            }
        }

        return propertyAssertions;
    }

    public DatatypePropertyContext generateDatatypePropertyContext(String datatypePropertyURI,
                                                                   boolean withInferredAxioms) {

        try {
            ObjectFactory objectFactory = new ObjectFactory();
            String ontologyURI = resourceManager.resolveOntologyURIFromResourceURI(datatypePropertyURI);
            Model baseModel = persistenceProvider.getModel(ontologyURI);
            OntModel ontModel = ModelFactory.createOntologyModel(getOntModelSpec(withInferredAxioms),
                baseModel);

            DatatypeProperty datatypeProperty = ontModel.getDatatypeProperty(datatypePropertyURI);

            if (this.useReasoner && withInferredAxioms) {
                OWLOntology owlOntology = jenaToOWlApi(ontModel);
                OWLlinkHTTPXMLReasoner reasoner = getOWLLinkReasoner(owlOntology);
                OWLDataFactory factory = owlOntology.getOWLOntologyManager().getOWLDataFactory();
                IRI owlDataPropertyIRI = IRI.create(datatypePropertyURI);
                OWLDataProperty owlDataProperty = factory.getOWLDataProperty(owlDataPropertyIRI);
                if (owlOntology.containsDataPropertyInSignature(owlDataPropertyIRI)) {
                    DatatypePropertyContext datatypePropertyContext = objectFactory
                            .createDatatypePropertyContext();
                    datatypePropertyContext
                            .setPropertyMetaInformation(generatePropertyMetaInformation(datatypePropertyURI));
                    // Domain
                    Domain domain = generateDatatypePropertyDomain(objectFactory, owlDataProperty, reasoner);
                    if (domain != null) datatypePropertyContext.setDomain(domain);

                    // Range
                    Range range = generateDatatypePropertyRange(objectFactory, datatypeProperty);
                    if (range != null) datatypePropertyContext.setRange(range);

                    // isFunctional
                    // FIXME How to understand that reasoner concluded the
                    // datatypeProperty is functional or not
                    datatypePropertyContext.setIsFunctional(datatypeProperty.isFunctionalProperty());

                    // Equivalent Properties
                    EquivalentProperties equivalentProperties = generateDatatypePropertyEquivalentProperties(
                        objectFactory, owlDataProperty, reasoner);
                    datatypePropertyContext.setEquivalentProperties(equivalentProperties);

                    // Super Properties
                    SuperProperties superProperties = generateDatatypePropertySuperProperties(objectFactory,
                        owlDataProperty, reasoner);
                    datatypePropertyContext.setSuperProperties(superProperties);
                    reasoner.answer(new ReleaseKB(reasoner.getDefaultKB()));
                    reasoner = null;
                    return datatypePropertyContext;
                }
                reasoner.answer(new ReleaseKB(reasoner.getDefaultKB()));
                reasoner = null;
            } else {
                if (datatypeProperty.getURI() != null) {
                    DatatypePropertyContext datatypePropertyContext = objectFactory
                            .createDatatypePropertyContext();
                    datatypePropertyContext
                            .setPropertyMetaInformation(generatePropertyMetaInformation(datatypePropertyURI));

                    // Domain
                    Domain domain = generateDatatypePropertyDomain(objectFactory, datatypeProperty);
                    if (domain != null) datatypePropertyContext.setDomain(domain);

                    // Range
                    Range range = generateDatatypePropertyRange(objectFactory, datatypeProperty);
                    if (range != null) datatypePropertyContext.setRange(range);

                    // isFunctional
                    datatypePropertyContext.setIsFunctional(datatypeProperty.isFunctionalProperty());

                    // Equivalent Properties
                    EquivalentProperties equivalentProperties = generateDatatypePropertyEquivalentProperties(
                        objectFactory, datatypeProperty);
                    datatypePropertyContext.setEquivalentProperties(equivalentProperties);

                    // Super Properties
                    SuperProperties superProperties = generateDatatypePropertySuperProperties(objectFactory,
                        datatypeProperty);
                    datatypePropertyContext.setSuperProperties(superProperties);
                    return datatypePropertyContext;
                }
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        } finally {

        }
        return null;
    }

    private Domain generateDatatypePropertyDomain(ObjectFactory objectFactory,
                                                  DatatypeProperty datatypeProperty) {
        List<? extends OntResource> domainResources = datatypeProperty.listDomain().toList();
        Domain domain = objectFactory.createDomain();
        for (OntResource domainResource : domainResources) {
            if (domainResource != null && domainResource.isClass()) {
                OntClass domainClass = domainResource.asClass();
                String resourceURI = domainClass.getURI();
                if (resourceURI != null) {
                    if (JenaUtil.isBuiltInClass(resourceURI)) {
                        BuiltInResource builtInResource = objectFactory.createBuiltInResource();
                        builtInResource.setURI(resourceURI);
                        domain.getClassMetaInformationOrBuiltInResource().add(builtInResource);
                    } else {
                        domain.getClassMetaInformationOrBuiltInResource().add(
                            generateClassMetaInformation(domainClass.getURI()));
                    }
                } else {
                    logger.debug(
                        "domain for datatypeProperty {}  is resolved to OntClass but it does not have URI",
                        datatypeProperty.getURI());
                }
            } else {
                logger.debug("domain for datatypeProperty {} cannot be resolved to OntClass",
                    datatypeProperty.getURI());
            }
        }
        return domain;
    }

    private Domain generateDatatypePropertyDomain(ObjectFactory objectFactory,
                                                  OWLDataProperty owlDataProperty,
                                                  OWLReasoner reasoner) {
        Set<OWLClass> domainClasses = reasoner.getDataPropertyDomains(owlDataProperty, false).getFlattened();
        Domain domain = objectFactory.createDomain();
        for (OWLClass domainClass : domainClasses) {

            if (domainClass.isOWLThing() || domainClass.isOWLNothing()) {
                BuiltInResource builtInResource = objectFactory.createBuiltInResource();
                builtInResource.setURI(domainClass.getIRI().toString());
                domain.getClassMetaInformationOrBuiltInResource().add(builtInResource);
            } else {
                domain.getClassMetaInformationOrBuiltInResource().add(
                    generateClassMetaInformation(domainClass.getIRI().toString()));
            }
        }
        return domain;
    }

    private Range generateDatatypePropertyRange(ObjectFactory objectFactory, DatatypeProperty datatypeProperty) {
        List<? extends OntResource> rangeResources = datatypeProperty.listRange().toList();
        Range range = objectFactory.createRange();
        for (OntResource rangeResource : rangeResources) {
            if (rangeResource != null && rangeResource.isClass()) {
                OntClass rangeClass = rangeResource.asClass();
                String resourceURI = rangeClass.getURI();
                if (resourceURI != null) {
                    if (JenaUtil.isBuiltInClass(resourceURI) || JenaUtil.isBuiltInType(resourceURI)) {
                        BuiltInResource builtInResource = objectFactory.createBuiltInResource();
                        builtInResource.setURI(resourceURI);
                        range.getClassMetaInformationOrBuiltInResource().add(builtInResource);
                    } else {
                        range.getClassMetaInformationOrBuiltInResource().add(
                            generateClassMetaInformation(rangeClass.getURI()));
                    }
                } else {
                    logger.debug(
                        "range for datatypeProperty {}  is resolved to OntClass but it does not have URI",
                        datatypeProperty.getURI());
                }
            } else {
                logger.debug("domain for datatypeProperty {}  cannot be resolved to OntClass",
                    datatypeProperty.getURI());
            }
        }
        return range;
    }

    // FIXME How to get DatatypeProperty ranges in OWLReasoner

    private EquivalentProperties generateDatatypePropertyEquivalentProperties(ObjectFactory objectFactory,
                                                                              DatatypeProperty datatypeProperty) {
        EquivalentProperties equivalentProperties = objectFactory.createEquivalentProperties();
        ExtendedIterator equivalentPropertiesItr = datatypeProperty.listEquivalentProperties();
        while (equivalentPropertiesItr.hasNext()) {
            OntProperty curEquivalentProperty = (OntProperty) equivalentPropertiesItr.next();
            if (curEquivalentProperty.getURI() != null) {
                PropertyMetaInformation datatypePropertyMetaInformation = generatePropertyMetaInformation(curEquivalentProperty
                        .getURI());
                equivalentProperties.getPropertyMetaInformation().add(datatypePropertyMetaInformation);
            } else {
                logger.debug("equivalent property with uri, localName {}",
                    curEquivalentProperty.getLocalName());
            }
        }
        return equivalentProperties;
    }

    private EquivalentProperties generateDatatypePropertyEquivalentProperties(ObjectFactory objectFactory,
                                                                              OWLDataProperty owlDataProperty,
                                                                              OWLReasoner reasoner) {
        EquivalentProperties equivalentProperties = objectFactory.createEquivalentProperties();
        Set<OWLDataProperty> equiProperties = reasoner.getEquivalentDataProperties(owlDataProperty)
                .getEntities();
        for (OWLDataProperty dataProp : equiProperties) {
            PropertyMetaInformation datatypePropertyMetaInformation = generatePropertyMetaInformation(dataProp
                    .getIRI().toString());
            equivalentProperties.getPropertyMetaInformation().add(datatypePropertyMetaInformation);
        }
        return equivalentProperties;
    }

    private SuperProperties generateDatatypePropertySuperProperties(ObjectFactory objectFactory,
                                                                    DatatypeProperty datatypeProperty) {
        SuperProperties superProperties = objectFactory.createSuperProperties();
        ExtendedIterator superPropertiesItr = datatypeProperty.listSuperProperties();
        while (superPropertiesItr.hasNext()) {
            OntProperty curSuperProperty = (OntProperty) superPropertiesItr.next();
            if (curSuperProperty.getURI() != null) {
                PropertyMetaInformation datatypePropertyMetaInformation = generatePropertyMetaInformation(curSuperProperty
                        .getURI());
                superProperties.getPropertyMetaInformation().add(datatypePropertyMetaInformation);
            } else {
                logger.debug("equivalent property with uri, localName {}", curSuperProperty.getLocalName());
            }
        }
        return superProperties;
    }

    private SuperProperties generateDatatypePropertySuperProperties(ObjectFactory objectFactory,
                                                                    OWLDataProperty owlDataProperty,
                                                                    OWLReasoner reasoner) {
        SuperProperties superProperties = objectFactory.createSuperProperties();
        Set<OWLDataProperty> supProperties = reasoner.getSuperDataProperties(owlDataProperty, false)
                .getFlattened();
        for (OWLDataProperty dataProp : supProperties) {
            PropertyMetaInformation datatypePropertyMetaInformation = generatePropertyMetaInformation(dataProp
                    .getIRI().toString());
            superProperties.getPropertyMetaInformation().add(datatypePropertyMetaInformation);
        }
        return superProperties;
    }

    public ObjectPropertyContext generateObjectPropertyContext(String objectPropertyURI,
                                                               boolean withInferredAxioms) {
        try {
            ObjectFactory objectFactory = new ObjectFactory();
            String ontologyURI = resourceManager.resolveOntologyURIFromResourceURI(objectPropertyURI);
            Model baseModel = persistenceProvider.getModel(ontologyURI);
            OntModel ontModel = ModelFactory.createOntologyModel(getOntModelSpec(withInferredAxioms),
                baseModel);
            ObjectProperty objectProperty = ontModel.getObjectProperty(objectPropertyURI);
            if (this.useReasoner && withInferredAxioms) {
                OWLOntology owlOntology = jenaToOWlApi(ontModel);
                OWLlinkHTTPXMLReasoner reasoner = getOWLLinkReasoner(owlOntology);
                OWLDataFactory factory = owlOntology.getOWLOntologyManager().getOWLDataFactory();
                IRI owlObjectPropertyIRI = IRI.create(objectPropertyURI);
                OWLObjectProperty owlObjectProperty = factory.getOWLObjectProperty(owlObjectPropertyIRI);
                if (owlOntology.containsObjectPropertyInSignature(owlObjectPropertyIRI)) {
                    ObjectPropertyContext objectPropertyContext = objectFactory.createObjectPropertyContext();
                    objectPropertyContext
                            .setPropertyMetaInformation(generatePropertyMetaInformation(objectPropertyURI));
                    Domain domain = generateObjectPropertyDomain(objectFactory, owlObjectProperty, reasoner);
                    if (domain != null) {
                        objectPropertyContext.setDomain(domain);
                    }

                    // Range
                    Range range = generateObjectPropertyRange(objectFactory, owlObjectProperty, reasoner);
                    if (range != null) {
                        objectPropertyContext.setRange(range);
                    }
                    // isFunctional
                    objectPropertyContext.setIsFunctional(objectProperty.isFunctionalProperty());
                    objectPropertyContext
                            .setIsInverseFunctional(objectProperty.isInverseFunctionalProperty());
                    objectPropertyContext.setIsTransitive(objectProperty.isTransitiveProperty());
                    objectPropertyContext.setIsSymmetric(objectProperty.isSymmetricProperty());
                    // Equivalent Properties
                    EquivalentProperties equivalentProperties = generateObjectPropertyEquivalentProperties(
                        objectFactory, owlObjectProperty, reasoner);
                    objectPropertyContext.setEquivalentProperties(equivalentProperties);
                    // Super Properties
                    SuperProperties superProperties = generateObjectPropertySuperProperties(objectFactory,
                        owlObjectProperty, reasoner);
                    objectPropertyContext.setSuperProperties(superProperties);
                    reasoner.answer(new ReleaseKB(reasoner.getDefaultKB()));
                    reasoner = null;
                    return objectPropertyContext;
                }
                reasoner.answer(new ReleaseKB(reasoner.getDefaultKB()));
                reasoner = null;
            } else {
                if (objectProperty.getURI() != null) {
                    ObjectPropertyContext objectPropertyContext = objectFactory.createObjectPropertyContext();
                    objectPropertyContext
                            .setPropertyMetaInformation(generatePropertyMetaInformation(objectPropertyURI));

                    // Domain
                    // FIXME Current Schema can not handle multiple domains and
                    // ranges
                    Domain domain = generateObjectPropertyDomain(objectFactory, objectProperty);
                    if (domain != null) {
                        objectPropertyContext.setDomain(domain);
                    }

                    // Range
                    Range range = generateObjectPropertyRange(objectFactory, objectProperty);
                    if (range != null) {
                        objectPropertyContext.setRange(range);
                    }
                    // isFunctional
                    objectPropertyContext.setIsFunctional(objectProperty.isFunctionalProperty());
                    objectPropertyContext
                            .setIsInverseFunctional(objectProperty.isInverseFunctionalProperty());
                    objectPropertyContext.setIsTransitive(objectProperty.isTransitiveProperty());
                    objectPropertyContext.setIsSymmetric(objectProperty.isSymmetricProperty());

                    // Equivalent Properties
                    EquivalentProperties equivalentProperties = generateObjectPropertyEquivalentProperties(
                        objectFactory, objectProperty);
                    objectPropertyContext.setEquivalentProperties(equivalentProperties);

                    // Super Properties
                    SuperProperties superProperties = generateObjectPropertySuperProperties(objectFactory,
                        objectProperty);
                    objectPropertyContext.setSuperProperties(superProperties);
                    return objectPropertyContext;
                }
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        } finally {

        }
        return null;
    }

    private Domain generateObjectPropertyDomain(ObjectFactory objectFactory, ObjectProperty objectProperty) {
        List<? extends OntResource> domainResources = objectProperty.listDomain().toList();
        Domain domain = objectFactory.createDomain();
        for (OntResource domainResource : domainResources) {
            if (domainResource != null && domainResource.isClass()) {
                OntClass domainClass = domainResource.asClass();
                String resourceURI = domainResource.getURI();
                if (domainClass.getURI() != null) {
                    if (JenaUtil.isBuiltInClass(domainClass.getURI())) {
                        BuiltInResource builtInResource = objectFactory.createBuiltInResource();
                        builtInResource.setURI(resourceURI);
                        domain.getClassMetaInformationOrBuiltInResource().add(builtInResource);
                    } else {
                        domain.getClassMetaInformationOrBuiltInResource().add(
                            generateClassMetaInformation(domainClass.getURI()));
                    }
                } else {
                    logger.debug(
                        "domain for datatypeProperty {} is resolved to OntClass but it does not have URI",
                        objectProperty.getURI());
                }
            } else {
                logger.debug("domain for datatypeProperty {} cannot be resolved to OntClass",
                    objectProperty.getURI());
            }
        }
        return domain;
    }

    private Domain generateObjectPropertyDomain(ObjectFactory objectFactory,
                                                OWLObjectProperty owlObjectProperty,
                                                OWLReasoner reasoner) {
        Set<OWLClass> domainClasses = reasoner.getObjectPropertyDomains(owlObjectProperty, false)
                .getFlattened();
        Domain domain = objectFactory.createDomain();
        for (OWLClass domainClass : domainClasses) {
            if (domainClass.isOWLThing() || domainClass.isOWLNothing()) {
                BuiltInResource builtInResource = objectFactory.createBuiltInResource();
                builtInResource.setURI(domainClass.getIRI().toString());
                domain.getClassMetaInformationOrBuiltInResource().add(builtInResource);
            } else {
                domain.getClassMetaInformationOrBuiltInResource().add(
                    generateClassMetaInformation(domainClass.getIRI().toString()));
            }
        }
        return domain;
    }

    private Range generateObjectPropertyRange(ObjectFactory objectFactory, ObjectProperty objectProperty) {
        List<? extends OntResource> rangeResources = objectProperty.listRange().toList();
        Range range = objectFactory.createRange();
        for (OntResource rangeResource : rangeResources) {
            if (rangeResource != null && rangeResource.isClass()) {
                OntClass rangeClass = rangeResource.asClass();
                String resourceURI = rangeClass.getURI();
                if (resourceURI != null) {
                    if (JenaUtil.isBuiltInClass(resourceURI) || JenaUtil.isBuiltInType(resourceURI)) {
                        BuiltInResource builtInResource = objectFactory.createBuiltInResource();
                        builtInResource.setURI(resourceURI);
                        range.getClassMetaInformationOrBuiltInResource().add(builtInResource);
                    } else {
                        range.getClassMetaInformationOrBuiltInResource().add(
                            generateClassMetaInformation(rangeClass.getURI()));
                    }
                } else {
                    logger.debug(
                        "range for datatypeProperty {}  is resolved to OntClass but it does not have URI",
                        objectProperty.getURI());
                }
            } else {
                logger.debug("domain for datatypeProperty {}  cannot be resolved to OntClass",
                    objectProperty.getURI());
            }
        }
        return range;
    }

    private Range generateObjectPropertyRange(ObjectFactory objectFactory,
                                              OWLObjectProperty owlObjectProperty,
                                              OWLReasoner reasoner) {
        Set<OWLClass> rangeClasses = reasoner.getObjectPropertyRanges(owlObjectProperty, false)
                .getFlattened();
        Range range = objectFactory.createRange();
        for (OWLClass rangeClass : rangeClasses) {

            if (rangeClass.isOWLThing() || rangeClass.isOWLNothing()) {
                BuiltInResource builtInResource = objectFactory.createBuiltInResource();
                builtInResource.setURI(rangeClass.getIRI().toString());
                range.getClassMetaInformationOrBuiltInResource().add(builtInResource);
            } else {
                range.getClassMetaInformationOrBuiltInResource().add(
                    generateClassMetaInformation(rangeClass.getIRI().toString()));
            }
        }
        return range;
    }

    private EquivalentProperties generateObjectPropertyEquivalentProperties(ObjectFactory objectFactory,
                                                                            ObjectProperty objectProperty) {
        EquivalentProperties equivalentProperties = objectFactory.createEquivalentProperties();
        ExtendedIterator equivalentPropertiesItr = objectProperty.listEquivalentProperties();
        while (equivalentPropertiesItr.hasNext()) {
            OntProperty curEquivalentProperty = (OntProperty) equivalentPropertiesItr.next();
            if (curEquivalentProperty.getURI() != null) {
                PropertyMetaInformation datatypePropertyMetaInformation = generatePropertyMetaInformation(curEquivalentProperty
                        .getURI());
                equivalentProperties.getPropertyMetaInformation().add(datatypePropertyMetaInformation);
            } else {
                logger.debug("equivalent property with uri, localName {}",
                    curEquivalentProperty.getLocalName());
            }
        }
        return equivalentProperties;
    }

    private EquivalentProperties generateObjectPropertyEquivalentProperties(ObjectFactory objectFactory,
                                                                            OWLObjectProperty owlObjectProperty,
                                                                            OWLReasoner reasoner) {
        EquivalentProperties equivalentProperties = objectFactory.createEquivalentProperties();
        Set<OWLObjectPropertyExpression> equiProperties = reasoner.getEquivalentObjectProperties(owlObjectProperty)
                .getEntities();
        for (OWLObjectPropertyExpression objectProp : equiProperties) {

            PropertyMetaInformation datatypePropertyMetaInformation = generatePropertyMetaInformation(objectProp
                    .getNamedProperty().getIRI().toString());
            equivalentProperties.getPropertyMetaInformation().add(datatypePropertyMetaInformation);
        }
        return equivalentProperties;
    }

    private SuperProperties generateObjectPropertySuperProperties(ObjectFactory objectFactory,
                                                                  ObjectProperty objectProperty) {
        SuperProperties superProperties = objectFactory.createSuperProperties();
        ExtendedIterator superPropertiesItr = objectProperty.listSuperProperties();
        while (superPropertiesItr.hasNext()) {
            OntProperty curSuperProperty = (OntProperty) superPropertiesItr.next();
            if (curSuperProperty.getURI() != null) {
                PropertyMetaInformation datatypePropertyMetaInformation = generatePropertyMetaInformation(curSuperProperty
                        .getURI());
                superProperties.getPropertyMetaInformation().add(datatypePropertyMetaInformation);
            } else {
                logger.debug("equivalent property with uri, localName {}", curSuperProperty.getLocalName());
            }
        }
        return superProperties;
    }

    private SuperProperties generateObjectPropertySuperProperties(ObjectFactory objectFactory,
                                                                  OWLObjectProperty owlObjectProperty,
                                                                  OWLReasoner reasoner) {
        SuperProperties superProperties = objectFactory.createSuperProperties();
        Set<OWLObjectPropertyExpression> supProperties = reasoner.getSuperObjectProperties(owlObjectProperty, false)
                .getFlattened();
        for (OWLObjectPropertyExpression objectProp : supProperties) {
            PropertyMetaInformation datatypePropertyMetaInformation = generatePropertyMetaInformation(objectProp
                    .getNamedProperty().getIRI().toString());
            superProperties.getPropertyMetaInformation().add(datatypePropertyMetaInformation);
        }
        return superProperties;
    }

    public boolean addDisjointClass(String classURI, String disjointClassURI) throws Exception {

        try {
            String class_ontologyURI = resourceManager.resolveOntologyURIFromResourceURI(classURI);
            Model class_baseModel = persistenceProvider.getModel(class_ontologyURI);
            OntModel class_ontModel = ModelFactory.createOntologyModel(getOntModelSpec(false),
                class_baseModel);
            OntClass ontClass = class_ontModel.getOntClass(classURI);

            String disjointClass_ontologyURI = resourceManager
                    .resolveOntologyURIFromResourceURI(disjointClassURI);
            Model disjointClass_baseModel = persistenceProvider.getModel(disjointClass_ontologyURI);
            OntModel disjointClass_ontModel = ModelFactory.createOntologyModel(getOntModelSpec(false),
                disjointClass_baseModel);
            OntClass disjointOntClass = disjointClass_ontModel.getOntClass(disjointClassURI);

            if (ontClass != null && disjointOntClass != null) {
                ontClass.addDisjointWith(disjointOntClass);
                return true;
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return false;
    }

    public boolean deleteDisjointClass(String classURI, String disjointClassURI) throws Exception {

        try {
            String class_ontologyURI = resourceManager.resolveOntologyURIFromResourceURI(classURI);
            Model class_baseModel = persistenceProvider.getModel(class_ontologyURI);
            OntModel class_ontModel = ModelFactory.createOntologyModel(getOntModelSpec(false),
                class_baseModel);
            OntClass ontClass = class_ontModel.getOntClass(classURI);

            String disjointClass_ontologyURI = resourceManager
                    .resolveOntologyURIFromResourceURI(disjointClassURI);
            Model disjointClass_baseModel = persistenceProvider.getModel(disjointClass_ontologyURI);
            OntModel disjointClass_ontModel = ModelFactory.createOntologyModel(getOntModelSpec(false),
                disjointClass_baseModel);
            OntClass disjointOntClass = disjointClass_ontModel.getOntClass(disjointClassURI);

            if (ontClass != null && disjointOntClass != null) {
                ontClass.removeDisjointWith(disjointOntClass);
                return true;
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return false;
    }

    public boolean addEquivalentClass(String classURI, String equivalentClassURI) throws Exception {

        try {
            String class_ontologyURI = resourceManager.resolveOntologyURIFromResourceURI(classURI);
            Model class_baseModel = persistenceProvider.getModel(class_ontologyURI);
            OntModel class_ontModel = ModelFactory.createOntologyModel(getOntModelSpec(false),
                class_baseModel);
            OntClass ontClass = class_ontModel.getOntClass(classURI);

            String equivalentClass_ontologyURI = resourceManager
                    .resolveOntologyURIFromResourceURI(equivalentClassURI);
            Model equivalentClass_baseModel = persistenceProvider.getModel(equivalentClass_ontologyURI);
            OntModel equivalentClass_ontModel = ModelFactory.createOntologyModel(getOntModelSpec(false),
                equivalentClass_baseModel);
            OntClass equivalentOntClass = equivalentClass_ontModel.getOntClass(equivalentClassURI);

            if (ontClass != null && equivalentOntClass != null) {
                ontClass.addEquivalentClass(equivalentOntClass);
                return true;
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return false;
    }

    public boolean deleteEquivalentClass(String classURI, String equivalentClassURI) throws Exception {

        try {
            String class_ontologyURI = resourceManager.resolveOntologyURIFromResourceURI(classURI);
            Model class_baseModel = persistenceProvider.getModel(class_ontologyURI);
            OntModel class_ontModel = ModelFactory.createOntologyModel(getOntModelSpec(false),
                class_baseModel);
            OntClass ontClass = class_ontModel.getOntClass(classURI);

            String equivalentClass_ontologyURI = resourceManager
                    .resolveOntologyURIFromResourceURI(equivalentClassURI);
            Model equivalentClass_baseModel = persistenceProvider.getModel(equivalentClass_ontologyURI);
            OntModel equivalentClass_ontModel = ModelFactory.createOntologyModel(getOntModelSpec(false),
                equivalentClass_baseModel);
            OntClass equivalentOntClass = equivalentClass_ontModel.getOntClass(equivalentClassURI);

            if (ontClass != null && equivalentOntClass != null) {
                ontClass.removeEquivalentClass(equivalentOntClass);
                return true;
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return false;
    }

    public boolean makeUnionClassOf(String classURI, List<String> unionClassURIs) throws Exception {

        try {
            boolean successful = true;

            String class_ontologyURI = resourceManager.resolveOntologyURIFromResourceURI(classURI);
            Model class_baseModel = persistenceProvider.getModel(class_ontologyURI);
            OntModel class_ontModel = ModelFactory.createOntologyModel(getOntModelSpec(false),
                class_baseModel);
            try {
                OntClass ontClass = class_ontModel.getOntClass(classURI);
                RDFList rdfList = class_ontModel.createList();

                Iterator<String> unionClassURIsItr = unionClassURIs.iterator();
                while (unionClassURIsItr.hasNext()) {
                    String curUnionClassURI = unionClassURIsItr.next();
                    String curUnionClass_ontologyURI = resourceManager
                            .resolveOntologyURIFromResourceURI(curUnionClassURI);

                    Model curUnionClass_baseModel = persistenceProvider.getModel(curUnionClass_ontologyURI);
                    OntModel curUnionClass_ontModel = ModelFactory.createOntologyModel(
                        getOntModelSpec(false), curUnionClass_baseModel);
                    try {
                        OntClass curUnionOntClass = curUnionClass_ontModel.getOntClass(curUnionClassURI);
                        if (curUnionOntClass != null) {
                            rdfList = rdfList.cons(curUnionOntClass);
                        } else {
                            successful = false;
                        }
                    } finally {
                        persistenceProvider.commit(curUnionClass_ontModel);
                    }
                }
                if (successful) {
                    ontClass.convertToUnionClass(rdfList);
                    return true;
                }
            } finally {
                persistenceProvider.commit(class_ontModel);
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        } finally {
            // closeDBConnection(m_conn);
        }
        return false;
    }

    public boolean addUnionClass(String classURI, String unionClassURI) throws Exception {
        try {
            String class_ontologyURI = resourceManager.resolveOntologyURIFromResourceURI(classURI);
            Model class_baseModel = persistenceProvider.getModel(class_ontologyURI);
            OntModel class_ontModel = ModelFactory.createOntologyModel(getOntModelSpec(false),
                class_baseModel);
            OntClass ontClass = class_ontModel.getOntClass(classURI);

            String curUnionClass_ontologyURI = resourceManager
                    .resolveOntologyURIFromResourceURI(unionClassURI);

            Model curUnionClass_baseModel = persistenceProvider.getModel(curUnionClass_ontologyURI);
            OntModel unionClass_ontModel = ModelFactory.createOntologyModel(getOntModelSpec(false),
                curUnionClass_baseModel);

            OntClass unionOntClass = unionClass_ontModel.getOntClass(unionClassURI);
            if (unionOntClass != null) {
                // Check if we can view as UnionClass
                if (ontClass.isUnionClass()) {
                    // Already union class just add the new union class
                    // assertion
                    ontClass.asUnionClass().addOperand(unionOntClass);
                } else {
                    RDFList unionList = class_baseModel.createList();
                    unionList.cons(unionOntClass);
                    ontClass.convertToUnionClass(unionList);
                }
                return true;
            }

        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return false;
    }

    public boolean deleteUnionClass(String classURI, String unionClassURI) throws Exception {
        try {
            String class_ontologyURI = resourceManager.resolveOntologyURIFromResourceURI(classURI);
            Model class_baseModel = persistenceProvider.getModel(class_ontologyURI);
            OntModel class_ontModel = ModelFactory.createOntologyModel(getOntModelSpec(false),
                class_baseModel);
            OntClass ontClass = class_ontModel.getOntClass(classURI);

            String curUnionClass_ontologyURI = resourceManager
                    .resolveOntologyURIFromResourceURI(unionClassURI);

            Model curUnionClass_baseModel = persistenceProvider.getModel(curUnionClass_ontologyURI);
            OntModel unionClass_ontModel = ModelFactory.createOntologyModel(getOntModelSpec(false),
                curUnionClass_baseModel);

            OntClass unionOntClass = unionClass_ontModel.getOntClass(unionClassURI);
            if (unionOntClass != null) {
                // Check if we can view as UnionClass
                if (ontClass.isUnionClass()) {
                    // Already union class just add the new union class
                    // assertion
                    ontClass.asUnionClass().removeOperand(unionOntClass);
                    return true;
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return false;
    }

    public ResourceMetaInformationType retrieveResourceWithURI(String resourceURI) throws Exception {

        try {
            ObjectFactory objectFactory = new ObjectFactory();
            String ontologyURI = resourceManager.resolveOntologyURIFromResourceURI(resourceURI);
            Model baseModel = persistenceProvider.getModel(ontologyURI);
            OntModel ontModel = ModelFactory.createOntologyModel(getOntModelSpec(false), baseModel);
            OntResource ontResource = ontModel.getOntResource(resourceURI);
            if (ontResource != null) {
                ResourceMetaInformationType resourceMetaInformationType = null;
                if (ontResource.isIndividual()) {
                    resourceMetaInformationType = objectFactory.createIndividualMetaInformation();
                } else if (ontResource.isClass()) {
                    resourceMetaInformationType = objectFactory.createClassMetaInformation();
                } else if (ontResource.isProperty()) {
                    resourceMetaInformationType = objectFactory.createPropertyMetaInformation();
                }
                resourceMetaInformationType.setURI(ontResource.getURI());
                resourceMetaInformationType.setNamespace(ontResource.getNameSpace());
                resourceMetaInformationType.setLocalName(ontResource.getLocalName());
                resourceMetaInformationType.setHref(resourceManager.getResourceFullPath(resourceURI));
                /** FIXME: Descriptions have to be administerable **/
                resourceMetaInformationType.setDescription("");
                return resourceMetaInformationType;
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        } finally {

        }
        return null;
    }

    

    /**
     * Non-Interface Functions *
     * 
     * 
     */

    private List<String> getSavedOntologyURIs() {
        return persistenceProvider.listModels();
    }

    private ClassMetaInformation generateClassMetaInformation(String classURI) {
        try {
            ObjectFactory objectFactory = new ObjectFactory();
            String ontologyURI = resourceManager.resolveOntologyURIFromResourceURI(classURI);
            if (classURI != null && ontologyURI != null) {
                // The class description exists in ResourceManager
                Model baseModel = persistenceProvider.getModel(ontologyURI);
                OntModel ontModel = ModelFactory.createOntologyModel(getOntModelSpec(false), baseModel);
                OntClass ontClass = ontModel.getOntClass(classURI);
                if (ontClass != null && ontClass.getURI() != null) {
                    ClassMetaInformation classMetaInformation = objectFactory.createClassMetaInformation();
                    classMetaInformation.setHref(resourceManager.getResourceFullPath(ontClass.getURI()));
                    classMetaInformation.setNamespace(ontClass.getNameSpace());
                    classMetaInformation.setLocalName(ontClass.getLocalName());
                    classMetaInformation.setURI(ontClass.getURI());
                    /** FIXME: Description has to be administerable **/
                    classMetaInformation.setDescription("");
                    return classMetaInformation;
                }
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return null;
    }

    private PropertyMetaInformation generatePropertyMetaInformation(String propertyURI) {
        try {
            ObjectFactory objectFactory = new ObjectFactory();
            String ontologyURI = resourceManager.resolveOntologyURIFromResourceURI(propertyURI);
            if (ontologyURI != null) {
                Model baseModel = persistenceProvider.getModel(ontologyURI);
                OntModel ontModel = ModelFactory.createOntologyModel(getOntModelSpec(false), baseModel);
                OntProperty ontProperty = ontModel.getOntProperty(propertyURI);
                if (ontProperty.getURI() != null) {
                    PropertyMetaInformation propertyMetaInformation = objectFactory
                            .createPropertyMetaInformation();
                    propertyMetaInformation
                            .setHref(resourceManager.getResourceFullPath(ontProperty.getURI()));
                    propertyMetaInformation.setNamespace(ontProperty.getNameSpace());
                    propertyMetaInformation.setLocalName(ontProperty.getLocalName());
                    propertyMetaInformation.setURI(ontProperty.getURI());
                    /** FIXME: Description has to be administerable **/
                    propertyMetaInformation.setDescription("");
                    return propertyMetaInformation;
                }
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        } finally {

        }
        return null;
    }

    private ResourceMetaInformationType generateResourceMetaInformation(Resource resource) {
        try {
            ObjectFactory objectFactory = new ObjectFactory();
            ResourceMetaInformationType resourceMetaInformation = objectFactory
                    .createResourceMetaInformationType();
            resourceMetaInformation.setLocalName(resource.getLocalName());
            resourceMetaInformation.setHref(null);
            resourceMetaInformation.setNamespace(resource.getNameSpace());
            resourceMetaInformation.setURI(resource.getURI());
            /** FIXME: Description has to be administerable **/
            resourceMetaInformation.setDescription("");
            return resourceMetaInformation;
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return null;
    }

    private IndividualMetaInformation generateIndividualMetaInformation(String individualURI) {

        try {
            ObjectFactory objectFactory = new ObjectFactory();
            String ontologyURI = resourceManager.resolveOntologyURIFromResourceURI(individualURI);
            Model baseModel = persistenceProvider.getModel(ontologyURI);
            OntModel ontModel = ModelFactory.createOntologyModel(getOntModelSpec(false), baseModel);
            Individual individual = ontModel.getIndividual(individualURI);
            if (individual.getURI() != null) {
                IndividualMetaInformation individualMetaInformation = objectFactory
                        .createIndividualMetaInformation();
                individualMetaInformation.setHref(resourceManager.getResourceFullPath(individual.getURI()));
                individualMetaInformation.setNamespace(individual.getNameSpace());
                individualMetaInformation.setLocalName(individual.getLocalName());
                individualMetaInformation.setURI(individual.getURI());
                /** FIXME: Description has to be administerable **/
                individualMetaInformation.setDescription("");
                return individualMetaInformation;
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return null;
    }

    /**
     * FIXME:: Currently, some individuals and some data/object properties are also URIs without any
     * references to classes
     **/
    /** FIXME:: You have to deal with them to!!! **/
    /** FIXME OWLLinkReasoner should be used here too. **/
    private List<ClassConstraint> resolveOntClass(OntClass ontClass) {
        List<ClassConstraint> allConstraints = new Vector<ClassConstraint>();
        ObjectFactory objectFactory = new ObjectFactory();
        List<OntClass> restrictionClasses = ontClass.listEquivalentClasses().toList();
        restrictionClasses.addAll(ontClass.listSuperClasses().toList());
        restrictionClasses.add(ontClass);
        for (OntClass restrictionClass : restrictionClasses) {

            if (restrictionClass.isComplementClass()) {
                ClassConstraint classConstraint = objectFactory.createClassConstraint();
                classConstraint.setType(ConstraintType.COMPLEMENT_OF);
                ComplementClass complementClass = restrictionClass.asComplementClass();
                ExtendedIterator operandsItr = complementClass.listOperands();
                while (operandsItr.hasNext()) {
                    OntClass nextClass = (OntClass) operandsItr.next();
                    try {
                        if (nextClass.getURI() != null) {
                            classConstraint
                                    .getClassConstraintOrClassMetaInformationOrPropertyMetaInformation().add(
                                        generateClassMetaInformation(nextClass.getURI()));
                        } else {
                            List<ClassConstraint> resolvedConstraints = resolveOntClass(nextClass);
                            Iterator resolvedConstraintsItr = resolvedConstraints.iterator();
                            while (resolvedConstraintsItr.hasNext()) {
                                classConstraint
                                        .getClassConstraintOrClassMetaInformationOrPropertyMetaInformation()
                                        .add(resolvedConstraintsItr.next());
                            }
                        }
                    } catch (Exception e) {
                        logger.debug(e.getMessage());
                        logger.debug("error in resolve OntClass {}  proceeding with next input.",
                            restrictionClass.toString());
                    }
                }
                allConstraints.add(classConstraint);
            }
            if (restrictionClass.isEnumeratedClass()) {
                ClassConstraint classConstraint = objectFactory.createClassConstraint();
                classConstraint.setType(ConstraintType.ENUMERATION_OF);
                EnumeratedClass enumeratedClass = restrictionClass.asEnumeratedClass();
                ExtendedIterator operandsItr = enumeratedClass.listOneOf();
                while (operandsItr.hasNext()) {
                    OntResource nextResource = (OntResource) operandsItr.next();
                    try {
                        if (nextResource.getURI() != null) {
                            if (nextResource.isClass()) {
                                classConstraint
                                        .getClassConstraintOrClassMetaInformationOrPropertyMetaInformation()
                                        .add(generateClassMetaInformation(nextResource.getURI()));
                            } else if (nextResource.isIndividual()) {
                                classConstraint
                                        .getClassConstraintOrClassMetaInformationOrPropertyMetaInformation()
                                        .add(generateIndividualMetaInformation(nextResource.getURI()));

                            } else {
                                classConstraint
                                        .getClassConstraintOrClassMetaInformationOrPropertyMetaInformation()
                                        .add(nextResource.getURI());
                            }
                        }
                    } catch (Exception e) {
                        logger.debug(e.getMessage());
                        logger.debug("error in resolve OntClass {}  proceeding with next input.",
                            restrictionClass.toString());
                    }
                }
                allConstraints.add(classConstraint);
            }
            if (restrictionClass.isIntersectionClass()) {
                ClassConstraint classConstraint = objectFactory.createClassConstraint();
                classConstraint.setType(ConstraintType.INTERSECTION_OF);
                IntersectionClass intersectionClass = restrictionClass.asIntersectionClass();
                ExtendedIterator operandsItr = intersectionClass.listOperands();
                while (operandsItr.hasNext()) {
                    OntClass nextClass = (OntClass) operandsItr.next();
                    try {
                        if (nextClass.getURI() != null) {
                            classConstraint
                                    .getClassConstraintOrClassMetaInformationOrPropertyMetaInformation().add(
                                        generateClassMetaInformation(nextClass.getURI()));
                        } else {
                            List<ClassConstraint> resolvedConstraints = resolveOntClass(nextClass);
                            Iterator resolvedConstraintsItr = resolvedConstraints.iterator();
                            while (resolvedConstraintsItr.hasNext()) {
                                classConstraint
                                        .getClassConstraintOrClassMetaInformationOrPropertyMetaInformation()
                                        .add(resolvedConstraintsItr.next());
                            }
                        }
                    } catch (Exception e) {
                        logger.debug(e.getMessage());
                        logger.debug("error in resolve OntClass {} proceeding with next input.",
                            restrictionClass.toString());
                    }
                }
                allConstraints.add(classConstraint);
            }
            if (restrictionClass.isRestriction()) {
                Restriction restriction = restrictionClass.asRestriction();
                try {
                    if (restriction.isAllValuesFromRestriction()) {
                        ClassConstraint classConstraint = objectFactory.createClassConstraint();
                        classConstraint.setType(ConstraintType.ALL_VALUES_FROM);
                        AllValuesFromRestriction allValuesFromRestriction = restriction
                                .asAllValuesFromRestriction();
                        Resource resource = allValuesFromRestriction.getAllValuesFrom();
                        organizePropertyReference(classConstraint, allValuesFromRestriction);

                        if (resource.isURIResource()) {
                            classConstraint
                                    .getClassConstraintOrClassMetaInformationOrPropertyMetaInformation().add(
                                        generateClassMetaInformation(resource.getURI()));
                        } else {
                            // FIXME Resource may not necessarily be a OntClass,
                            // it can be a DataRange either
                            try {
                                List<ClassConstraint> resolvedConstraints = resolveOntClass((OntClass) resource);
                                Iterator resolvedConstraintsItr = resolvedConstraints.iterator();
                                while (resolvedConstraintsItr.hasNext()) {
                                    classConstraint
                                            .getClassConstraintOrClassMetaInformationOrPropertyMetaInformation()
                                            .add(resolvedConstraintsItr.next());
                                }
                            } catch (ClassCastException e) {
                                logger.debug("Can not convert {}  to OntClass", resource.toString());
                            }

                            try {
                                organizeDataRange(resource, classConstraint);
                            } catch (ClassCastException e) {
                                logger.debug("Can not convert {} to DataRange", resource.toString());
                            }

                        }
                        allConstraints.add(classConstraint);
                    }
                    if (restriction.isSomeValuesFromRestriction()) {
                        ClassConstraint classConstraint = objectFactory.createClassConstraint();
                        classConstraint.setType(ConstraintType.SOME_VALUES_FROM);
                        SomeValuesFromRestriction someValuesFromRestriction = restriction
                                .asSomeValuesFromRestriction();
                        Resource resource = someValuesFromRestriction.getSomeValuesFrom();

                        organizePropertyReference(classConstraint, someValuesFromRestriction);
                        if (resource.isURIResource()) {
                            classConstraint
                                    .getClassConstraintOrClassMetaInformationOrPropertyMetaInformation().add(
                                        generateClassMetaInformation(resource.getURI()));
                        } else {
                            try {
                                List<ClassConstraint> resolvedConstraints = resolveOntClass((OntClass) resource);
                                Iterator resolvedConstraintsItr = resolvedConstraints.iterator();
                                while (resolvedConstraintsItr.hasNext()) {
                                    classConstraint
                                            .getClassConstraintOrClassMetaInformationOrPropertyMetaInformation()
                                            .add(resolvedConstraintsItr.next());
                                }
                            } catch (ClassCastException e) {
                                logger.debug("Can not convert {} to OntClass", resource.toString());
                            }
                            try {
                                organizeDataRange(resource, classConstraint);
                            } catch (ClassCastException e) {
                                logger.debug("Can not convert {} to DataRange", resource.toString());
                            }
                        }
                        allConstraints.add(classConstraint);
                    }
                    if (restriction.isHasValueRestriction()) {
                        ClassConstraint classConstraint = objectFactory.createClassConstraint();
                        classConstraint.setType(ConstraintType.HAS_VALUE);
                        HasValueRestriction hasValueRestriction = restriction.asHasValueRestriction();

                        organizePropertyReference(classConstraint, hasValueRestriction);
                        RDFNode node = hasValueRestriction.getHasValue();
                        if (node.isLiteral()) {
                            classConstraint
                                    .getClassConstraintOrClassMetaInformationOrPropertyMetaInformation().add(
                                        node.asLiteral().getValue());

                        } else if (node.isURIResource()) {
                            classConstraint
                                    .getClassConstraintOrClassMetaInformationOrPropertyMetaInformation().add(
                                        generateIndividualMetaInformation(node.asResource().getURI()));
                        }
                        allConstraints.add(classConstraint);
                    }
                    if (restriction.isCardinalityRestriction()) {
                        ClassConstraint classConstraint = objectFactory.createClassConstraint();
                        classConstraint.setType(ConstraintType.CARDINALITY);
                        organizePropertyReference(classConstraint, restriction);
                        classConstraint
                                .getClassConstraintOrClassMetaInformationOrPropertyMetaInformation()
                                .add(
                                    Integer.toString(restriction.asCardinalityRestriction().getCardinality()));
                        allConstraints.add(classConstraint);
                    }
                    if (restriction.isMaxCardinalityRestriction()) {
                        ClassConstraint classConstraint = objectFactory.createClassConstraint();
                        classConstraint.setType(ConstraintType.MAX_CARDINALITY);
                        organizePropertyReference(classConstraint, restriction);
                        classConstraint.getClassConstraintOrClassMetaInformationOrPropertyMetaInformation()
                                .add(
                                    Integer.toString(restriction.asMaxCardinalityRestriction()
                                            .getMaxCardinality()));
                        allConstraints.add(classConstraint);
                    }
                    if (restriction.isMinCardinalityRestriction()) {
                        ClassConstraint classConstraint = objectFactory.createClassConstraint();
                        classConstraint.setType(ConstraintType.MIN_CARDINALITY);
                        organizePropertyReference(classConstraint, restriction);
                        classConstraint.getClassConstraintOrClassMetaInformationOrPropertyMetaInformation()
                                .add(
                                    Integer.toString(restriction.asMinCardinalityRestriction()
                                            .getMinCardinality()));
                        allConstraints.add(classConstraint);
                    }
                } catch (Exception e) {
                    logger.debug(e.getMessage());
                    logger.debug("error in resolve OntClass {}  proceeding with next input.",
                        restrictionClass.toString());
                }
            }
            if (restrictionClass.isUnionClass()) {
                ClassConstraint classConstraint = objectFactory.createClassConstraint();
                classConstraint.setType(ConstraintType.UNION_OF);
                UnionClass unionClass = restrictionClass.asUnionClass();
                ExtendedIterator operandsItr = unionClass.listOperands();
                while (operandsItr.hasNext()) {
                    try {
                        OntClass nextClass = (OntClass) operandsItr.next();
                        if (nextClass.getURI() != null) {
                            ClassMetaInformation classMetaInformation = generateClassMetaInformation(nextClass
                                    .getURI());
                            classConstraint
                                    .getClassConstraintOrClassMetaInformationOrPropertyMetaInformation().add(
                                        classMetaInformation);
                        } else {
                            List<ClassConstraint> resolvedConstraints = resolveOntClass(nextClass);
                            Iterator resolvedConstraintsItr = resolvedConstraints.iterator();
                            while (resolvedConstraintsItr.hasNext()) {
                                classConstraint
                                        .getClassConstraintOrClassMetaInformationOrPropertyMetaInformation()
                                        .add(resolvedConstraintsItr.next());
                            }
                        }
                    } catch (Exception e) {
                        logger.debug(e.getMessage());
                        logger.debug("error in resolve OntClass {} proceeding with next input.",
                            restrictionClass.toString());
                    }
                }
                allConstraints.add(classConstraint);
            }
        }
        return allConstraints;
    }

    private void organizeDataRange(Resource resource, ClassConstraint classConstraint) {
        DataRange dataRange = (DataRange) resource;
        for (RDFNode node : dataRange.getOneOf().asJavaList()) {
            if (node.isURIResource()) {
                IndividualMetaInformation individualMetaInformation = generateIndividualMetaInformation(resource
                        .getURI());
                if (individualMetaInformation != null) {
                    classConstraint.getClassConstraintOrClassMetaInformationOrPropertyMetaInformation().add(
                        individualMetaInformation);
                }
            } else if (node.isLiteral()) {
                classConstraint.getClassConstraintOrClassMetaInformationOrPropertyMetaInformation().add(
                    resource.asLiteral().getValue());
            } else {
                logger.warn("RDFNode {}  is neither Literal nor URI Resource", node.toString());
            }
        }
    }

    private void organizePropertyReference(ClassConstraint classConstraint, Restriction restriction) {
        Resource propertyResource = restriction.getPropertyResourceValue(OWL.onProperty);
        PropertyMetaInformation propertyInf = generatePropertyMetaInformation(propertyResource.getURI());
        if (propertyInf != null) {
            classConstraint.getClassConstraintOrClassMetaInformationOrPropertyMetaInformation().add(
                propertyInf);

        } else {
            classConstraint.getClassConstraintOrClassMetaInformationOrPropertyMetaInformation().add(
                generateResourceMetaInformation(propertyResource));
        }
    }



    private OntModelSpec getOntModelSpecWithoutModelMaker() {
        if (this.useReasoner) {
            return OntModelSpec.OWL_DL_MEM_TRANS_INF;
        } else {
            return OntModelSpec.OWL_DL_MEM;
        }
    }

    // FIXME Jena and OWLApi model should be synchronized to prevent creating a
    // KB for an ontology every time a resource on that ontology requested
    private OWLlinkHTTPXMLReasoner getOWLLinkReasoner(OWLOntology ontology) throws OWLOntologyCreationException {
        OWLlinkHTTPXMLReasonerFactory factory = new OWLlinkHTTPXMLReasonerFactory();
        ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor();
        OWLlinkReasonerConfiguration config = new OWLlinkReasonerConfiguration(progressMonitor, REASONER_URL,
                IndividualNodeSetPolicy.BY_NAME);

        OWLlinkHTTPXMLReasoner reasoner = (OWLlinkHTTPXMLReasoner) factory.createNonBufferingReasoner(
            ontology, config);
        reasoner.flush();
        return reasoner;
    }

    private OntModel addInferencesToModel(OntModel model, String ontologyURI, InferenceScope infScope) {
        OntModel returnModel = model;
        try {
            long start = System.currentTimeMillis();
            OWLOntology original = jenaToOWlApi(model);
            long t1 = System.currentTimeMillis();
            OWLOntologyManager manager = original.getOWLOntologyManager();
            OWLlinkHTTPXMLReasonerFactory factory = new OWLlinkHTTPXMLReasonerFactory();
            ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor();
            OWLlinkReasonerConfiguration config = new OWLlinkReasonerConfiguration(progressMonitor,
                    REASONER_URL, IndividualNodeSetPolicy.BY_NAME);
            OWLlinkHTTPXMLReasoner reasoner = (OWLlinkHTTPXMLReasoner) factory.createNonBufferingReasoner(
                original, config);
            IRI kb = reasoner.getDefaultKB();
            try {
                reasoner.flush();
                long t2 = System.currentTimeMillis();
                OWLOntology ont = manager.createOntology();
                generateInferredAxioms(manager, ont, reasoner, inferredAxiomGenerators);
                manager.addAxioms(original, ont.getAxioms());
                long t3 = System.currentTimeMillis();
                OntModel newModel = owlApiToJena(original);
                long end = System.currentTimeMillis();
                logger.info("Jena -> OWlapi {} ms", (t1 - start));
                logger.info("Inferring Statements {} ms ", (t2 - t1));
                logger.info("Prepare inferred model {} ms ", (t3 - t2));
                logger.info("Owlapi -> Jena {} ms ", (end - t3));
                logger.info("Inferred statements are computed in {} ms ", (end - start));
                returnModel = newModel;
            } finally {
                reasoner.answer(new ReleaseKB(kb));
            }
        } catch (Exception e) {
            logger.warn("Unable to get inference model. Returning original model");
            logger.error("Error ", e);
        }
        return returnModel;
    }

    private OWLOntology jenaToOWlApi(OntModel model) throws OWLOntologyCreationException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        model.write(bos, "RDF/XML", null);
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology owlOntology = manager.loadOntologyFromOntologyDocument(new ByteArrayInputStream(bos
                .toByteArray()));
        return owlOntology;
    }

    private OWLOntology jenaToOWlApi(OntModel model, String ontologyURI) throws OWLOntologyCreationException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        RDFWriter rdfWriter = model.getWriter("RDF/XML");
        rdfWriter.setProperty("xmlbase", ontologyURI);
        rdfWriter.write(model.getBaseModel(), bos, ontologyURI);
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology owlOntology = manager.loadOntologyFromOntologyDocument(new ByteArrayInputStream(bos
                .toByteArray()));
        return owlOntology;
    }

    private OntModel owlApiToJena(OWLOntology owlOntology) throws OWLOntologyStorageException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        OWLOntologyManager owlmanager = owlOntology.getOWLOntologyManager();
        owlmanager.saveOntology(owlOntology, new RDFXMLOntologyFormat(), bos);
        OntModel ontModel = ModelFactory.createOntologyModel(getOntModelSpecWithoutModelMaker());
        OWLOntologyID id = owlOntology.getOntologyID();
        String ontologyURI;
        if (id.isAnonymous()) {
            ontologyURI = null;
        } else {
            ontologyURI = id.toString().replace("<", "").replace(">", "");
        }
        ontModel.read(new ByteArrayInputStream(bos.toByteArray()), ontologyURI);
        return ontModel;
    }

    private void generateInferredAxioms(OWLOntologyManager manager,
                                        OWLOntology ontology,
                                        OWLReasoner reasoner,
                                        List<InferredAxiomGenerator> axiomGenerators) {
        List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
        for (InferredAxiomGenerator<? extends OWLAxiom> axiomGenerator : axiomGenerators) {
            try {
                for (OWLAxiom ax : axiomGenerator.createAxioms(manager, reasoner)) {
                    changes.add(new AddAxiom(ontology, ax));
                }
            } catch (OWLlinkReasonerRuntimeException e) {
                logger.warn("Can not compute inferred statements {} ", e.getMessage());
            }
        }
        manager.applyChanges(changes);
    }

    public JenaPersistenceProvider getPersistenceProvider() {
        return persistenceProvider;
    }

    public void bindResourceManager(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    public void unbindResourceManager(ResourceManager resourceManager) {
        this.synchronizerThread.done();
        this.resourceManager = null;
    }

    @Override
    public ImportsForOntology retrieveOntologyImports(String ontologyURI) throws Exception {
        ObjectFactory of = new ObjectFactory();
        Model model = persistenceProvider.getModel(ontologyURI);
        OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, model);
        ImportsForOntology imports = of.createImportsForOntology();
        imports.setOntologyMetaInformation(retrieveOntologyMetaInformation(ontologyURI));
        for (String importedOntologyURI : ontModel.listImportedOntologyURIs()) {
            OntologyImport ontImport = new OntologyImport();
            ontImport.setURI(importedOntologyURI);
            ontImport.setHref(resourceManager.getOntologyFullPath(importedOntologyURI));
            imports.getOntologyImport().add(ontImport);
        }
        return imports;
    }

    @Override
    public void addOntologyImport(String ontologyURI, String importURI) throws Exception {
        Model model = persistenceProvider.getModel(ontologyURI);
        OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, model);
        ontModel.add(ResourceFactory.createResource(ontologyURI), OWL.imports,
            ResourceFactory.createResource(importURI));
        if (!persistenceProvider.hasModel(importURI)) {
            saveOntology(new URL(importURI), importURI, "UTF-8");
        }

    }

    @Override
    public void removeOntologyImport(String ontologyURI, String importURI) throws Exception {
        Model model = persistenceProvider.getModel(ontologyURI);
        OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, model);
        ontModel.remove(ResourceFactory.createResource(ontologyURI), OWL.imports,
            ResourceFactory.createResource(importURI));
        List<Statement> toDelete = ontModel.listStatements(null, OWL.imports,
            ResourceFactory.createResource(importURI)).toList();
        ontModel.remove(toDelete);

    }
}

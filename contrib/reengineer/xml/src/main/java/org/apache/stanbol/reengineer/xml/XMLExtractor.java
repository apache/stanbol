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
package org.apache.stanbol.reengineer.xml;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.owl.OWLOntologyManagerFactory;
import org.apache.stanbol.ontologymanager.servicesapi.collector.DuplicateIDException;
import org.apache.stanbol.ontologymanager.servicesapi.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.servicesapi.scope.Scope;
import org.apache.stanbol.ontologymanager.servicesapi.scope.ScopeManager;
import org.apache.stanbol.ontologymanager.sources.owlapi.RootOntologySource;
import org.apache.stanbol.reengineer.base.api.DataSource;
import org.apache.stanbol.reengineer.base.api.Reengineer;
import org.apache.stanbol.reengineer.base.api.ReengineerManager;
import org.apache.stanbol.reengineer.base.api.Reengineer_OWL;
import org.apache.stanbol.reengineer.base.api.ReengineeringException;
import org.apache.stanbol.reengineer.base.api.util.ReengineerType;
import org.apache.stanbol.reengineer.base.api.util.ReengineerUriRefGenerator;
import org.apache.stanbol.reengineer.base.api.util.UnsupportedReengineerException;
import org.apache.stanbol.reengineer.xml.vocab.XML_OWL;
import org.apache.stanbol.reengineer.xml.vocab.XSD_OWL;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologySetProvider;
import org.semanticweb.owlapi.util.OWLOntologyMerger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * The {@code XMLExtractor} extends of the {@link XSDExtractor} that implements the {@link Reengineer} for XML
 * data sources.
 * 
 * @author andrea.nuzzolese
 * 
 */

@Component(immediate = true, metatype = true)
@Service(Reengineer.class)
public class XMLExtractor extends ReengineerUriRefGenerator implements Reengineer {

    public static final String _HOST_NAME_AND_PORT_DEFAULT = "localhost:8080";
    public static final String _REENGINEERING_SCOPE_DEFAULT = "xml_reengineering";
    // public static final String _XML_REENGINEERING_SESSION_SPACE_DEFAULT =
    // "/xml-reengineering-session-space";

    @Property(value = _HOST_NAME_AND_PORT_DEFAULT)
    public static final String HOST_NAME_AND_PORT = "host.name.port";

    @Property(value = _REENGINEERING_SCOPE_DEFAULT)
    public static final String REENGINEERING_SCOPE = "xml.reengineering.scope";

    // @Property(value = _XML_REENGINEERING_SESSION_SPACE_DEFAULT)
    // public static final String XML_REENGINEERING_SESSION_SPACE =
    // "http://kres.iks-project.eu/space/reengineering/db";

    public final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    ScopeManager onManager;

    @Reference
    ReengineerManager reengineeringManager;

    private Scope scope;
    private String scopeID;

    /**
     * This default constructor is <b>only</b> intended to be used by the OSGI environment with Service
     * Component Runtime support.
     * <p>
     * DO NOT USE to manually create instances - the XMLExtractor instances do need to be configured! YOU NEED
     * TO USE {@link #XMLExtractor(ScopeManager)} or its overloads, to parse the configuration and then
     * initialise the rule store if running outside a OSGI environment.
     */
    public XMLExtractor() {}

    public XMLExtractor(ReengineerManager reengineeringManager,
                        ScopeManager onManager,
                        Dictionary<String,Object> configuration) {
        this();
        this.reengineeringManager = reengineeringManager;
        this.onManager = onManager;
        activate(configuration);
    }

    /**
     * Used to configure an instance within an OSGi container.
     * 
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) throws IOException {
        log.info("in " + XMLExtractor.class + " activate with context " + context);
        if (context == null) {
            throw new IllegalStateException("No valid" + ComponentContext.class + " parsed in activate!");
        }
        activate((Dictionary<String,Object>) context.getProperties());
    }

    protected void activate(Dictionary<String,Object> configuration) {
        /* String */
        scopeID = (String) configuration.get(REENGINEERING_SCOPE);
        if (scopeID == null) scopeID = _REENGINEERING_SCOPE_DEFAULT;
        String hostPort = (String) configuration.get(HOST_NAME_AND_PORT);
        if (hostPort == null) hostPort = _HOST_NAME_AND_PORT_DEFAULT;
        // TODO: Manage the other properties

        reengineeringManager.bindReengineer(this);

        scope = null;
        try {
            // // A che cacchio serviva 'sta robba?
            // log.info("Semion XMLEtractor : created scope with IRI " + REENGINEERING_SCOPE);
            // IRI iri = IRI.create(XML_OWL.URI);
            // OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
            // OWLOntology owlOntology = ontologyManager.createOntology(iri);
            // log.info("Ontology {} created.", iri);

            IRI[] locations = onManager.getOfflineConfiguration().getOntologySourceLocations()
                    .toArray(new IRI[0]);
            OntologyInputSource xmlowlSrc = new RootOntologySource(IRI.create(XML_OWL.URI),
                    OWLOntologyManagerFactory.createOWLOntologyManager(locations));

            scope = onManager.createOntologyScope(scopeID, xmlowlSrc
            /* new OntologyInputSourceOXML() */);
            // scope.setUp();

            onManager.registerScope(scope);
        } catch (DuplicateIDException e) {
            log.info("Will perform XML reengineering in already existing scope {}", scopeID);
            scope = onManager.getScope(scopeID);
        } catch (OWLOntologyCreationException e) {
            throw new IllegalStateException("No valid schema was found in ontology " + XML_OWL.URI
                                            + "for reengineer" + XMLExtractor.class, e);
        }

        if (scope != null) onManager.setScopeActive(scopeID, true);

        log.info("Stanbol XML Reengineer active.");
    }

    @Override
    public boolean canPerformReengineering(DataSource dataSource) {
        if (dataSource.getDataSourceType() == ReengineerType.XML) return true;
        else return false;
    }

    @Override
    public boolean canPerformReengineering(int dataSourceType) {
        if (dataSourceType == getReengineerType()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean canPerformReengineering(OWLOntology schemaOntology) {

        OWLDataFactory factory = OWLManager.getOWLDataFactory();

        OWLClass dataSourceClass = factory.getOWLClass(Reengineer_OWL.DataSource);
        Set<OWLIndividual> individuals = dataSourceClass.getIndividuals(schemaOntology);

        int hasDataSourceType = -1;

        if (individuals != null && individuals.size() == 1) {
            for (OWLIndividual individual : individuals) {
                OWLDataProperty hasDataSourceTypeProperty = factory
                        .getOWLDataProperty(Reengineer_OWL.hasDataSourceType);
                Set<OWLLiteral> values = individual.getDataPropertyValues(hasDataSourceTypeProperty,
                    schemaOntology);
                if (values != null && values.size() == 1) {
                    for (OWLLiteral value : values) {
                        try {
                            Integer valueInteger = Integer.valueOf(value.getLiteral());
                            hasDataSourceType = valueInteger.intValue();
                        } catch (NumberFormatException e) {

                        }
                    }
                }
            }
        }

        if (hasDataSourceType == getReengineerType()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean canPerformReengineering(String dataSourceType) throws UnsupportedReengineerException {
        return canPerformReengineering(ReengineerType.getType(dataSourceType));
    }

    private IRI createElementResource(String ns,
                                      String schemaNS,
                                      Element element,
                                      String parentName,
                                      Integer id,
                                      OWLOntologyManager manager,
                                      OWLDataFactory factory,
                                      OWLOntology dataOntology) {

        IRI elementResourceIRI;
        OWLClassAssertionAxiom elementResource;
        if (id == null) {
            elementResourceIRI = IRI.create(ns + "root");
            elementResource = createOWLClassAssertionAxiom(factory, XML_OWL.XMLElement, elementResourceIRI);
        } else {
            elementResourceIRI = IRI.create(ns + parentName + "_" + element.getLocalName() + "_"
                                            + id.toString());
            elementResource = createOWLClassAssertionAxiom(factory, XML_OWL.XMLElement, elementResourceIRI);
        }
        manager.applyChange(new AddAxiom(dataOntology, elementResource));

        String schemaElementName = element.getLocalName();

        IRI elementDeclarationIRI = IRI.create(schemaNS + schemaElementName);

        manager.applyChange(new AddAxiom(dataOntology, createOWLObjectPropertyAssertionAxiom(factory,
            XML_OWL.hasElementDeclaration, elementResourceIRI, elementDeclarationIRI)));

        NamedNodeMap namedNodeMap = element.getAttributes();
        if (namedNodeMap != null) {
            for (int i = 0, j = namedNodeMap.getLength(); i < j; i++) {
                Node node = namedNodeMap.item(i);

                String attributeName = node.getNodeName();
                String attributeValue = node.getTextContent();

                String[] elementNames = elementResourceIRI.toString().split("#");
                String elementLocalName;
                if (elementNames.length == 2) {
                    elementLocalName = elementNames[1];
                } else {
                    elementLocalName = elementNames[0];
                }

                IRI xmlAttributeIRI = IRI.create(ns + elementLocalName + attributeName);
                log.debug("Attribute: " + ns + elementLocalName + attributeName);
                OWLClassAssertionAxiom xmlAttribute = createOWLClassAssertionAxiom(factory,
                    XML_OWL.XMLAttribute, xmlAttributeIRI);
                manager.addAxiom(dataOntology, xmlAttribute);

                manager.addAxiom(
                    dataOntology,
                    createOWLDataPropertyAssertionAxiom(factory, XML_OWL.nodeName, xmlAttributeIRI,
                        attributeName));
                manager.addAxiom(
                    dataOntology,
                    createOWLDataPropertyAssertionAxiom(factory, XML_OWL.nodeValue, xmlAttributeIRI,
                        attributeValue));

                IRI attributeDeclarationIRI = IRI.create(schemaNS + schemaElementName + "_" + attributeName);

                manager.addAxiom(
                    dataOntology,
                    createOWLObjectPropertyAssertionAxiom(factory, XML_OWL.hasAttributeDeclaration,
                        xmlAttributeIRI, attributeDeclarationIRI));
                manager.addAxiom(
                    dataOntology,
                    createOWLObjectPropertyAssertionAxiom(factory, XML_OWL.hasXMLAttribute,
                        elementResourceIRI, xmlAttributeIRI));

            }
        }

        return elementResourceIRI;
    }

    @Override
    public OWLOntology dataReengineering(String graphNS,
                                         IRI outputIRI,
                                         DataSource dataSource,
                                         final OWLOntology schemaOntology) throws ReengineeringException {

        if (schemaOntology == null) throw new IllegalArgumentException(
                "Cannot reengineer data with a null schema ontology.");

        OWLOntology ontology = null;

        log.info("Starting XML Reengineering");
        OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
        OWLDataFactory factory = OWLManager.getOWLDataFactory();

        OWLOntology localDataOntology = null;

        log.debug("XML output IRI: {}", outputIRI);

        if (outputIRI != null) {
            try {
                localDataOntology = ontologyManager.createOntology(outputIRI);
            } catch (OWLOntologyCreationException e) {
                throw new ReengineeringException("Failed to create local data ontology " + outputIRI);
            }
        } else {
            try {
                localDataOntology = ontologyManager.createOntology();
            } catch (OWLOntologyCreationException e) {
                throw new ReengineeringException("Failed to create anonymous local data ontology.");
            }
        }

        final OWLOntology dataOntology = localDataOntology;

        OWLImportsDeclaration importsDeclaration = factory.getOWLImportsDeclaration(IRI.create(XML_OWL.URI));

        ontologyManager.applyChange(new AddImport(dataOntology, importsDeclaration));

        graphNS = graphNS.replace("#", "");
        String schemaNS = graphNS + "/schema#";
        String dataNS = graphNS + "#";

        OWLClass dataSourceOwlClass = factory.getOWLClass(Reengineer_OWL.DataSource);

        Set<OWLIndividual> individuals = dataSourceOwlClass.getIndividuals(schemaOntology);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db;
        try {
            db = dbf.newDocumentBuilder();

            InputStream xmlStream = (InputStream) dataSource.getDataSource();

            Document dom = db.parse(xmlStream);

            Element documentElement = dom.getDocumentElement();

            String nodeName = documentElement.getNodeName();

            IRI rootElementIRI = createElementResource(dataNS, schemaNS, documentElement, null, null,
                ontologyManager, factory, dataOntology);

            iterateChildren(dataNS, schemaNS, rootElementIRI, documentElement, ontologyManager, factory,
                dataOntology);

        } catch (ParserConfigurationException e) {
            throw new ReengineeringException(e);
        } catch (SAXException e) {
            throw new ReengineeringException(e);
        } catch (IOException e) {
            throw new ReengineeringException(e);
        }

        OWLOntologyManager man = OWLManager.createOWLOntologyManager();

        OWLOntologySetProvider provider = new OWLOntologySetProvider() {

            @Override
            public Set<OWLOntology> getOntologies() {
                Set<OWLOntology> ontologies = new HashSet<OWLOntology>();
                ontologies.add(schemaOntology);
                ontologies.add(dataOntology);
                return ontologies;
            }
        };
        OWLOntologyMerger merger = new OWLOntologyMerger(provider);

        try {
            ontology = merger.createMergedOntology(man, outputIRI);
        } catch (OWLOntologyCreationException e) {
            throw new ReengineeringException(e);
        }

        return ontology;
    }

    private OWLOntology dataReengineering(String graphNS,
                                          IRI outputIRI,
                                          Document dom,
                                          OWLOntology schemaOntology) throws ReengineeringException {

        OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
        OWLDataFactory factory = OWLManager.getOWLDataFactory();

        IRI schemaOntologyIRI = schemaOntology.getOntologyID().getOntologyIRI();

        OWLOntology dataOntology = null;

        if (schemaOntology != null) {
            if (outputIRI != null) {
                try {
                    dataOntology = ontologyManager.createOntology(outputIRI);
                } catch (OWLOntologyCreationException e) {
                    throw new ReengineeringException();
                }
            } else {
                try {
                    dataOntology = ontologyManager.createOntology();
                } catch (OWLOntologyCreationException e) {
                    throw new ReengineeringException();
                }
            }

            OWLImportsDeclaration importsDeclaration = factory.getOWLImportsDeclaration(schemaOntologyIRI);

            ontologyManager.applyChange(new AddImport(dataOntology, importsDeclaration));

            String schemaNS = graphNS + "/schema#";
            String dataNS = graphNS + "#";

            OWLClass dataSourceOwlClass = factory.getOWLClass(Reengineer_OWL.DataSource);

            Set<OWLIndividual> individuals = dataSourceOwlClass.getIndividuals(schemaOntology);

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db;
            try {
                db = dbf.newDocumentBuilder();

                Element documentElement = dom.getDocumentElement();

                String nodeName = documentElement.getNodeName();

                IRI rootElementIRI = createElementResource(dataNS, schemaNS, documentElement, null, null,
                    ontologyManager, factory, dataOntology);

                iterateChildren(dataNS, schemaNS, rootElementIRI, documentElement, ontologyManager, factory,
                    dataOntology);

            } catch (ParserConfigurationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return dataOntology;
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("in " + XMLExtractor.class + " deactivate with context " + context);
        reengineeringManager.unbindReengineer(this);
    }

    @Override
    public int getReengineerType() {
        return ReengineerType.XML;
    }

    private void iterateChildren(String dataNS,
                                 String schemaNS,
                                 IRI parentResource,
                                 Node parentElement,
                                 OWLOntologyManager manager,
                                 OWLDataFactory factory,
                                 OWLOntology dataOntology) {

        NodeList children = parentElement.getChildNodes();
        if (children != null) {
            for (int i = 0, j = children.getLength(); i < j; i++) {
                Node child = children.item(i);
                if (child instanceof Element) {

                    String[] parentNames = parentResource.toString().split("#");
                    String parentLocalName;
                    if (parentNames.length == 2) {
                        parentLocalName = parentNames[1];
                    } else {
                        parentLocalName = parentNames[0];
                    }

                    IRI childResource = createElementResource(dataNS, schemaNS, (Element) child,
                        parentLocalName, Integer.valueOf(i), manager, factory, dataOntology);

                    manager.applyChange(new AddAxiom(dataOntology, createOWLObjectPropertyAssertionAxiom(
                        factory, XSD_OWL.child, parentResource, childResource)));
                    manager.applyChange(new AddAxiom(dataOntology, createOWLObjectPropertyAssertionAxiom(
                        factory, XSD_OWL.parent, childResource, parentResource)));

                    iterateChildren(dataNS, schemaNS, childResource, child, manager, factory, dataOntology);
                } else {
                    String textContent = child.getNodeValue();
                    if (textContent != null) {
                        textContent = textContent.trim();

                        if (!textContent.equals("")) {
                            log.debug("VALUE : " + textContent);
                            manager.applyChange(new AddAxiom(dataOntology,
                                    createOWLDataPropertyAssertionAxiom(factory, XML_OWL.nodeValue,
                                        parentResource, textContent)));
                        }
                    }
                }
            }
        }
    }

    @Override
    public OWLOntology reengineering(String graphNS, IRI outputIRI, DataSource dataSource) throws ReengineeringException {

        InputStream dataSourceAsStream = (InputStream) dataSource.getDataSource();

        InputStreamReader isr = new InputStreamReader(dataSourceAsStream);
        BufferedReader reader = new BufferedReader(isr);
        final StringBuilder stringBuilder1 = new StringBuilder();
        final StringBuilder stringBuilder2 = new StringBuilder();

        OutputStream out = new OutputStream() {

            @Override
            public void write(byte[] bytes) throws IOException {
                for (byte b : bytes) {
                    stringBuilder1.append((char) b);
                    stringBuilder2.append((char) b);
                }
            }

            @Override
            public void write(int arg0) throws IOException {
                stringBuilder1.append((char) arg0);
                stringBuilder2.append((char) arg0);

            }

        };

        String line = "";
        try {
            while ((line = reader.readLine()) != null) {
                out.write(line.getBytes());
            }
            out.flush();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        final ByteArrayOutputStream buff1 = new ByteArrayOutputStream();
        try {
            buff1.write(stringBuilder1.toString().getBytes());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        final ByteArrayOutputStream buff2 = new ByteArrayOutputStream();
        try {
            buff2.write(stringBuilder2.toString().getBytes());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        DataSource ds1 = new DataSource() {

            @Override
            public Object getDataSource() {
                ByteArrayInputStream byteArr = new ByteArrayInputStream(buff1.toByteArray());
                return byteArr;

            }

            @Override
            public int getDataSourceType() {
                // TODO Auto-generated method stub
                return ReengineerType.XML;
            }

            @Override
            public String getID() {
                // TODO Auto-generated method stub
                return null;
            }
        };

        DataSource ds2 = new DataSource() {

            @Override
            public Object getDataSource() {
                ByteArrayInputStream byteArr = new ByteArrayInputStream(buff2.toByteArray());
                return byteArr;

            }

            @Override
            public int getDataSourceType() {
                // TODO Auto-generated method stub
                return ReengineerType.XML;
            }

            @Override
            public String getID() {
                // TODO Auto-generated method stub
                return null;
            }
        };

        OWLOntology schemaOntology;

        log.debug("XML outputIRI : " + outputIRI);
        if (outputIRI != null && !outputIRI.equals("")) {
            IRI schemaIRI = IRI.create(outputIRI.toString() + "/schema");
            schemaOntology = schemaReengineering(graphNS + "/schema", schemaIRI, ds1);
        } else {
            schemaOntology = schemaReengineering(graphNS + "/schema", null, ds1);
        }
        OWLOntology ontology = dataReengineering(graphNS, outputIRI, ds2, schemaOntology);
        // // NO WAY!
        // try {
        // onManager.getOwlCacheManager().saveOntology(ontology, System.out);
        // } catch (OWLOntologyStorageException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        return ontology;
    }

    @Override
    public OWLOntology schemaReengineering(String graphNS, IRI outputIRI, DataSource dataSource) throws ReengineeringException {
        XSDExtractor xsdExtractor = new XSDExtractor();
        return xsdExtractor.getOntologySchema(graphNS, outputIRI, dataSource);
    }

}

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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.stanbol.commons.owl.transformation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.util.Set;

import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDatatypeDefinitionAxiom;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLIndividualAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.OWLOntologyMerger;

import com.hp.hpl.jena.ontology.AnnotationProperty;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

/**
 * 
 * @author elvio
 * @author andrea.nuzzolese
 */
public class JenaToOwlConvert {

    private boolean available = true;
    private boolean availablemain = true;

    public JenaToOwlConvert() {}

    // //////////////////////////////////////////////////////////////////////////////
    // ////////////////////////////FUNCTIONS/////////////////////////////////////////
    // //////////////////////////////////////////////////////////////////////////////

    /**
     * This function converts an ontology object from Jena to OWLapi
     * 
     * @param jenamodel
     *            {An OntModel object}
     * @param format
     *            {only in "RDF/XML"}
     * @return {An OWLOntology that is an owl object}
     */
    public synchronized OWLOntology ModelJenaToOwlConvert(Model jenamodel, String format) {

        while (availablemain == false) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println("ModelJenaToOwlConvert::: " + e);
            }
        }

        availablemain = false;

        try {

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            if (!format.equals("RDF/XML")) {
                System.err.println("The only format supported is RDF/XML. Please check the format!");

                availablemain = true;
                notifyAll();
                return null;
            } else {

                jenamodel.write(out, format);

                OWLOntologyManager owlmanager = OWLManager.createOWLOntologyManager();

                OWLOntology owlmodel = owlmanager.loadOntologyFromOntologyDocument(new ByteArrayInputStream(
                        out.toByteArray()));

                availablemain = true;
                notifyAll();
                return owlmodel;
            }
        } catch (OWLOntologyCreationException eoc) {
            System.err.print("ModelJenaToOwlConvert::: ");
            eoc.printStackTrace();
            return null;
        }
    }

    /**
     * This function converts an ontology object from Jena to OWLapi
     * 
     * @param jenamodel
     *            {An OntModel object}
     * @param format
     *            {only in "RDF/XML"}
     * @return {An OWLOntology that is an owl object}
     */
    public synchronized OWLOntology ModelJenaToOwlConvert(IRI ontologyIRI, Model jenamodel, String format) {

        while (availablemain == false) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println("ModelJenaToOwlConvert::: " + e);
            }
        }

        availablemain = false;

        try {

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            if (!format.equals("RDF/XML")) {
                System.err.println("The only format supported is RDF/XML. Please check the format!");

                availablemain = true;
                notifyAll();
                return null;
            } else {

                jenamodel.write(out, format);

                OWLOntologyManager owlmanager = OWLManager.createOWLOntologyManager();

                owlmanager.loadOntologyFromOntologyDocument(new ByteArrayInputStream(out.toByteArray()));

                OWLOntologyMerger merger = new OWLOntologyMerger(owlmanager);

                OWLOntology ontology = merger.createMergedOntology(owlmanager, ontologyIRI);

                availablemain = true;
                notifyAll();
                return ontology;
            }
        } catch (OWLOntologyCreationException eoc) {
            System.err.print("ModelJenaToOwlConvert::: ");
            eoc.printStackTrace();
            return null;
        }
    }

    // //////////////////////////////////////////////////////////////////////////////

    /**
     * This function converts an ontology object from OWLapi to Jena
     * 
     * @param owlmodel
     *            {An OWLOntology object}
     * @param format
     *            {RDF/XML or TURTLE}
     * @return {An OntModel that is a Jena object}
     */
    public synchronized OntModel ModelOwlToJenaConvert(OWLOntology owlmodel, String format) {

        while (availablemain == false) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println("ModelOwlToJenaConvert::: " + e);
            }
        }

        availablemain = false;
        OWLOntologyID id;

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            OWLOntologyManager owlmanager = owlmodel.getOWLOntologyManager();

            format = format.trim();

            if (format.equals("TURTLE") || format.equals("RDF/XML")) {

                if (format.equals("TURTLE")) owlmanager.setOntologyFormat(owlmodel,
                    new TurtleOntologyFormat());
                if (format.equals("RDF/XML")) owlmanager.setOntologyFormat(owlmodel,
                    new RDFXMLOntologyFormat());

                OWLOntologyFormat owlformat = owlmanager.getOntologyFormat(owlmodel);

                owlmanager.saveOntology(owlmodel, owlformat, out);

                OntModel jenamodel = ModelFactory.createOntologyModel();
                id = owlmodel.getOntologyID();
                jenamodel.read(new ByteArrayInputStream(out.toByteArray()), id.toString().replace("<", "")
                        .replace(">", ""), format);

                availablemain = true;
                notifyAll();
                return jenamodel;
            } else {
                System.err
                        .println("The only format supported is RDF/XML or TURTLE. Please check the format!");

                availablemain = true;
                notifyAll();
                return null;
            }
        } catch (OWLOntologyStorageException eos) {
            System.err.print("ModelOwlToJenaConvert::: ");
            eos.printStackTrace();
            return null;
        }
    }

    // //////////////////////////////////////////////////////////////////////////////

    /**
     * This function converts every statments relative to a resource in an a set of OWLAxiom objects
     * 
     * @param jenadata
     *            {A resource in the form (S,P,O), it could be any kind of resource (a class, a data property,
     *            an object property and an instance) except a litteral}
     * @param format
     *            {The format of the ontology, i.e. "RDF/XML"}
     * @return {A set of axiom in the form of Set<OWLAxiom>}
     */
    public synchronized Set<OWLAxiom> ResourceJenaToOwlAxiom(Resource jenadata, String format) {

        while (available == false) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println("ResourceJenaToOwlAxiom::: " + e);
            }
        }

        available = false;

        try {
            OntModel model = ModelFactory.createOntologyModel();

            StmtIterator prop = jenadata.listProperties();

            while (prop.hasNext()) {
                Statement stat = prop.nextStatement();
                model.add(stat);
                RDFNode obj = stat.getObject();
                if (obj.isResource()) {
                    if (!obj.isURIResource()) {
                        StmtIterator aux = ((Resource) obj).listProperties();
                        while (aux.hasNext()) {
                            Statement stataux = aux.nextStatement();
                            model.add(stataux);
                        }
                    }
                }
            }

            OWLOntology owlmodel = ModelJenaToOwlConvert(model, format);

            available = true;
            notifyAll();
            return owlmodel.getAxioms();
        } catch (Exception e) {
            System.err.print("ResourceJenaToOwlAxiom::: ");
            e.printStackTrace();
            return null;
        }
    }

    // //////////////////////////////////////////////////////////////////////////////

    /**
     * This function converts a set of OWLAxiom in an iterator over jena statements
     * 
     * @param axioms
     *            {A set of aximos}
     * @param format
     *            {RDF/XML or TURTLE}
     * @return {An iterator over statments}
     */
    public synchronized StmtIterator AxiomOwlToJenaResource(Set<OWLAxiom> axioms, String format) {

        while (available == false) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println("AxiomOwlToJenaResource::: " + e);
            }
        }

        available = false;

        try {
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.createOntology(IRI
                    .create("http://www.semanticweb.org/owlapi/ontologies/ontology"));

            Iterator<OWLAxiom> axiom = axioms.iterator();

            while (axiom.hasNext())
                manager.addAxiom(ontology, axiom.next());

            OntModel jenamodel = ModelOwlToJenaConvert(ontology, format);

            available = true;
            notifyAll();
            return jenamodel.listStatements();
        } catch (OWLOntologyCreationException eoc) {
            System.err.print("AxiomOwlToJenaResource::: ");
            eoc.printStackTrace();
            return null;
        }

    }

    // //////////////////////////////////////////////////////////////////////////////

    /**
     * This function converts any thingths relatives to an OWL entity in an iterator over Jena statement
     * 
     * @param entity
     *            {It could be a class, an object property or a data property}
     * @param owlmodel
     *            {OWLOntology model where to retrieve information about the entity}
     * @param format
     *            {RDF/XML or TURTLE}
     * @return {An iterator over jena statement}
     */
    public synchronized StmtIterator EntityOwlToJenaResource(OWLEntity entity,
                                                             OWLOntology owlmodel,
                                                             String format) {

        while (available == false) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println("EntityOwlToJenaResource::: " + e);
            }
        }

        available = false;

        try {
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.createOntology(IRI
                    .create("http://www.semanticweb.org/owlapi/ontologies/ontology"));

            // If the entity is a class
            if (entity.isOWLClass()) {
                OWLClass owldata = entity.asOWLClass();

                Iterator<OWLClassAxiom> entityaxiom = owlmodel.getAxioms(owldata).iterator();

                while (entityaxiom.hasNext())
                    manager.addAxiom(ontology, entityaxiom.next());

                Iterator<OWLAnnotationAssertionAxiom> annotations = entity.getAnnotationAssertionAxioms(
                    owlmodel).iterator();

                while (annotations.hasNext())
                    manager.addAxiom(ontology, annotations.next());
            }

            // If the entity is a data property
            if (entity.isOWLDataProperty()) {
                OWLDataProperty owldata = entity.asOWLDataProperty();

                Iterator<OWLDataPropertyAxiom> entityaxiom = owlmodel.getAxioms(owldata).iterator();

                while (entityaxiom.hasNext())
                    manager.addAxiom(ontology, entityaxiom.next());

                Iterator<OWLAnnotationAssertionAxiom> annotations = entity.getAnnotationAssertionAxioms(
                    owlmodel).iterator();

                while (annotations.hasNext())
                    manager.addAxiom(ontology, annotations.next());
            }

            // If the entity is an object property
            if (entity.isOWLObjectProperty()) {
                OWLObjectProperty owldata = entity.asOWLObjectProperty();

                Iterator<OWLObjectPropertyAxiom> entityaxiom = owlmodel.getAxioms(owldata).iterator();

                while (entityaxiom.hasNext())
                    manager.addAxiom(ontology, entityaxiom.next());

                Iterator<OWLAnnotationAssertionAxiom> annotations = entity.getAnnotationAssertionAxioms(
                    owlmodel).iterator();

                while (annotations.hasNext())
                    manager.addAxiom(ontology, annotations.next());
            }

            // If the entity is a data type
            if (entity.isOWLDatatype()) {
                OWLDatatype owldata = entity.asOWLDatatype();

                Iterator<OWLDatatypeDefinitionAxiom> entityaxiom = owlmodel.getAxioms(owldata).iterator();

                while (entityaxiom.hasNext())
                    manager.addAxiom(ontology, entityaxiom.next());

                Iterator<OWLAnnotationAssertionAxiom> annotations = entity.getAnnotationAssertionAxioms(
                    owlmodel).iterator();

                while (annotations.hasNext())
                    manager.addAxiom(ontology, annotations.next());
            }

            // If the entity is an individual
            if (entity.isOWLNamedIndividual()) {
                OWLNamedIndividual owldata = entity.asOWLNamedIndividual();

                Iterator<OWLIndividualAxiom> entityaxiom = owlmodel.getAxioms(owldata).iterator();

                while (entityaxiom.hasNext())
                    manager.addAxiom(ontology, entityaxiom.next());

                Iterator<OWLAnnotationAssertionAxiom> annotations = entity.getAnnotationAssertionAxioms(
                    owlmodel).iterator();

                while (annotations.hasNext())
                    manager.addAxiom(ontology, annotations.next());
            }

            // If the entity is an annotations property
            if (entity.isOWLAnnotationProperty()) {
                OWLAnnotationProperty owldata = entity.asOWLAnnotationProperty();

                Iterator<OWLAnnotationAxiom> entityaxiom = owlmodel.getAxioms(owldata).iterator();

                while (entityaxiom.hasNext())
                    manager.addAxiom(ontology, entityaxiom.next());

                Iterator<OWLAnnotationAssertionAxiom> annotations = entity.getAnnotationAssertionAxioms(
                    owlmodel).iterator();

                while (annotations.hasNext())
                    manager.addAxiom(ontology, annotations.next());
            }

            OntModel ontmodel = ModelOwlToJenaConvert(ontology, format);
            StmtIterator statement = ontmodel.listStatements();

            available = true;
            notifyAll();
            return statement;
        } catch (OWLOntologyCreationException eoc) {
            System.err.print("EntityOwlToJenaResource::: ");
            eoc.printStackTrace();
            return null;
        }
    }

    // //////////////////////////////////////////////////////////////////////////////

    /**
     * This function converts a single OntClass of Jena to an OWLClass of OWLAPI
     * 
     * @param jenadata
     *            {Jena class object}
     * @param format
     *            {RDF/XML}
     * @return {An OWLclass}
     */
    public synchronized OWLClass ClassJenaToOwl(OntClass jenadata, String format) {

        while (available == false) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println("ClassJenaToOwl::: " + e);
            }
        }

        available = false;

        try {
            OntModel model = ModelFactory.createOntologyModel();

            model.createClass(jenadata.getURI());

            OWLOntology owlmodel = ModelJenaToOwlConvert(model, format);

            available = true;
            notifyAll();
            return owlmodel.getClassesInSignature().iterator().next();
        } catch (Exception e) {
            System.err.print("ClassJenaToOwl::: ");
            e.printStackTrace();
            return null;
        }
    }

    // //////////////////////////////////////////////////////////////////////////////

    /**
     * This function converts a single OWLClass of OWLAPI to an OntClass of Jena
     * 
     * @param data
     *            {An OWLClass}
     * @param format
     *            {RDF/XML or TURTLE}
     * @return {An OntClass}
     */
    public synchronized OntClass ClassOwlToJena(OWLClass data, String format) {

        while (available == false) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println("ClassOwlToJena::: " + e);
            }
        }

        available = false;

        try {
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.createOntology(IRI
                    .create("http://www.semanticweb.org/owlapi/ontologies/ontology"));

            OWLDataFactory factory = manager.getOWLDataFactory();
            OWLDeclarationAxiom declarationAxiom = factory.getOWLDeclarationAxiom(data);
            manager.addAxiom(ontology, declarationAxiom);

            OntModel jenamodel = ModelOwlToJenaConvert(ontology, format);

            available = true;
            notifyAll();
            return jenamodel.getOntClass(data.getIRI().toString());
        } catch (OWLOntologyCreationException eoc) {
            System.err.print("ClassOwlToJena::: ");
            eoc.printStackTrace();
            return null;
        }

    }

    // //////////////////////////////////////////////////////////////////////////////

    /**
     * This function converts a single ObjectProperty of Jena to an OWLObjectProperty of OWLAPI
     * 
     * @param jenadata
     *            {Jena ObjectProperty object}
     * @param format
     *            {RDF/XML}
     * @return {An OWLObjectProperty}
     */
    public synchronized OWLObjectProperty ObjPropJenaToOwl(ObjectProperty jenadata, String format) {

        while (available == false) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println("ClassOwlToJena::: " + e);
            }
        }

        available = false;

        try {
            OntModel model = ModelFactory.createOntologyModel();

            model.createObjectProperty(jenadata.getURI());

            OWLOntology owlmodel = ModelJenaToOwlConvert(model, format);

            available = true;
            notifyAll();
            return owlmodel.getObjectPropertiesInSignature().iterator().next();
        } catch (Exception e) {
            System.err.print("ClassOwlToJena::: ");
            e.printStackTrace();
            return null;
        }

    }

    // //////////////////////////////////////////////////////////////////////////////

    /**
     * This function converts a single OWLObjectProperty of owl to an ObjectProperty of Jena
     * 
     * @param data
     *            {An OWLObjectProperty object}
     * @param format
     *            {RDF/XML or TURTLE}
     * @return {An ObjectProperty}
     */

    public synchronized ObjectProperty ObjPropOwlToJena(OWLObjectProperty data, String format) {

        while (available == false) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println("ObjPropOwlToJena::: " + e);
            }
        }

        available = false;
        try {
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.createOntology(IRI
                    .create("http://www.semanticweb.org/owlapi/ontologies/ontology"));

            OWLDataFactory factory = manager.getOWLDataFactory();
            OWLDeclarationAxiom declarationAxiom = factory.getOWLDeclarationAxiom(data);
            manager.addAxiom(ontology, declarationAxiom);

            OntModel jenamodel = ModelOwlToJenaConvert(ontology, format);

            available = true;
            notifyAll();
            return jenamodel.getObjectProperty(data.getIRI().toString());
        } catch (OWLOntologyCreationException eoc) {
            System.err.print("ObjPropOwlToJena::: ");
            eoc.printStackTrace();
            return null;
        }
    }

    // //////////////////////////////////////////////////////////////////////////////

    /**
     * This function converts a DatatypeProperty of Jena to and OWLDataProperty of owl
     * 
     * @param jenadata
     *            {Jena DatatypeProperty object}
     * @param format
     *            {RDF/XML}
     * @return {An OWLDataProperty}
     */
    public synchronized OWLDataProperty DataPropJenaToOwl(DatatypeProperty jenadata, String format) {

        while (available == false) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println("DataPropJenaToOwl::: " + e);
            }
        }

        available = false;
        try {
            OntModel model = ModelFactory.createOntologyModel();

            model.createDatatypeProperty(jenadata.getURI());

            OWLOntology owlmodel = ModelJenaToOwlConvert(model, format);

            available = true;
            notifyAll();
            return owlmodel.getDataPropertiesInSignature().iterator().next();
        } catch (Exception e) {
            System.err.print("DataPropJenaToOwl::: ");
            e.printStackTrace();
            return null;
        }

    }

    // //////////////////////////////////////////////////////////////////////////////

    /**
     * This function converts a single OWLDataProperty of OWL to DatatypeProperty of Jena
     * 
     * @param data
     *            {An OWLDataProperty object}
     * @param format
     *            {RDF/XML or TURTLE}
     * @return {A DatatypeProperty object}
     */
    public synchronized DatatypeProperty DataPropOwlToJena(OWLDataProperty data, String format) {

        while (available == false) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println("DataPropOwlToJena::: " + e);
            }
        }

        available = false;

        try {
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.createOntology(IRI
                    .create("http://www.semanticweb.org/owlapi/ontologies/ontology"));

            OWLDataFactory factory = manager.getOWLDataFactory();
            OWLDeclarationAxiom declarationAxiom = factory.getOWLDeclarationAxiom(data);
            manager.addAxiom(ontology, declarationAxiom);

            OntModel jenamodel = ModelOwlToJenaConvert(ontology, format);

            available = true;
            notifyAll();
            return jenamodel.getDatatypeProperty(data.getIRI().toString());
        } catch (OWLOntologyCreationException eoc) {
            System.err.print("DataPropOwlToJena::: ");
            eoc.printStackTrace();
            return null;
        }

    }

    // //////////////////////////////////////////////////////////////////////////////

    /**
     * This function converts a single AnnotationProperty of Jena to an OWLAnnotationProperty of OWL
     * 
     * @param jenadata
     *            {Jena DatatypeProperty object}
     * @param format
     *            {RDF/XML}
     * @return {An OWLAnnotationProperty object}
     */

    public synchronized OWLAnnotationProperty AnnotationPropJenaToOwl(AnnotationProperty jenadata,
                                                                      String format) {

        while (available == false) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println("AnnotationPropJenaToOwl::: " + e);
            }
        }

        available = false;
        try {
            OntModel model = ModelFactory.createOntologyModel();

            model.createAnnotationProperty(jenadata.getURI());

            OWLOntology owlmodel = ModelJenaToOwlConvert(model, format);

            available = true;
            notifyAll();
            return owlmodel.getAnnotationPropertiesInSignature().iterator().next();
        } catch (Exception e) {
            System.err.print("AnnotationPropJenaToOwl::: ");
            e.printStackTrace();
            return null;
        }
    }

    // //////////////////////////////////////////////////////////////////////////////

    /**
     * This function converts a single OWLAnnotationProperty of OWL to an AnnotationProperty of Jena
     * 
     * @param data
     *            {An OWLAnnotationProperty object}
     * @param format
     *            {RDF/XML or TURTLE}
     * @return {An AnnotationProperty object}
     */
    public synchronized AnnotationProperty AnnotationPropOwlToJena(OWLAnnotationProperty data, String format) {

        while (available == false) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println("AnnotationPropOwlToJena::: " + e);
            }
        }

        available = false;

        try {
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.createOntology(IRI
                    .create("http://www.semanticweb.org/owlapi/ontologies/ontology"));

            OWLDataFactory factory = manager.getOWLDataFactory();
            OWLDeclarationAxiom declarationAxiom = factory.getOWLDeclarationAxiom(data);
            manager.addAxiom(ontology, declarationAxiom);

            OntModel jenamodel = ModelOwlToJenaConvert(ontology, format);

            available = true;
            notifyAll();
            return jenamodel.getAnnotationProperty(data.getIRI().toString());
        } catch (OWLOntologyCreationException eoc) {
            System.err.print("AnnotationPropOwlToJena::: ");
            eoc.printStackTrace();
            return null;
        }
    }
    // //////////////////////////////////////////////////////////////////////////////

}

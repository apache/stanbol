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

/**
 *
 * @author elvio
 */
import java.net.URI;
import java.util.Set;

import junit.framework.TestCase;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLIndividualAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.hp.hpl.jena.ontology.AnnotationProperty;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

public class ConversionTester extends TestCase {

    private static final String RDFXML = "RDF/XML";

    private static final String _BASE = "http://example.org/dummy";

    private static final URI CLAZZ = URI.create(_BASE + "#" + "Peanut");

    private static final URI SUBJECT = URI.create(_BASE + "#" + "Lucy");

    private static final URI OP = URI.create(_BASE + "#" + "hasSibling");

    private static final URI DP = URI.create(_BASE + "#" + "hasAge");

    private static final URI label = URI.create("http://www.w3.org/2000/01/rdf-schema#label");

    private static final String clazzlabel = "Peanut";

    private static final String VALUE = "8";

    private static final URI DATATYPE = URI.create("http://www.w3.org/2001/XMLSchema#int");

    private static final URI OBJECT = URI.create(_BASE + "#" + "Linus");

    public void testAxiomOwlToJenaResource() {

        JenaToOwlConvert j2o = new JenaToOwlConvert();

        OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
        OWLOntology ont = null;
        try {
            ont = mgr.createOntology();
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            fail("Can not create ontology");
        }

        OWLDataFactory factory = mgr.getOWLDataFactory();

        StmtIterator resource = null;

        OWLClass cls = factory.getOWLClass(IRI.create(CLAZZ));
        OWLDataProperty dp = factory.getOWLDataProperty(IRI.create(DP));
        OWLObjectProperty op = factory.getOWLObjectProperty(IRI.create(OP));
        OWLAnnotationProperty oa = factory.getOWLAnnotationProperty(IRI.create(label));
        OWLAnnotation oav = factory.getOWLAnnotation(oa, factory.getOWLStringLiteral(clazzlabel, "en"));
        OWLDatatype dt = factory.getOWLDatatype(IRI.create(DATATYPE));
        OWLNamedIndividual sub = factory.getOWLNamedIndividual(IRI.create(SUBJECT));
        OWLNamedIndividual obj = factory.getOWLNamedIndividual(IRI.create(OBJECT));
        OWLLiteral literal1 = factory.getOWLTypedLiteral(VALUE, dt);
        OWLDeclarationAxiom daxiomcls = factory.getOWLDeclarationAxiom(cls); // Classe
        OWLDeclarationAxiom daxiomop = factory.getOWLDeclarationAxiom(op); // obj prop
        OWLDeclarationAxiom daxiomdp = factory.getOWLDeclarationAxiom(dp); // data prop
        OWLDeclarationAxiom daxiomsub = factory.getOWLDeclarationAxiom(sub); // subject
        OWLDeclarationAxiom daxiomobj = factory.getOWLDeclarationAxiom(obj); // object

        OWLClassAssertionAxiom axiomsub = factory.getOWLClassAssertionAxiom(cls, sub); // Istanza
        OWLClassAssertionAxiom axiomobj = factory.getOWLClassAssertionAxiom(cls, obj); // Istanza
        OWLObjectPropertyAssertionAxiom axiomop = factory.getOWLObjectPropertyAssertionAxiom(op, sub, obj); // Obj
                                                                                                            // prop
                                                                                                            // tra
                                                                                                            // individui
        OWLDataPropertyAssertionAxiom axiomvalue = factory
                .getOWLDataPropertyAssertionAxiom(dp, obj, literal1); // Dataprop all'istanza;
        OWLAnnotationAssertionAxiom axioman = factory.getOWLAnnotationAssertionAxiom(cls.getIRI(), oav); // Annotazione

        mgr.addAxiom(ont, daxiomcls);
        mgr.addAxiom(ont, daxiomop);
        mgr.addAxiom(ont, daxiomdp);
        mgr.addAxiom(ont, daxiomsub);
        mgr.addAxiom(ont, daxiomobj);
        mgr.addAxiom(ont, axiomsub);
        mgr.addAxiom(ont, axiomobj);
        mgr.addAxiom(ont, axiomop);
        mgr.addAxiom(ont, axiomvalue);
        mgr.addAxiom(ont, axioman);

        Set<OWLAxiom> setaxiom = ont.getAxioms();

        try {
            resource = j2o.AxiomOwlToJenaResource(setaxiom, RDFXML);
            if (resource == null) {
                fail("Some errors occur");
            } else {
                String statment = "[http://www.w3.org/2000/01/rdf-schema#label, http://www.w3.org/2000/01/rdf-schema#range, http://www.w3.org/2000/01/rdf-schema#Literal] "
                                  + "[http://example.org/dummy#hasAge, http://www.w3.org/1999/02/22-rdf-syntax-ns#type, http://www.w3.org/2002/07/owl#DatatypeProperty] "
                                  + "[http://example.org/dummy#Linus, http://example.org/dummy#hasAge, \"8\"^^http://www.w3.org/2001/XMLSchema#int] "
                                  + "[http://example.org/dummy#Linus, http://www.w3.org/1999/02/22-rdf-syntax-ns#type, http://example.org/dummy#Peanut] "
                                  + "[http://example.org/dummy#hasSibling, http://www.w3.org/1999/02/22-rdf-syntax-ns#type, http://www.w3.org/2002/07/owl#ObjectProperty] "
                                  + "[http://example.org/dummy#Lucy, http://example.org/dummy#hasSibling, http://example.org/dummy#Linus] "
                                  + "[http://example.org/dummy#Lucy, http://www.w3.org/1999/02/22-rdf-syntax-ns#type, http://example.org/dummy#Peanut] "
                                  + "[http://www.w3.org/2000/01/rdf-schema#label, http://www.w3.org/1999/02/22-rdf-syntax-ns#type, http://www.w3.org/2002/07/owl#AnnotationProperty] "
                                  + "[http://example.org/dummy#Peanut, http://www.w3.org/2000/01/rdf-schema#label, \"Peanut\"@en] "
                                  + "[http://example.org/dummy#Peanut, http://www.w3.org/1999/02/22-rdf-syntax-ns#type, http://www.w3.org/2002/07/owl#Class]";

                int size = setaxiom.size();
                int count = 0;
                while (resource.hasNext()) {
                    Statement stm = resource.nextStatement();
                    Resource jsubj = stm.getSubject();
                    if (jsubj.getURI().equals(OP.toString()) || jsubj.getURI().equals(DP.toString())
                        || jsubj.getURI().equals(CLAZZ.toString())
                        || jsubj.getURI().equals(OBJECT.toString())
                        || jsubj.getURI().equals(SUBJECT.toString())
                        || jsubj.getURI().equals(label.toString())) if (statment.contains(stm.toString())) count++;
                }

                assertEquals(size, count);

            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caugth");
        } finally {
            assertNotNull(resource);
        }
    }

    public void testAnnotationPropJenaToOwl() {
        JenaToOwlConvert j2o = new JenaToOwlConvert();
        OntModel model = ModelFactory.createOntologyModel();
        AnnotationProperty jp = model.createAnnotationProperty(label.toString());
        OWLAnnotationProperty wp = null;
        try {
            wp = j2o.AnnotationPropJenaToOwl(jp, RDFXML);
            if (wp == null) {
                fail("Some errors occur");
            } else {
                assertEquals(wp.getIRI().toURI().toString(), jp.getURI());
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caugth");
        } finally {
            assertNotNull(wp);
        }
    }

    public void testAnnotationPropOwlToJena() {
        JenaToOwlConvert j2o = new JenaToOwlConvert();
        OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
        OWLDataFactory factory = mgr.getOWLDataFactory();
        OWLAnnotationProperty wp = factory.getOWLAnnotationProperty(IRI.create(label));
        AnnotationProperty jp = null;
        try {
            jp = j2o.AnnotationPropOwlToJena(wp, RDFXML);
            if (jp == null) {
                fail("Some errors occur");
            } else {
                assertEquals(wp.getIRI().toURI().toString(), jp.getURI());
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caugth");
        } finally {
            assertNotNull(jp);
        }
    }

    public void testClassJenaToOwl() {
        JenaToOwlConvert j2o = new JenaToOwlConvert();
        OntModel model = ModelFactory.createOntologyModel();
        OntClass jc = model.createClass(CLAZZ.toString());
        OWLClass wc = null;
        try {
            wc = j2o.ClassJenaToOwl(jc, RDFXML);
            if (wc == null) fail("Some problems accours");
            else {
                assertEquals(wc.getIRI().toURI().toString(), jc.getURI());
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caugth");
        } finally {
            assertNotNull(wc);
        }
    }

    public void testClassOwlToJena() {
        JenaToOwlConvert j2o = new JenaToOwlConvert();
        OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
        OWLDataFactory factory = mgr.getOWLDataFactory();
        OWLClass c = factory.getOWLClass(IRI.create(CLAZZ));
        OntClass jc = null;
        try {
            jc = j2o.ClassOwlToJena(c, RDFXML);
            if (jc == null) {
                fail("Some problem accours");
            } else {
                assertEquals(jc.getURI(), c.getIRI().toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caught");
        } finally {
            assertNotNull(jc);
        }

    }

    public void testDataPropJenaToOwl() {
        JenaToOwlConvert j2o = new JenaToOwlConvert();
        OntModel model = ModelFactory.createOntologyModel();
        DatatypeProperty jp = model.createDatatypeProperty(DP.toString());
        OWLDataProperty wp = null;
        try {
            wp = j2o.DataPropJenaToOwl(jp, RDFXML);
            if (wp == null) {
                fail("Some problem accours");
            } else {
                assertEquals(wp.getIRI().toURI().toString(), jp.getURI());
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caugth");
        } finally {
            assertNotNull(wp);
        }
    }

    public void testDataPropOwlToJena() {
        JenaToOwlConvert j2o = new JenaToOwlConvert();
        OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
        OWLDataFactory factory = mgr.getOWLDataFactory();
        OWLDataProperty dp = factory.getOWLDataProperty(IRI.create(DP));
        DatatypeProperty jdp = null;
        try {
            jdp = j2o.DataPropOwlToJena(dp, RDFXML);
            if (jdp == null) {
                fail("Some errors accour");
            } else {
                assertEquals(jdp.getURI(), dp.getIRI().toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caught");
        } finally {
            assertNotNull(jdp);
        }
    }

    public void testModelJenaToOwlConvert() {
        JenaToOwlConvert j2o = new JenaToOwlConvert();
        OntModel model = ModelFactory.createOntologyModel();
        OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
        OWLDataFactory factory = mgr.getOWLDataFactory();
        String dul = "http://www.loa-cnr.it/ontologies/DUL.owl";
        OWLOntology owl = null;

        try {
            model.read(dul, RDFXML);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Could not load ontology");
        }

        try {
            owl = j2o.ModelJenaToOwlConvert(model, RDFXML);
            if (owl == null) {
                fail("Some errors occur");
            } else {

                ExtendedIterator<OntClass> jenaclass = model.listNamedClasses();
                int jenaclassset = jenaclass.toSet().size();
                jenaclass = model.listNamedClasses();
                Set<OWLClass> owlclass = owl.getClassesInSignature();
                int countclass = 0;

                while (jenaclass.hasNext())
                    if (owlclass.contains(factory.getOWLClass(IRI.create(jenaclass.next().getURI())))) countclass++;
                if (countclass == jenaclassset) assertEquals(countclass, jenaclassset);
                else fail("Error in number of classes");

                ExtendedIterator<ObjectProperty> jenaprop = model.listObjectProperties();
                int jenapropset = jenaprop.toSet().size();
                jenaprop = model.listObjectProperties();
                Set<OWLObjectProperty> owlprop = owl.getObjectPropertiesInSignature();
                int countprop = 0;

                while (jenaprop.hasNext())
                    if (owlprop.contains(factory.getOWLObjectProperty(IRI.create(jenaprop.next().getURI())))) countprop++;
                if (countprop == jenapropset) assertEquals(countprop, jenapropset);
                else fail("Error in number of object properties");

                ExtendedIterator<DatatypeProperty> jenadata = model.listDatatypeProperties();
                int jenadataset = jenadata.toSet().size();
                jenadata = model.listDatatypeProperties();
                Set<OWLDataProperty> owldata = owl.getDataPropertiesInSignature();
                int countdata = 0;

                while (jenadata.hasNext())
                    if (owldata.contains(factory.getOWLDataProperty(IRI.create(jenadata.next().getURI())))) countdata++;
                if (countdata == jenadataset) assertEquals(countdata, jenadataset);
                else fail("Error in number of data properties");
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caugth");
        } finally {
            assertNotNull(owl);
        }
    }

    public void testModelOwlToJenaConvert() {
        JenaToOwlConvert j2o = new JenaToOwlConvert();
        OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
        OWLOntologyManager mgrf = OWLManager.createOWLOntologyManager();
        OWLDataFactory factory = mgrf.getOWLDataFactory();
        String dul = "http://www.loa-cnr.it/ontologies/DUL.owl";
        OWLOntology owl = null;
        OntModel jena = null;
        try {
            owl = mgr.loadOntologyFromOntologyDocument(IRI.create(dul));
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            fail("Could not load ontology");
        }
        try {
            jena = j2o.ModelOwlToJenaConvert(owl, "RDF/XML");
            if (jena == null) {
                fail("Some errors occur");
            } else {

                ExtendedIterator<OntClass> jenaclass = jena.listNamedClasses();
                int jenaclassset = jenaclass.toSet().size();
                jenaclass = jena.listNamedClasses();
                Set<OWLClass> owlclass = owl.getClassesInSignature();
                int countclass = 0;

                while (jenaclass.hasNext())
                    if (owlclass.contains(factory.getOWLClass(IRI.create(jenaclass.next().getURI())))) countclass++;
                if (countclass == jenaclassset) assertEquals(countclass, jenaclassset);
                else fail("Error in number of classes");

                ExtendedIterator<ObjectProperty> jenaprop = jena.listObjectProperties();
                int jenapropset = jenaprop.toSet().size();
                jenaprop = jena.listObjectProperties();
                Set<OWLObjectProperty> owlprop = owl.getObjectPropertiesInSignature();
                int countprop = 0;

                while (jenaprop.hasNext())
                    if (owlprop.contains(factory.getOWLObjectProperty(IRI.create(jenaprop.next().getURI())))) countprop++;
                if (countprop == jenapropset) assertEquals(countprop, jenapropset);
                else fail("Error in number of object properties");

                ExtendedIterator<DatatypeProperty> jenadata = jena.listDatatypeProperties();
                int jenadataset = jenadata.toSet().size();
                jenadata = jena.listDatatypeProperties();
                Set<OWLDataProperty> owldata = owl.getDataPropertiesInSignature();
                int countdata = 0;

                while (jenadata.hasNext())
                    if (owldata.contains(factory.getOWLDataProperty(IRI.create(jenadata.next().getURI())))) countdata++;
                if (countdata == jenadataset) assertEquals(countdata, jenadataset);
                else fail("Error in number of data properties");
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caugth");
        } finally {
            assertNotNull(jena);
        }
    }

    public void testObjPropJenaToOwl() {
        JenaToOwlConvert j2o = new JenaToOwlConvert();
        OntModel model = ModelFactory.createOntologyModel();
        ObjectProperty jp = model.createObjectProperty(OP.toString());
        OWLObjectProperty wp = null;
        try {
            wp = j2o.ObjPropJenaToOwl(jp, RDFXML);
            if (wp == null) {
                fail("Some errors occurs");
            } else {
                assertEquals(wp.getIRI().toURI().toString(), jp.getURI());
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caugth");
        } finally {
            assertNotNull(wp);
        }
    }

    public void testObjPropOwlToJena() {
        JenaToOwlConvert j2o = new JenaToOwlConvert();
        OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
        OWLDataFactory factory = mgr.getOWLDataFactory();
        OWLObjectProperty op = factory.getOWLObjectProperty(IRI.create(OP));
        ObjectProperty jop = null;
        try {
            jop = j2o.ObjPropOwlToJena(op, RDFXML);
            if (jop == null) {
                fail("Some errore occurs");
            } else {
                assertEquals(jop.getURI(), op.getIRI().toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caught");
        } finally {
            assertNotNull(jop);
        }

    }

    public void testResourceJenaToOwlAxiom() {

        JenaToOwlConvert j2o = new JenaToOwlConvert();
        OntModel model = ModelFactory.createOntologyModel();

        OntClass jenaclass = model.createClass(CLAZZ.toString());
        ObjectProperty jenaobprop = model.createObjectProperty(OP.toString());
        DatatypeProperty jenadataprop = model.createDatatypeProperty(DP.toString());
        Individual jenasub = model.createIndividual(SUBJECT.toString(), jenaclass);
        Individual jenaobj = model.createIndividual(OBJECT.toString(), jenaclass);
        AnnotationProperty jenaanno = model.createAnnotationProperty(label.toString());
        Literal value = model.createTypedLiteral(VALUE, DATATYPE.toString());

        model.add(jenasub, jenaobprop, jenaobj);
        model.add(jenasub, jenadataprop, value);
        model.add(jenasub, jenaanno, "Lucy", "en");

        Set<OWLAxiom> owlaxiom = null;

        try {
            owlaxiom = j2o.ResourceJenaToOwlAxiom(jenasub, RDFXML);
            if (owlaxiom == null) {
                fail("Some errors occur");
            } else {

                StmtIterator str = model.listStatements();

                int count = 0;
                while (str.hasNext()) {
                    Statement stm = str.next();
                    Resource subject = stm.getSubject();
                    if (SUBJECT.toString().equals(subject.getURI())) count++;
                }

                if (count == owlaxiom.size()) {
                    assertEquals(count, owlaxiom.size());
                } else {
                    fail("The number of axioms don't match the number of statement");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caugth");
        } finally {
            assertNotNull(owlaxiom);
        }
    }

    public void testEntityOwlToJenaResource() {

        JenaToOwlConvert j2o = new JenaToOwlConvert();
        OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
        OWLOntology ont = null;
        StmtIterator resource = null;

        try {
            ont = mgr.createOntology();
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            fail("Could not load ontology");
        }

        OWLDataFactory factory = mgr.getOWLDataFactory();

        OWLClass cls = factory.getOWLClass(IRI.create(CLAZZ));
        OWLDataProperty dp = factory.getOWLDataProperty(IRI.create(DP));
        OWLObjectProperty op = factory.getOWLObjectProperty(IRI.create(OP));
        OWLAnnotationProperty oa = factory.getOWLAnnotationProperty(IRI.create(label));
        OWLAnnotation oav = factory.getOWLAnnotation(oa, factory.getOWLStringLiteral(clazzlabel, "en"));
        OWLDatatype dt = factory.getOWLDatatype(IRI.create(DATATYPE));
        OWLNamedIndividual sub = factory.getOWLNamedIndividual(IRI.create(SUBJECT));
        OWLNamedIndividual obj = factory.getOWLNamedIndividual(IRI.create(OBJECT));
        OWLLiteral literal1 = factory.getOWLTypedLiteral(VALUE, dt);
        OWLDeclarationAxiom daxiomcls = factory.getOWLDeclarationAxiom(cls); // Classe
        OWLDeclarationAxiom daxiomop = factory.getOWLDeclarationAxiom(op); // obj prop
        OWLDeclarationAxiom daxiomdp = factory.getOWLDeclarationAxiom(dp); // data prop
        OWLDeclarationAxiom daxiomsub = factory.getOWLDeclarationAxiom(sub); // subject
        OWLDeclarationAxiom daxiomobj = factory.getOWLDeclarationAxiom(obj); // object

        OWLClassAssertionAxiom axiomsub = factory.getOWLClassAssertionAxiom(cls, sub); // Istanza
        OWLClassAssertionAxiom axiomobj = factory.getOWLClassAssertionAxiom(cls, obj); // Istanza
        OWLObjectPropertyAssertionAxiom axiomop = factory.getOWLObjectPropertyAssertionAxiom(op, sub, obj); // Obj
                                                                                                            // prop
                                                                                                            // tra
                                                                                                            // individui
        OWLDataPropertyAssertionAxiom axiomvalue = factory
                .getOWLDataPropertyAssertionAxiom(dp, sub, literal1); // Dataprop all'istanza;
        OWLAnnotationAssertionAxiom axioman = factory.getOWLAnnotationAssertionAxiom(cls.getIRI(), oav); // Annotazione

        mgr.addAxiom(ont, daxiomcls);
        mgr.addAxiom(ont, daxiomop);
        mgr.addAxiom(ont, daxiomdp);
        mgr.addAxiom(ont, daxiomsub);
        mgr.addAxiom(ont, daxiomobj);
        mgr.addAxiom(ont, axiomsub);
        mgr.addAxiom(ont, axiomobj);
        mgr.addAxiom(ont, axiomop);
        mgr.addAxiom(ont, axiomvalue);
        mgr.addAxiom(ont, axioman);

        Set<OWLIndividualAxiom> ind = ont.getAxioms(sub);

        try {
            resource = j2o.EntityOwlToJenaResource(daxiomsub.getEntity(), ont, RDFXML);
            if (resource == null) {
                fail("Some errors accour");
            } else {

                int cont = 0;
                while (resource.hasNext()) {
                    Statement stm = resource.nextStatement();
                    IRI subres = IRI.create(stm.getSubject().getURI());
                    if (("<" + subres + ">").equals(daxiomsub.getEntity().toString())) cont++;
                }

                assertEquals(ind.size(), (cont - 1));

            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caugth");
        } finally {
            assertNotNull(resource);
        }
    }
}

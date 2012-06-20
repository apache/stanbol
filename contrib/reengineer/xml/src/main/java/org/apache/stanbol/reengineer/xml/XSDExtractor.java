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

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.stanbol.reengineer.base.api.DataSource;
import org.apache.stanbol.reengineer.base.api.ReengineeringException;
import org.apache.stanbol.reengineer.base.api.util.ReengineerUriRefGenerator;
import org.apache.stanbol.reengineer.xml.vocab.XSD_OWL;
import org.apache.xerces.dom.PSVIDocumentImpl;
import org.apache.xerces.impl.dv.DatatypeException;
import org.apache.xerces.impl.dv.xs.XSSimpleTypeDecl;
import org.apache.xerces.impl.xs.XSAnnotationImpl;
import org.apache.xerces.impl.xs.XSAttributeGroupDecl;
import org.apache.xerces.impl.xs.XSAttributeUseImpl;
import org.apache.xerces.impl.xs.XSComplexTypeDecl;
import org.apache.xerces.impl.xs.XSElementDecl;
import org.apache.xerces.impl.xs.XSModelGroupImpl;
import org.apache.xerces.impl.xs.XSModelImpl;
import org.apache.xerces.impl.xs.XSParticleDecl;
import org.apache.xerces.impl.xs.util.XSObjectListImpl;
import org.apache.xerces.xs.ElementPSVI;
import org.apache.xerces.xs.StringList;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModelGroup;
import org.apache.xerces.xs.XSNamedMap;
import org.apache.xerces.xs.XSObject;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSTerm;
import org.apache.xerces.xs.XSTypeDefinition;
import org.apache.xerces.xs.datatypes.ObjectList;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XSDExtractor extends ReengineerUriRefGenerator {

    public final Logger log = LoggerFactory.getLogger(getClass());

    public XSDExtractor() {}

    private void addComplexType(String schemaNS,
                                OWLOntologyManager manager,
                                OWLDataFactory factory,
                                OWLOntology schemaOntology,
                                IRI complexType,
                                XSComplexTypeDecl xsComplexTypeDefinition) {

        String name = xsComplexTypeDefinition.getName();
        if (name != null) {
            OWLDataPropertyAssertionAxiom hasName = createOWLDataPropertyAssertionAxiom(factory,
                XSD_OWL.name, complexType, name);
            manager.applyChange(new AddAxiom(schemaOntology, hasName));
        }

        XSAttributeGroupDecl xsAttributeGroupDecl = xsComplexTypeDefinition.getAttrGrp();
        if (xsAttributeGroupDecl != null) {
            String attrGroupName = xsAttributeGroupDecl.getName();

            IRI attrGroupIRI = IRI.create(schemaNS + attrGroupName);
            OWLClassAssertionAxiom attrGroup = createOWLClassAssertionAxiom(factory, XSD_OWL.AttributeGroup,
                attrGroupIRI);
            manager.applyChange(new AddAxiom(schemaOntology, attrGroup));

            XSObjectList xsObjectList = xsAttributeGroupDecl.getAttributeUses();
            for (int i = 0, j = xsObjectList.getLength(); i < j; i++) {
                XSAttributeUseImpl xsAttributeUseImpl = (XSAttributeUseImpl) xsObjectList.item(i);

                String attrName = xsAttributeUseImpl.getAttrDeclaration().getName();

                IRI attrResourceIRI = IRI.create(schemaNS + attrGroupName + "_" + attrName);

                OWLClassAssertionAxiom attrResource = createOWLClassAssertionAxiom(factory,
                    XSD_OWL.Attribute, attrResourceIRI);
                manager.applyChange(new AddAxiom(schemaOntology, attrResource));

                manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory,
                    XSD_OWL.name, attrResourceIRI, attrName)));
                manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory,
                    XSD_OWL.required, attrResourceIRI, xsAttributeUseImpl.getRequired())));

                IRI simpleTypeIRI = IRI.create(schemaNS
                                               + xsAttributeUseImpl.getAttrDeclaration().getTypeDefinition()
                                                       .getName());
                OWLClassAssertionAxiom simpleType = createOWLClassAssertionAxiom(factory, XSD_OWL.SimpleType,
                    simpleTypeIRI);
                manager.applyChange(new AddAxiom(schemaOntology, simpleType));

                // ADD SIMPLE TYPE DEFINITION TO THE RDF
                addSimpleType(schemaNS, manager, factory, schemaOntology, simpleTypeIRI,
                    (XSSimpleTypeDecl) xsAttributeUseImpl.getAttrDeclaration().getTypeDefinition());

                manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(
                    factory, XSD_OWL.type, attrResourceIRI, simpleTypeIRI)));
                manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(
                    factory, XSD_OWL.hasAttributeUse, attrGroupIRI, attrResourceIRI)));
            }
        }

        XSObjectList xsObjectList = xsComplexTypeDefinition.getAnnotations();
        if (xsObjectList != null) {
            for (int i = 0, j = xsObjectList.getLength(); i < j; i++) {

                XSAnnotationImpl xsAnnotationImpl = (XSAnnotationImpl) xsObjectList.item(i);

                String annotationString = xsAnnotationImpl.getAnnotationString();
                if (annotationString != null) {

                    IRI annotatioIRI = IRI.create(schemaNS + name + "_annotation_" + i);
                    OWLClassAssertionAxiom annotation = createOWLClassAssertionAxiom(factory,
                        XSD_OWL.Annotation, annotatioIRI);
                    manager.applyChange(new AddAxiom(schemaOntology, annotation));

                    log.debug("DOCUMENTATION : " + xsAnnotationImpl);

                    manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(
                        factory, XSD_OWL.value, annotatioIRI, annotationString)));
                    manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(
                        factory, XSD_OWL.hasAnnotation, complexType, annotatioIRI)));
                }
            }
        }

        short prohibitedSubstitution = xsComplexTypeDefinition.getProhibitedSubstitutions();

        // Derivation restriction
        if (prohibitedSubstitution == XSConstants.DERIVATION_RESTRICTION) {
            // Prohibited restriction
            manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory,
                XSD_OWL.hasProhibitedSubstitutions, complexType, XSD_OWL.PROHIBITED_RESTRICTION)));
        } else if (prohibitedSubstitution == XSConstants.DERIVATION_EXTENSION) {
            // Prohibited extension
            manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory,
                XSD_OWL.hasProhibitedSubstitutions, complexType, XSD_OWL.PROHIBITED_EXTENSION)));
        } else if (prohibitedSubstitution == XSConstants.DERIVATION_NONE) {
            // Prohibited none
            manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory,
                XSD_OWL.hasProhibitedSubstitutions, complexType, XSD_OWL.PROHIBITED_NONE)));
        }

        // Abstract
        boolean abstractProperty = xsComplexTypeDefinition.getAbstract();
        manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory,
            XSD_OWL.abstractProperty, complexType, abstractProperty)));

        // Final value
        short finalValue = xsComplexTypeDefinition.getFinal();
        if (finalValue == XSConstants.DERIVATION_EXTENSION) {
            // Derivation extension
            manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory,
                XSD_OWL.hasFinal, complexType, XSD_OWL.DERIVATION_EXTENSION)));
        } else if (finalValue == XSConstants.DERIVATION_RESTRICTION) {
            // Derivation restriction
            manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory,
                XSD_OWL.hasFinal, complexType, XSD_OWL.DERIVATION_RESTRICTION)));
        } else if (finalValue == XSConstants.DERIVATION_NONE) {
            // Derivation none
            manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory,
                XSD_OWL.hasFinal, complexType, XSD_OWL.DERIVATION_NONE)));
        }

        XSParticle xsParticle = xsComplexTypeDefinition.getParticle();

        short contentType = xsComplexTypeDefinition.getContentType();

        if (contentType == XSComplexTypeDefinition.CONTENTTYPE_EMPTY) {
            log.debug("CONTENTTYPE_EMPTY");
        } else if (contentType == XSComplexTypeDefinition.CONTENTTYPE_ELEMENT) {
            log.debug("CONTENTTYPE_ELEMENT");

        } else if (contentType == XSComplexTypeDefinition.CONTENTTYPE_MIXED) {
            log.debug("CONTENTTYPE_MIXED");
        } else if (contentType == XSComplexTypeDefinition.CONTENTTYPE_SIMPLE) {
            log.debug("CONTENTTYPE_SIMPLE");
        }

        XSObjectList objectList = xsComplexTypeDefinition.getAttributeUses();
        log.debug("XSOBJECT SIZE: " + objectList.getLength());
        for (int i = 0, j = objectList.getLength(); i < j; i++) {

            XSAttributeUseImpl xsAttributeUseImpl = (XSAttributeUseImpl) objectList.item(i);

            String attrName = xsAttributeUseImpl.getAttrDeclaration().getName();

            IRI attrResourceIRI = IRI.create(schemaNS + name + "_" + attrName);
            OWLClassAssertionAxiom attrResource = createOWLClassAssertionAxiom(factory, XSD_OWL.Attribute,
                attrResourceIRI);
            manager.addAxiom(schemaOntology, attrResource);

            manager.addAxiom(schemaOntology,
                createOWLDataPropertyAssertionAxiom(factory, XSD_OWL.name, attrResourceIRI, attrName));
            manager.addAxiom(
                schemaOntology,
                createOWLDataPropertyAssertionAxiom(factory, XSD_OWL.required, attrResourceIRI,
                    xsAttributeUseImpl.getRequired()));

            IRI simpleTypeIRI = IRI.create(schemaNS
                                           + xsAttributeUseImpl.getAttrDeclaration().getTypeDefinition()
                                                   .getName());
            OWLClassAssertionAxiom simpleType = createOWLClassAssertionAxiom(factory, XSD_OWL.SimpleType,
                simpleTypeIRI);
            manager.applyChange(new AddAxiom(schemaOntology, simpleType));

            // ADD SIMPLE TYPE DEFINITION TO THE RDF
            addSimpleType(schemaNS, manager, factory, schemaOntology, simpleTypeIRI,
                (XSSimpleTypeDecl) xsAttributeUseImpl.getAttrDeclaration().getTypeDefinition());

            manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory,
                XSD_OWL.type, attrResourceIRI, simpleTypeIRI)));

            log.debug("ATTRIBUTE USES REQUIRED "
                      + xsAttributeUseImpl.getAttrDeclaration().getTypeDefinition().getName());

            manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory,
                XSD_OWL.hasAttributeUse, complexType, attrResourceIRI)));

        }

        if (xsParticle != null) {

            int maxOccurs = xsParticle.getMaxOccurs();
            int minOccurs = xsParticle.getMinOccurs();

            manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory,
                XSD_OWL.maxOccurs, complexType, maxOccurs)));
            manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory,
                XSD_OWL.minOccurs, complexType, minOccurs)));

            XSTerm xsTerm = xsParticle.getTerm();
            if (xsTerm instanceof XSModelGroupImpl) {
                XSModelGroupImpl xsModelGroup = (XSModelGroupImpl) xsTerm;
                XSObjectList list = xsModelGroup.getParticles();

                IRI particleIRI = null;
                OWLClassAssertionAxiom particle = null;

                short compositor = xsModelGroup.getCompositor();
                if (compositor == XSModelGroup.COMPOSITOR_ALL) {
                    particleIRI = IRI.create(schemaNS + name + "_all");
                    particle = createOWLClassAssertionAxiom(factory, XSD_OWL.All, particleIRI);
                } else if (compositor == XSModelGroup.COMPOSITOR_CHOICE) {
                    particleIRI = IRI.create(schemaNS + name + "_choice");
                    particle = createOWLClassAssertionAxiom(factory, XSD_OWL.Choice, particleIRI);
                } else {
                    particleIRI = IRI.create(schemaNS + name + "_sequence");
                    particle = createOWLClassAssertionAxiom(factory, XSD_OWL.Sequence, particleIRI);
                }

                manager.applyChange(new AddAxiom(schemaOntology, particle));

                if (particle != null) {

                    manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(
                        factory, XSD_OWL.hasCompositor, complexType, particleIRI)));

                    for (int i = 0, j = list.getLength(); i < j; i++) {

                        XSParticleDecl xsParticleDecl = (XSParticleDecl) list.item(i);

                        XSTerm xsParticleTerm = xsParticleDecl.getTerm();
                        if (xsParticleTerm instanceof XSElementDecl) {
                            XSElementDecl xsElementDecl = (XSElementDecl) xsParticleTerm;
                            String particleName = xsParticleDecl.getTerm().getName();

                            IRI elementParticleIRI = IRI.create(schemaNS + particleName);
                            OWLClassAssertionAxiom elementParticle = createOWLClassAssertionAxiom(factory,
                                XSD_OWL.Element, elementParticleIRI);
                            manager.applyChange(new AddAxiom(schemaOntology, elementParticle));

                            XSTypeDefinition xsTypeDefinition = xsElementDecl.getTypeDefinition();
                            String type = schemaNS + xsTypeDefinition.getName();

                            XSTypeDefinition baseTypeDefinition = xsTypeDefinition.getBaseType();
                            short baseType = baseTypeDefinition.getTypeCategory();

                            IRI typeResourceIRI = IRI.create(type);
                            OWLClassAssertionAxiom typeResource;
                            if (baseType == XSTypeDefinition.SIMPLE_TYPE) {
                                log.debug("SIMPLE TYPE");
                                typeResource = createOWLClassAssertionAxiom(factory, XSD_OWL.SimpleType,
                                    typeResourceIRI);

                                addSimpleType(schemaNS, manager, factory, schemaOntology, typeResourceIRI,
                                    (XSSimpleTypeDecl) baseTypeDefinition);
                            } else {
                                log.debug("COMPLEX TYPE");
                                typeResource = createOWLClassAssertionAxiom(factory, XSD_OWL.ComplexType,
                                    typeResourceIRI);

                                addComplexType(schemaNS, manager, factory, schemaOntology, typeResourceIRI,
                                    (XSComplexTypeDecl) baseTypeDefinition);
                            }

                            manager.applyChange(new AddAxiom(schemaOntology, typeResource));

                            manager.applyChange(new AddAxiom(schemaOntology,
                                    createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.type,
                                        elementParticleIRI, typeResourceIRI)));

                            log.debug("OBJ " + xsElementDecl.getTypeDefinition().getName());
                        }

                    }
                }

                log.debug("COMPOSITOR : " + xsModelGroup.getCompositor());
                log.debug("COMPOSITOR_SEQUENCE : " + XSModelGroup.COMPOSITOR_SEQUENCE);

            }
            log.debug("TERM: " + xsTerm.getClass().getCanonicalName());
        } else {
            log.debug("NO PARTICLE");
        }
    }

    private void addSimpleType(String schemaNS,
                               OWLOntologyManager manager,
                               OWLDataFactory factory,
                               OWLOntology schemaOntology,
                               IRI simpleType,
                               XSSimpleTypeDecl xsSimpleTypeDefinition) {

        // NAME
        String name = xsSimpleTypeDefinition.getName();

        log.debug("NAME OF SIMPLE TYPE : " + name);

        // add name literal to the simple type declaration

        manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory,
            XSD_OWL.name, simpleType, name)));

        // FINAL
        short finalValue = xsSimpleTypeDefinition.getFinal();
        if (finalValue == XSConstants.DERIVATION_EXTENSION) {
            // Derivation extension
            manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory,
                XSD_OWL.name, simpleType, XSD_OWL.DERIVATION_EXTENSION)));
        } else if (finalValue == XSConstants.DERIVATION_RESTRICTION) {
            // Derivation restriction
            manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory,
                XSD_OWL.name, simpleType, XSD_OWL.DERIVATION_RESTRICTION)));
        } else if (finalValue == XSConstants.DERIVATION_NONE) {
            // Derivation none
            manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory,
                XSD_OWL.name, simpleType, XSD_OWL.DERIVATION_NONE)));
        }

        ObjectList objectList = xsSimpleTypeDefinition.getActualEnumeration();

        if (objectList != null && objectList.getLength() > 0) {

            IRI enumerationIRI = IRI.create(schemaNS + name + "_enumeration");
            OWLClassAssertionAxiom enumeration = createOWLClassAssertionAxiom(factory, XSD_OWL.Enumeration,
                enumerationIRI);
            manager.applyChange(new AddAxiom(schemaOntology, enumeration));

            // add value property to enumeration UriRef
            for (int i = 0, j = objectList.getLength(); i < j; i++) {
                manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory,
                    XSD_OWL.value, simpleType, objectList.item(i).toString())));
            }

            // add triple asserting that a simple type has an enumeration
            manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory,
                XSD_OWL.hasEnumeration, simpleType, enumerationIRI)));
        }

        IRI option = null;
        try {
            // Whitepace
            /*
             * This line, sometimes, generates an exception when trying to get simple type definition for
             * white space. However, even if there is the exception, the line returns a zero value. In this
             * case, the WS_PRESERVE option is set in the catch block.
             */
            short whitespace = xsSimpleTypeDefinition.getWhitespace();
            if (whitespace == XSSimpleTypeDecl.WS_COLLAPSE) option = XSD_OWL.COLLAPSE; // Collapse
            else if (whitespace == XSSimpleTypeDecl.WS_PRESERVE) option = XSD_OWL.PRESERVE; // Preserve
            else if (whitespace == XSSimpleTypeDecl.WS_REPLACE) option = XSD_OWL.REPLACE; // Replace
            log.debug("Whitespace facet value for XSD simple type definition is {}.", whitespace);
        } catch (DatatypeException e) {
            // Exception fallback is to preserve the simple type definition.
            log.warn(
                "Unable to obtain whitespace facet value for simple type definition. Defaulting to WS_PRESERVE."
                        + "\n\tOriginal message follows :: {}", e.getMessage());
            option = XSD_OWL.PRESERVE;
        } finally {
            OWLAxiom axiom = createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.hasWhitespace,
                simpleType, option);
            if (option != null) manager.applyChange(new AddAxiom(schemaOntology, axiom));
        }

        // ADD BASE TYPE
        XSTypeDefinition baseTypeDefinition = xsSimpleTypeDefinition.getBaseType();
        String baseTypeNS = baseTypeDefinition.getNamespace();
        String baseTypeName = baseTypeDefinition.getName();

        // add base type
        manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory,
            XSD_OWL.base, simpleType, IRI.create(baseTypeNS + "#" + baseTypeName))));

        // ADD MAX ECLUSIVE VALUE
        Object obj = xsSimpleTypeDefinition.getMaxExclusiveValue();
        if (obj != null) {
            IRI maxExclusiveIRI = IRI.create(schemaNS + name + "_maxEclusive");
            OWLClassAssertionAxiom maxExclusive = createOWLClassAssertionAxiom(factory, XSD_OWL.MaxEclusive,
                maxExclusiveIRI);
            manager.applyChange(new AddAxiom(schemaOntology, maxExclusive));

            manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory,
                XSD_OWL.value, maxExclusiveIRI, obj.toString())));
        }

        // ADD MIN ECLUSIVE VALUE
        obj = xsSimpleTypeDefinition.getMinExclusiveValue();
        if (obj != null) {
            IRI minExclusiveIRI = IRI.create(schemaNS + name + "_minEclusive");
            OWLClassAssertionAxiom minExclusive = createOWLClassAssertionAxiom(factory, XSD_OWL.MinEclusive,
                minExclusiveIRI);
            manager.applyChange(new AddAxiom(schemaOntology, minExclusive));

            manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory,
                XSD_OWL.value, minExclusiveIRI, obj.toString())));
        }

        // ADD MAX INCLUSIVE VALUE
        obj = xsSimpleTypeDefinition.getMaxInclusiveValue();
        if (obj != null) {
            IRI maxInclusiveIRI = IRI.create(schemaNS + name + "_maxInclusive");
            OWLClassAssertionAxiom maxInclusive = createOWLClassAssertionAxiom(factory, XSD_OWL.MaxInclusive,
                maxInclusiveIRI);
            manager.applyChange(new AddAxiom(schemaOntology, maxInclusive));

            manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory,
                XSD_OWL.value, maxInclusiveIRI, obj.toString())));
        }

        // ADD MIN INCLUSIVE VALUE
        obj = xsSimpleTypeDefinition.getMinInclusiveValue();
        if (obj != null) {
            IRI minInclusiveIRI = IRI.create(schemaNS + name + "_minInclusive");
            OWLClassAssertionAxiom minInclusive = createOWLClassAssertionAxiom(factory, XSD_OWL.MinInclusive,
                minInclusiveIRI);
            manager.applyChange(new AddAxiom(schemaOntology, minInclusive));

            manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory,
                XSD_OWL.value, minInclusiveIRI, obj.toString())));
        }

        // ADD PATTERNS
        StringList stringList = xsSimpleTypeDefinition.getLexicalPattern();
        if (stringList != null) {
            for (int i = 0, j = stringList.getLength(); i < j; i++) {

                IRI patternIRI = IRI.create(schemaNS + name + "_pattern_" + i);
                OWLClassAssertionAxiom pattern = createOWLClassAssertionAxiom(factory, XSD_OWL.Pattern,
                    patternIRI);
                manager.applyChange(new AddAxiom(schemaOntology, pattern));

                manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory,
                    XSD_OWL.value, patternIRI, stringList.item(i))));
                manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(
                    factory, XSD_OWL.value, simpleType, patternIRI)));

            }
        }

        XSObjectListImpl xsObjectListImpl = xsSimpleTypeDefinition.patternAnnotations;
        if (xsObjectListImpl != null) {
            for (int i = 0, j = xsObjectListImpl.getLength(); i < j; i++) {
                XSObject xsObject = xsObjectListImpl.item(i);
                log.debug("PATTERN : " + xsObject.getClass().getCanonicalName());
            }
        }

    }

    public OWLOntology getOntologySchema(String graphNS, IRI outputIRI, DataSource dataSource) throws ReengineeringException {

        if (dataSource == null) throw new IllegalArgumentException("Data source cannot be null.");

        if (!graphNS.endsWith("#")) {
            graphNS += "#";
        }

        OWLOntology dataSourceSchemaOntology = null;

        new DocumentTraversal() {

            @Override
            public TreeWalker createTreeWalker(Node arg0, int arg1, NodeFilter arg2, boolean arg3) throws DOMException {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public NodeIterator createNodeIterator(Node arg0, int arg1, NodeFilter arg2, boolean arg3) throws DOMException {
                // TODO Auto-generated method stub
                return null;
            }
        };

        PSVIDocumentImpl psviDocumentImpl = new PSVIDocumentImpl();
        XSSimpleTypeDecl m;

        OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
        OWLDataFactory factory = OWLManager.getOWLDataFactory();

        log.debug("XSD output IRI : " + outputIRI);

        try {
            if (outputIRI != null) dataSourceSchemaOntology = ontologyManager.createOntology(outputIRI);
            else dataSourceSchemaOntology = ontologyManager.createOntology();
        } catch (OWLOntologyCreationException e) {
            throw new ReengineeringException(e);
        }

        if (dataSourceSchemaOntology != null) {

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

            dbf.setNamespaceAware(true);

            String id = "http://apache.org/xml/properties/dom/document-class-name";
            Object value = "org.apache.xerces.dom.PSVIDocumentImpl";
            try {
                dbf.setAttribute(id, value);
                dbf.setNamespaceAware(true);
                dbf.setValidating(true);
                dbf.setAttribute("http://apache.org/xml/features/validation/schema", Boolean.TRUE);
            } catch (IllegalArgumentException e) {
                log.error("Could not set parser property", e);
            }

            DocumentBuilder db;
            Document document;

            try {
                db = dbf.newDocumentBuilder();

                // FIXME hack for unit tests, this should have a configurable offline mode!!!
                db.setEntityResolver(new EntityResolver() {
                    public InputSource resolveEntity(String publicId, String systemId) throws SAXException,
                                                                                      IOException {
                        if (systemId.endsWith("DWML.xsd")) {
                            InputStream dtdStream = XSDExtractor.class.getResourceAsStream("/xml/DWML.xsd");
                            return new InputSource(dtdStream);
                        }
                        // else
                        // if (systemId.endsWith("ndfd_data.xsd"))
                        // {
                        // InputStream dtdStream = XSDExtractor.class
                        // .getResourceAsStream("/xml/ndfd_data.xsd");
                        // return new InputSource(dtdStream);
                        // }
                        // else
                        // if (systemId.endsWith("meta_data.xsd"))
                        // {
                        // InputStream dtdStream = XSDExtractor.class
                        // .getResourceAsStream("/xml/meta_data.xsd");
                        // return new InputSource(dtdStream);
                        // }
                        else {
                            return null;
                        }
                    }
                });

                document = db.parse((InputStream) dataSource.getDataSource());
                Element root = document.getDocumentElement();

                log.debug("Root is : " + root.getNodeName());

                ElementPSVI rootPsvi = (ElementPSVI) root;

                XSModelImpl xsModel = (XSModelImpl) rootPsvi.getSchemaInformation();

                log.debug("Schema model : " + xsModel.getClass().getCanonicalName());

                XSNamedMap xsNamedMap = xsModel.getComponents(XSConstants.ELEMENT_DECLARATION);
                for (int i = 0, j = xsNamedMap.getLength(); i < j; i++) {
                    XSObject xsObject = xsNamedMap.item(i);
                    if (xsObject instanceof XSElementDeclaration) {

                        XSElementDeclaration xsElementDeclaration = (XSElementDeclaration) xsObject;

                        String name = xsElementDeclaration.getName();
                        if (name != null && !name.equals("")) {

                            IRI elementIndividual = IRI.create(graphNS + name);

                            OWLClassAssertionAxiom element = createOWLClassAssertionAxiom(factory,
                                XSD_OWL.Element, elementIndividual);
                            ontologyManager.applyChange(new AddAxiom(dataSourceSchemaOntology, element));

                            OWLDataPropertyAssertionAxiom data = createOWLDataPropertyAssertionAxiom(factory,
                                XSD_OWL.name, elementIndividual, name);
                            ontologyManager.applyChange(new AddAxiom(dataSourceSchemaOntology, data));

                            boolean boolValue = xsElementDeclaration.getAbstract();
                            data = createOWLDataPropertyAssertionAxiom(factory, XSD_OWL.abstractProperty,
                                elementIndividual, boolValue);
                            ontologyManager.applyChange(new AddAxiom(dataSourceSchemaOntology, data));

                            XSTypeDefinition xsTypeDefinition = xsElementDeclaration.getTypeDefinition();
                            String type = graphNS + xsTypeDefinition.getName();

                            XSTypeDefinition baseTypeDefinition = xsTypeDefinition.getBaseType();
                            short baseType = baseTypeDefinition.getTypeCategory();

                            OWLClassAssertionAxiom typeResource;
                            log.debug("SIMPLE TYPE PRINT " + XSTypeDefinition.SIMPLE_TYPE);
                            log.debug("COMPLEX TYPE PRINT " + XSTypeDefinition.COMPLEX_TYPE);

                            IRI typeIRI = IRI.create(type);

                            if (baseType == XSTypeDefinition.SIMPLE_TYPE) {
                                log.debug("SIMPLE TYPE");
                                typeResource = createOWLClassAssertionAxiom(factory, XSD_OWL.SimpleType,
                                    typeIRI);
                                addSimpleType(graphNS, ontologyManager, factory, dataSourceSchemaOntology,
                                    typeIRI, (XSSimpleTypeDecl) xsTypeDefinition);

                            } else {
                                log.debug("COMPLEX TYPE");
                                typeResource = createOWLClassAssertionAxiom(factory, XSD_OWL.ComplexType,
                                    typeIRI);

                                addComplexType(graphNS, ontologyManager, factory, dataSourceSchemaOntology,
                                    typeIRI, (XSComplexTypeDecl) xsTypeDefinition);
                            }

                            ontologyManager.applyChange(new AddAxiom(dataSourceSchemaOntology, typeResource));

                            // add the type property to the element declaration

                            log.debug("---- graph NS : " + graphNS);
                            log.debug("---- type IRI : " + typeIRI.toString());
                            OWLObjectPropertyAssertionAxiom hasType = createOWLObjectPropertyAssertionAxiom(
                                factory, XSD_OWL.type, elementIndividual, typeIRI);
                            ontologyManager.applyChange(new AddAxiom(dataSourceSchemaOntology, hasType));

                            // add the scope property to the element declaration
                            short scope = xsElementDeclaration.getScope();

                            OWLObjectPropertyAssertionAxiom scopeAxiom;
                            if (scope == XSConstants.SCOPE_ABSENT) {
                                // Scope absent
                                scopeAxiom = createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.hasScope,
                                    elementIndividual, XSD_OWL.ScopeAbsent);
                            } else if (scope == XSConstants.SCOPE_LOCAL) {
                                scopeAxiom = createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.hasScope,
                                    elementIndividual, XSD_OWL.ScopeLocal);
                            } else {
                                scopeAxiom = createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.hasScope,
                                    elementIndividual, XSD_OWL.ScopeGlobal);
                            }

                            ontologyManager.applyChange(new AddAxiom(dataSourceSchemaOntology, scopeAxiom));

                            // add the constraint type property to the element declaration
                            short constraingType = xsElementDeclaration.getConstraintType();
                            OWLObjectPropertyAssertionAxiom constraintAxiom;
                            if (constraingType == XSConstants.VC_NONE) {
                                // Value constraint none
                                constraintAxiom = createOWLObjectPropertyAssertionAxiom(factory,
                                    XSD_OWL.hasConstraintType, elementIndividual, XSD_OWL.VC_NONE);
                            } else if (constraingType == XSConstants.VC_DEFAULT) {
                                constraintAxiom = createOWLObjectPropertyAssertionAxiom(factory,
                                    XSD_OWL.hasConstraintType, elementIndividual, XSD_OWL.VC_DEFAULT);
                            } else {
                                // Value constraint fixed
                                constraintAxiom = createOWLObjectPropertyAssertionAxiom(factory,
                                    XSD_OWL.hasConstraintType, elementIndividual, XSD_OWL.VC_FIXED);
                            }

                            ontologyManager.applyChange(new AddAxiom(dataSourceSchemaOntology,
                                    constraintAxiom));

                            // add the constraint value literal to the element delcaration
                            String contstraintValue = xsElementDeclaration.getConstraintValue();
                            if (contstraintValue != null) {

                                ontologyManager.applyChange(new AddAxiom(dataSourceSchemaOntology,
                                        createOWLDataPropertyAssertionAxiom(factory, XSD_OWL.constraint,
                                            elementIndividual, contstraintValue)));
                            }

                        }

                    }
                }

            } catch (ParserConfigurationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (SAXException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return dataSourceSchemaOntology;
    }

}

package eu.iksproject.kres.semion.reengineer.xml;


import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.clerezza.rdf.core.access.WeightedTcProvider;
import org.apache.stanbol.ontologymanager.ontonet.api.KReSONManager;
import org.apache.stanbol.reengineer.base.DataSource;
import org.apache.stanbol.reengineer.base.util.SemionUriRefGenerator;
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
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
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
import org.xml.sax.SAXException;

import org.apache.stanbol.reengineer.xml.XSD_OWL;

public class XSDExtractor extends SemionUriRefGenerator {


	private WeightedTcProvider weightedTcProvider;
	
	private KReSONManager onManager;
	
	public final Logger log = LoggerFactory.getLogger(getClass());
	
	public XSDExtractor(WeightedTcProvider weightedTcProvider) {
		this.weightedTcProvider = weightedTcProvider;
	}
	
	public XSDExtractor(KReSONManager onManager) {
		this.onManager = onManager;
	}
	
	
	private void addComplexType(String schemaNS, OWLOntologyManager manager, OWLDataFactory factory, OWLOntology schemaOntology, IRI complexType, XSComplexTypeDecl xsComplexTypeDefinition){
		
		String name = xsComplexTypeDefinition.getName();
		if(name != null){
			OWLDataPropertyAssertionAxiom hasName = createOWLDataPropertyAssertionAxiom(factory, XSD_OWL.name, complexType, name);
			manager.applyChange(new AddAxiom(schemaOntology, hasName));
		}
		
		
		XSAttributeGroupDecl xsAttributeGroupDecl = xsComplexTypeDefinition.getAttrGrp();
		if(xsAttributeGroupDecl != null){
			String attrGroupName = xsAttributeGroupDecl.getName();
			
			IRI attrGroupIRI = IRI.create(schemaNS+attrGroupName);
			OWLClassAssertionAxiom attrGroup = createOWLClassAssertionAxiom(factory, XSD_OWL.AttributeGroup, attrGroupIRI);
			manager.applyChange(new AddAxiom(schemaOntology, attrGroup));
			
			XSObjectList xsObjectList = xsAttributeGroupDecl.getAttributeUses();
			for(int i=0, j=xsObjectList.getLength(); i<j; i++){
				XSAttributeUseImpl xsAttributeUseImpl = (XSAttributeUseImpl)xsObjectList.item(i);
				
				String attrName = xsAttributeUseImpl.getAttrDeclaration().getName();
				
				IRI attrResourceIRI = IRI.create(schemaNS+attrGroupName+"_"+attrName);
				
				OWLClassAssertionAxiom attrResource = createOWLClassAssertionAxiom(factory, XSD_OWL.Attribute, attrResourceIRI);
				manager.applyChange(new AddAxiom(schemaOntology, attrResource));
				
				manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory, XSD_OWL.name, attrResourceIRI, attrName)));
				manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory, XSD_OWL.required, attrResourceIRI, xsAttributeUseImpl.getRequired())));
				
				
				
				
				IRI simpleTypeIRI = IRI.create(schemaNS+xsAttributeUseImpl.getAttrDeclaration().getTypeDefinition().getName());
				OWLClassAssertionAxiom simpleType = createOWLClassAssertionAxiom(factory, XSD_OWL.SimpleType, simpleTypeIRI);
				manager.applyChange(new AddAxiom(schemaOntology, simpleType));
				
				
				//ADD SIMPLE TYPE DEFINITION TO THE RDF
				addSimpleType(schemaNS, manager, factory, schemaOntology, simpleTypeIRI, (XSSimpleTypeDecl)xsAttributeUseImpl.getAttrDeclaration().getTypeDefinition());
				
				manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.type, attrResourceIRI, simpleTypeIRI)));
				manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.hasAttributeUse, attrGroupIRI, attrResourceIRI)));
			}
		}
		
		XSObjectList xsObjectList = xsComplexTypeDefinition.getAnnotations();
		if(xsObjectList != null){
			for(int i=0, j=xsObjectList.getLength(); i<j; i++){
				
				XSAnnotationImpl xsAnnotationImpl = (XSAnnotationImpl)xsObjectList.item(i);
				
				String annotationString = xsAnnotationImpl.getAnnotationString();
				if(annotationString != null){
					
					
					IRI annotatioIRI = IRI.create(schemaNS+name+"_annotation_"+i);
					OWLClassAssertionAxiom annotation = createOWLClassAssertionAxiom(factory, XSD_OWL.Annotation, annotatioIRI);
					manager.applyChange(new AddAxiom(schemaOntology, annotation));
					
					log.info("DOCUMENTATION : "+xsAnnotationImpl);
					
					manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory, XSD_OWL.value, annotatioIRI, annotationString)));
					manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.hasAnnotation, complexType, annotatioIRI)));
				}
			}
		}
		
		short prohibitedSubstitution = xsComplexTypeDefinition.getProhibitedSubstitutions();
		
		//Derivation restriction
		if(prohibitedSubstitution == XSConstants.DERIVATION_RESTRICTION){
			//Prohibited restriction
			manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.hasProhibitedSubstitutions, complexType, XSD_OWL.PROHIBITED_RESTRICTION)));
		}
		else if(prohibitedSubstitution == XSConstants.DERIVATION_EXTENSION){
			//Prohibited extension
			manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.hasProhibitedSubstitutions, complexType, XSD_OWL.PROHIBITED_EXTENSION)));
		}
		else if(prohibitedSubstitution == XSConstants.DERIVATION_NONE){
			//Prohibited none
			manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.hasProhibitedSubstitutions, complexType, XSD_OWL.PROHIBITED_NONE)));
		}
		
		//Abstract
		boolean abstractProperty = xsComplexTypeDefinition.getAbstract();
		manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory, XSD_OWL.abstractProperty, complexType, abstractProperty)));
		
		//Final value
		short finalValue = xsComplexTypeDefinition.getFinal();
		if(finalValue == XSConstants.DERIVATION_EXTENSION){
			//Derivation extension
			manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.hasFinal, complexType, XSD_OWL.DERIVATION_EXTENSION)));
		}
		else if(finalValue == XSConstants.DERIVATION_RESTRICTION){
			//Derivation restriction
			manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.hasFinal, complexType, XSD_OWL.DERIVATION_RESTRICTION)));
		}
		else if(finalValue == XSConstants.DERIVATION_NONE){
			//Derivation none
			manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.hasFinal, complexType, XSD_OWL.DERIVATION_NONE)));
		}
		
		XSParticle xsParticle = xsComplexTypeDefinition.getParticle();
		
		short contentType = xsComplexTypeDefinition.getContentType();
		
		if(contentType == XSComplexTypeDefinition.CONTENTTYPE_EMPTY){
			log.info("CONTENTTYPE_EMPTY");
		}
		else if(contentType == XSComplexTypeDefinition.CONTENTTYPE_ELEMENT){
			log.info("CONTENTTYPE_ELEMENT");
			
		}
		else if(contentType == XSComplexTypeDefinition.CONTENTTYPE_MIXED){
			log.info("CONTENTTYPE_MIXED");
		}
		else if(contentType == XSComplexTypeDefinition.CONTENTTYPE_SIMPLE){
			log.info("CONTENTTYPE_SIMPLE");
		}
		
		
		
		XSObjectList objectList = xsComplexTypeDefinition.getAttributeUses();
		log.info("XSOBJECT SIZE: "+objectList.getLength());
		for(int i=0, j=objectList.getLength(); i<j; i++){
			
			
			XSAttributeUseImpl xsAttributeUseImpl = (XSAttributeUseImpl)objectList.item(i);
			
			String attrName = xsAttributeUseImpl.getAttrDeclaration().getName();
			
			IRI attrResourceIRI = IRI.create(schemaNS+name+"_"+attrName);
			OWLClassAssertionAxiom attrResource = createOWLClassAssertionAxiom(factory, XSD_OWL.Attribute, attrResourceIRI);
			manager.addAxiom(schemaOntology, attrResource);
			
			manager.addAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory, XSD_OWL.name, attrResourceIRI, attrName));
			manager.addAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory, XSD_OWL.required, attrResourceIRI, xsAttributeUseImpl.getRequired()));
			
			
			IRI simpleTypeIRI = IRI.create(schemaNS+xsAttributeUseImpl.getAttrDeclaration().getTypeDefinition().getName());
			OWLClassAssertionAxiom simpleType = createOWLClassAssertionAxiom(factory, XSD_OWL.SimpleType, simpleTypeIRI);
			manager.applyChange(new AddAxiom(schemaOntology, simpleType));
			
			
			//ADD SIMPLE TYPE DEFINITION TO THE RDF
			addSimpleType(schemaNS, manager, factory, schemaOntology, simpleTypeIRI, (XSSimpleTypeDecl)xsAttributeUseImpl.getAttrDeclaration().getTypeDefinition());
			
			manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.type, attrResourceIRI, simpleTypeIRI)));
			
			log.info("ATTRIBUTE USES REQUIRED "+xsAttributeUseImpl.getAttrDeclaration().getTypeDefinition().getName());
			
			manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.hasAttributeUse, complexType, attrResourceIRI)));
			
		}
		
		if(xsParticle != null){
			
			int maxOccurs = xsParticle.getMaxOccurs();
			int minOccurs = xsParticle.getMinOccurs();
			
			manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory, XSD_OWL.maxOccurs, complexType, maxOccurs)));
			manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory, XSD_OWL.minOccurs, complexType, minOccurs)));
			
			XSTerm xsTerm = xsParticle.getTerm();
			if(xsTerm instanceof XSModelGroupImpl){
				XSModelGroupImpl xsModelGroup = (XSModelGroupImpl)xsTerm;
				XSObjectList list = xsModelGroup.getParticles();
				
				IRI particleIRI = null; 
				OWLClassAssertionAxiom particle = null;
				
				short compositor = xsModelGroup.getCompositor();
				if(compositor == XSModelGroup.COMPOSITOR_ALL){
					particleIRI = IRI.create(schemaNS+name+"_all");
					particle = createOWLClassAssertionAxiom(factory, XSD_OWL.All, particleIRI);
				}
				else if(compositor == XSModelGroup.COMPOSITOR_CHOICE){
					particleIRI = IRI.create(schemaNS+name+"_choice");
					particle = createOWLClassAssertionAxiom(factory, XSD_OWL.Choice, particleIRI);
				}
				else{
					particleIRI = IRI.create(schemaNS+name+"_sequence");
					particle = createOWLClassAssertionAxiom(factory, XSD_OWL.Sequence, particleIRI);
				}
				
				manager.applyChange(new AddAxiom(schemaOntology, particle));
				
				
				
				if(particle != null){
				
					
					manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.hasCompositor, complexType, particleIRI)));
					
					for(int i=0, j=list.getLength(); i<j; i++){
						
						XSParticleDecl xsParticleDecl = (XSParticleDecl)list.item(i);
						
						XSTerm xsParticleTerm = xsParticleDecl.getTerm();
						if(xsParticleTerm instanceof XSElementDecl){
							XSElementDecl xsElementDecl = (XSElementDecl)xsParticleTerm;
							String particleName = xsParticleDecl.getTerm().getName();
							
							IRI elementParticleIRI = IRI.create(schemaNS+particleName);
							OWLClassAssertionAxiom elementParticle = createOWLClassAssertionAxiom(factory, XSD_OWL.Element, elementParticleIRI);
							manager.applyChange(new AddAxiom(schemaOntology, elementParticle));

							
							XSTypeDefinition xsTypeDefinition = xsElementDecl.getTypeDefinition();
							String type = schemaNS+xsTypeDefinition.getName();
							
							
							XSTypeDefinition baseTypeDefinition = xsTypeDefinition.getBaseType();
							short baseType = baseTypeDefinition.getTypeCategory();
							

							IRI typeResourceIRI = IRI.create(type);
							OWLClassAssertionAxiom typeResource;
							if(baseType == XSTypeDefinition.SIMPLE_TYPE){
								log.info("SIMPLE TYPE");
								typeResource = createOWLClassAssertionAxiom(factory, XSD_OWL.SimpleType, typeResourceIRI);
								
								addSimpleType(schemaNS, manager, factory, schemaOntology, typeResourceIRI, (XSSimpleTypeDecl) baseTypeDefinition);
							}
							else{
								log.info("COMPLEX TYPE");
								typeResource = createOWLClassAssertionAxiom(factory, XSD_OWL.ComplexType, typeResourceIRI);
								
								addComplexType(schemaNS, manager, factory, schemaOntology, typeResourceIRI, (XSComplexTypeDecl) baseTypeDefinition);
							}
							
							manager.applyChange(new AddAxiom(schemaOntology, typeResource));
							
							
							manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.type, elementParticleIRI, typeResourceIRI)));
							
							
							log.info("OBJ "+xsElementDecl.getTypeDefinition().getName());
						}
						
					}
				}
				
				log.info("COMPOSITOR : "+xsModelGroup.getCompositor());
				log.info("COMPOSITOR_SEQUENCE : "+XSModelGroup.COMPOSITOR_SEQUENCE);
				
				
			}
			log.info("TERM: "+xsTerm.getClass().getCanonicalName());
		}
		else{
			log.info("NO PARTICLE");
		}
	}
	
	private void addSimpleType(String schemaNS, OWLOntologyManager manager, OWLDataFactory factory, OWLOntology schemaOntology, IRI simpleType, XSSimpleTypeDecl xsSimpleTypeDefinition){
		
		//NAME
		String name = xsSimpleTypeDefinition.getName();
		
		log.info("NAME OF SIMPLE TYPE : "+name);
		
		//add name literal to the simple type declaration
		
		manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory, XSD_OWL.name, simpleType, name)));
		
		
		//FINAL
		short finalValue = xsSimpleTypeDefinition.getFinal();
		if(finalValue == XSConstants.DERIVATION_EXTENSION){
			//Derivation extension
			manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.name, simpleType, XSD_OWL.DERIVATION_EXTENSION)));
		}
		else if(finalValue == XSConstants.DERIVATION_RESTRICTION){
			//Derivation restriction
			manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.name, simpleType, XSD_OWL.DERIVATION_RESTRICTION)));
		}
		else if(finalValue == XSConstants.DERIVATION_NONE){
			//Derivation none
			manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.name, simpleType, XSD_OWL.DERIVATION_NONE)));
		}
		
		ObjectList objectList = xsSimpleTypeDefinition.getActualEnumeration();
		
		if(objectList != null && objectList.getLength()>0){
			
			IRI enumerationIRI = IRI.create(schemaNS+name+"_enumeration");
			OWLClassAssertionAxiom enumeration = createOWLClassAssertionAxiom(factory, XSD_OWL.Enumeration, enumerationIRI);
			manager.applyChange(new AddAxiom(schemaOntology, enumeration));
			
			
			//add value property to enumeration UriRef
			for(int i=0, j=objectList.getLength(); i<j; i++){
				manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory, XSD_OWL.value, simpleType, objectList.item(i).toString())));
			}
			
			//add triple asserting that a simple type has an enumeration
			manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.hasEnumeration, simpleType, enumerationIRI)));
		}

		try {
			//Whitepace
                        /* This line, sometimes, generates an exception when try to get simple type definition for white space.
                                However, even if there is the exception, the line returns the ZERO value, so in the catch block is perfomed the
                                option with ZERO value that is WS_PRESERVE.*/
			short whitespace = xsSimpleTypeDefinition.getWhitespace();
			if(whitespace == XSSimpleTypeDecl.WS_COLLAPSE){
				//Collapse
				manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.hasWhitespace, simpleType, XSD_OWL.COLLAPSE)));
			}
			else if(whitespace == XSSimpleTypeDecl.WS_PRESERVE){
				//Preserve
				manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.hasWhitespace, simpleType, XSD_OWL.PRESERVE)));
			}
			else if(whitespace == XSSimpleTypeDecl.WS_REPLACE){
				//Replace
				manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.hasWhitespace, simpleType, XSD_OWL.REPLACE)));
			}
			
			log.info("WHITESPACE : "+whitespace);
		} catch (DatatypeException e) {
			// TODO Auto-generated catch block
                        /*In case of exception is run the option that preserves the simple type.*/
                        manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.hasWhitespace, simpleType, XSD_OWL.PRESERVE)));
			log.warn("PROBLEM TO GET WHITE SPACE FROM SIMPLE TYPE DEFINITION", e);
		}
		
		
		//ADD BASE TYPE
		XSTypeDefinition baseTypeDefinition = xsSimpleTypeDefinition.getBaseType();
		String baseTypeNS = baseTypeDefinition.getNamespace();
		String baseTypeName = baseTypeDefinition.getName();
		
		//add base type
		manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.base, simpleType, IRI.create(baseTypeNS+"#"+baseTypeName))));
		
		//ADD MAX ECLUSIVE VALUE
		Object obj = xsSimpleTypeDefinition.getMaxExclusiveValue();
		if(obj != null){
			IRI maxExclusiveIRI = IRI.create(schemaNS+name+"_maxEclusive");
			OWLClassAssertionAxiom maxExclusive = createOWLClassAssertionAxiom(factory, XSD_OWL.MaxEclusive, maxExclusiveIRI);
			manager.applyChange(new AddAxiom(schemaOntology, maxExclusive));
			
			manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory, XSD_OWL.value, maxExclusiveIRI, obj.toString())));
		}
		
		
		//ADD MIN ECLUSIVE VALUE
		obj = xsSimpleTypeDefinition.getMinExclusiveValue();
		if(obj != null){
			IRI minExclusiveIRI = IRI.create(schemaNS+name+"_minEclusive");
			OWLClassAssertionAxiom minExclusive = createOWLClassAssertionAxiom(factory, XSD_OWL.MinEclusive, minExclusiveIRI);
			manager.applyChange(new AddAxiom(schemaOntology, minExclusive));
			
			manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory, XSD_OWL.value, minExclusiveIRI, obj.toString())));
		}
		
		
		//ADD MAX INCLUSIVE VALUE
		obj = xsSimpleTypeDefinition.getMaxInclusiveValue();
		if(obj != null){
			IRI maxInclusiveIRI = IRI.create(schemaNS+name+"_maxInclusive");
			OWLClassAssertionAxiom maxInclusive = createOWLClassAssertionAxiom(factory, XSD_OWL.MaxInclusive, maxInclusiveIRI);
			manager.applyChange(new AddAxiom(schemaOntology, maxInclusive));
			
			manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory, XSD_OWL.value, maxInclusiveIRI, obj.toString())));
		}
		
		
		//ADD MIN INCLUSIVE VALUE
		obj = xsSimpleTypeDefinition.getMinInclusiveValue();
		if(obj != null){
			IRI minInclusiveIRI = IRI.create(schemaNS+name+"_minInclusive");
			OWLClassAssertionAxiom minInclusive = createOWLClassAssertionAxiom(factory, XSD_OWL.MinInclusive, minInclusiveIRI);
			manager.applyChange(new AddAxiom(schemaOntology, minInclusive));
			
			manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory, XSD_OWL.value, minInclusiveIRI, obj.toString())));
		}
		
		//ADD PATTERNS
		StringList stringList = xsSimpleTypeDefinition.getLexicalPattern();
		if(stringList != null){
			for(int i=0, j=stringList.getLength(); i<j; i++){
				
				IRI patternIRI = IRI.create(schemaNS+name+"_pattern_"+i);
				OWLClassAssertionAxiom pattern = createOWLClassAssertionAxiom(factory, XSD_OWL.Pattern, patternIRI);
				manager.applyChange(new AddAxiom(schemaOntology, pattern));
				
				manager.applyChange(new AddAxiom(schemaOntology, createOWLDataPropertyAssertionAxiom(factory, XSD_OWL.value, patternIRI, stringList.item(i))));
				manager.applyChange(new AddAxiom(schemaOntology, createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.value, simpleType, patternIRI)));
				
			}
		}
		
		
		
		XSObjectListImpl xsObjectListImpl = xsSimpleTypeDefinition.patternAnnotations;
		if(xsObjectListImpl != null){
			for(int i=0, j=xsObjectListImpl.getLength(); i<j; i++){
				XSObject xsObject = xsObjectListImpl.item(i);
				log.info("PATTERN : "+xsObject.getClass().getCanonicalName());
			}
		}
		
		
	}
	
	

	
	public OWLOntology getOntologySchema(String graphNS, IRI outputIRI, DataSource dataSource) {
		
		
		if(!graphNS.endsWith("#")){
			graphNS += "#";
		}
		System.out.println("GET ONTOLOGY SCHEMA");
		OWLOntology dataSourceSchemaOntology = null;
		
		new DocumentTraversal() {
			
			@Override
			public TreeWalker createTreeWalker(Node arg0, int arg1, NodeFilter arg2,
					boolean arg3) throws DOMException {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public NodeIterator createNodeIterator(Node arg0, int arg1,
					NodeFilter arg2, boolean arg3) throws DOMException {
				// TODO Auto-generated method stub
				return null;
			}
		};
		
		PSVIDocumentImpl psviDocumentImpl = new PSVIDocumentImpl();
        XSSimpleTypeDecl m;
		if(dataSource != null){
			
			OWLOntologyManager ontologyManager = onManager.getOwlCacheManager();		
			OWLDataFactory factory = onManager.getOwlFactory();
			
			System.out.println("XSD output IRI : "+outputIRI);
			
			if(outputIRI != null){
				try {
					dataSourceSchemaOntology = ontologyManager.createOntology(outputIRI);
				} catch (OWLOntologyCreationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else{
				try {
					dataSourceSchemaOntology = ontologyManager.createOntology();
				} catch (OWLOntologyCreationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			if(dataSourceSchemaOntology != null){
			
				
				
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				
				dbf.setNamespaceAware(true);
		
				String id    = "http://apache.org/xml/properties/dom/document-class-name";
				Object value = "org.apache.xerces.dom.PSVIDocumentImpl";
				try {
				    dbf.setAttribute(id, value);
				    dbf.setNamespaceAware(true);
				    dbf.setValidating(true);
				    dbf.setAttribute("http://apache.org/xml/features/validation/schema", 
				        Boolean.TRUE);
				} 
				catch (IllegalArgumentException e) {
				    System.err.println("could not set parser property");
				}
				
				DocumentBuilder db;
				Document document;
				
				try {
					db = dbf.newDocumentBuilder();
					
					document = db.parse((InputStream) dataSource.getDataSource());
					Element root = document.getDocumentElement();
					
					System.out.println("ROOT IS : "+root.getNodeName());
					
					ElementPSVI rootPsvi = (ElementPSVI)root;
					
					XSModelImpl xsModel = (XSModelImpl)rootPsvi.getSchemaInformation();
					
					
					System.out.println("SCHEMA MODEL  : "+xsModel.getClass().getCanonicalName());
					
					XSNamedMap xsNamedMap = xsModel.getComponents(XSConstants.ELEMENT_DECLARATION);
					for(int i=0, j=xsNamedMap.getLength(); i<j; i++){
						XSObject xsObject = xsNamedMap.item(i);
						if(xsObject instanceof XSElementDeclaration){
							
							
							XSElementDeclaration xsElementDeclaration = (XSElementDeclaration)xsObject;
							
							String name = xsElementDeclaration.getName();
							if(name != null && !name.equals("")){
								
								IRI elementIndividual = IRI.create(graphNS+name);
								
								OWLClassAssertionAxiom element = createOWLClassAssertionAxiom(factory, XSD_OWL.Element, elementIndividual);							
								ontologyManager.applyChange(new AddAxiom(dataSourceSchemaOntology, element));
								
								OWLDataPropertyAssertionAxiom data = createOWLDataPropertyAssertionAxiom(factory, XSD_OWL.name, elementIndividual, name);
								ontologyManager.applyChange(new AddAxiom(dataSourceSchemaOntology, data));
								
								boolean boolValue = xsElementDeclaration.getAbstract();							
								data = createOWLDataPropertyAssertionAxiom(factory, XSD_OWL.abstractProperty, elementIndividual, boolValue);
								ontologyManager.applyChange(new AddAxiom(dataSourceSchemaOntology, data));
								
								XSTypeDefinition xsTypeDefinition = xsElementDeclaration.getTypeDefinition();
								String type = graphNS+xsTypeDefinition.getName();
								
								
								XSTypeDefinition baseTypeDefinition = xsTypeDefinition.getBaseType();
								short baseType = baseTypeDefinition.getTypeCategory();
								
								
								OWLClassAssertionAxiom typeResource;
								log.info("SIMPLE TYPE PRINT "+XSTypeDefinition.SIMPLE_TYPE);
								log.info("COMPLEX TYPE PRINT "+XSTypeDefinition.COMPLEX_TYPE);
								
								IRI typeIRI = IRI.create(type);
								
								if(baseType == XSTypeDefinition.SIMPLE_TYPE){
									log.info("SIMPLE TYPE");
									typeResource = createOWLClassAssertionAxiom(factory, XSD_OWL.SimpleType, typeIRI);
                                                                        addSimpleType(graphNS, ontologyManager, factory, dataSourceSchemaOntology, typeIRI, (XSSimpleTypeDecl) xsTypeDefinition);
                                                                       
								}
								else {
									System.out.println("COMPLEX TYPE");
									typeResource = createOWLClassAssertionAxiom(factory, XSD_OWL.ComplexType, typeIRI);
									
									addComplexType(graphNS, ontologyManager, factory, dataSourceSchemaOntology, typeIRI, (XSComplexTypeDecl) xsTypeDefinition);
								}
								
								ontologyManager.applyChange(new AddAxiom(dataSourceSchemaOntology, typeResource));
								
								
								
								//add the type property to the element declaration
								
								System.out.println("---- graph NS : "+graphNS);
								System.out.println("---- type IRI : "+typeIRI.toString());
								OWLObjectPropertyAssertionAxiom hasType = createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.type, elementIndividual, typeIRI);
								ontologyManager.applyChange(new AddAxiom(dataSourceSchemaOntology, hasType));
								
								
								
								//add the scope property to the element declaration
								short scope = xsElementDeclaration.getScope();
							
								OWLObjectPropertyAssertionAxiom scopeAxiom;
								if(scope == XSConstants.SCOPE_ABSENT){
									//Scope absent
									scopeAxiom = createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.hasScope, elementIndividual, XSD_OWL.ScopeAbsent);
								}
								else if(scope == XSConstants.SCOPE_LOCAL){
									scopeAxiom = createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.hasScope, elementIndividual, XSD_OWL.ScopeLocal);
								}
								else {
									scopeAxiom = createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.hasScope, elementIndividual, XSD_OWL.ScopeGlobal);
								}
								
								ontologyManager.applyChange(new AddAxiom(dataSourceSchemaOntology, scopeAxiom));
								
								//add the constraint type property to the element declaration
								short constraingType = xsElementDeclaration.getConstraintType();
								OWLObjectPropertyAssertionAxiom constraintAxiom;
								if(constraingType == XSConstants.VC_NONE){
									//Value constraint none
									constraintAxiom = createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.hasConstraintType, elementIndividual, XSD_OWL.VC_NONE);
								}
								else if(constraingType == XSConstants.VC_DEFAULT){
									constraintAxiom = createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.hasConstraintType, elementIndividual, XSD_OWL.VC_DEFAULT);
								}
								else{
									//Value constraint fixed
									constraintAxiom = createOWLObjectPropertyAssertionAxiom(factory, XSD_OWL.hasConstraintType, elementIndividual, XSD_OWL.VC_FIXED);
								}
								
								ontologyManager.applyChange(new AddAxiom(dataSourceSchemaOntology, constraintAxiom));
								
								
								//add the constraint value literal to the element delcaration
								String contstraintValue = xsElementDeclaration.getConstraintValue();
								if(contstraintValue != null){
									
									ontologyManager.applyChange(new AddAxiom(dataSourceSchemaOntology, createOWLDataPropertyAssertionAxiom(factory, XSD_OWL.constraint, elementIndividual, contstraintValue)));
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
		}
		
		return dataSourceSchemaOntology;
	}
	
	 
	
}

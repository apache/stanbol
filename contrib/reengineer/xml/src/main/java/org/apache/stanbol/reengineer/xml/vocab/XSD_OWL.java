package org.apache.stanbol.reengineer.xml.vocab;

import org.semanticweb.owlapi.model.IRI;

public class XSD_OWL {

	/** <p>The namespace of the vocabulary as a string</p> */
    public static final String NS = "http://www.ontologydesignpatterns.org/ont/iks/oxsd.owl#";
    
    /** <p>The namespace of the vocabulary as a string</p>
     *  @see #NS */
    public static String getURI() {return NS;}
    
    /** <p>The namespace of the vocabulary as a resource</p> */
    //public static final IRI NAMESPACE = m_model.createIRI( NS );
    
    public static final IRI Annotation = IRI.create(NS + "Annotation" );
    
    public static final IRI Any = IRI.create( NS + "Any" );
    
    public static final IRI AnyAttribute = IRI.create( NS + "AnyAttribute" );
    
    public static final IRI Appinfo = IRI.create( NS + "Appinfo" );
    
    public static final IRI Attribute = IRI.create( NS + "Attribute" );
    
    public static final IRI AttributeGroup = IRI.create( NS + "AttributeGroup" );
    
    public static final IRI Choice = IRI.create( NS + "Choice" );
    
    public static final IRI ComplexContent = IRI.create( NS + "ComplexContent" );
    
    public static final IRI ComplexType = IRI.create( NS + "ComplexType" );
    
    public static final IRI Documentation = IRI.create( NS + "Documentation" );
    
    public static final IRI Element = IRI.create( NS + "Element" );
    
    public static final IRI Enumeration = IRI.create( NS + "Enumeration" );
    
    public static final IRI Extension = IRI.create( NS + "Extension" );
    
    public static final IRI Field = IRI.create( NS + "Field" );
    
    public static final IRI FractionDigits = IRI.create( NS + "FractionDigits" );
    
    public static final IRI Group = IRI.create( NS + "Group" );
    
    public static final IRI HasFacet = IRI.create( NS + "HasFacet" );
    
    public static final IRI HasFacetAndPropertyhasProperty = IRI.create( NS + "HasFacetAndPropertyhasProperty" );
    
    public static final IRI Import = IRI.create( NS + "Import" );
    
    public static final IRI Key = IRI.create( NS + "Key" );
    
    public static final IRI List = IRI.create( NS + "List" );
    
    public static final IRI MaxEclusive = IRI.create( NS + "MaxEclusive" );
    
    public static final IRI MinEclusive = IRI.create( NS + "MinEclusive" );
    
    public static final IRI MaxInclusive = IRI.create( NS + "MaxInclusive" );
    
    public static final IRI MinInclusive = IRI.create( NS + "MinInclusive" );
    
    public static final IRI MinLength = IRI.create( NS + "MinLength" );
    
    public static final IRI Notation = IRI.create( NS + "Notation" );
    
    public static final IRI Pattern = IRI.create( NS + "Pattern" );
    
    public static final IRI All = IRI.create( NS + "All" );
    
    public static final IRI Restriction = IRI.create( NS + "Restriction" );
    
    public static final IRI Schema = IRI.create( NS + "Schema" );
    
    public static final IRI Selector = IRI.create( NS + "Selector" );
    
    public static final IRI Sequence = IRI.create( NS + "Sequence" );
    
    public static final IRI SimpleType = IRI.create( NS + "SimpleType" );
    
    public static final IRI Union = IRI.create( NS + "Union" );
    
    public static final IRI WhiteSpace = IRI.create( NS + "WhiteSpace" );
    
    public static final IRI ScopeAbsent = IRI.create( NS + "Absent" );
    
    public static final IRI ScopeLocal = IRI.create( NS + "Local" );
    
    public static final IRI ScopeGlobal = IRI.create( NS + "Global" );
    
    public static final IRI VC_NONE = IRI.create( NS + "VC_NONE" );
    
    public static final IRI VC_DEFAULT = IRI.create( NS + "VC_DEFAULT" );
    
    public static final IRI VC_FIXED = IRI.create( NS + "VC_FIXED" );
    
    public static final IRI DERIVATION_EXTENSION = IRI.create( NS + "DERIVATION_EXTENSION" );
    
    public static final IRI DERIVATION_RESTRICTION = IRI.create( NS + "DERIVATION_RESTRICTION" );
    
    public static final IRI DERIVATION_NONE = IRI.create( NS + "DERIVATION_NONE" );
    
    public static final IRI PROHIBITED_EXTENSION = IRI.create( NS + "PROHIBITED_EXTENSION" );
    
    public static final IRI PROHIBITED_RESTRICTION = IRI.create( NS + "PROHIBITED_RESTRICTION" );
    
    public static final IRI PROHIBITED_NONE = IRI.create( NS + "PROHIBITED_NONE" );
    
    public static final IRI COLLAPSE = IRI.create( NS + "COLLAPSE" );
    
    public static final IRI PRESERVE = IRI.create( NS + "PRESERVE" );
    
    public static final IRI REPLACE = IRI.create( NS + "REPLACE" );
    
    public static final IRI abstractProperty = IRI.create( NS + "abstract" );
    
    public static final IRI name = IRI.create( NS + "name" );
    
    public static final IRI type = IRI.create( NS + "type" );
    
    public static final IRI hasScope = IRI.create( NS + "hasScope" );
    
    public static final IRI hasConstraintType = IRI.create( NS + "hasConstraintType" );
    
    public static final IRI hasFinal = IRI.create( NS + "hasFinal" );
    
    public static final IRI constraint = IRI.create( NS + "constraint" );
    
    public static final IRI maxOccurs = IRI.create( NS + "maxOccurs" );
    
    public static final IRI minOccurs = IRI.create( NS + "minOccurs" );
    
    public static final IRI hasProhibitedSubstitutions = IRI.create( NS + "hasProhibitedSubstitutions" );
    
    public static final IRI required = IRI.create( NS + "required" );
    
    public static final IRI hasParticle = IRI.create( NS + "hasParticle" );
    
    public static final IRI hasCompositor = IRI.create( NS + "hasCompositor" );
    
    public static final IRI value = IRI.create( NS + "value" );
    
    public static final IRI hasEnumeration = IRI.create( NS + "hasEnumeration" );
    
    public static final IRI hasWhitespace = IRI.create( NS + "hasWhitespace" );
    
    public static final IRI base = IRI.create( NS + "base" );
    
    public static final IRI hasAnnotation = IRI.create( NS + "hasAnnotation" );
    
    public static final IRI hasPattern = IRI.create( NS + "hasPattern" );
    
    public static final IRI hasAttributeUse = IRI.create( NS + "hasAttributeUse" );
    
    public static final IRI child = IRI.create( "http://www.topbraid.org/2007/05/composite.owl#child" );
    
    public static final IRI parent = IRI.create( "http://www.topbraid.org/2007/05/composite.owl#parent" );
	
}

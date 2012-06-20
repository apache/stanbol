package org.apache.stanbol.reengineer.xml.vocab;

/* CVS $Id: XSD.java 1085994 2011-03-27 17:30:29Z alexdma $ */
 
import org.apache.clerezza.rdf.core.UriRef;
 
/**
 * Vocabulary definitions from http://ontologydesignpatterns.org/ont/iks/xsd.owl 
 * @author andrea.nuzzolese
 */
public class XSD {
    
	/** <p>The namespace of the vocabulary as a string</p> */
    public static final String NS = "http://ontologydesignpatterns.org/ont/iks/xsd.owl#";
    
    /** <p>The namespace of the vocabulary as a string</p>
     *  @see #NS */
    public static String getURI() {return NS;}
    
    /** <p>The namespace of the vocabulary as a resource</p> */
    //public static final UriRef NAMESPACE = m_model.createUriRef( NS );
    
    public static final UriRef Annotation = new UriRef(NS + "Annotation" );
    
    public static final UriRef Any = new UriRef( NS + "Any" );
    
    public static final UriRef AnyAttribute = new UriRef( NS + "AnyAttribute" );
    
    public static final UriRef Appinfo = new UriRef( NS + "Appinfo" );
    
    public static final UriRef Attribute = new UriRef( NS + "Attribute" );
    
    public static final UriRef AttributeGroup = new UriRef( NS + "AttributeGroup" );
    
    public static final UriRef Choice = new UriRef( NS + "Choice" );
    
    public static final UriRef ComplexContent = new UriRef( NS + "ComplexContent" );
    
    public static final UriRef ComplexType = new UriRef( NS + "ComplexType" );
    
    public static final UriRef Documentation = new UriRef( NS + "Documentation" );
    
    public static final UriRef Element = new UriRef( NS + "Element" );
    
    public static final UriRef Enumeration = new UriRef( NS + "Enumeration" );
    
    public static final UriRef Extension = new UriRef( NS + "Extension" );
    
    public static final UriRef Field = new UriRef( NS + "Field" );
    
    public static final UriRef FractionDigits = new UriRef( NS + "FractionDigits" );
    
    public static final UriRef Group = new UriRef( NS + "Group" );
    
    public static final UriRef HasFacet = new UriRef( NS + "HasFacet" );
    
    public static final UriRef HasFacetAndPropertyhasProperty = new UriRef( NS + "HasFacetAndPropertyhasProperty" );
    
    public static final UriRef Import = new UriRef( NS + "Import" );
    
    public static final UriRef Key = new UriRef( NS + "Key" );
    
    public static final UriRef List = new UriRef( NS + "List" );
    
    public static final UriRef MaxEclusive = new UriRef( NS + "MaxEclusive" );
    
    public static final UriRef MinEclusive = new UriRef( NS + "MinEclusive" );
    
    public static final UriRef MaxInclusive = new UriRef( NS + "MaxInclusive" );
    
    public static final UriRef MinInclusive = new UriRef( NS + "MinInclusive" );
    
    public static final UriRef MinLength = new UriRef( NS + "MinLength" );
    
    public static final UriRef Notation = new UriRef( NS + "Notation" );
    
    public static final UriRef Pattern = new UriRef( NS + "Pattern" );
    
    public static final UriRef All = new UriRef( NS + "All" );
    
    public static final UriRef Restriction = new UriRef( NS + "Restriction" );
    
    public static final UriRef Schema = new UriRef( NS + "Schema" );
    
    public static final UriRef Selector = new UriRef( NS + "Selector" );
    
    public static final UriRef Sequence = new UriRef( NS + "Sequence" );
    
    public static final UriRef SimpleType = new UriRef( NS + "SimpleType" );
    
    public static final UriRef Union = new UriRef( NS + "Union" );
    
    public static final UriRef WhiteSpace = new UriRef( NS + "WhiteSpace" );
    
    public static final UriRef ScopeAbsent = new UriRef( NS + "Absent" );
    
    public static final UriRef ScopeLocal = new UriRef( NS + "Local" );
    
    public static final UriRef ScopeGlobal = new UriRef( NS + "Global" );
    
    public static final UriRef VC_NONE = new UriRef( NS + "VC_NONE" );
    
    public static final UriRef VC_DEFAULT = new UriRef( NS + "VC_DEFAULT" );
    
    public static final UriRef VC_FIXED = new UriRef( NS + "VC_FIXED" );
    
    public static final UriRef DERIVATION_EXTENSION = new UriRef( NS + "DERIVATION_EXTENSION" );
    
    public static final UriRef DERIVATION_RESTRICTION = new UriRef( NS + "DERIVATION_RESTRICTION" );
    
    public static final UriRef DERIVATION_NONE = new UriRef( NS + "DERIVATION_NONE" );
    
    public static final UriRef PROHIBITED_EXTENSION = new UriRef( NS + "PROHIBITED_EXTENSION" );
    
    public static final UriRef PROHIBITED_RESTRICTION = new UriRef( NS + "PROHIBITED_RESTRICTION" );
    
    public static final UriRef PROHIBITED_NONE = new UriRef( NS + "PROHIBITED_NONE" );
    
    public static final UriRef COLLAPSE = new UriRef( NS + "COLLAPSE" );
    
    public static final UriRef PRESERVE = new UriRef( NS + "PRESERVE" );
    
    public static final UriRef REPLACE = new UriRef( NS + "REPLACE" );
    
    public static final UriRef abstractProperty = new UriRef( NS + "abstract" );
    
    public static final UriRef name = new UriRef( NS + "name" );
    
    public static final UriRef type = new UriRef( NS + "type" );
    
    public static final UriRef hasScope = new UriRef( NS + "hasScope" );
    
    public static final UriRef hasConstraintType = new UriRef( NS + "hasConstraintType" );
    
    public static final UriRef hasFinal = new UriRef( NS + "hasFinal" );
    
    public static final UriRef constraint = new UriRef( NS + "constraint" );
    
    public static final UriRef maxOccurs = new UriRef( NS + "maxOccurs" );
    
    public static final UriRef minOccurs = new UriRef( NS + "minOccurs" );
    
    public static final UriRef hasProhibitedSubstitutions = new UriRef( NS + "hasProhibitedSubstitutions" );
    
    public static final UriRef required = new UriRef( NS + "required" );
    
    public static final UriRef hasParticle = new UriRef( NS + "hasParticle" );
    
    public static final UriRef hasCompositor = new UriRef( NS + "hasCompositor" );
    
    public static final UriRef value = new UriRef( NS + "value" );
    
    public static final UriRef hasEnumeration = new UriRef( NS + "hasEnumeration" );
    
    public static final UriRef hasWhitespace = new UriRef( NS + "hasWhitespace" );
    
    public static final UriRef base = new UriRef( NS + "base" );
    
    public static final UriRef hasAnnotation = new UriRef( NS + "hasAnnotation" );
    
    public static final UriRef hasPattern = new UriRef( NS + "hasPattern" );
    
    public static final UriRef hasAttributeUse = new UriRef( NS + "hasAttributeUse" );
    
    public static final UriRef child = new UriRef( "http://www.topbraid.org/2007/05/composite.owl#child" );
    
    public static final UriRef parent = new UriRef( "http://www.topbraid.org/2007/05/composite.owl#parent" );
    
}

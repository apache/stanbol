package org.apache.stanbol.reengineer.xml.vocab;

/* CVS $Id: XML.java 1085994 2011-03-27 17:30:29Z alexdma $ */
 
import org.apache.clerezza.rdf.core.UriRef;
 
/**
 * Vocabulary definitions from http://ontologydesignpatterns.org/ont/iks/xml.owl 
 * @author andrea.nuzzolese 
 */
public class XML {
    
    /** <p>The namespace of the vocabulary as a string</p> */
    public static final String NS = "http://ontologydesignpatterns.org/ont/iks/xml.owl#";
    
    /** <p>The namespace of the vocabulary as a string</p> */
    public static final String URI = "http://ontologydesignpatterns.org/ont/iks/xml.owl";
    
    /** <p>The namespace of the vocabulary as a string</p>
     *  @see #NS */
    
    public static final UriRef SpecialAttrs = new UriRef( NS+"SpecialAttrs" );
    
    public static final UriRef XMLElement = new UriRef( NS+"XMLElement" );
    
    public static final UriRef XMLAttribute = new UriRef( NS+"XMLAttribute" );
    
    public static final UriRef Node = new UriRef( NS+"Node" );
    
    public static final UriRef hasSpecialAttrs = new UriRef( NS+"hasSpecialAttrs" );
    
    public static final UriRef isSpecialAttrsOf = new UriRef( NS+"isSpecialAttrsOf" );
    
    public static final UriRef hasXMLAttribute = new UriRef( NS+"hasXMLAttribute" );
    
    public static final UriRef isXMLAttributeOf = new UriRef( NS+"isXMLAttributeOf" );
    
    public static final UriRef nodeName = new UriRef( NS+"nodeName" );
    
    public static final UriRef nodeValue = new UriRef( NS+"nodeValue" );
    
    public static final UriRef hasElementDeclaration = new UriRef( NS+"hasElementDeclaration" );
    
    public static final UriRef isElementDeclarationOf = new UriRef( NS+"isElementDeclarationOf" );
    
    public static final UriRef hasAttributeDeclaration = new UriRef( NS+"hasAttributeDeclaration" );
    
    public static final UriRef isAttributetDeclarationOf = new UriRef( NS+"isAttributeDeclarationOf" );
    
    public static final UriRef textContent = new UriRef( NS+"textContent" );
    
}

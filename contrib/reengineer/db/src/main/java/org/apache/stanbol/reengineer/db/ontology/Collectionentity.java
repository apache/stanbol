package org.apache.stanbol.reengineer.db.ontology;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class Collectionentity {
	 /** <p>The RDF model that holds the vocabulary terms</p> */
    private static Model m_model = ModelFactory.createDefaultModel();
    
    /** <p>The namespace of the vocabulary as a string</p> */
    public static final String NS = "http://www.ontologydesignpatterns.org/cp/owl/collectionentity.owl#";
    
    /** <p>The namespace of the vocabulary as a string</p>
     *  @see #NS */
    public static String getURI() {return NS;}
    
    /** <p>The namespace of the vocabulary as a resource</p> */
    public static final Resource NAMESPACE = m_model.createResource( NS );
    
    public static final Property hasMember = m_model.createProperty( "http://www.ontologydesignpatterns.org/cp/owl/collectionentity.owl#hasMember" );
    
    public static final Property isMemberOf = m_model.createProperty( "http://www.ontologydesignpatterns.org/cp/owl/collectionentity.owl#isMemberOf" );
    
    /** <p>Any physical, social, or mental object, or a substance</p> */
    public static final Resource Collection = m_model.createResource( "http://www.ontologydesignpatterns.org/cp/owl/collectionentity.owl#Collection" );
}

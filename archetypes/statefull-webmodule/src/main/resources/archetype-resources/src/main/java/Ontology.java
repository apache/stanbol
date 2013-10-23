#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import org.apache.clerezza.rdf.core.UriRef;


/**
 * Ideally this should be a dereferenceable ontology on the web. Given such 
 * an ontology a class of constant (similar to this) can be generated with
 * the org.apache.clerezza:maven-ontologies-plugin
 */
public class Ontology {
    /**
     * Resources of this type can be dereferenced and will return a description
     * of the resource of which the IRI is specified in the "iri" query parameter.
     * 
     */
    public static final UriRef ResourceResolver = new UriRef("http://example.org/service-description${symbol_pound}ResourceResolver");
    
    /**
     * Point to the resource resolved by the subject.
     */
    public static final UriRef describes = new UriRef("http://example.org/service-description${symbol_pound}describes");
    
    /**
     * The description of a Request in the log.
     */
    public static final UriRef LoggedRequest = new UriRef("http://example.org/service-description${symbol_pound}LoggedRequest");
    
    /**
     * The User Agent performing the requested described by the subject.
     */
    public static final UriRef userAgent = new UriRef("http://example.org/service-description${symbol_pound}userAgent");
    
    /**
     * The Entity of which a description was requested in the request
     * described by the subject.
     */
    public static final UriRef requestedEntity = new UriRef("http://example.org/service-description${symbol_pound}requestedEntity");
}

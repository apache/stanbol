package org.apache.stanbol.cmsadapter.servicesapi.mapping;

import org.apache.clerezza.rdf.core.MGraph;

/**
 * Goal of this interface is to provide a uniform mechanism to store RDF data to JCR or CMIS repositories
 * based on cms vocabulary annotations on top of the raw RDF.
 * 
 * @author suat
 * 
 */
public interface RDFMapper {

    /**
     * This method stores the data passed within an {@link MGraph} to repository according
     * "CMS vocabulary annotations".
     * 
     * In the example RDF below, it is assumed that a CMS Object will be created under the path <b>/rootPath</b>
     * (specified as a parameter), having name <b>TomHanks</b>. It will also have a child object named
     * <b>son</b> and a property named <b>hasHomepage</b>. Please note that this design can be implemented as
     * far as the API to access content repository e.g JCR, CMIS allows. As a result the updates on content
     * repository may not be strictly same as the definition above.
     * <p>
     * You can see an example annotated RDF below. Bold assertion shows the CMS Vocabulary annotations:
     * 
     * <pre>
     * &lt;rdf:RDF
     *  xml:base="http://www.example.org#"
     *  xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
     *  xmlns:foaf="http://xmlns.com/foaf/0.1/"
     *  xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
     *  xmlns:cms="http://org.apache.stanbol/cms/"><br>
     * &lt;rdf:Description rdf:about="#TomHanks">
     *  &lt;rdf:type rdf:resource="http://xmlns.com/foaf/0.1/Person"/>
     *  &lt;foaf:givenname rdf:datatype="http://www.w3.org/2001/XMLSchema#string">Tom&lt;/foaf:givenname>
     *  &lt;foaf:homepage rdf:datatype="http://www.w3.org/2001/XMLSchema#string">http://www.tomhanks-online.com&lt;/foaf:homepage>
     *  &lt;rdfs:comment rdf:datatype="http://www.w3.org/2001/XMLSchema#string">Famous actor&lt;/rdfs:comment>
     *  <b>&lt;rdf:type rdf:resource="http://org.apache.stanbol/cms/CMSObject"/>
     *  &lt;cms:name rdf:datatype="http://www.w3.org/2001/XMLSchema#string">TomHanks&lt;/cms:name>
     *  &lt;cms:hasProperty rdf:resource="#homepageProperty"/></b>
     * &lt;/rdf:Description>
     * 
     * &lt;rdf:Description rdf:about="#ColinHanks">
     *  &lt;rdf:type rdf:resource="http://xmlns.com/foaf/0.1/Person"/>
     *  &lt;foaf:givenname rdf:datatype="http://www.w3.org/2001/XMLSchema#string">Colin&lt;/foaf:givenname>
     *  &lt;foaf:homepage rdf:datatype="http://www.w3.org/2001/XMLSchema#string">http://colin-hanks.net/&lt;/foaf:homepage>
     *  &lt;rdfs:comment rdf:datatype="http://www.w3.org/2001/XMLSchema#string">Son of Tom Hanks&lt;/rdfs:comment>
     *  <b>&lt;rdf:type rdf:resource="http://org.apache.stanbol/cms/CMSObject"/>
     *  &lt;cms:parentRef rdf:resource="#TomHanks"/>
     *  &lt;cms:name rdf:datatype="http://www.w3.org/2001/XMLSchema#string">son&lt;/cms:name>
     *  &lt;cms:hasProperty rdf:resource="#homepageProperty"/></b>
     * &lt;/rdf:Description>
     * 
     * <b>&lt;rdf:Description rdf:about="#homepageProperty">
     *  &lt;cms:propertyName rdf:datatype="http://www.w3.org/2001/XMLSchema#string">hasHomepage&lt;/cms:propertyName>
     *  &lt;cms:propertyURI rdf:resource="http://xmlns.com/foaf/0.1/homepage"/>
     * &lt;/rdf:Description></b>
     * &lt;/rdf:RDF>
     * </pre>
     * 
     * @param session
     *            This is a session object which is used to interact with JCR or CMIS repositories
     * @param rootPath
     *            Content repository path in which root objects (e.g object represented by the URI
     *            <b>#TomHanks</b> in the above example) will be created
     * @param annotatedGraph
     *            This {@link MGraph} object is an enhanced version of raw RDF data with "CMS vocabulary"
     *            annotations according to {@link RDFBridge}s.
     * @throws RDFBridgeException
     */
    void storeRDFinRepository(Object session, String rootPath, MGraph annotatedGraph) throws RDFBridgeException;

}

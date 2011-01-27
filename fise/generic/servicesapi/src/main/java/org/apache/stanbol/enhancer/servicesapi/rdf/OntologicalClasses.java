package org.apache.stanbol.enhancer.servicesapi.rdf;

import org.apache.clerezza.rdf.core.UriRef;

/**
 * Common entity types, a.k.a. ontological classes to be used as valuation of
 * the rdf:type ({@link Properties#RDF_TYPE}) property in the metadata graph of
 * content items.
 *
 * Copy and paste the URLs of the classes in a browser to get the definition of
 * the class.
 *
 * @author ogrisel@nuxeo.com
 */
public class OntologicalClasses {

    public static final UriRef DBPEDIA_PERSON = new UriRef(
            NamespaceEnum.dbpedia_ont+"Person");

    public static final UriRef DBPEDIA_PLACE = new UriRef(
            NamespaceEnum.dbpedia_ont+"Place");

    public static final UriRef DBPEDIA_ORGANISATION = new UriRef(
            NamespaceEnum.dbpedia_ont+"Organisation");

    private OntologicalClasses() {
    }

}

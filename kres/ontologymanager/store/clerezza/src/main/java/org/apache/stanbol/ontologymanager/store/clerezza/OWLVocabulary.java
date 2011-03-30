package org.apache.stanbol.ontologymanager.store.clerezza;

import org.apache.clerezza.rdf.core.UriRef;

public class OWLVocabulary {

    private static final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private static final String OWL = "http://www.w3.org/2002/07/owl#";

    public static final UriRef RDF_TYPE = new UriRef(RDF + "type");
    public static final UriRef OWL_CLASS = new UriRef(OWL + "Class");
    public static final UriRef OWL_DATATYPE_PROPERTY = new UriRef(OWL + "DatatypeProperty");
    public static final UriRef OWL_OBJECT_PROPERTY = new UriRef(OWL + "ObjectProperty");
    public static final UriRef OWL_INDIVIDUAL = new UriRef(OWL + "Individual");
}

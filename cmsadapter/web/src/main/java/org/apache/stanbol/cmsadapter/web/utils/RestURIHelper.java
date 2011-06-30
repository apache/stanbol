package org.apache.stanbol.cmsadapter.web.utils;

public class RestURIHelper {
	private static final String ONTOLOGY = "ontology";
	private static final String CLASSES = "classes";
	private static final String INDIVIDUALS = "individuals";
	private static final String DATATYPE_PROPERTIES = "datatypeProperties";
	private static final String OBJECT_PROPERTIES = "objectProperties";

	public static void main(String[] args) {
		String targetStr = "http://jcr_test/NClassO41_1";
		System.out.println(targetStr.substring(0, targetStr.lastIndexOf('#')));
	}

	public static String getOntologyHref(String ontologyURI) {
		return "/" + ONTOLOGY + "/" + ontologyURI;
	}

	public static String getClassHref(String ontologyURI, String classURI) {
		return getOntologyHref(ontologyURI) + "/" + CLASSES + "/"
				+ expelLastSharp(classURI);
	}

	public static String getObjectPropertyHref(String ontologyURI,
			String objectPropertyURI) {
		return getOntologyHref(ontologyURI) + "/" + OBJECT_PROPERTIES + "/"
				+ expelLastSharp(objectPropertyURI);
	}

	public static String getDatatypePropertyHref(String ontologyURI,
			String datatypePropertyURI) {
		return getOntologyHref(ontologyURI) + "/" + DATATYPE_PROPERTIES + "/"
				+ expelLastSharp(datatypePropertyURI);
	}

	public static String getIndividualHref(String ontologyURI,
			String individualURI) {
		return getOntologyHref(ontologyURI) + "/" + INDIVIDUALS + "/"
				+ expelLastSharp(individualURI);
	}

	private static String expelLastSharp(String targetStr) {
		return targetStr.substring(0, targetStr.lastIndexOf('#')) + "/"
				+ targetStr.substring(targetStr.lastIndexOf('#') + 1);
	}
}

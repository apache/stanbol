package org.apache.stanbol.ontologymanager.ontonet.impl.util;

import org.semanticweb.owlapi.model.IRI;

public class StringUtils {

	public static IRI stripIRITerminator(IRI iri) {
		if (iri == null)
			return null;
		return IRI.create(stripIRITerminator(iri.toString()));
	}

	public static String stripIRITerminator(String iri) {
		if (iri == null)
			return null;
		if (iri.endsWith("/") || iri.endsWith("#") || iri.endsWith(":"))
			// Shorten the string by one
			return stripIRITerminator(iri.substring(0, iri.length() - 1));
		else
			return iri;
	}

}

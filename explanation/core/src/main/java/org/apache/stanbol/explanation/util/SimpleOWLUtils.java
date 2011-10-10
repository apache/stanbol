package org.apache.stanbol.explanation.util;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLIndividual;

public class SimpleOWLUtils {

    public static OWLIndividual getIndividual(IRI iri) {
        return OWLManager.getOWLDataFactory().getOWLNamedIndividual(iri);
    }
    
}

package org.apache.stanbol.ontologymanager.ontonet.api.io;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLOntology;

public interface OntologySetInputSource {

    Set<OWLOntology> getOntologies();

}

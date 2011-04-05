package org.apache.stanbol.ontologymanager.ontonet.api.ontology;

import org.semanticweb.owlapi.model.IRI;

public interface OntologySpaceListener {

	void onOntologyAdded(IRI spaceId, IRI addedOntology);

	void onOntologyRemoved(IRI spaceId, IRI removedOntology);

}

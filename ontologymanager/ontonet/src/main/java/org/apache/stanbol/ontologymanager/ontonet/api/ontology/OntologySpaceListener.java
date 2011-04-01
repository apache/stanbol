package org.apache.stanbol.ontologymanager.ontonet.api.ontology;

import org.semanticweb.owlapi.model.IRI;

public interface OntologySpaceListener {

	public void onOntologyAdded(IRI spaceId, IRI addedOntology);

	public void onOntologyRemoved(IRI spaceId, IRI removedOntology);

}

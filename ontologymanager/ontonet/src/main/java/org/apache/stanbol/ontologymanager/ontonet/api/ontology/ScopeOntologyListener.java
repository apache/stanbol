package org.apache.stanbol.ontologymanager.ontonet.api.ontology;

import org.semanticweb.owlapi.model.IRI;

public interface ScopeOntologyListener {

	void onOntologyAdded(IRI scopeId, IRI addedOntology);

	void onOntologyRemoved(IRI scopeId, IRI removedOntology);
	
}

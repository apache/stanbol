package eu.iksproject.kres.api.manager.ontology;

import org.semanticweb.owlapi.model.IRI;

public interface ScopeOntologyListener {

	public void onOntologyAdded(IRI scopeId, IRI addedOntology);

	public void onOntologyRemoved(IRI scopeId, IRI removedOntology);
	
}

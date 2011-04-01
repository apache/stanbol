package org.apache.stanbol.ontologymanager.ontonet.api.io;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * An input source that provides the supplied OWL ontology straight away. The
 * physical IRI is either obtained from the default document IRI in the
 * ontology, or supplied manually using the appropriate constructor (e.g.
 * retrieved from the ontology manager that actually loaded the ontology).
 * 
 * @author alessandro
 * 
 */
public class RootOntologySource extends AbstractOntologyInputSource {

	public RootOntologySource(OWLOntology rootOntology) {
		this.rootOntology = rootOntology;
		try {
			physicalIri = rootOntology.getOntologyID().getDefaultDocumentIRI();
		} catch (Exception e) {
			// Ontology might be anonymous, no physical IRI then...
		}

	}

	public RootOntologySource(OWLOntology rootOntology, IRI phyicalIRI) {
		this.rootOntology = rootOntology;
		this.physicalIri = phyicalIRI;
	}

	/*
	 * (non-Javadoc)
	 * @see eu.iksproject.kres.manager.io.AbstractOntologyInputSource#toString()
	 */
	@Override
	public String toString() {
		return "ROOT_ONT<" + rootOntology.getOntologyID() + ">";
	}

}

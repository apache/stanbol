package org.apache.stanbol.ontologymanager.ontonet.api.io;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * An input source that provides the OWL Ontology loaded from the supplied
 * physical IRI, as well as the physical IRI itself for consumers that need to
 * load the ontology themselves.<br>
 * <br>
 * For convenience, an existing OWL ontology manager can be supplied for loading
 * the ontology.
 * 
 * @author alessandro
 * 
 */
public class RootOntologyIRISource extends AbstractOntologyInputSource {

	public RootOntologyIRISource(IRI rootPhysicalIri)
			throws OWLOntologyCreationException {
		this(rootPhysicalIri, OWLManager.createOWLOntologyManager());
	}

	public RootOntologyIRISource(IRI rootPhysicalIri, OWLOntologyManager manager)
			throws OWLOntologyCreationException {
		physicalIri = rootPhysicalIri;
		rootOntology = manager
				.loadOntologyFromOntologyDocument(rootPhysicalIri);
	}

	/*
	 * (non-Javadoc)
	 * @see eu.iksproject.kres.manager.io.AbstractOntologyInputSource#toString()
	 */
	@Override
	public String toString() {
		return "ROOT_ONT_IRI<" + getPhysicalIRI() + ">";
	}

}

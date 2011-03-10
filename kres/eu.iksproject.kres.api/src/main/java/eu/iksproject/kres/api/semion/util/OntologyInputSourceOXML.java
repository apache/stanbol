package eu.iksproject.kres.api.semion.util;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import eu.iksproject.kres.api.manager.io.OntologyInputSource;
import eu.iksproject.kres.ontologies.XML_OWL;

@Deprecated
public class OntologyInputSourceOXML implements OntologyInputSource {

	@Override
	public IRI getPhysicalIRI() {
		return null;
	}

	@Override
	public OWLOntology getRootOntology() {
		try {
			return OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(IRI.create(XML_OWL.URI));
		} catch (OWLOntologyCreationException e) {
			return null;
		}
	}

	@Override
	public boolean hasPhysicalIRI() {
		return false;
	}

	@Override
	public boolean hasRootOntology() {
		return XML_OWL.URI != null;
	}

}

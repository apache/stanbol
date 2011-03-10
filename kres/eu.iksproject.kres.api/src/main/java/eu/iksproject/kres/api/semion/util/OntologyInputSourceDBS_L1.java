package eu.iksproject.kres.api.semion.util;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import eu.iksproject.kres.api.manager.io.OntologyInputSource;
import eu.iksproject.kres.ontologies.DBS_L1;

@Deprecated
public class OntologyInputSourceDBS_L1 implements OntologyInputSource {

	
	@Override
	public IRI getPhysicalIRI() {
		return null;
	}

	@Override
	public OWLOntology getRootOntology() {
		OWLOntology dbsL1Ontology = null;
		
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		
		try {
			dbsL1Ontology = manager.loadOntologyFromOntologyDocument(IRI.create(DBS_L1.getURI()));
		} catch (Exception e) {
			System.out.println("Error while loading ontology "+DBS_L1.URI);
		}
		return dbsL1Ontology;
	}

	@Override
	public boolean hasPhysicalIRI() {
		return false;
	}

	@Override
	public boolean hasRootOntology() {
		return DBS_L1.getURI() != null;
	} 

}

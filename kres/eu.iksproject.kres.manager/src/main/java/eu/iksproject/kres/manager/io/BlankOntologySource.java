package eu.iksproject.kres.manager.io;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * A utility input source that contains an unnamed, empty ontology.
 * 
 * @author alessandro
 *
 */
public class BlankOntologySource extends AbstractOntologyInputSource {

	public BlankOntologySource() {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		try {
			this.rootOntology = manager.createOntology();
		} catch (OWLOntologyCreationException e) {
			this.rootOntology = null;
		}
	}

	@Override
	public String toString() {
		return "";
	}

}

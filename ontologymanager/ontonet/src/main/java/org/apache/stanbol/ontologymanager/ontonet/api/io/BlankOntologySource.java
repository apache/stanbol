package org.apache.stanbol.ontologymanager.ontonet.api.io;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * A utility input source that contains an unnamed, empty ontology. An example usage of this class is to avoid
 * a {@link NullPointerException} to be thrown when an {@link OntologyInputSource} is to be passed to a
 * method, but we are not actually interested in the ontology to pass.
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

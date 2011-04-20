package org.apache.stanbol.ontologymanager.ontonet.api.io;

import java.io.File;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.AutoIRIMapper;

public class ParentPathInputSource extends AbstractOntologyInputSource {

    public ParentPathInputSource(File rootFile) throws OWLOntologyCreationException {
        OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
        mgr.addIRIMapper(new AutoIRIMapper(rootFile.getParentFile(), true));
        rootOntology = mgr.loadOntologyFromOntologyDocument(rootFile);
    }

    @Override
    public String toString() {
        return "ROOT_ONT_IRI<" + getPhysicalIRI() + ">";
    }

}

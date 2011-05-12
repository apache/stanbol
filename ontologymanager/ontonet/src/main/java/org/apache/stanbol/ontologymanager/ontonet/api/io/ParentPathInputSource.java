package org.apache.stanbol.ontologymanager.ontonet.api.io;

import java.io.File;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.AutoIRIMapper;

/**
 * An {@link OntologyInputSource} that recursively tries to hijack all import declarations to the directory
 * containing the input ontology (i.e. the parent of the file itself). It can be used for offline ontology
 * loading, if one has the entire imports closure available in single directory.<br>
 * <br>
 * The behaviour of this class is inherited from the {@link AutoIRIMapper} in the OWL API, and so are its
 * limitations and fallback policies.
 * 
 * @author alessandro
 * 
 */
public class ParentPathInputSource extends AbstractOntologyInputSource {

    public ParentPathInputSource(File rootFile) throws OWLOntologyCreationException {
        this(rootFile, OWLManager.createOWLOntologyManager());
    }

    /**
     * If the developer wishes to recycle an {@link OWLOntologyManager}, they can do so by passing it to the
     * method. Please note that recycling ontology managers will increase the likelihood of
     * {@link OWLOntologyAlreadyExistsException}s being thrown.
     * 
     * @param rootFile
     * @param mgr
     *            the ontology manager to recycle. Note that an {@link AutoIRIMapper} will be added to it.
     * @throws OWLOntologyCreationException
     */
    public ParentPathInputSource(File rootFile, OWLOntologyManager mgr) throws OWLOntologyCreationException {
        AutoIRIMapper mapper = new AutoIRIMapper(rootFile.getParentFile(), true);
        mgr.addIRIMapper(mapper);
        rootOntology = mgr.loadOntologyFromOntologyDocument(rootFile);
        // TODO : do we really want this to happen?
        physicalIri = IRI.create(rootFile);
    }

    @Override
    public String toString() {
        return "ROOT_ONT_IRI<" + getPhysicalIRI() + ">";
    }

}

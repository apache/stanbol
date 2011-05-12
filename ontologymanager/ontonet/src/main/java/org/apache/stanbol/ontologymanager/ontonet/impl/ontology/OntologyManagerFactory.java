package org.apache.stanbol.ontologymanager.ontonet.impl.ontology;

import java.io.File;
import java.util.Iterator;

import org.apache.stanbol.ontologymanager.ontonet.conf.OfflineConfiguration;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.AutoIRIMapper;

/**
 * FIXME: decide on this class either implementing an interface or providing static methods.
 * 
 * @author alessandro
 *
 */
public class OntologyManagerFactory {

    private OfflineConfiguration config;

    private OWLOntologyIRIMapper[] iriMappers = new OWLOntologyIRIMapper[0];

    public OntologyManagerFactory() {
        this(null);
    }

    public OntologyManagerFactory(OfflineConfiguration config) {
        this.config = config;
        if (this.config != null) {
            // Create IRI mappers to reuse for all ontology managers.
            iriMappers = new OWLOntologyIRIMapper[this.config.getDirectories().size()];
            Iterator<File> it = this.config.getDirectories().iterator();
            int j = 0;
            while (it.hasNext()) {
                iriMappers[j++] = new AutoIRIMapper(it.next(), true);
            }
        }
    }

    public OWLOntologyManager createOntologyManager(boolean withOfflineSupport) {
        OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
        if (withOfflineSupport) for (OWLOntologyIRIMapper mapper : iriMappers)
            mgr.addIRIMapper(mapper);
        return mgr;
    }

}

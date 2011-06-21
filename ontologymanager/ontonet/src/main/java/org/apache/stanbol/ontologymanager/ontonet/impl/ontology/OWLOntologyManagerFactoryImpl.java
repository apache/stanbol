package org.apache.stanbol.ontologymanager.ontonet.impl.ontology;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OWLOntologyManagerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.AutoIRIMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FIXME: decide on this class either implementing an interface or providing static methods.
 * 
 * @author alessandro
 * 
 */
public class OWLOntologyManagerFactoryImpl implements OWLOntologyManagerFactory {

    private List<OWLOntologyIRIMapper> iriMappers;

    private Logger log = LoggerFactory.getLogger(getClass());

    public OWLOntologyManagerFactoryImpl() {
        this(null);
    }

    /**
     * 
     * @param dirs
     */
    public OWLOntologyManagerFactoryImpl(List<String> dirs) {

        if (dirs != null) {
            iriMappers = new ArrayList<OWLOntologyIRIMapper>(dirs.size());
            for (String path : dirs) {
                File dir = null;
                if (path.startsWith("/")) {
                    try {
                        dir = new File(getClass().getResource(path).toURI());
                    } catch (URISyntaxException e) {
                        // Don't give up. It could still an absolute path.
                    }
                } else try {
                    dir = new File(path);
                } catch (Exception e1) {
                    try {
                        dir = new File(URI.create(path));
                    } catch (Exception e2) {
                        log.warn("Unable to obtain a path for {}", dir, e2);
                    }
                }
                if (dir != null && dir.isDirectory()) iriMappers.add(new AutoIRIMapper(dir, true));
            }
        }
    }

    @Override
    public OWLOntologyManager createOntologyManager(boolean withOfflineSupport) {
        OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
        if (withOfflineSupport) for (OWLOntologyIRIMapper mapper : getLocalIRIMapperList())
            mgr.addIRIMapper(mapper);
        return mgr;
    }

    @Override
    public List<OWLOntologyIRIMapper> getLocalIRIMapperList() {
        return iriMappers;
    }

}

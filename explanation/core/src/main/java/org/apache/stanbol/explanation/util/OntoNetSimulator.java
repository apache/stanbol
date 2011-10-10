package org.apache.stanbol.explanation.util;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.AutoIRIMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OntoNetSimulator {

    private Logger log = LoggerFactory.getLogger(OntoNetSimulator.class);

    private Map<IRI,OWLOntologyManager> simulatedScopes = new HashMap<IRI,OWLOntologyManager>();

    OWLOntologyIRIMapper[] mapper = new OWLOntologyIRIMapper[2];

    public OntoNetSimulator() {
        URL url = null;
        try {
            url = getClass().getResource("/schemas");
            log.info("Automapping to {}", url.toURI());
            mapper[0] = new AutoIRIMapper(new File(url.toURI()), true);
            url = getClass().getResource("/ontologies");
            log.info("Automapping to {}", url.toURI());
            mapper[1] = new AutoIRIMapper(new File(url.toURI()), true);
        } catch (URISyntaxException e) {
            log.warn("Failed to add IRI mapping for resource {}, aborting mappings altogether.", url);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to add IRI mapping for resource {}, aborting mappings altogether.", url);
        }
    }

    public void addToScope(OntologyInputSource ontSrc, IRI scopeId) {
        OWLDataFactory df = OWLManager.getOWLDataFactory();
        OWLOntology root;
        if (!simulatedScopes.containsKey(scopeId) || simulatedScopes.get(scopeId) == null) try {
            root = OWLManager.createOWLOntologyManager().createOntology(scopeId);
            for (OWLOntologyIRIMapper m : mapper)
                root.getOWLOntologyManager().addIRIMapper(m);
        } catch (OWLOntologyCreationException e) {
            log.error("Failed to create simulated scope " + scopeId, e);
            return;
        }
        else {
            root = simulatedScopes.get(scopeId).getOntology(scopeId);
        }

        root.getOWLOntologyManager().applyChange(
            new AddImport(root, df.getOWLImportsDeclaration(ontSrc.getRootOntology().getOntologyID()
                    .getDefaultDocumentIRI())));
        try {
            root.getOWLOntologyManager().loadOntology(ontSrc.getPhysicalIRI());
        } catch (OWLOntologyCreationException e) {
            log.error("Failed to load " + ontSrc.getPhysicalIRI(), e);
            return;
        }

        simulatedScopes.put(scopeId, root.getOWLOntologyManager());

    }

    public OWLOntology getScopeRoot(IRI scopeid) {
        return simulatedScopes.get(scopeid).getOntology(scopeid);
    }

}

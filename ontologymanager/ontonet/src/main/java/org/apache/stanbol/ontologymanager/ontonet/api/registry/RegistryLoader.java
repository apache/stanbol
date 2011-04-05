package org.apache.stanbol.ontologymanager.ontonet.api.registry;

import java.util.Set;

import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.Registry;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.RegistryContentException;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.RegistryItem;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.RegistryLibrary;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public interface RegistryLoader {

	
	Set<OWLOntology> gatherOntologies(RegistryItem registryItem,
            OWLOntologyManager manager, boolean recurseRegistries)
			throws OWLOntologyCreationException;
			
	RegistryLibrary getLibrary(Registry reg, IRI libraryID);
	
	Object getParent(Object child);

	boolean hasChildren(Object parent);

	boolean hasLibrary(Registry reg, IRI libraryID);

//	public boolean isPrintingLoadedOntologies();

	void loadLocations() throws RegistryContentException;
	
	
	/**
	 * The ontology at <code>physicalIRI</code> may in turn include more than
	 * one registry.
	 * 
	 * @param physicalIRI
	 * @return
	 */
    Set<Registry> loadRegistriesEager(IRI physicalIRI);

//	public void setPrintLoadedOntologies(boolean doPrint);
}

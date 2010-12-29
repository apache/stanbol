package eu.iksproject.kres.api.manager.registry;

import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.stlab.xd.registry.models.Registry;
import org.stlab.xd.registry.models.RegistryContentException;
import org.stlab.xd.registry.models.RegistryItem;
import org.stlab.xd.registry.models.RegistryLibrary;

public interface KReSRegistryLoader {

	
	public Set<OWLOntology> gatherOntologies(RegistryItem registryItem,
			OWLOntologyManager manager, boolean recurseRegistries)
			throws OWLOntologyCreationException;
			
	public RegistryLibrary getLibrary(Registry reg, IRI libraryID);
	
	public Object getParent(Object child);

	public boolean hasChildren(Object parent);

	public boolean hasLibrary(Registry reg, IRI libraryID);

//	public boolean isPrintingLoadedOntologies();

	public void loadLocations() throws RegistryContentException;
	
	
	/**
	 * The ontology at <code>physicalIRI</code> may in turn include more than
	 * one registry.
	 * 
	 * @param physicalIRI
	 * @return
	 */
	public Set<Registry> loadRegistriesEager(IRI physicalIRI);

//	public void setPrintLoadedOntologies(boolean doPrint);
}

package eu.iksproject.kres.manager.io;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stlab.xd.registry.models.Registry;
import org.stlab.xd.registry.models.RegistryItem;

import eu.iksproject.kres.api.manager.ontology.OntologyInputSource;
import eu.iksproject.kres.api.manager.registry.KReSRegistryLoader;
import eu.iksproject.kres.manager.util.OntologyUtils;

/**
 * An input source that provides a single ontology that imports all the imported
 * ontology libraries found in the ontology registry obtained by dereferencing a
 * supplied IRI.
 * 
 * @author alessandro
 * 
 */
public class OntologyRegistryIRISource extends AbstractOntologyInputSource {

	protected IRI registryIRI = null;

	public OntologyRegistryIRISource(IRI registryIRI,
			OWLOntologyManager ontologyManager, KReSRegistryLoader loader) {
		this(registryIRI, ontologyManager, loader, null);
	}

	/**
	 * Creates a new ontology input source by providing a new root ontology that
	 * imports the entire network addressed by the ontology registry at the
	 * supplied IRI.
	 * 
	 * @param registryIRI
	 */
	public OntologyRegistryIRISource(IRI registryIRI,
			OWLOntologyManager ontologyManager, KReSRegistryLoader loader,
			OntologyInputSource parentSrc) {

		this.registryIRI = registryIRI;

		Logger log = LoggerFactory.getLogger(getClass());

		Set<OWLOntology> subtrees = new HashSet<OWLOntology>();
		for (Registry reg : loader.loadRegistriesEager(registryIRI)) {
			for (RegistryItem ri : reg.getChildren()) {
				if (ri.isLibrary())
					try {
						Set<OWLOntology> adds = loader.gatherOntologies(ri,
								ontologyManager, true);
						subtrees.addAll(adds);
					} catch (OWLOntologyAlreadyExistsException e) {
						// Chettefreca
						continue;
					} catch (OWLOntologyCreationException e) {
						log.warn(
								"KReS : [NONFATAL] Failed to load ontology library "
										+ ri.getName() + ". Skipping.", e);
						// If we can't load this library at all, scrap it.
						// TODO : not entirely convinced of this step.
						continue;
					}
			}
		}
		// We always construct a new root now, even if there's just one subtree.

		// Set<OWLOntology> subtrees = mgr.getOntologies();
		// if (subtrees.size() == 1)
		// rootOntology = subtrees.iterator().next();
		// else
		try {
			if (parentSrc != null)
				rootOntology = OntologyUtils.buildImportTree(parentSrc,
						subtrees, ontologyManager);
			else
				rootOntology = OntologyUtils.buildImportTree(subtrees,
						ontologyManager);
		} catch (OWLOntologyCreationException e) {
			log.error(
					"KReS :: Failed to build import tree for registry source "
							+ registryIRI, e);
		}
	}

	/**
	 * This method always return null. The ontology that imports the whole
	 * network is created in-memory, therefore it has no physical IRI.
	 */
	@Override
	public IRI getPhysicalIRI() {
		return null;
	}

	/**
	 * This method always return false. The ontology that imports the whole
	 * network is created in-memory, therefore it has no physical IRI.
	 */
	@Override
	public boolean hasPhysicalIRI() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eu.iksproject.kres.manager.io.AbstractOntologyInputSource#toString()
	 */
	@Override
	public String toString() {
		return "REGISTRY_IRI<" + registryIRI + ">";
	}

}

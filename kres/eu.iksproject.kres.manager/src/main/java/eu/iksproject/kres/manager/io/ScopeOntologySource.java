package eu.iksproject.kres.manager.io;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.LoggerFactory;

/**
 * An ontology source that rewrites the physical IRI by appending the logical
 * one to the scope ID. If the ontology is anonymous, the original physical IRI
 * is retained.
 * 
 * @author alessandro
 * 
 */
public class ScopeOntologySource extends AbstractOntologyInputSource {

	public ScopeOntologySource(IRI scopeIri, OWLOntology ontology, IRI origin) {
		rootOntology = ontology;
		LoggerFactory.getLogger(ScopeOntologySource.class).debug(
				"[KReS] :: REWRITING " + origin + " TO " + scopeIri + "/"
						+ ontology.getOntologyID().getOntologyIRI());
		physicalIri = !ontology.isAnonymous() ? IRI.create(scopeIri + "/"
				+ ontology.getOntologyID().getOntologyIRI()) : origin;
	}

	@Override
	public String toString() {
		return "SCOPE_ONT_IRI<" + getPhysicalIRI() + ">";
	}

}

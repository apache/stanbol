package org.apache.stanbol.ontologymanager.ontonet.api.io;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;


/**
 * Abstract implementation of {@link OntologyInputSource} with the basic methods
 * for obtaining root ontologies and their physical IRIs where applicable.
 * 
 * @author alessandro
 * 
 */
public abstract class AbstractOntologyInputSource implements
		OntologyInputSource {

	protected IRI physicalIri = null;

	protected OWLOntology rootOntology = null;

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof OntologyInputSource))
			return false;
		OntologyInputSource src = (OntologyInputSource) obj;
		return this.physicalIri.equals(src.getPhysicalIRI())
				&& this.rootOntology.equals(src.getRootOntology());
	}

	/*
	 * (non-Javadoc)
	 * @see eu.iksproject.kres.api.manager.ontology.OntologyInputSource#getPhysicalIRI()
	 */
	@Override
	public IRI getPhysicalIRI() {
		return physicalIri;
	}

	/*
	 * (non-Javadoc)
	 * @see eu.iksproject.kres.api.manager.ontology.OntologyInputSource#getRootOntology()
	 */
	@Override
	public OWLOntology getRootOntology() {
		return rootOntology;
	}

	/*
	 * (non-Javadoc)
	 * @see eu.iksproject.kres.api.manager.ontology.OntologyInputSource#hasPhysicalIRI()
	 */
	@Override
	public boolean hasPhysicalIRI() {
		return physicalIri != null;
	}
	
	/*
	 * (non-Javadoc)
	 * @see eu.iksproject.kres.api.manager.ontology.OntologyInputSource#hasRootOntology()
	 */
	@Override
	public boolean hasRootOntology() {
		return rootOntology != null;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public abstract String toString();

}

package org.apache.stanbol.ontologymanager.ontonet.api.ontology;

import org.semanticweb.owlapi.model.IRI;

/**
 * Thrown whenever an operation on a scope that has not been registered is
 * thrown.
 * 
 * @author alessandro
 * 
 */
public class NoSuchScopeException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6339531579406287445L;

	private IRI scopeID = null;

	public NoSuchScopeException(IRI scopeID) {
		this.scopeID = scopeID;
	}

	public IRI getScopeId() {
		return scopeID;
	}

}

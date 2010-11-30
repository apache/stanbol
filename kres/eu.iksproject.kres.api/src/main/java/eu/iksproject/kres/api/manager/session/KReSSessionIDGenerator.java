package eu.iksproject.kres.api.manager.session;

import java.util.Set;

import org.semanticweb.owlapi.model.IRI;

/**
 * Implementations of this interface provide algorithms for generating valid
 * identifiers for KReS sessions. These algorithms should take into account the
 * need for excluding existing session IDs.
 * 
 * @author alessandro
 * 
 */
public interface KReSSessionIDGenerator {

	/**
	 * Generates a new context-free session ID. Whether this causes duplicate
	 * IDs, it should be care of the object that invoked this method to check
	 * it.
	 * 
	 * @return the newly generated session ID.
	 */
	public IRI createSessionID();

	/**
	 * Generates a new session ID that is different from any IRI in the
	 * <code>exclude</code> set. Whether this causes duplicate IDs (supposing
	 * the <code>exclude</code> set does not include all of them), it should be
	 * care of the object that invoked this method to check it.
	 * 
	 * @param exclude
	 *            the set of IRIs none of which the generate ID must be equal
	 *            to.
	 * @return the newly generated session ID.
	 */
	public IRI createSessionID(Set<IRI> exclude);

	/**
	 * Returns the base IRI for all generated IDs to start with. It should be
	 * used by all <code>createSessionID()</code> methods, or ignore if null.
	 * 
	 * @param baseIRI
	 *            the base IRI.
	 */
	public IRI getBaseIRI();

	/**
	 * Sets the base IRI for all generated IDs to start with. It should be used
	 * by all <code>createSessionID()</code> methods, or ignore if null.
	 * 
	 * @param baseIRI
	 *            the base IRI.
	 */
	public void setBaseIRI(IRI baseIRI);
}

package eu.iksproject.kres.api.manager.ontology;

import java.util.Collection;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

import eu.iksproject.kres.api.manager.io.OntologyInputSource;

/**
 * An ontology space identifies the set of OWL ontologies that should be
 * "active" in a given context, e.g. for a certain user session or a specific
 * reasoning service. Each ontology space has an ID and a top ontology that can
 * be used as a shared resource for mutual exclusion and locking strategies.
 * 
 * @author alessandro
 * 
 */
public interface OntologySpace {

	/**
	 * Adds the given ontology to the ontology space.
	 * 
	 * @param ontology
	 *            the ontology to be added
	 * @throws UnmodifiableOntologySpaceException
	 *             if the ontology space is read-only
	 */
	public void addOntology(OntologyInputSource ontologySource)
			throws UnmodifiableOntologySpaceException;

	public void addOntologySpaceListener(OntologySpaceListener listener);

	public void clearOntologySpaceListeners();

	/**
	 * Returns a Unique Resource Identifier (URI) that identifies this ontology
	 * space. For instance, this URI could be the parent of (some/most of) the
	 * base URIs for the ontologies within this space.
	 * 
	 * @return the URI that identifies this ontology space
	 */
	public IRI getID();

	/**
	 * Returns all the ontologies encompassed by this ontology space.
	 * 
	 * @return the set of ontologies in the ontology space
	 */
	public Set<OWLOntology> getOntologies();

	/**
	 * Returns the ontology identified by the supplied <i>logical</i> IRI, if
	 * such an ontology has been loaded in this space.<br>
	 * <br>
	 * Note that ontologies are not identified by physical IRI here. There's no
	 * need to ask KReS for ontologies by physical IRI, use a browser or some
	 * other program instead!
	 * 
	 * @param ontologyIri
	 *            the <i>logical</i> identifier of the ontology to query for.
	 * 
	 * @return the requested ontology, or null if no ontology with this ID has
	 *         been loaded.
	 */
	public OWLOntology getOntology(IRI ontologyIri);
	
	public boolean containsOntology(IRI ontologyIri);

	public Collection<OntologySpaceListener> getOntologyScopeListeners();

	/**
	 * Returns the ontology that serves as a root module for this ontology
	 * space.
	 * 
	 * @return the root module of the ontology space
	 */
	public OWLOntology getTopOntology();

	/**
	 * Determines if the ontology identified by the supplied <i>logical</i> IRI
	 * has been loaded in this space.<br>
	 * <br>
	 * Note that ontologies are not identified by physical IRI here. There's no
	 * need to ask KReS for ontologies by physical IRI, use a browser or some
	 * other program instead!
	 * 
	 * @param ontologyIri
	 *            the <i>logical</i> identifier of the ontology to query for.
	 * 
	 * @return true if an ontology with this ID has been loaded in this space.
	 */
	public boolean hasOntology(IRI ontologyIri);

	/**
	 * Determines if it is no longer possible to modify this space until it is
	 * torn down.
	 * 
	 * @return true if this space is write-locked, false otherwise.
	 */
	public boolean isLocked();

	public boolean isSilentMissingOntologyHandling();

	/**
	 * Removes the given ontology from the ontology space, if the ontology is a
	 * direct child of the top ontology. This means that the ontology must
	 * neither be the top ontology for this space, nor a subtree of an imported
	 * ontology. This is a conservative measure to avoid using undefined
	 * entities in the space.
	 * 
	 * @param ontology
	 *            the ontology to be removed
	 * @throws UnmodifiableOntologySpaceException
	 *             if the ontology space is read-only
	 */
	public void removeOntology(OntologyInputSource src)
			throws OntologySpaceModificationException;

	public void removeOntologySpaceListener(OntologySpaceListener listener);
	
	public void setSilentMissingOntologyHandling(boolean silent);
	
	/**
	 * Sets the supplied ontology as the root ontology that (recursively)
	 * references the whole underlying ontology network. This actually
	 * <i>replaces</i> the ontology to be obtained by a call to
	 * <code>getTopOntology()</code> with this one, i.e. it is <code>not</code>
	 * equivalent to adding this ontology to a blank network!<br>
	 * <br>
	 * Implementations can arbitrarily behave with respect to the unset
	 * <code>createParent</code> parameter from the other method signature.
	 * 
	 * @param ontology
	 *            the new top ontology.
	 * @throws OntologySpaceModificationException
	 *             if the ontology space is read-only or the ontology could not
	 *             be removed.
	 */
	public void setTopOntology(OntologyInputSource ontologySource)
			throws UnmodifiableOntologySpaceException;

	/**
	 * Sets the supplied ontology as the root ontology that (recursively)
	 * references the whole underlying ontology network. This actually
	 * <i>replaces</i> the ontology to be obtained by a call to
	 * <code>getTopOntology()</code> with this one, i.e. it is <code>not</code>
	 * equivalent to adding this ontology to a blank network!
	 * 
	 * @param ontology
	 *            the new top ontology.
	 * @param createParent
	 *            if true, a new ontology will be created and set as the top
	 *            ontology that will import this one.
	 * @throws UnmodifiableOntologySpaceException
	 *             if the ontology space is read-only.
	 */
	public void setTopOntology(OntologyInputSource ontologySource,
			boolean createParent) throws UnmodifiableOntologySpaceException;

	/**
	 * Bootstraps the ontology space. In some cases (such as with core and
	 * custom spaces) this also implies write-locking its ontologies.
	 */
	public void setUp();

	/**
	 * Performs all required operations for disposing of an ontology space and
	 * releasing its resources (e.g. removing the writelock).
	 */
	public void tearDown();

}

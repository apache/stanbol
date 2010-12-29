package eu.iksproject.kres.manager.ontology;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.slf4j.LoggerFactory;

import eu.iksproject.kres.api.manager.ontology.CoreOntologySpace;
import eu.iksproject.kres.api.manager.ontology.CustomOntologySpace;
import eu.iksproject.kres.api.manager.ontology.OntologyInputSource;
import eu.iksproject.kres.api.manager.ontology.OntologyScope;
import eu.iksproject.kres.api.manager.ontology.OntologySpace;
import eu.iksproject.kres.api.manager.ontology.OntologySpaceFactory;
import eu.iksproject.kres.api.manager.ontology.OntologySpaceListener;
import eu.iksproject.kres.api.manager.ontology.ScopeOntologyListener;
import eu.iksproject.kres.api.manager.ontology.SessionOntologySpace;
import eu.iksproject.kres.api.manager.ontology.UnmodifiableOntologySpaceException;

/**
 * The default implementation of an ontology scope.
 * 
 * @author alessandro
 * 
 */
public class OntologyScopeImpl implements OntologyScope, OntologySpaceListener {

	/**
	 * The core ontology space for this scope, always set as default.
	 */
	protected CoreOntologySpace coreSpace;

	/**
	 * The custom ontology space for this scope. This is optional, but cannot be
	 * set after the scope has been setup.
	 */
	protected CustomOntologySpace customSpace;

	/**
	 * The unique identifier for this scope.
	 */
	protected IRI id = null;

	private Set<ScopeOntologyListener> listeners = new HashSet<ScopeOntologyListener>();

	/**
	 * An ontology scope knows whether it's write-locked or not. Initially it is
	 * not.
	 */
	protected boolean locked = false;

	/**
	 * Maps session IDs to ontology space. A single scope has at most one space
	 * per session.
	 */
	protected Map<IRI, SessionOntologySpace> sessionSpaces;

	public OntologyScopeImpl(IRI id, OntologySpaceFactory factory,OntologyInputSource coreRoot) {
		this(id, factory, coreRoot,null);
	}

	public OntologyScopeImpl(IRI id,OntologySpaceFactory factory, OntologyInputSource coreRoot, 
			OntologyInputSource customRoot) {
		if (id == null)
			throw new NullPointerException(
					"Ontology scope must be identified by a non-null IRI.");

		this.id = id;
		this.coreSpace = factory.createCoreOntologySpace(id, coreRoot);
		this.coreSpace.addOntologySpaceListener(this);
		// let's just lock it. Once the core space is done it's done.
		this.coreSpace.setUp();
		// if (customRoot != null) {
		try {
			setCustomSpace(factory.createCustomOntologySpace(id, customRoot));
		} catch (UnmodifiableOntologySpaceException e) {
			// Can't happen unless the factory or space implementations are
			// really naughty.
			LoggerFactory
					.getLogger(getClass())
					.warn(
							"KReS :: Ontology scope "
									+ id
									+ " was denied creation of its own custom space upon initialization! This should not happen.",
							e);
		}
		this.customSpace.addOntologySpaceListener(this);
		// }
		sessionSpaces = new HashMap<IRI, SessionOntologySpace>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeeu.iksproject.kres.api.manager.ontology.OntologyScope#
	 * addOntologyScopeListener
	 * (eu.iksproject.kres.api.manager.ontology.ScopeOntologyListener)
	 */
	@Override
	public void addOntologyScopeListener(ScopeOntologyListener listener) {
		listeners.add(listener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eu.iksproject.kres.api.manager.ontology.OntologyScope#addSessionSpace
	 * (eu.iksproject.kres.api.manager.ontology.OntologySpace,
	 * org.semanticweb.owlapi.model.IRI)
	 */
	@Override
	public synchronized void addSessionSpace(OntologySpace sessionSpace,
			IRI sessionId) {
		if (sessionSpace instanceof SessionOntologySpace) {
			sessionSpaces.put(sessionId, (SessionOntologySpace) sessionSpace);
			sessionSpace.addOntologySpaceListener(this);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeeu.iksproject.kres.api.manager.ontology.OntologyScope#
	 * clearOntologyScopeListeners()
	 */
	@Override
	public void clearOntologyScopeListeners() {
		listeners.clear();
	}

	protected void fireOntologyAdded(IRI ontologyIri) {
		for (ScopeOntologyListener listener : listeners)
			listener.onOntologyAdded(this.getID(), ontologyIri);
	}

	protected void fireOntologyRemoved(IRI ontologyIri) {
		for (ScopeOntologyListener listener : listeners)
			listener.onOntologyRemoved(this.getID(), ontologyIri);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eu.iksproject.kres.api.manager.ontology.OntologyScope#getCoreSpace()
	 */
	@Override
	public OntologySpace getCoreSpace() {
		return coreSpace;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eu.iksproject.kres.api.manager.ontology.OntologyScope#getCustomSpace()
	 */
	@Override
	public OntologySpace getCustomSpace() {
		return customSpace;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eu.iksproject.kres.api.manager.ontology.OntologyScope#getID()
	 */
	@Override
	public IRI getID() {
		return id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeeu.iksproject.kres.api.manager.ontology.OntologyScope#
	 * getOntologyScopeListeners()
	 */
	@Override
	public Collection<ScopeOntologyListener> getOntologyScopeListeners() {
		return listeners;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eu.iksproject.kres.api.manager.ontology.OntologyScope#getSessionSpace
	 * (org.semanticweb.owlapi.model.IRI)
	 */
	@Override
	public SessionOntologySpace getSessionSpace(IRI sessionID) {
		return sessionSpaces.get(sessionID);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eu.iksproject.kres.api.manager.ontology.OntologyScope#getSessionSpaces()
	 */
	@Override
	public Set<OntologySpace> getSessionSpaces() {
		return new HashSet<OntologySpace>(sessionSpaces.values());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eu.iksproject.kres.api.manager.ontology.OntologySpaceListener#onOntologyAdded
	 * (org.semanticweb.owlapi.model.IRI, org.semanticweb.owlapi.model.IRI)
	 */
	@Override
	public void onOntologyAdded(IRI spaceId, IRI addedOntology) {
		// Propagate events to scope listeners
		fireOntologyAdded(addedOntology);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeeu.iksproject.kres.api.manager.ontology.OntologySpaceListener#
	 * onOntologyRemoved(org.semanticweb.owlapi.model.IRI,
	 * org.semanticweb.owlapi.model.IRI)
	 */
	@Override
	public void onOntologyRemoved(IRI spaceId, IRI removedOntology) {
		// Propagate events to scope listeners
		fireOntologyRemoved(removedOntology);
	}

	@Override
	public void removeOntologyScopeListener(ScopeOntologyListener listener) {
		listeners.remove(listener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eu.iksproject.kres.api.manager.ontology.OntologyScope#setCustomSpace(
	 * eu.iksproject.kres.api.manager.ontology.OntologySpace)
	 */
	@Override
	public synchronized void setCustomSpace(OntologySpace customSpace)
			throws UnmodifiableOntologySpaceException {
		if (this.customSpace != null && this.customSpace.isLocked())
			throw new UnmodifiableOntologySpaceException(getCustomSpace());
		else if (!(customSpace instanceof CustomOntologySpace))
			throw new ClassCastException(
					"supplied object is not a CustomOntologySpace instance.");
		else {
			this.customSpace = (CustomOntologySpace) customSpace;
			this.customSpace.addOntologySpaceListener(this);
			this.customSpace.attachCoreSpace(this.coreSpace, true);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eu.iksproject.kres.api.manager.ontology.OntologyScope#setUp()
	 */
	@Override
	public synchronized void setUp() {
		if (locked || (customSpace != null && !customSpace.isLocked()))
			return;
		this.coreSpace.addOntologySpaceListener(this);
		this.coreSpace.setUp();
		if (this.customSpace != null) {
			this.customSpace.addOntologySpaceListener(this);
			this.customSpace.setUp();
		}
		locked = true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eu.iksproject.kres.api.manager.ontology.OntologyScope#tearDown()
	 */
	@Override
	public synchronized void tearDown() {
		// this.coreSpace.addOntologySpaceListener(this);
		this.coreSpace.tearDown();
		if (this.customSpace != null) {
			// this.customSpace.addOntologySpaceListener(this);
			this.customSpace.tearDown();
		}
		locked = false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getID().toString();
	}

	@Override
	public void synchronizeSpaces() {
		// TODO Auto-generated method stub

	}

}

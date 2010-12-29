package eu.iksproject.kres.manager.ontology;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyLoaderListener;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.RemoveImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.kres.api.manager.ontology.IrremovableOntologyException;
import eu.iksproject.kres.api.manager.ontology.MissingOntologyException;
import eu.iksproject.kres.api.manager.ontology.OntologyInputSource;
import eu.iksproject.kres.api.manager.ontology.OntologyScope;
import eu.iksproject.kres.api.manager.ontology.OntologySpace;
import eu.iksproject.kres.api.manager.ontology.OntologySpaceListener;
import eu.iksproject.kres.api.manager.ontology.OntologySpaceModificationException;
import eu.iksproject.kres.api.manager.ontology.SessionOntologySpace;
import eu.iksproject.kres.api.manager.ontology.SpaceType;
import eu.iksproject.kres.api.manager.ontology.UnmodifiableOntologySpaceException;
import eu.iksproject.kres.api.storage.OntologyStorage;
import eu.iksproject.kres.manager.ONManager;
import eu.iksproject.kres.manager.io.RootOntologySource;
import eu.iksproject.kres.manager.util.OntologyUtils;
import eu.iksproject.kres.manager.util.StringUtils;

/**
 * Abstract implementation of an ontology space. While it still leaves it up to
 * developers to decide what locking policies to adopt for subclasses (in the
 * <code>setUp()</code> method), it provides default implementations of all
 * other interface methods.<br>
 * <br>
 * NOTE: By default, an ontology space is NOT write-locked. Developers need to
 * set the <code>locked</code> variable to true to make the space read-only.
 * 
 * 
 * @author alessandro
 * 
 */
public abstract class AbstractOntologySpaceImpl implements OntologySpace {

	protected IRI _id = null;

	private Set<OntologySpaceListener> listeners = new HashSet<OntologySpaceListener>();

	/**
	 * Indicates whether this ontology space is marked as read-only. Default
	 * value is false.
	 */
	protected boolean locked = false;

	protected Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Each ontology space comes with its OWL ontology manager. By default, it
	 * is not available to the outside world, unless subclasses implement
	 * methods to return it.
	 */
	protected OWLOntologyManager ontologyManager;

	protected OntologyStorage storage;

	protected IRI parentID = null;

//	public static String SUFFIX = "";
	
	protected OWLOntology rootOntology = null;

	protected boolean silent = false;

	protected AbstractOntologySpaceImpl(IRI spaceID, SpaceType type/*, IRI parentID*/, OntologyStorage storage) {
		this(spaceID, type, /*parentID,*/ storage,OWLManager.createOWLOntologyManager());
	}

//	/**
//	 * TODO: manage IDs properly
//	 * 
//	 * @param rootOntology
//	 */
//	public AbstractOntologySpaceImpl(IRI spaceID, SpaceType type, IRI parentID,
//			OntologyInputSource rootOntology) {
//		this(spaceID, type, parentID, OWLManager.createOWLOntologyManager(),
//				rootOntology);
//	}

	/**
	 * Creates a new ontology space with the supplied ontology manager as the
	 * default manager for this space.
	 * 
	 * @param spaceID
	 *            the IRI that will uniquely identify this space.
	 	 * @param parentID
	 *             IRI of the parent scope (TODO: get rid of it).
	 * @param ontologyManager
	 *            the default ontology manager for this space.
	 */
	protected AbstractOntologySpaceImpl(IRI spaceID, SpaceType type, OntologyStorage storage, /*IRI parentID,*/
			OWLOntologyManager ontologyManager) {

//		this.parentID = parentID;
//		SUFFIX = type.getIRISuffix();

//		// FIXME: ensure that this is not null
//		OntologyScope parentScope = ONManager.get().getScopeRegistry()
//				.getScope(parentID);
//
//		if (parentScope != null && parentScope instanceof OntologySpaceListener)
//			this.addOntologySpaceListener((OntologySpaceListener) parentScope);
this.storage = storage;
		this._id = spaceID;
		if (ontologyManager != null)
			this.ontologyManager = ontologyManager;
		else
			this.ontologyManager = OWLManager.createOWLOntologyManager();

		this.ontologyManager
				.addOntologyLoaderListener(new OWLOntologyLoaderListener() {

					@Override
					public void finishedLoadingOntology(
							LoadingFinishedEvent arg0) {
						if (arg0.isSuccessful()) {
							fireOntologyAdded(arg0.getOntologyID()
									.getOntologyIRI());
						}
					}

					@Override
					public void startedLoadingOntology(LoadingStartedEvent arg0) {
						// TODO Auto-generated method stub

					}

				});
	}

//	/**
//	 * Creates a new ontology space with the supplied ontology set as its top
//	 * ontology and the supplied ontology manager as the default manager for
//	 * this space.
//	 * 
//	 * @param spaceID
//	 *            the IRI that will uniquely identify this space.
//	 * @param ontologyManager
//	 *            the default ontology manager for this space.
//	 * @param rootSource
//	 *            the root ontology for this space.
//	 */
//	public AbstractOntologySpaceImpl(IRI spaceID,SpaceType type, IRI parentID,
//			OWLOntologyManager ontologyManager, OntologyInputSource rootSource) {
//		this(spaceID, type,parentID, ontologyManager);
//		// Set the supplied ontology's parent as the root for this space.
//		try {
//			this.setTopOntology(rootSource, true);
//		} catch (UnmodifiableOntologySpaceException e) {
//			log.error("KReS :: Ontology space " + spaceID
//					+ " found locked at creation time!", e);
//		}
//	}

	/**
	 * TODO: manage import statements
	 * 
	 * TODO 2 : manage anonymous ontologies.
	 */
	@Override
	public synchronized void addOntology(OntologyInputSource ontologySource)
			throws UnmodifiableOntologySpaceException {

		if (locked)
			throw new UnmodifiableOntologySpaceException(this);

		if (getTopOntology() == null) {
			// If no top ontology has been set, we must create one first.
			IRI rootIri = null;
			try {
				rootIri = IRI.create(StringUtils.stripIRITerminator(getID())
						+ "/root.owl");
				OntologyInputSource src = new RootOntologySource(
						ontologyManager.createOntology(rootIri), null);
				// Don't bother about the ontology to be added right now.
				setTopOntology(src, false);
			} catch (OWLOntologyCreationException e) {
				log.error(
						"KReS :: Exception caught when creating top ontology "
								+ rootIri + " for space " + this.getID() + ".",
						e);
				// No point in continuing if we can't even create the root...
				return;
			}
		}

		// Now add the new ontology.
		if (ontologySource != null && ontologySource.hasRootOntology()) {
			// Remember that this method also fores the event
			performAdd(ontologySource);
		}

	}

	@Override
	public void addOntologySpaceListener(OntologySpaceListener listener) {
		listeners.add(listener);
	}

	@Override
	public void clearOntologySpaceListeners() {
		listeners.clear();
	}

	@Override
	public boolean containsOntology(IRI ontologyIri) {
		return ontologyManager.contains(ontologyIri);
	}

	protected void fireOntologyAdded(IRI ontologyIri) {
		for (OntologySpaceListener listener : listeners)
			listener.onOntologyAdded(this.getID(), ontologyIri);
	}

	protected void fireOntologyRemoved(IRI ontologyIri) {
		for (OntologySpaceListener listener : listeners)
			listener.onOntologyRemoved(this.getID(), ontologyIri);
	}

	@Override
	public IRI getID() {
		return _id;
	}

	@Override
	public synchronized Set<OWLOntology> getOntologies() {
		return ontologyManager.getOntologies();
	}

	@Override
	public OWLOntology getOntology(IRI ontologyIri) {
		return ontologyManager.getOntology(ontologyIri);
	}

	@Override
	public Collection<OntologySpaceListener> getOntologyScopeListeners() {
		return listeners;
	}

	@Override
	public OWLOntology getTopOntology() {
		return rootOntology;
	}

	@Override
	public boolean hasOntology(IRI ontologyIri) {
		return this.getOntology(ontologyIri) != null;
	}

	@Override
	public boolean isLocked() {
		return locked;
	}

	@Override
	public boolean isSilentMissingOntologyHandling() {
		return silent;
	}

	private void performAdd(OntologyInputSource ontSrc) {
		OWLOntology ontology = ontSrc.getRootOntology();
		OWLOntologyID id = ontology.getOntologyID();

		// Should not modify the child ontology in any way.
		// TODO implement transaction control.
		OntologyUtils.appendOntology(new RootOntologySource(getTopOntology(),
				null), ontSrc, ontologyManager/* ,parentID */);

		StringDocumentTarget tgt = new StringDocumentTarget();
		try {
			ontologyManager.saveOntology(ontology, new RDFXMLOntologyFormat(),
					tgt);
		} catch (OWLOntologyStorageException e) {
			log.error("KReS : [FATAL] Failed to store ontology " + id
					+ " in memory.", e);
			return;
		}

		try {
			ontologyManager.removeOntology(ontology);
			ontologyManager
					.loadOntologyFromOntologyDocument(new StringDocumentSource(
							tgt.toString()));
		} catch (OWLOntologyAlreadyExistsException e) {
			// Could happen if we supplied an ontology manager that already
			// knows this ontology. Nothing to do then.
			log.warn("KReS : [NONFATAL] Tried to copy ontology " + id
					+ " to existing one.");
		} catch (OWLOntologyCreationException e) {
			log.error("Unexpected exception caught while copying ontology "
					+ id + " across managers", e);
			return;
		}

		try {
			// Store the top ontology
			if (!(this instanceof SessionOntologySpace)) {
				if (storage == null)
					log
							.error("KReS :: [NONFATAL] no ontology storage found. Ontology "
									+ ontology.getOntologyID()
									+ " will be stored in-memory only.");
				else {
					storage.store(ontology);
				}
			}
			// ONManager.get().getOntologyStore().load(rootOntology.getOntologyID().getOntologyIRI());
		} catch (Exception ex) {
			log.error(
					"KReS :: [NONFATAL] An error occurred while storing ontology "
							+ ontology
							+ " . Ontology management will be volatile!", ex);
		}

		fireOntologyAdded(id.getOntologyIRI());
	}

	/**
	 * TODO 1 : optimize addition/removal <br>
	 * TODO 2 : set import statements
	 */
	@Override
	public synchronized void removeOntology(OntologyInputSource src)
			throws OntologySpaceModificationException {
		if (locked)
			throw new UnmodifiableOntologySpaceException(this);
		else {
			// TODO : find a way to remove anonymous ontologies.
			OWLOntology o = src.getRootOntology();
			IRI logicalID = null, physicalIRI = null;
			try {
				logicalID = o.getOntologyID().getOntologyIRI();
				physicalIRI = src.getPhysicalIRI();
				if (physicalIRI == null)
					if (isSilentMissingOntologyHandling())
						return;
					else
						throw new MissingOntologyException(this, null);
				if (logicalID == null)
					logicalID = physicalIRI;
			} catch (RuntimeException ex) {
				if (isSilentMissingOntologyHandling())
					return;
				else
					throw new MissingOntologyException(this, null);
			}
			if (o.equals(getTopOntology()))
				// setTopOntology(null, false);
				throw new IrremovableOntologyException(this, logicalID);
			try {
				OWLImportsDeclaration imp = ontologyManager.getOWLDataFactory()
						.getOWLImportsDeclaration(physicalIRI);
				ontologyManager.applyChange(new RemoveImport(getTopOntology(),
						imp));
				ontologyManager.removeOntology(o);
				fireOntologyRemoved(logicalID);
			} catch (RuntimeException ex) {
				throw new OntologySpaceModificationException(this);
			}
		}
	}

	@Override
	public void removeOntologySpaceListener(OntologySpaceListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void setSilentMissingOntologyHandling(boolean silent) {
		this.silent = silent;
	}

	@Override
	public synchronized void setTopOntology(OntologyInputSource ontologySource)
			throws UnmodifiableOntologySpaceException {
		setTopOntology(ontologySource, true);
	}

	/**
	 * TODO 1 : Attention: ontology is NOT added to ontology manager!
	 */
	@Override
	public synchronized void setTopOntology(OntologyInputSource ontologySource,
			boolean createParent) throws UnmodifiableOntologySpaceException {

		// TODO : implement or avoid passing of OWLOntology objects across
		// managers

		// Clear the ontology manager
		for (OWLOntology o : ontologyManager.getOntologies()) {
			ontologyManager.removeOntology(o);
			fireOntologyRemoved(o.getOntologyID().getOntologyIRI());
		}

		OWLOntologyID id = new OWLOntologyID(IRI.create(StringUtils
				.stripIRITerminator(_id)
				+ "/root.owl"));
		OWLOntology ontology = null;
		if (ontologySource != null) {
			ontology = ontologySource.getRootOntology();
		}
		OWLOntology /* oTarget = null, */oParent = null;

		// If set to create a parent ontology or this one is anonymous, create
		// the parent
		if (createParent || ontology == null || ontology.isAnonymous()) {

			try {
				oParent = ontologyManager.createOntology(id);
			} catch (OWLOntologyAlreadyExistsException e) {
				oParent = ontologyManager.getOntology(id);
			} catch (OWLOntologyCreationException e) {
				log.error("KReS :: Failed to copy ontology "
						+ ontology.getOntologyID()
						+ " across ontology managers.", e);
			}
		} else {
			// If we don't have to create a parent, assign the original ontology
			// to it.
			oParent = ontology;
		}

		if (ontologySource != null)
			try {

				// Append the supplied ontology to the parent.
				oParent = OntologyUtils.appendOntology(new RootOntologySource(
						oParent, null), ontologySource, ontologyManager/*
																		 * ,parentID
																		 */);

				// Save and reload it to make sure the whole import closure is
				// loaded in memory.
				StringDocumentTarget tgt = new StringDocumentTarget();
				ontologyManager.saveOntology(oParent,
						new RDFXMLOntologyFormat(), tgt);
				ontologyManager.removeOntology(oParent);
				ontologyManager
						.loadOntologyFromOntologyDocument(new StringDocumentSource(
								tgt.toString()));

			} catch (OWLOntologyAlreadyExistsException e) {
				log.warn("KReS : [NONFATAL] Tried to copy ontology " + id
						+ " to existing one.", e);
			} catch (OWLOntologyCreationException e) {
				log.error("KReS : [FATAL] Failed to create ontology " + id, e);
			} catch (OWLOntologyStorageException e) {
				// Shouldn't be a problem to save it in memory as RDF/XML...
				log.error("KReS : [FATAL] In-memory store failed for ontology "
						+ id, e);
			}

		// Assign the ontology and fire the corresponding event.
		rootOntology = oParent != null ? oParent : ontology;

		try {

			// Store the top ontology
			if (!(this instanceof SessionOntologySpace)) {
				if (storage == null)
					log
							.error("KReS :: [NONFATAL] no ontology storage found. Ontology "
									+ rootOntology.getOntologyID()
									+ " will be stored in-memory only.");
				else {
					storage.store(rootOntology);
				}
			}
		} catch (Exception ex) {
			log.error(
					"KReS :: [NONFATAL] An error occurred while storing root ontology "
							+ rootOntology
							+ " . Ontology management will be volatile!", ex);
		}

		fireOntologyAdded(rootOntology.getOntologyID().getOntologyIRI());

	}

}

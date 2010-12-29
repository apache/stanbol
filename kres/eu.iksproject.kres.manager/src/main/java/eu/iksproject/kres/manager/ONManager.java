package eu.iksproject.kres.manager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.IRIDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.StreamDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.kres.api.manager.DuplicateIDException;
import eu.iksproject.kres.api.manager.KReSONManager;
import eu.iksproject.kres.api.manager.ontology.CoreOntologySpace;
import eu.iksproject.kres.api.manager.ontology.CustomOntologySpace;
import eu.iksproject.kres.api.manager.ontology.NoSuchScopeException;
import eu.iksproject.kres.api.manager.ontology.OntologyIndex;
import eu.iksproject.kres.api.manager.ontology.OntologyInputSource;
import eu.iksproject.kres.api.manager.ontology.OntologyScope;
import eu.iksproject.kres.api.manager.ontology.OntologyScopeFactory;
import eu.iksproject.kres.api.manager.ontology.OntologySpace;
import eu.iksproject.kres.api.manager.ontology.OntologySpaceFactory;
import eu.iksproject.kres.api.manager.ontology.ScopeRegistry;
import eu.iksproject.kres.api.manager.ontology.UnmodifiableOntologySpaceException;
import eu.iksproject.kres.api.manager.session.KReSSessionManager;
import eu.iksproject.kres.api.storage.OntologyStorage;
import eu.iksproject.kres.manager.io.BlankOntologySource;
import eu.iksproject.kres.manager.io.RootOntologyIRISource;
import eu.iksproject.kres.manager.ontology.OntologyIndexImpl;
import eu.iksproject.kres.manager.ontology.OntologyScopeFactoryImpl;
import eu.iksproject.kres.manager.ontology.OntologySpaceFactoryImpl;
import eu.iksproject.kres.manager.ontology.ScopeRegistryImpl;
import eu.iksproject.kres.manager.registry.model.impl.RegistryLoader;
import eu.iksproject.kres.manager.session.KReSSessionManagerImpl;
import eu.iksproject.kres.manager.session.ScopeSessionSynchronizer;

/**
 * The running context of a KReS Ontology Network Manager instance. From this
 * object it is possible to obtain factories, indices, registries and what have
 * you.
 * 
 * @author alessandro
 * 
 */
@Component(immediate = true, metatype = true)
@Service(KReSONManager.class)
// @Property(name="service.ranking",intValue=5)
public class ONManager implements KReSONManager {

	public static final String _ALIAS_DEFAULT = "/ontology";
	public static final String _CONFIG_FILE_PATH_DEFAULT = "";
	public static final String _KRES_NAMESPACE_DEFAULT = "http://kres.iksproject.eu/";

	// @Property(value = _ALIAS_DEFAULT)
	public static final String ALIAS = "eu.iksproject.kres.manager.ontologyNetworkManager.alias";

	@Property(value = _CONFIG_FILE_PATH_DEFAULT)
	public static String CONFIG_FILE_PATH = "eu.iksproject.kres.manager.ontologyNetworkManager.config_ont";

	@Property(value = _KRES_NAMESPACE_DEFAULT)
	public static String KRES_NAMESPACE = "kres.namespace";

	@SuppressWarnings("unused")
	private String alias = _ALIAS_DEFAULT;
	private String configPath = _CONFIG_FILE_PATH_DEFAULT;
	private String kresNs = _KRES_NAMESPACE_DEFAULT;

	// private static ONManager me = new ONManager();
	//
	// public static ONManager get() {
	// return me;
	// }

	// private ComponentContext ce;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private OntologyIndex oIndex;

	private OntologyScopeFactory ontologyScopeFactory;

	private OntologySpaceFactory ontologySpaceFactory;

	private OWLOntologyManager owlCacheManager;

	private OWLDataFactory owlFactory;

	private RegistryLoader registryLoader;

	private ScopeRegistry scopeRegistry;

	private KReSSessionManager sessionManager;

	@Reference
	private OntologyStorage storage;

	/*
	 * The identifiers (not yet parsed as IRIs) of the ontology scopes that
	 * should be activated.
	 */
	private String[] toActivate = new String[] {};

	public ONManager() {
		super();
		owlFactory = OWLManager.getOWLDataFactory();
		owlCacheManager = OWLManager.createOWLOntologyManager();

		// These depend on one another
		scopeRegistry = new ScopeRegistryImpl();
		oIndex = new OntologyIndexImpl(this);

		// These may require the OWL cache manager
		ontologySpaceFactory = new OntologySpaceFactoryImpl(scopeRegistry,
				storage);
		ontologyScopeFactory = new OntologyScopeFactoryImpl(scopeRegistry,
				ontologySpaceFactory);
		ontologyScopeFactory.addScopeEventListener(oIndex);

		// This requires the OWL cache manager
		registryLoader = new RegistryLoader(this);

		// TODO : assign dynamically in case the FISE persistence store is not
		// available.
		// storage = new FISEPersistenceStorage();

		sessionManager = new KReSSessionManagerImpl(IRI
				.create("http://kres.iks-project.eu/"), getScopeRegistry(),
				getOntologyStore());
		sessionManager.addSessionListener(new ScopeSessionSynchronizer(this));
	}

	/**
	 * Instantiates all the default providers.
	 * 
	 * TODO : Felix component constraints prevent this constructor from being
	 * private, find a way around...
	 */
	public ONManager(OntologyStorage ontologyStorage,
			Dictionary<String, Object> configuration) {
		this();
		try {
			activate(configuration);
		} catch (IOException e) {
			log.error("Unable to access servlet context.", e);
		}
	}

	/**
	 * Used to configure an instance within an OSGi container.
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	@Activate
	protected void activate(ComponentContext context) throws IOException {
		log.info("in " + ONManager.class + " activate with context " + context);
		if (context == null) {
			throw new IllegalStateException("No valid" + ComponentContext.class
					+ " parsed in activate!");
		}
		activate((Dictionary<String, Object>) context.getProperties());
	}

	protected void activate(Dictionary<String, Object> configuration)
			throws IOException {

		// log.debug("KReS :: activating main component...");
		//
		// me = this;
		// this.ce = ce;

		String tfile = (String) configuration.get(CONFIG_FILE_PATH);
		if (tfile != null)
			this.configPath = tfile;
		String tns = (String) configuration.get(KRES_NAMESPACE);
		if (tns != null)
			this.kresNs = tns;

		// configPath = (String) configuration.get(CONFIG_FILE_PATH);

		/*
		 * If there is no configuration file, just start with an empty scope set
		 */
		if (configPath != null && !configPath.trim().isEmpty()) {
			OWLOntology oConf = null;
			OWLOntologyManager tempMgr = OWLManager.createOWLOntologyManager();
			OWLOntologyDocumentSource oConfSrc = null;

			try {
				log
						.debug("Try to load the configuration ontology from a local bundle relative path");
				InputStream is = this.getClass()
						.getResourceAsStream(configPath);
				oConfSrc = new StreamDocumentSource(is);
			} catch (Exception e1) {
				try {
					log.debug("Cannot load from a local bundle relative path",
							e1);
					log
							.debug("Try to load the configuration ontology resolving the given IRI");
					IRI iri = IRI.create(configPath);
					if (!iri.isAbsolute())
						throw new Exception(
								"IRI seems to be not absolute! value was: "
										+ iri.toQuotedString());
					oConfSrc = new IRIDocumentSource(iri);
					if (oConfSrc == null)
						throw new Exception("Cannot load from the IRI: "
								+ iri.toQuotedString());
				} catch (Exception e) {
					try {
						log.debug("Cannot load from the web", e1);
						log
								.debug("Try to load the configuration ontology as full local file path");
						oConfSrc = new FileDocumentSource(new File(configPath));
					} catch (Exception e2) {
						log.error(
								"Cannot load the configuration ontology from parameter value: "
										+ configPath, e2);
					}
				}
			}

			if (oConfSrc == null) {
				log
						.warn("KReS :: [NONFATAL] No ONM configuration file found at path "
								+ configPath
								+ ". Starting with blank scope set.");
			} else {
				try {
					oConf = tempMgr.loadOntologyFromOntologyDocument(oConfSrc);
				} catch (OWLOntologyCreationException e) {
					log.error("Cannot create the configuration ontology", e);
				}
			}

			// Create and populate the scopes from the config ontology.
			bootstrapOntologyNetwork(oConf);

		}
		log.debug("KReS :: ONManager activated.");

	}

	private void bootstrapOntologyNetwork(OWLOntology configOntology) {
		if (configOntology == null) {
			log
					.debug("KReS :: Ontology Network Manager starting with empty scope set.");
			return;
		}
		try {

			/**
			 * We create and register the scopes before activating
			 */
			for (String scopeIRI : ConfigurationManagement
					.getScopes(configOntology)) {

				String[] cores = ConfigurationManagement.getCoreOntologies(
						configOntology, scopeIRI);
				String[] customs = ConfigurationManagement.getCustomOntologies(
						configOntology, scopeIRI);

				// "Be a man. Use printf"
				log.debug("KReS :: Scope " + scopeIRI);
				for (String s : cores) {
					log.debug("\tKReS :: Core ontology " + s);
				}
				for (String s : customs) {
					log.debug("\tKReS :: Custom ontology " + s);
				}

				// Create the scope
				IRI iri = IRI.create(scopeIRI);
				OntologyScope sc = null;
				sc = ontologyScopeFactory.createOntologyScope(iri,
						new BlankOntologySource());

				// Populate the core space
				if (cores.length > 0) {
					OntologySpace corespc = sc.getCoreSpace();
					corespc.tearDown();
					for (int i = 0; i < cores.length; i++)
						try {
							corespc.addOntology(new RootOntologyIRISource(IRI
									.create(cores[i])));
						} catch (Exception ex) {
							log.warn("KReS :: failed to import ontology "
									+ cores[i], ex);
							continue;
						}
					// TODO: this call should be automatic
					((CustomOntologySpace) sc.getCustomSpace())
							.attachCoreSpace((CoreOntologySpace) corespc, false);
				}

				sc.setUp();
				scopeRegistry.registerScope(sc);

				// getScopeHelper().createScope(scopeIRI);
//				getScopeHelper().addToCoreSpace(scopeIRI, cores);
				getScopeHelper().addToCustomSpace(scopeIRI, customs);
			}

			/**
			 * Try to get activation policies
			 */
			toActivate = ConfigurationManagement
					.getScopesToActivate(configOntology);

			for (String scopeID : toActivate) {
				try {
					IRI scopeId = IRI.create(scopeID.trim());
					scopeRegistry.setScopeActive(scopeId, true);
					log.info("KReS :: Ontology scope " + scopeID
							+ " activated.");
				} catch (NoSuchScopeException ex) {
					log.warn("Tried to activate unavailable scope " + scopeID
							+ ".");
				} catch (Exception ex) {
					log.error("Exception caught while activating scope "
							+ scopeID + " . Skipping.", ex);
					continue;
				}
			}

		} catch (Throwable e) {
			log.error("[NONFATAL] Invalid ONM configuration file found. "
					+ "Starting with blank scope set.", e);
		}

	}

	/**
	 * Deactivation of the ONManager resets all its resources.
	 */
	@Deactivate
	protected void deactivate(ComponentContext context) {
		log.info("in " + ONManager.class + " deactivate with context "
				+ context);
	}

	@Override
	public String getKReSNamespace() {
		return kresNs;
	}

	public OntologyIndex getOntologyIndex() {
		return oIndex;
	}

	/**
	 * Returns the ontology scope factory that was created along with the
	 * manager context.
	 * 
	 * @return the ontology scope factory
	 */
	public OntologyScopeFactory getOntologyScopeFactory() {
		return ontologyScopeFactory;
	}

	/**
	 * Returns the ontology space factory that was created along with the
	 * manager context.
	 * 
	 * @return the ontology space factory
	 */
	public OntologySpaceFactory getOntologySpaceFactory() {
		return ontologySpaceFactory;
	}

	@Override
	public OntologyStorage getOntologyStore() {
		return storage;
	}

	public OWLOntologyManager getOwlCacheManager() {
		// return OWLManager.createOWLOntologyManager();
		return owlCacheManager;
	}

	/**
	 * Returns a factory object that can be used for obtaining OWL API objects.
	 * 
	 * @return the default OWL data factory
	 */
	public OWLDataFactory getOwlFactory() {
		return owlFactory;
	}

	/**
	 * Returns the default ontology registry loader.
	 * 
	 * @return the default ontology registry loader
	 */
	public RegistryLoader getRegistryLoader() {
		return registryLoader;
	}

	/**
	 * Returns the unique ontology scope registry for this context.
	 * 
	 * @return the ontology scope registry
	 */
	public ScopeRegistry getScopeRegistry() {
		return scopeRegistry;
	}

	public KReSSessionManager getSessionManager() {
		return sessionManager;
	}

	public String[] getUrisToActivate() {
		return toActivate;
	}

	private Helper helper = null;

	public Helper getScopeHelper() {
		if (helper == null) {
			helper = new Helper();
		}
		return helper;
	}

	public class Helper {
		private Helper() {
		}

		/**
		 * Create an empty scope. The scope is created, registered and activated
		 * 
		 * @param scopeID
		 * @return
		 * @throws DuplicateIDException
		 */
		public synchronized OntologyScope createScope(String scopeID)
				throws DuplicateIDException {
			OntologyInputSource oisbase = new BlankOntologySource();

			IRI scopeIRI = IRI.create(scopeID);

			/*
			 * The scope is created by the ScopeFactory or loaded to the scope
			 * registry of KReS
			 */
			OntologyScope scope;
			scope = ontologyScopeFactory.createOntologyScope(scopeIRI, oisbase);

			scope.setUp();
			scopeRegistry.registerScope(scope, true);
			log.debug("Created scope " + scopeIRI, this);
			return scope;
		}

		/**
		 * Adds the ontology from the given iri to the core space of the given
		 * scope
		 * 
		 * @param scopeID
		 * @param locationIri
		 */
		public synchronized void addToCoreSpace(String scopeID,
				String[] locationIris) {
			OntologyScope scope = getScopeRegistry().getScope(
					IRI.create(scopeID));
			OntologySpace corespc = scope.getCoreSpace();
			scope.tearDown();
			corespc.tearDown();
			for (String locationIri : locationIris) {
				try {
					corespc.addOntology(new RootOntologyIRISource(IRI
							.create(locationIri)));
					//					
					// corespc.addOntology(
					// createOntologyInputSource(locationIri));
					log.debug("Added " + locationIri + " to scope " + scopeID
							+ " in the core space.", this);
					// OntologySpace cs = scope.getCustomSpace();
					// if (cs instanceof CustomOntologySpace) {
					// (
					// (CustomOntologySpace)cs).attachCoreSpace((CoreOntologySpace)corespc,
					// false);
					// }
				} catch (UnmodifiableOntologySpaceException e) {
					log.error("Core space for scope " + scopeID
							+ " denied addition of ontology " + locationIri, e);
				} catch (OWLOntologyCreationException e) {
					log.error("Creation of ontology from source " + locationIri
							+ " failed.", e);
				}
			}
			corespc.setUp();
		}

		/**
		 * Adds the ontology fromt he given iri to the custom space of the given
		 * scope
		 * 
		 * @param scopeID
		 * @param locationIri
		 */
		public synchronized void addToCustomSpace(String scopeID,
				String[] locationIris) {
			OntologyScope scope = getScopeRegistry().getScope(
					IRI.create(scopeID));

			scope.getCustomSpace().tearDown();
			for (String locationIri : locationIris) {
				try {
					scope.getCustomSpace().addOntology(
							createOntologyInputSource(locationIri));
					log.debug("Added " + locationIri + " to scope " + scopeID
							+ " in the custom space.", this);
				} catch (UnmodifiableOntologySpaceException e) {
					log.error(
							"An error occurred while trying to add the ontology from location: "
									+ locationIri, e);
				}
			}
			scope.getCustomSpace().setUp();
		}

		private OntologyInputSource createOntologyInputSource(final String uri) {
			/*
			 * The scope factory needs an OntologyInputSource as input for the
			 * core ontology space. We want to use the dbpedia ontology as core
			 * ontology of our scope.
			 */
			OntologyInputSource ois = new OntologyInputSource() {

				@Override
				public boolean hasRootOntology() {
					return true;
				}

				@Override
				public boolean hasPhysicalIRI() {
					return false;
				}

				@Override
				public OWLOntology getRootOntology() {

					try {

						OWLOntologyManager manager = OWLManager
								.createOWLOntologyManager();
						return manager.loadOntologyFromOntologyDocument(IRI
								.create(uri));
					} catch (OWLOntologyCreationException e) {
						log.error("Cannot load the ontology " + uri, e);
					} catch (Exception e) {
						log.error("Cannot load the ontology " + uri, e);
					}
					/** If some errors occur **/
					return null;
				}

				@Override
				public IRI getPhysicalIRI() {
					return null;
				}
			};

			return ois;
		}
	}
}

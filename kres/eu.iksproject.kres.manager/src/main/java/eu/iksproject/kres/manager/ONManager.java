package eu.iksproject.kres.manager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
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
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.kres.api.manager.KReSONManager;
import eu.iksproject.kres.api.manager.ontology.NoSuchScopeException;
import eu.iksproject.kres.api.manager.ontology.OntologyIndex;
import eu.iksproject.kres.api.manager.ontology.OntologyScope;
import eu.iksproject.kres.api.manager.ontology.OntologyScopeFactory;
import eu.iksproject.kres.api.manager.ontology.OntologySpaceFactory;
import eu.iksproject.kres.api.manager.ontology.ScopeRegistry;
import eu.iksproject.kres.api.manager.session.KReSSessionManager;
import eu.iksproject.kres.api.storage.OntologyStorage;
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

	// @Property(value = "/ontology")
	public static final String ALIAS_PROPERTY = "eu.iksproject.kres.manager.ontologyNetworkManager.alias";

	@Property(value = "")
	public static String CONFIG_FILE_PATH = "eu.iksproject.kres.manager.ontologyNetworkManager.config_ont";

	@Property(value = "http://kres.iksproject.eu/")
	public static String KRES_NAMESPACE = "kres.namespace";

	private static ONManager me = new ONManager();

	public static ONManager get() {
		return me;
	}

	private ComponentContext ce;

	public final Logger log = LoggerFactory.getLogger(getClass());

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

	/**
	 * Instantiates all the default providers.
	 * 
	 * TODO : Felix component constraints prevent this constructor from being
	 * private, find a way around...
	 */
	public ONManager() {
		owlFactory = OWLManager.getOWLDataFactory();
		owlCacheManager = OWLManager.createOWLOntologyManager();

		// These may require the OWL cache manager
		ontologyScopeFactory = new OntologyScopeFactoryImpl();
		ontologySpaceFactory = new OntologySpaceFactoryImpl();

		// These depend on one another
		scopeRegistry = new ScopeRegistryImpl();
		oIndex = new OntologyIndexImpl(scopeRegistry);
		ontologyScopeFactory.addScopeEventListener(oIndex);

		// This requires the OWL cache manager
		registryLoader = new RegistryLoader();

		// TODO : assign dynamically in case the FISE persistence store is not
		// available.
		// storage = new FISEPersistenceStorage();

		sessionManager = new KReSSessionManagerImpl(IRI
				.create("http://kres.iks-project.eu/"));
		sessionManager.addSessionListener(ScopeSessionSynchronizer.get());
	}

	protected void activate(ComponentContext ce) throws IOException {

		log.debug("KReS :: activating main component...");

		me = this;
		this.ce = ce;

		ScopeRegistry reg = get().getScopeRegistry();
		Set<OntologyScope> scopez = reg.getRegisteredScopes();

		String path = (String) ce.getProperties().get(CONFIG_FILE_PATH);
		// CONFIG_FILE_PATH can be a path or a URI. Try to make up for both
		OWLOntology oConf = null;
		OWLOntologyManager tempMgr = OWLManager.createOWLOntologyManager();
		OWLOntologyDocumentSource oConfSrc = null;
		if (path != null && !path.trim().isEmpty()) {
			// Replace path with default path
			path = "/META-INF/conf/onm.owl";
			InputStream is = this.getClass().getResourceAsStream(path);
			oConfSrc = new StreamDocumentSource(is);
		} else {
			IRI iri = IRI.create(path);
			if (iri.isAbsolute())
				oConfSrc = new IRIDocumentSource(IRI.create(path));
			else
				oConfSrc = new FileDocumentSource(new File(path));
		}

		if (oConfSrc == null)
			log
					.warn("KReS :: [NONFATAL] No ONM configuration file found at path "
							+ path + ". Starting with blank scope set.");
		else
			try {
				oConf = tempMgr.loadOntologyFromOntologyDocument(oConfSrc);
				// The OWLOntologyManager is brand new, it's impossible for it
				// to throw an OWLOntologyAlreadyExistsException ...
			} catch (Throwable e) {
				log.error(
						"KReS :: [NONFATAL] Invalid ONM configuration file found at path "
								+ path + ". Starting with blank scope set.", e);
			}

		if (oConf != null)
			toActivate = ConfigurationManagement.getScopesToActivate(oConf);
		else
			toActivate = new String[0];
		for (String token : toActivate) {
			try {
				IRI scopeId = IRI.create(token.trim());
				reg.setScopeActive(scopeId, true);
				log.info("KReS :: Ontology scope " + token + " activated.");
				scopez.remove(reg.getScope(scopeId));
			} catch (NoSuchScopeException ex) {
				log.warn("KReS :: Tried to activate unavailable scope " + token
						+ ".");
			} catch (Exception ex) {
				log.error("KReS :: Exception caught while activating scope "
						+ token + " . Skipping.", ex);
				continue;
			}
		}
		// Stop deactivating other scopes
		// for (OntologyScope scope : scopez) {
		// IRI scopeId = scope.getID();
		// try {
		// if (reg.isScopeActive(scopeId)) {
		// reg.setScopeActive(scopeId, false);
		// System.out.println("KReS :: Ontology scope " + scopeId
		// + " " + " deactivated.");
		// }
		// } catch (NoSuchScopeException ex) {
		// // Shouldn't happen because we already have the scope handle,
		// // however exceptions could be thrown erroneously...
		// System.err
		// .println("KReS :: Tried to deactivate unavailable scope "
		// + scopeId + ".");
		// } catch (Exception ex) {
		// System.err.println("Exception caught while deactivating scope "
		// + scope.getID() + " " + ex.getClass());
		// continue;
		// }
		// }

		log.debug("KReS :: main component activated.");

	}

	protected void deactivate(ComponentContext ce) throws IOException {
		log.debug("KReS :: deactivating main component...");
	}

	@Override
	public String getKReSNamespace() {
		String ns = (String) ce.getProperties().get(KRES_NAMESPACE);
		return ns;
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

}

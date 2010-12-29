package eu.iksproject.kres.storage.provider;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.kres.api.storage.OntologyStorage;
import eu.iksproject.kres.api.storage.OntologyStoreProvider;

@Component(immediate = true, metatype = true)
@Service(OntologyStoreProvider.class)
public class OntologyStorageProviderImpl implements OntologyStoreProvider {
	
	public static final String _ACTIVE_STORAGE_DEFAULT = "eu.iksproject.kres.storage.ClerezzaStorage";

	@Property(value = _ACTIVE_STORAGE_DEFAULT)
    public static final String ACTIVE_STORAGE = "activeStorage";

	private static Logger log = LoggerFactory
			.getLogger(OntologyStorageProviderImpl.class);

	private OntologyStorage activeOntologyStorage;
	/*
	 * For safety in non-OSGi environments, we initially set this variable to
	 * its default value.
	 */
	private String activeStorage = _ACTIVE_STORAGE_DEFAULT;
	private Hashtable<String, OntologyStorage> registeredStorages;

	/**
	 * This default constructor is <b>only</b> intended to be used by the OSGI
	 * environment with Service Component Runtime support.
	 * <p>
	 * DO NOT USE to manually create instances - the OntologyStorageProviderImpl
	 * instances do need to be configured! YOU NEED TO USE
	 * {@link #OntologyStorageProviderImpl(Dictionary)} or its overloads, to
	 * parse the configuration and then initialise the rule store if running
	 * outside a OSGI environment.
	 */
	public OntologyStorageProviderImpl() {
	
	}

	/**
	 * Basic constructor to be used if outside of an OSGi environment. Invokes
	 * default constructor.
	 * 
	 * @param configuration
	 */
	public OntologyStorageProviderImpl(Dictionary<String, Object> configuration) {
		this();
		activate(configuration);
	}

	/**
	 * Used to configure an instance within an OSGi container.
	 */
	@SuppressWarnings("unchecked")
	@Activate
	protected void activate(ComponentContext context) {
		log.info("in " + OntologyStorageProviderImpl.class
				+ " activate with context " + context);
		if (context == null) {
			throw new IllegalStateException("No valid" + ComponentContext.class
					+ " parsed in activate!");
		}
		activate((Dictionary<String, Object>) context.getProperties());
	}

	/**
	 * Internally used to configure an instance (within and without an OSGi
	 * container.
	 * 
	 * @param configuration
	 */
	protected void activate(Dictionary<String, Object> configuration) {
		String tStorage = (String) configuration.get(ACTIVE_STORAGE);
		if (tStorage != null)
			this.activeStorage = tStorage;
		registeredStorages = new Hashtable<String, OntologyStorage>();
	}
	
	@Override
	public void activateOntologyStorage(OntologyStorage ontologyStorage) {
		activeOntologyStorage = ontologyStorage;
	}
		
	@Deactivate
	protected void deactivate(ComponentContext context) {
		registeredStorages = null;
	}

	@Override
	public void deactivateOntologyStorage() {
		activeOntologyStorage = null;
	}

	@Override
	public OntologyStorage getActiveOntologyStorage() {
		return activeOntologyStorage;
	}

	@Override
	public boolean isActiveOntologyStorage(OntologyStorage ontologyStorage) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Collection<OntologyStorage> listOntologyStorages() {
		return registeredStorages.values();
	}

	@Override
	public void registerOntologyStorage(OntologyStorage ontologyStorage) {
		String storageClass = ontologyStorage.getClass().getCanonicalName();
		registeredStorages.put(storageClass, ontologyStorage);
		log.info("Registerd " + registeredStorages.size() + " storages -> "
				+ storageClass);
		log.info("Active storage class is " + activeStorage);
		if (storageClass.equals(activeStorage)) {
			activeOntologyStorage = ontologyStorage;
			log.debug("Setted active storage");
		}
		
	}

	@Override
	public void unregisterOntologyStorage(
			Class<? extends OntologyStorage> ontologyStorage) {
		registeredStorages.remove(ontologyStorage.getCanonicalName());
	}
}

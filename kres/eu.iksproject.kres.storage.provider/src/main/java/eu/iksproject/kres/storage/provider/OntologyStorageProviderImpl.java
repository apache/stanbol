package eu.iksproject.kres.storage.provider;

import java.util.Collection;
import java.util.Hashtable;


import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;

import eu.iksproject.kres.api.storage.OntologyStorage;
import eu.iksproject.kres.api.storage.OntologyStoreProvider;

@Component(immediate = true, metatype = true)
@Service(OntologyStoreProvider.class)
public class OntologyStorageProviderImpl implements OntologyStoreProvider {
	
	@Property(value = "eu.iksproject.kres.storage.ClerezzaStorage")
    public static final String ACTIVE_STORAGE = "activeStorage";

	private Hashtable<String, OntologyStorage> registeredStorages;
	private OntologyStorage activeOntologyStorage;
	private String activeStorage;
	
	
	@Override
	public void activateOntologyStorage(OntologyStorage ontologyStorage) {
		activeOntologyStorage = ontologyStorage;
		
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
		registeredStorages.put(ontologyStorage.getClass().getCanonicalName(), ontologyStorage);
		System.out.println("Registerd "+registeredStorages.size()+" storages -> "+ontologyStorage.getClass().getCanonicalName());
		System.out.println("Active storage class is "+activeStorage);
		if(ontologyStorage.getClass().getCanonicalName().equals(activeStorage)){
			activeOntologyStorage = ontologyStorage;
			System.out.println("Setted active storage");
		}
		
	}

	@Override
	public void unregisterOntologyStorage(Class<? extends OntologyStorage> ontologyStorage) {
		registeredStorages.remove(ontologyStorage.getCanonicalName());
		
	}

	protected void activate(ComponentContext context) {
		registeredStorages = new Hashtable<String, OntologyStorage>();
		activeStorage = (String) context.getProperties().get(ACTIVE_STORAGE);
		
	}

	protected void deactivate(ComponentContext context) {
		registeredStorages = null;
	}
}

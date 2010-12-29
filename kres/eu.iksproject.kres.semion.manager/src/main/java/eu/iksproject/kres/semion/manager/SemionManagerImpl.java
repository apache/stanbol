package eu.iksproject.kres.semion.manager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.kres.api.manager.KReSONManager;
import eu.iksproject.kres.api.semion.DataSource;
import eu.iksproject.kres.api.semion.ReengineeringException;
import eu.iksproject.kres.api.semion.SemionManager;
import eu.iksproject.kres.api.semion.SemionReengineer;
import eu.iksproject.kres.api.semion.SemionRefactorer;
import eu.iksproject.kres.api.storage.NoSuchOntologyInStoreException;
import eu.iksproject.kres.api.storage.OntologyStorage;

/**
 * Concrete implementation of the
 * {@link eu.iksproject.kres.api.semion.SemionManager} interface defined in the
 * KReS APIs.
 * 
 * @author andrea.nuzzolese
 *
 */

@Component(immediate = true, metatype = true)
@Service(SemionManager.class)
public class SemionManagerImpl implements SemionManager{

	private final Logger log = LoggerFactory.getLogger(getClass());
	@Reference
	KReSONManager onManager;
	
	private ArrayList<SemionReengineer> reengineers;

	private SemionRefactorer semionRefactorer;

	/**
	 * This default constructor is <b>only</b> intended to be used by the OSGI
	 * environment with Service Component Runtime support.
	 * <p>
	 * DO NOT USE to manually create instances - the SemionManagerImpl instances
	 * do need to be configured! YOU NEED TO USE
	 * {@link #SemionManagerImpl(KReSONManager)} or its overloads, to parse the
	 * configuration and then initialise the rule store if running outside a
	 * OSGI environment.
	 */
	public SemionManagerImpl() {
		reengineers = new ArrayList<SemionReengineer>();
	}
	
	/**
	 * Basic constructor to be used if outside of an OSGi environment. Invokes
	 * default constructor.
	 * 
	 * @param onm
	 */
	public SemionManagerImpl(KReSONManager onManager) {
		this();
		this.onManager = onManager;
		activate(new Hashtable<String, Object>());
	}

	/**
	 * Used to configure an instance within an OSGi container.
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	@Activate
	protected void activate(ComponentContext context) throws IOException {
		log.info("in " + SemionManagerImpl.class + " activate with context "
				+ context);
		if (context == null) {
			throw new IllegalStateException("No valid" + ComponentContext.class
					+ " parsed in activate!");
		}
		activate((Dictionary<String, Object>) context.getProperties());
	}

	protected void activate(Dictionary<String, Object> configuration) {
		reengineers = new ArrayList<SemionReengineer>();
	}

	/**
	 * @param semionReengineer
	 *            {@link eu.iksproject.kres.api.semion.SemionReengineer}
	 * @return true if the reengineer is bound, false otherwise
	 */
	@Override
	public boolean bindReengineer(SemionReengineer semionReengineer) {
		boolean found = false;
		Iterator<SemionReengineer> it = reengineers.iterator();
		while(it.hasNext() && !found){
			SemionReengineer reengineer = it.next();
			if (reengineer.getReengineerType() == semionReengineer
					.getReengineerType()) {
				found = true;
			}
		}
		
		if(!found){
			reengineers.add(semionReengineer);
			String info = "Reengineering Manager : " + reengineers.size()
					+ " reengineers";
			log.info(info);
			return true;
		} else {
			log.info("Reengineer already existing");
			return false;
		}
		
	}

	@Override
	public int countReengineers() {
		return reengineers.size();
		}
		
	@Deactivate
	protected void deactivate(ComponentContext context) {
		log.info("in " + SemionManagerImpl.class + " deactivate with context "
				+ context);
		reengineers = null;
	}
	
	@Override
	public SemionRefactorer getRegisteredRefactorer() {
		return semionRefactorer;
		}
		
	@Override
	public Collection<SemionReengineer> listReengineers() {
		return reengineers;
	}
	
	@Override
	public OWLOntology performDataReengineering(String graphNS, IRI outputIRI,
			DataSource dataSource, IRI schemaOntologyIRI)
			throws ReengineeringException, NoSuchOntologyInStoreException {
		
		OWLOntology reengineeredDataOntology = null;
		
		OntologyStorage ontologyStorage = onManager.getOntologyStore();
		
		OWLOntology schemaOntology = ontologyStorage.load(schemaOntologyIRI);
		
		if(schemaOntology == null){
			throw new NoSuchOntologyInStoreException(schemaOntologyIRI);
		} else {
		
			boolean reengineered = false;
			Iterator<SemionReengineer> it = reengineers.iterator();
			while(it.hasNext() && !reengineered){
				SemionReengineer semionReengineer = it.next();
				if(semionReengineer.canPerformReengineering(schemaOntology)){
					reengineeredDataOntology = semionReengineer
							.dataReengineering(graphNS, outputIRI, dataSource,
									schemaOntology);
					reengineered = true;
				}
			}
		}
		
		return reengineeredDataOntology;
	}
	
	@Override
	public OWLOntology performDataReengineering(String graphNS, IRI outputIRI,
			DataSource dataSource, OWLOntology schemaOntology)
			throws ReengineeringException {
		
		OWLOntology reengineeredDataOntology = null;
		
		boolean reengineered = false;
		Iterator<SemionReengineer> it = reengineers.iterator();
		while(it.hasNext() && !reengineered){
			SemionReengineer semionReengineer = it.next();
			if(semionReengineer.canPerformReengineering(schemaOntology)){
				reengineeredDataOntology = semionReengineer.dataReengineering(
						graphNS, outputIRI, dataSource, schemaOntology);
				reengineered = true;
			}
		}
		
		return reengineeredDataOntology;
	}
	
	public OWLOntology performReengineering(String graphNS, IRI outputIRI,
			DataSource dataSource) throws ReengineeringException {
	
		OWLOntology reengineeredOntology = null;

		boolean reengineered = false;
		Iterator<SemionReengineer> it = reengineers.iterator();
		while (it.hasNext() && !reengineered) {
			SemionReengineer semionReengineer = it.next();
			if (semionReengineer.canPerformReengineering(dataSource)) {
				log.debug(semionReengineer.getClass().getCanonicalName()
						+ " can perform the reengineering");
				reengineeredOntology = semionReengineer.reengineering(graphNS,
						outputIRI, dataSource);
				reengineered = true;
			} else {
				log.debug(semionReengineer.getClass().getCanonicalName()
						+ " cannot perform the reengineering");
			}
		}

		return reengineeredOntology;
	}

	public OWLOntology performSchemaReengineering(String graphNS,
			IRI outputIRI, DataSource dataSource) throws ReengineeringException {

		OWLOntology reengineeredSchemaOntology = null;

		boolean reengineered = false;
		Iterator<SemionReengineer> it = reengineers.iterator();
		while (it.hasNext() && !reengineered) {
			SemionReengineer semionReengineer = it.next();
			if (semionReengineer.canPerformReengineering(dataSource)) {
				reengineeredSchemaOntology = semionReengineer
						.schemaReengineering(graphNS, outputIRI, dataSource);
				if (reengineeredSchemaOntology == null) {
					throw new ReengineeringException();
				}
				reengineered = true;
			}
		}

		return reengineeredSchemaOntology;
	}

	@Override
	public void registerRefactorer(SemionRefactorer semionRefactorer) {
		this.semionRefactorer = semionRefactorer;
	}
	
	@Override
	public boolean unbindReengineer(int reenginnerType) {
		boolean found = false;
		for(int i=0, j=reengineers.size(); i<j && !found; i++){
			SemionReengineer reengineer = reengineers.get(i);
			if(reengineer.getReengineerType() == reenginnerType){
				reengineers.remove(i);
				found = true;
			}
		}
		return found;
	}
	
	@Override
	public boolean unbindReengineer(SemionReengineer semionReengineer) {
		boolean found = false;
		for (int i = 0, j = reengineers.size(); i < j && !found; i++) {
			if (semionReengineer.equals(reengineers.get(i))) {
				reengineers.remove(i);
				found = true;
	}
	}
		return found;
	}

	@Override
	public void unregisterRefactorer() {
		this.semionRefactorer = null;
	}
	
}

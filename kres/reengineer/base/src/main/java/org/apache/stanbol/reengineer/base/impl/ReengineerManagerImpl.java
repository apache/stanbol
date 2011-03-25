package org.apache.stanbol.reengineer.base.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.access.WeightedTcProvider;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.impl.io.ClerezzaOntologyStorage;
import org.apache.stanbol.reengineer.base.api.DataSource;
import org.apache.stanbol.reengineer.base.api.ReengineeringException;
import org.apache.stanbol.reengineer.base.api.ReengineerManager;
import org.apache.stanbol.reengineer.base.api.Reengineer;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Concrete implementation of the
 * {@link org.apache.stanbol.reengineer.base.api.ReengineerManager} interface defined in the
 * KReS APIs.
 * 
 * @author andrea.nuzzolese
 *
 */

@Component(immediate = true, metatype = true)
@Service(ReengineerManager.class)
public class ReengineerManagerImpl implements ReengineerManager{

	private final Logger log = LoggerFactory.getLogger(getClass());
	
    @Reference
    private TcManager tcm;

    @Reference
    private WeightedTcProvider wtcp;
    
    private ClerezzaOntologyStorage storage;
	
	private ArrayList<Reengineer> reengineers;
//
//	private SemionRefactorer semionRefactorer;

	/**
	 * This default constructor is <b>only</b> intended to be used by the OSGI
	 * environment with Service Component Runtime support.
	 * <p>
	 * DO NOT USE to manually create instances - the ReengineerManagerImpl instances
	 * do need to be configured! YOU NEED TO USE
	 * {@link #ReengineerManagerImpl(ONManager)} or its overloads, to parse the
	 * configuration and then initialise the rule store if running outside a
	 * OSGI environment.
	 */
	public ReengineerManagerImpl() {
		reengineers = new ArrayList<Reengineer>();
	}
	
	/**
	 * Basic constructor to be used if outside of an OSGi environment. Invokes
	 * default constructor.
	 * 
	 * @param onm
	 */
	public ReengineerManagerImpl(TcManager tcm, WeightedTcProvider wtcp) {
		this();
        storage = new ClerezzaOntologyStorage(tcm, wtcp);
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
		log.info("in " + ReengineerManagerImpl.class + " activate with context "
				+ context);
		if (context == null) {
			throw new IllegalStateException("No valid" + ComponentContext.class
					+ " parsed in activate!");
		}
		activate((Dictionary<String, Object>) context.getProperties());
	}

	protected void activate(Dictionary<String, Object> configuration) {
        if (storage == null) storage = new ClerezzaOntologyStorage(this.tcm, this.wtcp);
		reengineers = new ArrayList<Reengineer>();
	}

	/**
	 * @param semionReengineer
	 *            {@link org.apache.stanbol.reengineer.base.api.Reengineer}
	 * @return true if the reengineer is bound, false otherwise
	 */
	@Override
	public boolean bindReengineer(Reengineer semionReengineer) {
		boolean found = false;
		Iterator<Reengineer> it = reengineers.iterator();
		while(it.hasNext() && !found){
			Reengineer reengineer = it.next();
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
		log.info("in " + ReengineerManagerImpl.class + " deactivate with context "
				+ context);
		reengineers = null;
	}
	
//	@Override
//	public SemionRefactorer getRegisteredRefactorer() {
//		return semionRefactorer;
//		}
		
	@Override
	public Collection<Reengineer> listReengineers() {
		return reengineers;
	}
	
	@Override
	public OWLOntology performDataReengineering(String graphNS, IRI outputIRI,
			DataSource dataSource, IRI schemaOntologyIRI)
			throws ReengineeringException, NoSuchOntologyInStoreException {
		
		OWLOntology reengineeredDataOntology = null;
		
//		OntologyStorage ontologyStorage = onManager.getOntologyStore();
		
		OWLOntology schemaOntology = storage.load(schemaOntologyIRI);
		
		if(schemaOntology == null){
			throw new NoSuchOntologyInStoreException(schemaOntologyIRI);
		} else {
		
			boolean reengineered = false;
			Iterator<Reengineer> it = reengineers.iterator();
			while(it.hasNext() && !reengineered){
				Reengineer semionReengineer = it.next();
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
		Iterator<Reengineer> it = reengineers.iterator();
		while(it.hasNext() && !reengineered){
			Reengineer semionReengineer = it.next();
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
		Iterator<Reengineer> it = reengineers.iterator();
		while (it.hasNext() && !reengineered) {
			Reengineer semionReengineer = it.next();
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
		Iterator<Reengineer> it = reengineers.iterator();
		while (it.hasNext() && !reengineered) {
			Reengineer semionReengineer = it.next();
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

//	@Override
//	public void registerRefactorer(SemionRefactorer semionRefactorer) {
//		this.semionRefactorer = semionRefactorer;
//	}
	
	@Override
	public boolean unbindReengineer(int reenginnerType) {
		boolean found = false;
		for(int i=0, j=reengineers.size(); i<j && !found; i++){
			Reengineer reengineer = reengineers.get(i);
			if(reengineer.getReengineerType() == reenginnerType){
				reengineers.remove(i);
				found = true;
			}
		}
		return found;
	}
	
	@Override
	public boolean unbindReengineer(Reengineer semionReengineer) {
		boolean found = false;
		for (int i = 0, j = reengineers.size(); i < j && !found; i++) {
			if (semionReengineer.equals(reengineers.get(i))) {
				reengineers.remove(i);
				found = true;
	}
	}
		return found;
	}

//	@Override
//	public void unregisterRefactorer() {
//		this.semionRefactorer = null;
//	}
	
}

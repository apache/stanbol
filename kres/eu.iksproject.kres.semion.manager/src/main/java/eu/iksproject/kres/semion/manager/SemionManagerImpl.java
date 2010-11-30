package eu.iksproject.kres.semion.manager;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.access.WeightedTcProvider;
import org.apache.clerezza.rdf.core.sparql.ParseException;
import org.apache.clerezza.rdf.core.sparql.QueryParser;
import org.apache.clerezza.rdf.core.sparql.ResultSet;
import org.apache.clerezza.rdf.core.sparql.SolutionMapping;
import org.apache.clerezza.rdf.core.sparql.query.Query;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.vocabulary.RDF;

import eu.iksproject.kres.api.manager.KReSONManager;
import eu.iksproject.kres.api.semion.DataSource;
import eu.iksproject.kres.api.semion.ReengineeringException;
import eu.iksproject.kres.api.semion.SemionManager;
import eu.iksproject.kres.api.semion.SemionReengineer;
import eu.iksproject.kres.api.semion.SemionRefactorer;
import eu.iksproject.kres.api.semion.util.SemionStructuredDataSource;
import eu.iksproject.kres.api.storage.NoSuchOntologyInStoreException;
import eu.iksproject.kres.api.storage.OntologyStorage;
import eu.iksproject.kres.ontologies.Semion;

/**
 * Concrete implementation of the {@link eu.iksproject.kres.api.semion.SemionManager} interface defined in the KReS
 * APIs.
 * 
 * @author andrea.nuzzolese
 *
 */

@Component(immediate = true, metatype = true)
@Service(SemionManager.class)
public class SemionManagerImpl implements SemionManager{

	private ArrayList<SemionReengineer> reengineers;
	private SemionRefactorer semionRefactorer;
	
	@Reference
	KReSONManager onManager;
	
	private final Logger log =
	    LoggerFactory.getLogger(getClass());

	public SemionManagerImpl() {
		reengineers = new ArrayList<SemionReengineer>();
	}
	
	/**
	 * @param semionReengineer {@link eu.iksproject.kres.api.semion.SemionReengineer}
	 * @return true if the reengineer is bound, false otherwise
	 */
	@Override
	public boolean bindReengineer(SemionReengineer semionReengineer) {
		boolean found = false;
		Iterator<SemionReengineer> it = reengineers.iterator();
		while(it.hasNext() && !found){
			SemionReengineer reengineer = it.next();
			if(reengineer.getReengineerType() == semionReengineer.getReengineerType()){
				found = true;
			}
		}
		
		if(!found){
			
			reengineers.add(semionReengineer);
			String info = "Reengineering Manager : "+reengineers.size()+" reengineers";
			log.info(info);
			System.out.println(info);
			return true;
		}
		else{
			System.out.println("Reengineer already existing");
			return false;
		}
		
	}

	public OWLOntology performReengineering(String graphNS, IRI outputIRI, DataSource dataSource) throws ReengineeringException {
		
		OWLOntology reengineeredOntology = null;
		
		boolean reengineered = false;
		Iterator<SemionReengineer> it = reengineers.iterator();
		while(it.hasNext() && !reengineered){
			SemionReengineer semionReengineer = it.next();
			if(semionReengineer.canPerformReengineering(dataSource)){
				System.out.println(semionReengineer.getClass().getCanonicalName()+" can perform the reengineering");
				reengineeredOntology = semionReengineer.reengineering(graphNS, outputIRI, dataSource);
				reengineered = true;
			}
			else{
				System.out.println(semionReengineer.getClass().getCanonicalName()+" cannot perform the reengineering");
			}
		}
		
		return reengineeredOntology;
	}
	
	public OWLOntology performSchemaReengineering(String graphNS, IRI outputIRI, DataSource dataSource) throws ReengineeringException {
		
		OWLOntology reengineeredSchemaOntology = null;
		
		boolean reengineered = false;
		Iterator<SemionReengineer> it = reengineers.iterator();
		while(it.hasNext() && !reengineered){
			SemionReengineer semionReengineer = it.next();
			if(semionReengineer.canPerformReengineering(dataSource)){
				reengineeredSchemaOntology = semionReengineer.schemaReengineering(graphNS, outputIRI, dataSource);
				if(reengineeredSchemaOntology == null){
					throw new ReengineeringException();
				}
				reengineered = true;
			}
		}
		
		return reengineeredSchemaOntology;
	}
	
	@Override
	public OWLOntology performDataReengineering(String graphNS, IRI outputIRI, DataSource dataSource, IRI schemaOntologyIRI) throws ReengineeringException, NoSuchOntologyInStoreException {
		
		OWLOntology reengineeredDataOntology = null;
		
		OntologyStorage ontologyStorage = onManager.getOntologyStore();
		
		OWLOntology schemaOntology = ontologyStorage.load(schemaOntologyIRI);
		
		if(schemaOntology == null){
			throw new NoSuchOntologyInStoreException(schemaOntologyIRI);
		}
		else{
		
			boolean reengineered = false;
			Iterator<SemionReengineer> it = reengineers.iterator();
			while(it.hasNext() && !reengineered){
				SemionReengineer semionReengineer = it.next();
				if(semionReengineer.canPerformReengineering(schemaOntology)){
					reengineeredDataOntology = semionReengineer.dataReengineering(graphNS, outputIRI, dataSource, schemaOntology);
					reengineered = true;
				}
			}
		}
		
		return reengineeredDataOntology;
	}
	
	@Override
	public OWLOntology performDataReengineering(String graphNS, IRI outputIRI, DataSource dataSource, OWLOntology schemaOntology) throws ReengineeringException {
		
		OWLOntology reengineeredDataOntology = null;
		
		boolean reengineered = false;
		Iterator<SemionReengineer> it = reengineers.iterator();
		while(it.hasNext() && !reengineered){
			SemionReengineer semionReengineer = it.next();
			if(semionReengineer.canPerformReengineering(schemaOntology)){
				reengineeredDataOntology = semionReengineer.dataReengineering(graphNS, outputIRI, dataSource, schemaOntology);
				reengineered = true;
			}
		}
		
		return reengineeredDataOntology;
	}
	
	

	@Override
	public boolean unbindReengineer(SemionReengineer semionReengineer) {
		boolean found = false;
		for(int i=0, j=reengineers.size(); i<j && !found; i++){
			if(semionReengineer.equals(reengineers.get(i))){
				reengineers.remove(i);
				found = true;
			}
		}
		return found;
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
	
	protected void activate(ComponentContext context){
		reengineers = new ArrayList<SemionReengineer>();
		log.info("Activated KReS Semion Reengineering Manager");
	}
	
	protected void deactivate(ComponentContext context){
		reengineers = null;
		log.info("Deactivated KReS Semion Reengineering Manager");
	}

	@Override
	public Collection<SemionReengineer> listReengineers() {
		return reengineers;
	}
	
	@Override
	public int countReengineers() {
		return reengineers.size();
	}

	@Override
	public void registerRefactorer(SemionRefactorer semionRefactorer) {
		this.semionRefactorer = semionRefactorer;
	}

	@Override
	public void unregisterRefactorer() {
		this.semionRefactorer = null;
	}
	
	@Override
	public SemionRefactorer getRegisteredRefactorer(){
		return semionRefactorer;
	}

}

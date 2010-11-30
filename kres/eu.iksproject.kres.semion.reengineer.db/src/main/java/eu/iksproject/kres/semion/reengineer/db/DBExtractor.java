package eu.iksproject.kres.semion.reengineer.db;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Observer;
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
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.vocabulary.RDF;

import eu.iksproject.kres.api.manager.DuplicateIDException;
import eu.iksproject.kres.api.manager.KReSONManager;
import eu.iksproject.kres.api.manager.ontology.OntologyInputSource;
import eu.iksproject.kres.api.manager.ontology.OntologyScope;
import eu.iksproject.kres.api.manager.ontology.OntologyScopeFactory;
import eu.iksproject.kres.api.manager.ontology.OntologySpace;
import eu.iksproject.kres.api.manager.ontology.OntologySpaceFactory;
import eu.iksproject.kres.api.manager.ontology.ScopeRegistry;
import eu.iksproject.kres.api.manager.ontology.SessionOntologySpace;
import eu.iksproject.kres.api.manager.ontology.UnmodifiableOntologySpaceException;
import eu.iksproject.kres.api.manager.session.KReSSession;
import eu.iksproject.kres.api.manager.session.KReSSessionManager;
import eu.iksproject.kres.api.semion.DataSource;
import eu.iksproject.kres.api.semion.ReengineeringException;
import eu.iksproject.kres.api.semion.SemionManager;
import eu.iksproject.kres.api.semion.SemionReengineer;
import eu.iksproject.kres.api.semion.settings.ConnectionSettings;
import eu.iksproject.kres.api.semion.util.ReengineerType;
import eu.iksproject.kres.api.semion.util.UnsupportedReengineerException;
import eu.iksproject.kres.ontologies.DBS_L1;
import eu.iksproject.kres.ontologies.DBS_L1_OWL;
import eu.iksproject.kres.api.semion.util.OntologyInputSourceDBS_L1;
import eu.iksproject.kres.semion.reengineer.db.connection.DatabaseConnection;
import eu.iksproject.kres.shared.transformation.JenaToClerezzaConverter;
import eu.iksproject.kres.shared.transformation.JenaToOwlConvert;

/**
 * The {@code DBExtractor} is an implementation of the {@link SemionReengineer} for relational databases.
 * 
 * @author andrea.nuzzolese
 *
 */

@Component(immediate = true, metatype = true)
@Service(SemionReengineer.class)
public class DBExtractor implements SemionReengineer {
	
	
	@Property(value = "localhost:8080")
    public static final String HOST_NAME_AND_PORT = "host.name.port";
	
	@Property(value = "/db-schema-reengineering-session-space")
    public static final String DB_REENGINEERING_SESSION_SPACE = "http://kres.iks-project.eu/space/reengineering/db";
	
	@Property(value = "/db-schema-reengineering-ontology-space")
    public static final String DB_SCHEMA_REENGINEERING_ONTOLOGY_SPACE = "eu.iksproject.kres.semion.reengineer.ontology.space.db";
	
	@Property(value = "/db-data-reengineering-session-space")
    public static final String DB_DATA_REENGINEERING_SESSION_SPACE = "eu.iksproject.kres.semion.reengineer.space.db.data";
	
	
	@Property(value = "/db-schema-reengineering-session")
    public static final String DB_SCHEMA_REENGINEERING_SESSION = "eu.iksproject.kres.semion.reengineer.db.schema";
	
	@Property(value = "/db-data-reengineering-session")
    public static final String DB_DATA_REENGINEERING_SESSION = "eu.iksproject.kres.semion.reengineer.db.data";
	
	@Property(value = "db_reengineering")
    public static final String REENGINEERING_SCOPE = "db.reengineering.scope";
	
	@Reference
	SemionManager reengineeringManager;
	
	@Reference
	WeightedTcProvider weightedTcProvider;
	
	@Reference
	TcManager tcManager;
	
	@Reference
	KReSONManager onManager;
	
	private final Logger log =
	    LoggerFactory.getLogger(getClass());
	
	protected OntologyScope scope;

	private IRI reengineeringScopeIRI;
	private IRI reengineeringSpaceIRI;
	
	private IRI kReSSessionID;
	
	String databaseURI;
	ConnectionSettings connectionSettings;
	MGraph schemaGraph;
	
	
	
	
	
	/**
	 * 
	 * Create a new {@link DBExtractor} that is formally a {@link SemionReengineer}.
	 * 
	 */
	public DBExtractor() {
		
	}
	
	
	/**
	 * Create a new {@link DBExtractor} that is formally a {@link SemionReengineer}.
	 * 
	 * @param databaseURI {@link String}
	 * @param schemaGraph {@link MGraph}
	 * @param connectionSettings {@link ConnectionSettings}
	 */
	public DBExtractor(String databaseURI, MGraph schemaGraph, ConnectionSettings connectionSettings) {
		this.schemaGraph = schemaGraph;
		this.databaseURI = databaseURI;
		this.connectionSettings = connectionSettings;
	}
	
	
	
	protected void activate(ComponentContext context){
		
		String reengineeringScopeID = (String) context.getProperties().get(REENGINEERING_SCOPE);
		
		String hostNameAndPort = (String) context.getProperties().get(HOST_NAME_AND_PORT);
		
		hostNameAndPort = "http://" + hostNameAndPort;
		
		reengineeringScopeIRI = IRI.create(hostNameAndPort + "/kres/ontology/" + reengineeringScopeID);
		reengineeringSpaceIRI = IRI.create(DB_REENGINEERING_SESSION_SPACE);
		
		
		reengineeringManager.bindReengineer(this);
		
		KReSSessionManager kReSSessionManager = onManager.getSessionManager();
		KReSSession kReSSession = kReSSessionManager.createSession();
		
		kReSSessionID = kReSSession.getID();
		
		
		OntologyScopeFactory ontologyScopeFactory = onManager.getOntologyScopeFactory();
		
		ScopeRegistry scopeRegistry = onManager.getScopeRegistry();
		
		OntologySpaceFactory ontologySpaceFactory = onManager.getOntologySpaceFactory();
		
		scope = null;
		try {
			log.info("Semion DBExtractor : created scope with IRI "+REENGINEERING_SCOPE);
			IRI iri = IRI.create(DBS_L1.URI);
			OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
			OWLOntology owlOntology = ontologyManager.createOntology(iri);
			
			System.out.println("Created ONTOLOGY OWL");
		
			scope = ontologyScopeFactory.createOntologyScope(reengineeringScopeIRI, new OntologyInputSourceDBS_L1());
			
			//scope.setUp();
			
			scopeRegistry.registerScope(scope);
			
		} catch (DuplicateIDException e) {
			log.info("Semion DBExtractor : already existing scope for IRI "+REENGINEERING_SCOPE);
			scope = scopeRegistry.getScope(reengineeringScopeIRI);
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			log.error("Semion DBExtractor : No OntologyInputSource for ONManager.");
		}
		
		if(scope != null){
			scope.addSessionSpace(ontologySpaceFactory.createSessionOntologySpace(reengineeringSpaceIRI), kReSSession.getID());
			
			scopeRegistry.setScopeActive(reengineeringScopeIRI, true);
		}
		
		log.info("Activated KReS Semion RDB Reengineer");
	}
	
	protected void deactivate(ComponentContext context){
		reengineeringManager.unbindReengineer(this);
		log.info("Deactivated KReS Semion RDB Reengineer");
	}

	@Override
	public int getReengineerType() {
		return ReengineerType.RDB;
	}

	@Override
	public boolean canPerformReengineering(DataSource dataSource) {
		if(dataSource.getDataSourceType() == getReengineerType()){
			return true;
		}
		else{
			return false;
		}
	}

	@Override
	public boolean canPerformReengineering(int dataSourceType) {
		if(dataSourceType == getReengineerType()){
			return true;
		}
		else{
			return false;
		}
	}
	
	private OntologyScope getScope() {
		OntologyScope ontologyScope = null;
		
		ScopeRegistry scopeRegistry = onManager.getScopeRegistry();
		
		if(scopeRegistry.isScopeActive(reengineeringScopeIRI)){
			ontologyScope = scopeRegistry.getScope(reengineeringScopeIRI);
		}
		
		return ontologyScope;
	}


	@Override
	public boolean canPerformReengineering(String dataSourceType) throws UnsupportedReengineerException {
		return canPerformReengineering(ReengineerType.getType(dataSourceType));
	}


	@Override
	public boolean canPerformReengineering(OWLOntology schemaOntology) {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public OWLOntology dataReengineering(String graphNS, IRI outputIRI, DataSource dataSource,
			OWLOntology schemaOntology) throws ReengineeringException {
		
		SemionDBDataTransformer semionDBDataTransformer = new SemionDBDataTransformer(onManager, schemaOntology);
		return semionDBDataTransformer.transformData(graphNS, outputIRI);
		
	}


	@Override
	public OWLOntology reengineering(String graphNS, IRI outputIRI, DataSource dataSource)
			throws ReengineeringException {
		IRI schemaIRI;
		if(outputIRI != null){
			schemaIRI = IRI.create(outputIRI.toString() + "/schema");
		}
		else{
			schemaIRI = IRI.create("/schema");
		}
		OWLOntology schemaOntology = schemaReengineering(graphNS+"/schema", schemaIRI, dataSource);
		
		return dataReengineering(graphNS, outputIRI, dataSource, schemaOntology);
	}


	@Override
	public OWLOntology schemaReengineering(String graphNS, IRI outputIRI, DataSource dataSource) {
		OWLOntology schemaOntology = null;
		
		if(outputIRI != null){
			log.info("Semion DBExtractor : starting to generate RDF graph with URI "+outputIRI.toString()+" of a db schema ");
		}
		else{
			log.info("Semion DBExtractor : starting to generate RDF graph of a db schema ");
		}
		
		OntologyScope reengineeringScope = getScope();
		if(reengineeringScope != null){
			ConnectionSettings connectionSettings = (ConnectionSettings) dataSource.getDataSource();
			SemionDBSchemaGenerator schemaGenerator = new SemionDBSchemaGenerator(outputIRI, connectionSettings);
			
			System.out.println("OWL MANAGER IN SEMION: "+onManager);
			OWLOntologyManager ontologyManager = onManager.getOwlCacheManager();
			OWLDataFactory dataFactory = onManager.getOwlFactory();
			schemaOntology = schemaGenerator.getSchema(ontologyManager, dataFactory);
			
			if(outputIRI != null){
				log.info("Created graph with URI "+outputIRI.toString()+" of DB Schema.");
			}
			else{
				log.info("Created graph of DB Schema.");
			}
			
			
			
		}
		return schemaOntology;
	}
}

package eu.iksproject.kres.semion.reengineer.db;

import java.io.IOException;
import java.util.Dictionary;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.access.WeightedTcProvider;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.kres.api.manager.DuplicateIDException;
import eu.iksproject.kres.api.manager.KReSONManager;
import eu.iksproject.kres.api.manager.ontology.OntologyScope;
import eu.iksproject.kres.api.manager.ontology.OntologyScopeFactory;
import eu.iksproject.kres.api.manager.ontology.OntologySpaceFactory;
import eu.iksproject.kres.api.manager.ontology.ScopeRegistry;
import eu.iksproject.kres.api.manager.session.KReSSession;
import eu.iksproject.kres.api.manager.session.KReSSessionManager;
import eu.iksproject.kres.api.semion.DataSource;
import eu.iksproject.kres.api.semion.ReengineeringException;
import eu.iksproject.kres.api.semion.SemionManager;
import eu.iksproject.kres.api.semion.SemionReengineer;
import eu.iksproject.kres.api.semion.settings.ConnectionSettings;
import eu.iksproject.kres.api.semion.util.OntologyInputSourceDBS_L1;
import eu.iksproject.kres.api.semion.util.ReengineerType;
import eu.iksproject.kres.api.semion.util.UnsupportedReengineerException;
import eu.iksproject.kres.ontologies.DBS_L1;

/**
 * The {@code DBExtractor} is an implementation of the {@link SemionReengineer}
 * for relational databases.
 * 
 * @author andrea.nuzzolese
 *
 */

@Component(immediate = true, metatype = true)
@Service(SemionReengineer.class)
public class DBExtractor implements SemionReengineer {
	
	public static final String _DB_DATA_REENGINEERING_SESSION_DEFAULT = "/db-data-reengineering-session";
	public static final String _DB_DATA_REENGINEERING_SESSION_SPACE_DEFAULT = "/db-data-reengineering-session-space";
	public static final String _DB_REENGINEERING_SESSION_SPACE_DEFAULT = "/db-schema-reengineering-session-space";
	public static final String _DB_SCHEMA_REENGINEERING_ONTOLOGY_SPACE_DEFAULT = "/db-schema-reengineering-ontology-space";
	public static final String _DB_SCHEMA_REENGINEERING_SESSION_DEFAULT = "/db-schema-reengineering-session";
	public static final String _HOST_NAME_AND_PORT_DEFAULT = "localhost:8080";
	public static final String _REENGINEERING_SCOPE_DEFAULT = "db_reengineering";
	
	@Property(value = _DB_DATA_REENGINEERING_SESSION_DEFAULT)
	public static final String DB_DATA_REENGINEERING_SESSION = "eu.iksproject.kres.semion.reengineer.db.data";

	@Property(value = _DB_DATA_REENGINEERING_SESSION_SPACE_DEFAULT)
	public static final String DB_DATA_REENGINEERING_SESSION_SPACE = "eu.iksproject.kres.semion.reengineer.space.db.data";
	
	@Property(value = _DB_REENGINEERING_SESSION_SPACE_DEFAULT)
    public static final String DB_REENGINEERING_SESSION_SPACE = "http://kres.iks-project.eu/space/reengineering/db";
	
	@Property(value = _DB_SCHEMA_REENGINEERING_ONTOLOGY_SPACE_DEFAULT)
    public static final String DB_SCHEMA_REENGINEERING_ONTOLOGY_SPACE = "eu.iksproject.kres.semion.reengineer.ontology.space.db";
	
	@Property(value = _DB_SCHEMA_REENGINEERING_SESSION_DEFAULT)
    public static final String DB_SCHEMA_REENGINEERING_SESSION = "eu.iksproject.kres.semion.reengineer.db.schema";
	
	@Property(value = _HOST_NAME_AND_PORT_DEFAULT)
	public static final String HOST_NAME_AND_PORT = "host.name.port";
	
	@Property(value = _REENGINEERING_SCOPE_DEFAULT)
    public static final String REENGINEERING_SCOPE = "db.reengineering.scope";
	
	ConnectionSettings connectionSettings;
	
	String databaseURI;
	
	private IRI kReSSessionID;

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Reference
	KReSONManager onManager;
	
	@Reference
	SemionManager reengineeringManager;

	private IRI reengineeringScopeIRI;
	
	private IRI reengineeringSpaceIRI;
	
	MGraph schemaGraph;
	protected OntologyScope scope;
	
	@Reference
	TcManager tcManager;
	
	@Reference
	WeightedTcProvider weightedTcProvider;
	
	/**
	 * This default constructor is <b>only</b> intended to be used by the OSGI
	 * environment with Service Component Runtime support.
	 * <p>
	 * DO NOT USE to manually create instances - the DBExtractor instances do
	 * need to be configured! YOU NEED TO USE
	 * {@link #DBExtractor(KReSONManager)} or its overloads, to parse the
	 * configuration and then initialise the rule store if running outside a
	 * OSGI environment.
	 */
	public DBExtractor() {
	
	}
	
	/**
	 * 
	 * Create a new {@link DBExtractor} that is formally a
	 * {@link SemionReengineer}.
	 * 
	 */
	public DBExtractor(SemionManager reengineeringManager,
			KReSONManager onManager, TcManager tcManager,
			WeightedTcProvider weightedTcProvider,
			Dictionary<String, Object> configuration) {
		this();
		this.reengineeringManager = reengineeringManager;
		this.onManager = onManager;
		this.tcManager = tcManager;
		this.weightedTcProvider = weightedTcProvider;
		activate(configuration);
	}
	
	/**
	 * Create a new {@link DBExtractor} that is formally a
	 * {@link SemionReengineer}.
	 * 
	 * @param databaseURI
	 *            {@link String}
	 * @param schemaGraph
	 *            {@link MGraph}
	 * @param connectionSettings
	 *            {@link ConnectionSettings}
	 */
	public DBExtractor(SemionManager reengineeringManager,
			KReSONManager onManager, TcManager tcManager,
			WeightedTcProvider weightedTcProvider,
			Dictionary<String, Object> configuration, String databaseURI,
			MGraph schemaGraph, ConnectionSettings connectionSettings) {
		// Copy code from overloaded constructor, except that the call to
		// activate() goes at the end.
		this();
		this.reengineeringManager = reengineeringManager;
		this.onManager = onManager;
		this.tcManager = tcManager;
		this.weightedTcProvider = weightedTcProvider;
		this.databaseURI = databaseURI;
		this.schemaGraph = schemaGraph;
		this.connectionSettings = connectionSettings;
		activate(configuration);
	}
	
	/**
	 * Used to configure an instance within an OSGi container.
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	@Activate
	protected void activate(ComponentContext context) throws IOException {
		log.info("in " + DBExtractor.class + " activate with context "
				+ context);
		if (context == null) {
			throw new IllegalStateException("No valid" + ComponentContext.class
					+ " parsed in activate!");
		}
		activate((Dictionary<String, Object>) context.getProperties());
	}

	protected void activate(Dictionary<String, Object> configuration) {

		String reengineeringScopeID = (String) configuration
				.get(REENGINEERING_SCOPE);
		if (reengineeringScopeID == null)
			reengineeringScopeID = _REENGINEERING_SCOPE_DEFAULT;
		String hostNameAndPort = (String) configuration.get(HOST_NAME_AND_PORT);
		if (hostNameAndPort == null)
			hostNameAndPort = _HOST_NAME_AND_PORT_DEFAULT;
		// TODO: Manage the other properties
		
		hostNameAndPort = "http://" + hostNameAndPort;
		
		reengineeringScopeIRI = IRI.create(hostNameAndPort + "/kres/ontology/"
				+ reengineeringScopeID);
		reengineeringSpaceIRI = IRI.create(DB_REENGINEERING_SESSION_SPACE);
		
		reengineeringManager.bindReengineer(this);
		
		KReSSessionManager kReSSessionManager = onManager.getSessionManager();
		KReSSession kReSSession = kReSSessionManager.createSession();
		
		kReSSessionID = kReSSession.getID();
		
		OntologyScopeFactory ontologyScopeFactory = onManager
				.getOntologyScopeFactory();
		
		ScopeRegistry scopeRegistry = onManager.getScopeRegistry();
		
		OntologySpaceFactory ontologySpaceFactory = onManager
				.getOntologySpaceFactory();
		
		scope = null;
		try {
			log.info("Semion DBExtractor : created scope with IRI "
					+ REENGINEERING_SCOPE);
			IRI iri = IRI.create(DBS_L1.URI);
			OWLOntologyManager ontologyManager = OWLManager
					.createOWLOntologyManager();
			OWLOntology owlOntology = ontologyManager.createOntology(iri);
			
			System.out.println("Created ONTOLOGY OWL");
		
			scope = ontologyScopeFactory.createOntologyScope(
					reengineeringScopeIRI, new OntologyInputSourceDBS_L1());
			
			//scope.setUp();
			
			scopeRegistry.registerScope(scope);
			
		} catch (DuplicateIDException e) {
			log.info("Semion DBExtractor : already existing scope for IRI "
					+ REENGINEERING_SCOPE);
			scope = scopeRegistry.getScope(reengineeringScopeIRI);
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			log
					.error("Semion DBExtractor : No OntologyInputSource for ONManager.");
		}
		
		if(scope != null){
			scope.addSessionSpace(ontologySpaceFactory
					.createSessionOntologySpace(reengineeringSpaceIRI),
					kReSSession.getID());
			
			scopeRegistry.setScopeActive(reengineeringScopeIRI, true);
		}
		
		log.info("Activated KReS Semion RDB Reengineer");
	}
	
	@Override
	public boolean canPerformReengineering(DataSource dataSource) {
		if(dataSource.getDataSourceType() == getReengineerType()){
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean canPerformReengineering(int dataSourceType) {
		if(dataSourceType == getReengineerType()){
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public boolean canPerformReengineering(OWLOntology schemaOntology) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canPerformReengineering(String dataSourceType)
			throws UnsupportedReengineerException {
		return canPerformReengineering(ReengineerType.getType(dataSourceType));
	}

	@Override
	public OWLOntology dataReengineering(String graphNS, IRI outputIRI,
			DataSource dataSource, OWLOntology schemaOntology)
			throws ReengineeringException {

		SemionDBDataTransformer semionDBDataTransformer = new SemionDBDataTransformer(
				onManager, schemaOntology);
		return semionDBDataTransformer.transformData(graphNS, outputIRI);

	}

	@Deactivate
	protected void deactivate(ComponentContext context) {
		log.info("in " + DBExtractor.class + " deactivate with context "
				+ context);
		reengineeringManager.unbindReengineer(this);
	}

	@Override
	public int getReengineerType() {
		return ReengineerType.RDB;
	}
		
	private OntologyScope getScope() {
		OntologyScope ontologyScope = null;
		
		ScopeRegistry scopeRegistry = onManager.getScopeRegistry();

		if (scopeRegistry.isScopeActive(reengineeringScopeIRI)) {
			ontologyScope = scopeRegistry.getScope(reengineeringScopeIRI);
	}

		return ontologyScope;
	}

	@Override
	public OWLOntology reengineering(String graphNS, IRI outputIRI,
			DataSource dataSource) throws ReengineeringException {
		IRI schemaIRI;
		if(outputIRI != null){
			schemaIRI = IRI.create(outputIRI.toString() + "/schema");
		} else {
			schemaIRI = IRI.create("/schema");
		}
		OWLOntology schemaOntology = schemaReengineering(graphNS + "/schema",
				schemaIRI, dataSource);
		
		return dataReengineering(graphNS, outputIRI, dataSource, schemaOntology);
	}

	@Override
	public OWLOntology schemaReengineering(String graphNS, IRI outputIRI,
			DataSource dataSource) {
		OWLOntology schemaOntology = null;
		
		if(outputIRI != null){
			log
					.info("Semion DBExtractor : starting to generate RDF graph with URI "
							+ outputIRI.toString() + " of a db schema ");
		} else {
			log
					.info("Semion DBExtractor : starting to generate RDF graph of a db schema ");
		}
		
		OntologyScope reengineeringScope = getScope();
		if(reengineeringScope != null){
			ConnectionSettings connectionSettings = (ConnectionSettings) dataSource
					.getDataSource();
			SemionDBSchemaGenerator schemaGenerator = new SemionDBSchemaGenerator(
					outputIRI, connectionSettings);
			
			System.out.println("OWL MANAGER IN SEMION: "+onManager);
			OWLOntologyManager ontologyManager = onManager.getOwlCacheManager();
			OWLDataFactory dataFactory = onManager.getOwlFactory();
			schemaOntology = schemaGenerator.getSchema(ontologyManager,
					dataFactory);
			
			if(outputIRI != null){
				log.info("Created graph with URI " + outputIRI.toString()
						+ " of DB Schema.");
			} else {
				log.info("Created graph of DB Schema.");
			}
			
		}
		return schemaOntology;
	}
}

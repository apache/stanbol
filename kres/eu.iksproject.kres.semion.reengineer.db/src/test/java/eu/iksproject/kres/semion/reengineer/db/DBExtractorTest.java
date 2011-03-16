package eu.iksproject.kres.semion.reengineer.db;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.stanbol.ontologymanager.ontonet.api.KReSONManager;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;

import eu.iksproject.kres.semion.manager.SemionManagerImpl;

public class DBExtractorTest {

	static DBExtractor dbExtractor;
	static String graphNS;
	static KReSONManager onManager;
	static IRI outputIRI;
	
	@BeforeClass
	public static void setupClass() {
		Dictionary<String, Object> emptyConf = new Hashtable<String, Object>();
		onManager = new ONManager(null, emptyConf);
		dbExtractor = new DBExtractor(new SemionManagerImpl(onManager),
				onManager, new TcManager(), null, emptyConf);
		graphNS = "http://kres.iks-project.eu/reengineering/test";
		outputIRI = IRI.create(graphNS);
	}
	
	@Before
	public void setup() {
		
	}
		
	@Before
	public void tearDown() {
		
	}
	
	@Test
	public void testDataReengineering(){
//		graphNS = "http://kres.iks-project.eu/reengineering/test";
//		outputIRI = IRI.create(graphNS);
//		try {
//			OWLOntology ontology = dbExtractor.dataReengineering(graphNS,
//					outputIRI, null, dbExtractor.schemaReengineering(graphNS,
//							outputIRI, null));
//		} catch (ReengineeringException e) {
//			fail("Some errors occur with dataReengineering of DBExtractor.");
//		}
	}
	
	@Test
	public void testReengineering(){
//		graphNS = "http://kres.iks-project.eu/reengineering/test";
//		outputIRI = IRI.create(graphNS);
//		try {
//			OWLOntology ontology = dbExtractor.reengineering(graphNS,
//					outputIRI, null);
//		} catch (ReengineeringException e) {
//			fail("Some errors occur with reengineering of DBExtractor.");
//		}
	}

	@Test
	public void testSchemaReengineering() {
//		OWLOntology ontology = dbExtractor.schemaReengineering(graphNS,
//				outputIRI, null);
	}
}

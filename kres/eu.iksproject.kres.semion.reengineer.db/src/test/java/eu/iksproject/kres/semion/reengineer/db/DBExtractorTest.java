package eu.iksproject.kres.semion.reengineer.db;

import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import eu.iksproject.kres.api.manager.KReSONManager;
import eu.iksproject.kres.api.manager.ontology.OntologyIndex;
import eu.iksproject.kres.api.manager.ontology.OntologyScopeFactory;
import eu.iksproject.kres.api.manager.ontology.OntologySpaceFactory;
import eu.iksproject.kres.api.manager.ontology.ScopeRegistry;
import eu.iksproject.kres.api.manager.registry.KReSRegistryLoader;
import eu.iksproject.kres.api.manager.session.KReSSessionManager;
import eu.iksproject.kres.api.semion.ReengineeringException;
import eu.iksproject.kres.api.storage.OntologyStorage;


public class DBExtractorTest {

	static DBExtractor dbExtractor;
	static KReSONManager onManager;
	static String graphNS;
	static IRI outputIRI;
	
	@BeforeClass
	public static void setup(){
		
		onManager = new KReSONManager() {
			
			@Override
			public String[] getUrisToActivate() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public KReSSessionManager getSessionManager() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public ScopeRegistry getScopeRegistry() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public KReSRegistryLoader getRegistryLoader() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public OWLDataFactory getOwlFactory() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public OWLOntologyManager getOwlCacheManager() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public OntologyStorage getOntologyStore() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public OntologySpaceFactory getOntologySpaceFactory() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public OntologyScopeFactory getOntologyScopeFactory() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public OntologyIndex getOntologyIndex() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String getKReSNamespace() {
				// TODO Auto-generated method stub
				return null;
			}
		};
		
		dbExtractor = new DBExtractor();
		graphNS = "http://kres.iks-project.eu/reengineering/test";
		outputIRI = IRI.create(graphNS);
	}
	
	@Test
	public void testSchemaReengineering(){
		
		
		OWLOntology ontology = dbExtractor.schemaReengineering(graphNS, outputIRI, null);
	}
	
	@Test
	public void testDataReengineering(){
		
		graphNS = "http://kres.iks-project.eu/reengineering/test";
		outputIRI = IRI.create(graphNS);
		try {
			OWLOntology ontology = dbExtractor.dataReengineering(graphNS, outputIRI, null, dbExtractor.schemaReengineering(graphNS, outputIRI, null));
		} catch (ReengineeringException e) {
			fail("Some errors occur with dataReengineering of DBExtractor.");
		}
	}
	
	@Test
	public void testReengineering(){
		
		graphNS = "http://kres.iks-project.eu/reengineering/test";
		outputIRI = IRI.create(graphNS);
		try {
			OWLOntology ontology = dbExtractor.reengineering(graphNS, outputIRI, null);
		} catch (ReengineeringException e) {
			fail("Some errors occur with reengineering of DBExtractor.");
		}
	}
}

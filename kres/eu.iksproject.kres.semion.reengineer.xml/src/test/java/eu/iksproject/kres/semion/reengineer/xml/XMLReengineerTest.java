package eu.iksproject.kres.semion.reengineer.xml;

import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

import eu.iksproject.kres.api.manager.KReSONManager;
import eu.iksproject.kres.api.semion.DataSource;
import eu.iksproject.kres.api.semion.SemionReengineer;
import eu.iksproject.kres.api.semion.util.ReengineerType;
import eu.iksproject.kres.manager.ONManager;
import eu.iksproject.kres.semion.manager.SemionManagerImpl;

public class XMLReengineerTest {
	
	static DataSource dataSource;
	static String graphNS;
	static IRI outputIRI;
	static SemionReengineer xmlExtractor;
	
	@BeforeClass
	public static void setupClass() {
		graphNS = "http://kres.iks-project.eu/reengineering/test";
		outputIRI = IRI.create(graphNS);
		dataSource = new DataSource() {
			
			@Override
			public Object getDataSource() {
				InputStream xmlStream = this.getClass().getResourceAsStream(
						"/META-INF/test/weather.xml");
				return xmlStream;
			}
			
			@Override
			public int getDataSourceType() {
				return ReengineerType.XML;
			}
			
			@Override
			public String getID() {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}
	
	@Test
	public void dataReengineeringTest() throws Exception {
		OWLOntology schemaOntology = xmlExtractor.schemaReengineering(graphNS,
				outputIRI, dataSource);
		xmlExtractor.dataReengineering(graphNS, outputIRI, dataSource,
				schemaOntology);
	}
	
	@Test
	public void reengineeringTest() throws Exception {
		xmlExtractor.reengineering(graphNS, outputIRI, dataSource);
	}
	
	@Test
	public void schemaReengineeringTest() throws Exception {
		xmlExtractor.schemaReengineering(graphNS, outputIRI, dataSource);
		}

	@Before
	public void setup() {
		Dictionary<String, Object> emptyConf = new Hashtable<String, Object>();
		KReSONManager onManager = new ONManager(null, emptyConf);
		xmlExtractor = new XMLExtractor(new SemionManagerImpl(onManager),
				onManager, emptyConf);
	}

	@Before
	public void tearDown() {
		xmlExtractor = null;
	}

}

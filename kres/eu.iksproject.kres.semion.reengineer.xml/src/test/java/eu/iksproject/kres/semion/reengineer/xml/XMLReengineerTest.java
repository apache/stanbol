package eu.iksproject.kres.semion.reengineer.xml;

import static org.junit.Assert.fail;

import java.io.InputStream;

import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

import eu.iksproject.kres.api.semion.DataSource;
import eu.iksproject.kres.api.semion.ReengineeringException;
import eu.iksproject.kres.api.semion.util.ReengineerType;


public class XMLReengineerTest {
	
	static String graphNS;
	static IRI outputIRI;
	static DataSource dataSource;
	
	@BeforeClass
	public static void setup(){
		graphNS = "http://kres.iks-project.eu/reengineering/test";
		outputIRI = IRI.create(graphNS);
		dataSource = new DataSource() {
			
			@Override
			public String getID() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public int getDataSourceType() {
				return ReengineerType.XML;
			}
			
			@Override
			public Object getDataSource() {
				InputStream xmlStream = this.getClass().getResourceAsStream("/META-INF/test/weather.xml");
				return xmlStream;
			}
		};
	}
	
	@Test
	public void scemaReengineeringTest(){
		XMLExtractor xmlExtractor = new XMLExtractor();
		xmlExtractor.schemaReengineering(graphNS, outputIRI, dataSource);
	}
	
	@Test
	public void dataReengineeringTest(){
		XMLExtractor xmlExtractor = new XMLExtractor();
		OWLOntology schemaOntology = xmlExtractor.schemaReengineering(graphNS, outputIRI, dataSource);
		
		try {
			xmlExtractor.dataReengineering(graphNS, outputIRI, dataSource, schemaOntology);
		} catch (ReengineeringException e) {
			fail("Some errors occur with dataReengineeringTest of XMLExtractor.");
		}
	}
	
	@Test
	public void reengineeringTest(){
		XMLExtractor xmlExtractor = new XMLExtractor();
		try {
			xmlExtractor.reengineering(graphNS, outputIRI, dataSource);
		} catch (ReengineeringException e) {
			fail("Some errors occur with reengineeringTest of XMLExtractor.");
		}
	}

}

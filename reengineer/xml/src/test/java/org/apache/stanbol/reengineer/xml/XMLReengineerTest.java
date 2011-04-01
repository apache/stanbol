package org.apache.stanbol.reengineer.xml;

import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.access.WeightedTcProvider;
import org.apache.clerezza.rdf.core.sparql.QueryEngine;
import org.apache.clerezza.rdf.jena.sparql.JenaSparqlEngine;
import org.apache.clerezza.rdf.simple.storage.SimpleTcProvider;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerImpl;
import org.apache.stanbol.reengineer.base.api.DataSource;
import org.apache.stanbol.reengineer.base.api.Reengineer;
import org.apache.stanbol.reengineer.base.api.util.ReengineerType;
import org.apache.stanbol.reengineer.base.impl.ReengineerManagerImpl;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

public class XMLReengineerTest {
	
	static DataSource dataSource;
	static String graphNS;
	static IRI outputIRI;
	static Reengineer xmlExtractor;
	
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
		xmlExtractor.dataReengineering(graphNS, IRI.create(outputIRI.toString()+"_new"), dataSource,
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
		
	      class SpecialTcManager extends TcManager {
	            public SpecialTcManager(QueryEngine qe, WeightedTcProvider wtcp) {
	                super();
	                bindQueryEngine(qe);
	                bindWeightedTcProvider(wtcp);
	            }
	        }

	        QueryEngine qe = new JenaSparqlEngine();
	        WeightedTcProvider wtcp = new SimpleTcProvider();
	        TcManager tcm = new SpecialTcManager(qe, wtcp);

	        // Two different ontology storagez, the same sparql engine and tcprovider
		ONManager onManager = new ONManagerImpl(tcm, wtcp ,emptyConf);
		xmlExtractor = new XMLExtractor(new ReengineerManagerImpl(tcm, wtcp),
				onManager, emptyConf);
	}

	@Before
	public void tearDown() {
		xmlExtractor = null;
	}

}

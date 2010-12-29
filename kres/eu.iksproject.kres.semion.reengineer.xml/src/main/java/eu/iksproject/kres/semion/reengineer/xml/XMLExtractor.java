package eu.iksproject.kres.semion.reengineer.xml;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologySetProvider;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.OWLOntologyMerger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
import eu.iksproject.kres.api.semion.util.OntologyInputSourceOXML;
import eu.iksproject.kres.api.semion.util.ReengineerType;
import eu.iksproject.kres.api.semion.util.SemionUriRefGenerator;
import eu.iksproject.kres.api.semion.util.UnsupportedReengineerException;
import eu.iksproject.kres.ontologies.Semion_OWL;
import eu.iksproject.kres.ontologies.XML_OWL;
import eu.iksproject.kres.ontologies.XSD_OWL;

/**
 * The {@code XMLExtractor} extends of the {@link XSDExtractor} that implements
 * the {@link SemionReengineer} for XML data sources.
 * 
 * @author andrea.nuzzolese
 *
 */

@Component(immediate = true, metatype = true)
@Service(SemionReengineer.class)
public class XMLExtractor extends SemionUriRefGenerator implements
		SemionReengineer {

	public static final String _HOST_NAME_AND_PORT_DEFAULT = "localhost:8080";
	public static final String _REENGINEERING_SCOPE_DEFAULT = "xml_reengineering";
	public static final String _XML_REENGINEERING_SESSION_SPACE_DEFAULT = "/xml-reengineering-session-space";
	
	@Property(value = _HOST_NAME_AND_PORT_DEFAULT)
    public static final String HOST_NAME_AND_PORT = "host.name.port";
	
	@Property(value = _REENGINEERING_SCOPE_DEFAULT)
	public static final String REENGINEERING_SCOPE = "xml.reengineering.scope";

	@Property(value = _XML_REENGINEERING_SESSION_SPACE_DEFAULT)
    public static final String XML_REENGINEERING_SESSION_SPACE = "http://kres.iks-project.eu/space/reengineering/db";
	
	private IRI kReSSessionID;
	
	public final Logger log = LoggerFactory.getLogger(getClass());
	
	@Reference
	KReSONManager onManager;
	
	@Reference
	SemionManager reengineeringManager;
	
	private OntologyScope scope;
	private IRI scopeIRI;
	private IRI spaceIRI;
	
	/**
	 * This default constructor is <b>only</b> intended to be used by the OSGI
	 * environment with Service Component Runtime support.
	 * <p>
	 * DO NOT USE to manually create instances - the XMLExtractor instances do
	 * need to be configured! YOU NEED TO USE
	 * {@link #XMLExtractor(KReSONManager)} or its overloads, to parse the
	 * configuration and then initialise the rule store if running outside a
	 * OSGI environment.
	 */
	public XMLExtractor() {
		
	}
	
	public XMLExtractor(SemionManager reengineeringManager,
			KReSONManager onManager, Dictionary<String, Object> configuration) {
		this();
		this.reengineeringManager = reengineeringManager;
		this.onManager = onManager;
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
		log.info("in " + XMLExtractor.class + " activate with context "
				+ context);
		if (context == null) {
			throw new IllegalStateException("No valid" + ComponentContext.class
					+ " parsed in activate!");
		}
		activate((Dictionary<String, Object>) context.getProperties());
	}

	protected void activate(Dictionary<String, Object> configuration) {
		String scopeID = (String) configuration.get(REENGINEERING_SCOPE);
		if (scopeID == null)
			scopeID = _REENGINEERING_SCOPE_DEFAULT;
		String hostPort = (String) configuration.get(HOST_NAME_AND_PORT);
		if (hostPort == null)
			hostPort = _HOST_NAME_AND_PORT_DEFAULT;
		// TODO: Manage the other properties
					
		spaceIRI = IRI.create(XML_REENGINEERING_SESSION_SPACE);
		scopeIRI = IRI.create("http://" + hostPort + "/kres/ontology/"
				+ scopeID);
					
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
			log.info("Semion XMLEtractor : created scope with IRI "
					+ REENGINEERING_SCOPE);
			IRI iri = IRI.create(XML_OWL.URI);
			OWLOntologyManager ontologyManager = OWLManager
					.createOWLOntologyManager();
			OWLOntology owlOntology = ontologyManager.createOntology(iri);
		
			System.out.println("Created ONTOLOGY OWL");
		
			scope = ontologyScopeFactory.createOntologyScope(scopeIRI,
					new OntologyInputSourceOXML());
			// scope.setUp();
			
			scopeRegistry.registerScope(scope);
		} catch (DuplicateIDException e) {
			log.info("Semion DBExtractor : already existing scope for IRI "
					+ REENGINEERING_SCOPE);
			scope = onManager.getScopeRegistry().getScope(scopeIRI);
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			log
					.error(
							"Semion XMLExtractor : No OntologyInputSource for ONManager.",
							e);
		}

		if (scope != null) {
			scope.addSessionSpace(ontologySpaceFactory
					.createSessionOntologySpace(spaceIRI), kReSSession.getID());

			scopeRegistry.setScopeActive(scopeIRI, true);
		}
				
		log.info("Activated KReS Semion RDB Reengineer");
			}
			
			@Override
	public boolean canPerformReengineering(DataSource dataSource) {
		if (dataSource.getDataSourceType() == ReengineerType.XML)
			return true;
		else
			return false;
	}

	@Override
	public boolean canPerformReengineering(int dataSourceType) {
		if (dataSourceType == getReengineerType()) {
			return true;
		} else {
			return false;
				}
			}
			
	@Override
	public boolean canPerformReengineering(OWLOntology schemaOntology) {
			
		OWLDataFactory factory = onManager.getOwlFactory();
			
		OWLClass dataSourceClass = factory.getOWLClass(Semion_OWL.DataSource);
		Set<OWLIndividual> individuals = dataSourceClass
				.getIndividuals(schemaOntology);
			
		int hasDataSourceType = -1;
		
		if (individuals != null && individuals.size() == 1) {
			for (OWLIndividual individual : individuals) {
				OWLDataProperty hasDataSourceTypeProperty = factory
						.getOWLDataProperty(Semion_OWL.hasDataSourceType);
				Set<OWLLiteral> values = individual.getDataPropertyValues(
						hasDataSourceTypeProperty, schemaOntology);
				if (values != null && values.size() == 1) {
					for (OWLLiteral value : values) {
		try {
							Integer valueInteger = Integer.valueOf(value
									.getLiteral());
							hasDataSourceType = valueInteger.intValue();
						} catch (NumberFormatException e) {

						}
			}
		}
		}
		}
		
		if (hasDataSourceType == getReengineerType()) {
			return true;
		} else {
			return false;
		}
			}
			
			@Override
	public boolean canPerformReengineering(String dataSourceType)
			throws UnsupportedReengineerException {
		return canPerformReengineering(ReengineerType.getType(dataSourceType));
			}
			
	private IRI createElementResource(String ns, String schemaNS,
			Element element, String parentName, Integer id,
			OWLOntologyManager manager, OWLDataFactory factory,
			OWLOntology dataOntology) {
				
		IRI elementResourceIRI;
		OWLClassAssertionAxiom elementResource;
		if (id == null) {
			elementResourceIRI = IRI.create(ns + "root");
			elementResource = createOWLClassAssertionAxiom(factory,
					XML_OWL.XMLElement, elementResourceIRI);
		} else {
			elementResourceIRI = IRI.create(ns + parentName + "_"
					+ element.getLocalName() + "_" + id.toString());
			elementResource = createOWLClassAssertionAxiom(factory,
					XML_OWL.XMLElement, elementResourceIRI);
			}
		manager.applyChange(new AddAxiom(dataOntology, elementResource));
		
		String schemaElementName = element.getLocalName();
			
		IRI elementDeclarationIRI = IRI.create(schemaNS + schemaElementName);
			
		manager.applyChange(new AddAxiom(dataOntology,
				createOWLObjectPropertyAssertionAxiom(factory,
						XML_OWL.hasElementDeclaration, elementResourceIRI,
						elementDeclarationIRI)));
			
		NamedNodeMap namedNodeMap = element.getAttributes();
		if (namedNodeMap != null) {
			for (int i = 0, j = namedNodeMap.getLength(); i < j; i++) {
				Node node = namedNodeMap.item(i);
				
				String attributeName = node.getNodeName();
				String attributeValue = node.getTextContent();

				String[] elementNames = elementResourceIRI.toString()
						.split("#");
				String elementLocalName;
				if (elementNames.length == 2) {
					elementLocalName = elementNames[1];
				} else {
					elementLocalName = elementNames[0];
			}
		
				IRI xmlAttributeIRI = IRI.create(ns + elementLocalName
						+ attributeName);
				System.out.println("Attribute: " + ns + elementLocalName
						+ attributeName);
				OWLClassAssertionAxiom xmlAttribute = createOWLClassAssertionAxiom(
						factory, XML_OWL.XMLAttribute, xmlAttributeIRI);
				manager.addAxiom(dataOntology, xmlAttribute);
		
				manager.addAxiom(dataOntology,
						createOWLDataPropertyAssertionAxiom(factory,
								XML_OWL.nodeName, xmlAttributeIRI,
								attributeName));
				manager.addAxiom(dataOntology,
						createOWLDataPropertyAssertionAxiom(factory,
								XML_OWL.nodeValue, xmlAttributeIRI,
								attributeValue));

				IRI attributeDeclarationIRI = IRI.create(schemaNS
						+ schemaElementName + "_" + attributeName);

				manager.addAxiom(dataOntology,
						createOWLObjectPropertyAssertionAxiom(factory,
								XML_OWL.hasAttributeDeclaration,
								xmlAttributeIRI, attributeDeclarationIRI));
				manager.addAxiom(dataOntology,
						createOWLObjectPropertyAssertionAxiom(factory,
								XML_OWL.hasXMLAttribute, elementResourceIRI,
								xmlAttributeIRI));
		
		}
	}
	
		return elementResourceIRI;
	}
	
	@Override
	public OWLOntology dataReengineering(String graphNS, IRI outputIRI,
			DataSource dataSource, final OWLOntology schemaOntology)
			throws ReengineeringException {
		
		OWLOntology ontology = null;
		
		System.out.println("Starting XML Reengineering");
		OWLOntologyManager ontologyManager = onManager.getOwlCacheManager();
		OWLDataFactory factory = onManager.getOwlFactory();
		
		IRI schemaOntologyIRI = schemaOntology.getOntologyID().getOntologyIRI();
		
		OWLOntology localDataOntology = null;
			
		System.out.println("XML output IRI: "+outputIRI);
		if(schemaOntology != null){
			if(outputIRI != null){
				try {
					localDataOntology = ontologyManager
							.createOntology(outputIRI);
				} catch (OWLOntologyCreationException e) {
					e.printStackTrace();
					throw new ReengineeringException();
				}
			} else {
				try {
					localDataOntology = ontologyManager.createOntology();
				} catch (OWLOntologyCreationException e) {
					throw new ReengineeringException();
				}	
			}
			
			final OWLOntology dataOntology = localDataOntology;
			
			OWLImportsDeclaration importsDeclaration = factory
					.getOWLImportsDeclaration(IRI.create(XML_OWL.URI));
			
			ontologyManager.applyChange(new AddImport(dataOntology,
					importsDeclaration));
			
			graphNS = graphNS.replace("#", "");
			String schemaNS = graphNS + "/schema#";
			;
			String dataNS = graphNS + "#";
			
			OWLClass dataSourceOwlClass = factory
					.getOWLClass(Semion_OWL.DataSource);
			
			Set<OWLIndividual> individuals = dataSourceOwlClass
					.getIndividuals(schemaOntology);
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			DocumentBuilder db;
			try {
				db = dbf.newDocumentBuilder();
				
				InputStream xmlStream = (InputStream) dataSource
						.getDataSource();
				
				Document dom = db.parse(xmlStream);
				
				Element documentElement = dom.getDocumentElement();
				
				String nodeName = documentElement.getNodeName();
				
				IRI rootElementIRI = createElementResource(dataNS, schemaNS,
						documentElement, null, null, ontologyManager, factory,
						dataOntology);
				
				iterateChildren(dataNS, schemaNS, rootElementIRI,
						documentElement, ontologyManager, factory, dataOntology);
				
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); 
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			OWLOntologyManager man = OWLManager.createOWLOntologyManager();
			
			OWLOntologySetProvider provider = new OWLOntologySetProvider() {
				
				@Override
				public Set<OWLOntology> getOntologies() {
					Set<OWLOntology> ontologies = new HashSet<OWLOntology>();
					ontologies.add(schemaOntology);
					ontologies.add(dataOntology);
					return ontologies;
				}
			};
			OWLOntologyMerger merger = new OWLOntologyMerger(provider);
			
			try {
				ontology = merger.createMergedOntology(man, outputIRI);
			} catch (OWLOntologyCreationException e) {
				e.printStackTrace();
			}
		}
		
		return ontology;
	}
	
	private OWLOntology dataReengineering(String graphNS, IRI outputIRI,
			Document dom, OWLOntology schemaOntology)
			throws ReengineeringException {
		
		OWLOntologyManager ontologyManager = onManager.getOwlCacheManager();
		OWLDataFactory factory = onManager.getOwlFactory();
		
		IRI schemaOntologyIRI = schemaOntology.getOntologyID().getOntologyIRI();
		
		OWLOntology dataOntology = null;
		
		if(schemaOntology != null){
			if(outputIRI != null){
				try {
					dataOntology = ontologyManager.createOntology(outputIRI);
				} catch (OWLOntologyCreationException e) {
					throw new ReengineeringException();
				}
			} else {
				try {
					dataOntology = ontologyManager.createOntology();
				} catch (OWLOntologyCreationException e) {
					throw new ReengineeringException();
				}	
			}
			
			OWLImportsDeclaration importsDeclaration = factory
					.getOWLImportsDeclaration(schemaOntologyIRI);
			
			ontologyManager.applyChange(new AddImport(dataOntology,
					importsDeclaration));
			
			String schemaNS = graphNS + "/schema#";
			;
			String dataNS = graphNS + "#";
			;
			
			OWLClass dataSourceOwlClass = factory
					.getOWLClass(Semion_OWL.DataSource);
			
			Set<OWLIndividual> individuals = dataSourceOwlClass
					.getIndividuals(schemaOntology);
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			DocumentBuilder db;
			try {
				db = dbf.newDocumentBuilder();
				
				Element documentElement = dom.getDocumentElement();
				
				String nodeName = documentElement.getNodeName();
				
				IRI rootElementIRI = createElementResource(dataNS, schemaNS,
						documentElement, null, null, ontologyManager, factory,
						dataOntology);
				
				iterateChildren(dataNS, schemaNS, rootElementIRI,
						documentElement, ontologyManager, factory, dataOntology);
				
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); 
			} 
		}
		
		return dataOntology;
	}
	
	@Deactivate
	protected void deactivate(ComponentContext context) {
		log.info("in " + XMLExtractor.class + " deactivate with context "
				+ context);
		reengineeringManager.unbindReengineer(this);
	}
	
	@Override
	public int getReengineerType() {
		return ReengineerType.XML;
		}
		
	private OntologyScope getScope() {
		OntologyScope ontologyScope = null;
				
		ScopeRegistry scopeRegistry = onManager.getScopeRegistry();
				
		if (scopeRegistry.isScopeActive(scopeIRI)) {
			ontologyScope = scopeRegistry.getScope(scopeIRI);
				}
				
		return ontologyScope;
	}
				
	private void iterateChildren(String dataNS, String schemaNS,
			IRI parentResource, Node parentElement, OWLOntologyManager manager,
			OWLDataFactory factory, OWLOntology dataOntology) {
				
		NodeList children = parentElement.getChildNodes();
		if (children != null) {
			for (int i = 0, j = children.getLength(); i < j; i++) {
				Node child = children.item(i);
				if (child instanceof Element) {
				
					String[] parentNames = parentResource.toString().split("#");
					String parentLocalName;
					if (parentNames.length == 2) {
						parentLocalName = parentNames[1];
					} else {
						parentLocalName = parentNames[0];
					}
				
					IRI childResource = createElementResource(dataNS, schemaNS,
							(Element) child, parentLocalName, Integer
									.valueOf(i), manager, factory, dataOntology);

					manager.applyChange(new AddAxiom(dataOntology,
							createOWLObjectPropertyAssertionAxiom(factory,
									XSD_OWL.child, parentResource,
									childResource)));
					manager.applyChange(new AddAxiom(dataOntology,
							createOWLObjectPropertyAssertionAxiom(factory,
									XSD_OWL.parent, childResource,
									parentResource)));

					iterateChildren(dataNS, schemaNS, childResource, child,
							manager, factory, dataOntology);
				} else {
					String textContent = child.getNodeValue();
					if (textContent != null) {
						textContent = textContent.trim();
				
						if (!textContent.equals("")) {
							log.info("VALUE : " + textContent);
							manager.applyChange(new AddAxiom(dataOntology,
									createOWLDataPropertyAssertionAxiom(
											factory, XML_OWL.nodeValue,
											parentResource, textContent)));
						}
					}
				}
			}
		}
	}
	
	@Override
	public OWLOntology reengineering(String graphNS, IRI outputIRI,
			DataSource dataSource) throws ReengineeringException {

		InputStream dataSourceAsStream = (InputStream) dataSource
				.getDataSource();
	
		InputStreamReader isr = new InputStreamReader(dataSourceAsStream);
		BufferedReader reader = new BufferedReader(isr);
		final StringBuilder stringBuilder1 = new StringBuilder();
		final StringBuilder stringBuilder2 = new StringBuilder();
	
		OutputStream out = new OutputStream() {
	
	@Override
			public void write(byte[] bytes) throws IOException {
				for (byte b : bytes) {
					stringBuilder1.append((char) b);
					stringBuilder2.append((char) b);
		}
	}
	
	@Override
			public void write(int arg0) throws IOException {
				stringBuilder1.append((char) arg0);
				stringBuilder2.append((char) arg0);
		
			}
		
		};
		
		String line = "";
						try{
			while ((line = reader.readLine()) != null) {
				out.write(line.getBytes());
			}
			out.flush();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		final ByteArrayOutputStream buff1 = new ByteArrayOutputStream();
		try {
			buff1.write(stringBuilder1.toString().getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
	}
	
		final ByteArrayOutputStream buff2 = new ByteArrayOutputStream();
		try {
			buff2.write(stringBuilder2.toString().getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		DataSource ds1 = new DataSource() {

			@Override
			public Object getDataSource() {
				ByteArrayInputStream byteArr = new ByteArrayInputStream(buff1
						.toByteArray());
				return byteArr;
			
			}
			
			@Override
			public int getDataSourceType() {
				// TODO Auto-generated method stub
				return ReengineerType.XML;
			}
			
			@Override
			public String getID() {
				// TODO Auto-generated method stub
				return null;
		}
		};
		
		DataSource ds2 = new DataSource() {

			@Override
			public Object getDataSource() {
				ByteArrayInputStream byteArr = new ByteArrayInputStream(buff2
						.toByteArray());
				return byteArr;
		
	}
	
			@Override
			public int getDataSourceType() {
				// TODO Auto-generated method stub
				return ReengineerType.XML;
	}

			@Override
			public String getID() {
				// TODO Auto-generated method stub
				return null;
			}
		};
		
		OWLOntology schemaOntology;
		
		System.out.println("XML outputIRI : " + outputIRI);
		if (outputIRI != null && !outputIRI.equals("")) {
			IRI schemaIRI = IRI.create(outputIRI.toString() + "/schema");
			schemaOntology = schemaReengineering(graphNS + "/schema",
					schemaIRI, ds1);
		} else {
			schemaOntology = schemaReengineering(graphNS + "/schema", null, ds1);
		}
		OWLOntology ontology = dataReengineering(graphNS, outputIRI, ds2,
				schemaOntology);
		
		try {
			onManager.getOwlCacheManager().saveOntology(ontology, System.out);
		} catch (OWLOntologyStorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ontology;
	}

	@Override
	public OWLOntology schemaReengineering(String graphNS, IRI outputIRI,
			DataSource dataSource) {
		XSDExtractor xsdExtractor = new XSDExtractor(onManager);
		return xsdExtractor.getOntologySchema(graphNS, outputIRI, dataSource);
	}

}

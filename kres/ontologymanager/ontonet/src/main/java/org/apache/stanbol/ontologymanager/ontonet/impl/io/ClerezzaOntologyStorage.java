package org.apache.stanbol.ontologymanager.ontonet.impl.io;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.access.WeightedTcProvider;
import org.apache.clerezza.rdf.core.impl.SimpleGraph;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.sparql.ParseException;
import org.apache.clerezza.rdf.core.sparql.QueryParser;
import org.apache.clerezza.rdf.core.sparql.query.Query;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;

import org.apache.stanbol.ontologymanager.ontonet.impl.ontology.NoSuchStoreException;
import org.apache.stanbol.owlapi.trasformation.JenaToClerezzaConverter;
import org.apache.stanbol.owlapi.trasformation.JenaToOwlConvert;

public class ClerezzaOntologyStorage {

	private static Logger log = LoggerFactory.getLogger(ClerezzaOntologyStorage.class);
	
	public static final String URI = "http://ontologydesignpatterns.org/ont/iks/oxml.owl";
	
	TcManager tcManager;
	
	WeightedTcProvider weightedTcProvider;
	
	/**
	 * This default constructor is <b>only</b> intended to be used by the OSGI
	 * environment with Service Component Runtime support.
	 * <p>
	 * DO NOT USE to manually create instances - the ClerezzaStorage instances
	 * do need to be configured! YOU NEED TO USE
	 * {@link #ClerezzaStorage(TcManager, WeightedTcProvider, OntologyStoreProvider)}
	 * or its overloads, to parse the configuration and then initialise the rule
	 * store if running outside a OSGI environment.
	 */
	protected ClerezzaOntologyStorage() {

	}
	
	/**
	 * Basic constructor to be used if outside of an OSGi environment. Invokes
	 * default constructor.
	 * 
	 * @param tcManager
	 * @param wtcProvider
	 * @param osProvider
	 */
	public ClerezzaOntologyStorage(TcManager tcManager, WeightedTcProvider wtcProvider) {
		this();
		this.tcManager = tcManager;
		this.weightedTcProvider = wtcProvider;
		activate(new Hashtable<String, Object>());
	}

	@SuppressWarnings("unchecked")
    protected void activate(ComponentContext context) {
		log.info("in " + ClerezzaOntologyStorage.class + " activate with context "
				+ context);
		if (context == null) {
			throw new IllegalStateException("No valid" + ComponentContext.class
					+ " parsed in activate!");
		}
		activate((Dictionary<String, Object>) context.getProperties());
	}

	protected void activate(Dictionary<String, Object> configuration) {

	}

	public void clear() {
		// TODO Auto-generated method stub
	}

	protected void deactivate(ComponentContext context) {
		log.info("in " + ClerezzaOntologyStorage.class + " deactivate with context "
				+ context);
		tcManager = null;
		weightedTcProvider = null;
	}

	public void delete(IRI ontologyId) {
		// TODO Auto-generated method stub
	}

	public void deleteAll(Set<IRI> ontologyIds) {
		// TODO Auto-generated method stub
	}

	public OWLOntology getGraph(IRI ontologyID) throws NoSuchStoreException {
		OWLOntology ontology = null;

		if (tcManager != null) {
			MGraph mGraph = tcManager.getMGraph(new UriRef(ontologyID
					.toString()));
			JenaToOwlConvert jowl = new JenaToOwlConvert();
			OntModel ontModel = ModelFactory.createOntologyModel(
					OntModelSpec.OWL_DL_MEM, FileManager.get().loadModel(
					    URI));
			ontModel.add(JenaToClerezzaConverter
					.clerezzaMGraphToJenaModel(mGraph));
			ontology = jowl.ModelJenaToOwlConvert(ontModel, "RDF/XML");
			// ontology =
			// OWLAPIToClerezzaConverter.clerezzaMGraphToOWLOntology(mGraph);

		} else {
			throw new NoSuchStoreException(
					"No store registered or activated in the environment.");
		}
		return ontology;
	}

	public Set<IRI> listGraphs() {

		Set<IRI> iris = null;
		Set<UriRef> uriRefs = tcManager.listTripleCollections();
		if (uriRefs != null) {
			iris = new HashSet<IRI>();
			for (UriRef uriRef : uriRefs) {
				iris.add(IRI.create(uriRef.toString()));
			}
		}
		return iris;

	}

	public OWLOntology load(IRI ontologyId) {
		MGraph triples = TcManager.getInstance().getMGraph(
				new UriRef(ontologyId.toString()));
		Model om = JenaToClerezzaConverter.clerezzaMGraphToJenaModel(triples);
		JenaToOwlConvert converter = new JenaToOwlConvert();
		return converter.ModelJenaToOwlConvert(om, "RDF/XML");
	}
	
	public OWLOntology sparqlConstruct(String sparql, String datasetURI) {
		
		Query query;
		
		MGraph mGraph = new SimpleMGraph();
		try {
			query = QueryParser.getInstance().parse(sparql);
			
			UriRef datasetUriRef = new UriRef(datasetURI);
			MGraph dataset = weightedTcProvider.getMGraph(datasetUriRef);
			
			mGraph.addAll((SimpleGraph) tcManager.executeSparqlQuery(query,
					dataset));
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Model om = JenaToClerezzaConverter.clerezzaMGraphToJenaModel(mGraph);
		JenaToOwlConvert converter = new JenaToOwlConvert();
		return converter.ModelJenaToOwlConvert(om, "RDF/XML");
	}

	public void store(OWLOntology o) {
		
		JenaToOwlConvert converter = new JenaToOwlConvert();
		OntModel om = converter.ModelOwlToJenaConvert(o, "RDF/XML");
		MGraph mg = JenaToClerezzaConverter.jenaModelToClerezzaMGraph(om);
		// MGraph mg = OWLAPIToClerezzaConverter.owlOntologyToClerezzaMGraph(o);
		MGraph mg2 = tcManager.createMGraph(new UriRef(o.getOntologyID()
				.getOntologyIRI().toString()));
		mg2.addAll(mg);
	}
	
	public void store(OWLOntology o, IRI ontologyID) {
			
		JenaToOwlConvert converter = new JenaToOwlConvert();
		OntModel om = converter.ModelOwlToJenaConvert(o, "RDF/XML");
		MGraph mg = JenaToClerezzaConverter.jenaModelToClerezzaMGraph(om);
		// MGraph mg = OWLAPIToClerezzaConverter.owlOntologyToClerezzaMGraph(o);
		MGraph mg2 = tcManager.createMGraph(new UriRef(ontologyID.toString()));
		mg2.addAll(mg);
	}
}

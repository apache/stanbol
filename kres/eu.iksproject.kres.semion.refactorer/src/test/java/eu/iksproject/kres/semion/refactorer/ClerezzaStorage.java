package eu.iksproject.kres.semion.refactorer;

import java.util.HashSet;
import java.util.Set;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.impl.SimpleGraph;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.sparql.ParseException;
import org.apache.clerezza.rdf.core.sparql.QueryParser;
import org.apache.clerezza.rdf.core.sparql.query.Query;
import org.apache.stanbol.ontologymanager.store.api.NoSuchStoreException;
import org.apache.stanbol.ontologymanager.store.api.OntologyStorage;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;

import org.apache.stanbol.reengineer.xml.XML_OWL;
import eu.iksproject.kres.shared.transformation.JenaToClerezzaConverter;
import eu.iksproject.kres.shared.transformation.JenaToOwlConvert;

public class ClerezzaStorage implements OntologyStorage {

	// private static Logger log =
	// LoggerFactory.getLogger(ClerezzaStorage.class);

	TcManager tcManager;
		
	public ClerezzaStorage() {
		tcManager = TcManager.getInstance();
	}
	
	@Override
	public void store(OWLOntology o) {

		JenaToOwlConvert converter = new JenaToOwlConvert();
		OntModel om = converter.ModelOwlToJenaConvert(o, "RDF/XML");
		MGraph mg = JenaToClerezzaConverter.jenaModelToClerezzaMGraph(om);
		//MGraph mg = OWLAPIToClerezzaConverter.owlOntologyToClerezzaMGraph(o);
		MGraph mg2 = tcManager.createMGraph(
				new UriRef(o.getOntologyID().getOntologyIRI().toString()));
		mg2.addAll(mg);
	}
	
	@Override
	public void store(OWLOntology o, IRI ontologyID) {

		JenaToOwlConvert converter = new JenaToOwlConvert();
		OntModel om = converter.ModelOwlToJenaConvert(o, "RDF/XML");
		MGraph mg = JenaToClerezzaConverter.jenaModelToClerezzaMGraph(om);
		//MGraph mg = OWLAPIToClerezzaConverter.owlOntologyToClerezzaMGraph(o);
		MGraph mg2 = tcManager.createMGraph(new UriRef(ontologyID.toString()));
		mg2.addAll(mg);
	}

	@Override
	public OWLOntology load(IRI ontologyId) {
		MGraph triples = TcManager.getInstance().getMGraph(
				new UriRef(ontologyId.toString()));
		Model om = JenaToClerezzaConverter.clerezzaMGraphToJenaModel(triples);
		JenaToOwlConvert converter = new JenaToOwlConvert();
		return converter.ModelJenaToOwlConvert(om, "RDF/XML");
	}
	
	@Override
	public OWLOntology sparqlConstruct(String sparql, String datasetURI) {
		
		Query query;
		
		MGraph mGraph = new SimpleMGraph();
		try {
			query = QueryParser.getInstance().parse(sparql);
			
			UriRef datasetUriRef = new UriRef(datasetURI);
			MGraph dataset = tcManager.getMGraph(datasetUriRef);
			
			mGraph.addAll((SimpleGraph) tcManager.executeSparqlQuery(query, dataset));
			
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Model om = JenaToClerezzaConverter.clerezzaMGraphToJenaModel(mGraph);
		JenaToOwlConvert converter = new JenaToOwlConvert();
		return converter.ModelJenaToOwlConvert(om, "RDF/XML");
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(IRI ontologyId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void 
	deleteAll(Set<IRI> ontologyIds) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public Set<IRI> listGraphs(){
		
		Set<IRI> iris = null;
		Set<UriRef> uriRefs = tcManager.listTripleCollections();
		if(uriRefs != null){
			iris = new HashSet<IRI>();
			for(UriRef uriRef : uriRefs){
				iris.add(IRI.create(uriRef.toString()));
			}
		}
		return iris;
		
	}
	
	@Override
	public OWLOntology getGraph(IRI ontologyID) throws NoSuchStoreException {
		OWLOntology ontology = null;
		
		if(tcManager != null){
			MGraph mGraph = tcManager.getMGraph(new UriRef(ontologyID.toString()));
			JenaToOwlConvert jowl = new JenaToOwlConvert();
			OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, FileManager.get().loadModel(XML_OWL.URI));
			ontModel.add(JenaToClerezzaConverter.clerezzaMGraphToJenaModel(mGraph));
			ontology = jowl.ModelJenaToOwlConvert(ontModel, "RDF/XML");
			//ontology = OWLAPIToClerezzaConverter.clerezzaMGraphToOWLOntology(mGraph);
			
			
		}
		else{
			throw new NoSuchStoreException("No store registered or activated in the environment.");
		}
		return ontology;
	}
}

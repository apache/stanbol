package eu.iksproject.kres.rules.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.WeightedTcProvider;
import org.apache.clerezza.rdf.core.serializedform.SerializingProvider;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.jena.serializer.JenaSerializerProvider;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class RuleOntologyInputSource implements OntologyInputSource {

	private OWLOntology rootOntology;
	private IRI physicalIri;
	private WeightedTcProvider weightedTcProvider;
	
	public RuleOntologyInputSource(OWLOntology rootOntology, WeightedTcProvider weightedTcProvider) {
		this.rootOntology = rootOntology;
		this.weightedTcProvider = weightedTcProvider;
		try {
			physicalIri = rootOntology.getOntologyID().getDefaultDocumentIRI();
		} catch (Exception e) {
			// Ontology might be anonymous, no physical IRI then...
		}

	}
	
	@Override
	public IRI getPhysicalIRI() {
		return physicalIri;
	}

	@Override
	public OWLOntology getRootOntology() {
		OWLOntology ontology = null;
		
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		
		try {
			
			MGraph mGraph = weightedTcProvider.getMGraph(new UriRef(physicalIri.toString()));
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			
			SerializingProvider serializingProvider = new JenaSerializerProvider();
			
			serializingProvider.serialize(out, mGraph, SupportedFormat.RDF_XML);
			
			ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
			ontology = manager.loadOntologyFromOntologyDocument(in);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
		return ontology;
	}

	@Override
	public boolean hasPhysicalIRI() {
		return physicalIri != null;
	}

	@Override
	public boolean hasRootOntology() {
		return rootOntology != null;
	} 

}

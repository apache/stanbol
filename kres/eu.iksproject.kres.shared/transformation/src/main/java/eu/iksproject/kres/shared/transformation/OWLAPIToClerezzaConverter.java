package eu.iksproject.kres.shared.transformation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.PlainLiteral;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TypedLiteral;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.ParsingProvider;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SerializingProvider;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.jena.parser.JenaParserProvider;
import org.apache.clerezza.rdf.jena.serializer.JenaSerializerProvider;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFactory;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.w3c.dom.Document;

import com.hp.hpl.jena.datatypes.BaseDatatype;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.arp.impl.ANode;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.RDFWriterFImpl;
import com.hp.hpl.jena.util.FileManager;


/**
 * This class provides static methods to convert:
 * 
 * <ul>
 * <li> a Jena Model (see {@link Model}) to a list of Clerezza triples (see {@link Triple})
 * <li> a Jena Model to a Clerezza MGraph (see {@link MGraph})
 * <li> a Clerezza MGraph a Jena Model
 * <li> a Clerezza MGraph a Jena Graph (see {@link Graph}}
 * </ul>
 * 
 * 
 * @author andrea.nuzzolese
 *
 */

public class OWLAPIToClerezzaConverter {

	
	/**
	 * 
	 * Converts an OWL API {@link OWLOntology} to an {@link ArrayList} of Clerezza triples (instances of class {@link Triple}).
	 * 
	 * @param ontology {@link OWLOntology}
	 * @return an {@link ArrayList} that contains the generated Clerezza triples (see {@link Triple}) 
	 */
	public static ArrayList<Triple> owlOntologyToClerezzaTriples(OWLOntology ontology){
		
		ArrayList<Triple> clerezzaTriples = new ArrayList<Triple>();
		
		MGraph mGraph = owlOntologyToClerezzaMGraph(ontology);
		
		Iterator<Triple> tripleIterator = mGraph.iterator();
		while(tripleIterator.hasNext()){
			Triple triple = tripleIterator.next();
			clerezzaTriples.add(triple);
		}
		
		return clerezzaTriples;
		
	}
	
	
	
	
	
	/**
	 * 
	 * Converts a OWL API {@link OWLOntology} to Clerezza {@link MGraph}.
	 * 
	 * @param ontology {@link OWLOntology}
	 * @return the equivalent Clerezza {@link MGraph}.
	 */
	
	public static MGraph owlOntologyToClerezzaMGraph(OWLOntology ontology){
		
		MGraph mGraph = null;
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		
		try {
			manager.saveOntology(ontology, new RDFXMLOntologyFormat(), out);
			
			ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
			
			ParsingProvider parser = new JenaParserProvider();
			mGraph = new SimpleMGraph();
			parser.parse(mGraph, in, SupportedFormat.RDF_XML, null);
		} catch (OWLOntologyStorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return mGraph;
		
	}
	
	
	/**
	 * Converts a Clerezza {@link MGraph} to an OWL API {@link OWLOntology}.
	 * 
	 * @param mGraph {@link MGraph}
	 * @return the equivalent OWL API {@link OWLOntology}.
	 */
	public static OWLOntology clerezzaMGraphToOWLOntology(MGraph mGraph){
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		SerializingProvider serializingProvider = new JenaSerializerProvider();
		
		serializingProvider.serialize(out, mGraph, SupportedFormat.RDF_XML);
		
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		
		OWLOntology ontology = null;
		try {
			ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(in);
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return ontology;
		
	}
	
	
	
	
}

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

public class JenaToClerezzaConverter {

	
	/**
	 * 
	 * Converts a Jena {@link Model} to an {@link ArrayList} of Clerezza triples (instances of class {@link Triple}).
	 * 
	 * @param model {@link Model}
	 * @return an {@link ArrayList} that contains the generated Clerezza triples (see {@link Triple}) 
	 */
	public static ArrayList<Triple> jenaModelToClerezzaTriples(Model model){
		
		ArrayList<Triple> clerezzaTriples = new ArrayList<Triple>();
		
		MGraph mGraph = jenaModelToClerezzaMGraph(model);
		
		Iterator<Triple> tripleIterator = mGraph.iterator();
		while(tripleIterator.hasNext()){
			Triple triple = tripleIterator.next();
			clerezzaTriples.add(triple);
		}
		
		return clerezzaTriples;
		
	}
	
	
	
	
	
	/**
	 * 
	 * Converts a Jena {@link Model} to Clerezza {@link MGraph}.
	 * 
	 * @param model {@link Model}
	 * @return the equivalent Clerezza {@link MGraph}.
	 */
	
	public static MGraph jenaModelToClerezzaMGraph(Model model){
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		model.write(out);
		
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		
		ParsingProvider parser = new JenaParserProvider();
		org.apache.clerezza.rdf.core.Graph deserializedGraph = parser.parse(in, SupportedFormat.RDF_XML, null);
		
		
		MGraph mGraph = new SimpleMGraph();
		mGraph.addAll(deserializedGraph);
		
		return mGraph;
		
	}
	
	
	/**
	 * Converts a Clerezza {@link MGraph} to a Jena {@link Model}.
	 * 
	 * @param mGraph {@link MGraph}
	 * @return the equivalent Jena {@link Model}.
	 */
	public static Model clerezzaMGraphToJenaModel(MGraph mGraph){
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		SerializingProvider serializingProvider = new JenaSerializerProvider();
		
		serializingProvider.serialize(out, mGraph, SupportedFormat.RDF_XML);
		
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		
		Model jenaModel = ModelFactory.createDefaultModel();
		
		jenaModel.read(in, null);
		
		return jenaModel;
		
	}
	
	
	/**
	 * Converts a Clerezza {@link MGraph} to a Jena {@link Graph}.
	 * 
	 * @param mGraph {@link MGraph}
	 * @return the equivalent Jena {@link Graph}.
	 */
	public static com.hp.hpl.jena.graph.Graph clerezzaMGraphToJenaGraph(MGraph mGraph){
		
		Model jenaModel = clerezzaMGraphToJenaModel(mGraph);
		if(jenaModel != null){
			return jenaModel.getGraph();
		}
		else{
			return null;
		}
		
	}
	
}

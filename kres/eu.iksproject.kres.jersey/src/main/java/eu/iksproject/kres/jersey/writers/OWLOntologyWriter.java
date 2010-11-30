package eu.iksproject.kres.jersey.writers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Iterator;

import javax.servlet.ServletContext;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.jena.serializer.JenaSerializerProvider;
import org.apache.clerezza.rdf.rdfjson.serializer.RdfJsonSerializingProvider;
import org.apache.commons.lang.SystemUtils;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.json.simple.JSONArray;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import com.hp.hpl.jena.rdf.model.Model;

import eu.iksproject.kres.api.format.KReSFormat;
import eu.iksproject.kres.shared.transformation.JenaToOwlConvert;
import eu.iksproject.kres.shared.transformation.OWLAPIToClerezzaConverter;

@Provider
@Produces( { KReSFormat.RDF_XML, KReSFormat.OWL_XML, KReSFormat.MANCHERSTER_OWL, 
	KReSFormat.FUNCTIONAL_OWL, KReSFormat.TURTLE, KReSFormat.RDF_JSON})
public class OWLOntologyWriter implements MessageBodyWriter<OWLOntology> {

	protected Serializer serializer;
	
	protected ServletContext servletContext;
	
	public OWLOntologyWriter(@Context ServletContext servletContext) {
		this.servletContext = servletContext;
		System.out.println("setted context "+servletContext);
		serializer = (Serializer) this.servletContext.getAttribute(Serializer.class.getName());
		if(serializer == null){
			System.out.println("Serializer not found in ServletContext");
			serializer = new Serializer();
		}
	}
	
	@Override
	public long getSize(OWLOntology arg0, Class<?> arg1, Type arg2,
			Annotation[] arg3, MediaType arg4) {
		// TODO Auto-generated method stub
		return -1;
	}

	@Override
	public boolean isWriteable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
		return OWLOntology.class.isAssignableFrom(type);
	}

	@Override
	public void writeTo(OWLOntology ontology, Class<?> arg1, Type arg2,
			Annotation[] arg3, MediaType mediaType,
			MultivaluedMap<String, Object> arg5, OutputStream out)
			throws IOException, WebApplicationException {
		
		
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

			
			System.out.println("KReS FORMAT!!! : "+mediaType);
			System.out.println("KReS FORMAT 2!!! : "+mediaType.getType());
		    if(mediaType.toString().equals(KReSFormat.RDF_XML)){
		    	try {
		    		System.out.println("RDF/XML!!!");
					manager.saveOntology(ontology, new RDFXMLOntologyFormat(), out);
				} catch (OWLOntologyStorageException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    }
		    else if(mediaType.toString().equals(KReSFormat.OWL_XML)){
		    	try {
					manager.saveOntology(ontology, new OWLXMLOntologyFormat(), out);
				} catch (OWLOntologyStorageException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    }
		    else if(mediaType.toString().equals(KReSFormat.MANCHERSTER_OWL)){
		    	try {
					manager.saveOntology(ontology, new ManchesterOWLSyntaxOntologyFormat(), out);
				} catch (OWLOntologyStorageException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    }
		    else if(mediaType.toString().equals(KReSFormat.FUNCTIONAL_OWL)){
		    	try {
					manager.saveOntology(ontology, new OWLFunctionalSyntaxOntologyFormat(), out);
				} catch (OWLOntologyStorageException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    }
		    else if(mediaType.toString().equals(KReSFormat.TURTLE)){
		    	try {
					manager.saveOntology(ontology, new TurtleOntologyFormat(), out);
				} catch (OWLOntologyStorageException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    }
		    else if(mediaType.toString().equals(KReSFormat.RDF_JSON)){
		    	 
		    	MGraph mGraph = OWLAPIToClerezzaConverter.owlOntologyToClerezzaMGraph(ontology);
	    		
		    	RdfJsonSerializingProvider provider = new RdfJsonSerializingProvider();
	    		provider.serialize(out, mGraph, SupportedFormat.RDF_JSON);
				
		    }
		
		    out.flush();
	}

	
}

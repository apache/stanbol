package eu.iksproject.kres.jersey.writers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;


import com.hp.hpl.jena.rdf.model.Model;


@Provider
@Produces({"application/rdf+xml", "application/xml", "text/xml"})

public class JenaModelWriter implements MessageBodyWriter<Model> {

	
	
	public static final String ENCODING = "UTF-8";
	
	public long getSize(Model arg0, Class<?> arg1, Type arg2,
			Annotation[] arg3, MediaType arg4) {
		return -1;
	}

	public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2,
			MediaType arg3) {

		return Model.class.isAssignableFrom(arg0);
	}

	public void writeTo(Model model, Class<?> arg1, Type arg2,
			Annotation[] arg3, MediaType arg4,
			MultivaluedMap<String, Object> arg5, OutputStream outputStream)
	throws IOException, WebApplicationException {
		Document doc = null;

		try {
			doc = new JenaModelTransformer().toDocument(model);
			DOMSource domSource = new DOMSource(doc);
			StreamResult streamResult = new StreamResult(outputStream);
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer serializer = tf.newTransformer();
			serializer.setOutputProperty(OutputKeys.ENCODING,ENCODING);
			serializer.setOutputProperty(OutputKeys.INDENT,"yes");
			serializer.transform(domSource, streamResult);
		} catch(TransformerException te) {
			throw new IOException("TransformerException in writeTo()");
		}

		outputStream.flush();
		
	}

}

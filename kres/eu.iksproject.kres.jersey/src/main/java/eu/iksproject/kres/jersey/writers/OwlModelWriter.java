/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.iksproject.kres.jersey.writers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

/**
 *
 * @author elvio
 */
@Provider
@Produces({"application/rdf+xml", "application/xml", "text/xml"})
public class OwlModelWriter implements MessageBodyWriter<OWLOntology>{

    @Override
    public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
        return OWLOntology.class.isAssignableFrom(arg0);
    }

    @Override
    public long getSize(OWLOntology arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
        return -1;
    }

    @Override
    public void writeTo(OWLOntology model, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4, MultivaluedMap<String, Object> arg5, OutputStream outputStream) throws IOException, WebApplicationException {

        try {
                model.getOWLOntologyManager().saveOntology(model, new RDFXMLOntologyFormat(), outputStream);
            } catch (OWLOntologyStorageException ex) {
                Logger.getLogger(OwlModelWriter.class.getName()).log(Level.SEVERE, null, ex);
                throw new IOException("TransformerException in writeTo()");
            }

	outputStream.flush();
    }

}


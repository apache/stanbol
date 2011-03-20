package org.apache.stanbol.kres.jersey.writers;

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

import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;

@Provider
@Produces( { MediaType.TEXT_PLAIN, SupportedFormat.N3,
        SupportedFormat.N_TRIPLE, SupportedFormat.RDF_XML,
        SupportedFormat.TURTLE, SupportedFormat.X_TURTLE,
        SupportedFormat.RDF_JSON })
public class GraphWriter implements MessageBodyWriter<TripleCollection> {

    @Context
    protected ServletContext servletContext;

    protected Serializer getSerializer() {
        return (Serializer) servletContext.getAttribute(Serializer.class.getName());
    }

    public boolean isWriteable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return TripleCollection.class.isAssignableFrom(type);
    }

    public long getSize(TripleCollection t, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    public void writeTo(TripleCollection t, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException,
            WebApplicationException {

        // Fallback to our own "serializer" with text/plain, useful if no
        // other serializer services are available
        if (MediaType.TEXT_PLAIN.equals(mediaType.toString())) {
            dump(entityStream, t);
        } else {
            getSerializer().serialize(entityStream, t, mediaType.toString());
        }
    }

    private void dump(OutputStream os, TripleCollection t) {
        final PrintWriter pw = new PrintWriter(new OutputStreamWriter(os));
        final Iterator<Triple> it = t.iterator();
        while (it.hasNext()) {
            pw.println(it.next());
        }
        pw.flush();
    }
}
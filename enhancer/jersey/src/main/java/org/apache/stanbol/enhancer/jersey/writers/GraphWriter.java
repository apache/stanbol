package org.apache.stanbol.enhancer.jersey.writers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.servlet.ServletContext;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

@Provider
@Produces( { TEXT_PLAIN, SupportedFormat.N3,
        SupportedFormat.N_TRIPLE, SupportedFormat.RDF_XML,
        SupportedFormat.TURTLE, SupportedFormat.X_TURTLE,
        SupportedFormat.RDF_JSON, APPLICATION_JSON })
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

        if (mediaType == null || mediaType.isWildcardType() || TEXT_PLAIN.equals(mediaType.toString())) {
           getSerializer().serialize(entityStream, t, APPLICATION_JSON);
        } else {
            getSerializer().serialize(entityStream, t, mediaType.toString());
        }
    }
}

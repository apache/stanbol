package org.apache.stanbol.commons.web.base.writers;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N3;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N_TRIPLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_JSON;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_XML;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.TURTLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.X_TURTLE;

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
import org.apache.stanbol.commons.web.base.ContextHelper;


@Provider
@Produces({TEXT_PLAIN, N3, N_TRIPLE, RDF_XML, TURTLE, X_TURTLE, RDF_JSON, APPLICATION_JSON})
public class GraphWriter implements MessageBodyWriter<TripleCollection> {

    @Context
    protected ServletContext servletContext;

    protected Serializer getSerializer() {
        return ContextHelper.getServiceFromContext(Serializer.class, servletContext);
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

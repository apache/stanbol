package org.apache.stanbol.reasoners.web.writers;

import java.io.ByteArrayOutputStream;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFWriter;

@Provider
@Produces({"application/rdf+xml", "text/turtle", "text/n3", "text/plain", "application/turtle"})
public class JenaModelWriter implements MessageBodyWriter<Model> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Model.class.isAssignableFrom(type);
    }

    private ByteArrayOutputStream stream = null;

    @Override
    public long getSize(Model t,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType) {
        log.debug("Called size of item");
        stream = toStream(t, mediaType.toString());
        log.error("Returning {} bytes", stream.size());
        return Integer.valueOf(stream.toByteArray().length).longValue();
    }

    public ByteArrayOutputStream toStream(Model t, String mediaType) {
        log.info("Serializing model to {}. Statements are {}", mediaType, t.listStatements().toSet().size());
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if (mediaType.equals("application/rdf+xml")) {
            t.write(stream);
        } else if (mediaType.equals("application/turtle")) {
            // t.write(stream, "TURTLE");
            RDFWriter writer = t.getWriter("TURTLE");
            log.info("Writer for TURTLE: {}", writer);
            writer.write(t, stream, null);
        } else if (mediaType.equals("text/turtle")) {
            t.write(stream, "TURTLE");
        } else if (mediaType.equals("text/plain")) {
            t.write(stream, "TURTLE");
        } else if (mediaType.equals("text/n3")) {
            t.write(stream, "N3");
        }
        log.info("Written {} bytes to stream", stream.toByteArray().length);
        return stream;
    }

    @Override
    public void writeTo(Model t,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType,
                        MultivaluedMap<String,Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException {
        if (stream == null) {
            toStream(t, mediaType.toString()).writeTo(entityStream);
        } else {
            stream.writeTo(entityStream);
            stream = null;
        }
    }

}

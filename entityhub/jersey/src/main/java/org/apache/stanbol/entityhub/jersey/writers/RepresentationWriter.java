package org.apache.stanbol.entityhub.jersey.writers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.commons.io.IOUtils;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.codehaus.jettison.json.JSONException;

@Provider
@Produces( {MediaType.APPLICATION_JSON, SupportedFormat.N3, SupportedFormat.N_TRIPLE,
            SupportedFormat.RDF_XML, SupportedFormat.TURTLE, SupportedFormat.X_TURTLE,
            SupportedFormat.RDF_JSON})
public class RepresentationWriter implements MessageBodyWriter<Representation> {

    public static final Set<String> supportedMediaTypes;
    static {
        Set<String> types = new HashSet<String>();
        types.add(MediaType.APPLICATION_JSON);
        types.add(SupportedFormat.N3);
        types.add(SupportedFormat.N_TRIPLE);
        types.add(SupportedFormat.RDF_JSON);
        types.add(SupportedFormat.RDF_XML);
        types.add(SupportedFormat.TURTLE);
        types.add(SupportedFormat.X_TURTLE);
        supportedMediaTypes = Collections.unmodifiableSet(types);
    }
    @Context
    protected ServletContext servletContext;

    @Override
    public long getSize(Representation t,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Representation.class.isAssignableFrom(type) && supportedMediaTypes.contains(mediaType.toString());
    }

    @Override
    public void writeTo(Representation rep,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType,
                        MultivaluedMap<String,Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException {
        if (mediaType == null || MediaType.APPLICATION_JSON.equals(mediaType.toString())) {
            try {
                IOUtils.write(SignToJSON.toJSON(rep).toString(4), entityStream);
            } catch (JSONException e) {
                throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
            }
        } else { // RDF
            Serializer ser = ContextHelper.getServiceFromContext(Serializer.class, servletContext);
            ser.serialize(entityStream, SignToRDF.toRDF(rep), mediaType.toString());
        }
        
    }

}

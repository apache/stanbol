package org.apache.stanbol.entityhub.jersey.writers;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
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

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.commons.io.IOUtils;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.codehaus.jettison.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Replace with Serializer infrastructure similar to {@link Serializer}
 */
@Provider
@Produces({APPLICATION_JSON, N3, N_TRIPLE, RDF_XML, TURTLE, X_TURTLE, RDF_JSON})
public class QueryResultListWriter implements MessageBodyWriter<QueryResultList<?>> {

    private final Logger log = LoggerFactory.getLogger(QueryResultListWriter.class);
    @Context
    protected ServletContext servletContext;

    protected Serializer getSerializer() {
        return (Serializer) servletContext.getAttribute(Serializer.class.getName());
    }

    @Override
    public long getSize(QueryResultList<?> result, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        //TODO: The type is also parsed as genericType ... so we can only check
        //for the type :(
        return QueryResultList.class.isAssignableFrom(type);
//       if(QueryResultList.class.isAssignableFrom(type) &&
//               genericType != null &&  //QueryResult is always a generic Type
//               genericType instanceof Class<?>){ //and such types do not use generics
//           //This writer supports String, Representation and all types of Signs
//           Class<?> genericClass  = (Class<?>) genericType;
//
//           if(String.class.isAssignableFrom(genericClass) ||
//                   Representation.class.isAssignableFrom(genericClass) ||
//                   Sign.class.isAssignableFrom(genericClass)){
//               //maybe we need further checks if we do not support all data types
//               //for all generic types! But currently all different types of
//               //QueryResultList support all the different MediaTypes!
//               return true;
//           }
//       }
//       log.info("Request for not writeable combination: type="+type+"|genericType="+genericType+"|mediaType="+mediaType);
//       return false;
    }

    @Override
    public void writeTo(QueryResultList<?> resultList, Class<?> __doNotUse, Type genericType,
            Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException, WebApplicationException {
        Class<?> genericClass = (Class<?>) genericType;
        if (APPLICATION_JSON.equals(mediaType.toString())) {
            try {
                IOUtils.write(QueryResultsToJSON.toJSON(resultList).toString(4), entityStream);
            } catch (JSONException e) {
                throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
            }
        } else { //RDF
            /*
             * TODO: We would need to add the query to the RDF Result.
             *       Currently not implemented, because I do not want to create
             *       a triple version of the query and there is not yet String
             *       representation defined for FieldQuery
             */
            MGraph resultGraph = QueryResultsToRDF.toRDF(resultList);
            getSerializer().serialize(entityStream, resultGraph, mediaType.toString());
        }
    }


}

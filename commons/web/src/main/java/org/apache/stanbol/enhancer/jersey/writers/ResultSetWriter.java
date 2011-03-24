package org.apache.stanbol.enhancer.jersey.writers;

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
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.clerezza.rdf.core.sparql.ResultSet;
import org.w3c.dom.Document;

/**
 * MessageBodyWriter for a ResultSet.
 * Clerezza does provide such a writer, but it seems to require
 * quite a lot of Clerezza dependencies that we don't really need.
 */
@Provider
@Produces({"application/sparql-results+xml", "application/xml", "text/xml"})
public class ResultSetWriter implements MessageBodyWriter<ResultSet> {

    public static final String ENCODING = "UTF-8";

    @Override
    public long getSize(ResultSet resultSet, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return ResultSet.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(ResultSet resultSet, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {

        try {
            Document doc = new ResultSetToXml().toDocument(resultSet);
            DOMSource domSource = new DOMSource(doc);
            StreamResult streamResult = new StreamResult(entityStream);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer serializer = tf.newTransformer();
            serializer.setOutputProperty(OutputKeys.ENCODING,ENCODING);
            serializer.setOutputProperty(OutputKeys.INDENT,"yes");
            serializer.transform(domSource, streamResult);
        } catch(TransformerException te) {
            throw new IOException("TransformerException in writeTo()", te);
        } catch(ParserConfigurationException pce) {
            throw new IOException("Exception in ResultSetToXml.toDocument()", pce);
        }

        entityStream.flush();
    }
}

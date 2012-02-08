package org.apache.stanbol.enhancer.jersey.writers;

import static org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper.getContentParts;
import static org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper.getMimeTypeWithParameters;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import javax.servlet.ServletContext;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.jena.serializer.JenaSerializerProvider;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.HttpMultipart;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.AbstractContentBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.ContentDescriptor;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.helper.ExecutionMetadataHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.InMemoryBlob;
import org.apache.stanbol.enhancer.servicesapi.helper.InMemoryContentItem;

import com.sun.jersey.core.util.StringKeyIgnoreCaseMultivaluedMap;

@Provider
public class ContentItemWriter implements MessageBodyWriter<ContentItem> {

    protected ServletContext servletContext;
    
    protected Serializer serializer;

    public ContentItemWriter(@Context ServletContext servletContext){
        this.servletContext = servletContext;
        if(servletContext != null){
            serializer = ContextHelper.getServiceFromContext(Serializer.class, servletContext);
        } else {
            serializer = new Serializer();
            serializer.bindSerializingProvider(new JenaSerializerProvider());
        }
    }
    
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(mediaType) &&
                ContentItem.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(ContentItem t,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(ContentItem ci,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType,
                        MultivaluedMap<String,Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException {

        String boundary = "contentItem";
        String charsetName = mediaType.getParameters().get("charset");
        if(charsetName == null){
            charsetName = "UTF-8";
        }
        Charset charset = Charset.forName(charsetName);
        String contentType = String.format("%s/%s; charset=%s; boundary=%s",
            mediaType.getType(),mediaType.getSubtype(),charset.toString(),boundary);
        HttpMultipart entity = new HttpMultipart("from-data", charset,boundary);
        entity.addBodyPart(new FormBodyPart("metadata", new ClerezzaContentBody(
            ci.getUri().getUnicodeString(), ci.getMetadata(),
            //TODO: find a way to parse the intended RDF serialisation format
            SupportedFormat.RDF_XML)));
        
        HttpMultipart content = new HttpMultipart("alternate", Charset.forName("UTF-8"),"contentParts");
        for(Entry<UriRef,Blob> entry : getContentParts(ci,Blob.class).entrySet()){
            content.addBodyPart(new FormBodyPart(entry.getKey().getUnicodeString(), 
                new InputStreamBody(
                    entry.getValue().getStream(),
                    getMimeTypeWithParameters(entry.getValue()),
                    null))); //no file name
        }
        //add all the blobs
        entity.addBodyPart(new FormBodyPart("content",new MultipartContentBody(content, null)));
        //add additional metadata stored in contentParts
        for(Entry<UriRef,TripleCollection> entry : getContentParts(ci, TripleCollection.class).entrySet()){
            entity.addBodyPart(new FormBodyPart(entry.getKey().getUnicodeString(), 
                new ClerezzaContentBody(null, //no file name
                    entry.getValue(),SupportedFormat.RDF_XML)));
        }
        entity.writeTo(entityStream);
        httpHeaders.put(HttpHeaders.CONTENT_TYPE, 
            Collections.singletonList((Object)contentType));
    }

    /**
     * Supports sending multipart mime as {@link ContentBody}.
     * @author Rupert Westenthaler
     *
     */
    private class MultipartContentBody extends AbstractContentBody implements ContentBody,ContentDescriptor {

        private HttpMultipart multipart;
        private String name;

        public MultipartContentBody(HttpMultipart multipart,String name){
            super(String.format("multipart/%s; boundary=%s",
                multipart.getSubType(), multipart.getBoundary()));
            this.name = name;
            this.multipart = multipart;
        }
        @Override
        public String getCharset() {
            return multipart.getCharset().toString();
        }

        @Override
        public String getTransferEncoding() {
            return "7bit";
        }

        @Override
        public long getContentLength() {
            return multipart.getTotalLength();
        }

        @Override
        public String getFilename() {
            return name;
        }

        @Override
        public void writeTo(OutputStream out) throws IOException {
            multipart.writeTo(out);
        }
        
    }
    /**
     * Supports serialised RDF graphs as {@link ContentBody}
     * @author Rupert Westenthaler
     *
     */
    private class ClerezzaContentBody extends AbstractContentBody implements ContentBody,ContentDescriptor {

        private TripleCollection graph;
        private String name;

        protected ClerezzaContentBody(String name, TripleCollection graph, String mimeType){
            super(mimeType);
            this.name = name;
            this.graph = graph;
        }

        @Override
        public String getCharset() {
            return "UTF-8"; //clerezza uses statically UTF-8
        }

        @Override
        public String getTransferEncoding() {
            // TODO Javadoc says 7bit; constants is MIMETYPE define 8bit and binary
            return "7bit";
        }

        @Override
        public long getContentLength() {
            return -1;
        }

        @Override
        public String getFilename() {
            return name;
        }

        @Override
        public void writeTo(OutputStream out) throws IOException {
            serializer.serialize(out, graph, getMediaType()+'/'+getSubType());
        }
        
        
    }
}

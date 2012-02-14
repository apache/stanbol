/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.stanbol.enhancer.jersey.reader;

import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.randomUUID;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.print.attribute.standard.Media;
import javax.servlet.ServletContext;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.MessageBodyReader;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.ParsingProvider;
import org.apache.clerezza.rdf.jena.parser.JenaParserProvider;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.mime.HttpMultipart;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.enhancer.jersey.writers.ContentItemWriter;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.InMemoryBlob;
import org.apache.stanbol.enhancer.servicesapi.helper.InMemoryContentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.core.util.StringKeyIgnoreCaseMultivaluedMap;

public class ContentItemReader implements MessageBodyReader<ContentItem> {
    
    private static Logger log = LoggerFactory.getLogger(ContentItemReader.class);
    FileUpload fu = new FileUpload();
    private ServletContext servletContext;
    private Parser parser;
    
    public static final MediaType MULTIPART = MediaType.valueOf(MediaType.MULTIPART_FORM_DATA_TYPE.getType()+"/*");

    public ContentItemReader(@Context ServletContext context) {
        this.servletContext = context;
        if(context != null){
            this.parser = ContextHelper.getServiceFromContext(Parser.class, context);
        } else { //mainly for unit tests we want also allow initialisation without context
            this.parser = new Parser();
            parser.bindParsingProvider(new JenaParserProvider());
        }
    }
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return ContentItem.class.isAssignableFrom(type);
    }

    @Override
    public ContentItem readFrom(Class<ContentItem> type,
                                Type genericType,
                                Annotation[] annotations,
                                MediaType mediaType,
                                MultivaluedMap<String,String> httpHeaders,
                                InputStream entityStream) throws IOException, WebApplicationException {
        //boolean withMetadata = withMetadata(httpHeaders);
        ContentItem contentItem = null;
        if(mediaType.isCompatible(MULTIPART)){
            //try to read ContentItem from "multipart/from-data"
            MGraph metadata = null;
            FileItemIterator fileItemIterator;
            String contentItemId = null;
            try {
                fileItemIterator = fu.getItemIterator(new MessageBodyReaderContext(entityStream, mediaType));
                while(fileItemIterator.hasNext()){
                    FileItemStream fis = fileItemIterator.next();
                    if(fis.getFieldName().equals("metadata")){
                        if(contentItem != null){
                            throw new WebApplicationException(
                                Response.status(Response.Status.BAD_REQUEST)
                                .entity("The Multipart MIME part with the 'metadata' " +
                                		"MUST BE before the MIME part containing the " +
                                		"'content'!").build());
                        }
                        //the metadata may define the ID for the contentItem
                        if(fis.getName() != null && !fis.getName().isEmpty()){
                            contentItemId = fis.getName();
                        }
                        metadata = new IndexedMGraph();
                        try {
                            parser.parse(metadata, fis.openStream(), fis.getContentType());
                        } catch (Exception e) {
                            throw new WebApplicationException(e, 
                                Response.status(Response.Status.BAD_REQUEST)
                                .entity(String.format("Unable to parse Metadata " +
                                		"from Multipart MIME part '%s' (" +
                                		"contentItem: %s| contentType: %s)",
                                		fis.getFieldName(),fis.getName(),fis.getContentType()))
                                .build());
                        }
                    } else if(fis.getFieldName().equals("content")){
                        contentItem = createContentItem(contentItemId, metadata, fis);
                    } else { //additional metadata as serialised RDF
                        if(contentItem == null){
                            throw new WebApplicationException(
                                Response.status(Response.Status.BAD_REQUEST)
                                .entity("Multipart MIME parts for additional " +
                                		"contentParts MUST BE after the MIME " +
                                		"parts for 'metadata' AND 'content'")
                                .build());
                        }
                        if(fis.getFieldName() == null || fis.getFieldName().isEmpty()){
                            throw new WebApplicationException(
                                Response.status(Response.Status.BAD_REQUEST)
                                .entity("Multipart MIME parts representing " +
                                		"ContentParts for additional RDF metadata" +
                                		"MUST define the contentParts URI as" +
                                		"'name' of the MIME part!").build());
                        }
                        MGraph graph = new IndexedMGraph();
                        try {
                            parser.parse(graph, fis.openStream(), fis.getContentType());
                        } catch (Exception e) {
                            throw new WebApplicationException(e, 
                                Response.status(Response.Status.BAD_REQUEST)
                                .entity(String.format("Unable to parse RDF " +
                                        "for ContentPart '%s' ( contentType: %s)",
                                        fis.getName(),fis.getContentType()))
                                .build());
                        }
                        contentItem.addPart(new UriRef(fis.getFieldName()), graph);
                    }
                }
            } catch (FileUploadException e) {
                throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
            }
        } else { //normal content
            contentItem = new InMemoryContentItem(
                IOUtils.toByteArray(entityStream), mediaType.toString());
        }
        return contentItem;
    }
    private ContentItem createContentItem(String id, MGraph metadata, FileItemStream content) throws IOException, FileUploadException {
        MediaType partContentType = MediaType.valueOf(content.getContentType());
        ContentItem contentItem = null;
        if(partContentType.isCompatible(MULTIPART)){
            //multiple contentParts are parsed
            FileItemIterator contentPartIterator = fu.getItemIterator(
                new MessageBodyReaderContext(
                    content.openStream(), partContentType));
            while(contentPartIterator.hasNext()){
                FileItemStream fis = contentPartIterator.next();
                String contentPartUri = fis.getFieldName();
                if(contentItem == null){
                    contentItem = new InMemoryContentItem(id, 
                        IOUtils.toByteArray(fis.openStream()),
                        fis.getContentType(), metadata);
                } else {
                    Blob blob = new InMemoryBlob(fis.openStream(), fis.getContentType());
                    UriRef contentPartId = null;
                    if(fis.getFieldName() != null && !fis.getFieldName().isEmpty()){
                        contentPartId = new UriRef(fis.getFieldName());
                    } else {
                        //generating a random ID might break metadata 
                        //TODO maybe we should throw an exception instead
                        contentPartId = new UriRef("urn:contentpart:"+ randomUUID());
                    }
                    contentItem.addPart(contentPartId, blob);
                }
            }
        } else {
            contentItem = new InMemoryContentItem(id, 
                IOUtils.toByteArray(content.openStream()),
                content.getContentType(), metadata);
        }
        return contentItem;
    }
    
    /**
     * @param httpHeaders
     * @return if this requests parses an contentItem with metadata
     */
    private boolean withMetadata(MultivaluedMap<String,String> httpHeaders) {
        boolean withMetadata = httpHeaders.containsKey("inputWithMetadata");
        if(withMetadata){
            String value = httpHeaders.getFirst("inputWithMetadata");
            //null empty or "true"
            withMetadata = value == null || value.isEmpty() || Boolean.parseBoolean(value);
        }
        return withMetadata;
    }
    /**
     * Adapter from the parameter present in an {@link MessageBodyReader} to
     * the {@link RequestContext} as used by the commons.fileupload framework
     * @author rwesten
     *
     */
    private static class MessageBodyReaderContext implements RequestContext{

        private final InputStream in;
        private final String contentType;
        private final String charEncoding;

        public MessageBodyReaderContext(InputStream in, MediaType mediaType){
            this.in = in;
            this.contentType = mediaType.toString();
            String charset = mediaType.getParameters().get("charset");
            this.charEncoding = charset == null ? "UTF-8" : charset;
        }
        
        @Override
        public String getCharacterEncoding() {
            return charEncoding;
        }

        @Override
        public String getContentType() {
            return  contentType;
        }

        @Override
        public int getContentLength() {
            return -1;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return in;
        }
        
    }
    
}

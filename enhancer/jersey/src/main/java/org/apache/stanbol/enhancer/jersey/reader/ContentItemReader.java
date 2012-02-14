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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.apache.stanbol.enhancer.jersey.utils.EnhancementPropertiesHelper.ENHANCEMENT_PROPERTIES_URI;
import static org.apache.stanbol.enhancer.jersey.utils.EnhancementPropertiesHelper.PARSED_CONTENT_URIS;
import static org.apache.stanbol.enhancer.jersey.utils.EnhancementPropertiesHelper.getEnhancementProperties;
import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.randomUUID;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.jena.parser.JenaParserProvider;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.io.IOUtils;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.enhancer.jersey.utils.EnhancementPropertiesHelper;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.helper.InMemoryBlob;
import org.apache.stanbol.enhancer.servicesapi.helper.InMemoryContentItem;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.mapped.SimpleConverter;
import org.mortbay.log.Log;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class ContentItemReader implements MessageBodyReader<ContentItem> {
    
    private static Logger log = LoggerFactory.getLogger(ContentItemReader.class);
    FileUpload fu = new FileUpload();
    private Parser __parser;
    private ServletContext context;
    
    public static final MediaType MULTIPART = MediaType.valueOf(MediaType.MULTIPART_FORM_DATA_TYPE.getType()+"/*");

    public ContentItemReader(@Context ServletContext context) {
        this.context = context;
    }
    /**
     * Lazy initialisation for the parser.
     * @return teh parser
     */
    protected final Parser getParser(){
        /*
         * Needed because Jersey tries to create an instance
         * during initialisation. At that time the {@link BundleContext} required
         * by {@link ContextHelper#getServiceFromContext(Class, ServletContext)}
         * is not yet present resulting in an Exception.
         */
        if(__parser == null){
            if(context != null){
                __parser = ContextHelper.getServiceFromContext(Parser.class, context);
            } else { //mainly for unit tests we want also allow initialisation without context
                __parser = new Parser();
                __parser.bindParsingProvider(new JenaParserProvider());
            }
        }
        return __parser;
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
        Set<String> parsedContentIds = new HashSet<String>();
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
                            getParser().parse(metadata, fis.openStream(), fis.getContentType());
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
                        contentItem = createContentItem(contentItemId, metadata, fis, parsedContentIds);
                    } else if(fis.getFieldName().equals("properties") ||
                            fis.getFieldName().equals(ENHANCEMENT_PROPERTIES_URI.getUnicodeString())){
                        //parse the enhancementProperties
                        if(contentItem == null){
                            throw new WebApplicationException(
                                Response.status(Response.Status.BAD_REQUEST)
                                .entity("Multipart MIME parts for " +
                                		"EnhancementProperties MUST BE after the " +
                                		"MIME parts for 'metadata' AND 'content'")
                                .build());
                        }
                        MediaType propMediaType = MediaType.valueOf(fis.getContentType());
                        if(!APPLICATION_JSON_TYPE.isCompatible(propMediaType)){
                            throw new WebApplicationException(
                                Response.status(Response.Status.BAD_REQUEST)
                                .entity("EnhancementProperties (Multipart MIME parts" +
                                		"with the name '"+fis.getFieldName()+"') MUST " +
                                		"BE encoded as 'appicaltion/json' (encountered: '" +
                                		fis.getContentType()+"')!")
                                .build());
                        }
                        String propCharset = propMediaType.getParameters().get("charset");
                        if(propCharset == null){
                            propCharset = "UTF-8";
                        }
                        Map<String,Object> enhancementProperties = getEnhancementProperties(contentItem); 
                        try {
                            enhancementProperties.putAll(toMap(new JSONObject(
                                IOUtils.toString(fis.openStream(),propCharset))));
                        } catch (JSONException e) {
                            throw new WebApplicationException(e,
                                Response.status(Response.Status.BAD_REQUEST)
                                .entity("Unable to parse EnhancementProperties from" +
                                		"Multipart MIME parts with the name 'properties'!")
                                .build());
                        }
                        
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
                            getParser().parse(graph, fis.openStream(), fis.getContentType());
                        } catch (Exception e) {
                            throw new WebApplicationException(e, 
                                Response.status(Response.Status.BAD_REQUEST)
                                .entity(String.format("Unable to parse RDF " +
                                        "for ContentPart '%s' ( contentType: %s)",
                                        fis.getName(),fis.getContentType()))
                                .build());
                        }
                        UriRef contentPartId = new UriRef(fis.getFieldName());
                        contentItem.addPart(contentPartId, graph);
                    }
                }
                if(contentItem == null){
                    throw new WebApplicationException(
                        Response.status(Response.Status.BAD_REQUEST)
                        .entity("The parsed multipart content item does not contain "
                            + "any content. The content is expected to be contained "
                            + "in a MIME part with the name 'content'. This part can "
                            + " be also a 'multipart/alternate' if multiple content "
                            + "parts need to be included in requests.").build());
                }
            } catch (FileUploadException e) {
                throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
            }
        } else { //normal content
            contentItem = new InMemoryContentItem(
                IOUtils.toByteArray(entityStream), mediaType.toString());
            //add the URI of the main content
            parsedContentIds.add(contentItem.getPartUri(0).getUnicodeString());
        }
        //set the parsed contentIDs to the EnhancementProperties
        getEnhancementProperties(contentItem).put(PARSED_CONTENT_URIS, 
            Collections.unmodifiableSet(parsedContentIds));
        return contentItem;
    }
    /**
     * Creates a ContentItem
     * @param id the ID or <code>null</code> if not known
     * @param metadata the metadata or <code>null</code> if not parsed. NOTE that
     * if <code>id == null</code> also <code>metadata == null</code> and 
     * <code>id != null</code> also <code>metadata != null</code>.
     * @param content the {@link FileItemStream} of the MIME part representing
     * the content. If {@link FileItemStream#getContentType()} is compatible with
     * "multipart/*" than this will further parse for multiple parsed content
     * version. In any other case the contents of the parsed {@link FileItemStream}
     * will be directly add as content for the {@link ContentItem} created by
     * this method.
     * @param parsedContentParts used to add the IDs of parsed contentParts 
     * @return the created content item
     * @throws IOException on any error while accessing the contents of the parsed
     * {@link FileItemStream}
     * @throws FileUploadException if the parsed contents are not correctly
     * encoded Multipoart MIME
     */
    private ContentItem createContentItem(String id, MGraph metadata, FileItemStream content,Set<String> parsedContentParts) throws IOException, FileUploadException {
        MediaType partContentType = MediaType.valueOf(content.getContentType());
        ContentItem contentItem = null;
        if(MULTIPART.isCompatible(partContentType)){
            //multiple contentParts are parsed
            FileItemIterator contentPartIterator = fu.getItemIterator(
                new MessageBodyReaderContext(
                    content.openStream(), partContentType));
            while(contentPartIterator.hasNext()){
                FileItemStream fis = contentPartIterator.next();
                if(contentItem == null){
                    log.debug("create ContentItem {} for content (type:{})",
                        id,content.getContentType());
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
                    log.debug("  ... add Blob {} to ContentItem {} with content (type:{})",
                        new Object[]{contentPartId, id, fis.getContentType()});
                    contentItem.addPart(contentPartId, blob);
                    parsedContentParts.add(contentPartId.getUnicodeString());
                }
            }
        } else {
            log.debug("create ContentItem {} for content (type:{})",
                id,content.getContentType());
            contentItem = new InMemoryContentItem(id, 
                IOUtils.toByteArray(content.openStream()),
                content.getContentType(), metadata);
        }
        //add the URI of the main content to the parsed contentParts
        parsedContentParts.add(contentItem.getPartUri(0).getUnicodeString());
        return contentItem;
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
    /**
     * Converts a JSON object to a java Map. Nested JSONArrays are converted
     * to collections and nested JSONObjects are converted to Maps.
     * @param object
     * @return
     * @throws JSONException
     */
    private Map<String,Object> toMap(JSONObject object) throws JSONException {
        Map<String,Object> data = new HashMap<String,Object>();
        for(Iterator<?> keys = object.keys();keys.hasNext();){
            String key = (String)keys.next();
            data.put(key, getValue(object.get(key)));
        }
        
        return data;
    }
    /**
     * @param object
     * @param data
     * @param key
     * @throws JSONException
     */
    private Object getValue(Object value) throws JSONException {
        if(value instanceof JSONObject){
            return toMap((JSONObject)value);
        } else if(value instanceof JSONArray){
            Collection<Object> values =  new ArrayList<Object>(((JSONArray)value).length());
            for(int i=0;i<((JSONArray)value).length();i++){
                values.add(getValue(((JSONArray)value).get(i)));
            }
            return values;
        } else {
            return value;
        }
    }
    
}

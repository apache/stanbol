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
package org.apache.stanbol.enhancer.jersey.writers;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.MediaType.WILDCARD_TYPE;
import static org.apache.stanbol.enhancer.jersey.utils.EnhancementPropertiesHelper.ENHANCEMENT_PROPERTIES_URI;
import static org.apache.stanbol.enhancer.jersey.utils.EnhancementPropertiesHelper.getEnhancementProperties;
import static org.apache.stanbol.enhancer.jersey.utils.EnhancementPropertiesHelper.getOutputContent;
import static org.apache.stanbol.enhancer.jersey.utils.EnhancementPropertiesHelper.getOutputContentParts;
import static org.apache.stanbol.enhancer.jersey.utils.EnhancementPropertiesHelper.getParsedContentURIs;
import static org.apache.stanbol.enhancer.jersey.utils.EnhancementPropertiesHelper.isOmitMetadata;
import static org.apache.stanbol.enhancer.jersey.utils.EnhancementPropertiesHelper.isOmitParsedContent;
import static org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper.getBlob;
import static org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper.getContentParts;
import static org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper.getMimeTypeWithParameters;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.jena.serializer.JenaSerializerProvider;
import org.apache.commons.io.IOUtils;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.HttpMultipart;
import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.content.AbstractContentBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.ContentDescriptor;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.writers.JsonLdSerializerProvider;
import org.apache.stanbol.enhancer.jersey.utils.EnhancementPropertiesHelper;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

@Provider
public class ContentItemWriter implements MessageBodyWriter<ContentItem> {

    /**
     * The "multipart/*" wilrcard
     */
    private static final MediaType MULTIPART = MediaType.valueOf(MULTIPART_FORM_DATA_TYPE.getType()+"/*");
    private static final String CONTENT_ITEM_BOUNDARY = "contentItem";
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final MediaType DEFAULT_RDF_FORMAT = new MediaType(
        APPLICATION_JSON_TYPE.getType(),
        APPLICATION_JSON_TYPE.getSubtype(),
        Collections.singletonMap("charset", UTF8.toString()));
    
    private Serializer __serializer;
    
    private ServletContext context;

    public ContentItemWriter(@Context ServletContext context){
        this.context = context;
    }
    /**
     * Lazzy initialisation for the {@link Serializer}
     * @return the {@link Serializer}
     */
    protected final Serializer getSerializer(){
        /*
         * Needed because Jersey tries to create an instance
         * during initialisation. At that time the {@link BundleContext} required
         * by {@link ContextHelper#getServiceFromContext(Class, ServletContext)}
         * is not yet present resulting in an Exception.
         */
        if(__serializer == null){
            if(context != null){
                __serializer = ContextHelper.getServiceFromContext(Serializer.class, context);
            } else {
                __serializer = new Serializer();
                __serializer.bindSerializingProvider(new JenaSerializerProvider());
                __serializer.bindSerializingProvider(new JsonLdSerializerProvider());
            }
        }
        return __serializer;
    }
    
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return //MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(mediaType) &&
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

        //(0) handle default dataType
        Map<String,Object> properties = getEnhancementProperties(ci);
        boolean omitMetadata = isOmitMetadata(properties);
        if(!MULTIPART.isCompatible(mediaType)){ //two possible cases
            if(!omitMetadata){ //  (1) just return the RDF data
                //(1.a) Backward support for default dataType if no Accept header is set
                if (mediaType.isWildcardType() || 
                        TEXT_PLAIN_TYPE.isCompatible(mediaType) || 
                        APPLICATION_OCTET_STREAM_TYPE.isCompatible(mediaType)) {
                    mediaType = new MediaType(APPLICATION_JSON_TYPE.getType(), 
                        APPLICATION_JSON_TYPE.getSubtype(),
                        //Clerezza serialisers are hard coded to use UTF-8
                        Collections.singletonMap("charset", UTF8.toString()));
                    httpHeaders.putSingle("Content-Type", mediaType.toString());
                }
                getSerializer().serialize(entityStream, ci.getMetadata(), mediaType.toString());
            } else { //  (2) return a single content part
                Entry<UriRef,Blob> contentPart = getBlob(ci, Collections.singleton(mediaType.toString()));
                if(contentPart == null){ //no alternate content with the requeste media type
                    throw new WebApplicationException(
                        Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                        .entity("The requested enhancement chain has not created an " +
                        		"version of the parsed content in the reuqest media " +
                        		"type "+mediaType.toString())
                        .build());
                } else { //found -> stream the content to the client
                    //NOTE: This assumes that the presence of a charset
                    //      implies reading/writing character streams
                    String requestedCharset = mediaType.getParameters().get("charset");
                    String blobCharset = contentPart.getValue().getParameter().get("charset");
                    Charset readerCharset = blobCharset == null ? UTF8 : Charset.forName(blobCharset);
                    Charset writerCharset = requestedCharset == null ? null : Charset.forName(requestedCharset);
                    if(writerCharset != null && !writerCharset.equals(readerCharset)){
                        //we need to transcode
                        Reader reader = new InputStreamReader(
                            contentPart.getValue().getStream(),readerCharset);
                        Writer writer = new OutputStreamWriter(entityStream, writerCharset);
                        IOUtils.copy(reader, writer);
                        IOUtils.closeQuietly(reader);
                    } else { //no transcoding
                        if(requestedCharset == null && blobCharset != null){
                            httpHeaders.putSingle(HttpHeaders.CONTENT_TYPE, 
                                mediaType.toString()+"; charset="+blobCharset);
                        }
                        InputStream in  = contentPart.getValue().getStream();
                        IOUtils.copy(in, entityStream);
                        IOUtils.closeQuietly(in);
                    }
                }
            }
        } else { // multipart mime requested!
            final String charsetName = mediaType.getParameters().get("charset");
            final Charset charset = charsetName != null ? Charset.forName(charsetName) : UTF8;
            MediaType rdfFormat;
            String rdfFormatString = EnhancementPropertiesHelper.getRdfFormat(properties);
            if(rdfFormatString == null || rdfFormatString.isEmpty()){
                rdfFormat = DEFAULT_RDF_FORMAT;
            } else {
                try {
                    rdfFormat = MediaType.valueOf(rdfFormatString);
                    if(rdfFormat.getParameters().get("charset") == null){
                        //use the charset of the default RDF format
                        rdfFormat = new MediaType(
                            rdfFormat.getType(), rdfFormat.getSubtype(), 
                            DEFAULT_RDF_FORMAT.getParameters());
                    }
                } catch (IllegalArgumentException e) {
                    throw new WebApplicationException(e, 
                        Response.status(Response.Status.BAD_REQUEST)
                        .entity(String.format("The specified RDF format '%s' (used "
                            + " to serialize all RDF parts of multipart MIME responses)"
                            + " is not a well formated MIME type",rdfFormatString))
                        .build());
                }
            }
            //(1) setting the correct header
            String contentType = String.format("%s/%s; charset=%s; boundary=%s",
                mediaType.getType(),mediaType.getSubtype(),charset.toString(),CONTENT_ITEM_BOUNDARY);
            httpHeaders.putSingle(HttpHeaders.CONTENT_TYPE,contentType);
            HttpMultipart entity = new HttpMultipart("from-data", charset ,CONTENT_ITEM_BOUNDARY);
            //(2) serialising the metadata
            if(!isOmitMetadata(properties)){
                entity.addBodyPart(new FormBodyPart("metadata", new ClerezzaContentBody(
                    ci.getUri().getUnicodeString(), ci.getMetadata(),
                    rdfFormat)));
            }
            //(3) serialising the Content (Bloby)
            //(3.a) Filter based on parameter
            List<Entry<UriRef,Blob>> includedBlobs = filterBlobs(ci, properties);
            //(3.b) Serialise the filtered
            if(!includedBlobs.isEmpty()) {
                HttpMultipart content = new HttpMultipart("alternate", UTF8 ,"contentParts");
                for(Entry<UriRef,Blob> entry : includedBlobs){
                    content.addBodyPart(new FormBodyPart(entry.getKey().getUnicodeString(), 
                        new BlobContentBody(entry.getValue()))); //no file name
                }
                //add all the blobs
                entity.addBodyPart(new FormBodyPart("content",new MultipartContentBody(content, null)));
            } //else no content to include
            Set<String> includeContentParts = getIncludedContentPartURIs(properties);
            if(includeContentParts != null){
                //(4) serialise EnhancementProperties
                if(includeContentParts.isEmpty() || includeContentParts.contains(
                    ENHANCEMENT_PROPERTIES_URI.getUnicodeString())) {
                    JSONObject object;
                    try {
                        object = toJson(properties);
                    } catch (JSONException e) {
                        throw new WebApplicationException(e,
                            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity("Unable to convert EnhancementProperties to " +
                            		"JSON (values : "+properties+")!").build());
                    }
                    entity.addBodyPart(new FormBodyPart(
                        ENHANCEMENT_PROPERTIES_URI.getUnicodeString(), 
                        new StringBody(object.toString(),MediaType.APPLICATION_JSON,UTF8)));
                }
                //(5) additional RDF metadata stored in contentParts
                for(Entry<UriRef,TripleCollection> entry : getContentParts(ci, TripleCollection.class).entrySet()){
                    if(includeContentParts.isEmpty() || includeContentParts.contains(
                        entry.getKey())){
                        entity.addBodyPart(new FormBodyPart(entry.getKey().getUnicodeString(), 
                            new ClerezzaContentBody(null, //no file name
                                entry.getValue(),rdfFormat)));
                    } // else ignore this content part
                }
            }
            entity.writeTo(entityStream);
        }   
            
    }
    /**
     * @param properties
     * @return
     */
    private JSONObject toJson(Map<?,?> map) throws JSONException {
        JSONObject object = new JSONObject();
        for(Entry<?,?> entry : map.entrySet()){
            Object value = getValue(entry.getValue());
            object.put(entry.getKey().toString(),value);
        }
        return object;
    }
    /**
     * @param entry
     * @return
     * @throws JSONException
     */
    private Object getValue(Object javaValue) throws JSONException {
        Object value;
        if(javaValue instanceof Collection<?>){
            value = new JSONArray();
            for(Object o : (Collection<?>)javaValue){
                ((JSONArray)value).put(getValue(o));
            }
        } else if(javaValue instanceof Map<?,?>){
            value = toJson((Map<?,?>)javaValue);
        } else {
            value = javaValue;
        }
        return value;
    }
    /**
     * @param properties
     * @return
     */
    private Set<String> getIncludedContentPartURIs(Map<String,Object> properties) {
        Collection<String> ocp = getOutputContentParts(properties);
        if(ocp == null || ocp.isEmpty()){
            return null;
        }
        Set<String> includeContentParts = new HashSet<String>(ocp);
        if(includeContentParts != null){
            if(includeContentParts.isEmpty()){ //empty == none
                includeContentParts = null;
            } else if (includeContentParts.contains("*")){ // * == all -> empty list
                includeContentParts = Collections.emptySet();
            }
        }
        return includeContentParts;
    }
    /**
     * @param ci
     * @param properties
     * @return
     */
    private List<Entry<UriRef,Blob>> filterBlobs(ContentItem ci, Map<String,Object> properties) {
        final List<Entry<UriRef,Blob>> includedContentPartList;
        Set<MediaType> includeMediaTypes = getIncludedMediaTypes(properties);
        if(includeMediaTypes == null){
            includedContentPartList = Collections.emptyList();
        } else {
            includedContentPartList = new ArrayList<Map.Entry<UriRef,Blob>>();
            Set<String> ignoreContentPartUris = getIgnoredContentURIs(properties);
            nextContentPartEntry: 
            for(Entry<UriRef,Blob> entry : getContentParts(ci,Blob.class).entrySet()){
                if(!ignoreContentPartUris.contains(entry.getKey().getUnicodeString())){
                    Blob blob = entry.getValue();
                    MediaType blobMediaType = MediaType.valueOf(blob.getMimeType());
                    for(MediaType included : includeMediaTypes) {
                        if(blobMediaType.isCompatible(included)){
                            includedContentPartList.add(entry);
                            continue nextContentPartEntry;
                        }
                    }
                } //else ignore this Blob
            }
        }
        return includedContentPartList;
    }
    /**
     * @param properties
     * @return
     */
    private Set<String> getIgnoredContentURIs(Map<String,Object> properties) {
        Set<String> ignoreContentPartUris = isOmitParsedContent(properties) ?
                new HashSet<String>(getParsedContentURIs(properties)) : null;
        if(ignoreContentPartUris == null){
            ignoreContentPartUris = Collections.emptySet();
        }
        return ignoreContentPartUris;
    }
    /**
     * @param properties
     * @return
     */
    private Set<MediaType> getIncludedMediaTypes(Map<String,Object> properties) throws WebApplicationException {
        Collection<String> includeMediaTypeStrings = getOutputContent(properties);
        if(includeMediaTypeStrings == null){
            return null;
        }
        Set<MediaType> includeMediaTypes = new HashSet<MediaType>(includeMediaTypeStrings.size());
        for(String includeString : includeMediaTypeStrings){
            if(includeString != null){
                includeString.trim();
                if(!includeString.isEmpty()){
                    if("*".equals(includeString)){ //also support '*' for '*/*'
                        includeMediaTypes.add(WILDCARD_TYPE);
                    } else {
                        try {
                            includeMediaTypes.add(MediaType.valueOf(includeString));
                        } catch (IllegalArgumentException e){
                            throw new WebApplicationException(e, 
                                Response.status(Response.Status.BAD_REQUEST)
                                .entity("The parsed outputContent parameter "
                                    + includeMediaTypeStrings +" contain an "
                                    + "illegal formated MediaType!")
                                .build());
                        }
                    }
                }
            }
        }
        if(includeMediaTypes.contains(WILDCARD_TYPE)){
            includeMediaTypes = Collections.singleton(WILDCARD_TYPE);
        }
        return includeMediaTypes;
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
            return MIME.ENC_8BIT;
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
        private String charset;
        private String name;

        protected ClerezzaContentBody(String name, TripleCollection graph, MediaType mimeType){
            super(mimeType.getType()+'/'+mimeType.getSubtype());
            charset = mimeType.getParameters().get("charset");
            if(charset == null || charset.isEmpty()){
                charset = UTF8.toString();
            }
            this.name = name;
            this.graph = graph;
        }

        @Override
        public String getCharset() {
            return charset;
        }

        @Override
        public String getTransferEncoding() {
            return MIME.ENC_8BIT;
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
            getSerializer().serialize(out, graph, getMediaType()+'/'+getSubType());
        }
    }
    private class BlobContentBody extends AbstractContentBody {
        
        private Blob blob;

        public BlobContentBody(Blob blob) {
            super(blob.getMimeType());
            this.blob = blob;
        }

        @Override
        public String getFilename() {
            return null;
        }

        @Override
        public void writeTo(OutputStream out) throws IOException {
            InputStream in = blob.getStream();
            IOUtils.copy(in, out);
            IOUtils.closeQuietly(in);
        }

        @Override
        public String getCharset() {
            return blob.getParameter().get("charset");
        }

        @Override
        public String getTransferEncoding() {
            return blob.getParameter().get("charset") == null ?
                    MIME.ENC_BINARY : MIME.ENC_8BIT;
        }

        @Override
        public long getContentLength() {
            return -1;
        }
        
    }
    
}

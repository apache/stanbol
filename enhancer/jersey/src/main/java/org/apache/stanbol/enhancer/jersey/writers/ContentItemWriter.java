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

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.MediaType.WILDCARD_TYPE;
import static org.apache.stanbol.enhancer.jersey.utils.RequestPropertiesHelper.REQUEST_PROPERTIES_URI;
import static org.apache.stanbol.enhancer.jersey.utils.RequestPropertiesHelper.getOutputContent;
import static org.apache.stanbol.enhancer.jersey.utils.RequestPropertiesHelper.getOutputContentParts;
import static org.apache.stanbol.enhancer.jersey.utils.RequestPropertiesHelper.getParsedContentURIs;
import static org.apache.stanbol.enhancer.jersey.utils.RequestPropertiesHelper.getRdfFormat;
import static org.apache.stanbol.enhancer.jersey.utils.RequestPropertiesHelper.isOmitMetadata;
import static org.apache.stanbol.enhancer.jersey.utils.RequestPropertiesHelper.isOmitParsedContent;
import static org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper.getBlob;
import static org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper.getContentParts;

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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.UnsupportedSerializationFormatException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.AbstractContentBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.ContentDescriptor;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.message.BasicNameValuePair;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service(Object.class)
@Property(name = "javax.ws.rs", boolValue = true)
@Provider
public class ContentItemWriter implements MessageBodyWriter<ContentItem> {

    public static final String CONTENT_ITEM_BOUNDARY;
    public static final String CONTENT_PARTS_BOUNDERY;;
    /**
     * The pool of ASCII chars to be used for generating a multipart boundary.
     */
    private final static char[] MULTIPART_CHARS =
            "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
                    .toCharArray();
    static {
        final Random rand = new Random();
        final int count = rand.nextInt(11) + 10; // a random size from 10 to 20
        StringBuilder randomString = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            randomString.append(MULTIPART_CHARS[rand.nextInt(MULTIPART_CHARS.length)]);
        }
        CONTENT_ITEM_BOUNDARY = "contentItem-"+randomString;
        CONTENT_PARTS_BOUNDERY = "contentParts-"+randomString;
    }
    private static final ContentType MULTIPART_ALTERNATE = ContentType.create("multipart/alternate");

    Logger log = LoggerFactory.getLogger(ContentItemWriter.class);
    
    /**
     * The "multipart/*" wilrcard
     */
    private static final MediaType MULTIPART = MediaType.valueOf(MULTIPART_FORM_DATA_TYPE.getType()+"/*");
    private static final Charset UTF8 = Charset.forName("UTF-8");
    /**
     * The media type for JSON-LD (<code>application/ld+json</code>)
     */
    private static String APPLICATION_LD_JSON = "application/ld+json";
    private static MediaType APPLICATION_LD_JSON_TYPE = MediaType.valueOf(APPLICATION_LD_JSON);
    private static final MediaType DEFAULT_RDF_FORMAT = new MediaType(
        APPLICATION_LD_JSON_TYPE.getType(), 
        APPLICATION_LD_JSON_TYPE.getSubtype(), 
        Collections.singletonMap("charset", UTF8.name()));
    
    @Reference
    private Serializer serializer;
    
    /**
     * Default Constructor used by OSGI. This expects that the {@link #serializer}
     * is injected
     */
    public ContentItemWriter(){};
    /**
     * Creates a {@link ContentItemWriter} by using the parsed Clerezza
     * {@link Serializer}. Intended to be used by unit tests or when running not
     * in an OSGI environment.
     * @param serializer
     */
    public ContentItemWriter(Serializer serializer) {
		this.serializer = serializer;
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
        Map<String,Object> reqProp = ContentItemHelper.getRequestPropertiesContentPart(ci);
        boolean omitMetadata = isOmitMetadata(reqProp);
        if(!MULTIPART.isCompatible(mediaType)){ //two possible cases
            if(!omitMetadata){ //  (1) just return the RDF data
                //(1.a) Backward support for default dataType if no Accept header is set
                StringBuilder ctb = new StringBuilder();
                if (mediaType.isWildcardType() || 
                        TEXT_PLAIN_TYPE.isCompatible(mediaType) || 
                        APPLICATION_OCTET_STREAM_TYPE.isCompatible(mediaType)) {
                    ctb.append(APPLICATION_LD_JSON);
                } else {
                    ctb.append(mediaType.getType()).append('/').append(mediaType.getSubtype());
                }
                ctb.append(";charset=").append(UTF8.name());
                String contentType = ctb.toString();
                httpHeaders.putSingle(HttpHeaders.CONTENT_TYPE, contentType);
                try {
                	serializer.serialize(entityStream, ci.getMetadata(), contentType);
                } catch (UnsupportedSerializationFormatException e) {
                    throw new WebApplicationException("The enhancement results "
                        + "cannot be serialized in the requested media type: "
                        + mediaType.toString(),Response.Status.NOT_ACCEPTABLE);
                }
            } else { //  (2) return a single content part
                Entry<IRI,Blob> contentPart = getBlob(ci, Collections.singleton(mediaType.toString()));
                if(contentPart == null){ //no alternate content with the requeste media type
                    throw new WebApplicationException("The requested enhancement chain has not created an "
                            + "version of the parsed content in the reuqest media type "
                            + mediaType.toString(),Response.Status.UNSUPPORTED_MEDIA_TYPE);
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
            String rdfFormatString = getRdfFormat(reqProp);
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
                    throw new WebApplicationException("The specified RDF format '"
                        + rdfFormatString +"' (used to serialize all RDF parts of " 
                        + "multipart MIME responses) is not a well formated MIME type",
                        Response.Status.BAD_REQUEST);
                }
            }
            //(1) setting the correct header
            String contentType = String.format("%s/%s; charset=%s; boundary=%s",
                mediaType.getType(),mediaType.getSubtype(),charset.toString(),CONTENT_ITEM_BOUNDARY);
            httpHeaders.putSingle(HttpHeaders.CONTENT_TYPE,contentType);
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
            entityBuilder.setBoundary(CONTENT_ITEM_BOUNDARY);
            //HttpMultipart entity = new HttpMultipart("from-data", charset ,CONTENT_ITEM_BOUNDARY);
            //(2) serialising the metadata
            if(!isOmitMetadata(reqProp)){
                entityBuilder.addPart("metadata", new ClerezzaContentBody(
                    ci.getUri().getUnicodeString(), ci.getMetadata(), rdfFormat));
//                entity.addBodyPart(new FormBodyPart("metadata", new ClerezzaContentBody(
//                    ci.getUri().getUnicodeString(), ci.getMetadata(),
//                    rdfFormat)));
            }
            //(3) serialising the Content (Bloby)
            //(3.a) Filter based on parameter
            List<Entry<IRI,Blob>> includedBlobs = filterBlobs(ci, reqProp);
            //(3.b) Serialise the filtered
            if(!includedBlobs.isEmpty()) {
                Map<String,ContentBody> contentParts = new LinkedHashMap<String,ContentBody>();
                for(Entry<IRI,Blob> entry : includedBlobs){
                    Blob blob = entry.getValue();
                    ContentType ct = ContentType.create(blob.getMimeType());
                    String cs = blob.getParameter().get("charset");
                    if(StringUtils.isNotBlank(cs)){
                        ct = ct.withCharset(cs);
                    }
                    contentParts.put(entry.getKey().getUnicodeString(), 
                        new InputStreamBody(blob.getStream(),ct));
                }
                //add all the blobs
                entityBuilder.addPart("content", new MultipartContentBody(contentParts,
                    CONTENT_PARTS_BOUNDERY, MULTIPART_ALTERNATE));
            } //else no content to include
            Set<String> includeContentParts = getIncludedContentPartURIs(reqProp);
            if(includeContentParts != null){
                //(4) serialise the Request Properties
                if(includeContentParts.isEmpty() || includeContentParts.contains(
                    REQUEST_PROPERTIES_URI.getUnicodeString())) {
                    JSONObject object;
                    try {
                        object = toJson(reqProp);
                    } catch (JSONException e) {
                        String message = "Unable to convert Request Properties " 
                                + "to JSON (values : "+reqProp+")!";
                        log.error(message,e);
                        throw new WebApplicationException(message, Response.Status.INTERNAL_SERVER_ERROR);
                    }
                    entityBuilder.addTextBody(
                        REQUEST_PROPERTIES_URI.getUnicodeString(), object.toString(),
                         ContentType.APPLICATION_JSON.withCharset(UTF8));
                }
                //(5) additional RDF metadata stored in contentParts
                for(Entry<IRI,Graph> entry : getContentParts(ci, Graph.class).entrySet()){
                    if(includeContentParts.isEmpty() || includeContentParts.contains(
                        entry.getKey())){
                        entityBuilder.addPart(entry.getKey().getUnicodeString(), 
                            new ClerezzaContentBody(null, //no file name
                                entry.getValue(),rdfFormat));
                    } // else ignore this content part
                }
            }
            entityBuilder.build().writeTo(entityStream);
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
    private List<Entry<IRI,Blob>> filterBlobs(ContentItem ci, Map<String,Object> properties) {
        final List<Entry<IRI,Blob>> includedContentPartList;
        Set<MediaType> includeMediaTypes = getIncludedMediaTypes(properties);
        if(includeMediaTypes == null){
            includedContentPartList = Collections.emptyList();
        } else {
            includedContentPartList = new ArrayList<Map.Entry<IRI,Blob>>();
            Set<String> ignoreContentPartUris = getIgnoredContentURIs(properties);
            nextContentPartEntry: 
            for(Entry<IRI,Blob> entry : getContentParts(ci,Blob.class).entrySet()){
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
                includeString = includeString.trim();
                if(!includeString.isEmpty()){
                    if("*".equals(includeString)){ //also support '*' for '*/*'
                        includeMediaTypes.add(WILDCARD_TYPE);
                    } else {
                        try {
                            includeMediaTypes.add(MediaType.valueOf(includeString));
                        } catch (IllegalArgumentException e){
                            throw new WebApplicationException("The parsed outputContent "
                                + "parameter " + includeMediaTypeStrings +" contain an "
                                + "illegal formated MediaType!", Response.Status.BAD_REQUEST);
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

        private Map<String,ContentBody> parts;
        private String boundary;

        public MultipartContentBody(Map<String,ContentBody> parts, String boundary, ContentType contentType){
            super(ContentType.create(contentType.getMimeType(), new BasicNameValuePair("boundary",boundary)));
            this.parts = parts;
            this.boundary = boundary;
        }
//        @Override
//        public String getCharset() {
//            return null; //no charset for multipart parts
//        }
//        @Override
//        public String getMimeType() {
//            String mime = new StringBuilder(super.getMimeType()).append("; boundary=")
//                    .append(boundary).toString();
//            log.info("!!! {}",mime);
//            return mime;
//            
//        }
        @Override
        public String getTransferEncoding() {
            return MIME.ENC_8BIT;
        }

        @Override
        public long getContentLength() {
            //not known as we would need to count the content length AND
            //the length of the different mime headers.
            return -1;
        }

        @Override
        public String getFilename() {
            return null;
        }

        @Override
        public void writeTo(OutputStream out) throws IOException {
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setBoundary(boundary);
            for(Entry<String,ContentBody> part : parts.entrySet()){
                builder.addPart(part.getKey(), part.getValue());
            }
            HttpEntity entity = builder.build();
            entity.writeTo(out);
        }
        
    }
    /**
     * Supports serialised RDF graphs as {@link ContentBody}
     * @author Rupert Westenthaler
     *
     */
    private class ClerezzaContentBody extends AbstractContentBody implements ContentBody,ContentDescriptor {

        private Graph graph;
        private String charset;
        private String name;

        protected ClerezzaContentBody(String name, Graph graph, MediaType mimeType){
            super(ContentType.create(new StringBuilder(mimeType.getType())
            .append('/').append(mimeType.getSubtype()).toString(), UTF8));
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
            serializer.serialize(out, graph, getMediaType()+'/'+getSubType());
        }
    }
    
}

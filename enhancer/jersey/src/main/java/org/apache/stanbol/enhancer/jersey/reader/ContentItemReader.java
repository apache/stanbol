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
import static org.apache.stanbol.enhancer.jersey.utils.RequestPropertiesHelper.REQUEST_PROPERTIES_URI;
import static org.apache.stanbol.enhancer.jersey.utils.RequestPropertiesHelper.PARSED_CONTENT_URIS;
import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.randomUUID;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_CREATED;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_CREATOR;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_LANGUAGE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_EXTRACTED_FROM;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.DCTERMS_LINGUISTIC_SYSTEM;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_ENHANCEMENT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_TEXTANNOTATION;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.StreamSource;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service(Object.class)
@Property(name = "javax.ws.rs", boolValue = true)
@Provider
public class ContentItemReader implements MessageBodyReader<ContentItem> {
    
    
    private static Logger log = LoggerFactory.getLogger(ContentItemReader.class);
    FileUpload fu = new FileUpload();
    @Reference
    private Parser parser;

    @Reference
    private ContentItemFactory ciFactory;
    
    /**
     * Used to read the queryParameter with the ContentItem ID
     */
    @Context
    UriInfo uriInfo;
    /**
     * Used to access the Content-Language header for STANBOL-660
     */
    @Context
    HttpHeaders headers;
    /**
     * Clerezza LiteralFactory
     */
    private LiteralFactory lf = LiteralFactory.getInstance();

    public static final MediaType MULTIPART = MediaType.valueOf(MediaType.MULTIPART_FORM_DATA_TYPE.getType()+"/*");

    
    protected Parser getParser(){
        return parser;
    }
    protected ContentItemFactory getContentItemFactory(){
        return ciFactory;
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
        IRI contentItemId = getContentItemId();
        if(log.isTraceEnabled()){
            //NOTE: enabling TRACE level logging will copy the parsed content
            //      into a BYTE array
            log.trace("Parse ContentItem from");
            log.trace("  - MediaType: {}",mediaType);
            log.trace("  - Headers:");
            for(Entry<String,List<String>> header : httpHeaders.entrySet()){
                log.trace("      {}: {}", header.getKey(), header.getValue());
            }
            byte[] content = IOUtils.toByteArray(entityStream);
            log.trace("content: \n{}", new String(content,"UTF-8"));
            IOUtils.closeQuietly(entityStream);
            entityStream = new ByteArrayInputStream(content);
        }
        Set<String> parsedContentIds = new HashSet<String>();
        if(mediaType.isCompatible(MULTIPART)){
            log.debug(" - parse Multipart MIME ContentItem");
            //try to read ContentItem from "multipart/from-data"
            Graph metadata = null;
            FileItemIterator fileItemIterator;
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
                        //only used if not parsed as query param
                        if(contentItemId == null && fis.getName() != null && !fis.getName().isEmpty()){
                            contentItemId = new IRI(fis.getName());
                        }
                        metadata = new IndexedGraph();
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
                            fis.getFieldName().equals(REQUEST_PROPERTIES_URI.getUnicodeString())){
                        //parse the RequestProperties
                        if(contentItem == null){
                            throw new WebApplicationException(
                                Response.status(Response.Status.BAD_REQUEST)
                                .entity("Multipart MIME parts for " +
                                		"Request Properties MUST BE after the " +
                                		"MIME parts for 'metadata' AND 'content'")
                                .build());
                        }
                        MediaType propMediaType = MediaType.valueOf(fis.getContentType());
                        if(!APPLICATION_JSON_TYPE.isCompatible(propMediaType)){
                            throw new WebApplicationException(
                                Response.status(Response.Status.BAD_REQUEST)
                                .entity("Request Properties (Multipart MIME parts" +
                                		"with the name '"+fis.getFieldName()+"') MUST " +
                                		"BE encoded as 'appicaltion/json' (encountered: '" +
                                		fis.getContentType()+"')!")
                                .build());
                        }
                        String propCharset = propMediaType.getParameters().get("charset");
                        if(propCharset == null){
                            propCharset = "UTF-8";
                        }
                        Map<String,Object> reqProp = 
                                ContentItemHelper.initRequestPropertiesContentPart(contentItem); 
                        try {
                            reqProp.putAll(toMap(new JSONObject(
                                IOUtils.toString(fis.openStream(),propCharset))));
                        } catch (JSONException e) {
                            throw new WebApplicationException(e,
                                Response.status(Response.Status.BAD_REQUEST)
                                .entity("Unable to parse Request Properties from" +
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
                        Graph graph = new IndexedGraph();
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
                        IRI contentPartId = new IRI(fis.getFieldName());
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
            ContentItemFactory ciFactory = getContentItemFactory();
            contentItem = ciFactory.createContentItem(contentItemId,
                new StreamSource(entityStream, mediaType.toString()));
            //add the URI of the main content
            parsedContentIds.add(contentItem.getPartUri(0).getUnicodeString());
        }
        //set the parsed contentIDs to the EnhancementProperties
        Map<String,Object> ep = ContentItemHelper.initRequestPropertiesContentPart(contentItem);
        parseEnhancementPropertiesFromParameters(ep);
        ep.put(PARSED_CONTENT_URIS, Collections.unmodifiableSet(parsedContentIds));
        
        //STANBOL-660: set the language of the content if explicitly parsed in the request
        String contentLanguage = getContentLanguage();
        if(!StringUtils.isBlank(contentLanguage)){
            //language codes are case insensitive ... so we convert to lower case
            contentLanguage = contentLanguage.toLowerCase(Locale.ROOT);
            createParsedLanguageAnnotation(contentItem,contentLanguage);
// previously only the dc:language property was set to the contentItem. However this
// information is only used as fallback if no Language annotation is present. However
// if a user explicitly parses the language he expects this language to be used
// so this was change with STANBOL-1417
//            EnhancementEngineHelper.set(contentItem.getMetadata(), contentItem.getUri(), 
//                DC_LANGUAGE, new PlainLiteralImpl(contentLanguage));
        }
        return contentItem;
    }
    /**
     * Creates a fise:TextAnnotation for the explicitly parsed Content-Language
     * header. The confidence of this annotation is set <code>1.0</code> (see 
     * <a href="https://issues.apache.org/jira/browse/STANBOL-1417">STANBOL-1417</a>).
     * @param ci the {@link ContentItem} to the the language annotation
     * @param lang the parsed language
     */
    private void createParsedLanguageAnnotation(ContentItem ci, String lang){
        Graph m = ci.getMetadata();
        IRI la = new IRI("urn:enhancement-"+ EnhancementEngineHelper.randomUUID());
        //add the fise:Enhancement information
        m.add(new TripleImpl(la, RDF_TYPE, ENHANCER_ENHANCEMENT));
        m.add(new TripleImpl(la, RDF_TYPE, ENHANCER_TEXTANNOTATION));
        m.add(new TripleImpl(la, ENHANCER_EXTRACTED_FROM, ci.getUri()));
        m.add(new TripleImpl(la, DC_CREATED, lf.createTypedLiteral(new Date())));
        m.add(new TripleImpl(la, DC_CREATOR, lf.createTypedLiteral("Content-Language Header of the request")));
        //add fise:TextAnnotation information as expected by a Language annotation.
        m.add(new TripleImpl(la, DC_TYPE, DCTERMS_LINGUISTIC_SYSTEM));
        m.add(new TripleImpl(la, DC_LANGUAGE, new PlainLiteralImpl(lang)));
        //we set the confidence to 1.0^^xsd:double
        m.add(new TripleImpl(la, ENHANCER_CONFIDENCE, lf.createTypedLiteral(Double.valueOf(1.0f))));
    }
    /**
     * tries to retrieve the ContentItem from the 'uri' query parameter of the
     * {@link #request}.
     * @return the parsed URI or <code>null</code> if none
     */
    private IRI getContentItemId() {
        //NOTE: check for request NULL is needed because of unit tests
        if (uriInfo == null) return null;
        URI uri = uriInfo.getRequestUri();
        String query = uri.getQuery();
        log.debug("parse ContentItem ID from URI: {}",uri);
        //TODO: workaround for uriInfo.getPathParameters() not working as expected
        //MultivaluedMap<String,String> params = uriInfo.getPathParameters();
        //final String ciUri = params.getFirst("uri");
        String ciUri = null; //parse the uri parameter manually from the request URI
        String source = null;
        if(query != null){
            int index = query.indexOf("uri=");
            if(index >= 0){
               index = index+4; //add "uri=".length
               int endIndex = query.indexOf('&', index);
               if(endIndex <= index){
                   endIndex = query.length();
               }
               if(index < endIndex){
                   try {
                       ciUri = URLDecoder.decode(query.substring(index, endIndex), "UTF-8");
                       source = "'uri' parameter";
                   } catch (UnsupportedEncodingException e) {
                       throw new IllegalStateException(e); //should never happen
                   }
               }
            }
        }
        if(ciUri == null){ //try to get the URI from the Content-Location header
            ciUri = headers.getRequestHeaders().getFirst(HttpHeaders.CONTENT_LOCATION);
            source = "'Content-Location' header";
        }
        if(ciUri != null){
            try { //validate the parsed URI
                new URI(ciUri);
            } catch (URISyntaxException e) {
               throw new WebApplicationException("The parsed ContentItem URI '"
                       + ciUri +"' is not a valid URI. Please check the value "
                       + "of the " + source, Response.Status.BAD_REQUEST);
            }
        }
        return ciUri == null ? null : new IRI(ciUri);
    }
    /**
     * Getter for the <code>Content-Language</code> header
     * @return the language of the content as parsed in the request or 
     * <code>null</code> if the header is not present.
     */
    private String getContentLanguage(){
        if(headers != null){
        	List<String> languages = headers.getRequestHeader(HttpHeaders.CONTENT_LANGUAGE);
        	if(languages != null && !languages.isEmpty()){
        		if(languages.size() > 1){
        			log.warn(" ... only single valued Content-Language headers are "
        					+ "supported (will use first language of {})", languages);
        		}
        		return languages.get(0);
        	} else {
        		return null;
        	}
        } else {
        	return null;
        }
    }
    
    /**
     * Parsed EnhancementProperties from the request parameters. <p>
     * This does NOT override existing values in the parsed map.
     * @param ep the map to add the properties
     */
    private void parseEnhancementPropertiesFromParameters(Map<String,Object> ep){
        if(uriInfo == null) {
            return; //for unit tests
        }
        MultivaluedMap<String,String> parameters = uriInfo.getQueryParameters();
        log.debug("read EnhancementPropertis from Request Parameters:");
        for(Entry<String,List<String>> entry : parameters.entrySet()){
            if(entry.getKey().contains("enhancer.")){
                if(!ep.containsKey(entry.getKey())){
                    log.debug(" + {}",entry.getKey());
                    Object value;
                    if(entry.getValue() == null || entry.getValue().isEmpty()){
                        value = null;
                    } if(entry.getValue().size() == 1){
                        value = entry.getValue().get(0);
                    } else {
                    	//we do want our own read-only copy!
                        value = Collections.unmodifiableList(
                        		new ArrayList<String>(entry.getValue()));
                    }
                    log.debug("      value: {}", value);
                    ep.put(entry.getKey(), value);
                } else if(log.isDebugEnabled()){
                    log.debug(" - ignore key {} because it is already present");
                    log.debug("   current value: {}",ep.get(entry.getKey()));
                    log.debug("   request value: {} (ignored)", entry.getValue());
                }
            } else {
                log.debug(" - {}", entry.getKey());
            }
        }
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
     * encoded Multipart MIME
     */
    private ContentItem createContentItem(IRI id, Graph metadata, FileItemStream content,Set<String> parsedContentParts) throws IOException, FileUploadException {
        MediaType partContentType = MediaType.valueOf(content.getContentType());
        ContentItem contentItem = null;
        ContentItemFactory ciFactory = getContentItemFactory();
        if(MULTIPART.isCompatible(partContentType)){
            log.debug("  - multiple (alternate) ContentParts"); 
            //multiple contentParts are parsed
            FileItemIterator contentPartIterator = fu.getItemIterator(
                new MessageBodyReaderContext(
                    content.openStream(), partContentType));
            while(contentPartIterator.hasNext()){
                FileItemStream fis = contentPartIterator.next();
                if(contentItem == null){
                    log.debug("  - create ContentItem {} for content (type:{})",
                        id,fis.getContentType());
                    contentItem = ciFactory.createContentItem(id,
                        new StreamSource(fis.openStream(),fis.getContentType()), 
                        metadata);
                } else {
                    log.debug("  - create Blob for content (type:{})", fis.getContentType());
                    Blob blob = ciFactory.createBlob(new StreamSource(fis.openStream(), fis.getContentType()));
                    IRI contentPartId = null;
                    if(fis.getFieldName() != null && !fis.getFieldName().isEmpty()){
                        contentPartId = new IRI(fis.getFieldName());
                    } else {
                        //generating a random ID might break metadata 
                        //TODO maybe we should throw an exception instead
                        contentPartId = new IRI("urn:contentpart:"+ randomUUID());
                    }
                    log.debug("    ... add Blob {} to ContentItem {} with content (type:{})",
                        new Object[]{contentPartId, id, fis.getContentType()});
                    contentItem.addPart(contentPartId, blob);
                    parsedContentParts.add(contentPartId.getUnicodeString());
                }
            }
        } else {
            log.debug("  - create ContentItem {} for content (type:{})",
                id,content.getContentType());
            contentItem = ciFactory.createContentItem(id,
                new StreamSource(content.openStream(),content.getContentType()), 
                metadata);
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

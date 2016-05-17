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
package org.apache.stanbol.entityhub.web.reader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.core.serializedform.UnsupportedParsingFormatException;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.entityhub.jersey.utils.JerseyUtils;
import org.apache.stanbol.entityhub.jersey.utils.MessageBodyReaderUtils;
import org.apache.stanbol.entityhub.jersey.utils.MessageBodyReaderUtils.RequestData;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Provides support for reading Representation form Requests. This implementation
 * supports all RDF supports as well as {@link MediaType#APPLICATION_FORM_URLENCODED}
 * - in case the data are sent from an HTML form - and 
 * {@link MediaType#MULTIPART_FORM_DATA} - mime encoded data.
 * In case of an  HTML form the encoding need to be specified by the parameter
 * "encoding" for the entity data the parameters "entity" or "content" can be
 * used.
 * @author Rupert Westenthaler
 *
 */
@Component
@Service(Object.class)
@Property(name="javax.ws.rs", boolValue=true)
@Provider
@Consumes({ //First the data types directly supported for parsing representations
            MediaType.APPLICATION_JSON, SupportedFormat.N3, SupportedFormat.N_TRIPLE,
            SupportedFormat.RDF_XML, SupportedFormat.TURTLE, SupportedFormat.X_TURTLE,
            SupportedFormat.RDF_JSON,
            //finally this also supports sending the data as form and mime multipart
            MediaType.APPLICATION_FORM_URLENCODED, 
            MediaType.MULTIPART_FORM_DATA})
public class RepresentationReader implements MessageBodyReader<Map<String,Representation>> {
    
    private static final Logger log = LoggerFactory.getLogger(RepresentationReader.class);


    public static final Set<String> supportedMediaTypes;
    private static final MediaType DEFAULT_ACCEPTED_MEDIA_TYPE = MediaType.TEXT_PLAIN_TYPE;
    static {
        Set<String> types = new HashSet<String>();
        //ensure everything is lower case
        types.add(MediaType.APPLICATION_JSON.toLowerCase());
        types.add(SupportedFormat.N3.toLowerCase());
        types.add(SupportedFormat.N_TRIPLE.toLowerCase());
        types.add(SupportedFormat.RDF_JSON.toLowerCase());
        types.add(SupportedFormat.RDF_XML.toLowerCase());
        types.add(SupportedFormat.TURTLE.toLowerCase());
        types.add(SupportedFormat.X_TURTLE.toLowerCase());
        supportedMediaTypes = Collections.unmodifiableSet(types);
    }
    
    @Reference
    private Parser parser;
    
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        String mediaTypeWithoutParameter = 
                mediaType.getType().toLowerCase()+'/'+
                mediaType.getSubtype().toLowerCase();
        log.debug("isreadable: [genericType: {}| mediaType {}]",
            genericType,mediaTypeWithoutParameter);
        //second the media type
        boolean mediaTypeOK = (//the MimeTypes of Representations
                supportedMediaTypes.contains(mediaTypeWithoutParameter) ||
                //as well as URL encoded
                MediaType.APPLICATION_FORM_URLENCODED.equals(mediaTypeWithoutParameter) ||
                //and mime multipart
                MediaType.MULTIPART_FORM_DATA.equals(mediaTypeWithoutParameter));
        boolean typeOk = JerseyUtils.testParameterizedType(Map.class, 
            new Class[]{String.class,Representation.class}, genericType);
        log.debug("type is {} for {} against Map<String,Representation>",
            typeOk ? "compatible" : "incompatible" ,genericType);
        return typeOk && mediaTypeOK;
    }

    @Override
    public Map<String,Representation> readFrom(Class<Map<String,Representation>> type,
                                   Type genericType,
                                   Annotation[] annotations,
                                   MediaType mediaType,
                                   MultivaluedMap<String,String> httpHeaders,
                                   InputStream entityStream) throws IOException, WebApplicationException {
        log.info("Read Representations from Request Data");
        long start = System.currentTimeMillis();
        //(1) get the charset and the acceptedMediaType
        String charset = "UTF-8";
        if(mediaType.getParameters().containsKey("charset")){
            charset = mediaType.getParameters().get("charset");
        }
        MediaType acceptedMediaType  = getAcceptedMediaType(httpHeaders);
        log.info("readFrom: mediaType {} | accepted {} | charset {}",
            new Object[]{mediaType,acceptedMediaType,charset});
        // (2) read the Content from the request (this needs to deal with  
        //    MediaType.APPLICATION_FORM_URLENCODED_TYPE and 
        //    MediaType.MULTIPART_FORM_DATA_TYPE requests!
        RequestData content;
        if(mediaType.isCompatible(MediaType.APPLICATION_FORM_URLENCODED_TYPE)) {
            try {
                content = MessageBodyReaderUtils.formForm(entityStream, charset,
                "encoding",Arrays.asList("entity","content"));
            } catch (IllegalArgumentException e) {
                log.info("Bad Request: {}",e);
                throw new WebApplicationException(
                    Response.status(Status.BAD_REQUEST).entity(e.toString()).
                    header(HttpHeaders.ACCEPT, acceptedMediaType).build());
            }
            if(content.getMediaType() == null){
                String message = String.format(
                    "Missing parameter %s used to specify the media type" +
                    "(supported values: %s",
                    "encoding",supportedMediaTypes);
                log.info("Bad Request: {}",message);
                throw new WebApplicationException(
                    Response.status(Status.BAD_REQUEST).entity(message).
                    header(HttpHeaders.ACCEPT, acceptedMediaType).build());
            }
            if(!isSupported(content.getMediaType())){
                String message = String.format(
                    "Unsupported Content-Type specified by parameter " +
                    "encoding=%s (supported: %s)",
                    content.getMediaType().toString(),supportedMediaTypes);
                log.info("Bad Request: {}",message);
                throw new WebApplicationException(
                    Response.status(Status.BAD_REQUEST).
                    entity(message).
                    header(HttpHeaders.ACCEPT, acceptedMediaType).build());
            }
        } else if(mediaType.isCompatible(MediaType.MULTIPART_FORM_DATA_TYPE)){
            log.info("read from MimeMultipart");
            List<RequestData> contents;
            try {
                contents = MessageBodyReaderUtils.fromMultipart(entityStream, mediaType);
            } catch (IllegalArgumentException e) {
                log.info("Bad Request: {}",e.toString());
                throw new WebApplicationException(
                    Response.status(Status.BAD_REQUEST).entity(e.toString()).
                    header(HttpHeaders.ACCEPT, acceptedMediaType).build());
            }
            if(contents.isEmpty()){
                String message = "Request does not contain any Mime BodyParts.";
                log.info("Bad Request: {}",message);
                throw new WebApplicationException(
                    Response.status(Status.BAD_REQUEST).entity(message).
                    header(HttpHeaders.ACCEPT, acceptedMediaType).build());
            } else if(contents.size()>1){ 
                //print warnings about ignored parts
                log.warn("{} Request contains more than one Parts: others than " +
                		"the first will be ignored",
                		MediaType.MULTIPART_FORM_DATA_TYPE);
                for(int i=1;i<contents.size();i++){
                    RequestData ignored = contents.get(i);
                    log.warn("  ignore Content {}: Name {}| MediaType {}",
                        new Object[] {i+1,ignored.getName(),ignored.getMediaType()});
                }
            }
            content = contents.get(0);
            if(content.getMediaType() == null){
                String message = String.format(
                    "MediaType not specified for mime body part for file %s. " +
                    "The media type must be one of the supported values: %s",
                    content.getName(), supportedMediaTypes);
                log.info("Bad Request: {}",message);
                throw new WebApplicationException(
                    Response.status(Status.BAD_REQUEST).entity(message).
                    header(HttpHeaders.ACCEPT, acceptedMediaType).build());
            }
            if(!isSupported(content.getMediaType())){
                String message = String.format(
                    "Unsupported Content-Type %s specified for mime body part " +
                    "for file %s (supported: %s)",
                    content.getMediaType(),content.getName(),supportedMediaTypes);
                log.info("Bad Request: {}",message);
                throw new WebApplicationException(
                    Response.status(Status.BAD_REQUEST).
                    entity(message).
                    header(HttpHeaders.ACCEPT, acceptedMediaType).build());
            }
        } else {
            content = new RequestData(mediaType, null, entityStream);
        }
        long readingCompleted = System.currentTimeMillis();
        log.info("   ... reading request data {}ms",readingCompleted-start);
        Map<String,Representation> parsed = parseFromContent(content,acceptedMediaType);
        long parsingCompleted = System.currentTimeMillis();
        log.info("   ... parsing data {}ms",parsingCompleted-readingCompleted);
        return parsed;
    }
    
    public Map<String,Representation> parseFromContent(RequestData content, MediaType acceptedMediaType){
        // (3) Parse the Representtion(s) form the entity stream
        if(content.getMediaType().isCompatible(MediaType.APPLICATION_JSON_TYPE)){
            //parse from json
            throw new UnsupportedOperationException("Parsing of JSON not yet implemented :(");
       } else if(isSupported(content.getMediaType())){ //from RDF serialisation
            RdfValueFactory valueFactory = RdfValueFactory.getInstance();
            Map<String,Representation> representations = new HashMap<String,Representation>();
            Set<BlankNodeOrIRI> processed = new HashSet<BlankNodeOrIRI>();
            Graph graph = new IndexedGraph();
            try {
                parser.parse(graph,content.getEntityStream(), content.getMediaType().toString());
            } catch (UnsupportedParsingFormatException e) {
                //String acceptedMediaType = httpHeaders.getFirst("Accept");
                //throw an internal server Error, because we check in
                //isReadable(..) for supported types and still we get here a
                //unsupported format -> therefore it looks like an configuration
                //error the server (e.g. a missing Bundle with the required bundle)
                String message = "Unable to create the Parser for the supported format"
                    +content.getMediaType()+" ("+e+")";
                log.error(message,e);
                throw new WebApplicationException(
                    Response.status(Status.INTERNAL_SERVER_ERROR).
                    entity(message).
                    header(HttpHeaders.ACCEPT, acceptedMediaType).build());
            } catch (RuntimeException e){
                //NOTE: Clerezza seams not to provide specific exceptions on
                //      parsing errors. Hence the catch for all RuntimeException
                String message = "Unable to parse the provided RDF data (format: "
                    +content.getMediaType()+", message: "+e.getMessage()+")";
                log.error(message,e);
                throw new WebApplicationException(
                    Response.status(Status.BAD_REQUEST).
                    entity(message).
                    header(HttpHeaders.ACCEPT, acceptedMediaType).build());
            }
            for(Iterator<Triple> st = graph.iterator();st.hasNext();){
                BlankNodeOrIRI resource = st.next().getSubject();
                if(resource instanceof IRI && processed.add(resource)){
                    //build a new representation
                    representations.put(((IRI)resource).getUnicodeString(),
                        valueFactory.createRdfRepresentation((IRI)resource, graph));
                }
            }
            return representations;
        } else { //unsupported media type
            String message = String.format(
                "Parsed Content-Type '%s' is not one of the supported %s",
                content.getMediaType(),supportedMediaTypes);
            log.info("Bad Request: {}",message);
            throw new WebApplicationException(
                Response.status(Status.BAD_REQUEST).
                entity(message).
                header(HttpHeaders.ACCEPT, acceptedMediaType).build());
        }
    }
    /**
     * Internally used to get the accepted media type used when returning
     * {@link WebApplicationException}s.
     * @param httpHeaders
     * @param acceptedMediaType
     * @return
     */
    private static MediaType getAcceptedMediaType(MultivaluedMap<String,String> httpHeaders) {
        MediaType acceptedMediaType;
        String acceptedMediaTypeString = httpHeaders.getFirst("Accept");
        if(acceptedMediaTypeString != null){
            try {
                acceptedMediaType = MediaType.valueOf(acceptedMediaTypeString);
                if(acceptedMediaType.isWildcardType()){
                    acceptedMediaType = DEFAULT_ACCEPTED_MEDIA_TYPE;
                }
            } catch (IllegalArgumentException e) {
                acceptedMediaType = DEFAULT_ACCEPTED_MEDIA_TYPE;
            }
        } else {
            acceptedMediaType = DEFAULT_ACCEPTED_MEDIA_TYPE;
        }
        return acceptedMediaType;
    }
    /**
     * Converts the type and the subtype of the parsed media type to the
     * string representation as stored in {@link #supportedMediaTypes} and than
     * checks if the parsed media type is contained in this list.
     * @param mediaType the MediaType instance to check
     * @return <code>true</code> if the parsed media type is not 
     * <code>null</code> and supported. 
     */
    private boolean isSupported(MediaType mediaType){
        return mediaType == null ? false : supportedMediaTypes.contains(
            mediaType.getType().toLowerCase()+'/'+
            mediaType.getSubtype().toLowerCase());
    }
    
}

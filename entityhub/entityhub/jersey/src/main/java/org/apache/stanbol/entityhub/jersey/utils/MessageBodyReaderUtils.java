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
package org.apache.stanbol.entityhub.jersey.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.ParseException;
import javax.mail.util.ByteArrayDataSource;
import javax.ws.rs.FormParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.MessageBodyReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for implementing {@link MessageBodyReader}.
 * @author Rupert Westenthaler
 *
 */
public final class MessageBodyReaderUtils {
    
    private MessageBodyReaderUtils(){ /*do not instantiate Util classes */ };
    private final static Logger log = LoggerFactory.getLogger(MessageBodyReaderUtils.class);
    /**
     * Returns content parsed as {@link MediaType#APPLICATION_FORM_URLENCODED}.
     * It assumes that the encoding and the content is defined by own parameters.
     * For the content this method allows to parse several parameters. The first
     * existing one is used to get the content. The parameter actually used to
     * retrieve the content will be available via {@link RequestData#getName()}.<p>
     * This Method will load the content several time into memory and should
     * not be used for big contents. However this should be fine in cases
     * data are parsed as {@link MediaType#APPLICATION_FORM_URLENCODED}<p>
     * This Method is necessary because within {@link MessageBodyReader} one
     * can not use the {@link FormParam} annotations because only the 
     * {@link InputStream} is parsed to the 
     * {@link MessageBodyReader#readFrom(Class, Type, java.lang.annotation.Annotation[], MediaType, javax.ws.rs.core.MultivaluedMap, InputStream)}
     * method<p>
     * To test this Method with curl use:
     * <code><pre>
     * curl -v -X POST --data-urlencode "{encodingParam}=application/rdf+xml" 
     *                 --data-urlencode "{contentParam}@{datafile}" 
     *                  {serviceURL}
     * </pre></code>
     * Note that between {contentParam} and the datafile MUST NOT be a '='!
     * @param formData the data of the form as stream
     * @param charset the charset used for the form data
     * @param encodingParam the parameter name used to parse the encoding
     * @param contentParams the list of parameters used for the content. The first
     * existing parameter is used to parse the content. Additional ones are
     * ignored.
     * @return The parsed content (MediaType and InputStream)
     * @throws IOException On any exception while reading from the parsed stream.
     * @throws UnsupportedEncodingException if the parsed charset is not supported
     *    by this plattform
     * @throws IllegalArgumentException In case of a {@link Status#BAD_REQUEST}
     */
    public static RequestData formForm(InputStream formData,String charset, String encodingParam,List<String> contentParams) throws IOException,UnsupportedEncodingException,IllegalArgumentException{
        Map<String,String> params = JerseyUtils.parseForm(formData, charset);
        log.debug("Read from Form:");
        MediaType mediaType;
        if(encodingParam != null){
            String mediaTypeString = params.get(encodingParam);
            log.debug("  > encoding: {}={}",encodingParam,mediaTypeString);
            if(mediaTypeString != null){
                try {
                    mediaType = MediaType.valueOf(mediaTypeString);
                } catch (IllegalArgumentException e) {
                    throw new IllegalStateException(String.format(
                        "Illegal formatted Content-Type %s parsed by parameter %s",
                        encodingParam,mediaTypeString),e);
                }
            } else {
                mediaType = null;
            }
        } else {
            log.debug("  > encoding: no encoding prameter set");
            mediaType = null;
        }
        log.debug("      <- mediaType = {}",mediaType);
        InputStream entityStream = null;
        String contentParam = null;
        Iterator<String> contentParamIterator = contentParams.iterator();
        while(entityStream == null && contentParamIterator.hasNext()){
            contentParam = contentParamIterator.next();
            String content = params.get(contentParam);
            log.debug("  > content: {}={}",contentParam,content);
            if(content != null){
                entityStream = new ByteArrayInputStream(content.getBytes(charset));
            }
        }
        if(entityStream == null){
            throw new IllegalArgumentException(String.format(
                "No content found for any of the following parameters %s",
                contentParams));
        }
        return new RequestData(mediaType,contentParam,entityStream);
    }
    /**
     * Returns content parsed from {@link MediaType#MULTIPART_FORM_DATA}.
     * It iterates over all {@link BodyPart}s and tries to create {@link RequestData}
     * instances. In case the {@link BodyPart#getContentType()} is not present or
     * can not be parsed, the {@link RequestData#getMediaType()} is set to 
     * <code>null</code>. If {@link BodyPart#getInputStream()} is not defined an
     * {@link IllegalArgumentException} is thrown. The {@link BodyPart#getFileName()}
     * is used for {@link RequestData#getName()}. The ordering of the returned
     * Content instances is the same as within the {@link MimeMultipart} instance
     * parsed from the input stream. <p>
     * This Method does NOT load the data into memory, but returns directly the
     * {@link InputStream}s as returned by the {@link BodyPart}s. Therefore
     * it is saved to be used with big attachments.<p>
     * This Method is necessary because within {@link MessageBodyReader} one
     * can not use the usual annotations as used within Resources. so this method
     * allows to access the data directly from the parameters available from the
     * {@link MessageBodyReader#readFrom(Class, Type, java.lang.annotation.Annotation[], MediaType, javax.ws.rs.core.MultivaluedMap, InputStream)}
     * method<p>
     * To test this Method with curl use:
     * <code><pre>
     * curl -v -X POST -F "content=@{dataFile};type={mimeType}" 
     *      {serviceURL}
     * </pre></code>
     * Note that between {contentParam} and the datafile MUST NOT be a '='!
     * @param mimeData the mime encoded data
     * @param mediaType the mediaType (parsed to the {@link ByteArrayDataSource}
     * constructor)
     * @return the contents parsed from the {@link BodyPart}s
     * @throws IOException an any Exception while reading the stream or 
     * {@link MessagingException} exceptions other than {@link ParseException}s
     * @throws IllegalArgumentException If a {@link InputStream} is not available
     * for any {@link BodyPart} or on {@link ParseException}s while reading the
     * MimeData from the stream.
     */
    public static List<RequestData> fromMultipart(InputStream mimeData, MediaType mediaType) throws IOException, IllegalArgumentException{
        ByteArrayDataSource ds = new ByteArrayDataSource(mimeData, mediaType.toString());
        List<RequestData> contents = new ArrayList<RequestData>();
        try {
            MimeMultipart data = new MimeMultipart(ds);
            //For now search the first bodypart that fits and only debug the others
            for(int i = 0;i < data.getCount();i++){
                BodyPart bp = data.getBodyPart(i);
                String fileName = bp.getFileName();
                MediaType mt;
                try {
                    mt = bp.getContentType()!=null?MediaType.valueOf(bp.getContentType()):null;
                }catch (IllegalArgumentException e) {
                    log.warn(String.format(
                        "Unable to parse MediaType form Mime Bodypart %s: " +
                        " fileName %s | Disposition %s | Description %s",
                        i+1,fileName,bp.getDisposition(),bp.getDescription()),e);
                    mt = null;
                }
                InputStream stream = bp.getInputStream();
                if(stream == null){
                    throw new IllegalArgumentException(String.format(
                        "Unable to get InputStream for Mime Bodypart %s: " +
                        "mediaType %s fileName %s | Disposition %s | Description %s",
                        i+1,fileName,bp.getDisposition(),bp.getDescription()));
                } else {
                    contents.add(new RequestData(mt,bp.getFileName(),stream));
                }
            }
        } catch (ParseException e) {
            throw new IllegalStateException(String.format(
                    "Unable to parse data from %s request",
                    MediaType.MULTIPART_FORM_DATA_TYPE),e);
        } catch (MessagingException e) {
            throw new IOException("Exception while reading "+
                MediaType.MULTIPART_FORM_DATA_TYPE+" request",e);
        }
        return contents;
    }
    
    /**
     * Simple class that holds the MediaType, Name and the content as 
     * {@link InputStream}.
     * @author Rupert Westenthaler
     *
     */
    public static class RequestData {
        private final MediaType mediaType;
        private final InputStream entityStream;
        private final String contentName;
        public RequestData(MediaType mediaType,String contentName,InputStream entityStream) {
            if(entityStream == null){
                throw new IllegalArgumentException("The parsed Inputstream MUST NOT be NULL!");
            }
            this.mediaType = mediaType;
            this.entityStream = entityStream;
            this.contentName = contentName;
        }
        /**
         * The media type or <code>null</code> if not known
         * @return the mediaType
         */
        public MediaType getMediaType() {
            return mediaType;
        }
        /**
         * The stream with the data
         * @return the entityStream
         */
        public InputStream getEntityStream() {
            return entityStream;
        }
        /**
         * The name of the content (e.g. the parameter used to parse the content
         * or the filename provided by the {@link BodyPart}) or <code>null</code>
         * if not present. The documentation of methods returning instances of 
         * this class should document what is used as name.
         * @return the contentName
         */
        public String getName() {
            return contentName;
        }
    }
}

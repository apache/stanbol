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
package org.apache.stanbol.enhancer.nlp.json.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

import javax.servlet.ServletContext;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.felix.scr.annotations.Reference;
import org.apache.stanbol.enhancer.nlp.json.AnalyzedTextSerializer;
import org.apache.stanbol.enhancer.nlp.json.valuetype.ValueTypeSerializer;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS {@link MessageBodyWriter} for {@link AnalysedText} that works
 * within Apache Stanbol <code>commons.web.base</code> as well as outside
 * of an OSGI environment.<p>
 * This implementation depends on the {@link AnalyzedTextSerializer} service.
 * This dependency is initialised as follows:<ul>
 * <li> via a {@link Reference} annotation on the member
 * <li> via the {@link ServletContext} by using the {@link AnalyzedTextSerializer}
 * class name as attribute name
 * <li> via a OSGI {@link BundleContext} by obtaining the {@link BundleContext}
 * from the {@link ServletContext} by using the {@link BundleContext} class name
 * as attribute name. This is the way Stanbol currently uses)
 * <li> via the {@link AnalyzedTextSerializer#getDefaultInstance()}. This is
 * the expected way to initialize outside an OSGI environment.
 * </ul>
 * Users can also directly set the {@link #serializer} instance in sub-classes.
 * To access the {@link #serializer} the {@link #getSerializer()} method should 
 * be used.
 * @author Rupert Westenthaler
 *
 */
@Provider
@Produces(value=MediaType.APPLICATION_JSON)
public class AnalyzedTextWriter implements MessageBodyWriter<AnalysedText> {

    Logger log = LoggerFactory.getLogger(AnalyzedTextWriter.class);
    
    @Context
    protected ServletContext servletContext;
    /**
     * The serializer (might be lazy initialised in case injection via
     * {@link Reference} does not work
     */
    @Reference
    protected AnalyzedTextSerializer serializer;
    
    /**
     * Getter for the {@link AnalyzedTextSerializer}. If {@link #serializer} is 
     * not yet initialised (meaning that the {@link Reference} annotation has
     * no effect) this tries to (1) get the service via the {@link #servletContext} 
     * (2) get a {@link BundleContext} via the {@link #servletContext} and than the
     * service from the {@link BundleContext} and (3) obtain the default instance
     * using {@link AnalyzedTextSerializer#getDefaultInstance()}. <p>
     * When running within OSGI (3) could be problematic as some 
     * {@link ValueTypeSerializer} might not get registered through to 
     * classpath issues.
     * @return the {@link AnalyzedTextSerializer} instance
     */
    protected final AnalyzedTextSerializer getSerializer(){
        if(serializer == null){
            synchronized (this) {
                if(serializer != null){ //check again because of concurrency
                    return serializer;
                }
                //(1) try to init directly get the service via the servlet context
                Object s = servletContext.getAttribute(AnalyzedTextSerializer.class.getName());
                if(s != null && s instanceof AnalyzedTextSerializer){
                    serializer = (AnalyzedTextSerializer)s;
                    return serializer;
                }
                //(2) try to init via BundleContext available in the servlet context
                Object bc = servletContext.getAttribute(BundleContext.class.getName());
                if(bc != null && bc instanceof BundleContext){
                    ServiceReference reference = ((BundleContext)bc).getServiceReference(
                        AnalyzedTextSerializer.class.getName());
                    if(reference != null){
                        serializer = (AnalyzedTextSerializer)((BundleContext)bc).getService(reference);
                        return serializer;
                    }
                }
                //(3) get the default instance
                serializer = AnalyzedTextSerializer.getDefaultInstance();
            }
        }
        return serializer;
    }
    
    
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return AnalysedText.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(AnalysedText t, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(AnalysedText at, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String,Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        String charsetName = mediaType.getParameters().get("charset");
        Charset charset = null;
        if(charsetName != null){
            try {
                charset = Charset.forName(charsetName);
            } catch (IllegalCharsetNameException e) {
                log.warn("Unable to use charset defined by the parsed MediaType '"
                        + mediaType+"! Fallback to default (UTF-8).",e);
            } catch (UnsupportedCharsetException e){
                log.warn("Charset defined by the parsed MediaType '"+mediaType
                        +" is not supported! Fallback to default (UTF-8).",e);
            }
        }
        getSerializer().serialize(at, entityStream, charset);
    }

}

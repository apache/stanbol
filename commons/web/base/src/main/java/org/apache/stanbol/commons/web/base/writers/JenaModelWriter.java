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
package org.apache.stanbol.commons.web.base.writers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;


import com.hp.hpl.jena.rdf.model.Model;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

@Component
@Service(Object.class)
@Property(name="javax.ws.rs", boolValue=true)
@Provider
//@Produces({"application/rdf+xml", "application/xml", "text/xml"})
public class JenaModelWriter implements MessageBodyWriter<Model> {

    public static final Set<String> supportedMediaTypes;
    static {
        Set<String> types = new HashSet<String>();
        types.add("application/rdf+xml");
        types.add("application/xml");
        types.add("text/xml");
        supportedMediaTypes = Collections.unmodifiableSet(types);
    }
	
	public static final String ENCODING = "UTF-8";
	
	public long getSize(Model arg0, Class<?> arg1, Type arg2,
			Annotation[] arg3, MediaType arg4) {
		return -1;
	}

	public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2,
			MediaType mediaType) {
	    String mediaTypeString = mediaType.getType()+'/'+mediaType.getSubtype();
		return Model.class.isAssignableFrom(arg0) && supportedMediaTypes.contains(mediaTypeString);
	}

	public void writeTo(Model model, Class<?> arg1, Type arg2,
			Annotation[] arg3, MediaType mediaType,
			MultivaluedMap<String, Object> arg5, OutputStream outputStream)
	throws IOException, WebApplicationException {
		Document doc = null;
		String encoding = mediaType.getParameters().get("charset");
		if(encoding == null){
		    encoding = ENCODING;
		}
		try {
			doc = new JenaModelTransformer().toDocument(model);
			DOMSource domSource = new DOMSource(doc);
			StreamResult streamResult = new StreamResult(outputStream);
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer serializer = tf.newTransformer();
			serializer.setOutputProperty(OutputKeys.ENCODING,encoding);
			serializer.setOutputProperty(OutputKeys.INDENT,"yes");
			serializer.transform(domSource, streamResult);
		} catch(TransformerException te) {
			throw new IOException("TransformerException in writeTo()");
		}

		outputStream.flush();
		
	}

}

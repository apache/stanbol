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
package org.apache.stanbol.cmsadapter.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Vector;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.ObjectFactory;
import org.xml.sax.SAXException;

import com.sun.jersey.spi.resource.Singleton;

@Singleton
@Provider
@Produces(MediaType.APPLICATION_XML)
public class JAXBProvider implements MessageBodyReader, MessageBodyWriter {

	private Marshaller marshaller;

	private Unmarshaller unmarshaller;

	public JAXBProvider() throws JAXBException, SAXException {
		ClassLoader cl = ObjectFactory.class.getClassLoader();
		JAXBContext jc = JAXBContext.newInstance(
				ObjectFactory.class.getPackage().getName(), cl);
		marshaller = jc.createMarshaller();
		unmarshaller = jc.createUnmarshaller();
		String schemaLocations[] = { "model/CMSObjectModel.xsd",
				"model/CR2OntologyMap.xsd" };
		SchemaFactory schemaFactory = SchemaFactory
				.newInstance(
					    "http://www.w3.org/2001/XMLSchema",
						"com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory",
						JAXBProvider.class.getClassLoader());

		List<StreamSource> streamSourceList = new Vector<StreamSource>();
		for (String schemaLocation : schemaLocations) {
			InputStream is = cl.getResourceAsStream(schemaLocation);
			StreamSource streamSource = new StreamSource(is);
			streamSourceList.add(streamSource);
		}
		StreamSource sources[] = new StreamSource[streamSourceList.size()];
		Schema schema = schemaFactory.newSchema(streamSourceList
				.toArray(sources));
		marshaller.setSchema(schema);
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		unmarshaller.setSchema(schema);
	}

	@Override
	public long getSize(Object t, Class type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	@Override
	public boolean isWriteable(Class type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		String className = type.getName();
		if (className.startsWith(ObjectFactory.class.getPackage().getName())
				|| className
						.startsWith(org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectFactory.class
								.getPackage().getName())) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void writeTo(Object t, Class type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap httpHeaders, OutputStream entityStream)
			throws IOException, WebApplicationException {
		try {
			marshaller.marshal(t, entityStream);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean isReadable(Class type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		String className = type.getName();
		if (className.startsWith(ObjectFactory.class.getPackage().getName())
				|| className
						.startsWith(org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectFactory.class
								.getPackage().getName())) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public Object readFrom(Class type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap httpHeaders, InputStream entityStream)
			throws IOException, WebApplicationException {
		try {
			return unmarshaller.unmarshal(entityStream);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}

}

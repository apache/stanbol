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
package org.apache.stanbol.ontologymanager.store.rest;

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

import org.apache.stanbol.ontologymanager.store.model.AdministeredOntologies;
import org.apache.stanbol.ontologymanager.store.model.BuiltInResource;
import org.apache.stanbol.ontologymanager.store.model.ClassConstraint;
import org.apache.stanbol.ontologymanager.store.model.ClassContext;
import org.apache.stanbol.ontologymanager.store.model.ClassMetaInformation;
import org.apache.stanbol.ontologymanager.store.model.ClassesForOntology;
import org.apache.stanbol.ontologymanager.store.model.ConstraintType;
import org.apache.stanbol.ontologymanager.store.model.ContainerClasses;
import org.apache.stanbol.ontologymanager.store.model.DatatypePropertiesForOntology;
import org.apache.stanbol.ontologymanager.store.model.DatatypePropertyContext;
import org.apache.stanbol.ontologymanager.store.model.DisjointClasses;
import org.apache.stanbol.ontologymanager.store.model.Domain;
import org.apache.stanbol.ontologymanager.store.model.EquivalentClasses;
import org.apache.stanbol.ontologymanager.store.model.EquivalentProperties;
import org.apache.stanbol.ontologymanager.store.model.ImportsForOntology;
import org.apache.stanbol.ontologymanager.store.model.IndividualContext;
import org.apache.stanbol.ontologymanager.store.model.IndividualMetaInformation;
import org.apache.stanbol.ontologymanager.store.model.IndividualsForOntology;
import org.apache.stanbol.ontologymanager.store.model.ObjectFactory;
import org.apache.stanbol.ontologymanager.store.model.ObjectPropertiesForOntology;
import org.apache.stanbol.ontologymanager.store.model.ObjectPropertyContext;
import org.apache.stanbol.ontologymanager.store.model.OntologyImport;
import org.apache.stanbol.ontologymanager.store.model.OntologyMetaInformation;
import org.apache.stanbol.ontologymanager.store.model.PropertyAssertions;
import org.apache.stanbol.ontologymanager.store.model.PropertyMetaInformation;
import org.apache.stanbol.ontologymanager.store.model.Range;
import org.apache.stanbol.ontologymanager.store.model.ResourceMetaInformationType;
import org.apache.stanbol.ontologymanager.store.model.SuperProperties;
import org.apache.stanbol.ontologymanager.store.model.Superclasses;
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
        JAXBContext jc = JAXBContext.newInstance("org.apache.stanbol.ontologymanager.store.model", cl);
        marshaller = jc.createMarshaller();
        unmarshaller = jc.createUnmarshaller();
        String schemaLocations[] = {"model/xlinks.xsd", "model/PersistenceStoreRESTfulInterface.xsd",
                                    "model/SearchRESTfulInterface.xsd"};
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI,
            "com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory",
            JAXBProvider.class.getClassLoader());

        List<StreamSource> streamSourceList = new Vector<StreamSource>();
        for (String schemaLocation : schemaLocations) {
            InputStream is = cl.getResourceAsStream(schemaLocation);
            StreamSource streamSource = new StreamSource(is);
            streamSourceList.add(streamSource);
        }
        StreamSource sources[] = new StreamSource[streamSourceList.size()];
        Schema schema = schemaFactory.newSchema(streamSourceList.toArray(sources));
        marshaller.setSchema(schema);
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        unmarshaller.setSchema(schema);
    }

    @Override
    public long getSize(Object t, Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (type.equals(AdministeredOntologies.class) || type.equals(BuiltInResource.class)
            || type.equals(ClassConstraint.class) || type.equals(ClassContext.class)
            || type.equals(ClassesForOntology.class) || type.equals(ClassMetaInformation.class)
            || type.equals(ConstraintType.class) || type.equals(ContainerClasses.class)
            || type.equals(DatatypePropertiesForOntology.class) || type.equals(DatatypePropertyContext.class)
            || type.equals(DisjointClasses.class) || type.equals(Domain.class)
            || type.equals(EquivalentClasses.class) || type.equals(EquivalentProperties.class)
            || type.equals(IndividualContext.class) || type.equals(IndividualMetaInformation.class)
            || type.equals(IndividualsForOntology.class) || type.equals(ObjectPropertiesForOntology.class)
            || type.equals(ObjectPropertyContext.class) || type.equals(OntologyMetaInformation.class)
            || type.equals(PropertyAssertions.class) || type.equals(PropertyMetaInformation.class)
            || type.equals(Range.class) || type.equals(ResourceMetaInformationType.class)
            || type.equals(Superclasses.class) || type.equals(SuperProperties.class)
            || type.equals(OntologyImport.class) || type.equals(ImportsForOntology.class)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void writeTo(Object t,
                        Class type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType,
                        MultivaluedMap httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException {
        try {
            marshaller.marshal(t, entityStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isReadable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (type.equals(AdministeredOntologies.class) || type.equals(BuiltInResource.class)
            || type.equals(ClassConstraint.class) || type.equals(ClassContext.class)
            || type.equals(ClassesForOntology.class) || type.equals(ClassMetaInformation.class)
            || type.equals(ConstraintType.class) || type.equals(ContainerClasses.class)
            || type.equals(DatatypePropertiesForOntology.class) || type.equals(DatatypePropertyContext.class)
            || type.equals(DisjointClasses.class) || type.equals(Domain.class)
            || type.equals(EquivalentClasses.class) || type.equals(EquivalentProperties.class)
            || type.equals(IndividualContext.class) || type.equals(IndividualMetaInformation.class)
            || type.equals(IndividualsForOntology.class) || type.equals(ObjectPropertiesForOntology.class)
            || type.equals(ObjectPropertyContext.class) || type.equals(OntologyMetaInformation.class)
            || type.equals(PropertyAssertions.class) || type.equals(PropertyMetaInformation.class)
            || type.equals(Range.class) || type.equals(ResourceMetaInformationType.class)
            || type.equals(Superclasses.class) || type.equals(SuperProperties.class)
            || type.equals(OntologyImport.class) || type.equals(ImportsForOntology.class)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Object readFrom(Class type,
                           Type genericType,
                           Annotation[] annotations,
                           MediaType mediaType,
                           MultivaluedMap httpHeaders,
                           InputStream entityStream) throws IOException, WebApplicationException {
        try {
            return unmarshaller.unmarshal(entityStream);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

}

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
package org.apache.stanbol.cmsadapter.rest.client;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.Dictionary;
import java.util.List;
import java.util.Vector;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.BridgeDefinitions;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.ObjectFactory;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObjects;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo;
import org.osgi.service.component.ComponentContext;
import org.xml.sax.SAXException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

@Component(immediate = true, metatype = true)
@Service
public class CMSAdapterRESTClientImpl implements CMSAdapterRESTClient {
    private static final String CMS_ADAPTER_REST_PROP = "org.apache.stanbol.cmsadapter.web";
    private static final String NOTIFY = "notify";
    private static final String BRIDGE_DEFINITONS = "bridgeDefinitions";
    private static final String DELIMITER = "/";

    @Property(name = CMS_ADAPTER_REST_PROP, value = "http://localhost:8080/cmsadapter/")
    private String cmsAdapterEndpoint;

    private Client client = null;
    private Marshaller marshaller = null;

    public CMSAdapterRESTClientImpl() throws JAXBException, SAXException {
        client = Client.create();
        ClassLoader cl = ObjectFactory.class.getClassLoader();
        JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName(), cl);
        marshaller = jc.createMarshaller();
        String schemaLocations[] = {"model/CMSObjectModel.xsd", "model/CR2OntologyMap.xsd"};
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI,
            "com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory",
            CMSAdapterRESTClientImpl.class.getClassLoader());

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
    }

    @Activate
    public void activate(ComponentContext context) {
        Dictionary properties = context.getProperties();
        this.cmsAdapterEndpoint = ((String) properties.get(CMS_ADAPTER_REST_PROP));
        if (!cmsAdapterEndpoint.endsWith("/")) {
            cmsAdapterEndpoint += "/";
        }
    }

    @Override
    public void addBridgeDefinitions(BridgeDefinitions bridgeDefinitions, ConnectionInfo connectionInfo) {
        WebResource webResource = client.resource(cmsAdapterEndpoint + BRIDGE_DEFINITONS);
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("bridgeDefinitions", serialize(bridgeDefinitions));
        formData.add("connectionInfo", serialize(connectionInfo));
        webResource.type(MediaType.APPLICATION_FORM_URLENCODED).post(String.class, formData);
    }

    @Override
    public void updateBridgeDefinitions(String ontologyURI, BridgeDefinitions bridgeDefinitions) {
        WebResource webResource = client.resource(cmsAdapterEndpoint + BRIDGE_DEFINITONS);
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("bridgeDefinitions", serialize(bridgeDefinitions));
        formData.add("ontologyURI", ontologyURI);
        webResource.type(MediaType.APPLICATION_FORM_URLENCODED).put(String.class, formData);
    }

    @Override
    public void notifyContentChange(String ontologyURI, CMSObjects cmsObjects) {
        WebResource webResource = client.resource(cmsAdapterEndpoint + ontologyURI + DELIMITER + NOTIFY);
        MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
        formData.add("changedObjects", serialize(cmsObjects));
        webResource.type(MediaType.APPLICATION_FORM_URLENCODED).post(formData);

    }

    // FIXME Check whether Jersey provides automatic serialization
    private String serialize(Object object) {
        StringWriter writer = new StringWriter();
        try {
            marshaller.marshal(object, writer);
            return writer.toString();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }

    }
}

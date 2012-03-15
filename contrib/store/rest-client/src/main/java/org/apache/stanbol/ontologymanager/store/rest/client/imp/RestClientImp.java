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
package org.apache.stanbol.ontologymanager.store.rest.client.imp;

import java.io.StringReader;
import java.util.Dictionary;
import java.util.List;
import java.util.Locale;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.ontologymanager.store.model.AdministeredOntologies;
import org.apache.stanbol.ontologymanager.store.model.ClassContext;
import org.apache.stanbol.ontologymanager.store.model.ClassMetaInformation;
import org.apache.stanbol.ontologymanager.store.model.ClassesForOntology;
import org.apache.stanbol.ontologymanager.store.model.DatatypePropertiesForOntology;
import org.apache.stanbol.ontologymanager.store.model.DatatypePropertyContext;
import org.apache.stanbol.ontologymanager.store.model.IndividualContext;
import org.apache.stanbol.ontologymanager.store.model.IndividualMetaInformation;
import org.apache.stanbol.ontologymanager.store.model.IndividualsForOntology;
import org.apache.stanbol.ontologymanager.store.model.ObjectFactory;
import org.apache.stanbol.ontologymanager.store.model.ObjectPropertiesForOntology;
import org.apache.stanbol.ontologymanager.store.model.ObjectPropertyContext;
import org.apache.stanbol.ontologymanager.store.model.OntologyMetaInformation;
import org.apache.stanbol.ontologymanager.store.model.PropertyMetaInformation;
import org.apache.stanbol.ontologymanager.store.model.ResourceMetaInformationType;
import org.apache.stanbol.ontologymanager.store.rest.client.RestClient;
import org.apache.stanbol.ontologymanager.store.rest.client.RestClientException;
import org.osgi.service.component.ComponentContext;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

@Component(immediate = true, metatype=true)
@Service
public class RestClientImp implements RestClient {
    private static final String ONTOLOGY = "/ontology/";

    private static final String CLASSES = "/classes/";

    private static final String INDIVIDUALS = "/individuals/";

    private static final String DATATYPE_PROPERTIES = "/datatypeProperties/";

    private static final String OBJECT_PROPERTIES = "/objectProperties/";

    private static final String SUPER_CLASSES = "/superClasses/";

    private static final String EQUIVALENT_CLASSES = "/equivalentClasses/";

    private static final String DISJOINT_CLASSES = "/disjointClasses/";

    private static final String UNION_CLASSES = "/unionClasses/";

    private static final String DOMAINS = "/domains/";

    private static final String RANGES = "/ranges/";

    private static final String SUPER_PROPERTIES = "/superProperties/";

    private static final String TYPES = "/types/";

    private static final String PROPERTY_ASSERTIONS = "/propertyAssertions/";

    private static final String LITERALS = "/literals/";

    private static final String OBJECTS = "/objects/";

    private static final String JAXB_CONTEXT = "org.apache.stanbol.ontologymanager.store.model";

    private static final String APPLICATION_RDF_XML = "application/rdf+xml";

    private static final String WITH_INFERRED_AXIOMS = "withInferredAxioms";

    private static final String DELIMITER = "/";
    
    public static final String PROPERTY_STORE_URI= "org.apache.stanbol.ontologymanager.store.uri";
    
    
    /**
     * Base URL for Persistence Store RESTful interface.
     */
    @Property(name=PROPERTY_STORE_URI, value="http://localhost:8080")
    private String storeURI;

    private Client client;

    public RestClientImp() {
        this.client = Client.create();
    }

    private static final String normalizeURI(String uri) {
        return uri.replaceAll("#", "/");
    }

    private static Object unmarshall(String content) throws RestClientException {
        Locale oldLocale = Locale.getDefault();
        Locale.setDefault(new Locale("en"));
        try {
            ClassLoader cl = ObjectFactory.class.getClassLoader();
            JAXBContext jc = JAXBContext.newInstance(JAXB_CONTEXT, cl);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            Object obj = unmarshaller.unmarshal(new StringReader(content));
            Locale.setDefault(oldLocale);
            return obj;
        } catch (JAXBException ex) {
            throw new RestClientException("Exception in marshalling", ex);
        } finally {
            Locale.setDefault(oldLocale);
        }
    }
    
    @Activate
    public void activate(ComponentContext context){
    	Dictionary properties = context.getProperties();
    	this.storeURI = (String) properties.get(PROPERTY_STORE_URI);
    }

    @Override
    public void setPsURL(String PsURL) {
        storeURI = PsURL;
    }

    public RestClientImp getRestClient() {
        return this;
    }

    @Override
    public AdministeredOntologies retrieveAdministeredOntologies() throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + ONTOLOGY);
            String content = webResource.accept(MediaType.APPLICATION_XML).get(String.class);
            AdministeredOntologies onts = (AdministeredOntologies) unmarshall(content);
            return onts;
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    @Override
    public ClassesForOntology retrieveClassesOfOntology(String ontologyURI) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + ontologyURI + CLASSES);
            String content = webResource.accept(MediaType.APPLICATION_XML).get(String.class);
            ClassesForOntology classes = (ClassesForOntology) unmarshall(content);
            return classes;
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    @Override
    public DatatypePropertiesForOntology retrieveDatatypePropertiesOfOntology(String ontologyURI) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + ontologyURI
                                                      + DATATYPE_PROPERTIES);
            String content = webResource.accept(MediaType.APPLICATION_XML).get(String.class);
            DatatypePropertiesForOntology datatypeProps = (DatatypePropertiesForOntology) unmarshall(content);
            return datatypeProps;
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    @Override
    public IndividualsForOntology retrieveIndividualsOfOntology(String ontologyURI) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + ontologyURI + INDIVIDUALS);
            String content = webResource.accept(MediaType.APPLICATION_XML).get(String.class);
            IndividualsForOntology onts = (IndividualsForOntology) unmarshall(content);
            return onts;
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    @Override
    public ObjectPropertiesForOntology retrieveObjectPropertiesOfOntology(String ontologyURI) throws RestClientException {
        try {
            WebResource webResource = client
                    .resource(storeURI + DELIMITER + ontologyURI + OBJECT_PROPERTIES);
            String content = webResource.accept(MediaType.APPLICATION_XML).get(String.class);
            ObjectPropertiesForOntology objectProps = (ObjectPropertiesForOntology) unmarshall(content);
            return objectProps;
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    @Override
    public String retrieveOntology(String ontologyURI, String language, boolean withInferredAxioms) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + ontologyURI);
            String content = webResource.accept(APPLICATION_RDF_XML).get(String.class);
            return content;
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    @Override
    public OntologyMetaInformation retrieveOntologyMetaInformation(String ontologyURI) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + ontologyURI);
            String content = webResource.accept(MediaType.APPLICATION_XML).get(String.class);
            OntologyMetaInformation ont = (OntologyMetaInformation) unmarshall(content);
            return ont;
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    public ResourceMetaInformationType retrieveResourceWithURI(String resourceURI) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + resourceURI);
            String content = webResource.accept(MediaType.APPLICATION_XML).get(String.class);
            ResourceMetaInformationType resourceInfo = (ResourceMetaInformationType) unmarshall(content);
            return resourceInfo;
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    @Override
    public OntologyMetaInformation saveOntology(String ontologyContent, String ontologyURI, String encoding) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + ONTOLOGY);
            MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
            formData.add("ontologyURI", ontologyURI);
            formData.add("ontologyContent", ontologyContent);
            String content = webResource.type(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_XML_TYPE).post(String.class, formData);
            OntologyMetaInformation resourceInfo = (OntologyMetaInformation) unmarshall(content);
            return resourceInfo;
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    @Override
    public void addContainerClassForIndividual(String individualURI, String classURI) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + individualURI + TYPES);
            MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
            formData.add("containerClassURIs", classURI);
            webResource.type(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.APPLICATION_XML_TYPE)
                    .post(String.class, formData);
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    @Override
    public void addDisjointClass(String classURI, String disjointClassURI) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + classURI + DISJOINT_CLASSES);
            MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
            formData.add("disjointClassURIs", disjointClassURI);
            webResource.type(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.APPLICATION_XML_TYPE)
                    .post(String.class, formData);
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    @Override
    public void addDomain(String propertyURI, String domainURI) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + propertyURI + DOMAINS);
            MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
            formData.add("domainURIs", domainURI);
            webResource.type(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.APPLICATION_XML_TYPE)
                    .post(String.class, formData);
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    @Override
    public void addEquivalentClass(String classURI, String equivalentClassURI) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + classURI + EQUIVALENT_CLASSES);
            MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
            formData.add("equivalentClassURIs", equivalentClassURI);
            webResource.type(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.APPLICATION_XML_TYPE)
                    .post(String.class, formData);
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    @Override
    public void addRange(String propertyURI, String rangeURI) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + propertyURI + RANGES);
            MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
            formData.add("rangeURIs", rangeURI);
            webResource.type(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.APPLICATION_XML_TYPE)
                    .post(String.class, formData);
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }

    }

    @Override
    public void addUnionClass(String classURI, String unionClassURI) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + classURI + UNION_CLASSES);
            MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
            formData.add("unionClassURIs", unionClassURI);
            webResource.type(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.APPLICATION_XML_TYPE)
                    .post(String.class, formData);
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }

    }

    @Override
    public void assertPropertyValue(String individualURI,
                                    String propertyURI,
                                    String individualAsValueURI,
                                    String literalAsValue) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + individualURI
                                                      + PROPERTY_ASSERTIONS + normalizeURI(propertyURI));
            MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
            formData.add("literalValues", literalAsValue);
            formData.add("objectValues", individualAsValueURI);
            webResource.type(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.APPLICATION_XML_TYPE)
                    .post(String.class, formData);
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    @Override
    public void clearPersistenceStore() throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + ONTOLOGY);
            webResource.delete();
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    @Override
    public void deleteContainerClassForIndividual(String individualURI, String classURI) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + individualURI + TYPES
                                                      + normalizeURI(classURI));
            webResource.delete();
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    @Override
    public void deleteDisjointClass(String classURI, String disjointClassURI) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + classURI + DISJOINT_CLASSES
                                                      + normalizeURI(disjointClassURI));
            webResource.delete();
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }

    }

    @Override
    public void deleteDomain(String propertyURI, String domainURI) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + propertyURI + DOMAINS
                                                      + normalizeURI(domainURI));
            webResource.delete();
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    @Override
    public void deleteEquivalentClass(String classURI, String equivalentClassURI) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + classURI + EQUIVALENT_CLASSES
                                                      + normalizeURI(equivalentClassURI));
            webResource.delete();
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    @Override
    public void deleteOntology(String ontologyURI) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + ontologyURI);
            webResource.delete();
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    @Override
    public void deletePropertyAssertion(String individualURI,
                                        String propertyURI,
                                        String individualAsValueURI,
                                        String literalAsValue) throws RestClientException {
        try {
            WebResource webResource;
            if (literalAsValue != null) {
                MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
                formData.add("value", literalAsValue);
                webResource = client.resource(storeURI + DELIMITER + individualURI + PROPERTY_ASSERTIONS
                                              + normalizeURI(propertyURI) + LITERALS);
                webResource.type(MediaType.APPLICATION_FORM_URLENCODED)
                        .header("X-HTTP-Method-Override", "DELETE").post(String.class, formData);
            } else if (individualAsValueURI != null) {
                webResource = client.resource(storeURI + DELIMITER + individualURI + PROPERTY_ASSERTIONS
                                              + normalizeURI(propertyURI) + OBJECTS
                                              + normalizeURI(individualAsValueURI));
                webResource.delete();
            } else {
                throw new IllegalArgumentException(
                        "Either individualAsValueURI or literalAsValue must not be null");
            }

        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }

    }

    @Override
    public void deleteRange(String propertyURI, String rangeURI) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + propertyURI + RANGES
                                                      + normalizeURI(rangeURI));
            webResource.delete();
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    @Override
    public void deleteResource(String resourceURI) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + resourceURI);
            webResource.delete();
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }

    }

    @Override
    public void deleteSuperClass(String subClassURI, String superClassURI) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + subClassURI + SUPER_CLASSES
                                                      + normalizeURI(superClassURI));
            webResource.delete();
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    @Override
    public void deleteSuperPropertyAssertion(String subPropertyURI, String superPropertyURI) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + subPropertyURI
                                                      + SUPER_PROPERTIES + normalizeURI(superPropertyURI));
            webResource.delete();
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }

    }

    @Override
    public void deleteUnionClass(String classURI, String unionClassURI) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + classURI + UNION_CLASSES
                                                      + normalizeURI(unionClassURI));
            webResource.delete();
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    @Override
    public ClassContext generateClassContext(String classURI, boolean withInferredAxioms) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + classURI);
            String content = webResource
                    .queryParam(WITH_INFERRED_AXIOMS, Boolean.toString(withInferredAxioms))
                    .accept(MediaType.APPLICATION_XML).get(String.class);
            ClassContext classContext = (ClassContext) unmarshall(content);
            return classContext;
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    @Override
    public ClassMetaInformation generateClassForOntology(String ontologyURI, String classURI) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + ontologyURI + CLASSES);
            MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
            formData.add("classURI", classURI);
            String content = webResource.type(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_XML_TYPE).post(String.class, formData);
            ClassMetaInformation classMetaInformation = (ClassMetaInformation) unmarshall(content);
            return classMetaInformation;
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    @Override
    public DatatypePropertyContext generateDatatypePropertyContext(String datatypePropertyURI,
                                                                   boolean withInferredAxioms) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + datatypePropertyURI);
            String content = webResource
                    .queryParam(WITH_INFERRED_AXIOMS, Boolean.toString(withInferredAxioms))
                    .accept(MediaType.APPLICATION_XML).get(String.class);
            DatatypePropertyContext datatypePropertyContext = (DatatypePropertyContext) unmarshall(content);
            return datatypePropertyContext;
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    @Override
    public PropertyMetaInformation generateDatatypePropertyForOntology(String ontologyURI,
                                                                       String datatypePropertyURI) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + ontologyURI
                                                      + DATATYPE_PROPERTIES);
            MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
            formData.add("datatypePropertyURI", datatypePropertyURI);
            String content = webResource.type(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_XML_TYPE).post(String.class, formData);
            PropertyMetaInformation propertyMetaInformation = (PropertyMetaInformation) unmarshall(content);
            return propertyMetaInformation;
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    @Override
    public IndividualContext generateIndividualContext(String individualURI, boolean withInferredAxioms) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + individualURI);
            String content = webResource
                    .queryParam(WITH_INFERRED_AXIOMS, Boolean.toString(withInferredAxioms))
                    .accept(MediaType.APPLICATION_XML).get(String.class);
            IndividualContext individualContext = (IndividualContext) unmarshall(content);
            return individualContext;
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    @Override
    public IndividualMetaInformation generateIndividualForOntology(String ontologyURI,
                                                                   String classURI,
                                                                   String individualURI) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + ontologyURI + INDIVIDUALS);
            MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
            formData.add("classURI", classURI);
            formData.add("individualURI", individualURI);
            String content = webResource.type(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_XML_TYPE).post(String.class, formData);
            IndividualMetaInformation individualMetaInformation = (IndividualMetaInformation) unmarshall(content);
            return individualMetaInformation;
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    @Override
    public ObjectPropertyContext generateObjectPropertyContext(String objectPropertyURI,
                                                               boolean withInferredAxioms) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + objectPropertyURI);
            String content = webResource
                    .queryParam(WITH_INFERRED_AXIOMS, Boolean.toString(withInferredAxioms))
                    .accept(MediaType.APPLICATION_XML).get(String.class);
            ObjectPropertyContext objectPropertyContext = (ObjectPropertyContext) unmarshall(content);
            return objectPropertyContext;
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    @Override
    public PropertyMetaInformation generateObjectPropertyForOntology(String ontologyURI,
                                                                     String objectPropertyURI) throws RestClientException {
        try {
            WebResource webResource = client
                    .resource(storeURI + DELIMITER + ontologyURI + OBJECT_PROPERTIES);
            MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
            formData.add("objectPropertyURI", objectPropertyURI);
            String content = webResource.type(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_XML_TYPE).post(String.class, formData);
            PropertyMetaInformation propertyMetaInformation = (PropertyMetaInformation) unmarshall(content);
            return propertyMetaInformation;
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    @Override
    public void makeSubClassOf(String subClassURI, String superClassURI) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + subClassURI + SUPER_CLASSES);
            MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
            formData.add("superClassURIs", superClassURI);
            webResource.type(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.APPLICATION_XML_TYPE)
                    .post(String.class, formData);
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception " + e.getMessage(), e);
        }
    }

    @Override
    public void makeSubPropertyOf(String subPropertyURI, String superPropertyURI) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + subPropertyURI
                                                      + SUPER_PROPERTIES);
            MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
            formData.add("superPropertyURIs", superPropertyURI);
            webResource.type(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.APPLICATION_XML_TYPE)
                    .post(String.class, formData);
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }

    }

    @Override
    public void addDomains(String propertyURI, List<String> domainURIs) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + propertyURI + DOMAINS);
            MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
            for (String domainURI : domainURIs) {
                formData.add("domainURIs", domainURI);
            }
            webResource.type(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.APPLICATION_XML_TYPE)
                    .post(String.class, formData);
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    @Override
    public void setPropertyAttributes(String propertyURI,
                                      Boolean isFunctional,
                                      Boolean isTransitive,
                                      Boolean isSymmetric,
                                      Boolean isInverseFunctional) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + propertyURI);
            MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
            if (isFunctional != null) {
                formData.add("isFunctional", Boolean.toString(isFunctional));
            }
            if (isTransitive != null) {
                formData.add("isTransitive", Boolean.toString(isTransitive));
            }
            if (isSymmetric != null) {
                formData.add("isSymmetric", Boolean.toString(isSymmetric));
            }
            if (isInverseFunctional != null) {
                formData.add("isInverseFunctional", Boolean.toString(isInverseFunctional));
            }

            webResource.type(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.APPLICATION_XML_TYPE)
                    .post(String.class, formData);
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }

    }

    @Override
    public void addRanges(String propertyURI, List<String> rangeURIs) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + propertyURI + RANGES);
            MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
            for (String rangeURI : rangeURIs) {
                formData.add("rangeURIs", rangeURI);
            }
            webResource.type(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.APPLICATION_XML_TYPE)
                    .post(String.class, formData);
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }

    @Override
    public String mergeOntology(String ontologyPath, String targetOntology, String targetOntologyBaseURI) throws RestClientException {
        try {
            WebResource webResource = client.resource(storeURI + DELIMITER + ontologyPath);
            MultivaluedMap<String,String> formData = new MultivaluedMapImpl();
            formData.add("targetOntology", targetOntology);
            formData.add("targetOntologyBaseURI", targetOntologyBaseURI);
            String content = webResource.accept(APPLICATION_RDF_XML).post(String.class, formData);
            return content;
        } catch (UniformInterfaceException e) {
            throw new RestClientException("REST service exception", e);
        }
    }
}

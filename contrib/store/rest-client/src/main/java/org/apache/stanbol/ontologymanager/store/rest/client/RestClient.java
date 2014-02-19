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
package org.apache.stanbol.ontologymanager.store.rest.client;

import java.util.List;

import org.apache.stanbol.ontologymanager.store.model.AdministeredOntologies;
import org.apache.stanbol.ontologymanager.store.model.ClassContext;
import org.apache.stanbol.ontologymanager.store.model.ClassMetaInformation;
import org.apache.stanbol.ontologymanager.store.model.ClassesForOntology;
import org.apache.stanbol.ontologymanager.store.model.DatatypePropertiesForOntology;
import org.apache.stanbol.ontologymanager.store.model.DatatypePropertyContext;
import org.apache.stanbol.ontologymanager.store.model.IndividualContext;
import org.apache.stanbol.ontologymanager.store.model.IndividualMetaInformation;
import org.apache.stanbol.ontologymanager.store.model.IndividualsForOntology;
import org.apache.stanbol.ontologymanager.store.model.ObjectPropertiesForOntology;
import org.apache.stanbol.ontologymanager.store.model.ObjectPropertyContext;
import org.apache.stanbol.ontologymanager.store.model.OntologyMetaInformation;
import org.apache.stanbol.ontologymanager.store.model.PropertyMetaInformation;

public interface RestClient {

    /**
     * @param PsURL
     *            URL of the external Persistence Store.
     */
    void setPsURL(String PsURL);

    /**
     * Interface method for registering an ontology to the persistence store
     * 
     * @param ontologyContent
     *            the ontology itself, encoded as text
     * @param ontologyURI
     *            the base URI to be used for the ontology
     * @param encoding
     *            the encoding of ontologyContent (e.g. UTF-8), important: not to be mixed up with the output
     *            style of an ontology (i.e. RDF/XML, OWL, etc.)
     * @return OntologyMetaInformation: the XSD element contains the "URI" and the "description" of the
     *         ontology
     * @throws Exception
     */
    OntologyMetaInformation saveOntology(String ontologyContent, String ontologyURI, String encoding) throws RestClientException;

    /**
     * Interface method to retrieve a particular ontology
     * 
     * @param ontologyPath
     *            the path of the ontology to be retrieved
     * @param language
     *            the language (i.e RDF/XML, OWL, etc.) see particular persistence store implementation (i.e.
     *            JenaPersistenceStore) for the set of supported languages
     * @param withInferredAxioms
     *            connects to the reasoner if true
     * @return the ontology encoded in the given language
     * @throws RestClientException
     */
    String retrieveOntology(String ontologyPath, String language, boolean withInferredAxioms) throws RestClientException;

    /**
     * Interface method to return a list of all registered ontologies
     * 
     * @return AdministeredOntologies: List of all registered ontologies (i.e. list of
     *         OntologyMetaInformation)
     * @throws RestClientException
     */
    AdministeredOntologies retrieveAdministeredOntologies() throws RestClientException;

    /**
     * Interface method to return the OntologyMetaInformation element associated with a particular ontology
     * 
     * @param ontologyPath
     *            the path of the ontology
     * @return OntologyMetaInformation: the XSD element contains the "URI" and the "description" of the
     *         ontology
     * @throws RestClientException
     */
    OntologyMetaInformation retrieveOntologyMetaInformation(String ontologyPath) throws RestClientException;

    /**
     * Interface method to list all of the classes that the ontology contains
     * 
     * @param ontologyPath
     *            the Path of the ontology
     * @return ClassesForOntology: contains the OntologyMetaInformation of the ontology as well as a list of
     *         ClassMetaInformation elements. Each ClassMetaInformation element contains the "URI",
     *         "description", "namespace", "localname" and "href". "href" is the *URL* that the Persistence
     *         Layer Service assigns to each registered class (e.g. ontologies
     *         /784360a5-2194-4f4a-8fd6-14f4dbd34262/classes/3f5fa9ff-4 cdc-42c8-8629-f2d7ecdbf16b)
     * @throws RestClientException
     */
    ClassesForOntology retrieveClassesOfOntology(String ontologyPath) throws RestClientException;

    /**
     * Interface method to list all of the data type properties that the ontology contains
     * 
     * @param ontologyPath
     *            the Path of the ontology
     * @return DatatypePropertiesForOntology: contains the OntologyMetaInformation of the ontology as well as
     *         a list of PropertyMetaInformation elements. Each PropertyMetaInformation element contains the
     *         "URI", "description", "namespace", "localname" and "href". "href" is the *URL* that the
     *         Persistence Layer Service assigns to each registered data type property. (e.g.
     *         ontologies/45514659-c5e8-423e-80a9-e86256eb7b99/
     *         datatypeProperties/36c453b5-f619-4828-82cb-2414c9749e87)
     * @throws RestClientException
     */
    DatatypePropertiesForOntology retrieveDatatypePropertiesOfOntology(String ontologyPath) throws RestClientException;

    /**
     * Interface method to list all of the object properties that the ontology contains
     * 
     * @param ontologyPath
     *            the Path of the ontology
     * @return ObjectPropertiesForOntology: contains the OntologyMetaInformation of the ontology as well as a
     *         list of PropertyMetaInformation elements. Each PropertyMetaInformation element contains the
     *         "URI", "description", "namespace", "localname" and "href". "href" is the *URL* that the
     *         Persistence Layer Service assigns to each registered object property. (e.g.
     *         ontologies/45514659-c5e8-423e-80 a9-e86256eb7b99/objectProperties/a2bf8f9a
     *         -dbb3-4d4d-a7d9-187733ba238c)
     * @throws RestClientException
     */
    ObjectPropertiesForOntology retrieveObjectPropertiesOfOntology(String ontologyPath) throws RestClientException;

    /**
     * Interface method to list all of the individuals that the ontology contains
     * 
     * @param ontologyPath
     *            the path of the ontology
     * @return IndividualsForOntology: contains the OntologyMetaInformation of the ontology as well as a list
     *         of PropertyMetaInformation elements. Each PropertyMetaInformation element contains the "URI",
     *         "description", "namespace", "localname" and "href". "href" is the *URL* that the Persistence
     *         Layer Service assigns to each registered individual. (e.g.
     *         ontologies/0d541ddc-7afd-4901-a2ff-41d
     *         cad687efb/individuals/1aefd64c-8700-4f24-b705-9ced6caa6951)
     * @throws RestClientException
     */
    IndividualsForOntology retrieveIndividualsOfOntology(String ontologyPath) throws RestClientException;

    /**
     * Interface method to get a Protege-like view of the particular ontology class
     * 
     * @param classPath
     *            the path of the class
     * @param withInferredAxioms
     *            connects to the reasoner if true
     * @return ClassContext: Each ClassContext contains a ClassMetaInformation, the equivalent, super and
     *         disjoint classes of the class in question and an unbounded array of the constraints on the
     *         class.
     * @throws RestClientException
     */
    ClassContext generateClassContext(String classPath, boolean withInferredAxioms) throws RestClientException;

    /**
     * Interface method to get a Protege-like view of the particular data type property
     * 
     * @param datatypePropertyPath
     *            the Path of the data type property
     * @param withInferredAxioms
     *            connects to the reasoner if true
     * @return DatatypePropertyContext: Each DatatypePropertyContext contains a PropertyMetaInformation, the
     *         domain and range of the data type property as well as the equivalent and super properties.
     *         Finally, some attributes of the data type property (i.e. isFunctional) are also conveyed.
     * @throws RestClientException
     */
    DatatypePropertyContext generateDatatypePropertyContext(String datatypePropertyPath,
            boolean withInferredAxioms) throws RestClientException;

    /**
     * Interface method to get a Protege-like view of the particular object property
     * 
     * @param objectPropertyPath
     *            the Path of the object property
     * @param withInferredAxioms
     *            connects to the reasoner if true
     * @return ObjectPropertyContext: Each ObjectPropertyContext contains a PropertyMetaInformation, the
     *         domain and range of the object property as well as the equivalent and super properties.
     *         Finally, some attributes of the object property (i.e. isFunctional, isInverseFunctional,
     *         isTransitive, isSymmetric) are also conveyed.
     * @throws RestClientException
     */
    ObjectPropertyContext generateObjectPropertyContext(String objectPropertyPath,
            boolean withInferredAxioms) throws RestClientException;

    /**
     * Interface method to get a Protege-like view of the particular individual
     * 
     * @param individualPath
     *            the Path of the individual
     * @param withInferredAxioms
     *            connects to the reasoner if true
     * @return IndividualContext: Each IndividualContext contains an IndividualMetaInformation, the list of
     *         classes to which this individual belongs (ContainerClasses) and finally, the names and values
     *         of any properties associated with this individual.
     * @throws RestClientException
     */
    IndividualContext generateIndividualContext(String individualPath, boolean withInferredAxioms) throws RestClientException;

    /**
     * Interface method to create a new class for a particular ontology
     * 
     * @param ontologyPath
     *            the Path of the ontology in which the new class will be generated
     * @param classURI
     *            the URI to assign to the new class
     * @return ClassMetaInformation: contains the "URI", "description", "namespace", "localname" and "href".
     *         "href" is the *URL* that the Persistence Layer Service assigns to the newly registered class
     *         (e.g. ontologies/784360a5-2194-4f4a-8fd6-14f4dbd34262/classes/3f5f
     *         a9ff-4cdc-42c8-8629-f2d7ecdbf16b).
     * @throws RestClientException
     */
    ClassMetaInformation generateClassForOntology(String ontologyPath, String classURI) throws RestClientException;

    /**
     * Interface method to create a new data type property for a particular ontology
     * 
     * @param ontologyPath
     *            the Path of the ontology in which the new data type property will be generated
     * @param datatypePropertyURI
     *            the URI to assign to the new data type property
     * @return PropertyMetaInformation: contains the "URI", "description", "namespace", "localname" and
     *         "href". "href" is the *URL* that the Persistence Layer Service assigns to the newly registered
     *         data type property (e.g. ontologies/45514659-c5e8-423e-80a9-e86256eb7b99
     *         /datatypeProperties/36c453b5-f619-4828-82cb-2414c9749e87).
     * @throws RestClientException
     */
    PropertyMetaInformation generateDatatypePropertyForOntology(String ontologyPath,
            String datatypePropertyURI) throws RestClientException;

    /**
     * Interface method to create a new object property for a particular ontology
     * 
     * @param ontologyPath
     *            the Path of the ontology in which the new object property will be generated
     * @param objectPropertyURI
     *            the URI to assign to the new object property
     * @return PropertyMetaInformation: contains the "URI", "description", "namespace", "localname" and
     *         "href". "href" is the *URL* that the Persistence Layer Service assigns to the newly registered
     *         object property (e.g. ontologies/45514659-c5e8-423e-80a9-e86256eb7b99/ objectProperties
     *         /a2bf8f9a-dbb3-4d4d-a7d9-187733ba238c)
     * @throws RestClientException
     */
    PropertyMetaInformation generateObjectPropertyForOntology(String ontologyPath,
            String objectPropertyURI) throws RestClientException;

    /**
     * Interface method to create a new individual for a particular ontology
     * 
     * @param ontologyPath
     *            the Path of the ontology in which the new individual will be generated
     * @param classURI
     *            the URI of the class to which the new individual belongs
     * @param individualURI
     *            the URI to assign to the new individual
     * @return IndividualMetaInformation: contains the "URI", "description", "namespace", "localname" and
     *         "href". "href" is the *URL* that the Persistence Layer Service assigns to the newly registered
     *         individual (e.g. ontologies/0d541ddc-7afd-4901-a2ff-41dcad687efb/individuals
     *         /1aefd64c-8700-4f24-b705-9ced6caa6951)
     * @throws RestClientException
     */
    IndividualMetaInformation generateIndividualForOntology(String ontologyPath,
            String classURI,
            String individualURI) throws RestClientException;

    /**
     * Interface method to *add* a new super-sub class association
     * 
     * @param subClassPath
     *            the Path of the subclass
     * @param superClassURI
     *            the URI of the superclass
     * @return true if successful (e.g. all required resources exit)
     * @throws RestClientException
     */
    void makeSubClassOf(String subClassPath, String superClassURI) throws RestClientException;

    /**
     * Interface method to *delete* a superclass association
     * 
     * @param subClassPath
     *            the Path of the subclass
     * @param superClassURI
     *            the URI of the superclass
     * @return true if successful (e.g. all required resources exit)
     * @throws RestClientException
     */
    void deleteSuperClass(String subClassPath, String superClassURI) throws RestClientException;

    /**
     * Interface method to *add* a new equivalent class association
     * 
     * @param classPath
     *            the Path of the class in context
     * @param equivalentClassURI
     *            the URI of the class which will be equivalent to the class in context
     * @return true if successful (e.g. all required resources exit)
     * @throws RestClientException
     */
    void addEquivalentClass(String classPath, String equivalentClassURI) throws RestClientException;

    /**
     * Interface method to *delete* a new equivalent class association
     * 
     * @param classPath
     *            the Path of the class in context
     * @param equivalentClassURI
     *            the URI of the class which will be deleted from equivalent classes
     * @return true if successful (e.g. all required resources exit)
     * @throws RestClientException
     */
    void deleteEquivalentClass(String classPath, String equivalentClassURI) throws RestClientException;

    /**
     * Interface method to *add* a new disjoint class association
     * 
     * @param classPath
     *            the Path of the class in context
     * @param equivalentClassURI
     *            the URI of the class which will be disjoint to the class in context
     * @return true if successful (e.g. all required resources exit)
     * @throws RestClientException
     */
    void addDisjointClass(String classPath, String disjointClassURI) throws RestClientException;

    /**
     * Interface method to *delete* an existing disjoint class association
     * 
     * @param classPath
     *            the Path of the class in context
     * @param disjointClassURI
     *            the URI of the class which will be deleted from disjoint classes
     * @return true if successful (e.g. all required resources exit)
     * @throws RestClientException
     */
    void deleteDisjointClass(String classPath, String disjointClassURI) throws RestClientException;

    /**
     * Interface method to *add* a particular class to union set of another class
     * 
     * @param classPath
     *            the Path of the class in context
     * @param unionClassURIs
     *            the URI of the classes to be added to union set
     * @return true if successful (e.g. all required resources exit)
     * @throws RestClientException
     */
    void addUnionClass(String classPath, String unionClassURI) throws RestClientException;

    /**
     * Interface method to *delete* a particular class from the union set of another class
     * 
     * @param classPath
     *            the Path of the class in context
     * @param unionClassURIs
     *            the URI of the class to be deleted from union set
     * @return true if successful (e.g. all required resources exit)
     * @throws RestClientException
     */
    void deleteUnionClass(String classPath, String unionClassURI) throws RestClientException;

    /**
     * Interface method to *add* a new super-sub property association
     * 
     * @param subPropertyPath
     *            the Path of the subproperty
     * @param superPropertyURI
     *            the URI of the superproperty
     * @return true if successful (e.g. all required resources exit)
     * @throws RestClientException
     */
    void makeSubPropertyOf(String subPropertyPath, String superPropertyURI) throws RestClientException;

    /**
     * Interface method to *delete* a super property association
     * 
     * @param subPropertyPath
     *            the Path of the subproperty
     * @param superPropertyURI
     *            the URI of the superproperty
     * @return true if successful (e.g. all required resources exit)
     * @throws RestClientException
     */
    void deleteSuperPropertyAssertion(String subPropertyPath, String superPropertyURI) throws RestClientException;

    /**
     * Interface method to *add* multiple domains to domain set of a property
     * 
     * @param propertyPath
     *            the Path of the property in context
     * @param domainURIs
     *            the URI list of the domain classes
     * @return true if successful (e.g. all required resources exit)
     * @throws RestClientException
     */
    void addDomains(String propertyPath, List<String> domainURIs) throws RestClientException;

    /**
     * Interface method to *add* a domain to the domain set of an object and data type property
     * 
     * @param propertyPath
     *            the Path of the property in context
     * @return true if successful (e.g. all required resources exit)
     * @throws RestClientException
     */
    void addDomain(String propertyPath, String domainURI) throws RestClientException;

    /**
     * Interface method to *delete* a domain from the domain set of an object or data type property
     * 
     * @param propertyPath
     *            the Path of the property in context
     * @param domainURI
     *            the URI of the domain class
     * @return true if successful (e.g. all required resources exit)
     * @throws RestClientException
     */
    void deleteDomain(String propertyPath, String domainURI) throws RestClientException;

    /**
     * Interface method to *add* multiple ranges to range set of a property
     * 
     * @param propertyPath
     *            the Path of the property in context
     * @param rangeURIs
     *            the URI list of the range classes or one of OWL-built in types for data type properties
     * @return true if successful (e.g. all required resources exit)
     * @throws RestClientException
     */
    void addRanges(String propertyPath, List<String> rangeURIs) throws RestClientException;

    /**
     * Interface method to *add* a range to range set of an object or data type property
     * 
     * @param propertyPath
     *            the Path of the property in context
     * @param rangeURI
     *            the URI of the range class or one of OWL-built in types for data type properties
     * @return true if successful (e.g. all required resources exit)
     * @throws RestClientException
     */
    void addRange(String propertyPath, String rangeURI) throws RestClientException;

    /**
     * Interface method to *delete* a range from range set of an object or data type property
     * 
     * @param propertyPath
     *            the Path of the property in context
     * @param rangeURI
     *            the URI of the range class or one of OWL-built in types for data type properties
     * @return true if successful (e.g. all required resources exit)
     * @throws RestClientException
     */
    void deleteRange(String propertyPath, String rangeURI) throws RestClientException;

    /**
     * Interface method to *set* the various property attributes (e.g. isFunctional, isTransitive, etc.)
     * Please note that some apply to object properties only while some apply to both
     * 
     * @param propertyPath
     *            the Path of the property in context
     * @param isFunctional
     *            true/false/null
     * @param isTransitive
     *            true/false/null
     * @param isSymmetric
     *            true/false/null
     * @param isInverseFunctional
     *            true/false/null
     * @return true if successful (e.g. all required resources exit)
     * @throws RestClientException
     */
    void setPropertyAttributes(String propertyPath,
            Boolean isFunctional,
            Boolean isTransitive,
            Boolean isSymmetric,
            Boolean isInverseFunctional) throws RestClientException;

    /**
     * Interface method to *set* the property value of a particular individual
     * 
     * @param individualPath
     *            the Path of the individual in context
     * @param propertyURI
     *            the URI of the property whose value will be set
     * @param individualAsValueURI
     *            a choice between {individualAsValueURI} and {literalAsValue} has to be made: if it is an
     *            object property {individualAsValueURI} cannot be null
     * @param literalAsValue
     * @return true if successful (e.g. all required resources exit)
     * @throws RestClientException
     */
    void assertPropertyValue(String individualPath,
            String propertyURI,
            String individualAsValueURI,
            String literalAsValue) throws RestClientException;

    /**
     * Interface method to *delete* the property assertion of a particular individual
     * 
     * @param individualPath
     *            the Path of the individual in context
     * @param propertyURI
     *            the URI of the property whose assertion will be deleted
     * @param individualAsValueURI
     *            a choice between {individualAsValueURI} and {literalAsValue} has to be made: if it is an
     *            object property {individualAsValueURI} cannot be null
     * @param literalAsValue
     *            if it is an datatype property {literalAsValue} cannot be null
     * @return true if successful (e.g. all required resources exit)
     * @throws RestClientException
     */
    void deletePropertyAssertion(String individualPath,
            String propertyURI,
            String individualAsValueURI,
            String literalAsValue) throws RestClientException;

    /**
     * Interface method to *add* a class for an individual
     * 
     * @param individualPath
     *            the Path of the individual in context
     * @param classURI
     *            the URI of the class in context
     * @return true if successful (e.g. all required resources exit)
     * @throws RestClientException
     */
    void addContainerClassForIndividual(String individualPath, String classURI) throws RestClientException;

    /**
     * Interface method to *delete* a class for an individual
     * 
     * @param individualPath
     *            the Path of the individual in context
     * @param classURI
     *            the URI of the class in context
     * @return true if successful
     * @throws RestClientException
     */
    void deleteContainerClassForIndividual(String individualPath, String classURI) throws RestClientException;

    /**
     * Delete any resource on persistence store
     * 
     * @param resourcePath
     *            REST path of the resource
     * @throws RestClientException
     */
    void deleteResource(String resourcePath) throws RestClientException;

    /**
     * Interface method to *delete* a particular ontology
     * 
     * @param ontologyPath
     *            the Path of the ontology to delete
     * @return true if successful (e.g. all required resources exit)
     */
    void deleteOntology(String ontologyPath) throws RestClientException;

    /**
     * Interface method to *delete* all registered ontologies together with their resources
     * 
     * @return true if successful
     * @throws RestClientException
     */
    void clearPersistenceStore() throws RestClientException;

    /**
     * Interface method to *merge* resources
     * 
     * @param ontologyPath
     *            the path of the ontology onto which target ontology will be merged
     * @param targetOntology
     *            ontology content to be merged
     * @param targetOntologyBaseURI
     *            base URI of the target ontology
     * @throws RestClientException
     */
    String mergeOntology(String ontologyPath, String targetOntology, String targetOntologyBaseURI) throws RestClientException;
}
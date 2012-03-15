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
package org.apache.stanbol.ontologymanager.store.api;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.apache.stanbol.ontologymanager.store.model.AdministeredOntologies;
import org.apache.stanbol.ontologymanager.store.model.ClassContext;
import org.apache.stanbol.ontologymanager.store.model.ClassMetaInformation;
import org.apache.stanbol.ontologymanager.store.model.ClassesForOntology;
import org.apache.stanbol.ontologymanager.store.model.DatatypePropertiesForOntology;
import org.apache.stanbol.ontologymanager.store.model.DatatypePropertyContext;
import org.apache.stanbol.ontologymanager.store.model.ImportsForOntology;
import org.apache.stanbol.ontologymanager.store.model.IndividualContext;
import org.apache.stanbol.ontologymanager.store.model.IndividualMetaInformation;
import org.apache.stanbol.ontologymanager.store.model.IndividualsForOntology;
import org.apache.stanbol.ontologymanager.store.model.ObjectPropertiesForOntology;
import org.apache.stanbol.ontologymanager.store.model.ObjectPropertyContext;
import org.apache.stanbol.ontologymanager.store.model.OntologyImport;
import org.apache.stanbol.ontologymanager.store.model.OntologyMetaInformation;
import org.apache.stanbol.ontologymanager.store.model.PropertyMetaInformation;
import org.apache.stanbol.ontologymanager.store.model.ResourceMetaInformationType;

/**
 * A Java Interface to be used by any persistence store implementation i.e. Jena
 * OWL Persistence Store, Jena RDF Persistence Store, Sesame Persistence Store,
 * or any other arbitrary persistence store.
 * 
 * @author gunes
 */
public interface PersistenceStore {

	/**
	 * Interface method for registering an ontology to the persistence store
	 * 
	 * @param ontologyContent
	 *            the ontology itself, encoded as text
	 * @param ontologyURI
	 *            the base URI to be used for the ontology
	 * @param encoding
	 *            the encoding of ontologyContent (e.g. UTF-8), important: not
	 *            to be mixed up with the output style of an ontology (i.e.
	 *            RDF/XML, OWL, etc.)
	 * @return OntologyMetaInformation: the XSD element contains the "URI" and
	 *         the "description" of the ontology
	 * @throws Exception
	 */
    OntologyMetaInformation saveOntology(String ontologyContent,
            String ontologyURI, String encoding) throws Exception;
    
    /**
     * Interface method for registering an ontology to the persistence store
     * 
     * @param ontologyContent
     *            the input stream of the ontology serialization
     * @param ontologyURI
     *            the base URI to be used for the ontology
     * @param encoding
     *            the encoding of ontologyContent (e.g. UTF-8), important: not
     *            to be mixed up with the output style of an ontology (i.e.
     *            RDF/XML, OWL, etc.)
     * @return OntologyMetaInformation: the XSD element contains the "URI" and
     *         the "description" of the ontology
     * @throws Exception
     */
    OntologyMetaInformation saveOntology(InputStream ontologyContent,
            String ontologyURI, String encoding) throws Exception;
    
    /**
     * Interface method for registering an ontology to the persistence store
     * 
     * @param ontologyContent
     *            the URL of the ontology
     * @param ontologyURI
     *            the base URI to be used for the ontology
     * @param encoding
     *            the encoding of ontologyContent (e.g. UTF-8), important: not
     *            to be mixed up with the output style of an ontology (i.e.
     *            RDF/XML, OWL, etc.)
     * @return OntologyMetaInformation: the XSD element contains the "URI" and
     *         the "description" of the ontology
     * @throws Exception
     */
    OntologyMetaInformation saveOntology(URL ontologyContent,
            String ontologyURI, String encoding) throws Exception;

	/**
	 * Interface method to retrieve a particular ontology
	 * 
	 * @param ontologyURI
	 *            the base URI of the ontology to retrieve
	 * @param language
	 *            the language (i.e RDF/XML, OWL, etc.) see particular
	 *            persistence store implementation (i.e. JenaPersistenceStore)
	 *            for the set of supported languages
	 * @param withInferredAxioms
	 *            connects to the reasoner if true
	 * @return the ontology encoded in the given language
	 * @throws Exception
	 */
    String retrieveOntology(String ontologyURI, String language,
            boolean withInferredAxioms) throws Exception;

	/**
	 * 
	 * @param ontologyURI
	 *            the baseURI of the ontology onto which target ontology will be
	 *            merged
	 * @param targetOntology
	 *            external ontology to be merged
	 * @param targetOntologyURI
	 *            the baseURI of the external ontology
	 * @param withInferredAxioms
	 *            connects to the reasoner if true
	 * @return the main ontology merged with the external ontology
	 * @throws Exception
	 */
    String mergeOntology(String ontologyURI, String targetOntology,
            String targetOntologyURI, boolean withInferredAxioms)
			throws Exception;

	/**
	 * Interface method to return a list of all registered ontologies
	 * 
	 * @return AdministeredOntologies: List of all registered ontologies (i.e.
	 *         list of OntologyMetaInformation)
	 * @throws Exception
	 */
    AdministeredOntologies retrieveAdministeredOntologies()
			throws Exception;

	/**
	 * Interface method to return the OntologyMetaInformation element associated
	 * with a particular ontology
	 * 
	 * @param ontologyURI
	 *            the base URI of the ontology
	 * @return OntologyMetaInformation: the XSD element contains the "URI" and
	 *         the "description" of the ontology
	 * @throws Exception
	 */
    OntologyMetaInformation retrieveOntologyMetaInformation(
            String ontologyURI) throws Exception;

	/**
	 * Interface method to list all of the classes that the ontology contains
	 * 
	 * @param ontologyURI
	 *            the base URI of the ontology
	 * @return ClassesForOntology: contains the OntologyMetaInformation of the
	 *         ontology as well as a list of ClassMetaInformation elements. Each
	 *         ClassMetaInformation element contains the "URI", "description",
	 *         "namespace", "localname" and "href". "href" is the *URL* that the
	 *         Persistence Layer Service assigns to each registered class (e.g.
	 *         ontologies
	 *         /784360a5-2194-4f4a-8fd6-14f4dbd34262/classes/3f5fa9ff-4
	 *         cdc-42c8-8629-f2d7ecdbf16b)
	 * @throws Exception
	 */
    ClassesForOntology retrieveClassesOfOntology(String ontologyURI)
			throws Exception;

	/**
	 * Interface method to list all of the data type properties that the
	 * ontology contains
	 * 
	 * @param ontologyURI
	 *            the base URI of the ontology
	 * @return DatatypePropertiesForOntology: contains the
	 *         OntologyMetaInformation of the ontology as well as a list of
	 *         PropertyMetaInformation elements. Each PropertyMetaInformation
	 *         element contains the "URI", "description", "namespace",
	 *         "localname" and "href". "href" is the *URL* that the Persistence
	 *         Layer Service assigns to each registered data type property.
	 *         (e.g. ontologies/45514659-c5e8-423e-80a9-e86256eb7b99/
	 *         datatypeProperties/36c453b5-f619-4828-82cb-2414c9749e87)
	 * @throws Exception
	 */
    DatatypePropertiesForOntology retrieveDatatypePropertiesOfOntology(
            String ontologyURI) throws Exception;

	/**
	 * Interface method to list all of the object properties that the ontology
	 * contains
	 * 
	 * @param ontologyURI
	 *            the base URI of the ontology
	 * @return ObjectPropertiesForOntology: contains the OntologyMetaInformation
	 *         of the ontology as well as a list of PropertyMetaInformation
	 *         elements. Each PropertyMetaInformation element contains the
	 *         "URI", "description", "namespace", "localname" and "href". "href"
	 *         is the *URL* that the Persistence Layer Service assigns to each
	 *         registered object property. (e.g.
	 *         ontologies/45514659-c5e8-423e-80
	 *         a9-e86256eb7b99/objectProperties/a2bf8f9a
	 *         -dbb3-4d4d-a7d9-187733ba238c)
	 * @throws Exception
	 */
    ObjectPropertiesForOntology retrieveObjectPropertiesOfOntology(
            String ontologyURI) throws Exception;

	/**
	 * Interface method to list all of the individuals that the ontology
	 * contains
	 * 
	 * @param ontologyURI
	 *            the base URI of the ontology
	 * @return IndividualsForOntology: contains the OntologyMetaInformation of
	 *         the ontology as well as a list of PropertyMetaInformation
	 *         elements. Each PropertyMetaInformation element contains the
	 *         "URI", "description", "namespace", "localname" and "href". "href"
	 *         is the *URL* that the Persistence Layer Service assigns to each
	 *         registered individual. (e.g.
	 *         ontologies/0d541ddc-7afd-4901-a2ff-41d
	 *         cad687efb/individuals/1aefd64c-8700-4f24-b705-9ced6caa6951)
	 * @throws Exception
	 */
    IndividualsForOntology retrieveIndividualsOfOntology(
            String ontologyURI) throws Exception;

	/**
	 * Interface method to list the MetaInformation about a particular resource.
	 * A resource could be a Class, a Property (Object/Data type property), or
	 * an Individual
	 * 
	 * @param resourceURI
	 *            the URI of the resource to look for
	 * @return null if not found, ResourceMetaInformation type otherwise.
	 *         ResourceMetaInformationType: the base Element for
	 *         ClassMetaInformation, PropertyMetaInformation or
	 *         IndividualMetaInformation
	 * @throws Exception
	 */
    ResourceMetaInformationType retrieveResourceWithURI(
            String resourceURI) throws Exception;

	/**
	 * Interface method to get a Protege-like view of the particular ontology
	 * class
	 * 
	 * @param classURI
	 *            the URI of the class
	 * @param withInferredAxioms
	 *            connects to the reasoner if true
	 * @return ClassContext: Each ClassContext contains a ClassMetaInformation,
	 *         the equivalent, super and disjoint classes of the class in
	 *         question and an unbounded array of the constraints on the class.
	 * @throws Exception
	 */
    ClassContext generateClassContext(String classURI,
            boolean withInferredAxioms) throws Exception;

	/**
	 * Interface method to get a Protege-like view of the particular data type
	 * property
	 * 
	 * @param datatypePropertyURI
	 *            the URI of the data type property
	 * @param withInferredAxioms
	 *            connects to the reasoner if true
	 * @return DatatypePropertyContext: Each DatatypePropertyContext contains a
	 *         PropertyMetaInformation, the domain and range of the data type
	 *         property as well as the equivalent and super properties. Finally,
	 *         some attributes of the data type property (i.e. isFunctional) are
	 *         also conveyed.
	 * @throws Exception
	 */
    DatatypePropertyContext generateDatatypePropertyContext(
            String datatypePropertyURI, boolean withInferredAxioms)
			throws Exception;

	/**
	 * Interface method to get a Protege-like view of the particular object
	 * property
	 * 
	 * @param objectPropertyURI
	 *            the URI of the object property
	 * @param withInferredAxioms
	 *            connects to the reasoner if true
	 * @return ObjectPropertyContext: Each ObjectPropertyContext contains a
	 *         PropertyMetaInformation, the domain and range of the object
	 *         property as well as the equivalent and super properties. Finally,
	 *         some attributes of the object property (i.e. isFunctional,
	 *         isInverseFunctional, isTransitive, isSymmetric) are also
	 *         conveyed.
	 * @throws Exception
	 */
    ObjectPropertyContext generateObjectPropertyContext(
            String objectPropertyURI, boolean withInferredAxioms)
			throws Exception;

	/**
	 * Interface method to get a Protege-like view of the particular individual
	 * 
	 * @param individualURI
	 *            the URI of the individual
	 * @param withInferredAxioms
	 *            connects to the reasoner if true
	 * @return IndividualContext: Each IndividualContext contains an
	 *         IndividualMetaInformation, the list of classes to which this
	 *         individual belongs (ContainerClasses) and finally, the names and
	 *         values of any properties associated with this individual.
	 * @throws Exception
	 */
    IndividualContext generateIndividualContext(String individualURI,
            boolean withInferredAxioms) throws Exception;

	/**
	 * Interface method to create a new class for a particular ontology
	 * 
	 * @param ontologyURI
	 *            the URI of the ontology in which the new class will be
	 *            generated
	 * @param classURI
	 *            the URI to assign to the new class
	 * @return ClassMetaInformation: contains the "URI", "description",
	 *         "namespace", "localname" and "href". "href" is the *URL* that the
	 *         Persistence Layer Service assigns to the newly registered class
	 *         (e.g.
	 *         ontologies/784360a5-2194-4f4a-8fd6-14f4dbd34262/classes/3f5f
	 *         a9ff-4cdc-42c8-8629-f2d7ecdbf16b).
	 * @throws Exception
	 */
    ClassMetaInformation generateClassForOntology(String ontologyURI,
            String classURI) throws Exception;

	/**
	 * Interface method to create a new data type property for a particular
	 * ontology
	 * 
	 * @param ontologyURI
	 *            the URI of the ontology in which the new data type property
	 *            will be generated
	 * @param datatypePropertyURI
	 *            the URI to assign to the new data type property
	 * @return PropertyMetaInformation: contains the "URI", "description",
	 *         "namespace", "localname" and "href". "href" is the *URL* that the
	 *         Persistence Layer Service assigns to the newly registered data
	 *         type property (e.g.
	 *         ontologies/45514659-c5e8-423e-80a9-e86256eb7b99
	 *         /datatypeProperties/36c453b5-f619-4828-82cb-2414c9749e87).
	 * @throws Exception
	 */
    PropertyMetaInformation generateDatatypePropertyForOntology(
            String ontologyURI, String datatypePropertyURI) throws Exception;

	/**
	 * Interface method to create a new object property for a particular
	 * ontology
	 * 
	 * @param ontologyURI
	 *            the URI of the ontology in which the new object property will
	 *            be generated
	 * @param objectPropertyURI
	 *            the URI to assign to the new object property
	 * @return PropertyMetaInformation: contains the "URI", "description",
	 *         "namespace", "localname" and "href". "href" is the *URL* that the
	 *         Persistence Layer Service assigns to the newly registered object
	 *         property (e.g. ontologies/45514659-c5e8-423e-80a9-e86256eb7b99/
	 *         objectProperties /a2bf8f9a-dbb3-4d4d-a7d9-187733ba238c)
	 * @throws Exception
	 */
    PropertyMetaInformation generateObjectPropertyForOntology(
            String ontologyURI, String objectPropertyURI) throws Exception;

	/**
	 * Interface method to create a new individual for a particular ontology
	 * 
	 * @param ontologyURI
	 *            the URI of the ontology in which the new individual will be
	 *            generated
	 * @param classURI
	 *            the URI of the class to which the new individual belongs
	 * @param individualURI
	 *            the URI to assign to the new individual
	 * @return IndividualMetaInformation: contains the "URI", "description",
	 *         "namespace", "localname" and "href". "href" is the *URL* that the
	 *         Persistence Layer Service assigns to the newly registered
	 *         individual (e.g.
	 *         ontologies/0d541ddc-7afd-4901-a2ff-41dcad687efb/individuals
	 *         /1aefd64c-8700-4f24-b705-9ced6caa6951)
	 * @throws Exception
	 */
    IndividualMetaInformation generateIndividualForOntology(
            String ontologyURI, String classURI, String individualURI)
			throws Exception;

	/**
	 * Interface method to *add* a new super-sub class association
	 * 
	 * @param subClassURI
	 *            the URI of the subclass
	 * @param superClassURI
	 *            the URI of the superclass
	 * @return true if successful (e.g. all required resources exit)
	 * @throws Exception
	 */
    boolean makeSubClassOf(String subClassURI, String superClassURI)
			throws Exception;

	/**
	 * Interface method to *delete* a superclass association
	 * 
	 * @param subClassURI
	 *            the URI of the subclass
	 * @param superClassURI
	 *            the URI of the superclass
	 * @return true if successful (e.g. all required resources exit)
	 * @throws Exception
	 */
    boolean deleteSuperClass(String subClassURI, String superClassURI)
			throws Exception;

	/**
	 * Interface method to *add* a new equivalent class association
	 * 
	 * @param classURI
	 *            the URI of the class in context
	 * @param equivalentClassURI
	 *            the URI of the class which will be equivalent to the class in
	 *            context
	 * @return true if successful (e.g. all required resources exit)
	 * @throws Exception
	 */
    boolean addEquivalentClass(String classURI, String equivalentClassURI)
			throws Exception;

	/**
	 * Interface method to *delete* a new equivalent class association
	 * 
	 * @param classURI
	 *            the URI of the class in context
	 * @param equivalentClassURI
	 *            the URI of the class which will be deleted from equivalent
	 *            classes
	 * @return true if successful (e.g. all required resources exit)
	 * @throws Exception
	 */
    boolean deleteEquivalentClass(String classURI,
            String equivalentClassURI) throws Exception;

	/**
	 * Interface method to *add* a new disjoint class association
	 * 
	 * @param classURI
	 *            the URI of the class in context
	 * @param equivalentClassURI
	 *            the URI of the class which will be disjoint to the class in
	 *            context
	 * @return true if successful (e.g. all required resources exit)
	 * @throws Exception
	 */
    boolean addDisjointClass(String classURI, String disjointClassURI)
			throws Exception;

	/**
	 * Interface method to *delete* an existing disjoint class association
	 * 
	 * @param classURI
	 *            the URI of the class in context
	 * @param disjointClassURI
	 *            the URI of the class which will be deleted from disjoint
	 *            classes
	 * @return true if successful (e.g. all required resources exit)
	 * @throws Exception
	 */
    boolean deleteDisjointClass(String classURI, String disjointClassURI)
			throws Exception;

	/**
	 * Interface method to *set* a particular class as the union of a set of
	 * classes
	 * 
	 * @param classURI
	 *            the URI of the class in context
	 * @param unionClassURIs
	 *            list of URIs of the classes to be unioned
	 * @return true if successful (e.g. all required resources exit)
	 * @throws Exception
	 */
    boolean makeUnionClassOf(String classURI, List<String> unionClassURIs)
			throws Exception;

	/**
	 * Interface method to *add* a particular class to union set of another
	 * class
	 * 
	 * @param classURI
	 *            the URI of the class in context
	 * @param unionClassURIs
	 *            the URI of the classes to be added to union set
	 * @return true if successful (e.g. all required resources exit)
	 * @throws Exception
	 */
    boolean addUnionClass(String classURI, String unionClassURI)
			throws Exception;

	/**
	 * Interface method to *delete* a particular class from the union set of
	 * another class
	 * 
	 * @param classURI
	 *            the URI of the class in context
	 * @param unionClassURIs
	 *            the URI of the class to be deleted from union set
	 * @return true if successful (e.g. all required resources exit)
	 * @throws Exception
	 */
    boolean deleteUnionClass(String classURI, String unionClassURI)
			throws Exception;

	/**
	 * Interface method to *add* a new super-sub property association
	 * 
	 * @param subPropertyURI
	 *            the URI of the subproperty
	 * @param superPropertyURI
	 *            the URI of the superproperty
	 * @return true if successful (e.g. all required resources exit)
	 * @throws Exception
	 */
    boolean makeSubPropertyOf(String subPropertyURI,
            String superPropertyURI) throws Exception;

	/**
	 * Interface method to *delete* a super property association
	 * 
	 * @param subPropertyURI
	 *            the URI of the subproperty
	 * @param superPropertyURI
	 *            the URI of the superproperty
	 * @return true if successful (e.g. all required resources exit)
	 * @throws Exception
	 */
    boolean deleteSuperPropertyAssertion(String subPropertyURI,
            String superPropertyURI) throws Exception;

	/**
	 * Interface method to *set* the domain of an object or data type property
	 * 
	 * @param propertyURI
	 *            the URI of the property in context
	 * @param domainURI
	 *            the URI of the domain class
	 * @return true if successful (e.g. all required resources exit)
	 * @throws Exception
	 */
    boolean setDomain(String propertyURI, List<String> domainURI)
			throws Exception;

	/**
	 * Interface method to *add* a domain to the domain set of an object and
	 * data type property
	 * 
	 * @param propertyURI
	 *            the URI of the property in context
	 * @return true if successful (e.g. all required resources exit)
	 * @throws Exception
	 */
    boolean addDomain(String propertyURI, String domainURI)
			throws Exception;

	/**
	 * Interface method to *delete* a domain from the domain set of an object or
	 * data type property
	 * 
	 * @param propertyURI
	 *            the URI of the property in context
	 * @param domainURI
	 *            the URI of the domain class
	 * @return true if successful (e.g. all required resources exit)
	 * @throws Exception
	 */
    boolean deleteDomain(String propertyURI, String domainURI)
			throws Exception;

	/**
	 * Interface method to *set* the range of an object or data type property
	 * 
	 * @param propertyURI
	 *            the URI of the property in context
	 * @param rangeURI
	 *            the URI of the range class or one of OWL-built in types for
	 *            data type properties
	 * @return true if successful (e.g. all required resources exit)
	 * @throws Exception
	 */
    boolean setRange(String propertyURI, List<String> rangeURI)
			throws Exception;

	/**
	 * Interface method to *add* a range to range set of an object or data type
	 * property
	 * 
	 * @param propertyURI
	 *            the URI of the property in context
	 * @param rangeURI
	 *            the URI of the range class or one of OWL-built in types for
	 *            data type properties
	 * @return true if successful (e.g. all required resources exit)
	 * @throws Exception
	 */
    boolean addRange(String propertyURI, String rangeURI)
			throws Exception;

	/**
	 * Interface method to *delete* a range from range set of an object or data
	 * type property
	 * 
	 * @param propertyURI
	 *            the URI of the property in context
	 * @param rangeURI
	 *            the URI of the range class or one of OWL-built in types for
	 *            data type properties
	 * @return true if successful (e.g. all required resources exit)
	 * @throws Exception
	 */
    boolean deleteRange(String propertyURI, String rangeURI)
			throws Exception;

	/**
	 * Interface method to *set* the various property attributes (e.g.
	 * isFunctional, isTransitive, etc.) Please note that some apply to object
	 * properties only while some apply to both
	 * 
	 * @param propertyURI
	 *            the URI of the property in context
	 * @param isFunctional
	 *            true/false/null
	 * @param isTransitive
	 *            true/false/null
	 * @param isSymmetric
	 *            true/false/null
	 * @param isInverseFunctional
	 *            true/false/null
	 * @return true if successful (e.g. all required resources exit)
	 * @throws Exception
	 */
    boolean setPropertyAttributes(String propertyURI,
            Boolean isFunctional, Boolean isTransitive, Boolean isSymmetric,
            Boolean isInverseFunctional) throws Exception;

	/**
	 * Interface method to *set* the property value of a particular individual
	 * 
	 * @param individualURI
	 *            the URI of the individual in context
	 * @param propertyURI
	 *            the URI of the property whose value will be set
	 * @param individualAsValueURI
	 *            a choice between {individualAsValueURI} and {literalAsValue}
	 *            has to be made: if it is an object property
	 *            {individualAsValueURI} cannot be null
	 * @param literalAsValue
	 * @return true if successful (e.g. all required resources exit)
	 * @throws Exception
	 */
    boolean assertPropertyValue(String individualURI,
            String propertyURI, String individualAsValueURI,
            String literalAsValue) throws Exception;

	/**
	 * Interface method to *delete* the property assertion of a particular
	 * individual
	 * 
	 * @param individualURI
	 *            the URI of the individual in context
	 * @param propertyURI
	 *            the URI of the property whose assertion will be deleted
	 * @param individualAsValueURI
	 *            a choice between {individualAsValueURI} and {literalAsValue}
	 *            has to be made: if it is an object property
	 *            {individualAsValueURI} cannot be null
	 * @param literalAsValue
	 *            if it is an datatype property {literalAsValue} cannot be null
	 * @return true if successful (e.g. all required resources exit)
	 * @throws Exception
	 */
    boolean deletePropertyAssertion(String individualURI,
            String propertyURI, String individualAsValueURI,
            String literalAsValue) throws Exception;

	/**
	 * Interface method to *add* a class for an individual
	 * 
	 * @param individualURI
	 *            the URI of the individual in context
	 * @param classURI
	 *            the URI of the class in context
	 * @return true if successful (e.g. all required resources exit)
	 * @throws Exception
	 */
    boolean addContainerClassForIndividual(String individualURI,
            String classURI) throws Exception;

	/**
	 * Interface method to *delete* a class for an individual
	 * 
	 * @param individualURI
	 *            the URI of the individual in context
	 * @param classURI
	 *            the URI of the class in context
	 * @return true if successful
	 * @throws Exception
	 */
    boolean deleteContainerClassForIndividual(String individualURI,
            String classURI) throws Exception;

	/**
	 * Interface method to *delete* a particular ontology
	 * 
	 * @param ontologyURI
	 *            the URI of the ontology to delete
	 * @return true if successful (e.g. all required resources exit)
	 */
    boolean deleteOntology(String ontologyURI) throws Exception;

	/**
	 * Interface method to *delete* a class, property or individual
	 * 
	 * @param resourceURI
	 *            the URI of the resource (class, property or individual)
	 * @return true if successful (e.g. all required resources exit)
	 * @throws Exception
	 */
    boolean deleteResource(String resourceURI) throws Exception;

    /**
     * Interface method to *list* ontology imports of an ontology
     * @param ontologyURI 
     *          the URI of the ontology
     * @return
     *          A {@link ImportsForOntology} objects.
     * @throws Exception
     */
    ImportsForOntology retrieveOntologyImports(String ontologyURI) throws Exception;
    
    /**
     * Interface method to *add* an import to an existing ontology
     * @param ontologyURI
     *          the URI of the ontology.
     * @param importURI
     *          the URI of the ontology to be imported.
     * @throws Exception
     */
    void addOntologyImport(String ontologyURI, String importURI) throws Exception;
    
    /**
     * Interface method to *delete* an import from an existing ontology.
     * If the ontology does not import the specified one, then nothing happens.
     * @param ontologyURI
     *          the URI of the ontology.
     * @param importURI
     *          the URI of the imported ontology
     * @throws Exception
     */
    void removeOntologyImport(String ontologyURI, String importURI) throws Exception;
    
	/**
	 * Interface method to *delete* all registered ontologies together with
	 * their resources
	 * 
	 * @return true if successful
	 * @throws Exception
	 */
    boolean clearPersistenceStore() throws Exception;
}

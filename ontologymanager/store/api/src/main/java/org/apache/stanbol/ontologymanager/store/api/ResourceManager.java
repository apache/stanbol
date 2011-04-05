package org.apache.stanbol.ontologymanager.store.api;

public interface ResourceManager {

    /**
     * For registration of the ontology to the Resource Manager
     * 
     * @param ontologyURI
     *            the URI of the ontology to be registered
     */
    void registerOntology(String ontologyURI);

    /**
     * For registration of the ontology class to the Resource Manager The Resource Manager assigns a unique
     * URL (or path) to the ontology and updates the internal hashtables and database
     * 
     * @param ontologyURI
     *            the URI of the ontology
     * @param classURI
     *            the URI of the class that is being registered
     */
    void registerClass(String ontologyURI, String classURI);

    /**
     * For registration of the data type property to the Resource Manager The Resource Manager assigns a
     * unique URL (or path) to the data type property and updates the internal hashtables and database
     * 
     * @param ontologyURI
     *            the URI of the ontology
     * @param dataPropertyURI
     *            the URI of the data type property that is being registered
     */
    void registerDatatypeProperty(String ontologyURI, String dataPropertyURI);

    /**
     * For registration of the object property to the Resource Manager The Resource Manager assigns a unique
     * URL (or path) to the object property and updates the internal hashtables and database
     * 
     * @param ontologyURI
     *            the URI of the ontology
     * @param objectPropertyURI
     *            the URI of the object property that is being registered
     */
    void registerObjectProperty(String ontologyURI, String objectPropertyURI);

    /**
     * For registration of the individual to the Resource Manager The Resource Manager assigns a unique URL
     * (or path) to the individual and updates the internal hashtables and database
     * 
     * @param ontologyURI
     *            the URI of the ontology
     * @param individualURI
     *            the URI of the individual that is being registered
     */
    void registerIndividual(String ontologyURI, String individualURI);

    boolean hasOntology(String ontologyURI);

    String getOntologyPath(String ontologyURI);

    String getOntologyFullPath(String ontologyURI);

    String getResourceFullPath(String resourceURI);

    String getOntologyURIForPath(String ontologyPath);

    String getResourceURIForPath(String ontologyPath, String resourcePath);

    /**
     * Converts referenceable REST sub-path of a class, property or individual into URI
     * 
     * @param entityPath
     *            Path to be converted
     * @return URI of the specified entity
     */
    String convertEntityRelativePathToURI(String entityPath);

    String getResourceType(String resourceURI);

    void removeOntology(String ontologyURI);

    void removeResource(String resourceURI);

    // FIXME:: make sure that this method returns the reference to the imported
    // class!!!
    String resolveOntologyURIFromResourceURI(String resourceURI);

    /**
     * To be used together with Jena's cleanDB function which deletes all stored triples
     */
    void clearResourceManager();

}
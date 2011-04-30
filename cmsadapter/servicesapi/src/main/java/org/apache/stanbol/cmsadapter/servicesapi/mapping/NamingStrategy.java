package org.apache.stanbol.cmsadapter.servicesapi.mapping;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccess;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.RDFList;

public interface NamingStrategy {

    String getClassName(String ontologyURI, CMSObject cmsObject);

    String getClassName(String ontologyURI, ObjectTypeDefinition objectTypeDefinition);

    String getClassName(String ontologyURI, String reference);

    String getIndividualName(String ontologyURI, CMSObject cmsObject);

    String getIndividualName(String ontologyURI, String reference);

    String getPropertyName(String ontologyURI, String reference);

    String getObjectPropertyName(String ontologyURI, String reference);

    String getObjectPropertyName(String ontologyURI, PropertyDefinition propertyDefinition);

    String getDataPropertyName(String ontologyURI, String reference);

    String getDataPropertyName(String ontologyURI, PropertyDefinition propertyDefinition);

    String getUnionClassURI(String ontologyURI, RDFList list);

    void setRepositoryAccess(RepositoryAccess repositoryAccess);

    void setSession(Object session);

    void setOntModel(OntModel ontModel);

}

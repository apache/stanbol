package org.apache.stanbol.cmsadapter.servicesapi.mapping;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition;

import com.hp.hpl.jena.rdf.model.RDFList;

/**
 * Represents how different type of OWL entities are named in an extraction context.
 * 
 * @author Suat
 * 
 */
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

}

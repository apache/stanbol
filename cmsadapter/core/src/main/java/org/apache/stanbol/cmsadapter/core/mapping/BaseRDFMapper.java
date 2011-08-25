package org.apache.stanbol.cmsadapter.core.mapping;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.stanbol.cmsadapter.servicesapi.helper.CMSAdapterVocabulary;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.RDFMapper;

/**
 * This class contains common methods to be used in {@link RDFMapper} implementations
 * 
 * @author suat
 * 
 */
public class BaseRDFMapper {
    /**
     * Obtains the name for the CMS object based on the RDF data provided. If
     * {@link CMSAdapterVocabulary#CMS_OBJECT_NAME} assertion is already provided, its value is returned;
     * otherwise the local name is extracted from the URI given.
     * 
     * @param subject
     *            {@link NonLiteral} representing the URI of the CMS object resource
     * @param graph
     *            {@link MGraph} holding the resources
     * @return the name for the CMS object to be created/updated in the repository
     */
    protected String getObjectName(NonLiteral subject, MGraph graph) {
        String objectName = RDFBridgeHelper.getResourceStringValue(subject,
            CMSAdapterVocabulary.CMS_OBJECT_NAME, graph);
        if (objectName.contentEquals("")) {
            return RDFBridgeHelper.extractLocalNameFromURI(subject);
        } else {
            return objectName;
        }
    }

    /**
     * Obtains the path for the CMS object based on the name of the object and RDF provided. If
     * {@link CMSAdapterVocabulary#CMS_OBJECT_PATH} assertion is already provided, its value is returned;
     * otherwise its name is returned together with a preceding "/" character. This means CMS object will be
     * searched under the root path
     * 
     * @param subject
     *            {@link NonLiteral} representing the URI of the CMS object resource
     * @param name
     *            name of the CMS object to be created in the repository
     * @param graph
     *            {@link MGraph} holding the resource
     * @return the path for the CMS object to be created/updated in the repository
     */
    protected String getObjectPath(NonLiteral subject, String name, MGraph graph) {
        String objectPath = RDFBridgeHelper.getResourceStringValue(subject,
            CMSAdapterVocabulary.CMS_OBJECT_PATH, graph);
        if (objectPath.contentEquals("")) {
            return "/" + name;
        } else {
            return objectPath;
        }
    }
}

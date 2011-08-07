package org.apache.stanbol.cmsadapter.servicesapi.mapping;

import org.apache.clerezza.rdf.core.MGraph;

/**
 * Goal of this interface is to provide a uniform mechanism to store RDF data to JCR or CMIS repositories
 * based on cms vocabulary annotations on top of the raw RDF.
 * 
 * @author suat
 * 
 */
public interface RDFMapper {

    /**
     * This method stores the data passed within an {@link MGraph} to repository according
     * "CMS vocabulary annotations".
     * 
     * @param session
     *            This is a session object which is used to interact with JCR or CMIS repositories
     * @param annotatedGraph
     *            This {@link MGraph} object is an enhanced version of raw RDF data with "CMS vocabulary"
     *            annotations according to {@link RDFBridge}s.
     * @throws RDFBridgeException
     */
    void storeRDFinRepository(Object session, MGraph annotatedGraph) throws RDFBridgeException;

}

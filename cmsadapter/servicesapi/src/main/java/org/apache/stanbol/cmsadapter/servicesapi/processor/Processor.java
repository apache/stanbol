package org.apache.stanbol.cmsadapter.servicesapi.processor;

import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngine;

/**
 * An extractor which is responsible for creation and deletion of triples.
 * With the same list of CMS objects and mapping environment a processor is expected to
 * be able to delete all the triples it generated after successive calls of 
 * {@link #createObjects(List, MappingEngine)} and {@linkplain #deleteObjects(List, MappingEngine)} 
 * @author cihan
 *
 */
public interface Processor {
    /**
     * Method for determining if the processor can process the specified CMS object.
     * @param cmsObject
     * @param session a JCR or CMIS Session object 
     * @return true if the CMS object can be processed.
     */
    Boolean canProcess(Object cmsObject, Object session);
    
    /**
     * Creates extracted triples from the provided CMS objects. 
     * The ontology should be available through <b>engine</b> parameter. 
     * @param objects a list of CMS objects to process
     * @param engine 
     */
    void createObjects(List<Object> objects, MappingEngine engine);
    
    /**
     * Deletes previously extracted triples from the provided CMS objects, by this processor.
     * @param objects a list of CMS objects to process
     * @param engine
     */
    void deleteObjects(List<Object> objects, MappingEngine engine);
}

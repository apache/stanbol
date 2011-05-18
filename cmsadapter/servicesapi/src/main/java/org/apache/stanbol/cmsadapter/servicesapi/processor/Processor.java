package org.apache.stanbol.cmsadapter.servicesapi.processor;

import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngine;

public interface Processor {
    Boolean canProcess(Object cmsObject);
    
    void createObjects(List<Object> objects, MappingEngine engine);
    
    void deleteObjects(List<Object> objects, MappingEngine engine);
}

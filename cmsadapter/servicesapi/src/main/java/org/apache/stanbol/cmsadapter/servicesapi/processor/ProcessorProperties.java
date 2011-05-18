package org.apache.stanbol.cmsadapter.servicesapi.processor;

import java.util.Map;

public interface ProcessorProperties {
    String PROCESSING_ORDER = "org.apache.stanbol.cmsadapter.servicesapi.processor.processing_order";

    Integer OBJECT_TYPE = 0;
    Integer CMSOBJECT_POST = 30;
    Integer CMSOBJECT_DEFAULT = 20; 
    Integer CMSOBJECT_PRE = 10;

    Map<String,Object> getProcessorProperties();
}

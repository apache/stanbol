package org.apache.stanbol.enhancer.engines.entitytagging.impl;

import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
//removed annotations until engine actually does something
//@Component(configurationFactory = true, policy = ConfigurationPolicy.REQUIRE, // the baseUri is required!
//    specVersion = "1.1", metatype = true, immediate = true)
//@Service
public class LabelBasedEntityTaggingEngine implements EnhancementEngine, ServiceProperties {

    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
    // TODO Auto-generated method stub

    }

    @Override
    public Map<String,Object> getServiceProperties() {
        // TODO Auto-generated method stub
        return null;
    }

}

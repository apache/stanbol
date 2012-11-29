package org.apache.stanbol.enhancer.nlp;

import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;

public interface NlpServiceProperties extends ServiceProperties{
    
    /**
     * Property Key used by NLP engines to provide their {@link NlpProcessingRole}
     */
    String ENHANCEMENT_ENGINE_NLP_ROLE = "org.apache.stanbol.enhancer.engine.nlp.role";

    
}

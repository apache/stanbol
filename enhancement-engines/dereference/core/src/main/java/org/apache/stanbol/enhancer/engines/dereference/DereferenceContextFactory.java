package org.apache.stanbol.enhancer.engines.dereference;

import java.util.Map;

/**
 * Factory used by the {@link EntityDereferenceEngine} to create
 * {@link DereferenceContext} instances for enhancement requests.
 * @author Rupert Westenthaler
 *
 */
public interface DereferenceContextFactory {

    /**
     * Creates a Dereference Context for the given DereferenceEngine configuration
     * and the request specific enhancement properties
     * @param engine the engine the context is built for
     * @param enhancementProperties the request specific enhancement properties
     * @return the dereference context
     * @throws DereferenceConfigurationException if the request specific configuration
     * is invalid or not supported.
     */
    DereferenceContext createContext(EntityDereferenceEngine engine, 
            Map<String,Object> enhancementProperties) 
                    throws DereferenceConfigurationException;
    
}

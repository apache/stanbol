package org.apache.stanbol.reasoners.servicesapi;

import java.io.IOException;
import java.util.Iterator;

public interface ReasoningServiceInputProvider {

    /**
     * The input data form this provider.
     * 
     * @param type
     * @return
     */
    public <T> Iterator<T> getInput(Class<T> type) throws IOException;
    
    /**
     * If this provider adapts the input to the specified type.
     * 
     * @param type
     * @return
     */
    public <T> boolean adaptTo(Class<T> type);
}

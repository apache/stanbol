package org.apache.stanbol.reasoners.servicesapi;

import java.util.Iterator;
import java.util.List;

import org.apache.stanbol.reasoners.servicesapi.ReasoningServiceInputProvider;

public interface ReasoningServiceInputManager {

    /**
     * Add an input provider
     * 
     * @param provider
     */
    public void addInputProvider(ReasoningServiceInputProvider provider);

    /**
     * Remove the input provider
     * 
     * @param provider
     */
    public void removeInputProvider(ReasoningServiceInputProvider provider);

    /**
     * Get the input data. This should iterate over the collection from all the registered input providers.
     * 
     * Consider that this can be called more then once, to obtain more then one input depending on the type.
     * 
     * It is the Type of the object to instruct about its usage.
     * 
     * @param type
     * @return
     */
    public <T> Iterator<T> getInputData(Class<T> type);
    
    /**
     * Returns the immutable list of registered providers.
     * 
     * @return
     */
    public List<ReasoningServiceInputProvider> getProviders();
}

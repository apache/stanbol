package org.apache.stanbol.reasoners.servicesapi;

import java.util.List;
import java.util.Map;

public interface ReasoningServiceInputFactory {

    /**
     * Creates a new {@see ReasoningServiceInputManager} with registered {@see ReasoningServiceInputProvider}
     * 
     * @return
     */
    public ReasoningServiceInputManager createInputManager(Map<String,List<String>> parameters);
}

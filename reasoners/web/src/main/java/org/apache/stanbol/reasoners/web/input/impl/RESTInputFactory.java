package org.apache.stanbol.reasoners.web.input.impl;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.reasoners.servicesapi.ReasoningServiceInputFactory;
import org.apache.stanbol.reasoners.servicesapi.ReasoningServiceInputManager;
import org.apache.stanbol.reasoners.web.input.provider.impl.FileInputProvider;
import org.apache.stanbol.reasoners.web.input.provider.impl.OntonetInputProvider;
import org.apache.stanbol.reasoners.web.input.provider.impl.RecipeInputProvider;
import org.apache.stanbol.reasoners.web.input.provider.impl.UrlInputProvider;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RESTInputFactory implements ReasoningServiceInputFactory {
    ONManager onm;
    RuleStore rStore;
    
    private Logger log = LoggerFactory.getLogger(getClass());
    
    public RESTInputFactory(ONManager onm,RuleStore rStore){
        this.onm = onm;
        this.rStore = rStore;
    }
    
    @Override
    public ReasoningServiceInputManager createInputManager(Map<String,List<String>> parameters) {
        ReasoningServiceInputManager inmgr = new SimpleInputManager();
        String scope = null;
        String session = null;
        for(Entry<String,List<String>> entry : parameters.entrySet()){
            if (entry.getKey().equals("url")) {
                if(!entry.getValue().isEmpty()){
                    // We keep only the first value 
                    // XXX (make sense support multiple values?)
                    inmgr.addInputProvider(new UrlInputProvider(entry.getValue().iterator().next()));
                }else{
                    // Parameter exists with no value
                    log.error("Parameter 'url' must have a value!");
                    throw new WebApplicationException(Response.Status.BAD_REQUEST);
                }
            }else
            if (entry.getKey().equals("file")) {
                if(!entry.getValue().isEmpty()){
                    // We keep only the first value
                    // FIXME We create the file once again...
                    inmgr.addInputProvider(new FileInputProvider(new File(entry.getValue().iterator().next())));
                }else{
                    // Parameter exists with no value
                    log.error("Parameter 'url' must have a value!");
                    throw new WebApplicationException(Response.Status.BAD_REQUEST);
                }
            }else
            if(entry.getKey().equals("scope")){
                if(!entry.getValue().isEmpty()){
                    scope = entry.getValue().iterator().next();
                }else{
                    // Parameter exists with no value
                    log.error("Parameter 'scope' must have a value!");
                    throw new WebApplicationException(Response.Status.BAD_REQUEST);
                }
               
            }else
            if(entry.getKey().equals("session")){
                if(!entry.getValue().isEmpty()){
                    session = entry.getValue().iterator().next();
                }else{
                    // Parameter exists with no value
                    log.error("Parameter 'session' must have a value!");
                    throw new WebApplicationException(Response.Status.BAD_REQUEST);
                }
               
            }else
            if(entry.getKey().equals("recipe")){
                if(!entry.getValue().isEmpty()){
                    inmgr.addInputProvider(new RecipeInputProvider(rStore, entry.getValue().iterator().next()));
                }else{
                    // Parameter exists with no value
                    log.error("Parameter 'recipe' must have a value!");
                    throw new WebApplicationException(Response.Status.BAD_REQUEST);
                }
               
            }
        }
        if(scope!=null){
            inmgr.addInputProvider(new OntonetInputProvider(onm, scope, session));
        }
        return inmgr;
    }

}

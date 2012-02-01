package org.apache.stanbol.enhancer.jersey.resource;

import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.commons.web.base.CorsHelper.enableCORS;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ChainManager;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import com.sun.jersey.api.view.Viewable;

@Path("/enhancer/chain")
public class ChainsRootResource extends BaseStanbolResource {

    
    private final Map<String, Entry<ServiceReference,Chain>> chains;
    private final Chain defaultChain;
    
    public ChainsRootResource(@Context ServletContext context) {
        // bind the job manager by looking it up from the servlet request context
        ChainManager chainManager = ContextHelper.getServiceFromContext(ChainManager.class, context);
        if(chainManager == null){
            throw new WebApplicationException(new IllegalStateException(
                "The required ChainManager Service is not available!"));
        }
        defaultChain = chainManager.getDefault();
        chains = new HashMap<String,Map.Entry<ServiceReference,Chain>>();
        for(String chainName : chainManager.getActiveChainNames()){
            ServiceReference chainRef = chainManager.getReference(chainName);
            if(chainRef != null){
                Chain chain = chainManager.getChain(chainRef);
                if(chain != null){
                    Map<ServiceReference,Chain> m = Collections.singletonMap(chainRef, chain);
                    chains.put(chainName, m.entrySet().iterator().next());
                }
            }
        }
    }
    @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers){
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext,res, headers);
        return res.build();
    }

    @GET
    @Produces(TEXT_HTML)
    public Response get(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok(new Viewable("index", this),TEXT_HTML);
        addCORSOrigin(servletContext,res, headers);
        return res.build();
    }

    public Collection<Chain> getChains(){
        Set<Chain> chains = new HashSet<Chain>();
        for(Entry<ServiceReference,Chain> entry : this.chains.values()){
            chains.add(entry.getValue());
        }
        return chains;
    }
    public String getServicePid(String name){
        Entry<ServiceReference,Chain> entry = chains.get(name);
        if(entry != null){
            return (String)entry.getKey().getProperty(Constants.SERVICE_PID);
        } else {
            return null;
        }
    }
    public Integer getServiceRanking(String name){
        Entry<ServiceReference,Chain> entry = chains.get(name);
        Integer ranking = null;
        if(entry != null){
            ranking = (Integer)entry.getKey().getProperty(Constants.SERVICE_RANKING);
        }
        if(ranking == null){
            return new Integer(0);
        } else {
            return ranking;
        }
    }
    public Long getServiceId(String name){
        Entry<ServiceReference,Chain> entry = chains.get(name);
        if(entry != null){
            return (Long)entry.getKey().getProperty(Constants.SERVICE_ID);
        } else {
            return null;
        }
    }
    public Chain getDefaultChain(){
        return defaultChain;
    }
    public boolean isDefault(String name){
        return defaultChain.getName().equals(name);
    }
    
    
}

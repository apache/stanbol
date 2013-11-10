package org.apache.stanbol.reasoners.web.resources;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

public interface ReasoningResult {
    
    public void setResult(Object result);
    
    public Object getResult() ;
    
    public HttpHeaders getHeaders() ;
    
    public UriInfo getUriInfo();
}

package org.apache.stanbol.rules.web.writers;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;

public class RulesPrettyPrintResource extends BaseStanbolResource {

	private Object result;

    public RulesPrettyPrintResource(@Context ServletContext servletContext, UriInfo uriInfo, Object result) {
        this.result = result;
        this.uriInfo = uriInfo;
        this.servletContext = servletContext;
    }

    public Object getResult() {
    	
        return this.result;
    }

}

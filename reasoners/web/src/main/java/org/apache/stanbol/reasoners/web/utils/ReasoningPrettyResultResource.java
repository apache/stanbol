package org.apache.stanbol.reasoners.web.utils;

import javax.servlet.ServletContext;
import javax.ws.rs.core.UriInfo;

import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;

public class ReasoningPrettyResultResource extends BaseStanbolResource {
    private Object result;

    public ReasoningPrettyResultResource(ServletContext context, UriInfo uriInfo, Object result) {
        this.result = result;
        this.servletContext = context;
        this.uriInfo = uriInfo;
    }

    public Object getResult() {
        return this.result;
    }
}

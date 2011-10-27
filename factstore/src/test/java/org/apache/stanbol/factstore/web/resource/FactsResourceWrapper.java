package org.apache.stanbol.factstore.web.resource;

import javax.servlet.ServletContext;
import javax.ws.rs.core.UriInfo;

public class FactsResourceWrapper extends FactsResource {

    public FactsResourceWrapper(ServletContext context, UriInfo uriInfo) {
        super(context);
        this.servletContext = context;
        this.uriInfo = uriInfo;
    }

}

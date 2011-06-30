package org.apache.stanbol.ontologymanager.web.resources;

import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;

import com.sun.jersey.api.view.ImplicitProduces;

@Path("/ontonet/documentation/restful")
@ImplicitProduces(MediaType.TEXT_HTML + ";qs=2")
public class RESTfulResource extends BaseStanbolResource {

}

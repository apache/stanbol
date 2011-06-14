package org.apache.stanbol.intcheck.resource;

import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.view.ImplicitProduces;

@Path("/demo")
@ImplicitProduces(MediaType.TEXT_HTML + ";qs=2")
public class WIkinewsDemoResource extends NavigationMixin {

}

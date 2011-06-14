package org.apache.stanbol.intcheck.reasoners;

import javax.ws.rs.Path;

import com.sun.jersey.api.view.ImplicitProduces;

import org.apache.stanbol.intcheck.resource.NavigationMixin;

@Path("/reasoning")
@ImplicitProduces("text/html")
public class ReasoningResource extends NavigationMixin {

}

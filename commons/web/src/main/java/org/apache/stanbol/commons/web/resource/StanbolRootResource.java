package org.apache.stanbol.commons.web.resource;

import javax.ws.rs.Path;


import com.sun.jersey.api.view.ImplicitProduces;

/**
 * Root JAX-RS resource. The HTML view is implicitly rendered by a freemarker
 * template to be found in the META-INF/templates folder.
 */
@Path("/")
@ImplicitProduces("text/html")
public class StanbolRootResource extends NavigationMixin {


}

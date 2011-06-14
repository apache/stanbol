package org.apache.stanbol.intcheck.resource;

import javax.ws.rs.Path;

import com.sun.jersey.api.view.ImplicitProduces;


/**
 * 
 * @author andrea.nuzzolese
 *
 */

@Path("/")
@ImplicitProduces("text/html")
public class RootResource extends NavigationMixin {

}

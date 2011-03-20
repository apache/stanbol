package org.apache.stanbol.kres.jersey.resource;

import javax.ws.rs.Path;

import com.sun.jersey.api.view.ImplicitProduces;


/**
 * 
 * @author andrea.nuzzolese
 *
 */

@Path("/")
@ImplicitProduces("text/html")
public class KReSResource extends NavigationMixin {

}

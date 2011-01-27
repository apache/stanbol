package org.apache.stanbol.enhancer.jersey.resource;

import javax.ws.rs.Path;

import com.sun.jersey.api.view.ImplicitProduces;

/**
 * Root JAX-RS resource. The HTML view is implicitly rendered by a freemarker
 * template to be found in the META-INF/templates folder.
 */
@Path("/")
@ImplicitProduces("text/html")
public class EnhancerRootResource extends NavigationMixin {

    // TODO: add here some controllers to provide some stats on the usage of the
    // Stanbol Enhancer instances: np of content items in the store, nb of registered
    // engines, nb of extracted enhancements, ...

    // Also disable some of the features in the HTML view if the store, sparql
    // engine, engines are not registered.

}

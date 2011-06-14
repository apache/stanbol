/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.stanbol.intcheck.resource;

import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.view.ImplicitProduces;

@Path("/reengineer")
@ImplicitProduces(MediaType.TEXT_HTML + ";qs=2")
public class ReengineerResource extends NavigationMixin {
    
}

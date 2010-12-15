package eu.iksproject.kres.jersey.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/prova")
public class ProvaResource extends NavigationMixin {

	@GET
	public Response prova() {
		return Response.ok("ci hai provato").build();
	}
	
	@GET
	@Path("/saluto")
	public Response saluto() {
		return Response.ok("ciao").build();
	}
	
}

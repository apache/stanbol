package org.apache.stanbol.demos.integritycheck.resources;

import static javax.ws.rs.core.MediaType.TEXT_HTML;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.stanbol.commons.web.base.LinkResource;
import org.apache.stanbol.commons.web.base.ScriptResource;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;

import com.sun.jersey.api.view.ImplicitProduces;
import org.apache.stanbol.commons.ldviewable.Viewable;

/**
 * 
 * @author enridaga
 */
@Path("/integritycheck")
@ImplicitProduces(MediaType.TEXT_HTML + ";qs=2")
public class IntegrityCheckResource extends BaseStanbolResource{
	private final String[] TRUSTED_FRAGMENTS = {"home","integritycheck"};
	@GET
    @Produces(TEXT_HTML)
    public Response get() {
        return Response.ok(new Viewable("index", this), TEXT_HTML).build();
    }
	
	@Override
	public List<LinkResource> getRegisteredLinkResources() {
		List<LinkResource> links = super.getRegisteredLinkResources();
		List<LinkResource> myLinks = new ArrayList<LinkResource>();
		List<String> trusted =  Arrays.asList(TRUSTED_FRAGMENTS);
		for(LinkResource r : links){
			if(trusted.contains(r.getFragmentName())){
				myLinks.add(r);
			}
		}
		return myLinks;
	}
	
	@Override
	public List<ScriptResource> getRegisteredScriptResources() {
		List<ScriptResource> scripts = super.getRegisteredScriptResources();
		List<ScriptResource> myScripts = new ArrayList<ScriptResource>();
		List<String> trusted =  Arrays.asList(TRUSTED_FRAGMENTS);
		for(ScriptResource r : scripts){
			if(trusted.contains(r.getFragmentName())){
				myScripts.add(r);
			}
		}
		return myScripts;
	}
}

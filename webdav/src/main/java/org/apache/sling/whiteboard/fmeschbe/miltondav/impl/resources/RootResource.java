package org.apache.sling.whiteboard.fmeschbe.miltondav.impl.resources;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Resource;

public class RootResource implements PropFindableResource, CollectionResource {
 
	private static final String FOLDERNAME = "root";
 
	
	public String getUniqueId() {
		return FOLDERNAME;
	}
 
	
	public String getName() {
		return FOLDERNAME;
	}
 
	
	public Object authenticate(String user, String password) {
		return "anonymous";
	}
 
	
	public boolean authorise(Request request, Method method, Auth auth) {
		return true;
	}
 
	
	public String getRealm() {
		return null;
	}
 
	
	public Date getCreateDate() {
		return new Date();
	}
 
	
	public Date getModifiedDate() {
		return new Date();
	}
 
	
	public String checkRedirect(Request request) {
		return null;
	}
 
	
	public Resource child(String childName) {
		return null;
	}
 
	
	public List getChildren() {
		List resources = new ArrayList();
		resources.add(new SlingResource());
		return resources;
	}
 
	
	/*public Resource createNew(String newName, InputStream inputStream, Long length, String contentType) throws IOException, ConflictException, NotAuthorizedException, BadRequestException {
		if (SlingResource.getFilename().equals(newName)) {
			StringWriter writer = new StringWriter();
			IOUtils.copy(inputStream, writer, "UTF-8");
			Scratchpad.get().setText(writer.toString());
			return new SlingResource();
		} else {
			throw new BadRequestException(this);
		}
	}*/
 
}

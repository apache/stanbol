package org.apache.sling.whiteboard.fmeschbe.miltondav.impl.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.PutableResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.ResourceFactory;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

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

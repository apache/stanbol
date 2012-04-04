package org.apache.stanbol.webdav.resources;

import java.util.List;

import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

public abstract class AbstractCollectionResource implements CollectionResource {
 
	
	public Resource child(String childName)  throws NotAuthorizedException, BadRequestException {
		@SuppressWarnings("unchecked")
		final List<Resource> resources = (List<Resource>) getChildren();
		for (Resource resource : resources) {
			if (resource.getName().equals(childName)) {
				return resource;
			}
		}
		return null;
	}
 
 
}

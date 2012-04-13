package org.apache.stanbol.contenthub.vfolders;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.stanbol.contenthub.servicesapi.search.featured.ConstrainedDocumentSet;
import org.apache.stanbol.contenthub.servicesapi.search.featured.Constraint;
import org.apache.stanbol.webdav.resources.AbstractCollectionResource;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.GetableResource;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

public class ConstraintResource extends AbstractCollectionResource implements PropFindableResource, GetableResource, CollectionResource {


	private static final String MESSAGE = "Hello world";
	private ConstrainedDocumentSet cds;
	private Constraint constraint;

	public ConstraintResource(ConstrainedDocumentSet cds, Constraint constraint) {
		this.cds = cds;
		this.constraint = constraint;
	}


	public String getUniqueId() {
		return constraint.getValue();
	}
 

	public String getName() {
		return constraint.getValue();
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
 

	public Long getMaxAgeSeconds(Auth auth) {
		return null;
	}
 
	public String getContentType(String accepts) {
		return "text/plain";
	}
 
	public Long getContentLength() {
		return Long.valueOf(MESSAGE.length());
	}
	
	@Override
	public void sendContent(OutputStream out, Range range, Map<String, String>  params, String contentType) throws IOException, NotAuthorizedException, BadRequestException {
		out.write(MESSAGE.getBytes());
	}



	@Override
	public List<? extends Resource> getChildren()
			throws NotAuthorizedException, BadRequestException {
		// TODO Auto-generated method stub
		List<Resource> resources = new ArrayList<Resource>();
		//TODO here we should add:
		//- The categories as in root for the categories that have an entry that can reduce the current set of matching documents (but not to the empty set) 
		//- Add the matching items
		
		return resources;
	}

}

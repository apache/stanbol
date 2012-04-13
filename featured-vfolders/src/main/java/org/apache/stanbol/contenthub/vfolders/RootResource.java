package org.apache.stanbol.contenthub.vfolders;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.contenthub.servicesapi.search.SearchException;
import org.apache.stanbol.contenthub.servicesapi.search.featured.ConstrainedDocumentSet;
import org.apache.stanbol.contenthub.servicesapi.search.featured.Facet;
import org.apache.stanbol.contenthub.servicesapi.search.featured.FacetResult;
import org.apache.stanbol.contenthub.servicesapi.search.featured.FeaturedSearch;
import org.apache.stanbol.contenthub.servicesapi.search.featured.SearchResult;
import org.apache.stanbol.contenthub.servicesapi.search.featured.Constraint;
import org.apache.stanbol.webdav.resources.AbstractCollectionResource;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.SimpleResource;


@Component
@Service(CollectionResource.class)
public class RootResource extends AbstractCollectionResource implements PropFindableResource, CollectionResource {
 
	private static final String FOLDERNAME = "root";
 
	@Reference
	FeaturedSearch featuredSearch;
	
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
 
	
	public List<Resource> getChildren() {
		try {
			ConstrainedDocumentSet cds = featuredSearch.search("*:*", new HashSet<Constraint>());
			Set<Facet> facetResults = cds.getFacets();
			List<Resource> resources = new ArrayList<Resource>();
			for (Facet fr : facetResults) {
				final String name = fr.getLabel(null);
				System.out.println("name: "+name);
				resources.add(new FacetedResource(cds, fr));
			}
			//resources.add(new SlingResource());
			System.out.println("returning: "+resources);
			return resources;
		} catch (SearchException e) {
			throw new RuntimeException(e);
		}
		
	}
 
	

 
}

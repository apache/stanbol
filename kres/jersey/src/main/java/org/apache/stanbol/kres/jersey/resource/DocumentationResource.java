package org.apache.stanbol.kres.jersey.resource;

import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.view.ImplicitProduces;

@Path("/documentation")
@ImplicitProduces(MediaType.TEXT_HTML + ";qs=2")
public class DocumentationResource extends NavigationMixin {

	private final String JAVADOC = "http://stlab.istc.cnr.it/documents/iks/kres/";
	private final String ALPHA_RELEASE_DOCUMENTATION = "http://stlab.istc.cnr.it/documents/iks/IKS-D5.2-1.0-final.pdf";
	private final String REQUIREMENTS_RELEASE_DOCUMENTATION = "http://www.interactive-knowledge.org/sites/www.interactive-knowledge.org/files/IKS_D3.2-1.1.1.pdf";
	private final String ALPHA_RELEASE_CODE = "http://iks-project.googlecode.com/svn/sandbox/kres/trunk";
	
	
	public String getJavadoc(){
		return JAVADOC;
	}
	
	public String getAlphaReleaseDocumentation(){
		return ALPHA_RELEASE_DOCUMENTATION;
	}
	
	public String getRequirementsReleaseDocumentation(){
		return REQUIREMENTS_RELEASE_DOCUMENTATION;
	}
	
	public String getAlphaReleaseCode(){
		return ALPHA_RELEASE_CODE;
	}
}

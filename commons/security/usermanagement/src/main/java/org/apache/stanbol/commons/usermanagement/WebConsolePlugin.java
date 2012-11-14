package org.apache.stanbol.commons.usermanagement;

import java.io.IOException;
import java.net.URL;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.ldpathtemplate.LdRenderer;
import org.apache.stanbol.commons.usermanagement.resource.UserResource;
import org.osgi.framework.BundleContext;

@Component
@Service(Servlet.class)
@Properties({
		@Property(name = "felix.webconsole.label", value = "usermanagement"),
		@Property(name = "felix.webconsole.title", value = "User Management") })
public class WebConsolePlugin extends
		org.apache.felix.webconsole.AbstractWebConsolePlugin {

	private static final String STATIC_PREFIX = "/usermanagement/res/";

	@Reference
	private UserResource userManager;
	
	@Reference
	private LdRenderer ldRenderer;
	
	@Reference
	private Serializer serializer;
	
	public static final String NAME = "User Management";
	public static final String LABEL = "usermanagement";

	public String getLabel() {
		return LABEL;
	}

	public String getTitle() {
		return NAME;
	}

	protected void renderContent(HttpServletRequest req,
			HttpServletResponse response) throws ServletException, IOException {
            
		//TODO enhance LDPath template to support rdf:Lists and return list
		ldRenderer.render(userManager.getUserType(), 
				"html/org/apache/stanbol/commons/usermanagement/webConsole.ftl", response.getWriter());
		serializer.serialize(System.out, userManager.getUserType().getGraph(), SupportedFormat.TURTLE);

	}
	
	protected String[] getCssReferences() {
        String[] result = new String[1];
        //this is to use the stanbol way for static resources
        //http://felix.apache.org/site/providing-resources.html describes the webconsole way
        //TODO make sure things work when stanbol is not in root
        result[0] = "../../static/user-management/styles/webconsole.css";
		return result;
    }

	public void activateBundle(BundleContext bundleContext) {
		super.activate(bundleContext);
	}

	public void deactivate() {
		super.deactivate();

	}
	
	public URL getResource(String path){
		if(path.startsWith(STATIC_PREFIX)){
			return this.getClass().getResource(path.substring(STATIC_PREFIX.length()));
			
		}else {
			return null;
		}
	}
}
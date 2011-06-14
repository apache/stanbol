package org.apache.stanbol.intcheck.web;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.apache.stanbol.intcheck.processors.IntcheckViewProcessor;
import org.apache.stanbol.intcheck.reasoners.ClassifyDemo;
import org.apache.stanbol.intcheck.reasoners.ReasoningResource;
import org.apache.stanbol.intcheck.resource.DocumentationResource;
import org.apache.stanbol.intcheck.resource.GraphsResource;
import org.apache.stanbol.intcheck.resource.RESTfulResource;
import org.apache.stanbol.intcheck.resource.RecipeResource;
import org.apache.stanbol.intcheck.resource.WIkinewsDemoResource;
import org.apache.stanbol.intcheck.resource.RootResource;
import org.apache.stanbol.intcheck.resource.ReengineerResource;
import org.apache.stanbol.intcheck.resource.RefactorerResource;
import org.apache.stanbol.intcheck.resource.SessionResource;

/**
 * Statically define the list of available resources and providers to be used by
 * the KReS JAX-RS Endpoint.
 * 
 * The jersey auto-scan mechanism does not seem to work when deployed through
 * OSGi's HttpService initialization.
 * 
 * In the future this class might get refactored as an OSGi service to allow for
 * dynamic configuration and deployment of additional JAX-RS resources and
 * providers.
 * 
 * @author andrea.nuzzolese
 */

public class JerseyEndpointApplication extends Application {

	@Override
	public Set<Class<?>> getClasses() {
            Set<Class<?>> classes = new HashSet<Class<?>>();
		
            classes.add(WIkinewsDemoResource.class);//OK
            //classes.add(ReasoningResource.class);//OK
            classes.add(ClassifyDemo.class);//OK
            //classes.add(DocumentationResource.class);//OK
            //classes.add(RootResource.class);
            //classes.add(RESTfulResource.class);
            classes.add(RecipeResource.class);
            //classes.add(SessionResource.class);
            //classes.add(ReengineerResource.class);
            //classes.add(RefactorerResource.class);
            //classes.add(GraphsResource.class);
        
            return classes;
	}

	@Override
	public Set<Object> getSingletons() {
		Set<Object> singletons = new HashSet<Object>();
		// view processors
		singletons.add(new IntcheckViewProcessor());
		return singletons;
	}

}
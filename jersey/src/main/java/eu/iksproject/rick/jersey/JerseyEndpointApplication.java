package eu.iksproject.rick.jersey;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.rick.jersey.resource.ReferencedSiteRootResource;
import eu.iksproject.rick.jersey.resource.RickEntityMappingResource;
import eu.iksproject.rick.jersey.resource.RickRootResource;
import eu.iksproject.rick.jersey.resource.RickSymbolResource;
import eu.iksproject.rick.jersey.resource.SiteManagerRootResource;
import eu.iksproject.rick.jersey.writers.JettisonWriter;
import eu.iksproject.rick.jersey.writers.QueryResultListWriter;
import eu.iksproject.rick.jersey.writers.SignWriter;

/**
 * Statically define the list of available resources and providers to be used by
 * the FISE JAX-RS Endpoint.
 *
 * The jersey auto-scan mechanism does not seem to work when deployed through
 * OSGi's HttpService initialization.
 *
 * In the future this class might get refactored as an OSGi service to allow for
 * dynamic configuration and deployment of additional JAX-RS resources and
 * providers.
 */
public class JerseyEndpointApplication extends Application {
	
	Logger log = LoggerFactory.getLogger(getClass());
	public JerseyEndpointApplication() {
		log.info("JerseyEndpointApplication instanceiated");
	}

    @Override
    public Set<Class<?>> getClasses() {
		log.info("JerseyEndpointApplication getClasses called ...");
        Set<Class<?>> classes = new HashSet<Class<?>>();
        // resources
        classes.add(RickRootResource.class);
        classes.add(RickEntityMappingResource.class);
        classes.add(ReferencedSiteRootResource.class);
        classes.add(SiteManagerRootResource.class);
        classes.add(RickSymbolResource.class);
        // message body writers
        classes.add(QueryResultListWriter.class);
        classes.add(SignWriter.class);
        //TODO: somehow writing of Json has not worked because of
        //      A message body writer for Java class org.codehaus.jettison.json.JSONArray, 
        //     and Java type class org.codehaus.jettison.json.JSONArray, and MIME media 
        //     type application/json was not found
        //     As a workaround I have implemented this small workaround!
        classes.add(JettisonWriter.class);
        return classes;
    }

//    @Override
//    public Set<Object> getSingletons() {
//        Set<Object> singletons = new HashSet<Object>();
//        // view processors
//        singletons.add(new FreemarkerViewProcessor());
//        return singletons;
//    }

}

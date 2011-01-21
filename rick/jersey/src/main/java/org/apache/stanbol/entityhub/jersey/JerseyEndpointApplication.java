package org.apache.stanbol.entityhub.jersey;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.apache.stanbol.entityhub.jersey.resource.ReferencedSiteRootResource;
import org.apache.stanbol.entityhub.jersey.resource.EntityMappingResource;
import org.apache.stanbol.entityhub.jersey.resource.EntityhubRootResource;
import org.apache.stanbol.entityhub.jersey.resource.SymbolResource;
import org.apache.stanbol.entityhub.jersey.resource.SiteManagerRootResource;
import org.apache.stanbol.entityhub.jersey.writers.JettisonWriter;
import org.apache.stanbol.entityhub.jersey.writers.QueryResultListWriter;
import org.apache.stanbol.entityhub.jersey.writers.SignWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
        classes.add(EntityhubRootResource.class);
        classes.add(EntityMappingResource.class);
        classes.add(ReferencedSiteRootResource.class);
        classes.add(SiteManagerRootResource.class);
        classes.add(SymbolResource.class);
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

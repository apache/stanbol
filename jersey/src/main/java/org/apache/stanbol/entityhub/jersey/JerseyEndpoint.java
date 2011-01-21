package org.apache.stanbol.entityhub.jersey;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.spi.container.servlet.ServletContainer;

/**
 * Jersey-based RESTful endpoint for the Entityhub
 *
 * This OSGi component serves as a bridge between the OSGi context and the
 * Servlet context available to JAX-RS resources.
 *
 * NOTE: Original Code taken from the FISE
 * @author Rupert Westenthaler
 */

@Component(immediate = true, metatype = true)
public class JerseyEndpoint {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property(value = "/entityhub")
    public static final String ALIAS_PROPERTY = "org.apache.stanbol.entityhub.jersey.alias";

    @Property(value = "/entityhub/static")
    public static final String STATIC_RESOURCES_URL_ROOT_PROPERTY = "org.apache.stanbol.entityhub.jersey.static.url";

    @Property(value = "/entityhub/META-INF/static")
    public static final String STATIC_RESOURCES_CLASSPATH_PROPERTY = "org.apache.stanbol.entityhub.jersey.static.classpath";

    //@Property(value = "/META-INF/templates")
    //public static final String FREEMARKER_TEMPLATE_CLASSPATH_PROPERTY = "org.apache.stanbol.entityhub.jersey.templates.classpath";

    @Reference
    TcManager tcManager;

    @Reference
    Entityhub entityhub;

    @Reference
    ReferencedSiteManager referencedSiteManager;


    @Reference
    HttpService httpService;

    @Reference
    Serializer serializer;


    protected ServletContext servletContext;

    public Dictionary<String, String> getInitParams() {
        // pass configuration for Jersey resource
        // TODO: make the list of enabled JAX-RS resources and providers
        // configurable using an OSGi service
        Dictionary<String, String> initParams = new Hashtable<String, String>();
        initParams.put("javax.ws.rs.Application", JerseyEndpointApplication.class.getName());

        // make jersey automatically turn resources into Viewable models and
        // hence lookup matching freemarker templates
        //initParams.put("com.sun.jersey.config.feature.ImplicitViewables","true");
        return initParams;
    }

    protected void activate(ComponentContext ctx) throws IOException,
            ServletException, NamespaceException {

        // register the JAX-RS resources as a servlet under configurable alias
        ServletContainer container = new ServletContainer();
        String alias = (String) ctx.getProperties().get(ALIAS_PROPERTY);
        String staticUrlRoot = (String) ctx.getProperties().get(
                STATIC_RESOURCES_URL_ROOT_PROPERTY);
        String staticClasspath = (String) ctx.getProperties().get(
                STATIC_RESOURCES_CLASSPATH_PROPERTY);
        //String freemakerTemplates = (String) ctx.getProperties().get(
        //        FREEMARKER_TEMPLATE_CLASSPATH_PROPERTY);

        log.info("Registering servlets with HTTP service "
                + httpService.toString());
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            httpService.registerServlet(alias, container, getInitParams(), null);
            httpService.registerResources(staticUrlRoot, staticClasspath, null);
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }

        // forward the main FISE OSGi components to the servlet context so that
        // they can be looked up by the JAX-RS resources
        servletContext = container.getServletContext();
        //servletContext.setAttribute(EnhancementJobManager.class.getName(),
        //        referencedSiteManager);
        servletContext.setAttribute(Entityhub.class.getName(), entityhub);
        servletContext.setAttribute(BundleContext.class.getName(),
                ctx.getBundleContext());
        servletContext.setAttribute(Serializer.class.getName(), serializer);
        servletContext.setAttribute(TcManager.class.getName(), tcManager);
        servletContext.setAttribute(ReferencedSiteManager.class.getName(),
                referencedSiteManager);
        servletContext.setAttribute(STATIC_RESOURCES_URL_ROOT_PROPERTY,
                staticUrlRoot);
        log.info("Jersey servlet registered at {}", alias);
    }

    protected void deactivate(ComponentContext ctx) {
        log.info("Deactivating jersey bundle");
        String alias = (String) ctx.getProperties().get(ALIAS_PROPERTY);
        httpService.unregister(alias);
        servletContext = null;
    }

    protected void bindRick(Entityhub entityhub) {
        this.entityhub = entityhub;
        if (servletContext != null) {
            servletContext.setAttribute(Entityhub.class.getName(), entityhub);
        }
    }

    protected void unbindRick(Entityhub entityhub) {
        this.entityhub = null;
        if (servletContext != null) {
            servletContext.removeAttribute(Entityhub.class.getName());
        }
    }

    protected void bindReferencedSiteManager(ReferencedSiteManager referencedSiteManager) {
        this.referencedSiteManager = referencedSiteManager;
        if (servletContext != null) {
            servletContext.setAttribute(ReferencedSiteManager.class.getName(), referencedSiteManager);
        }
    }

    protected void unbindReferencedSiteManager(ReferencedSiteManager referencedSiteManager) {
        this.referencedSiteManager = null;
        if (servletContext != null) {
            servletContext.removeAttribute(ReferencedSiteManager.class.getName());
        }
    }

    protected void bindHttpService(HttpService httpService) {
        this.httpService = httpService;
    }

    protected void unbindHttpService(HttpService httpService) {
        this.httpService = null;
    }

}

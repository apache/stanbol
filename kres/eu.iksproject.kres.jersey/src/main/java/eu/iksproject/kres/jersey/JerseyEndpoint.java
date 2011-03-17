package eu.iksproject.kres.jersey;

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
import org.apache.stanbol.ontologymanager.ontonet.api.KReSONManager;
import org.apache.stanbol.ontologymanager.store.api.OntologyStoreProvider;
import org.apache.stanbol.reengineer.base.api.SemionManager;
import org.apache.stanbol.reengineer.base.api.SemionReengineer;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.refactor.api.SemionRefactorer;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.spi.container.servlet.ServletContainer;

import eu.iksproject.kres.jersey.processors.KReSViewProcessor;

/**
 * Jersey-based RESTful endpoint for KReS.
 *
 * This OSGi component serves as a bridge between the OSGi context and the
 * Servlet context available to JAX-RS resources.
 * 
 * @author andrea.nuzzolese
 */

@Component(immediate = true, metatype = true)
public class JerseyEndpoint {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property(value = "/kres")
    public static final String ALIAS_PROPERTY = "eu.iksproject.kres.jersey.alias";

    @Property(value = "/kres/static")
    public static final String STATIC_RESOURCES_URL_ROOT_PROPERTY = "eu.iksproject.kres.jersey.static.url";

    @Property(value = "/META-INF/static")
    public static final String STATIC_RESOURCES_CLASSPATH_PROPERTY = "eu.iksproject.kres.jersey.static.classpath";

    @Property(value = "/META-INF/templates")
    public static final String FREEMARKER_TEMPLATE_CLASSPATH_PROPERTY = "eu.iksproject.kres.jersey.templates.classpath";

    @Reference
    TcManager tcManager;

    @Reference
    HttpService httpService;

    @Reference
    Serializer serializer;

    @Reference
    SemionReengineer semionReengineer;
    
    @Reference
    SemionRefactorer semionRefactorer;
    
    @Reference
    SemionManager reengineeringManager;
    
//    @Reference
//    LinkDiscovery linkDiscovery; 
    
    @Reference
    OntologyStoreProvider ontologyStoreProvider;
	
	@Reference
    RuleStore ruleStore;
	
    @Reference
    KReSONManager onm;

    protected ServletContext servletContext;

    public Dictionary<String, String> getInitParams() {
        // pass configuration for Jersey resource
        // TODO: make the list of enabled JAX-RS resources and providers
        // configurable using an OSGi service
        Dictionary<String, String> initParams = new Hashtable<String, String>();
        initParams.put("javax.ws.rs.Application",
                JerseyEndpointApplication.class.getName());

        // make jersey automatically turn resources into Viewable models and
        // hence lookup matching freemarker templates
        initParams.put("com.sun.jersey.config.feature.ImplicitViewables",
                "true");
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
        String freemakerTemplates = (String) ctx.getProperties().get(
                FREEMARKER_TEMPLATE_CLASSPATH_PROPERTY);

        log.info("Registering servlets with HTTP service "
                + httpService.toString());
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(
                getClass().getClassLoader());
        try {
            httpService.registerServlet(alias, container, getInitParams(), null);
            httpService.registerResources(staticUrlRoot, staticClasspath, null);
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }

        // forward the main KReS OSGi components to the servlet context so that
        // they can be looked up by the JAX-RS resources
        servletContext = container.getServletContext();
        servletContext.setAttribute(BundleContext.class.getName(),
                ctx.getBundleContext());
        servletContext.setAttribute(Serializer.class.getName(), serializer);
        servletContext.setAttribute(SemionReengineer.class.getName(),
                semionReengineer);
        servletContext.setAttribute(SemionRefactorer.class.getName(),
                semionRefactorer);
//        servletContext.setAttribute(LinkDiscovery.class.getName(),
//        		linkDiscovery);
        servletContext.setAttribute(SemionManager.class.getName(),
        		reengineeringManager);
        servletContext.setAttribute(TcManager.class.getName(), tcManager);
        
		servletContext.setAttribute(KReSONManager.class.getName(), onm);
		
		servletContext.setAttribute(OntologyStoreProvider.class.getName(), ontologyStoreProvider);

        servletContext.setAttribute(STATIC_RESOURCES_URL_ROOT_PROPERTY,
                staticUrlRoot);
        servletContext.setAttribute(
                KReSViewProcessor.FREEMARKER_TEMPLATE_PATH_INIT_PARAM,
                freemakerTemplates);
				
		//Rule manager
		servletContext.setAttribute(RuleStore.class.getName(), 
				ruleStore);

        log.info("Jersey servlet registered at {}", alias);
    }

    protected void deactivate(ComponentContext ctx) {
        log.info("Deactivating jersey bundle");
        String alias = (String) ctx.getProperties().get(ALIAS_PROPERTY);
        httpService.unregister(alias);
        servletContext = null;
    }

    
    protected void bindHttpService(HttpService httpService) {
        this.httpService = httpService;
    }

    protected void unbindHttpService(HttpService httpService) {
        this.httpService = null;
    }

}

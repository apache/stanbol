package org.apache.stanbol.commons.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.stanbol.commons.web.resource.NavigationMixin;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.spi.container.servlet.ServletContainer;

import freemarker.cache.ClassTemplateLoader;

/**
 * Jersey-based RESTful endpoint for the Stanbol Enhancer engines and store.
 * <p>
 * This OSGi component serves as a bridge between the OSGi context and the Servlet context available to JAX-RS
 * resources.
 */
@Component(immediate = true, metatype = true)
@Reference(name = "webFragment", referenceInterface = WebFragment.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
public class JerseyEndpoint {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property(value = "/")
    public static final String ALIAS_PROPERTY = "org.apache.stanbol.commons.web.alias";

    @Property(value = "/static")
    public static final String STATIC_RESOURCES_URL_ROOT_PROPERTY = "org.apache.stanbol.commons.web.static.url";

    @Property(value = "/META-INF/static")
    public static final String STATIC_RESOURCES_CLASSPATH_PROPERTY = "org.apache.stanbol.commons.web.static.classpath";

    @Property(value = "/META-INF/templates")
    public static final String FREEMARKER_TEMPLATE_CLASSPATH_PROPERTY = "org.apache.stanbol.commons.web.templates.classpath";

    @Reference
    HttpService httpService;

    protected ComponentContext componentContext;

    protected ServletContext servletContext;

    protected final List<WebFragment> webFragments = new ArrayList<WebFragment>();

    protected ArrayList<String> registeredAlias;

    public Dictionary<String,String> getInitParams() {
        Dictionary<String,String> initParams = new Hashtable<String,String>();
        // make jersey automatically turn resources into Viewable models and
        // hence lookup matching freemarker templates
        initParams.put("com.sun.jersey.config.feature.ImplicitViewables", "true");
        return initParams;
    }

    @Activate
    protected void activate(ComponentContext ctx) throws IOException, ServletException, NamespaceException {
        this.componentContext = ctx;
        this.registeredAlias = new ArrayList<String>();

        // register all the JAX-RS resources into a a JAX-RS application and bind it to a configurable URL
        // prefix
        JerseyEndpointApplication app = new JerseyEndpointApplication();
        String staticUrlRoot = (String) ctx.getProperties().get(STATIC_RESOURCES_URL_ROOT_PROPERTY);
        String staticClasspath = (String) ctx.getProperties().get(STATIC_RESOURCES_CLASSPATH_PROPERTY);
        String templateClasspath = (String) ctx.getProperties().get(FREEMARKER_TEMPLATE_CLASSPATH_PROPERTY);

        // register the base template loader
        templateClasspath = templateClasspath.replaceAll("/$", "");
        app.contributeTemplateLoader(new ClassTemplateLoader(getClass(), templateClasspath));

        // register the root of static resources
        httpService.registerResources(staticUrlRoot, staticClasspath, null);
        registeredAlias.add(staticUrlRoot);

        // incrementally contribute fragment resources
        List<LinkResource> linkResources = new ArrayList<LinkResource>();
        List<ScriptResource> scriptResources = new ArrayList<ScriptResource>();
        
        for (WebFragment fragment : webFragments) {
            log.info("Registering web fragment '{}' into jaxrs application", fragment.getName());
            linkResources.addAll(fragment.getLinkResources());
            scriptResources.addAll(fragment.getScriptResources());
            app.contributeClasses(fragment.getJaxrsResourceClasses());
            app.contributeSingletons(fragment.getJaxrsResourceSingletons());
            app.contributeTemplateLoader(fragment.getTemplateLoader());
            String resourceAlias = staticUrlRoot + '/' + fragment.getName();
            httpService.registerResources(resourceAlias, fragment.getStaticResourceClassPath(), null);
            registeredAlias.add(resourceAlias);
        }

        ServletContainer container = new ServletContainer(app);
        String alias = (String) ctx.getProperties().get(ALIAS_PROPERTY);

        // TODO: check whether this class-loading hack is still necessary or not
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            httpService.registerServlet(alias, container, getInitParams(), null);
            registeredAlias.add(alias);
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }

        // forward the main Stanbol OSGi runtime context so that JAX-RS resources can lookup arbitrary
        // services
        servletContext = container.getServletContext();
        servletContext.setAttribute(BundleContext.class.getName(), ctx.getBundleContext());
        servletContext.setAttribute(NavigationMixin.STATIC_RESOURCES_ROOT_URL, staticUrlRoot);
        servletContext.setAttribute(NavigationMixin.LINK_RESOURCES, linkResources);
        servletContext.setAttribute(NavigationMixin.SCRIPT_RESOURCES, scriptResources);
        log.info("JerseyEndpoint servlet registered at {}", alias);
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        for (String alias : registeredAlias) {
            httpService.unregister(alias);
        }
        servletContext = null;
    }

    protected void bindHttpService(HttpService httpService) {
        this.httpService = httpService;
    }

    protected void unbindHttpService(HttpService httpService) {
        this.httpService = null;
    }

    protected void bindWebFragment(WebFragment webFragment) throws IOException,
                                                           ServletException,
                                                           NamespaceException {
        // TODO: support some ordering for jax-rs resource and template overrides?
        webFragments.add(webFragment);
        if (componentContext != null) {
            deactivate(componentContext);
            activate(componentContext);
        }
    }

    protected void unbindWebFragment(WebFragment webFragment) throws IOException,
                                                             ServletException,
                                                             NamespaceException {
        webFragments.remove(webFragment);
        if (componentContext != null) {
            deactivate(componentContext);
            activate(componentContext);
        }
    }

}

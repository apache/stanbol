package org.apache.stanbol.commons.web.base;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.spi.container.servlet.ServletContainer;

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

    @Reference
    HttpService httpService;

    protected ComponentContext componentContext;

    protected ServletContext servletContext;

    protected final List<WebFragment> webFragments = new ArrayList<WebFragment>();

    protected ArrayList<String> registeredAliases;

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
        this.registeredAliases = new ArrayList<String>();

        // register all the JAX-RS resources into a a JAX-RS application and bind it to a configurable URL
        // prefix
        JerseyEndpointApplication app = new JerseyEndpointApplication();
        String staticUrlRoot = (String) ctx.getProperties().get(STATIC_RESOURCES_URL_ROOT_PROPERTY);

        // incrementally contribute fragment resources
        List<LinkResource> linkResources = new ArrayList<LinkResource>();
        List<ScriptResource> scriptResources = new ArrayList<ScriptResource>();
        List<NavigationLink> navigationLinks = new ArrayList<NavigationLink>();
        for (WebFragment fragment : webFragments) {
            log.info("Registering web fragment '{}' into jaxrs application", fragment.getName());
            linkResources.addAll(fragment.getLinkResources());
            scriptResources.addAll(fragment.getScriptResources());
            navigationLinks.addAll(fragment.getNavigationLinks());
            app.contributeClasses(fragment.getJaxrsResourceClasses());
            app.contributeSingletons(fragment.getJaxrsResourceSingletons());
            app.contributeTemplateLoader(fragment.getTemplateLoader());
            String resourceAlias = staticUrlRoot + '/' + fragment.getName();
            httpService.registerResources(resourceAlias, fragment.getStaticResourceClassPath(),
                new BundleHttpContext(fragment));
            registeredAliases.add(resourceAlias);
        }
        Collections.sort(linkResources);
        Collections.sort(scriptResources);
        Collections.sort(navigationLinks);

        // bind the aggregate JAX-RS application to a dedicated servlet
        ServletContainer container = new ServletContainer(app);
        String applicationAlias = (String) ctx.getProperties().get(ALIAS_PROPERTY);
        Bundle appBundle = ctx.getBundleContext().getBundle();
        httpService.registerServlet(applicationAlias, container, getInitParams(), new BundleHttpContext(
                appBundle));
        registeredAliases.add(applicationAlias);

        // forward the main Stanbol OSGi runtime context so that JAX-RS resources can lookup arbitrary
        // services
        servletContext = container.getServletContext();
        servletContext.setAttribute(BundleContext.class.getName(), ctx.getBundleContext());
        servletContext.setAttribute(BaseStanbolResource.ROOT_URL, applicationAlias);
        servletContext.setAttribute(BaseStanbolResource.STATIC_RESOURCES_ROOT_URL, staticUrlRoot);
        servletContext.setAttribute(BaseStanbolResource.LINK_RESOURCES, linkResources);
        servletContext.setAttribute(BaseStanbolResource.SCRIPT_RESOURCES, scriptResources);
        servletContext.setAttribute(BaseStanbolResource.NAVIGATION_LINKS, navigationLinks);
        log.info("JerseyEndpoint servlet registered at {}", applicationAlias);
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        for (String alias : registeredAliases) {
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

    public List<WebFragment> getWebFragments() {
        return webFragments;
    }

}

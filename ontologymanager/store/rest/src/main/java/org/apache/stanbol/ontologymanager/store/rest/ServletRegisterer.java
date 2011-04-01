package org.apache.stanbol.ontologymanager.store.rest;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.stanbol.ontologymanager.store.api.PersistenceStore;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.spi.container.servlet.ServletContainer;

@Component(immediate = true, metatype = true)
public class ServletRegisterer {

    private static Logger logger = LoggerFactory.getLogger(ServletRegisterer.class.getName());

    @Property(value = "/ontologymanager/store")
    private static final String ALIAS_PROP = "org.apache.stanbol.ontologymanager.store.rest.alias";

    @Property(value = "/ontologymanager/store/static")
    public static final String STATIC_RESOURCES_URL_ROOT_PROPERTY = "eu.iksproject.fise.jersey.static.url";

    @Property(value = "/META-INF/static")
    public static final String STATIC_RESOURCES_CLASSPATH_PROPERTY = "eu.iksproject.fise.jersey.static.classpath";

    @Property(value = "/META-INF/templates")
    public static final String FREEMARKER_TEMPLATE_CLASSPATH_PROPERTY = "eu.iksproject.fise.jersey.templates.classpath";

    private String alias;

    @Reference
    private PersistenceStore persistenceStore;

    @Reference
    private HttpService httpService;

    protected ServletContext servletContext;

    public ServletRegisterer() {}

    @Activate
    public void activate(ComponentContext cc) {

        this.alias = (String) cc.getProperties().get(ALIAS_PROP);
        String staticUrlRoot = (String) cc.getProperties().get(STATIC_RESOURCES_URL_ROOT_PROPERTY);
        String staticClasspath = (String) cc.getProperties().get(STATIC_RESOURCES_CLASSPATH_PROPERTY);
        String freemakerTemplates = (String) cc.getProperties().get(FREEMARKER_TEMPLATE_CLASSPATH_PROPERTY);
        ServletContainer container = new ServletContainer();
        ClassLoader ctc = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(ServletRegisterer.class.getClassLoader());
        try {
            httpService.registerServlet(alias, container, getInitParams(), null);
            httpService.registerResources(staticUrlRoot, staticClasspath, null);
        } catch (ServletException e) {
            throw new RuntimeException("Servlet Registration Failed", e);
        } catch (NamespaceException e) {
            throw new RuntimeException("Namespace exception", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(ctc);
        }
        servletContext = container.getServletContext();
        servletContext.setAttribute(PersistenceStore.class.getName(), persistenceStore);
        servletContext.setAttribute(STATIC_RESOURCES_URL_ROOT_PROPERTY, staticUrlRoot);
        servletContext.setAttribute(FreemarkerViewProcessor.FREEMARKER_TEMPLATE_PATH_INIT_PARAM,
            freemakerTemplates);
        logger.info("Registered resources");

    }

    @Deactivate
    public void deactive() {
        httpService.unregister(alias);
        servletContext = null;
    }

    public Dictionary<String,String> getInitParams() {
        Dictionary<String,String> initParams = new Hashtable<String,String>();
        initParams.put("javax.ws.rs.Application", JerseyApplication.class.getName());
        initParams.put("com.sun.jersey.config.feature.ImplicitViewables", "true");
        initParams.put("com.sun.jersey.spi.container.ContainerRequestFilters",
            "com.sun.jersey.api.container.filter.PostReplaceFilter");

        return initParams;
    }

    public void bindPersistenceStore(PersistenceStore persistenceStore) {
        this.persistenceStore = persistenceStore;
        if (servletContext != null) {
            servletContext.setAttribute(PersistenceStore.class.getName(), persistenceStore);
        }
    }

    public void unbindPersistenceStore(PersistenceStore persistenceStore) {
        this.persistenceStore = null;
        if (servletContext != null) {
            servletContext.setAttribute(PersistenceStore.class.getName(), null);
        }
    }

    public void bindHttpService(HttpService httpService) {
        this.httpService = httpService;
    }

    public void unbindHttpService(HttpService httpService) {
        this.httpService = null;
    }
}

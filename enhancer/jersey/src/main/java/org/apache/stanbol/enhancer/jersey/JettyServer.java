package org.apache.stanbol.enhancer.jersey;

import java.net.URI;
import java.util.Dictionary;
import java.util.Enumeration;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.spi.container.servlet.ServletContainer;

/**
 * Standalone (OSGi independent) Jetty-based server with the Stanbol Enhancer Jersey
 * endpoint. The OSGi component need be injected manually to the ServletContext
 * to make them available to the resources.
 * <p>
 * This class is mainly useful for testing the JAX-RS resources without faking a
 * complete OSGI runtime.
 * <p>
 * For seamless OSGi deployments the JerseyEndpoint component should be
 * automatically registered in the container and registers the JAXRS resources
 * automatically.
 */
public class JettyServer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static String DEFAULT_BASE_URI = "http://localhost:9998/";

    protected Server server = new Server();

    private Context context;

    public void start(String baseUri) throws Exception {
        log.info("starting the Jetty / Jersey endpoint");
        ServletHolder servletHolder = new ServletHolder(ServletContainer.class);
        Dictionary<String, String> initParams = new JerseyEndpoint().getInitParams();
        Enumeration<String> keys = initParams.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            servletHolder.setInitParameter(key, initParams.get(key));
        }

        URI uri = new URI(baseUri);
        // restart any previous server instance while keeping the previous
        // attribute settings
        if (server.isRunning()) {
            server.stop();
        }
        server = new Server(uri.getPort());
        context = new Context(server, "/", Context.SESSIONS);
        context.addServlet(servletHolder, "/*");
        server.start();
    }

    public void stop() throws Exception {
        log.info("stopping the Jetty / Jersey endpoint");
        server.stop();
    }

    public void setAttribute(String name, Object value) {
        if (context != null) {
            context.getServletContext().setAttribute(name, value);
        }
    }

    public Object getAttribute(String name) {
        if (context != null) {
            return context.getServletContext().getAttribute(name);
        }
        return null;
    }

    public void removeAttribute(String name) {
        if (context != null) {
            context.getServletContext().removeAttribute(name);
        }
    }

    /**
     * For starting manually.
     */
    public static void main(String[] args) throws Exception {
        JettyServer server = new JettyServer();
        server.start(DEFAULT_BASE_URI);
        System.out.println("Hit enter to stop it...");
        System.in.read();
        server.stop();
    }

}

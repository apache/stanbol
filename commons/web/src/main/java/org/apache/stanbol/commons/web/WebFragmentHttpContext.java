package org.apache.stanbol.commons.web;

import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

/**
 * Custom HTTP Context to lookup the resources from the classloader of the WebFragment bundle.
 */
public class WebFragmentHttpContext implements HttpContext {

    private Bundle bundle;

    public WebFragmentHttpContext(WebFragment fragment) {
        this.bundle = fragment.getBundleContext().getBundle();
    }

    public String getMimeType(String name) {
        // someone in the chain seems to already be doing the Mime type mapping
        return null;
    }

    public URL getResource(String name) {
        if (name.startsWith("/")) {
            name = name.substring(1);
        }

        return this.bundle.getResource(name);
    }

    public boolean handleSecurity(HttpServletRequest req, HttpServletResponse res) {
        return true;
    }

}

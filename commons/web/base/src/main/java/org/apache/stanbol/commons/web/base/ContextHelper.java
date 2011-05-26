package org.apache.stanbol.commons.web.base;

import javax.servlet.ServletContext;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class ContextHelper {

    // TODO: turn the following into a annotation that derives from the JAX-RS @Context annotation
    /**
     * Fetch an OSGi service instance broadcasted into the OSGi context.
     * 
     * @param <T>
     *            the type of the service
     * @param clazz
     *            the class of the service
     * @param context
     *            the servlet context
     * @return the registered instance of the service (assuming cardinality 1)
     */
    @SuppressWarnings("unchecked")
    public static <T> T getServiceFromContext(Class<T> clazz, ServletContext context) {
        BundleContext bundleContext = (BundleContext) context.getAttribute(BundleContext.class.getName());
        ServiceReference reference = bundleContext.getServiceReference(clazz.getName());
        //TODO: returning the service will cause the service reference not to be
        //  released bundleContext.ungetService(reference) will not be called!
        return (T) bundleContext.getService(reference);
    }

}

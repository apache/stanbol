package org.apache.stanbol.commons.freemarker.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import freemarker.log.Logger;

/**
 * {@link BundleActivator} for Freebase. Currently it sets only SLFJ as
 * Logging framework to the Freebase {@link Logger}
 * @author westei
 *
 */
public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        Logger.selectLoggerLibrary(Logger.LIBRARY_SLF4J);
    }

    @Override
    public void stop(BundleContext context) throws Exception {    }

}

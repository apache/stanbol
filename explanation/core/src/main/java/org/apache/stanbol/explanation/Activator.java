package org.apache.stanbol.explanation;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void start(BundleContext context) throws Exception {
        log.info("Apache Stanbol Explanation engine OSGi bundle activated");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        log.info("Apache Stanbol Explanation engine OSGi bundle deactivated");
    }

}

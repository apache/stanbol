package org.apache.stanbol.enhancer.engines.autotagging;

import org.apache.stanbol.autotagging.Autotagger;
import org.osgi.framework.BundleContext;


/**
 * Interface to fetch a configured instance of an autotagger.
 */
public interface AutotaggerProvider {

    /**
     * @return the autotagger instance or null if none is configured
     */
    Autotagger getAutotagger();

    BundleContext getBundleContext();

}

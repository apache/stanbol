package eu.iksproject.fise.engines.autotagging;

import org.osgi.framework.BundleContext;

import eu.iksproject.autotagging.Autotagger;

/**
 * Interface to fetch a configured instance of an autotagger.
 */
public interface AutotaggerProvider {

    /**
     * @return the autotagger instance or null if none is configured
     */
    public Autotagger getAutotagger();

    public BundleContext getBundleContext();

}

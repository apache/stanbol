package org.apache.stanbol.commons.solr.impl;

import java.util.Properties;

import org.apache.solr.cloud.ZkController;
import org.apache.solr.cloud.ZkSolrResourceLoader;
import org.apache.solr.common.SolrException;
import org.osgi.framework.BundleContext;

/**
 * Extends the {@link ZkSolrResourceLoader} to support findClass(..) methods
 * to load classes via OSGI.
 * 
 * @author Rupert Westenthaler
 *
 */
public class OsgiZkSolrResourceLoader extends ZkSolrResourceLoader {


    protected final BundleContext bc;
    
    public OsgiZkSolrResourceLoader(BundleContext bc, String instanceDir, 
            String collection, ZkController zooKeeperController) {
        super(instanceDir,collection,zooKeeperController);
        this.bc = bc;
    }
    
    public OsgiZkSolrResourceLoader(BundleContext bc, String instanceDir, 
            String collection, ClassLoader parent, Properties coreProperties, 
            ZkController zooKeeperController) {
        super(instanceDir, collection, parent, coreProperties, zooKeeperController);
        this.bc = bc;
    }
    
    @Override
    public <T> Class<? extends T> findClass(String cname, Class<T> expectedType, String... subpackages) {
        Class<? extends T> clazz = null;
        RuntimeException parentEx = null;
        try {
            clazz = super.findClass(cname, expectedType, subpackages);
        } catch (RuntimeException e) {
            parentEx = e;
        }
        if (clazz != null) {
            return clazz;
        } else {
            try {
                //try to load via the OSGI service factory
                return OsgiResourceLoaderUtil.findOsgiClass(bc, cname, expectedType, subpackages);
            } catch (SolrException e) {
                //prefer to throw the first exception
                if(parentEx != null){
                    throw parentEx;
                } else {
                    throw e;
                }
            }
        }
    }
}

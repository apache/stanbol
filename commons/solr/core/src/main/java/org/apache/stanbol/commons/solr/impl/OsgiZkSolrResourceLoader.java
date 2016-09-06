/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

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

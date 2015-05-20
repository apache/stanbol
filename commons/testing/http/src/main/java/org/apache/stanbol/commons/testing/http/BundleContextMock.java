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
package org.apache.stanbol.commons.testing.http;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Simple Mock to mock a BundleContext in tests.
 *
 * <p>
 * Use the {@link #putService(String clazz, Object instance)} method to put add a service instance to
 * the BundleContextMock.
 *
 * @author Fabian Christ
 */
public class BundleContextMock implements BundleContext {
    
    private Map<String,ServiceReference> serviceReferenceMap = new HashMap<String,ServiceReference>();
    private Map<ServiceReference, Object> serviceMap = new HashMap<ServiceReference,Object>();

    public void addBundleListener(BundleListener listener) {
    

    }

    public void addFrameworkListener(FrameworkListener listener) {
    

    }

    public void addServiceListener(ServiceListener listener) {
    

    }

    public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException {
    

    }

    public Filter createFilter(String filter) throws InvalidSyntaxException {
        
        return null;
    }

    public ServiceReference[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
        
        return null;
    }

    public Bundle getBundle() {
        
        return null;
    }

    public Bundle getBundle(long id) {
        
        return null;
    }

    public Bundle[] getBundles() {
        
        return null;
    }

    public File getDataFile(String filename) {
        
        return null;
    }

    public String getProperty(String key) {
        
        return null;
    }

    public <S> S getService(ServiceReference<S> reference) {
        return (S)this.serviceMap.get(reference);
    }
    
    /**
     * Use this method to add a service instance to the mock.
     * 
     * @param clazz FQCN of the class (or the interface)
     * @param instance An instance of that class.
     */
    public void putService(String clazz, Object instance) {
        ServiceReference dummy = new ServiceReference() {
            
            public boolean isAssignableTo(Bundle bundle, String className) {
                return false;
            }
            
            public Bundle[] getUsingBundles() {
                return null;
            }
            
            public String[] getPropertyKeys() {
                return null;
            }
            
            public Object getProperty(String key) {
                return null;
            }
            
            public Bundle getBundle() {
                return null;
            }
            
            public int compareTo(Object reference) {
                return 0;
            }
        };
        this.serviceReferenceMap.put(clazz, dummy);
        this.serviceMap.put(dummy, instance);
    }

    public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
        return null;
    }

    public Bundle installBundle(String location) throws BundleException {
        return null;
    }

    public Bundle installBundle(String location, InputStream input) throws BundleException {
        return null;
    }

    public void removeBundleListener(BundleListener listener) {
    }

    public void removeFrameworkListener(FrameworkListener listener) {
    }

    public void removeServiceListener(ServiceListener listener) {
    }

    @Override
    public ServiceRegistration<?> registerService(String[] clazzes, Object service,
            Dictionary<String,?> properties) {
        return null;
    }

    @Override
    public ServiceRegistration<?> registerService(String clazz, Object service,
            Dictionary<String,?> properties) {
        return null;
    }

    @Override
    public <S> ServiceRegistration<S> registerService(Class<S> clazz, S service,
            Dictionary<String,?> properties) {
        return null;
    }

    @Override
    public <S> ServiceReference<S> getServiceReference(Class<S> clazz) {
        return (ServiceReference<S>) getServiceReference(clazz.getName());
    }
    @Override
    public ServiceReference<?> getServiceReference(String clazz) {
        return this.serviceReferenceMap.get(clazz);
    }

    @Override
    public <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> clazz, String filter)
            throws InvalidSyntaxException {
        return null;
    }

    public boolean ungetService(ServiceReference<?> reference) {
        return false;
    }

    @Override
    public Bundle getBundle(String location) {
        return null;
    }


}

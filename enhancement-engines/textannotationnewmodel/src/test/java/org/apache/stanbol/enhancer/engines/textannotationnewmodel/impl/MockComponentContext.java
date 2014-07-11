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
package org.apache.stanbol.enhancer.engines.textannotationnewmodel.impl;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;

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
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentInstance;

public class MockComponentContext implements ComponentContext {

    protected final Dictionary<String, Object> properties;
    protected final BundleContext bundleContext = new MockBundleContext();

    public MockComponentContext() {
        properties = new Hashtable<String, Object>();
    }

    public MockComponentContext(Dictionary<String, Object> properties) {
        this.properties = properties;
    }

    public void disableComponent(String name) {
        throw new UnsupportedOperationException("Mock implementation");
    }

    public void enableComponent(String name) {
        throw new UnsupportedOperationException("Mock implementation");
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public ComponentInstance getComponentInstance() {
        throw new UnsupportedOperationException("Mock implementation");
    }

    public Dictionary<String, Object> getProperties() {
        return properties;
    }

    public ServiceReference getServiceReference() {
        throw new UnsupportedOperationException("Mock implementation");
    }

    public Bundle getUsingBundle() {
        throw new UnsupportedOperationException("Mock implementation");
    }

    public Object locateService(String name) {
        throw new UnsupportedOperationException("Mock implementation");
    }

    public Object locateService(String name, ServiceReference reference) {
        throw new UnsupportedOperationException("Mock implementation");
    }

    public Object[] locateServices(String name) {
        throw new UnsupportedOperationException("Mock implementation");
    }

    private static final class MockBundleContext implements BundleContext {
        /**
         * Used by the Engine to read System properties
         */
        @Override
        public String getProperty(String key) {
            return System.getProperty(key);
        }

        @Override
        public Bundle getBundle() {
            throw new UnsupportedOperationException("Mock implementation");
        }

        @Override
        public Bundle installBundle(String location) throws BundleException {
            throw new UnsupportedOperationException("Mock implementation");
        }

        @Override
        public Bundle installBundle(String location, InputStream input) throws BundleException {
            throw new UnsupportedOperationException("Mock implementation");
        }

        @Override
        public Bundle getBundle(long id) {
            throw new UnsupportedOperationException("Mock implementation");
        }

        @Override
        public Bundle[] getBundles() {
            throw new UnsupportedOperationException("Mock implementation");
        }

        @Override
        public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException {
            throw new UnsupportedOperationException("Mock implementation");
        }

        @Override
        public void addServiceListener(ServiceListener listener) {
            throw new UnsupportedOperationException("Mock implementation");
        }

        @Override
        public void removeServiceListener(ServiceListener listener) {
            throw new UnsupportedOperationException("Mock implementation");
        }

        @Override
        public void addBundleListener(BundleListener listener) {
            throw new UnsupportedOperationException("Mock implementation");
        }

        @Override
        public void removeBundleListener(BundleListener listener) {
            throw new UnsupportedOperationException("Mock implementation");
        }

        @Override
        public void addFrameworkListener(FrameworkListener listener) {
            throw new UnsupportedOperationException("Mock implementation");
        }

        @Override
        public void removeFrameworkListener(FrameworkListener listener) {
            throw new UnsupportedOperationException("Mock implementation");
        }

        @Override
        public ServiceRegistration registerService(String[] clazzes, Object service, Dictionary properties) {
            throw new UnsupportedOperationException("Mock implementation");
        }

        @Override
        public ServiceRegistration registerService(String clazz, Object service, Dictionary properties) {
            throw new UnsupportedOperationException("Mock implementation");
        }

        @Override
        public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
            throw new UnsupportedOperationException("Mock implementation");
        }

        @Override
        public ServiceReference[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
            throw new UnsupportedOperationException("Mock implementation");
        }

        @Override
        public ServiceReference getServiceReference(String clazz) {
            throw new UnsupportedOperationException("Mock implementation");
        }

        @Override
        public Object getService(ServiceReference reference) {
            throw new UnsupportedOperationException("Mock implementation");
        }

        @Override
        public boolean ungetService(ServiceReference reference) {
            throw new UnsupportedOperationException("Mock implementation");
        }

        @Override
        public File getDataFile(String filename) {
            throw new UnsupportedOperationException("Mock implementation");
        }

        @Override
        public Filter createFilter(String filter) throws InvalidSyntaxException {
            throw new UnsupportedOperationException("Mock implementation");
        }

        @Override
        public <S> ServiceRegistration<S> registerService(Class<S> clazz, S service, Dictionary<String, ?> properties) {
            return null;
        }

        @Override
        public <S> ServiceReference<S> getServiceReference(Class<S> clazz) {
            return null;
        }

        @Override
        public <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> clazz, String filter) throws InvalidSyntaxException {
            return null;
        }

        @Override
        public Bundle getBundle(String location) {
            return null;
        }
        
    }
}

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
package org.apache.stanbol.enhancer.engines.keywordextraction.impl;

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

    public MockComponentContext() {
        properties = new Hashtable<String, Object>();
    }

    public MockComponentContext(Dictionary<String, Object> properties) {
        this.properties = properties;
    }

    public void disableComponent(String name) {
    }

    public void enableComponent(String name) {
    }

    public BundleContext getBundleContext() {
        return new BundleContext() {

            @Override
            public boolean ungetService(ServiceReference reference) {
                return false;
            }

            @Override
            public void removeServiceListener(ServiceListener listener) {
            }

            @Override
            public void removeFrameworkListener(FrameworkListener listener) {
            }

            @Override
            public void removeBundleListener(BundleListener listener) {
            }

            @Override
            public ServiceRegistration registerService(String clazz,
                    Object service, Dictionary properties) {
                return null;
            }

            @Override
            public ServiceRegistration registerService(String[] clazzes,
                    Object service, Dictionary properties) {
                return null;
            }

            @Override
            public Bundle installBundle(String location, InputStream input)
                    throws BundleException {
                return null;
            }

            @Override
            public Bundle installBundle(String location) throws BundleException {
                return null;
            }

            @Override
            public ServiceReference[] getServiceReferences(String clazz,
                    String filter) throws InvalidSyntaxException {
                return null;
            }

            @Override
            public ServiceReference getServiceReference(String clazz) {
                return null;
            }

            @Override
            public Object getService(ServiceReference reference) {
                return null;
            }

            @Override
            public String getProperty(String key) {
                return null;
            }

            @Override
            public File getDataFile(String filename) {
                return new File(System.getProperty("java.io.tmpdir"));
            }

            @Override
            public Bundle[] getBundles() {
                return null;
            }

            @Override
            public Bundle getBundle(long id) {
                return null;
            }

            @Override
            public Bundle getBundle() {
                return null;
            }

            @Override
            public ServiceReference[] getAllServiceReferences(String clazz,
                    String filter) throws InvalidSyntaxException {
                return null;
            }

            @Override
            public Filter createFilter(String filter)
                    throws InvalidSyntaxException {
                return null;
            }

            @Override
            public void addServiceListener(ServiceListener listener,
                    String filter) throws InvalidSyntaxException {

            }

            @Override
            public void addServiceListener(ServiceListener listener) {
            }

            @Override
            public void addFrameworkListener(FrameworkListener listener) {
            }

            @Override
            public void addBundleListener(BundleListener listener) {
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
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
    }

    public ComponentInstance getComponentInstance() {
        return null;
    }

    public Dictionary<String, Object> getProperties() {
        return properties;
    }

    public ServiceReference getServiceReference() {
        return null;
    }

    public Bundle getUsingBundle() {
        return null;
    }

    public Object locateService(String name) {
        return null;
    }

    public Object locateService(String name, ServiceReference reference) {
        return null;
    }

    public Object[] locateServices(String name) {
        return null;
    }

}

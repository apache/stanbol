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
package org.apache.stanbol.ontologymanager.store.tdb;

import java.io.File;
import java.io.InputStream;
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

public class DummyComponentContext implements ComponentContext {

    private Dictionary<String,String> props = new Hashtable<String,String>();

    public DummyComponentContext() {}

    @Override
    public void disableComponent(String arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void enableComponent(String arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public BundleContext getBundleContext() {
        return new DummyBundleContext();
    }

    @Override
    public ComponentInstance getComponentInstance() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Dictionary getProperties() {
        return null;
    }

    @Override
    public ServiceReference getServiceReference() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Bundle getUsingBundle() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object locateService(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object locateService(String arg0, ServiceReference arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object[] locateServices(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    private class DummyBundleContext implements BundleContext {

        @Override
        public void addBundleListener(BundleListener listener) {
            // TODO Auto-generated method stub

        }

        @Override
        public void addFrameworkListener(FrameworkListener listener) {
            // TODO Auto-generated method stub

        }

        @Override
        public void addServiceListener(ServiceListener listener) {
            // TODO Auto-generated method stub

        }

        @Override
        public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException {
            // TODO Auto-generated method stub

        }

        @Override
        public Filter createFilter(String filter) throws InvalidSyntaxException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ServiceReference[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Bundle getBundle() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Bundle getBundle(long id) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Bundle[] getBundles() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public File getDataFile(String filename) {
            File file = new File("target/testDir");
            if (file.exists()) {
                file.delete();
            }
            file.mkdir();
            return file;

        }

        @Override
        public String getProperty(String key) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Object getService(ServiceReference reference) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ServiceReference getServiceReference(String clazz) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Bundle installBundle(String location) throws BundleException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Bundle installBundle(String location, InputStream input) throws BundleException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ServiceRegistration registerService(String[] clazzes, Object service, Dictionary properties) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ServiceRegistration registerService(String clazz, Object service, Dictionary properties) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void removeBundleListener(BundleListener listener) {
            // TODO Auto-generated method stub

        }

        @Override
        public void removeFrameworkListener(FrameworkListener listener) {
            // TODO Auto-generated method stub

        }

        @Override
        public void removeServiceListener(ServiceListener listener) {
            // TODO Auto-generated method stub

        }

        @Override
        public boolean ungetService(ServiceReference reference) {
            // TODO Auto-generated method stub
            return false;
        }

    }
}

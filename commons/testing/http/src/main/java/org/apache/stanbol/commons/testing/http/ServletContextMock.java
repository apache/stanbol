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

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.osgi.framework.BundleContext;

/**
 * Simple Mock class that can be used to mock the ServletContext in tests.
 * 
 * <p>
 * By default the attribute map contains a {@link org.apache.stanbol.commons.testing.http.BundleContextMock}
 * under the name "org.osgi.framework.BundleContext".
 * 
 * @author Fabian Christ
 */
public class ServletContextMock implements ServletContext {

    private Map<String,Object> attributeMap = new HashMap<String,Object>();

    public ServletContextMock() {
        this.attributeMap.put(BundleContext.class.getName(), new BundleContextMock());
    }

    public Object getAttribute(String name) {
        return this.attributeMap.get(name);
    }

    public Enumeration getAttributeNames() {
        return Collections.enumeration(this.attributeMap.keySet());
    }

    public void putAttribute(String name, Object value) {
        this.attributeMap.put(name, value);
    }

    public ServletContext getContext(String uripath) {
        throw new UnsupportedOperationException("not included in the mock implementation");
    }

    public String getInitParameter(String name) {
        throw new UnsupportedOperationException("not included in the mock implementation");
    }

    public Enumeration getInitParameterNames() {
        throw new UnsupportedOperationException("not included in the mock implementation");
    }

    public int getMajorVersion() {
        throw new UnsupportedOperationException("not included in the mock implementation");
    }

    public String getMimeType(String file) {
        throw new UnsupportedOperationException("not included in the mock implementation");
    }

    public int getMinorVersion() {
        throw new UnsupportedOperationException("not included in the mock implementation");
    }

    public RequestDispatcher getNamedDispatcher(String name) {
        throw new UnsupportedOperationException("not included in the mock implementation");
    }

    public String getRealPath(String path) {
        throw new UnsupportedOperationException("not included in the mock implementation");
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        throw new UnsupportedOperationException("not included in the mock implementation");
    }

    public URL getResource(String path) throws MalformedURLException {
        throw new UnsupportedOperationException("not included in the mock implementation");
    }

    public InputStream getResourceAsStream(String path) {
        throw new UnsupportedOperationException("not included in the mock implementation");
    }

    public Set getResourcePaths(String path) {
        throw new UnsupportedOperationException("not included in the mock implementation");
    }

    public String getServerInfo() {
        throw new UnsupportedOperationException("not included in the mock implementation");
    }

    public Servlet getServlet(String name) throws ServletException {
        throw new UnsupportedOperationException("not included in the mock implementation");
    }

    public String getServletContextName() {
        throw new UnsupportedOperationException("not included in the mock implementation");
    }

    public Enumeration getServletNames() {
        throw new UnsupportedOperationException("not included in the mock implementation");
    }

    public Enumeration getServlets() {
        throw new UnsupportedOperationException("not included in the mock implementation");
    }

    public void log(String msg) {
        throw new UnsupportedOperationException("not included in the mock implementation");
    }

    public void log(Exception exception, String msg) {
        throw new UnsupportedOperationException("not included in the mock implementation");
    }

    public void log(String message, Throwable throwable) {
        throw new UnsupportedOperationException("not included in the mock implementation");
    }

    public void removeAttribute(String name) {
        throw new UnsupportedOperationException("not included in the mock implementation");
    }

    public void setAttribute(String name, Object object) {
        this.putAttribute(name, object);
    }

    @Override
    public String getContextPath() {
        throw new UnsupportedOperationException("not included in the mock implementation");
    }

}

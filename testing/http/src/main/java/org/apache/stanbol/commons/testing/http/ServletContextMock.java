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
        // TODO Auto-generated method stub
        return null;
    }

    public String getInitParameter(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    public Enumeration getInitParameterNames() {
        // TODO Auto-generated method stub
        return null;
    }

    public int getMajorVersion() {
        // TODO Auto-generated method stub
        return 0;
    }

    public String getMimeType(String file) {
        // TODO Auto-generated method stub
        return null;
    }

    public int getMinorVersion() {
        // TODO Auto-generated method stub
        return 0;
    }

    public RequestDispatcher getNamedDispatcher(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    public String getRealPath(String path) {
        // TODO Auto-generated method stub
        return null;
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        // TODO Auto-generated method stub
        return null;
    }

    public URL getResource(String path) throws MalformedURLException {
        // TODO Auto-generated method stub
        return null;
    }

    public InputStream getResourceAsStream(String path) {
        // TODO Auto-generated method stub
        return null;
    }

    public Set getResourcePaths(String path) {
        // TODO Auto-generated method stub
        return null;
    }

    public String getServerInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    public Servlet getServlet(String name) throws ServletException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getServletContextName() {
        // TODO Auto-generated method stub
        return null;
    }

    public Enumeration getServletNames() {
        // TODO Auto-generated method stub
        return null;
    }

    public Enumeration getServlets() {
        // TODO Auto-generated method stub
        return null;
    }

    public void log(String msg) {
    // TODO Auto-generated method stub

    }

    public void log(Exception exception, String msg) {
    // TODO Auto-generated method stub

    }

    public void log(String message, Throwable throwable) {
    // TODO Auto-generated method stub

    }

    public void removeAttribute(String name) {
    // TODO Auto-generated method stub

    }

    public void setAttribute(String name, Object object) {
        this.putAttribute(name, object);
    }

}

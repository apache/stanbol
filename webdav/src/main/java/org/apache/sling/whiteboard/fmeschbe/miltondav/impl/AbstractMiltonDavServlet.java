/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.whiteboard.fmeschbe.miltondav.impl;

import javax.servlet.ServletException;
import com.bradmcevoy.http.MiltonServlet;

/**
 * The <code>AbstractMiltonDavServlet</code> is an abstract base
 * <code>MiltonServlet</code> used as the basis for the two Sling
 * implementations:
 * <dl>
 * <dt>{@link MiltonDavServlet}</dt>
 * <dd>A servlet registered directly with the OSGi Http Service to serve WebDAV
 * requests outside of Sling in its own URL space</dd>
 * <dt>{@link MiltonDavSlingServlet}</dt>
 * <dd>A servlet registered with Sling (using the whiteboard pattern) to provide
 * WebDAV services through the Sling request processing infrastructure</dd>
 * </dl>
 * <p>
 * This base class implementation only overwrites the
 * {@link #instantiate(String)} method to ensure using the bundle's class loader
 * and to not create a memory hole. The base class unfortunately uses the static
 * <code>Class.forName(String)</code> which is well-known for this problem.
 */
public class AbstractMiltonDavServlet extends MiltonServlet {

    @SuppressWarnings("unchecked")
    @Override
    protected <T> T instantiate(String className) throws ServletException {
    	//TODO have factories and handler be retrieved from service registry instead of
    	//getting class from classloader and instantiate
        try {
            Class<?> c = getClass().getClassLoader().loadClass(className);
            T rf = (T) c.newInstance();
            return rf;
        } catch (Throwable ex) {
            throw new ServletException("Failed to instantiate: " + className,
                ex);
        }
    }

}
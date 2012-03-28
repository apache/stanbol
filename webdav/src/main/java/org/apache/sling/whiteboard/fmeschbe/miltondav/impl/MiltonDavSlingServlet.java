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

import javax.servlet.Servlet;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.whiteboard.fmeschbe.miltondav.impl.resources.SlingResourceFactory;
import org.osgi.framework.Constants;

/**
 * The <code>MiltonDavSlingServlet</code> is a servlet based on Milton WebDAV
 * registering as a plain servlet to serve requests controlled by the Sling Main
 * Servlet.
 */
@Component(metatype = false)
@Service(value = Servlet.class)
@Properties({
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Sling WebDAV Servlet"),
    @Property(name = Constants.SERVICE_VENDOR, value = "The Apache Software Foundation"),

    // registering with Sling (default resource type, handling all methods)
    @Property(name = "sling.servlet.resourceTypes", value = "sling/servlet/default", propertyPrivate = true),
    @Property(name = "sling.servlet.methods", value = "*", propertyPrivate = true),

    // setup the helper classes for the MiltonServlet
    @Property(name = "resource.factory.class", value = SlingResourceFactory.NAME, propertyPrivate = true),
    @Property(name = "authentication.handler.classes", value = SlingAuthenticationHandler.NAME, propertyPrivate = true),
    @Property(name = "response.handler.class", value = SlingResponseHandler.NAME, propertyPrivate = true) })
public class MiltonDavSlingServlet extends AbstractMiltonDavServlet {

}
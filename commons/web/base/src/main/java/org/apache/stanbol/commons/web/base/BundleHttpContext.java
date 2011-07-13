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
package org.apache.stanbol.commons.web.base;

import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

/**
 * Custom HTTP Context to lookup the resources from the classloader of the OSGi bundle.
 */
public class BundleHttpContext implements HttpContext {

    private Bundle bundle;

    public BundleHttpContext(WebFragment fragment) {
        this.bundle = fragment.getBundleContext().getBundle();
    }

    public BundleHttpContext(Bundle bundle) {
        this.bundle = bundle;
    }

    public String getMimeType(String name) {
        // someone in the chain seems to already be doing the Mime type mapping
        return null;
    }

    public URL getResource(String name) {
        if (name.startsWith("/")) {
            name = name.substring(1);
        }

        return this.bundle.getResource(name);
    }

    public boolean handleSecurity(HttpServletRequest req, HttpServletResponse res) {
        return true;
    }

}

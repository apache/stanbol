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

import javax.servlet.ServletContext;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class ContextHelper {

    // TODO: turn the following into a annotation that derives from the JAX-RS @Context annotation
    /**
     * Fetch an OSGi service instance broadcasted into the OSGi context.
     * 
     * @param <T>
     *            the type of the service
     * @param clazz
     *            the class of the service
     * @param context
     *            the servlet context
     * @return the registered instance of the service (assuming cardinality 1)
     */
    @SuppressWarnings("unchecked")
    public static <T> T getServiceFromContext(Class<T> clazz, ServletContext context) {
        BundleContext bundleContext = getBundleContext(context);
        ServiceReference reference = bundleContext.getServiceReference(clazz.getName());
        //TODO: returning the service will cause the service reference not to be
        //  released bundleContext.ungetService(reference) will not be called!
        if(reference != null){
            return (T) bundleContext.getService(reference);
        } else {
            return null;
        }
    }
    /**
     * Fetches the BundleContext
     * @param context the {@link ServletContext}
     * @return the BundleContext or <code>null</code> if not registered under
     * <code>BundleContext.class.getName()</code>.
     */
    public static BundleContext getBundleContext(ServletContext context){
        return (BundleContext) context.getAttribute(BundleContext.class.getName());
    }

}

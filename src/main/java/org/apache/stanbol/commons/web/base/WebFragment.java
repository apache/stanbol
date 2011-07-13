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

import java.util.List;
import java.util.Set;

import org.osgi.framework.BundleContext;

import freemarker.cache.TemplateLoader;

/**
 * Interface to be implemented by bundles that want to customize the stanbol web interface and REST API by
 * contributing static resources, JAX-RS resources and Freemarker views.
 * 
 * TODO: add some kind of ordering information
 */
public interface WebFragment {

    /**
     * Name of the fragment. Should be a lowercase short name without any kind of special character, so as to
     * be used as a path component in the URL of the static resources.
     */
    String getName();

    /**
     * Java package name that is the classloading root of the static resources of the fragment to be published
     * by the OSGi HttpService under {@code /static-url-root/fragment-name/}
     * <p>
     * Note: this package should be exported by the bundle.
     */
    String getStaticResourceClassPath();

    /**
     * Set of JAX-RS resources provided as classes.
     * <p>
     * Note: those classes should be visible: use the Export-Package bundle declaration to export their
     * packages.
     */
    Set<Class<?>> getJaxrsResourceClasses();

    /**
     * Set of JAX-RS resources provided as singleton instances.
     * <p>
     * Note: those objects should be visible: use the Export-Package bundle declaration to export their
     * packages.
     */
    Set<Object> getJaxrsResourceSingletons();

    /**
     * @return a template load instance that can be used by the FreemarkerViewProcessor for building the HTML
     *         UI incrementally. If this is an instance of ClassTemplateLoader, the class path visibility
     *         should be exported using the Export-Package bundle declaration.
     */
    TemplateLoader getTemplateLoader();

    /**
     * CSS and favicon resources to be linked in the head of all HTML pages controlled by the NavigationMixin
     * abstract resource. The resources will be published under:
     * 
     * ${it.staticRootUrl}/${link.fragmentName}/${link.relativePath}
     */
    List<LinkResource> getLinkResources();

    /**
     * Javascript resources to be linked in the head of all HTML pages controlled by the NavigationMixin
     * abstract resource. The resources will be published under:
     * 
     * ${it.staticRootUrl}/${script.fragmentName}/${script.relativePath}
     */
    List<ScriptResource> getScriptResources();

    /**
     * List of link descriptions to contribute to the main navigation menu.
     */
    List<NavigationLink> getNavigationLinks();

    /**
     * @return the bundle context who contributed this fragment (useful for loading the resources from the
     *         right classloading context)
     */
    BundleContext getBundleContext();

}

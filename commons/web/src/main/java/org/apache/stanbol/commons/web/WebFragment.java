package org.apache.stanbol.commons.web;

import java.util.List;
import java.util.Set;

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
    public String getName();

    /**
     * Java package name that is the classloading root of the static resources of the fragment to be published
     * by the OSGi HttpService under /static-url-root/fragment-name/
     * 
     * Note: this package should be exported by the bundle.
     */
    public String getStaticResourceClassPath();

    /**
     * Set of JAX-RS resources provided as classes.
     * 
     * Note: those classes should be visible: use the Export-Package bundle declaration to export their
     * packages.
     */
    public Set<Class<?>> getJaxrsResourceClasses();

    /**
     * Set of JAX-RS resources provided as singleton instances.
     * 
     * Note: those objects should be visible: use the Export-Package bundle declaration to export their
     * packages.
     */
    public Set<Object> getJaxrsResourceSingletons();

    /**
     * @return a template load instance that can be used by the FreemarkerViewProcessor for building the HTML
     *         UI incrementally. If this is an instance of ClassTemplateLoader, the class path visibility
     *         should be exported using the Export-Package bundle declaration.
     */
    public TemplateLoader getTemplateLoader();

    /**
     * CSS and favicon resources to be linked in the head of all HTML pages controlled by the NavigationMixin
     * abstract resource. The resources will be published under:
     * 
     * ${it.staticRootUrl}/${link.fragmentName}/${link.relativePath}
     */
    public List<LinkResource> getLinkResources();

    /**
     * Javascript resources to be linked in the head of all HTML pages controlled by the NavigationMixin
     * abstract resource. The resources will be published under:
     * 
     * ${it.staticRootUrl}/${script.fragmentName}/${script.relativePath}
     */
    public List<ScriptResource> getScriptResources();

}

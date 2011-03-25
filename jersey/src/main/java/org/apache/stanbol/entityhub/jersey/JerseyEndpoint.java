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
package org.apache.stanbol.entityhub.jersey;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.spi.container.servlet.ServletContainer;

/**
 * Jersey-based RESTful endpoint for the Entityhub
 *
 * This OSGi component serves as a bridge between the OSGi context and the
 * Servlet context available to JAX-RS resources.
 *
 * NOTE: Original Code taken from the FISE
 * @author Rupert Westenthaler
 */

@Component(immediate = true, metatype = true)
@References(value={
       @Reference(
           name="entityhub",
           referenceInterface=Entityhub.class,
           policy=ReferencePolicy.DYNAMIC,
           bind="bindEntityhub",
           unbind="unbindEntityhub",
           cardinality=ReferenceCardinality.OPTIONAL_UNARY),
       @Reference(
           name="referencedSiteManager",
           referenceInterface=ReferencedSiteManager.class,
           policy=ReferencePolicy.DYNAMIC,
           bind="bindReferencedSiteManager",
           unbind="unbindReferencedSiteManager",
           cardinality=ReferenceCardinality.OPTIONAL_UNARY),
       @Reference(
           name="serializer",
           referenceInterface=Serializer.class,
           policy=ReferencePolicy.DYNAMIC,
           bind="bindSerializer",
           unbind="unbindSerializer",
           cardinality=ReferenceCardinality.OPTIONAL_UNARY)
       })
public class JerseyEndpoint {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property(value = "/entityhub")
    public static final String ALIAS_PROPERTY = "org.apache.stanbol.entityhub.jersey.alias";

    @Property(value = "/entityhub/static")
    public static final String STATIC_RESOURCES_URL_ROOT_PROPERTY = "org.apache.stanbol.entityhub.jersey.static.url";

    @Property(value = "/entityhub/META-INF/static")
    public static final String STATIC_RESOURCES_CLASSPATH_PROPERTY = "org.apache.stanbol.entityhub.jersey.static.classpath";

    //@Property(value = "/META-INF/templates")
    //public static final String FREEMARKER_TEMPLATE_CLASSPATH_PROPERTY = "org.apache.stanbol.entityhub.jersey.templates.classpath";

//    @Reference
//    private TcManager tcManager;

    /**
     * Dynamically adds the {@link Entityhub} to the {@link ServletContext}
     */
    protected void bindEntityhub(Entityhub entityhub){
        log.info("add "+entityhub.getClass().getSimpleName()+" to ServletContext");
        addManagedAttribute(Entityhub.class.getName(), entityhub);
    }
    /**
     * Dynamically removes the {@link Entityhub} to the {@link ServletContext}
     */
    protected void unbindEntityhub(Entityhub entityhub){
        log.info("remove"+entityhub.getClass().getSimpleName()+" from ServletContext");
        removeManagedAttribute(Entityhub.class.getName());
    }
    /**
     * Dynamically adds the {@link ReferencedSiteManager} to the {@link ServletContext}
     */
    protected void bindReferencedSiteManager(ReferencedSiteManager referencedSiteManager){
        log.info("add "+referencedSiteManager.getClass().getSimpleName()+" to ServletContext");
        addManagedAttribute(ReferencedSiteManager.class.getName(), referencedSiteManager);
    }
    /**
     * Dynamically removes the {@link ReferencedSiteManager} to the {@link ServletContext}
     */
    protected void unbindReferencedSiteManager(ReferencedSiteManager referencedSiteManager){
        log.info("remove "+referencedSiteManager.getClass().getSimpleName()+" from ServletContext");
        removeManagedAttribute(ReferencedSiteManager.class.getName());
    }

    /**
     * Dynamically adds the {@link Serializer} to the {@link ServletContext}
     */
    protected void bindSerializer(Serializer serializer){
        log.info("add "+serializer.getClass().getSimpleName()+" to ServletContext");
        addManagedAttribute(Serializer.class.getName(), serializer);
    }
    /**
     * Dynamically removes the {@link Serializer} to the {@link ServletContext}
     */
    protected void unbindSerializer(Serializer serializer){
        log.info("remove"+serializer.getClass().getSimpleName()+" from ServletContext");
        removeManagedAttribute(Serializer.class.getName());
    }

    /**
     * Internally manages the attributes currently added to the {@link ServletContext}.
     * This is necessary because Properties can bind/unbind even when this
     * Component is not activated (e.g. between construction and 
     * {@link #activate(ComponentContext)} is called). Therefore the
     * {@link #servletContext} might still be <code>null</code> 
     */
    private Map<String,Object> managedAttributes = new HashMap<String,Object>();

    /**
     * Adds a managed Attribute and to the {@link #servletContext} if not <code>null</code>
     * @param key the key
     * @param value the value
     * @throws IllegalArgumentException if the key is <code>null</code>
     */
    private void addManagedAttribute(String key, Object value) throws IllegalArgumentException {
        if(key == null){ //throw Exception to find bugs early!
            throw new IllegalArgumentException("The key for Managed Attributes MUST NOT be NULL");
        }
        //use local copy to avoid NullPointers in multi thread environments
        ServletContext servletContext = this.servletContext;
        synchronized (managedAttributes) {
            managedAttributes.put(key, value);
            if(servletContext != null){
                servletContext.setAttribute(key, value);
            }
        }
    }
    /**
     * Removes a managed Attribute and to the {@link #servletContext} if not <code>null</code>
     * @param key the key to remove
     * @throws IllegalArgumentException if the key is <code>null</code>
     */
    private void removeManagedAttribute(String key) throws IllegalArgumentException {
        if(key == null){ //throw Exception to find bugs early!
            throw new IllegalArgumentException("The key for Managed Attributes MUST NOT be NULL");
        }
        //use local copy to avoid NullPointers in multi thread environments
        ServletContext servletContext = this.servletContext;
        synchronized (managedAttributes) {
            if(managedAttributes.containsKey(key) && servletContext != null){
                servletContext.removeAttribute(key);
            }
            managedAttributes.remove(key);
        }
    }
    /**
     * Adds all currently managed Attributes to the parsed {@link ServletContext}.
     * Used in {@link #activate(ComponentContext)} to initialise the new
     * servlet context with previously added
     * managed attributes.
     * @param servletContext the context to add the managed attributes
     */
    private void addManagedAttributes(ServletContext servletContext){
        if(servletContext == null){
            return;
        }
        synchronized (managedAttributes) {
            for(Entry<String,Object> managedEntry: managedAttributes.entrySet()){
                servletContext.setAttribute(managedEntry.getKey(), managedEntry.getValue());
            }
        }
    }
    /**
     * Removes all currently managed Attributes from the parsed {@link ServletContext}.
     * Used in {@link #deactivate(ComponentContext)} to reset the servlet context.
     * @param servletContext the context to add the managed attributes
     */
    private void removeManagedAttributes(ServletContext servletContext){
        if(servletContext == null){
            return;
        }
        synchronized (managedAttributes) {
            for(Entry<String,Object> managedEntry: managedAttributes.entrySet()){
                servletContext.removeAttribute(managedEntry.getKey());
            }
        }
    }
    
    @Reference
    private HttpService httpService;


    protected ServletContext servletContext;

    public Dictionary<String, String> getInitParams() {
        // pass configuration for Jersey resource
        // TODO: make the list of enabled JAX-RS resources and providers
        // configurable using an OSGi service
        Dictionary<String, String> initParams = new Hashtable<String, String>();
        initParams.put("javax.ws.rs.Application", JerseyEndpointApplication.class.getName());

        // make jersey automatically turn resources into Viewable models and
        // hence lookup matching freemarker templates
        //initParams.put("com.sun.jersey.config.feature.ImplicitViewables","true");
        return initParams;
    }

    protected void activate(ComponentContext ctx) throws IOException,
            ServletException, NamespaceException {
        log.info("activate "+JerseyEndpoint.class+" ...");
        // register the JAX-RS resources as a servlet under configurable alias
        ServletContainer container = new ServletContainer();
        String alias = (String) ctx.getProperties().get(ALIAS_PROPERTY);
        String staticUrlRoot = (String) ctx.getProperties().get(
                STATIC_RESOURCES_URL_ROOT_PROPERTY);
        String staticClasspath = (String) ctx.getProperties().get(
                STATIC_RESOURCES_CLASSPATH_PROPERTY);
        //String freemakerTemplates = (String) ctx.getProperties().get(
        //        FREEMARKER_TEMPLATE_CLASSPATH_PROPERTY);

        log.info("Registering servlets with HTTP service "
                + httpService.toString());
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            httpService.registerServlet(alias, container, getInitParams(), null);
            httpService.registerResources(staticUrlRoot, staticClasspath, null);
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }

        
        // This is now done dynamically based on there activation/deactivation
        servletContext = container.getServletContext();
        servletContext.setAttribute(BundleContext.class.getName(),
                ctx.getBundleContext());
        servletContext.setAttribute(STATIC_RESOURCES_URL_ROOT_PROPERTY,
                staticUrlRoot);
        // References (such as entityhub and ReferencedSiteManager) are no 
        // longer statically forwarded to JAX-RS components.
        //But still we need to add all managed attributes that where added
        //prior to activating Jersey
        addManagedAttributes(servletContext);
        log.info("Jersey servlet registered at {}", alias);
    }

    protected void deactivate(ComponentContext ctx) {
        log.info("Deactivating jersey bundle");
        String alias = (String) ctx.getProperties().get(ALIAS_PROPERTY);
        httpService.unregister(alias);
        //clean up managed Attributes 
        // ... just to make sure that they are no longer referenced by this
        //     object even if an other component keep the context!
        removeManagedAttributes(servletContext);
        servletContext = null;
    }

}

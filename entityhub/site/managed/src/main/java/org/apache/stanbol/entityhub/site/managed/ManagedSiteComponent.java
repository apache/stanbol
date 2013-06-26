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
package org.apache.stanbol.entityhub.site.managed;

import static org.apache.stanbol.entityhub.servicesapi.site.Site.PROHIBITED_SITE_IDS;
import static org.apache.stanbol.entityhub.servicesapi.site.SiteConfiguration.DESCRIPTION;
import static org.apache.stanbol.entityhub.servicesapi.site.SiteConfiguration.ENTITY_PREFIX;
import static org.apache.stanbol.entityhub.servicesapi.site.SiteConfiguration.ID;
import static org.apache.stanbol.entityhub.servicesapi.site.SiteConfiguration.NAME;
import static org.apache.stanbol.entityhub.servicesapi.site.SiteConfiguration.SITE_FIELD_MAPPINGS;

import java.util.Dictionary;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.core.utils.SiteUtils;
import org.apache.stanbol.entityhub.servicesapi.site.ManagedSite;
import org.apache.stanbol.entityhub.servicesapi.site.ManagedSiteConfiguration;
import org.apache.stanbol.entityhub.servicesapi.site.Site;
import org.apache.stanbol.entityhub.servicesapi.site.SiteConfiguration;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.apache.stanbol.entityhub.site.managed.impl.YardSite;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link ManagedSite} component. This allows to configure a
 * {@link ManagedSite} on top of an {@link Yard} service referenced by its
 * {@link Yard#ID}. The required {@link Yard} is tracked using {@link ServiceTracker}.
 * If the {@link Yard} service becomes available the {@link ManagedSite} will
 * be initialised and registered as {@link BundleContext#registerService(String, Object, Dictionary) OSGI service}.
 * As soon as the Yard goes away also the {@link ManagedSite} is unregistered.<p>
 * This {@link ManagedSiteComponent} will be active even if the {@link ManagedSite}
 * is not registered. This allows to decouples the lifecycle of the {@link ManagedSite}
 * with the one of the configuration.<p>
 */
@Component(
    name="org.apache.stanbol.entityhub.site.managed.YardSite",
    configurationFactory=true,
    policy=ConfigurationPolicy.REQUIRE, //the baseUri is required!
    specVersion="1.1",
    metatype = true,
    immediate = true
    )
@Properties(value={
    @Property(name=ID,value="changeme"),
    @Property(name=NAME,value="change me"),
    @Property(name=DESCRIPTION,value="optional description"),
    @Property(name=ENTITY_PREFIX, cardinality=1000),
    @Property(name=ManagedSiteConfiguration.YARD_ID,value="yardId"),
    @Property(name=SITE_FIELD_MAPPINGS,cardinality=1000,
        value={"skos:prefLabel > rdfs:label",
               "skos:altLabel > rdfs:label",
               "foaf:name > rdfs:label",
               "dc:title > rdfs:label",
               "dc-elements:title > rdfs:label",
               "schema:name > rdfs:label"})
    })
public class ManagedSiteComponent {

    private static final Logger log = LoggerFactory.getLogger(ManagedSiteComponent.class);
    
    private ManagedSiteConfiguration siteConfiguration;

    private final Object yardReferenceLock = new Object();
    
    private ServiceReference yardReference;

    private ServiceTracker yardTracker;

    private YardSite managedSite;

    private BundleContext bundleContext;

    private ServiceRegistration managedSiteRegistration;
    
    @Reference(cardinality=ReferenceCardinality.OPTIONAL_UNARY)
    protected NamespacePrefixService nsPrefixService;
    /**
     * Activates this {@link ManagedSiteComponent}. This might be overridden to
     * perform additional configuration. In such cases super should be called
     * before the additional configuration steps.
     * @param context
     * @throws ConfigurationException
     * @throws YardException
     * @throws InvalidSyntaxException
     */
    @Activate
    protected void activate(final ComponentContext context) throws ConfigurationException, YardException, InvalidSyntaxException {
        this.bundleContext = context.getBundleContext();
        //NOTE that the constructor also validation of the parsed configuration
        this.siteConfiguration = new ManagedSiteConfigurationImpl(context.getProperties());
        if(PROHIBITED_SITE_IDS.contains(siteConfiguration.getId().toLowerCase())){
            throw new ConfigurationException(SiteConfiguration.ID, String.format(
                "The ID '%s' of this Referenced Site is one of the following " +
                "prohibited IDs: {} (case insensitive)",siteConfiguration.getId(),
                PROHIBITED_SITE_IDS));
        }
        log.info(" > initialise Managed Site {}",siteConfiguration.getId());
        SiteUtils.extractSiteMetadata(siteConfiguration, InMemoryValueFactory.getInstance());
        //Initialise the Yard
        final String yardId = siteConfiguration.getYardId();
        String yardFilterString = String.format("(&(%s=%s)(%s=%s))", 
            Constants.OBJECTCLASS,Yard.class.getName(),
            Yard.ID,yardId);
        Filter yardFilter = bundleContext.createFilter(yardFilterString);
        
        yardTracker = new ServiceTracker(
            bundleContext, yardFilter, new ServiceTrackerCustomizer() {
                
                @Override
                public void removedService(ServiceReference reference, Object service) {
                    synchronized (yardReferenceLock) {
                        if(reference.equals(yardReference)){
                            deactivateManagedSite();
                            yardReference = null;
                            
                        }
                        bundleContext.ungetService(reference);
                    }
                }
                
                @Override
                public void modifiedService(ServiceReference reference, Object service) {
                }
                
                @Override
                public Object addingService(ServiceReference reference) {
                    Yard yard = (Yard)bundleContext.getService(reference);
                    synchronized (yardReferenceLock) {
                        if(yardReference == null){
                            if(yard != null){
                                activateManagedSite(yard);
                                yardReference = reference;
                            } else {
                                log.warn("Unable to addService for ServiceReference because"
                                    + "unable to obtain referenced Yard via the BundleContext!");
                            }
                        } else {
                            log.warn("Tracking two Yard instances with the Yard ID '{}' "
                                + "configured for ManagedSite '{}'", yardId, siteConfiguration.getId());
                            log.warn("used  : {}",yardReference.getProperty(Constants.SERVICE_PID));
                            log.warn("unused: {}",reference.getProperty(Constants.SERVICE_PID));
                        }
                    }
                    return yard;
                }
            });
        yardTracker.open();
        //will be moved to a Solr specific implementation
//        //chaeck if we are allowed to init an yard with the provided id
//        boolean allowInit = false;
//        if(configAdmin!= null){
//            Configuration[] configs;
//            try {
//                String yardIdFilter = String.format("(%s=%s)",
//                    Yard.ID,yardId);
//                configs = configAdmin.listConfigurations(yardIdFilter);
//                if(configs == null || configs.length < 1){
//                    allowInit = true;
//                }
//            } catch (IOException e) {
//                log.warn("Unable to access ManagedService configurations ",e);
//            }
//        } else if (yardTracker.getService() == null){
//            log.warn("Unable to check for Yard configuration of ManagedSite {} "
//                + "Because the ConfigurationAdmin service is not available");
//            log.warn(" -> unable to create YardConfiguration");
//        }
//        if(allowInit){
//            //TODO: This has SolrYard specific code - this needs to be refactored
//            String factoryPid = "org.apache.stanbol.entityhub.yard.solr.impl.SolrYard";
//            try {
//                Configuration config = configAdmin.createFactoryConfiguration(factoryPid,null);
//                //configure the required properties
//                Dictionary<String,Object> yardConfig = new Hashtable<String,Object>();
//                yardConfig.put(Yard.ID, siteConfiguration.getYardId());
//                yardConfig.put(Yard.NAME, siteConfiguration.getYardId());
//                yardConfig.put(Yard.DESCRIPTION, "Yard for the ManagedSite "+siteConfiguration.getId());
//                yardConfig.put("org.apache.stanbol.entityhub.yard.solr.solrUri", siteConfiguration.getId());
//                yardConfig.put("org.apache.stanbol.entityhub.yard.solr.useDefaultConfig", true);
//                config.update(yardConfig); //this create the solrYard
//            } catch (IOException e) {
//                log.warn("Unable to create SolrYard configuration for MnagedSite "+siteConfiguration.getId(),e);
//            }
//        }

    }
    /**
     * Deactivates the component and unregisters the {@link ManagedSite} service
     * if currently active.<p>
     * If overridden calls to super typically should be made after deactivating
     * specific things of the sub-class.
     * @param context
     */
    @Deactivate
    protected void deactivate(BundleContext context){
        if(yardTracker != null){
            yardTracker.close();
            yardTracker = null;
        }
        synchronized (yardReferenceLock) {
            yardReference = null;
        }
        deactivateManagedSite();
    }
    
    protected void activateManagedSite(Yard yard) {
        managedSite = new YardSite(yard,siteConfiguration, nsPrefixService);
        managedSiteRegistration = bundleContext.registerService(
            new String[]{ManagedSite.class.getName(), Site.class.getName()},
            managedSite, siteConfiguration.getConfiguration());
    }

    protected void deactivateManagedSite() {
        managedSiteRegistration.unregister();
        managedSite.close();
        managedSite = null;
        
    }

}

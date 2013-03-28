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
package org.apache.stanbol.entityhub.core.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.entityhub.core.utils.OsgiUtils;
import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.EntityhubConfiguration;
import org.apache.stanbol.entityhub.servicesapi.model.ManagedEntityState;
import org.apache.stanbol.entityhub.servicesapi.model.MappingState;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
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
 * Implementation of the Entityhub Configuration that consumes the configuration
 * form OSGI. This replaces the old EntityhubConfigurationImpl (up to 0.11.0)
 * @since 0.12.0
 * @author Rupert Westenthaler
 *
 */
@Component(immediate=true,metatype=true,
        name = "org.apache.stanbol.entityhub.core.impl.EntityhubConfigurationImpl")
public class EntityhubComponent implements EntityhubConfiguration {

    private final Logger log = LoggerFactory.getLogger(EntityhubComponent.class);

    @Property(name=EntityhubConfiguration.ID,value="entityhub")
    private String entityhubID;
    @Property(name=EntityhubConfiguration.NAME,value="<organisations> Entityhub")
    private String entityhubName;
    @Property(name=EntityhubConfiguration.DESCRIPTION,value="The entityhub holding all entities of <organisation>")
    private String entityhubDescription;
    @Property(name=EntityhubConfiguration.PREFIX,value="urn:org.apache.stanbol:entityhub:")
    private String entityhubPrefix;
    @Property(name=EntityhubConfiguration.ENTITYHUB_YARD_ID,value=EntityhubConfiguration.DEFAULT_ENTITYHUB_YARD_ID)
    private String entityhubYardId;
    @Property(
            name=EntityhubConfiguration.FIELD_MAPPINGS,
            value ={ //This is the default config for well known Ontologies
                // --- Define the Languages for all fields ---
                //NOTE: the leading space is required for the global filter!
                " | @=null;en;de;fr;it", //will filter all labels with other languages
                // --- RDF, RDFS and OWL Mappings ---
                "rdfs:label", //rdf:label
                "rdfs:label > entityhub:label",
                "rdfs:comment",//rdf:comment
                "rdfs:comment > entityhub:description",
                "rdf:type | d=entityhub:ref",//The types
                "owl:sameAs | d=entityhub:ref",//used by LOD to link to URIs used to identify the same Entity
                // --- Dublin Core ---
                "dc:*", //all DC Terms properties
                "dc:title > entityhub:label",
                "dc:description > entityhub:description",
                "dc-elements:*", //all DC Elements (one could also define the mappings to the DC Terms counterparts here
                "dc-elements:title > entityhub:label",
                "dc-elements:description > entityhub:description",
                // --- Spatial Things ---
                "geo:lat | d=xsd:double",
                "geo:long | d=xsd:double",
                "geo:alt | d=xsd:int;xsd:float", //also allow floating point if one needs to use fractions of meters
                // --- Thesaurus (via SKOS) ---
                //SKOS can be used to define hierarchical terminologies
                "skos:*",
                "skos:prefLabel  > entityhub:label",
                "skos:definition > entityhub:description",
                "skos:note > entityhub:description",
                "skos:broader | d=entityhub:ref",
                "skos:narrower | d=entityhub:ref",
                "skos:related | d=entityhub:ref",
//                "skos:member | d=entityhub:ref",
                "skos:subject | d=entityhub:ref",
                "skos:inScheme | d=entityhub:ref",
//                "skos:hasTopConcept | d=entityhub:ref",
//                "skos:topConceptOf | d=entityhub:ref",
                // --- Social Networks (via foaf) ---
                "foaf:*", //The Friend of a Friend schema often used to describe social relations between people
                "foaf:name > entityhub:label",
//                "foaf:knows | d=entityhub:ref",
//                "foaf:made | d=entityhub:ref",
//                "foaf:maker | d=entityhub:ref",
//                "foaf:member | d=entityhub:ref",
                "foaf:homepage | d=xsd:anyURI",
                "foaf:depiction | d=xsd:anyURI",
                "foaf:img | d=xsd:anyURI",
                "foaf:logo | d=xsd:anyURI",
                "foaf:page | d=xsd:anyURI" //page about the entity
            })
    private String[] fieldMappingConfig;
    //NOTE: there is no other way than hard coding the names there!
    @Property(name=EntityhubConfiguration.DEFAULT_MAPPING_STATE,options={
            @PropertyOption( //seems, that name and value are exchanged ...
                    value='%'+EntityhubConfiguration.DEFAULT_MAPPING_STATE+".option.proposed",
                    name="proposed"),
            @PropertyOption(
                    value='%'+EntityhubConfiguration.DEFAULT_MAPPING_STATE+".option.confirmed",
                    name="confirmed")
            },value="proposed")
    private String defaultMappingStateString;
    @Property(name=EntityhubConfiguration.DEFAULT_SYMBOL_STATE,options={
            @PropertyOption( //seems, that name and value are exchanged ...
                    value='%'+EntityhubConfiguration.DEFAULT_SYMBOL_STATE+".option.proposed",
                    name="proposed"),
            @PropertyOption(
                    value='%'+EntityhubConfiguration.DEFAULT_SYMBOL_STATE+".option.active",
                    name="active")
            },value="proposed")
    private String defaultSymblStateString;

    private BundleContext bc;
    
    /**
     * Tracks the availability of the Yard used by the Entityhub.
     */
    private ServiceTracker entityhubYardTracker; //reference initialised in the activate method
    private Yard entityhubYard;
    
    private ServiceRegistration entityhubRegistration;
    private Entityhub entityhub;
    
    /**
     * The site manager is used to search for entities within the Entityhub framework
     */
    @Reference // 1..1, static
    private SiteManager siteManager;
    
    

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY,
            policy = ReferencePolicy.DYNAMIC,
            bind = "bindNamespacePrefixService",
            unbind = "unbindNamespacePrefixService",
            strategy = ReferenceStrategy.EVENT)
    private NamespacePrefixService nsPrefixService;
    
    
    protected void bindNamespacePrefixService(NamespacePrefixService ps){
        this.nsPrefixService = ps;
        updateServiceRegistration(bc, entityhubYard, siteManager, nsPrefixService);
    }
    

    protected void unbindNamespacePrefixService(NamespacePrefixService ps){
        if(ps.equals(this.nsPrefixService)){
            this.nsPrefixService = null;
            updateServiceRegistration(bc, entityhubYard, siteManager, nsPrefixService);
        }
    }

    
    @Activate
    protected void activate(final ComponentContext context) throws ConfigurationException {
        this.bc = context.getBundleContext();
        Dictionary<?, ?> properties = context.getProperties();
        log.info("Activate Entityhub Component:");

        
        this.entityhubID = OsgiUtils.checkProperty(properties, ID).toString();
        if(entityhubID == null || entityhubID.isEmpty()){
            throw new ConfigurationException(ID, "The id for the Entityhub MUST NOT be empty!");
        } else {
            log.debug("   + id: {}", entityhubID);
        }
        this.entityhubName = OsgiUtils.checkProperty(properties, NAME, this.entityhubID).toString();
        if(entityhubName.isEmpty()){
            throw new ConfigurationException(NAME, "The name for the Entityhub MUST NOT be empty!");
        } else {
            log.debug("   + name: {}",entityhubName);
        }
        Object entityhubDescriptionObject = properties.get(DESCRIPTION);
        this.entityhubDescription = entityhubDescriptionObject==null ? null : entityhubDescriptionObject.toString();
        log.debug("   + description: {}",entityhubDescription == null ? "<none>" : entityhubDescription);
        
        this.entityhubPrefix = OsgiUtils.checkProperty(properties, PREFIX).toString();
        if(entityhubPrefix.isEmpty()){
            throw new ConfigurationException(PREFIX, "The UIR preix for the Entityub MUST NOT be empty!");
        }
        try {
            new URI(entityhubPrefix);
            log.info("   + prefix: "+entityhubPrefix);
        } catch (URISyntaxException e) {
            throw new ConfigurationException(PREFIX, "The URI prefix for the Entityhub "
                + "MUST BE an valid URI (prefix="+entityhubPrefix+")",e);
        }
        
        Object defaultSymbolState = properties.get(DEFAULT_SYMBOL_STATE);
        if(defaultSymbolState == null){
            this.defaultSymblStateString = ManagedEntity.DEFAULT_SYMBOL_STATE.name();
        } else {
            this.defaultSymblStateString = defaultSymbolState.toString();
        }
        Object defaultMappingState = properties.get(DEFAULT_MAPPING_STATE);
        if(defaultMappingState == null){
            this.defaultMappingStateString = EntityMapping.DEFAULT_MAPPING_STATE.name();
        } else {
            this.defaultMappingStateString = defaultMappingState.toString();
        }
        Object fieldMappingConfigObject = OsgiUtils.checkProperty(properties, FIELD_MAPPINGS);
        if(fieldMappingConfigObject instanceof String[]){
            this.fieldMappingConfig = (String[])fieldMappingConfigObject;
        } else {
            throw new ConfigurationException(FIELD_MAPPINGS, "Values for this property must be of type Stirng[]!");
        }
        String entityhubYardId = OsgiUtils.checkProperty(properties, ENTITYHUB_YARD_ID).toString();
        String filterString = String.format("(&(%s=%s)(%s=%s))",
            Constants.OBJECTCLASS,Yard.class.getName(),
            Yard.ID,entityhubYardId);
        log.debug(" ... tracking EntityhubYard by Filter:"+filterString);
        Filter filter;
        try {
            filter = context.getBundleContext().createFilter(filterString);
        } catch (InvalidSyntaxException e) {
            throw new ConfigurationException(ENTITYHUB_YARD_ID, "Unable to parse OSGI filter '"
                + filterString + "' for configured Yard id '"+entityhubYardId+"'!",e);
        }
        entityhubYardTracker = new ServiceTracker(context.getBundleContext(), filter, 
            new ServiceTrackerCustomizer() {
                final BundleContext bc = context.getBundleContext();
                @Override
                public void removedService(ServiceReference reference, Object service) {
                    if(service.equals(entityhubYard)){
                        entityhubYard = (Yard)entityhubYardTracker.getService();
                        updateServiceRegistration(bc, entityhubYard, siteManager, nsPrefixService);
                    }
                    bc.ungetService(reference);
                }
                
                @Override
                public void modifiedService(ServiceReference reference, Object service) {
                    //the service.ranking might have changed ... so check if the
                    //top ranked yard is a different one
                    Yard newYard = (Yard)entityhubYardTracker.getService();
                    if(newYard == null || !newYard.equals(entityhubYard)){
                        entityhubYard = newYard; //set the new yard
                        //and update the service registration
                        updateServiceRegistration(bc, entityhubYard, siteManager, nsPrefixService);
                    }
                }
                
                @Override
                public Object addingService(ServiceReference reference) {
                    Object service = bc.getService(reference);
                    if(service != null){
                        if(entityhubYardTracker.getServiceReference() == null || //the first added Service or
                                //the new service as higher ranking as the current
                                (reference.compareTo(entityhubYardTracker.getServiceReference()) > 0)){
                            entityhubYard = (Yard)service;
                            updateServiceRegistration(bc, entityhubYard, siteManager, nsPrefixService);
                        } // else the new service has lower ranking as the currently use one
                    } //else service == null -> ignore
                    return service;
                }
            });
        entityhubYardTracker.open(); //start the tracking

    }
    
    private synchronized void updateServiceRegistration(BundleContext bc, Yard entityhubYard,
            SiteManager siteManager, NamespacePrefixService nsPrefixService) {
        
        if(entityhubRegistration != null){
            entityhubRegistration.unregister();
            entityhubRegistration = null;
            entityhub = null;
        }
        
        if(bc != null && entityhubYard != null && siteManager != null){
            entityhub = new EntityhubImpl(entityhubYard,siteManager, this, nsPrefixService);
            entityhubRegistration = bc.registerService(Entityhub.class.getName(), entityhub, 
                new Hashtable<String,Object>());
        }
        
    }

    
    @Override
    public String getID() {
        return entityhubID;
    }

    @Override
    public String getEntityhubYardId() {
        return entityhubYardId;
    }

    @Override
    public String getDescription() {
        return entityhubDescription;
    }

    @Override
    public String getEntityhubPrefix() {
        return entityhubPrefix;
    }

    @Override
    public String getName() {
        return entityhubName;
    }

    @Override
    public Collection<String> getFieldMappingConfig() {
        return Arrays.asList(fieldMappingConfig);
    }

    @Override
    public MappingState getDefaultMappingState() {
        try {
            return MappingState.valueOf(defaultMappingStateString);
        } catch (IllegalArgumentException e) {
            log.warn("The value \""+defaultMappingStateString+"\" configured as default MappingState does not match any value of the Enumeration! " +
                    "Return the default state as defined by the "+EntityMapping.class+".");
            return EntityMapping.DEFAULT_MAPPING_STATE;
        }
    }

    @Override
    public ManagedEntityState getDefaultManagedEntityState() {
        try {
            return ManagedEntityState.valueOf(defaultSymblStateString);
        } catch (IllegalArgumentException e) {
            log.warn("The value \""+defaultSymblStateString+"\" configured as default SymbolState does not match any value of the Enumeration! " +
                    "Return the default state as defined by the "+ManagedEntity.class+".");
            return ManagedEntity.DEFAULT_SYMBOL_STATE;
        }
    }
}

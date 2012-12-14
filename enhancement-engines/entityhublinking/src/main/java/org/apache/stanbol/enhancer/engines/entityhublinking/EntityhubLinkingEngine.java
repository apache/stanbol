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
package org.apache.stanbol.enhancer.engines.entityhublinking;

import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.CASE_SENSITIVE;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.DEFAULT_CASE_SENSITIVE_MATCHING_STATE;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.DEFAULT_DEREFERENCE_ENTITIES_STATE;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.DEFAULT_MATCHING_LANGUAGE;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.DEFAULT_MIN_SEARCH_TOKEN_LENGTH;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.DEFAULT_MIN_TOKEN_SCORE;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.DEFAULT_SUGGESTIONS;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.DEREFERENCE_ENTITIES;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.DEREFERENCE_ENTITIES_FIELDS;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.MIN_SEARCH_TOKEN_LENGTH;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.MIN_TOKEN_SCORE;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.NAME_FIELD;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.REDIRECT_FIELD;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.REDIRECT_MODE;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.SUGGESTIONS;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.TYPE_FIELD;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.TYPE_MAPPINGS;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig.DEFAULT_PROCESS_ONLY_PROPER_NOUNS_STATE;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig.PROCESSED_LANGUAGES;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig.PROCESS_ONLY_PROPER_NOUNS_STATE;
import static org.apache.stanbol.enhancer.servicesapi.EnhancementEngine.PROPERTY_NAME;
import static org.osgi.framework.Constants.SERVICE_RANKING;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.enhancer.engines.entitylinking.LabelTokenizer;
import org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.engine.EntityLinkingEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
/**
 * The EntityhubLinkingEngine in NOT an {@link EnhancementEngine} but only an
 * OSGI {@link Component} that allows to configure instances of the
 * {@link EntityLinkingEngine} using an {@link ReferencedSiteSearcher} or
 * {@link EntityhubSearcher} to link entities.
 * @author Rupert Westenthaler
 *
 */
@Component(
    configurationFactory = true, 
    policy = ConfigurationPolicy.REQUIRE, // the baseUri is required!
    specVersion = "1.1", 
    metatype = true, 
    immediate = true,
    inherit = true)
@org.apache.felix.scr.annotations.Properties(value={
    @Property(name=PROPERTY_NAME),
    @Property(name=EntityhubLinkingEngine.SITE_ID),
    @Property(name=NAME_FIELD,value="rdfs:label"),
    @Property(name=CASE_SENSITIVE,boolValue=DEFAULT_CASE_SENSITIVE_MATCHING_STATE),
    @Property(name=TYPE_FIELD,value="rdf:type"),
    @Property(name=REDIRECT_FIELD,value="rdfs:seeAlso"),
    @Property(name=REDIRECT_MODE,options={
        @PropertyOption(
            value='%'+REDIRECT_MODE+".option.ignore",
            name="IGNORE"),
        @PropertyOption(
            value='%'+REDIRECT_MODE+".option.addValues",
            name="ADD_VALUES"),
        @PropertyOption(
                value='%'+REDIRECT_MODE+".option.follow",
                name="FOLLOW")
        },value="IGNORE"),
    @Property(name=MIN_SEARCH_TOKEN_LENGTH, intValue=DEFAULT_MIN_SEARCH_TOKEN_LENGTH),
    @Property(name=MIN_TOKEN_SCORE,floatValue=DEFAULT_MIN_TOKEN_SCORE),
    @Property(name=SUGGESTIONS, intValue=DEFAULT_SUGGESTIONS),
    @Property(name=PROCESS_ONLY_PROPER_NOUNS_STATE, boolValue=DEFAULT_PROCESS_ONLY_PROPER_NOUNS_STATE),
    @Property(name=PROCESSED_LANGUAGES,
        cardinality=Integer.MAX_VALUE,
        value={"*;lmmtip;uc=LINK;prop=0.75;pprob=0.75", // link multiple matchable tokens in chunks; link upper case words
               "de;uc=MATCH", //in German all Nouns are upper case
               "es;lc=Noun", //the OpenNLP POS tagger for Spanish does not support ProperNouns
               "nl;lc=Noun"}), //same for Dutch 
    @Property(name=DEFAULT_MATCHING_LANGUAGE,value=""),
    @Property(name=TYPE_MAPPINGS,cardinality=Integer.MAX_VALUE),
    @Property(name=DEREFERENCE_ENTITIES, boolValue=DEFAULT_DEREFERENCE_ENTITIES_STATE),
    @Property(name=DEREFERENCE_ENTITIES_FIELDS,cardinality=Integer.MAX_VALUE,
    	value={"http://www.w3.org/2000/01/rdf-schema#comment",
            "http://www.w3.org/2003/01/geo/wgs84_pos#lat",
            "http://www.w3.org/2003/01/geo/wgs84_pos#long",
            "http://xmlns.com/foaf/0.1/depiction",
            "http://dbpedia.org/ontology/thumbnail"}),
    @Property(name=SERVICE_RANKING,intValue=0)
})
public class EntityhubLinkingEngine implements ServiceTrackerCustomizer {

    //private final Logger log = LoggerFactory.getLogger(EntityhubLinkingEngine.class);

    @Reference
    NamespacePrefixService prefixService;
    
    /**
     * The id of the Entityhub Site (Referenced or Managed Site) used for matching. <p>
     * To match against the Entityhub use "entityhub" as value.
     */
    public static final String SITE_ID = "enhancer.engines.linking.entityhub.siteId";

    /**
     * The engine initialised based on the configuration of this component
     */
    protected EntityLinkingEngine entityLinkingEngine;
    protected Dictionary<String,Object> engineMetadata;
    /**
     * The service registration for the {@link #entityLinkingEngine}
     */
    protected ServiceRegistration engineRegistration;
    /**
     * The EntitySearcher used for the {@link #entityLinkingEngine}
     */
    private TrackingEntitySearcher<?> entitySearcher;
    int trackedServiceCount = 0;
    
    /**
     * the MainLabelTokenizer
     */
    @Reference
    protected LabelTokenizer labelTokenizer;
    
    
    /**
     * The name of the reference site ('local' or 'entityhub') if the
     * Entityhub is used for enhancing
     */
    protected String siteName;

    private BundleContext bundleContext;

    /**
     * Default constructor as used by OSGI. This expects that 
     * {@link #activate(ComponentContext)} is called before usage
     */
    public EntityhubLinkingEngine() {
    }

    @Activate
    @SuppressWarnings("unchecked")
    protected void activate(ComponentContext ctx) throws ConfigurationException {
        Dictionary<String,Object> properties = ctx.getProperties();
        bundleContext = ctx.getBundleContext();
        EntityLinkerConfig linkerConfig = EntityLinkerConfig.createInstance(properties,prefixService);
        TextProcessingConfig textProcessingConfig = TextProcessingConfig.createInstance(properties);
        Object value = properties.get(SITE_ID);
        //init the EntitySource
        if (value == null) {
            throw new ConfigurationException(SITE_ID,
                    "The ID of the Referenced Site is a required Parameter and MUST NOT be NULL!");
        }
        siteName = value.toString();
        if (siteName.isEmpty()) {
            throw new ConfigurationException(SITE_ID,
                    "The ID of the Referenced Site is a required Parameter and MUST NOT be an empty String!");
        }
        //get the metadata later set to the enhancement engine
        String engineName;
        engineMetadata = new Hashtable<String,Object>();
        value = properties.get(PROPERTY_NAME);
        if(value == null || value.toString().isEmpty()){
            throw new ConfigurationException(PROPERTY_NAME, "The EnhancementEngine name MUST BE configured!");
        } else {
            engineName = value.toString();
        }
        engineMetadata.put(PROPERTY_NAME, value);
        value = properties.get(Constants.SERVICE_RANKING);
        engineMetadata.put(Constants.SERVICE_RANKING, value == null ? Integer.valueOf(0) : value);
        
        //init the tracking entity searcher
        trackedServiceCount = 0;
        if(Entityhub.ENTITYHUB_IDS.contains(siteName.toLowerCase())){
            entitySearcher = new EntityhubSearcher(bundleContext, 10, this);
        } else {
            entitySearcher = new ReferencedSiteSearcher(bundleContext,siteName,10,this);
        }
        //create the engine
        entityLinkingEngine = new EntityLinkingEngine(engineName,
            entitySearcher, //the searcher might not be available
            textProcessingConfig, linkerConfig, 
            labelTokenizer);
        //start tracking
        entitySearcher.open();
    }
    /**
     * Deactivates this components. 
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {
        //TODO: 
        //* unregister service
        ServiceRegistration reg = engineRegistration;
        if(reg != null){
            reg.unregister();
            engineRegistration = null;
        }
        //* reset engine
        entityLinkingEngine = null;
        engineMetadata = null;
        //close the tracking EntitySearcher
        entitySearcher.close();
        entitySearcher = null;
    }
    @Override
    public Object addingService(ServiceReference reference) {
        BundleContext bc = this.bundleContext;
        if(bc != null){
            Object service =  bc.getService(reference);
            if(service != null){
                if(trackedServiceCount == 0){
                    //register the service
                    engineRegistration = bc.registerService(
                        new String[]{EnhancementEngine.class.getName(),
                                     ServiceProperties.class.getName()},
                    entityLinkingEngine,
                    engineMetadata);
                    
                }
                trackedServiceCount++;
            }
            return service;
        } else {
            return null;
        }
    }
    @Override
    public void modifiedService(ServiceReference reference, Object service) {
    }
    
    @Override
    public void removedService(ServiceReference reference, Object service) {
        BundleContext bc = this.bundleContext;
        if(bc != null){
            trackedServiceCount--;
            if(trackedServiceCount == 0 && engineRegistration != null){
               engineRegistration.unregister();
            }
            bc.ungetService(reference);
        }
    }
}

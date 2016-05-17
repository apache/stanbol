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
package org.apache.stanbol.enhancer.engines.opennlp.impl;

import static org.apache.stanbol.enhancer.nlp.NlpServiceProperties.ENHANCEMENT_ENGINE_NLP_ROLE;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import opennlp.tools.namefind.TokenNameFinderModel;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.opennlp.OpenNLP;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileListener;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileTracker;
import org.apache.stanbol.enhancer.nlp.NlpProcessingRole;
import org.apache.stanbol.enhancer.nlp.NlpServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Apache Stanbol Enhancer Named Entity Recognition enhancement engine based on opennlp's Maximum Entropy
 * models. In contrast to the {@link NamedEntityExtractionEnhancementEngine} this
 * engine is intended to be used for custom build models. 
 */
@Component(
    metatype = true, 
    immediate = true,
    inherit = true,
    configurationFactory = true, 
    policy = ConfigurationPolicy.REQUIRE,
    specVersion = "1.1", 
    label = "%stanbol.CustomNERModelEnhancementEngine.name", 
    description = "%stanbol.CustomNERModelEnhancementEngine.description")
@Service
@org.apache.felix.scr.annotations.Properties(value={
    @Property(name=EnhancementEngine.PROPERTY_NAME,value="changeme"),
    @Property(name=CustomNERModelEnhancementEngine.NAME_FINDER_MODELS, cardinality=Integer.MAX_VALUE,
    value={"openNlp-namefinder-model-name.bin"}),
    @Property(name=CustomNERModelEnhancementEngine.NAMED_ENTITY_TYPE_MAPPINGS, cardinality=Integer.MAX_VALUE,
        value={"person > http://dbpedia.org/ontology/Person",
               "organization > http://dbpedia.org/ontology/Organisation",
               "location > http://dbpedia.org/ontology/Place"}),
    //set the ranking of the default config to a negative value (ConfigurationPolicy.OPTIONAL) 
    @Property(name=Constants.SERVICE_RANKING,intValue=-100) 
})
@Reference(name="openNLP",referenceInterface=OpenNLP.class, 
    cardinality=ReferenceCardinality.MANDATORY_UNARY,
    policy=ReferencePolicy.STATIC)
public class CustomNERModelEnhancementEngine 
        extends NEREngineCore
        implements EnhancementEngine, ServiceProperties {

    protected final Logger log = LoggerFactory.getLogger(CustomNERModelEnhancementEngine.class);
    
    /**
     * Do hold the named entity type to dc:type value mappings as used for
     * created fise:TextAnnotations. If a named entity type is not mapped than
     * created fise:TextAnnotations will not have any dc:type values
     */
    public static final String NAMED_ENTITY_TYPE_MAPPINGS = "stanbol.engines.opennlp-ner.typeMappings";
    /**
     * Allows to define the list of custom NER models
     */
    public static final String NAME_FINDER_MODELS = "stanbol.engines.opennlp-ner.nameFinderModels";

    /**
     * The default value for the Execution of this Engine. Currently set to
     * {@link ServiceProperties#ORDERING_CONTENT_EXTRACTION}
     */
    public static final Integer defaultOrder = ORDERING_CONTENT_EXTRACTION;

    private static final Map<String,Object> SERVICE_PROPERTIES;
    static {
        Map<String,Object> sp = new HashMap<String,Object>();
        sp.put(ENHANCEMENT_ENGINE_ORDERING,defaultOrder);
        sp.put(ENHANCEMENT_ENGINE_NLP_ROLE, NlpProcessingRole.NamedEntityRecognition);
        SERVICE_PROPERTIES = Collections.unmodifiableMap(sp);
        
    }
    /**
     * Bind method of {@link NEREngineCore#openNLP}
     * @param openNlp
     */
    protected void bindOpenNLP(OpenNLP openNlp){
        this.openNLP = openNlp;
    }
    /**
     * Unbind method of {@link NEREngineCore#openNLP}
     * @param openNLP
     */
    protected void unbindOpenNLP(OpenNLP openNLP){
        this.openNLP = null;
    }
    
    @Reference
    private DataFileTracker dataFileTracker;
    
    private DataFileListener modelFileListener;
    
    protected void activate(ComponentContext ctx) throws IOException, ConfigurationException {
        super.activate(ctx);
        config = new NEREngineConfig();
        config.getDefaultModelTypes().clear(); //this engine does not use default models
        Object value = ctx.getProperties().get(NAMED_ENTITY_TYPE_MAPPINGS);
        if(value instanceof String[]){ //support array
            value = Arrays.asList((String[])value);
        } else if(value instanceof String) { //single value
            value = Collections.singleton(value);
        }
        if(value instanceof Collection<?>){ //and collection
            log.info("Init Named Entity Type Mappings");
            configs :
            for(Object o : (Iterable<?>)value){
                if(o != null){
                    StringBuilder usage = new StringBuilder("useage: ");
                    usage.append("'{namedEntityType} > {dc-type-uri}'");
                    String[] config = o.toString().split(">");
                    String namedEntityType = config[0].trim();
                    if(namedEntityType.isEmpty()){
                        log.warn("Invalid Type Mapping Config '{}': Missing namedEntityType ({}) -> ignore this config",
                            o,usage);
                        continue configs;
                    }
                    if(config.length < 2 || config[1].isEmpty()){
                        log.warn("Invalid Type Mapping Config '{}': Missing dc:type URI '{}' ({}) -> ignore this config",
                            o,usage);
                        continue configs;
                    }
                    String dcTypeUri = config[1].trim();
                    if(config.length > 2){
                        log.warn("Configuration after 2nd '>' gets ignored. Will use mapping '{} > {}' from config {}",
                            new Object[]{namedEntityType,dcTypeUri,o});
                    }
                    //TODO support short names (ns:localName)
                    try { //validate
                        new URI(dcTypeUri);
                    } catch (URISyntaxException e) {
                        log.warn("Invalid URI '{}' in Type Mapping Config '{}' -> ignore this config",
                            dcTypeUri,o);
                        continue configs;
                    }
                    this.config.setMappedType(namedEntityType,new IRI(dcTypeUri));
                    log.info("  add mapping {} > {}",namedEntityType,dcTypeUri);
                }
            }
        } else {
            log.debug("No Type mappings configured");
        }        
        value = ctx.getProperties().get(NAME_FINDER_MODELS);
        Set<String> nameFinderModelNames = new HashSet<String>();
        if(value instanceof String[]){
            nameFinderModelNames.addAll(Arrays.asList((String[]) value));
            nameFinderModelNames.remove(null); //remove null
            nameFinderModelNames.remove(""); //remove empty
        } else if (value instanceof Collection<?>){
            for(Object o : ((Collection<?>)value)){
                if(o != null){
                    nameFinderModelNames.add(o.toString());
                }
            }
            nameFinderModelNames.remove(""); //remove empty
        } else if(value != null && !value.toString().isEmpty()){
            //if a single String is parsed we support ',' as seperator
            String[] languageArray = value.toString().split(",");
            nameFinderModelNames.addAll(Arrays.asList(languageArray));
            nameFinderModelNames.remove(null); //remove null
            nameFinderModelNames.remove(""); //remove empty
        } else {//no configuration
            throw new ConfigurationException(NAME_FINDER_MODELS, "Configurations for the " 
                    + getClass().getSimpleName() +" MUST HAVE at least a single custom "
                    + "OpenNLP NameFinder model configured! Supported are comma separated " 
                    + "Strings, Arrays and Collections. Values are the file names of the " 
                    + "Modles. Models are Loaded via the Apache Stanbol DataFileProvider "
                    + "Infrastructure (usually user wants to copy modles in the 'datafile' "
                    + "directory under the {stanbol.home} directory - {working.dir}/stanbol"
                    + "/datafiles).");
        }
        //register the configured models with the DataFileTracker
        modelFileListener = new NamedModelFileListener();
        Map<String,String> modelProperties = new HashMap<String,String>();
        modelProperties.put("Description", 
            String.format("Statistical NameFinder (NER) model for OpenNLP as configured "
                +"for the %s (name: %s)",getClass().getSimpleName(),getName()));
        modelProperties.put("Model Type", TokenNameFinderModel.class.getSimpleName());
        log.info(" - register DataFileTracker for {}", nameFinderModelNames);
        for(String modelName : nameFinderModelNames){
           dataFileTracker.add(modelFileListener, modelName, modelProperties);
        }
    }

    protected void deactivate(ComponentContext ctx) {
        dataFileTracker.removeAll(modelFileListener); //remove all tracked files
        config = null;
        super.deactivate(ctx);
    }
    
    @Override
    public Map<String,Object> getServiceProperties() {
        return SERVICE_PROPERTIES;
    }

    private class NamedModelFileListener implements DataFileListener {
        
        private Map<String,String> registeredModels = Collections.synchronizedMap(
            new HashMap<String,String>());
        
        @Override
        public boolean available(String resourceName, InputStream is) {
            TokenNameFinderModel model;
            try {
                log.info(" - NER model {} is now available ...", resourceName);
                model = openNLP.getModel(TokenNameFinderModel.class, resourceName, null);
                //register the new model to the configuration
                String modelLang = model.getLanguage().toLowerCase();
                log.info(" - registered custom NameFinderModel from resource: {} for language: {} to {} (name:{})",
                    new Object[]{resourceName,model.getLanguage(),getClass().getSimpleName(),getName()});
                String currentLang = registeredModels.remove(resourceName);
                if(currentLang != null && !modelLang.equals(currentLang)){
                    config.removeCustomNameFinderModel(currentLang, resourceName);
                }
                config.addCustomNameFinderModel(modelLang, resourceName);
                registeredModels.put(resourceName, modelLang);
            } catch (IOException e) {
                log.warn("Error while loading custom TokenNameFinderModel model from resource " +
                        resourceName + ". This model will NOT be available for the "+
                        getClass().getSimpleName()+" (name:"+getName()+")",e);
            } catch (RuntimeException e){
                log.warn("Error while loading custom TokenNameFinderModel model from resource " +
                        resourceName + ". This model will NOT be available for the "+
                        getClass().getSimpleName()+" (name:"+getName()+")",e);
            }
            return false; //keep tracking
        }

        @Override
        public boolean unavailable(String resource) {
            String language  = registeredModels.remove(resource);
            if(language != null){
                log.info("unregister custom NameFinderModel for resource: {} for language: {} to {} (name:{})" +
                		"because the resource is no longer available via the DataFileProvider infrastructure.",
                		new Object[]{resource,language,getClass().getSimpleName(),getName()});
                config.removeCustomNameFinderModel(language, resource);
            }
            return false; //keep tracking
        }
        
    }

}
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
package org.apache.stanbol.cmsadapter.core.mapping;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.RDFBridgeConfiguration;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main aim of this class is to provide ability to create {@code RDFBridgeConfiguration}s through the
 * configuration interface of Felix console. Currently there are 5 configuration field:
 * 
 * <ul>
 * <li><b>Resource selector:</b></li> This property is used to filter resources from the RDF data. It should
 * have the following syntax:<br>
 * <br>
 * rdf:Type > skos:Concept <br>
 * <br>
 * This example states that triples having <b>rdf:Type</b> predicate and <b>skos:Concept</b> object will be
 * filtered. And subject of selected triples indicates the resource to be created as node/object in the
 * repository. It is also acceptable to pass full URIs such as <br>
 * http://www.w3.org/1999/02/22-rdf-syntax-ns#Type > http://www.w3.org/2004/02/skos/core#Concept<br>
 * <li><b>Name:</b></li> This property indicates the predicate which points to the name of node/object to be
 * created in the repository. It should indicate a single URI such as <b>rdfs:label</b> or
 * <b>http://www.w3.org/2000/01/rdf-schema#label</b>. Actually name value is obtained through the triple
 * (s,p,o) where s is one of the subjects filtered by the "Resource Selector" configuration parameter, p is
 * this parameter.<br>
 * <li><b>Properties:</b></li> This property specifies the properties of nodes/objects to be created in the
 * repository. Value of this configuration should be like <b>skos:Definition > definition</b> or
 * <b>skos:Definition</b>. First option states that skos:Definition property of a filtered subject will be
 * created as a property having name "definition" of repository object. In the second case the name of the
 * property will directly be "skos:Definition".
 * <li><b>Children:</b></li> This property specifies the children of nodes/objecs to be created in the
 * repository. Value of this configuration should be like <b>skos:narrower > narrowerObject</b> or
 * <b>skos:narrower > rdfs:label</b>. First option has same logic with the previous parameter. In the second
 * case, rdfs:label of resource representing child object will be set as the name of child object/node in the
 * repository. This option would be useful to create hierarchies.
 * </ul>
 * 
 * @author suat
 * 
 */
@Component(configurationFactory = true, metatype = true, immediate = true)
@Service(value = RDFBridgeConfiguration.class)
@Properties(value = {
                     @Property(name = DefaultRDFBridgeConfigurationImpl.PROP_RESOURCE_SELECTOR, value = "rdf:Type > skos:Concept"),
                     @Property(name = DefaultRDFBridgeConfigurationImpl.PROP_NAME, value = "rdfs:label"),
                     @Property(name = DefaultRDFBridgeConfigurationImpl.PROP_PROPERTIES, cardinality = 1000, value = {
                                                                                                                      "skos:related > relatedWith",
                                                                                                                      "skos:definition > definition"}),
                     @Property(name = DefaultRDFBridgeConfigurationImpl.PROP_CHILDREN, cardinality = 1000, value = {"skos:narrower > narrowerConcept"})})
public class DefaultRDFBridgeConfigurationImpl implements RDFBridgeConfiguration {

    public static final String PROP_RESOURCE_SELECTOR = "org.apache.stanbol.cmsadapter.rdfbridge.resourceSelector";
    public static final String PROP_PROPERTIES = "org.apache.stanbol.cmsadapter.rdfbridge.properties";
    public static final String PROP_NAME = "org.apache.stanbol.cmsadapter.rdfbridge.name";
    public static final String PROP_CHILDREN = "org.apache.stanbol.cmsadapter.rdfbridge.children";

    private static final Logger log = LoggerFactory.getLogger(DefaultRDFBridgeConfigurationImpl.class);

    private UriRef targetResourcePredicate;
    private UriRef targetResourceValue;
    private UriRef nameResource;
    Map<UriRef,Object> targetPropertyMappings = new HashMap<UriRef,Object>();
    Map<UriRef,Object> targetChildrenMappings = new HashMap<UriRef,Object>();

    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) throws ConfigurationException {
        Dictionary<String,Object> properties = (Dictionary<String,Object>) context.getProperties();
        parseTargetResourceConfig(properties);
        parsePropertyMappings(properties);
        parseChilrenMappings(properties);
        this.nameResource = parseUriRefFromConfig((String) checkProperty(properties, PROP_NAME, true));
    }

    @Override
    public UriRef getTargetResourcePredicate() {
        return this.targetResourcePredicate;
    }

    @Override
    public UriRef getTargetResourceValue() {
        return this.targetResourceValue;
    }

    @Override
    public UriRef getNameResource() {
        return this.nameResource;
    }

    @Override
    public Map<UriRef,Object> getTargetPropertyResources() {
        return this.targetPropertyMappings;
    }

    @Override
    public Map<UriRef,Object> getChildrenResources() {
        return this.targetChildrenMappings;
    }

    private void parseTargetResourceConfig(Dictionary<String,Object> properties) throws ConfigurationException {
        String targetResourceConfig = (String) checkProperty(properties, PROP_RESOURCE_SELECTOR, true);
        String[] configParts = parseConfigParts(targetResourceConfig, true);

        this.targetResourcePredicate = parseUriRefFromConfig(configParts[0]);
        this.targetResourceValue = parseUriRefFromConfig(configParts[1]);
    }

    private void parsePropertyMappings(Dictionary<String,Object> properties) {
        Object value = null;
        try {
            value = checkProperty(properties, PROP_PROPERTIES, false);
        } catch (ConfigurationException e) {
            // not the case
        }
        if (value != null) {
            if (value instanceof String) {
                String config = (String) value;
                try {
                    String[] configParts = parseConfigParts(config, false);
                    this.targetPropertyMappings.put(parseUriRefFromConfig(configParts[0]),
                        configParts[configParts.length - 1]);
                } catch (ConfigurationException e) {
                    log.warn("Failed to parse configuration value: {}", config);
                    log.warn("Configuration value should be in the format e.g skos:Definition > definition");
                }

            } else if (value instanceof String[]) {
                for (String config : (String[]) value) {
                    try {
                        String[] configParts = parseConfigParts(config, false);
                        this.targetPropertyMappings.put(parseUriRefFromConfig(configParts[0]),
                            configParts[configParts.length - 1]);
                    } catch (ConfigurationException e) {
                        log.warn("Failed to parse configuration value: {}", config);
                        log.warn("Configuration value should be in the format e.g skos:Definition > definition");
                    }
                }
            }
        }
    }

    private void parseChilrenMappings(Dictionary<String,Object> properties) {
        Object value = null;
        try {
            value = checkProperty(properties, PROP_CHILDREN, false);
        } catch (ConfigurationException e) {
            // not the case
        }
        if (value != null) {
            if (value instanceof String) {
                String config = (String) value;
                try {
                    String[] configParts = parseConfigParts(config, true);
                    this.targetChildrenMappings.put(parseUriRefFromConfig(configParts[0]),
                        parsePropertyName(configParts[1], true));
                } catch (ConfigurationException e) {
                    log.warn("Failed to parse configuration value: {}", config);
                    log.warn("Configuration value should be in the format e.g skos:Definition > definition");
                }

            } else if (value instanceof String[]) {
                for (String config : (String[]) value) {
                    try {
                        String[] configParts = parseConfigParts(config, true);
                        this.targetChildrenMappings.put(parseUriRefFromConfig(configParts[0]),
                            parsePropertyName(configParts[1], true));
                    } catch (ConfigurationException e) {
                        log.warn("Failed to parse configuration value: {}", config);
                        log.warn("Configuration value should be in the format e.g skos:Definition > definition");
                    }
                }
            }
        }
    }

    private UriRef parseUriRefFromConfig(String config) {
        return new UriRef(NamespaceEnum.getFullName(config));
    }

    private Object parsePropertyName(String config, boolean resolveURI) {
        if (config.contains(":") && resolveURI) {
            return new UriRef(NamespaceEnum.getFullName(config));
        } else {
            return config;
        }
    }
    
    private Object checkProperty(Dictionary<String,Object> properties, String key, boolean required) throws ConfigurationException {
        Object value = properties.get(key);
        if (value == null) {
            if (required) {
                throw new ConfigurationException(key, "Failed to get value for this property");
            } else {
                return null;
            }
        } else {
            return value;
        }
    }

    private String[] parseConfigParts(String config, boolean twoParts) throws ConfigurationException {
        String[] configParts = config.split(">");
        int parts = configParts.length;
        if (parts != 2 && twoParts) {
            throw new ConfigurationException(PROP_RESOURCE_SELECTOR,
                    "Target resource and resource value should be seperated by a single'>' sign");
        }

        if (parts == 1 || parts == 2) {
            configParts[0] = configParts[0].trim();
            if (parts == 2) {
                configParts[1] = configParts[1].trim();
            }
        } else {
            throw new ConfigurationException(PROP_RESOURCE_SELECTOR,
                    "Target resource and resource value should be seperated by a single'>' sign");
        }
        return configParts;
    }
}

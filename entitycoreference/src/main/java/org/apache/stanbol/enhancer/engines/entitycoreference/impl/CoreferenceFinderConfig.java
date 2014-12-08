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
package org.apache.stanbol.enhancer.engines.entitycoreference.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.enhancer.engines.entitycoreference.Constants;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;
import org.osgi.service.cm.ConfigurationException;

/**
 * Contains configuration parameters for the {@link CoreferenceFinder}.
 * 
 * @author Cristian Petroaca
 * 
 */
public class CoreferenceFinderConfig {
    private static final String NAMED_ENTITY_CONFIG = "named_entity.properties";
    private static final String SPATIAL_PLACE_ATTRIBUTES_PROP = "spatial.ont.place.attributes";
    private static final String SPATIAL_ORG_ATTRIBUTES_PROP = "spatial.ont.organisation.attributes";
    private static final String SPATIAL_PERSON_ATTRIBUTES_PROP = "spatial.ont.person.attributes";
    private static final String ENTITY_CLASSES_TO_EXCLUDE_PROP = "entity.classes.to.exclude";

    /**
     * The maximum distance (in sentence numbers) between a NER and a {@link NounPhrase} for which we look for
     * a coreference.
     */
    private int maxDistance;

    /**
     * The Uris for spatial properties for the NER to be inspected when doing the coref spatial match.
     */
    private Map<UriRef,Set<String>> spatialRulesOntology;

    /**
     * Entity classes which will not be used for coreference because they are too general.
     */
    private Set<String> entityClassesToExclude;

    public CoreferenceFinderConfig(int maxDistance) throws ConfigurationException {
        // First read the ontology from config used for entity properties matching
        Properties props = new Properties();
        InputStream in = null;

        try {
            in = CoreferenceFinderConfig.class.getResourceAsStream(Constants.CONFIG_FOLDER + "/"
                                                                   + NAMED_ENTITY_CONFIG);
            props.load(in);
        } catch (IOException e) {
            throw new ConfigurationException("", "Could not read " + NAMED_ENTITY_CONFIG);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {}
            }
        }

        this.spatialRulesOntology = new HashMap<UriRef,Set<String>>();
        Set<String> attributes = new HashSet<String>();

        String placeAttributes = props.getProperty(SPATIAL_PLACE_ATTRIBUTES_PROP);
        if (placeAttributes == null || placeAttributes.isEmpty()) {
            throw new ConfigurationException(SPATIAL_PLACE_ATTRIBUTES_PROP, "Missing property in "
                                                                            + NAMED_ENTITY_CONFIG);
        }
        for (String attribute : placeAttributes.split(",")) {
            attributes.add(attribute);
        }
        this.spatialRulesOntology.put(OntologicalClasses.DBPEDIA_PLACE, attributes);

        String orgAttributes = props.getProperty(SPATIAL_ORG_ATTRIBUTES_PROP);
        if (orgAttributes == null || placeAttributes.isEmpty()) {
            throw new ConfigurationException(SPATIAL_ORG_ATTRIBUTES_PROP, "Missing property in "
                                                                          + NAMED_ENTITY_CONFIG);
        }
        attributes.clear();
        for (String attribute : orgAttributes.split(",")) {
            attributes.add(attribute);
        }
        this.spatialRulesOntology.put(OntologicalClasses.DBPEDIA_ORGANISATION, attributes);

        String personAttributes = props.getProperty(SPATIAL_PERSON_ATTRIBUTES_PROP);
        if (personAttributes == null || placeAttributes.isEmpty()) {
            throw new ConfigurationException(SPATIAL_PERSON_ATTRIBUTES_PROP, "Missing property in "
                                                                             + NAMED_ENTITY_CONFIG);
        }
        attributes.clear();
        for (String attribute : personAttributes.split(",")) {
            attributes.add(attribute);
        }
        this.spatialRulesOntology.put(OntologicalClasses.DBPEDIA_PERSON, attributes);

        this.maxDistance = maxDistance;

        String entityClassesToExcludeString = props.getProperty(ENTITY_CLASSES_TO_EXCLUDE_PROP);
        if (entityClassesToExcludeString != null && !entityClassesToExcludeString.isEmpty()) {
            this.entityClassesToExclude = new HashSet<String>();

            for (String clazz : entityClassesToExcludeString.split(",")) {
                this.entityClassesToExclude.add(clazz);
            }
        }
    }

    /**
     * Gets the max distance parameter.
     * 
     * @return
     */
    public int getMaxDistance() {
        return maxDistance;
    }

    /**
     * Gets the URIs for the spatial properties for a given Entity Type.
     * 
     * @param uri
     *            of the Entity type for which we want to get the ontology.
     * @return
     */
    public Set<String> getSpatialOntology(UriRef uri) {
        return this.spatialRulesOntology.get(uri);
    }

    /**
     * Checks whether we should exclude the given class based on our config.
     * 
     * @param clazz
     * @return
     */
    public boolean shouldExcludeClass(String clazz) {
        return this.entityClassesToExclude.contains(clazz);
    }
}

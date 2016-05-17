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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.clerezza.commons.rdf.IRI;

import org.apache.stanbol.enhancer.engines.entitycoreference.datamodel.NounPhrase;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;
import org.osgi.service.cm.ConfigurationException;

/**
 * Contains configuration parameters for the {@link CoreferenceFinder}.
 * 
 * @author Cristian Petroaca
 * 
 */
public class CoreferenceFinderConfig {
    /**
     * The maximum distance (in sentence numbers) between a NER and a {@link NounPhrase} for which we look for
     * a coreference.
     */
    private int maxDistance;

    /**
     * The Uris for spatial properties for the NER to be inspected when doing the coref spatial match.
     */
    private Map<IRI,Set<String>> spatialAttributes;
    
    /**
     * The Uris for org membership properties for the NER to be inspected when doing the coref match.
     */
    private Map<IRI,Set<String>> orgMembershipAttributes;

    /**
     * Entity classes which will not be used for coreference because they are too general.
     */
    private Set<String> entityClassesToExclude;

    public CoreferenceFinderConfig(int maxDistance,
    							   String spatialAttrForPerson,
						           String spatialAttrForOrg,
						           String spatialAttrForPlace,
						           String orgAttrForPerson,
						           String entityClassesToExclude) throws ConfigurationException {
    	this.maxDistance = maxDistance;
    	
    	this.spatialAttributes = new HashMap<IRI,Set<String>>();
    	this.orgMembershipAttributes = new HashMap<IRI, Set<String>>();
    	
        if (spatialAttrForPerson != null) {
        	Set<String> attributes = new HashSet<String>();
	        for (String attribute : spatialAttrForPerson.split(",")) {
	            attributes.add(attribute);
	        }
	        this.spatialAttributes.put(OntologicalClasses.DBPEDIA_PERSON, attributes);
        }
        
        if (spatialAttrForOrg != null) {
        	Set<String> attributes = new HashSet<String>();
	        for (String attribute : spatialAttrForOrg.split(",")) {
	            attributes.add(attribute);
	        }
	        this.spatialAttributes.put(OntologicalClasses.DBPEDIA_ORGANISATION, attributes);
        }
        
        
        if (spatialAttrForPlace != null) {
        	Set<String> attributes = new HashSet<String>();
	        for (String attribute : spatialAttrForPlace.split(",")) {
	            attributes.add(attribute);
	        }
	        this.spatialAttributes.put(OntologicalClasses.DBPEDIA_PLACE, attributes);
        }
        
        if (orgAttrForPerson != null) {
        	Set<String> attributes = new HashSet<String>();
	        for (String attribute : orgAttrForPerson.split(",")) {
	            attributes.add(attribute);
	        }
	        
	        this.orgMembershipAttributes.put(OntologicalClasses.DBPEDIA_PERSON, attributes);
        }
        
        if (entityClassesToExclude != null) {
            this.entityClassesToExclude = new HashSet<String>();

            for (String clazz : entityClassesToExclude.split(",")) {
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
    public Set<String> getSpatialAttributes(IRI uri) {
        return this.spatialAttributes.get(uri);
    }

    /**
     * Gets the URIs for the org membership properties for a given Entity Type.
     * 
     * @param uri
     *            of the Entity type for which we want to get the ontology.
     * @return
     */
    public Set<String> getOrgMembershipAttributes(IRI uri) {
        return this.orgMembershipAttributes.get(uri);
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

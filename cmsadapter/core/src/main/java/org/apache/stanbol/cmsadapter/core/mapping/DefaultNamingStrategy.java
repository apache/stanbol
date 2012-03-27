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

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.stanbol.cmsadapter.servicesapi.helper.CMSAdapterVocabulary;
import org.apache.stanbol.cmsadapter.servicesapi.helper.OntologyResourceHelper;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.NamingStrategy;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccess;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.RDFList;

public class DefaultNamingStrategy implements NamingStrategy {
    private static final Logger log = LoggerFactory.getLogger(DefaultNamingStrategy.class);

    /**
     * Used to separate any path segment
     */
    public static final Character PATH_DELIMITER = '/';

    private static final String TYPE_SEPARATOR = "-";
    private static final String CLASS_DELIMITER = "class";
    private static final String INDIVIDUAL_DELIMITER = "individual";
    private static final String PROPERTY_DELIMITER = "prop";
    private static final String OBJECT_PROPERTY_DELIMITER = "oprop";
    private static final String DATA_PROPERTY_DELIMITER = "dprop";
    private static final String UNION_CLASS_DELIMITER = "unions";

    private RepositoryAccess repositoryAccess;
    private Object session;
    private OntModel processedModel;

    public DefaultNamingStrategy(RepositoryAccess repositoryAccess, Object session, OntModel processedModel) {
        this.repositoryAccess = repositoryAccess;
        this.session = session;
        this.processedModel = processedModel;
    }

    @Override
    public String getClassName(String ontologyURI, CMSObject cmsObject) {
        List<String> candidateNames = Arrays.asList(new String[] {cmsObject.getLocalname(),
                                                                  cmsObject.getPath(),
                                                                  cmsObject.getUniqueRef()});
        return getAvailableResourceName(ontologyURI, candidateNames, CLASS_DELIMITER);
    }

    @Override
    public String getClassName(String ontologyURI, ObjectTypeDefinition objectTypeDefinition) {
        List<String> candidateNames = Arrays.asList(new String[] {objectTypeDefinition.getLocalname()});
        return getAvailableResourceName(ontologyURI, candidateNames, CLASS_DELIMITER);
    }

    @Override
    public String getClassName(String ontologyURI, String reference) {
        List<String> candidateNames = Arrays.asList(new String[] {reference});
        return getAvailableResourceName(ontologyURI, candidateNames, CLASS_DELIMITER);
    }

    @Override
    public String getIndividualName(String ontologyURI, CMSObject cmsObject) {
        List<String> candidateNames = Arrays.asList(new String[] {cmsObject.getLocalname(),
                                                                  cmsObject.getPath(),
                                                                  cmsObject.getUniqueRef()});
        return getAvailableResourceName(ontologyURI, candidateNames, INDIVIDUAL_DELIMITER);
    }

    @Override
    public String getObjectPropertyName(String ontologyURI, PropertyDefinition propertyDefinition) {
        List<String> candidateNames = Arrays.asList(new String[] {propertyDefinition.getLocalname(),
                                                                  propertyDefinition.getUniqueRef()});
        return getAvailableResourceName(ontologyURI, candidateNames, OBJECT_PROPERTY_DELIMITER);
    }

    @Override
    public String getDataPropertyName(String ontologyURI, PropertyDefinition propertyDefinition) {
        List<String> candidateNames = Arrays.asList(new String[] {propertyDefinition.getLocalname(),
                                                                  propertyDefinition.getUniqueRef()});
        return getAvailableResourceName(ontologyURI, candidateNames, DATA_PROPERTY_DELIMITER);
    }

    @Override
    public String getUnionClassURI(String ontologyURI, RDFList list) {
        List<String> candidateNames = Arrays.asList(new String[] {RandomStringUtils.randomAlphanumeric(4)});
        return getAvailableResourceName(ontologyURI, candidateNames, UNION_CLASS_DELIMITER);
    }

    @Override
    public String getIndividualName(String ontologyURI, String reference) {
        List<String> candidateNames = Arrays.asList(new String[] {reference});
        return getAvailableResourceName(ontologyURI, candidateNames, INDIVIDUAL_DELIMITER);
    }

    @Override
    public String getPropertyName(String ontologyURI, String reference) {
        List<String> candidateNames = Arrays.asList(new String[] {reference});
        return getAvailableResourceName(ontologyURI, candidateNames, PROPERTY_DELIMITER);
    }

    @Override
    public String getObjectPropertyName(String ontologyURI, String reference) {
        List<String> candidateNames = Arrays.asList(new String[] {reference});
        return getAvailableResourceName(ontologyURI, candidateNames, OBJECT_PROPERTY_DELIMITER);
    }

    @Override
    public String getDataPropertyName(String ontologyURI, String reference) {
        List<String> candidateNames = Arrays.asList(new String[] {reference});
        return getAvailableResourceName(ontologyURI, candidateNames, DATA_PROPERTY_DELIMITER);
    }

    private String getAvailableResourceName(String ontologyURI,
                                            List<String> candidates,
                                            String resourceDelimiter) {
        ResourceNameRepresentation nameRep;
        StringBuilder sb;
        String name;

        for (String candidate : candidates) {
            nameRep = getPrecedingBaseURI(candidate, ontologyURI);
            sb = new StringBuilder(nameRep.getResourceBaseURI());
            sb.append(resourceDelimiter).append(TYPE_SEPARATOR);
            sb.append(nameRep.getResourceName());
            name = sb.toString();

            if (processedModel.getOntResource(name) == null) {
                log.debug("Generated resource name {} from {}.", name, candidate);
                return name;
            }
        }

        log.warn("No suitable URI produced for candidates {}.", candidates);
        return null;
    }

    private ResourceNameRepresentation getPrecedingBaseURI(String resourceName, String ontologyURI) {
        ResourceNameRepresentation resourceNameRep = new ResourceNameRepresentation();

        if (resourceName.contains(":")) {
            String newURI;
            String prefix = detectPrefix(resourceName);
            resourceNameRep.setResourceName(resourceName);

            // first try to resolve prefix through processed OntModel
            newURI = processedModel.getNsPrefixURI(prefix);
            if (newURI != null) {
                resourceNameRep.setResourceBaseURI(OntologyResourceHelper.addResourceDelimiter(newURI));
                return resourceNameRep;
            }

            // second try to resolve prefix through content repository itself
            if (repositoryAccess != null && session != null) {
                try {
                    newURI = repositoryAccess.getNamespaceURI(prefix, session);
                    if (newURI != null) {
                        resourceNameRep.setResourceBaseURI(OntologyResourceHelper
                                .addResourceDelimiter(newURI));
                        return resourceNameRep;
                    }
                } catch (RepositoryAccessException e) {
                    log.warn("Cannot access repository.", e);
                }
            }

            // prefix still cannot be resolved, create a dummy URI
            newURI = CMSAdapterVocabulary.DEFAULT_NS_URI + PATH_DELIMITER + prefix;
            resourceNameRep.setResourceBaseURI(OntologyResourceHelper.addResourceDelimiter(newURI));
            processedModel.setNsPrefix(prefix, newURI);

            log.warn("Cannot find URI for prefix {}. Created dummy URI {}.", prefix, newURI);

        } else {
            resourceNameRep.setResourceName(resourceName);
            resourceNameRep.setResourceBaseURI(OntologyResourceHelper.addResourceDelimiter(ontologyURI));
        }
        return resourceNameRep;
    }

    private String detectPrefix(String resourceName) {
        int colonIndex = resourceName.lastIndexOf(":");
        String prefix = "";
        for (int i = colonIndex - 1; i >= 0 && resourceName.charAt(i) != PATH_DELIMITER; i--) {
            prefix = resourceName.charAt(i) + prefix;
        }
        return prefix;
    }
}

class ResourceNameRepresentation {
    private String resourceName;
    private String resourceBaseURI;

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getResourceName() {
        return this.resourceName;
    }

    public void setResourceBaseURI(String resourceBaseURI) {
        this.resourceBaseURI = resourceBaseURI;
    }

    public String getResourceBaseURI() {
        return this.resourceBaseURI;
    }
}

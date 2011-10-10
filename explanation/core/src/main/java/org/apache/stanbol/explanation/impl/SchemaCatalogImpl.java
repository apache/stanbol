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
package org.apache.stanbol.explanation.impl;

import java.io.IOException;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.explanation.api.Configuration;
import org.apache.stanbol.explanation.api.Schema;
import org.apache.stanbol.explanation.api.SchemaCatalog;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScope;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of a knowledge schema.
 * 
 * @author alessandro
 * 
 */
@Component(metatype = true, immediate = true, configurationFactory = true, policy = ConfigurationPolicy.REQUIRE, specVersion = "1.1")
@Service
@Properties(value = {@Property(name = SchemaCatalog.ID), @Property(name = SchemaCatalog.LOCATION),
                     @Property(name = SchemaCatalog.CUSTOM_SCHEMAS, cardinality = 1000)})
public class SchemaCatalogImpl implements SchemaCatalog {

    private String id;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Map<IRI,Schema> schemas = new HashMap<IRI,Schema>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private Configuration config;

    public SchemaCatalogImpl() {
        super();
    }

    public SchemaCatalogImpl(String id, Configuration config) {
        this();
        // Copy the id over to a hashtable
        Dictionary<String,Object> componentConfig = new Hashtable<String,Object>();
        componentConfig.put(SchemaCatalog.ID, id);
        this.config = config;
        try {
            activate(componentConfig);
        } catch (IOException e) {
            log.error("Unable to access servlet context.", e);
        }
    }

    /**
     * Used to configure an instance within an OSGi container.
     * 
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) throws IOException {
        log.info("in " + ExplanationGeneratorImpl.class + " activate with context " + context);
        if (context == null) {
            throw new IllegalStateException("No valid" + ComponentContext.class + " parsed in activate!");
        }
        activate((Dictionary<String,Object>) context.getProperties());
    }

    /**
     * Called within both OSGi and non-OSGi environments.
     * 
     * @param configuration
     * @throws IOException
     */
    protected void activate(Dictionary<String,Object> configuration) throws IOException {

        id = (String) configuration.get(SchemaCatalog.ID);
        if (id == null || id.trim().isEmpty()) id = "unnamed-" + new Date().getTime();

        ONManager onm = config.getOntologyNetworkManager();

        // Get the scope and populate the custom space with the registry ontologies.
        String registry = (String) configuration.get(SchemaCatalog.LOCATION);
        if (registry != null && !id.trim().isEmpty()) {
            OntologyScope scope = onm.getScopeRegistry().getScope(config.getScopeID());

//            // TODO: to cache or not to cache?
//            OntologyInputSource src = new OntologyRegistryIRISource(IRI.create(registry),
//                    onm.getOwlCacheManager(), onm.getRegistryLoader());
//            try {
//                scope.getCustomSpace().addOntology(src);
//            } catch (UnmodifiableOntologySpaceException e) {
//                log.warn("Could not add registry {} to unmodifiable space {}.", registry,
//                    scope.getCustomSpace());
//            }
        }

        log.debug("Schema Catalog activated.");

    }

    @Override
    public void addSchema(Schema schema) {
        schemas.put(schema.getID(), schema);
    }

    @Override
    public void clearSchems() {
        schemas.clear();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Schema getSchema(IRI schemaID) {
        return schemas.get(schemaID);
    }

    @Override
    public Set<Schema> getSchemas() {
        return new HashSet<Schema>(schemas.values());
    }

    @Override
    public boolean hasSchema(IRI schemaID) {
        return schemas.containsKey(schemaID);
    }

    @Override
    public void removeSchema(IRI schemaId) {
        schemas.remove(schemaId);
    }

    @Override
    public void removeSchema(Schema schema) {
        Schema sc = schemas.get(schema.getID());
        if (sc != null && sc.equals(schema)) schemas.remove(schema.getID());
    }

}
